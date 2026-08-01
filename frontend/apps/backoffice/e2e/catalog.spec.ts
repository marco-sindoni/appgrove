import { test, expect, type Page } from '@playwright/test'

const ORIGIN = 'http://localhost:4173'

function base64url(obj: unknown): string {
  return Buffer.from(JSON.stringify(obj))
    .toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}
const jwt = (payload: Record<string, unknown>) =>
  `${base64url({ alg: 'none' })}.${base64url(payload)}.sig`

const idToken = jwt({ sub: 'user-1', email: 'u@x.io', name: 'Utente Uno' })
const accessToken = (roles: string[]) =>
  jwt({ sub: 'user-1', tenant_id: 'tenant-1', roles, upn: 'u@x.io' })

interface CatalogApp {
  appSlug: string
  name: string
  category?: string
  descriptions?: Record<string, string>
  state: string
  planName?: string
  trialEndsAt?: string
  cancelAt?: string
  startingPrice?: { amount: number; currency: string; billingCycle: string }
}

const app = (over: Partial<CatalogApp> = {}): CatalogApp => ({
  appSlug: 'notes',
  name: 'Notes',
  category: 'amber',
  descriptions: { en: 'Shared notes and lightweight docs for your team' },
  state: 'available',
  startingPrice: { amount: 900, currency: 'EUR', billingCycle: 'monthly' },
  ...over,
})

/** Le sei card di stato del riferimento visivo, più il riempimento per esercitare la paginazione. */
const SIX_STATES: CatalogApp[] = [
  app({ appSlug: 'notes', name: 'Notes', state: 'available' }),
  app({ appSlug: 'teams', name: 'Teams', state: 'active', planName: 'Teams Pro' }),
  app({ appSlug: 'books', name: 'Books', state: 'trial', trialEndsAt: '2030-08-10T00:00:00Z' }),
  app({ appSlug: 'timesheets', name: 'Timesheets', state: 'payment_pending' }),
  app({
    appSlug: 'bookings',
    name: 'Bookings',
    state: 'cancellation_scheduled',
    cancelAt: '2030-08-31T00:00:00Z',
  }),
  app({ appSlug: 'minicrm', name: 'Mini-CRM', state: 'disabled_by_platform' }),
]

async function mockBackend(
  page: Page,
  options: { apps?: CatalogApp[] | 'error'; roles?: string[] } = {},
) {
  const { apps = SIX_STATES, roles = ['owner'] } = options
  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: {
        env: 'test',
        authBaseUrl: ORIGIN,
        coreBaseUrl: ORIGIN,
        cognito: { userPoolId: '', clientId: '' },
      },
    }),
  )
  await page.route('**/api/auth/refresh', (route) =>
    route.fulfill({
      json: { access_token: accessToken(roles), id_token: idToken, token_type: 'Bearer' },
    }),
  )
  await page.route('**/api/platform/v1/users/me', (route) =>
    route.fulfill({
      json: {
        id: 'u1',
        email: 'u@x.io',
        displayName: 'Utente Uno',
        role: roles[0],
        status: 'active',
        tenantId: 'tenant-1',
      },
    }),
  )
  await page.route('**/api/platform/v1/accounts/me', (route) =>
    route.fulfill({ json: { id: 'a1', name: 'Acme', status: 'active' } }),
  )
  await page.route('**/api/platform/v1/me/entitlements', (route) =>
    route.fulfill({ json: { entitlements: [] } }),
  )
  await page.route('**/api/platform/v1/me/catalog', (route) =>
    apps === 'error'
      ? route.fulfill({ status: 503, json: { title: 'Service Unavailable' } })
      : route.fulfill({ json: { apps } }),
  )
}

const card = (page: Page, name: string) => page.getByRole('article', { name })

test('[L2-CATALOG] la vetrina si raggiunge dal menu e mostra i sei stati con l’azione giusta', async ({
  page,
}) => {
  await mockBackend(page)
  await page.goto('/')

  await page.getByRole('link', { name: 'App catalog' }).click()
  await expect(page).toHaveURL(/\/catalog$/)
  await expect(page.getByRole('heading', { name: 'App catalog', level: 1 })).toBeVisible()

  await expect(card(page, 'Notes').getByRole('button', { name: 'Subscribe' })).toBeEnabled()
  await expect(card(page, 'Notes')).toContainText('from')
  await expect(card(page, 'Teams').getByRole('button', { name: 'Open' })).toBeVisible()
  await expect(card(page, 'Teams')).toContainText('Teams Pro')
  await expect(card(page, 'Books')).toContainText('Trial ends')
  await expect(card(page, 'Timesheets').getByRole('button', { name: 'Fix payment' })).toBeVisible()
  await expect(
    card(page, 'Bookings').getByRole('button', { name: 'Undo cancellation' }),
  ).toBeVisible()

  // App spenta dalla piattaforma: lo stato si vede, e nessuna azione d'acquisto viene offerta.
  await expect(card(page, 'Mini-CRM')).toContainText('Disabled by platform')
  await expect(card(page, 'Mini-CRM').getByRole('button', { name: 'Contact support' })).toBeVisible()
  await expect(card(page, 'Mini-CRM').getByRole('button', { name: 'Subscribe' })).toHaveCount(0)
})

