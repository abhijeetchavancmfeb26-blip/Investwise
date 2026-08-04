import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { holdingSchema, redeemSchema } from '../lib/schemas';
import { del, download, get, post, toError } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { Badge, Empty, ErrorBox, Loading, Modal, Notice, PageTitle, Stat, Table } from '../components/Ui';
import { ChartBox, DoughnutChart } from '../components/Charts';
import { FormError, Input, Select, Submit } from '../components/Form';
import { compact, date, gainClass, money, number, percent } from '../lib/format';

const EMPTY_HOLDING = {
  productId: '', goalId: '', amount: '', buyPrice: '',
  purchaseDate: new Date().toISOString().slice(0, 10),
};

export default function Portfolio() {
  const [addOpen, setAddOpen] = useState(false);
  const [redeeming, setRedeeming] = useState(null);
  const [removing, setRemoving] = useState(null);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);

  const { data: portfolio, loading, error: loadError, reload } = useFetch(() => get('/api/v1/portfolio'), []);
  const { data: products } = useFetch(() => get('/api/v1/products', { size: 100 }), []);
  const { data: goals } = useFetch(() => get('/api/v1/goals/all'), []);

  const holdingForm = useForm({ resolver: zodResolver(holdingSchema), mode: 'onChange',
                                defaultValues: EMPTY_HOLDING });
  const redeemForm = useForm({ resolver: zodResolver(redeemSchema), mode: 'onChange',
                               defaultValues: { units: '' } });

  const submitHolding = async (values) => {
    setError(null);
    try {
      await post('/api/v1/portfolio/holdings', {
        productId: Number(values.productId),
        goalId: values.goalId === '' ? null : Number(values.goalId),
        amount: values.amount, buyPrice: values.buyPrice, purchaseDate: values.purchaseDate,
      });
      setNotice({ type: 'success', text: 'Holding added to your portfolio.' });
      setAddOpen(false);
      holdingForm.reset(EMPTY_HOLDING);
      reload();
    } catch (err) {
      setError(err.status ? err : toError(err));
    }
  };

  const submitRedeem = async (values) => {
    setError(null);
    try {
      await post(`/api/v1/portfolio/holdings/${redeeming.id}/redeem`, values);
      setNotice({ type: 'success', text: 'Redemption recorded.' });
      setRedeeming(null);
      redeemForm.reset();
      reload();
    } catch (err) {
      setError(err.status ? err : toError(err));
    }
  };

  const confirmRemove = async () => {
    try {
      await del(`/api/v1/portfolio/holdings/${removing.id}`);
      setNotice({ type: 'success', text: 'Holding removed.' });
      setRemoving(null);
      reload();
    } catch (err) {
      setNotice({ type: 'error', text: toError(err).message });
      setRemoving(null);
    }
  };

  const downloadReport = async (format) => {
    try {
      const filename = await download('PORTFOLIO_SUMMARY', format);
      setNotice({ type: 'success', text: `Downloaded ${filename}` });
    } catch (err) {
      setNotice({ type: 'error', text: toError(err).message });
    }
  };

  if (loading) return <Loading label="Loading your portfolio…" />;
  if (loadError) return <ErrorBox error={loadError} onRetry={reload} />;
  if (!portfolio) return null;

  const hasHoldings = portfolio.holdings?.length > 0;

  return (
    <div className="space-y-6">
      <PageTitle title="My portfolio"
                 subtitle="Record what you have invested and the platform computes returns, allocation
                           and tax classification for you."
                 action={
                   <div className="flex flex-wrap gap-2">
                     {hasHoldings && (
                       <>
                         <button type="button" onClick={() => downloadReport('pdf')} className="btn-secondary">PDF</button>
                         <button type="button" onClick={() => downloadReport('csv')} className="btn-secondary">CSV</button>
                       </>
                     )}
                     <button type="button" onClick={() => { setAddOpen(true); setError(null); }}
                             className="btn-primary">Add holding</button>
                   </div>
                 } />

      {notice && <Notice type={notice.type} onDismiss={() => setNotice(null)}>{notice.text}</Notice>}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Total invested" value={compact(portfolio.totalInvested)} sub={money(portfolio.totalInvested)} />
        <Stat label="Current value" value={compact(portfolio.currentValue)}
              sub={`${portfolio.holdingCount} active holdings`} />
        <Stat label="Absolute gain" value={compact(portfolio.gain)} sub={percent(portfolio.gainPct)} />
        <Stat label="Annualised return" value={percent(portfolio.annualisedPct)}
              sub="Since your first purchase" />
      </div>

      {!hasHoldings ? (
        <Empty title="No holdings recorded yet"
               description="Add what you already own, or act on a recommendation and record the purchase
                            here. Returns and allocation follow automatically."
               action={<button type="button" onClick={() => setAddOpen(true)} className="btn-primary">
                         Add your first holding</button>} />
      ) : (
        <>
          <div className="grid gap-6 lg:grid-cols-2">
            <ChartBox title="Allocation by category" subtitle="Share of current value">
              <DoughnutChart data={portfolio.allocationByCategory} />
            </ChartBox>
            <ChartBox title="Allocation by risk band" subtitle="Where your volatility sits">
              <DoughnutChart data={portfolio.allocationByRisk} />
            </ChartBox>
          </div>

          <div>
            <h2 className="mb-3 font-semibold text-slate-900">Holdings</h2>
            <Table headers={['Product', 'Goal', 'Units', 'Invested', 'Current', 'Return', 'Held', 'Actions']}>
              {portfolio.holdings.map((holding) => (
                <tr key={holding.id}>
                  <td>
                    <p className="font-medium text-slate-900">{holding.productName}</p>
                    <p className="text-xs text-slate-500">
                      {holding.category} · {holding.riskLevel.replace('_', ' ')}
                    </p>
                  </td>
                  <td className="text-xs">
                    {holding.goalTitle
                      ? <span className="badge bg-teal-100 text-teal-800">{holding.goalTitle}</span>
                      : <span className="text-slate-400">Unlinked</span>}
                  </td>
                  <td>{number(holding.units, 3)}</td>
                  <td>{money(holding.investedAmount)}</td>
                  <td className="font-semibold">{money(holding.currentValue)}</td>
                  <td>
                    <p className={`font-semibold ${gainClass(holding.gainPct)}`}>{percent(holding.gainPct)}</p>
                    <p className={`text-xs ${gainClass(holding.gain)}`}>{compact(holding.gain)}</p>
                  </td>
                  <td>
                    <p className="text-xs text-slate-600">{date(holding.purchaseDate)}</p>
                    <Badge status={holding.longTerm ? 'ACHIEVED' : 'PENDING'}>
                      {holding.longTerm ? 'Long term' : 'Short term'}
                    </Badge>
                  </td>
                  <td>
                    <div className="flex gap-1">
                      <button type="button" className="btn-secondary px-2 py-1 text-xs"
                              onClick={() => { setRedeeming(holding); setError(null); redeemForm.reset(); }}>
                        Redeem
                      </button>
                      <button type="button" onClick={() => setRemoving(holding)}
                              className="btn-secondary px-2 py-1 text-xs text-red-600">Remove</button>
                    </div>
                  </td>
                </tr>
              ))}
            </Table>
          </div>
        </>
      )}

      {/* ---------- add holding ---------- */}
      <Modal open={addOpen} onClose={() => setAddOpen(false)} title="Record a purchase"
             footer={<>
               <button type="button" onClick={() => setAddOpen(false)} className="btn-secondary">Cancel</button>
               <Submit form="holding-form" loading={holdingForm.formState.isSubmitting}
                       disabled={!holdingForm.formState.isValid}>Add holding</Submit>
             </>}>
        <form id="holding-form" onSubmit={holdingForm.handleSubmit(submitHolding)} noValidate className="space-y-4">
          <FormError error={error} />

          <Select label="Product" required placeholder="Choose the product you bought"
                  options={products?.content?.map((p) => ({
                    value: String(p.id), label: `${p.name} (min ${money(p.minInvestment)})`,
                  })) ?? []}
                  error={holdingForm.formState.errors.productId} {...holdingForm.register('productId')} />

          <Select label="Link to a goal" placeholder="Not linked to a goal"
                  options={goals?.map((g) => ({ value: String(g.id), label: g.title })) ?? []}
                  hint="Linking means this investment counts towards that goal's progress"
                  error={holdingForm.formState.errors.goalId} {...holdingForm.register('goalId')} />

          <div className="grid gap-4 sm:grid-cols-2">
            <Input label="Amount invested" required type="number" min="100" step="500" placeholder="50000"
                   error={holdingForm.formState.errors.amount} {...holdingForm.register('amount')} />
            <Input label="Price per unit / NAV" required type="number" min="0.0001" step="0.0001"
                   placeholder="125.4500" hint="Units are derived from amount ÷ price"
                   error={holdingForm.formState.errors.buyPrice} {...holdingForm.register('buyPrice')} />
          </div>

          <Input label="Purchase date" required type="date" max={new Date().toISOString().slice(0, 10)}
                 error={holdingForm.formState.errors.purchaseDate} {...holdingForm.register('purchaseDate')} />
        </form>
      </Modal>

      {/* ---------- redeem ---------- */}
      <Modal open={Boolean(redeeming)} onClose={() => setRedeeming(null)}
             title={`Redeem ${redeeming?.productName ?? ''}`}
             footer={<>
               <button type="button" onClick={() => setRedeeming(null)} className="btn-secondary">Cancel</button>
               <Submit form="redeem-form" loading={redeemForm.formState.isSubmitting}
                       disabled={!redeemForm.formState.isValid}>Confirm redemption</Submit>
             </>}>
        <form id="redeem-form" onSubmit={redeemForm.handleSubmit(submitRedeem)} noValidate className="space-y-4">
          <FormError error={error} />

          {redeeming && (
            <div className="rounded bg-slate-50 p-3 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-600">Units held</span>
                <span className="font-semibold">{number(redeeming.units, 4)}</span>
              </div>
              <div className="mt-1 flex justify-between">
                <span className="text-slate-600">Current price</span>
                <span className="font-semibold">{money(redeeming.currentPrice)}</span>
              </div>
              <div className="mt-1 flex justify-between">
                <span className="text-slate-600">Gain classification</span>
                <span className="font-semibold">{redeeming.longTerm ? 'Long term' : 'Short term'}</span>
              </div>
            </div>
          )}

          <Input label="Units to redeem" required type="number" min="0.0001" step="0.0001" autoFocus
                 max={redeeming?.units} placeholder={redeeming ? String(redeeming.units) : ''}
                 error={redeemForm.formState.errors.units} {...redeemForm.register('units')} />

          {redeeming && !redeeming.longTerm && (
            <p className="rounded bg-amber-50 p-3 text-xs leading-relaxed text-amber-800">
              This holding is {redeeming.holdingDays} days old. Waiting until it crosses 365 days
              would move the gain into the long-term tax bracket.
            </p>
          )}
        </form>
      </Modal>

      {/* ---------- remove ---------- */}
      <Modal open={Boolean(removing)} onClose={() => setRemoving(null)} title="Remove this holding?"
             footer={<>
               <button type="button" onClick={() => setRemoving(null)} className="btn-secondary">Cancel</button>
               <button type="button" onClick={confirmRemove} className="btn-danger">Remove</button>
             </>}>
        <p className="text-sm leading-relaxed text-slate-600">
          This deletes the position entirely. Use it to correct a data-entry mistake. If you actually
          sold the units, record a redemption instead so the transaction history stays accurate.
        </p>
      </Modal>
    </div>
  );
}
