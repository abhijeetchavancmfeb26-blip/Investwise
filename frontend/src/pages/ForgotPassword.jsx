import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { forgotSchema } from '../lib/schemas';
import { post, toError } from '../lib/api';
import { FormError, Input, Submit } from '../components/Form';

export default function ForgotPassword() {
  const [sent, setSent] = useState(false);
  const [error, setError] = useState(null);

  const { register, handleSubmit, getValues, formState: { errors, isSubmitting, isValid } } =
    useForm({ resolver: zodResolver(forgotSchema), mode: 'onChange', defaultValues: { email: '' } });

  const onSubmit = async (values) => {
    setError(null);
    try {
      await post('/api/v1/auth/forgot-password', values);
      setSent(true);
    } catch (err) {
      setError(toError(err));
    }
  };

  if (sent) {
    return (
      <div className="card text-center">
        <h1 className="text-xl font-bold text-slate-900">Check your inbox</h1>
        <p className="mt-2 text-sm text-slate-600">
          If <strong>{getValues('email')}</strong> is registered with us, a reset link is on its way.
          The link is valid for 30 minutes and can be used once.
        </p>
        <p className="mt-3 text-xs text-slate-500">
          We deliberately do not confirm whether an address exists, because doing so would let anyone
          check who has an account here.
        </p>
        <Link to="/login" className="btn-primary mt-5 w-full">Back to sign in</Link>
      </div>
    );
  }

  return (
    <div className="card">
      <h1 className="text-xl font-bold text-slate-900">Reset your password</h1>
      <p className="mt-1 text-sm text-slate-600">
        Enter the email address on your account and we will send you a link.
      </p>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-5 space-y-4">
        <FormError error={error} />
        <Input label="Email address" required type="email" autoFocus placeholder="you@example.com"
               error={errors.email} {...register('email')} />
        <Submit loading={isSubmitting} disabled={!isValid} className="w-full">Send reset link</Submit>
      </form>

      <p className="mt-4 text-center text-xs text-slate-500">
        Remembered it? <Link to="/login" className="font-medium text-brand hover:underline">Sign in</Link>
      </p>
    </div>
  );
}
