import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { SupportPage } from './SupportPage'
import { useAuthStore } from '../../auth/authStore'
import { renderWithProviders, fakeAccessToken } from '../../test/utils'

let tickets: Array<Record<string, unknown>>
let threads: Map<string, Array<Record<string, unknown>>>

const server = setupServer(
  http.get('http://localhost/api/platform/v1/tickets', () => HttpResponse.json(tickets)),
  http.get('http://localhost/api/platform/v1/tickets/:id', ({ params }) => {
    const ticket = tickets.find((candidate) => candidate.id === params.id)
    if (!ticket) return HttpResponse.json(null, { status: 404 })
    return HttpResponse.json({ ticket, thread: threads.get(params.id as string) ?? [] })
  }),
  http.post('http://localhost/api/platform/v1/tickets', async ({ request }) => {
    const body = (await request.json()) as { type: string; subject: string; message: string }
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
    threads.set(ticket.id, [
      { id: 'm-1', author: 'user', body: body.message, createdAt: '2026-07-04T10:00:00Z' },
    ])
    return HttpResponse.json(ticket, { status: 201 })
  }),
  http.post('http://localhost/api/platform/v1/tickets/:id/messages', async ({ params, request }) => {
    const body = (await request.json()) as { body: string }
    const message = {
      id: `m-${Date.now()}`,
      author: 'user',
      body: body.body,
      createdAt: '2026-07-04T11:00:00Z',
    }
    threads.set(params.id as string, [...(threads.get(params.id as string) ?? []), message])
    return HttpResponse.json(message, { status: 201 })
  }),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  tickets = []
  threads = new Map()
  useAuthStore.getState().setSession({ accessToken: fakeAccessToken({ roles: ['member'] }) })
})

describe('Pagina Supporto (UC 0034)', () => {
  it('apre un ticket privacy e mostra il thread con la scadenza legale', async () => {
    renderWithProviders(<SupportPage />)
    const user = userEvent.setup()

    await user.selectOptions(await screen.findByLabelText('Type'), 'privacy')
    await user.type(screen.getByLabelText('Subject'), 'Richiesta limitazione')
    await user.type(screen.getByLabelText('Message'), 'Chiedo la limitazione (art. 18).')
    await user.click(screen.getByRole('button', { name: 'Open ticket' }))

    // si apre il dettaglio: thread con il primo messaggio e la scadenza
    expect(await screen.findByText('Chiedo la limitazione (art. 18).')).toBeInTheDocument()
    expect(screen.getByText(/Due by/)).toBeInTheDocument()
  })

  it('elenca i ticket e permette di rispondere nel thread', async () => {
    tickets = [
      {
        id: 't-9',
        type: 'support',
        subject: 'Domanda fatture',
        priority: 'normal',
        status: 'in_progress',
        createdAt: '2026-07-01T10:00:00Z',
      },
    ]
    threads.set('t-9', [
      { id: 'm-1', author: 'user', body: 'Come esporto?', createdAt: '2026-07-01T10:00:00Z' },
      { id: 'm-2', author: 'admin', body: 'Dal menu Fatture.', createdAt: '2026-07-02T10:00:00Z' },
    ])
    renderWithProviders(<SupportPage />)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Domanda fatture' }))
    expect(await screen.findByText('Dal menu Fatture.')).toBeInTheDocument()

    await user.type(screen.getByLabelText('Reply'), 'Grazie, risolto!')
    await user.click(screen.getByRole('button', { name: 'Send reply' }))
    expect(await screen.findByText('Grazie, risolto!')).toBeInTheDocument()
  })

  it('un ticket chiuso non offre il form di risposta', async () => {
    tickets = [
      {
        id: 't-3',
        type: 'support',
        subject: 'Chiuso da tempo',
        priority: 'low',
        status: 'closed',
        createdAt: '2026-05-01T10:00:00Z',
        closedAt: '2026-05-02T10:00:00Z',
      },
    ]
    threads.set('t-3', [
      { id: 'm-1', author: 'user', body: 'Vecchia domanda', createdAt: '2026-05-01T10:00:00Z' },
    ])
    renderWithProviders(<SupportPage />)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Chiuso da tempo' }))
    expect(
      await screen.findByText('This ticket is closed: open a new one if you need anything else.'),
    ).toBeInTheDocument()
    expect(screen.queryByLabelText('Reply')).not.toBeInTheDocument()
  })
})
