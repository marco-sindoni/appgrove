import { defineConfig, devices } from '@playwright/test'

/**
 * Suite end-to-end di piattaforma (UC 0090): browser vero contro lo stack backend VERO
 * (Postgres + ElasticMQ + Mailpit + tutti i servizi in profilo dev) e le SPA costruite
 * davvero. NIENTE intercettazione di rotte: qui il backend non si simula mai.
 *
 * L'orchestrazione (stack, build, server statici) è di run.sh — questo config esegue solo
 * i journey. Ogni journey è indipendente e parallelizzabile: crea da zero il proprio tenant.
 *
 * Anti-flakiness (requisiti dello use case): retries = 1 al massimo — un journey instabile è
 * un difetto da correggere, non da ritentare a oltranza; il retry che passa è comunque
 * segnalato dal reporter come "flaky". Vietato dormire a tempo fisso nei journey: solo
 * attese su condizioni (API Mailpit, stati UI, polling DB).
 */
export default defineConfig({
  testDir: './journeys',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: 1,
  reporter: 'list',
  timeout: 120_000,
  outputDir: './test-results',
  use: {
    baseURL: process.env.PLATFORM_BACKOFFICE_URL ?? 'http://localhost:24173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
