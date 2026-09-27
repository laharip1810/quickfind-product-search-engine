import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import ProductArt from '../components/ProductArt.jsx';
import Rating from '../components/Rating.jsx';
import Price from '../components/Price.jsx';
import StockBadge from '../components/StockBadge.jsx';
import RecommendationList from '../components/RecommendationList.jsx';
import SkeletonGrid from '../components/SkeletonGrid.jsx';
import StateMessage from '../components/StateMessage.jsx';
import useAsync from '../hooks/useAsync.js';
import useReferenceData from '../hooks/useReferenceData.js';
import { api, track } from '../api/quickfind.js';
import { showToast } from '../hooks/toast.js';

// React StrictMode runs effects twice in development; don't count that as two views.
const recentViews = new Map();
function trackViewOnce(productId) {
  const now = Date.now();
  if (now - (recentViews.get(productId) || 0) < 2000) return;
  recentViews.set(productId, now);
  track('PRODUCT_VIEW', { productId });
}

export default function ProductPage() {
  const { id } = useParams();
  const { data: reference } = useReferenceData();
  const userId = reference?.demoUser?.id;
  const product = useAsync(() => api.getProduct(id), [id]);
  const [size, setSize] = useState('');
  const [wishlisted, setWishlisted] = useState(false);

  // One PRODUCT_VIEW per product page visit; it feeds trending and co-interaction recommendations.
  useEffect(() => {
    trackViewOnce(Number(id));
    setSize('');
    api.wishlist().then((items) => setWishlisted(items.some((p) => p.id === Number(id)))).catch(() => {});
  }, [id]);

  if (product.loading) {
    return <div className="detail detail-loading"><div className="skeleton skeleton-detail" /><div className="skeleton skeleton-detail" /></div>;
  }
  if (product.error) {
    return (
      <StateMessage
        tone={product.error.status === 404 ? 'empty' : 'error'}
        title={product.error.status === 404 ? 'Product not found' : 'Could not load this product'}
        action={<Link className="btn" to="/search">Back to search</Link>}
      >
        {product.error.message}
      </StateMessage>
    );
  }

  const p = product.data;

  async function toggleWishlist() {
    try {
      if (wishlisted) {
        await api.removeFromWishlist(p.id);
        setWishlisted(false);
        showToast('Removed from wishlist');
      } else {
        await api.recordInteraction({ eventType: 'WISHLIST', productId: p.id });
        setWishlisted(true);
        showToast('Saved to wishlist', 'success');
      }
    } catch (e) {
      showToast(e.message, 'error');
    }
  }

  function addToCart() {
    if (p.sizes.length > 1 && !size) {
      showToast('Choose a size first', 'error');
      return;
    }
    track('ADD_TO_CART', { productId: p.id });
    showToast('Added to cart (demo: recorded as an interaction)', 'success');
  }

  return (
    <>
      <nav className="breadcrumbs" aria-label="Breadcrumb">
        <Link to="/">Home</Link>
        <span>/</span>
        <Link to={`/search?category=${encodeURIComponent(p.category)}`}>{p.category}</Link>
        <span>/</span>
        <Link to={`/search?category=${encodeURIComponent(p.subcategory)}`}>{p.subcategory}</Link>
      </nav>

      <div className="detail">
        <ProductArt product={p} size="large" />
        <div className="detail-info">
          <Link className="detail-brand" to={`/search?brand=${encodeURIComponent(p.brand)}`}>{p.brand}</Link>
          <h1>{p.name}</h1>
          <Rating value={p.rating} count={p.reviewCount} />
          <Price product={p} large />
          <StockBadge product={p} />

          <div className="detail-block">
            <span className="filter-label">Colors</span>
            <div className="detail-colors">
              {p.colors.map((c) => (
                <span key={c.id} className="detail-color"><span className="swatch" style={{ background: c.hexCode }} />{c.name}</span>
              ))}
            </div>
          </div>

          {p.sizes.length > 0 && (
            <div className="detail-block">
              <span className="filter-label">Size</span>
              <div className="size-options" role="radiogroup" aria-label="Size">
                {p.sizes.map((s) => (
                  <button
                    key={s} type="button" role="radio" aria-checked={size === s}
                    className={`size-option ${size === s ? 'selected' : ''}`}
                    onClick={() => setSize(s)}
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>
          )}

          <div className="detail-actions">
            <button className="btn btn-primary btn-lg" disabled={!p.inStock} onClick={addToCart}>
              {p.inStock ? 'Add to cart' : 'Currently unavailable'}
            </button>
            <button className={`btn btn-lg ${wishlisted ? 'btn-selected' : ''}`} onClick={toggleWishlist} aria-pressed={wishlisted}>
              {wishlisted ? '♥ Wishlisted' : '♡ Wishlist'}
            </button>
          </div>

          <p className="detail-description">{p.description}</p>
        </div>
      </div>

      <Recommendations productId={p.id} userId={userId} />
    </>
  );
}

function Recommendations({ productId, userId }) {
  const reco = useAsync(() => api.recommendations(productId, userId, 8), [productId, userId]);
  return (
    <section className="section">
      <div className="section-head">
        <h2>You may also like</h2>
        <span className="muted">Ranked by category, brand, price, co-views, popularity and rating</span>
      </div>
      {reco.loading && <SkeletonGrid count={4} />}
      {reco.error && (
        <StateMessage tone="error" title="Recommendations are unavailable" action={<button className="btn" onClick={reco.reload}>Try again</button>}>
          {reco.error.message}
        </StateMessage>
      )}
      {reco.data && reco.data.recommendations.length === 0 && <StateMessage title="No recommendations yet" />}
      {reco.data && reco.data.recommendations.length > 0 && <RecommendationList items={reco.data.recommendations} />}
    </section>
  );
}
