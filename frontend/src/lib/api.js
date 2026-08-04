import axios from 'axios';

/**
 * One axios client pointed at the API gateway.
 *
 * The original needed two clients, a refresh-token interceptor and a promise to
 * deduplicate concurrent refreshes. With a single gateway origin and a
 * longer-lived token, none of that is necessary: attach the token, unwrap the
 * envelope, normalise the error.
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
  timeout: 20000,
});

export const session = {
  token: () => sessionStorage.getItem('investwise.token'),
  user: () => {
    try {
      return JSON.parse(sessionStorage.getItem('investwise.user') ?? 'null');
    } catch {
      return null;
    }
  },
  save: (token, user) => {
    sessionStorage.setItem('investwise.token', token);
    sessionStorage.setItem('investwise.user', JSON.stringify(user));
  },
  clear: () => {
    sessionStorage.removeItem('investwise.token');
    sessionStorage.removeItem('investwise.user');
  },
};

api.interceptors.request.use((config) => {
  const token = session.token();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

let onUnauthorized = () => {};
export const setUnauthorizedHandler = (handler) => { onUnauthorized = handler; };

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const isLogin = error.config?.url?.includes('/auth/');

    // An expired token means the session is over; there is nothing to refresh
    if (status === 401 && !isLogin) {
      session.clear();
      onUnauthorized();
    }
    return Promise.reject(toError(error));
  },
);

/** Flattens the backend's error body into a predictable shape for the UI. */
export function toError(error) {
  if (!error.response) {
    return { status: 0, message: 'Cannot reach the server. Is the gateway running on port 8080?', fieldErrors: {} };
  }
  const body = error.response.data ?? {};
  return {
    status: error.response.status,
    message: body.message ?? 'Something went wrong. Please try again.',
    fieldErrors: body.fieldErrors ?? {},
  };
}

/** Every endpoint returns { success, message, data }; callers only want data. */
const unwrap = (response) => response.data?.data;

export const get = (url, params) => api.get(url, { params }).then(unwrap);
export const post = (url, body, params) => api.post(url, body, { params }).then(unwrap);
export const put = (url, body) => api.put(url, body).then(unwrap);
export const patch = (url, body) => api.patch(url, body).then(unwrap);
export const del = (url) => api.delete(url).then(unwrap);

/** Downloads a report and hands the browser a save dialog. */
export async function download(type, format) {
  const response = await api.get(`/api/v1/reports/${type}/${format}`, { responseType: 'blob' });
  const match = (response.headers['content-disposition'] ?? '').match(/filename="?([^"]+)"?/);
  const filename = match ? match[1] : `investwise-report.${format}`;

  const url = URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
  return filename;
}

export default api;
