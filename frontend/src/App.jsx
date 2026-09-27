import { Route, Routes } from 'react-router-dom';
import Header from './components/Header.jsx';
import ToastHost from './components/ToastHost.jsx';
import HomePage from './pages/HomePage.jsx';
import SearchPage from './pages/SearchPage.jsx';
import ProductPage from './pages/ProductPage.jsx';
import WishlistPage from './pages/WishlistPage.jsx';
import AdminPage from './pages/AdminPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';

export default function App() {
  return (
    <>
      <Header />
      <main className="page">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/products/:id" element={<ProductPage />} />
          <Route path="/wishlist" element={<WishlistPage />} />
          <Route path="/admin" element={<AdminPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
      <footer className="footer">
        <span>QuickFind demo · Spring Boot, MySQL, Redis, React</span>
        <span>Prices in INR · Product images are generated illustrations</span>
      </footer>
      <ToastHost />
    </>
  );
}
