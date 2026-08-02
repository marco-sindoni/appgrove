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

/** Fondamenta comuni (config + sessione owner) per la sezione abbonamenti (UC 0028, UC 0067). */
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
  // Billing ha ora anche lo storico pagamenti (UC 0096): qui non è l'oggetto della prova, ma senza
  // risposta la sua sezione mostrerebbe un errore e sporcherebbe le asserzioni di questi percorsi.
  await page.route('**/api/platform/v1/me/payments', (route) => route.fulfill({ json: { payments: [] } }))
}

/** Un abbonamento del read-model, con i valori tipici già impostati. */
const subscription = (over: Record<string, unknown> = {}) => ({
  appSlug: 'demo',
  appName: 'Demo app',
  status: 'active',
  tierKey: 'pro',
  tierName: 'Pro',
  currentPeriodEnd: '2027-01-01T00:00:00Z',
  phase: 'ACTIVE',
  limits: { seats: { cap: 10, nature: 'stock' } },
  usage: {},
  blockedTiers: {},
  canUpgrade: true,
  canDowngrade: true,
  canCancel: true,
  canResume: false,
  canReactivate: false,
  portalAvailable: true,
  appDisabled: false,
  ...over,
})

/** Read-model servito da una funzione, così un comando può cambiarne l'esito fra due letture. */
async function mockSubscriptions(page: Page, next: () => Record<string, unknown>[]) {
  await page.route('**/api/platform/v1/me/subscriptions', (route, request) => {
    if (request.method() !== 'GET') return route.fallback()
    return route.fulfill({ json: { subscriptions: next() } })
  })
}

async function mockTiers(page: Page) {
  await page.route('**/api/platform/v1/checkout/apps/*/tiers', (route) =>
    route.fulfill({
      json: {
        appId: 'app-1',
        slug: 'demo',
        name: 'Demo app',
        tiers: [
          { tierId: 't-basic', key: 'basic', name: 'Basic', limits: { metric: 'seats', cap: 2, type: 'stock' }, features: {}, trialDays: 0, prices: [{ billingCycle: 'monthly', amount: 500, currency: 'EUR' }] },
          { tierId: 't-pro', key: 'pro', name: 'Pro', limits: { metric: 'seats', cap: 10, type: 'stock' }, features: {}, trialDays: 0, prices: [{ billingCycle: 'monthly', amount: 900, currency: 'EUR' }] },
        ],
      },
    }),
  )
}

test('[L2-SUB] self-service: riduzione di piano confermata e programmata a fine periodo', async ({ page }) => {
  await mockSession(page)
  let downgraded = false
  await mockSubscriptions(page, () => [
    subscription({
      scheduledTierKey: downgraded ? 'basic' : null,
      scheduledChangeAt: downgraded ? '2027-01-01T00:00:00Z' : null,
    }),
  ])
  await mockTiers(page)
  await page.route('**/api/platform/v1/me/subscriptions/*/change-tier', (route) => {
    downgraded = true
    return route.fulfill({ json: { direction: 'DOWNGRADE', effectiveAt: '2027-01-01T00:00:00Z' } })
  })

  await page.goto('/billing')
  await expect(page.getByRole('heading', { name: 'Your subscriptions' })).toBeVisible()
  await expect(page.getByText('Plan: Pro')).toBeVisible()

  await page.getByRole('button', { name: 'Change plan' }).click()
  const dialog = page.getByRole('dialog')
  // la finestra mostra prezzi e il piano attuale marcato, non più pulsanti nudi
  await expect(dialog.getByText('€9.00 /mo')).toBeVisible()
  await expect(dialog.getByText('Current plan').first()).toBeVisible()

  const basic = dialog.getByRole('listitem').filter({ hasText: 'Basic' })
  await basic.getByRole('button', { name: 'Choose' }).click()

  // conferma esplicita: dice cosa succede e da quando, prima che parta qualunque comando
  await expect(page.getByText('Move to a lower plan')).toBeVisible()
  await expect(page.getByText(/from 1\/1\/2027/)).toBeVisible()
  await page.getByRole('button', { name: 'Confirm' }).click()

  // refetch → la riduzione programmata è mostrata (resta su Pro fino a fine periodo)
  await expect(page.getByText(/Downgrade scheduled to “basic”/)).toBeVisible()
})

