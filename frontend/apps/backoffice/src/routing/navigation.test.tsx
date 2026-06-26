import { describe, it, expect, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import { useAuthStore } from '../auth/authStore'
import { renderApp, fakeAccessToken } from '../test/utils'

describe('navigazione + route guard (app intera)', () => {
  beforeEach(() => useAuthStore.getState().clear())

  it('utente entitled: il modulo viene montato sotto /app/demo', async () => {
    useAuthStore.getState().setSession({ accessToken: fakeAccessToken() })
    renderApp({ entitled: ['demo'], route: '/app/demo' })
    expect(await screen.findByTestId('demo-module')).toBeInTheDocument()
  })

  it('utente NON entitled: /app/demo è bloccato dalla guard → /forbidden', async () => {
    useAuthStore.getState().setSession({ accessToken: fakeAccessToken() })
    renderApp({ entitled: [], route: '/app/demo' })
    expect(await screen.findByText('You don’t have access to this app')).toBeInTheDocument()
    expect(screen.queryByTestId('demo-module')).not.toBeInTheDocument()
  })

  it('anonimo: una route protetta redirige al login', async () => {
    useAuthStore.getState().clear()
    renderApp({ entitled: ['demo'], route: '/' })
    expect(await screen.findByText('Please sign in to continue')).toBeInTheDocument()
  })
})
