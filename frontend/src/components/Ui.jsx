import { Link } from 'react-router-dom';
import { badgeClass } from '../lib/format';

/**
 * Every shared presentational piece, in one file.
 *
 * The original split these across Primitives.jsx, Form.jsx and three layout
 * files. They are small enough that one module is easier to search than four.
 */

export function Loading({ label = 'Loading…' }) {
  return (
    <div className="py-16 text-center text-sm text-slate-500" role="status">{label}</div>
  );
}

export function ErrorBox({ error, onRetry }) {
  if (!error) return null;
  return (
    <div className="rounded border border-red-200 bg-red-50 p-4 text-center">
      <p className="text-sm font-medium text-red-800">{error.message}</p>
      {onRetry && (
        <button type="button" onClick={onRetry} className="btn-secondary mt-3">Try again</button>
      )}
    </div>
  );
}

/** Inline feedback, replacing the original's toast context. */
export function Notice({ type = 'info', children, onDismiss }) {
  if (!children) return null;
  const styles = {
    success: 'border-emerald-200 bg-emerald-50 text-emerald-800',
    error: 'border-red-200 bg-red-50 text-red-800',
    info: 'border-sky-200 bg-sky-50 text-sky-800',
    warning: 'border-amber-200 bg-amber-50 text-amber-800',
  }[type];

  return (
    <div className={`flex items-start justify-between gap-3 rounded border p-3 text-sm ${styles}`} role="alert">
      <span>{children}</span>
      {onDismiss && (
        <button type="button" onClick={onDismiss} className="shrink-0 font-bold opacity-60 hover:opacity-100"
                aria-label="Dismiss">✕</button>
      )}
    </div>
  );
}

export function Empty({ title, description, action }) {
  return (
    <div className="rounded border border-dashed border-slate-300 bg-white px-6 py-12 text-center">
      <h3 className="font-semibold text-slate-800">{title}</h3>
      {description && <p className="mx-auto mt-2 max-w-md text-sm text-slate-500">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

export function Badge({ status, children }) {
  return <span className={`badge ${badgeClass(status)}`}>{children ?? status}</span>;
}

export function Stat({ label, value, sub, to }) {
  const body = (
    <div className="card h-full">
      <p className="text-xs font-medium uppercase text-slate-500">{label}</p>
      <p className="mt-2 text-2xl font-bold text-slate-900">{value}</p>
      {sub && <p className="mt-1 text-xs text-slate-500">{sub}</p>}
    </div>
  );
  return to ? <Link to={to} className="block">{body}</Link> : body;
}

export function Progress({ value }) {
  const pct = Math.max(0, Math.min(100, Number(value ?? 0)));
  const colour = pct >= 75 ? 'bg-emerald-500' : pct >= 40 ? 'bg-brand' : 'bg-amber-500';
  return (
    <div className="h-2 w-full overflow-hidden rounded bg-slate-200">
      <div className={`h-full ${colour}`} style={{ width: `${pct}%` }}
           role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100} />
    </div>
  );
}

export function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;
  return (
    <div className="flex items-center justify-between pt-4 text-sm">
      <span className="text-slate-500">Page {page + 1} of {totalPages}</span>
      <div className="flex gap-2">
        <button type="button" className="btn-secondary" disabled={page === 0}
                onClick={() => onChange(page - 1)}>Previous</button>
        <button type="button" className="btn-secondary" disabled={page >= totalPages - 1}
                onClick={() => onChange(page + 1)}>Next</button>
      </div>
    </div>
  );
}

export function Modal({ open, onClose, title, children, footer }) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-slate-900/50" onClick={onClose} role="presentation" />
      <div className="relative z-10 w-full max-w-2xl rounded bg-white shadow-lg">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-3">
          <h2 className="font-semibold text-slate-900">{title}</h2>
          <button type="button" onClick={onClose} className="text-slate-400 hover:text-slate-700"
                  aria-label="Close">✕</button>
        </div>
        <div className="max-h-[70vh] overflow-y-auto px-5 py-4">{children}</div>
        {footer && <div className="flex justify-end gap-2 border-t border-slate-200 px-5 py-3">{footer}</div>}
      </div>
    </div>
  );
}

export function PageTitle({ title, subtitle, action }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
        {subtitle && <p className="mt-1 max-w-2xl text-sm text-slate-600">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}

export function Table({ headers, children }) {
  return (
    <div className="overflow-x-auto rounded border border-slate-200 bg-white">
      <table className="table">
        <thead><tr>{headers.map((h) => <th key={h}>{h}</th>)}</tr></thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  );
}
