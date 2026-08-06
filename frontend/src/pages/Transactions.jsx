import { useState } from 'react';
import { download, get, toError } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { Badge, Empty, ErrorBox, Loading, Notice, PageTitle, Pagination, Table } from '../components/Ui';
import { date, money, number } from '../lib/format';

const TYPES = ['BUY', 'REDEEM'];

export default function Transactions() {
  const [type, setType] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);
  const [notice, setNotice] = useState(null);

  const { data, loading, error, reload } = useFetch(
    () => get('/api/v1/portfolio/transactions', {
      type: type || undefined,
      // The API binds ISO date-times; a bare date would fail
      from: from ? `${from}T00:00:00` : undefined,
      to: to ? `${to}T23:59:59` : undefined,
      page, size: 15,
    }),
    [type, from, to, page],
  );

  const clear = () => { setType(''); setFrom(''); setTo(''); setPage(0); };

  const downloadReport = async (format) => {
    try {
      const filename = await download('TRANSACTION_STATEMENT', format);
      setNotice({ type: 'success', text: `Downloaded ${filename}` });
    } catch (err) {
      setNotice({ type: 'error', text: toError(err).message });
    }
  };

  return (
    <div className="space-y-6">
      <PageTitle title="Transaction ledger"
                 subtitle="Every purchase and redemption with its own reference. Entries are immutable once written."
                 action={
                   <div className="flex gap-2">
                     <button type="button" onClick={() => downloadReport('pdf')} className="btn-secondary">PDF</button>
                     <button type="button" onClick={() => downloadReport('csv')} className="btn-secondary">CSV</button>
                   </div>
                 } />

      {notice && <Notice type={notice.type} onDismiss={() => setNotice(null)}>{notice.text}</Notice>}

      <div className="card">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <label className="label" htmlFor="type">Transaction type</label>
            <select id="type" value={type} className="input"
                    onChange={(e) => { setType(e.target.value); setPage(0); }}>
              <option value="">All types</option>
              {TYPES.map((option) => <option key={option} value={option}>{option}</option>)}
            </select>
          </div>
          <div>
            <label className="label" htmlFor="from">From date</label>
            <input id="from" type="date" value={from} className="input"
                   onChange={(e) => { setFrom(e.target.value); setPage(0); }} />
          </div>
          <div>
            <label className="label" htmlFor="to">To date</label>
            <input id="to" type="date" value={to} className="input"
                   onChange={(e) => { setTo(e.target.value); setPage(0); }} />
          </div>
          <div className="flex items-end">
            <button type="button" onClick={clear} className="btn-secondary w-full"
                    disabled={!type && !from && !to}>Clear filters</button>
          </div>
        </div>
      </div>

      {loading ? <Loading label="Loading transactions…" />
        : error ? <ErrorBox error={error} onRetry={reload} />
        : !data?.content?.length ? (
          <Empty title="No transactions found"
                 description={type || from || to
                   ? 'Nothing matches those filters. Try widening the date range.'
                   : 'Transactions appear here automatically once you record a holding or a redemption.'} />
        ) : (
          <>
            <p className="text-sm text-slate-500">
              {data.totalElements} transaction{data.totalElements === 1 ? '' : 's'}
            </p>
            <Table headers={['Date', 'Reference', 'Product', 'Type', 'Units', 'Price', 'Amount']}>
              {data.content.map((tx) => (
                <tr key={tx.id}>
                  <td>{date(tx.createdAt, true)}</td>
                  <td className="font-mono text-xs text-slate-500">{tx.referenceNo}</td>
                  <td className="font-medium text-slate-800">{tx.productName ?? '—'}</td>
                  <td><Badge status={tx.type === 'BUY' ? 'ACTIVE' : 'PENDING'}>{tx.type}</Badge></td>
                  <td>{number(tx.units, 3)}</td>
                  <td>{money(tx.price)}</td>
                  <td className="font-semibold">{money(tx.amount)}</td>
                </tr>
              ))}
            </Table>
            <Pagination page={data.pageNumber} totalPages={data.totalPages} onChange={setPage} />
          </>
        )}
    </div>
  );
}
