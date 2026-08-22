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

  /**
   * UC 0103 — l'addebito del posto rifiutato è un 402, ma **non** è un gate di entitlement: l'abbonamento
   * dell'account è attivo, manca il pagamento di un posto nuovo. Il banner globale direbbe «riattiva
   * l'abbonamento o esporta i dati», che è falso e nasconde la cosa vera da fare (controllare il metodo di
   * pagamento). Il rifiuto si legge sul modulo dell'invito, col motivo del fornitore accanto.
   */
  it('non alza il banner per l’addebito del posto rifiutato (UC 0103)', () => {
    expect(
      enforcementFromError(
        new ApiError(402, {
          type: 'urn:appgrove:seats:charge-declined',
          title: 'Payment Required',
          detail: 'carta scaduta',
        }),
      ),
    ).toBeNull()
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
