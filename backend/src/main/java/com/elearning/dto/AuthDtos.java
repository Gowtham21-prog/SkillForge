package com.elearning.dto;

import com.elearning.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDtos {

    @Data
    public static class RegisterRequest {
        @NotBlank
        private String name;

        @NotBlank @Email
        private String email;

        @NotBlank @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        private User.Role role = User.Role.STUDENT;
    }

    @Data
    public static class LoginRequest {
        @NotBlank @Email
        private String email;

        @NotBlank
        private String password;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthResponse {
        private String token;
        private String refreshToken;
        private Long id;
        private String name;
        private String email;
        private String role;
    }

    @Data
    public static class RefreshRequest {
        @NotBlank
        private String refreshToken;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RefreshResponse {
        private String token;
        private String refreshToken;
    }

    @Data
    public static class LogoutRequest {
        @NotBlank
        private String refreshToken;
    }

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank @Email
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank
        private String token;

        @NotBlank @Size(min = 6, message = "Password must be at least 6 characters")
        private String newPassword;
    }

    @Data
    public static class VerifyEmailRequest {
        @NotBlank
        private String token;
    }
}
