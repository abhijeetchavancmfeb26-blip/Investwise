import { useEffect, useState } from 'react';
import { del, get, post, toError } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { Badge, Empty, ErrorBox, Loading, Modal, Notice, PageTitle, Pagination } from '../components/Ui';
import { date, title } from '../lib/format';

const STATUSES = ['NEW', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

export default function AdminMessages() {
  const [keyword, setKeyword] = useState('');
  const [debounced, setDebounced] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [replying, setReplying] = useState(null);
  const [reply, setReply] = useState('');
  const [replyStatus, setReplyStatus] = useState('RESOLVED');
  const [deleting, setDeleting] = useState(null);
  const [notice, setNotice] = useState(null);

  useEffect(() => {
    const timer = setTimeout(() => { setDebounced(keyword); setPage(0); }, 400);
    return () => clearTimeout(timer);
  }, [keyword]);

  const { data, loading, error, reload } = useFetch(
    () => get('/api/v1/admin/contact-messages', {
      keyword: debounced || undefined, status: status || undefined, page, size: 15,
    }),
    [debounced, status, page],
  );

  const submitReply = async () => {
    try {
      await post(`/api/v1/admin/contact-messages/${replying.id}/reply`,
                 { reply, status: replyStatus });
      setNotice({ type: 'success', text: 'Reply recorded.' });
      setReplying(null);
      reload();
    } catch (err) {
      setNotice({ type: 'error', text: toError(err).message });
      setReplying(null);
    }
  };

  const confirmDelete = async () => {
    try {
      await del(`/api/v1/admin/contact-messages/${deleting.id}`);
      setNotice({ type: 'success', text: 'Enquiry deleted.' });
      setDeleting(null);
      reload();
    } catch (err) {
      setNotice({ type: 'error', text: toError(err).message });
      setDeleting(null);
    }
  };

  return (
    <div className="space-y-6">
      <PageTitle title="Contact enquiries"
                 subtitle="Replies are recorded against the enquiry and written to the activity log
                           with your identity attached." />

      {notice && <Notice type={notice.type} onDismiss={() => setNotice(null)}>{notice.text}</Notice>}

      <div className="card">
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="label" htmlFor="search">Search</label>
            <input id="search" type="search" value={keyword} className="input"
                   placeholder="Name, email or subject…" onChange={(e) => setKeyword(e.target.value)} />
          </div>
          <div>
            <label className="label" htmlFor="status">Status</label>
            <select id="status" value={status} className="input"
                    onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
              <option value="">All statuses</option>
              {STATUSES.map((s) => <option key={s} value={s}>{title(s)}</option>)}
            </select>
          </div>
        </div>
      </div>

      {loading ? <Loading label="Loading enquiries…" />
        : error ? <ErrorBox error={error} onRetry={reload} />
        : !data?.content?.length ? <Empty title="No enquiries match those filters" />
        : (
          <>
            <div className="space-y-4">
              {data.content.map((message) => (
                <article key={message.id} className="card">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <h2 className="font-semibold text-slate-900">{message.subject}</h2>
                        <Badge status={message.status} />
                      </div>
                      <p className="mt-1 text-sm text-slate-600">
                        <strong>{message.name}</strong> · {message.email}
                        {message.phone && ` · ${message.phone}`}
                      </p>
                      <p className="mt-0.5 text-xs text-slate-400">
                        {date(message.createdAt, true)}
                        {message.userId && ` · registered user #${message.userId}`}
                      </p>
                    </div>

                    <div className="flex shrink-0 gap-1">
                      <button type="button" className="btn-primary px-3 py-1 text-xs"
                              onClick={() => {
                                setReplying(message);
                                setReply(message.adminReply ?? '');
                                setReplyStatus(message.status === 'NEW' ? 'RESOLVED' : message.status);
                              }}>
                        {message.adminReply ? 'Edit reply' : 'Reply'}
                      </button>
                      <button type="button" onClick={() => setDeleting(message)}
                              className="btn-secondary px-2 py-1 text-xs text-red-600">Delete</button>
                    </div>
                  </div>

                  <p className="mt-3 whitespace-pre-wrap rounded bg-slate-50 p-3 text-sm leading-relaxed text-slate-700">
                    {message.message}
                  </p>

                  {message.adminReply && (
                    <div className="mt-3 rounded border-l-4 border-l-teal-400 bg-teal-50 p-3">
                      <p className="text-xs font-semibold uppercase text-teal-700">
                        Replied by {message.repliedBy} · {date(message.repliedAt, true)}
                      </p>
                      <p className="mt-1 whitespace-pre-wrap text-sm leading-relaxed text-slate-700">
                        {message.adminReply}
                      </p>
                    </div>
                  )}
                </article>
              ))}
            </div>

            <Pagination page={data.pageNumber} totalPages={data.totalPages} onChange={setPage} />
          </>
        )}

      <Modal open={Boolean(replying)} onClose={() => setReplying(null)}
             title={`Reply to ${replying?.name ?? ''}`}
             footer={<>
               <button type="button" onClick={() => setReplying(null)} className="btn-secondary">Cancel</button>
               <button type="button" onClick={submitReply} className="btn-primary"
                       disabled={reply.trim().length < 5}>Record reply</button>
             </>}>
        <div className="space-y-4">
          <div className="rounded bg-slate-50 p-3">
            <p className="text-xs uppercase text-slate-400">Original enquiry</p>
            <p className="mt-1 font-medium text-slate-900">{replying?.subject}</p>
            <p className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-slate-600">
              {replying?.message}
            </p>
          </div>

          <div>
            <label className="label" htmlFor="reply">Your reply</label>
            <textarea id="reply" rows={7} value={reply} className="input" maxLength={2000}
                      placeholder="Answer the question directly. Avoid individual investment recommendations."
                      onChange={(e) => setReply(e.target.value)} />
            <p className="mt-1 text-xs text-slate-500">{reply.length} / 2000</p>
          </div>

          <div>
            <label className="label" htmlFor="reply-status">Set status to</label>
            <select id="reply-status" value={replyStatus} className="input"
                    onChange={(e) => setReplyStatus(e.target.value)}>
              {STATUSES.map((s) => <option key={s} value={s}>{title(s)}</option>)}
            </select>
          </div>

          <p className="rounded bg-amber-50 p-3 text-xs leading-relaxed text-amber-800">
            Remember we do not provide individual investment recommendations over email. Point the
            enquirer at the recommendation engine, which has their full risk profile, rather than
            answering from partial information.
          </p>
        </div>
      </Modal>

      <Modal open={Boolean(deleting)} onClose={() => setDeleting(null)} title="Delete this enquiry?"
             footer={<>
               <button type="button" onClick={() => setDeleting(null)} className="btn-secondary">Cancel</button>
               <button type="button" onClick={confirmDelete} className="btn-danger">Delete</button>
             </>}>
        <p className="text-sm leading-relaxed text-slate-600">
          The enquiry from {deleting?.name} and any recorded reply will be removed. Consider closing
          it instead if you may need the record later.
        </p>
      </Modal>
    </div>
  );
}
