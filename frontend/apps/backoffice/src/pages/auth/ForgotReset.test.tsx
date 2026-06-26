import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { ForgotPasswordPage } from './ForgotPasswordPage'
import { ResetPasswordPage } from './ResetPasswordPage'
import { renderWithProviders } from '../../test/utils'

const server = setupServer(
  http.post('http://localhost/api/auth/password/forgot', () => new HttpResponse(null, { status: 202 })),
  http.post('http://localhost/api/auth/password/reset', async ({ request }) => {
    const body = (await request.json()) as { token: string }
    if (body.token === 'bad') return HttpResponse.json({ title: 'Bad Request' }, { status: 400 })
    return new HttpResponse(null, { status: 204 })
  }),
)
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('ForgotPasswordPage', () => {
  it('mostra una risposta neutra dopo l’invio (anti-enumeration)', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ForgotPasswordPage />, { route: '/forgot' })
    await user.type(screen.getByLabelText('Email'), 'whoever@x.io')
    await user.click(screen.getByRole('button', { name: 'Send reset link' }))
    expect(
      await screen.findByText('If an account exists for that email, a reset link is on its way.'),
    ).toBeInTheDocument()
  })
})

describe('ResetPasswordPage', () => {
  it('senza token → messaggio di link non valido', () => {
    renderWithProviders(<ResetPasswordPage />, { route: '/reset' })
    expect(screen.getByText('This reset link is invalid or expired.')).toBeInTheDocument()
  })

  it('con token valido → aggiorna la password', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ResetPasswordPage />, { route: '/reset?token=ok' })
    await user.type(screen.getByLabelText('New password'), 'Password1!')
    await user.click(screen.getByRole('button', { name: 'Update password' }))
    expect(await screen.findByText('Your password has been updated.')).toBeInTheDocument()
  })
})
