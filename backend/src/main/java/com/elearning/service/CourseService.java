package com.elearning.service;

import com.elearning.dto.CourseDtos.CourseCreateRequest;
import com.elearning.dto.CourseDtos.LectureRequest;
import com.elearning.entity.Course;
import com.elearning.entity.Lecture;
import com.elearning.entity.User;
import com.elearning.exception.ApiException;
import com.elearning.repository.CourseRepository;
import com.elearning.repository.EnrollmentRepository;
import com.elearning.repository.ReviewRepository;
import com.elearning.repository.UserRepository;
import com.elearning.specification.CourseSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ReviewRepository reviewRepository;

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", 404));
    }

    @Transactional
    public Course createCourse(String instructorEmail, CourseCreateRequest request) {
        User instructor = getUserByEmail(instructorEmail);

        if (instructor.getRole() != User.Role.INSTRUCTOR && instructor.getRole() != User.Role.ADMIN) {
            throw new ApiException("Only instructors can create courses", 403);
        }

        Course course = new Course();
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setPrice(request.getPrice());
        course.setCategory(request.getCategory());
        course.setLevel(request.getLevel());
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setInstructor(instructor);
        course.setPublished(true);

        if (request.getLectures() != null) {
            int idx = 0;
            for (LectureRequest lr : request.getLectures()) {
                Lecture lecture = new Lecture();
                lecture.setTitle(lr.getTitle());
                lecture.setVideoUrl(lr.getVideoUrl());
                lecture.setContent(lr.getContent());
                lecture.setDurationMinutes(lr.getDurationMinutes());
                lecture.setOrderIndex(lr.getOrderIndex() != null ? lr.getOrderIndex() : idx++);
                lecture.setPreview(lr.isPreview());
                lecture.setCourse(course);
                course.getLectures().add(lecture);
            }
        }

        return courseRepository.save(course);
    }

    @Transactional
    public Course updateCourse(String instructorEmail, Long courseId, CourseCreateRequest request) {
        Course course = getCourseEntity(courseId);
        User instructor = getUserByEmail(instructorEmail);

        if (!course.getInstructor().getId().equals(instructor.getId()) && instructor.getRole() != User.Role.ADMIN) {
            throw new ApiException("You are not allowed to edit this course", 403);
        }

        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setPrice(request.getPrice());
        course.setCategory(request.getCategory());
        course.setLevel(request.getLevel());
        if (request.getThumbnailUrl() != null) {
            course.setThumbnailUrl(request.getThumbnailUrl());
        }

        if (request.getLectures() != null) {
            course.getLectures().clear();
            int idx = 0;
            for (LectureRequest lr : request.getLectures()) {
                Lecture lecture = new Lecture();
                lecture.setTitle(lr.getTitle());
                lecture.setVideoUrl(lr.getVideoUrl());
                lecture.setContent(lr.getContent());
                lecture.setDurationMinutes(lr.getDurationMinutes());
                lecture.setOrderIndex(lr.getOrderIndex() != null ? lr.getOrderIndex() : idx++);
                lecture.setPreview(lr.isPreview());
                lecture.setCourse(course);
                course.getLectures().add(lecture);
            }
        }

        return courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(String instructorEmail, Long courseId) {
        Course course = getCourseEntity(courseId);
        User instructor = getUserByEmail(instructorEmail);

        if (!course.getInstructor().getId().equals(instructor.getId()) && instructor.getRole() != User.Role.ADMIN) {
            throw new ApiException("You are not allowed to delete this course", 403);
        }

        courseRepository.delete(course);
    }

    public List<Course> getAllPublishedCourses() {
        return enrichAll(courseRepository.findByPublishedTrue());
    }

    /**
     * Filtered, sorted, paginated course search — backs the main catalog browse page.
     * All filter params are optional; pass null/blank to skip a filter.
     */
    public Page<Course> searchCoursesPaged(
            String keyword, String category, String level,
            BigDecimal minPrice, BigDecimal maxPrice,
            String sortBy, String sortDir,
            int page, int size
    ) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortField = switch (sortBy == null ? "" : sortBy) {
            case "price" -> "price";
            case "title" -> "title";
            case "oldest" -> "createdAt";
            default -> "createdAt"; // "newest" / default
        };
        // "newest" should default to descending even if caller didn't specify a direction
        if ((sortBy == null || sortBy.equals("newest")) && sortDir == null) {
            direction = Sort.Direction.DESC;
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by(direction, sortField));

        var spec = CourseSpecifications.filter(keyword, category, level, minPrice, maxPrice, true);
        Page<Course> results = courseRepository.findAll(spec, pageable);
        return results.map(this::enrich);
    }

    public List<String> getCategories() {
        return courseRepository.findDistinctCategories();
    }

    public List<Course> searchCourses(String keyword) {
        return enrichAll(courseRepository.searchByKeyword(keyword));
    }

    public List<Course> getCoursesByCategory(String category) {
        return enrichAll(courseRepository.findByCategory(category));
    }

    public Course getCourseById(Long id) {
        return enrich(getCourseEntity(id));
    }

    /**
     * Same as getCourseById, but strips video URLs and content from lectures that aren't
     * marked as free previews, UNLESS the viewer is enrolled or owns the course. This is
     * what the public course-detail endpoint should use — otherwise anyone can read full
     * lecture content/video links straight from the API without ever paying.
     */
    public Course getCourseByIdForViewer(Long id, String viewerEmail) {
        Course course = enrich(getCourseEntity(id));

        boolean hasFullAccess = false;
        if (viewerEmail != null) {
            User viewer = userRepository.findByEmail(viewerEmail).orElse(null);
            if (viewer != null) {
                boolean isOwner = course.getInstructor().getId().equals(viewer.getId());
                boolean isAdmin = viewer.getRole() == User.Role.ADMIN;
                boolean isEnrolled = enrollmentRepository.existsByStudentAndCourse(viewer, course);
                hasFullAccess = isOwner || isAdmin || isEnrolled;
            }
        }

        if (!hasFullAccess) {
            course.getLectures().forEach(lecture -> {
                if (!lecture.isPreview()) {
                    lecture.setVideoUrl(null);
                    lecture.setContent(null);
                }
            });
        }

        return course;
    }

    public Course getCourseEntity(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ApiException("Course not found", 404));
    }

    public List<Course> getCoursesByInstructor(String instructorEmail) {
        User instructor = getUserByEmail(instructorEmail);
        return enrichAll(courseRepository.findByInstructor(instructor));
    }

    private Course enrich(Course course) {
        course.setInstructorName(course.getInstructor().getName());
        course.setInstructorId(course.getInstructor().getId());

        Double avgRating = reviewRepository.findAverageRatingForCourse(course);
        course.setAverageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : null);
        course.setReviewCount(reviewRepository.countForCourse(course));
        course.setEnrollmentCount(enrollmentRepository.countByCourse(course));

        return course;
    }

    private List<Course> enrichAll(List<Course> courses) {
        return courses.stream().map(this::enrich).collect(Collectors.toList());
    }
}
