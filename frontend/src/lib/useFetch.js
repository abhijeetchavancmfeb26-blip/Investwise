import { useCallback, useEffect, useState } from 'react';
import { toError } from './api';

/**
 * The only custom hook.
 *
 * The original had four (useApi, useDebounce, useRazorpayScript, plus the toast
 * hook). Debouncing is now a two-line effect where it is actually needed, and
 * the Razorpay script loads from index.html.
 */
export function useFetch(fetcher, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const run = useCallback(() => {
    setLoading(true);
    setError(null);
    return fetcher()
      .then((result) => { setData(result); return result; })
      .catch((err) => { setError(err.status ? err : toError(err)); return null; })
      .finally(() => setLoading(false));
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => { run(); }, [run]);

  return { data, loading, error, reload: run, setData };
}
