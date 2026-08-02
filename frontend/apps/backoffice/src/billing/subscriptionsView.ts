/**
 * Logica **pura** della sezione abbonamenti (UC 0028, estesa da UC 0067): riga di stato, limiti del piano,
 * **consumo della quota**, opzioni di cambio piano e riepilogo della conferma. Ritorna **descrittori i18n**
 * (chiave + parametri), tradotti dal componente → niente React/rete qui, testabile a tavolino.
 *
 * Sul consumo: il read-model espone solo l'uso **a giacenza** (posti occupati e simili, proiezione
 * `app_usage_stock`, UC 0054). Per le metriche **a finestra** core non conosce il consumo corrente, quindi
 * si continua a mostrare il solo tetto del piano invece di inventare un numero.
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
  status?: string | null
  scheduledTierKey?: string | null
  scheduledChangeAt?: string | null
  cancelAt?: string | null
  currentPeriodEnd?: string | null
  limits?: Record<string, MetricLimit> | null
  usage?: Record<string, number> | null
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

/** Sopra questa frazione del tetto si avvisa: il limite non è ancora raggiunto ma sta per esserlo. */
export const QUOTA_WARN_RATIO = 0.8

/** Consumo di una metrica misurata: quanto è usato del tetto, e quanto la cosa è seria. */
export interface QuotaUsage {
  metric: string
  used: number
  cap: number
  /** Frazione del tetto occupata, limitata a 1 per la barra. */
  ratio: number
  level: 'ok' | 'warn' | 'full'
}

/**
 * Consumo per metrica, per le sole metriche di cui **conosciamo** l'uso corrente e che hanno un tetto
 * definito. Un tetto illimitato (`cap < 0`) non produce nulla: non c'è una barra da riempire, e una barra
 * al 3% su "illimitato" è più fuorviante che informativa.
 */
export function quotaUsages(
  limits: Record<string, MetricLimit> | null | undefined,
  usage: Record<string, number> | null | undefined,
): QuotaUsage[] {
  return Object.entries(limits ?? {})
    .filter(([metric, l]) => typeof l.cap === 'number' && l.cap > 0 && typeof usage?.[metric] === 'number')
    .map(([metric, l]) => {
      const cap = l.cap as number
      const used = (usage as Record<string, number>)[metric]
      const ratio = Math.min(1, used / cap)
      const level: QuotaUsage['level'] = used >= cap ? 'full' : ratio >= QUOTA_WARN_RATIO ? 'warn' : 'ok'
      return { metric, used, cap, ratio, level }
    })
}

/** Direzione di un cambio piano, dedotta dal prezzo come fa il backend (importo minore = riduzione). */
export type ChangeDirection = 'upgrade' | 'downgrade' | 'same'

export function directionFor(currentAmount: number, targetAmount: number): ChangeDirection {
  if (targetAmount < currentAmount) return 'downgrade'
  return targetAmount > currentAmount ? 'upgrade' : 'same'
}

/**
 * Riepilogo della conferma: cosa succede e **da quando**. Un aumento si applica subito con addebito
 * proporzionale; una riduzione parte a fine periodo e fino a lì non cambia nulla. Dirlo prima di inviare
 * il comando è il punto di UC 0067 §6: la sorpresa sull'addebito è il motivo per cui si apre un ticket.
 */
export function changeSummary(
  direction: ChangeDirection,
  tierName: string,
  periodEnd: string | null | undefined,
  locale = 'it-IT',
): I18nLine {
  if (direction === 'downgrade') {
    return {
      key: 'subscriptions.confirmDowngradeBody',
      params: { tier: tierName, date: formatDate(periodEnd, locale) },
    }
  }
  return { key: 'subscriptions.confirmUpgradeBody', params: { tier: tierName } }
}

/** Un piano come lo mostra la finestra "cambia piano", già giudicato: attuale, consigliato, bloccato. */
export interface TierOption {
  key: string
  name: string
  /** Importo nel ciclo scelto, in centesimi; `null` se il piano non ha prezzo per quel ciclo. */
  amount: number | null
  currency: string | null
  limits: I18nLine[]
  isCurrent: boolean
  isRecommended: boolean
  /** Spiegazione del rifiuto se il piano non è ammissibile adesso, altrimenti `null`. */
  blockedReason: string | null
  direction: ChangeDirection
}

/** Forma minima di un piano come lo serve `GET /checkout/apps/{slug}/tiers`. */
export interface TierInput {
  key?: string | null
  name?: string | null
  limits?: Record<string, unknown> | null
  prices?: { billingCycle?: string | null; amount?: number | null; currency?: string | null }[] | null
}

