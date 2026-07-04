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
const accessToken = jwt({
  sub: 'admin-1',
  tenant_id: 'tenant-1',
  roles: ['platform-admin'],
  upn: 'admin@x.io',
})
const idToken = jwt({ sub: 'admin-1', email: 'admin@x.io', name: 'Admin Uno' })
const tokenBody = { access_token: accessToken, id_token: idToken, token_type: 'Bearer' }

/**
 * Console "Diritti GDPR" (UC 0034): platform-admin autenticato via refresh-on-load, endpoint
 * mockati — aggregazione con deep-link, dettaglio ticket con risposta, limitazione art. 18.
 */
async function mockAuthed(page: Page) {
  let ticket: Record<string, unknown> = {
    id: 't-1',
    tenantId: 'a-1',
    accountName: 'Acme',
    type: 'privacy',
    subject: 'Export fallito',
    priority: 'high',
    status: 'open',
    dueAt: '2026-08-02T10:00:00Z',
    exportJobId: 'job-1',
    createdAt: '2026-07-03T10:00:00Z',
  }
  let thread: Array<Record<string, unknown>> = [
    { id: 'm-1', author: 'system', body: 'Export fallito: errore X', createdAt: '2026-07-03T10:00:00Z' },
  ]
  let restrictions = { active: [] as Array<Record<string, unknown>>, auditTrail: [] as Array<Record<string, unknown>> }

  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env: 'local', authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
    }),
  )
  await page.route('**/api/auth/refresh', (route) => route.fulfill({ json: tokenBody }))
  await page.route('**/api/platform/v1/admin/gdpr/requests*', (route) =>
    route.fulfill({
      json: [
        {
          type: 'export',
          refId: 'job-1',
          tenantId: 'a-1',
          accountName: 'Acme',
          status: 'FAILED',
          requestedAt: '2026-07-01T10:00:00Z',
          error: 'errore X',
          logsUrl: 'https://eu-south-1.console.aws.amazon.com/cloudwatch/#logs',
        },
        {
          type: 'privacy_ticket',
          refId: 't-1',
          tenantId: 'a-1',
          accountName: 'Acme',
          status: 'open',
          requestedAt: '2026-07-03T10:00:00Z',
          dueAt: '2026-08-02T10:00:00Z',
        },
      ],
    }),
  )
  await page.route('**/api/platform/v1/admin/gdpr/tickets?*', (route) => route.fulfill({ json: [ticket] }))
  await page.route('**/api/platform/v1/admin/gdpr/tickets', (route) => route.fulfill({ json: [ticket] }))
  await page.route('**/api/platform/v1/admin/gdpr/tickets/t-1', (route) =>
    route.fulfill({ json: { ticket, thread } }),
  )
  await page.route('**/api/platform/v1/admin/gdpr/tickets/t-1/messages', async (route, request) => {
    const body = request.postDataJSON() as { body: string }
    const message = { id: `m-${thread.length + 1}`, author: 'admin', body: body.body, createdAt: '2026-07-04T12:00:00Z' }
    thread = [...thread, message]
    ticket = { ...ticket, status: 'in_progress' }
    await route.fulfill({ status: 201, json: message })
  })
  await page.route('**/api/platform/v1/admin/gdpr/restrictions', async (route, request) => {
    if (request.method() === 'POST') {
      const body = request.postDataJSON() as { targetKind: string; targetId: string }
      restrictions = {
        active: [{ targetKind: body.targetKind, targetId: body.targetId, tenantId: 'a-1', label: 'Acme' }],
        auditTrail: [
          {
            id: 'ra-1',
            tenantId: 'a-1',
            targetKind: body.targetKind,
            targetId: body.targetId,
            action: 'applied',
            actor: 'admin-1',
            executedAt: '2026-07-04T12:00:00Z',
          },
        ],
      }
      return route.fulfill({ status: 201, json: { outcome: 'APPLIED' } })
    }
    return route.fulfill({ json: restrictions })
  })
  await page.route('**/api/platform/v1/admin/gdpr/exports/job-1', (route) =>
    route.fulfill({
      json: {
        request: {
          type: 'export',
          refId: 'job-1',
          tenantId: 'a-1',
          accountName: 'Acme',
          status: 'FAILED',
          requestedAt: '2026-07-01T10:00:00Z',
          error: 'errore X',
          logsUrl: 'https://eu-south-1.console.aws.amazon.com/cloudwatch/#logs',
        },
        items: [
          { appId: 'platform', status: 'COMPLETED', error: null },
          { appId: 'fatture', status: 'FAILED', error: 'errore X' },
        ],
        zipKey: 'jobs/job-1/export.zip',
        s3ConsoleUrl: 'https://eu-south-1.console.aws.amazon.com/s3/object/gdpr-export?prefix=jobs%2Fjob-1%2Fexport.zip',
      },
    }),
  )
  await page.route('**/api/platform/v1/admin/gdpr/purge-audit', (route) =>
    route.fulfill({
      json: [
        {
          id: 'pa-1',
          tenantId: 'a-9',
          appId: 'platform',
          reason: 'account-deletion-grace-expired',
          total: 42,
          executedAt: '2026-06-01T10:00:00Z',
        },
      ],
    }),
  )
}

test('console GDPR: aggregazione → ticket → risposta → limitazione art. 18', async ({ page }) => {
  await mockAuthed(page)
  await page.goto('/')
  await page.getByRole('link', { name: 'GDPR rights' }).click()

  // aggregazione: export FAILED con deep-link ai log + ticket privacy
  await expect(page.getByText('FAILED')).toBeVisible()
  await expect(page.getByRole('link', { name: 'Logs' })).toHaveAttribute(
    'href',
    'https://eu-south-1.console.aws.amazon.com/cloudwatch/#logs',
  )
  await expect(page.getByText('account-deletion-grace-expired')).toBeVisible()

  // dettaglio export: avanzamento per-servizio + puntatore S3 (chiave + console)
  await page.getByRole('row', { name: /FAILED/ }).getByRole('link', { name: 'Detail' }).click()
  await expect(page.getByText('jobs/job-1/export.zip')).toBeVisible()
  await expect(page.getByText('fatture')).toBeVisible()
  await expect(page.getByRole('link', { name: 'Open in S3 console' })).toHaveAttribute(
    'href',
    'https://eu-south-1.console.aws.amazon.com/s3/object/gdpr-export?prefix=jobs%2Fjob-1%2Fexport.zip',
  )
  await page.getByRole('link', { name: '← GDPR rights' }).click()

  // dettaglio ticket: thread di sistema (auto-creato) + risposta admin
  await page.getByRole('link', { name: 'Export fallito' }).first().click()
  await expect(page.getByText('Export fallito: errore X')).toBeVisible()
  await page.getByLabel('Reply to the user').fill('Ce ne stiamo occupando.')
  await page.getByRole('button', { name: 'Send reply' }).click()
  await expect(page.getByText('Ce ne stiamo occupando.')).toBeVisible()

  // limitazione art. 18: applica con conferma, la prova compare nel registro
  await page.getByRole('link', { name: 'GDPR rights', exact: true }).click()
  await page.getByLabel('Target ID (UUID)').fill('a0000000-0000-4000-8000-000000000001')
  await page.getByRole('button', { name: 'Apply restriction' }).click()
  await expect(page.getByRole('dialog')).toContainText('Apply the restriction?')
  await page.getByRole('dialog').getByRole('button', { name: 'Apply restriction' }).click()
  await expect(page.getByText('applied')).toBeVisible()
})
