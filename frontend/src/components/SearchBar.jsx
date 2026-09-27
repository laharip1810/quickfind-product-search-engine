import { useEffect, useId, useRef, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { api, track } from '../api/quickfind.js';
import useDebouncedValue from '../hooks/useDebouncedValue.js';

const TYPE_LABELS = { PRODUCT: 'Product', CATEGORY: 'Category', BRAND: 'Brand', QUERY: 'Popular', PHRASE: '' };

/**
 * Search input with Trie-backed autocomplete: debounced requests, arrow-key navigation,
 * Enter to search, Escape to close. Choosing a product suggestion opens the product;
 * anything else runs a search.
 */
export default function SearchBar({ compact = false, autoFocus = false }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [params] = useSearchParams();
  const listId = useId();
  const [text, setText] = useState(location.pathname === '/search' ? params.get('query') || '' : '');
  const [suggestions, setSuggestions] = useState([]);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const [loading, setLoading] = useState(false);
  const debounced = useDebouncedValue(text, 150);
  const wrapperRef = useRef(null);

  // Keep the box in sync when the URL query changes (back/forward, chips).
  useEffect(() => {
    if (location.pathname === '/search') setText(params.get('query') || '');
  }, [location.pathname, params]);

  useEffect(() => {
    const prefix = debounced.trim();
    if (prefix.length < 1) {
      setSuggestions([]);
      return undefined;
    }
    const controller = new AbortController();
    setLoading(true);
    api.suggestions(debounced, 8, controller.signal)
      .then((data) => {
        setSuggestions(data.suggestions);
        setActive(-1);
      })
      .catch(() => setSuggestions([]))
      .finally(() => setLoading(false));
    return () => controller.abort();
  }, [debounced]);

  useEffect(() => {
    const close = (event) => {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target)) setOpen(false);
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  function runSearch(query) {
    const q = query.trim();
    setOpen(false);
    if (!q) {
      navigate('/search');
      return;
    }
    track('SEARCH', { query: q });
    navigate(`/search?query=${encodeURIComponent(q)}`);
  }

  function choose(suggestion) {
    if (suggestion.type === 'PRODUCT' && suggestion.productId) {
      setOpen(false);
      setText(suggestion.text);
      navigate(`/products/${suggestion.productId}`);
    } else {
      setText(suggestion.text);
      runSearch(suggestion.text);
    }
  }

  function onKeyDown(event) {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setOpen(true);
      setActive((i) => Math.min(i + 1, suggestions.length - 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setActive((i) => Math.max(i - 1, -1));
    } else if (event.key === 'Enter') {
      event.preventDefault();
      if (open && active >= 0 && suggestions[active]) choose(suggestions[active]);
      else runSearch(text);
    } else if (event.key === 'Escape') {
      setOpen(false);
    }
  }

  const showList = open && text.trim().length > 0 && (suggestions.length > 0 || !loading);

  return (
    <div className={`searchbar ${compact ? 'searchbar-compact' : ''}`} ref={wrapperRef}>
      <form
        role="search"
        onSubmit={(e) => {
          e.preventDefault();
          runSearch(text);
        }}
      >
        <svg className="searchbar-icon" viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="11" cy="11" r="7" fill="none" stroke="currentColor" strokeWidth="2" />
          <path d="M16.5 16.5L21 21" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        </svg>
        <input
          type="search"
          value={text}
          autoFocus={autoFocus}
          placeholder="Search for running shoes, watches, backpacks…"
          aria-label="Search products"
          role="combobox"
          aria-expanded={showList}
          aria-controls={listId}
          aria-autocomplete="list"
          aria-activedescendant={active >= 0 ? `${listId}-${active}` : undefined}
          onChange={(e) => {
            setText(e.target.value);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={onKeyDown}
          maxLength={200}
        />
        {loading && <span className="spinner" aria-hidden="true" />}
        <button type="submit" className="btn btn-primary searchbar-button">Search</button>
      </form>
      {showList && (
        <ul className="suggestions" id={listId} role="listbox">
          {suggestions.length === 0 && <li className="suggestion-empty">No suggestions. Press Enter to search.</li>}
          {suggestions.map((s, i) => (
            <li
              key={`${s.type}-${s.text}`}
              id={`${listId}-${i}`}
              role="option"
              aria-selected={i === active}
              className={`suggestion ${i === active ? 'active' : ''}`}
              onMouseDown={(e) => {
                e.preventDefault();
                choose(s);
              }}
              onMouseEnter={() => setActive(i)}
            >
              <Highlight text={s.text} prefix={text.trim()} />
              {TYPE_LABELS[s.type] && <span className={`suggestion-type type-${s.type.toLowerCase()}`}>{TYPE_LABELS[s.type]}</span>}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function Highlight({ text, prefix }) {
  if (prefix && text.toLowerCase().startsWith(prefix.toLowerCase())) {
    return (
      <span className="suggestion-text">
        <strong>{text.slice(0, prefix.length)}</strong>
        {text.slice(prefix.length)}
      </span>
    );
  }
  return <span className="suggestion-text">{text}</span>;
}
