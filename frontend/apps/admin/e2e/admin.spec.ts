import { test, expect, type Page } from '@playwright/test'

const ORIGIN = 'http://localhost:4174'

function base64url(obj: unknown): string {
  return Buffer.from(JSON.stringify(obj))
    .toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}
const jwt = (payload: Record<string, unknown>) =>
  `${base64url({ alg: 'none' })}.${base64url(payload)}.sig`
// L'access token porta `platform-admin`: il refresh-on-load stabilisce la sessione admin senza login.
const accessToken = jwt({
  sub: 'admin-1',
  tenant_id: 'tenant-1',
  roles: ['platform-admin'],
  upn: 'admin@x.io',
})
const idToken = jwt({ sub: 'admin-1', email: 'admin@x.io', name: 'Admin Uno' })
const tokenBody = { access_token: accessToken, id_token: idToken, token_type: 'Bearer' }

type AuditRow = {
  id: string
  appId: string
  appSlug: string
  appName: string
  fromStatus: string
  toStatus: string
  actor: string
  reason: string | null
  executedAt: string
}

/** Stato di partenza: già autenticato come platform-admin via refresh-on-load + mock degli endpoint admin. */
async function mockAuthed(page: Page) {
  const apps = [
    { id: 'app-1', slug: 'crm', name: 'CRM', userModel: 'b2b', status: 'active' },
    { id: 'app-2', slug: 'billing', name: 'Billing', userModel: 'b2c', status: 'inactive' },
  ]
  // Il registro delle transizioni (UC 0076) si comporta come lato servizio: una riga per ogni
  // transizione effettiva, più recenti prima, con la motivazione se data.
  const audit: AuditRow[] = []

  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env: 'local', authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
    }),
  )
  await page.route('**/api/auth/refresh', (route) => route.fulfill({ json: tokenBody }))
  await page.route('**/api/platform/v1/admin/overview', (route) =>
    route.fulfill({ json: { accounts: 12, users: 47, activeSubscriptions: 8, disabledApps: 2 } }),
  )
  await page.route('**/api/platform/v1/admin/accounts', (route) =>
    route.fulfill({
      json: [{ id: 'a-1', name: 'Acme', status: 'active', users: 5, activeSubscriptions: 2 }],
    }),
  )
  await page.route('**/api/platform/v1/admin/users', (route) => route.fulfill({ json: [] }))
  await page.route('**/api/platform/v1/admin/entitlements', (route) => route.fulfill({ json: [] }))
  await page.route('**/api/platform/v1/admin/billing', (route) => route.fulfill({ json: [] }))
  await page.route('**/api/platform/v1/admin/apps', (route) => route.fulfill({ json: apps }))
  await page.route('**/api/platform/v1/admin/apps/*', async (route) => {
    if (route.request().method() === 'PATCH') {
      const body = route.request().postDataJSON() as { status: string; reason?: string }
      const idx = apps.findIndex((a) => a.id === 'app-1')
      const from = apps[idx].status
      apps[idx] = { ...apps[idx], status: body.status }
      if (from !== body.status) {
        audit.unshift({
          id: `audit-${audit.length + 1}`,
          appId: apps[idx].id,
          appSlug: apps[idx].slug,
          appName: apps[idx].name,
          fromStatus: from,
          toStatus: body.status,
          actor: 'admin-1',
          reason: body.reason ?? null,
          executedAt: '2026-08-01T10:00:00Z',
        })
      }
      return route.fulfill({ json: apps[idx] })
    }
    return route.continue()
  })
  // Registrata DOPO il jolly: Playwright dà la precedenza all'ultima rotta registrata.
  await page.route('**/api/platform/v1/admin/apps/audit', (route) => route.fulfill({ json: audit }))
}

test('[L2-ADMIN-APPS] overview → apps → disabilita e riabilita un’app, con motivazione nel registro', async ({ page }) => {
  await mockAuthed(page)

  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Overview' })).toBeVisible()
  await expect(page.getByText('12')).toBeVisible()

  await page.getByRole('link', { name: 'Apps' }).click()
  await expect(page.getByRole('heading', { name: 'Apps', exact: true })).toBeVisible()
  // il registro parte vuoto e lo dice
  await expect(page.getByText('No disable recorded yet.')).toBeVisible()

  const appsTable = page.getByRole('table').first()
  await appsTable.getByRole('row', { name: /CRM/ }).getByRole('button', { name: 'Disable' }).click()

  const dialog = page.getByRole('dialog')
  // il copy distingue la pausa reversibile dalla dismissione definitiva (UC 0076)
  await expect(dialog.getByText(/reversible/i)).toBeVisible()
  await expect(dialog.getByText(/not the permanent removal/i)).toBeVisible()
  await dialog.getByLabel('Reason (optional)').fill('security incident')
  await dialog.getByRole('button', { name: 'Disable' }).click()

  // badge aggiornato nella tabella app…
  await expect(
    page.getByRole('table').first().getByRole('row', { name: /CRM/ }).getByText('Disabled'),
  ).toBeVisible()
  // …e transizione tracciata nel registro, con la motivazione
  const register = page.getByRole('table').nth(1)
  await expect(register.getByText('security incident')).toBeVisible()

  // riabilita: seconda riga nel registro, l'app torna attiva
  await page
    .getByRole('table')
    .first()
    .getByRole('row', { name: /CRM/ })
    .getByRole('button', { name: 'Re-enable' })
    .click()
  await page.getByRole('dialog').getByRole('button', { name: 'Re-enable' }).click()

  await expect(
    page.getByRole('table').first().getByRole('row', { name: /CRM/ }).getByText('Active'),
  ).toBeVisible()
  await expect(page.getByRole('table').nth(1).getByRole('row')).toHaveCount(3) // intestazione + 2 transizioni
})
