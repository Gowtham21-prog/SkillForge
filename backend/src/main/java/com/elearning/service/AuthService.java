package com.elearning.service;

import com.elearning.dto.AuthDtos.*;
import com.elearning.entity.RefreshToken;
import com.elearning.entity.User;
import com.elearning.entity.VerificationToken;
import com.elearning.entity.VerificationToken.TokenType;
import com.elearning.exception.ApiException;
import com.elearning.repository.UserRepository;
import com.elearning.repository.VerificationTokenRepository;
import com.elearning.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final com.elearning.security.CustomUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final MailService mailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Email is already registered", 400);
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : User.Role.STUDENT);
        user.setEmailVerified(false);
        user.setAccountEnabled(true);

        userRepository.save(user);

        // Fire off a verification email (failure here does not break registration)
        issueVerificationToken(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (org.springframework.security.authentication.DisabledException e) {
            throw new ApiException("This account has been disabled. Contact support.", 403);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("User not found", 404));

        return buildAuthResponse(user);
    }

    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        RefreshToken existing = refreshTokenService.verify(request.getRefreshToken());
        User user = existing.getUser();

        // Rotate: revoke the old token, issue a new pair (mitigates replay of stolen refresh tokens)
        refreshTokenService.revoke(existing.getToken());
        RefreshToken newRefresh = refreshTokenService.createRefreshToken(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtil.generateToken(userDetails);

        return new RefreshResponse(accessToken, newRefresh.getToken());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        VerificationToken token = verificationTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ApiException("Invalid or expired verification link", 400));

        if (token.getType() != TokenType.EMAIL_VERIFICATION) {
            throw new ApiException("Invalid token type", 400);
        }
        if (token.isUsed()) {
            throw new ApiException("This verification link has already been used", 400);
        }
        if (token.isExpired()) {
            throw new ApiException("This verification link has expired. Please request a new one.", 400);
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        token.setUsed(true);
        verificationTokenRepository.save(token);
    }

    @Transactional
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("No account found with that email", 404));

        if (user.isEmailVerified()) {
            throw new ApiException("Email is already verified", 400);
        }
        issueVerificationToken(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always behave the same whether or not the email exists, to avoid leaking which
        // emails are registered. The controller returns a generic message either way.
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            VerificationToken token = new VerificationToken();
            token.setUser(user);
            token.setToken(UUID.randomUUID().toString());
            token.setType(TokenType.PASSWORD_RESET);
            token.setExpiresAt(LocalDateTime.now().plusHours(1));
            verificationTokenRepository.save(token);

            mailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token.getToken());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        VerificationToken token = verificationTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ApiException("Invalid or expired reset link", 400));

        if (token.getType() != TokenType.PASSWORD_RESET) {
            throw new ApiException("Invalid token type", 400);
        }
        if (token.isUsed()) {
            throw new ApiException("This reset link has already been used", 400);
        }
        if (token.isExpired()) {
            throw new ApiException("This reset link has expired. Please request a new one.", 400);
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        verificationTokenRepository.save(token);

        // Invalidate all existing sessions on password change
        refreshTokenService.revokeAllForUser(user);
    }

    private void issueVerificationToken(User user) {
        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setType(TokenType.EMAIL_VERIFICATION);
        token.setExpiresAt(LocalDateTime.now().plusHours(24));
        verificationTokenRepository.save(token);

        mailService.sendVerificationEmail(user.getEmail(), user.getName(), token.getToken());
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtil.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
