import { useState } from 'react';
import { Link } from 'react-router-dom';
import { get } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { ChartBox, LineChart } from '../components/Charts';
import { Loading } from '../components/Ui';
import { compact, money, percent } from '../lib/format';

const FEATURES = [
  ['Goal-based planning', 'Targets are inflated to future rupees, because a ₹25 lakh education fund needed in 2038 will not cost ₹25 lakh in 2038.'],
  ['Transparent risk profiling', 'Seven scored factors separate what you can afford to lose from what you are willing to lose. You see which answer moved your score.'],
  ['Explained recommendations', 'Products are scored on risk fit, horizon, goal affinity, quality and cost. Every suggestion arrives with its reasoning.'],
  ['Portfolio tracking', 'Absolute and annualised returns, allocation by asset class and risk band, and long versus short term gain classification.'],
  ['Premium analytics', 'Concentration and diversification scoring, projections and a rebalancing plan that names specific holdings.'],
  ['Reports you can keep', 'Portfolio, goal, transaction, recommendation and capital gains statements as PDF or CSV, generated on demand.'],
];

const STEPS = [
  ['Create your account', 'Register and confirm your email. Two minutes, no payment details.'],
  ['Set a goal', 'Name what you are saving for, the amount and the date. We handle the inflation maths.'],
  ['Complete the questionnaire', 'Seven questions produce a profile and a strategic asset allocation.'],
  ['Get your allocation', 'The engine scores the catalogue and divides your money across asset classes.'],
  ['Track and adjust', 'Record holdings, watch progress against the glide path, rebalance annually.'],
];

