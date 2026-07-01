import { describe, expect, it } from 'vitest'
import { ApiError } from '@appgrove/api-client'
import { enforcementFromError } from './enforcement'

describe('enforcementFromError', () => {
  it('mappa 402 → entitlement (riattiva/esporta)', () => {
    expect(enforcementFromError(new ApiError(402, { title: 'Abbonamento richiesto' }))).toBe('entitlement')
  })

  it('mappa 429 → quota (upgrade)', () => {
    expect(enforcementFromError(new ApiError(429, { title: 'Quota esaurita' }))).toBe('quota')
  })

  it('ignora gli altri status', () => {
    expect(enforcementFromError(new ApiError(404, null))).toBeNull()
    expect(enforcementFromError(new ApiError(500, null))).toBeNull()
  })

  it('ignora gli errori non-ApiError', () => {
    expect(enforcementFromError(new Error('boom'))).toBeNull()
    expect(enforcementFromError(undefined)).toBeNull()
  })
})
