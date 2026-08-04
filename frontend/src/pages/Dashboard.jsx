import { Link } from 'react-router-dom';
import { get } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { useAuth } from '../lib/auth';
import { Badge, Empty, ErrorBox, Loading, PageTitle, Progress, Stat, Table } from '../components/Ui';
import { ChartBox, DoughnutChart } from '../components/Charts';
import { compact, date, gainClass, money, percent } from '../lib/format';

export default function Dashboard() {
  const { user, isPremium } = useAuth();
  const { data, loading, error, reload } = useFetch(() => get('/api/v1/portfolio/dashboard'), []);

  if (loading) return <Loading label="Loading your dashboard…" />;
  if (error) return <ErrorBox error={error} onRetry={reload} />;
  if (!data) return null;

  const hasPortfolio = Number(data.totalInvested ?? 0) > 0;
  const goalProgress = Number(data.totalGoalTarget) > 0
    ? (Number(data.totalGoalProgress) / Number(data.totalGoalTarget)) * 100 : 0;

  return (
    <div className="space-y-6">
      <PageTitle title={`Welcome back, ${user?.firstName ?? 'investor'}`}
                 subtitle="Where your money is, how your goals are tracking, and what needs attention next."
                 action={<Link to="/recommendations" className="btn-primary">Get recommendations</Link>} />

      {/* ---------- setup prompts ---------- */}
      {!data.riskAssessmentComplete && (
        <div className="card flex flex-wrap items-center justify-between gap-4 border-l-4 border-l-amber-400">
          <div>
            <p className="font-semibold text-slate-900">Complete your risk assessment</p>
            <p className="mt-0.5 text-sm text-slate-600">
              Seven questions. Without it we cannot produce recommendations tailored to what you can
              actually tolerate.
            </p>
          </div>
          <Link to="/risk" className="btn-primary">Start the questionnaire</Link>
        </div>
      )}

      {data.totalGoals === 0 && (
        <div className="card flex flex-wrap items-center justify-between gap-4 border-l-4 border-l-teal-400">
          <div>
            <p className="font-semibold text-slate-900">Set your first financial goal</p>
            <p className="mt-0.5 text-sm text-slate-600">
              Name what you are saving for and by when. We inflate the target and work out the
              monthly contribution.
            </p>
          </div>
          <Link to="/goals" className="btn-primary">Create a goal</Link>
        </div>
      )}

      {/* ---------- headline figures ---------- */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Total invested" value={compact(data.totalInvested)} sub={money(data.totalInvested)} />
        <Stat label="Current value" value={compact(data.currentValue)}
              sub={hasPortfolio
                ? `${Number(data.gain) >= 0 ? 'Up' : 'Down'} ${compact(Math.abs(Number(data.gain)))}`
                : 'No holdings recorded yet'} />
        <Stat label="Return" value={percent(data.gainPct)} sub="Absolute, since first purchase" />
        <Stat label="Risk profile" to="/risk"
              value={data.riskProfile ? data.riskProfile.replace(/_/g, ' ') : 'Not assessed'}
              sub={data.riskScore != null ? `Score ${data.riskScore} of 100` : 'Complete the questionnaire'} />
      </div>

      {/* ---------- goals ---------- */}
      <div className="grid gap-6 lg:grid-cols-3">
        <div className="card lg:col-span-2">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-semibold text-slate-900">Goals needing attention</h2>
            <Link to="/goals" className="text-sm font-medium text-brand hover:underline">
              View all {data.totalGoals}
            </Link>
          </div>

          {data.upcomingGoals?.length ? (
            <div className="space-y-4">
              {data.upcomingGoals.map((goal) => (
                <div key={goal.id} className="rounded border border-slate-200 p-4">
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div>
                      <p className="font-semibold text-slate-900">{goal.title}</p>
                      <p className="mt-0.5 text-xs text-slate-500">
                        {goal.goalTypeLabel} · target {date(goal.targetDate)} · {goal.monthsRemaining} months left
                      </p>
                    </div>
                    <Badge status={goal.status} />
                  </div>

                  <div className="mt-3">
                    <div className="mb-1 flex justify-between text-xs">
                      <span className="text-slate-600">
                        {compact(goal.currentAmount)} of {compact(goal.targetAmount)}
                      </span>
                      <span className="font-semibold">{percent(goal.progressPct, 1)}</span>
                    </div>
                    <Progress value={goal.progressPct} />
                  </div>

                  <p className="mt-3 text-xs text-slate-500">
                    Needs <strong>{money(goal.requiredMonthly)}</strong> per month from here.
                    Currently contributing <strong>{money(goal.monthlyContribution)}</strong>.
                  </p>
                </div>
              ))}
            </div>
          ) : (
            <Empty title="No active goals"
                   description="Goals give the recommendation engine something concrete to optimise for."
                   action={<Link to="/goals" className="btn-primary">Create your first goal</Link>} />
          )}
        </div>

        <div className="space-y-6">
          <div className="card">
            <h2 className="font-semibold text-slate-900">Goal progress overall</h2>
            <dl className="mt-4 space-y-2 text-sm">
              <div className="flex justify-between">
                <dt className="text-slate-600">On track</dt>
                <dd className="font-bold text-emerald-600">{data.goalsOnTrack}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-slate-600">Behind schedule</dt>
                <dd className="font-bold text-amber-600">{data.goalsBehind}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-slate-600">Achieved</dt>
                <dd className="font-bold text-brand">{data.goalsAchieved}</dd>
              </div>
            </dl>

            <div className="mt-4 border-t border-slate-200 pt-4">
              <div className="mb-1 flex justify-between text-xs">
                <span className="text-slate-600">Total saved towards goals</span>
                <span className="font-semibold">{percent(goalProgress, 1)}</span>
              </div>
              <Progress value={goalProgress} />
              <p className="mt-2 text-xs text-slate-500">
                {compact(data.totalGoalProgress)} of {compact(data.totalGoalTarget)}
              </p>
            </div>
          </div>

          {isPremium ? (
            <div className="card">
              <h2 className="font-semibold text-slate-900">{data.tier} active</h2>
              <p className="mt-2 text-sm text-slate-600">
                {data.subscriptionDaysRemaining} days remaining on your plan.
              </p>
              <Link to="/analytics" className="btn-primary mt-4 w-full">Open premium analytics</Link>
            </div>
          ) : (
            <div className="card">
              <h2 className="font-semibold text-slate-900">Unlock deeper analysis</h2>
              <p className="mt-2 text-sm text-slate-600">
                Diversification and concentration scoring, a rebalancing plan, tax classification and
                unlimited goals.
              </p>
              <Link to="/pricing" className="btn-primary mt-4 w-full">See Premium plans</Link>
            </div>
          )}
        </div>
      </div>

      {/* ---------- allocation and holdings ---------- */}
      {hasPortfolio && (
        <div className="grid gap-6 lg:grid-cols-2">
          <ChartBox title="Allocation by category" subtitle="Share of current portfolio value">
            <DoughnutChart data={data.allocationByCategory} />
          </ChartBox>

          <div className="card">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="font-semibold text-slate-900">Largest holdings</h2>
              <Link to="/portfolio" className="text-sm font-medium text-brand hover:underline">View all</Link>
            </div>
            <div className="space-y-3">
              {data.topHoldings?.map((holding) => (
                <div key={holding.id} className="flex items-center justify-between gap-3 border-b border-slate-100 pb-3 last:border-0">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-slate-900">{holding.productName}</p>
                    <p className="text-xs text-slate-500">{holding.category}</p>
                  </div>
                  <div className="shrink-0 text-right">
                    <p className="text-sm font-bold">{compact(holding.currentValue)}</p>
                    <p className={`text-xs font-semibold ${gainClass(holding.gainPct)}`}>
                      {percent(holding.gainPct)}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ---------- recent transactions ---------- */}
      {data.recentTransactions?.length > 0 && (
        <div>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="font-semibold text-slate-900">Recent transactions</h2>
            <Link to="/transactions" className="text-sm font-medium text-brand hover:underline">Full ledger</Link>
          </div>
          <Table headers={['Date', 'Product', 'Type', 'Units', 'Amount']}>
            {data.recentTransactions.map((tx) => (
              <tr key={tx.id}>
                <td>{date(tx.createdAt)}</td>
                <td className="font-medium text-slate-800">{tx.productName ?? '—'}</td>
                <td><Badge status={tx.type === 'BUY' ? 'ACTIVE' : 'PENDING'}>{tx.type}</Badge></td>
                <td>{Number(tx.units).toFixed(2)}</td>
                <td className="font-semibold">{money(tx.amount)}</td>
              </tr>
            ))}
          </Table>
        </div>
      )}
    </div>
  );
}
