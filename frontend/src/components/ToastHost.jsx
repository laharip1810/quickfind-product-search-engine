import { useEffect, useState } from 'react';
import { subscribeToasts } from '../hooks/toast.js';

export default function ToastHost() {
  const [toasts, setToasts] = useState([]);

  useEffect(() => subscribeToasts((toast) => {
    setToasts((current) => [...current, toast]);
    setTimeout(() => setToasts((current) => current.filter((t) => t.id !== toast.id)), 3200);
  }), []);

  return (
    <div className="toast-host" role="status" aria-live="polite">
      {toasts.map((t) => (
        <div key={t.id} className={`toast toast-${t.tone}`}>{t.message}</div>
      ))}
    </div>
  );
}
