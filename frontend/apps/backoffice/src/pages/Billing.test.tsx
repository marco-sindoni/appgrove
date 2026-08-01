import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen, within } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { Billing } from './Billing'
import { useAuthStore } from '../auth/authStore'
import { renderWithProviders, fakeAccessToken } from '../test/utils'

const SUBSCRIPTIONS_URL = 'http://localhost/api/platform/v1/me/subscriptions'
const PAYMENTS_URL = 'http://localhost/api/platform/v1/me/payments'
const CATALOG_URL = 'http://localhost/api/platform/v1/me/catalog'

let subscriptions: Array<Record<string, unknown>>
let payments: Array<Record<string, unknown>>
let failPayments = false

const subscription = (over: Record<string, unknown> = {}) => ({
  appSlug: 'notes',
  appName: 'Notes',
  status: 'active',
  phase: 'ACTIVE',
  tierKey: 'pro',
  tierName: 'Notes Pro',
  limits: {},
  canUpgrade: true,
  canDowngrade: true,
  canCancel: true,
  canResume: false,
  canReactivate: false,
  portalAvailable: false,
  appDisabled: false,
  ...over,
})

const payment = (over: Record<string, unknown> = {}) => ({
  billedAt: '2026-07-31T10:00:00Z',
  appSlug: 'notes',
  appName: 'Notes',
  planName: 'Notes Pro',
  billingCycle: 'monthly',
  amount: 900,
  currency: 'EUR',
  status: 'paid',
  receiptUrl: 'https://paddle.example/r/1',
  ...over,
})

const server = setupServer(
  http.get(SUBSCRIPTIONS_URL, () => HttpResponse.json({ subscriptions })),
  http.get(PAYMENTS_URL, () => {
    if (failPayments) return HttpResponse.json({ title: 'boom' }, { status: 500 })
    return HttpResponse.json({ payments })
  }),
  // Il catalogo non deve essere interpellato da Billing: se lo fosse, questo gestore lo renderebbe
  // invisibile — perciò l'assenza di elementi di vetrina è asserita sul reso, non sulla rete.
  http.get(CATALOG_URL, () => HttpResponse.json({ apps: [] })),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  failPayments = false
  subscriptions = [subscription()]
  payments = [payment()]
  useAuthStore.getState().setSession({ accessToken: fakeAccessToken({ roles: ['owner'] }) })
})

describe('Pagina Billing — solo fatturazione (UC 0096)', () => {
  it('si intitola "Billing" e non contiene più alcun elemento di vetrina', async () => {
    renderWithProviders(<Billing />)
    expect(await screen.findByRole('heading', { name: 'Billing', level: 1 })).toBeInTheDocument()
    expect(screen.getByText('Manage your plans, payments and receipts.')).toBeInTheDocument()
    // il vecchio titolo d'acquisto e la griglia delle app acquistabili sono migrati nel catalogo
    expect(screen.queryByText('Get an app')).not.toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Choose an app' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Subscribe' })).not.toBeInTheDocument()
  })

  it('mostra le due sezioni: abbonamenti e storico pagamenti', async () => {
    renderWithProviders(<Billing />)
    expect(await screen.findByRole('heading', { name: 'Your subscriptions' })).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Payments & receipts' })).toBeInTheDocument()
  })

  it('la tabella dei pagamenti mostra importo, esito e ricevuta', async () => {
    renderWithProviders(<Billing />)
    const table = await screen.findByRole('table')
    expect(within(table).getByText('Notes Pro — monthly')).toBeInTheDocument()
    expect(within(table).getByText('€9.00')).toBeInTheDocument()
    expect(within(table).getByText('Paid')).toBeInTheDocument()
    const receipt = within(table).getByRole('link', { name: /Receipt/ })
    expect(receipt).toHaveAttribute('href', 'https://paddle.example/r/1')
    expect(receipt).toHaveAttribute('target', '_blank')
  })

  it('un pagamento fallito compare con il suo esito', async () => {
    payments = [payment({ status: 'failed', receiptUrl: undefined })]
    renderWithProviders(<Billing />)
    const table = await screen.findByRole('table')
    expect(within(table).getByText('Failed')).toBeInTheDocument()
    // ricevuta non disponibile: la riga resta, senza collegamento e con uno stato onesto
    expect(within(table).queryByRole('link', { name: /Receipt/ })).not.toBeInTheDocument()
    expect(within(table).getByText('Not available yet')).toBeInTheDocument()
  })

  it('senza pagamenti mostra lo stato vuoto della sola sezione', async () => {
    payments = []
    renderWithProviders(<Billing />)
    expect(await screen.findByText('No payments yet.')).toBeInTheDocument()
    // gli abbonamenti restano visibili: sono due sezioni indipendenti
    expect(screen.getByText('Notes')).toBeInTheDocument()
  })

  it('un guasto dello storico non rende rossa tutta la pagina', async () => {
    failPayments = true
    renderWithProviders(<Billing />)
    // Attesa generosa: la shell ritenta una volta prima di dichiarare il guasto (api/queryClient.ts).
    expect(
      await screen.findByText('We couldn’t load your payment history.', {}, { timeout: 5_000 }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument()
    // l'abbonamento è ancora lì, con le sue azioni: due guasti indipendenti
    expect(screen.getByText('Notes')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Change plan' })).toBeInTheDocument()
  })

  it('senza abbonamenti rimanda al catalogo invece di offrire una griglia d’acquisto', async () => {
    subscriptions = []
    renderWithProviders(<Billing />)
    expect(await screen.findByText('You don’t have any subscriptions yet.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Browse the app catalog' })).toBeInTheDocument()
  })

  it('un’app sospesa dalla piattaforma lo dice, con una via verso l’assistenza', async () => {
    subscriptions = [subscription({ appDisabled: true })]
    renderWithProviders(<Billing />)
    expect(await screen.findByText('App suspended by the platform')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Contact support' })).toBeInTheDocument()
  })

  it('a un membro la sezione pagamenti non viene nemmeno chiesta', async () => {
    useAuthStore.getState().setSession({ accessToken: fakeAccessToken({ roles: ['member'] }) })
    renderWithProviders(<Billing />)
    expect(
      await screen.findByText('Only owners and admins can see payments and receipts.'),
    ).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })
})
