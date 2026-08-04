import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { get, post, session, setUnauthorizedHandler } from './api';

const AuthContext = createContext(null);

/**
 * The only context in the application.
 *
 * The original also had a ToastContext; feedback is now shown inline by the page
 * that triggered it, which is both less plumbing and closer to where the user is
 * looking.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => session.user());
  const [loading, setLoading] = useState(Boolean(session.token()));
  const navigate = useNavigate();

  // Revalidate the cached profile once, so a revoked account is caught
  useEffect(() => {
    if (!session.token()) { setLoading(false); return; }

    get('/api/v1/users/me')
      .then((profile) => {
        setUser(profile);
        session.save(session.token(), profile);
      })
      .catch(() => {
        session.clear();
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(() => setUser(null));
  }, []);

  const login = useCallback(async (credentials) => {
    const result = await post('/api/v1/auth/login', credentials);
    session.save(result.accessToken, result.user);
    setUser(result.user);
    return result.user;
  }, []);

  const logout = useCallback(() => {
    session.clear();
    setUser(null);
    navigate('/login', { replace: true });
  }, [navigate]);

  const refresh = useCallback(async () => {
    const profile = await get('/api/v1/users/me');
    setUser(profile);
    session.save(session.token(), profile);
    return profile;
  }, []);

  const value = useMemo(() => ({
    user,
    loading,
    isAuthenticated: Boolean(user),
    isAdmin: Boolean(user?.roles?.includes('ROLE_ADMIN')),
    isPremium: Boolean(user?.premium),
    login,
    logout,
    refresh,
  }), [user, loading, login, logout, refresh]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside an AuthProvider');
  return context;
};
