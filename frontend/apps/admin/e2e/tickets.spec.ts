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

const DAY = 24 * 60 * 60 * 1000

/**
 * Sezione «Ticket» della console (UC 0075): coda cross-account con filtri e scadenze, dettaglio con
 * filo, risposta che mette la richiesta in attesa dell'utente, chiusura sotto conferma. Endpoint
 * finti: qui si collauda la superficie, non il servizio.
 */
async function mockAuthed(page: Page) {
  let flagged: Record<string, unknown> = {
    id: 't-1',
    tenantId: 'a-1',
    accountName: 'Acme',
    type: 'privacy',
    source: 'event',
    subject: 'Export fallito',
    priority: 'high',
    status: 'open',
    flaggedForReview: true,
    dueAt: new Date(Date.now() + 2 * DAY).toISOString(),
    exportJobId: 'job-1',
    createdAt: '2026-07-03T10:00:00Z',
  }
  const ordinary = {
    id: 't-2',
    tenantId: 'a-2',
    accountName: 'Globex',
    type: 'support',
    source: 'form',
    subject: 'Domanda sul catalogo',
    priority: 'normal',
    status: 'open',
    flaggedForReview: false,
    createdAt: '2026-07-02T10:00:00Z',
  }
  let thread: Array<Record<string, unknown>> = [
    { id: 'm-1', author: 'system', body: 'Export fallito: errore X', createdAt: '2026-07-03T10:00:00Z' },
  ]

  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env: 'local', authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
    }),
  )
  await page.route('**/api/auth/refresh', (route) => route.fulfill({ json: tokenBody }))
  await page.route('**/api/platform/v1/admin/tickets?*', (route, request) => {
    const priority = new URL(request.url()).searchParams.get('priority')
    const rows = [flagged, ordinary].filter((t) => !priority || t.priority === priority)
    return route.fulfill({ json: rows })
  })
  await page.route('**/api/platform/v1/admin/tickets', (route) =>
    route.fulfill({ json: [flagged, ordinary] }),
  )
  await page.route('**/api/platform/v1/admin/tickets/t-1', async (route, request) => {
    if (request.method() === 'PATCH') {
      const body = request.postDataJSON() as { status: string; priority: string }
      flagged = { ...flagged, status: body.status, priority: body.priority }
      return route.fulfill({ json: flagged })
    }
    return route.fulfill({ json: { ticket: flagged, thread } })
  })
  await page.route('**/api/platform/v1/admin/tickets/t-1/messages', async (route, request) => {
    const body = request.postDataJSON() as { body: string }
    const message = {
      id: `m-${thread.length + 1}`,
      author: 'admin',
      body: body.body,
      createdAt: '2026-07-04T12:00:00Z',
    }
    thread = [...thread, message]
    flagged = { ...flagged, status: 'waiting_user' }
    await route.fulfill({ status: 201, json: message })
  })
}

test('[L2-ADMIN-TICKETS] coda ticket: scadenza e contrassegno → filtro priorità → risposta → chiusura con conferma', async ({
  page,
}) => {
  await mockAuthed(page)
  await page.goto('/')
  await page.getByRole('link', { name: 'Support requests' }).click()
  await expect(page.getByRole('heading', { name: 'Support requests', level: 1 })).toBeVisible()

  // la coda dice a colpo d'occhio cosa scade e cosa va letto da un umano
  const urgent = page.getByRole('row', { name: /Export fallito/ })
  await expect(urgent.getByText('Needs review')).toBeVisible()
  await expect(urgent.getByText('Due soon')).toBeVisible()
  await expect(urgent.getByText('System event')).toBeVisible()
  await expect(page.getByRole('row', { name: /Domanda sul catalogo/ })).toBeVisible()

  // filtro per priorità: resta solo ciò che è stato marcato urgente
  await page.getByLabel('Priority').selectOption('high')
  await expect(page.getByRole('row', { name: /Domanda sul catalogo/ })).toHaveCount(0)

  // dettaglio: filo di sistema, avviso del contrassegno, risposta → in attesa dell'utente
  await page.getByRole('link', { name: 'Export fallito' }).click()
  await expect(page.getByText('Export fallito: errore X')).toBeVisible()
  await expect(page.getByText(/may touch special categories of data/)).toBeVisible()
  await page.getByLabel('Reply to the user').fill('Ce ne stiamo occupando.')
  await page.getByRole('button', { name: 'Send reply' }).click()
  await expect(page.getByText('Ce ne stiamo occupando.')).toBeVisible()
  await expect(page.getByText('Status: Waiting for the user')).toBeVisible()

  // la chiusura non è mai silenziosa: chiede conferma
  await page.getByLabel('Status').selectOption('closed')
  await page.getByRole('button', { name: 'Update' }).click()
  await expect(page.getByRole('dialog')).toContainText('Close this request?')
  await page.getByRole('dialog').getByRole('button', { name: 'Close the request' }).click()
  await expect(page.getByText('This request is closed')).toBeVisible()
})
