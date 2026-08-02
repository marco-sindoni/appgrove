// ─────────────────────────────────────────────────────────────────────────────
// tools/design-tokens/lib.mjs — rilevatore di divergenza dal brand kit (UC 0086).
//
// Il problema che risolve. I token del design system sono la fonte unica dell'aspetto
// di appgrove. Ma se un consumatore scrive un colore a mano invece di leggerlo dal
// pacchetto, l'estetica diverge e NESSUN test diventa rosso: il difetto è invisibile
// finché qualcuno non guarda una pagina, un'email o un'anteprima social e nota che il
// grigio è freddo dove il brand è caldo. È il §5 dello use case 0086, e non è teorico:
// prima di questa change il generatore dell'immagine social disegnava su un blu-navy
// inesistente nella palette, e le email usavano una scala di grigi di un'altra libreria.
//
// Come lo rileva. Scandaglia i file dei consumatori dichiarati, estrae ogni colore
// scritto per esteso (esadecimale o `rgb(...)`) e lo confronta con l'insieme dei colori
// legittimi del brand, letto dai token veri. Ciò che non corrisponde è drift, a meno
// che non sia dichiarato fra le eccezioni CON un motivo scritto.
//
// Cosa NON è: un formattatore né un giudice del gusto. Non guarda i valori usati via
// variabile CSS o classe Tailwind (quelli vengono dal pacchetto per costruzione): guarda
// solo i valori COTTI, che sono l'unica via per cui il drift entra.
// ─────────────────────────────────────────────────────────────────────────────
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, relative, extname } from 'node:path'

/**
 * I consumatori sorvegliati e il motivo per cui lo sono. Ogni voce è una radice da
 * scandagliare: se un giorno nasce un nuovo consumatore del brand kit, si aggiunge qui.
 */
export const WATCHED = [
  { path: 'frontend/apps', why: 'le due SPA: devono dipingere solo con classi e variabili del pacchetto' },
  { path: 'frontend/packages/design-system/src', why: 'il pacchetto stesso, esclusa la sorgente dei token' },
  { path: 'site/src', why: 'vetrina Astro e landing generate' },
  { path: 'shared/email-templates', why: 'email: i client di posta non leggono le variabili CSS, i valori sono cotti per forza' },
  { path: 'tools/finalize-landing/lib', why: 'generatore dell immagine social on-brand' },
]

/** Estensioni dei file che possono contenere colori scritti a mano. */
const EXTENSIONS = new Set(['.ts', '.tsx', '.js', '.jsx', '.mjs', '.cjs', '.css', '.astro', '.html', '.svg'])

/** Cartelle mai scandagliate: dipendenze, esiti di compilazione, cache. */
const SKIP_DIRS = new Set(['node_modules', 'dist', 'build', '.astro', 'storybook-static', 'coverage', '.git'])

/**
 * File esclusi dalla scansione, con il motivo. Sono i posti in cui un colore per esteso
 * è la cosa GIUSTA, non un difetto.
 */
export const EXCLUDED_FILES = [
  {
    file: 'frontend/packages/design-system/src/tokens/tokens.css',
    why: 'è la sorgente unica dei token: è il posto in cui i colori DEVONO essere scritti per esteso',
  },
  {
    file: 'frontend/packages/design-system/src/tokens/tokens.test.ts',
    why: 'il test del lettore dei token verifica proprio i valori attesi, quindi li nomina',
  },
  {
    file: 'frontend/packages/design-system/src/brand/logo.test.ts',
    why: 'il test del logo passa colori finti per verificare che il disegno non ne cabli altri',
  },
]

/**
 * Eccezioni dichiarate: colori per esteso ammessi fuori dai token, con il motivo per cui
 * lo sono. Sono pochi e devono restare pochi: ogni voce è un pezzo di brand che vive
 * fuori dalla fonte unica, quindi un debito, non una soluzione.
 */
