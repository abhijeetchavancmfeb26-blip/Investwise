/** Formatting helpers. Indian numbering, because that is who the platform is for. */

const currency = new Intl.NumberFormat('en-IN', {
  style: 'currency', currency: 'INR', maximumFractionDigits: 0,
});

export const money = (value) => {
  const amount = Number(value ?? 0);
  return Number.isNaN(amount) ? '—' : currency.format(amount);
};

/** Abbreviates the way Indian financial UIs do: 1.2 Cr, 45.5 L. */
export const compact = (value) => {
  const amount = Number(value ?? 0);
  if (Number.isNaN(amount)) return '—';
  const abs = Math.abs(amount);
  const sign = amount < 0 ? '-' : '';

  if (abs >= 1e7) return `${sign}₹${(abs / 1e7).toFixed(2)} Cr`;
  if (abs >= 1e5) return `${sign}₹${(abs / 1e5).toFixed(2)} L`;
  if (abs >= 1000) return `${sign}₹${(abs / 1000).toFixed(1)}K`;
  return currency.format(amount);
};

export const percent = (value, decimals = 2) => {
  const num = Number(value ?? 0);
  return Number.isNaN(num) ? '—' : `${num.toFixed(decimals)}%`;
};

export const number = (value, decimals = 2) => {
  const num = Number(value ?? 0);
  return Number.isNaN(num) ? '—' : num.toFixed(decimals);
};

export const date = (value, withTime = false) => {
  if (!value) return '—';
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return '—';
  return parsed.toLocaleDateString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric',
    ...(withTime ? { hour: '2-digit', minute: '2-digit' } : {}),
  });
};

export const title = (value) =>
  (value ?? '').toString().toLowerCase().split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');

/** Status to badge colour, in one place. */
export const badgeClass = (status) => ({
  ACTIVE: 'bg-emerald-100 text-emerald-800',
  ON_TRACK: 'bg-emerald-100 text-emerald-800',
  ACHIEVED: 'bg-teal-100 text-teal-800',
  SUCCESS: 'bg-emerald-100 text-emerald-800',
  BEHIND: 'bg-amber-100 text-amber-800',
  PENDING: 'bg-amber-100 text-amber-800',
  IN_PROGRESS: 'bg-amber-100 text-amber-800',
  NEW: 'bg-sky-100 text-sky-800',
  CREATED: 'bg-sky-100 text-sky-800',
  RESOLVED: 'bg-emerald-100 text-emerald-800',
  EXPIRED: 'bg-slate-100 text-slate-700',
  CANCELLED: 'bg-slate-100 text-slate-700',
  CLOSED: 'bg-slate-100 text-slate-700',
  SUSPENDED: 'bg-red-100 text-red-800',
  LOCKED: 'bg-red-100 text-red-800',
  FAILED: 'bg-red-100 text-red-800',
}[status] ?? 'bg-slate-100 text-slate-700');

export const gainClass = (value) =>
  Number(value ?? 0) > 0 ? 'text-emerald-600'
    : Number(value ?? 0) < 0 ? 'text-red-600' : 'text-slate-600';
