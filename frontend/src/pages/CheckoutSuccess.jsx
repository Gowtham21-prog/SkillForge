import React, { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';

// Stripe redirects here with ?session_id=... after a successful payment. The actual
// enrollment is created server-side by the webhook (StripeWebhookController), which can
// take a second or two to arrive — so this page just confirms the payment went through
// and points the user to their dashboard rather than trying to verify enrollment itself.
export default function CheckoutSuccess() {
  const [searchParams] = useSearchParams();
  const sessionId = searchParams.get('session_id');
  const [secondsWaited, setSecondsWaited] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => setSecondsWaited((s) => s + 1), 1000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="container page">
      <div className="form-card" style={{ textAlign: 'center' }}>
        <div style={{ fontSize: 40, marginBottom: 12 }}>✓</div>
        <h2>Payment successful</h2>
        <p style={{ marginTop: 10 }}>
          Thanks for your purchase! We're finalizing your enrollment now — this usually takes
          just a few seconds.
        </p>
        {sessionId && (
          <p style={{ fontSize: 12, fontFamily: 'var(--font-mono)', color: 'var(--color-ink-soft)' }}>
            Reference: {sessionId.slice(0, 24)}…
          </p>
        )}
        <Link to="/dashboard" className="btn btn-primary" style={{ marginTop: 20 }}>
          Go to my learning {secondsWaited > 3 ? '→' : ''}
        </Link>
      </div>
    </div>
  );
}
