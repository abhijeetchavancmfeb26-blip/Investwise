import { Link, useParams } from 'react-router-dom';
import { PageTitle } from '../components/Ui';

/**
 * Both legal documents in one component, selected by the route param.
 * The original had three files: two pages and a shared renderer.
 */
const DOCUMENTS = {
  privacy: {
    title: 'Privacy Policy',
    updated: '28 July 2026',
    intro: 'What InvestWise collects, why each item is necessary, how long it is kept and what we '
         + 'will never do with it. Written to be read rather than skimmed past.',
    sections: [
      ['Information we collect', [
        'We collect only what the platform needs to function: identity details you supply at '
        + 'registration, financial details you supply while planning, and technical details your '
        + 'browser sends with every request.',
      ], [
        'Identity: name, email, mobile number, date of birth, gender and optionally your PAN',
        'Financial: annual income, occupation, monthly surplus, dependents, goals, holdings and transactions',
        'Technical: IP address and timestamps of sign-ins and account changes',
        'Payment: Razorpay order and payment identifiers. Card numbers, CVV and UPI credentials never reach our servers',
      ]],
      ['How we use it', [
        'Your financial details drive the risk assessment and the recommendation engine. Without your '
        + 'horizon, surplus and dependents there is no way to produce an allocation reflecting your '
        + 'actual capacity for risk, which is the entire purpose of the platform.',
      ], [
        'Producing your risk profile, recommendations, projections and reports',
        'Sending transactional email: verification, password reset and account alerts',
        'Detecting and responding to suspicious account activity',
      ]],
      ['What we never do', [
        'We do not sell your personal information. We do not share it with product manufacturers, and '
        + 'no third party pays to have a product ranked more favourably. Our revenue comes from '
        + 'subscriptions, which is what allows the catalogue ratings to be honest.',
      ], []],
      ['How long we keep it', [
        'Retention differs by data type because purposes differ.',
      ], [
        'Account and planning data: for as long as your account is open',
        'Activity logs: one year, applied automatically by a database expiry index',
        'Notifications: 90 days',
        'Payment records: the statutory period applicable to financial records',
      ]],
      ['Security measures', [
        'Passwords are hashed with BCrypt at a work factor of 12 and never stored, logged or returned '
        + 'in plain text. Sessions use signed JSON Web Tokens held in browser session storage, so '
        + 'closing the tab ends the session.',
        'PAN is masked wherever it is displayed back to you, showing only the final four characters.',
      ], []],
      ['Your rights', [
        'You can view and correct your profile at any time, and your full activity history is available there too.',
      ], [
        'Access: request a copy of the personal information we hold',
        'Correction: update your profile directly',
        'Deletion: request account closure',
        'Objection: ask us to stop sending non-transactional communications',
      ]],
      ['Cookies and third parties', [
        'We use no advertising or tracking cookies and load no third-party analytics. Razorpay '
        + 'processes payments and receives only what a transaction needs; our SMTP provider handles '
        + 'outbound email. No other third party receives your personal information.',
      ], []],
    ],
  },
  terms: {
    title: 'Terms & Conditions',
    updated: '28 July 2026',
    intro: 'The most important point is stated first and deliberately: this is a planning platform, '
         + 'not a broker or an adviser, and the investment decisions remain yours.',
    sections: [
      ['What InvestWise is, and is not', [
        'InvestWise is an information and planning platform. It helps you define goals, assess your '
        + 'risk capacity and tolerance, understand which categories of instrument suit that profile, '
        + 'and track what you have invested.',
        'InvestWise is not a broker, distributor, asset manager or investment adviser. We do not hold '
        + 'your money, do not execute transactions, and receive no commission from any product '
        + 'manufacturer. Purchases happen through your own broker, AMC, bank or the relevant portal.',
      ], []],
      ['Eligibility', [
        'You must be at least 18 and legally capable of entering a contract. The platform is designed '
        + 'for Indian residents; certain products, such as PPF and Sovereign Gold Bonds, are available '
        + 'only to resident individuals.',
      ], []],
      ['Your account', [
        'You are responsible for the accuracy of what you enter. The engine reasons from your stated '
        + 'income, surplus, horizon and dependents; understating any of them produces a plan that does '
        + 'not match your circumstances.',
      ], [
        'Keep your password confidential and do not share your account',
        'Notify us immediately if you suspect unauthorised access',
        'We may suspend an account for suspected fraud, abuse or misuse',
      ]],
      ['Nature of the information provided', [
        'Every projection, expected return and allocation is an illustration based on assumptions '
        + 'stated wherever they are used. Expected returns are long-run category estimates, not '
        + 'forecasts for a period, and they are not guaranteed.',
        'Inflation assumptions, return assumptions and the engine\'s scoring weights are our considered '
        + 'judgements. Reasonable analysts would choose differently, and you may disagree with ours.',
      ], []],
      ['Subscriptions and payment', [
        'Paid plans are billed in advance for the term shown at checkout. Payment is processed by '
        + 'Razorpay and premium access is granted only once the signature has been verified on our servers.',
      ], [
        'Prices are in Indian Rupees and inclusive of applicable taxes unless stated',
        'Cancelling preserves access until the end of the paid term',
        'We do not pro-rate refunds for the unused portion of a term',
      ]],
      ['Acceptable use', [
        'You may use the platform for your own personal financial planning.',
      ], [
        'Do not scrape, republish or resell the catalogue, ratings or research content',
        'Do not attempt to access another user\'s data or bypass authorisation controls',
        'Do not use automated tooling to place load beyond normal individual use',
      ]],
      ['Limitation of liability', [
        'To the maximum extent permitted by law, InvestWise is not liable for investment losses arising '
        + 'from decisions you take, whether or not informed by information on this platform. Market '
        + 'risk is borne by the investor.',
        'Where liability cannot be excluded, it is limited to the subscription fees paid to us in the '
        + 'twelve months preceding the claim.',
      ], []],
      ['Governing law', [
        'These terms are governed by the laws of India. Any dispute is subject to the exclusive '
        + 'jurisdiction of the courts at Pune, Maharashtra.',
      ], []],
    ],
  },
};

export default function Legal() {
  const { document } = useParams();
  const content = DOCUMENTS[document] ?? DOCUMENTS.privacy;

  return (
    <div className="page max-w-3xl">
      <PageTitle title={content.title} />

      <article className="card">
        <p className="text-xs uppercase text-slate-400">Last updated {content.updated}</p>
        <p className="mt-3 leading-relaxed text-slate-600">{content.intro}</p>

        <div className="mt-6 space-y-6">
          {content.sections.map(([heading, paragraphs, bullets], index) => (
            <section key={heading}>
              <h2 className="font-bold text-slate-900">{index + 1}. {heading}</h2>
              <div className="mt-2 space-y-2">
                {paragraphs.map((paragraph, i) => (
                  <p key={i} className="text-sm leading-relaxed text-slate-600">{paragraph}</p>
                ))}
              </div>
              {bullets.length > 0 && (
                <ul className="mt-2 space-y-1">
                  {bullets.map((bullet) => (
                    <li key={bullet} className="text-sm text-slate-600">• {bullet}</li>
                  ))}
                </ul>
              )}
            </section>
          ))}
        </div>

        <p className="mt-6 border-t border-slate-200 pt-4 text-sm text-slate-600">
          Questions? <Link to="/contact" className="font-medium text-brand hover:underline">Contact us</Link>.
        </p>
      </article>
    </div>
  );
}
