import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { Settings } from './Settings'
import { useAuthStore } from '../auth/authStore'
import { renderWithProviders, fakeAccessToken } from '../test/utils'

const server = setupServer(
  http.get('http://localhost/api/platform/v1/accounts/me', () =>
    HttpResponse.json({ id: 'a1', name: 'Acme', status: 'active' }),
  ),
  http.patch('http://localhost/api/platform/v1/accounts/me', async ({ request }) => {
    const body = (await request.json()) as { name: string }
    return HttpResponse.json({ id: 'a1', name: body.name, status: 'active' })
  }),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe('Settings — form RHF + Zod (schemi ↔ Bean Validation)', () => {
  it('valida il nome obbligatorio e salva un nome valido', async () => {
    useAuthStore.getState().setSession({ accessToken: fakeAccessToken() })
    const user = userEvent.setup()
    renderWithProviders(<Settings />)

    const field = await screen.findByDisplayValue('Acme')

    // svuota → submit → errore required (Bean Validation @NotBlank)
    await user.clear(field)
    await user.click(screen.getByRole('button', { name: 'Save' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('This field is required')

    // nome valido → submit → salvato
    await user.type(field, 'Nuovo nome')
    await user.click(screen.getByRole('button', { name: 'Save' }))
    expect(await screen.findByText('Saved')).toBeInTheDocument()
  })
})
