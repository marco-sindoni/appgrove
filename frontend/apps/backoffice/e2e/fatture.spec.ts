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

/** Avvia la SPA autenticata come owner; backend fatture mockato (fatture mutabili in memoria). */
async function mockAuthed(page: Page, env: 'local' | 'test') {
  const invoices: Array<Record<string, unknown>> = [
    { id: 'inv-1', number: '2026-0001', customerName: 'Mario Rossi', status: 'issued', currency: 'EUR', totalAmount: 120 },
  ]
  const quota = { metric: 'fatture', used: invoices.length, limit: 10, remaining: 10 - invoices.length }

  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env, authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
    }),
  )
  await page.route('**/api/auth/refresh', (route) => route.fulfill({ json: tokenBody }))
  await page.route('**/api/platform/v1/users/me', (route) =>
    route.fulfill({ json: { id: 'u1', email: 'owner@acme.test', displayName: 'Owner', role: 'owner', status: 'active', tenantId: 'tenant-1' } }),
  )
  await page.route('**/api/platform/v1/accounts/me', (route) =>
    route.fulfill({ json: { id: 'a1', name: 'Acme', status: 'active' } }),
  )
  await page.route('**/api/fatture/v1/quota', (route) => route.fulfill({ json: quota }))
  await page.route('**/api/fatture/v1/invoices?*', (route) =>
    route.fulfill({ json: { content: invoices, page: 0, size: 20, totalElements: invoices.length, totalPages: 1 } }),
  )
  await page.route('**/api/fatture/v1/invoices', async (route, request) => {
    const body = request.postDataJSON() as { customerName: string }
    const created = {
      id: `inv-${invoices.length + 1}`,
      number: `2026-000${invoices.length + 1}`,
      customerName: body.customerName,
      status: 'draft',
      currency: 'EUR',
      totalAmount: 0,
      lines: [],
    }
    invoices.push(created)
    quota.used += 1
    quota.remaining -= 1
    await route.fulfill({ status: 201, json: created })
  })
}

test('core-loop fatture: lista + banner quota → crea → compare in lista', async ({ page }) => {
  await mockAuthed(page, 'local') // env local → fatture entitled

  await page.goto('/app/fatture')
  await expect(page.getByRole('heading', { name: 'Fatture', level: 1 })).toBeVisible()
  await expect(page.getByRole('cell', { name: 'Mario Rossi' })).toBeVisible()
  // banner quota consumo/limite
  await expect(page.getByText('1 / 10')).toBeVisible()

  await page.getByRole('button', { name: 'Nuova fattura' }).click()
  await page.getByLabel('Nome cliente').fill('Cliente E2E')
  await page.getByRole('button', { name: 'Crea fattura' }).click()

  await expect(page.getByRole('cell', { name: 'Cliente E2E' })).toBeVisible()
})

test("un'app non entitled è bloccata dalla route guard", async ({ page }) => {
  await mockAuthed(page, 'test') // env test → nessun entitlement
  await page.goto('/app/fatture')
  await expect(page.getByText('You don’t have access to this app')).toBeVisible()
  await expect(page.getByTestId('fatture-module')).toHaveCount(0)
})
