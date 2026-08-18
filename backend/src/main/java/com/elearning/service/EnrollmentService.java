package com.elearning.service;

import com.elearning.entity.Course;
import com.elearning.entity.Enrollment;
import com.elearning.entity.Payment;
import com.elearning.entity.User;
import com.elearning.exception.ApiException;
import com.elearning.repository.EnrollmentRepository;
import com.elearning.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final CourseService courseService;
    private final StripeService stripeService;
    private final MailService mailService;

    /**
     * Entry point for the "buy" button. Free courses enroll immediately. Paid courses either
     * redirect to a real Stripe Checkout session (if Stripe is configured) or fall back to
     * the instant-simulated purchase so the app still works without Stripe keys set up.
     *
     * Returns a map describing what happened so the controller can respond appropriately:
     * {type: "ENROLLED", ...} or {type: "REDIRECT", url: "..."}
     */
    @Transactional
    public PurchaseResult purchaseCourse(String studentEmail, Long courseId) {
        User student = courseService.getUserByEmail(studentEmail);
        Course course = courseService.getCourseEntity(courseId);

        if (course.getInstructor().getId().equals(student.getId())) {
            throw new ApiException("You cannot enroll in your own course", 400);
        }
        if (enrollmentRepository.existsByStudentAndCourse(student, course)) {
            throw new ApiException("You are already enrolled in this course", 400);
        }

        boolean isFree = course.getPrice().compareTo(BigDecimal.ZERO) <= 0;

        if (isFree) {
            Enrollment enrollment = enrollFreeCourse(student, course);
            return PurchaseResult.enrolled(enrollment);
        }

        if (stripeService.isEnabled()) {
            String checkoutUrl = stripeService.createCheckoutSession(student, course);
            return PurchaseResult.redirect(checkoutUrl);
        }

        // Fallback: no Stripe configured, simulate a successful paid purchase so the demo
        // still works end-to-end. Replace this branch entirely once Stripe is set up in prod.
        Enrollment enrollment = simulatePaidEnrollment(student, course);
        return PurchaseResult.enrolled(enrollment);
    }

    @Transactional
    public Enrollment enrollFreeCourse(User student, Course course) {
        Payment payment = new Payment();
        payment.setStudent(student);
        payment.setCourse(course);
        payment.setAmount(BigDecimal.ZERO);
        payment.setStatus("SUCCESS");
        payment.setTransactionRef("FREE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        paymentRepository.save(payment);

        return createEnrollment(student, course);
    }

    @Transactional
    public Enrollment simulatePaidEnrollment(User student, Course course) {
        Payment payment = new Payment();
        payment.setStudent(student);
        payment.setCourse(course);
        payment.setAmount(course.getPrice());
        payment.setStatus("SUCCESS");
        payment.setTransactionRef("SIM-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        paymentRepository.save(payment);

        Enrollment enrollment = createEnrollment(student, course);
        mailService.sendPurchaseReceipt(student.getEmail(), student.getName(), course.getTitle(), course.getPrice().toString());
        return enrollment;
    }

    /**
     * Called from the Stripe webhook handler once a checkout.session.completed event is
     * verified. Marks the pending Payment as SUCCESS and creates the enrollment.
     */
    @Transactional
    public void confirmStripePayment(String stripeSessionId, String paymentIntentId) {
        Payment payment = paymentRepository.findByStripeSessionId(stripeSessionId)
                .orElseThrow(() -> new ApiException("No payment found for this Stripe session", 404));

        if ("SUCCESS".equals(payment.getStatus())) {
            log.info("Stripe session {} already confirmed, ignoring duplicate webhook", stripeSessionId);
            return;
        }

        payment.setStatus("SUCCESS");
        payment.setStripePaymentIntentId(paymentIntentId);
        payment.setTransactionRef(paymentIntentId);
        paymentRepository.save(payment);

        User student = payment.getStudent();
        Course course = payment.getCourse();

        if (!enrollmentRepository.existsByStudentAndCourse(student, course)) {
            createEnrollment(student, course);
        }

        mailService.sendPurchaseReceipt(student.getEmail(), student.getName(), course.getTitle(), payment.getAmount().toString());
    }

    private Enrollment createEnrollment(User student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setProgressPercent(0);
        return enrollmentRepository.save(enrollment);
    }

    public List<Enrollment> getMyEnrollments(String studentEmail) {
        User student = courseService.getUserByEmail(studentEmail);
        return enrollmentRepository.findByStudent(student);
    }

    public boolean isEnrolled(String studentEmail, Long courseId) {
        User student = courseService.getUserByEmail(studentEmail);
        Course course = courseService.getCourseEntity(courseId);
        return enrollmentRepository.existsByStudentAndCourse(student, course);
    }

    @Transactional
    public Enrollment updateProgress(String studentEmail, Long courseId, int progressPercent) {
        User student = courseService.getUserByEmail(studentEmail);
        Course course = courseService.getCourseEntity(courseId);

        Enrollment enrollment = enrollmentRepository.findByStudentAndCourse(student, course)
                .orElseThrow(() -> new ApiException("You are not enrolled in this course", 403));

        enrollment.setProgressPercent(Math.max(0, Math.min(100, progressPercent)));
        return enrollmentRepository.save(enrollment);
    }

    public long getEnrollmentCount(Course course) {
        return enrollmentRepository.countByCourse(course);
    }

    /**
     * Discriminated result of a purchase attempt: either the user is enrolled immediately
     * (free course or simulated payment), or they need to be redirected to Stripe Checkout.
     */
    public static class PurchaseResult {
        private final String type; // "ENROLLED" or "REDIRECT"
        private final Enrollment enrollment;
        private final String redirectUrl;

        private PurchaseResult(String type, Enrollment enrollment, String redirectUrl) {
            this.type = type;
            this.enrollment = enrollment;
            this.redirectUrl = redirectUrl;
        }

        public static PurchaseResult enrolled(Enrollment enrollment) {
            return new PurchaseResult("ENROLLED", enrollment, null);
        }

        public static PurchaseResult redirect(String url) {
            return new PurchaseResult("REDIRECT", null, url);
        }

        public String getType() { return type; }
        public Enrollment getEnrollment() { return enrollment; }
        public String getRedirectUrl() { return redirectUrl; }
    }
}
