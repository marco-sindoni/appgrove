// ─────────────────────────────────────────────────────────────────────────────
// tools/finalize-landing/lib/screenshot.mjs — cattura screenshot reali (UC 0057).
//
// Cattura una figura reale del modulo frontend dell'app, UNA PER LINGUA, via Playwright.
// Seed pragmatico = intercettazione delle rotte in-browser (mock-route): la stessa tecnica
// delle prove e2e generate da new-application (sessione autenticata sintetica con JWT
// `alg:none` + record in memoria). Così la cattura non richiede l'intero stack backend ed è
// portabile per qualunque app. Il seed completo dello stack reale (Postgres + seed.sql) è un
// raffinamento tracciato in UC 0057, non il default.
//
// Playwright riusa la cache browser GLOBALE (~/Library/Caches/ms-playwright), quindi il
// chromium già installato dal frontend è visibile anche qui: nessuna seconda installazione di
// browser. Se il browser non è disponibile, `captureShots`/`captureHtmlToPng` sollevano un
// errore chiaro e il chiamante (CLI o test) degrada con grazia.
// ─────────────────────────────────────────────────────────────────────────────
import fs from 'node:fs'
import path from 'node:path'
import { LOCALES } from './branding.mjs'

const VIEWPORT = { width: 1280, height: 800 }

/** JWT `alg:none` sintetico, come nelle prove e2e generate: sessione autenticata finta. */
function base64url(obj) {
  return Buffer.from(JSON.stringify(obj)).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}
function jwt(payload) {
  return `${base64url({ alg: 'none' })}.${base64url(payload)}.sig`
}

/** Carica il modulo `playwright` (browser dalla cache globale). Errore chiaro se assente. */
export async function loadChromium() {
  try {
    const { chromium } = await import('playwright')
    return chromium
  } catch (err) {
    throw new Error(
      'Playwright non disponibile. Installare le dipendenze dello strumento '
      + '(cd tools/finalize-landing && npm ci) e assicurarsi che chromium sia in cache '
      + `(npx playwright install chromium). Dettaglio: ${err.message}`,
    )
  }
}

/**
 * Applica alla pagina gli stub delle rotte per una sessione autenticata come owner, con il
 * backend dell'app simulato. Speculare a `mockAuthed` delle prove e2e generate.
 */
async function installMocks(page, { appId, origin, metric, freeCap }) {
  const accessToken = jwt({ sub: 'u1', tenant_id: 'tenant-1', roles: ['owner'], upn: 'owner@acme.test' })
  const tokenBody = { access_token: accessToken, id_token: jwt({ sub: 'u1', email: 'owner@acme.test', name: 'Owner' }), token_type: 'Bearer' }
  const items = [
    { id: 'item-1', code: '2026-0001', contactName: 'Mario Rossi', status: 'active', currency: 'EUR', totalAmount: 120 },
    { id: 'item-2', code: '2026-0002', contactName: 'Anna Bianchi', status: 'active', currency: 'EUR', totalAmount: 340 },
  ]
  const quota = { metric, used: items.length, limit: freeCap, remaining: freeCap - items.length }

  await page.route('**/config.json', (r) => r.fulfill({ json: { env: 'local', authBaseUrl: origin, coreBaseUrl: origin, cognito: { userPoolId: '', clientId: '' } } }))
  await page.route('**/api/auth/refresh', (r) => r.fulfill({ json: tokenBody }))
  await page.route('**/api/platform/v1/users/me', (r) => r.fulfill({ json: { id: 'u1', email: 'owner@acme.test', displayName: 'Owner', role: 'owner', status: 'active', tenantId: 'tenant-1' } }))
  await page.route('**/api/platform/v1/accounts/me', (r) => r.fulfill({ json: { id: 'a1', name: 'Acme', status: 'active' } }))
  await page.route('**/api/platform/v1/me/entitlements', (r) => r.fulfill({ json: { entitlements: [{ appSlug: appId, tierKey: 'free', limits: { [metric]: { cap: freeCap, nature: 'flow', window: 'month' } } }] } }))
  await page.route(`**/api/${appId}/v1/quota`, (r) => r.fulfill({ json: quota }))
  await page.route(`**/api/${appId}/v1/items?*`, (r) => r.fulfill({ json: { content: items, page: 0, size: 20, totalElements: items.length, totalPages: 1 } }))
  await page.route(`**/api/${appId}/v1/items`, (r) => r.fulfill({ json: { content: items, page: 0, size: 20, totalElements: items.length, totalPages: 1 } }))
}

/**
 * Cattura una figura del modulo `appId` per ogni lingua richiesta, contro l'app servita a
 * `baseURL` (es. l'anteprima Astro/Vite del frontend). Scrive `hero.<locale>.png` in `outDir`
 * e ritorna la lista dei percorsi prodotti. Il chiamante avvia e ferma il server dell'app.
 */
export async function captureShots({ baseURL, appId, locales = LOCALES, outDir, metric = 'items', freeCap = 10 }) {
  const chromium = await loadChromium()
  const origin = new URL(baseURL).origin
  fs.mkdirSync(outDir, { recursive: true })
  const browser = await chromium.launch()
  const produced = []
  try {
    for (const locale of locales) {
      const context = await browser.newContext({ viewport: VIEWPORT, locale, extraHTTPHeaders: { 'Accept-Language': locale } })
      const page = await context.newPage()
      await installMocks(page, { appId, origin, metric, freeCap })
      await page.goto(`${baseURL}/app/${appId}`, { waitUntil: 'networkidle' })
      const outPath = path.join(outDir, `hero.${locale}.png`)
      await page.screenshot({ path: outPath })
      produced.push(outPath)
      await context.close()
    }
  } finally {
    await browser.close()
  }
  return produced
}

/**
 * Rasterizza dell'HTML statico in un PNG via il browser (plumbing generico). Serve al test
 * per dimostrare la cattura senza dover buildare l'intera app, e come utilità riusabile.
 */
export async function captureHtmlToPng({ html, outPath, width = 1280, height = 800 }) {
  const chromium = await loadChromium()
  const browser = await chromium.launch()
  try {
    const context = await browser.newContext({ viewport: { width, height } })
    const page = await context.newPage()
    await page.setContent(html, { waitUntil: 'load' })
    fs.mkdirSync(path.dirname(outPath), { recursive: true })
    await page.screenshot({ path: outPath })
    return outPath
  } finally {
    await browser.close()
  }
}
