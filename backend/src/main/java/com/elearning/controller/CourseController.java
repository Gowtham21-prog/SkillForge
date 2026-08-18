package com.elearning.controller;

import com.elearning.dto.CourseDtos.CourseCreateRequest;
import com.elearning.entity.Course;
import com.elearning.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * Main catalog browse/search endpoint with filtering, sorting, and pagination.
     * All query params are optional. Response is a Spring Page (content, totalElements,
     * totalPages, number, size, etc.) so the frontend can render pagination controls.
     */
    @GetMapping
    public ResponseEntity<Page<Course>> getAllCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "newest") String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(courseService.searchCoursesPaged(
                keyword, category, level, minPrice, maxPrice, sortBy, sortDir, page, size
        ));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(courseService.getCategories());
    }

    /** Legacy simple search — kept for backward compatibility; prefer GET /courses?keyword=... */
    @GetMapping("/search")
    public ResponseEntity<List<Course>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(courseService.searchCourses(keyword));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Course>> byCategory(@PathVariable String category) {
        return ResponseEntity.ok(courseService.getCoursesByCategory(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourse(@PathVariable Long id,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(courseService.getCourseByIdForViewer(id, viewerEmail));
    }

    @GetMapping("/instructor/mine")
    public ResponseEntity<List<Course>> myCourses(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(courseService.getCoursesByInstructor(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<Course> createCourse(@AuthenticationPrincipal UserDetails userDetails,
                                                @Valid @RequestBody CourseCreateRequest request) {
        return ResponseEntity.ok(courseService.createCourse(userDetails.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@AuthenticationPrincipal UserDetails userDetails,
                                                @PathVariable Long id,
                                                @Valid @RequestBody CourseCreateRequest request) {
        return ResponseEntity.ok(courseService.updateCourse(userDetails.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@AuthenticationPrincipal UserDetails userDetails,
                                              @PathVariable Long id) {
        courseService.deleteCourse(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
