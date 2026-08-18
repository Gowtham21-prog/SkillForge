package com.elearning.controller;

import com.elearning.dto.AdminDtos.*;
import com.elearning.entity.Course;
import com.elearning.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * All endpoints here require ROLE_ADMIN — enforced both at the method level via @PreAuthorize
 * and again in SecurityConfig's URL matcher, so a misconfiguration in one place doesn't
 * silently expose admin functionality.
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<PlatformStats> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserSummary>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PatchMapping("/users/{userId}")
    public ResponseEntity<UserSummary> updateUserStatus(@AuthenticationPrincipal UserDetails admin,
                                                          @PathVariable Long userId,
                                                          @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(adminService.updateUserStatus(userId, request, admin.getUsername()));
    }

    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId) {
        adminService.deleteCourseAsAdmin(courseId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/courses/{courseId}/publish")
    public ResponseEntity<Course> togglePublished(@PathVariable Long courseId, @RequestBody Map<String, Boolean> body) {
        boolean published = body.getOrDefault("published", true);
        return ResponseEntity.ok(adminService.togglePublished(courseId, published));
    }
}
