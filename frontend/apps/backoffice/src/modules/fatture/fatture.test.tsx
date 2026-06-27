import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'jest-axe'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { useAuthStore } from '../../auth/authStore'
import { renderApp, fakeAccessToken } from '../../test/utils'

const API = 'http://localhost/api/fatture/v1'

type Invoice = Record<string, unknown>
let invoices: Invoice[]
let quota: { metric: string; used: number; limit: number | null; remaining: number | null }

const server = setupServer(
  http.get(`${API}/quota`, () => HttpResponse.json(quota)),
  http.get(`${API}/invoices`, () =>
    HttpResponse.json({ content: invoices, page: 0, size: 20, totalElements: invoices.length, totalPages: 1 }),
  ),
  http.post(`${API}/invoices`, async ({ request }) => {
    if (quota.limit != null && quota.used >= quota.limit) {
      return HttpResponse.json(
        { type: 'about:blank', title: 'Too Many Requests', status: 429 },
        { status: 429, headers: { 'content-type': 'application/problem+json' } },
      )
    }
    const body = (await request.json()) as { customerName: string }
    const created = {
      id: `inv-${invoices.length + 1}`,
      number: `2026-000${invoices.length + 1}`,
      customerName: body.customerName,
      status: 'draft',
      currency: 'EUR',
      totalAmount: 0,
      lines: [],
    }
    invoices = [...invoices, created]
    quota = { ...quota, used: quota.used + 1, remaining: (quota.remaining ?? 1) - 1 }
    return HttpResponse.json(created, { status: 201 })
  }),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  invoices = [
    { id: 'inv-1', number: '2026-0001', customerName: 'Mario Rossi', status: 'issued', currency: 'EUR', totalAmount: 120 },
    { id: 'inv-2', number: '2026-0002', customerName: 'Acme SRL', status: 'draft', currency: 'EUR', totalAmount: 0 },
  ]
  quota = { metric: 'fatture', used: 2, limit: 10, remaining: 8 }
  useAuthStore.getState().setSession({ accessToken: fakeAccessToken({ roles: ['owner'] }) })
})

describe('Modulo fatture (UC 0052)', () => {
  it('mostra la lista delle fatture e il banner quota (consumo/limite)', async () => {
    renderApp({ route: '/app/fatture', entitled: ['fatture'] })
    expect(await screen.findByText('Mario Rossi')).toBeInTheDocument()
    expect(screen.getByText('Acme SRL')).toBeInTheDocument()
    // banner quota consumo/limite
    expect(screen.getByText('2 / 10')).toBeInTheDocument()
  })

  it('stato empty quando non ci sono fatture', async () => {
    invoices = []
    renderApp({ route: '/app/fatture', entitled: ['fatture'] })
    expect(await screen.findByText(/Nessuna fattura/i)).toBeInTheDocument()
  })

  it('crea una fattura: compare nella lista al ritorno', async () => {
    const user = userEvent.setup()
    renderApp({ route: '/app/fatture', entitled: ['fatture'] })
    await screen.findByText('Mario Rossi')

    await user.click(screen.getByRole('button', { name: 'Nuova fattura' }))
    await user.type(await screen.findByLabelText('Nome cliente'), 'Nuovo Cliente')
    await user.click(screen.getByRole('button', { name: 'Crea fattura' }))

    expect(await screen.findByText('Nuovo Cliente')).toBeInTheDocument()
  })

  it('a quota raggiunta la creazione mostra errore 429 + CTA upgrade', async () => {
    quota = { metric: 'fatture', used: 10, limit: 10, remaining: 0 }
    const user = userEvent.setup()
    renderApp({ route: '/app/fatture/new', entitled: ['fatture'] })

    await user.type(await screen.findByLabelText('Nome cliente'), 'Oltre Limite')
    await user.click(screen.getByRole('button', { name: 'Crea fattura' }))

    expect(await screen.findByText(/Limite mensile raggiunto/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Passa a un piano superiore/i })).toBeInTheDocument()
  })

  it("un'app non entitled è bloccata dalla route guard (modulo non montato)", async () => {
    renderApp({ route: '/app/fatture', entitled: [] })
    await waitFor(() => expect(screen.queryByTestId('fatture-module')).not.toBeInTheDocument())
  })

  it('nessuna violazione di accessibilità sulla lista', async () => {
    const { container } = renderApp({ route: '/app/fatture', entitled: ['fatture'] })
    await screen.findByText('Mario Rossi')
    expect(await axe(container)).toHaveNoViolations()
  })
})
