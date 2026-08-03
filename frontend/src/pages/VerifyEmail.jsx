import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { get, toError } from '../lib/api';

export default function VerifyEmail() {
  const [params] = useSearchParams();
  const token = params.get('token');
  const [state, setState] = useState('verifying');
  const [message, setMessage] = useState('');
  // The token is single-use, and StrictMode double-invokes effects in development
  const attempted = useRef(false);

  useEffect(() => {
    if (!token) {
      setState('error');
      setMessage('This link is missing its verification token.');
      return;
    }
    if (attempted.current) return;
    attempted.current = true;

    get('/api/v1/auth/verify-email', { token })
      .then(() => {
        setState('done');
        setMessage('Your email address is confirmed and your account is now active.');
      })
      .catch((err) => {
        setState('error');
        setMessage(toError(err).message);
      });
  }, [token]);

  return (
    <div className="card text-center">
      {state === 'verifying' && (
        <>
          <h1 className="text-xl font-bold text-slate-900">Confirming your email…</h1>
          <p className="mt-2 text-sm text-slate-600">This will only take a moment.</p>
        </>
      )}

      {state === 'done' && (
        <>
          <h1 className="text-xl font-bold text-slate-900">Email verified</h1>
          <p className="mt-2 text-sm text-slate-600">{message}</p>
          <Link to="/login" className="btn-primary mt-5 w-full">Sign in to your account</Link>
        </>
      )}

      {state === 'error' && (
        <>
          <h1 className="text-xl font-bold text-slate-900">We could not verify this link</h1>
          <p className="mt-2 text-sm text-slate-600">{message}</p>
          <p className="mt-2 text-xs text-slate-500">
            Verification links expire after 24 hours and can be used only once. Request a fresh one
            from the sign-in screen.
          </p>
          <Link to="/login" className="btn-primary mt-5 w-full">Go to sign in</Link>
        </>
      )}
    </div>
  );
}
