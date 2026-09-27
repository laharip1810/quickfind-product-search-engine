export default function SkeletonGrid({ count = 8 }) {
  return (
    <div className="grid" aria-busy="true" aria-label="Loading products">
      {Array.from({ length: count }, (_, i) => (
        <div className="card skeleton-card" key={i}>
          <div className="skeleton skeleton-art" />
          <div className="card-body">
            <div className="skeleton skeleton-line short" />
            <div className="skeleton skeleton-line" />
            <div className="skeleton skeleton-line medium" />
            <div className="skeleton skeleton-line short" />
          </div>
        </div>
      ))}
    </div>
  );
}
