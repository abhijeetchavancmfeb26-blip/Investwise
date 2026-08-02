import { useEffect, useState } from 'react';
import { del, get, patch, toError } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { Badge, Empty, ErrorBox, Loading, Modal, Notice, PageTitle, Pagination, Table } from '../components/Ui';
import { date, money, title } from '../lib/format';

const STATUSES = ['PENDING', 'ACTIVE', 'SUSPENDED', 'LOCKED'];
const TIERS = ['FREE', 'PREMIUM', 'ELITE'];

export default function AdminUsers() {
  const [keyword, setKeyword] = useState('');
  const [debounced, setDebounced] = useState('');
  const [status, setStatus] = useState('');
  const [tier, setTier] = useState('');
  const [page, setPage] = useState(0);
  const [statusTarget, setStatusTarget] = useState(null);
  const [newStatus, setNewStatus] = useState('');
  const [reason, setReason] = useState('');
  const [deleting, setDeleting] = useState(null);
  const [notice, setNotice] = useState(null);

  useEffect(() => {
    const timer = setTimeout(() => { setDebounced(keyword); setPage(0); }, 400);
    return () => clearTimeout(timer);
  }, [keyword]);

  const { data, loading, error, reload } = useFetch(
    () => get('/api/v1/admin/users', {
      keyword: debounced || undefined, status: status || undefined,
      tier: tier || undefined, page, size: 15,
    }),
    [debounced, status, tier, page],
  );

  const submitStatus = async () => {
    try {
      await patch(`/api/v1/admin/users/${statusTarget.id}/status`,
                  { status: newStatus, reason: reason || null });
      setNotice({ type: 'success', text: `${statusTarget.fullName} is now ${title(newStatus)}.` });
      setStatusTarget(null);
      reload();
    } catch (err) {
      setNotice({ type: 'error', text: toError(err).message });
      setStatusTarget(null);
    }
  };

  const confirmDelete = async () => {
    try {
      await del(`/api/v1/admin/users/${deleting.id}`);
      setNotice({ type: 'success', text: 'User deleted permanently.' });
      setDeleting(null);
      reload();
    } catch (err) {
      setNotice({ type: 'error', text: toError(err).message });
      setDeleting(null);
    }
  };

  const clear = () => { setKeyword(''); setStatus(''); setTier(''); setPage(0); };

  return (
    <div className="space-y-6">
      <PageTitle title="User management"
                 subtitle="Status changes email the account holder and are written to the activity log." />

      {notice && <Notice type={notice.type} onDismiss={() => setNotice(null)}>{notice.text}</Notice>}

      <div className="card">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div className="lg:col-span-2">
            <label className="label" htmlFor="search">Search</label>
            <input id="search" type="search" value={keyword} className="input"
                   placeholder="Name, email or phone…" onChange={(e) => setKeyword(e.target.value)} />
          </div>
          <div>
            <label className="label" htmlFor="status">Account status</label>
            <select id="status" value={status} className="input"
                    onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
              <option value="">All statuses</option>
              {STATUSES.map((s) => <option key={s} value={s}>{title(s)}</option>)}
            </select>
          </div>
          <div>
            <label className="label" htmlFor="tier">Subscription tier</label>
            <select id="tier" value={tier} className="input"
                    onChange={(e) => { setTier(e.target.value); setPage(0); }}>
              <option value="">All tiers</option>
              {TIERS.map((t) => <option key={t} value={t}>{title(t)}</option>)}
            </select>
          </div>
        </div>
        {(keyword || status || tier) && (
          <button type="button" onClick={clear} className="btn-secondary mt-3">Clear all filters</button>
        )}
      </div>

      {loading ? <Loading label="Loading users…" />
        : error ? <ErrorBox error={error} onRetry={reload} />
        : !data?.content?.length ? <Empty title="No users match those filters" />
        : (
          <>
            <p className="text-sm text-slate-500">
              {data.totalElements} user{data.totalElements === 1 ? '' : 's'}
            </p>
            <Table headers={['User', 'Contact', 'Status', 'Tier', 'Income', 'Joined', 'Actions']}>
              {data.content.map((user) => {
                const isAdmin = user.roles?.includes('ROLE_ADMIN');
                return (
                  <tr key={user.id}>
                    <td>
                      <p className="font-medium text-slate-900">{user.fullName}</p>
                      <p className="text-xs text-slate-500">
                        {isAdmin ? 'Administrator' : 'Investor'}{user.age && ` · ${user.age}y`}
                      </p>
                    </td>
                    <td>
                      <p className="max-w-[180px] truncate text-xs">{user.email}</p>
                      <p className="text-xs text-slate-500">{user.phone}</p>
                    </td>
                    <td>
                      <Badge status={user.status} />
                      {!user.emailVerified && <p className="mt-0.5 text-xs text-amber-600">Unverified</p>}
                    </td>
                    <td>
                      <span className={`badge ${user.premium ? 'bg-amber-100 text-amber-800' : 'bg-slate-100 text-slate-600'}`}>
                        {user.tier}
                      </span>
                    </td>
                    <td className="text-xs">{user.annualIncome ? money(user.annualIncome) : '—'}</td>
                    <td className="text-xs">{date(user.createdAt)}</td>
                    <td>
                      <div className="flex gap-1">
                        <button type="button" disabled={isAdmin} className="btn-secondary px-2 py-1 text-xs"
                                onClick={() => {
                                  setStatusTarget(user);
                                  setNewStatus(user.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE');
                                  setReason('');
                                }}>
                          Status
                        </button>
                        <button type="button" disabled={isAdmin}
                                className="btn-secondary px-2 py-1 text-xs text-red-600"
                                onClick={() => setDeleting(user)}>Delete</button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </Table>
            <Pagination page={data.pageNumber} totalPages={data.totalPages} onChange={setPage} />
          </>
        )}

      <Modal open={Boolean(statusTarget)} onClose={() => setStatusTarget(null)}
             title={`Change status for ${statusTarget?.fullName ?? ''}`}
             footer={<>
               <button type="button" onClick={() => setStatusTarget(null)} className="btn-secondary">Cancel</button>
               <button type="button" onClick={submitStatus} className="btn-primary"
                       disabled={!newStatus || newStatus === statusTarget?.status}>Apply change</button>
             </>}>
        <div className="space-y-4">
          <div className="rounded bg-slate-50 p-3 text-sm">
            <div className="flex justify-between">
              <span className="text-slate-600">Current status</span>
              <Badge status={statusTarget?.status} />
            </div>
            <div className="mt-1 flex justify-between">
              <span className="text-slate-600">Email</span>
              <span className="font-medium">{statusTarget?.email}</span>
            </div>
          </div>

          <div>
            <label className="label" htmlFor="new-status">New status</label>
            <select id="new-status" value={newStatus} className="input"
                    onChange={(e) => setNewStatus(e.target.value)}>
              {STATUSES.map((s) => <option key={s} value={s}>{title(s)}</option>)}
            </select>
          </div>

          <div>
            <label className="label" htmlFor="reason">Reason</label>
            <textarea id="reason" rows={3} value={reason} className="input" maxLength={300}
                      placeholder="Included in the email sent to the account holder."
                      onChange={(e) => setReason(e.target.value)} />
            <p className="mt-1 text-xs text-slate-500">{reason.length} / 300</p>
          </div>
        </div>
      </Modal>

      <Modal open={Boolean(deleting)} onClose={() => setDeleting(null)}
             title="Delete this user permanently?"
             footer={<>
               <button type="button" onClick={() => setDeleting(null)} className="btn-secondary">Cancel</button>
               <button type="button" onClick={confirmDelete} className="btn-danger">Delete permanently</button>
             </>}>
        <p className="text-sm leading-relaxed text-slate-600">
          <strong>{deleting?.fullName}</strong> ({deleting?.email}) will be removed along with their
          roles and tokens. This cannot be undone. In most cases suspending the account is the better
          action, since it is reversible and preserves the record.
        </p>
      </Modal>
    </div>
  );
}
