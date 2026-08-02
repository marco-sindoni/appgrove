import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach, vi } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { SubscriptionsPanel } from './SubscriptionsPanel'
import { useAuthStore } from '../auth/authStore'
import { renderWithProviders, fakeAccessToken } from '../test/utils'

const SUBSCRIPTIONS_URL = 'http://localhost/api/platform/v1/me/subscriptions'
const TIERS_URL = 'http://localhost/api/platform/v1/checkout/apps/:appSlug/tiers'
const CANCEL_URL = 'http://localhost/api/platform/v1/me/subscriptions/:appSlug/cancel'
const CHANGE_URL = 'http://localhost/api/platform/v1/me/subscriptions/:appSlug/change-tier'

let subscriptions: Array<Record<string, unknown>>
let failSubscriptions = false
let commands: Array<{ path: string; body?: unknown }>

const subscription = (over: Record<string, unknown> = {}) => ({
  appSlug: 'notes',
  appName: 'Notes',
  status: 'active',
  phase: 'ACTIVE',
  tierKey: 'pro',
  tierName: 'Notes Pro',
  currentPeriodEnd: '2026-09-12T00:00:00Z',
  limits: { seats: { cap: 10, nature: 'stock' } },
  usage: {},
  blockedTiers: {},
  canUpgrade: true,
  canDowngrade: true,
  canCancel: true,
  canResume: false,
  canReactivate: false,
  portalAvailable: false,
  appDisabled: false,
  ...over,
})

const tiers = {
  appId: 'a1',
  slug: 'notes',
  name: 'Notes',
  tiers: [
    {
      tierId: 't-free',
      key: 'free',
      name: 'Free',
      limits: { metric: 'seats', cap: 2, type: 'stock' },
      features: {},
      trialDays: 0,
      prices: [{ billingCycle: 'monthly', amount: 0, currency: 'EUR' }],
    },
    {
      tierId: 't-pro',
      key: 'pro',
      name: 'Notes Pro',
      limits: { metric: 'seats', cap: 10, type: 'stock' },
      features: {},
      trialDays: 0,
      prices: [
        { billingCycle: 'monthly', amount: 900, currency: 'EUR' },
        { billingCycle: 'annual', amount: 9000, currency: 'EUR' },
      ],
    },
    {
      tierId: 't-team',
      key: 'team',
      name: 'Team',
      limits: { metric: 'seats', cap: 50, type: 'stock' },
      features: {},
      trialDays: 0,
      prices: [{ billingCycle: 'monthly', amount: 1900, currency: 'EUR' }],
    },
  ],
}

const server = setupServer(
  http.get(SUBSCRIPTIONS_URL, () => {
    if (failSubscriptions) return HttpResponse.json({ title: 'boom' }, { status: 500 })
    return HttpResponse.json({ subscriptions })
  }),
  http.get(TIERS_URL, () => HttpResponse.json(tiers)),
  http.post(CANCEL_URL, () => {
    commands.push({ path: 'cancel' })
    return HttpResponse.json({ direction: 'CANCEL', effectiveAt: '2026-09-12T00:00:00Z' })
  }),
  http.post(CHANGE_URL, async ({ request }) => {
    commands.push({ path: 'change-tier', body: await request.json() })
    return HttpResponse.json({ direction: 'DOWNGRADE', effectiveAt: '2026-09-12T00:00:00Z' })
  }),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  failSubscriptions = false
  commands = []
  subscriptions = [subscription()]
  useAuthStore.getState().setSession({ accessToken: fakeAccessToken({ roles: ['owner'] }) })
})

const noop = () => {}

/** La riga di un piano nella finestra: il nome compare anche come dicitura di prezzo ("Free"). */
function planRow(dialog: HTMLElement, name: string): HTMLElement {
  const row = within(dialog)
    .getAllByRole('listitem')
    .find((li) => within(li).queryByText(name, { selector: 'span.font-medium' }))
  if (!row) throw new Error(`piano non trovato: ${name}`)
  return row
}