test('[L2-SUB] consumo della quota visibile e piano troppo piccolo non selezionabile', async ({ page }) => {
  await mockSession(page)
  await mockSubscriptions(page, () => [
    subscription({
      usage: { seats: 9 },
      blockedTiers: { basic: 'Downgrade bloccato: la metrica «seats» è a 9, sopra il limite 2 del piano scelto.' },
    }),
  ])
  await mockTiers(page)

  await page.goto('/billing')
  // il consumo reale, non il solo tetto del piano
  await expect(page.getByText('9 of 10 seats used')).toBeVisible()
  await expect(page.getByText(/You are close to your plan limit for seats/)).toBeVisible()

  await page.getByRole('button', { name: 'Change plan' }).click()
  const basic = page.getByRole('dialog').getByRole('listitem').filter({ hasText: 'Basic' })
  await expect(basic.getByText(/sopra il limite 2/)).toBeVisible()
  await expect(basic.getByRole('button', { name: 'Choose' })).toBeDisabled()
})

test('[L2-SUB] disdetta con conferma e successiva riattivazione', async ({ page }) => {
  await mockSession(page)
  let canceled = false
  await mockSubscriptions(page, () => [
    subscription({
      cancelAt: canceled ? '2027-01-01T00:00:00Z' : null,
      phase: canceled ? 'CANCELING' : 'ACTIVE',
      canCancel: !canceled,
      canResume: canceled,
    }),
  ])
  await page.route('**/api/platform/v1/me/subscriptions/*/cancel', (route) => {
    canceled = true
    return route.fulfill({ json: { direction: 'CANCEL', effectiveAt: '2027-01-01T00:00:00Z' } })
  })
  await page.route('**/api/platform/v1/me/subscriptions/*/resume', (route) => {
    canceled = false
    return route.fulfill({ json: { direction: 'RESUME' } })
  })

  await page.goto('/billing')
  await page.getByRole('button', { name: 'Cancel', exact: true }).click()
  await expect(page.getByText('Cancel subscription')).toBeVisible()
  await expect(page.getByText(/access stays until 1\/1\/2027/)).toBeVisible()
  await page.getByRole('button', { name: 'Confirm' }).click()

  await expect(page.getByText(/Cancellation scheduled: access until/)).toBeVisible()

  await page.getByRole('button', { name: 'Undo cancellation' }).click()
  await expect(page.getByText(/Renews on/)).toBeVisible()
})

test('[L2-SUB] pagamento in ritardo: avviso persistente con accesso al portale', async ({ page }) => {
  await mockSession(page)
  await mockSubscriptions(page, () => [subscription({ status: 'past_due', phase: 'ACTIVE' })])
  await page.route('**/api/platform/v1/me/portal-session', (route) =>
    route.fulfill({ json: { url: 'https://sandbox-customer-portal.paddle.com/stub/abc' } }),
  )

  await page.goto('/billing')
  const alert = page.getByRole('alert').filter({ hasText: 'Payment overdue' })
  await expect(alert).toBeVisible()
  await expect(alert.getByText(/Access stays on for a short grace period/)).toBeVisible()
  await expect(alert.getByRole('button', { name: 'Update payment method' })).toBeVisible()
})

test('[L2-SUB] abbonamento scaduto: riattivazione e diritti sui dati sempre disponibili', async ({ page }) => {
  await mockSession(page)
  await mockSubscriptions(page, () => [
    subscription({
      status: 'canceled',
      phase: 'ENDED',
      canUpgrade: false,
      canDowngrade: false,
      canCancel: false,
      canReactivate: true,
    }),
  ])

  await page.goto('/billing')
  await expect(page.getByText('Subscription expired', { exact: true })).toBeVisible()
  await expect(page.getByText(/Your data is still here/)).toBeVisible()
  await expect(page.getByRole('button', { name: 'Reactivate' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Export / delete your data' })).toBeVisible()
})

test('[L2-SUB] enforcement: 429 quota → banner azionabile con CTA upgrade', async ({ page }) => {
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

test('[L2-SUB] app disabilitata dalla piattaforma: l’abbonamento resta ma l’avviso spiega perché', async ({
  page,
}) => {
  await mockSession(page)
  // Il read-model marca l'app come sospesa a livello piattaforma (UC 0076): l'abbonamento resta
  // elencato e "attivo", ma la barra laterale non mostra l'app — senza avviso sarebbe una contraddizione.
  await mockSubscriptions(page, () => [
    subscription({
      limits: {},
      canUpgrade: false,
      canDowngrade: false,
      portalAvailable: false,
      appDisabled: true,
    }),
  ])

  await page.goto('/billing')
  await expect(page.getByRole('heading', { name: 'Your subscriptions' })).toBeVisible()
  await expect(page.getByText('Suspended', { exact: true })).toBeVisible()
  await expect(page.getByText('App suspended by the platform')).toBeVisible()
  await expect(page.getByText(/Your subscription stays valid and your data is untouched/)).toBeVisible()
})
