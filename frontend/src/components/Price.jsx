import { formatPrice } from '../utils/format.js';

export default function Price({ product, large = false }) {
  const discount = Number(product.discountPercent || 0);
  return (
    <div className={`price ${large ? 'price-large' : ''}`}>
      <span className="price-sale">{formatPrice(product.salePrice)}</span>
      {discount > 0 && (
        <>
          <span className="price-mrp">{formatPrice(product.price)}</span>
          <span className="price-discount">{Math.round(discount)}% off</span>
        </>
      )}
    </div>
  );
}
