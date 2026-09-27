const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });

export function formatPrice(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '';
  return inr.format(Number(value));
}

export function formatNumber(value) {
  return new Intl.NumberFormat('en-IN').format(value ?? 0);
}

export function formatDateTime(value) {
  if (!value) return '';
  // The API returns UTC LocalDateTime without an offset.
  const date = new Date(value.endsWith('Z') ? value : `${value}Z`);
  return date.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
}
