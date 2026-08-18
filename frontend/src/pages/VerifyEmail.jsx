import React, { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { authApi } from '../services/courseService';

export default function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [status, setStatus] = useState('verifying'); // verifying | success | error
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (!token) {
      setStatus('error');
      setMessage('Missing verification token.');
      return;
    }
    authApi.verifyEmail(token)
      .then(() => setStatus('success'))
      .catch((err) => {
        setStatus('error');
        setMessage(err.response?.data?.message || 'Verification failed. The link may have expired.');
      });
  }, [token]);

  return (
    <div className="container page">
      <div className="form-card" style={{ textAlign: 'center' }}>
        {status === 'verifying' && <p>Verifying your email…</p>}

        {status === 'success' && (
          <>
            <div style={{ fontSize: 40, marginBottom: 12 }}>✓</div>
            <h2>Email verified</h2>
            <p style={{ marginTop: 10 }}>Your email address has been confirmed.</p>
            <Link to="/" className="btn btn-primary" style={{ marginTop: 20 }}>Continue to LearnHub</Link>
          </>
        )}

        {status === 'error' && (
          <>
            <div style={{ fontSize: 40, marginBottom: 12 }}>⚠</div>
            <h2>Verification failed</h2>
            <p style={{ marginTop: 10 }}>{message}</p>
            <Link to="/" className="btn btn-outline" style={{ marginTop: 20, color: 'var(--color-ink)', borderColor: 'var(--color-line)' }}>
              Back to home
            </Link>
          </>
        )}
      </div>
    </div>
  );
}
