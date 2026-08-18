package com.elearning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class AdminDtos {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlatformStats {
        private long totalUsers;
        private long totalStudents;
        private long totalInstructors;
        private long totalCourses;
        private long publishedCourses;
        private long totalEnrollments;
        private BigDecimal totalRevenue;
        private long totalReviews;
        private double averageRating;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserSummary {
        private Long id;
        private String name;
        private String email;
        private String role;
        private boolean emailVerified;
        private boolean accountEnabled;
        private long courseCount;
        private long enrollmentCount;
        private String createdAt;
    }

    @Data
    public static class UpdateUserStatusRequest {
        private Boolean accountEnabled;
        private String role; // STUDENT, INSTRUCTOR, ADMIN
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RevenuePoint {
        private String label; // e.g. "2026-08" or course title
        private BigDecimal amount;
        private long count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InstructorEarnings {
        private BigDecimal totalRevenue;
        private BigDecimal platformFee;
        private BigDecimal netEarnings;
        private long totalSales;
        private List<CourseEarning> byCourse;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CourseEarning {
        private Long courseId;
        private String courseTitle;
        private BigDecimal revenue;
        private long sales;
    }
}
