import React from 'react';
import { Link } from 'react-router-dom';

export default function CourseCard({ course }) {
  const price = Number(course.price);
  const isFree = price === 0;

  return (
    <Link to={`/courses/${course.id}`} className="course-card">
      <div className="course-thumb">
        {course.thumbnailUrl ? (
          <img src={course.thumbnailUrl.startsWith('http') ? course.thumbnailUrl : `http://localhost:8080${course.thumbnailUrl}`} alt={course.title} />
        ) : (
          <span>{course.title}</span>
        )}
        {course.category && <span className="course-cat-tag">{course.category}</span>}
      </div>
      <div className="course-body">
        <div className="course-title">{course.title}</div>
        <div className="course-instructor">by {course.instructorName || 'Instructor'}</div>
        {course.averageRating != null && (
          <div style={{ fontSize: 12.5, color: 'var(--color-ink-soft)' }}>
            <span className="stars">{'★'.repeat(Math.round(course.averageRating))}{'☆'.repeat(5 - Math.round(course.averageRating))}</span>
            {' '}{course.averageRating} ({course.reviewCount || 0})
          </div>
        )}
        <div className="course-meta-row">
          <span className={`course-price ${isFree ? 'free' : ''}`}>
            {isFree ? 'Free' : `$${price.toFixed(2)}`}
          </span>
          {course.level && <span className="course-level">{course.level}</span>}
        </div>
      </div>
    </Link>
  );
}
