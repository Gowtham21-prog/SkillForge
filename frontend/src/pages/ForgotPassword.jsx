import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../services/courseService';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await authApi.forgotPassword(email);
      // Always show the same success state, whether or not the email exists —
      // matches the backend's intentional non-disclosure behavior.
      setSubmitted(true);
    } catch (err) {
      setError(err.response?.data?.message || 'Something went wrong. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container page">
      <div className="form-card">
        <span className="eyebrow">Account recovery</span>
        <h2 style={{ marginTop: 8, marginBottom: 24 }}>Reset your password</h2>

        {submitted ? (
          <p>
            If an account exists for <strong>{email}</strong>, we've sent a link to reset your
            password. It expires in 1 hour.
          </p>
        ) : (
          <>
            {error && <div className="form-error">{error}</div>}
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Email</label>
                <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
              </div>
              <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
                {loading ? 'Sending…' : 'Send reset link'}
              </button>
            </form>
          </>
        )}

        <p style={{ marginTop: 20, fontSize: 14, textAlign: 'center' }}>
          <Link to="/login" style={{ color: 'var(--color-accent)', fontWeight: 600 }}>Back to log in</Link>
        </p>
      </div>
    </div>
  );
}
