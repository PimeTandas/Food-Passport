import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// In development the browser only ever talks to Vite (localhost:5173).
// Anything under /api is forwarded to Spring on :8080, so there are no cross-origin (CORS) issues.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
