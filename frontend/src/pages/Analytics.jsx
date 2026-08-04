import { useState } from 'react';
import { Link } from 'react-router-dom';
import { download, get, toError } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { Empty, ErrorBox, Loading, Notice, PageTitle, Progress, Stat, Table } from '../components/Ui';
import { ChartBox, DoughnutChart } from '../components/Charts';
import { compact, gainClass, money, percent } from '../lib/format';

export default function Analytics() {
  const [notice, setNotice] = useState(null);
  const { data, loading, error, reload } = useFetch(() => get('/api/v1/portfolio/analytics'), []);

  const downloadReport = async (format) => {
    try {
      const filename = await download('PREMIUM_ANALYTICS', format);
      setNotice({ type: 'success', text: `Downloaded ${filename}` });
    } catch (err) {
      setNotice({ type: 'error', text: toError(err).message });
    }
  };

  if (loading) return <Loading label="Crunching your portfolio…" />;

  // A 403 here means the free tier, which deserves an upgrade prompt not an error
  if (error?.status === 403) {
    return (
      <div className="mx-auto max-w-lg">
        <div className="card text-center">
          <h1 className="text-xl font-bold text-slate-900">This is a Premium feature</h1>
          <p className="mt-3 text-sm leading-relaxed text-slate-600">
            Premium unlocks advanced portfolio analytics, concentration and diversification scoring,
            projection modelling, a rebalancing plan and downloadable analytics reports.
          </p>
          <div className="mt-5 flex flex-wrap justify-center gap-3">
            <Link to="/pricing" className="btn-primary">See plans</Link>
            <Link to="/dashboard" className="btn-secondary">Back to dashboard</Link>
          </div>
        </div>
      </div>
    );
  }

  if (error) return <ErrorBox error={error} onRetry={reload} />;
  if (!data) return null;

  if (!Number(data.currentValue)) {
    return (
      <div className="space-y-6">
        <PageTitle title="Portfolio analytics" />
        <Empty title="Add a holding to unlock the analysis"
               description="Diversification, concentration and projection metrics all need at least one
                            recorded position to work from."
               action={<Link to="/portfolio" className="btn-primary">Go to portfolio</Link>} />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageTitle title="Portfolio analytics"
                 subtitle="Concentration, diversification, projections and a rebalancing plan that points
                           at specific positions rather than generalities."
                 action={
                   <div className="flex gap-2">
                     <button type="button" onClick={() => downloadReport('pdf')} className="btn-secondary">PDF</button>
                     <button type="button" onClick={() => downloadReport('csv')} className="btn-secondary">CSV</button>
                   </div>
                 } />

      {notice && <Notice type={notice.type} onDismiss={() => setNotice(null)}>{notice.text}</Notice>}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Current value" value={compact(data.currentValue)}
              sub={`Invested ${compact(data.totalInvested)}`} />
        <Stat label="Absolute return" value={percent(data.returnPct)} sub={compact(data.gain)} />
        <Stat label="Annualised return" value={percent(data.annualisedPct)} sub="Compound annual growth" />
        <Stat label="Diversification" value={`${data.diversificationScore} / 100`}
              sub="Breadth and evenness across asset classes" />
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="card">
          <h2 className="text-sm font-semibold text-slate-800">Concentration</h2>
          <p className="mt-3 text-2xl font-bold text-slate-900">{percent(data.concentrationPct, 1)}</p>
          <p className="mt-1 text-sm text-slate-600">held in <strong>{data.largestHolding}</strong></p>
          <div className="mt-3"><Progress value={data.concentrationPct} /></div>
          <p className="mt-3 text-xs leading-relaxed text-slate-500">
            {Number(data.concentrationPct) > 40
              ? 'Above 40% in one position means your outcome is driven by that fund rather than by your asset allocation.'
              : 'No single position dominates the portfolio, which is where you want to be.'}
          </p>
        </div>

        <div className="card">
          <h2 className="text-sm font-semibold text-slate-800">Best performer</h2>
          <p className="mt-3 truncate font-bold text-slate-900">{data.bestHolding}</p>
          <p className={`mt-1 text-2xl font-bold ${gainClass(data.bestReturnPct)}`}>
            {percent(data.bestReturnPct)}
          </p>
          <p className="mt-3 text-xs leading-relaxed text-slate-500">
            A strong run is not automatically a reason to add more. Check whether it has pushed your
            allocation away from its target.
          </p>
        </div>

        <div className="card">
          <h2 className="text-sm font-semibold text-slate-800">Weakest performer</h2>
          <p className="mt-3 truncate font-bold text-slate-900">{data.worstHolding}</p>
          <p className={`mt-1 text-2xl font-bold ${gainClass(data.worstReturnPct)}`}>
            {percent(data.worstReturnPct)}
          </p>
          <p className="mt-3 text-xs leading-relaxed text-slate-500">
            Check whether the fall reflects the whole asset class or this fund specifically. The first
            is noise; the second is worth investigating.
          </p>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <ChartBox title="Allocation by asset class" subtitle="Share of current value">
          <DoughnutChart data={data.allocationByAssetClass} />
        </ChartBox>
        <ChartBox title="Allocation by risk band" subtitle="Where your volatility is concentrated">
          <DoughnutChart data={data.allocationByRisk} />
        </ChartBox>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="card">
          <h2 className="font-semibold text-slate-900">Forward projection</h2>
          <p className="mt-1 text-sm text-slate-600">
            Assuming your current holdings and a blended expected return of{' '}
            <strong className="text-emerald-600">{percent(data.weightedExpectedReturn)}</strong>.
          </p>
          <dl className="mt-4 space-y-3 text-sm">
            <div className="flex justify-between">
              <dt className="text-slate-600">Today</dt>
              <dd className="font-bold">{compact(data.currentValue)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-600">In 5 years</dt>
              <dd className="font-bold">{compact(data.projected5Years)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-600">In 10 years</dt>
              <dd className="font-bold">{compact(data.projected10Years)}</dd>
            </div>
          </dl>
          <p className="mt-4 text-xs leading-relaxed text-slate-500">
            These assume you hold what you hold today and add nothing further. Illustrations of
            compounding, not forecasts.
          </p>
        </div>

        <div className="card">
          <h2 className="font-semibold text-slate-900">Unrealised gain classification</h2>
          <p className="mt-1 text-sm text-slate-600">How gains would be treated if you redeemed today.</p>
          <dl className="mt-4 space-y-3">
            <div className="rounded border border-emerald-200 bg-emerald-50 p-3">
              <dt className="text-xs font-semibold uppercase text-emerald-700">Long term (over 1 year)</dt>
              <dd className="mt-1 text-xl font-bold text-emerald-800">{money(data.longTermGains)}</dd>
            </div>
            <div className="rounded border border-amber-200 bg-amber-50 p-3">
              <dt className="text-xs font-semibold uppercase text-amber-700">Short term (under 1 year)</dt>
              <dd className="mt-1 text-xl font-bold text-amber-800">{money(data.shortTermGains)}</dd>
            </div>
          </dl>
          <p className="mt-4 text-xs leading-relaxed text-slate-500">
            Tax treatment depends on the instrument, holding period and your circumstances. Download
            the capital gains statement for the position-level breakdown.
          </p>
        </div>
      </div>

      {/* ---------- rebalancing plan ---------- */}
      {data.rebalance && (
        <div className="card">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 className="font-semibold text-slate-900">Rebalancing plan</h2>
              <p className="mt-1 max-w-2xl text-sm text-slate-600">{data.rebalance.summary}</p>
            </div>
            <span className={`badge ${data.rebalance.actionNeeded
              ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-800'}`}>
              {data.rebalance.actionNeeded ? 'Action suggested' : 'Within tolerance'}
            </span>
          </div>

          <div className="mt-4">
            <Table headers={['Asset class', 'Current', 'Target', 'Drift', 'Adjustment', 'Status']}>
              {Object.entries(data.rebalance.driftByAssetClass).map(([assetClass, row]) => (
                <tr key={assetClass}>
                  <td className="font-medium text-slate-900">{assetClass.replace(/_/g, ' ')}</td>
                  <td>
                    {percent(row.currentPct, 1)}
                    <span className="block text-xs text-slate-400">{compact(row.currentAmount)}</span>
                  </td>
                  <td>{percent(row.targetPct, 0)}</td>
                  <td className="font-semibold">
                    {Number(row.driftPct) > 0 ? '+' : ''}{percent(row.driftPct, 1)}
                  </td>
                  <td className={`font-semibold ${
                    Number(row.differenceAmount) >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                    {Number(row.differenceAmount) >= 0 ? 'Add ' : 'Trim '}
                    {compact(Math.abs(Number(row.differenceAmount)))}
                  </td>
                  <td>
                    <span className={`badge ${
                      row.status === 'ON_TARGET' ? 'bg-emerald-100 text-emerald-800'
                        : row.status === 'OVERWEIGHT' ? 'bg-amber-100 text-amber-800'
                        : 'bg-sky-100 text-sky-800'}`}>
                      {row.status.replace('_', ' ').toLowerCase()}
                    </span>
                  </td>
                </tr>
              ))}
            </Table>
          </div>

          {data.rebalance.actions?.length > 0 && (
            <ol className="mt-4 space-y-3 border-t border-slate-200 pt-4">
              {data.rebalance.actions.map((action) => (
                <li key={action.step} className="flex items-start gap-3 rounded bg-slate-50 p-3">
                  <span className={`grid h-6 w-6 shrink-0 place-items-center rounded-full text-xs font-bold ${
                    action.type === 'REDUCE' ? 'bg-amber-200 text-amber-800' : 'bg-emerald-200 text-emerald-800'}`}>
                    {action.step}
                  </span>
                  <div>
                    <p className="text-sm font-semibold text-slate-900">
                      {action.type === 'REDUCE' ? 'Trim' : 'Add'} {money(action.amount)}{' '}
                      {action.type === 'REDUCE' ? 'from' : 'to'}{' '}
                      {action.assetClass.replace(/_/g, ' ').toLowerCase()}
                    </p>
                    <p className="mt-0.5 text-xs leading-relaxed text-slate-600">{action.rationale}</p>
                  </div>
                </li>
              ))}
            </ol>
          )}

          <p className="mt-4 text-xs leading-relaxed text-slate-500">
            Actions are ordered so proceeds exist before they are spent. Check any lock-in periods
            first, and remember a redemption may realise a taxable gain.
          </p>
        </div>
      )}

      {/* ---------- insights ---------- */}
      {data.insights?.length > 0 && (
        <div className="card">
          <h2 className="font-semibold text-slate-900">What we noticed</h2>
          <ul className="mt-4 space-y-3">
            {data.insights.map((insight, index) => (
              <li key={index} className="rounded bg-slate-50 p-3 text-sm leading-relaxed text-slate-700">
                {insight}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
