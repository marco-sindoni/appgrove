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
  timeout: 180_000,
  outputDir: './test-results',
  globalSetup: './global-setup.ts',
  globalTeardown: './global-teardown.ts',
  use: {
    baseURL: process.env.PLATFORM_BACKOFFICE_URL ?? 'http://localhost:24173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  // Progetti in CATENA (UC 0091 dec. 8 della change 0070, estesa da UC 0092): i journey che
  // toccano solo il proprio tenant girano in parallelo; quelli che muovono stato GLOBALE girano
  // da soli, in coda, uno alla volta. La dipendenza fra progetti è l'unico meccanismo che
  // garantisce sequenzialità stretta anche fra file diversi (`fullyParallel: false` serializza
  // i test dentro un file, non i file fra loro).
  //
  //   chromium        → tutti i journey confinati al proprio tenant (in parallelo)
  //   admin-serial    → A-CONSOLE : disabilita un'app di CATALOGO, stato globale per ogni tenant
  //   degrade-serial  → F-DEGRADE : ferma DAVVERO un servizio condiviso
  //   legal-serial    → J-LEGAL   : pubblica una nuova versione legale, vincolante per tutti
  //
  // J-LEGAL resta ultimo: la sua leva è la più invasiva e gli altri seriali partono così da un
  // catalogo e da una versione legale intatti.
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
      testIgnore: /(J-LEGAL|A-CONSOLE|F-DEGRADE)\.spec\.ts/,
    },
    {
      name: 'admin-serial',
      use: { ...devices['Desktop Chrome'] },
      testMatch: /A-CONSOLE\.spec\.ts/,
      dependencies: ['chromium'],
    },
    {
      name: 'degrade-serial',
      use: { ...devices['Desktop Chrome'] },
      testMatch: /F-DEGRADE\.spec\.ts/,
      dependencies: ['admin-serial'],
    },
    {
      name: 'legal-serial',
      use: { ...devices['Desktop Chrome'] },
      testMatch: /J-LEGAL\.spec\.ts/,
      dependencies: ['degrade-serial'],
    },
  ],
})
