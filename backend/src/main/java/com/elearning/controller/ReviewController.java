package com.elearning.controller;

import com.elearning.dto.CourseDtos.ReviewRequest;
import com.elearning.entity.Review;
import com.elearning.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses/{courseId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<Review>> getReviews(@PathVariable Long courseId) {
        return ResponseEntity.ok(reviewService.getCourseReviews(courseId));
    }

    @PostMapping
    public ResponseEntity<Review> addReview(@AuthenticationPrincipal UserDetails userDetails,
                                             @PathVariable Long courseId,
                                             @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.addReview(userDetails.getUsername(), courseId, request));
    }
}
