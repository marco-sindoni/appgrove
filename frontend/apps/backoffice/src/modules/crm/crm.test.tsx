import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'jest-axe'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { useAuthStore } from '../../auth/authStore'
import { renderApp, fakeAccessToken } from '../../test/utils'

const API = 'http://localhost/api/crm/v1'

type Contact = Record<string, unknown>
type Seat = { id: string; userId: string }
let contacts: Contact[]
let seats: Seat[]
let quota: { metric: string; used: number; limit: number | null; remaining: number | null }

function seatSummary() {
  const cap = quota.limit
  const used = seats.length
  return {
    used,
    limit: cap,
    remaining: cap == null ? null : Math.max(0, cap - used),
    seats,
  }
}

const server = setupServer(
  http.get(`${API}/quota`, () => HttpResponse.json(quota)),
  http.get(`${API}/contacts`, () =>
    HttpResponse.json({
      content: contacts,
      page: 0,
      size: 20,
      totalElements: contacts.length,
      totalPages: 1,
    }),
  ),
  http.post(`${API}/contacts`, async ({ request }) => {
    const body = (await request.json()) as { displayName: string; organization?: string }
    const created = {
      id: `contact-${contacts.length + 1}`,
      displayName: body.displayName,
      organization: body.organization,
      stage: 'lead',
    }
    contacts = [...contacts, created]
    return HttpResponse.json(created, { status: 201 })
  }),
  http.get(`${API}/seats`, () => HttpResponse.json(seatSummary())),
  http.post(`${API}/seats`, async ({ request }) => {
    if (quota.limit != null && seats.length >= quota.limit) {
      return HttpResponse.json(
        { type: 'about:blank', title: 'Too Many Requests', status: 429 },
        { status: 429, headers: { 'content-type': 'application/problem+json' } },
      )
    }
    const body = (await request.json()) as { userId: string }
    const seat = { id: `seat-${seats.length + 1}`, userId: body.userId }
    seats = [...seats, seat]
    return HttpResponse.json(seat, { status: 201 })
  }),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  contacts = [
    { id: 'contact-1', displayName: 'Mario Rossi', organization: 'ACME S.p.A.', stage: 'qualified' },
    { id: 'contact-2', displayName: 'Anna Bianchi', organization: 'Globex', stage: 'lead' },
  ]
  seats = [{ id: 'seat-1', userId: 'u-owner' }]
  quota = { metric: 'seats', used: 1, limit: 2, remaining: 1 }
  useAuthStore.getState().setSession({ accessToken: fakeAccessToken({ roles: ['owner'] }) })
})

describe('Modulo crm', () => {
  it('mostra l\'elenco dei contatti e il banner posti (occupati/limite)', async () => {
    renderApp({ route: '/app/crm', entitled: ['crm'] })
    expect(await screen.findByText('Mario Rossi')).toBeInTheDocument()
    expect(screen.getByText('Anna Bianchi')).toBeInTheDocument()
    expect(screen.getByText('1 / 2')).toBeInTheDocument()
  })

  it('senza posto (403) mostra un messaggio azionabile, non l\'errore generico', async () => {
    server.use(
      http.get(`${API}/contacts`, () =>
        HttpResponse.json(
          { type: 'about:blank', title: 'Forbidden', status: 403 },
          { status: 403, headers: { 'content-type': 'application/problem+json' } },
        ),
      ),
    )
    renderApp({ route: '/app/crm', entitled: ['crm'] })
    // il QueryClient dell'app fa retry:1 con backoff → l'errore si assesta dopo ~1s
    expect(await screen.findByText(/Non hai un posto assegnato/i, {}, { timeout: 4000 })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Membri' })).toBeInTheDocument()
  })

  it('stato vuoto quando non ci sono contatti', async () => {
    contacts = []
    renderApp({ route: '/app/crm', entitled: ['crm'] })
    expect(await screen.findByText(/Nessun contatto/i)).toBeInTheDocument()
  })

  it('crea un contatto: compare nell\'elenco al ritorno', async () => {
    const user = userEvent.setup()
    renderApp({ route: '/app/crm', entitled: ['crm'] })
    await screen.findByText('Mario Rossi')

    await user.click(screen.getByRole('button', { name: 'Nuovo contatto' }))
    await user.type(await screen.findByLabelText('Nome'), 'Nuovo Contatto')
    await user.click(screen.getByRole('button', { name: 'Crea contatto' }))

    expect(await screen.findByText('Nuovo Contatto')).toBeInTheDocument()
  })

  it('la schermata Membri assegna un posto e mostra i posti occupati', async () => {
    const user = userEvent.setup()
    renderApp({ route: '/app/crm/members', entitled: ['crm'] })

    expect(await screen.findByText('u-owner')).toBeInTheDocument()
    await user.type(screen.getByLabelText('Identificativo utente'), 'u-collega')
    await user.click(screen.getByRole('button', { name: 'Assegna posto' }))

    expect(await screen.findByText('u-collega')).toBeInTheDocument()
    expect(screen.getByText('2 / 2')).toBeInTheDocument()
  })

  it('a posti esauriti l\'assegnazione mostra errore 429 + invito all\'upgrade', async () => {
    seats = [
      { id: 'seat-1', userId: 'u1' },
      { id: 'seat-2', userId: 'u2' },
    ]
    quota = { metric: 'seats', used: 2, limit: 2, remaining: 0 }
    const user = userEvent.setup()
    renderApp({ route: '/app/crm/members', entitled: ['crm'] })

    await user.type(await screen.findByLabelText('Identificativo utente'), 'u-oltre')
    await user.click(screen.getByRole('button', { name: 'Assegna posto' }))

    expect(await screen.findByText(/Posti esauriti/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Passa a un piano superiore/i })).toBeInTheDocument()
  })

  it('un\'app non entitled è bloccata dalla route guard (modulo non montato)', async () => {
    renderApp({ route: '/app/crm', entitled: [] })
    await waitFor(() => expect(screen.queryByTestId('crm-module')).not.toBeInTheDocument())
  })

  it('nessuna violazione di accessibilità sull\'elenco', async () => {
    const { container } = renderApp({ route: '/app/crm', entitled: ['crm'] })
    await screen.findByText('Mario Rossi')
    expect(await axe(container)).toHaveNoViolations()
  })
})
