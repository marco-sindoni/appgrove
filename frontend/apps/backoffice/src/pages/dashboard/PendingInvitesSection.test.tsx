import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { PendingInvitesSection } from './PendingInvitesSection'
import { useAuthStore } from '../../auth/authStore'
import { renderWithProviders, fakeAccessToken } from '../../test/utils'

let invitations: Array<Record<string, unknown>>
let accepted: string[]
let rejected: string[]

const server = setupServer(
  http.get('http://localhost/api/platform/v1/me/invitations', () => HttpResponse.json({ invitations })),
  http.post('http://localhost/api/platform/v1/me/invitations/:id/accept', ({ params }) => {
    accepted.push(String(params.id))
    return new HttpResponse(null, { status: 204 })
  }),
  http.post('http://localhost/api/platform/v1/me/invitations/:id/reject', ({ params }) => {
    rejected.push(String(params.id))
    invitations = invitations.filter((i) => i.id !== params.id)
    return new HttpResponse(null, { status: 204 })
  }),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
beforeEach(() => {
  invitations = [{ id: 'inv-1', accountId: 'tenant-b', accountName: 'Beta Srl' }]
  accepted = []
  rejected = []
  useAuthStore.getState().setSession({ accessToken: fakeAccessToken(), idToken: null })
})

describe('PendingInvitesSection (UC 0118)', () => {
  it('mostra chi invita e dice chi paga il posto', async () => {
    // Il nome dell'azienda non è un ornamento: senza di esso il consenso è alla cieca. E la riga sul
    // posto risparmia la domanda «ma lo pago io?».
    renderWithProviders(<PendingInvitesSection />)
    expect(await screen.findByText('Beta Srl invites you to work in their account.')).toBeInTheDocument()
    expect(
      screen.getByText('The seat is paid by the account that invites you, not by you.'),
    ).toBeInTheDocument()
  })

  it('senza inviti non rende nulla — nessun titolo, nessuno spazio occupato', async () => {
    invitations = []
    const { container } = renderWithProviders(<PendingInvitesSection />)
    // Attesa della lettura: prima che risponda la sezione è comunque vuota.
    await new Promise((r) => setTimeout(r, 0))
    expect(screen.queryByText('Invitations for you')).not.toBeInTheDocument()
    expect(container.querySelector('section')).toBeNull()
  })

  it('accettare chiama l’accettazione e RICARICA l’applicazione', async () => {
    // Il ricaricamento è parte del comportamento (UC 0117/0118): l'account accettato è ora quello
    // attivo, ed è il ricaricamento a far nascere il token con il claim nuovo. Mezza applicazione con
    // l'account nuovo e mezza col vecchio è il modo peggiore di sbagliare.
    const assign = vi.fn()
    vi.spyOn(window, 'location', 'get').mockReturnValue({ assign } as unknown as Location)
    const user = userEvent.setup()
    renderWithProviders(<PendingInvitesSection />)
    await user.click(await screen.findByRole('button', { name: 'Accept' }))
    await vi.waitFor(() => expect(accepted).toEqual(['inv-1']))
    expect(assign).toHaveBeenCalledWith('/')
    vi.restoreAllMocks()
  })

  it('rifiutare chiude l’invito e la voce sparisce', async () => {
    const user = userEvent.setup()
    renderWithProviders(<PendingInvitesSection />)
    await user.click(await screen.findByRole('button', { name: 'Decline' }))
    await vi.waitFor(() => expect(rejected).toEqual(['inv-1']))
    await vi.waitFor(() =>
      expect(screen.queryByText('Beta Srl invites you to work in their account.')).not.toBeInTheDocument(),
    )
  })
})
