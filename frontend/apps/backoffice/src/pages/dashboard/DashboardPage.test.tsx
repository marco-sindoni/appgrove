import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen, within } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { DashboardPage } from './DashboardPage'
import { useAuthStore } from '../../auth/authStore'
import { renderWithProviders, fakeAccessToken, fakeIdToken } from '../../test/utils'

const BASE = 'http://localhost'
const CATALOG_URL = `${BASE}/api/platform/v1/me/catalog`
const SUBSCRIPTIONS_URL = `${BASE}/api/platform/v1/me/subscriptions`
const ACCOUNT_URL = `${BASE}/api/platform/v1/accounts/me`
const USERS_URL = `${BASE}/api/platform/v1/users`
const INVITATIONS_URL = `${BASE}/api/platform/v1/invitations`
const TWOFA_URL = `${BASE}/api/auth/2fa/status`
const FATTURE_QUOTA_URL = `${BASE}/api/fatture/v1/quota`
const CRM_QUOTA_URL = `${BASE}/api/crm/v1/quota`

let apps: Array<Record<string, unknown>>
let twoFaEnabled: boolean
let failCatalog = false
let failFattureQuota = false

const server = setupServer(
  http.get(CATALOG_URL, () => {
    if (failCatalog) return HttpResponse.json({ title: 'boom' }, { status: 500 })
    return HttpResponse.json({ apps })
  }),
  http.get(SUBSCRIPTIONS_URL, () =>
    HttpResponse.json({
      subscriptions: [{ appSlug: 'fatture', phase: 'ACTIVE', currentPeriodEnd: '2027-01-31T00:00:00Z' }],
    }),
  ),
  http.get(ACCOUNT_URL, () => HttpResponse.json({ id: 'a1', name: 'Acme', status: 'active' })),
  http.get(USERS_URL, () => HttpResponse.json({ content: [], totalElements: 4 })),
  http.get(INVITATIONS_URL, () => HttpResponse.json({ content: [], totalElements: 1 })),
  http.get(TWOFA_URL, () => HttpResponse.json({ enabled: twoFaEnabled })),
  http.get(FATTURE_QUOTA_URL, () => {
    if (failFattureQuota) return HttpResponse.json({ title: 'boom' }, { status: 500 })
    return HttpResponse.json({ metric: 'invoices', used: 18, limit: 20, remaining: 2 })
  }),
  http.get(CRM_QUOTA_URL, () =>
    HttpResponse.json({ metric: 'seats', used: 1, limit: null, remaining: null }),
  ),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  failCatalog = false
  failFattureQuota = false
  twoFaEnabled = true
  apps = [
    { appSlug: 'fatture', name: 'Invoices', category: 'blue', state: 'active', planName: 'Free' },
    { appSlug: 'crm', name: 'Mini-CRM', category: 'teal', state: 'trial' },
    { appSlug: 'other', name: 'Other', state: 'available' },
  ]
  useAuthStore.getState().setSession({
    accessToken: fakeAccessToken({ roles: ['owner'] }),
    idToken: fakeIdToken({ name: 'Marco' }),
  })
})

