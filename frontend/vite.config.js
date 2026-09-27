import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// In development, /api is proxied to the Spring Boot backend, so the browser sees a
// single origin (no CORS needed). Override the target with VITE_PROXY_TARGET.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  };
});
