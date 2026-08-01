import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen, within, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'jest-axe'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { Overview } from './Overview'
import { Accounts } from './Accounts'
import { Entitlements } from './Entitlements'
import { Apps } from './Apps'
import { useAuthStore } from '../auth/authStore'
import { renderWithProviders, renderApp, fakeAccessToken } from '../test/utils'

const OVERVIEW = { accounts: 12, users: 47, activeSubscriptions: 8, disabledApps: 2 }

const ACCOUNTS = [
  { id: 'a-1', name: 'Acme', status: 'active', users: 5, activeSubscriptions: 2 },
  { id: 'a-2', name: 'Globex', status: 'suspended', users: 1, activeSubscriptions: 0 },
]

const ENTITLEMENTS = [
  {
    tenantId: 'a-1',
    tenantName: 'Acme',
    appId: 'app-1',
    appSlug: 'crm',
    appName: 'CRM',
    subscriptionStatus: 'active',
    appActive: true,
    entitled: true,
  },
  {
    tenantId: 'a-2',
    tenantName: 'Globex',
    appId: 'app-1',
    appSlug: 'crm',
    appName: 'CRM',
    subscriptionStatus: 'canceled',
    appActive: true,
    entitled: false,
  },
]

let apps: Array<Record<string, unknown>>
let auditRows: Array<Record<string, unknown>>
let patched: Array<{ id: string; status: string; reason?: string }>

const server = setupServer(
  http.get('http://localhost/api/platform/v1/admin/overview', () => HttpResponse.json(OVERVIEW)),
  http.get('http://localhost/api/platform/v1/admin/accounts', () => HttpResponse.json(ACCOUNTS)),
  http.get('http://localhost/api/platform/v1/admin/entitlements', () => HttpResponse.json(ENTITLEMENTS)),
  http.get('http://localhost/api/platform/v1/admin/apps', () => HttpResponse.json(apps)),
  http.get('http://localhost/api/platform/v1/admin/apps/audit', () => HttpResponse.json(auditRows)),
  http.patch('http://localhost/api/platform/v1/admin/apps/:id', async ({ params, request }) => {
    const body = (await request.json()) as { status: string; reason?: string }
    patched.push({ id: params.id as string, status: body.status, reason: body.reason })
    apps = apps.map((a) => (a.id === params.id ? { ...a, status: body.status } : a))
    const app = apps.find((a) => a.id === params.id) as Record<string, string>
    // il registro cresce di una riga per ogni transizione, come lato servizio (UC 0076)
    auditRows = [
      {
        id: `audit-${auditRows.length + 1}`,
        appId: app.id,
        appSlug: app.slug,
        appName: app.name,
        fromStatus: body.status === 'active' ? 'inactive' : 'active',
        toStatus: body.status,
        actor: 'sub-admin',
        reason: body.reason ?? null,
        executedAt: '2026-08-01T10:00:00Z',
      },
      ...auditRows,
    ]
    return HttpResponse.json(app)
  }),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  apps = [
    { id: 'app-1', slug: 'crm', name: 'CRM', userModel: 'b2b', status: 'active' },
    { id: 'app-2', slug: 'billing', name: 'Billing', userModel: 'b2c', status: 'inactive' },
  ]
  auditRows = []
  patched = []
  useAuthStore
    .getState()
    .setSession({ accessToken: fakeAccessToken({ roles: ['platform-admin'] }) })
})

describe('Console admin (UC 0021)', () => {
  it('Overview: mostra le 4 card KPI con i valori', async () => {
    renderWithProviders(<Overview />)
    expect(await screen.findByText('12')).toBeInTheDocument()
    expect(screen.getByText('47')).toBeInTheDocument()
    expect(screen.getByText('8')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
  })

  it('Accounts: elenca i tenant con link al dettaglio', async () => {
    renderWithProviders(<Accounts />)
    const acme = await screen.findByRole('link', { name: 'Acme' })
    expect(acme).toHaveAttribute('href', '/accounts/a-1')
    expect(screen.getByText('Globex')).toBeInTheDocument()
  })

  it('Entitlements: evidenzia entitled sì/no', async () => {
    renderWithProviders(<Entitlements />)
    await screen.findByText('Acme')
    expect(screen.getByText('Yes')).toBeInTheDocument()
    expect(screen.getByText('No')).toBeInTheDocument()
  })

  it('Apps disable-app: conferma con motivazione → PATCH → stato riflesso e registro aggiornato', async () => {
    const user = userEvent.setup()
    renderWithProviders(<Apps />)
    await screen.findByText('CRM')
    // registro inizialmente vuoto, con un testo che lo dice (UC 0076)
    expect(await screen.findByText('No disable recorded yet.')).toBeInTheDocument()

    const crmRow = screen.getByText('CRM').closest('tr') as HTMLElement
    await user.click(within(crmRow).getByRole('button', { name: 'Disable' }))

    const dialog = await screen.findByRole('dialog')
    // il dialogo distingue la pausa reversibile dalla dismissione definitiva
    expect(within(dialog).getByText(/not the permanent removal/i)).toBeInTheDocument()
    await user.type(within(dialog).getByLabelText('Reason (optional)'), 'incident 42')
    await user.click(within(dialog).getByRole('button', { name: 'Disable' }))

    await waitFor(() =>
      expect(patched).toEqual([{ id: 'app-1', status: 'inactive', reason: 'incident 42' }]),
    )
    await waitFor(() => {
      // getAllByText: dopo la transizione 'CRM' compare anche nel registro sotto la tabella
      const row = screen.getAllByText('CRM')[0].closest('tr') as HTMLElement
      expect(within(row).getByText('Disabled')).toBeInTheDocument()
    })
    // la transizione compare nel registro con la sua motivazione
    expect(await screen.findByText('incident 42')).toBeInTheDocument()
  })

  it('Apps non ha violazioni di accessibilità', async () => {
    const { container } = renderWithProviders(<Apps />)
    await screen.findByText('CRM')
    expect(await axe(container)).toHaveNoViolations()
  })

  it('gating: token senza platform-admin → la route protetta redirige a /forbidden', async () => {
    useAuthStore.getState().setSession({ accessToken: fakeAccessToken({ roles: ['owner'] }) })
    renderApp({ route: '/' })
    expect(await screen.findByText('You don’t have access to the admin console.')).toBeInTheDocument()
  })
})
