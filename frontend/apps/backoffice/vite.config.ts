/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  // host:true → ascolta su 0.0.0.0 così il proxy Caddy (container) raggiunge Vite via
  // host.docker.internal:5173 per il single-origin app.local.appgrove.app (UC 0009).
  // allowedHosts → Vite accetta l'Host inoltrato da Caddy (altrimenti 403 host-check).
  server: { port: 5173, host: true, allowedHosts: ['.local.appgrove.app'] },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
    css: false,
    include: ['src/**/*.test.{ts,tsx}'],
    // gli E2E Playwright vivono in e2e/ e non vanno raccolti da Vitest
    exclude: ['e2e/**', 'node_modules/**'],
  },
})
