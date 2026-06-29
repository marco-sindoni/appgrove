import { test, expect, type Page } from '@playwright/test'

const ORIGIN = 'http://localhost:4173'

function base64url(obj: unknown): string {
  return Buffer.from(JSON.stringify(obj))
    .toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}
const jwt = (payload: Record<string, unknown>) => `${base64url({ alg: 'none' })}.${base64url(payload)}.sig`
const accessToken = jwt({ sub: 'u1', tenant_id: 'tenant-1', roles: ['owner'], upn: 'owner@acme.test' })
const idToken = jwt({ sub: 'u1', email: 'owner@acme.test', name: 'Owner' })
const tokenBody = { access_token: accessToken, id_token: idToken, token_type: 'Bearer' }

/**
 * E2E L2 del checkout (UC 0024, #09 D20): backend mockato via `page.route`; l'overlay Paddle è il **Fake
 * Paddle.js** del bundle (UC 0023), che emette `checkout.completed` sintetico. Il polling dello stato
 * subscription passa da non-attivo ad attivo simulando l'arrivo del webhook → UX "attivazione"→"attivato".
 */
async function mockCheckout(page: Page) {
  let pollCount = 0

  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env: 'local', authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
    }),
  )
  await page.route('**/api/auth/refresh', (route) => route.fulfill({ json: tokenBody }))
  await page.route('**/api/platform/v1/users/me', (route) =>
    route.fulfill({
      json: { id: 'u1', email: 'owner@acme.test', displayName: 'Owner', role: 'owner', status: 'active', tenantId: 'tenant-1' },
    }),
  )
  await page.route('**/api/platform/v1/accounts/me', (route) =>
    route.fulfill({ json: { id: 'a1', name: 'Acme', status: 'active' } }),
  )

  // Catalogo tier/prezzi (default annuale + sconto + trial).
  await page.route('**/api/platform/v1/checkout/apps/*/tiers', (route) =>
    route.fulfill({
      json: {
        appId: 'app-1',
        slug: 'demo',
        name: 'Demo app',
        tiers: [
          {
            tierId: 'tier-pro',
            key: 'pro',
            name: 'Pro',
            limits: {},
            features: {},
            trialDays: 14,
            prices: [
              { billingCycle: 'monthly', amount: 900, currency: 'EUR' },
              { billingCycle: 'annual', amount: 9000, currency: 'EUR' },
            ],
          },
        ],
      },
    }),
  )

  // Checkout server-initiated → token (il client passa solo questo all'overlay).
  await page.route('**/api/platform/v1/checkout/apps/*', (route, request) => {
    if (request.method() === 'POST') {
      return route.fulfill({ json: { checkoutToken: 'chk_e2e' } })
    }
    return route.fallback()
  })

  // Polling stato: i primi poll non-attivi, poi attivo (= arrivo webhook).
  await page.route('**/api/platform/v1/checkout/apps/*/subscription', (route) => {
    pollCount += 1
    const active = pollCount >= 2
    return route.fulfill({ json: { status: active ? 'active' : null, active } })
  })
}

test('checkout: scelta tier → overlay → polling attivazione → attivato', async ({ page }) => {
  await mockCheckout(page)

  await page.goto('/billing')
  await expect(page.getByRole('heading', { name: 'Get an app', level: 1 })).toBeVisible()

  // scegli un'app → entra nella scelta tier (default UI language = EN)
  await page.getByRole('button', { name: 'Subscribe' }).first().click()

  // tier Pro con prezzo annuale (default) + sconto + trial
  await expect(page.getByRole('heading', { name: 'Pro' })).toBeVisible()
  await expect(page.getByText('2 months free')).toBeVisible()
  await expect(page.getByText('14-day free trial')).toBeVisible()
  await expect(page.getByText(/90,00/)).toBeVisible()

  // sottoscrivi → overlay stub → checkout.completed → attivazione (polling)
  await page.getByRole('button', { name: 'Subscribe' }).click()
  await expect(page.getByText('Activating your subscription…')).toBeVisible()

  // arrivo webhook simulato → attivato (mai un errore di pagamento)
  await expect(page.getByText('All set! Your subscription is active.')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Open app' })).toBeVisible()
})
