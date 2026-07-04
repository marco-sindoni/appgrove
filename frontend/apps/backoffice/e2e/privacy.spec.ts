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
 * Avvia la SPA autenticata come owner con lo stato GDPR mockato in memoria (UC 0033):
 * profilo, account (con eliminazione richiedibile/annullabile), export job, recesso per-app.
 */
async function mockAuthed(page: Page) {
  let me = { id: 'u1', email: 'owner@acme.test', displayName: 'Owner', role: 'owner', status: 'active', tenantId: 'tenant-1' }
  let account: Record<string, unknown> = { id: 'a1', name: 'Acme', status: 'active' }
  const jobs = new Map<string, Record<string, unknown>>()
  const purges: Array<{ slug: string; exportJobId: string }> = []

  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env: 'local', authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
    }),
  )
  await page.route('**/api/auth/refresh', (route) => route.fulfill({ json: tokenBody }))
  await page.route('**/api/platform/v1/me/entitlements', (route) =>
    route.fulfill({ json: { entitlements: [] } }),
  )
  await page.route('**/api/platform/v1/users/me', async (route, request) => {
    if (request.method() === 'PATCH') {
      const body = request.postDataJSON() as { displayName: string }
      me = { ...me, displayName: body.displayName }
    }
    await route.fulfill({ json: me })
  })
  await page.route('**/api/platform/v1/users/me/export', (route) =>
    route.fulfill({
      json: { generatedAt: 'now', profile: me, account: { id: 'a1', name: 'Acme' }, invitations: [] },
      headers: { 'content-disposition': 'attachment; filename="appgrove-profilo.json"' },
    }),
  )
  await page.route('**/api/platform/v1/accounts/me', (route) => route.fulfill({ json: account }))
  await page.route('**/api/platform/v1/accounts/me/deletion', async (route, request) => {
    if (request.method() === 'POST') {
      account = {
        ...account,
        status: 'pending_deletion',
        deletionRequestedAt: '2026-07-04T10:00:00Z',
        deletionEffectiveAt: '2026-07-18T10:00:00Z',
      }
      await route.fulfill({ status: 202, json: account })
    } else {
      account = { ...account, status: 'active', deletionRequestedAt: null, deletionEffectiveAt: null }
      await route.fulfill({ json: account })
    }
  })
  await page.route('**/api/platform/v1/me/subscriptions', (route) =>
    route.fulfill({
      json: { subscriptions: [{ appSlug: 'fatture', appName: 'Fatture', status: 'active', phase: 'ACTIVE' }] },
    }),
  )
  await page.route('**/api/platform/v1/gdpr/exports', async (route, request) => {
    const body = request.postDataJSON() as { kind: string; appId?: string }
    const id = `job-${jobs.size + 1}`
    const job = {
      id,
      kind: body.kind,
      appId: body.appId,
      status: 'COMPLETED',
      progress: { completed: 1, total: 1 },
      items: [],
    }
    jobs.set(id, job)
    await route.fulfill({ status: 202, json: job })
  })
  await page.route('**/api/platform/v1/gdpr/exports/*', (route, request) => {
    const id = request.url().split('/').pop() as string
    return route.fulfill({ json: jobs.get(id) })
  })
  await page.route('**/api/platform/v1/gdpr/exports/*/download', (route) =>
    route.fulfill({ json: { url: `${ORIGIN}/fake-export.zip`, expiresAt: '2026-07-11T10:00:00Z' } }),
  )
  await page.route('**/api/platform/v1/gdpr/apps/*/withdrawal', async (route, request) => {
    const slug = request.url().split('/').at(-2) as string
    const body = request.postDataJSON() as { exportJobId: string }
    purges.push({ slug, exportJobId: body.exportJobId })
    await route.fulfill({ status: 202, json: { appId: slug, status: 'PURGE_REQUESTED' } })
  })
  return { purges }
}

test('I miei dati: rettifica del nome visualizzato', async ({ page }) => {
  await mockAuthed(page)
  await page.goto('/privacy')
  await expect(page.getByRole('heading', { name: 'My data', level: 1 })).toBeVisible()

  await page.getByLabel('Display name').fill('Owner Rettificato')
  await page.getByRole('button', { name: 'Save' }).click()
  await expect(page.getByText('Saved')).toBeVisible()
})

test('I miei dati: export profilo scarica un JSON', async ({ page }) => {
  await mockAuthed(page)
  await page.goto('/privacy')

  const downloadPromise = page.waitForEvent('download')
  await page.getByRole('button', { name: 'Download my data' }).click()
  const download = await downloadPromise
  expect(download.suggestedFilename()).toBe('appgrove-profilo.json')
})

test('I miei dati: export account → pronto → link con scadenza', async ({ page }) => {
  await mockAuthed(page)
  await page.goto('/privacy')

  await page.getByRole('button', { name: /Start export/ }).click()
  await expect(page.getByText('Export ready.')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Download archive' })).toBeEnabled()
})

test('I miei dati: recesso per-app esporta → conferma → recesso completato', async ({ page }) => {
  const { purges } = await mockAuthed(page)
  await page.goto('/privacy')

  await expect(page.getByText('Fatture')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Confirm withdrawal' })).toBeDisabled()

  await page.getByRole('button', { name: 'Export app data' }).click()
  await expect(page.getByRole('button', { name: 'Confirm withdrawal' })).toBeEnabled()

  await page.getByRole('button', { name: 'Confirm withdrawal' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Confirm withdrawal' }).click()

  await expect(page.getByText(/Withdrawal completed/)).toBeVisible()
  expect(purges).toEqual([{ slug: 'fatture', exportJobId: 'job-1' }])
})

test('I miei dati: elimina account → grace con scadenza → annulla', async ({ page }) => {
  await mockAuthed(page)
  await page.goto('/privacy')

  await page.getByRole('button', { name: 'Delete this account' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Delete this account' }).click()

  await expect(page.getByText(/Deletion requested/)).toBeVisible()
  await expect(page.getByText(/Permanent deletion on/)).toBeVisible()

  await page.getByRole('button', { name: 'Cancel deletion' }).click()
  await expect(page.getByText(/Deletion canceled/)).toBeVisible()
})
