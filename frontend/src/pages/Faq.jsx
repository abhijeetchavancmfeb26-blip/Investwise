import { useState } from 'react';
import { Link } from 'react-router-dom';
import { PageTitle } from '../components/Ui';

const GROUPS = [
  ['Getting started', [
    ['Do I need to pay to use InvestWise?', 'No. The free Starter plan includes three financial goals, the full risk assessment, the complete standard catalogue, the recommendation engine and portfolio tracking. Premium adds analytical depth rather than unlocking basic functionality.'],
    ['Does InvestWise execute trades or hold my money?', 'No. InvestWise is a planning and tracking platform. You record what you have invested and we analyse it. Purchases are made through your own broker, AMC or bank. We never take custody of funds.'],
    ['Why do you ask for my PAN?', 'PAN is optional. When supplied it is validated for structure and stored so reports are usable for your own tax records. It is masked everywhere it is displayed back to you, showing only the last four characters.'],
    ['What happens if I never verify my email?', 'The account stays PENDING and cannot sign in. Verification links are valid for 24 hours and you can request a fresh one from the sign-in screen.'],
  ]],
  ['Risk profiling and recommendations', [
    ['How is my risk profile calculated?', 'Seven factors are scored and summed to 100: age, investment horizon, savings capacity, dependents, your stated reaction to a 20% drawdown, market knowledge, and whether you have an emergency fund and health cover. The first four measure capacity, the next two tolerance, the last your safety net.'],
    ['Why did the engine recommend something above my risk band?', 'Products outside your ceiling are penalised heavily but not excluded, because a marginally more aggressive fund can still improve a long-horizon portfolio at the edges. The rationale says explicitly when this has happened.'],
    ['How are the recommendation weights decided?', 'Risk alignment carries 35%, horizon fit 25%, goal affinity 20%, and product quality and cost 20%. Risk and horizon dominate because getting either wrong causes real harm, whereas a slightly costlier fund does not.'],
    ['Why was a product excluded entirely?', 'A lock-in longer than your horizon is a hard exclusion, not a penalty. Money trapped past the goal date cannot fund the goal, however attractive the product otherwise looks.'],
  ]],
  ['Goals and portfolio', [
    ['Why is my goal target higher than the number I entered?', 'Because we inflate it. A ₹25 lakh education fund needed in 2038 will cost considerably more than ₹25 lakh in 2038 rupees. Each goal type carries its own inflation assumption, with education at 9% and general expenses at 6%.'],
    ['What does "behind schedule" mean?', 'Your goal is compared against a linear glide path from its creation date to its target date. If you should have accumulated 40% by now and you are at 25%, it is flagged as behind. It is a prompt to review the contribution, not a judgement.'],
    ['How are current values calculated?', 'A nightly job marks each holding forward from its purchase price using the product\'s expected annual return, pro-rated for the holding period, with a small random deviation. In a live deployment this would read a NAV feed.'],
    ['Why can I not redeem a holding?', 'Products with a lock-in cannot be redeemed before it expires, and the platform enforces that. PPF, ELSS and certain fixed deposits all carry lock-ins, shown on the product card before you invest.'],
  ]],
  ['Payments and subscriptions', [
    ['Is my payment information safe?', 'Card and UPI details are handled entirely by Razorpay and never reach our servers. We store the order id, payment id and signature so a disputed transaction can be re-verified. Every signature is validated server side before premium access is granted.'],
    ['What happens if a payment fails midway?', 'The subscription remains PENDING and no premium access is granted. If money was deducted without a verified signature, Razorpay reverses it automatically within the standard settlement window.'],
    ['Can I cancel and get a refund?', 'You can cancel at any time, which keeps your access until the end of the term you have already paid for. We do not pro-rate refunds for the unused portion.'],
  ]],
  ['Data and security', [
    ['How are my passwords stored?', 'Hashed with BCrypt at a work factor of 12. Plain text passwords are never written to the database, never logged, and never returned by any API.'],
    ['What happens after repeated failed logins?', 'The account locks after five consecutive failures and you are emailed about it. Resetting your password unlocks it.'],
    ['Can I see what has happened on my account?', 'Yes. Every sign-in, profile change, goal edit, recommendation run and report download is recorded in your activity log, visible from your profile page.'],
    ['How long does my session last?', 'Eight hours, or thirty days if you tick "keep me signed in". Tokens are held in browser session storage, so closing the tab ends the session either way.'],
  ]],
];

function Item({ question, answer, open, onToggle }) {
  return (
    <div className="border-b border-slate-200 last:border-0">
      <button type="button" onClick={onToggle} aria-expanded={open}
              className="flex w-full items-start justify-between gap-4 py-3 text-left">
        <span className="text-sm font-medium text-slate-800">{question}</span>
        <span className="shrink-0 text-slate-400">{open ? '−' : '+'}</span>
      </button>
      {open && <p className="pb-3 pr-6 text-sm leading-relaxed text-slate-600">{answer}</p>}
    </div>
  );
}

export default function Faq() {
  const [openKey, setOpenKey] = useState('0-0');

  return (
    <div className="page">
      <PageTitle title="Frequently asked questions"
                 subtitle="Straight answers about how the platform works and what it does not do." />

      <div className="space-y-6">
        {GROUPS.map(([group, items], groupIndex) => (
          <section key={group}>
            <h2 className="mb-2 font-bold text-slate-900">{group}</h2>
            <div className="card py-1">
              {items.map(([question, answer], itemIndex) => {
                const key = `${groupIndex}-${itemIndex}`;
                return (
                  <Item key={key} question={question} answer={answer}
                        open={openKey === key}
                        onToggle={() => setOpenKey(openKey === key ? null : key)} />
                );
              })}
            </div>
          </section>
        ))}
      </div>

      <div className="card mt-8 text-center">
        <h2 className="font-bold text-slate-900">Still stuck?</h2>
        <p className="mt-2 text-sm text-slate-600">We respond within one business day.</p>
        <Link to="/contact" className="btn-primary mt-4">Contact support</Link>
      </div>
    </div>
  );
}
