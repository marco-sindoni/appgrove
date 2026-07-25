// ─────────────────────────────────────────────────────────────────────────────
// lib/pricing.mjs — modifiche DETERMINISTICHE al pricing-as-code (UC 0047), sul
// contratto YAML CONGELATO dalla change 0019 (services/core/.../pricing/<slug>.yaml).
//
// Usa la Document API di `yaml` (parseDocument → modifica → toString), che PRESERVA
// i commenti: i listini portano header curati a mano (perché un'app è inactive, come
// si conta la metrica, …) e non vanno persi da una riserializzazione.
//
// Le funzioni sono PURE su un Document (nessun I/O): change.mjs le avvolge con fs.
// I guardrail lanciano: chi chiama (la skill) mostra l'errore e non scrive nulla.
//
// IMMUTABILITÀ (#09 H35/H37) — il punto delicato. L'identità di un prezzo nel catalogo
// è la tripla (slug, tier.key, billingCycle) → UUID deterministico SENZA versione
// (CatalogIds.priceId). Perciò un prezzo VIVO (già sincronizzato) non può cambiare
// importo: il motore di sync lo rifiuta. Le due vie:
//   • changePriceInPlace → per un prezzo NON ancora sincronizzato (nessun paddle_price_id):
//     l'importo si corregge sul posto. La liveness vive nel DB per-ambiente, NON nello
//     YAML: solo la skill/lo sviluppatore sa se il prezzo è ancora una bozza. Perciò la
//     scelta della via è ESPLICITA di chi chiama, non dedotta dal file.
//   • changePriceNewTier → per un prezzo VIVO: il nuovo prezzo si porta con un NUOVO tier
//     (nuova chiave stabile), clonando il vecchio; il vecchio tier resta DEFINITO per gli
//     abbonati esistenti (grandfathering). È la mappatura del «nuovo Price + archivia il
//     vecchio» sul modello appgrove dove (tier × ciclo) = un prezzo.
// ─────────────────────────────────────────────────────────────────────────────
import { parseDocument } from 'yaml'

export const BILLING_CYCLES = ['monthly', 'annual']

/** Parsa il testo YAML in un Document (commenti preservati). */
export function load(text) {
  const doc = parseDocument(text)
  if (doc.errors.length > 0) {
    throw new Error(`YAML non valido: ${doc.errors[0].message}`)
  }
  return doc
}

/** Riserializza il Document in testo YAML, preservando i commenti. */
export function serialize(doc) {
  return doc.toString()
}

/** Slug (chiave stabile) dell'app del listino. */
export function slug(doc) {
  return doc.get('slug')
}

/** Elenco delle chiavi dei tier, nell'ordine dichiarato. */
export function tierKeys(doc) {
  return tiersSeq(doc).items.map((t) => t.get('key'))
}

// ── operazioni ───────────────────────────────────────────────────────────────

/**
 * Aggiunge un nuovo tier in coda. Guard: la chiave non deve già esistere.
 * @param {object} tier {key, name, trialDays, limits, prices?}
 */
export function addTier(doc, tier) {
  requireKey(tier, 'key')
  requireKey(tier, 'name')
  const key = tier.key
  if (findTier(doc, key)) {
    throw new Error(`il tier "${key}" esiste già in "${slug(doc)}"`)
  }
  const node = doc.createNode({
    key,
    name: tier.name,
    trialDays: tier.trialDays ?? 0,
    limits: tier.limits ?? {},
    features: tier.features ?? {},
    prices: (tier.prices ?? []).map(normalizePrice),
  })
  inlineMap(node.get('limits', true))
  tiersSeq(doc).add(node)
  return doc
}

/**
 * Aggiunge un ciclo di fatturazione (prezzo) a un tier esistente.
 * Guard: il tier esiste e non ha già quel ciclo.
 */
export function addCycle(doc, { tierKey, billingCycle, amount, currency = 'EUR' }) {
  const tier = requireTier(doc, tierKey)
  requireCycle(billingCycle)
  requireAmount(amount)
  const prices = pricesSeq(tier)
  if (prices.items.some((p) => p.get('billingCycle') === billingCycle)) {
    throw new Error(`il tier "${tierKey}" ha già un prezzo ${billingCycle}`)
  }
  prices.add(doc.createNode(normalizePrice({ billingCycle, amount, currency })))
  return doc
}

/** Sostituisce i limiti (mappa) di un tier esistente. Non tocca prezzi → nessun vincolo di immutabilità. */
export function setLimits(doc, { tierKey, limits }) {
  const tier = requireTier(doc, tierKey)
  if (limits == null || typeof limits !== 'object') {
    throw new Error('limits deve essere una mappa (es. { metric: seats, cap: 20, type: stock })')
  }
  const node = doc.createNode(limits)
  inlineMap(node)
  tier.set('limits', node)
  return doc
}

/**
 * Cambio prezzo IN LOCO — SOLO per un prezzo non ancora sincronizzato (bozza).
 * Corregge l'importo del prezzo (tierKey, billingCycle). Guard: prezzo presente.
 * ATTENZIONE: usare solo se il prezzo non è vivo; altrimenti il sync rifiuta (usare la via nuovo-tier).
 */
export function changePriceInPlace(doc, { tierKey, billingCycle, amount }) {
  const tier = requireTier(doc, tierKey)
  requireCycle(billingCycle)
  requireAmount(amount)
  const price = pricesSeq(tier).items.find((p) => p.get('billingCycle') === billingCycle)
  if (!price) {
    throw new Error(`il tier "${tierKey}" non ha un prezzo ${billingCycle} da cambiare`)
  }
  price.set('amount', amount)
  return doc
}

