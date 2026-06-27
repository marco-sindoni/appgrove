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

/** Stato di partenza: già autenticato come platform-admin via refresh-on-load + mock degli endpoint admin. */
async function mockAuthed(page: Page) {
  const apps = [
    { id: 'app-1', slug: 'crm', name: 'CRM', userModel: 'b2b', status: 'active' },
    { id: 'app-2', slug: 'billing', name: 'Billing', userModel: 'b2c', status: 'inactive' },
  ]

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
      const body = route.request().postDataJSON() as { status: string }
      const idx = apps.findIndex((a) => a.id === 'app-1')
      apps[idx] = { ...apps[idx], status: body.status }
      return route.fulfill({ json: apps[idx] })
    }
    return route.continue()
  })
}

test('overview → apps → disabilita un’app (la riga mostra inactive)', async ({ page }) => {
  await mockAuthed(page)

  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Overview' })).toBeVisible()
  await expect(page.getByText('12')).toBeVisible()

  await page.getByRole('link', { name: 'Apps' }).click()
  await expect(page.getByRole('heading', { name: 'Apps' })).toBeVisible()

  const crmRow = page.getByRole('row', { name: /CRM/ })
  await crmRow.getByRole('button', { name: 'Disable' }).click()

  const dialog = page.getByRole('dialog')
  await dialog.getByRole('button', { name: 'Disable' }).click()

  await expect(page.getByRole('row', { name: /CRM/ }).getByText('inactive')).toBeVisible()
})
