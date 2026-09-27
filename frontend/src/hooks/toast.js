// Tiny global toast bus, so any component can show a short confirmation message.
const listeners = new Set();

export function showToast(message, tone = 'default') {
  listeners.forEach((listener) => listener({ id: Date.now() + Math.random(), message, tone }));
}

export function subscribeToasts(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}
