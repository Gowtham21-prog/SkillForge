package com.elearning.controller;

import com.elearning.service.EnrollmentService;
import com.elearning.service.StripeService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Receives Stripe webhook events. This endpoint is publicly reachable (Stripe can't send
 * a JWT), so it must NOT be behind the normal auth filter — it's exempted in SecurityConfig
 * and instead trusts Stripe's own signature verification (see StripeService.parseWebhookEvent).
 *
 * Configure this URL (https://yourdomain.com/api/webhooks/stripe) in the Stripe dashboard,
 * subscribed to the "checkout.session.completed" event.
 */
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final StripeService stripeService;
    private final EnrollmentService enrollmentService;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request) throws IOException {
        String payload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String signatureHeader = request.getHeader("Stripe-Signature");

        Event event = stripeService.parseWebhookEvent(payload, signatureHeader);

        if ("checkout.session.completed".equals(event.getType())) {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            if (deserializer.getObject().isPresent()) {
                Session session = (Session) deserializer.getObject().get();
                String paymentIntentId = session.getPaymentIntent();
                try {
                    enrollmentService.confirmStripePayment(session.getId(), paymentIntentId);
                } catch (Exception e) {
                    log.error("Failed to confirm Stripe payment for session {}: {}", session.getId(), e.getMessage());
                    // Return 200 anyway so Stripe doesn't endlessly retry a permanently-broken event;
                    // the failure is logged for manual reconciliation.
                }
            }
        } else {
            log.debug("Ignoring unhandled Stripe event type: {}", event.getType());
        }

        return ResponseEntity.ok("received");
    }
}
