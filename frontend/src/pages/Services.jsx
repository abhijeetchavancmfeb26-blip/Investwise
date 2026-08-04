import { Link } from 'react-router-dom';
import { PageTitle } from '../components/Ui';

const SERVICES = [
  {
    title: 'Financial goal management', tier: 'Free',
    body: 'Define what you are saving for, the amount and the date. The platform inflates the target '
        + 'to future rupees, computes the contribution required, and tracks you against a linear glide '
        + 'path so "behind schedule" is a fact rather than a feeling.',
    points: ['Eleven goal types, each with its own inflation assumption',
             'Required-contribution maths that accounts for expected returns',
             'Automatic status: on track, behind, achieved',
             'Priority ordering so the urgent goal surfaces first'],
  },
  {
    title: 'Risk assessment', tier: 'Free',
    body: 'A seven-factor questionnaire that separates risk capacity from risk tolerance and returns a '
        + 'profile, a strategic asset allocation and a per-factor breakdown showing exactly which '
        + 'answer moved your score.',
    points: ['Age, horizon, savings capacity and dependents score capacity',
             'Drawdown reaction and market knowledge score tolerance',
             'Emergency fund and health cover score the safety net',
             'Four profile bands from Conservative to Aggressive'],
  },
  {
    title: 'Personalised recommendations', tier: 'Free',
    body: 'The engine scores every eligible product on risk alignment, horizon fit, goal affinity, '
        + 'quality and cost, then allocates your money across asset classes according to your profile.',
    points: ['Weighted, readable scoring model with published weights',
             'Bounded selection: at most three products per asset class',
             'Allocation that sums to exactly your investable amount',
             'A written rationale attached to every suggestion'],
  },
  {
    title: 'Portfolio tracking', tier: 'Free',
    body: 'Record what you hold and see absolute return, annualised return, allocation by asset class '
        + 'and risk band, and which positions have crossed into long-term tax treatment.',
    points: ['Holding-level and portfolio-level ROI', 'Nightly mark to market',
             'Goal linkage so investments count towards their target',
             'Full transaction ledger with references'],
  },
  {
    title: 'Premium analytics', tier: 'Premium',
    body: 'Concentration and diversification scoring, projections at five and ten years, gain '
        + 'classification, a rebalancing plan, and observations that name the specific holding worth reviewing.',
    points: ['Herfindahl-based diversification score out of 100',
             'Best and worst performer identification',
             'Ordered rebalancing actions, sells before buys',
             'Actionable observations, not just more charts'],
  },
  {
    title: 'Reports', tier: 'Free & Premium',
    body: 'Portfolio summaries, goal progress, transaction statements and recommendation sheets as PDF '
        + 'or CSV. Premium adds the analytics pack and a capital gains statement.',
    points: ['Branded, print-ready PDFs', 'CSV exports for your own analysis',
             'Six report types', 'Every download recorded in your activity log'],
  },
];

export default function Services() {
  return (
    <div className="page">
      <PageTitle title="Services"
                 subtitle="Each capability, what tier it sits in, and the mechanics behind it." />

      <div className="space-y-5">
        {SERVICES.map((service) => (
          <article key={service.title} className="card">
            <div className="grid gap-5 lg:grid-cols-3">
              <div>
                <h2 className="font-bold text-slate-900">{service.title}</h2>
                <span className={`badge mt-2 ${
                  service.tier === 'Premium' ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-800'
                }`}>{service.tier}</span>
              </div>
              <div className="lg:col-span-2">
                <p className="leading-relaxed text-slate-600">{service.body}</p>
                <ul className="mt-3 grid gap-1.5 sm:grid-cols-2">
                  {service.points.map((point) => (
                    <li key={point} className="text-sm text-slate-600">• {point}</li>
                  ))}
                </ul>
              </div>
            </div>
          </article>
        ))}
      </div>

      <div className="card mt-8 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="font-bold text-slate-900">Try the calculators without signing up</h2>
          <p className="mt-1 text-sm text-slate-600">
            SIP, lumpsum and goal calculators use exactly the same arithmetic as the planning engine.
          </p>
        </div>
        <Link to="/" className="btn-primary">Open the calculator</Link>
      </div>
    </div>
  );
}
