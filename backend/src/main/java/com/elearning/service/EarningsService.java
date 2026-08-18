package com.elearning.service;

import com.elearning.dto.AdminDtos.CourseEarning;
import com.elearning.dto.AdminDtos.InstructorEarnings;
import com.elearning.entity.Course;
import com.elearning.entity.User;
import com.elearning.repository.CourseRepository;
import com.elearning.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EarningsService {

    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.20"); // 20% platform cut

    private final PaymentRepository paymentRepository;
    private final CourseRepository courseRepository;
    private final CourseService courseService;

    public InstructorEarnings getEarningsForInstructor(String instructorEmail) {
        User instructor = courseService.getUserByEmail(instructorEmail);

        BigDecimal totalRevenue = paymentRepository.sumRevenueForInstructor(instructor);
        long totalSales = paymentRepository.countSuccessfulForInstructor(instructor);

        BigDecimal platformFee = totalRevenue.multiply(PLATFORM_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netEarnings = totalRevenue.subtract(platformFee);

        List<Course> courses = courseRepository.findByInstructor(instructor);
        List<CourseEarning> byCourse = courses.stream()
                .map(course -> {
                    BigDecimal courseRevenue = paymentRepository.sumRevenueForCourse(course);
                    long courseSales = paymentRepository.countSuccessfulForCourse(course);
                    return new CourseEarning(course.getId(), course.getTitle(), courseRevenue, courseSales);
                })
                .filter(ce -> ce.getSales() > 0)
                .sorted((a, b) -> b.getRevenue().compareTo(a.getRevenue()))
                .collect(Collectors.toList());

        return new InstructorEarnings(totalRevenue, platformFee, netEarnings, totalSales, byCourse);
    }
}
