import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { changePasswordSchema, profileSchema } from '../lib/schemas';
import { get, post, put, toError } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { useAuth } from '../lib/auth';
import { Badge, ErrorBox, Loading, Notice, PageTitle, Pagination, Table } from '../components/Ui';
import { FormError, Input, Password, Select, Submit } from '../components/Form';
import { date, title } from '../lib/format';

const GENDERS = [
  { value: 'MALE', label: 'Male' }, { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' }, { value: 'PREFER_NOT_TO_SAY', label: 'Prefer not to say' },
];

const TABS = [['details', 'Profile details'], ['security', 'Security'], ['activity', 'Activity log']];

export default function Profile() {
  const { refresh } = useAuth();
  const [tab, setTab] = useState('details');
  const [profileError, setProfileError] = useState(null);
  const [passwordError, setPasswordError] = useState(null);
  const [notice, setNotice] = useState(null);
  const [activityPage, setActivityPage] = useState(0);

  const { data: profile, loading, error, reload } = useFetch(() => get('/api/v1/users/me'), []);
  const { data: activity, loading: activityLoading } = useFetch(
    () => get('/api/v1/users/me/activity', { page: activityPage, size: 15 }), [activityPage]);

  const profileForm = useForm({
    resolver: zodResolver(profileSchema),
    values: profile ? {
      firstName: profile.firstName ?? '', lastName: profile.lastName ?? '', phone: profile.phone ?? '',
      dateOfBirth: profile.dateOfBirth ?? '', gender: profile.gender ?? '',
      // PAN comes back masked; blank means "leave unchanged"
      panNumber: '', annualIncome: profile.annualIncome ?? '', occupation: profile.occupation ?? '',
      address: profile.address ?? '', city: profile.city ?? '', state: profile.state ?? '',
      pincode: profile.pincode ?? '',
    } : undefined,
  });

  const passwordForm = useForm({
    resolver: zodResolver(changePasswordSchema), mode: 'onChange',
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
  });

  const newPassword = passwordForm.watch('newPassword') ?? '';

  const submitProfile = async (values) => {
    setProfileError(null);
    try {
      await put('/api/v1/users/me', Object.fromEntries(
        Object.entries(values).map(([key, value]) => [key, value === '' ? null : value])));
      await refresh();
      setNotice({ type: 'success', text: 'Profile updated.' });
      reload();
    } catch (err) {
      setProfileError(err.status ? err : toError(err));
    }
  };

  const submitPassword = async (values) => {
    setPasswordError(null);
    try {
      await post('/api/v1/users/me/change-password', values);
      setNotice({ type: 'success', text: 'Password changed successfully.' });
      passwordForm.reset();
    } catch (err) {
      setPasswordError(err.status ? err : toError(err));
    }
  };

  if (loading) return <Loading label="Loading your profile…" />;
  if (error) return <ErrorBox error={error} onRetry={reload} />;

  return (
    <div className="space-y-6">
      <PageTitle title="My profile" />

      {notice && <Notice type={notice.type} onDismiss={() => setNotice(null)}>{notice.text}</Notice>}

      <div className="card">
        <div className="flex flex-wrap items-start gap-4">
          <span className="grid h-14 w-14 shrink-0 place-items-center rounded-full bg-teal-100 text-xl font-bold text-brand">
            {(profile?.firstName?.[0] ?? 'U').toUpperCase()}
          </span>
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-lg font-bold text-slate-900">{profile?.fullName}</h2>
              <Badge status={profile?.status} />
              {profile?.premium && <span className="badge bg-amber-100 text-amber-800">{profile.tier}</span>}
            </div>
            <p className="mt-1 text-sm text-slate-600">{profile?.email} · {profile?.phone}</p>
            <p className="mt-1 text-xs text-slate-500">
              Member since {date(profile?.createdAt)}
              {profile?.panNumber && ` · PAN ${profile.panNumber}`}
              {profile?.age && ` · age ${profile.age}`}
              {profile?.lastLoginAt && ` · last seen ${date(profile.lastLoginAt, true)}`}
            </p>
          </div>
        </div>
      </div>

      <div className="border-b border-slate-200">
        <nav className="-mb-px flex gap-1">
          {TABS.map(([id, label]) => (
            <button key={id} type="button" onClick={() => setTab(id)}
                    className={`border-b-2 px-4 py-2 text-sm font-medium ${
                      tab === id ? 'border-brand text-brand'
                                 : 'border-transparent text-slate-500 hover:text-slate-700'}`}>
              {label}
            </button>
          ))}
        </nav>
      </div>

      {/* ---------- details ---------- */}
      {tab === 'details' && (
        <form onSubmit={profileForm.handleSubmit(submitProfile)} noValidate className="space-y-5">
          <FormError error={profileError} />

          <fieldset className="card">
            <legend className="text-sm font-bold uppercase text-brand">Personal details</legend>
            <p className="mt-1 text-sm text-slate-500">
              Your email address is your sign-in identity and cannot be changed here.
            </p>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <Input label="First name" required error={profileForm.formState.errors.firstName}
                     {...profileForm.register('firstName')} />
              <Input label="Last name" required error={profileForm.formState.errors.lastName}
                     {...profileForm.register('lastName')} />
              <Input label="Mobile number" required type="tel" error={profileForm.formState.errors.phone}
                     {...profileForm.register('phone')} />
              <Input label="Date of birth" type="date" error={profileForm.formState.errors.dateOfBirth}
                     {...profileForm.register('dateOfBirth')} />
              <Select label="Gender" placeholder="Prefer not to say" options={GENDERS}
                      error={profileForm.formState.errors.gender} {...profileForm.register('gender')} />
              <Input label="PAN" placeholder={profile?.panNumber ?? 'ABCPE1234F'}
                     hint="Leave blank to keep your existing PAN"
                     error={profileForm.formState.errors.panNumber} {...profileForm.register('panNumber')} />
            </div>
          </fieldset>

          <fieldset className="card">
            <legend className="text-sm font-bold uppercase text-brand">Financial details</legend>
            <p className="mt-1 text-sm text-slate-500">
              Used to size your recommendations. Keeping these current keeps the advice relevant.
            </p>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <Input label="Annual income" type="number" min="0" step="10000"
                     error={profileForm.formState.errors.annualIncome}
                     {...profileForm.register('annualIncome')} />
              <Input label="Occupation" error={profileForm.formState.errors.occupation}
                     {...profileForm.register('occupation')} />
            </div>
          </fieldset>

          <fieldset className="card">
            <legend className="text-sm font-bold uppercase text-brand">Address</legend>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <Input label="Address" className="sm:col-span-2" error={profileForm.formState.errors.address}
                     {...profileForm.register('address')} />
              <Input label="City" error={profileForm.formState.errors.city}
                     {...profileForm.register('city')} />
              <Input label="State" error={profileForm.formState.errors.state}
                     {...profileForm.register('state')} />
              <Input label="PIN code" error={profileForm.formState.errors.pincode}
                     {...profileForm.register('pincode')} />
            </div>
          </fieldset>

          <Submit loading={profileForm.formState.isSubmitting}
                  disabled={!profileForm.formState.isDirty}>Save changes</Submit>
        </form>
      )}

      {/* ---------- security ---------- */}
      {tab === 'security' && (
        <div className="grid gap-6 lg:grid-cols-2">
          <form onSubmit={passwordForm.handleSubmit(submitPassword)} noValidate className="card space-y-4">
            <h2 className="font-semibold text-slate-900">Change your password</h2>
            <FormError error={passwordError} />

            <Password label="Current password" required autoComplete="current-password"
                      error={passwordForm.formState.errors.currentPassword}
                      {...passwordForm.register('currentPassword')} />
            <Password label="New password" required meter value={newPassword} autoComplete="new-password"
                      error={passwordForm.formState.errors.newPassword}
                      {...passwordForm.register('newPassword')} />
            <Password label="Confirm new password" required autoComplete="new-password"
                      error={passwordForm.formState.errors.confirmPassword}
                      {...passwordForm.register('confirmPassword')} />

            <Submit loading={passwordForm.formState.isSubmitting}
                    disabled={!passwordForm.formState.isValid}>Update password</Submit>
          </form>

          <div className="space-y-5">
            <div className="card">
              <h2 className="font-semibold text-slate-900">How your account is protected</h2>
              <ul className="mt-3 space-y-2 text-sm text-slate-600">
                {[
                  'Passwords are hashed with BCrypt at a work factor of 12 and never stored in plain text.',
                  'Five consecutive failed sign-ins lock the account and trigger an email alert.',
                  'Your PAN is masked everywhere it is displayed, showing only the last four characters.',
                  'Tokens are held in browser session storage, so closing the tab ends the session.',
                ].map((item) => <li key={item}>• {item}</li>)}
              </ul>
            </div>

            <div className="card">
              <h2 className="font-semibold text-slate-900">Account status</h2>
              <dl className="mt-3 space-y-2 text-sm">
                <div className="flex justify-between">
                  <dt className="text-slate-600">Email verified</dt>
                  <dd className="font-semibold">{profile?.emailVerified ? 'Yes' : 'No'}</dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-slate-600">Account status</dt>
                  <dd><Badge status={profile?.status} /></dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-slate-600">Roles</dt>
                  <dd className="font-semibold">
                    {profile?.roles?.map((r) => title(r.replace('ROLE_', ''))).join(', ')}
                  </dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-slate-600">Subscription</dt>
                  <dd className="font-semibold">{profile?.tier}</dd>
                </div>
              </dl>
            </div>
          </div>
        </div>
      )}

      {/* ---------- activity ---------- */}
      {tab === 'activity' && (
        <div>
          <p className="mb-3 text-sm text-slate-500">
            Every sign-in, profile change and report download on your account.
          </p>
          {activityLoading ? <Loading label="Loading activity…" />
            : !activity?.content?.length ? (
              <p className="card text-center text-sm text-slate-400">No activity recorded yet.</p>
            ) : (
              <>
                <Table headers={['When', 'Activity', 'Detail', 'IP address', 'Result']}>
                  {activity.content.map((entry) => (
                    <tr key={entry.id}>
                      <td>{date(entry.createdAt, true)}</td>
                      <td className="font-medium text-slate-800">{title(entry.action)}</td>
                      <td className="max-w-xs truncate text-xs">{entry.description}</td>
                      <td className="font-mono text-xs text-slate-500">{entry.ipAddress ?? '—'}</td>
                      <td>
                        <Badge status={entry.successful ? 'SUCCESS' : 'FAILED'}>
                          {entry.successful ? 'Success' : 'Failed'}
                        </Badge>
                      </td>
                    </tr>
                  ))}
                </Table>
                <Pagination page={activity.pageNumber} totalPages={activity.totalPages}
                            onChange={setActivityPage} />
              </>
            )}
        </div>
      )}
    </div>
  );
}
