import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { registerSchema } from '../lib/schemas';
import { get, post, toError } from '../lib/api';
import { Checkbox, FormError, Input, Password, Select, Submit } from '../components/Form';

const GENDERS = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
  { value: 'PREFER_NOT_TO_SAY', label: 'Prefer not to say' },
];

export default function Register() {
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  const [emailStatus, setEmailStatus] = useState(null);

  const { register, handleSubmit, watch, formState: { errors, isSubmitting, isValid } } =
    useForm({
      resolver: zodResolver(registerSchema), mode: 'onChange',
      defaultValues: {
        firstName: '', lastName: '', email: '', password: '', confirmPassword: '',
        phone: '', dateOfBirth: '', gender: '', panNumber: '', annualIncome: '',
        occupation: '', acceptTerms: false,
      },
    });

  const password = watch('password') ?? '';
  const email = watch('email') ?? '';

  // Live availability check, debounced inline
  useEffect(() => {
    if (!email.includes('@') || errors.email) { setEmailStatus(null); return undefined; }

    const timer = setTimeout(() => {
      setEmailStatus('checking');
      get('/api/v1/auth/check-email', { email })
        .then((result) => setEmailStatus(result?.available ? 'available' : 'taken'))
        .catch(() => setEmailStatus(null));
    }, 600);
    return () => clearTimeout(timer);
  }, [email, errors.email]);

  const onSubmit = async (values) => {
    setError(null);
    try {
      const { acceptTerms, ...payload } = values;
      await post('/api/v1/auth/register', {
        ...payload,
        annualIncome: payload.annualIncome === '' ? null : payload.annualIncome,
        panNumber: payload.panNumber === '' ? null : payload.panNumber,
        gender: payload.gender === '' ? null : payload.gender,
        occupation: payload.occupation === '' ? null : payload.occupation,
      });
      navigate('/login', { replace: true });
    } catch (err) {
      setError(err.status ? err : toError(err));
    }
  };

  const maxDob = new Date(Date.now() - 18 * 365.25 * 864e5).toISOString().slice(0, 10);

  return (
    <div className="card">
      <h1 className="text-xl font-bold text-slate-900">Create your account</h1>
      <p className="mt-1 text-sm text-slate-600">
        Already registered? <Link to="/login" className="font-medium text-brand hover:underline">Sign in</Link>
      </p>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-5 space-y-4">
        <FormError error={error} />

        <div className="grid gap-4 sm:grid-cols-2">
          <Input label="First name" required autoFocus placeholder="Rahul"
                 error={errors.firstName} {...register('firstName')} />
          <Input label="Last name" required placeholder="Sharma"
                 error={errors.lastName} {...register('lastName')} />
        </div>

        <div>
          <Input label="Email address" required type="email" placeholder="you@example.com"
                 error={errors.email} {...register('email')} />
          {!errors.email && emailStatus === 'available' && (
            <p className="mt-1 text-xs text-emerald-600">This address is available.</p>
          )}
          {!errors.email && emailStatus === 'taken' && (
            <p className="mt-1 text-xs text-red-600">
              Already registered. <Link to="/login" className="underline">Sign in instead?</Link>
            </p>
          )}
          {emailStatus === 'checking' && <p className="mt-1 text-xs text-slate-400">Checking…</p>}
        </div>

        <Password label="Password" required meter value={password} autoComplete="new-password"
                  placeholder="Choose a strong password" error={errors.password} {...register('password')} />

        <Password label="Confirm password" required autoComplete="new-password"
                  placeholder="Re-enter your password"
                  error={errors.confirmPassword} {...register('confirmPassword')} />

        <div className="grid gap-4 sm:grid-cols-2">
          <Input label="Mobile number" required type="tel" placeholder="9876543210"
                 hint="10 digits, starting 6 to 9" error={errors.phone} {...register('phone')} />
          <Input label="Date of birth" required type="date" max={maxDob}
                 hint="You must be 18 or older" error={errors.dateOfBirth} {...register('dateOfBirth')} />
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <Select label="Gender" placeholder="Prefer not to say" options={GENDERS}
                  error={errors.gender} {...register('gender')} />
          <Input label="PAN" placeholder="ABCPE1234F" hint="Optional, used only on your own reports"
                 error={errors.panNumber} {...register('panNumber')} />
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <Input label="Annual income" type="number" min="0" step="10000" placeholder="900000"
                 hint="Optional. Helps size your recommendations."
                 error={errors.annualIncome} {...register('annualIncome')} />
          <Input label="Occupation" placeholder="Software Engineer"
                 error={errors.occupation} {...register('occupation')} />
        </div>

        <Checkbox
          label={<>I accept the{' '}
            <Link to="/legal/terms" target="_blank" className="font-medium text-brand hover:underline">Terms</Link>
            {' '}and{' '}
            <Link to="/legal/privacy" target="_blank" className="font-medium text-brand hover:underline">Privacy Policy</Link>
            , and understand InvestWise is a planning platform, not a broker or adviser.</>}
          error={errors.acceptTerms} {...register('acceptTerms')} />

        <Submit loading={isSubmitting} disabled={!isValid || emailStatus === 'taken'} className="w-full">
          Create my account
        </Submit>
      </form>
    </div>
  );
}
