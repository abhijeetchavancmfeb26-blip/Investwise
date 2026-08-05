import { useState } from 'react';
import { get } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { Badge, Empty, ErrorBox, Loading, PageTitle, Pagination, Stat, Table } from '../components/Ui';
import { BarChart, ChartBox, LineChart } from '../components/Charts';
import { compact, date, money, title } from '../lib/format';

const SUB_STATUSES = ['PENDING', 'ACTIVE', 'EXPIRED', 'CANCELLED'];
const PAY_STATUSES = ['CREATED', 'SUCCESS', 'FAILED'];

/**
 * Subscriptions and payments together.
 * The original split these into two admin pages; they answer the same question
 * from two angles, so one page with two tables is easier to work from.
 */
export default function AdminBilling() {
  const [tab, setTab] = useState('payments');
  const [subStatus, setSubStatus] = useState('');
  const [payStatus, setPayStatus] = useState('');
  const [userId, setUserId] = useState('');
  const [page, setPage] = useState(0);

  const { data: stats } = useFetch(() => get('/api/v1/admin/stats'), []);
  const { data: subscriptions, loading: subLoading, error: subError, reload: reloadSubs } = useFetch(
    () => get('/api/v1/admin/subscriptions', { status: subStatus || undefined, page, size: 15 }),
    [subStatus, page],
  );
  const { data: payments, loading: payLoading, error: payError, reload: reloadPays } = useFetch(
    () => get('/api/v1/admin/payments', {
      status: payStatus || undefined, userId: userId || undefined, page, size: 15,
    }),
    [payStatus, userId, page],
  );

  return (
    <div className="space-y-6">
      <PageTitle title="Billing"
                 subtitle="Every Razorpay order mirrored locally, and every subscription it activated." />

      {stats && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Stat label="Total captured" value={compact(stats.totalRevenue)} sub={money(stats.totalRevenue)} />
          <Stat label="Last 30 days" value={compact(stats.revenueLast30Days)} />
          <Stat label="Active subscriptions" value={stats.activeSubscriptions}
                sub={`${compact(stats.recurringRevenue)} recurring`} />
          <Stat label="Failed payments" value={stats.failedPayments}
                sub={`${stats.successfulPayments} successful`} />
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        {Object.keys(stats?.revenueByMonth ?? {}).length > 0 && (
          <ChartBox title="Revenue by month" subtitle="Captured payments over twelve months">
            <LineChart data={stats.revenueByMonth} label="Revenue" />
          </ChartBox>
        )}
        {Object.keys(stats?.subscriptionsByPlan ?? {}).length > 0 && (
          <ChartBox title="Active subscriptions by plan" subtitle="Where paying users sit">
            <BarChart data={stats.subscriptionsByPlan} label="Subscribers" />
          </ChartBox>
        )}
      </div>

      <div className="border-b border-slate-200">
        <nav className="-mb-px flex gap-1">
          {[['payments', 'Payments'], ['subscriptions', 'Subscriptions']].map(([id, label]) => (
            <button key={id} type="button" onClick={() => { setTab(id); setPage(0); }}
                    className={`border-b-2 px-4 py-2 text-sm font-medium ${
                      tab === id ? 'border-brand text-brand'
                                 : 'border-transparent text-slate-500 hover:text-slate-700'}`}>
              {label}
            </button>
          ))}
        </nav>
      </div>

      {/* ---------- payments ---------- */}
      {tab === 'payments' && (
        <>
          <div className="card">
            <div className="grid gap-4 sm:grid-cols-3">
              <div>
                <label className="label" htmlFor="pay-status">Status</label>
                <select id="pay-status" value={payStatus} className="input"
                        onChange={(e) => { setPayStatus(e.target.value); setPage(0); }}>
                  <option value="">All statuses</option>
                  {PAY_STATUSES.map((s) => <option key={s} value={s}>{title(s)}</option>)}
                </select>
              </div>
              <div>
                <label className="label" htmlFor="user-id">User ID</label>
                <input id="user-id" type="number" min="1" value={userId} className="input" placeholder="e.g. 2"
                       onChange={(e) => { setUserId(e.target.value); setPage(0); }} />
              </div>
              <div className="flex items-end">
                <button type="button" className="btn-secondary w-full" disabled={!payStatus && !userId}
                        onClick={() => { setPayStatus(''); setUserId(''); setPage(0); }}>Clear</button>
              </div>
            </div>
          </div>

          {payLoading ? <Loading label="Loading payments…" />
            : payError ? <ErrorBox error={payError} onRetry={reloadPays} />
            : !payments?.content?.length ? <Empty title="No payments match those filters" />
            : (
              <>
                <Table headers={['Date', 'User', 'Order ID', 'Invoice', 'Method', 'Amount', 'Status']}>
                  {payments.content.map((payment) => (
                    <tr key={payment.id}>
                      <td className="text-xs">{date(payment.createdAt, true)}</td>
                      <td className="text-xs">
                        <p className="font-medium">{payment.userEmail ?? `user ${payment.userId}`}</p>
                        <p className="text-slate-400">{payment.planName ?? '—'}</p>
                      </td>
                      <td className="max-w-[140px] truncate font-mono text-xs text-slate-500"
                          title={payment.orderId}>{payment.orderId}</td>
                      <td className="font-mono text-xs">{payment.invoiceNo ?? '—'}</td>
                      <td className="text-xs uppercase">{payment.method ?? '—'}</td>
                      <td className="font-semibold">{money(payment.amount)}</td>
                      <td>
                        <Badge status={payment.status} />
                        {payment.failureReason && (
                          <p className="mt-0.5 max-w-[150px] truncate text-xs text-red-600"
                             title={payment.failureReason}>{payment.failureReason}</p>
                        )}
                      </td>
                    </tr>
                  ))}
                </Table>
                <Pagination page={payments.pageNumber} totalPages={payments.totalPages} onChange={setPage} />
              </>
            )}
        </>
      )}

      {/* ---------- subscriptions ---------- */}
      {tab === 'subscriptions' && (
        <>
          <div className="card">
            <label className="label" htmlFor="sub-status">Filter by status</label>
            <select id="sub-status" value={subStatus} className="input sm:max-w-xs"
                    onChange={(e) => { setSubStatus(e.target.value); setPage(0); }}>
              <option value="">All statuses</option>
              {SUB_STATUSES.map((s) => <option key={s} value={s}>{title(s)}</option>)}
            </select>
          </div>

          {subLoading ? <Loading label="Loading subscriptions…" />
            : subError ? <ErrorBox error={subError} onRetry={reloadSubs} />
            : !subscriptions?.content?.length ? <Empty title="No subscriptions match that filter" />
            : (
              <>
                <Table headers={['ID', 'User', 'Plan', 'Price', 'Started', 'Ends', 'Remaining', 'Status']}>
                  {subscriptions.content.map((subscription) => (
                    <tr key={subscription.id}>
                      <td className="font-mono text-xs text-slate-500">#{subscription.id}</td>
                      <td className="font-mono text-xs">user {subscription.userId}</td>
                      <td>
                        <p className="font-medium text-slate-800">{subscription.plan.name}</p>
                        <p className="text-xs text-slate-500">{subscription.plan.tier}</p>
                      </td>
                      <td className="font-semibold">{money(subscription.plan.price)}</td>
                      <td className="text-xs">{date(subscription.startDate)}</td>
                      <td className="text-xs">{date(subscription.endDate)}</td>
                      <td className="text-xs">
                        {subscription.status === 'ACTIVE' ? (
                          <span className={subscription.expiringSoon ? 'font-semibold text-amber-600' : ''}>
                            {subscription.daysRemaining} days
                          </span>
                        ) : '—'}
                      </td>
                      <td><Badge status={subscription.status} /></td>
                    </tr>
                  ))}
                </Table>
                <Pagination page={subscriptions.pageNumber} totalPages={subscriptions.totalPages}
                            onChange={setPage} />
              </>
            )}
        </>
      )}
    </div>
  );
}
