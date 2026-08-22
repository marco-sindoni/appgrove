import { test, expect, type Page } from '@playwright/test'

const ORIGIN = 'http://localhost:4173'

function base64url(obj: unknown): string {
  return Buffer.from(JSON.stringify(obj))
    .toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}
const jwt = (payload: Record<string, unknown>) =>
  `${base64url({ alg: 'none' })}.${base64url(payload)}.sig`
const accessToken = jwt({ sub: 'u1', tenant_id: 'tenant-1', roles: ['owner'], upn: 'owner@acme.test' })
const idToken = jwt({ sub: 'u1', email: 'owner@acme.test', name: 'Owner' })
const tokenBody = { access_token: accessToken, id_token: idToken, token_type: 'Bearer' }

/**
 * **L'invito che passa dalla cassa**, visto dallo schermo (UC 0103).
 *
 * Questo percorso prova la cosa che nessuna prova di servizio può provare: che chi sta per spendere dei
 * soldi **li vede prima**. Tre momenti, in ordine:
 *
 * 1. il riquadro dei posti dice quanti posti si usano, che non si sta pagando nulla, e quanto costerà il
 *    prossimo;
 * 2. la **stima** appare accanto al modulo dell'invito, col numero d'ordine del posto e il nuovo totale;
 * 3. dopo l'invito il riquadro cambia da sé: il posto è occupato dall'invito, non dall'accettazione.
 *
 * E poi il caso che conta di più: l'**addebito rifiutato**. L'invito non compare nell'elenco e il motivo
 * del fornitore è a schermo — perché «non è andata» senza il perché non permette di rimediare.
 *
 * Il backend è simulato ({@link https://playwright.dev} `page.route`): il tratto con lo stack vero, il
 * simulatore del fornitore di pagamento e la banca dati appartiene alla suite di piattaforma e arriva con
 * UC 0113. Quello che si può già provare qui — le frasi che il cliente legge e il pulsante che resta
 * spento — si prova qui.
 */

/** Lo stato dei posti che il servizio restituirebbe: cambia dopo l'invito, come nella realtà. */
interface SeatsState {
  usedSeats: number
  paidSeats: number
  dueCents: number
  paidQuantity: number
  hasSubscription: boolean
  invited: number
}

const franchigia: SeatsState = {
  usedSeats: 3,
  paidSeats: 0,
  dueCents: 0,
  paidQuantity: 0,
  hasSubscription: false,
  invited: 0,
}

/** Il corpo del riquadro, derivato dallo stato — come lo calcolerebbe il servizio col listino iniziale. */
function seatsBody(s: SeatsState) {
  const nextSeat = s.usedSeats + 1
  const paidAfter = Math.max(0, nextSeat - 3)
  const dueAfter = paidAfter * 299
  return {
    usedSeats: s.usedSeats,
    composition: { active: s.usedSeats - s.invited, suspended: 0, pendingInvitations: s.invited },
    currency: 'EUR',
    freeSeats: 3,
    paidSeats: s.paidSeats,
    dueCents: s.dueCents,
    paidQuantity: s.paidQuantity,
    currentBand:
      s.usedSeats <= 3
        ? { fromSeat: 1, toSeat: 3, unitPriceCents: 0 }
        : { fromSeat: 4, toSeat: 10, unitPriceCents: 299 },
    next: {
      seatNumber: nextSeat,
      unitPriceCents: nextSeat <= 3 ? 0 : 299,
      dueCentsAfter: dueAfter,
      chargeCents: Math.max(0, dueAfter - s.paidQuantity * 299),
      cheaperThanPrevious: false,
    },
    pendingReduction: false,
    hasSubscription: s.hasSubscription,
  }
}

/**
 * Avvia la SPA già autenticata come owner, con il riquadro dei posti e gli inviti simulati.
 *
 * @param declineReason se valorizzato, il servizio rifiuta l'addebito del posto con quel motivo (402) e
 *     l'invito **non** nasce: è il percorso della carta scaduta.
 */