describe('Dashboard operativa (UC 0097)', () => {
  it('saluta e nomina il workspace, senza mostrare l’identificativo tecnico', async () => {
    renderWithProviders(<DashboardPage />)
    expect(
      await screen.findByRole('heading', { name: 'Welcome back, Marco', level: 1 }),
    ).toBeInTheDocument()
    expect(await screen.findByText('Here’s what’s happening in the Acme workspace.')).toBeInTheDocument()
    // il codice del workspace è migrato in Account: qui non deve comparire in nessuna forma
    expect(screen.queryByText('tenant-1')).not.toBeInTheDocument()
  })

  it('mostra una card per app in uso, con la barra di consumo e le azioni', async () => {
    renderWithProviders(<DashboardPage />)
    const card = await screen.findByRole('article', { name: 'Invoices' })
    expect(within(card).getByText('Active')).toBeInTheDocument()
    expect(await within(card).findByText('18 of 20 invoices')).toBeInTheDocument()
    const bar = within(card).getByRole('progressbar')
    expect(bar).toHaveAttribute('aria-valuenow', '90')
    expect(within(card).getByRole('button', { name: 'Open' })).toBeInTheDocument()
    expect(within(card).getByRole('button', { name: 'Manage plan' })).toBeInTheDocument()

    // le app soltanto disponibili restano in vetrina, non qui
    expect(screen.queryByRole('article', { name: 'Other' })).not.toBeInTheDocument()
  })

  it('senza tetto mostra il consumo, non una barra che non finisce mai', async () => {
    renderWithProviders(<DashboardPage />)
    const card = await screen.findByRole('article', { name: 'Mini-CRM' })
    expect(await within(card).findByText('1 posti — no limit')).toBeInTheDocument()
    expect(within(card).queryByRole('progressbar')).not.toBeInTheDocument()
  })

  it('il guasto della quota di un’app lascia intatta la card dell’altra', async () => {
    failFattureQuota = true
    renderWithProviders(<DashboardPage />)
    const crm = await screen.findByRole('article', { name: 'Mini-CRM' })
    expect(await within(crm).findByText('1 posti — no limit')).toBeInTheDocument()
    // la card senza consumo leggibile resta utile: niente barra, niente errore di pagina
    const invoices = await screen.findByRole('article', { name: 'Invoices' })
    expect(within(invoices).queryByRole('progressbar')).not.toBeInTheDocument()
    expect(within(invoices).getByRole('button', { name: 'Open' })).toBeInTheDocument()
  })

  it('avvisa del pagamento in sospeso e del secondo fattore mancante, in quest’ordine', async () => {
    twoFaEnabled = false
    apps = [{ appSlug: 'crm', name: 'Mini-CRM', state: 'payment_pending' }]
    renderWithProviders(<DashboardPage />)
    const alerts = await screen.findAllByRole('alert')
    expect(alerts).toHaveLength(2)
    expect(alerts[0]).toHaveTextContent('Mini-CRM has a payment pending')
    expect(within(alerts[0]!).getByRole('button', { name: 'Go to Billing' })).toBeInTheDocument()
    expect(alerts[1]).toHaveTextContent('enable two-factor authentication')
    expect(within(alerts[1]!).getByRole('button', { name: 'Enable 2FA' })).toBeInTheDocument()
  })

  it('con il secondo fattore attivo non lo propone a nessuno', async () => {
    renderWithProviders(<DashboardPage />)
    await screen.findByRole('article', { name: 'Invoices' })
    expect(screen.queryByRole('button', { name: 'Enable 2FA' })).not.toBeInTheDocument()
  })

  it('un workspace senza app in uso vede l’invito al catalogo, non una griglia vuota', async () => {
    apps = [{ appSlug: 'other', name: 'Other', state: 'available' }]
    renderWithProviders(<DashboardPage />)
    expect(await screen.findByText('Your workspace is empty')).toBeInTheDocument()
    expect(screen.queryByRole('article')).not.toBeInTheDocument()
  })

  it('riassume il workspace e offre le scorciatoie', async () => {
    renderWithProviders(<DashboardPage />)
    const glance = await screen.findByRole('complementary', { name: 'Workspace at a glance' })
    expect(await within(glance).findByText('4')).toBeInTheDocument() // membri
    expect(within(glance).getByText('Pending invites')).toBeInTheDocument()
    expect(within(glance).getByText('Active apps')).toBeInTheDocument()
    expect(within(glance).getByRole('button', { name: 'Invite a member' })).toBeInTheDocument()
    expect(within(glance).getByRole('button', { name: 'Payments and receipts' })).toBeInTheDocument()
    expect(within(glance).getByRole('button', { name: 'Browse the catalog' })).toBeInTheDocument()
  })

  it('a un membro non offre le azioni riservate né le righe che non può leggere', async () => {
    useAuthStore.getState().setSession({
      accessToken: fakeAccessToken({ roles: ['member'] }),
      idToken: fakeIdToken({ name: 'Marco' }),
    })
    renderWithProviders(<DashboardPage />)
    const glance = await screen.findByRole('complementary', { name: 'Workspace at a glance' })
    expect(within(glance).queryByText('Members')).not.toBeInTheDocument()
    expect(within(glance).queryByText('Pending invites')).not.toBeInTheDocument()
    expect(within(glance).queryByRole('button', { name: 'Invite a member' })).not.toBeInTheDocument()
    // la panoramica resta: un membro vede le sue app, senza il cambio piano
    const card = await screen.findByRole('article', { name: 'Invoices' })
    expect(within(card).getByRole('button', { name: 'Open' })).toBeInTheDocument()
    expect(within(card).queryByRole('button', { name: 'Manage plan' })).not.toBeInTheDocument()
  })

  it('un guasto della vetrina degrada la sola sezione delle app', async () => {
    failCatalog = true
    renderWithProviders(<DashboardPage />)
    expect(
      await screen.findByText('We couldn’t load your apps.', {}, { timeout: 5_000 }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument()
    // il resto della pagina resta utile
    expect(screen.getByRole('complementary', { name: 'Workspace at a glance' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Browse the catalog' })).toBeInTheDocument()
  })
})
