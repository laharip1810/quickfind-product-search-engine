import { useEffect, useState } from 'react';
import { api } from '../api/quickfind.js';
import useDebouncedValue from '../hooks/useDebouncedValue.js';

const RATINGS = [
  { value: '', label: 'Any rating' },
  { value: '4.5', label: '4.5 ★ & up' },
  { value: '4', label: '4 ★ & up' },
  { value: '3', label: '3 ★ & up' },
];

/**
 * Filter sidebar. Every change is written to the URL by the parent, so filters survive
 * reloads and can be shared as links.
 */
export default function FilterSidebar({ filters, reference, onChange, onClear, open = false }) {
  const [minPrice, setMinPrice] = useState(filters.minPrice || '');
  const [maxPrice, setMaxPrice] = useState(filters.maxPrice || '');
  const [brandQuery, setBrandQuery] = useState('');

  useEffect(() => setMinPrice(filters.minPrice || ''), [filters.minPrice]);
  useEffect(() => setMaxPrice(filters.maxPrice || ''), [filters.maxPrice]);

  const priceCount = usePriceRangeCount(filters.category, minPrice, maxPrice);

  if (!reference) {
    return <aside className={`filters ${open ? 'open' : ''}`}><div className="skeleton skeleton-filters" /></aside>;
  }

  const selectedBrands = filters.brand;
  const toggleBrand = (name) => {
    const next = selectedBrands.includes(name)
      ? selectedBrands.filter((b) => b !== name)
      : [...selectedBrands, name];
    onChange({ brand: next });
  };
  const applyPrice = () => onChange({ minPrice, maxPrice });
  const visibleBrands = reference.brands.filter((b) => b.name.toLowerCase().includes(brandQuery.toLowerCase()));

  return (
    <aside className={`filters ${open ? 'open' : ''}`} aria-label="Filters">
      <div className="filters-head">
        <h2>Filters</h2>
        <button className="link-btn" onClick={onClear}>Clear all</button>
      </div>

      <section className="filter-group">
        <label className="filter-label" htmlFor="filter-category">Category</label>
        <select id="filter-category" value={filters.category} onChange={(e) => onChange({ category: e.target.value })}>
          <option value="">All categories</option>
          {reference.categories.map((top) => (
            <optgroup key={top.id} label={top.name}>
              <option value={top.name}>All {top.name}</option>
              {top.subcategories.map((sub) => (
                <option key={sub.id} value={sub.name}>{sub.name}</option>
              ))}
            </optgroup>
          ))}
        </select>
      </section>

      <section className="filter-group">
        <span className="filter-label">Price (₹)</span>
        <div className="price-inputs">
          <input
            type="number" min="0" inputMode="numeric" placeholder="Min" aria-label="Minimum price"
            value={minPrice}
            onChange={(e) => setMinPrice(e.target.value)}
            onBlur={applyPrice}
            onKeyDown={(e) => e.key === 'Enter' && applyPrice()}
          />
          <span aria-hidden="true">–</span>
          <input
            type="number" min="0" inputMode="numeric" placeholder="Max" aria-label="Maximum price"
            value={maxPrice}
            onChange={(e) => setMaxPrice(e.target.value)}
            onBlur={applyPrice}
            onKeyDown={(e) => e.key === 'Enter' && applyPrice()}
          />
        </div>
        {priceCount !== null && (
          <p className="filter-hint">{priceCount} in-stock product{priceCount === 1 ? '' : 's'} in this price range</p>
        )}
      </section>

      <section className="filter-group">
        <span className="filter-label">Brand</span>
        {reference.brands.length > 8 && (
          <input
            className="brand-search" type="search" placeholder="Find a brand" aria-label="Find a brand"
            value={brandQuery} onChange={(e) => setBrandQuery(e.target.value)}
          />
        )}
        <div className="checklist">
          {visibleBrands.map((b) => (
            <label key={b.id} className="check">
              <input type="checkbox" checked={selectedBrands.includes(b.name)} onChange={() => toggleBrand(b.name)} />
              <span>{b.name}</span>
            </label>
          ))}
        </div>
      </section>

      <section className="filter-group">
        <span className="filter-label">Color</span>
        <div className="color-filter">
          {reference.colors.map((c) => (
            <button
              key={c.id}
              type="button"
              className={`color-chip ${filters.color === c.name ? 'selected' : ''}`}
              style={{ '--swatch': c.hexCode }}
              aria-pressed={filters.color === c.name}
              title={c.name}
              onClick={() => onChange({ color: filters.color === c.name ? '' : c.name })}
            >
              <span className="color-dot" />
              <span className="sr-only">{c.name}</span>
            </button>
          ))}
        </div>
      </section>

      <section className="filter-group">
        <label className="filter-label" htmlFor="filter-size">Size</label>
        <select id="filter-size" value={filters.productSize} onChange={(e) => onChange({ productSize: e.target.value })}>
          <option value="">Any size</option>
          {reference.sizes.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
      </section>

      <section className="filter-group">
        <span className="filter-label">Customer rating</span>
        {RATINGS.map((r) => (
          <label key={r.label} className="check">
            <input
              type="radio" name="rating" checked={(filters.minRating || '') === r.value}
              onChange={() => onChange({ minRating: r.value })}
            />
            <span>{r.label}</span>
          </label>
        ))}
      </section>

      <section className="filter-group">
        <span className="filter-label">Availability</span>
        <label className="check">
          <input
            type="checkbox" checked={filters.inStock === 'true'}
            onChange={(e) => onChange({ inStock: e.target.checked ? 'true' : '' })}
          />
          <span>In stock only</span>
        </label>
      </section>
    </aside>
  );
}

/** Live count from the backend's binary-search price index while the user types a range. */
function usePriceRangeCount(category, minPrice, maxPrice) {
  const [count, setCount] = useState(null);
  const min = useDebouncedValue(minPrice, 250);
  const max = useDebouncedValue(maxPrice, 250);
  useEffect(() => {
    if (min === '' && max === '') {
      setCount(null);
      return undefined;
    }
    let active = true;
    api.priceRangeCount({ category, minPrice: min, maxPrice: max })
      .then((data) => active && setCount(data.count))
      .catch(() => active && setCount(null));
    return () => {
      active = false;
    };
  }, [category, min, max]);
  return count;
}
