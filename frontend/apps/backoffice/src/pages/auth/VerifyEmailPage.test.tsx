import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { screen } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { VerifyEmailPage } from './VerifyEmailPage'
import { renderWithProviders } from '../../test/utils'

// Contratto del verify (UC 0015): provider locale → token (auto-login);
// provider Cognito → { status: "confirmed" } senza token (la UI rimanda al login).
// UC 0018: il corpo può portare `token`, oppure `email` + `code` (collegamento generato dal
// Custom Message Lambda, che quando compone il messaggio non conosce ancora il codice).
let lastVerifyBody: Record<string, unknown> | null = null

const server = setupServer(
  http.post('http://localhost/api/auth/verify', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>
    lastVerifyBody = body
    if (body.token === 'cognito-ok' || body.code === '123456')
      return HttpResponse.json({ status: 'confirmed' })
    if (body.token === 'local-ok')
      return HttpResponse.json({ access_token: 'a.b.c', id_token: 'i.d.t', token_type: 'Bearer' })
    return HttpResponse.json({ title: 'Bad Request' }, { status: 400 })
  }),
)
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('VerifyEmailPage', () => {
  it('senza token → errore di link non valido', () => {
    renderWithProviders(<VerifyEmailPage />, { route: '/verify' })
    expect(screen.getByText('This verification link is invalid or expired.')).toBeInTheDocument()
  })

  it('conferma senza auto-login (provider Cognito) → invito ad accedere', async () => {
    renderWithProviders(<VerifyEmailPage />, { route: '/verify?token=cognito-ok' })
    expect(await screen.findByText('Email verified! Sign in to continue.')).toBeInTheDocument()
  })

  it('token invalido → errore', async () => {
    renderWithProviders(<VerifyEmailPage />, { route: '/verify?token=bad' })
    expect(
      await screen.findByText('This verification link is invalid or expired.'),
    ).toBeInTheDocument()
  })

  // UC 0018: è la forma dei collegamenti generati da Cognito. Se la pagina la ignorasse,
  // in cloud NESSUNA verifica email funzionerebbe — e in locale non se ne accorgerebbe nessuno.
  it('collegamento con indirizzo e codice (email Cognito) → conferma', async () => {
    renderWithProviders(<VerifyEmailPage />, {
      route: '/verify?email=utente%40esempio.test&code=123456',
    })
    expect(await screen.findByText('Email verified! Sign in to continue.')).toBeInTheDocument()
    expect(lastVerifyBody).toEqual({ email: 'utente@esempio.test', code: '123456' })
  })

  it('indirizzo senza codice → link non valido (non si chiama il backend a vuoto)', () => {
    renderWithProviders(<VerifyEmailPage />, { route: '/verify?email=utente%40esempio.test' })
    expect(screen.getByText('This verification link is invalid or expired.')).toBeInTheDocument()
  })
})
