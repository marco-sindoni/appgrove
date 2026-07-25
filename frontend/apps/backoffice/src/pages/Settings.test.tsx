import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { Settings } from './Settings'
import { useAuthStore } from '../auth/authStore'
import { renderWithProviders, fakeAccessToken } from '../test/utils'

let lastNewsletterPref: boolean | null = null

const server = setupServer(
  http.get('http://localhost/api/platform/v1/accounts/me', () =>
    HttpResponse.json({ id: 'a1', name: 'Acme', status: 'active' }),
  ),
  http.patch('http://localhost/api/platform/v1/accounts/me', async ({ request }) => {
    const body = (await request.json()) as { name: string }
    return HttpResponse.json({ id: 'a1', name: body.name, status: 'active' })
  }),
  // Newsletter (UC 0039): preferenza dell'utente autenticato.
  http.get('http://localhost/api/platform/v1/newsletter/preference', () =>
    HttpResponse.json({ subscribed: false }),
  ),
  http.put('http://localhost/api/platform/v1/newsletter/preference', async ({ request }) => {
    const body = (await request.json()) as { subscribed: boolean }
    lastNewsletterPref = body.subscribed
    return HttpResponse.json({ subscribed: body.subscribed })
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

  // UC 0039: il toggle newsletter riflette lo stato e invia il cambiamento (canale account).
  it('il toggle newsletter parte spento e attiva l’iscrizione al click', async () => {
    useAuthStore.getState().setSession({ accessToken: fakeAccessToken() })
    const user = userEvent.setup()
    renderWithProviders(<Settings />)

    const toggle = await screen.findByRole('switch', { name: 'Receive the appgrove newsletter' })
    expect(toggle).toHaveAttribute('aria-checked', 'false')

    await user.click(toggle)
    await waitFor(() => expect(lastNewsletterPref).toBe(true))
  })
})
