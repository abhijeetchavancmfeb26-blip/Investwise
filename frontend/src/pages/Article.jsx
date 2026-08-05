import { Link, useParams } from 'react-router-dom';
import { get } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { ErrorBox, Loading } from '../components/Ui';
import { date, title } from '../lib/format';

export default function Article() {
  const { slug } = useParams();
  const { data: article, loading, error, reload } = useFetch(() => get(`/api/v1/education/${slug}`), [slug]);
  const { data: popular } = useFetch(() => get('/api/v1/education/popular'), []);

  if (loading) return <div className="page"><Loading label="Loading article…" /></div>;

  // A 403 means the article exists but is gated, which deserves an upgrade prompt
  if (error?.status === 403) {
    return (
      <div className="page max-w-lg text-center">
        <div className="card">
          <h1 className="font-bold text-slate-900">This article is for Premium members</h1>
          <p className="mt-2 text-sm text-slate-600">{error.message}</p>
          <div className="mt-5 flex flex-wrap justify-center gap-3">
            <Link to="/pricing" className="btn-primary">See Premium plans</Link>
            <Link to="/learn" className="btn-secondary">Back to the library</Link>
          </div>
        </div>
      </div>
    );
  }

  if (error) return <div className="page"><ErrorBox error={error} onRetry={reload} /></div>;
  if (!article) return null;

  // The backend stores plain text with blank-line paragraph breaks
  const paragraphs = (article.content ?? '').split(/\n\s*\n/).filter(Boolean);

  return (
    <div className="page">
      <Link to="/learn" className="text-sm text-brand hover:underline">← Back to the library</Link>

      <div className="mt-4 grid gap-8 lg:grid-cols-[1fr_260px]">
        <article>
          <span className="badge bg-teal-100 text-teal-800">{title(article.category)}</span>
          <h1 className="mt-3 text-3xl font-bold leading-tight text-slate-900">{article.title}</h1>
          {article.summary && <p className="mt-3 text-lg text-slate-600">{article.summary}</p>}

          <p className="mt-4 border-y border-slate-200 py-3 text-sm text-slate-500">
            {article.author} · {article.readMinutes} min read · {article.viewCount} views · {date(article.createdAt)}
          </p>

          <div className="mt-6 space-y-4">
            {paragraphs.map((paragraph, index) => (
              <p key={index} className="leading-relaxed text-slate-700">{paragraph}</p>
            ))}
          </div>

          <p className="mt-8 rounded border border-slate-200 bg-slate-50 p-4 text-xs leading-relaxed text-slate-500">
            This article is educational and does not constitute investment advice. Consider your own
            circumstances, and where appropriate consult a qualified adviser, before acting on it.
          </p>
        </article>

        <aside>
          <div className="card">
            <h2 className="text-xs font-bold uppercase text-slate-500">Read next</h2>
            <ul className="mt-3 space-y-3">
              {popular?.filter((item) => item.slug !== slug).slice(0, 4).map((item) => (
                <li key={item.id}>
                  <Link to={`/learn/${item.slug}`} className="text-sm text-slate-700 hover:text-brand">
                    {item.title}
                  </Link>
                  <p className="mt-0.5 text-xs text-slate-400">{item.readMinutes} min read</p>
                </li>
              ))}
            </ul>
          </div>
        </aside>
      </div>
    </div>
  );
}
