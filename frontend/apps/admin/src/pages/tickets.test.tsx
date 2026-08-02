import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { Tickets } from './Tickets'
import { TicketDetail } from './TicketDetail'
import { dueState } from './ticketLabels'
import { useAuthStore } from '../auth/authStore'
import { renderWithProviders, fakeAccessToken } from '../test/utils'

/**
 * Sezione «Ticket» della console (UC 0075): coda cross-account con filtri, evidenza delle scadenze
 * e del contrassegno «da rivedere»; dettaglio con filo, risposta e chiusura sotto conferma.
 */

const DAY = 24 * 60 * 60 * 1000

let queue: Array<Record<string, unknown>>
let ticket: Record<string, unknown>
let thread: Array<Record<string, unknown>>
let replies: string[]
let lastQuery: URLSearchParams

const server = setupServer(
  http.get('http://localhost/api/platform/v1/admin/tickets', ({ request }) => {
    lastQuery = new URL(request.url).searchParams
    const priority = lastQuery.get('priority')
    return HttpResponse.json(priority ? queue.filter((t) => t.priority === priority) : queue)
  }),
  http.get('http://localhost/api/platform/v1/admin/tickets/:id', () =>
    HttpResponse.json({ ticket, thread }),
  ),
  http.post('http://localhost/api/platform/v1/admin/tickets/:id/messages', async ({ request }) => {
    const body = (await request.json()) as { body: string }
    replies.push(body.body)
    const message = {
      id: `m-${thread.length + 1}`,
      author: 'admin',
      body: body.body,
      createdAt: '2026-07-04T12:00:00Z',
    }
    thread = [...thread, message]
    ticket = { ...ticket, status: 'waiting_user' }
    return HttpResponse.json(message, { status: 201 })
  }),
  http.patch('http://localhost/api/platform/v1/admin/tickets/:id', async ({ request }) => {
    const body = (await request.json()) as { status: string; priority: string }
    ticket = { ...ticket, status: body.status, priority: body.priority }
    return HttpResponse.json(ticket)
  }),
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
    source: 'event',
    subject: 'Export fallito',
    priority: 'high',
    status: 'open',
    flaggedForReview: true,
    dueAt: new Date(Date.now() + 2 * DAY).toISOString(),
    exportJobId: 'job-1',
    createdAt: '2026-07-03T10:00:00Z',
  }
  queue = [
    ticket,
    {
      id: 't-2',
      tenantId: 'a-2',
      accountName: 'Globex',
      type: 'support',
      source: 'form',
      subject: 'Domanda sul catalogo',
      priority: 'normal',
      status: 'waiting_user',
      flaggedForReview: false,
      createdAt: '2026-07-02T10:00:00Z',
    },
  ]
  thread = [
    { id: 'm-1', author: 'system', body: 'Export fallito: errore X', createdAt: '2026-07-03T10:00:00Z' },
  ]
  replies = []
  lastQuery = new URLSearchParams()
  useAuthStore
    .getState()
    .setSession({ accessToken: fakeAccessToken({ roles: ['platform-admin'] }) })
})

describe('dueState — evidenza della scadenza legale', () => {
  const base = { id: 't', status: 'open' as const }

  it('segnala scaduto quando il termine è passato', () => {
    expect(dueState({ ...base, dueAt: new Date(Date.now() - DAY).toISOString() })).toBe('overdue')
  })

  it('segnala in scadenza entro una settimana e nulla oltre', () => {
    expect(dueState({ ...base, dueAt: new Date(Date.now() + 3 * DAY).toISOString() })).toBe('dueSoon')
    expect(dueState({ ...base, dueAt: new Date(Date.now() + 20 * DAY).toISOString() })).toBe('none')
  })

  it('non segnala nulla senza scadenza o su una richiesta già chiusa', () => {
    expect(dueState({ ...base })).toBe('none')
    expect(
      dueState({ ...base, status: 'closed', dueAt: new Date(Date.now() - DAY).toISOString() }),
    ).toBe('none')
  })
})

describe('Coda dei ticket (UC 0075)', () => {
  it('mostra la coda cross-account con provenienza, scadenza in evidenza e contrassegno da rivedere', async () => {
    renderWithProviders(<Tickets />)

    const flagged = await screen.findByRole('row', { name: /Export fallito/ })
    expect(within(flagged).getByText('Acme')).toBeInTheDocument()
    expect(within(flagged).getByText('System event')).toBeInTheDocument()
    expect(within(flagged).getByText('Needs review')).toBeInTheDocument()
    expect(within(flagged).getByText('Due soon')).toBeInTheDocument()

    const other = screen.getByRole('row', { name: /Domanda sul catalogo/ })
    expect(within(other).getByText('In-app form')).toBeInTheDocument()
    expect(within(other).getByText('Waiting for the user')).toBeInTheDocument()
  })

  it('filtra per priorità passando il filtro al servizio', async () => {
    renderWithProviders(<Tickets />)
    const user = userEvent.setup()
    await screen.findByRole('row', { name: /Export fallito/ })

    await user.selectOptions(screen.getByLabelText('Priority'), 'high')

    expect(await screen.findByRole('row', { name: /Export fallito/ })).toBeInTheDocument()
    expect(screen.queryByRole('row', { name: /Domanda sul catalogo/ })).not.toBeInTheDocument()
    expect(lastQuery.get('priority')).toBe('high')
  })
})

describe('Dettaglio del ticket (UC 0075)', () => {
  const renderDetail = () =>
    renderWithProviders(
      <Routes>
        <Route path="/tickets/:id" element={<TicketDetail />} />
      </Routes>,
      { route: '/tickets/t-1' },
    )

  it('mostra il filo, avverte del contrassegno e la risposta mette in attesa dell’utente', async () => {
    renderDetail()
    const user = userEvent.setup()

    expect(await screen.findByText('Export fallito: errore X')).toBeInTheDocument()
    expect(
      screen.getByText(/may touch special categories of data/),
    ).toBeInTheDocument()

    await user.type(screen.getByLabelText('Reply to the user'), 'Ci stiamo lavorando')
    await user.click(screen.getByRole('button', { name: 'Send reply' }))

    expect(await screen.findByText('Ci stiamo lavorando')).toBeInTheDocument()
    expect(replies).toEqual(['Ci stiamo lavorando'])
    // La riga di intestazione riflette il nuovo stato (l'elenco a tendina ha la stessa etichetta:
    // si cerca il riquadro dei dati, non una qualunque occorrenza del testo).
    expect(
      await screen.findByText(
        (_, el) => el?.tagName === 'SPAN' && el.textContent === 'Status: Waiting for the user',
      ),
    ).toBeInTheDocument()
  })

  it('chiede conferma prima di chiudere la richiesta, e senza conferma non chiude nulla', async () => {
    renderDetail()
    const user = userEvent.setup()
    await screen.findByText('Export fallito: errore X')

    await user.selectOptions(screen.getByLabelText('Status'), 'closed')
    await user.click(screen.getByRole('button', { name: 'Update' }))

    const dialog = await screen.findByRole('dialog')
    expect(dialog).toHaveTextContent('Close this request?')
    await user.click(within(dialog).getByRole('button', { name: 'Cancel' }))
    expect(ticket.status).toBe('open')

    await user.click(screen.getByRole('button', { name: 'Update' }))
    await user.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Close the request' }),
    )
    expect(await screen.findByText(/This request is closed/)).toBeInTheDocument()
  })
})
