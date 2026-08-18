package com.elearning.specification;

import com.elearning.entity.Course;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Composable filter predicates for the course catalog search. Each static method returns
 * a Specification that can be combined with .and(); any null/blank argument is treated as
 * "no filter" rather than excluding everything.
 */
public class CourseSpecifications {

    private CourseSpecifications() {}

    public static Specification<Course> filter(
            String keyword,
            String category,
            String level,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean publishedOnly
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (Boolean.TRUE.equals(publishedOnly)) {
                predicates.add(cb.isTrue(root.get("published")));
            }

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like)
                ));
            }

            if (category != null && !category.isBlank() && !category.equalsIgnoreCase("All")) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (level != null && !level.isBlank()) {
                predicates.add(cb.equal(root.get("level"), level));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