async function mockAuthed(page: Page, declineReason?: string) {
  const state: SeatsState = { ...franchigia }
  const invites: Array<Record<string, unknown>> = []
  const people = [
    { id: 'u1', email: 'owner@acme.test', displayName: 'Owner', role: 'owner', status: 'active', tenantId: 'tenant-1', joinedAt: '2024-01-01T00:00:00Z', apps: [] },
    { id: 'u2', email: 'due@acme.test', displayName: 'Due', role: 'member', status: 'active', tenantId: 'tenant-1', joinedAt: '2025-01-01T00:00:00Z', apps: [] },
    { id: 'u3', email: 'tre@acme.test', displayName: 'Tre', role: 'member', status: 'active', tenantId: 'tenant-1', joinedAt: '2025-06-01T00:00:00Z', apps: [] },
  ]

  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: { env: 'local', authBaseUrl: ORIGIN, coreBaseUrl: ORIGIN, cognito: { userPoolId: '', clientId: '' } },
    }),
  )
  await page.route('**/api/auth/refresh', (route) => route.fulfill({ json: tokenBody }))
  await page.route('**/api/platform/v1/users/me', (route) =>
    route.fulfill({ json: { id: 'u1', email: 'owner@acme.test', displayName: 'Owner', role: 'owner', status: 'active', tenantId: 'tenant-1' } }),
  )
  await page.route('**/api/platform/v1/accounts/me', (route) =>
    route.fulfill({ json: { id: 'a1', name: 'Acme', status: 'active' } }),
  )
  await page.route('**/api/platform/v1/me/entitlements', (route) =>
    route.fulfill({ json: { entitlements: [] } }),
  )
  await page.route('**/api/platform/v1/users?*', (route) =>
    route.fulfill({ json: { content: people, page: 0, size: 100, totalElements: people.length } }),
  )
  await page.route('**/api/platform/v1/invitations?*', (route) =>
    route.fulfill({ json: { content: invites, page: 0, size: 100, totalElements: invites.length } }),
  )
  await page.route('**/api/platform/v1/me/seats', (route) =>
    route.fulfill({ json: seatsBody(state) }),
  )
  await page.route('**/api/platform/v1/invitations', async (route, request) => {
    if (declineReason) {
      // L'ordine degli atti visto da fuori: il servizio addebita PRIMA di creare, quindi un rifiuto non
      // lascia nulla dietro di sé — né una riga di invito, né un posto occupato.
      await route.fulfill({
        status: 402,
        contentType: 'application/problem+json',
        body: JSON.stringify({
          type: 'urn:appgrove:seats:charge-declined',
          title: 'Payment Required',
          status: 402,
          detail: declineReason,
        }),
      })
      return
    }
    const email = (request.postDataJSON() as { email: string }).email
    invites.push({ id: 'inv-1', email, status: 'pending', expiresAt: '2026-07-03T00:00:00Z' })
    // Il posto è occupato dall'invito, non dall'accettazione: il riquadro cambia adesso.
    state.usedSeats += 1
    state.invited += 1
    state.paidSeats = Math.max(0, state.usedSeats - 3)
    state.paidQuantity = state.paidSeats
    state.dueCents = state.paidSeats * 299
    state.hasSubscription = state.paidSeats > 0
    await route.fulfill({
      status: 201,
      json: { id: 'inv-1', email, status: 'pending', expiresAt: '2026-07-03T00:00:00Z', token: 'tok-1' },
    })
  })
  await page.route('**/api/auth/invitations/send', (route) => route.fulfill({ status: 202, body: '' }))
}

test('[J-SEATS] posti: riquadro, stima prima della conferma, primo posto a pagamento e addebito rifiutato', async ({
  page,
}) => {
  await mockAuthed(page)
  await page.goto('/members')
  await expect(page.getByRole('heading', { name: 'Members', level: 1 })).toBeVisible()

  // ── 1. Il riquadro: quanti posti, quanto pago, quanto costa il prossimo ─────
  await expect(page.getByText('3 seats in use')).toBeVisible()
  await expect(page.getByText('3 active · 0 suspended · 0 pending invitations')).toBeVisible()
  // «Compresi», non «€0,00»: non sono la stessa promessa.
  await expect(page.getByText('You are paying nothing: the first 3 seats are included.')).toBeVisible()
  await expect(page.getByText('The next seat costs €2.99: your total will become €2.99.')).toBeVisible()

  // ── 2. La stima, PRIMA della conferma ──────────────────────────────────────
  await expect(
    page.getByText(
      'This person will be seat number 4: it costs €2.99 per month, and your total will go from €0.00 to €2.99.',
    ),
  ).toBeVisible()

  // ── 3. L'invito: il posto è occupato subito, e il riquadro lo dice ──────────
  await page.getByLabel('Email').fill('quattro@acme.test')
  await page.getByRole('button', { name: 'Send invitation' }).click()

  await expect(page.getByText('Invitation sent to quattro@acme.test.')).toBeVisible()
  await expect(page.getByText('4 seats in use')).toBeVisible()
  await expect(page.getByText('You are paying €2.99 per month')).toBeVisible()
  await expect(page.getByText('1 paid seat')).toBeVisible()
  await expect(page.getByText('seats 4–10: €2.99 each')).toBeVisible()
  // La persona è nell'elenco come invito in attesa, e occupa già il posto.
  const invited = page.getByRole('row').filter({ hasText: 'quattro@acme.test' })
  await expect(invited).toHaveCount(1)
  await expect(invited.getByText('Invitation pending')).toBeVisible()
})

test('[J-SEATS] senza addebito riuscito l’invito non nasce, e il motivo del fornitore è a schermo', async ({
  page,
}) => {
  await mockAuthed(page, 'carta scaduta')
  await page.goto('/members')
  await expect(page.getByText('3 seats in use')).toBeVisible()

  await page.getByLabel('Email').fill('quattro@acme.test')
  await page.getByRole('button', { name: 'Send invitation' }).click()

  await expect(
    page.getByText(
      'The payment for the seat did not go through (carta scaduta): the invitation was not created. Check your payment method in Billing and try again.',
    ),
  ).toBeVisible()
  // Nessuna riga nuova: l'invito non è nato. È la regola d'oro della storia, letta a schermo.
  await expect(page.getByRole('row').filter({ hasText: 'quattro@acme.test' })).toHaveCount(0)
  await expect(page.getByText('3 seats in use')).toBeVisible()
})
