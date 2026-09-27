import { Link } from 'react-router-dom';
import ProductArt from './ProductArt.jsx';
import Rating from './Rating.jsx';
import Price from './Price.jsx';
import StockBadge from './StockBadge.jsx';
import ColorSwatches from './ColorSwatches.jsx';

export default function ProductCard({ product, footer }) {
  return (
    <article className={`card ${product.inStock ? '' : 'card-muted'}`}>
      <Link to={`/products/${product.id}`} className="card-link">
        <ProductArt product={product} />
        <div className="card-body">
          <div className="card-meta">
            <span>{product.brand}</span>
            <span className="dot" aria-hidden="true">·</span>
            <span>{product.subcategory}</span>
          </div>
          <h3 className="card-title">{product.name}</h3>
          <Rating value={product.rating} count={product.reviewCount} />
          <Price product={product} />
          <div className="card-row">
            <StockBadge product={product} />
            <ColorSwatches colors={product.colors} />
          </div>
        </div>
      </Link>
      {footer && <div className="card-footer">{footer}</div>}
    </article>
  );
}
