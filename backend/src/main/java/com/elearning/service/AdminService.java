package com.elearning.service;

import com.elearning.dto.AdminDtos.*;
import com.elearning.entity.Course;
import com.elearning.entity.Review;
import com.elearning.entity.User;
import com.elearning.exception.ApiException;
import com.elearning.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public PlatformStats getStats() {
        long totalUsers = userRepository.count();
        long totalStudents = userRepository.countByRole(User.Role.STUDENT);
        long totalInstructors = userRepository.countByRole(User.Role.INSTRUCTOR);
        long totalCourses = courseRepository.count();
        long publishedCourses = courseRepository.findByPublishedTrue().size();
        long totalEnrollments = enrollmentRepository.count();
        java.math.BigDecimal totalRevenue = paymentRepository.sumAllSuccessfulRevenue();

        List<Review> allReviews = reviewRepository.findAll();
        long totalReviews = allReviews.size();
        double avgRating = allReviews.isEmpty()
                ? 0.0
                : allReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);

        return new PlatformStats(
                totalUsers, totalStudents, totalInstructors,
                totalCourses, publishedCourses, totalEnrollments,
                totalRevenue, totalReviews, Math.round(avgRating * 10.0) / 10.0
        );
    }

    public List<UserSummary> getAllUsers() {
        return userRepository.findAllOrderByCreatedAtDesc().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserSummary updateUserStatus(Long userId, UpdateUserStatusRequest request, String actingAdminEmail) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", 404));

        User actingAdmin = userRepository.findByEmail(actingAdminEmail)
                .orElseThrow(() -> new ApiException("Admin not found", 404));

        if (target.getId().equals(actingAdmin.getId())) {
            throw new ApiException("You cannot modify your own admin account here", 400);
        }

        if (request.getAccountEnabled() != null) {
            target.setAccountEnabled(request.getAccountEnabled());
        }
        if (request.getRole() != null) {
            try {
                target.setRole(User.Role.valueOf(request.getRole()));
            } catch (IllegalArgumentException e) {
                throw new ApiException("Invalid role: " + request.getRole(), 400);
            }
        }

        userRepository.save(target);
        return toSummary(target);
    }

    @Transactional
    public void deleteCourseAsAdmin(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException("Course not found", 404));
        courseRepository.delete(course);
    }

    @Transactional
    public Course togglePublished(Long courseId, boolean published) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException("Course not found", 404));
        course.setPublished(published);
        return courseRepository.save(course);
    }

    private UserSummary toSummary(User user) {
        long courseCount = user.getRole() == User.Role.INSTRUCTOR || user.getRole() == User.Role.ADMIN
                ? courseRepository.findByInstructor(user).size()
                : 0;
        long enrollmentCount = enrollmentRepository.findByStudent(user).size();

        return new UserSummary(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.isEmailVerified(),
                user.isAccountEnabled(),
                courseCount,
                enrollmentCount,
                user.getCreatedAt() != null ? user.getCreatedAt().format(DATE_FMT) : null
        );
    }
}
