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

const idToken = jwt({ sub: 'user-1', email: 'u@x.io', name: 'Marco' })
const accessToken = (roles: string[]) =>
  jwt({ sub: 'user-1', tenant_id: 'tenant-1', roles, upn: 'u@x.io' })

interface CatalogApp {
  appSlug: string
  name: string
  category?: string
  descriptions?: Record<string, string>
  state: string
  planName?: string
}

/** Le due app impacchettate nel frontend, in uso: sono quelle che hanno anche un descrittore di quota. */
const IN_USE: CatalogApp[] = [
  { appSlug: 'fatture', name: 'Invoices', category: 'blue', state: 'active', planName: 'Free' },
  { appSlug: 'crm', name: 'Mini-CRM', category: 'teal', state: 'trial' },
  { appSlug: 'notes', name: 'Notes', category: 'amber', state: 'available' },
]

async function mockBackend(
  page: Page,
  options: {
    apps?: CatalogApp[] | 'error'
    roles?: string[]
    twoFaEnabled?: boolean
    fattureQuota?: 'error' | { used: number; limit: number | null }
  } = {},
) {
  const {
    apps = IN_USE,
    roles = ['owner'],
    twoFaEnabled = true,
    fattureQuota = { used: 18, limit: 20 },
  } = options

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
  await page.route('**/api/auth/2fa/status', (route) =>
    route.fulfill({ json: { enabled: twoFaEnabled } }),
  )
  await page.route('**/api/platform/v1/users/me', (route) =>
    route.fulfill({
      json: {
        id: 'u1',
        email: 'u@x.io',
        displayName: 'Marco',
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
  await page.route('**/api/platform/v1/me/subscriptions', (route) =>
    route.fulfill({
      json: {
        subscriptions: [
          { appSlug: 'fatture', phase: 'ACTIVE', currentPeriodEnd: '2027-01-31T00:00:00Z' },
        ],
      },
    }),
  )
  await page.route('**/api/platform/v1/users?**', (route) =>
    route.fulfill({ json: { content: [], totalElements: 4 } }),
  )
  await page.route('**/api/platform/v1/invitations?**', (route) =>
    route.fulfill({ json: { content: [], totalElements: 1 } }),
  )
  await page.route('**/api/fatture/v1/quota', (route) =>
    fattureQuota === 'error'
      ? route.fulfill({ status: 503, json: { title: 'Service Unavailable' } })
      : route.fulfill({
          json: {
            metric: 'invoices',
            used: fattureQuota.used,
            limit: fattureQuota.limit,
            remaining: fattureQuota.limit == null ? null : fattureQuota.limit - fattureQuota.used,
          },
        }),
  )
  await page.route('**/api/crm/v1/quota', (route) =>
    route.fulfill({ json: { metric: 'seats', used: 1, limit: null, remaining: null } }),
  )
}

const card = (page: Page, name: string) => page.getByRole('article', { name })

test('[L2-DASHBOARD] la pagina d’atterraggio è una panoramica: saluto, app in uso, riepilogo, scorciatoie', async ({
  page,
}) => {
  await mockBackend(page)
  await page.goto('/')

  await expect(page.getByRole('heading', { name: 'Welcome back, Marco', level: 1 })).toBeVisible()
  await expect(page.getByText('Here’s what’s happening in the Acme workspace.')).toBeVisible()

  // una card per app in uso, con consumo e azioni; ciò che è solo disponibile resta in vetrina
  const invoices = card(page, 'Invoices')
  await expect(invoices.getByText('Active')).toBeVisible()
  await expect(invoices.getByText('18 of 20 invoices')).toBeVisible()
  await expect(invoices.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '90')
  await expect(invoices.getByRole('button', { name: 'Open' })).toBeVisible()
  await expect(invoices.getByRole('button', { name: 'Manage plan' })).toBeVisible()
  await expect(card(page, 'Mini-CRM').getByText('1 posti — no limit')).toBeVisible()
  await expect(card(page, 'Notes')).toHaveCount(0)

  // riepilogo e scorciatoie
  const glance = page.getByRole('complementary', { name: 'Workspace at a glance' })
  await expect(glance.getByText('Members')).toBeVisible()
  await expect(glance.getByText('4')).toBeVisible()
  await expect(glance.getByText('Next renewal')).toBeVisible()
  await glance.getByRole('button', { name: 'Browse the catalog' }).click()
  await expect(page).toHaveURL(/\/catalog$/)
})

test('[L2-DASHBOARD] avvisa del pagamento in sospeso e del secondo fattore, in ordine di gravità', async ({
  page,
}) => {
  await mockBackend(page, {
    twoFaEnabled: false,
    apps: [{ appSlug: 'crm', name: 'Mini-CRM', state: 'payment_pending' }],
  })
  await page.goto('/')

  const alerts = page.getByRole('alert')
  await expect(alerts).toHaveCount(2)
  await expect(alerts.first()).toContainText('Mini-CRM has a payment pending')
  await expect(alerts.last()).toContainText('enable two-factor authentication')

  await alerts.last().getByRole('button', { name: 'Enable 2FA' }).click()
  await expect(page).toHaveURL(/\/security$/)
})

test('[L2-DASHBOARD] con il secondo fattore attivo nessuno lo propone più, nemmeno nel guscio', async ({
  page,
}) => {
  await mockBackend(page, { twoFaEnabled: true })
  await page.goto('/')

  await expect(card(page, 'Invoices')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Enable 2FA' })).toHaveCount(0)
  await expect(page.getByText('Protect your account')).toHaveCount(0)
})

test('[L2-DASHBOARD] un workspace senza app in uso invita al catalogo, non mostra una griglia vuota', async ({
  page,
}) => {
  await mockBackend(page, { apps: [{ appSlug: 'notes', name: 'Notes', state: 'available' }] })
  await page.goto('/')

  await expect(page.getByRole('article')).toHaveCount(0)
  await page.getByText('Your workspace is empty').click()
  await expect(page).toHaveURL(/\/catalog$/)
  await expect(page.getByRole('heading', { name: 'App catalog', level: 1 })).toBeVisible()
})

test('[L2-DASHBOARD] il guasto di una fonte degrada la sua sola sezione, il resto resta utile', async ({
  page,
}) => {
  await mockBackend(page, { apps: 'error' })
  await page.goto('/')

  await expect(page.getByText('We couldn’t load your apps.')).toBeVisible({ timeout: 15_000 })
  await expect(page.getByRole('button', { name: 'Retry' })).toBeVisible()
  // riepilogo e scorciatoie restano: la pagina non è tutta rossa
  const glance = page.getByRole('complementary', { name: 'Workspace at a glance' })
  await expect(glance.getByText('Next renewal')).toBeVisible()
  await expect(glance.getByRole('button', { name: 'Payments and receipts' })).toBeVisible()
})

test('[L2-DASHBOARD] il guasto della quota di un’app non spegne la card dell’altra', async ({
  page,
}) => {
  await mockBackend(page, { fattureQuota: 'error' })
  await page.goto('/')

  await expect(card(page, 'Mini-CRM').getByText('1 posti — no limit')).toBeVisible()
  const invoices = card(page, 'Invoices')
  await expect(invoices.getByRole('progressbar')).toHaveCount(0)
  await expect(invoices.getByRole('button', { name: 'Open' })).toBeVisible()
})

test('[L2-DASHBOARD] l’identificativo del workspace vive in Account, con la copia, e non più in Dashboard', async ({
  page,
}) => {
  await mockBackend(page)
  await page.goto('/')

  await expect(page.getByText('tenant-1')).toHaveCount(0)

  await page.getByRole('link', { name: 'Account' }).click()
  await expect(page.getByRole('heading', { name: 'Account', level: 1 })).toBeVisible()
  // "Workspace" compare anche nel menu laterale: si guarda dentro il contenuto, non nella pagina intera
  const content = page.getByRole('main')
  await expect(content.getByRole('heading', { name: 'Workspace' })).toBeVisible()
  await expect(content.getByText('Acme')).toBeVisible()
  await expect(content.getByText('tenant-1')).toBeVisible()
  await expect(content.getByText('Share this ID with support when opening a ticket.')).toBeVisible()
  await expect(content.getByRole('button', { name: 'Copy workspace ID' })).toBeVisible()
})
