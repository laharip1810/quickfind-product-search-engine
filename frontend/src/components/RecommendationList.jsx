import ProductCard from './ProductCard.jsx';

export default function RecommendationList({ items }) {
  return (
    <div className="grid grid-reco">
      {items.map((item) => (
        <ProductCard
          key={item.product.id}
          product={item.product}
          footer={(
            <div className="reco-why">
              <div className="reco-score" title="Weighted score: 1.0 is a perfect content match (+0.1 personal bonus)">
                <span className="reco-bar"><span style={{ width: `${Math.min(100, item.score * 100)}%` }} /></span>
                <span>Score {item.score.toFixed(2)}</span>
              </div>
              <ul className="reco-reasons">
                {item.reasons.slice(0, 3).map((r) => <li key={r}>{r}</li>)}
              </ul>
            </div>
          )}
        />
      ))}
    </div>
  );
}
