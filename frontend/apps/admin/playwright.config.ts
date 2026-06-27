import { defineConfig, devices } from '@playwright/test'

/**
 * E2E della console admin (UC 0021). App servita da `vite preview`; il backend è **mockato** via
 * `page.route` nei test (login programmatico = mock di `/api/auth/refresh` al load → storageState
 * non necessario, coerente col "nessun token persistito"). aria-snapshot/getByRole come rete primaria.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  reporter: 'list',
  use: {
    baseURL: 'http://localhost:4174',
    trace: 'on-first-retry',
  },
  webServer: {
    command: 'npm run build && npm run preview',
    url: 'http://localhost:4174',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
