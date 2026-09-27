export default function Rating({ value, count }) {
  const rating = Number(value || 0);
  return (
    <span className="rating" aria-label={`Rated ${rating} out of 5`}>
      <span className="rating-stars" style={{ '--fill': `${(rating / 5) * 100}%` }} aria-hidden="true">★★★★★</span>
      <span className="rating-value">{rating.toFixed(1)}</span>
      {count !== undefined && <span className="rating-count">({new Intl.NumberFormat('en-IN').format(count)})</span>}
    </span>
  );
}
