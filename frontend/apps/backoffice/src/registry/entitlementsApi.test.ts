import { describe, it, expect } from 'vitest'
import { computeEntitled, deriveEntitlementsStatus } from './entitlementsApi'

describe('deriveEntitlementsStatus — stati della lettura come li vede la shell (UC 0077)', () => {
  it('primo caricamento in corso: caricamento, non errore', () => {
    expect(deriveEntitlementsStatus(true, false, false)).toEqual({ isLoading: true, isError: false })
  })

  it('lettura mai partita (sessione non autenticata): né caricamento né errore', () => {
    // Caso che ha fatto esplodere la shell: la lettura è disabilitata finché la sessione non è
    // autenticata, quindi non sarà MAI "conclusa". Dichiararla in caricamento lascia appeso per
    // sempre chi aspetta che finisca — e la rotta protetta non redirige più al login.
    expect(deriveEntitlementsStatus(false, false, false)).toEqual({
      isLoading: false,
      isError: false,
    })
  })

  it('con dati è pronto', () => {
    expect(deriveEntitlementsStatus(false, true, true)).toEqual({ isLoading: false, isError: false })
  })

  it('dopo un esito senza dati è errore, e lo resta mentre si ritenta', () => {
    // Durante il ritentativo la libreria torna "in attesa" e azzera l'errore; senza questa lettura
    // la pagina aperta verrebbe smontata a ogni ritentativo e per un istante l'elenco vuoto
    // passerebbe per "non hai diritto a nulla".
    expect(deriveEntitlementsStatus(false, true, false)).toEqual({ isLoading: false, isError: true })
    expect(deriveEntitlementsStatus(true, true, false)).toEqual({ isLoading: false, isError: true })
  })
})

describe('computeEntitled — slug entitled dal read-model /me/entitlements (UC 0027)', () => {
  const view = {
    entitlements: [
      { appSlug: 'fatture', tierKey: 'free' },
      { appSlug: 'teams', tierKey: 'team' },
    ],
  }

  it('estrae gli slug entitled dalla risposta', () => {
    expect(computeEntitled(view, false)).toEqual(['fatture', 'teams'])
  })

  it('in locale aggiunge il modulo demo (senza backend) senza duplicati', () => {
    expect(computeEntitled(view, true)).toEqual(['demo', 'fatture', 'teams'])
    expect(computeEntitled({ entitlements: [{ appSlug: 'demo' }] }, true)).toEqual(['demo'])
  })

  it('gestisce risposta vuota/assente e slug non validi', () => {
    expect(computeEntitled(undefined, false)).toEqual([])
    expect(computeEntitled({ entitlements: [] }, false)).toEqual([])
    expect(computeEntitled({ entitlements: [{}] }, false)).toEqual([])
    expect(computeEntitled(undefined, true)).toEqual(['demo'])
  })
})
