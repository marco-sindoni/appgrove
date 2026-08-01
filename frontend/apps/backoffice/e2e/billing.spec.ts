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

/** Sessione con il ruolo richiesto: lo storico pagamenti è di owner/admin (UC 0096 §2). */
async function mockSession(page: Page, role: 'owner' | 'member' = 'owner') {
  const accessToken = jwt({ sub: 'u1', tenant_id: 'tenant-1', roles: [role], upn: `${role}@acme.test` })
  const idToken = jwt({ sub: 'u1', email: `${role}@acme.test`, name: 'Utente' })
  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env: 'local', authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
    }),
  )
  await page.route('**/api/auth/refresh', (route) =>
    route.fulfill({ json: { access_token: accessToken, id_token: idToken, token_type: 'Bearer' } }),
  )
  await page.route('**/api/platform/v1/users/me', (route) =>
    route.fulfill({
      json: { id: 'u1', email: `${role}@acme.test`, displayName: 'Utente', role, status: 'active', tenantId: 'tenant-1' },
    }),
  )
  await page.route('**/api/platform/v1/accounts/me', (route) =>
    route.fulfill({ json: { id: 'a1', name: 'Acme', status: 'active' } }),
  )
  await page.route('**/api/platform/v1/me/entitlements', (route) =>
    route.fulfill({ json: { entitlements: [] } }),
  )
  await page.route('**/api/platform/v1/me/catalog', (route) => route.fulfill({ json: { apps: [] } }))
}

const subscription = (over: Record<string, unknown> = {}) => ({
  appSlug: 'demo',
  appName: 'Demo app',
  status: 'active',
  tierKey: 'pro',
  tierName: 'Demo Pro',
  currentPeriodEnd: '2027-01-01T00:00:00Z',
  phase: 'ACTIVE',
  limits: {},
  canUpgrade: true,
  canDowngrade: true,
  canCancel: true,
  canResume: false,
  canReactivate: false,
  portalAvailable: false,
  ...over,
})

async function mockSubscriptions(page: Page, subscriptions: unknown[]) {
  await page.route('**/api/platform/v1/me/subscriptions', (route, request) => {
    if (request.method() !== 'GET') return route.fallback()
    return route.fulfill({ json: { subscriptions } })
  })
}

test('[L2-BILLING] la pagina è di sola fatturazione: nessun elemento di vetrina, due sezioni', async ({
  page,
}) => {
  await mockSession(page)
  await mockSubscriptions(page, [subscription()])
  await page.route('**/api/platform/v1/me/payments', (route) =>
    route.fulfill({ json: { payments: [] } }),
  )

  await page.goto('/billing')
  await expect(page.getByRole('heading', { name: 'Billing', level: 1 })).toBeVisible()
  await expect(page.getByText('Manage your plans, payments and receipts.')).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Your subscriptions' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Payments & receipts' })).toBeVisible()

  // ciò che è migrato nel catalogo (UC 0095) non deve essere rimasto qui nemmeno per sbaglio
  await expect(page.getByText('Get an app')).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Subscribe' })).toHaveCount(0)
})

test('[L2-BILLING] lo storico mostra pagato, fallito e la riga senza ricevuta', async ({ page }) => {
  await mockSession(page)
  await mockSubscriptions(page, [subscription()])
  await page.route('**/api/platform/v1/me/payments', (route) =>
    route.fulfill({
      json: {
        payments: [
          {
            billedAt: '2026-07-31T10:00:00Z',
            appSlug: 'demo',
            appName: 'Demo app',
            planName: 'Demo Pro',
            billingCycle: 'monthly',
            amount: 900,
            currency: 'EUR',
            status: 'paid',
            receiptUrl: 'https://paddle.example/receipts/1',
          },
          {
            billedAt: '2026-06-30T10:00:00Z',
            appSlug: 'demo',
            appName: 'Demo app',
            planName: 'Demo Pro',
            billingCycle: 'monthly',
            amount: 900,
            currency: 'EUR',
            status: 'failed',
          },
        ],
      },
    }),
  )

  await page.goto('/billing')
  const table = page.getByRole('table')
  await expect(table).toBeVisible()
  await expect(table.getByText('Demo Pro — monthly').first()).toBeVisible()
  await expect(table.getByText('Paid')).toBeVisible()
  // un pagamento fallito è proprio ciò che l'utente deve poter vedere
  await expect(table.getByText('Failed')).toBeVisible()
  await expect(table.getByRole('link', { name: /Receipt/ })).toHaveAttribute(
    'href',
    'https://paddle.example/receipts/1',
  )
  // ricevuta non ancora disponibile: la riga resta, senza collegamento
  await expect(table.getByText('Not available yet')).toBeVisible()
})

test('[L2-BILLING] senza abbonamenti si rimanda al catalogo, non a una griglia d’acquisto', async ({
  page,
}) => {
  await mockSession(page)
  await mockSubscriptions(page, [])
  await page.route('**/api/platform/v1/me/payments', (route) =>
    route.fulfill({ json: { payments: [] } }),
  )

  await page.goto('/billing')
  await expect(page.getByText('You don’t have any subscriptions yet.')).toBeVisible()
  await page.getByRole('button', { name: 'Browse the app catalog' }).click()
  await expect(page).toHaveURL(/\/catalog$/)
  await expect(page.getByRole('heading', { name: 'App catalog', level: 1 })).toBeVisible()
})

test('[L2-BILLING] un guasto dello storico non spegne gli abbonamenti', async ({ page }) => {
  await mockSession(page)
  await mockSubscriptions(page, [subscription()])
  await page.route('**/api/platform/v1/me/payments', (route) =>
    route.fulfill({
      status: 500,
      contentType: 'application/problem+json',
      json: { status: 500, title: 'Errore', detail: 'boom' },
    }),
  )

  await page.goto('/billing')
  await expect(page.getByText('We couldn’t load your payment history.')).toBeVisible({ timeout: 15_000 })
  await expect(page.getByRole('button', { name: 'Retry' })).toBeVisible()
  // due guasti indipendenti: gli abbonamenti restano visibili e azionabili
  await expect(page.getByText('Plan: Demo Pro')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Change plan' })).toBeVisible()
})

test('[L2-BILLING] a un membro lo storico non è mostrato, con la spiegazione', async ({ page }) => {
  await mockSession(page, 'member')
  await mockSubscriptions(page, [subscription()])

  await page.goto('/billing')
  await expect(page.getByRole('heading', { name: 'Payments & receipts' })).toBeVisible()
  await expect(page.getByText('Only owners and admins can see payments and receipts.')).toBeVisible()
  await expect(page.getByRole('table')).toHaveCount(0)
})
