// ─────────────────────────────────────────────────────────────────────────────
// tools/finalize-landing/lib/seeds.mjs — seed per-app degli screenshot (UC 0057).
//
// Il comando `screenshots` cattura una figura reale del modulo frontend dell'app via
// mock-route in-browser (stessa tecnica delle prove e2e generate). I dati che popolano
// la lista NON sono universali: un'app scaffoldata da `new-application` espone la risorsa
// generica `items` (campi code/contactName), ma un'app che diverge dallo scaffold — come
// `fatture` (risorsa `invoices`, campi number/customerName) — ha una forma diversa. Con
// un solo mock cablato sulla forma generica, quelle app renderebbero una lista VUOTA.
//
// Qui il seed è GUIDATO DAI DATI: ogni app può fornire un descrittore
// `tools/finalize-landing/seeds/<appId>.mjs` (risorsa di lista, metric della quota, tetto,
// record d'esempio); se assente si usa il DEFAULT generico. Così le app scaffoldate
// funzionano senza toccare nulla (parità scaffold), e solo chi diverge aggiunge un file.
// ─────────────────────────────────────────────────────────────────────────────
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

/** Cartella dei descrittori di seed per-app. */
const SEEDS_DIR = path.join(path.dirname(fileURLToPath(import.meta.url)), '..', 'seeds')

/**
 * Descrittore di seed generico: la forma prodotta da `new-application` (risorsa `items`).
 * Un'app appena scaffoldata combacia con questo default e non richiede un file dedicato.
 */
export const DEFAULT_SEED = {
  listPath: 'items',
  metric: 'items',
  freeCap: 10,
  records: [
    { id: 'item-1', code: '2026-0001', contactName: 'Mario Rossi', status: 'active', currency: 'EUR', totalAmount: 120 },
    { id: 'item-2', code: '2026-0002', contactName: 'Anna Bianchi', status: 'active', currency: 'EUR', totalAmount: 340 },
  ],
}

/** Normalizza un descrittore parziale coi default (funzione pura, testabile). */
export function normalizeSeed(seed) {
  const s = seed ?? {}
  return {
    listPath: s.listPath ?? DEFAULT_SEED.listPath,
    metric: s.metric ?? DEFAULT_SEED.metric,
    freeCap: s.freeCap ?? DEFAULT_SEED.freeCap,
    records: Array.isArray(s.records) ? s.records : DEFAULT_SEED.records,
  }
}

/** Percorso del descrittore di seed di un'app (esista o meno). */
export function seedFile(appId) {
  return path.join(SEEDS_DIR, `${appId}.mjs`)
}

/**
 * Risolve il seed di un'app: se `seeds/<appId>.mjs` esiste ne carica il descrittore
 * (export nominato `seed` o default), altrimenti ricade sul DEFAULT generico. Sempre
 * normalizzato. È il punto in cui il tool smette di essere cablato su una sola forma.
 */
export async function resolveSeed(appId) {
  const file = seedFile(appId)
  if (fs.existsSync(file)) {
    const mod = await import(pathToFileURL(file).href)
    return normalizeSeed(mod.seed ?? mod.default)
  }
  return normalizeSeed(DEFAULT_SEED)
}