export const ALLOWED = [
  {
    color: '#ffffff',
    why: 'bianco puro: è già il token accent-contrast e il neutro di fondo delle superfici email; ammesso ovunque',
  },
  {
    color: '#000000',
    why: 'nero puro: usato solo come base di trasparenze e ombre, mai come colore di superficie',
  },
]

/** Trova tutti i file scandagliabili sotto una radice. */
function walk(root, base, out = []) {
  let entries
  try {
    entries = readdirSync(root)
  } catch {
    return out // radice inesistente: la segnala il chiamante, non è compito del cammino
  }
  for (const name of entries) {
    if (SKIP_DIRS.has(name)) continue
    const full = join(root, name)
    const st = statSync(full)
    if (st.isDirectory()) walk(full, base, out)
    else if (EXTENSIONS.has(extname(name))) out.push(relative(base, full))
  }
  return out
}

/** Normalizza un esadecimale a 6 cifre minuscole (espande la forma a 3 cifre). */
export function normalizeHex(raw) {
  let h = raw.toLowerCase()
  if (h.length === 4) h = `#${h[1]}${h[1]}${h[2]}${h[2]}${h[3]}${h[3]}`
  if (h.length === 9) h = h.slice(0, 7) // scarta il canale di trasparenza: il colore è il resto
  return h
}

/** Converte una scrittura `rgb(r, g, b)` o `rgba(r, g, b, a)` nel suo esadecimale. */
function rgbToHex(r, g, b) {
  return `#${[r, g, b].map((n) => Number(n).toString(16).padStart(2, '0')).join('')}`
}

/**
 * Estrae i colori scritti per esteso da un testo, con la riga in cui compaiono.
 * Ignora le scritture funzionali che PRENDONO il colore da una variabile
 * (`rgb(var(--ag-accent))`): quelle vengono dal pacchetto, non sono drift.
 *
 * @returns {{ color: string, line: number, raw: string }[]}
 */
export function extractColors(text) {
  const found = []
  const lines = text.split('\n')
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    for (const m of line.matchAll(/#[0-9a-fA-F]{3}(?:[0-9a-fA-F]{3}(?:[0-9a-fA-F]{2})?)?\b/g)) {
      found.push({ color: normalizeHex(m[0]), line: i + 1, raw: m[0] })
    }
    for (const m of line.matchAll(/\brgba?\(\s*(\d{1,3})\s*[, ]\s*(\d{1,3})\s*[, ]\s*(\d{1,3})/g)) {
      found.push({ color: rgbToHex(m[1], m[2], m[3]), line: i + 1, raw: m[0] })
    }
  }
  return found
}

/**
 * Esegue il controllo anti-drift.
 *
 * @param {{ root: string, brandHexes: Set<string>, watched?: typeof WATCHED }} opts
 * @returns {{ findings: object[], scanned: number, missingRoots: string[] }}
 *   `findings` = i colori fuori dal brand trovati, con file e riga.
 */
export function scan({ root, brandHexes, watched = WATCHED }) {
  const allowed = new Set(ALLOWED.map((a) => normalizeHex(a.color)))
  const excluded = new Set(EXCLUDED_FILES.map((e) => e.file))
  const findings = []
  const missingRoots = []
  let scanned = 0

  for (const entry of watched) {
    const rootDir = join(root, entry.path)
    let files
    try {
      statSync(rootDir)
      files = walk(rootDir, root)
    } catch {
      missingRoots.push(entry.path)
      continue
    }
    for (const file of files) {
      if (excluded.has(file)) continue
      scanned++
      const text = readFileSync(join(root, file), 'utf8')
      for (const hit of extractColors(text)) {
        if (brandHexes.has(hit.color) || allowed.has(hit.color)) continue
        findings.push({ file, line: hit.line, color: hit.color, raw: hit.raw, consumer: entry.path })
      }
    }
  }
  return { findings, scanned, missingRoots }
}
