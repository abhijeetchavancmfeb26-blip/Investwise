import { useState } from 'react';
import { Link } from 'react-router-dom';
import { del, get, patch, toError } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { Empty, ErrorBox, Loading, Notice, PageTitle, Pagination } from '../components/Ui';
import { date } from '../lib/format';

const BORDER = {
  SUCCESS: 'border-l-emerald-400', INFO: 'border-l-sky-400',
  WARNING: 'border-l-amber-400', ALERT: 'border-l-red-400',
};

export default function Notifications() {
  const [page, setPage] = useState(0);
  const [notice, setNotice] = useState(null);

  const { data, loading, error, reload } = useFetch(
    () => get('/api/v1/notifications', { page, size: 20 }), [page]);

  const act = async (action) => {
    try {
      await action();
      reload();
    } catch (err) {
      setNotice({ type: 'error', text: toError(err).message });
    }
  };

  if (loading) return <Loading label="Loading notifications…" />;
  if (error) return <ErrorBox error={error} onRetry={reload} />;

  const unreadCount = data?.content?.filter((item) => !item.read).length ?? 0;

  return (
    <div className="space-y-6">
      <PageTitle title="Notifications"
                 subtitle="Goal milestones, subscription changes and payment outcomes."
                 action={unreadCount > 0 && (
                   <button type="button" className="btn-secondary"
                           onClick={() => act(() => patch('/api/v1/notifications/read-all'))}>
                     Mark all as read
                   </button>
                 )} />

      {notice && <Notice type={notice.type} onDismiss={() => setNotice(null)}>{notice.text}</Notice>}

      {!data?.content?.length ? (
        <Empty title="Nothing here yet"
               description="Notifications appear when a goal is achieved, a payment completes, or your
                            subscription changes."
               action={<Link to="/dashboard" className="btn-primary">Back to dashboard</Link>} />
      ) : (
        <>
          <div className="space-y-3">
            {data.content.map((item) => (
              <article key={item.id}
                       className={`card border-l-4 ${item.read ? 'border-l-slate-200' : BORDER[item.type] ?? BORDER.INFO}`}>
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <h2 className={`font-semibold ${item.read ? 'text-slate-700' : 'text-slate-900'}`}>
                        {item.title}
                      </h2>
                      {!item.read && <span className="h-2 w-2 rounded-full bg-brand" aria-label="Unread" />}
                    </div>
                    <p className="mt-1 text-sm leading-relaxed text-slate-600">{item.message}</p>
                    <div className="mt-2 flex flex-wrap items-center gap-3 text-xs">
                      <span className="text-slate-400">{date(item.createdAt, true)}</span>
                      {item.actionUrl && (
                        <Link to={item.actionUrl}
                              onClick={() => !item.read && act(() => patch(`/api/v1/notifications/${item.id}/read`))}
                              className="font-medium text-brand hover:underline">Open</Link>
                      )}
                    </div>
                  </div>

                  <div className="flex shrink-0 gap-1">
                    {!item.read && (
                      <button type="button" className="btn-secondary px-2 py-1 text-xs"
                              onClick={() => act(() => patch(`/api/v1/notifications/${item.id}/read`))}>
                        Mark read
                      </button>
                    )}
                    <button type="button" className="btn-secondary px-2 py-1 text-xs text-red-600"
                            onClick={() => act(() => del(`/api/v1/notifications/${item.id}`))}>
                      Delete
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </div>

          <Pagination page={data.pageNumber} totalPages={data.totalPages} onChange={setPage} />
          <p className="text-center text-xs text-slate-400">
            Notifications are automatically removed after 90 days.
          </p>
        </>
      )}
    </div>
  );
}
