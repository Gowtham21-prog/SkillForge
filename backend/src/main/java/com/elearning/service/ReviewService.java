package com.elearning.service;

import com.elearning.dto.CourseDtos.ReviewRequest;
import com.elearning.entity.Course;
import com.elearning.entity.Review;
import com.elearning.entity.User;
import com.elearning.exception.ApiException;
import com.elearning.repository.EnrollmentRepository;
import com.elearning.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;

    @Transactional
    public Review addReview(String studentEmail, Long courseId, ReviewRequest request) {
        User student = courseService.getUserByEmail(studentEmail);
        Course course = courseService.getCourseEntity(courseId);

        if (!enrollmentRepository.existsByStudentAndCourse(student, course)) {
            throw new ApiException("You must be enrolled to review this course", 403);
        }

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new ApiException("Rating must be between 1 and 5", 400);
        }

        Review review = reviewRepository.findByStudentAndCourse(student, course).orElse(new Review());
        review.setStudent(student);
        review.setCourse(course);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        return reviewRepository.save(review);
    }

    public List<Review> getCourseReviews(Long courseId) {
        Course course = courseService.getCourseEntity(courseId);
        return reviewRepository.findByCourse(course).stream().map(r -> {
            r.setStudentName(r.getStudent().getName());
            return r;
        }).collect(Collectors.toList());
    }
}
