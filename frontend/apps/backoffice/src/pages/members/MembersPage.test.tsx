import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach, vi } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'jest-axe'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { MembersPage } from './MembersPage'
import { useAuthStore } from '../../auth/authStore'
import { renderWithProviders, fakeAccessToken } from '../../test/utils'

const ME = { id: 'u-owner', email: 'owner@test', displayName: 'Owner', role: 'owner', status: 'active' }

let members: Array<Record<string, unknown>>
let invites: Array<Record<string, unknown>>
let sentEmails: Array<{ email: string; token: string; body: Record<string, unknown> }>
/**
 * Il riquadro dei posti (UC 0103). Mutabile perché i collaudi ne cambiano un pezzo alla volta: entro la
 * franchigia, oltre, al confine di fascia in cui il posto successivo costa meno, in errore.
 *
 * I numeri sono quelli che darebbe il servizio col listino iniziale — 3 posti compresi, poi 2,99 € —
 * perché è l'unico modo di provare le frasi che il cliente legge davvero.
 */
let seats: Record<string, unknown> | null
/** Esito della lettura dei posti: `null` = errore, per provare l'invito impedito. */
const franchigia = () => ({
  usedSeats: 3,
  composition: { active: 3, suspended: 0, pendingInvitations: 0 },
  currency: 'EUR',
  freeSeats: 3,
  paidSeats: 0,
  dueCents: 0,
  paidQuantity: 0,
  currentBand: { fromSeat: 1, toSeat: 3, unitPriceCents: 0 },
  next: {
    seatNumber: 4,
    unitPriceCents: 299,
    dueCentsAfter: 299,
    chargeCents: 299,
    cheaperThanPrevious: false,
  },
  pendingReduction: false,
  hasSubscription: false,
})

/**
 * Il riquadro con una **riduzione in attesa** (UC 0104): una persona indicata, esecuzione il 14 settembre,
 * dovuto che dai 2,99 € di oggi torna a zero (tre posti, tutti dentro la franchigia).
 */
const riduzioneInAttesa = () => ({
  ...franchigia(),
  usedSeats: 4,
  composition: { active: 4, suspended: 0, pendingInvitations: 0 },
  paidSeats: 1,
  dueCents: 299,
  paidQuantity: 1,
  currentBand: { fromSeat: 4, toSeat: 10, unitPriceCents: 299 },
  hasSubscription: true,
  pendingReduction: true,
  reduction: {
    id: 'red-1',
    executeAt: '2026-09-14T00:00:00Z',
    requestedAt: '2026-08-20T00:00:00Z',
    overdue: false,
    people: [{ userId: 'u-member', email: 'member@test', displayName: 'Member' }],
    seatsAfter: 3,
    dueCentsNow: 299,
    dueCentsAfter: 0,
    currency: 'EUR',
    bandsAfter: [
      { fromSeat: 1, toSeat: 3, unitPriceCents: 0, seats: 3, subtotalCents: 0 },
    ],
  },
})

/**
 * Marca `member@test` come **indicata per la cessazione**: è il campo che il servizio mette sulla riga
 * della persona (UC 0104) e da cui nasce il quarto stato dell'elenco.
 */
const indicata = (rows: Array<Record<string, unknown>>) =>
  rows.map((m) => (m.id === 'u-member' ? { ...m, endingAt: '2026-09-14T00:00:00Z' } : m))

/** L'effetto prima della conferma, come lo calcolerebbe il servizio. */
const anteprima = () => ({
  executeAt: '2026-09-14T00:00:00Z',
  people: [{ userId: 'u-member', email: 'member@test', displayName: 'Member' }],
  seatsNow: 4,
  seatsAfter: 3,
  dueCentsNow: 299,
  dueCentsAfter: 0,
  currency: 'EUR',
  bandsNow: [
    { fromSeat: 1, toSeat: 3, unitPriceCents: 0, seats: 3, subtotalCents: 0 },
    { fromSeat: 4, toSeat: 10, unitPriceCents: 299, seats: 1, subtotalCents: 299 },
  ],
  bandsAfter: [{ fromSeat: 1, toSeat: 3, unitPriceCents: 0, seats: 3, subtotalCents: 0 }],
})

