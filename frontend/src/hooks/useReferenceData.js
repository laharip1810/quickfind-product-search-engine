import { useEffect, useState } from 'react';
import { api } from '../api/quickfind.js';

// Categories, brands, colors, sizes and the demo user rarely change: load them once per page load.
let cache = null;
let inflight = null;

function loadReferenceData() {
  if (cache) return Promise.resolve(cache);
  if (!inflight) {
    inflight = Promise.all([api.categories(), api.brands(), api.colors(), api.sizes(), api.demoUser()])
      .then(([categories, brands, colors, sizes, demoUser]) => {
        cache = { categories, brands, colors, sizes, demoUser };
        return cache;
      })
      .finally(() => {
        inflight = null;
      });
  }
  return inflight;
}

export default function useReferenceData() {
  const [data, setData] = useState(cache);
  const [error, setError] = useState(null);
  useEffect(() => {
    if (cache) return;
    let active = true;
    loadReferenceData()
      .then((d) => active && setData(d))
      .catch((e) => active && setError(e));
    return () => {
      active = false;
    };
  }, []);
  return { data, error };
}
