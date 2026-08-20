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
    // UC 0118 — più appartenenze e nessuna scelta: la sfida di scelta, nessun token.
    if (body.email === 'multi@x.io')
      return HttpResponse.json({
        account_selection_required: true,
        choice_token: 'ch-account-1',
        accounts: [
          { account_id: 'tenant-a', account_name: 'Acme Corp' },
          { account_id: 'tenant-b', account_name: 'Beta Srl' },
        ],
      })
    return HttpResponse.json(session)
  }),
  http.post('http://localhost/api/auth/login/account', async ({ request }) => {
    const body = (await request.json()) as { choice_token: string; account_id: string }
    if (body.choice_token !== 'ch-account-1') return HttpResponse.json({ title: 'Unauthorized' }, { status: 401 })
    if (body.account_id === 'tenant-revocato') return HttpResponse.json({ title: 'Not Found' }, { status: 404 })
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

  it('con più account e nessuna scelta mostra la schermata di scelta e nessuna sessione', async () => {
    // UC 0118: la proprietà essenziale è che NESSUN token esista finché la persona non ha scelto.
    const user = userEvent.setup()
    renderWithProviders(<LoginPage />)
    await user.type(screen.getByLabelText('Email'), 'multi@x.io')
    await user.type(screen.getByLabelText('Password'), 'Password1!')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('button', { name: 'Acme Corp' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Beta Srl' })).toBeInTheDocument()
    expect(useAuthStore.getState().status).toBe('anonymous')

    await user.click(screen.getByRole('button', { name: 'Beta Srl' }))
    await new Promise((r) => setTimeout(r, 0))
    expect(useAuthStore.getState().status).toBe('authenticated')
  })

  it('non ha violazioni a11y', async () => {
    const { container } = renderWithProviders(<LoginPage />)
    expect(await axe(container)).toHaveNoViolations()
  })
})
