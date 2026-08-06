import { Link } from 'react-router-dom';
import { get } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { useAuth } from '../lib/auth';
import { ErrorBox, Loading, PageTitle } from '../components/Ui';
import { money } from '../lib/format';

const COMPARISON = [
  ['Financial goals', 'Up to 3', 'Unlimited', 'Unlimited'],
  ['Risk assessment', '✓', '✓', '✓'],
  ['Product catalogue', 'Standard', 'Standard + premium', 'Full access'],
  ['Recommendation engine', '✓', '✓', '✓'],
  ['Portfolio tracking', '✓', '✓', '✓'],
  ['Basic reports (PDF & CSV)', '✓', '✓', '✓'],
  ['Advanced analytics', '—', '✓', '✓'],
  ['Diversification & concentration scoring', '—', '✓', '✓'],
  ['Rebalancing plan', '—', '✓', '✓'],
  ['Capital gains statement', '—', '✓', '✓'],
  ['Priority support', '—', '✓', '✓'],
  ['Dedicated advisor', '—', '—', '✓'],
];

export default function Pricing() {
  const { data: plans, loading, error, reload } = useFetch(() => get('/api/v1/plans'), []);
  const { isAuthenticated, user } = useAuth();

  return (
    <div className="page">
      <PageTitle title="Start free. Upgrade when the analytics earn it."
                 subtitle="The free tier is a complete planning tool, not a trial: three goals, full risk
                           profiling, the recommendation engine and portfolio tracking." />

      {loading ? <Loading label="Loading plans…" />
        : error ? <ErrorBox error={error} onRetry={reload} />
        : (
          <>
            <div className="grid gap-5 lg:grid-cols-4">
              {plans?.map((plan) => {
                const current = isAuthenticated && user?.tier === plan.tier;
                const highlighted = plan.code === 'PREMIUM_Y';

                return (
                  <div key={plan.code} className={`card flex flex-col ${highlighted ? 'border-brand border-2' : ''}`}>
                    {highlighted && (
                      <p className="mb-2 text-xs font-bold uppercase text-brand">Most popular</p>
                    )}
                    <h2 className="font-bold text-slate-900">{plan.name}</h2>
                    <p className="mt-1 min-h-[40px] text-sm text-slate-600">{plan.description}</p>

                    <p className="mt-4">
                      <span className="text-2xl font-bold text-slate-900">
                        {plan.price > 0 ? money(plan.price) : 'Free'}
                      </span>
                      {plan.price > 0 && (
                        <span className="text-sm text-slate-500">
                          {plan.durationMonths === 1 ? ' / month' : ` / ${plan.durationMonths} months`}
                        </span>
                      )}
                    </p>
                    {plan.price > 0 && plan.durationMonths > 1 && (
                      <p className="mt-1 text-xs text-emerald-600">
                        Works out to {money(plan.monthlyEquivalent)} per month
                      </p>
                    )}

                    <ul className="mt-4 flex-1 space-y-2">
                      {plan.features.map((feature) => (
                        <li key={feature} className="text-sm text-slate-600">✓ {feature}</li>
                      ))}
                    </ul>

                    <div className="mt-5">
                      {current ? (
                        <span className="btn-secondary w-full cursor-default">Your current plan</span>
                      ) : plan.price === 0 ? (
                        <Link to={isAuthenticated ? '/dashboard' : '/register'} className="btn-secondary w-full">
                          {isAuthenticated ? 'Go to dashboard' : 'Start free'}
                        </Link>
                      ) : (
                        <Link to={isAuthenticated ? `/checkout/${plan.code}` : '/register'}
                              className={highlighted ? 'btn-primary w-full' : 'btn-secondary w-full'}>
                          {isAuthenticated ? 'Choose this plan' : 'Sign up to subscribe'}
                        </Link>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>

            <h2 className="mb-4 mt-10 text-xl font-bold text-slate-900">Feature comparison</h2>
            <div className="overflow-x-auto rounded border border-slate-200 bg-white">
              <table className="table">
                <thead>
                  <tr>
                    <th className="min-w-[220px]">Feature</th>
                    <th className="text-center">Starter</th>
                    <th className="text-center">Premium</th>
                    <th className="text-center">Elite</th>
                  </tr>
                </thead>
                <tbody>
                  {COMPARISON.map(([feature, free, premium, elite]) => (
                    <tr key={feature}>
                      <td className="font-medium text-slate-800">{feature}</td>
                      <td className="text-center">{free}</td>
                      <td className="text-center">{premium}</td>
                      <td className="text-center">{elite}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="mt-8 grid gap-5 md:grid-cols-3">
              {[
                ['Can I cancel at any time?', 'Yes. Cancelling keeps your premium access until the end of the term you have already paid for.'],
                ['What happens if I downgrade?', 'Nothing is deleted. Goals beyond the free limit become read-only rather than disappearing.'],
                ['Is payment secure?', 'Payments run through Razorpay. Card details never reach our servers, and every signature is verified server side.'],
              ].map(([question, answer]) => (
                <div key={question} className="card">
                  <h3 className="font-semibold text-slate-900">{question}</h3>
                  <p className="mt-2 text-sm text-slate-600">{answer}</p>
                </div>
              ))}
            </div>
          </>
        )}
    </div>
  );
}
