import client from './client.js';

/** Removes empty values so they are not sent as query parameters. */
function clean(params) {
  const result = {};
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return;
    if (Array.isArray(value) && value.length === 0) return;
    result[key] = value;
  });
  return result;
}

// Repeated params (brand=Nike&brand=Puma), which Spring binds to a List.
const paramsSerializer = { indexes: null };

export const api = {
  searchProducts: (params) =>
    client.get('/api/products/search', { params: clean(params), paramsSerializer }).then((r) => r.data),
  listProducts: (params) => client.get('/api/products', { params: clean(params) }).then((r) => r.data),
  getProduct: (id) => client.get(`/api/products/${id}`).then((r) => r.data),
  createProduct: (body) => client.post('/api/products', body).then((r) => r.data),
  updateProduct: (id, body) => client.put(`/api/products/${id}`, body).then((r) => r.data),
  updateStock: (id, stockQuantity) =>
    client.patch(`/api/products/${id}/stock`, { stockQuantity }).then((r) => r.data),
  deleteProduct: (id) => client.delete(`/api/products/${id}`),

  suggestions: (prefix, limit = 8, signal) =>
    client.get('/api/search/suggestions', { params: { prefix, limit }, signal }).then((r) => r.data),
  recommendations: (id, userId, limit = 8) =>
    client.get(`/api/products/${id}/recommendations`, { params: clean({ userId, limit }) }).then((r) => r.data),
  trending: (limit = 8) => client.get('/api/products/trending', { params: { limit } }).then((r) => r.data),
  priceRangeCount: (params) =>
    client.get('/api/products/price-range-count', { params: clean(params) }).then((r) => r.data),

  categories: () => client.get('/api/categories').then((r) => r.data),
  brands: () => client.get('/api/brands').then((r) => r.data),
  colors: () => client.get('/api/colors').then((r) => r.data),
  sizes: () => client.get('/api/sizes').then((r) => r.data),
  demoUser: () => client.get('/api/users/demo').then((r) => r.data),

  recordInteraction: (body) => client.post('/api/interactions', body).then((r) => r.data),
  wishlist: () => client.get('/api/interactions/wishlist').then((r) => r.data),
  removeFromWishlist: (productId) => client.delete(`/api/interactions/wishlist/${productId}`),
  recentActivity: (limit = 15) => client.get('/api/interactions/recent', { params: { limit } }).then((r) => r.data),
};

/** Fire-and-forget event tracking: analytics must never break the page. */
export function track(eventType, fields = {}) {
  api.recordInteraction({ eventType, ...fields }).catch(() => {});
}
