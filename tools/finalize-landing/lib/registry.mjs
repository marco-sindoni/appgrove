// ─────────────────────────────────────────────────────────────────────────────
// tools/finalize-landing/lib/registry.mjs — modifiche meccaniche ai file landing.
//
// I passi deterministici che `finalize-landing` applica alla cartella per-app della
// landing (`site/src/content/landings/<appId>/`):
//   - cablare il percorso dello screenshot reale al posto del placeholder (`src: null`);
//   - cablare il percorso dell'immagine Open Graph (`ogImage: null`);
//   - portare lo stato `draft → published` (un solo punto: l'index per-app).
//
// Sono edit su TESTO, non riscritture: nella bozza generata da new-application c'è UN
// solo `src:` (lo screenshot dell'hero) e UN solo `ogImage:` per file lingua, quindi la
// sostituzione mirata è sicura. La copy vera e propria la rifinisce la skill (LLM); qui
// si fa solo il cablaggio che una macchina può fare in modo affidabile e ripetibile.
// ─────────────────────────────────────────────────────────────────────────────
import fs from 'node:fs'
import path from 'node:path'
import { LOCALES } from './branding.mjs'

/** Cartella della landing di un'app nel sito vetrina. */
export function landingDir(repoRoot, appId) {
  return path.join(repoRoot, 'site/src/content/landings', appId)
}

/** Convenzione dei percorsi asset (root del sito): screenshot per lingua + immagine OG. */
export function screenshotAssetPath(appId, locale) {
  return `/landings/${appId}/hero.${locale}.png`
}
export function ogImageAssetPath(appId) {
  return `/landings/${appId}/og.png`
}
/** Cartella su disco degli asset pubblici della landing (`site/public/landings/<appId>`). */
export function publicAssetDir(repoRoot, appId) {
  return path.join(repoRoot, 'site/public/landings', appId)
}

/** Legge il percorso di un file lingua della landing. */
function localeFile(appDir, locale) {
  return path.join(appDir, `${locale}.ts`)
}

/**
 * Cabla il percorso dello screenshot reale in un file lingua, al posto del
 * placeholder `src: null`. Ritorna `changed`. Idempotente: se non c'è `src: null`
 * (già cablato) non fa nulla.
 */
export function setScreenshotSrc(appDir, locale, src) {
  const file = localeFile(appDir, locale)
  const before = fs.readFileSync(file, 'utf8')
  const after = before.replace(/(\bsrc:\s*)null/, `$1'${src}'`)
  if (after !== before) fs.writeFileSync(file, after)
  return after !== before
}

/** Cabla il percorso dell'immagine Open Graph in un file lingua (al posto di `ogImage: null`). */
export function setOgImage(appDir, locale, ogImage) {
  const file = localeFile(appDir, locale)
  const before = fs.readFileSync(file, 'utf8')
  const after = before.replace(/(\bogImage:\s*)null/, `$1'${ogImage}'`)
  if (after !== before) fs.writeFileSync(file, after)
  return after !== before
}

/** Legge lo stato dichiarato nell'index per-app (`draft` | `published` | null). */
export function readStatus(appDir) {
  const file = path.join(appDir, 'index.ts')
  const text = fs.readFileSync(file, 'utf8')
  const m = text.match(/status:\s*'(draft|published)'/)
  return m ? m[1] : null
}

/**
 * Porta la landing a `published` cambiando lo stato nell'index per-app. Idempotente:
 * se è già `published` ritorna `changed:false`; se non trova né draft né published
 * solleva un errore (formato inatteso). NON esegue il preflight: chi chiama deve
 * averlo già passato — la pubblicazione senza preflight verde è un difetto.
 */
export function publish(appDir) {
  const file = path.join(appDir, 'index.ts')
  const before = fs.readFileSync(file, 'utf8')
  if (/status:\s*'published'/.test(before)) return { changed: false }
  if (!/status:\s*'draft'/.test(before)) {
    throw new Error(`${file}: stato non riconosciuto (atteso 'draft' o 'published')`)
  }
  const after = before.replace(/status:\s*'draft'/, "status: 'published'")
  fs.writeFileSync(file, after)
  return { changed: true }
}

/** Riporta la landing a `draft` (utile in test/rollback). Inverso di publish. */
export function unpublish(appDir) {
  const file = path.join(appDir, 'index.ts')
  const before = fs.readFileSync(file, 'utf8')
  if (!/status:\s*'published'/.test(before)) return { changed: false }
  const after = before.replace(/status:\s*'published'/, "status: 'draft'")
  fs.writeFileSync(file, after)
  return { changed: true }
}

export { LOCALES }
