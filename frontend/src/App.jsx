import { useEffect } from 'react';
import { Route, Routes, useLocation } from 'react-router-dom';
import { AppLayout, AuthLayout, GuestOnly, PublicLayout } from './components/Layout';

// Public
import Home from './pages/Home';
import About from './pages/About';
import Services from './pages/Services';
import Pricing from './pages/Pricing';
import Plans from './pages/Plans';
import Learn from './pages/Learn';
import Article from './pages/Article';
import Faq from './pages/Faq';
import Contact from './pages/Contact';
import Legal from './pages/Legal';
import NotFound from './pages/NotFound';

// Auth
import Login from './pages/Login';
import Register from './pages/Register';
import VerifyEmail from './pages/VerifyEmail';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';

// Investor
import Dashboard from './pages/Dashboard';
import Goals from './pages/Goals';
import Risk from './pages/Risk';
import Recommendations from './pages/Recommendations';
import Portfolio from './pages/Portfolio';
import Transactions from './pages/Transactions';
import Reports from './pages/Reports';
import Analytics from './pages/Analytics';
import Subscription from './pages/Subscription';
import Checkout from './pages/Checkout';
import Notifications from './pages/Notifications';
import Profile from './pages/Profile';

// Admin
import AdminOverview from './pages/AdminOverview';
import AdminUsers from './pages/AdminUsers';
import AdminProducts from './pages/AdminProducts';
import AdminBilling from './pages/AdminBilling';
import AdminContent from './pages/AdminContent';
import AdminMessages from './pages/AdminMessages';

/** A SPA does not restore scroll position on navigation, so this does. */
function ScrollToTop() {
  const { pathname } = useLocation();
  useEffect(() => { window.scrollTo(0, 0); }, [pathname]);
  return null;
}

export default function App() {
  return (
    <>
      <ScrollToTop />
      <Routes>
        {/* ---------- public ---------- */}
        <Route element={<PublicLayout />}>
          <Route path="/" element={<Home />} />
          <Route path="/about" element={<About />} />
          <Route path="/services" element={<Services />} />
          <Route path="/pricing" element={<Pricing />} />
          <Route path="/plans" element={<Plans />} />
          <Route path="/learn" element={<Learn />} />
          <Route path="/learn/:slug" element={<Article />} />
          <Route path="/faq" element={<Faq />} />
          <Route path="/contact" element={<Contact />} />
          {/* One component serves both legal documents, chosen by the route param */}
          <Route path="/legal/:document" element={<Legal />} />
          <Route path="*" element={<NotFound />} />
        </Route>

        {/* ---------- authentication ---------- */}
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<GuestOnly><Login /></GuestOnly>} />
          <Route path="/register" element={<GuestOnly><Register /></GuestOnly>} />
          <Route path="/verify-email" element={<VerifyEmail />} />
          <Route path="/forgot-password" element={<GuestOnly><ForgotPassword /></GuestOnly>} />
          <Route path="/reset-password" element={<ResetPassword />} />
        </Route>

        {/* ---------- investor ---------- */}
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/goals" element={<Goals />} />
          <Route path="/risk" element={<Risk />} />
          <Route path="/recommendations" element={<Recommendations />} />
          <Route path="/portfolio" element={<Portfolio />} />
          <Route path="/transactions" element={<Transactions />} />
          <Route path="/reports" element={<Reports />} />
          <Route path="/analytics" element={<Analytics />} />
          <Route path="/subscription" element={<Subscription />} />
          <Route path="/checkout/:planCode" element={<Checkout />} />
          <Route path="/notifications" element={<Notifications />} />
          <Route path="/profile" element={<Profile />} />
        </Route>

        {/* ---------- administration ---------- */}
        <Route element={<AppLayout admin />}>
          <Route path="/admin" element={<AdminOverview />} />
          <Route path="/admin/users" element={<AdminUsers />} />
          <Route path="/admin/products" element={<AdminProducts />} />
          <Route path="/admin/billing" element={<AdminBilling />} />
          <Route path="/admin/content" element={<AdminContent />} />
          <Route path="/admin/messages" element={<AdminMessages />} />
        </Route>
      </Routes>
    </>
  );
}
