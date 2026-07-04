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
 * Ticketing lato utente (UC 0034): SPA autenticata come owner con lo stato ticket mockato in
 * memoria — apertura ticket, lista, thread con risposta.
 */
async function mockAuthed(page: Page) {
  let tickets: Array<Record<string, unknown>> = []
  const threads = new Map<string, Array<Record<string, unknown>>>()

  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env: 'local', authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
    }),
  )
  await page.route('**/api/auth/refresh', (route) => route.fulfill({ json: tokenBody }))
  await page.route('**/api/platform/v1/me/entitlements', (route) =>
    route.fulfill({ json: { entitlements: [] } }),
  )
  await page.route('**/api/platform/v1/tickets', async (route, request) => {
    if (request.method() === 'POST') {
      const body = request.postDataJSON() as { type: string; subject: string; message: string }
      const ticket = {
        id: `t-${tickets.length + 1}`,
        type: body.type,
        subject: body.subject,
        priority: 'normal',
        status: 'open',
        dueAt: body.type === 'privacy' ? '2026-08-03T10:00:00Z' : null,
        createdAt: '2026-07-04T10:00:00Z',
      }
      tickets = [ticket, ...tickets]
      threads.set(ticket.id as string, [
        { id: 'm-1', author: 'user', body: body.message, createdAt: '2026-07-04T10:00:00Z' },
      ])
      return route.fulfill({ status: 201, json: ticket })
    }
    return route.fulfill({ json: tickets })
  })
  await page.route('**/api/platform/v1/tickets/*', (route, request) => {
    const id = request.url().split('/').pop() as string
    const ticket = tickets.find((candidate) => candidate.id === id)
    return route.fulfill({ json: { ticket, thread: threads.get(id) ?? [] } })
  })
  await page.route('**/api/platform/v1/tickets/*/messages', async (route, request) => {
    const id = request.url().split('/').slice(-2)[0]
    const body = request.postDataJSON() as { body: string }
    const message = { id: `m-${Date.now()}`, author: 'user', body: body.body, createdAt: '2026-07-04T11:00:00Z' }
    threads.set(id, [...(threads.get(id) ?? []), message])
    await route.fulfill({ status: 201, json: message })
  })
}

test('supporto: apri un ticket privacy e rispondi nel thread', async ({ page }) => {
  await mockAuthed(page)
  await page.goto('/')
  await page.getByRole('link', { name: 'Support' }).click()
  await expect(page.getByRole('heading', { name: 'Support', level: 1 })).toBeVisible()

  await page.getByLabel('Type').selectOption('privacy')
  await page.getByLabel('Subject').fill('Richiesta art. 18')
  await page.getByLabel('Message').fill('Chiedo la limitazione del trattamento.')
  await page.getByRole('button', { name: 'Open ticket' }).click()

  // dettaglio: thread con il primo messaggio e scadenza legale
  await expect(page.getByText('Chiedo la limitazione del trattamento.')).toBeVisible()
  await expect(page.getByText(/Due by/)).toBeVisible()

  // risposta nel thread
  await page.getByLabel('Reply').fill('Aggiungo un dettaglio.')
  await page.getByRole('button', { name: 'Send reply' }).click()
  await expect(page.getByText('Aggiungo un dettaglio.')).toBeVisible()
})
