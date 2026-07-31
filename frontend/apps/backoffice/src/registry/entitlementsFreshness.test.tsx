import { describe, it, expect, beforeAll, beforeEach, afterAll, afterEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'
import { focusManager } from '@tanstack/react-query'
import { useAuthStore } from '../auth/authStore'
import { makeQueryClient } from '../api/queryClient'
import { renderWithProviders, fakeAccessToken } from '../test/utils'
import { computeEntitled, useInvalidateEntitlements, useMyEntitlements } from './entitlementsApi'

/** Elenco servito dal finto backend; i test lo cambiano per simulare un entitlement che muta. */
let entitledOnServer: string[] = []

const server = setupServer(
  http.get('http://localhost/api/platform/v1/me/entitlements', () =>
    HttpResponse.json({ entitlements: entitledOnServer.map((appSlug) => ({ appSlug })) }),
  ),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }))
afterEach(() => {
  server.resetHandlers()
  focusManager.setFocused(undefined) // torna al rilevamento reale per le altre suite
})
afterAll(() => server.close())

beforeEach(() => {
  entitledOnServer = ['fatture']
  useAuthStore.getState().setSession({ accessToken: fakeAccessToken() })
})

/** Sonda minima: mostra gli slug entitled e offre l'invalidazione esplicita. */
function Probe() {
  const query = useMyEntitlements()
  const invalidate = useInvalidateEntitlements()
  return (
    <>
      <span data-testid="slugs">{computeEntitled(query.data, false).join(',')}</span>
      <button type="button" onClick={() => void invalidate()}>
        invalida
      </button>
    </>
  )
}

describe('freschezza degli entitlement (UC 0077)', () => {
  it('rilegge quando l’utente torna sulla scheda', async () => {
    renderWithProviders(<Probe />)
    await waitFor(() => expect(screen.getByTestId('slugs')).toHaveTextContent('fatture'))

    // Cambiamento non originato dall'utente: un'app disabilitata dalla piattaforma mentre la
    // scheda era aperta. Senza la rilettura al ritorno, il menu resterebbe stantio.
    entitledOnServer = []
    focusManager.setFocused(false)
    focusManager.setFocused(true)

    await waitFor(() => expect(screen.getByTestId('slugs')).toHaveTextContent(''))
  })

  it('l’invalidazione esplicita rilegge subito (acquisto, disdetta, cambio piano)', async () => {
    renderWithProviders(<Probe />)
    await waitFor(() => expect(screen.getByTestId('slugs')).toHaveTextContent('fatture'))

    entitledOnServer = ['fatture', 'crm']
    await userEvent.click(screen.getByRole('button', { name: 'invalida' }))

    await waitFor(() => expect(screen.getByTestId('slugs')).toHaveTextContent('fatture,crm'))
  })

  it('la rilettura al ritorno sulla scheda resta disattivata per le altre letture', () => {
    // Il comportamento è dichiarato sulla singola query, non riattivato in generale: le altre
    // letture della shell non devono iniziare a chiamare a ogni cambio di finestra.
    expect(makeQueryClient().getDefaultOptions().queries?.refetchOnWindowFocus).toBe(false)
  })
})
