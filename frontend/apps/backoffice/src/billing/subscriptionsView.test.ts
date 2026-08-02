import { describe, expect, it } from 'vitest'
import {
  changeSummary,
  directionFor,
  formatDate,
  isStillPending,
  limitDescriptors,
  quotaUsages,
  snapshotOf,
  statusLine,
  tierOptions,
  PENDING_TIMEOUT_MS,
} from './subscriptionsView'

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

describe('quotaUsages (UC 0067)', () => {
  const limits = {
    seats: { cap: 10, nature: 'stock' },
    invoices: { cap: 50, nature: 'flow', window: 'month' },
    unlimited: { cap: -1, nature: 'stock' },
  }

  it('rende il consumo solo per le metriche di cui conosciamo l’uso', () => {
    // di `invoices` non arriva alcun uso (metrica a finestra): resta fuori invece di mostrare zero
    expect(quotaUsages(limits, { seats: 8 })).toEqual([
      { metric: 'seats', used: 8, cap: 10, ratio: 0.8, level: 'warn' },
    ])
  })

  it('classifica ok / vicino al limite / limite raggiunto', () => {
    expect(quotaUsages(limits, { seats: 3 })[0].level).toBe('ok')
    expect(quotaUsages(limits, { seats: 9 })[0].level).toBe('warn')
    expect(quotaUsages(limits, { seats: 10 })[0].level).toBe('full')
    expect(quotaUsages(limits, { seats: 12 })[0].level).toBe('full')
  })

  it('un uso oltre il tetto non fa uscire la barra dal suo contenitore', () => {
    expect(quotaUsages(limits, { seats: 40 })[0].ratio).toBe(1)
  })

  it('una metrica illimitata non produce alcuna barra', () => {
    expect(quotaUsages(limits, { unlimited: 3 })).toEqual([])
  })

  it('nessun uso noto, nessun consumo da mostrare', () => {
    expect(quotaUsages(limits, {})).toEqual([])
    expect(quotaUsages(null, null)).toEqual([])
  })
})

describe('tierOptions (UC 0067)', () => {
  const tiers = [
    { key: 'free', name: 'Free', limits: { metric: 'seats', cap: 2, type: 'stock' }, prices: [] },
    {
      key: 'pro',
      name: 'Pro',
      limits: { metric: 'seats', cap: 10, type: 'stock' },
      prices: [
        { billingCycle: 'monthly', amount: 900, currency: 'EUR' },
        { billingCycle: 'annual', amount: 9000, currency: 'EUR' },
      ],
    },
    {
      key: 'team',
      name: 'Team',
      limits: { metric: 'seats', cap: 50, type: 'stock' },
      prices: [{ billingCycle: 'monthly', amount: 1900, currency: 'EUR' }],
    },
  ]

  it('ordina per prezzo, marca l’attuale e consiglia il primo superiore', () => {
    const options = tierOptions(tiers, 'pro', 'monthly', {})
    expect(options.map((o) => o.key)).toEqual(['free', 'pro', 'team'])
    expect(options.find((o) => o.isCurrent)?.key).toBe('pro')
    expect(options.find((o) => o.isRecommended)?.key).toBe('team')
  })

  it('sul piano più alto non c’è nulla da consigliare', () => {
    expect(tierOptions(tiers, 'team', 'monthly', {}).some((o) => o.isRecommended)).toBe(false)
  })

  it('riporta il prezzo del ciclo scelto', () => {
    expect(tierOptions(tiers, 'pro', 'annual', {}).find((o) => o.key === 'pro')?.amount).toBe(9000)
    // Team non ha prezzo annuale: nessun importo inventato per quel ciclo
    expect(tierOptions(tiers, 'pro', 'annual', {}).find((o) => o.key === 'team')?.amount).toBeNull()
  })

  it('mostra il divieto deciso dal backend, senza rivalutarlo', () => {
    const options = tierOptions(tiers, 'pro', 'monthly', { free: 'Hai 8 posti occupati.' })
    expect(options.find((o) => o.key === 'free')?.blockedReason).toBe('Hai 8 posti occupati.')
    expect(options.find((o) => o.key === 'team')?.blockedReason).toBeNull()
  })

  it('deduce la direzione dal prezzo, come fa il backend', () => {
    const options = tierOptions(tiers, 'pro', 'monthly', {})
    expect(options.find((o) => o.key === 'free')?.direction).toBe('downgrade')
    expect(options.find((o) => o.key === 'pro')?.direction).toBe('same')
    expect(options.find((o) => o.key === 'team')?.direction).toBe('upgrade')
  })

  it('porta con sé i limiti del piano, già formattati', () => {
    expect(tierOptions(tiers, 'pro', 'monthly', {}).find((o) => o.key === 'team')?.limits).toEqual([
      { key: 'subscriptions.limitStock', params: { cap: 50, metric: 'seats' } },
    ])
  })
})

describe('directionFor / changeSummary (UC 0067)', () => {
  it('prezzo minore = riduzione', () => {
    expect(directionFor(900, 0)).toBe('downgrade')
    expect(directionFor(900, 1900)).toBe('upgrade')
    expect(directionFor(900, 900)).toBe('same')
  })

  it('la riduzione dice da quando, l’aumento dice che è immediato', () => {
    expect(changeSummary('downgrade', 'Free', '2026-09-12T00:00:00Z', 'it-IT')).toEqual({
      key: 'subscriptions.confirmDowngradeBody',
      params: { tier: 'Free', date: '12/09/2026' },
    })
    expect(changeSummary('upgrade', 'Team', '2026-09-12T00:00:00Z')).toEqual({
      key: 'subscriptions.confirmUpgradeBody',
      params: { tier: 'Team' },
    })
  })
})

describe('isStillPending (UC 0067)', () => {
  const sub = { tierKey: 'pro', scheduledTierKey: null, cancelAt: null, currentPeriodEnd: null }
  const snap = snapshotOf(sub, 1_000)

  it('resta in attesa finché il read-model non riflette il comando', () => {
    expect(isStillPending(snap, sub, 1_500)).toBe(true)
  })

  it('si risolve quando cambia il piano, la riduzione programmata o la disdetta', () => {
    expect(isStillPending(snap, { ...sub, tierKey: 'team' }, 1_500)).toBe(false)
    expect(isStillPending(snap, { ...sub, scheduledTierKey: 'free' }, 1_500)).toBe(false)
    expect(isStillPending(snap, { ...sub, cancelAt: '2026-09-12T00:00:00Z' }, 1_500)).toBe(false)
  })

  it('smette comunque dopo il tempo massimo: insistere per sempre è mentire', () => {
    expect(isStillPending(snap, sub, 1_000 + PENDING_TIMEOUT_MS)).toBe(false)
  })

  it('un abbonamento sparito dalla lista non tiene in attesa', () => {
    expect(isStillPending(snap, undefined, 1_500)).toBe(false)
  })
})
