// ─────────────────────────────────────────────────────────────────────────────
// @appgrove/design-system/tokens.js — lettura PROGRAMMATICA dei token (UC 0086).
//
// Perché esiste. I token vivono in `tokens.css` come variabili CSS: perfetto per chi
// compila CSS (le due SPA, la vetrina Astro, le landing), inutile per chi non lo fa.
// Un generatore di immagini in Node, un renderer di email, uno script: nessuno di
// questi può leggere un foglio di stile, e finora la conseguenza è stata ricopiare i
// valori a mano — con la divergenza che ne segue (§5 dello use case 0086).
//
// Come lo risolve. Questo modulo NON ridichiara un solo valore: legge `tokens.css`,
// che resta l'unica sorgente, e ne estrae le dichiarazioni. Non può quindi divergere
// per costruzione. L'alternativa (sorgente JavaScript e CSS generato) avrebbe creato
// un secondo artefatto da tenere allineato, cioè il problema che vogliamo eliminare.
//
// Node puro, nessuna dipendenza: lo importano anche strumenti fuori dal workspace
// frontend (tools/finalize-landing, tools/design-tokens).
// ─────────────────────────────────────────────────────────────────────────────
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

/**
 * Percorso della sorgente unica dei token. Risolto PIGRAMENTE, alla prima lettura: così
 * il solo import del modulo non fallisce dove `import.meta.url` non è un percorso su
 * disco (per esempio dentro un esecutore di test che serve i moduli via HTTP).
 */
export function tokensCssPath() {
  return fileURLToPath(new URL('./tokens.css', import.meta.url))
}

/** Nomi dei temi, allineati ai blocchi di `tokens.css`. */
export const THEMES = ['light', 'dark']

/** Accenti selezionabili, allineati ai blocchi `[data-accent="…"]` di `tokens.css`. */
export const ACCENTS = ['coral', 'violet', 'teal', 'blue']

/**
 * Estrae le dichiarazioni `--ag-*` da un blocco CSS, dato il suo selettore.
 * Ritorna una mappa `nome-token → valore grezzo` (senza il prefisso `--ag-`).
 *
 * La ricerca del blocco è testuale e volutamente semplice: `tokens.css` è un file
 * piatto di sole dichiarazioni, senza annidamenti né at-rule. Se un giorno smettesse
 * di esserlo, i test di questo modulo diventerebbero rossi — che è il punto.
 */
function readBlock(css, selector) {
  const start = css.indexOf(selector)
  if (start === -1) return {}
  const open = css.indexOf('{', start)
  const close = css.indexOf('}', open)
  if (open === -1 || close === -1) return {}
  const body = css.slice(open + 1, close)
  const out = {}
  for (const [, name, value] of body.matchAll(/--ag-([a-z0-9-]+)\s*:\s*([^;]+);/gi)) {
    out[name] = value.trim()
  }
  return out
}

/**
 * Legge i token dalla sorgente CSS e li restituisce risolti per tema e accento.
 *
 * @param {{ theme?: 'light'|'dark', accent?: 'coral'|'violet'|'teal'|'blue', css?: string }} [opts]
 *   `css` serve solo ai test (permette di passare un contenuto invece di leggere il file).
 * @returns {Record<string, string>} mappa `nome-token → valore` (es. `bg` → `"244 244 241"`).
 */
export function readTokens(opts = {}) {
  const { theme = 'light', accent = 'coral', css = readFileSync(tokensCssPath(), 'utf8') } = opts
  if (!THEMES.includes(theme)) throw new Error(`tema sconosciuto: ${theme}`)
  if (!ACCENTS.includes(accent)) throw new Error(`accento sconosciuto: ${accent}`)

  // Ordine di sovrascrittura identico a quello che applica il browser leggendo il file
  // dall'alto: base `:root`, poi il tema scuro, poi l'accento scelto.
  const base = readBlock(css, ':root')
  const dark = theme === 'dark' ? readBlock(css, '[data-theme="dark"]') : {}
  const acc = readBlock(css, `[data-accent="${accent}"]`)
  return { ...base, ...dark, ...acc }
}

/** Vero se il valore è una terna RGB del formato usato dai token colore ("R G B"). */
function isRgbTriplet(value) {
  return /^\d{1,3}\s+\d{1,3}\s+\d{1,3}$/.test(value)
}

/**
 * Converte una terna RGB dei token ("236 90 114") nel suo esadecimale ("#ec5a72").
 * Serve a tutti i consumatori che non parlano CSS: immagini social, email, favicon.
 */
export function tripletToHex(triplet) {
  if (!isRgbTriplet(triplet)) throw new Error(`non è una terna RGB: ${triplet}`)
  return `#${triplet
    .split(/\s+/)
    .map((n) => Number(n).toString(16).padStart(2, '0'))
    .join('')}`
}

/**
 * Esadecimale di un token colore, per tema e accento.
 *
 * @param {string} name nome del token senza prefisso (es. `accent`, `bg`, `cat-blue`)
 * @param {{ theme?: 'light'|'dark', accent?: 'coral'|'violet'|'teal'|'blue', css?: string }} [opts]
 * @returns {string} esadecimale minuscolo con cancelletto
 */
export function hex(name, opts = {}) {
  const tokens = readTokens(opts)
  const value = tokens[name]
  if (value === undefined) throw new Error(`token sconosciuto: --ag-${name}`)
  return tripletToHex(value)
}

/**
 * Tutti i token COLORE (quelli il cui valore è una terna RGB) in esadecimale, per
 * tema e accento. Base dell'anti-drift: l'insieme dei colori legittimi del brand.
 */
export function colorHexes(opts = {}) {
  const out = {}
  for (const [name, value] of Object.entries(readTokens(opts))) {
    if (isRgbTriplet(value)) out[name] = tripletToHex(value)
  }
  return out
}

/**
 * L'insieme di TUTTI gli esadecimali legittimi del brand: ogni token colore, per ogni
 * combinazione di tema e accento. Lo usa il controllo anti-drift (tools/design-tokens)
 * per stabilire se un colore scritto a mano in un consumatore è un colore del brand o
 * un valore inventato.
 *
 * @returns {Set<string>} esadecimali minuscoli con cancelletto
 */
export function allBrandHexes(opts = {}) {
  const css = opts.css ?? readFileSync(tokensCssPath(), 'utf8')
  const out = new Set()
  for (const theme of THEMES) {
    for (const accent of ACCENTS) {
      for (const value of Object.values(colorHexes({ theme, accent, css }))) out.add(value)
    }
  }
  return out
}
