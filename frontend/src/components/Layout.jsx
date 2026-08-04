import { useEffect, useState } from 'react';
import { Link, NavLink, Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../lib/auth';
import { get } from '../lib/api';
import { Loading } from './Ui';

/** Layouts and route guards together — they are all about "where does this render". */

const PUBLIC_LINKS = [
  { to: '/', label: 'Home', end: true },
  { to: '/about', label: 'About' },
  { to: '/services', label: 'Services' },
  { to: '/plans', label: 'Plans' },
  { to: '/pricing', label: 'Pricing' },
  { to: '/learn', label: 'Learn' },
  { to: '/contact', label: 'Contact' },
];

const APP_LINKS = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/goals', label: 'Goals' },
  { to: '/risk', label: 'Risk Profile' },
  { to: '/recommendations', label: 'Recommendations' },
  { to: '/portfolio', label: 'Portfolio' },
  { to: '/transactions', label: 'Transactions' },
  { to: '/reports', label: 'Reports' },
  { to: '/analytics', label: 'Analytics' },
  { to: '/subscription', label: 'Subscription' },
  { to: '/notifications', label: 'Notifications' },
  { to: '/profile', label: 'Profile' },
];

const ADMIN_LINKS = [
  { to: '/admin', label: 'Overview', end: true },
  { to: '/admin/users', label: 'Users' },
  { to: '/admin/products', label: 'Products' },
  { to: '/admin/billing', label: 'Billing' },
  { to: '/admin/content', label: 'Content' },
  { to: '/admin/messages', label: 'Enquiries' },
];

function Navbar() {
  const { isAuthenticated, isAdmin, user, logout } = useAuth();
  const [unread, setUnread] = useState(0);
  const location = useLocation();

  // Refresh the badge on navigation rather than on a timer: fewer moving parts
  useEffect(() => {
    if (!isAuthenticated) return;
    get('/api/v1/notifications/unread-count')
      .then((result) => setUnread(result?.count ?? 0))
      .catch(() => {});
  }, [isAuthenticated, location.pathname]);

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
        <Link to="/" className="text-lg font-bold text-brand">InvestWise</Link>

        <nav className="hidden gap-1 lg:flex">
          {PUBLIC_LINKS.map((link) => (
            <NavLink key={link.to} to={link.to} end={link.end}
                     className={({ isActive }) => `rounded px-3 py-2 text-sm ${
                       isActive ? 'bg-teal-50 text-brand' : 'text-slate-600 hover:bg-slate-100'}`}>
              {link.label}
            </NavLink>
          ))}
        </nav>

        <div className="flex items-center gap-2">
          {isAuthenticated ? (
            <>
              <Link to="/notifications" className="relative rounded px-2 py-1 text-sm text-slate-600 hover:bg-slate-100">
                Alerts
                {unread > 0 && (
                  <span className="ml-1 rounded-full bg-red-500 px-1.5 text-xs font-bold text-white">{unread}</span>
                )}
              </Link>
              <Link to="/dashboard" className="btn-secondary">{user?.firstName}</Link>
              {isAdmin && <Link to="/admin" className="btn-secondary">Admin</Link>}
              <button type="button" onClick={logout} className="btn-secondary">Sign out</button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn-secondary">Sign in</Link>
              <Link to="/register" className="btn-primary">Get started</Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}

function Footer() {
  return (
    <footer className="mt-auto border-t border-slate-200 bg-white">
      <div className="mx-auto max-w-6xl px-4 py-8">
        <div className="flex flex-wrap gap-x-6 gap-y-2 text-sm text-slate-600">
          <Link to="/about" className="hover:text-brand">About</Link>
          <Link to="/services" className="hover:text-brand">Services</Link>
          <Link to="/pricing" className="hover:text-brand">Pricing</Link>
          <Link to="/faq" className="hover:text-brand">FAQ</Link>
          <Link to="/contact" className="hover:text-brand">Contact</Link>
          <Link to="/legal/privacy" className="hover:text-brand">Privacy Policy</Link>
          <Link to="/legal/terms" className="hover:text-brand">Terms &amp; Conditions</Link>
        </div>
        <p className="mt-4 text-xs leading-relaxed text-slate-500">
          Investments in securities are subject to market risk; read all scheme related documents
          carefully. Projected values assume the stated rate of return and are illustrations, not
          guarantees. InvestWise provides planning tools and does not execute trades on your behalf.
        </p>
        <p className="mt-3 text-xs text-slate-400">
          © {new Date().getFullYear()} InvestWise. Built as a CDAC final project.
        </p>
      </div>
    </footer>
  );
}

function Sidebar({ links }) {
  return (
    <aside className="hidden w-52 shrink-0 border-r border-slate-200 bg-white lg:block">
      <nav className="sticky top-0 space-y-1 p-4">
        {links.map((link) => (
          <NavLink key={link.to} to={link.to} end={link.end}
                   className={({ isActive }) => `block rounded px-3 py-2 text-sm ${
                     isActive ? 'bg-brand text-white' : 'text-slate-600 hover:bg-slate-100'}`}>
            {link.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}

/** Public marketing shell. */
export function PublicLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <Navbar />
      <main className="flex-1"><Outlet /></main>
      <Footer />
    </div>
  );
}

/** Centred shell for the auth screens. */
export function AuthLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <Navbar />
      <main className="flex flex-1 items-center justify-center px-4 py-10">
        <div className="w-full max-w-md"><Outlet /></div>
      </main>
    </div>
  );
}

/** Signed-in shells; the only difference is which links the sidebar shows. */
export function AppLayout({ admin = false }) {
  const { isAuthenticated, isAdmin, loading } = useAuth();
  const location = useLocation();

  if (loading) return <Loading label="Checking your session…" />;
  if (!isAuthenticated) return <Navigate to="/login" state={{ from: location.pathname }} replace />;

  if (admin && !isAdmin) {
    return (
      <div className="flex min-h-screen flex-col bg-slate-50">
        <Navbar />
        <main className="page">
          <div className="card mx-auto max-w-md text-center">
            <h1 className="font-bold text-slate-900">Administrator access only</h1>
            <p className="mt-2 text-sm text-slate-600">
              This area is restricted to platform administrators.
            </p>
            <Link to="/dashboard" className="btn-primary mt-4">Back to my dashboard</Link>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <Navbar />
      <div className="flex flex-1">
        <Sidebar links={admin ? ADMIN_LINKS : APP_LINKS} />
        <main className="min-w-0 flex-1 px-4 py-6"><Outlet /></main>
      </div>
    </div>
  );
}

/** Keeps a signed-in user off the login and register screens. */
export function GuestOnly({ children }) {
  const { isAuthenticated, isAdmin, loading } = useAuth();
  if (loading) return <Loading />;
  return isAuthenticated ? <Navigate to={isAdmin ? '/admin' : '/dashboard'} replace /> : children;
}
