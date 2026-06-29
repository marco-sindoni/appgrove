import { describe, it, expect } from 'vitest'
import { computeEntitled } from './entitlementsApi'

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
