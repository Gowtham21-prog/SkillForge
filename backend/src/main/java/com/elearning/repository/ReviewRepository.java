package com.elearning.repository;

import com.elearning.entity.Course;
import com.elearning.entity.Review;
import com.elearning.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByCourse(Course course);
    Optional<Review> findByStudentAndCourse(User student, Course course);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.course = :course")
    Double findAverageRatingForCourse(@Param("course") Course course);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.course = :course")
    long countForCourse(@Param("course") Course course);
}
