import { Link, NavLink } from 'react-router-dom';
import SearchBar from './SearchBar.jsx';

export default function Header() {
  return (
    <header className="header">
      <div className="header-inner">
        <Link to="/" className="logo" aria-label="QuickFind home">
          <svg viewBox="0 0 32 32" width="28" height="28" aria-hidden="true">
            <rect width="32" height="32" rx="8" fill="currentColor" />
            <circle cx="14" cy="14" r="7" fill="none" stroke="#fff" strokeWidth="3" />
            <path d="M19 19l6 6" stroke="#fff" strokeWidth="3" strokeLinecap="round" />
          </svg>
          <span>QuickFind</span>
        </Link>
        <div className="header-search">
          <SearchBar compact />
        </div>
        <nav className="nav">
          <NavLink to="/search">Browse</NavLink>
          <NavLink to="/wishlist">Wishlist</NavLink>
          <NavLink to="/admin">Admin</NavLink>
        </nav>
      </div>
    </header>
  );
}