test('[L2-CATALOG] la ricerca filtra, conta e ha uno stato vuoto distinto dall’errore', async ({
  page,
}) => {
  await mockBackend(page)
  await page.goto('/catalog')
  await expect(page.getByText('6 apps')).toBeVisible()

  const search = page.getByRole('searchbox', { name: 'Search apps' })
  await search.fill('teams')
  await expect(page.getByText('1 app', { exact: true })).toBeVisible()
  await expect(card(page, 'Notes')).toHaveCount(0)

  await search.fill('zzz')
  await expect(page.getByText('No apps match your search')).toBeVisible()
  await page.getByRole('button', { name: 'Clear search' }).click()
  await expect(card(page, 'Notes')).toBeVisible()
})

test('[L2-CATALOG] la paginazione scorre il catalogo e la ricerca riporta alla prima pagina', async ({
  page,
}) => {
  const many = [
    ...SIX_STATES,
    app({ appSlug: 'x1', name: 'Zeta uno', descriptions: { en: 'primo riempitivo' } }),
    app({ appSlug: 'x2', name: 'Zeta due', descriptions: { en: 'secondo riempitivo' } }),
  ]
  await mockBackend(page, { apps: many })
  await page.goto('/catalog')

  await expect(page.getByText('Page 1 of 2')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Previous' })).toBeDisabled()
  await expect(card(page, 'Zeta uno')).toHaveCount(0)

  await page.getByRole('button', { name: 'Next' }).click()
  await expect(page.getByText('Page 2 of 2')).toBeVisible()
  await expect(card(page, 'Zeta uno')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Next' })).toBeDisabled()

  // Cercare mentre si è a pagina due deve mostrare i risultati, non una pagina vuota.
  await page.getByRole('searchbox', { name: 'Search apps' }).fill('Zeta')
  await expect(page.getByText('Page 1 of 1')).toBeVisible()
  await expect(card(page, 'Zeta due')).toBeVisible()
})

test('[L2-CATALOG] a un member l’azione d’acquisto non è offerta, con la spiegazione', async ({
  page,
}) => {
  await mockBackend(page, { roles: ['member'] })
  await page.goto('/catalog')
  await expect(card(page, 'Notes').getByRole('button', { name: 'Subscribe' })).toBeDisabled()
  await expect(card(page, 'Notes')).toContainText('Ask an owner to activate it')
})

test('[L2-CATALOG] un guasto di lettura è un errore con riprova, mai “nessuna app”', async ({
  page,
}) => {
  await mockBackend(page, { apps: 'error' })
  await page.goto('/catalog')
  await expect(page.getByRole('alert')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Retry' })).toBeVisible()
  await expect(page.getByText('No apps match your search')).toHaveCount(0)
})

test('[L2-CATALOG] l’acquisto parte dalla vetrina e riusa il checkout esistente', async ({ page }) => {
  await mockBackend(page)
  await page.route('**/api/platform/v1/checkout/apps/notes/tiers', (route) =>
    route.fulfill({
      json: {
        appId: 'a-notes',
        slug: 'notes',
        name: 'Notes',
        tiers: [
          {
            tierId: 't-pro',
            key: 'pro',
            name: 'Notes Pro',
            trialDays: 14,
            limits: {},
            features: {},
            prices: [
              { billingCycle: 'monthly', amount: 900, currency: 'EUR' },
              { billingCycle: 'annual', amount: 9000, currency: 'EUR' },
            ],
          },
        ],
      },
    }),
  )
  await page.goto('/catalog')
  await card(page, 'Notes').getByRole('button', { name: 'Subscribe' }).click()

  await expect(page.getByText('Notes Pro')).toBeVisible()
  await expect(page.getByText('14-day free trial')).toBeVisible()

  // Tornare indietro riporta alla vetrina, che è dove si osserva il cambio di stato della card.
  await page.getByRole('button', { name: 'Back' }).click()
  await expect(card(page, 'Notes').getByRole('button', { name: 'Subscribe' })).toBeVisible()
})
