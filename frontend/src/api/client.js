import axios from 'axios';

/**
 * Axios instance for the QuickFind API. The base URL is empty because the dev server
 * (vite.config.js) and nginx (Docker) both proxy /api to the backend.
 */
const client = axios.create({
  baseURL: '',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

/** Turns any failure into an Error with a readable message plus the API error body. */
client.interceptors.response.use(
  (response) => response,
  (error) => {
    const data = error.response?.data;
    const message =
      data?.message ||
      (error.code === 'ECONNABORTED' ? 'The server took too long to respond.' : null) ||
      (error.response ? `Request failed (${error.response.status})` : 'Cannot reach the QuickFind API. Is the backend running?');
    const wrapped = new Error(message);
    wrapped.status = error.response?.status;
    wrapped.code = data?.error;
    wrapped.fieldErrors = data?.fieldErrors || [];
    return Promise.reject(wrapped);
  },
);

export default client;
