import { describe, expect, it } from 'vitest'
import { formatDate, limitDescriptors, statusLine } from './subscriptionsView'

describe('statusLine', () => {
  it('scaduto ha priorità massima', () => {
    expect(statusLine({ phase: 'ENDED', currentPeriodEnd: '2027-01-01T00:00:00Z' })).toEqual({
      key: 'subscriptions.expired',
    })
  })

  it('mostra il downgrade programmato quando presente', () => {
    const line = statusLine({
      phase: 'ACTIVE',
      scheduledTierKey: 'basic',
      scheduledChangeAt: '2027-01-15T00:00:00Z',
      currentPeriodEnd: '2027-01-15T00:00:00Z',
    })
    expect(line?.key).toBe('subscriptions.scheduledDowngrade')
    expect(line?.params?.tier).toBe('basic')
  })

  it('mostra la disdetta programmata quando non c’è un downgrade', () => {
    const line = statusLine({ phase: 'CANCELING', cancelAt: '2027-02-01T00:00:00Z' })
    expect(line?.key).toBe('subscriptions.cancelAt')
  })

  it('altrimenti mostra il rinnovo di fine periodo', () => {
    expect(statusLine({ phase: 'ACTIVE', currentPeriodEnd: '2027-03-01T00:00:00Z' })?.key).toBe(
      'subscriptions.periodEnd',
    )
  })

  it('null se non c’è nulla da dire', () => {
    expect(statusLine({ phase: 'ACTIVE' })).toBeNull()
  })
})

describe('limitDescriptors', () => {
  it('flow → chiave con finestra, stock → chiave senza finestra', () => {
    const lines = limitDescriptors({
      invoices: { cap: 100, nature: 'flow', window: 'month' },
      projects: { cap: 3, nature: 'stock', window: null },
    })
    expect(lines).toContainEqual({
      key: 'subscriptions.limitFlow',
      params: { cap: 100, metric: 'invoices', window: 'month' },
    })
    expect(lines).toContainEqual({
      key: 'subscriptions.limitStock',
      params: { cap: 3, metric: 'projects' },
    })
  })

  it('scarta i cap negativi (nessun limite) e le mappe vuote', () => {
    expect(limitDescriptors({ x: { cap: -1, nature: 'stock' } })).toEqual([])
    expect(limitDescriptors(null)).toEqual([])
  })
})

describe('formatDate', () => {
  it('stringa vuota se assente o invalida', () => {
    expect(formatDate(null)).toBe('')
    expect(formatDate('non-una-data')).toBe('')
  })
})
