import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { courseApi } from '../services/courseService';

export default function InstructorDashboard() {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    courseApi.myCourses()
      .then((res) => setCourses(res.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this course? This cannot be undone.')) return;
    try {
      await courseApi.remove(id);
      setCourses((prev) => prev.filter((c) => c.id !== id));
    } catch (err) {
      alert(err.response?.data?.message || 'Could not delete course');
    }
  };

  return (
    <div className="container page">
      <div className="section-head">
        <div>
          <span className="eyebrow">Instructor studio</span>
          <h2>Your courses</h2>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <Link to="/instructor/earnings" className="btn btn-dark">View earnings</Link>
          <Link to="/instructor/new" className="btn btn-primary">+ New course</Link>
        </div>
      </div>

      {loading && <p>Loading…</p>}

      {!loading && courses.length === 0 && (
        <div className="empty-state">
          <h3>Nothing listed yet</h3>
          <p>Create your first course and start earning from what you know.</p>
          <Link to="/instructor/new" className="btn btn-primary" style={{ marginTop: 16 }}>Create a course</Link>
        </div>
      )}

      <div className="manage-list">
        {courses.map((c) => (
          <div className="manage-row" key={c.id}>
            <div className="manage-row-thumb" />
            <div className="manage-row-info">
              <div className="manage-row-title">{c.title}</div>
              <div className="manage-row-meta">
                ${Number(c.price).toFixed(2)} · {c.lectures?.length || 0} lectures · {c.category || 'Uncategorized'}
              </div>
            </div>
            <Link to={`/courses/${c.id}`} className="btn btn-sm btn-outline" style={{ color: 'var(--color-ink)', borderColor: 'var(--color-line)' }}>View</Link>
            <Link to={`/instructor/edit/${c.id}`} className="btn btn-sm btn-dark">Edit</Link>
            <button className="btn btn-sm btn-danger" onClick={() => handleDelete(c.id)}>Delete</button>
          </div>
        ))}
      </div>
    </div>
  );
}
