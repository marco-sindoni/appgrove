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

/** Avvia la SPA già autenticata come owner, con membri/inviti mockati (inviti mutabili in memoria). */
async function mockAuthed(page: Page) {
  const invites: Array<Record<string, unknown>> = []
  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env: 'local', authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
    }),
  )
  await page.route('**/api/auth/refresh', (route) => route.fulfill({ json: tokenBody }))
  await page.route('**/api/platform/v1/users/me', (route) =>
    route.fulfill({ json: { id: 'u1', email: 'owner@acme.test', displayName: 'Owner', role: 'owner', status: 'active', tenantId: 'tenant-1' } }),
  )
  await page.route('**/api/platform/v1/accounts/me', (route) =>
    route.fulfill({ json: { id: 'a1', name: 'Acme', status: 'active' } }),
  )
  await page.route('**/api/platform/v1/users?*', (route) =>
    route.fulfill({
      json: {
        content: [
          { id: 'u1', email: 'owner@acme.test', displayName: 'Owner', role: 'owner', status: 'active', tenantId: 'tenant-1' },
        ],
        page: 0,
        size: 100,
        totalElements: 1,
      },
    }),
  )
  await page.route('**/api/platform/v1/invitations?*', (route) =>
    route.fulfill({ json: { content: invites, page: 0, size: 100, totalElements: invites.length } }),
  )
  await page.route('**/api/platform/v1/invitations', async (route, request) => {
    const body = request.postDataJSON() as { email: string; role: string }
    const created = { id: 'inv-1', email: body.email, role: body.role, status: 'pending', expiresAt: '2026-07-03T00:00:00Z', token: 'tok-1' }
    invites.push({ ...created, token: undefined })
    await route.fulfill({ status: 201, json: created })
  })
  await page.route('**/api/platform/v1/invitations/*', async (route) => {
    invites.length = 0
    await route.fulfill({ status: 204, body: '' })
  })
  await page.route('**/api/auth/invitations/send', (route) => route.fulfill({ status: 202, body: '' }))
}

test('membri: invita → compare tra i pendenti → revoca', async ({ page }) => {
  await mockAuthed(page)

  await page.goto('/members')
  await expect(page.getByRole('heading', { name: 'Members', level: 1 })).toBeVisible()

  await page.getByLabel('Email').fill('teammate@acme.test')
  await page.getByRole('button', { name: 'Send invitation' }).click()

  await expect(page.getByText('Invitation sent to teammate@acme.test.')).toBeVisible()
  await expect(page.getByRole('cell', { name: 'teammate@acme.test' })).toBeVisible()

  await page.getByRole('button', { name: 'Revoke' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Revoke' }).click()
  await expect(page.getByRole('cell', { name: 'teammate@acme.test' })).toHaveCount(0)
})
