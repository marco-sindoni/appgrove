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
const accessToken = jwt({ sub: 'user-1', tenant_id: 'tenant-1', roles: ['owner'], upn: 'u@x.io' })
const idToken = jwt({ sub: 'user-1', email: 'u@x.io', name: 'Utente Uno' })
const tokenBody = { access_token: accessToken, id_token: idToken, token_type: 'Bearer' }

/** Stato di partenza: anonimo (refresh-on-load fallisce) finché non si effettua il login/verifica. */
async function mockAnonymous(page: Page, authed: { value: boolean }) {
  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env: 'local', authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
    }),
  )
  await page.route('**/api/auth/refresh', (route) =>
    authed.value
      ? route.fulfill({ json: tokenBody })
      : route.fulfill({ status: 401, json: { title: 'Unauthorized' } }),
  )
  await page.route('**/api/platform/v1/users/me', (route) =>
    route.fulfill({ json: { id: 'u1', email: 'u@x.io', displayName: 'U', role: 'owner', status: 'active', tenantId: 'tenant-1' } }),
  )
  await page.route('**/api/platform/v1/accounts/me', (route) =>
    route.fulfill({ json: { id: 'a1', name: 'Acme', status: 'active' } }),
  )
}

test('login con credenziali porta alla dashboard', async ({ page }) => {
  const authed = { value: false }
  await mockAnonymous(page, authed)
  await page.route('**/api/auth/login', (route) => {
    authed.value = true
    return route.fulfill({ json: tokenBody })
  })

  await page.goto('/login')
  await page.getByLabel('Email').fill('owner@acme.test')
  await page.getByLabel('Password').fill('Password1!')
  await page.getByRole('button', { name: 'Sign in' }).click()

  await expect(page.getByText('Platform')).toBeVisible()
  await expect(page.getByText('Your apps')).toBeVisible()
})

test('signup wizard: account → verifica → workspace → done → dashboard', async ({ page }) => {
  const authed = { value: false }
  await mockAnonymous(page, authed)
  await page.route('**/api/auth/signup', (route) => route.fulfill({ status: 201, json: { status: 'verification_required' } }))
  await page.route('**/api/platform/v1/accounts/me', (route) => route.fulfill({ json: { id: 'a1', name: 'Acme', status: 'active' } }))

  await page.goto('/signup')
  await page.getByLabel('Email').fill('new@x.io')
  await page.getByLabel('Password', { exact: true }).fill('Password1!')
  await page.getByRole('button', { name: 'Create account' }).click()

  // step Verifica: simula il click sul link email abilitando il refresh, poi "continua"
  await expect(page.getByText(/We sent a verification link/)).toBeVisible()
  authed.value = true
  await page.getByRole('button', { name: 'I’ve verified — continue' }).click()

  // step Workspace → Done → dashboard
  await page.getByRole('button', { name: 'Continue' }).click()
  await page.getByRole('button', { name: 'Go to dashboard' }).click()
  await expect(page.getByText('Platform')).toBeVisible()
})
