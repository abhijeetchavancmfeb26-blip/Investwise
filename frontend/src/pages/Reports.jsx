import { useState } from 'react';
import { Link } from 'react-router-dom';
import { download, get, toError } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { useAuth } from '../lib/auth';
import { Loading, Notice, PageTitle } from '../components/Ui';

const DESCRIPTIONS = {
  PORTFOLIO_SUMMARY: 'Total invested, current value, absolute and annualised return, plus a full holdings table with per-position gains.',
  GOAL_PROGRESS: 'Each goal with its target, amount saved, progress, target date and the monthly contribution required from here.',
  TRANSACTION_STATEMENT: 'Your full ledger of purchases and redemptions with references, for reconciliation against your broker statement.',
  RECOMMENDATION_SHEET: 'Your current suggested basket with allocations, match scores and the written rationale behind each product.',
  PREMIUM_ANALYTICS: 'Diversification and concentration scoring, best and worst performers, and the observations from your analytics page.',
  TAX_STATEMENT: 'Unrealised gains split into long and short term with holding periods, so you can plan a tax-efficient redemption.',
};

export default function Reports() {
  const { isPremium } = useAuth();
  const [busy, setBusy] = useState(null);
  const [notice, setNotice] = useState(null);

  const { data: meta, loading } = useFetch(() => get('/api/v1/metadata'), []);

  const run = async (type, format) => {
    setBusy(`${type}-${format}`);
    try {
      const filename = await download(type, format);
      setNotice({ type: 'success', text: `Downloaded ${filename}` });
    } catch (err) {
      setNotice({ type: 'error', text: toError(err).message });
    } finally {
      setBusy(null);
    }
  };

  if (loading) return <Loading />;

  return (
    <div className="space-y-6">
      <PageTitle title="Reports"
                 subtitle="Generated on demand from your live data. PDFs are print-ready; CSVs are for
                           your own spreadsheet analysis." />

      {notice && <Notice type={notice.type} onDismiss={() => setNotice(null)}>{notice.text}</Notice>}

      {!isPremium && (
        <div className="card flex flex-wrap items-center justify-between gap-4 border-l-4 border-l-amber-400">
          <div>
            <p className="font-semibold text-slate-900">Two reports need Premium</p>
            <p className="mt-0.5 text-sm text-slate-600">
              The analytics pack and the capital gains statement are part of the paid tier.
            </p>
          </div>
          <Link to="/pricing" className="btn-primary">See plans</Link>
        </div>
      )}

      <div className="grid gap-5 md:grid-cols-2">
        {meta?.reportTypes?.map((report) => {
          const locked = report.premiumOnly && !isPremium;
          return (
            <article key={report.value} className={`card flex flex-col ${locked ? 'opacity-70' : ''}`}>
              <div className="flex items-start justify-between gap-2">
                <h2 className="font-semibold text-slate-900">{report.label}</h2>
                {report.premiumOnly && <span className="badge bg-amber-100 text-amber-800">Premium</span>}
              </div>
              <p className="mt-2 flex-1 text-sm leading-relaxed text-slate-600">
                {DESCRIPTIONS[report.value]}
              </p>

              <div className="mt-4 flex gap-2 border-t border-slate-100 pt-3">
                {locked ? (
                  <Link to="/pricing" className="btn-secondary w-full">Upgrade to unlock</Link>
                ) : (
                  <>
                    <button type="button" onClick={() => run(report.value, 'pdf')}
                            disabled={busy !== null} className="btn-primary flex-1">
                      {busy === `${report.value}-pdf` ? 'Generating…' : 'Download PDF'}
                    </button>
                    <button type="button" onClick={() => run(report.value, 'csv')}
                            disabled={busy !== null} className="btn-secondary flex-1">
                      {busy === `${report.value}-csv` ? 'Generating…' : 'CSV'}
                    </button>
                  </>
                )}
              </div>
            </article>
          );
        })}
      </div>

      <div className="card bg-slate-100">
        <p className="text-xs leading-relaxed text-slate-600">
          <strong>A note on report contents.</strong> Every figure derives from data you have entered.
          Projected values assume the stated rate of return and are illustrations, not guarantees.
          Capital gains figures are unrealised and become taxable only on redemption; tax treatment
          depends on the instrument, the holding period and your circumstances. Consult a qualified
          tax adviser before acting on the statement.
        </p>
      </div>
    </div>
  );
}
