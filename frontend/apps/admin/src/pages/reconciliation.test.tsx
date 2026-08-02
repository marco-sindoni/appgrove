import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'jest-axe'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { Reconciliation } from './Reconciliation'
import { useAuthStore } from '../auth/authStore'
import { renderWithProviders, fakeAccessToken } from '../test/utils'

const URL = 'http://localhost/api/platform/v1/admin/reconciliation'

/** Vista con tutto acceso: soglia superata, accredito in ritardo, i tre esiti di quadratura. */
const FULL = {
  currency: 'EUR',
  totals: {
    gross: 100_000,
    fee: 9_000,
    net: 91_000,
    reversed: 2_000,
    settled: 40_000,
    unsettled: 51_000,
    transactions: 20,
    estimatedFeeTransactions: 7,
    oldestUnsettledAt: '2026-06-01T00:00:00Z',
  },
  periods: [
    {
      period: '2026-07',
      gross: 60_000,
      fee: 6_000,
      net: 54_000,
      reversed: 2_000,
      transactions: 12,
      feePercent: 10,
      feeOverThreshold: true,
    },
    {
      period: '2026-06',
      gross: 40_000,
      fee: 3_000,
      net: 37_000,
      reversed: 0,
      transactions: 8,
      feePercent: 7.5,
      feeOverThreshold: false,
    },
  ],
  payouts: [
    {
      paddlePayoutId: 'pay_ok',
      paidAt: '2026-07-15T00:00:00Z',
      currency: 'EUR',
      amount: 37_000,
      linesNet: 37_000,
      difference: 0,
      status: 'matched',
      lines: 8,
      coveredFrom: '2026-06-01T00:00:00Z',
      coveredTo: '2026-06-30T00:00:00Z',
    },
    {
      paddlePayoutId: 'pay_ko',
      paidAt: '2026-07-01T00:00:00Z',
      currency: 'EUR',
      amount: 2_900,
      linesNet: 3_000,
      difference: -100,
      status: 'mismatch',
      lines: 1,
      coveredFrom: null,
      coveredTo: null,
    },
    {
      paddlePayoutId: 'pay_mixed',
      paidAt: '2026-06-20T00:00:00Z',
      currency: 'EUR',
      amount: 1_000,
      linesNet: 900,
      difference: null,
      status: 'mixed_currency',
      lines: 2,
      coveredFrom: null,
      coveredTo: null,
    },
  ],
  feeAlertPercent: 8,
  payoutOverdue: true,
  payoutMaxAgeDays: 14,
}

/** Vista "tutto tranquillo": nessun avviso, nessuna stima. */
const QUIET = {
  ...FULL,
  totals: { ...FULL.totals, estimatedFeeTransactions: 0, oldestUnsettledAt: null, unsettled: 0 },
  periods: [{ ...FULL.periods[1] }],
  payouts: [FULL.payouts[0]],
  payoutOverdue: false,
}

let payload: Record<string, unknown> = FULL
// quante chiamate devono fallire: la lettura ritenta una volta da sola, quindi per vedere lo stato
// di errore servono due fallimenti, e la riprova dell'utente è la terza chiamata.
let failures = 0
let calls = 0

const server = setupServer(
  http.get(URL, () => {
    calls += 1
    if (calls <= failures) {
      return new HttpResponse(null, { status: 500 })
    }
    return HttpResponse.json(payload)
  }),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  payload = FULL
  failures = 0
  calls = 0
  useAuthStore
    .getState()
    .setSession({ accessToken: fakeAccessToken({ roles: ['platform-admin'] }) })
})

describe('Riconciliazione netto/revenue (UC 0071)', () => {
  it('mostra i totali della catena lordo → commissioni → netto → in attesa', async () => {
    renderWithProviders(<Reconciliation />)
    expect(await screen.findByText('€1,000.00')).toBeInTheDocument() // lordo
    expect(screen.getByText('€90.00')).toBeInTheDocument() // commissioni
    expect(screen.getByText('€910.00')).toBeInTheDocument() // netto
    expect(screen.getByText('€510.00')).toBeInTheDocument() // in attesa di accredito
  })

  it('attribuisce ogni riga al mese dell’addebito e segnala il mese sopra soglia', async () => {
    renderWithProviders(<Reconciliation />)
    const heavy = within(await screen.findByRole('row', { name: /2026-07/ }))
    expect(heavy.getByText('10.00%')).toBeInTheDocument()
    const light = within(screen.getByRole('row', { name: /2026-06/ }))
    expect(light.getByText('7.50%')).toBeInTheDocument()
  })

  it('mostra la quadratura di ogni accredito, incluso lo scostamento e la valuta mista', async () => {
    renderWithProviders(<Reconciliation />)
    const ok = within(await screen.findByRole('row', { name: /pay_ok/ }))
    expect(ok.getByText('Matched')).toBeInTheDocument()

    const ko = within(screen.getByRole('row', { name: /pay_ko/ }))
    expect(ko.getByText('Difference')).toBeInTheDocument()
    expect(ko.getByText('-€1.00')).toBeInTheDocument()

    // valute diverse: nessuno scostamento inventato, solo l'avvertimento che non è quadrabile
    const mixed = within(screen.getByRole('row', { name: /pay_mixed/ }))
    expect(mixed.getByText('Mixed currency')).toBeInTheDocument()
    expect(mixed.getAllByText('—').length).toBeGreaterThan(0)
  })

  it('avvisa quando l’accredito atteso non è arrivato e quando le commissioni pesano troppo', async () => {
    renderWithProviders(<Reconciliation />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Expected payout has not arrived')
    expect(screen.getByRole('status')).toHaveTextContent('Fees weigh more than expected')
  })

  it('dichiara quante commissioni sono stimate invece che dichiarate dal fornitore', async () => {
    renderWithProviders(<Reconciliation />)
    expect(await screen.findByText(/7 transactions have an estimated fee/)).toBeInTheDocument()
  })

  it('senza avvisi non mostra né allarme né nota sulle stime', async () => {
    payload = QUIET
    renderWithProviders(<Reconciliation />)
    await screen.findByRole('row', { name: /2026-06/ })
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(screen.queryByText(/estimated fee/)).not.toBeInTheDocument()
  })

  it('stato vuoto quando non c’è ancora nulla da riconciliare', async () => {
    payload = { ...QUIET, periods: [], payouts: [], totals: { ...QUIET.totals, gross: 0 } }
    renderWithProviders(<Reconciliation />)
    expect(await screen.findByText('No transactions recorded yet.')).toBeInTheDocument()
  })

  it('errore con riprova: il pulsante rilancia la lettura', async () => {
    failures = 2
    const user = userEvent.setup()
    renderWithProviders(<Reconciliation />)
    // la lettura ritenta da sola con una breve attesa: lo stato di errore compare dopo il secondo
    // fallimento, non subito
    const retry = await screen.findByRole('button', { name: /retry/i }, { timeout: 5000 })
    await user.click(retry)
    expect(await screen.findByRole('row', { name: /2026-07/ })).toBeInTheDocument()
  })

  it('non ha violazioni di accessibilità', async () => {
    const { container } = renderWithProviders(<Reconciliation />)
    await screen.findByRole('row', { name: /2026-07/ })
    expect(await axe(container)).toHaveNoViolations()
  })
})
