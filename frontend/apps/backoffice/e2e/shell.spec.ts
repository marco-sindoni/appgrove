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

/**
 * Login programmatico: invece di compilare la (futura, UC 0017) UI di login, mockiamo il
 * `/api/auth/refresh` che la shell chiama al load → la sessione viene ripristinata in memoria.
 */
async function mockBackend(page: Page, env: 'local' | 'test') {
  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env, authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
    }),
  )
  await page.route('**/api/auth/refresh', (route) =>
    route.fulfill({ json: { access_token: accessToken, id_token: idToken, token_type: 'Bearer' } }),
  )
  await page.route('**/api/platform/v1/users/me', (route) =>
    route.fulfill({
      json: { id: 'u1', email: 'u@x.io', displayName: 'Utente Uno', role: 'owner', status: 'active', tenantId: 'tenant-1' },
    }),
  )
  await page.route('**/api/platform/v1/accounts/me', (route) =>
    route.fulfill({ json: { id: 'a1', name: 'Acme', status: 'active' } }),
  )
}

test('ripristina la sessione, naviga la shell e monta il modulo entitled', async ({ page }) => {
  await mockBackend(page, 'local') // env local → demo entitled
  await page.goto('/')

  // chrome permanente + sezioni
  await expect(page.getByText('Platform')).toBeVisible()
  await expect(page.getByText('Your apps')).toBeVisible()
  await expect(page.getByText('Demo app')).toBeVisible()

  // monta il modulo entitled
  await page.getByRole('link', { name: 'Overview' }).first().click()
  await expect(page).toHaveURL(/\/app\/demo/)
  await expect(page.getByTestId('demo-module')).toBeVisible()
})

test('un modulo NON entitled è bloccato dalla route guard', async ({ page }) => {
  await mockBackend(page, 'test') // env test → nessun entitlement
  await page.goto('/app/demo')
  await expect(page.getByText('You don’t have access to this app')).toBeVisible()
  await expect(page.getByTestId('demo-module')).toHaveCount(0)
})
