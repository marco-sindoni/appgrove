import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { screen } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { VerifyEmailPage } from './VerifyEmailPage'
import { renderWithProviders } from '../../test/utils'

// Contratto del verify (UC 0015): provider locale → token (auto-login);
// provider Cognito → { status: "confirmed" } senza token (la UI rimanda al login).
const server = setupServer(
  http.post('http://localhost/api/auth/verify', async ({ request }) => {
    const body = (await request.json()) as { token: string }
    if (body.token === 'cognito-ok') return HttpResponse.json({ status: 'confirmed' })
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
})
