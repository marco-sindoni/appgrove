import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'jest-axe'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { useAuthStore } from '../../auth/authStore'
import { renderApp, fakeAccessToken } from '../../test/utils'

const API = 'http://localhost/api/@@APP_ID@@/v1'

type Item = Record<string, unknown>
let items: Item[]
let quota: { metric: string; used: number; limit: number | null; remaining: number | null }

const server = setupServer(
  http.get(`${API}/quota`, () => HttpResponse.json(quota)),
  http.get(`${API}/items`, () =>
    HttpResponse.json({
      content: items,
      page: 0,
      size: 20,
      totalElements: items.length,
      totalPages: 1,
    }),
  ),
  http.post(`${API}/items`, async ({ request }) => {
    if (quota.limit != null && quota.used >= quota.limit) {
      return HttpResponse.json(
        { type: 'about:blank', title: 'Too Many Requests', status: 429 },
        { status: 429, headers: { 'content-type': 'application/problem+json' } },
      )
    }
    const body = (await request.json()) as { contactName: string }
    const created = {
      id: `item-${items.length + 1}`,
      code: `2026-000${items.length + 1}`,
      contactName: body.contactName,
      status: 'draft',
      currency: 'EUR',
      totalAmount: 0,
      lines: [],
    }
    items = [...items, created]
    quota = { ...quota, used: quota.used + 1, remaining: (quota.remaining ?? 1) - 1 }
    return HttpResponse.json(created, { status: 201 })
  }),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  items = [
    { id: 'item-1', code: '2026-0001', contactName: 'Mario Rossi', status: 'active', currency: 'EUR', totalAmount: 120 },
    { id: 'item-2', code: '2026-0002', contactName: 'Acme SRL', status: 'draft', currency: 'EUR', totalAmount: 0 },
  ]
  quota = { metric: '@@METRIC@@', used: 2, limit: @@FREE_CAP@@, remaining: @@FREE_CAP@@ - 2 }
  useAuthStore.getState().setSession({ accessToken: fakeAccessToken({ roles: ['owner'] }) })
})

describe('Modulo @@APP_ID@@', () => {
  it('mostra l\'elenco dei record e il banner quota (consumo/limite)', async () => {
    renderApp({ route: '/app/@@APP_ID@@', entitled: ['@@APP_ID@@'] })
    expect(await screen.findByText('Mario Rossi')).toBeInTheDocument()
    expect(screen.getByText('Acme SRL')).toBeInTheDocument()
    expect(screen.getByText(`2 / @@FREE_CAP@@`)).toBeInTheDocument()
  })

  it('stato vuoto quando non ci sono record', async () => {
    items = []
    renderApp({ route: '/app/@@APP_ID@@', entitled: ['@@APP_ID@@'] })
    expect(await screen.findByText(/Nessun record/i)).toBeInTheDocument()
  })

  it('crea un record: compare nell\'elenco al ritorno', async () => {
    const user = userEvent.setup()
    renderApp({ route: '/app/@@APP_ID@@', entitled: ['@@APP_ID@@'] })
    await screen.findByText('Mario Rossi')

    await user.click(screen.getByRole('button', { name: 'Nuovo record' }))
    await user.type(await screen.findByLabelText('Nome contatto'), 'Nuovo Contatto')
    await user.click(screen.getByRole('button', { name: 'Crea record' }))

    expect(await screen.findByText('Nuovo Contatto')).toBeInTheDocument()
  })

  it('a quota raggiunta la creazione mostra errore 429 + invito all\'upgrade', async () => {
    quota = { metric: '@@METRIC@@', used: @@FREE_CAP@@, limit: @@FREE_CAP@@, remaining: 0 }
    const user = userEvent.setup()
    renderApp({ route: '/app/@@APP_ID@@/new', entitled: ['@@APP_ID@@'] })

    await user.type(await screen.findByLabelText('Nome contatto'), 'Oltre Limite')
    await user.click(screen.getByRole('button', { name: 'Crea record' }))

    expect(await screen.findByText(/Limite mensile raggiunto/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Passa a un piano superiore/i })).toBeInTheDocument()
  })

  it('un\'app non entitled è bloccata dalla route guard (modulo non montato)', async () => {
    renderApp({ route: '/app/@@APP_ID@@', entitled: [] })
    await waitFor(() =>
      expect(screen.queryByTestId('@@APP_ID@@-module')).not.toBeInTheDocument(),
    )
  })

  it('nessuna violazione di accessibilità sull\'elenco', async () => {
    const { container } = renderApp({ route: '/app/@@APP_ID@@', entitled: ['@@APP_ID@@'] })
    await screen.findByText('Mario Rossi')
    expect(await axe(container)).toHaveNoViolations()
  })
})