/**
 * Cambio prezzo via NUOVO TIER — la via immutabilità-safe per un prezzo VIVO (#09 H35/H37).
 * Clona il tier sorgente in un nuovo tier `newTierKey` e vi imposta il nuovo importo per il
 * ciclo indicato; il tier sorgente resta INVARIATO (grandfathering degli abbonati esistenti).
 * Guard: il tier sorgente esiste, il nuovo non esiste, il ciclo esiste sul sorgente.
 * @param {object} p {tierKey, newTierKey, billingCycle, amount, newName?}
 */
export function changePriceNewTier(doc, { tierKey, newTierKey, billingCycle, amount, newName }) {
  const source = requireTier(doc, tierKey)
  requireCycle(billingCycle)
  requireAmount(amount)
  if (!newTierKey || newTierKey === tierKey) {
    throw new Error('newTierKey deve essere una chiave nuova, diversa dal tier sorgente')
  }
  if (findTier(doc, newTierKey)) {
    throw new Error(`il tier "${newTierKey}" esiste già: scegliere una chiave nuova`)
  }
  const srcPrices = pricesSeq(source).items
  if (!srcPrices.some((p) => p.get('billingCycle') === billingCycle)) {
    throw new Error(`il tier "${tierKey}" non ha un prezzo ${billingCycle} da cambiare`)
  }
  const clonedPrices = srcPrices.map((p) => {
    const cycle = p.get('billingCycle')
    return normalizePrice({
      billingCycle: cycle,
      amount: cycle === billingCycle ? amount : p.get('amount'),
      currency: p.get('currency') ?? 'EUR',
    })
  })
  return addTier(doc, {
    key: newTierKey,
    name: newName ?? `${source.get('name')} (nuovo prezzo)`,
    trialDays: source.get('trialDays') ?? 0,
    limits: source.toJSON().limits ?? {},
    features: source.toJSON().features ?? {},
    prices: clonedPrices,
  })
}

// ── validazione di forma (per i test di round-trip; il loader Java resta autorevole) ──

/**
 * Controllo leggero che il documento abbia la forma del pricing-as-code congelato:
 * top-level slug/name/userModel/status/tiers; ogni tier key/name/trialDays/limits/features/prices;
 * ogni prezzo billingCycle valido/amount intero positivo/currency. Lancia sul primo problema.
 * La validazione AUTOREVOLE resta il loader del core (mvn -pl core test): questo è solo un presidio
 * locale del tool.
 */
export function validate(doc) {
  const obj = doc.toJSON()
  for (const f of ['slug', 'name', 'userModel', 'status', 'tiers']) {
    if (obj[f] == null) throw new Error(`campo di primo livello mancante: ${f}`)
  }
  if (!Array.isArray(obj.tiers) || obj.tiers.length === 0) {
    throw new Error('tiers deve essere una lista non vuota')
  }
  const seenKeys = new Set()
  for (const t of obj.tiers) {
    for (const f of ['key', 'name', 'limits', 'features', 'prices']) {
      if (t[f] == null) throw new Error(`tier "${t.key ?? '?'}": campo mancante ${f}`)
    }
    if (seenKeys.has(t.key)) throw new Error(`chiave di tier duplicata: ${t.key}`)
    seenKeys.add(t.key)
    if (!Array.isArray(t.prices)) throw new Error(`tier "${t.key}": prices deve essere una lista`)
    const seenCycles = new Set()
    for (const p of t.prices) {
      requireCycle(p.billingCycle)
      requireAmount(p.amount)
      if (!p.currency) throw new Error(`tier "${t.key}", ${p.billingCycle}: currency mancante`)
      if (seenCycles.has(p.billingCycle)) {
        throw new Error(`tier "${t.key}": ciclo ${p.billingCycle} duplicato`)
      }
      seenCycles.add(p.billingCycle)
    }
  }
  return true
}

// ── helper interni ─────────────────────────────────────────────────────────────

function tiersSeq(doc) {
  const seq = doc.get('tiers')
  if (!seq || !seq.items) throw new Error(`"${slug(doc) ?? '?'}": chiave "tiers" assente o non è una lista`)
  return seq
}

function pricesSeq(tier) {
  let seq = tier.get('prices')
  if (!seq || !seq.items) {
    // un tier senza prezzi (es. free) può avere prices: [] — normalizziamo a una seq vuota
    tier.set('prices', [])
    seq = tier.get('prices')
  }
  return seq
}

function findTier(doc, key) {
  return tiersSeq(doc).items.find((t) => t.get('key') === key)
}

function requireTier(doc, key) {
  const tier = findTier(doc, key)
  if (!tier) throw new Error(`tier "${key}" non trovato in "${slug(doc)}"`)
  return tier
}

function requireCycle(cycle) {
  if (!BILLING_CYCLES.includes(cycle)) {
    throw new Error(`billingCycle non valido: "${cycle}" (ammessi: ${BILLING_CYCLES.join(', ')})`)
  }
}

function requireAmount(amount) {
  if (!Number.isInteger(amount) || amount <= 0) {
    throw new Error(`importo non valido: ${amount} (atteso intero positivo in centesimi)`)
  }
}

function requireKey(obj, field) {
  if (obj[field] == null || obj[field] === '') throw new Error(`campo obbligatorio mancante: ${field}`)
}

function normalizePrice({ billingCycle, amount, currency = 'EUR' }) {
  requireCycle(billingCycle)
  requireAmount(amount)
  return { billingCycle, amount, currency }
}

/** Rende una mappa YAML in forma inline (`{ a: 1, b: 2 }`) — stile di casa dei `limits`. */
function inlineMap(node) {
  if (node && Array.isArray(node.items)) node.flow = true // YAMLMap/YAMLSeq: hanno `items`
}