export default function Home() {
  const [amount, setAmount] = useState(10000);
  const [years, setYears] = useState(15);
  const [rate, setRate] = useState(12);

  const { data: projection, loading } = useFetch(
    () => get('/api/v1/calculators/sip', { monthlyAmount: amount, annualRate: rate, years }),
    [amount, years, rate],
  );
  const { data: featured } = useFetch(() => get('/api/v1/products/featured'), []);

  return (
    <>
      {/* ---------- hero ---------- */}
      <section className="border-b border-slate-200 bg-white">
        <div className="page grid items-start gap-10 lg:grid-cols-2">
          <div>
            <h1 className="text-4xl font-bold leading-tight text-slate-900">
              The plan comes before the product.
            </h1>
            <p className="mt-5 text-lg leading-relaxed text-slate-600">
              Most platforms sell you a fund and hope you work out the rest. InvestWise starts with
              what you are saving for and how much volatility you can genuinely tolerate, then builds
              the allocation around those two answers.
            </p>
            <div className="mt-6 flex flex-wrap gap-3">
              <Link to="/register" className="btn-primary">Start planning free</Link>
              <Link to="/plans" className="btn-secondary">Browse products</Link>
            </div>
          </div>

          {/* ---------- live calculator ---------- */}
          <div className="card">
            <h2 className="font-semibold text-slate-900">What could a monthly SIP become?</h2>
            <p className="mt-1 text-sm text-slate-500">
              Computed server side with the same maths the planning engine uses.
            </p>

            <div className="mt-5 space-y-4">
              {[
                { id: 'amount', label: 'Monthly investment', value: amount, set: setAmount,
                  min: 1000, max: 100000, step: 1000, display: money(amount) },
                { id: 'years', label: 'Duration', value: years, set: setYears,
                  min: 1, max: 30, step: 1, display: `${years} years` },
                { id: 'rate', label: 'Expected annual return', value: rate, set: setRate,
                  min: 6, max: 20, step: 0.5, display: `${rate}%` },
              ].map((slider) => (
                <div key={slider.id}>
                  <div className="flex justify-between text-sm">
                    <label htmlFor={slider.id} className="text-slate-700">{slider.label}</label>
                    <span className="font-semibold text-brand">{slider.display}</span>
                  </div>
                  <input id={slider.id} type="range" className="mt-1 w-full accent-teal-700"
                         min={slider.min} max={slider.max} step={slider.step}
                         value={slider.value}
                         onChange={(e) => slider.set(Number(e.target.value))} />
                </div>
              ))}
            </div>

            {loading && !projection ? (
              <Loading label="Calculating…" />
            ) : projection ? (
              <div className="mt-5 grid grid-cols-3 gap-3 rounded bg-slate-50 p-4 text-center">
                <div>
                  <p className="text-xs text-slate-500">Invested</p>
                  <p className="mt-1 text-sm font-semibold">{compact(projection.totalInvested)}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-500">Returns</p>
                  <p className="mt-1 text-sm font-semibold text-emerald-600">
                    {compact(projection.estimatedReturns)}
                  </p>
                </div>
                <div>
                  <p className="text-xs text-slate-500">Maturity</p>
                  <p className="mt-1 text-sm font-semibold text-brand">
                    {compact(projection.maturityValue)}
                  </p>
                </div>
              </div>
            ) : (
              <p className="mt-5 rounded bg-amber-50 p-3 text-xs text-amber-800">
                The calculator needs the gateway running on port 8080.
              </p>
            )}

            <p className="mt-3 text-xs text-slate-400">
              Illustration only. Returns are not guaranteed.
            </p>
          </div>
        </div>
      </section>

      {/* ---------- projection ---------- */}
      {projection?.yearlyProjection && (
        <section className="page">
          <ChartBox title="How that corpus builds up"
                    subtitle={`${money(amount)} per month at ${rate}% for ${years} years`}
                    height="h-72">
            <LineChart data={projection.yearlyProjection} label="Projected corpus" />
          </ChartBox>
          <p className="mt-3 text-center text-sm text-slate-500">
            Notice the curve steepening. The final third contributes more than the first two combined,
            which is why starting earlier matters more than investing more.
          </p>
        </section>
      )}

      {/* ---------- features ---------- */}
      <section className="border-y border-slate-200 bg-white">
        <div className="page">
          <h2 className="text-2xl font-bold text-slate-900">Planning tools, not product pitches</h2>
          <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {FEATURES.map(([title, body]) => (
              <div key={title} className="card">
                <h3 className="font-semibold text-slate-900">{title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-slate-600">{body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ---------- how it works ---------- */}
      <section className="page">
        <h2 className="text-2xl font-bold text-slate-900">Five steps, then discipline</h2>
        <ol className="mt-8 grid gap-5 md:grid-cols-3 lg:grid-cols-5">
          {STEPS.map(([title, body], index) => (
            <li key={title} className="card">
              <span className="text-xl font-bold text-teal-300">{String(index + 1).padStart(2, '0')}</span>
              <h3 className="mt-1 font-semibold text-slate-900">{title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-slate-600">{body}</p>
            </li>
          ))}
        </ol>
      </section>

      {/* ---------- featured products ---------- */}
      {featured?.length > 0 && (
        <section className="border-t border-slate-200 bg-white">
          <div className="page">
            <div className="mb-6 flex items-end justify-between">
              <h2 className="text-2xl font-bold text-slate-900">Top rated products</h2>
              <Link to="/plans" className="text-sm font-medium text-brand hover:underline">
                See the full catalogue
              </Link>
            </div>

            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {featured.map((product) => (
                <article key={product.id} className="card">
                  <div className="flex items-start justify-between gap-2">
                    <span className="badge bg-teal-100 text-teal-800">{product.categoryLabel}</span>
                    <span className="text-xs text-amber-500">{'★'.repeat(product.rating)}</span>
                  </div>
                  <h3 className="mt-3 font-semibold text-slate-900">{product.name}</h3>
                  <p className="mt-1 text-xs text-slate-500">{product.fundHouse}</p>
                  <p className="mt-2 line-clamp-3 text-sm text-slate-600">{product.description}</p>
                  <dl className="mt-4 grid grid-cols-3 gap-2 border-t border-slate-100 pt-3 text-center">
                    <div>
                      <dt className="text-xs text-slate-400">Expected</dt>
                      <dd className="text-sm font-semibold text-emerald-600">
                        {percent(product.expectedReturn)}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-xs text-slate-400">Minimum</dt>
                      <dd className="text-sm font-semibold">{compact(product.minInvestment)}</dd>
                    </div>
                    <div>
                      <dt className="text-xs text-slate-400">Risk</dt>
                      <dd className="text-xs font-semibold">{product.riskLevel.replace('_', ' ')}</dd>
                    </div>
                  </dl>
                </article>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* ---------- CTA ---------- */}
      <section className="bg-brand py-12 text-white">
        <div className="page text-center">
          <h2 className="text-2xl font-bold">Start with a goal, not a fund</h2>
          <p className="mx-auto mt-3 max-w-xl text-teal-50">
            The free Starter plan covers three goals, risk profiling, the full catalogue and portfolio
            tracking. No payment details required.
          </p>
          <div className="mt-6 flex flex-wrap justify-center gap-3">
            <Link to="/register" className="btn border-white bg-white text-brand hover:bg-teal-50">
              Create a free account
            </Link>
            <Link to="/pricing" className="btn border-white text-white hover:bg-teal-800">
              Compare plans
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
