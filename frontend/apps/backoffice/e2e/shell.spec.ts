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
/**
 * Mocka il backend minimo della shell. `entitlements` descrive la risposta di
 * `/me/entitlements`: un elenco di slug, oppure `'error'` per simulare l'endpoint non raggiungibile
 * (UC 0077: la shell deve dirlo, non fingere "nessuna app").
 */
async function mockBackend(
  page: Page,
  env: 'local' | 'test',
  entitlements: string[] | 'error' = [],
) {
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
  await page.route('**/api/platform/v1/me/entitlements', (route) =>
    entitlements === 'error'
      ? route.fulfill({ status: 503, json: { title: 'Service Unavailable' } })
      : route.fulfill({ json: { entitlements: entitlements.map((appSlug) => ({ appSlug })) } }),
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
  await mockBackend(page, 'test', []) // entitlement letti correttamente: elenco vuoto
  await page.goto('/app/demo')
  await expect(page.getByText('You don’t have access to this app')).toBeVisible()
  await expect(page.getByTestId('demo-module')).toHaveCount(0)
})

test('senza app attive il menu invita all’acquisto (UC 0077)', async ({ page }) => {
  await mockBackend(page, 'test', [])
  await page.goto('/')
  await expect(page.getByText('No active apps yet')).toBeVisible()
  await expect(page.getByRole('link', { name: 'Browse the apps' })).toBeVisible()
})

test('entitlement non leggibili: il menu lo dice e offre la riprova, non "nessuna app" (UC 0077)', async ({
  page,
}) => {
  await mockBackend(page, 'test', 'error')
  await page.goto('/app/demo')

  // Il menu non mente: errore + riprova, mai lo stato vuoto…
  await expect(page.getByRole('alert').first()).toContainText('We couldn’t load your apps')
  await expect(page.getByRole('button', { name: 'Retry' })).toBeVisible()
  await expect(page.getByText('No active apps yet')).toHaveCount(0)
  // …e la route non trasforma un guasto in un diniego di accesso.
  await expect(page.getByText('You don’t have access to this app')).toHaveCount(0)
  await expect(page.getByTestId('demo-module')).toHaveCount(0)
})
