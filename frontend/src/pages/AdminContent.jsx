import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { articleSchema } from '../lib/schemas';
import { del, get, post, put, toError } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { Badge, Empty, ErrorBox, Loading, Modal, Notice, PageTitle, Pagination, Table } from '../components/Ui';
import { Checkbox, FormError, Input, Select, Submit, TextArea } from '../components/Form';
import { date, title } from '../lib/format';

const EMPTY = {
  title: '', summary: '', content: '', category: '',
  author: 'InvestWise Research Desk', readMinutes: 5, premiumOnly: false, published: true,
};

export default function AdminContent() {
  const [keyword, setKeyword] = useState('');
  const [debounced, setDebounced] = useState('');
  const [category, setCategory] = useState('');
  const [page, setPage] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);

  useEffect(() => {
    const timer = setTimeout(() => { setDebounced(keyword); setPage(0); }, 400);
    return () => clearTimeout(timer);
  }, [keyword]);

  const { data: meta } = useFetch(() => get('/api/v1/metadata'), []);
  const { data, loading, error: loadError, reload } = useFetch(
    () => get('/api/v1/education', {
      keyword: debounced || undefined, category: category || undefined, page, size: 15,
    }),
    [debounced, category, page],
  );

  const form = useForm({ resolver: zodResolver(articleSchema), mode: 'onChange', defaultValues: EMPTY });

  const openCreate = () => { setEditing(null); setError(null); form.reset(EMPTY); setFormOpen(true); };

  const openEdit = async (article) => {
    setError(null);
    try {
      // List responses omit the body to keep payloads small, so fetch the full article
      const full = await get(`/api/v1/education/${article.slug}`);
      setEditing(full);
      form.reset({
        title: full.title, summary: full.summary ?? '', content: full.content ?? '',
        category: full.category, author: full.author ?? '', readMinutes: full.readMinutes ?? 5,
        premiumOnly: full.premiumOnly, published: full.published,
      });
      setFormOpen(true);
    } catch (err) {
      setNotice({ type: 'error', text: toError(err).message });
    }
  };

  const submit = async (values) => {
    setError(null);
    const payload = Object.fromEntries(
      Object.entries(values).map(([key, value]) => [key, value === '' ? null : value]));
    try {
      if (editing) {
        await put(`/api/v1/admin/education/${editing.id}`, payload);
        setNotice({ type: 'success', text: 'Article updated.' });
      } else {
        await post('/api/v1/admin/education', payload);
        setNotice({ type: 'success', text: 'Article published.' });
      }
      setFormOpen(false);
      reload();
    } catch (err) {
      setError(err.status ? err : toError(err));
    }
  };

  const confirmDelete = async () => {
    try {
      await del(`/api/v1/admin/education/${deleting.id}`);
      setNotice({ type: 'success', text: 'Article deleted.' });
      setDeleting(null);
      reload();
    } catch (err) {
      setNotice({ type: 'error', text: toError(err).message });
      setDeleting(null);
    }
  };

  return (
    <div className="space-y-6">
      <PageTitle title="Educational content"
                 subtitle="Slugs are generated from the title and de-duplicated automatically."
                 action={<button type="button" onClick={openCreate} className="btn-primary">New article</button>} />

      {notice && <Notice type={notice.type} onDismiss={() => setNotice(null)}>{notice.text}</Notice>}

      <div className="card">
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="label" htmlFor="search">Search</label>
            <input id="search" type="search" value={keyword} className="input"
                   placeholder="Title or summary…" onChange={(e) => setKeyword(e.target.value)} />
          </div>
          <div>
            <label className="label" htmlFor="category">Category</label>
            <select id="category" value={category} className="input"
                    onChange={(e) => { setCategory(e.target.value); setPage(0); }}>
              <option value="">All categories</option>
              {meta?.articleCategories?.map((c) => <option key={c} value={c}>{title(c)}</option>)}
            </select>
          </div>
        </div>
      </div>

      {loading ? <Loading label="Loading articles…" />
        : loadError ? <ErrorBox error={loadError} onRetry={reload} />
        : !data?.content?.length ? (
          <Empty title="No articles found"
                 action={<button type="button" onClick={openCreate} className="btn-primary">Write one</button>} />
        ) : (
          <>
            <p className="text-sm text-slate-500">{data.totalElements} articles</p>
            <Table headers={['Title', 'Category', 'Read time', 'Views', 'Flags', 'Created', 'Actions']}>
              {data.content.map((article) => (
                <tr key={article.id}>
                  <td>
                    <p className="max-w-[260px] truncate font-medium text-slate-900">{article.title}</p>
                    <p className="max-w-[260px] truncate font-mono text-xs text-slate-400">/{article.slug}</p>
                  </td>
                  <td className="text-xs">{title(article.category)}</td>
                  <td className="text-xs">{article.readMinutes} min</td>
                  <td className="text-xs">{article.viewCount}</td>
                  <td>
                    <div className="flex flex-wrap gap-1">
                      <Badge status={article.published ? 'ACTIVE' : 'EXPIRED'}>
                        {article.published ? 'Published' : 'Draft'}
                      </Badge>
                      {article.premiumOnly && <span className="badge bg-amber-100 text-amber-800">Premium</span>}
                    </div>
                  </td>
                  <td className="text-xs">{date(article.createdAt)}</td>
                  <td>
                    <div className="flex gap-1">
                      <button type="button" onClick={() => openEdit(article)}
                              className="btn-secondary px-2 py-1 text-xs">Edit</button>
                      <button type="button" onClick={() => setDeleting(article)}
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
             title={editing ? 'Edit article' : 'Write an article'}
             footer={<>
               <button type="button" onClick={() => setFormOpen(false)} className="btn-secondary">Cancel</button>
               <Submit form="article-form" loading={form.formState.isSubmitting}
                       disabled={!form.formState.isValid}>
                 {editing ? 'Save changes' : 'Publish article'}
               </Submit>
             </>}>
        <form id="article-form" onSubmit={form.handleSubmit(submit)} noValidate className="space-y-4">
          <FormError error={error} />

          <Input label="Title" required placeholder="Asset Allocation Before Stock Selection"
                 error={form.formState.errors.title} {...form.register('title')} />

          <TextArea label="Summary" rows={2}
                    placeholder="One sentence that makes the reader want the rest."
                    error={form.formState.errors.summary} {...form.register('summary')} />

          <TextArea label="Content" required rows={12}
                    placeholder="Separate paragraphs with a blank line. Plain text; no markup required."
                    hint="Rendered as paragraphs on the public article page"
                    error={form.formState.errors.content} {...form.register('content')} />

          <div className="grid gap-4 sm:grid-cols-3">
            <Select label="Category" required placeholder="Choose a category"
                    options={meta?.articleCategories?.map((c) => ({ value: c, label: title(c) })) ?? []}
                    error={form.formState.errors.category} {...form.register('category')} />
            <Input label="Author" placeholder="InvestWise Research Desk"
                   error={form.formState.errors.author} {...form.register('author')} />
            <Input label="Read time (minutes)" type="number" min="1" max="120"
                   error={form.formState.errors.readMinutes} {...form.register('readMinutes')} />
          </div>

          <div className="space-y-2 border-t border-slate-200 pt-3">
            <Checkbox label="Premium members only" {...form.register('premiumOnly')} />
            <Checkbox label="Published and visible in the public library" {...form.register('published')} />
          </div>
        </form>
      </Modal>

      <Modal open={Boolean(deleting)} onClose={() => setDeleting(null)} title="Delete this article?"
             footer={<>
               <button type="button" onClick={() => setDeleting(null)} className="btn-secondary">Cancel</button>
               <button type="button" onClick={confirmDelete} className="btn-danger">Delete</button>
             </>}>
        <p className="text-sm leading-relaxed text-slate-600">
          &ldquo;{deleting?.title}&rdquo; will be removed along with its view count. If you only want
          it hidden, edit it and switch off &ldquo;Published&rdquo; instead.
        </p>
      </Modal>
    </div>
  );
}
