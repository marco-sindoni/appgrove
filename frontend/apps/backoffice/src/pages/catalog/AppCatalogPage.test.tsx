import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { AppCatalogPage } from './AppCatalogPage'
import { useAuthStore } from '../../auth/authStore'
import { renderWithProviders, fakeAccessToken } from '../../test/utils'

const CATALOG_URL = 'http://localhost/api/platform/v1/me/catalog'

let apps: Array<Record<string, unknown>>
let failCatalog = false

const card = (over: Record<string, unknown> = {}) => ({
  appSlug: 'notes',
  name: 'Notes',
  category: 'amber',
  descriptions: { en: 'Shared notes and lightweight docs', it: 'Note condivise' },
  state: 'available',
  startingPrice: { amount: 900, currency: 'EUR', billingCycle: 'monthly' },
  ...over,
})

const server = setupServer(
  http.get(CATALOG_URL, () => {
    if (failCatalog) return HttpResponse.json({ title: 'boom' }, { status: 500 })
    return HttpResponse.json({ apps })
  }),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  failCatalog = false
  apps = [card()]
  useAuthStore.getState().setSession({ accessToken: fakeAccessToken({ roles: ['owner'] }) })
})

/** La card di una app, individuata dall'etichetta accessibile (il nome commerciale). */
const cardOf = (name: string) => screen.getByRole('article', { name }) as HTMLElement

