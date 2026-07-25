// ─────────────────────────────────────────────────────────────────────────────
// lib/fee.mjs — calcolo della FEE EFFETTIVA del fornitore di pagamento (#09 K46/K47).
//
// È l'«arma principale» del co-pilota prezzi: prima di confermare un prezzo mostra
// quanto se ne va in commissioni e quanto resta netto, con un AVVISO SOFT (non un
// blocco) quando la fee effettiva supera ~10%. Su prezzi bassi e mensili la parte
// FISSA per transazione domina; l'annuale (una sola transazione l'anno) la abbatte.
//
// Modello (Paddle Merchant of Record, #09 K46):
//   • parte percentuale ~5% dell'importo;
//   • parte fissa ~$0,50 per transazione, convertita in euro con un tasso ASSUNTO
//     e documentato (default 1,0 → $0,50 ≈ €0,50). La fee è un segnale-guida SOFT,
//     non contabilità: non si interroga un cambio valuta in tempo reale, così il
//     calcolo resta deterministico e testabile. Il tasso 1,0 è prudenziale
//     (sovrastima lievemente la fee, dato che l'euro vale un po' più del dollaro) e
//     riproduce esattamente gli esempi di #09 K46: €5/mese → ~15%, €50/anno → ~6%.
//
// Funzione PURA (nessun I/O): input in minor units (centesimi), output in centesimi
// + percentuale. Il chiamante (change.mjs) formatta.
// ─────────────────────────────────────────────────────────────────────────────

/** Parte percentuale del fornitore di pagamento (5%). */
export const PROVIDER_PCT = 0.05

/** Parte fissa per transazione, in centesimi di dollaro ($0,50). */
export const FIXED_USD_CENTS = 50

/** Tasso di cambio ASSUNTO dollaro→euro (default 1,0: $0,50 ≈ €0,50; prudenziale e documentato). */
export const DEFAULT_USD_TO_EUR = 1.0

/** Soglia dell'avviso soft sulla fee effettiva (10%). Oltre = warning, mai blocco (#09 K47). */
export const WARN_THRESHOLD_PCT = 10

/**
 * Calcola la fee effettiva e il netto per un singolo importo (una transazione).
 *
 * @param {object} p
 * @param {number} p.amountMinor  importo in centesimi (intero positivo)
 * @param {number} [p.fxUsdToEur] tasso dollaro→euro (default {@link DEFAULT_USD_TO_EUR})
 * @returns {{amountMinor:number, feeMinor:number, netMinor:number, effectivePct:number, warn:boolean, fixedMinor:number, pctMinor:number}}
 */
export function computeFee({ amountMinor, fxUsdToEur = DEFAULT_USD_TO_EUR } = {}) {
  if (!Number.isInteger(amountMinor) || amountMinor <= 0) {
    throw new Error(`importo non valido: ${amountMinor} (atteso intero positivo in centesimi)`)
  }
  const pctMinor = Math.round(amountMinor * PROVIDER_PCT)
  const fixedMinor = Math.round(FIXED_USD_CENTS * fxUsdToEur)
  const feeMinor = pctMinor + fixedMinor
  const netMinor = amountMinor - feeMinor
  const effectivePct = (feeMinor / amountMinor) * 100
  return {
    amountMinor,
    feeMinor,
    netMinor,
    effectivePct,
    warn: effectivePct > WARN_THRESHOLD_PCT,
    fixedMinor,
    pctMinor,
  }
}

/** Formatta centesimi in euro (es. 1900 → "€19,00"). Solo per output leggibile. */
export function formatEur(minor) {
  const sign = minor < 0 ? '-' : ''
  const abs = Math.abs(minor)
  return `${sign}€${Math.floor(abs / 100)},${String(abs % 100).padStart(2, '0')}`
}
