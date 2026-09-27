import { useCallback, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import FilterSidebar from '../components/FilterSidebar.jsx';
import ProductCard from '../components/ProductCard.jsx';
import SkeletonGrid from '../components/SkeletonGrid.jsx';
import StateMessage from '../components/StateMessage.jsx';
import Pagination from '../components/Pagination.jsx';
import useAsync from '../hooks/useAsync.js';
import useReferenceData from '../hooks/useReferenceData.js';
import { api } from '../api/quickfind.js';

const PAGE_SIZE = 12;
const SORTS = [
  { value: 'relevance', label: 'Relevance' },
  { value: 'price_asc', label: 'Price: low to high' },
  { value: 'price_desc', label: 'Price: high to low' },
  { value: 'rating', label: 'Customer rating' },
  { value: 'popularity', label: 'Popularity' },
];
const FILTER_KEYS = ['category', 'color', 'productSize', 'minPrice', 'maxPrice', 'minRating', 'inStock'];

/** All search state lives in the URL, so results are linkable and back/forward works. */
export default function SearchPage() {
  const [params, setParams] = useSearchParams();
  const { data: reference } = useReferenceData();
  const [filtersOpen, setFiltersOpen] = useState(false);

  const filters = useMemo(() => {
    const f = { query: params.get('query') || '', brand: params.getAll('brand'), sort: params.get('sort') || 'relevance' };
    FILTER_KEYS.forEach((k) => { f[k] = params.get(k) || ''; });
    f.page = Number(params.get('page') || 0);
    return f;
  }, [params]);

  const result = useAsync(
    () => api.searchProducts({ ...filters, size: PAGE_SIZE }),
    [params.toString()],
  );

  const update = useCallback((changes, resetPage = true) => {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([key, value]) => {
      next.delete(key);
      if (Array.isArray(value)) value.forEach((v) => next.append(key, v));
      else if (value !== '' && value !== null && value !== undefined) next.set(key, value);
    });
    if (resetPage) next.delete('page');
    setParams(next);
  }, [params, setParams]);

  const clearFilters = () => {
    const next = new URLSearchParams();
    if (filters.query) next.set('query', filters.query);
    setParams(next);
  };

  const changePage = (page) => {
    update({ page: page === 0 ? '' : String(page) }, false);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const data = result.data;
  const activeChips = buildChips(filters);

  return (
    <div className="search-layout">
      <FilterSidebar filters={filters} reference={reference} onChange={update} onClear={clearFilters} open={filtersOpen} />

      <section className="results" aria-live="polite">
        <button className="btn filters-toggle" onClick={() => setFiltersOpen((o) => !o)} aria-expanded={filtersOpen}>
          {filtersOpen ? 'Hide filters' : `Filters${activeChips.length ? ` (${activeChips.length})` : ''}`}
        </button>
        <div className="results-head">
          <div>
            <h1 className="results-title">
              {filters.query ? <>Results for “{filters.query}”</> : filters.category || 'All products'}
            </h1>
            {data && (
              <p className="muted">
                {data.totalElements.toLocaleString('en-IN')} product{data.totalElements === 1 ? '' : 's'}
                {data.tookMs !== undefined && <> · {data.tookMs} ms</>}
                {data.normalizedTokens?.length > 0 && <> · searched for <code>{data.normalizedTokens.join(' ')}</code></>}
              </p>
            )}
          </div>
          <label className="sort">
            <span>Sort by</span>
            <select value={filters.sort} onChange={(e) => update({ sort: e.target.value === 'relevance' ? '' : e.target.value })}>
              {SORTS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
            </select>
          </label>
        </div>

        {activeChips.length > 0 && (
          <div className="active-filters">
            {activeChips.map((chip) => (
              <button key={chip.label} className="chip chip-removable" onClick={() => update(chip.remove)}>
                {chip.label} <span aria-hidden="true">×</span>
              </button>
            ))}
          </div>
        )}

        {data?.matchMode === 'ANY_TERM' && (
          <div className="notice">No product matched every word, so these results match some of them.</div>
        )}
        {data?.candidatesTruncated && (
          <div className="notice">Many products matched. Results were ranked from the most popular candidates; add a filter to narrow down.</div>
        )}

        {result.loading && <SkeletonGrid count={PAGE_SIZE} />}

        {!result.loading && result.error && (
          <StateMessage tone="error" title="Search failed" action={<button className="btn" onClick={result.reload}>Try again</button>}>
            {result.error.message}
          </StateMessage>
        )}

        {!result.loading && data && data.content.length === 0 && (
          <StateMessage title="No products found" action={activeChips.length > 0 && <button className="btn" onClick={clearFilters}>Clear filters</button>}>
            Try a different spelling, a more general term, or fewer filters.
          </StateMessage>
        )}

        {!result.loading && data && data.content.length > 0 && (
          <>
            <div className="grid">
              {data.content.map((hit) => <ProductCard key={hit.product.id} product={hit.product} />)}
            </div>
            <Pagination page={data.page} totalPages={data.totalPages} onChange={changePage} />
          </>
        )}
      </section>
    </div>
  );
}

function buildChips(f) {
  const chips = [];
  if (f.category) chips.push({ label: f.category, remove: { category: '' } });
  f.brand.forEach((b) => chips.push({ label: b, remove: { brand: f.brand.filter((x) => x !== b) } }));
  if (f.color) chips.push({ label: f.color, remove: { color: '' } });
  if (f.productSize) chips.push({ label: `Size ${f.productSize}`, remove: { productSize: '' } });
  if (f.minPrice || f.maxPrice) chips.push({ label: `₹${f.minPrice || 0} – ${f.maxPrice ? `₹${f.maxPrice}` : 'any'}`, remove: { minPrice: '', maxPrice: '' } });
  if (f.minRating) chips.push({ label: `${f.minRating}★ & up`, remove: { minRating: '' } });
  if (f.inStock === 'true') chips.push({ label: 'In stock', remove: { inStock: '' } });
  return chips;
}
