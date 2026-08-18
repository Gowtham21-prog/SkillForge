package com.elearning.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory per-IP rate limiter for the login and registration endpoints.
 * For a multi-instance deployment, swap the in-memory bucket map for a Redis-backed
 * Bucket4j proxy manager so limits are shared across nodes.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.ratelimit.login-attempts}")
    private int loginAttempts;

    @Value("${app.ratelimit.login-window-minutes}")
    private int loginWindowMinutes;

    @Value("${app.ratelimit.register-attempts}")
    private int registerAttempts;

    @Value("${app.ratelimit.register-window-minutes}")
    private int registerWindowMinutes;

    private final ConcurrentHashMap<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> registerBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = extractClientIp(request);

        Bucket bucket = null;
        if (path.endsWith("/auth/login")) {
            bucket = loginBuckets.computeIfAbsent(ip, k -> newBucket(loginAttempts, loginWindowMinutes));
        } else if (path.endsWith("/auth/register")) {
            bucket = registerBuckets.computeIfAbsent(ip, k -> newBucket(registerAttempts, registerWindowMinutes));
        }

        if (bucket != null) {
            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\":429,\"message\":\"Too many attempts. Please wait a few minutes and try again.\"}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket newBucket(int attempts, int windowMinutes) {
        Bandwidth limit = Bandwidth.classic(attempts, Refill.intervally(attempts, Duration.ofMinutes(windowMinutes)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.endsWith("/auth/login") || path.endsWith("/auth/register"));
    }
}
