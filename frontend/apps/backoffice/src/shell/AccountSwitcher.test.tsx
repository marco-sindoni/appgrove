import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { AccountSwitcher } from './AccountSwitcher'
import { AccountChangedBanner } from './AccountChangedBanner'
import { useAuthStore } from '../auth/authStore'
import { renderWithProviders, fakeAccessToken } from '../test/utils'

const BASE = 'http://localhost'
const MEMBERSHIPS = `${BASE}/api/platform/v1/me/memberships`
const ACTIVE = `${BASE}/api/platform/v1/me/active-account`

const ACME = { accountId: 'tenant-acme', accountName: 'Acme Corp' }
const BETA = { accountId: 'tenant-beta', accountName: 'Beta Srl' }

const server = setupServer()

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  useAuthStore.getState().setSession({ accessToken: fakeAccessToken({ tenant_id: 'tenant-acme' }) })
})

function withMemberships(activeAccountId: string | null, memberships: typeof ACME[]) {
  server.use(http.get(MEMBERSHIPS, () => HttpResponse.json({ activeAccountId, memberships })))
}

describe('Selettore dell’account attivo (UC 0117)', () => {
  it('con UNA sola appartenenza mostra il nome e NON rende il selettore', async () => {
    // Il caso del cento per cento degli utenti di oggi: un comando che non serve a nulla è rumore,
    // e «reso disabilitato» non basta — non deve esistere nel documento.
    withMemberships('tenant-acme', [ACME])
    renderWithProviders(<AccountSwitcher />)

    expect(await screen.findByTestId('active-account-name')).toHaveTextContent('Acme Corp')
    expect(screen.queryByLabelText('Switch account')).not.toBeInTheDocument()
    expect(screen.queryByText('Beta Srl')).not.toBeInTheDocument()
  })

  it('con DUE appartenenze mostra il nome attivo, il comando e l’elenco', async () => {
    withMemberships('tenant-acme', [ACME, BETA])
    renderWithProviders(<AccountSwitcher />)

    expect(await screen.findByTestId('active-account-name')).toHaveTextContent('Acme Corp')
    expect(screen.getByLabelText('Switch account')).toBeInTheDocument()
    expect(screen.getByText('2 accounts · switch')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Beta Srl/ })).toBeInTheDocument()
    // L'account su cui si sta lavorando è marcato: è l'informazione di sicurezza percepita.
    expect(screen.getByRole('button', { name: /Acme Corp/ })).toHaveAttribute('aria-current', 'true')
  })

  it('il cambio scrive l’account attivo e RICARICA l’applicazione', async () => {
    // Non si aggiorna lo stato in memoria: mezza applicazione con l'account nuovo e mezza col
    // vecchio è il modo peggiore di sbagliare.
    const assign = vi.fn()
    vi.stubGlobal('location', { ...window.location, assign })
    let body: unknown = null
    withMemberships('tenant-acme', [ACME, BETA])
    server.use(
      http.post(ACTIVE, async ({ request }) => {
        body = await request.json()
        return new HttpResponse(null, { status: 204 })
      }),
    )

    renderWithProviders(<AccountSwitcher />)
    await screen.findByTestId('active-account-name')
    await userEvent.click(screen.getByRole('button', { name: /Beta Srl/ }))

    expect(body).toEqual({ accountId: 'tenant-beta' })
    expect(assign).toHaveBeenCalledWith('/')
    vi.unstubAllGlobals()
  })

  it('un cambio verso un’appartenenza non più valida lo dice, senza ricaricare', async () => {
    // L'appartenenza revocata mentre il menu era aperto: il server risponde 404 (non 403, che
    // rivelerebbe l'esistenza dell'account) e l'interfaccia non finge che sia andata bene.
    const assign = vi.fn()
    vi.stubGlobal('location', { ...window.location, assign })
    withMemberships('tenant-acme', [ACME, BETA])
    server.use(http.post(ACTIVE, () => new HttpResponse(null, { status: 404 })))

    renderWithProviders(<AccountSwitcher />)
    await screen.findByTestId('active-account-name')
    await userEvent.click(screen.getByRole('button', { name: /Beta Srl/ }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Couldn’t switch account')
    expect(assign).not.toHaveBeenCalled()
    vi.unstubAllGlobals()
  })

  it('senza account attivo determinato non inventa un nome', async () => {
    // Più appartenenze e nessuna scelta valida: è lo stesso caso in cui il token nasce senza claim.
    withMemberships(null, [ACME, BETA])
    const { container } = renderWithProviders(<AccountSwitcher />)
    expect(screen.queryByTestId('active-account-name')).not.toBeInTheDocument()
    expect(container).not.toHaveTextContent('Acme Corp')
  })
})

describe('Avviso «account cambiato in un’altra scheda» (UC 0117)', () => {
  it('compare quando il token in uso punta a un account diverso da quello attivo', async () => {
    withMemberships('tenant-beta', [ACME, BETA])
    renderWithProviders(<AccountChangedBanner />)
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'The active account changed in another tab.',
    )
  })

  it('non compare quando il token è allineato all’account attivo', async () => {
    withMemberships('tenant-acme', [ACME, BETA])
    // Il selettore accanto serve da testimone: quando mostra il nome, la lettura è arrivata — così
    // l'assenza dell'avviso è un'assenza vera e non un «non è ancora arrivato».
    renderWithProviders(
      <>
        <AccountChangedBanner />
        <AccountSwitcher />
      </>,
    )
    expect(await screen.findByTestId('active-account-name')).toHaveTextContent('Acme Corp')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
