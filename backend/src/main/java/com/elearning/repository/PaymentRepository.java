package com.elearning.repository;

import com.elearning.entity.Course;
import com.elearning.entity.Payment;
import com.elearning.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByStudent(User student);
    Optional<Payment> findByStripeSessionId(String stripeSessionId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal sumAllSuccessfulRevenue();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS' AND p.course.instructor = :instructor")
    BigDecimal sumRevenueForInstructor(@Param("instructor") User instructor);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS' AND p.course = :course")
    BigDecimal sumRevenueForCourse(@Param("course") Course course);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'SUCCESS' AND p.course = :course")
    long countSuccessfulForCourse(@Param("course") Course course);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'SUCCESS' AND p.course.instructor = :instructor")
    long countSuccessfulForInstructor(@Param("instructor") User instructor);

    @Query("SELECT p FROM Payment p WHERE p.status = 'SUCCESS' AND p.course.instructor = :instructor ORDER BY p.paidAt DESC")
    List<Payment> findSuccessfulForInstructor(@Param("instructor") User instructor);
}
