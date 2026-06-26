import { describe, it, expect, beforeAll, afterAll, afterEach, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { OnboardingWizard } from './OnboardingWizard'
import { useAuthStore } from '../../auth/authStore'
import { renderWithProviders } from '../../test/utils'

const server = setupServer(
  http.post('http://localhost/api/auth/signup', async ({ request }) => {
    const body = (await request.json()) as { email: string }
    if (body.email === 'taken@x.io') {
      return HttpResponse.json({ title: 'Conflict', detail: 'già registrata' }, { status: 409 })
    }
    return new HttpResponse(JSON.stringify({ status: 'verification_required' }), {
      status: 201,
      headers: { 'content-type': 'application/json' },
    })
  }),
)
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
beforeEach(() => useAuthStore.getState().clear())

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

  it('email già registrata (409) → mostra errore e resta su Account', async () => {
    const user = userEvent.setup()
    renderWithProviders(<OnboardingWizard />, { route: '/signup' })
    await fillAccount(user, 'taken@x.io')
    expect(await screen.findByText('This email is already registered.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Create account' })).toBeInTheDocument()
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
