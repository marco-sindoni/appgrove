import { describe, expect, it } from 'vitest'
import type { CatalogApp } from '../../catalog/catalogApi'
import {
  QUOTA_WARNING_PERCENT,
  activeApps,
  buildAlerts,
  nextRenewal,
  quotaBar,
} from './dashboardModel'

const app = (over: Partial<CatalogApp>): CatalogApp => ({ appSlug: 'demo', name: 'Demo', ...over })

describe('buildAlerts', () => {
  it('non dice nulla quando non c’è nulla da fare', () => {
    expect(buildAlerts({ apps: [app({ state: 'active' })], twoFaEnabled: true })).toEqual([])
  })

  it('ordina per gravità: prima il pagamento in sospeso, poi il secondo fattore', () => {
    const alerts = buildAlerts({
      apps: [app({ state: 'active' }), app({ appSlug: 'crm', name: 'Mini-CRM', state: 'payment_pending' })],
      twoFaEnabled: false,
    })
    expect(alerts.map((a) => a.kind)).toEqual(['payment', 'twofa'])
    expect(alerts[0]).toMatchObject({ tone: 'danger', appName: 'Mini-CRM' })
    expect(alerts[1]?.tone).toBe('warning')
  })

  it('tace sul secondo fattore finché lo stato non è noto: un errore di rete non è un rimprovero', () => {
    expect(buildAlerts({ apps: [], twoFaEnabled: undefined })).toEqual([])
  })

  it('segnala un solo pagamento in sospeso anche con più app in sofferenza', () => {
    const alerts = buildAlerts({
      apps: [
        app({ appSlug: 'a', name: 'A', state: 'payment_pending' }),
        app({ appSlug: 'b', name: 'B', state: 'payment_pending' }),
      ],
      twoFaEnabled: true,
    })
    expect(alerts).toHaveLength(1)
    expect(alerts[0]?.appName).toBe('A')
  })
})

describe('quotaBar', () => {
  it('deriva la percentuale dal tetto', () => {
    expect(quotaBar(5, 20)).toMatchObject({ percent: 25, warning: false, unlimited: false })
  })

  it('passa in avviso alla soglia, non prima', () => {
    expect(quotaBar(79, 100).warning).toBe(false)
    expect(quotaBar(QUOTA_WARNING_PERCENT, 100).warning).toBe(true)
  })

  it('senza tetto non c’è barra da riempire', () => {
    expect(quotaBar(42, null)).toMatchObject({ used: 42, limit: null, percent: 0, unlimited: true })
    expect(quotaBar(42, undefined).unlimited).toBe(true)
  })

  it('tetto zero significa tetto raggiunto, non tetto assente', () => {
    expect(quotaBar(0, 0)).toMatchObject({ percent: 100, warning: true, unlimited: false })
  })

  it('un consumo oltre il tetto riempie la barra senza uscirne', () => {
    expect(quotaBar(30, 20).percent).toBe(100)
  })

  it('un consumo assurdo non produce una percentuale assurda', () => {
    expect(quotaBar(Number.NaN, 10).percent).toBe(0)
    expect(quotaBar(-3, 10).used).toBe(0)
  })
})

describe('activeApps', () => {
  it('tiene solo ciò che è davvero in uso: attivo o in prova', () => {
    const apps = [
      app({ appSlug: 'a', state: 'active' }),
      app({ appSlug: 'b', state: 'trial' }),
      app({ appSlug: 'c', state: 'available' }),
      app({ appSlug: 'd', state: 'payment_pending' }),
      app({ appSlug: 'e', state: 'disabled_by_platform' }),
    ]
    expect(activeApps(apps).map((a) => a.appSlug)).toEqual(['a', 'b'])
  })

  it('senza vetrina non inventa nulla', () => {
    expect(activeApps(undefined)).toEqual([])
  })
})

describe('nextRenewal', () => {
  it('prende la scadenza più vicina fra gli abbonamenti vivi', () => {
    expect(
      nextRenewal([
        { phase: 'ACTIVE', currentPeriodEnd: '2027-03-01T00:00:00Z' },
        { phase: 'TRIAL', currentPeriodEnd: '2026-09-01T00:00:00Z' },
      ]),
    ).toBe('2026-09-01T00:00:00Z')
  })

  it('ignora gli abbonamenti finiti: la loro data è passata', () => {
    expect(
      nextRenewal([
        { phase: 'ENDED', currentPeriodEnd: '2020-01-01T00:00:00Z' },
        { phase: 'ACTIVE', currentPeriodEnd: '2027-01-01T00:00:00Z' },
      ]),
    ).toBe('2027-01-01T00:00:00Z')
  })

  it('scarta le date non leggibili invece di mostrarle', () => {
    expect(nextRenewal([{ phase: 'ACTIVE', currentPeriodEnd: 'non-una-data' }])).toBeNull()
  })

  it('senza abbonamenti non c’è rinnovo', () => {
    expect(nextRenewal([])).toBeNull()
    expect(nextRenewal(undefined)).toBeNull()
  })
})
