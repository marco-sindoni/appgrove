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

/**
 * Avvia la SPA già autenticata come owner, con l'elenco delle persone e gli inviti simulati (gli
 * inviti sono mutabili in memoria, così l'invio e la revoca si vedono davvero cambiare la tabella).
 *
 * Le persone sono tre e diverse fra loro **di proposito**: l'owner con accesso implicito a due
 * applicazioni, una persona abilitata a una sola, e una che non è abilitata a nulla. Sono i tre casi
 * che la colonna delle applicazioni deve saper dire (UC 0100).
 */
async function mockAuthed(page: Page) {
  const invites: Array<Record<string, unknown>> = []
  const people = [
    {
      id: 'u1',
      email: 'owner@acme.test',
      displayName: 'Owner',
      role: 'owner',
      status: 'active',
      tenantId: 'tenant-1',
      joinedAt: '2024-01-01T00:00:00Z',
      apps: [
        { appId: 'app-1', app: 'crm', implicit: true },
        { appId: 'app-2', app: 'fatture', implicit: true },
      ],
    },
    {
      id: 'u2',
      email: 'teammate@acme.test',
      displayName: 'Teammate',
      role: 'member',
      status: 'active',
      tenantId: 'tenant-1',
      joinedAt: '2025-04-05T00:00:00Z',
      apps: [{ appId: 'app-1', app: 'crm', role: 'editor', implicit: false }],
    },
    {
      id: 'u3',
      email: 'nuova@acme.test',
      displayName: 'Nuova',
      role: 'member',
      status: 'active',
      tenantId: 'tenant-1',
      joinedAt: '2026-01-09T00:00:00Z',
      apps: [],
    },
  ]

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
  // Entitlement del tenant: va simulato sempre, altrimenti la shell segnala (giustamente) che non
  // riesce a leggerli — UC 0077.
  await page.route('**/api/platform/v1/me/entitlements', (route) =>
    route.fulfill({ json: { entitlements: [] } }),
  )
  await page.route('**/api/platform/v1/users?*', (route) =>
    route.fulfill({ json: { content: people, page: 0, size: 100, totalElements: people.length } }),
  )
  await page.route('**/api/platform/v1/invitations?*', (route) =>
    route.fulfill({ json: { content: invites, page: 0, size: 100, totalElements: invites.length } }),
  )
  await page.route('**/api/platform/v1/invitations', async (route, request) => {
    const body = request.postDataJSON() as Record<string, unknown>
    const created = {
      id: 'inv-1',
      email: body.email,
      status: 'pending',
      expiresAt: '2026-07-03T00:00:00Z',
      token: 'tok-1',
    }
    invites.push({ id: 'inv-1', email: body.email, status: 'pending', expiresAt: '2026-07-03T00:00:00Z' })
    await route.fulfill({ status: 201, json: created })
  })
  await page.route('**/api/platform/v1/invitations/*', async (route) => {
    invites.length = 0
    await route.fulfill({ status: 204, body: '' })
  })
  await page.route('**/api/auth/invitations/send', (route) => route.fulfill({ status: 202, body: '' }))
}

test('[L2-MEMBERS] persone: elenco unico senza ruolo, applicazioni per persona, invito e revoca', async ({ page }) => {
  await mockAuthed(page)

  const inviteBodies: Array<Record<string, unknown>> = []
  page.on('request', (req) => {
    if (req.method() === 'POST' && req.url().endsWith('/api/platform/v1/invitations')) {
      inviteBodies.push(req.postDataJSON() as Record<string, unknown>)
    }
  })

  await page.goto('/members')
  await expect(page.getByRole('heading', { name: 'Members', level: 1 })).toBeVisible()

  // ── una sola tabella, e nessuna colonna di ruolo ────────────────────────────
  await expect(page.getByRole('table')).toHaveCount(1)
  await expect(page.getByRole('columnheader')).toHaveText([
    'Email',
    'Name',
    'Status',
    'Apps',
    'Joined',
    'Actions',
  ])
  await expect(page.getByRole('columnheader', { name: 'Role' })).toHaveCount(0)

  // ── la colonna delle applicazioni dice i tre casi, e il dettaglio è in sola lettura ──
  await expect(page.getByRole('button', { name: 'Apps for owner@acme.test' })).toHaveText('2 apps')
  await expect(page.getByRole('button', { name: 'Apps for teammate@acme.test' })).toHaveText('1 app')
  await expect(page.getByRole('button', { name: 'Apps for nuova@acme.test' })).toHaveText('No apps')

  await page.getByRole('button', { name: 'Apps for teammate@acme.test' }).click()
  await expect(page.getByText('crm')).toBeVisible()
  await expect(page.getByText('Editor')).toBeVisible()
  await expect(
    page.getByText('Roles on an app are changed from that app’s user management.'),
  ).toBeVisible()

  // ── l'invito chiede solo l'indirizzo, e la persona compare NELLO STESSO elenco ──
  await expect(
    page.getByText(
      'There is no role to choose: whoever joins is part of the workspace, and permissions are granted inside each app.',
    ),
  ).toBeVisible()

  await page.getByLabel('Email').fill('arriva@acme.test')
  await page.getByRole('button', { name: 'Send invitation' }).click()

  await expect(page.getByText('Invitation sent to arriva@acme.test.')).toBeVisible()
  const invited = page.getByRole('row').filter({ hasText: 'arriva@acme.test' })
  await expect(invited).toHaveCount(1)
  await expect(invited.getByText('Invitation pending')).toBeVisible()
  expect(inviteBodies).toEqual([{ email: 'arriva@acme.test' }])

  // ── revoca ─────────────────────────────────────────────────────────────────
  await page.getByRole('button', { name: 'Revoke' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Revoke' }).click()
  await expect(page.getByRole('cell', { name: 'arriva@acme.test' })).toHaveCount(0)
})
