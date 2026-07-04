import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'jest-axe'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { PrivacyPage } from './PrivacyPage'
import { useAuthStore } from '../../auth/authStore'
import { renderWithProviders, fakeAccessToken } from '../../test/utils'

let me: Record<string, unknown>
let account: Record<string, unknown>
let exportJobs: Map<string, Record<string, unknown>>
let purgeRequests: Array<{ appSlug: string; exportJobId: string }>

const server = setupServer(
  http.get('http://localhost/api/platform/v1/users/me', () => HttpResponse.json(me)),
  http.patch('http://localhost/api/platform/v1/users/me', async ({ request }) => {
    const body = (await request.json()) as { displayName: string }
    me = { ...me, displayName: body.displayName }
    return HttpResponse.json(me)
  }),
  http.get('http://localhost/api/platform/v1/users/me/export', () =>
    HttpResponse.json({ generatedAt: 'now', profile: me, account: {}, invitations: [] }),
  ),
  http.get('http://localhost/api/platform/v1/accounts/me', () => HttpResponse.json(account)),
  http.post('http://localhost/api/platform/v1/accounts/me/deletion', () => {
    account = {
      ...account,
      status: 'pending_deletion',
      deletionRequestedAt: '2026-07-04T10:00:00Z',
      deletionEffectiveAt: '2026-07-18T10:00:00Z',
    }
    return HttpResponse.json(account, { status: 202 })
  }),
  http.delete('http://localhost/api/platform/v1/accounts/me/deletion', () => {
    account = { ...account, status: 'active', deletionRequestedAt: null, deletionEffectiveAt: null }
    return HttpResponse.json(account)
  }),
  http.get('http://localhost/api/platform/v1/me/subscriptions', () =>
    HttpResponse.json({
      subscriptions: [{ appSlug: 'fatture', appName: 'Fatture', status: 'active', phase: 'ACTIVE' }],
    }),
  ),
  http.post('http://localhost/api/platform/v1/gdpr/exports', async ({ request }) => {
    const body = (await request.json()) as { kind: string; appId?: string }
    const id = `job-${exportJobs.size + 1}`
    const job = {
      id,
      kind: body.kind,
      appId: body.appId,
      status: 'COMPLETED', // completa subito: il polling si spegne al primo giro
      progress: { completed: 1, total: 1 },
      items: [],
    }
    exportJobs.set(id, job)
    return HttpResponse.json(job, { status: 202 })
  }),
  http.get('http://localhost/api/platform/v1/gdpr/exports/:id', ({ params }) =>
    HttpResponse.json(exportJobs.get(params.id as string)),
  ),
  http.get('http://localhost/api/platform/v1/gdpr/exports/:id/download', () =>
    HttpResponse.json({ url: 'https://minio.local/zip', expiresAt: '2026-07-11T10:00:00Z' }),
  ),
  http.post(
    'http://localhost/api/platform/v1/gdpr/apps/:appSlug/withdrawal',
    async ({ params, request }) => {
      const body = (await request.json()) as { exportJobId: string }
      purgeRequests.push({ appSlug: params.appSlug as string, exportJobId: body.exportJobId })
      return HttpResponse.json(
        { appId: params.appSlug, status: 'PURGE_REQUESTED' },
        { status: 202 },
      )
    },
  ),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  me = { id: 'u-1', email: 'owner@test', displayName: 'Owner', role: 'owner', status: 'active' }
  account = { id: 't-1', name: 'Acme', status: 'active' }
  exportJobs = new Map()
  purgeRequests = []
  useAuthStore.getState().setSession({ accessToken: fakeAccessToken({ sub: 'u-1', roles: ['owner'] }) })
})

describe('PrivacyPage (UC 0033)', () => {
  it('mostra profilo e sezioni dei diritti; nessuna violazione di accessibilità', async () => {
    const { container } = renderWithProviders(<PrivacyPage />)
    expect(await screen.findByText('owner@test')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'My profile' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Export my profile' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Account export' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Withdraw from an app' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Delete account' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Other rights' })).toBeInTheDocument()
    expect(await axe(container)).toHaveNoViolations()
  })

  it('member: niente export account, recesso o eliminazione (sezioni owner/admin)', async () => {
    useAuthStore
      .getState()
      .setSession({ accessToken: fakeAccessToken({ sub: 'u-1', roles: ['member'] }) })
    me = { ...me, role: 'member' }
    renderWithProviders(<PrivacyPage />)
    await screen.findByText('owner@test')
    expect(screen.queryByRole('heading', { name: 'Account export' })).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Withdraw from an app' })).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Delete account' })).not.toBeInTheDocument()
  })

  it('rettifica il nome visualizzato (art. 16)', async () => {
    const user = userEvent.setup()
    renderWithProviders(<PrivacyPage />)
    const input = await screen.findByLabelText('Display name')
    await user.clear(input)
    await user.type(input, 'Nuovo Nome')
    await user.click(screen.getByRole('button', { name: 'Save' }))
    expect(await screen.findByText('Saved')).toBeInTheDocument()
    expect(me.displayName).toBe('Nuovo Nome')
  })

  it('export account: avvio, completamento e link con scadenza', async () => {
    const user = userEvent.setup()
    renderWithProviders(<PrivacyPage />)
    await screen.findByText('owner@test')
    await user.click(screen.getByRole('button', { name: /Start export/ }))
    expect(await screen.findByText('Export ready.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Download archive' }))
    expect(await screen.findByText(/Link valid until/)).toBeInTheDocument()
  })

  it('recesso per-app: esporta → conferma nel dialog → withdrawal con il job dell’export', async () => {
    const user = userEvent.setup()
    renderWithProviders(<PrivacyPage />)
    await screen.findByText('Fatture')

    // la conferma è disabilitata finché l'export non è pronto
    expect(screen.getByRole('button', { name: 'Confirm withdrawal' })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: 'Export app data' }))
    const confirm = await screen.findByRole('button', { name: 'Confirm withdrawal' })
    await expect.poll(() => (confirm as HTMLButtonElement).disabled).toBe(false)

    await user.click(confirm)
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Confirm withdrawal' }))

    expect(await screen.findByText(/Withdrawal completed/)).toBeInTheDocument()
    expect(purgeRequests).toEqual([{ appSlug: 'fatture', exportJobId: 'job-1' }])
  })

  it('eliminazione account: conferma → stato in grace con scadenza → annulla', async () => {
    const user = userEvent.setup()
    renderWithProviders(<PrivacyPage />)
    await screen.findByText('owner@test')

    await user.click(screen.getByRole('button', { name: 'Delete this account' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Delete this account' }))

    expect(await screen.findByText(/Deletion requested/)).toBeInTheDocument()
    expect(screen.getByText(/Permanent deletion on/)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Cancel deletion' }))
    expect(await screen.findByText(/Deletion canceled/)).toBeInTheDocument()
  })
})
