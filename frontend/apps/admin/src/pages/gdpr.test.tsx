import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { GdprRights } from './GdprRights'
import { GdprTicketDetail } from './GdprTicketDetail'
import { useAuthStore } from '../auth/authStore'
import { renderWithProviders, fakeAccessToken } from '../test/utils'

const REQUESTS = [
  {
    type: 'export',
    refId: 'job-1',
    tenantId: 'a-1',
    accountName: 'Acme',
    appId: null,
    status: 'COMPLETED',
    requestedAt: '2026-07-01T10:00:00Z',
    completedAt: '2026-07-01T10:05:00Z',
    dueAt: '2026-07-08T10:05:00Z',
    logsUrl: 'https://eu-south-1.console.aws.amazon.com/cloudwatch/#logs',
  },
  {
    type: 'account_deletion',
    refId: 'a-2',
    tenantId: 'a-2',
    accountName: 'Globex',
    status: 'GRACE_PENDING',
    requestedAt: '2026-07-02T10:00:00Z',
    dueAt: '2026-07-16T10:00:00Z',
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
]

let ticket: Record<string, unknown>
let thread: Array<Record<string, unknown>>
let replies: string[]
let restrictionCalls: Array<Record<string, unknown>>
let restrictions: { active: Array<Record<string, unknown>>; auditTrail: Array<Record<string, unknown>> }

const server = setupServer(
  http.get('http://localhost/api/platform/v1/admin/gdpr/requests', () => HttpResponse.json(REQUESTS)),
  http.get('http://localhost/api/platform/v1/admin/gdpr/tickets', () => HttpResponse.json([ticket])),
  http.get('http://localhost/api/platform/v1/admin/gdpr/tickets/:id', () =>
    HttpResponse.json({ ticket, thread }),
  ),
  http.post('http://localhost/api/platform/v1/admin/gdpr/tickets/:id/messages', async ({ request }) => {
    const body = (await request.json()) as { body: string }
    replies.push(body.body)
    const message = { id: `m-${thread.length + 1}`, author: 'admin', body: body.body, createdAt: '2026-07-04T12:00:00Z' }
    thread = [...thread, message]
    return HttpResponse.json(message, { status: 201 })
  }),
  http.patch('http://localhost/api/platform/v1/admin/gdpr/tickets/:id', async ({ request }) => {
    const body = (await request.json()) as { status: string; priority: string }
    ticket = { ...ticket, status: body.status, priority: body.priority }
    return HttpResponse.json(ticket)
  }),
  http.get('http://localhost/api/platform/v1/admin/gdpr/restrictions', () =>
    HttpResponse.json(restrictions),
  ),
  http.post('http://localhost/api/platform/v1/admin/gdpr/restrictions', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>
    restrictionCalls.push(body)
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
    return HttpResponse.json({ outcome: 'APPLIED' }, { status: 201 })
  }),
  http.get('http://localhost/api/platform/v1/admin/gdpr/purge-audit', () =>
    HttpResponse.json([
      {
        id: 'pa-1',
        tenantId: 'a-9',
        appId: 'platform',
        reason: 'account-deletion-grace-expired',
        total: 42,
        executedAt: '2026-06-01T10:00:00Z',
      },
    ]),
  ),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  ticket = {
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
  thread = [
    { id: 'm-1', author: 'system', body: 'Export fallito: errore X', createdAt: '2026-07-03T10:00:00Z' },
  ]
  replies = []
  restrictionCalls = []
  restrictions = { active: [], auditTrail: [] }
  useAuthStore
    .getState()
    .setSession({ accessToken: fakeAccessToken({ roles: ['platform-admin'] }) })
})

describe('Console Diritti GDPR (UC 0034)', () => {
  it('aggrega export, eliminazioni e ticket privacy con stato e prova di purge', async () => {
    renderWithProviders(<GdprRights />)
    // tabella aggregata: i tre tipi con i loro stati
    expect(await screen.findByText('COMPLETED')).toBeInTheDocument()
    expect(screen.getByText('GRACE_PENDING')).toBeInTheDocument()
    // i tipi compaiono nelle righe (oltre che nelle option del filtro)
    expect(screen.getAllByText('Account deletion').length).toBeGreaterThan(1)
    expect(screen.getAllByText('Privacy ticket').length).toBeGreaterThan(1)
    // deep-link Logs Insights presente solo dove il backend l'ha costruito
    expect(screen.getByRole('link', { name: 'Logs' })).toHaveAttribute(
      'href',
      'https://eu-south-1.console.aws.amazon.com/cloudwatch/#logs',
    )
    // registro prove di purge
    expect(await screen.findByText('account-deletion-grace-expired')).toBeInTheDocument()
  })

  it('applica la limitazione art. 18 con conferma e mostra registro e limitazione attiva', async () => {
    renderWithProviders(<GdprRights />)
    const user = userEvent.setup()

    await user.type(
      await screen.findByLabelText('Target ID (UUID)'),
      'a0000000-0000-4000-8000-000000000001',
    )
    await user.click(screen.getByRole('button', { name: 'Apply restriction' }))
    // dialog di conferma (ops sicura, reversibile)
    const dialog = await screen.findByRole('dialog')
    expect(dialog).toHaveTextContent('Apply the restriction?')
    await user.click(screen.getAllByRole('button', { name: 'Apply restriction' })[1])

    // registro prove: azione "applied" tracciata
    expect(await screen.findByText('applied')).toBeInTheDocument()
    expect(restrictionCalls[0]).toMatchObject({
      targetKind: 'account',
      targetId: 'a0000000-0000-4000-8000-000000000001',
    })
  })

  it('dettaglio ticket: thread, risposta admin e cambio stato', async () => {
    renderWithProviders(
      <Routes>
        <Route path="/gdpr/tickets/:id" element={<GdprTicketDetail />} />
      </Routes>,
      { route: '/gdpr/tickets/t-1' },
    )
    const user = userEvent.setup()

    expect(await screen.findByText('Export fallito: errore X')).toBeInTheDocument()

    await user.type(screen.getByLabelText('Reply to the user'), 'Ci stiamo lavorando')
    await user.click(screen.getByRole('button', { name: 'Send reply' }))
    expect(await screen.findByText('Ci stiamo lavorando')).toBeInTheDocument()
    expect(replies).toEqual(['Ci stiamo lavorando'])

    await user.selectOptions(screen.getByLabelText('Status'), 'resolved')
    await user.click(screen.getByRole('button', { name: 'Update' }))
    expect(await screen.findByText(/resolved/)).toBeInTheDocument()
  })
})
