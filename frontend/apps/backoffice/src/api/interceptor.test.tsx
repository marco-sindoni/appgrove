import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { screen } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { Account } from '../pages/Account'
import { useAuthStore } from '../auth/authStore'
import { renderWithProviders, fakeAccessToken, fakeIdToken } from '../test/utils'

const staleToken = fakeAccessToken({ marker: 'stale' })
const freshToken = fakeAccessToken({ marker: 'fresh' })

const userView = {
  id: '00000000-0000-0000-0000-000000000001',
  email: 'u@x.io',
  displayName: 'Utente Uno',
  role: 'owner',
  status: 'active',
  tenantId: 'tenant-1',
}

const server = setupServer(
  // /users/me: 200 solo col token "fresh"; col token scaduto → 401 problem+json
  http.get('http://localhost/api/platform/v1/users/me', ({ request }) => {
    if (request.headers.get('authorization') === `Bearer ${freshToken}`) {
      return HttpResponse.json(userView)
    }
    return HttpResponse.json({ title: 'expired' }, { status: 401 })
  }),
  // /auth/refresh: emette i token freschi (cookie HttpOnly simulato lato server)
  http.post('http://localhost/api/auth/refresh', () =>
    HttpResponse.json({ access_token: freshToken, id_token: fakeIdToken(), token_type: 'Bearer' }),
  ),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('interceptor 401 → refresh → retry (end-to-end)', () => {
  it('una 401 scatena il refresh e ritenta la richiesta, mostrando il profilo', async () => {
    // sessione con access token scaduto in memoria
    useAuthStore.getState().setSession({ accessToken: staleToken })

    renderWithProviders(<Account />, { entitled: ['demo'] })

    // dopo refresh+retry, il profilo è renderizzato e lo store ha il token fresco
    expect(await screen.findByText('u@x.io')).toBeInTheDocument()
    expect(useAuthStore.getState().accessToken).toBe(freshToken)
  })
})
