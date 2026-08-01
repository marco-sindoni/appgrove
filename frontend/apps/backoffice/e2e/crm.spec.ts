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

/** Avvia la SPA autenticata come owner; backend crm simulato (contatti/posti mutabili in memoria). */
async function mockAuthed(page: Page, env: 'local' | 'test') {
  const contacts: Array<Record<string, unknown>> = [
    { id: 'contact-1', displayName: 'Mario Rossi', organization: 'ACME S.p.A.', stage: 'qualified' },
  ]
  let seats: Array<{ id: string; userId: string }> = [{ id: 'seat-1', userId: 'u-owner' }]
  const quota = { metric: 'seats', used: seats.length, limit: 2, remaining: 2 - seats.length }
  const seatSummary = () => ({
    used: seats.length,
    limit: quota.limit,
    remaining: Math.max(0, quota.limit - seats.length),
    seats,
  })

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
            appSlug: 'crm',
            tierKey: 'free',
            limits: { seats: { cap: 2, nature: 'stock', window: null } },
          },
        ]
      : []
  await page.route('**/api/platform/v1/me/entitlements', (route) =>
    route.fulfill({ json: { entitlements } }),
  )
  await page.route('**/api/crm/v1/quota', (route) => route.fulfill({ json: quota }))
  await page.route('**/api/crm/v1/contacts?*', (route) =>
    route.fulfill({
      json: { content: contacts, page: 0, size: 20, totalElements: contacts.length, totalPages: 1 },
    }),
  )
  await page.route('**/api/crm/v1/contacts', async (route, request) => {
    const body = request.postDataJSON() as { displayName: string; organization?: string }
    const created = {
      id: `contact-${contacts.length + 1}`,
      displayName: body.displayName,
      organization: body.organization,
      stage: 'lead',
    }
    contacts.push(created)
    await route.fulfill({ status: 201, json: created })
  })
  await page.route('**/api/crm/v1/seats', async (route, request) => {
    if (request.method() === 'POST') {
      const body = request.postDataJSON() as { userId: string }
      const seat = { id: `seat-${seats.length + 1}`, userId: body.userId }
      seats = [...seats, seat]
      await route.fulfill({ status: 201, json: seat })
      return
    }
    await route.fulfill({ json: seatSummary() })
  })
}

test('[L2-CRM] ciclo principale crm: elenco + banner posti → crea contatto → compare in elenco', async ({ page }) => {
  await mockAuthed(page, 'local') // env local → app entitled

  await page.goto('/app/crm')
  await expect(page.getByRole('heading', { name: 'Contatti', level: 1 })).toBeVisible()
  await expect(page.getByRole('cell', { name: 'Mario Rossi' })).toBeVisible()
  await expect(page.getByText('1 / 2')).toBeVisible()

  await page.getByRole('button', { name: 'Nuovo contatto' }).click()
  await page.getByLabel('Nome', { exact: true }).fill('Contatto E2E')
  await page.getByRole('button', { name: 'Crea contatto' }).click()

  await expect(page.getByRole('cell', { name: 'Contatto E2E' })).toBeVisible()
})

test('[L2-CRM] schermata Membri: assegna un posto B2B', async ({ page }) => {
  await mockAuthed(page, 'local')

  await page.goto('/app/crm/members')
  await expect(page.getByRole('heading', { name: 'Membri', level: 1 })).toBeVisible()
  await expect(page.getByRole('cell', { name: 'u-owner' })).toBeVisible()

  await page.getByLabel('Identificativo utente').fill('u-collega')
  await page.getByRole('button', { name: 'Assegna posto' }).click()

  await expect(page.getByRole('cell', { name: 'u-collega' })).toBeVisible()
  await expect(page.getByText('2 / 2')).toBeVisible()
})

test("[L2-CRM] un'app non entitled è bloccata dalla route guard", async ({ page }) => {
  await mockAuthed(page, 'test') // env test → nessun entitlement
  await page.goto('/app/crm')
  await expect(page.getByText('You don’t have access to this app')).toBeVisible()
  await expect(page.getByTestId('crm-module')).toHaveCount(0)
})
