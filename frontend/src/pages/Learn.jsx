import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { get } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { Empty, ErrorBox, Loading, PageTitle, Pagination } from '../components/Ui';
import { title } from '../lib/format';

export default function Learn() {
  const [keyword, setKeyword] = useState('');
  const [debounced, setDebounced] = useState('');
  const [category, setCategory] = useState('');
  const [page, setPage] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => { setDebounced(keyword); setPage(0); }, 400);
    return () => clearTimeout(timer);
  }, [keyword]);

  const { data: meta } = useFetch(() => get('/api/v1/metadata'), []);
  const { data: popular } = useFetch(() => get('/api/v1/education/popular'), []);
  const { data, loading, error, reload } = useFetch(
    () => get('/api/v1/education', {
      keyword: debounced || undefined, category: category || undefined, page, size: 9,
    }),
    [debounced, category, page],
  );

  return (
    <div className="page">
      <PageTitle title="Educational resources"
                 subtitle="Short, specific pieces on the decisions that actually change outcomes." />

      <div className="grid gap-6 lg:grid-cols-[1fr_260px]">
        <div>
          <div className="card mb-5">
            <input type="search" value={keyword} className="input" placeholder="Search articles…"
                   onChange={(e) => setKeyword(e.target.value)} aria-label="Search articles" />

            <div className="mt-3 flex flex-wrap gap-2">
              <button type="button" onClick={() => { setCategory(''); setPage(0); }}
                      className={`badge ${category === '' ? 'bg-brand text-white' : 'bg-slate-100 text-slate-600'}`}>
                All topics
              </button>
              {meta?.articleCategories?.map((option) => (
                <button key={option} type="button"
                        onClick={() => { setCategory(option); setPage(0); }}
                        className={`badge ${category === option ? 'bg-brand text-white' : 'bg-slate-100 text-slate-600'}`}>
                  {title(option)}
                </button>
              ))}
            </div>
          </div>

          {loading ? <Loading label="Loading articles…" />
            : error ? <ErrorBox error={error} onRetry={reload} />
            : !data?.content?.length ? <Empty title="No articles found" />
            : (
              <>
                <div className="grid gap-5 sm:grid-cols-2">
                  {data.content.map((article) => (
                    <article key={article.id} className="card flex flex-col">
                      <div className="flex items-center justify-between gap-2">
                        <span className="badge bg-teal-100 text-teal-800">{title(article.category)}</span>
                        {article.premiumOnly && (
                          <span className="badge bg-amber-100 text-amber-800">Premium</span>
                        )}
                      </div>
                      <h2 className="mt-3 font-semibold">
                        <Link to={`/learn/${article.slug}`} className="text-slate-900 hover:text-brand">
                          {article.title}
                        </Link>
                      </h2>
                      <p className="mt-2 line-clamp-3 flex-1 text-sm text-slate-600">{article.summary}</p>
                      <p className="mt-3 border-t border-slate-100 pt-2 text-xs text-slate-500">
                        {article.readMinutes} min read · {article.viewCount} views
                      </p>
                    </article>
                  ))}
                </div>

                <Pagination page={data.pageNumber} totalPages={data.totalPages} onChange={setPage} />
              </>
            )}
        </div>

        <aside className="space-y-5">
          <div className="card">
            <h2 className="text-xs font-bold uppercase text-slate-500">Most read</h2>
            <ol className="mt-3 space-y-3">
              {popular?.length ? popular.map((article, index) => (
                <li key={article.id} className="flex gap-2 text-sm">
                  <span className="font-bold text-slate-300">{index + 1}</span>
                  <Link to={`/learn/${article.slug}`} className="text-slate-700 hover:text-brand">
                    {article.title}
                  </Link>
                </li>
              )) : <li className="text-sm text-slate-400">Nothing here yet.</li>}
            </ol>
          </div>

          <div className="card">
            <h2 className="font-semibold text-slate-900">Ready to put it into practice?</h2>
            <p className="mt-2 text-sm text-slate-600">
              Set your first goal and complete the risk questionnaire. Both are free.
            </p>
            <Link to="/register" className="btn-primary mt-3 w-full">Create an account</Link>
          </div>
        </aside>
      </div>
    </div>
  );
}
