/** Numbered pagination with a window around the current page (pages are 0-based in the API). */
export default function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;
  const pages = [];
  const start = Math.max(0, Math.min(page - 2, totalPages - 5));
  const end = Math.min(totalPages, start + 5);
  for (let p = start; p < end; p++) pages.push(p);

  return (
    <nav className="pagination" aria-label="Pagination">
      <button className="btn btn-ghost" disabled={page === 0} onClick={() => onChange(page - 1)}>‹ Prev</button>
      {start > 0 && (
        <>
          <button className="page-btn" onClick={() => onChange(0)}>1</button>
          {start > 1 && <span className="page-gap">…</span>}
        </>
      )}
      {pages.map((p) => (
        <button
          key={p}
          className={`page-btn ${p === page ? 'current' : ''}`}
          aria-current={p === page ? 'page' : undefined}
          onClick={() => onChange(p)}
        >
          {p + 1}
        </button>
      ))}
      {end < totalPages && (
        <>
          {end < totalPages - 1 && <span className="page-gap">…</span>}
          <button className="page-btn" onClick={() => onChange(totalPages - 1)}>{totalPages}</button>
        </>
      )}
      <button className="btn btn-ghost" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>Next ›</button>
    </nav>
  );
}
