import { useState } from 'react';
import { Link } from 'react-router-dom';
import ProductFormModal from '../components/ProductFormModal.jsx';
import Pagination from '../components/Pagination.jsx';
import StateMessage from '../components/StateMessage.jsx';
import useAsync from '../hooks/useAsync.js';
import useDebouncedValue from '../hooks/useDebouncedValue.js';
import useReferenceData from '../hooks/useReferenceData.js';
import { api } from '../api/quickfind.js';
import { showToast } from '../hooks/toast.js';
import { formatPrice } from '../utils/format.js';

const PAGE_SIZE = 15;

/**
 * Catalog management: add, edit, delete and restock. The demo has no login; the backend
 * keeps every write behind /api/products POST/PUT/PATCH/DELETE so role-based security can
 * be added in one place.
 */
export default function AdminPage() {
  const { data: reference } = useReferenceData();
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState(null); // null | 'new' | product id
  const debouncedQuery = useDebouncedValue(query.trim(), 300);

  const products = useAsync(
    () => (debouncedQuery
      ? api.searchProducts({ query: debouncedQuery, page, size: PAGE_SIZE }).then((r) => ({
        ...r, content: r.content.map((hit) => hit.product),
      }))
      : api.listProducts({ page, size: PAGE_SIZE, sort: 'popularity' })),
    [debouncedQuery, page],
  );

  const onSaved = (saved, isNew) => {
    setEditing(null);
    showToast(isNew ? `Created “${saved.name}”` : `Saved “${saved.name}”`, 'success');
    products.reload();
  };

  return (
    <div className="admin">
      <div className="admin-head">
        <div>
          <h1>Catalog admin</h1>
          <p className="muted">Changes invalidate the Redis cache and schedule an autocomplete rebuild.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setEditing('new')} disabled={!reference}>+ Add product</button>
      </div>

      <input
        className="admin-search" type="search" placeholder="Filter by name, brand or category…" aria-label="Filter products"
        value={query}
        onChange={(e) => {
          setQuery(e.target.value);
          setPage(0);
        }}
      />

      {products.error && (
        <StateMessage tone="error" title="Couldn't load products" action={<button className="btn" onClick={products.reload}>Try again</button>}>
          {products.error.message}
        </StateMessage>
      )}

      {!products.error && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Product</th>
                <th>Subcategory</th>
                <th className="num">Price</th>
                <th className="num">Discount</th>
                <th className="num">Sale price</th>
                <th className="num">Stock</th>
                <th aria-label="Actions" />
              </tr>
            </thead>
            <tbody>
              {products.loading && Array.from({ length: 6 }, (_, i) => (
                <tr key={`s${i}`}><td colSpan={7}><div className="skeleton skeleton-line" /></td></tr>
              ))}
              {!products.loading && products.data?.content.length === 0 && (
                <tr><td colSpan={7} className="muted center">No products match.</td></tr>
              )}
              {!products.loading && products.data?.content.map((p) => (
                <AdminRow key={p.id} product={p} onEdit={() => setEditing(p.id)} onChanged={products.reload} />
              ))}
            </tbody>
          </table>
        </div>
      )}

      {products.data && <Pagination page={products.data.page} totalPages={products.data.totalPages} onChange={setPage} />}

      {editing !== null && reference && (
        <ProductFormModal
          productId={editing === 'new' ? null : editing}
          reference={reference}
          onClose={() => setEditing(null)}
          onSaved={onSaved}
        />
      )}
    </div>
  );
}

function AdminRow({ product, onEdit, onChanged }) {
  const [stock, setStock] = useState(String(product.stockQuantity));
  const [saving, setSaving] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const stockChanged = stock !== String(product.stockQuantity);

  async function saveStock() {
    const value = Number(stock);
    if (!Number.isInteger(value) || value < 0) {
      showToast('Stock must be a whole number of 0 or more', 'error');
      return;
    }
    setSaving(true);
    try {
      await api.updateStock(product.id, value);
      showToast(`Stock for “${product.name}” set to ${value}`, 'success');
      onChanged();
    } catch (e) {
      showToast(e.message, 'error');
    } finally {
      setSaving(false);
    }
  }

  async function remove() {
    try {
      await api.deleteProduct(product.id);
      showToast(`Deleted “${product.name}”`);
      onChanged();
    } catch (e) {
      showToast(e.message, 'error');
      setConfirming(false);
    }
  }

  return (
    <tr className={product.inStock ? '' : 'row-muted'}>
      <td>
        <Link to={`/products/${product.id}`} className="table-name">{product.name}</Link>
        <div className="muted small">{product.brand}</div>
      </td>
      <td>{product.subcategory}</td>
      <td className="num">{formatPrice(product.price)}</td>
      <td className="num">{Number(product.discountPercent)}%</td>
      <td className="num">{formatPrice(product.salePrice)}</td>
      <td className="num">
        <div className="stock-edit">
          <input
            type="number" min="0" value={stock} aria-label={`Stock for ${product.name}`}
            onChange={(e) => setStock(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && stockChanged && saveStock()}
          />
          {stockChanged && <button className="btn btn-small btn-primary" onClick={saveStock} disabled={saving}>Save</button>}
        </div>
      </td>
      <td className="actions">
        <button className="btn btn-small" onClick={onEdit}>Edit</button>
        {confirming ? (
          <>
            <button className="btn btn-small btn-danger" onClick={remove}>Confirm</button>
            <button className="btn btn-small btn-ghost" onClick={() => setConfirming(false)}>Cancel</button>
          </>
        ) : (
          <button className="btn btn-small btn-ghost" onClick={() => setConfirming(true)}>Delete</button>
        )}
      </td>
    </tr>
  );
}
