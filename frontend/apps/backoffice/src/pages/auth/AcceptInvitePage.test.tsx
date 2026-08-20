import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { AcceptInvitePage } from './AcceptInvitePage'
import { useAuthStore } from '../../auth/authStore'
import { renderWithProviders, fakeAccessToken, fakeIdToken } from '../../test/utils'

const server = setupServer(
  // UC 0118 — l'ispezione dell'invito precede il modulo: dice se serve una parola d'accesso o solo
  // di autenticarsi. Il token `esistente` è di un indirizzo che ha già un'identità.
  http.post('http://localhost/api/auth/invitations/lookup', async ({ request }) => {
    const body = (await request.json()) as { token: string }
    if (body.token === 'esistente') {
      return HttpResponse.json({ mode: 'signin', email: 'chi-esiste@x.io' })
    }
    return HttpResponse.json({ mode: 'register', email: 'nuovo@x.io' })
  }),
  http.post('http://localhost/api/auth/invitations/accept', async ({ request }) => {
    const body = (await request.json()) as { token: string }
    if (body.token === 'expired') {
      return HttpResponse.json({ title: 'Gone', detail: 'scaduto' }, { status: 410 })
    }
    return HttpResponse.json({ access_token: fakeAccessToken({ roles: ['member'] }), id_token: fakeIdToken() })
  }),
)
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
beforeEach(() => useAuthStore.getState().clear())

describe('AcceptInvitePage', () => {
  it('token mancante → messaggio dedicato', () => {
    renderWithProviders(<AcceptInvitePage />, { route: '/accept' })
    expect(screen.getByText('Missing invitation token.')).toBeInTheDocument()
  })

  it('accetta l’invito e stabilisce la sessione (member)', async () => {
    const user = userEvent.setup()
    renderWithProviders(<AcceptInvitePage />, { route: '/accept?token=ok' })
    await user.type(await screen.findByLabelText(/Password/), 'Password1!')
    await user.click(screen.getByRole('button', { name: 'Accept invitation' }))
    await new Promise((r) => setTimeout(r, 0))
    expect(useAuthStore.getState().status).toBe('authenticated')
    expect(useAuthStore.getState().claims?.roles).toContain('member')
  })

  it('indirizzo che ha già un’identità → NON si chiede una parola d’accesso, si chiede di accedere', async () => {
    // La regola che questa schermata deve rendere vera (UC 0118): una parola d'accesso nuova su
    // un'identità esistente sarebbe una seconda identità mascherata. Il campo non deve esserci.
    renderWithProviders(<AcceptInvitePage />, { route: '/accept?token=esistente' })
    expect(await screen.findByRole('link', { name: 'Sign in to accept' })).toBeInTheDocument()
    expect(screen.queryByLabelText(/Password/)).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Accept invitation' })).not.toBeInTheDocument()
  })

  it('invito scaduto/usato (410) → messaggio dedicato', async () => {
    const user = userEvent.setup()
    renderWithProviders(<AcceptInvitePage />, { route: '/accept?token=expired' })
    await user.type(await screen.findByLabelText(/Password/), 'Password1!')
    await user.click(screen.getByRole('button', { name: 'Accept invitation' }))
    expect(await screen.findByText('This invitation has expired or was already used.')).toBeInTheDocument()
  })
})
