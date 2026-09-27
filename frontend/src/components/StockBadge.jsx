export default function StockBadge({ product }) {
  if (!product.inStock) return <span className="badge badge-out">Out of stock</span>;
  if (product.stockQuantity <= 10) return <span className="badge badge-low">Only {product.stockQuantity} left</span>;
  return <span className="badge badge-in">In stock</span>;
}
