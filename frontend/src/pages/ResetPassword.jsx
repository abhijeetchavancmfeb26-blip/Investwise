import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { resetSchema } from '../lib/schemas';
import { post, toError } from '../lib/api';
import { FormError, Password, Submit } from '../components/Form';

export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  const [done, setDone] = useState(false);

  const { register, handleSubmit, watch, formState: { errors, isSubmitting, isValid } } =
    useForm({ resolver: zodResolver(resetSchema), mode: 'onChange',
              defaultValues: { token, newPassword: '', confirmPassword: '' } });

  const newPassword = watch('newPassword') ?? '';

  const onSubmit = async (values) => {
    setError(null);
    try {
      await post('/api/v1/auth/reset-password', values);
      setDone(true);
      setTimeout(() => navigate('/login', { replace: true }), 2500);
    } catch (err) {
      setError(err.status ? err : toError(err));
    }
  };

  if (!token) {
    return (
      <div className="card text-center">
        <h1 className="text-xl font-bold text-slate-900">This link is incomplete</h1>
        <p className="mt-2 text-sm text-slate-600">
          The reset token is missing. Request a fresh link and open it directly from your email.
        </p>
        <Link to="/forgot-password" className="btn-primary mt-5 w-full">Request a new link</Link>
      </div>
    );
  }

  if (done) {
    return (
      <div className="card text-center">
        <h1 className="text-xl font-bold text-slate-900">Password updated</h1>
        <p className="mt-2 text-sm text-slate-600">Taking you to the sign-in screen now.</p>
        <Link to="/login" className="btn-primary mt-5 w-full">Sign in</Link>
      </div>
    );
  }

  return (
    <div className="card">
      <h1 className="text-xl font-bold text-slate-900">Choose a new password</h1>
      <p className="mt-1 text-sm text-slate-600">
        It must differ from your previous password and satisfy the rules below.
      </p>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-5 space-y-4">
        <FormError error={error} />
        <input type="hidden" {...register('token')} />

        <Password label="New password" required meter value={newPassword} autoFocus
                  autoComplete="new-password" error={errors.newPassword} {...register('newPassword')} />
        <Password label="Confirm new password" required autoComplete="new-password"
                  error={errors.confirmPassword} {...register('confirmPassword')} />

        <Submit loading={isSubmitting} disabled={!isValid} className="w-full">Update my password</Submit>
      </form>
    </div>
  );
}
