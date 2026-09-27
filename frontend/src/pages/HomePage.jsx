import { Link } from 'react-router-dom';
import SearchBar from '../components/SearchBar.jsx';
import ProductCard from '../components/ProductCard.jsx';
import SkeletonGrid from '../components/SkeletonGrid.jsx';
import StateMessage from '../components/StateMessage.jsx';
import useAsync from '../hooks/useAsync.js';
import useReferenceData from '../hooks/useReferenceData.js';
import { api, track } from '../api/quickfind.js';

const POPULAR = ['black running shoes', 'running socks', 'smart watch', 'laptop backpack', 'denim jacket', 'gym t-shirt'];

export default function HomePage() {
  const trending = useAsync(() => api.trending(8), []);
  const { data: reference } = useReferenceData();

  return (
    <>
      <section className="hero">
        <p className="eyebrow">Product discovery</p>
        <h1>Find the right product in a few keystrokes.</h1>
        <p className="hero-sub">
          Relevance-ranked search, instant suggestions and recommendations that say why they were picked.
        </p>
        <div className="hero-search">
          <SearchBar autoFocus />
        </div>
        <div className="chips" aria-label="Popular searches">
          {POPULAR.map((q) => (
            <Link key={q} className="chip" to={`/search?query=${encodeURIComponent(q)}`} onClick={() => track('SEARCH', { query: q })}>
              {q}
            </Link>
          ))}
        </div>
      </section>

      {reference && (
        <section className="section">
          <div className="section-head">
            <h2>Shop by category</h2>
          </div>
          <div className="category-tiles">
            {reference.categories.flatMap((top) => top.subcategories.map((sub) => (
              <Link key={sub.id} className="category-tile" to={`/search?category=${encodeURIComponent(sub.name)}`}>
                <span className="category-parent">{top.name}</span>
                <span className="category-name">{sub.name}</span>
              </Link>
            )))}
          </div>
        </section>
      )}

      <section className="section">
        <div className="section-head">
          <h2>Trending now</h2>
          <span className="muted">Most engagement in the last 7 days</span>
        </div>
        {trending.loading && <SkeletonGrid count={8} />}
        {trending.error && (
          <StateMessage tone="error" title="Couldn't load trending products" action={<button className="btn" onClick={trending.reload}>Try again</button>}>
            {trending.error.message}
          </StateMessage>
        )}
        {trending.data && (
          <div className="grid">
            {trending.data.map((p) => <ProductCard key={p.id} product={p} />)}
          </div>
        )}
      </section>
    </>
  );
}
