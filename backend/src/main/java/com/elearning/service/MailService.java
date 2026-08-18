package com.elearning.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails. When app.mail.enabled=false (the local-dev default), emails
 * are logged instead of sent, so you can develop without configuring real SMTP credentials.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled}")
    private boolean mailEnabled;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public void sendVerificationEmail(String toEmail, String name, String token) {
        String link = frontendBaseUrl + "/verify-email?token=" + token;
        String subject = "Verify your LearnHub email";
        String body = "Hi " + name + ",\n\n"
                + "Welcome to LearnHub! Please verify your email address by visiting the link below:\n\n"
                + link + "\n\n"
                + "This link expires in 24 hours. If you didn't create this account, you can ignore this email.\n\n"
                + "— The LearnHub team";
        send(toEmail, subject, body);
    }

    public void sendPasswordResetEmail(String toEmail, String name, String token) {
        String link = frontendBaseUrl + "/reset-password?token=" + token;
        String subject = "Reset your LearnHub password";
        String body = "Hi " + name + ",\n\n"
                + "We received a request to reset your password. Visit the link below to choose a new one:\n\n"
                + link + "\n\n"
                + "This link expires in 1 hour. If you didn't request this, you can safely ignore this email — "
                + "your password will not be changed.\n\n"
                + "— The LearnHub team";
        send(toEmail, subject, body);
    }

    public void sendPurchaseReceipt(String toEmail, String name, String courseTitle, String amount) {
        String subject = "Your LearnHub receipt: " + courseTitle;
        String body = "Hi " + name + ",\n\n"
                + "Thanks for your purchase! Here's your receipt:\n\n"
                + "Course: " + courseTitle + "\n"
                + "Amount: $" + amount + "\n\n"
                + "You can start learning right away from your dashboard.\n\n"
                + "— The LearnHub team";
        send(toEmail, subject, body);
    }

    private void send(String to, String subject, String body) {
        if (!mailEnabled) {
            log.info("[MAIL DISABLED] Would send to={} subject=\"{}\"\n{}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            // Never let a mail failure break the calling transaction (e.g. registration, purchase).
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
