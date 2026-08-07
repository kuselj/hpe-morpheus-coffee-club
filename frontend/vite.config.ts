import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

/**
 * Dev profile: the React dev server owns port 5173 and proxies every /api/* call through to the
 * Spring Boot backend on port 8080, so the two halves stay completely decoupled while coding and
 * Hot Module Replacement keeps working.
 *
 * Prod profile: `npm run build` emits ./dist, which Maven copies into the JAR's static resources
 * so a single artifact serves both the API and the UI on one port.
 */
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    strictPort: true,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    sourcemap: false,
  },
});
