import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="page text-center">
      <p className="text-5xl font-bold text-slate-300">404</p>
      <h1 className="mt-4 text-xl font-bold text-slate-900">That page does not exist</h1>
      <p className="mt-2 text-slate-600">
        The link may be out of date, or the address may have a typo. Nothing has gone wrong with your account.
      </p>
      <div className="mt-6 flex flex-wrap justify-center gap-3">
        <Link to="/" className="btn-primary">Back to home</Link>
        <Link to="/plans" className="btn-secondary">Browse products</Link>
        <Link to="/contact" className="btn-secondary">Get help</Link>
      </div>
    </div>
  );
}
