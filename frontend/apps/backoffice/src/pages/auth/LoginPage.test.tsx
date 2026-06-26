import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'jest-axe'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { LoginPage } from './LoginPage'
import { useAuthStore } from '../../auth/authStore'
import { renderWithProviders, fakeAccessToken, fakeIdToken } from '../../test/utils'

const session = { access_token: fakeAccessToken(), id_token: fakeIdToken(), token_type: 'Bearer' }

const server = setupServer(
  http.post('http://localhost/api/auth/login', async ({ request }) => {
    const body = (await request.json()) as { email: string }
    if (body.email === 'mfa@x.io') return HttpResponse.json({ mfa_required: true, challenge_token: 'ch-1' })
    if (body.email === 'bad@x.io') return HttpResponse.json({ title: 'Unauthorized' }, { status: 401 })
    return HttpResponse.json(session)
  }),
  http.post('http://localhost/api/auth/login/2fa', async ({ request }) => {
    const body = (await request.json()) as { code: string }
    if (body.code !== '123456') return HttpResponse.json({ title: 'Unauthorized' }, { status: 401 })
    return HttpResponse.json(session)
  }),
)
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
beforeEach(() => useAuthStore.getState().clear())

describe('LoginPage', () => {
  it('accede con credenziali valide e stabilisce la sessione', async () => {
    const user = userEvent.setup()
    renderWithProviders(<LoginPage />)
    await user.type(screen.getByLabelText('Email'), 'owner@acme.test')
    await user.type(screen.getByLabelText('Password'), 'Password1!')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))
    await new Promise((r) => setTimeout(r, 0))
    expect(useAuthStore.getState().status).toBe('authenticated')
  })

  it('su 2FA mostra lo step codice e completa con login/2fa', async () => {
    const user = userEvent.setup()
    renderWithProviders(<LoginPage />)
    await user.type(screen.getByLabelText('Email'), 'mfa@x.io')
    await user.type(screen.getByLabelText('Password'), 'Password1!')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    const code = await screen.findByLabelText('Code')
    await user.type(code, '123456')
    await user.click(screen.getByRole('button', { name: 'Verify' }))
    await new Promise((r) => setTimeout(r, 0))
    expect(useAuthStore.getState().status).toBe('authenticated')
  })

  it('mostra un errore localizzato su credenziali non valide', async () => {
    const user = userEvent.setup()
    renderWithProviders(<LoginPage />)
    await user.type(screen.getByLabelText('Email'), 'bad@x.io')
    await user.type(screen.getByLabelText('Password'), 'Password1!')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByText('Invalid email or password.')).toBeInTheDocument()
    expect(useAuthStore.getState().status).toBe('anonymous')
  })

  it('non ha violazioni a11y', async () => {
    const { container } = renderWithProviders(<LoginPage />)
    expect(await axe(container)).toHaveNoViolations()
  })
})
