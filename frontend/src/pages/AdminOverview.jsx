import { Link } from 'react-router-dom';
import { get } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { Badge, ErrorBox, Loading, PageTitle, Stat, Table } from '../components/Ui';
import { BarChart, ChartBox, DoughnutChart, LineChart } from '../components/Charts';
import { compact, date, money, percent, title } from '../lib/format';

export default function AdminOverview() {
  const { data: users, loading: usersLoading, error: usersError, reload: reloadUsers } =
    useFetch(() => get('/api/v1/admin/user-stats'), []);
  const { data: invest, loading: investLoading, error: investError, reload: reloadInvest } =
    useFetch(() => get('/api/v1/admin/stats'), []);

  if (usersLoading || investLoading) return <Loading label="Loading platform statistics…" />;
  if (usersError) return <ErrorBox error={usersError} onRetry={reloadUsers} />;
  if (investError) return <ErrorBox error={investError} onRetry={reloadInvest} />;

  const conversion = users.totalUsers ? (users.premiumUsers / users.totalUsers) * 100 : 0;
  const attempted = invest.successfulPayments + invest.failedPayments;
  const successRate = attempted > 0 ? (invest.successfulPayments / attempted) * 100 : 0;

  return (
    <div className="space-y-6">
      <PageTitle title="Platform overview"
                 subtitle="Aggregate figures across both domain services." />

      <section>
        <h2 className="mb-3 text-sm font-bold uppercase text-slate-500">Users</h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Stat label="Total users" value={users.totalUsers} to="/admin/users"
                sub={`${users.activeUsers} active`} />
          <Stat label="New this week" value={users.newUsersLast7Days}
                sub={`${users.newUsersLast30Days} in the last 30 days`} />
          <Stat label="Verified" value={users.verifiedUsers}
                sub={`${users.pendingVerification} still pending`} />
          <Stat label="Premium conversion" value={percent(conversion, 1)}
                sub={`${users.premiumUsers} of ${users.totalUsers} on a paid plan`} />
        </div>
      </section>

      <section>
        <h2 className="mb-3 text-sm font-bold uppercase text-slate-500">Revenue</h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Stat label="Total revenue" value={compact(invest.totalRevenue)} to="/admin/billing"
                sub={money(invest.totalRevenue)} />
          <Stat label="Last 30 days" value={compact(invest.revenueLast30Days)} />
          <Stat label="Recurring revenue" value={compact(invest.recurringRevenue)}
                sub={`${invest.activeSubscriptions} active subscriptions`} />
          <Stat label="Payment success rate" value={percent(successRate, 1)}
                sub={`${invest.failedPayments} failed of ${attempted}`} />
        </div>
      </section>

      <section>
        <h2 className="mb-3 text-sm font-bold uppercase text-slate-500">Platform activity</h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Stat label="Assets tracked" value={compact(invest.platformInvested)}
                sub={`Now worth ${compact(invest.platformCurrentValue)}`} />
          <Stat label="Goals created" value={invest.totalGoals} sub={`${invest.goalsAchieved} achieved`} />
          <Stat label="Recommendations (30d)" value={invest.recommendationsLast30Days} />
          <Stat label="Open enquiries" value={users.openContactMessages} to="/admin/messages"
                sub={`${users.loginsLast24Hours} sign-ins in 24h`} />
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        {Object.keys(users.registrationsByMonth ?? {}).length > 0 && (
          <ChartBox title="Registrations by month" subtitle="Last twelve months">
            <LineChart data={users.registrationsByMonth} label="New users" currency={false} />
          </ChartBox>
        )}
        {Object.keys(invest.revenueByMonth ?? {}).length > 0 && (
          <ChartBox title="Revenue by month" subtitle="Captured payments only">
            <LineChart data={invest.revenueByMonth} label="Revenue" />
          </ChartBox>
        )}
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <ChartBox title="Users by status" subtitle="Current distribution">
          <DoughnutChart data={users.usersByStatus} suffix=" users" />
        </ChartBox>
        <ChartBox title="Products by category" subtitle="Active catalogue">
          <DoughnutChart data={invest.productsByCategory} suffix=" products" />
        </ChartBox>
        <ChartBox title="Investors by risk profile" subtitle="Latest assessment per user">
          <DoughnutChart data={invest.usersByRiskProfile} suffix=" investors" />
        </ChartBox>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="font-semibold text-slate-900">Newest registrations</h2>
            <Link to="/admin/users" className="text-sm font-medium text-brand hover:underline">All users</Link>
          </div>
          {users.recentRegistrations?.length ? (
            <Table headers={['Name', 'Email', 'Status', 'Joined']}>
              {users.recentRegistrations.map((user) => (
                <tr key={user.id}>
                  <td className="font-medium text-slate-800">{user.fullName}</td>
                  <td className="max-w-[180px] truncate text-xs">{user.email}</td>
                  <td><Badge status={user.status} /></td>
                  <td className="text-xs">{date(user.createdAt)}</td>
                </tr>
              ))}
            </Table>
          ) : <p className="card text-center text-sm text-slate-400">No registrations yet.</p>}
        </div>

        <div>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="font-semibold text-slate-900">Latest successful payments</h2>
            <Link to="/admin/billing" className="text-sm font-medium text-brand hover:underline">All payments</Link>
          </div>
          {invest.recentPayments?.length ? (
            <Table headers={['Invoice', 'Plan', 'Amount', 'Date']}>
              {invest.recentPayments.map((payment) => (
                <tr key={payment.id}>
                  <td className="font-mono text-xs">{payment.invoiceNo ?? '—'}</td>
                  <td className="text-xs font-medium">{payment.planName ?? '—'}</td>
                  <td className="font-semibold">{money(payment.amount)}</td>
                  <td className="text-xs">{date(payment.createdAt)}</td>
                </tr>
              ))}
            </Table>
          ) : <p className="card text-center text-sm text-slate-400">No payments captured yet.</p>}
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {Object.keys(invest.goalsByType ?? {}).length > 0 && (
          <ChartBox title="Goals by type" subtitle="What people are actually saving for">
            <BarChart data={invest.goalsByType} label="Goals" horizontal />
          </ChartBox>
        )}

        <div className="card">
          <h2 className="font-semibold text-slate-900">Catalogue health</h2>
          <dl className="mt-4 grid grid-cols-2 gap-4 text-sm">
            <div>
              <dt className="text-xs uppercase text-slate-400">Total products</dt>
              <dd className="mt-1 text-lg font-bold">{invest.totalProducts}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase text-slate-400">Active</dt>
              <dd className="mt-1 text-lg font-bold text-emerald-600">{invest.activeProducts}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase text-slate-400">Premium only</dt>
              <dd className="mt-1 text-lg font-bold text-amber-600">{invest.premiumProducts}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase text-slate-400">Average expected return</dt>
              <dd className="mt-1 text-lg font-bold">{percent(invest.averageExpectedReturn)}</dd>
            </div>
          </dl>

          {invest.mostRecommended?.length > 0 && (
            <div className="mt-4 border-t border-slate-200 pt-3">
              <p className="mb-2 text-xs font-semibold uppercase text-slate-400">Most recommended</p>
              <ol className="space-y-1">
                {invest.mostRecommended.map((entry, index) => (
                  <li key={entry} className="text-sm text-slate-700">{index + 1}. {entry}</li>
                ))}
              </ol>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
