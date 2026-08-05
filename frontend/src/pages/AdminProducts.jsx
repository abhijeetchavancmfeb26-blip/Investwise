import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { productSchema } from '../lib/schemas';
import { del, get, patch, post, put, toError } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { Badge, Empty, ErrorBox, Loading, Modal, Notice, PageTitle, Pagination, Table } from '../components/Ui';
import { Checkbox, FormError, Input, Select, Submit, TextArea } from '../components/Form';
import { money, percent } from '../lib/format';

const EMPTY = {
  code: '', name: '', description: '', category: '', riskLevel: '', expectedReturn: '',
  minInvestment: '', lockInMonths: 0, fundHouse: '', expenseRatio: '', rating: 3,
  premiumOnly: false, active: true,
};

export default function AdminProducts() {
  const [keyword, setKeyword] = useState('');
  const [debounced, setDebounced] = useState('');
  const [category, setCategory] = useState('');
  const [page, setPage] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => { setDebounced(keyword); setPage(0); }, 400);
    return () => clearTimeout(timer);
  }, [keyword]);

  const { data: meta } = useFetch(() => get('/api/v1/metadata'), []);
  const { data, loading, error: loadError, reload } = useFetch(
    () => get('/api/v1/admin/products', {
      keyword: debounced || undefined, category: category || undefined, page, size: 15,
    }),
    [debounced, category, page],
  );

  const form = useForm({ resolver: zodResolver(productSchema), mode: 'onChange', defaultValues: EMPTY });

  const openCreate = () => { setEditing(null); setError(null); form.reset(EMPTY); setFormOpen(true); };

  const openEdit = (product) => {
    setEditing(product); setError(null);
    form.reset({
      code: product.code, name: product.name, description: product.description ?? '',
      category: product.category, riskLevel: product.riskLevel,
      expectedReturn: product.expectedReturn, minInvestment: product.minInvestment,
      lockInMonths: product.lockInMonths ?? 0, fundHouse: product.fundHouse ?? '',
      expenseRatio: product.expenseRatio ?? '', rating: product.rating,
      premiumOnly: product.premiumOnly, active: product.active,
    });
    setFormOpen(true);
  };

  const submit = async (values) => {
    setError(null);
    const payload = {
      ...values,
      expenseRatio: values.expenseRatio === '' ? null : values.expenseRatio,
      fundHouse: values.fundHouse === '' ? null : values.fundHouse,
    };
    try {
      if (editing) {
        await put(`/api/v1/admin/products/${editing.id}`, payload);
        setNotice({ type: 'success', text: 'Product updated.' });
      } else {
        await post('/api/v1/admin/products', payload);
        setNotice({ type: 'success', text: 'Product added to the catalogue.' });
      }
      setFormOpen(false);
      reload();
    } catch (err) {
      setError(err.status ? err : toError(err));
    }
  };

  const toggle = async (product) => {
    try {
      const updated = await patch(`/api/v1/admin/products/${product.id}/toggle`);
      setNotice({ type: 'success',
                  text: `${updated.name} is now ${updated.active ? 'active' : 'inactive'}.` });
      reload();
    } catch (err) {
      setNotice({ type: 'error', text: toError(err).message });
    }
  };

  const confirmDelete = async () => {
    try {
      await del(`/api/v1/admin/products/${deleting.id}`);
      setNotice({ type: 'success', text: 'Product deleted.' });
      setDeleting(null);
      reload();
    } catch (err) {
      // The API refuses deletion while investors hold the product
      setNotice({ type: 'error', text: toError(err).message });
      setDeleting(null);
    }
  };

  const runMaintenance = async () => {
    setBusy(true);
    try {
      const result = await post('/api/v1/admin/maintenance/run');
      setNotice({ type: 'success',
                  text: `${result.holdingsRevalued} holdings revalued, ${result.goalsRefreshed} goals refreshed, ${result.subscriptionsExpired} subscriptions expired.` });
    } catch (err) {
      setNotice({ type: 'error', text: toError(err).message });
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="space-y-6">
      <PageTitle title="Product catalogue"
                 subtitle="A product held in any portfolio cannot be deleted; deactivate it instead so
                           existing positions stay intact."
                 action={
                   <div className="flex flex-wrap gap-2">
                     <button type="button" onClick={runMaintenance} disabled={busy} className="btn-secondary">
                       Run maintenance
                     </button>
                     <button type="button" onClick={openCreate} className="btn-primary">New product</button>
                   </div>
                 } />

      {notice && <Notice type={notice.type} onDismiss={() => setNotice(null)}>{notice.text}</Notice>}

      <div className="card">
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
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
        </div>
      </div>

      {loading ? <Loading label="Loading catalogue…" />
        : loadError ? <ErrorBox error={loadError} onRetry={reload} />
        : !data?.content?.length ? <Empty title="No products match" />
        : (
          <>
            <p className="text-sm text-slate-500">{data.totalElements} products</p>
            <Table headers={['Code', 'Product', 'Category', 'Risk', 'Return', 'Minimum', 'Flags', 'Actions']}>
              {data.content.map((product) => (
                <tr key={product.id} className={product.active ? '' : 'opacity-60'}>
                  <td className="font-mono text-xs text-slate-500">{product.code}</td>
                  <td>
                    <p className="max-w-[200px] truncate font-medium text-slate-900">{product.name}</p>
                    <p className="text-xs text-slate-500">
                      {product.fundHouse ?? '—'} · {'★'.repeat(product.rating)}
                    </p>
                  </td>
                  <td className="text-xs">{product.categoryLabel}</td>
                  <td className="text-xs">{product.riskLevel.replace('_', ' ')}</td>
                  <td className="font-semibold text-emerald-600">{percent(product.expectedReturn)}</td>
                  <td>{money(product.minInvestment)}</td>
                  <td>
                    <div className="flex flex-wrap gap-1">
                      <Badge status={product.active ? 'ACTIVE' : 'EXPIRED'}>
                        {product.active ? 'Active' : 'Inactive'}
                      </Badge>
                      {product.premiumOnly && <span className="badge bg-amber-100 text-amber-800">Premium</span>}
                      {product.lockInMonths > 0 && (
                        <span className="badge bg-slate-100 text-slate-600">{product.lockInMonths}m lock</span>
                      )}
                    </div>
                  </td>
                  <td>
                    <div className="flex gap-1">
                      <button type="button" onClick={() => openEdit(product)}
                              className="btn-secondary px-2 py-1 text-xs">Edit</button>
                      <button type="button" onClick={() => toggle(product)}
                              className="btn-secondary px-2 py-1 text-xs">
                        {product.active ? 'Disable' : 'Enable'}
                      </button>
                      <button type="button" onClick={() => setDeleting(product)}
                              className="btn-secondary px-2 py-1 text-xs text-red-600">Delete</button>
                    </div>
                  </td>
                </tr>
              ))}
            </Table>
            <Pagination page={data.pageNumber} totalPages={data.totalPages} onChange={setPage} />
          </>
        )}

      <Modal open={formOpen} onClose={() => setFormOpen(false)}
             title={editing ? `Edit ${editing.name}` : 'Add a product'}
             footer={<>
               <button type="button" onClick={() => setFormOpen(false)} className="btn-secondary">Cancel</button>
               <Submit form="product-form" loading={form.formState.isSubmitting}
                       disabled={!form.formState.isValid}>
                 {editing ? 'Save changes' : 'Add product'}
               </Submit>
             </>}>
        <form id="product-form" onSubmit={form.handleSubmit(submit)} noValidate className="space-y-4">
          <FormError error={error} />

          <div className="grid gap-4 sm:grid-cols-3">
            <Input label="Product code" required placeholder="IW-EQ-001" hint="Format XX-XX-000"
                   disabled={Boolean(editing)} error={form.formState.errors.code} {...form.register('code')} />
            <Input label="Product name" required className="sm:col-span-2"
                   placeholder="Bluechip Large Cap Fund"
                   error={form.formState.errors.name} {...form.register('name')} />
          </div>

          <TextArea label="Description" rows={3}
                    placeholder="What the product does, who it suits and what the trade-off is."
                    error={form.formState.errors.description} {...form.register('description')} />

          <div className="grid gap-4 sm:grid-cols-2">
            <Select label="Category" required placeholder="Choose a category"
                    options={meta?.categories?.map((c) => ({ value: c.value, label: c.label })) ?? []}
                    error={form.formState.errors.category} {...form.register('category')} />
            <Select label="Risk level" required placeholder="Choose a risk level"
                    options={meta?.riskLevels?.map((r) => ({
                      value: r.value, label: r.value.replace('_', ' '),
                    })) ?? []}
                    error={form.formState.errors.riskLevel} {...form.register('riskLevel')} />
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <Input label="Expected return %" required type="number" min="0" max="60" step="0.1"
                   placeholder="13.5" error={form.formState.errors.expectedReturn}
                   {...form.register('expectedReturn')} />
            <Input label="Minimum investment" required type="number" min="100" step="100"
                   placeholder="5000" error={form.formState.errors.minInvestment}
                   {...form.register('minInvestment')} />
            <Input label="Lock-in (months)" type="number" min="0" max="360" placeholder="0"
                   error={form.formState.errors.lockInMonths} {...form.register('lockInMonths')} />
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <Input label="Fund house / issuer" placeholder="Axis Asset Management"
                   error={form.formState.errors.fundHouse} {...form.register('fundHouse')} />
            <Input label="Expense ratio %" type="number" min="0" max="5" step="0.01" placeholder="1.05"
                   error={form.formState.errors.expenseRatio} {...form.register('expenseRatio')} />
            <Input label="Rating (1-5)" required type="number" min="1" max="5"
                   error={form.formState.errors.rating} {...form.register('rating')} />
          </div>

          <div className="space-y-2 border-t border-slate-200 pt-3">
            <Checkbox label="Premium members only" {...form.register('premiumOnly')} />
            <Checkbox label="Active and available for investment" {...form.register('active')} />
          </div>
        </form>
      </Modal>

      <Modal open={Boolean(deleting)} onClose={() => setDeleting(null)} title="Delete this product?"
             footer={<>
               <button type="button" onClick={() => setDeleting(null)} className="btn-secondary">Cancel</button>
               <button type="button" onClick={confirmDelete} className="btn-danger">Delete</button>
             </>}>
        <p className="text-sm leading-relaxed text-slate-600">
          <strong>{deleting?.name}</strong> will be removed from the catalogue. If any investor
          currently holds it the request will be refused, because deleting it would orphan real
          positions. Deactivate the product instead to stop new investment while preserving history.
        </p>
      </Modal>
    </div>
  );
}
