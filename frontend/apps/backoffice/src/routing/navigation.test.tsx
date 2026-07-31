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

  it('entitlement non leggibili: /app/demo mostra un errore, NON "non hai accesso" (UC 0077)', async () => {
    // Un guasto di rete non deve trasformarsi in un diniego: chi ha pagato l'app non va informato
    // che non vi ha accesso perché una chiamata è fallita.
    useAuthStore.getState().setSession({ accessToken: fakeAccessToken() })
    renderApp({ entitled: [], entitlementsError: true, route: '/app/demo' })
    // Due occorrenze attese: il messaggio a tutta pagina della route e l'avviso in sidebar (che
    // porta anche la riprova).
    expect(await screen.findAllByText('We couldn’t load your apps')).toHaveLength(2)
    expect(screen.queryByText('You don’t have access to this app')).not.toBeInTheDocument()
    expect(screen.queryByTestId('demo-module')).not.toBeInTheDocument()
  })

  it('anonimo: una route protetta redirige al login', async () => {
    useAuthStore.getState().clear()
    renderApp({ entitled: ['demo'], route: '/' })
    // la pagina di login reale mostra il form (email) — non più il placeholder
    expect(await screen.findByLabelText('Email')).toBeInTheDocument()
  })

  it('ruolo member: /members è bloccato dalla guard di ruolo → /forbidden', async () => {
    useAuthStore.getState().setSession({ accessToken: fakeAccessToken({ roles: ['member'] }) })
    renderApp({ route: '/members' })
    expect(await screen.findByText('You don’t have access to this app')).toBeInTheDocument()
  })
})
