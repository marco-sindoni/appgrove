import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { SecurityPage } from './SecurityPage'
import { useAuthStore } from '../auth/authStore'
import { renderWithProviders, fakeAccessToken, fakeIdToken } from '../test/utils'

const server = setupServer(
  http.post('http://localhost/api/auth/2fa/enroll', () =>
    HttpResponse.json({ secret: 'JBSWY3DPEHPK3PXP', otpauth_uri: 'otpauth://totp/appgrove:u@x.io?secret=JBSWY3DPEHPK3PXP' }),
  ),
  http.post('http://localhost/api/auth/2fa/verify', async ({ request }) => {
    const body = (await request.json()) as { code: string }
    if (body.code !== '123456') return HttpResponse.json({ title: 'Unauthorized' }, { status: 401 })
    return new HttpResponse(null, { status: 204 })
  }),
)
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
beforeEach(() =>
  useAuthStore.getState().setSession({ accessToken: fakeAccessToken(), idToken: fakeIdToken() }),
)

describe('SecurityPage — setup 2FA', () => {
  it('enroll mostra il secret/QR e la verifica del codice attiva il 2FA', async () => {
    const user = userEvent.setup()
    renderWithProviders(<SecurityPage />)

    await user.click(screen.getByRole('button', { name: 'Enable 2FA' }))
    expect(await screen.findByText('JBSWY3DPEHPK3PXP')).toBeInTheDocument()

    await user.type(screen.getByLabelText(/Enter the 6-digit code/), '123456')
    await user.click(screen.getByRole('button', { name: 'Confirm' }))
    expect(await screen.findByText('Two-factor authentication is enabled.')).toBeInTheDocument()
  })

  it('codice errato → errore localizzato', async () => {
    const user = userEvent.setup()
    renderWithProviders(<SecurityPage />)
    await user.click(screen.getByRole('button', { name: 'Enable 2FA' }))
    await screen.findByText('JBSWY3DPEHPK3PXP')
    await user.type(screen.getByLabelText(/Enter the 6-digit code/), '000000')
    await user.click(screen.getByRole('button', { name: 'Confirm' }))
    expect(await screen.findByText('Invalid code.')).toBeInTheDocument()
  })
})
