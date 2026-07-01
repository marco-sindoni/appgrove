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

/** Fondamenta comuni (config + sessione owner) per il portale self-service (UC 0028). */
async function mockSession(page: Page) {
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
  await page.route('**/api/platform/v1/me/entitlements', (route) => route.fulfill({ json: { entitlements: [] } }))
}

test('self-service: downgrade programmato a fine periodo', async ({ page }) => {
  await mockSession(page)
  let downgraded = false

  // read-model: prima attivo su pro; dopo il change-tier, mostra il downgrade schedulato a basic.
  await page.route('**/api/platform/v1/me/subscriptions', (route, request) => {
    if (request.method() !== 'GET') return route.fallback()
    return route.fulfill({
      json: {
        subscriptions: [
          {
            appSlug: 'demo',
            appName: 'Demo app',
            status: 'active',
            tierKey: 'pro',
            tierName: 'Pro',
            currentPeriodEnd: '2027-01-01T00:00:00Z',
            scheduledTierKey: downgraded ? 'basic' : null,
            scheduledChangeAt: downgraded ? '2027-01-01T00:00:00Z' : null,
            phase: 'ACTIVE',
            limits: { invoices: { cap: 100, nature: 'flow', window: 'month' } },
            canUpgrade: true,
            canDowngrade: true,
            canCancel: true,
            canResume: false,
            canReactivate: false,
            portalAvailable: true,
          },
        ],
      },
    })
  })
  await page.route('**/api/platform/v1/checkout/apps/*/tiers', (route) =>
    route.fulfill({
      json: {
        appId: 'app-1',
        slug: 'demo',
        name: 'Demo app',
        tiers: [
          { tierId: 't-basic', key: 'basic', name: 'Basic', limits: {}, features: {}, trialDays: 0, prices: [{ billingCycle: 'monthly', amount: 500, currency: 'EUR' }] },
          { tierId: 't-pro', key: 'pro', name: 'Pro', limits: {}, features: {}, trialDays: 0, prices: [{ billingCycle: 'monthly', amount: 900, currency: 'EUR' }] },
        ],
      },
    }),
  )
  await page.route('**/api/platform/v1/me/subscriptions/*/change-tier', (route) => {
    downgraded = true
    return route.fulfill({ json: { direction: 'DOWNGRADE', effectiveAt: '2027-01-01T00:00:00Z' } })
  })

  page.on('dialog', (d) => d.accept())

  await page.goto('/billing')
  await expect(page.getByRole('heading', { name: 'Your subscriptions' })).toBeVisible()
  await expect(page.getByText('Plan: Pro')).toBeVisible()

  await page.getByRole('button', { name: 'Change plan' }).click()
  await page.getByRole('button', { name: 'Basic' }).click()

  // refetch → il downgrade schedulato è mostrato (resta su Pro fino a fine periodo)
  await expect(page.getByText(/Downgrade scheduled to “basic”/)).toBeVisible()
})

test('enforcement: 429 quota → banner azionabile con CTA upgrade', async ({ page }) => {
  await mockSession(page)
  await page.route('**/api/platform/v1/me/subscriptions', (route) =>
    route.fulfill({ json: { subscriptions: [] } }),
  )
  // un gate 429 su una query qualsiasi alza il banner globale (UC 0028 chiude il punto aperto di UC 0027)
  await page.unroute('**/api/platform/v1/me/entitlements')
  await page.route('**/api/platform/v1/me/entitlements', (route) =>
    route.fulfill({
      status: 429,
      contentType: 'application/problem+json',
      json: { status: 429, title: 'Quota esaurita', detail: 'Quota esaurita' },
    }),
  )

  await page.goto('/billing')
  await expect(page.getByRole('alert').getByText('Plan limit reached')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Upgrade' })).toBeVisible()
})
