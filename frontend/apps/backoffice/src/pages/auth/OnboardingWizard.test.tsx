import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { OnboardingWizard } from './OnboardingWizard'
import { useAuthStore } from '../../auth/authStore'
import { renderWithProviders } from '../../test/utils'

let lastSignupBody: Record<string, unknown> | null = null
let lastNewsletterBody: Record<string, unknown> | null = null

const server = setupServer(
  http.post('http://localhost/api/auth/signup', async ({ request }) => {
    const body = (await request.json()) as { email: string }
    lastSignupBody = body as Record<string, unknown>
    if (body.email === 'taken@x.io') {
      return HttpResponse.json({ title: 'Conflict', detail: 'già registrata' }, { status: 409 })
    }
    return new HttpResponse(JSON.stringify({ status: 'verification_required' }), {
      status: 201,
      headers: { 'content-type': 'application/json' },
    })
  }),
  // Newsletter (UC 0039): iscrizione pubblica dal signup (canale 'signup').
  http.post('http://localhost/api/platform/v1/newsletter/subscriptions', async ({ request }) => {
    lastNewsletterBody = (await request.json()) as Record<string, unknown>
    return new HttpResponse(null, { status: 202 })
  }),
)
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
beforeEach(() => {
  useAuthStore.getState().clear()
  lastNewsletterBody = null
})

async function fillAccount(user: ReturnType<typeof userEvent.setup>, email: string) {
  await user.type(screen.getByLabelText(/Email/), email)
  await user.type(screen.getByLabelText(/Password/), 'Password1!')
  await user.click(screen.getByRole('button', { name: 'Create account' }))
}

describe('OnboardingWizard', () => {
  it('signup valido → passa allo step Verifica email', async () => {
    const user = userEvent.setup()
    renderWithProviders(<OnboardingWizard />, { route: '/signup' })
    await fillAccount(user, 'new@x.io')
    expect(
      await screen.findByText('We sent a verification link to new@x.io. Open it to continue.'),
    ).toBeInTheDocument()
  })

  // UC 0018: senza questo la lingua non arriverebbe mai al backend, e ogni email di ogni utente
  // sarebbe in inglese — senza che nulla fallisca da nessuna parte.
  it('la registrazione trasmette la lingua attiva dell’interfaccia', async () => {
    const user = userEvent.setup()
    renderWithProviders(<OnboardingWizard />, { route: '/signup' })
    await fillAccount(user, 'lang@x.io')
    await screen.findByText(/We sent a verification link/)
    expect(lastSignupBody?.locale).toBe('en')
  })

  it('email già registrata (409) → messaggio AZIONABILE e resta su Account', async () => {
    // UC 0118: non si può creare un account per conto di un'identità senza autenticarla, quindi il
    // rifiuto resta — ma il testo deve dire dove andare. Chi è già membro di un'azienda apre il
    // proprio account DALLA SESSIONE (pagina Account), non da qui: senza questa riga la persona si
    // registra con un secondo indirizzo, e unire due identità è lavoro manuale e sgradevole.
    const user = userEvent.setup()
    renderWithProviders(<OnboardingWizard />, { route: '/signup' })
    await fillAccount(user, 'taken@x.io')
    expect(
      await screen.findByText(
        'This email is already registered. Sign in: from your session you can open a new account.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Create account' })).toBeInTheDocument()
  })

  // UC 0039: privacy by default — il consenso newsletter non è mai pre-attivato.
  it('la checkbox newsletter è NON pre-spuntata di default', async () => {
    renderWithProviders(<OnboardingWizard />, { route: '/signup' })
    expect(await screen.findByRole('checkbox')).not.toBeChecked()
  })

  it('con consenso spuntato → iscrive alla newsletter (canale signup, consenso esplicito)', async () => {
    const user = userEvent.setup()
    renderWithProviders(<OnboardingWizard />, { route: '/signup' })
    await user.type(screen.getByLabelText(/Email/), 'nl@x.io')
    await user.type(screen.getByLabelText(/Password/), 'Password1!')
    await user.click(screen.getByRole('checkbox'))
    await user.click(screen.getByRole('button', { name: 'Create account' }))
    await screen.findByText(/We sent a verification link/)
    expect(lastNewsletterBody).toMatchObject({ email: 'nl@x.io', consent: true, channel: 'signup' })
  })

  it('senza consenso → nessuna iscrizione newsletter', async () => {
    const user = userEvent.setup()
    renderWithProviders(<OnboardingWizard />, { route: '/signup' })
    await fillAccount(user, 'noNl@x.io')
    await screen.findByText(/We sent a verification link/)
    expect(lastNewsletterBody).toBeNull()
  })

  it('valida la password policy lato client', async () => {
    const user = userEvent.setup()
    renderWithProviders(<OnboardingWizard />, { route: '/signup' })
    await user.type(screen.getByLabelText(/Email/), 'new@x.io')
    await user.type(screen.getByLabelText(/Password/), 'short')
    await user.click(screen.getByRole('button', { name: 'Create account' }))
    expect(
      await screen.findByText('At least 10 characters, with an uppercase, a lowercase and a number'),
    ).toBeInTheDocument()
  })
})
