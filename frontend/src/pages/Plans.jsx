import { useEffect, useState } from 'react';
import { get } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { Empty, ErrorBox, Loading, PageTitle, Pagination } from '../components/Ui';
import { compact, money, percent } from '../lib/format';

const SORTS = [
  ['expectedReturn:desc', 'Highest expected return'],
  ['expectedReturn:asc', 'Lowest expected return'],
  ['rating:desc', 'Highest rated'],
  ['minInvestment:asc', 'Lowest minimum'],
  ['expenseRatio:asc', 'Lowest cost'],
  ['name:asc', 'Name A-Z'],
];

const RISK_CLASS = {
  VERY_LOW: 'bg-emerald-100 text-emerald-800',
  LOW: 'bg-lime-100 text-lime-800',
  MODERATE: 'bg-amber-100 text-amber-800',
  HIGH: 'bg-orange-100 text-orange-800',
  VERY_HIGH: 'bg-red-100 text-red-800',
};

export default function Plans() {
  const [keyword, setKeyword] = useState('');
  const [debounced, setDebounced] = useState('');
  const [category, setCategory] = useState('');
  const [riskLevel, setRiskLevel] = useState('');
  const [maxAmount, setMaxAmount] = useState('');
  const [sort, setSort] = useState('expectedReturn:desc');
  const [page, setPage] = useState(0);

  // A two-line debounce, replacing the original's dedicated hook
  useEffect(() => {
    const timer = setTimeout(() => { setDebounced(keyword); setPage(0); }, 400);
    return () => clearTimeout(timer);
  }, [keyword]);

  const [sortBy, sortDir] = sort.split(':');
  const { data: meta } = useFetch(() => get('/api/v1/metadata'), []);
  const { data, loading, error, reload } = useFetch(
    () => get('/api/v1/products', {
      keyword: debounced || undefined,
      category: category || undefined,
      riskLevel: riskLevel || undefined,
      maxAmount: maxAmount || undefined,
      page, size: 9, sortBy, sortDir,
    }),
    [debounced, category, riskLevel, maxAmount, page, sortBy, sortDir],
  );

  const clear = () => {
    setKeyword(''); setCategory(''); setRiskLevel(''); setMaxAmount(''); setPage(0);
  };

  return (
    <div className="page">
      <PageTitle title="Investment plans"
                 subtitle="Expected return, risk band, minimum ticket size, lock-in and cost — so you can
                           compare on the numbers rather than the marketing." />

      <div className="card mb-6">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          <div className="lg:col-span-2">
            <label className="label" htmlFor="search">Search</label>
            <input id="search" type="search" value={keyword} className="input"
                   placeholder="Name, description or fund house…"
                   onChange={(e) => setKeyword(e.target.value)} />
          </div>
          <div>
            <label className="label" htmlFor="category">Category</label>
            <select id="category" value={category} className="input"
                    onChange={(e) => { setCategory(e.target.value); setPage(0); }}>
              <option value="">All categories</option>
              {meta?.categories?.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
            </select>
          </div>
          <div>
            <label className="label" htmlFor="risk">Risk level</label>
            <select id="risk" value={riskLevel} className="input"
                    onChange={(e) => { setRiskLevel(e.target.value); setPage(0); }}>
              <option value="">Any risk level</option>
              {meta?.riskLevels?.map((r) => (
                <option key={r.value} value={r.value}>{r.value.replace('_', ' ')}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="label" htmlFor="amount">I can invest up to</label>
            <input id="amount" type="number" min="0" step="1000" value={maxAmount} className="input"
                   placeholder="50000"
                   onChange={(e) => { setMaxAmount(e.target.value); setPage(0); }} />
          </div>
        </div>

        <div className="mt-4 flex flex-wrap items-end justify-between gap-3">
          <div>
            <label className="label" htmlFor="sort">Sort by</label>
            <select id="sort" value={sort} className="input w-auto"
                    onChange={(e) => { setSort(e.target.value); setPage(0); }}>
              {SORTS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </div>
          {(keyword || category || riskLevel || maxAmount) && (
            <button type="button" onClick={clear} className="btn-secondary">Clear filters</button>
          )}
        </div>
      </div>

      {loading ? <Loading label="Loading products…" />
        : error ? <ErrorBox error={error} onRetry={reload} />
        : !data?.content?.length ? (
          <Empty title="No products match those filters"
                 description="Try widening the risk band or clearing the filters."
                 action={<button type="button" onClick={clear} className="btn-primary">Clear filters</button>} />
        ) : (
          <>
            <p className="mb-4 text-sm text-slate-500">
              Showing {data.content.length} of {data.totalElements} products
            </p>

            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {data.content.map((product) => (
                <article key={product.id} className="card flex flex-col">
                  <div className="flex items-start justify-between gap-2">
                    <span className="badge bg-teal-100 text-teal-800">{product.categoryLabel}</span>
                    <div className="flex gap-1">
                      {product.premiumOnly && <span className="badge bg-amber-100 text-amber-800">Premium</span>}
                      <span className={`badge ${RISK_CLASS[product.riskLevel]}`}>
                        {product.riskLevel.replace('_', ' ')}
                      </span>
                    </div>
                  </div>

                  <h2 className="mt-3 font-semibold text-slate-900">{product.name}</h2>
                  <p className="mt-0.5 text-xs text-slate-500">{product.fundHouse} · {product.code}</p>
                  <p className="mt-2 line-clamp-3 flex-1 text-sm text-slate-600">{product.description}</p>

                  <dl className="mt-4 grid grid-cols-2 gap-3 border-t border-slate-100 pt-3">
                    <div>
                      <dt className="text-xs text-slate-400">Expected return</dt>
                      <dd className="font-semibold text-emerald-600">{percent(product.expectedReturn)}</dd>
                    </div>
                    <div>
                      <dt className="text-xs text-slate-400">Minimum</dt>
                      <dd className="font-semibold">{compact(product.minInvestment)}</dd>
                    </div>
                    <div>
                      <dt className="text-xs text-slate-400">Lock-in</dt>
                      <dd className="text-sm">{product.lockInMonths > 0 ? `${product.lockInMonths} months` : 'None'}</dd>
                    </div>
                    <div>
                      <dt className="text-xs text-slate-400">Expense ratio</dt>
                      <dd className="text-sm">
                        {product.expenseRatio != null ? `${product.expenseRatio}%` : 'Not applicable'}
                      </dd>
                    </div>
                  </dl>

                  <p className="mt-3 border-t border-slate-100 pt-2 text-xs text-amber-500">
                    {'★'.repeat(product.rating)}
                    <span className="text-slate-300">{'★'.repeat(5 - product.rating)}</span>
                  </p>
                </article>
              ))}
            </div>

            <Pagination page={data.pageNumber} totalPages={data.totalPages} onChange={setPage} />
          </>
        )}
    </div>
  );
}
