// ─────────────────────────────────────────────────────────────────────────────
// tools/finalize-landing/lib/preflight.mjs — il cancello di pubblicazione (UC 0057).
//
// Verifica RAPIDA che una landing sia pronta per `published`, prima di aprire la PR.
// È un controllo su TESTO dei file per-lingua (stesso schema di postbuild-check.mjs,
// che controlla l'HTML generato): niente import di TypeScript, nessuna dipendenza.
// Coglie esattamente le condizioni che distinguono una bozza da una landing finita:
//   - i 5 file lingua + index presenti (parità 5 lingue, gate #14 52);
//   - nessun sentinella «DA RIFINIRE» residuo (copy non rifinita);
//   - nessuno screenshot placeholder (`src: null`) — l'immagine reale è stata catturata;
//   - nessuna immagine Open Graph mancante (`ogImage: null`) — l'anteprima social esiste.
//
// La validazione STRUTTURALE autorevole (parità di forma, slug ben formati/non
// riservati/non duplicati) resta il test del sito (`site` in run-tests.sh:
// validateLandings + astro build + postbuild-check): questo preflight è la rete a
// monte, veloce, che evita di aprire una PR che il build boccerebbe comunque.
// ─────────────────────────────────────────────────────────────────────────────
import fs from 'node:fs'
import path from 'node:path'
import { LOCALES, DRAFT_SENTINEL } from './branding.mjs'

/** I file attesi nella cartella di una landing: 5 lingue + l'index per-app. */
export const EXPECTED_FILES = [...LOCALES.map((l) => `${l}.ts`), 'index.ts']

/**
 * Preflight su una lista di file `{ name, text }` (funzione pura, testabile senza
 * disco). Ritorna `{ ok, errors }`: `errors` vuoto = pronta per la pubblicazione.
 */
export function preflightFiles(files) {
  const errors = []
  const byName = new Map(files.map((f) => [f.name, f.text]))

  // (1) Presenza di tutti i file attesi.
  for (const expected of EXPECTED_FILES) {
    if (!byName.has(expected)) errors.push(`manca il file "${expected}"`)
  }

  // (2..4) Controlli sui soli file lingua presenti.
  for (const locale of LOCALES) {
    const name = `${locale}.ts`
    const text = byName.get(name)
    if (text === undefined) continue

    if (text.includes(DRAFT_SENTINEL)) {
      errors.push(`${name}: copy non rifinita (sentinella «${DRAFT_SENTINEL}» ancora presente)`)
    }
    // Screenshot ancora placeholder: `screenshot: { src: null, … }`.
    if (/\bsrc:\s*null\b/.test(text)) {
      errors.push(`${name}: screenshot non catturato (src: null) — manca l'immagine reale`)
    }
    // Immagine Open Graph mancante.
    if (/\bogImage:\s*null\b/.test(text)) {
      errors.push(`${name}: immagine Open Graph mancante (ogImage: null)`)
    }
  }

  return { ok: errors.length === 0, errors }
}

/** Preflight sulla cartella di una landing (`site/src/content/landings/<appId>`). */
export function preflightDir(appDir) {
  if (!fs.existsSync(appDir) || !fs.statSync(appDir).isDirectory()) {
    return { ok: false, errors: [`cartella landing non trovata: ${appDir}`] }
  }
  const files = fs
    .readdirSync(appDir)
    .filter((n) => n.endsWith('.ts'))
    .map((name) => ({ name, text: fs.readFileSync(path.join(appDir, name), 'utf8') }))
  return preflightFiles(files)
}
