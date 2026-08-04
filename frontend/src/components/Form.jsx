import { useState } from 'react';
import { scorePassword } from '../lib/schemas';

/** Form controls wired for React Hook Form. Each handles its own error markup. */

function Wrapper({ label, error, hint, required, children }) {
  return (
    <div>
      {label && (
        <label className="label">
          {label}{required && <span className="text-red-500"> *</span>}
        </label>
      )}
      {children}
      {error && <p className="mt-1 text-xs text-red-600" role="alert">{error.message}</p>}
      {!error && hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
    </div>
  );
}

export function Input({ label, error, hint, required, ...props }) {
  return (
    <Wrapper label={label} error={error} hint={hint} required={required}>
      <input {...props} aria-invalid={Boolean(error)} className={`input ${error ? 'input-error' : ''}`} />
    </Wrapper>
  );
}

export function TextArea({ label, error, hint, required, rows = 4, ...props }) {
  return (
    <Wrapper label={label} error={error} hint={hint} required={required}>
      <textarea {...props} rows={rows} aria-invalid={Boolean(error)}
                className={`input ${error ? 'input-error' : ''}`} />
    </Wrapper>
  );
}

export function Select({ label, error, hint, required, options = [], placeholder, ...props }) {
  return (
    <Wrapper label={label} error={error} hint={hint} required={required}>
      <select {...props} aria-invalid={Boolean(error)} className={`input ${error ? 'input-error' : ''}`}>
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((option) => (
          <option key={option.value ?? option} value={option.value ?? option}>
            {option.label ?? option}
          </option>
        ))}
      </select>
    </Wrapper>
  );
}

export function Checkbox({ label, error, ...props }) {
  return (
    <div>
      <label className="flex cursor-pointer items-start gap-2 text-sm text-slate-700">
        <input type="checkbox" {...props} className="mt-0.5 h-4 w-4 rounded border-slate-300 text-brand" />
        <span>{label}</span>
      </label>
      {error && <p className="mt-1 text-xs text-red-600" role="alert">{error.message}</p>}
    </div>
  );
}

/** Password field with a visibility toggle and an optional live strength meter. */
export function Password({ label, error, hint, required, value = '', meter = false, ...props }) {
  const [visible, setVisible] = useState(false);
  const strength = meter ? scorePassword(value) : null;

  return (
    <Wrapper label={label} error={error} hint={hint} required={required}>
      <div className="relative">
        <input {...props} type={visible ? 'text' : 'password'} aria-invalid={Boolean(error)}
               className={`input pr-16 ${error ? 'input-error' : ''}`} />
        <button type="button" onClick={() => setVisible((v) => !v)} tabIndex={-1}
                className="absolute inset-y-0 right-0 px-3 text-xs font-medium text-slate-500 hover:text-slate-800">
          {visible ? 'Hide' : 'Show'}
        </button>
      </div>

      {meter && value.length > 0 && (
        <div className="mt-2">
          <div className="flex items-center gap-2">
            <div className="flex flex-1 gap-1">
              {[0, 1, 2, 3, 4].map((i) => (
                <span key={i} className={`h-1.5 flex-1 rounded ${i < strength.score ? strength.bar : 'bg-slate-200'}`} />
              ))}
            </div>
            <span className={`text-xs font-medium ${strength.text}`}>{strength.label}</span>
          </div>
          <ul className="mt-2 grid grid-cols-1 gap-x-4 sm:grid-cols-2">
            {strength.checks.map((check) => (
              <li key={check.label} className={`text-xs ${check.ok ? 'text-emerald-600' : 'text-slate-400'}`}>
                {check.ok ? '✓' : '○'} {check.label}
              </li>
            ))}
          </ul>
        </div>
      )}
    </Wrapper>
  );
}

/** Stays disabled while the form is invalid or in flight. */
export function Submit({ children, loading, disabled, className = '', ...props }) {
  return (
    <button type="submit" disabled={loading || disabled} className={`btn-primary ${className}`} {...props}>
      {loading ? 'Working…' : children}
    </button>
  );
}

/** Renders a server error, including per-field messages. */
export function FormError({ error }) {
  if (!error) return null;
  const fields = Object.entries(error.fieldErrors ?? {});
  return (
    <div className="rounded border border-red-200 bg-red-50 p-3" role="alert">
      <p className="text-sm font-medium text-red-800">{error.message}</p>
      {fields.length > 0 && (
        <ul className="mt-1 space-y-0.5 text-xs text-red-700">
          {fields.map(([field, message]) => <li key={field}>• {message}</li>)}
        </ul>
      )}
    </div>
  );
}
