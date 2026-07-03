import { defineConfig, devices } from '@playwright/test'

/**
 * L3 — smoke reale su Paddle Sandbox (UC 0029, #09 D20 L3). NON è per-PR e NON è cablata in
 * `run-tests.sh`: gira nel flusso di release tag→prod (cablaggio pipeline + gate/override: UC 0005).
 * Si attiva SOLO con `APPGROVE_L3_BASE_URL` che punta a un ambiente deployato con Paddle Sandbox
 * (account: UC 0001; loader Paddle.js reale: gated #14); senza env la suite si auto-skippa.
 * Mai in locale, mai pagamenti reali (#09 D20). Runbook: e2e-l3/README.md.
 */
export default defineConfig({
  testDir: './e2e-l3',
  fullyParallel: false,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL: process.env.APPGROVE_L3_BASE_URL,
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
