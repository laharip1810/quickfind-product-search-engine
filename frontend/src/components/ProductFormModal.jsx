import { useEffect, useState } from 'react';
import { api } from '../api/quickfind.js';

const EMPTY = {
  name: '', description: '', brandId: '', categoryId: '', price: '', discountPercent: '0',
  rating: '0', reviewCount: '0', stockQuantity: '0', colorIds: [], sizes: '', imageUrl: '', version: null,
};

/** Create/edit form. Shows the backend's field-level validation errors next to each field. */
export default function ProductFormModal({ productId, reference, onClose, onSaved }) {
  const [form, setForm] = useState(EMPTY);
  const [loading, setLoading] = useState(Boolean(productId));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  useEffect(() => {
    if (!productId) return;
    api.getProduct(productId)
      .then((p) => setForm({
        name: p.name,
        description: p.description,
        brandId: String(p.brandId),
        categoryId: String(p.subcategoryId),
        price: String(p.price),
        discountPercent: String(p.discountPercent),
        rating: String(p.rating),
        reviewCount: String(p.reviewCount),
        stockQuantity: String(p.stockQuantity),
        colorIds: p.colors.map((c) => c.id),
        sizes: p.sizes.join(', '),
        imageUrl: p.imageUrl || '',
        version: p.version,
      }))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [productId]);

  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onClose]);

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));
  const toggleColor = (id) => setForm((f) => ({
    ...f,
    colorIds: f.colorIds.includes(id) ? f.colorIds.filter((c) => c !== id) : [...f.colorIds, id],
  }));

  async function submit(e) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setFieldErrors({});
    const body = {
      name: form.name,
      description: form.description,
      brandId: form.brandId ? Number(form.brandId) : null,
      categoryId: form.categoryId ? Number(form.categoryId) : null,
      price: form.price === '' ? null : Number(form.price),
      discountPercent: Number(form.discountPercent || 0),
      rating: Number(form.rating || 0),
      reviewCount: Number(form.reviewCount || 0),
      stockQuantity: form.stockQuantity === '' ? null : Number(form.stockQuantity),
      colorIds: form.colorIds,
      sizes: form.sizes.split(',').map((s) => s.trim()).filter(Boolean),
      imageUrl: form.imageUrl.trim() || null,
      version: form.version,
    };
    try {
      const saved = productId ? await api.updateProduct(productId, body) : await api.createProduct(body);
      onSaved(saved, !productId);
    } catch (err) {
      setError(err.message);
      const byField = {};
      err.fieldErrors.forEach((fe) => { byField[fe.field] = fe.message; });
      setFieldErrors(byField);
    } finally {
      setSaving(false);
    }
  }

  const field = (key) => fieldErrors[key] && <span className="field-error">{fieldErrors[key]}</span>;

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="product-form-title">
        <div className="modal-head">
          <h2 id="product-form-title">{productId ? 'Edit product' : 'Add product'}</h2>
          <button className="icon-btn" onClick={onClose} aria-label="Close">×</button>
        </div>
        {loading ? <div className="skeleton skeleton-detail" /> : (
          <form className="form" onSubmit={submit} noValidate>
            {error && <div className="form-error" role="alert">{error}</div>}
            <label className="form-field span-2">
              <span>Name</span>
              <input value={form.name} onChange={set('name')} required minLength={3} maxLength={200} />
              {field('name')}
            </label>
            <label className="form-field span-2">
              <span>Description</span>
              <textarea rows={3} value={form.description} onChange={set('description')} maxLength={2000} />
              {field('description')}
            </label>
            <label className="form-field">
              <span>Brand</span>
              <select value={form.brandId} onChange={set('brandId')}>
                <option value="">Select a brand</option>
                {reference.brands.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
              </select>
              {field('brandId')}
            </label>
            <label className="form-field">
              <span>Subcategory</span>
              <select value={form.categoryId} onChange={set('categoryId')}>
                <option value="">Select a subcategory</option>
                {reference.categories.map((top) => (
                  <optgroup key={top.id} label={top.name}>
                    {top.subcategories.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                  </optgroup>
                ))}
              </select>
              {field('categoryId')}
            </label>
            <label className="form-field">
              <span>Price (₹)</span>
              <input type="number" min="1" step="0.01" value={form.price} onChange={set('price')} />
              {field('price')}
            </label>
            <label className="form-field">
              <span>Discount (%)</span>
              <input type="number" min="0" max="90" step="0.01" value={form.discountPercent} onChange={set('discountPercent')} />
              {field('discountPercent')}
            </label>
            <label className="form-field">
              <span>Rating (0–5)</span>
              <input type="number" min="0" max="5" step="0.1" value={form.rating} onChange={set('rating')} />
              {field('rating')}
            </label>
            <label className="form-field">
              <span>Review count</span>
              <input type="number" min="0" value={form.reviewCount} onChange={set('reviewCount')} />
              {field('reviewCount')}
            </label>
            <label className="form-field">
              <span>Stock</span>
              <input type="number" min="0" value={form.stockQuantity} onChange={set('stockQuantity')} />
              {field('stockQuantity')}
            </label>
            <label className="form-field">
              <span>Sizes (comma separated)</span>
              <input value={form.sizes} onChange={set('sizes')} placeholder="UK 7, UK 8, UK 9" />
              {field('sizes')}
            </label>
            <fieldset className="form-field span-2">
              <legend>Colors</legend>
              <div className="color-picker">
                {reference.colors.map((c) => (
                  <label key={c.id} className={`color-option ${form.colorIds.includes(c.id) ? 'selected' : ''}`}>
                    <input type="checkbox" checked={form.colorIds.includes(c.id)} onChange={() => toggleColor(c.id)} />
                    <span className="swatch" style={{ background: c.hexCode }} />
                    {c.name}
                  </label>
                ))}
              </div>
              {field('colorIds')}
            </fieldset>
            <label className="form-field span-2">
              <span>Image URL (optional)</span>
              <input type="url" value={form.imageUrl} onChange={set('imageUrl')} placeholder="https://…" />
              {field('imageUrl')}
            </label>
            <div className="form-actions span-2">
              <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save product'}</button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
