package com.elearning.controller;

import com.elearning.entity.Enrollment;
import com.elearning.service.EnrollmentService;
import com.elearning.service.EnrollmentService.PurchaseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * Kicks off a purchase. Response shape depends on outcome:
     * - Free/simulated: {"type": "ENROLLED", "enrollment": {...}}
     * - Real Stripe checkout: {"type": "REDIRECT", "url": "https://checkout.stripe.com/..."}
     * The frontend should check `type` and either show success or redirect the browser.
     */
    @PostMapping("/{courseId}/purchase")
    public ResponseEntity<Map<String, Object>> purchase(@AuthenticationPrincipal UserDetails userDetails,
                                                          @PathVariable Long courseId) {
        PurchaseResult result = enrollmentService.purchaseCourse(userDetails.getUsername(), courseId);

        if ("REDIRECT".equals(result.getType())) {
            return ResponseEntity.ok(Map.of("type", "REDIRECT", "url", result.getRedirectUrl()));
        }
        return ResponseEntity.ok(Map.of("type", "ENROLLED", "enrollment", result.getEnrollment()));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Enrollment>> myEnrollments(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(enrollmentService.getMyEnrollments(userDetails.getUsername()));
    }

    @GetMapping("/{courseId}/status")
    public ResponseEntity<Map<String, Boolean>> isEnrolled(@AuthenticationPrincipal UserDetails userDetails,
                                                             @PathVariable Long courseId) {
        boolean enrolled = enrollmentService.isEnrolled(userDetails.getUsername(), courseId);
        return ResponseEntity.ok(Map.of("enrolled", enrolled));
    }

    @PatchMapping("/{courseId}/progress")
    public ResponseEntity<Enrollment> updateProgress(@AuthenticationPrincipal UserDetails userDetails,
                                                       @PathVariable Long courseId,
                                                       @RequestBody Map<String, Integer> body) {
        int progress = body.getOrDefault("progressPercent", 0);
        return ResponseEntity.ok(enrollmentService.updateProgress(userDetails.getUsername(), courseId, progress));
    }
}