const server = setupServer(
  http.get('http://localhost/api/platform/v1/users/me', () => HttpResponse.json(ME)),
  http.get('http://localhost/api/platform/v1/users', () =>
    HttpResponse.json({ content: members, page: 0, size: 100, totalElements: members.length }),
  ),
  http.get('http://localhost/api/platform/v1/invitations', () =>
    HttpResponse.json({ content: invites, page: 0, size: 100, totalElements: invites.length }),
  ),
  http.get('http://localhost/api/platform/v1/me/seats', () =>
    seats === null
      ? HttpResponse.json({ title: 'Internal Server Error', status: 500 }, { status: 500 })
      : HttpResponse.json(seats),
  ),
  http.post('http://localhost/api/platform/v1/invitations', async ({ request }) => {
    const body = (await request.json()) as { email: string }
    // UC 0118 — le due collisioni LECITE, distinte dal campo `type` del problem+json. La terza
    // situazione («quella persona ha già un account appgrove») NON è qui di proposito: risponde 201
    // come un indirizzo sconosciuto, perché non è un'informazione dell'account che invita.
    if (body.email === 'gia-membro@x.io') {
      return HttpResponse.json(
        { type: 'urn:appgrove:invitation:already-member', title: 'Conflict', status: 409 },
        { status: 409 },
      )
    }
    if (body.email === 'gia-invitato@x.io') {
      return HttpResponse.json(
        { type: 'urn:appgrove:invitation:already-invited', title: 'Conflict', status: 409 },
        { status: 409 },
      )
    }
    const created = {
      id: `inv-${invites.length + 1}`,
      email: body.email,
      status: 'pending',
      expiresAt: '2026-07-03T00:00:00Z',
      token: 'raw-token-xyz',
    }
    invites = [...invites, { ...created, token: undefined }]
    return HttpResponse.json(created, { status: 201 })
  }),
  http.delete('http://localhost/api/platform/v1/invitations/:id', ({ params }) => {
    invites = invites.filter((i) => i.id !== params.id)
    return new HttpResponse(null, { status: 204 })
  }),
  http.patch('http://localhost/api/platform/v1/users/:id', async ({ params, request }) => {
    const body = (await request.json()) as { role?: string; status?: string }
    members = members.map((m) => (m.id === params.id ? { ...m, ...body } : m))
    return HttpResponse.json(members.find((m) => m.id === params.id))
  }),
  http.post('http://localhost/api/auth/invitations/send', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>
    sentEmails.push({ email: body.email as string, token: body.token as string, body })
    return new HttpResponse(null, { status: 202 })
  }),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  members = [
    {
      id: 'u-owner',
      email: 'owner@test',
      displayName: 'Owner',
      role: 'owner',
      status: 'active',
      joinedAt: '2024-01-01T00:00:00Z',
      apps: [
        { appId: 'app-1', app: 'crm', implicit: true },
        { appId: 'app-2', app: 'fatture', implicit: true },
      ],
    },
    {
      id: 'u-admin',
      email: 'admin@test',
      displayName: 'Admin',
      role: 'member',
      status: 'active',
      joinedAt: '2025-03-04T00:00:00Z',
      apps: [{ appId: 'app-1', app: 'crm', role: 'admin', implicit: false }],
    },
    {
      id: 'u-member',
      email: 'member@test',
      displayName: 'Member',
      role: 'member',
      status: 'active',
      joinedAt: '2025-06-07T00:00:00Z',
      apps: [],
    },
  ]
  invites = [
    { id: 'inv-old', email: 'pending@test', status: 'pending', expiresAt: '2026-07-01T00:00:00Z' },
  ]
  sentEmails = []
  seats = franchigia()
  useAuthStore.getState().setSession({ accessToken: fakeAccessToken({ sub: 'u-owner', roles: ['owner'] }) })
})

/**
 * Attende che il costo del posto sia noto. Da UC 0103 il pulsante di invito resta **spento** finché non lo
 * è: mai invitare alla cieca. Ogni collaudo che invita deve quindi aspettare la stima — non è un dettaglio
 * di sincronizzazione, è la regola della storia vista dal collaudo.
 */
async function attendiIlCostoDelPosto() {
  await screen.findByText(/will be seat number/)
}

