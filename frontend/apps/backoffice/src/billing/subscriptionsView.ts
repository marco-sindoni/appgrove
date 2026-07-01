/**
 * Logica **pura** del pannello self-service (UC 0028): riga di stato dell'abbonamento e descrizione dei
 * limiti del piano. Ritorna **descrittori i18n** (chiave + parametri), tradotti dal componente → niente
 * React/rete qui, testabile a tavolino. Il consumo quota in tempo reale è differito (vedi 0028 §Punti aperti):
 * qui si mostrano i **limiti del piano**, non l'uso corrente.
 */

/** Tetto di una metrica come esposto dal read-model (`metric → {cap, nature, window}`). */
export interface MetricLimit {
  cap?: number
  nature?: string | null
  window?: string | null
}

/** Sottoinsieme del read-model `/me/subscriptions` usato dagli helper puri. */
export interface SubscriptionSummary {
  phase?: string | null
  scheduledTierKey?: string | null
  scheduledChangeAt?: string | null
  cancelAt?: string | null
  currentPeriodEnd?: string | null
  limits?: Record<string, MetricLimit> | null
}

/** Descrittore i18n: chiave del catalogo + parametri di interpolazione. */
export interface I18nLine {
  key: string
  params?: Record<string, unknown>
}

/** Formatta una data ISO come data locale breve; stringa vuota se assente/invalida. */
export function formatDate(iso: string | null | undefined, locale = 'it-IT'): string {
  if (!iso) return ''
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? '' : d.toLocaleDateString(locale)
}

/**
 * Riga di stato principale, in ordine di priorità: scaduto → downgrade programmato → disdetta programmata →
 * rinnovo. `null` se non c'è nulla di rilevante da dire.
 */
export function statusLine(sub: SubscriptionSummary, locale = 'it-IT'): I18nLine | null {
  if (sub.phase === 'ENDED') return { key: 'subscriptions.expired' }
  if (sub.scheduledTierKey && sub.scheduledChangeAt) {
    return {
      key: 'subscriptions.scheduledDowngrade',
      params: { tier: sub.scheduledTierKey, date: formatDate(sub.scheduledChangeAt, locale) },
    }
  }
  if (sub.cancelAt) {
    return { key: 'subscriptions.cancelAt', params: { date: formatDate(sub.cancelAt, locale) } }
  }
  if (sub.currentPeriodEnd) {
    return { key: 'subscriptions.periodEnd', params: { date: formatDate(sub.currentPeriodEnd, locale) } }
  }
  return null
}

/** Descrittori dei limiti del piano (uno per metrica); flow → "N/finestra", stock → "fino a N". */
export function limitDescriptors(limits: Record<string, MetricLimit> | null | undefined): I18nLine[] {
  return Object.entries(limits ?? {})
    .filter(([, l]) => typeof l.cap === 'number' && l.cap >= 0)
    .map(([metric, l]) =>
      l.nature === 'flow'
        ? { key: 'subscriptions.limitFlow', params: { cap: l.cap, metric, window: l.window ?? '' } }
        : { key: 'subscriptions.limitStock', params: { cap: l.cap, metric } },
    )
}
