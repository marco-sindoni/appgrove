/**
 * Logica **pura** della UX post-checkout (UC 0024): fasi, soglia "rassicurante", mappatura del polling
 * stato → fase, e helper di pricing (sconto annuale, formattazione). Niente React/rete qui → testabile a
 * tavolino. L'orchestrazione (react-query, overlay) vive in `Billing.tsx`/`checkoutApi.ts`.
 */

/** Fasi della schermata: scelta → invio → attivazione (polling) → attivo; oppure errore del nostro backend. */
export type CheckoutPhase = 'idle' | 'submitting' | 'activating' | 'active' | 'error'

/** Oltre questa soglia in `activating` mostriamo il messaggio rassicurante (mai un errore) (#09 C17). */
export const REASSURE_AFTER_MS = 30_000

/** Intervallo di polling dello stato subscription (#09 C17: ogni 1–2s). */
export const POLL_INTERVAL_MS = 1_500

/** Vero se siamo in attivazione da oltre la soglia → messaggio rassicurante. */
export function shouldReassure(
  phase: CheckoutPhase,
  activatingSinceMs: number | null,
  nowMs: number,
): boolean {
  return phase === 'activating' && activatingSinceMs != null && nowMs - activatingSinceMs >= REASSURE_AFTER_MS
}

/**
 * Mappa lo stato del polling alla fase: se la subscription è `active` → `active` (fine polling), altrimenti
 * si resta in `activating`. Il ritardo del webhook **non** è un errore (#09 C17): non porta mai a `error`.
 */
export function phaseFromPoll(
  current: CheckoutPhase,
  poll: { active?: boolean } | undefined,
): CheckoutPhase {
  if (current !== 'activating') return current
  return poll?.active ? 'active' : 'activating'
}

/** Vero finché bisogna continuare a fare polling dello stato (in attivazione e non ancora attivo). */
export function shouldPoll(phase: CheckoutPhase): boolean {
  return phase === 'activating'
}

// ── pricing ──────────────────────────────────────────────────────────────────

/**
 * Mesi "gratis" dell'annuale rispetto a 12× il mensile (sconto esplicito, #09 A2/K49). Es. annuale = 10×
 * mensile → 2 mesi gratis. 0 se non c'è risparmio o mancano i due prezzi.
 */
export function annualFreeMonths(monthlyMinor: number | null, annualMinor: number | null): number {
  if (monthlyMinor == null || annualMinor == null || monthlyMinor <= 0) return 0
  const months = 12 - annualMinor / monthlyMinor
  const rounded = Math.round(months)
  return rounded > 0 ? rounded : 0
}

/** Formatta un importo in minor units come valuta (es. 900, EUR → "€9.00"/"9,00 €" secondo locale). */
export function formatPrice(amountMinor: number, currency: string, locale = 'it-IT'): string {
  return new Intl.NumberFormat(locale, { style: 'currency', currency }).format(amountMinor / 100)
}
