package com.elearning.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

public class CourseDtos {

    @Data
    public static class CourseCreateRequest {
        @NotBlank
        private String title;

        private String description;

        @NotNull @DecimalMin(value = "0.0", message = "Price cannot be negative")
        private BigDecimal price;

        private String category;
        private String level;
        private String thumbnailUrl;
        private List<LectureRequest> lectures;
    }

    @Data
    public static class LectureRequest {
        @NotBlank
        private String title;
        private String videoUrl;
        private String content;
        private Integer durationMinutes;
        private Integer orderIndex;
        private boolean preview;
    }

    @Data
    public static class ReviewRequest {
        @NotNull
        private Integer rating;
        private String comment;
    }
}
