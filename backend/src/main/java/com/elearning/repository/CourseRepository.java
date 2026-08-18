package com.elearning.repository;

import com.elearning.entity.Course;
import com.elearning.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    List<Course> findByPublishedTrue();
    List<Course> findByInstructor(User instructor);

    @Query("SELECT c FROM Course c WHERE c.published = true AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Course> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT c FROM Course c WHERE c.published = true AND c.category = :category")
    List<Course> findByCategory(@Param("category") String category);

    @Query("SELECT DISTINCT c.category FROM Course c WHERE c.published = true AND c.category IS NOT NULL ORDER BY c.category")
    List<String> findDistinctCategories();
}
