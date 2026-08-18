import React, { useEffect, useState } from 'react';
import { adminApi } from '../services/courseService';

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState('overview');
  const [error, setError] = useState('');

  const load = () => {
    setLoading(true);
    Promise.all([adminApi.stats(), adminApi.users()])
      .then(([statsRes, usersRes]) => {
        setStats(statsRes.data);
        setUsers(usersRes.data);
      })
      .catch(() => setError('Could not load admin data. Are you logged in as an admin?'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const toggleEnabled = async (user) => {
    try {
      await adminApi.updateUser(user.id, { accountEnabled: !user.accountEnabled });
      setUsers((prev) => prev.map((u) => (u.id === user.id ? { ...u, accountEnabled: !u.accountEnabled } : u)));
    } catch (err) {
      alert(err.response?.data?.message || 'Could not update user');
    }
  };

  const changeRole = async (user, newRole) => {
    try {
      await adminApi.updateUser(user.id, { role: newRole });
      setUsers((prev) => prev.map((u) => (u.id === user.id ? { ...u, role: newRole } : u)));
    } catch (err) {
      alert(err.response?.data?.message || 'Could not update user');
    }
  };

  if (loading) return <div className="container page">Loading admin dashboard…</div>;
  if (error) return <div className="container page"><div className="form-error">{error}</div></div>;

  return (
    <div className="container page">
      <div className="section-head">
        <div>
          <span className="eyebrow">Admin</span>
          <h2>Platform overview</h2>
        </div>
      </div>

      <div className="dash-tabs">
        <button className={`dash-tab ${tab === 'overview' ? 'active' : ''}`} onClick={() => setTab('overview')}>Overview</button>
        <button className={`dash-tab ${tab === 'users' ? 'active' : ''}`} onClick={() => setTab('users')}>Users ({users.length})</button>
      </div>

      {tab === 'overview' && stats && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 16 }}>
          <StatCard label="Total users" value={stats.totalUsers} />
          <StatCard label="Students" value={stats.totalStudents} />
          <StatCard label="Instructors" value={stats.totalInstructors} />
          <StatCard label="Total courses" value={stats.totalCourses} />
          <StatCard label="Published courses" value={stats.publishedCourses} />
          <StatCard label="Total enrollments" value={stats.totalEnrollments} />
          <StatCard label="Platform revenue" value={`$${Number(stats.totalRevenue).toFixed(2)}`} accent />
          <StatCard label="Total reviews" value={stats.totalReviews} />
          <StatCard label="Avg. rating" value={stats.averageRating > 0 ? `${stats.averageRating} ★` : '—'} />
        </div>
      )}

      {tab === 'users' && (
        <div className="manage-list">
          {users.map((u) => (
            <div className="manage-row" key={u.id}>
              <div className="manage-row-info">
                <div className="manage-row-title">
                  {u.name}{' '}
                  {!u.accountEnabled && <span className="badge" style={{ background: '#fbe9e5', color: '#9c3d24' }}>Disabled</span>}
                  {!u.emailVerified && <span className="badge" style={{ background: '#f0dcc9', color: '#9c4a2c', marginLeft: 6 }}>Unverified</span>}
                </div>
                <div className="manage-row-meta">
                  {u.email} · {u.courseCount} course{u.courseCount !== 1 ? 's' : ''} · {u.enrollmentCount} enrollment{u.enrollmentCount !== 1 ? 's' : ''}
                </div>
              </div>
              <select
                value={u.role}
                onChange={(e) => changeRole(u, e.target.value)}
                style={{ padding: '6px 10px', border: '1.5px solid var(--color-line)', borderRadius: 6, fontSize: 13 }}
              >
                <option value="STUDENT">Student</option>
                <option value="INSTRUCTOR">Instructor</option>
                <option value="ADMIN">Admin</option>
              </select>
              <button
                className={`btn btn-sm ${u.accountEnabled ? 'btn-danger' : 'btn-dark'}`}
                onClick={() => toggleEnabled(u)}
              >
                {u.accountEnabled ? 'Disable' : 'Enable'}
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function StatCard({ label, value, accent }) {
  return (
    <div className="form-card" style={{ maxWidth: 'none', padding: 20, textAlign: 'center' }}>
      <div style={{
        fontFamily: 'var(--font-mono)',
        fontSize: 26,
        fontWeight: 600,
        color: accent ? 'var(--color-accent)' : 'var(--color-board)',
      }}>
        {value}
      </div>
      <div style={{ fontSize: 12.5, color: 'var(--color-ink-soft)', marginTop: 6, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
        {label}
      </div>
    </div>
  );
}
