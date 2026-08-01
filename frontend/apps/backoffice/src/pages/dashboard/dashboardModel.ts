/**
 * Logica **pura** della Dashboard operativa (UC 0097): quali avvisi mostrare e in che ordine, come si
 * deriva la barra di consumo, quali app sono davvero in uso, quando cade il prossimo rinnovo.
 *
 * <p>Sta qui e non dentro il componente perché è la parte che può sbagliare in silenzio: una soglia
 * fuori posto o un avviso mostrato a chi non c'entra si vedono solo guardando la pagina al momento
 * giusto. Funzioni pure → provate a tavolino, senza React e senza rete.
 */

import type { CatalogApp } from '../../catalog/catalogApi'

// ── Avvisi azionabili ────────────────────────────────────────────────────────

/** Le cose che possono richiedere un'azione dalla pagina d'atterraggio. */
export type DashboardAlertKind = 'payment' | 'twofa'

/** Un avviso pronto da rendere: che cos'è, quanto è serio, e con quale dato si scrive il testo. */
export interface DashboardAlert {
  kind: DashboardAlertKind
  tone: 'danger' | 'warning' | 'info'
  /** Nome dell'app in sofferenza — solo per l'avviso di pagamento. */
  appName?: string
}

/** Ingressi degli avvisi: ciascuno da una fonte diversa, ciascuno facoltativo. */
export interface AlertInputs {
  /** Vetrina del catalogo: da qui si sa se un'app ha un pagamento in sospeso. */
  apps?: CatalogApp[]
  /**
   * Stato del secondo fattore. `undefined` = non ancora noto (lettura in corso o fallita): in quel
   * caso **non** si avvisa. Rimproverare chi ha già attivato il secondo fattore, per colpa di un
   * errore di rete, è il modo più rapido di far ignorare gli avvisi.
   */
  twoFaEnabled?: boolean
}

/**
 * Compone gli avvisi da mostrare, **ordinati per gravità**: prima ciò che fa perdere l'accesso
 * (pagamento in sospeso), poi ciò che espone il conto (secondo fattore mancante). Nessun avviso viene
 * emesso "per completezza": se non c'è nulla da fare la lista è vuota, altrimenti l'utente impara a
 * saltarla.
 *
 * <p>I documenti legali **non** sono qui di proposito: quelli vincolanti sostituiscono l'intera shell
 * prima che la Dashboard venga resa (UC 0056), e gli aggiornamenti minori hanno già il loro avviso
 * dedicato nel guscio — ripeterli qui direbbe la stessa cosa due volte, con meno strumenti.
 */
export function buildAlerts(input: AlertInputs): DashboardAlert[] {
  const alerts: DashboardAlert[] = []

  const pending = (input.apps ?? []).find((app) => app.state === 'payment_pending')
  if (pending) {
    alerts.push({ kind: 'payment', tone: 'danger', appName: pending.name ?? pending.appSlug ?? '' })
  }
  if (input.twoFaEnabled === false) {
    alerts.push({ kind: 'twofa', tone: 'warning' })
  }
  return alerts
}

// ── Barra di consumo ─────────────────────────────────────────────────────────

/** Oltre questa percentuale la barra passa in avviso: si è vicini al tetto, non ancora fermi. */
export const QUOTA_WARNING_PERCENT = 80

/** Barra pronta da rendere: quanto, su quanto, che percentuale e se è il caso di preoccuparsi. */
export interface QuotaBar {
  used: number
  /** `null` = nessun tetto applicato: si mostra il consumo, non una barra che non finisce mai. */
  limit: number | null
  percent: number
  warning: boolean
  unlimited: boolean
}

/**
 * Deriva la barra dal consumo grezzo del servizio dell'app. Tre casi che vanno tenuti distinti:
 * nessun tetto (illimitato), tetto **zero** (fascia senza diritto: il tetto è raggiunto, non assente)
 * e tetto normale. La percentuale è tagliata a 100: un consumo oltre il tetto — possibile dopo un
 * cambio di fascia verso il basso — deve mostrare una barra piena, non una barra che esce dalla card.
 */
export function quotaBar(used: number, limit: number | null | undefined): QuotaBar {
  const safeUsed = Number.isFinite(used) && used > 0 ? used : 0
  if (limit == null) {
    return { used: safeUsed, limit: null, percent: 0, warning: false, unlimited: true }
  }
  const percent = limit <= 0 ? 100 : Math.min(100, Math.round((safeUsed / limit) * 100))
  return {
    used: safeUsed,
    limit,
    percent,
    warning: percent >= QUOTA_WARNING_PERCENT,
    unlimited: false,
  }
}

// ── App attive e rinnovo ─────────────────────────────────────────────────────

/**
 * Le app **in uso** nel workspace: attive o in prova. Sono le uniche che meritano una card operativa —
 * un'app con la disdetta programmata resta usabile ma non è ciò di cui la panoramica deve parlare, e
 * un'app spenta dalla piattaforma o con il pagamento in sospeso ha già il suo avviso o il suo posto in
 * Billing. La vetrina (UC 0095) resta il luogo dove si vedono **tutti** gli stati.
 */
export function activeApps(apps: CatalogApp[] | undefined): CatalogApp[] {
  return (apps ?? []).filter((app) => app.state === 'active' || app.state === 'trial')
}

/** Forma minima di un abbonamento usata qui: si guarda solo la fine del periodo e la fase. */
export interface RenewalSummary {
  phase?: string | null
  currentPeriodEnd?: string | null
}

/**
 * Il **prossimo rinnovo** del workspace: la scadenza più vicina fra gli abbonamenti ancora vivi.
 * Gli abbonamenti finiti sono esclusi — la loro data è nel passato e mostrarla come "prossimo rinnovo"
 * sarebbe semplicemente falso. `null` quando non c'è nulla da rinnovare. Funzione pura.
 */
export function nextRenewal(subscriptions: RenewalSummary[] | undefined): string | null {
  const dates = (subscriptions ?? [])
    .filter((s) => s.phase !== 'ENDED')
    .map((s) => s.currentPeriodEnd)
    .filter((d): d is string => typeof d === 'string' && d.length > 0)
    .filter((d) => !Number.isNaN(new Date(d).getTime()))
    .sort()
  return dates[0] ?? null
}
