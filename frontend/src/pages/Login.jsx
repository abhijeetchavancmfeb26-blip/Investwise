import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { loginSchema } from '../lib/schemas';
import { useAuth } from '../lib/auth';
import { post, toError } from '../lib/api';
import { Checkbox, FormError, Input, Password, Submit } from '../components/Form';
import { Notice } from '../components/Ui';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState(null);
  const [resent, setResent] = useState(false);

  const { register, handleSubmit, watch, formState: { errors, isSubmitting, isValid } } =
    useForm({ resolver: zodResolver(loginSchema), mode: 'onChange',
              defaultValues: { email: '', password: '', rememberMe: false } });

  const email = watch('email');
  // A 403 mentioning verification means the credentials were right
  const needsVerification = error?.status === 403 && error.message?.toLowerCase().includes('verif');

  const onSubmit = async (values) => {
    setError(null);
    try {
      const user = await login(values);
      navigate(location.state?.from ?? (user.roles?.includes('ROLE_ADMIN') ? '/admin' : '/dashboard'),
               { replace: true });
    } catch (err) {
      setError(err.status ? err : toError(err));
    }
  };

  const resend = async () => {
    try {
      await post('/api/v1/auth/resend-verification', null, { email });
      setResent(true);
    } catch (err) {
      setError(toError(err));
    }
  };

  return (
    <div className="card">
      <h1 className="text-xl font-bold text-slate-900">Sign in to InvestWise</h1>
      <p className="mt-1 text-sm text-slate-600">
        New here? <Link to="/register" className="font-medium text-brand hover:underline">Create a free account</Link>
      </p>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-5 space-y-4">
        <FormError error={error} />

        {needsVerification && (
          <Notice type="warning">
            Your email address is not yet verified.{' '}
            {resent ? 'A fresh link is on its way.' : (
              <button type="button" onClick={resend} className="font-medium underline">
                Send me a new verification link
              </button>
            )}
          </Notice>
        )}

        <Input label="Email address" required type="email" autoComplete="email" autoFocus
               placeholder="you@example.com" error={errors.email} {...register('email')} />

        <div>
          <Password label="Password" required autoComplete="current-password"
                    placeholder="Enter your password" error={errors.password} {...register('password')} />
          <p className="mt-1 text-right">
            <Link to="/forgot-password" className="text-xs font-medium text-brand hover:underline">
              Forgot your password?
            </Link>
          </p>
        </div>

        <Checkbox label="Keep me signed in on this device for longer" {...register('rememberMe')} />

        <Submit loading={isSubmitting} disabled={!isValid} className="w-full">Sign in</Submit>
      </form>

      {/* <div className="mt-6 rounded border border-sky-200 bg-sky-50 p-3 text-xs text-sky-800">
        <p className="font-semibold">Demo credentials</p>
        <ul className="mt-1 space-y-0.5">
          <li>Investor: rahul.sharma@example.com / User@123</li>
          <li>Premium: priya.nair@example.com / User@123</li>
          <li>Admin: admin@investwise.in / Admin@123</li>
        </ul>
      </div> */}
    </div>
  );
}

