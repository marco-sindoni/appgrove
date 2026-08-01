import { test, expect, type Page } from '@playwright/test'

const ORIGIN = 'http://localhost:4173'

function base64url(obj: unknown): string {
  return Buffer.from(JSON.stringify(obj)).toString('base64')
    .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}
const jwt = (payload: Record<string, unknown>) => `${base64url({ alg: 'none' })}.${base64url(payload)}.sig`
const accessToken = jwt({ sub: 'u1', tenant_id: 'tenant-1', roles: ['owner'], upn: 'owner@acme.test' })
const idToken = jwt({ sub: 'u1', email: 'owner@acme.test', name: 'Owner' })
const tokenBody = { access_token: accessToken, id_token: idToken, token_type: 'Bearer' }

/** Avvia la SPA autenticata con i Termini (major) da ri-accettare: il gate deve bloccare l'ingresso. */
async function mockWithPendingLegal(page: Page) {
  // stato legale mutabile: pendente finché non arriva il POST di accettazione
  const legal: { pending: Array<Record<string, unknown>>; notices: unknown[] } = {
    pending: [{ component: 'terms', version: '2.0.0', effectiveDate: '2026-01-01', act: 'accept' }],
    notices: [],
  }

  await page.route('**/config.json', (route) =>
    route.fulfill({ json: { env: 'local', authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } } }),
  )
  await page.route('**/api/auth/refresh', (route) => route.fulfill({ json: tokenBody }))
  await page.route('**/api/platform/v1/users/me', (route) =>
    route.fulfill({ json: { id: 'u1', email: 'owner@acme.test', displayName: 'Owner', role: 'owner', status: 'active', tenantId: 'tenant-1' } }),
  )
  await page.route('**/api/platform/v1/accounts/me', (route) =>
    route.fulfill({ json: { id: 'a1', name: 'Acme', status: 'active' } }),
  )
  await page.route('**/api/platform/v1/me/entitlements', (route) => route.fulfill({ json: { entitlements: [] } }))
  // documento reso in-app (markdown coi token già risolti lato core)
  await page.route('**/api/platform/v1/legal/terms*', (route) =>
    route.fulfill({ json: { component: 'terms', lang: 'en', version: '2.0.0', effectiveDate: '2026-01-01', markdown: '# Terms of Service\n\nWelcome.' } }),
  )
  // stato: prima pendente; il POST lo svuota (ri-accettazione registrata)
  await page.route('**/api/platform/v1/me/legal/status', (route) => route.fulfill({ json: legal }))
  await page.route('**/api/platform/v1/me/legal/acceptance', (route) => {
    legal.pending = []
    route.fulfill({ json: legal })
  })
}

test('[L2-LEGAL] gate legale: major pendente → schermata bloccante → accetta → ingresso', async ({ page }) => {
  await mockWithPendingLegal(page)
  await page.goto('/')

  // schermata bloccante (default UI language = EN)
  await expect(page.getByText('Updated legal documents')).toBeVisible()
  // l'app non è ancora accessibile
  await expect(page.getByRole('navigation', { name: 'Platform' }).getByText('Your apps')).toHaveCount(0)

  // spunta l'accettazione dei Termini e continua
  await page.getByRole('checkbox').first().check()
  await page.getByRole('button', { name: 'Continue' }).click()

  // sbloccato: la shell è visibile
  await expect(page.getByRole('navigation', { name: 'Platform' }).getByText('Your apps')).toBeVisible()
})

test('[L2-LEGAL] diritti GDPR esenti: la pagina "I miei dati" è raggiungibile col blocco pendente', async ({ page }) => {
  await mockWithPendingLegal(page)
  await page.goto('/privacy')
  // /privacy è esente dal gate: NON deve comparire la schermata bloccante
  await expect(page.getByText('Updated legal documents')).toHaveCount(0)
})
