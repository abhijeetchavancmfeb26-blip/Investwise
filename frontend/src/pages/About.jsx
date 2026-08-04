import { Link } from 'react-router-dom';
import { PageTitle } from '../components/Ui';

const VALUES = [
  ['Show the reasoning', 'Every score, allocation and projection traces back to an input you provided and a rule you can read. A recommendation you cannot interrogate is not advice, it is marketing.'],
  ['Capacity before appetite', 'How much risk you can afford and how much you can tolerate are different questions. We score them separately, because an investor with a large surplus and a nervous disposition should not be handed an aggressive portfolio.'],
  ['Plan first, product second', 'The goal, the horizon and the allocation are decided before a single fund is named. That ordering is the biggest determinant of whether a plan survives a bad year.'],
  ['No hidden incentives', 'InvestWise earns from subscriptions, not from which products you choose. The catalogue is rated on cost, quality and fit, and the ratings are visible.'],
];

export default function About() {
  return (
    <div className="page">
      <PageTitle title="About InvestWise"
                 subtitle="Built because most investing advice starts in the wrong place." />

      <div className="card">
        <p className="leading-relaxed text-slate-600">
          The typical journey begins with a product recommendation and works backwards to justify it.
          That ordering explains a great deal of the poor outcomes retail investors experience: money
          needed in two years placed in equity, emergency funds locked into five year instruments,
          aggressive portfolios sold to people who will redeem at the first 20% drawdown.
        </p>
        <p className="mt-4 leading-relaxed text-slate-600">
          InvestWise inverts it. You state the objective and the date. We assess what you can and will
          tolerate. Only then does the platform score the catalogue and produce an allocation, with the
          reasoning attached to every line.
        </p>
      </div>

      <h2 className="mb-4 mt-8 text-xl font-bold text-slate-900">What we hold to</h2>
      <div className="grid gap-5 md:grid-cols-2">
        {VALUES.map(([title, body]) => (
          <div key={title} className="card">
            <h3 className="font-semibold text-slate-900">{title}</h3>
            <p className="mt-2 text-sm leading-relaxed text-slate-600">{body}</p>
          </div>
        ))}
      </div>

      <h2 className="mb-4 mt-8 text-xl font-bold text-slate-900">How the platform is built</h2>
      <div className="card">
        <p className="leading-relaxed text-slate-600">
          InvestWise runs as three services. An API gateway fronts the platform so the browser talks to
          one origin. Behind it, the User Service owns identity, authentication and profiles, and the
          Investment Service owns goals, risk profiling, the recommendation engine, portfolios,
          subscriptions and payments.
        </p>
        <p className="mt-4 leading-relaxed text-slate-600">
          The two domain services never share a database. They communicate over RabbitMQ for anything
          that can be eventually consistent, so a slow email server or a busy report queue never delays
          a user-facing request. Identity travels in the signed token itself, which is why no
          service-to-service lookup channel is needed at all.
        </p>
      </div>

      <div className="card mt-8 text-center">
        <h2 className="text-xl font-bold text-slate-900">Questions about the methodology?</h2>
        <p className="mt-2 text-slate-600">
          We are happy to walk through how the risk score is computed or how the engine weights its criteria.
        </p>
        <div className="mt-4 flex flex-wrap justify-center gap-3">
          <Link to="/contact" className="btn-primary">Get in touch</Link>
          <Link to="/faq" className="btn-secondary">Read the FAQ</Link>
        </div>
      </div>
    </div>
  );
}
