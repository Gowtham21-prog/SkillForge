package com.elearning.service;

import com.elearning.entity.Course;
import com.elearning.entity.Payment;
import com.elearning.entity.User;
import com.elearning.exception.ApiException;
import com.elearning.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Wraps Stripe Checkout. When app.stripe.enabled=false (the local-dev default, since no
 * real Stripe keys are configured), purchases fall back to the instant-simulated flow in
 * EnrollmentService so the app still works end-to-end without a Stripe account.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    private final PaymentRepository paymentRepository;

    @Value("${app.stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${app.stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${app.stripe.enabled}")
    private boolean stripeEnabled;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @PostConstruct
    public void init() {
        if (stripeEnabled) {
            Stripe.apiKey = stripeSecretKey;
        }
    }

    public boolean isEnabled() {
        return stripeEnabled;
    }

    /**
     * Creates a Stripe Checkout session for the given course purchase and a PENDING
     * Payment row to track it. The enrollment is NOT created here — it's created only
     * when the webhook confirms payment succeeded (see handleWebhookEvent).
     */
    @Transactional
    public String createCheckoutSession(User student, Course course) {
        if (!stripeEnabled) {
            throw new ApiException("Stripe is not configured on this server", 500);
        }

        try {
            long amountInCents = course.getPrice().multiply(BigDecimal.valueOf(100)).longValue();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendBaseUrl + "/checkout/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendBaseUrl + "/courses/" + course.getId() + "?checkout=cancelled")
                    .setCustomerEmail(student.getEmail())
                    .putMetadata("courseId", String.valueOf(course.getId()))
                    .putMetadata("studentId", String.valueOf(student.getId()))
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("usd")
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(course.getTitle())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);

            Payment payment = new Payment();
            payment.setStudent(student);
            payment.setCourse(course);
            payment.setAmount(course.getPrice());
            payment.setStatus("PENDING");
            payment.setStripeSessionId(session.getId());
            paymentRepository.save(payment);

            return session.getUrl();
        } catch (Exception e) {
            log.error("Stripe session creation failed: {}", e.getMessage());
            throw new ApiException("Could not start checkout: " + e.getMessage(), 500);
        }
    }

    /**
     * Verifies the incoming webhook signature and returns the parsed Event, or throws
     * if the signature doesn't match (protects against forged webhook calls).
     */
    public Event parseWebhookEvent(String payload, String signatureHeader) {
        try {
            return Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new ApiException("Invalid Stripe webhook signature", 400);
        }
    }
}
