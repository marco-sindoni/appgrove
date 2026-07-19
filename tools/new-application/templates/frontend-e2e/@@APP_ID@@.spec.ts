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
const accessToken = jwt({ sub: 'u1', tenant_id: 'tenant-1', roles: ['owner'], upn: 'owner@acme.test' })
const idToken = jwt({ sub: 'u1', email: 'owner@acme.test', name: 'Owner' })
const tokenBody = { access_token: accessToken, id_token: idToken, token_type: 'Bearer' }

/** Avvia la SPA autenticata come owner; backend @@APP_ID@@ simulato (record mutabili in memoria). */
async function mockAuthed(page: Page, env: 'local' | 'test') {
  const items: Array<Record<string, unknown>> = [
    { id: 'item-1', code: '2026-0001', contactName: 'Mario Rossi', status: 'active', currency: 'EUR', totalAmount: 120 },
  ]
  const quota = {
    metric: '@@METRIC@@',
    used: items.length,
    limit: @@FREE_CAP@@,
    remaining: @@FREE_CAP@@ - items.length,
  }

  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env, authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
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
  // Il registry deriva l'accesso da /me/entitlements (UC 0027):
  // 'local' → app entitled (baseline free); 'test' → nessun entitlement (per la route guard)
  const entitlements =
    env === 'local'
      ? [
          {
            appSlug: '@@APP_ID@@',
            tierKey: 'free',
            limits: { '@@METRIC@@': { cap: @@FREE_CAP@@, nature: 'flow', window: 'month' } },
          },
        ]
      : []
  await page.route('**/api/platform/v1/me/entitlements', (route) =>
    route.fulfill({ json: { entitlements } }),
  )
  await page.route('**/api/@@APP_ID@@/v1/quota', (route) => route.fulfill({ json: quota }))
  await page.route('**/api/@@APP_ID@@/v1/items?*', (route) =>
    route.fulfill({
      json: { content: items, page: 0, size: 20, totalElements: items.length, totalPages: 1 },
    }),
  )
  await page.route('**/api/@@APP_ID@@/v1/items', async (route, request) => {
    const body = request.postDataJSON() as { contactName: string }
    const created = {
      id: `item-${items.length + 1}`,
      code: `2026-000${items.length + 1}`,
      contactName: body.contactName,
      status: 'draft',
      currency: 'EUR',
      totalAmount: 0,
      lines: [],
    }
    items.push(created)
    quota.used += 1
    quota.remaining -= 1
    await route.fulfill({ status: 201, json: created })
  })
}

test('ciclo principale @@APP_ID@@: elenco + banner quota → crea → compare in elenco', async ({ page }) => {
  await mockAuthed(page, 'local') // env local → app entitled

  await page.goto('/app/@@APP_ID@@')
  await expect(page.getByRole('heading', { name: '@@APP_NAME@@', level: 1 })).toBeVisible()
  await expect(page.getByRole('cell', { name: 'Mario Rossi' })).toBeVisible()
  await expect(page.getByText(`1 / @@FREE_CAP@@`)).toBeVisible()

  await page.getByRole('button', { name: 'Nuovo record' }).click()
  await page.getByLabel('Nome contatto').fill('Contatto E2E')
  await page.getByRole('button', { name: 'Crea record' }).click()

  await expect(page.getByRole('cell', { name: 'Contatto E2E' })).toBeVisible()
})

test("un'app non entitled è bloccata dalla route guard", async ({ page }) => {
  await mockAuthed(page, 'test') // env test → nessun entitlement
  await page.goto('/app/@@APP_ID@@')
  await expect(page.getByText('You don’t have access to this app')).toBeVisible()
  await expect(page.getByTestId('@@APP_ID@@-module')).toHaveCount(0)
})
