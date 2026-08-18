import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { enrollmentApi } from '../services/courseService';

export default function Dashboard() {
  const [enrollments, setEnrollments] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    enrollmentApi.myEnrollments()
      .then((res) => setEnrollments(res.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const updateProgress = async (courseId, current) => {
    const next = Math.min(100, current + 20);
    try {
      await enrollmentApi.updateProgress(courseId, next);
      setEnrollments((prev) =>
        prev.map((e) => (e.course.id === courseId ? { ...e, progressPercent: next } : e))
      );
    } catch {
      // ignore
    }
  };

  return (
    <div className="container page">
      <div className="section-head">
        <div>
          <span className="eyebrow">Your progress</span>
          <h2>My learning</h2>
        </div>
      </div>

      {loading && <p>Loading…</p>}

      {!loading && enrollments.length === 0 && (
        <div className="empty-state">
          <h3>No courses yet</h3>
          <p>You haven't purchased any courses. Find something worth learning.</p>
          <Link to="/courses" className="btn btn-primary" style={{ marginTop: 16 }}>Browse courses</Link>
        </div>
      )}

      <div className="manage-list">
        {enrollments.map((e) => (
          <div className="manage-row" key={e.id}>
            <div className="manage-row-thumb" />
            <div className="manage-row-info">
              <Link to={`/courses/${e.course.id}`} className="manage-row-title">{e.course.title}</Link>
              <div className="manage-row-meta">{e.progressPercent}% complete</div>
              <div className="progress-bar-track" style={{ marginTop: 8, maxWidth: 320 }}>
                <div className="progress-bar-fill" style={{ width: `${e.progressPercent}%` }} />
              </div>
            </div>
            <button
              className="btn btn-sm btn-dark"
              onClick={() => updateProgress(e.course.id, e.progressPercent)}
              disabled={e.progressPercent >= 100}
            >
              {e.progressPercent >= 100 ? 'Completed ✓' : 'Mark progress +20%'}
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
