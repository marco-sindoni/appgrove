import { test, expect, type Page } from '@playwright/test'

const ORIGIN = 'http://localhost:4174'

function base64url(obj: unknown): string {
  return Buffer.from(JSON.stringify(obj))
    .toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}
const jwt = (payload: Record<string, unknown>) =>
  `${base64url({ alg: 'none' })}.${base64url(payload)}.sig`
const accessToken = jwt({
  sub: 'admin-1',
  tenant_id: 'tenant-1',
  roles: ['platform-admin'],
  upn: 'admin@x.io',
})
const idToken = jwt({ sub: 'admin-1', email: 'admin@x.io', name: 'Admin Uno' })
const tokenBody = { access_token: accessToken, id_token: idToken, token_type: 'Bearer' }

/**
 * Vista di riconciliazione con tutti i casi che contano: un mese sopra la soglia delle commissioni, un
 * accredito quadrato, uno in scostamento, uno non quadrabile per valute diverse, e l'accredito atteso
 * che non è arrivato.
 */
const RECONCILIATION = {
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
      paddlePayoutId: 'pay_quadra',
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
      paddlePayoutId: 'pay_scostamento',
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
      paddlePayoutId: 'pay_valuta_mista',
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

/** Già autenticato come amministratore di piattaforma, con gli endpoint della console simulati. */
async function mockAuthed(page: Page) {
  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: {
        env: 'local',
        authBaseUrl: ORIGIN,
        coreBaseUrl: ORIGIN,
        cognito: { userPoolId: '', clientId: '' },
      },
    }),
  )
  await page.route('**/api/auth/refresh', (route) => route.fulfill({ json: tokenBody }))
  await page.route('**/api/platform/v1/admin/overview', (route) =>
    route.fulfill({ json: { accounts: 12, users: 47, activeSubscriptions: 8, disabledApps: 2 } }),
  )
  await page.route('**/api/platform/v1/admin/reconciliation', (route) =>
    route.fulfill({ json: RECONCILIATION }),
  )
}

test('[L2-ADMIN-RECON] panoramica → riconciliazione: lordo → commissioni → netto → accrediti, con quadratura e avvisi', async ({
  page,
}) => {
  await mockAuthed(page)

  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Overview' })).toBeVisible()

  // la pagina si raggiunge dal menu, nel gruppo Revenue accanto a Fatturazione
  await page.getByRole('link', { name: 'Reconciliation' }).click()
  await expect(page.getByRole('heading', { name: 'Reconciliation' })).toBeVisible()

  // i quattro totali della catena
  await expect(page.getByText('€1,000.00')).toBeVisible() // lordo incassato
  await expect(page.getByText('€90.00')).toBeVisible() // commissioni
  await expect(page.getByText('€910.00')).toBeVisible() // netto
  await expect(page.getByText('€510.00')).toBeVisible() // ancora da accreditare

  // l'accredito atteso che non arriva è un avviso, non un dettaglio in fondo alla pagina
  await expect(page.getByRole('alert')).toContainText('Expected payout has not arrived')
  // e la nota dice quante commissioni sono stimate invece che dichiarate dal fornitore
  await expect(page.getByText(/7 transactions have an estimated fee/)).toBeVisible()

  // righe per mese di addebito: il mese sopra soglia è evidenziato
  const heavyMonth = page.getByRole('row', { name: /2026-07/ })
  await expect(heavyMonth).toContainText('10.00%')
  await expect(page.getByRole('row', { name: /2026-06/ })).toContainText('7.50%')

  // quadratura degli accrediti: i tre esiti sono distinguibili a colpo d'occhio
  await expect(page.getByRole('row', { name: /pay_quadra/ })).toContainText('Matched')
  const mismatch = page.getByRole('row', { name: /pay_scostamento/ })
  await expect(mismatch).toContainText('Difference')
  await expect(mismatch).toContainText('-€1.00')
  await expect(page.getByRole('row', { name: /pay_valuta_mista/ })).toContainText('Mixed currency')
})
