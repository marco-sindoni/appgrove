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
let patched: Array<{ id: string; status: string }>

const server = setupServer(
  http.get('http://localhost/api/platform/v1/admin/overview', () => HttpResponse.json(OVERVIEW)),
  http.get('http://localhost/api/platform/v1/admin/accounts', () => HttpResponse.json(ACCOUNTS)),
  http.get('http://localhost/api/platform/v1/admin/entitlements', () => HttpResponse.json(ENTITLEMENTS)),
  http.get('http://localhost/api/platform/v1/admin/apps', () => HttpResponse.json(apps)),
  http.patch('http://localhost/api/platform/v1/admin/apps/:id', async ({ params, request }) => {
    const body = (await request.json()) as { status: string }
    patched.push({ id: params.id as string, status: body.status })
    apps = apps.map((a) => (a.id === params.id ? { ...a, status: body.status } : a))
    return HttpResponse.json(apps.find((a) => a.id === params.id))
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

  it('Apps disable-app: conferma → PATCH → stato riflesso', async () => {
    const user = userEvent.setup()
    renderWithProviders(<Apps />)
    await screen.findByText('CRM')

    const crmRow = screen.getByText('CRM').closest('tr') as HTMLElement
    await user.click(within(crmRow).getByRole('button', { name: 'Disable' }))

    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Disable' }))

    await waitFor(() => expect(patched).toEqual([{ id: 'app-1', status: 'inactive' }]))
    await waitFor(() => {
      const row = screen.getByText('CRM').closest('tr') as HTMLElement
      expect(within(row).getByText('inactive')).toBeInTheDocument()
    })
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
