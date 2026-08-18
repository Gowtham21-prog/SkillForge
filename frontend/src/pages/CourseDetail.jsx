import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { courseApi, enrollmentApi, reviewApi } from '../services/courseService';
import { useAuth } from '../context/AuthContext';

export default function CourseDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [course, setCourse] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [enrolled, setEnrolled] = useState(false);
  const [loading, setLoading] = useState(true);
  const [purchasing, setPurchasing] = useState(false);
  const [error, setError] = useState('');
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([
      courseApi.getById(id),
      reviewApi.getForCourse(id),
    ])
      .then(([courseRes, reviewsRes]) => {
        setCourse(courseRes.data);
        setReviews(reviewsRes.data);
      })
      .catch(() => setError('Course not found'))
      .finally(() => setLoading(false));

    if (user) {
      enrollmentApi.status(id).then((res) => setEnrolled(res.data.enrolled)).catch(() => {});
    }
  }, [id, user]);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const handlePurchase = async () => {
    if (!user) {
      navigate('/login');
      return;
    }
    setPurchasing(true);
    setError('');
    try {
      const res = await enrollmentApi.purchase(id);
      if (res.data.type === 'REDIRECT') {
        // Real Stripe checkout — send the browser to Stripe's hosted payment page
        window.location.href = res.data.url;
        return;
      }
      // Free course or simulated purchase — already enrolled
      setEnrolled(true);
    } catch (err) {
      setError(err.response?.data?.message || 'Purchase failed');
    } finally {
      setPurchasing(false);
    }
  };

  const handleReviewSubmit = async (e) => {
    e.preventDefault();
    setSubmittingReview(true);
    try {
      await reviewApi.add(id, { rating: reviewRating, comment: reviewComment });
      setReviewComment('');
      const res = await reviewApi.getForCourse(id);
      setReviews(res.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not submit review');
    } finally {
      setSubmittingReview(false);
    }
  };

  if (loading) return <div className="container page">Loading…</div>;
  if (!course) return <div className="container page"><p>{error || 'Course not found'}</p></div>;

  const price = Number(course.price);
  const isFree = price === 0;
  const isOwner = user && course.instructorId === user.id;
  const avgRating = reviews.length
    ? (reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length).toFixed(1)
    : null;

  return (
    <div>
      <section className="detail-hero">
        <div className="container">
          {course.category && <span className="eyebrow">{course.category}</span>}
          <h1 style={{ color: 'var(--color-chalk)', fontSize: 34, marginTop: 10, maxWidth: 640 }}>
            {course.title}
          </h1>
          <p style={{ color: '#d8d2c0', maxWidth: 600, marginTop: 12 }}>{course.description}</p>
          <div style={{ display: 'flex', gap: 20, marginTop: 18, alignItems: 'center', flexWrap: 'wrap' }}>
            <span style={{ color: '#d8d2c0', fontSize: 14 }}>by <strong style={{ color: 'var(--color-chalk)' }}>{course.instructorName}</strong></span>
            {avgRating && (
              <span style={{ fontSize: 14, color: '#d8d2c0' }}>
                <span className="stars">{'★'.repeat(Math.round(avgRating))}{'☆'.repeat(5 - Math.round(avgRating))}</span>{' '}
                {avgRating} ({reviews.length} review{reviews.length !== 1 ? 's' : ''})
              </span>
            )}
            {course.level && <span className="badge badge-student" style={{ background: 'rgba(244,239,224,0.15)', color: 'var(--color-chalk)' }}>{course.level}</span>}
          </div>
        </div>
      </section>

      <div className="container page">
        <div className="detail-grid">
          <div>
            <h3 style={{ marginBottom: 16 }}>Course content</h3>
            <div className="lecture-list">
              {(!course.lectures || course.lectures.length === 0) && (
                <div className="lecture-item">No lectures added yet.</div>
              )}
              {course.lectures?.map((lec, idx) => (
                <div className="lecture-item" key={lec.id || idx}>
                  <span className="lecture-num">{String(idx + 1).padStart(2, '0')}</span>
                  <span>{lec.title}</span>
                  {lec.preview && <span className="lecture-preview-tag">Preview</span>}
                  {!lec.preview && !enrolled && !isOwner && <span style={{ fontSize: 12, color: 'var(--color-ink-soft)' }}>🔒</span>}
                  {lec.durationMinutes && <span className="lecture-duration">{lec.durationMinutes} min</span>}
                </div>
              ))}
            </div>

            <div style={{ marginTop: 40 }}>
              <h3 style={{ marginBottom: 16 }}>Reviews</h3>
              {reviews.length === 0 && <p>No reviews yet.</p>}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                {reviews.map((r) => (
                  <div key={r.id} className="form-card" style={{ maxWidth: 'none', padding: 18 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <strong>{r.studentName}</strong>
                      <span className="stars">{'★'.repeat(r.rating)}{'☆'.repeat(5 - r.rating)}</span>
                    </div>
                    {r.comment && <p style={{ marginTop: 6, marginBottom: 0 }}>{r.comment}</p>}
                  </div>
                ))}
              </div>

              {enrolled && (
                <form onSubmit={handleReviewSubmit} className="form-card" style={{ maxWidth: 'none', marginTop: 20, padding: 20 }}>
                  <label style={{ fontWeight: 600, fontSize: 13, display: 'block', marginBottom: 8 }}>Leave a review</label>
                  <select value={reviewRating} onChange={(e) => setReviewRating(Number(e.target.value))} style={{ marginBottom: 10, padding: 8 }}>
                    {[5, 4, 3, 2, 1].map((n) => <option key={n} value={n}>{n} star{n !== 1 ? 's' : ''}</option>)}
                  </select>
                  <textarea
                    value={reviewComment}
                    onChange={(e) => setReviewComment(e.target.value)}
                    placeholder="What did you think of this course?"
                    rows={3}
                    style={{ width: '100%', padding: 10, border: '1.5px solid var(--color-line)', borderRadius: 6, marginBottom: 12 }}
                  />
                  <button className="btn btn-dark btn-sm" disabled={submittingReview}>
                    {submittingReview ? 'Submitting…' : 'Submit review'}
                  </button>
                </form>
              )}
            </div>
          </div>

          <div className="detail-sidebar-card">
            {error && <div className="form-error">{error}</div>}
            <div className="course-price" style={{ fontSize: 28, marginBottom: 16 }}>
              {isFree ? 'Free' : `$${price.toFixed(2)}`}
            </div>

            {isOwner ? (
              <Link to="/instructor" className="btn btn-dark btn-block">Manage this course</Link>
            ) : enrolled ? (
              <Link to="/dashboard" className="btn btn-primary btn-block">Go to course →</Link>
            ) : (
              <button className="btn btn-primary btn-block" onClick={handlePurchase} disabled={purchasing}>
                {purchasing ? 'Processing…' : isFree ? 'Enroll for free' : 'Buy this course'}
              </button>
            )}

            <div style={{ marginTop: 18, fontSize: 13.5, color: 'var(--color-ink-soft)', display: 'flex', flexDirection: 'column', gap: 8 }}>
              <div>📚 {course.lectures?.length || 0} lectures</div>
              <div>♾️ Lifetime access</div>
              <div>🎓 Certificate on completion</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
