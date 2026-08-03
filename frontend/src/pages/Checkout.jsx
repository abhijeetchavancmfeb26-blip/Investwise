import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { get, post, toError } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { useAuth } from '../lib/auth';
import { ErrorBox, Loading, Notice, PageTitle } from '../components/Ui';
import { money } from '../lib/format';

/**
 * Razorpay checkout.
 *
 * Three-legged by design: the server creates the order, the browser opens Checkout
 * with only the public key, and the returned signature is verified back on the
 * server before anything is unlocked.
 */
export default function Checkout() {
  const { planCode } = useParams();
  const { user, refresh } = useAuth();
  const [stage, setStage] = useState('review');
  const [error, setError] = useState(null);
  const [payment, setPayment] = useState(null);

  const { data: plans, loading, error: loadError } = useFetch(() => get('/api/v1/plans'), []);
  const plan = plans?.find((item) => item.code === planCode);

  const startPayment = async () => {
    setError(null);
    setStage('creating');

    try {
      const order = await post('/api/v1/payments/create-order', { planCode });

      if (!window.Razorpay) {
        throw new Error('The payment gateway script could not be loaded.');
      }
      setStage('paying');

      const checkout = new window.Razorpay({
        key: order.razorpayKeyId,
        amount: order.amountInPaise,
        currency: order.currency,
        name: 'InvestWise',
        description: order.description,
        order_id: order.orderId,
        prefill: { name: order.customerName, email: order.customerEmail },
        theme: { color: '#0f766e' },
        handler: async (response) => {
          setStage('verifying');
          try {
            setPayment(await post('/api/v1/payments/verify', {
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            }));
            setStage('done');
            await refresh();
          } catch (err) {
            setError(err.status ? err : toError(err));
            setStage('failed');
          }
        },
        modal: {
          ondismiss: async () => {
            // Recording the abandonment keeps the payment ledger honest
            try {
              await post('/api/v1/payments/failed', null,
                         { razorpayOrderId: order.orderId, reason: 'Cancelled by the customer' });
            } catch { /* best effort */ }
            setStage('review');
          },
        },
      });

      checkout.on('payment.failed', async (response) => {
        const reason = response?.error?.description ?? 'The payment was declined.';
        try {
          await post('/api/v1/payments/failed', null,
                     { razorpayOrderId: order.orderId, reason });
        } catch { /* best effort */ }
        setError({ message: reason, fieldErrors: {} });
        setStage('failed');
      });

      checkout.open();

    } catch (err) {
      setError(err.status ? err : toError(err));
      setStage('failed');
    }
  };

  if (loading) return <Loading label="Loading plan details…" />;
  if (loadError) return <ErrorBox error={loadError} />;
  if (!plan) {
    return (
      <div className="mx-auto max-w-lg">
        <div className="card text-center">
          <h1 className="font-bold text-slate-900">That plan does not exist</h1>
          <Link to="/pricing" className="btn-primary mt-4">Back to plans</Link>
        </div>
      </div>
    );
  }

  if (stage === 'done') {
    return (
      <div className="mx-auto max-w-lg">
        <div className="card text-center">
          <h1 className="text-xl font-bold text-slate-900">Payment successful</h1>
          <p className="mt-3 text-sm leading-relaxed text-slate-600">
            Your {plan.name} plan is now active. Advanced analytics, premium reports and unlimited
            goals are unlocked.
          </p>

          {payment && (
            <dl className="mt-5 space-y-2 rounded bg-slate-50 p-4 text-left text-sm">
              <div className="flex justify-between">
                <dt className="text-slate-600">Amount paid</dt>
                <dd className="font-semibold">{money(payment.amount)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-slate-600">Invoice number</dt>
                <dd className="font-mono text-xs font-semibold">{payment.invoiceNo}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-slate-600">Payment reference</dt>
                <dd className="font-mono text-xs">{payment.paymentId}</dd>
              </div>
            </dl>
          )}

          <div className="mt-5 space-y-2">
            <Link to="/analytics" className="btn-primary w-full">Open premium analytics</Link>
            <Link to="/subscription" className="btn-secondary w-full">View my subscription</Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl space-y-5">
      <Link to="/pricing" className="text-sm text-brand hover:underline">← Back to plans</Link>
      <PageTitle title={`Subscribe to ${plan.name}`} />

      {stage === 'failed' && error && (
        <Notice type="error">
          {error.message} No premium access has been granted. If an amount was deducted without a
          verified signature, the gateway reverses it automatically.
        </Notice>
      )}

      <div className="grid gap-5 md:grid-cols-[1fr_280px]">
        <div className="card">
          <h2 className="font-semibold text-slate-900">What you are buying</h2>
          <p className="mt-2 text-sm text-slate-600">{plan.description}</p>

          <ul className="mt-4 space-y-2">
            {plan.features.map((feature) => (
              <li key={feature} className="text-sm text-slate-600">✓ {feature}</li>
            ))}
          </ul>

          <div className="mt-5 space-y-2 border-t border-slate-200 pt-4 text-xs leading-relaxed text-slate-600">
            <p>Card and UPI details are handled entirely by Razorpay and never reach our servers.</p>
            <p>
              The payment signature is verified on our servers before any feature is unlocked, so a
              forged success callback cannot grant access.
            </p>
          </div>
        </div>

        <div className="card h-fit">
          <h2 className="text-xs font-bold uppercase text-slate-500">Order summary</h2>
          <dl className="mt-3 space-y-2 text-sm">
            <div className="flex justify-between">
              <dt className="text-slate-600">Plan</dt><dd className="font-semibold">{plan.name}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-600">Term</dt>
              <dd className="font-semibold">{plan.durationMonths} months</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-600">Effective monthly</dt>
              <dd className="font-semibold">{money(plan.monthlyEquivalent)}</dd>
            </div>
          </dl>

          <div className="mt-3 flex items-baseline justify-between border-t border-slate-200 pt-3">
            <span className="text-sm font-semibold text-slate-700">Total due today</span>
            <span className="text-xl font-bold text-slate-900">{money(plan.price)}</span>
          </div>
          <p className="mt-1 text-xs text-slate-500">Inclusive of applicable taxes</p>

          <button type="button" onClick={startPayment} className="btn-primary mt-4 w-full"
                  disabled={['creating', 'paying', 'verifying'].includes(stage)}>
            {stage === 'creating' ? 'Creating order…'
              : stage === 'paying' ? 'Complete the payment…'
              : stage === 'verifying' ? 'Verifying…'
              : stage === 'failed' ? 'Try again' : 'Pay securely'}
          </button>

          <p className="mt-3 text-center text-xs leading-relaxed text-slate-400">
            By continuing you agree to our{' '}
            <Link to="/legal/terms" className="underline">Terms</Link>. Cancelling later keeps access
            until the end of the paid term.
          </p>
        </div>
      </div>
    </div>
  );
}
