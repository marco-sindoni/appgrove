import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { screen } from '@testing-library/react'
import { render } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { createI18n } from '@appgrove/i18n'
import { App } from './App'
import type { RuntimeConfig } from './config'
import { fakeAccessToken, fakeIdToken } from './test/utils'

const cfg: RuntimeConfig = {
  env: 'local',
  authBaseUrl: 'http://localhost',
  coreBaseUrl: 'http://localhost',
  cognito: { userPoolId: '', clientId: '' },
  errorIngestUrl: '', // vuoto nei test: il reporter errori è inerte
}

const server = setupServer(
  http.post('http://localhost/api/auth/refresh', () =>
    HttpResponse.json({ access_token: fakeAccessToken(), id_token: fakeIdToken() }),
  ),
  http.get('http://localhost/api/platform/v1/users/me', () =>
    HttpResponse.json({ id: 'u1', email: 'u@x.io', displayName: 'U', role: 'owner', status: 'active', tenantId: 'tenant-1' }),
  ),
  http.get('http://localhost/api/platform/v1/accounts/me', () =>
    HttpResponse.json({ id: 'a1', name: 'Acme', status: 'active' }),
  ),
  http.get('http://localhost/api/platform/v1/me/entitlements', () =>
    HttpResponse.json({
      entitlements: [
        { appSlug: 'fatture', tierKey: 'free', limits: { fatture: { cap: 10, nature: 'flow', window: 'month' } } },
      ],
    }),
  ),
)
beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('App smoke', () => {
  it('monta la shell dopo il refresh-on-load (idle → authenticated)', async () => {
    const { useAuthStore } = await import('./auth/authStore')
    useAuthStore.getState().clear()
    useAuthStore.setState({ status: 'idle' })
    render(<App config={cfg} i18n={createI18n()} />)
    expect(await screen.findByText('Platform')).toBeInTheDocument()
  })
})
