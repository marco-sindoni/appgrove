import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { Account } from './Account'
import { useAuthStore } from '../auth/authStore'
import { renderWithProviders, fakeAccessToken } from '../test/utils'

const BASE = 'http://localhost'

const server = setupServer(
  http.get(`${BASE}/api/platform/v1/users/me`, () =>
    HttpResponse.json({ id: 'u1', email: 'owner@acme.test', displayName: 'Marco', role: 'owner' }),
  ),
  http.get(`${BASE}/api/platform/v1/accounts/me`, () =>
    HttpResponse.json({ id: 'a1', name: 'Acme', status: 'active' }),
  ),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

beforeEach(() => {
  useAuthStore
    .getState()
    .setSession({ accessToken: fakeAccessToken({ tenant_id: 'tenant-42', roles: ['owner'] }) })
})

describe('Pagina Account — identificativo del workspace (UC 0097)', () => {
  it('mostra il workspace con il suo identificativo e la nota per l’assistenza', async () => {
    renderWithProviders(<Account />)
    expect(await screen.findByText('Workspace')).toBeInTheDocument()
    expect(await screen.findByText('Acme')).toBeInTheDocument()
    expect(screen.getByText('tenant-42')).toBeInTheDocument()
    expect(
      screen.getByText('Share this ID with support when opening a ticket.'),
    ).toBeInTheDocument()
  })

  it('il pulsante copia l’identificativo negli appunti e lo conferma', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.assign(navigator, { clipboard: { writeText } })

    renderWithProviders(<Account />)
    const button = await screen.findByRole('button', { name: 'Copy workspace ID' })
    await userEvent.click(button)

    expect(writeText).toHaveBeenCalledWith('tenant-42')
    expect(await screen.findByText('Copied')).toBeInTheDocument()
  })
})