describe('Sezione Abbonamenti (UC 0067)', () => {
  it('mostra uno scheletro mentre carica, non una riga di testo', async () => {
    renderWithProviders(<SubscriptionsPanel onReactivate={noop} />)
    expect(screen.getByRole('status', { name: 'Loading…' })).toBeInTheDocument()
    expect(await screen.findByText('Notes')).toBeInTheDocument()
  })

  it('un guasto della lista offre una riprova', async () => {
    failSubscriptions = true
    renderWithProviders(<SubscriptionsPanel onReactivate={noop} />)
    const alert = await screen.findByRole('alert', {}, { timeout: 5_000 })
    expect(within(alert).getByText('Something went wrong')).toBeInTheDocument()

    failSubscriptions = false
    await userEvent.click(within(alert).getByRole('button', { name: 'Retry' }))
    expect(await screen.findByText('Notes')).toBeInTheDocument()
  })

  it('mostra il consumo della quota misurata, con barra e avviso di soglia', async () => {
    subscriptions = [subscription({ usage: { seats: 9 } })]
    renderWithProviders(<SubscriptionsPanel onReactivate={noop} />)
    expect(await screen.findByText('9 of 10 seats used')).toBeInTheDocument()
    expect(
      screen.getByText('You are close to your plan limit for seats. Consider moving to a higher plan.'),
    ).toBeInTheDocument()
  })

  it('a limite raggiunto lo dice con l’invito a passare di piano', async () => {
    subscriptions = [subscription({ usage: { seats: 10 } })]
    renderWithProviders(<SubscriptionsPanel onReactivate={noop} />)
    expect(
      await screen.findByText('Plan limit reached for seats. Move to a higher plan to keep going.'),
    ).toBeInTheDocument()
  })

  it('per una metrica di cui non conosciamo l’uso mostra il solo limite del piano', async () => {
    subscriptions = [
      subscription({ limits: { invoices: { cap: 50, nature: 'flow', window: 'month' } }, usage: {} }),
    ]
    renderWithProviders(<SubscriptionsPanel onReactivate={noop} />)
    expect(await screen.findByText(/50 invoices \/ month/)).toBeInTheDocument()
    expect(screen.queryByText(/of 50 invoices used/)).not.toBeInTheDocument()
  })

  it('un abbonamento in ritardo di pagamento avvisa e porta al portale', async () => {
    subscriptions = [subscription({ status: 'past_due', portalAvailable: true })]
    renderWithProviders(<SubscriptionsPanel onReactivate={noop} />)
    const alert = await screen.findByRole('alert')
    expect(within(alert).getByText('Payment overdue')).toBeInTheDocument()
    expect(within(alert).getByRole('button', { name: 'Update payment method' })).toBeInTheDocument()
  })

  it('un abbonamento scaduto offre riattivazione e diritti sui dati', async () => {
    subscriptions = [
      subscription({ phase: 'ENDED', status: 'canceled', canReactivate: true, canCancel: false }),
    ]
    renderWithProviders(<SubscriptionsPanel onReactivate={noop} />)
    expect(await screen.findByText('Subscription expired')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reactivate' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Export / delete your data' })).toBeInTheDocument()
  })

  it('a chi non è titolare le azioni di fatturazione appaiono disabilitate, con la ragione', async () => {
    useAuthStore.getState().setSession({ accessToken: fakeAccessToken({ roles: ['member'] }) })
    renderWithProviders(<SubscriptionsPanel onReactivate={noop} />)
    expect(await screen.findByText('Only the workspace owner can change billing.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Change plan' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled()
  })

  it('la finestra cambio piano mostra prezzi, piano attuale e piano consigliato', async () => {
    renderWithProviders(<SubscriptionsPanel onReactivate={noop} />)
    await userEvent.click(await screen.findByRole('button', { name: 'Change plan' }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText('€9.00 /mo')).toBeInTheDocument()
    expect(within(dialog).getByText('€19.00 /mo')).toBeInTheDocument()
    // il piano corrente è marcato e non selezionabile; il primo superiore è il consigliato
    expect(within(dialog).getAllByText('Current plan').length).toBeGreaterThan(0)
    expect(within(dialog).getByText('Recommended')).toBeInTheDocument()
    expect(within(planRow(dialog, 'Team'), ).getByRole('button', { name: 'Choose' })).toBeEnabled()
  })

  it('un piano non ammissibile è disabilitato e spiega perché', async () => {
    subscriptions = [
      subscription({
        usage: { seats: 8 },
        blockedTiers: { free: 'Downgrade bloccato: la metrica «seats» è a 8, sopra il limite 2.' },
      }),
    ]
    renderWithProviders(<SubscriptionsPanel onReactivate={noop} />)
    await userEvent.click(await screen.findByRole('button', { name: 'Change plan' }))

    const dialog = await screen.findByRole('dialog')
    const free = planRow(dialog, 'Free')
    expect(within(free).getByText(/sopra il limite 2/)).toBeInTheDocument()
    expect(within(free).getByRole('button', { name: 'Choose' })).toBeDisabled()
  })

  it('la riduzione passa da una conferma che dice da quando, e solo allora invia il comando', async () => {
    renderWithProviders(<SubscriptionsPanel onReactivate={noop} />)
    await userEvent.click(await screen.findByRole('button', { name: 'Change plan' }))

    const dialog = await screen.findByRole('dialog')
    await userEvent.click(within(planRow(dialog, 'Free')).getByRole('button', { name: 'Choose' }))

    expect(await screen.findByText('Move to a lower plan')).toBeInTheDocument()
    expect(screen.getByText(/from 9\/12\/2026/)).toBeInTheDocument()
    // nessun comando finché non si conferma
    expect(commands).toHaveLength(0)

    await userEvent.click(screen.getByRole('button', { name: 'Confirm' }))
    await waitFor(() => expect(commands).toHaveLength(1))
    expect(commands[0]).toEqual({
      path: 'change-tier',
      body: { targetTierKey: 'free', billingCycle: 'monthly' },
    })
  })

  it('il ciclo annuale cambia i prezzi mostrati e viene usato nel comando', async () => {
    renderWithProviders(<SubscriptionsPanel onReactivate={noop} />)
    await userEvent.click(await screen.findByRole('button', { name: 'Change plan' }))

    const dialog = await screen.findByRole('dialog')
    await userEvent.click(within(dialog).getByRole('radio', { name: 'Annual' }))
    expect(await within(dialog).findByText('€90.00 /yr')).toBeInTheDocument()
  })

  it('la disdetta chiede conferma nell’applicazione, con la data di fine accesso', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm')
    renderWithProviders(<SubscriptionsPanel onReactivate={noop} />)
    await userEvent.click(await screen.findByRole('button', { name: 'Cancel' }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText('Cancel subscription')).toBeInTheDocument()
    expect(within(dialog).getByText(/access stays until 9\/12\/2026/)).toBeInTheDocument()
    expect(confirmSpy).not.toHaveBeenCalled()

    await userEvent.click(within(dialog).getByRole('button', { name: 'Confirm' }))
    await waitFor(() => expect(commands).toEqual([{ path: 'cancel' }]))
    confirmSpy.mockRestore()
  })

  it('dopo un comando la card dichiara l’aggiornamento in corso finché il dato non lo riflette', async () => {
    renderWithProviders(<SubscriptionsPanel onReactivate={noop} />)
    await userEvent.click(await screen.findByRole('button', { name: 'Cancel' }))
    const dialog = await screen.findByRole('dialog')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Confirm' }))

    expect(
      await screen.findByText('Update in progress — waiting for confirmation from the payment provider.'),
    ).toBeInTheDocument()

    // il webhook arriva: il read-model riflette la disdetta e lo stato transitorio sparisce da solo
    subscriptions = [subscription({ cancelAt: '2026-09-12T00:00:00Z', canCancel: false, canResume: true })]
    await waitFor(
      () =>
        expect(
          screen.queryByText(
            'Update in progress — waiting for confirmation from the payment provider.',
          ),
        ).not.toBeInTheDocument(),
      { timeout: 5_000 },
    )
  })
})
