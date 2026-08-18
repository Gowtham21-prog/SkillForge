import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { instructorApi } from '../services/courseService';

export default function InstructorEarnings() {
  const [earnings, setEarnings] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    instructorApi.earnings()
      .then((res) => setEarnings(res.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="container page">Loading earnings…</div>;
  if (!earnings) return <div className="container page"><p>Could not load earnings.</p></div>;

  return (
    <div className="container page">
      <div className="section-head">
        <div>
          <span className="eyebrow">Instructor studio</span>
          <h2>Earnings</h2>
        </div>
        <Link to="/instructor" className="btn btn-dark btn-sm">← Back to courses</Link>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 16, marginBottom: 32 }}>
        <MetricCard label="Gross revenue" value={`$${Number(earnings.totalRevenue).toFixed(2)}`} />
        <MetricCard label="Platform fee (20%)" value={`-$${Number(earnings.platformFee).toFixed(2)}`} />
        <MetricCard label="Net earnings" value={`$${Number(earnings.netEarnings).toFixed(2)}`} accent />
        <MetricCard label="Total sales" value={earnings.totalSales} />
      </div>

      <h3 style={{ marginBottom: 16 }}>Revenue by course</h3>

      {earnings.byCourse.length === 0 ? (
        <div className="empty-state">
          <h3>No sales yet</h3>
          <p>Once students start buying your courses, you'll see a breakdown here.</p>
        </div>
      ) : (
        <div className="manage-list">
          {earnings.byCourse.map((c) => (
            <div className="manage-row" key={c.courseId}>
              <div className="manage-row-info">
                <Link to={`/courses/${c.courseId}`} className="manage-row-title">{c.courseTitle}</Link>
                <div className="manage-row-meta">{c.sales} sale{c.sales !== 1 ? 's' : ''}</div>
              </div>
              <div style={{ fontFamily: 'var(--font-mono)', fontWeight: 600, color: 'var(--color-board)' }}>
                ${Number(c.revenue).toFixed(2)}
              </div>
            </div>
          ))}
        </div>
      )}

      <p style={{ marginTop: 24, fontSize: 13, color: 'var(--color-ink-soft)' }}>
        Note: payouts are not automated in this version — this dashboard shows earnings for
        reference. Actual payout transfer would need to be connected to Stripe Connect or a
        similar payout provider.
      </p>
    </div>
  );
}

function MetricCard({ label, value, accent }) {
  return (
    <div className="form-card" style={{ maxWidth: 'none', padding: 20, textAlign: 'center' }}>
      <div style={{
        fontFamily: 'var(--font-mono)',
        fontSize: 24,
        fontWeight: 600,
        color: accent ? 'var(--color-success)' : 'var(--color-board)',
      }}>
        {value}
      </div>
      <div style={{ fontSize: 12.5, color: 'var(--color-ink-soft)', marginTop: 6, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
        {label}
      </div>
    </div>
  );
}
