import { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { recommendSchema } from '../lib/schemas';
import { download, get, post, toError } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { Empty, Loading, Notice, PageTitle, Progress } from '../components/Ui';
import { ChartBox, DoughnutChart } from '../components/Charts';
import { FormError, Input, Select, Submit } from '../components/Form';
import { compact, date, money, percent, title } from '../lib/format';

export default function Recommendations() {
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);

  const { data: risk, loading } = useFetch(() => get('/api/v1/risk/me'), []);
  const { data: goals } = useFetch(() => get('/api/v1/goals/all'), []);
  const { data: latest, loading: latestLoading, reload } = useFetch(
    () => get('/api/v1/recommendations/latest').catch(() => null), []);

  const { register, handleSubmit, formState: { errors, isSubmitting, isValid } } =
    useForm({ resolver: zodResolver(recommendSchema), mode: 'onChange',
              defaultValues: { goalId: '', investableAmount: '', horizonYears: '' } });

  const onSubmit = async (values) => {
    setError(null);
    try {
      const generated = await post('/api/v1/recommendations/generate', {
        goalId: values.goalId === '' ? null : Number(values.goalId),
        investableAmount: values.investableAmount,
        horizonYears: values.horizonYears === '' ? null : Number(values.horizonYears),
      });
      setResult(generated);
      setNotice({ type: 'success', text: `${generated.items.length} recommendations generated.` });
      reload();
    } catch (err) {
      setError(err.status ? err : toError(err));
    }
  };

  const downloadSheet = async (format) => {
    try {
      const filename = await download('RECOMMENDATION_SHEET', format);
      setNotice({ type: 'success', text: `Downloaded ${filename}` });
    } catch (err) {
      setNotice({ type: 'error', text: toError(err).message });
    }
  };

  if (loading || latestLoading) return <Loading />;

  const active = result ?? latest;

  return (
    <div className="space-y-6">
      <PageTitle title="Personalised recommendations"
                 subtitle="The engine scores every eligible product on risk alignment, horizon fit, goal
                           affinity, quality and cost, then allocates your money across asset classes." />

      {notice && <Notice type={notice.type} onDismiss={() => setNotice(null)}>{notice.text}</Notice>}

      {!risk ? (
        <Navigate to="/risk" replace />
      ) : (
        <>
          <div className="card">
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="font-semibold text-slate-900">Generate a new basket</h2>
                <p className="mt-0.5 text-sm text-slate-600">
                  Profiled as <strong>{title(risk.profile)}</strong>, targeting {risk.equityPct}% equity,
                  {' '}{risk.debtPct}% debt and {risk.goldPct}% gold.
                </p>
              </div>
              <Link to="/risk" className="text-sm font-medium text-brand hover:underline">Review my profile</Link>
            </div>

            <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
              <FormError error={error} />

              <div className="grid gap-4 sm:grid-cols-3">
                <Input label="Amount to invest" required type="number" min="500" step="1000"
                       placeholder="200000" hint="Lump sum available now"
                       error={errors.investableAmount} {...register('investableAmount')} />
                <Select label="Towards a specific goal" placeholder="Overall portfolio"
                        options={goals?.map((g) => ({
                          value: String(g.id), label: `${g.title} (${g.monthsRemaining} months)`,
                        })) ?? []}
                        error={errors.goalId} {...register('goalId')} />
                <Input label="Override horizon (years)" type="number" min="1" max="40"
                       placeholder={String(risk.horizonYears ?? 10)} hint="Leave blank to use your profile"
                       error={errors.horizonYears} {...register('horizonYears')} />
              </div>

              <Submit loading={isSubmitting} disabled={!isValid}>Generate recommendations</Submit>
            </form>
          </div>

          {active?.items?.length ? (
            <>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <div className="card">
                  <p className="text-xs uppercase text-slate-500">Investable</p>
                  <p className="mt-2 text-xl font-bold">{compact(active.investableAmount)}</p>
                </div>
                <div className="card">
                  <p className="text-xs uppercase text-slate-500">Blended expected return</p>
                  <p className="mt-2 text-xl font-bold text-emerald-600">{percent(active.expectedReturn)}</p>
                </div>
                <div className="card">
                  <p className="text-xs uppercase text-slate-500">Projected value</p>
                  <p className="mt-2 text-xl font-bold text-brand">{compact(active.projectedValue)}</p>
                  <p className="mt-0.5 text-xs text-slate-500">in {active.horizonYears ?? '—'} years</p>
                </div>
                <div className="card">
                  <p className="text-xs uppercase text-slate-500">Products</p>
                  <p className="mt-2 text-xl font-bold">{active.items.length}</p>
                  <p className="mt-0.5 text-xs text-slate-500">
                    across {Object.keys(active.allocationByAssetClass ?? {}).length} asset classes
                  </p>
                </div>
              </div>

              <div className="grid gap-6 lg:grid-cols-[320px_1fr]">
                <ChartBox title="Allocation by asset class"
                          subtitle={active.goalTitle ? `For "${active.goalTitle}"` : 'Overall portfolio'}>
                  <DoughnutChart data={active.allocationByAssetClass} />
                </ChartBox>

                <div className="card">
                  <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <h2 className="font-semibold text-slate-900">Suggested basket</h2>
                      <p className="mt-0.5 text-xs text-slate-500">
                        Generated {date(active.createdAt, true)}
                      </p>
                    </div>
                    <div className="flex gap-2">
                      <button type="button" onClick={() => downloadSheet('pdf')} className="btn-secondary">PDF</button>
                      <button type="button" onClick={() => downloadSheet('csv')} className="btn-secondary">CSV</button>
                    </div>
                  </div>

                  <div className="divide-y divide-slate-100">
                    {active.items.map((item) => (
                      <article key={item.productId} className="py-4 first:pt-0 last:pb-0">
                        <div className="flex flex-wrap items-start justify-between gap-3">
                          <div className="min-w-0 flex-1">
                            <h3 className="font-semibold text-slate-900">{item.productName}</h3>
                            <p className="mt-0.5 text-xs text-slate-500">
                              {item.category} · {item.riskLevel.replace('_', ' ')} · {item.productCode}
                              {item.lockInMonths > 0 && ` · ${item.lockInMonths} month lock-in`}
                            </p>
                          </div>
                          <div className="shrink-0 text-right">
                            <p className="font-bold text-slate-900">{money(item.amount)}</p>
                            <p className="text-xs font-semibold text-brand">
                              {percent(item.allocationPct, 1)} of your amount
                            </p>
                          </div>
                        </div>

                        <div className="mt-3 grid gap-3 sm:grid-cols-3">
                          <div>
                            <p className="mb-1 text-xs text-slate-400">Match score</p>
                            <div className="flex items-center gap-2">
                              <Progress value={item.matchScore} />
                              <span className="text-xs font-bold">{Number(item.matchScore).toFixed(0)}%</span>
                            </div>
                          </div>
                          <div>
                            <p className="text-xs text-slate-400">Expected return</p>
                            <p className="text-sm font-bold text-emerald-600">{percent(item.expectedReturn)}</p>
                          </div>
                          <div>
                            <p className="text-xs text-slate-400">Minimum ticket</p>
                            <p className="text-sm font-bold">{money(item.minInvestment)}</p>
                          </div>
                        </div>

                        {item.rationale && (
                          <p className="mt-3 rounded bg-slate-50 p-3 text-xs leading-relaxed text-slate-600">
                            {item.rationale}
                          </p>
                        )}
                      </article>
                    ))}
                  </div>
                </div>
              </div>

              <div className="card flex flex-wrap items-center justify-between gap-4">
                <p className="max-w-2xl text-sm text-slate-600">
                  Once you have acted on any of these, record the purchase in your portfolio so
                  progress and returns are tracked. The platform does not place orders on your behalf.
                </p>
                <Link to="/portfolio" className="btn-primary">Record a purchase</Link>
              </div>

              <p className="text-xs leading-relaxed text-slate-500">
                Generated from the profile and inputs you supplied, for information only. Not
                investment advice. Expected returns are long-run category estimates, not guarantees.
              </p>
            </>
          ) : (
            <Empty title="No recommendations generated yet"
                   description="Enter an amount above and the engine will score the catalogue against
                                your risk profile and horizon." />
          )}
        </>
      )}
    </div>
  );
}
