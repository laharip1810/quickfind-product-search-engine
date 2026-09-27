import { Link } from 'react-router-dom';
import ProductCard from '../components/ProductCard.jsx';
import SkeletonGrid from '../components/SkeletonGrid.jsx';
import StateMessage from '../components/StateMessage.jsx';
import useAsync from '../hooks/useAsync.js';
import { api } from '../api/quickfind.js';
import { showToast } from '../hooks/toast.js';
import { formatDateTime } from '../utils/format.js';

const EVENT_LABELS = { PRODUCT_VIEW: 'Viewed', SEARCH: 'Searched', ADD_TO_CART: 'Added to cart', WISHLIST: 'Wishlisted' };

export default function WishlistPage() {
  const wishlist = useAsync(() => api.wishlist(), []);
  const recent = useAsync(() => api.recentActivity(15), []);

  async function remove(productId) {
    try {
      await api.removeFromWishlist(productId);
      showToast('Removed from wishlist');
      wishlist.reload();
      recent.reload();
    } catch (e) {
      showToast(e.message, 'error');
    }
  }

  return (
    <div className="wishlist-layout">
      <section>
        <div className="section-head">
          <h1>Your wishlist</h1>
          <span className="muted">Signed in as the demo shopper</span>
        </div>
        {wishlist.loading && <SkeletonGrid count={4} />}
        {wishlist.error && <StateMessage tone="error" title="Couldn't load your wishlist">{wishlist.error.message}</StateMessage>}
        {wishlist.data && wishlist.data.length === 0 && (
          <StateMessage title="Your wishlist is empty" action={<Link className="btn" to="/search">Browse products</Link>}>
            Tap the heart on any product to save it here.
          </StateMessage>
        )}
        {wishlist.data && wishlist.data.length > 0 && (
          <div className="grid">
            {wishlist.data.map((p) => (
              <ProductCard key={p.id} product={p} footer={<button className="btn btn-ghost btn-block" onClick={() => remove(p.id)}>Remove</button>} />
            ))}
          </div>
        )}
      </section>

      <aside className="activity">
        <h2>Recent activity</h2>
        <p className="muted">These events feed trending products and recommendations.</p>
        {recent.loading && <div className="skeleton skeleton-filters" />}
        {recent.data && recent.data.length === 0 && <p className="muted">No activity yet.</p>}
        {recent.data && (
          <ol className="activity-list">
            {recent.data.map((e) => (
              <li key={e.id}>
                <span className={`activity-type type-${e.eventType.toLowerCase()}`}>{EVENT_LABELS[e.eventType] || e.eventType}</span>
                {e.productId
                  ? <Link to={`/products/${e.productId}`}>{e.productName}</Link>
                  : <Link to={`/search?query=${encodeURIComponent(e.query)}`}>“{e.query}”</Link>}
                <time>{formatDateTime(e.createdAt)}</time>
              </li>
            ))}
          </ol>
        )}
      </aside>
    </div>
  );
}
