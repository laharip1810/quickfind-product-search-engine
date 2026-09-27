import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Runs an async loader whenever its dependencies change and tracks loading, error and
 * data. Responses that arrive after a newer request started are ignored, so fast
 * filter changes can never show stale results.
 */
export default function useAsync(loader, deps) {
  const [state, setState] = useState({ loading: true, error: null, data: null });
  const requestId = useRef(0);

  const run = useCallback(() => {
    const id = ++requestId.current;
    setState((s) => ({ ...s, loading: true, error: null }));
    loader()
      .then((data) => {
        if (id === requestId.current) setState({ loading: false, error: null, data });
      })
      .catch((error) => {
        if (id === requestId.current) setState({ loading: false, error, data: null });
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    run();
  }, [run]);

  return { ...state, reload: run };
}