function amountIn(tier: TierInput, cycle: string): { amount: number | null; currency: string | null } {
  const price = tier.prices?.find((p) => p.billingCycle === cycle)
  return { amount: price?.amount ?? null, currency: price?.currency ?? null }
}

/**
 * Piani ordinati per prezzo crescente, ciascuno con il proprio giudizio.
 *
 * Il **consigliato** è il primo piano di prezzo superiore a quello attuale: è un'indicazione derivata, non
 * un dato di catalogo — quale piano spingere sarebbe una scelta commerciale, e il catalogo non la esprime.
 * I **bloccati** arrivano dal backend (`blockedTiers`), che li calcola con la stessa regola che rifiuta il
 * comando: qui non si rivaluta nulla, si mostra.
 */
export function tierOptions(
  tiers: TierInput[] | null | undefined,
  currentTierKey: string | null | undefined,
  cycle: string,
  blockedTiers: Record<string, string> | null | undefined,
): TierOption[] {
  const rows = (tiers ?? [])
    .filter((tier): tier is TierInput & { key: string } => !!tier.key)
    .map((tier) => {
      const { amount, currency } = amountIn(tier, cycle)
      return { tier, key: tier.key, amount, currency }
    })
    .sort((a, b) => (a.amount ?? 0) - (b.amount ?? 0))

  const currentAmount = rows.find((r) => r.key === currentTierKey)?.amount ?? 0
  const recommendedKey = rows.find((r) => (r.amount ?? 0) > currentAmount)?.key ?? null

  return rows.map(({ tier, key, amount, currency }) => ({
    key,
    name: tier.name ?? key,
    amount,
    currency,
    limits: limitDescriptors(tierLimits(tier.limits)),
    isCurrent: key === currentTierKey,
    isRecommended: key === recommendedKey,
    blockedReason: blockedTiers?.[key] ?? null,
    direction: directionFor(currentAmount, amount ?? 0),
  }))
}

/** Ogni quanto si rilegge la lista mentre un comando è in corso di riconciliazione. */
export const PENDING_POLL_MS = 1500
/** Dopo questo tempo si smette di insistere: il webhook tarda più del ragionevole. */
export const PENDING_TIMEOUT_MS = 30_000

/** Fotografia dei campi che un comando fa cambiare, presa nell'istante in cui il comando parte. */
export interface PendingSnapshot {
  tierKey: string | null
  scheduledTierKey: string | null
  cancelAt: string | null
  since: number
}

export function snapshotOf(sub: SubscriptionSummary & { tierKey?: string | null }, now = Date.now()): PendingSnapshot {
  return {
    tierKey: sub.tierKey ?? null,
    scheduledTierKey: sub.scheduledTierKey ?? null,
    cancelAt: sub.cancelAt ?? null,
    since: now,
  }
}

/**
 * Vero finché il read-model **non** riflette ancora il comando inviato.
 *
 * Il modello è comando → fornitore → webhook → read-model: la riga non viene scritta dalla richiesta, e
 * dichiarare il successo prima che il webhook sia arrivato significherebbe mostrare un dato che non esiste
 * (UC 0067 §5). Si confronta con la fotografia presa alla partenza — un cambio di piano, di riduzione
 * programmata o di disdetta è il segno che la riconciliazione è avvenuta — e si smette comunque dopo
 * {@link PENDING_TIMEOUT_MS}, perché insistere per sempre è un modo elegante di mentire.
 */
export function isStillPending(
  snap: PendingSnapshot,
  sub: (SubscriptionSummary & { tierKey?: string | null }) | undefined,
  now = Date.now(),
): boolean {
  if (now - snap.since >= PENDING_TIMEOUT_MS) return false
  if (!sub) return false
  return (
    (sub.tierKey ?? null) === snap.tierKey &&
    (sub.scheduledTierKey ?? null) === snap.scheduledTierKey &&
    (sub.cancelAt ?? null) === snap.cancelAt
  )
}

/**
 * I limiti del catalogo arrivano nella forma grezza `{metric, cap, type, window}` (una metrica per piano),
 * mentre il read-model degli abbonamenti li serve già come `metrica → tetto`. Riportiamo la prima alla
 * seconda per riusare un solo formattatore.
 */
function tierLimits(raw: Record<string, unknown> | null | undefined): Record<string, MetricLimit> {
  const metric = raw?.metric
  if (typeof metric !== 'string') return {}
  return {
    [metric]: {
      cap: typeof raw?.cap === 'number' ? raw.cap : undefined,
      nature: typeof raw?.type === 'string' ? raw.type : null,
      window: typeof raw?.window === 'string' ? raw.window : null,
    },
  }
}
