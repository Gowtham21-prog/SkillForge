import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { courseApi } from '../services/courseService';
import CourseCard from '../components/CourseCard';

export default function Home() {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    courseApi.getAll({ size: 6, sortBy: 'newest' })
      .then((res) => setCourses(res.data.content))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <section className="hero">
        <div className="container hero-inner">
          <span className="eyebrow">The marketplace for real skills</span>
          <h1 style={{ marginTop: 12 }}>
            Teach what you know.<br /><em>Learn what you don't.</em>
          </h1>
          <p>
            LearnHub connects instructors who've done the work with students who want to skip
            the guesswork. List a course in minutes, or find one taught by someone who's actually
            shipped it.
          </p>
          <div className="hero-actions">
            <Link to="/courses" className="btn btn-primary">Browse courses</Link>
            <Link to="/register" className="btn btn-outline">Start teaching</Link>
          </div>

          <div className="hero-stats">
            <div>
              <div className="hero-stat-num">{courses.length > 0 ? '500+' : '—'}</div>
              <div className="hero-stat-label">Courses listed</div>
            </div>
            <div>
              <div className="hero-stat-num">12k+</div>
              <div className="hero-stat-label">Students learning</div>
            </div>
            <div>
              <div className="hero-stat-num">4.7 / 5</div>
              <div className="hero-stat-label">Avg. course rating</div>
            </div>
          </div>
        </div>
      </section>

      <div className="container page">
        <div className="section-head">
          <div>
            <span className="eyebrow">Fresh on the shelf</span>
            <h2>Newest courses</h2>
          </div>
          <Link to="/courses" className="btn btn-dark btn-sm">View all →</Link>
        </div>

        {loading && <p>Loading courses…</p>}

        {!loading && courses.length === 0 && (
          <div className="empty-state">
            <h3>No courses yet</h3>
            <p>Be the first to list one — it takes less time than writing this description.</p>
            <Link to="/register" className="btn btn-primary" style={{ marginTop: 16 }}>
              Create a course
            </Link>
          </div>
        )}

        <div className="course-grid">
          {courses.map((c) => (
            <CourseCard key={c.id} course={c} />
          ))}
        </div>
      </div>
    </div>
  );
}