describe('Pagina Catalogo app (UC 0095)', () => {
  it('mostra le app del catalogo del backend con descrizione, prezzo e azione', async () => {
    renderWithProviders(<AppCatalogPage />)
    expect(await screen.findByText('Shared notes and lightweight docs')).toBeInTheDocument()
    const notes = cardOf('Notes')
    expect(within(notes).getByRole('button', { name: 'Subscribe' })).toBeEnabled()
    expect(within(notes).getByText(/from/i)).toHaveTextContent('9')
  })

  it('un’app con sole fasce gratuite dice "Free", non "€0"', async () => {
    apps = [card({ startingPrice: undefined })]
    renderWithProviders(<AppCatalogPage />)
    expect(await screen.findByText('Free')).toBeInTheDocument()
  })

  it('rende i sei stati con l’azione giusta', async () => {
    apps = [
      card({ appSlug: 'a1', name: 'Uno', state: 'available' }),
      card({ appSlug: 'a2', name: 'Due', state: 'active', planName: 'Due Pro' }),
      card({ appSlug: 'a3', name: 'Tre', state: 'trial', trialEndsAt: '2030-08-10T00:00:00Z' }),
      card({ appSlug: 'a4', name: 'Quattro', state: 'payment_pending' }),
      card({
        appSlug: 'a5',
        name: 'Cinque',
        state: 'cancellation_scheduled',
        cancelAt: '2030-08-31T00:00:00Z',
      }),
      card({ appSlug: 'a6', name: 'Sei', state: 'disabled_by_platform' }),
    ]
    renderWithProviders(<AppCatalogPage />)
    await screen.findByText('Uno')

    expect(within(cardOf('Uno')).getByRole('button', { name: 'Subscribe' })).toBeInTheDocument()
    expect(within(cardOf('Due')).getByRole('button', { name: 'Open' })).toBeInTheDocument()
    expect(within(cardOf('Due')).getByText('Due Pro')).toBeInTheDocument()
    expect(within(cardOf('Tre')).getByText(/Trial ends/)).toBeInTheDocument()
    expect(within(cardOf('Quattro')).getByRole('button', { name: 'Fix payment' })).toBeInTheDocument()
    expect(
      within(cardOf('Cinque')).getByRole('button', { name: 'Undo cancellation' }),
    ).toBeInTheDocument()
    // App spenta dalla piattaforma: nessuna azione d'acquisto, mai.
    const sei = cardOf('Sei')
    expect(within(sei).getByRole('button', { name: 'Contact support' })).toBeInTheDocument()
    expect(within(sei).queryByRole('button', { name: 'Subscribe' })).not.toBeInTheDocument()
    expect(within(sei).getByText('Disabled by platform')).toBeInTheDocument()
  })

  it('un member vede prezzi e stati ma non può attivare, e legge il perché', async () => {
    useAuthStore.getState().setSession({ accessToken: fakeAccessToken({ roles: ['member'] }) })
    renderWithProviders(<AppCatalogPage />)
    await screen.findByText('Notes')
    expect(screen.getByRole('button', { name: 'Subscribe' })).toBeDisabled()
    expect(screen.getByText('Ask an owner to activate it')).toBeInTheDocument()
  })

  it('la ricerca filtra, conta i risultati e ha uno stato vuoto dedicato', async () => {
    apps = [card({ appSlug: 'notes', name: 'Notes' }), card({ appSlug: 'teams', name: 'Teams' })]
    renderWithProviders(<AppCatalogPage />)
    await screen.findByText('Notes')
    expect(screen.getByRole('status')).toHaveTextContent('2 apps')

    await userEvent.type(screen.getByRole('searchbox', { name: 'Search apps' }), 'teams')
    expect(screen.getByRole('status')).toHaveTextContent('1 app')
    expect(screen.queryByText('Notes')).not.toBeInTheDocument()

    await userEvent.clear(screen.getByRole('searchbox', { name: 'Search apps' }))
    await userEvent.type(screen.getByRole('searchbox', { name: 'Search apps' }), 'zzz')
    expect(screen.getByText('No apps match your search')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Clear search' }))
    expect(await screen.findByText('Notes')).toBeInTheDocument()
  })

  it('pagina l’elenco e la ricerca riporta alla prima pagina', async () => {
    apps = Array.from({ length: 8 }, (_, i) =>
      card({ appSlug: `a${i}`, name: `App ${i}`, descriptions: { en: `desc ${i}` } }),
    )
    renderWithProviders(<AppCatalogPage />)
    await screen.findByText('App 0')
    expect(screen.getByText('Page 1 of 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()
    expect(screen.queryByText('App 7')).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Next' }))
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument()
    expect(screen.getByText('App 7')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()

    // Una ricerca mentre si è a pagina due deve riportare in cima ai risultati, non nel vuoto.
    await userEvent.type(screen.getByRole('searchbox', { name: 'Search apps' }), 'App')
    expect(screen.getByText('Page 1 of 2')).toBeInTheDocument()
    expect(screen.getByText('App 0')).toBeInTheDocument()
  })

  it('un guasto di lettura è un errore con riprova, mai "nessuna app"', async () => {
    failCatalog = true
    renderWithProviders(<AppCatalogPage />)
    // Attesa generosa: la shell ritenta una volta prima di dichiarare il guasto (api/queryClient.ts),
    // e ciò che conta qui è dove si arriva — errore con riprova, non un catalogo vuoto.
    expect(await screen.findByRole('alert', {}, { timeout: 5_000 })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument()
    expect(screen.queryByText('No apps match your search')).not.toBeInTheDocument()
  })

  it('l’acquisto parte dal catalogo: Subscribe apre la scelta della fascia', async () => {
    server.use(
      http.get('http://localhost/api/platform/v1/checkout/apps/notes/tiers', () =>
        HttpResponse.json({
          appId: 'x',
          slug: 'notes',
          name: 'Notes',
          tiers: [
            {
              tierId: 't1',
              key: 'pro',
              name: 'Notes Pro',
              trialDays: 0,
              prices: [{ billingCycle: 'annual', amount: 9000, currency: 'EUR' }],
            },
          ],
        }),
      ),
    )
    renderWithProviders(<AppCatalogPage />)
    await screen.findByText('Notes')
    await userEvent.click(screen.getByRole('button', { name: 'Subscribe' }))
    expect(await screen.findByText('Notes Pro')).toBeInTheDocument()
  })
})