describe('MembersPage — elenco unico di persone (UC 0100)', () => {
  /**
   * Il cuore della storia: **una** tabella. Prima erano due — le persone e gli inviti in attesa — e
   * chi guardava doveva sommare a mente. Il collaudo pretende una sola tabella e che l'invito in
   * attesa stia dentro, con il suo stato.
   */
  it('è una sola tabella, e gli inviti in attesa stanno dentro', async () => {
    renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')

    expect(screen.getAllByRole('table')).toHaveLength(1)
    const table = screen.getByRole('table')
    for (const email of ['owner@test', 'admin@test', 'member@test', 'pending@test']) {
      expect(within(table).getByText(email)).toBeInTheDocument()
    }
    // La riga dell'invito si riconosce dallo stato, non da una tabella separata.
    const pendingRow = within(table).getByText('pending@test').closest('tr') as HTMLElement
    expect(within(pendingRow).getByText('Invitation pending')).toBeInTheDocument()
    expect(within(pendingRow).getByText(/expires/)).toBeInTheDocument()
  })

  /**
   * **Nessuna colonna di ruolo.** Il collaudo pretende l'assenza, non la presenza delle colonne
   * nuove: cancellare la colonna senza un collaudo che ne vieti il ritorno l'avrebbe fatta tornare
   * alla prima revisione, senza che nulla diventasse rosso.
   */
  it('non ha una colonna di ruolo, e nessuna etichetta di ruolo di piattaforma', async () => {
    renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')

    const headers = screen.getAllByRole('columnheader').map((h) => h.textContent)
    // La prima colonna è quella di SELEZIONE (UC 0104): intestazione non visibile ma con un nome per
    // chi naviga con lettore di schermo — un'intestazione vuota sarebbe un'omissione.
    expect(headers).toEqual([
      'Schedule termination',
      'Email',
      'Name',
      'Status',
      'Apps',
      'Joined',
      'Actions',
    ])
    expect(screen.queryByRole('columnheader', { name: 'Role' })).not.toBeInTheDocument()
    // Le due etichette del ruolo di piattaforma non compaiono da nessuna parte della pagina.
    const ownerRow = screen.getByText('owner@test').closest('tr') as HTMLElement
    expect(within(ownerRow).queryByText('Owner', { selector: 'span' })).not.toBeInTheDocument()
    const adminRow = screen.getByText('admin@test').closest('tr') as HTMLElement
    expect(within(adminRow).queryByText('Member')).not.toBeInTheDocument()
    // E non esiste nessun selettore: non c'è nulla da scegliere.
    expect(screen.queryAllByRole('combobox')).toHaveLength(0)
  })

  it('conta le applicazioni di ciascuno e apre il dettaglio in sola lettura', async () => {
    const user = userEvent.setup()
    renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')

    const ownerRow = screen.getByText('owner@test').closest('tr') as HTMLElement
    expect(within(ownerRow).getByRole('button', { name: 'Apps for owner@test' })).toHaveTextContent('2 apps')
    const adminRow = screen.getByText('admin@test').closest('tr') as HTMLElement
    expect(within(adminRow).getByRole('button', { name: 'Apps for admin@test' })).toHaveTextContent('1 app')
    // Chi non è abilitato a nulla: stato legittimo, e si dice come si abilita.
    const memberRow = screen.getByText('member@test').closest('tr') as HTMLElement
    expect(within(memberRow).getByRole('button', { name: 'Apps for member@test' })).toHaveTextContent('No apps')

    await user.click(within(adminRow).getByRole('button', { name: 'Apps for admin@test' }))
    // Il ruolo si legge NEL dettaglio, accanto al nome dell'applicazione: è il ruolo su QUELLA
    // applicazione, non un ruolo della persona.
    const detail = (await screen.findByText('crm')).closest('li') as HTMLElement
    expect(within(detail).getByText('Admin')).toBeInTheDocument()
    // Il dettaglio dice dove si cambia il ruolo: non c'è un comando qui, ed è voluto (UC 0111).
    expect(
      screen.getByText('Roles on an app are changed from that app’s user management.'),
    ).toBeInTheDocument()

    await user.click(within(memberRow).getByRole('button', { name: 'Apps for member@test' }))
    expect(
      await screen.findByText('Not enabled on any app yet — you enable it from that app’s user management.'),
    ).toBeInTheDocument()
  })

  it('l’owner è in testa all’elenco', async () => {
    renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')
    // La prima cella è quella di selezione (UC 0104): l'indirizzo sta nella seconda.
    const cells = screen.getAllByRole('row')[1].querySelectorAll('td')
    expect(cells[1]).toHaveTextContent('owner@test')
  })

  it('l’invito chiede solo l’indirizzo, e spiega perché non chiede il ruolo', async () => {
    const user = userEvent.setup()
    renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')

    expect(
      screen.getByText(
        'There is no role to choose: whoever joins is part of the workspace, and permissions are granted inside each app.',
      ),
    ).toBeInTheDocument()

    await attendiIlCostoDelPosto()
    await user.type(screen.getByLabelText('Email'), 'new@acme.test')
    await user.click(screen.getByRole('button', { name: 'Send invitation' }))

    expect(await screen.findByText('Invitation sent to new@acme.test.')).toBeInTheDocument()
    expect(sentEmails).toHaveLength(1)
    // Nessun ruolo verso il servizio delle email: non c'è più un ruolo da annunciare.
    expect(sentEmails[0].body.role).toBeUndefined()
    expect(await screen.findByText('new@acme.test')).toBeInTheDocument()
  })

  it('dice che il posto è dell’account, non della persona (UC 0118)', async () => {
    // La regola va scritta dove si invita: la prima reazione di chi invita qualcuno che ha già un
    // account altrove è «ma la paga già l'altra azienda».
    renderWithProviders(<MembersPage />)
    expect(
      await screen.findByText(
        'The seat belongs to this account: it is paid here even if the person already works in another account.',
      ),
    ).toBeInTheDocument()
  })

  it('le due collisioni lecite dell’invito hanno due messaggi distinti (UC 0118)', async () => {
    const user = userEvent.setup()
    renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')

    await attendiIlCostoDelPosto()
    await user.type(screen.getByLabelText('Email'), 'gia-membro@x.io')
    await user.click(screen.getByRole('button', { name: 'Send invitation' }))
    expect(
      await screen.findByText('This person is already a member of this account.'),
    ).toBeInTheDocument()

    await user.clear(screen.getByLabelText('Email'))
    await user.type(screen.getByLabelText('Email'), 'gia-invitato@x.io')
    await user.click(screen.getByRole('button', { name: 'Send invitation' }))
    expect(
      await screen.findByText('There is already a pending invitation for this address.'),
    ).toBeInTheDocument()
  })

  it('revoca un invito in attesa dopo conferma (DELETE)', async () => {
    const user = userEvent.setup()
    renderWithProviders(<MembersPage />)
    await screen.findByText('pending@test')

    await user.click(screen.getByRole('button', { name: 'Revoke' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Revoke' }))

    await vi.waitFor(() => expect(screen.queryByText('pending@test')).not.toBeInTheDocument())
  })

  it('protezioni UX: azioni distruttive disabilitate su se stessi / ultimo owner', async () => {
    renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')

    const ownerRow = screen.getByText('owner@test').closest('tr') as HTMLElement
    expect(within(ownerRow).getByRole('button', { name: 'Remove' })).toBeDisabled()
    expect(within(ownerRow).getByRole('button', { name: 'Suspend' })).toBeDisabled()

    const adminRow = screen.getByText('admin@test').closest('tr') as HTMLElement
    expect(within(adminRow).getByRole('button', { name: 'Remove' })).toBeEnabled()

    // Una riga di invito non ha né sospensione né rimozione: non c'è ancora nessuno da sospendere.
    const pendingRow = screen.getByText('pending@test').closest('tr') as HTMLElement
    expect(within(pendingRow).queryByRole('button', { name: 'Suspend' })).not.toBeInTheDocument()
    expect(within(pendingRow).getByRole('button', { name: 'Revoke' })).toBeEnabled()
  })

  it('non ha violazioni di accessibilità', async () => {
    const { container } = renderWithProviders(<MembersPage />)
    await screen.findByText('owner@test')
    expect(await axe(container)).toHaveNoViolations()
  })
})

/**
 * Il riquadro dei posti (UC 0103). Quello che si prova qui non è la grafica: sono le **frasi** che il
 * cliente legge quando sta per spendere dei soldi, e il presidio che gli impedisce di spenderli alla cieca.
 */
describe('MembersPage — riquadro dei posti (UC 0103)', () => {
  it('entro la franchigia dice che non si paga nulla, e quanto costerà il prossimo', async () => {
    renderWithProviders(<MembersPage />)

    expect(await screen.findByText('3 seats in use')).toBeInTheDocument()
    expect(screen.getByText('3 active · 0 suspended · 0 pending invitations')).toBeInTheDocument()
    // «Compresi», non «€0,00»: non sono la stessa promessa.
    expect(screen.getByText('You are paying nothing: the first 3 seats are included.')).toBeInTheDocument()
    expect(screen.queryByText(/You are paying €/)).not.toBeInTheDocument()
    expect(
      screen.getByText('The next seat costs €2.99: your total will become €2.99.'),
    ).toBeInTheDocument()
  })

  it('mostra la stima PRIMA della conferma, con il numero del posto e il nuovo totale', async () => {
    renderWithProviders(<MembersPage />)
    expect(
      await screen.findByText(
        'This person will be seat number 4: it costs €2.99 per month, and your total will go from €0.00 to €2.99.',
      ),
    ).toBeInTheDocument()
  })

  it('oltre la franchigia dice quanto si paga, con la fascia e i posti a pagamento', async () => {
    seats = {
      ...franchigia(),
      usedSeats: 5,
      composition: { active: 4, suspended: 0, pendingInvitations: 1 },
      paidSeats: 2,
      dueCents: 598,
      paidQuantity: 2,
      currentBand: { fromSeat: 4, toSeat: 10, unitPriceCents: 299 },
      next: {
        seatNumber: 6,
        unitPriceCents: 299,
        dueCentsAfter: 897,
        chargeCents: 299,
        cheaperThanPrevious: false,
      },
      hasSubscription: true,
    }
    renderWithProviders(<MembersPage />)

    expect(await screen.findByText('You are paying €5.98 per month')).toBeInTheDocument()
    expect(screen.getByText('2 paid seats')).toBeInTheDocument()
    expect(screen.getByText('seats 4–10: €2.99 each')).toBeInTheDocument()
  })

  /**
   * **Il caso che va detto per esteso.** Al confine di fascia il posto successivo costa meno del
   * precedente, ma il totale sale comunque: col listino progressivo sale sempre. Una frase che dicesse
   * solo «costa meno» accanto a un totale più alto sembrerebbe un errore di conteggio.
   */
  it('quando il posto successivo costa meno lo dice per esteso, totale compreso', async () => {
    seats = {
      ...franchigia(),
      usedSeats: 10,
      composition: { active: 10, suspended: 0, pendingInvitations: 0 },
      paidSeats: 7,
      dueCents: 2093,
      paidQuantity: 7,
      currentBand: { fromSeat: 4, toSeat: 10, unitPriceCents: 299 },
      next: {
        seatNumber: 11,
        unitPriceCents: 199,
        dueCentsAfter: 2292,
        chargeCents: 199,
        cheaperThanPrevious: true,
      },
      hasSubscription: true,
    }
    renderWithProviders(<MembersPage />)

    expect(
      await screen.findByText(
        'The next seat costs €1.99 instead of €2.99, because you enter the next tier. Your total goes from €20.93 to €22.92.',
      ),
    ).toBeInTheDocument()
  })

  it('se il posto era già pagato in questo periodo lo dice, e non promette un addebito', async () => {
    seats = {
      ...franchigia(),
      usedSeats: 3,
      paidSeats: 0,
      paidQuantity: 1,
      next: {
        seatNumber: 4,
        unitPriceCents: 299,
        dueCentsAfter: 299,
        chargeCents: 0,
        cheaperThanPrevious: false,
      },
      hasSubscription: true,
    }
    renderWithProviders(<MembersPage />)

    expect(
      await screen.findByText(
        'Seat number 4 is already paid for this period: this invitation triggers no new charge.',
      ),
    ).toBeInTheDocument()
  })

  /**
   * **Mai invitare alla cieca.** Se il costo non è noto — lettura in errore — l'invito è impedito e la
   * pagina dice perché. È il presidio contro la sorpresa in fattura, e vale anche quando il guasto è
   * nostro: preferiamo far aspettare che addebitare una cifra che nessuno ha visto.
   */
  it('se il costo del posto non è noto, l’invito è impedito e si può riprovare', async () => {
    seats = null
    const user = userEvent.setup()
    renderWithProviders(<MembersPage />)

    // Il client ritenta una volta prima di dichiarare l'errore: l'attesa è più lunga del solito, e va
    // dichiarata invece di sperare che il valore predefinito basti.
    expect(
      await screen.findByText(
        'We cannot read the cost of your seats. Until we know it you cannot invite anyone: we would rather make you wait than charge you an unexpected amount.',
        {},
        { timeout: 5000 },
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Send invitation' })).toBeDisabled()
    // Nessuna stima: non c'è nulla da stimare, e inventare uno zero sarebbe il difetto.
    expect(screen.queryByText(/will be seat number/)).not.toBeInTheDocument()
    // E si può riprovare: il riquadro offre il comando, non solo il messaggio.
    expect(within(screen.getByRole('alert')).getByRole('button', { name: 'Retry' })).toBeEnabled()
    await user.click(within(screen.getByRole('alert')).getByRole('button', { name: 'Retry' }))
  })

  /**
   * **L'addebito rifiutato non crea l'invito**, e il motivo del fornitore arriva a chi ha invitato: è
   * l'unica informazione con cui può rimediare.
   */
  it('mostra il motivo del fornitore quando l’addebito del posto è rifiutato', async () => {
    server.use(
      http.post('http://localhost/api/platform/v1/invitations', () =>
        HttpResponse.json(
          {
            type: 'urn:appgrove:seats:charge-declined',
            title: 'Payment Required',
            status: 402,
            detail: 'carta scaduta',
          },
          { status: 402 },
        ),
      ),
    )
    const user = userEvent.setup()
    renderWithProviders(<MembersPage />)
    await attendiIlCostoDelPosto()

    await user.type(screen.getByLabelText('Email'), 'quattro@acme.test')
    await user.click(screen.getByRole('button', { name: 'Send invitation' }))

    expect(
      await screen.findByText(
        'The payment for the seat did not go through (carta scaduta): the invitation was not created. Check your payment method in Billing and try again.',
      ),
    ).toBeInTheDocument()
    // L'invito non è nella tabella: non è stato creato.
    expect(screen.queryByText('quattro@acme.test')).not.toBeInTheDocument()
  })

  /**
   * **La riduzione in attesa vista dallo schermo** (UC 0104 §6): l'avviso dice quante persone e quando,
   * quanto si pagherà da allora, chi è indicato — e offre le due vie d'uscita. Il comando di invito è
   * spento, non nascosto: la funzione esiste, è temporaneamente non disponibile.
   */
  it('avverte quando c’è una riduzione in attesa, e impedisce l’invito (UC 0104)', async () => {
    seats = riduzioneInAttesa()
    members = indicata(members)
    renderWithProviders(<MembersPage />)

    expect(await screen.findByText('Scheduled reduction')).toBeInTheDocument()
    expect(
      screen.getByText('1 person will leave on 9/14/2026. Until then you cannot add people.'),
    ).toBeInTheDocument()
    // L'importo che si pagherà da allora, con il conto che lo giustifica.
    expect(
      screen.getByText('From 9/14/2026 you will pay €0.00 instead of €2.99, for 3 seats.'),
    ).toBeInTheDocument()
    expect(screen.getByText('3 seats included')).toBeInTheDocument()
    // Chi è indicato, con il comando che lo MANTIENE (non che lo rimuove: l'atto salva qualcuno).
    // L'indirizzo compare due volte — nell'avviso e nella riga della tabella — ed è giusto così: chi
    // legge l'avviso non deve andare a cercare nella tabella di chi si sta parlando.
    expect(screen.getAllByText('member@test')).toHaveLength(2)
    expect(screen.getByRole('button', { name: 'Keep member@test in the account' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Cancel the reduction' })).toBeEnabled()
    // Il comando di invito è SPENTO con spiegazione, non nascosto.
    expect(screen.getByRole('button', { name: 'Send invitation' })).toBeDisabled()
    // E lo stato «in cessazione dal …» compare nell'elenco, con la data: senza il quando non dice nulla.
    const row = within(screen.getByRole('table')).getByText('member@test').closest('tr') as HTMLElement
    expect(within(row).getByText('Ending 9/14/2026')).toBeInTheDocument()
  })

  /**
   * **Annullare l'attesa** riapre gli inviti. Il collaudo pretende la conseguenza osservabile — il
   * pulsante che si riaccende — e non la sola chiamata: una chiamata riuscita che non cambia lo schermo
   * è indistinguibile da nulla, per chi guarda.
   */
  it('annullando la riduzione l’invito torna possibile (UC 0104)', async () => {
    const user = userEvent.setup()
    seats = riduzioneInAttesa()
    members = indicata(members)
    server.use(
      http.delete('http://localhost/api/platform/v1/me/seats/reduction', () => {
        seats = franchigia()
        members = members.map((m) => ({ ...m, endingAt: undefined }))
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderWithProviders(<MembersPage />)

    await user.click(await screen.findByRole('button', { name: 'Cancel the reduction' }))
    // Conferma esplicita, come chiede la storia: non si annulla per errore.
    await user.click(
      within(await screen.findByRole('dialog')).getByRole('button', {
        name: 'Cancel the reduction',
      }),
    )

    await attendiIlCostoDelPosto()
    expect(screen.getByRole('button', { name: 'Send invitation' })).toBeEnabled()
    expect(screen.queryByText('Scheduled reduction')).not.toBeInTheDocument()
  })

  /**
   * **Indicare una persona**: si selezionano le caselle, si vede l'effetto PRIMA di confermare, e solo
   * dopo si conferma. Le righe non indicabili — l'owner e gli inviti in attesa — non hanno casella.
   */
  it('indica una persona per la cessazione mostrando l’effetto prima della conferma (UC 0104)', async () => {
    const user = userEvent.setup()
    const richieste: unknown[] = []
    server.use(
      http.get('http://localhost/api/platform/v1/me/seats/reduction/preview', () =>
        HttpResponse.json(anteprima()),
      ),
      http.post('http://localhost/api/platform/v1/me/seats/reduction', async ({ request }) => {
        richieste.push(await request.json())
        seats = riduzioneInAttesa()
        return HttpResponse.json({}, { status: 201 })
      }),
    )
    renderWithProviders(<MembersPage />)
    await attendiIlCostoDelPosto()

    // L'owner non è indicabile e un invito in attesa non si «cessa»: nessuna casella su quelle righe.
    expect(screen.queryByRole('checkbox', { name: 'Select owner@test' })).not.toBeInTheDocument()
    expect(screen.queryByRole('checkbox', { name: 'Select pending@test' })).not.toBeInTheDocument()

    await user.click(screen.getByRole('checkbox', { name: 'Select member@test' }))
    expect(screen.getByText('1 person selected')).toBeInTheDocument()
    // L'effetto, PRIMA di aprire la conferma.
    expect(
      await screen.findByText(
        'They will leave on 9/14/2026 and keep working normally until then. From 9/14/2026 you will pay €0.00 instead of €2.99.',
      ),
    ).toBeInTheDocument()
    // E il suggerimento per chi deve escludere qualcuno SUBITO, che è la domanda vera dell'owner.
    expect(screen.getByText(/Revoke their app access/)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Schedule termination' }))
    await user.click(
      within(await screen.findByRole('dialog')).getByRole('button', {
        name: 'Schedule termination',
      }),
    )

    expect(richieste).toEqual([{ userIds: ['u-member'] }])
  })

  /**
   * Il rifiuto «non stai pagando alcun posto» non è un vicolo cieco: dice che cosa fare invece, cioè
   * rimuovere la persona subito — immediato e gratuito.
   */
  it('spiega il rifiuto quando non c’è nulla da ridurre (UC 0104)', async () => {
    const user = userEvent.setup()
    server.use(
      http.get('http://localhost/api/platform/v1/me/seats/reduction/preview', () =>
        HttpResponse.json(anteprima()),
      ),
      http.post('http://localhost/api/platform/v1/me/seats/reduction', () =>
        HttpResponse.json(
          { type: 'urn:appgrove:seats:reduction-not-needed', title: 'Conflict', status: 409 },
          { status: 409 },
        ),
      ),
    )
    renderWithProviders(<MembersPage />)
    await attendiIlCostoDelPosto()

    await user.click(screen.getByRole('checkbox', { name: 'Select member@test' }))
    await user.click(await screen.findByRole('button', { name: 'Schedule termination' }))
    await user.click(
      within(await screen.findByRole('dialog')).getByRole('button', {
        name: 'Schedule termination',
      }),
    )

    expect(await screen.findByText(/there is nothing to reduce/)).toBeInTheDocument()
  })
})
