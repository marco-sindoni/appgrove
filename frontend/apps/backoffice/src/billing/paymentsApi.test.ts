import { describe, it, expect } from 'vitest'
import { describePayment, type Payment } from './paymentsApi'

const cycle = (c: string) => (c === 'monthly' ? 'mensile' : c)

const row = (over: Partial<Payment> = {}): Payment => ({
  billedAt: '2026-07-31T10:00:00Z',
  appSlug: 'notes',
  appName: 'Notes',
  planName: 'Notes Pro',
  billingCycle: 'monthly',
  amount: 900,
  currency: 'EUR',
  status: 'paid',
  ...over,
})

describe('descrizione di una riga di storico (UC 0096)', () => {
  it('unisce fascia e ciclo, come nel riferimento visivo approvato', () => {
    expect(describePayment(row(), cycle)).toBe('Notes Pro — mensile')
  })

  it('senza la fascia resta il solo ciclo (una fascia tolta dal listino non cancella il pagamento)', () => {
    expect(describePayment(row({ planName: undefined }), cycle)).toBe('mensile')
  })

  it('senza fascia né ciclo la cella resta vuota invece di inventare una didascalia', () => {
    expect(describePayment(row({ planName: undefined, billingCycle: undefined }), cycle)).toBe('')
  })

  it('un ciclo fuori catalogo si mostra così com’è, senza spegnere la riga', () => {
    expect(describePayment(row({ billingCycle: 'year' }), cycle)).toBe('Notes Pro — year')
  })
})
