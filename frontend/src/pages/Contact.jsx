import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { contactSchema } from '../lib/schemas';
import { post, toError } from '../lib/api';
import { useAuth } from '../lib/auth';
import { FormError, Input, Submit, TextArea } from '../components/Form';
import { Notice, PageTitle } from '../components/Ui';

const CHANNELS = [
  ['Email', 'support@investwise.in', 'Replies within one business day'],
  ['Phone', '+91 20 4000 1234', 'Mon to Fri, 9:30am to 6:30pm IST'],
  ['Office', 'Baner Road, Pune 411045', 'Visits by appointment only'],
];

export default function Contact() {
  const { user } = useAuth();
  const [error, setError] = useState(null);
  const [sent, setSent] = useState(false);

  const { register, handleSubmit, reset, watch, formState: { errors, isSubmitting, isValid } } =
    useForm({
      resolver: zodResolver(contactSchema),
      mode: 'onChange',
      defaultValues: {
        name: user?.fullName ?? '', email: user?.email ?? '', phone: user?.phone ?? '',
        subject: '', message: '',
      },
    });

  const messageLength = watch('message')?.length ?? 0;

  const onSubmit = async (values) => {
    setError(null);
    try {
      await post('/api/v1/contact', values);
      setSent(true);
      reset({ ...values, subject: '', message: '' });
    } catch (err) {
      setError(err.status ? err : toError(err));
    }
  };

  return (
    <div className="page">
      <PageTitle title="Talk to us"
                 subtitle="Questions about the methodology, your account, or a payment. This form reaches a person." />

      <div className="grid gap-6 lg:grid-cols-[1fr_300px]">
        <div className="card">
          {sent && (
            <Notice type="success" onDismiss={() => setSent(false)}>
              Message received. A confirmation has been emailed to you and our team will respond
              within one business day.
            </Notice>
          )}

          <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-4 space-y-4">
            <FormError error={error} />

            <div className="grid gap-4 sm:grid-cols-2">
              <Input label="Your name" required placeholder="Rahul Sharma"
                     error={errors.name} {...register('name')} />
              <Input label="Email address" required type="email" placeholder="you@example.com"
                     error={errors.email} {...register('email')} />
            </div>

            <Input label="Mobile number" type="tel" placeholder="9876543210"
                   hint="Optional. Include it if you would prefer a call back."
                   error={errors.phone} {...register('phone')} />

            <Input label="Subject" required placeholder="Question about the risk assessment"
                   error={errors.subject} {...register('subject')} />

            <div>
              <TextArea label="Message" required rows={6}
                        placeholder="Tell us what you need help with…"
                        error={errors.message} {...register('message')} />
              <p className={`mt-1 text-right text-xs ${messageLength > 2000 ? 'text-red-600' : 'text-slate-400'}`}>
                {messageLength} / 2000
              </p>
            </div>

            <div className="flex items-center gap-3 border-t border-slate-200 pt-4">
              <Submit loading={isSubmitting} disabled={!isValid}>Send message</Submit>
              <p className="text-xs text-slate-500">We never share your details with third parties.</p>
            </div>
          </form>
        </div>

        <aside className="space-y-4">
          {CHANNELS.map(([label, value, detail]) => (
            <div key={label} className="card">
              <p className="text-xs uppercase text-slate-400">{label}</p>
              <p className="mt-1 font-semibold text-slate-900">{value}</p>
              <p className="mt-0.5 text-xs text-slate-500">{detail}</p>
            </div>
          ))}
          <div className="card bg-slate-100">
            <p className="text-xs leading-relaxed text-slate-600">
              <strong>Please note:</strong> we cannot provide individual investment recommendations
              over email or phone. The recommendation engine exists precisely because personalised
              advice requires your full risk profile and goal set.
            </p>
          </div>
        </aside>
      </div>
    </div>
  );
}
