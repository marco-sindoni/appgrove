import { describe, it, expect } from 'vitest'
import {
  REASSURE_AFTER_MS,
  annualFreeMonths,
  formatPrice,
  phaseFromPoll,
  shouldPoll,
  shouldReassure,
} from './checkoutMachine'

describe('checkout — macchina a stati del polling (UC 0024)', () => {
  it('resta in activating finché la subscription non è active', () => {
    expect(phaseFromPoll('activating', { active: false })).toBe('activating')
    expect(phaseFromPoll('activating', undefined)).toBe('activating')
  })

  it('passa ad active quando il polling vede active', () => {
    expect(phaseFromPoll('activating', { active: true })).toBe('active')
  })

  it('il ritardo del webhook non è mai un errore', () => {
    // anche dopo molto tempo senza active, la fase resta activating (mai error)
    expect(phaseFromPoll('activating', { active: false })).not.toBe('error')
  })

  it('non cambia fase se non si sta attivando (idempotente fuori da activating)', () => {
    expect(phaseFromPoll('idle', { active: true })).toBe('idle')
    expect(phaseFromPoll('submitting', { active: true })).toBe('submitting')
    expect(phaseFromPoll('active', { active: false })).toBe('active')
  })

  it('fa polling solo durante activating', () => {
    expect(shouldPoll('activating')).toBe(true)
    expect(shouldPoll('idle')).toBe(false)
    expect(shouldPoll('submitting')).toBe(false)
    expect(shouldPoll('active')).toBe(false)
  })

  it('mostra il messaggio rassicurante solo oltre la soglia, e solo in activating', () => {
    const since = 1_000
    expect(shouldReassure('activating', since, since + REASSURE_AFTER_MS - 1)).toBe(false)
    expect(shouldReassure('activating', since, since + REASSURE_AFTER_MS)).toBe(true)
    // fuori da activating non rassicura mai
    expect(shouldReassure('active', since, since + REASSURE_AFTER_MS * 2)).toBe(false)
    expect(shouldReassure('activating', null, since + REASSURE_AFTER_MS * 2)).toBe(false)
  })
})

describe('checkout — pricing', () => {
  it('calcola i mesi gratis dell’annuale (sconto esplicito)', () => {
    expect(annualFreeMonths(900, 9000)).toBe(2) // 12 - 9000/900 = 2
    expect(annualFreeMonths(1000, 12000)).toBe(0) // nessun risparmio
    expect(annualFreeMonths(null, 9000)).toBe(0)
    expect(annualFreeMonths(900, null)).toBe(0)
    expect(annualFreeMonths(0, 9000)).toBe(0)
  })

  it('formatta gli importi in minor units come valuta', () => {
    expect(formatPrice(900, 'EUR', 'en-US')).toBe('€9.00')
    expect(formatPrice(9000, 'EUR', 'en-US')).toBe('€90.00')
  })
})
