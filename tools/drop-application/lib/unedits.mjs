// ─────────────────────────────────────────────────────────────────────────────
// tools/drop-application/lib/unedits.mjs — l'INVERSO di tools/new-application/lib/edits.mjs.
//
// Per ogni modifica che il generatore incolla in un file condiviso, qui c'è la
// funzione che la toglie. L'elenco dei file NON è riscritto: si importa
// EDITED_FILES dal generatore (sorgente unica), così se il generatore un giorno
// tocca un file condiviso in più, il test di parità (test/unedits.test.mjs) va
// rosso finché non esiste anche il suo inverso — è il presidio anti-divergenza
// della decisione 8 della change 0043.
//
// Ogni funzione è pura `(contenuto, ctx) => { content, changed }`:
//   • `changed: true`  → la modifica dell'app era presente ed è stata tolta;
//   • `changed: false` → il marcatore non c'era già (app mai innestata, o già
//     rimossa): il de-generatore è così RI-ESEGUIBILE senza errori.
// A differenza degli editor del generatore — che sollevano un errore se il segno
// è già presente (evitare il doppio innesto) — le unedit tollerano l'assenza:
// disfare due volte deve essere sicuro, innestare due volte no.
// ─────────────────────────────────────────────────────────────────────────────
import { EDITED_FILES } from '../../new-application/lib/edits.mjs'
import { toCamelCase } from '../../new-application/lib/context.mjs'

/** Toglie il modulo Maven dall'elenco dei moduli aggregati. */
export function uneditServicesPom(content, ctx) {
  const marker = `<module>${ctx.APP_ID}</module>`
  if (!content.includes(marker)) return { content, changed: false }
  // Rimuove la riga intera che porta il modulo (indentazione + marker + newline).
  const next = content.replace(new RegExp(`^[\\t ]*${escapeRe(marker)}[\\t ]*\\r?\\n`, 'm'), '')
  return { content: next, changed: next !== content }
}

/**
 * Toglie il modulo dall'App Registry del backoffice: la riga di import del
 * manifest e la voce nell'array MODULES (gestendo i separatori a virgola).
 */
export function uneditRegistry(content, ctx) {
  const appCamel = ctx.APP_CAMEL ?? toCamelCase(ctx.APP_ID)
  const manifestName = `${appCamel}Manifest`
  const importLine = `import { ${manifestName} } from '../modules/${ctx.APP_ID}/manifest'`

  let next = content
  let changed = false

  if (next.includes(importLine)) {
    next = next.replace(new RegExp(`${escapeRe(importLine)}\\r?\\n`), '')
    changed = true
  }

  const arrayRe = /(export const MODULES: ModuleManifest\[\] = \[)([^\]]*)(\])/
  const match = next.match(arrayRe)
  if (match) {
    const items = match[2]
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s !== '' && s !== manifestName)
    if (items.length !== match[2].split(',').map((s) => s.trim()).filter((s) => s !== '').length) {
      next = next.replace(arrayRe, (_m, open, _inner, close) => `${open}${items.join(', ')}${close}`)
      changed = true
    }
  }

  return { content: next, changed }
}

/** Toglie lo script `gen:<app_id>` dal package.json del backoffice. */
export function uneditBackofficePackageJson(content, ctx) {
  const pkg = JSON.parse(content)
  const scriptName = `gen:${ctx.APP_ID}`
  if (!pkg.scripts || !(scriptName in pkg.scripts)) return { content, changed: false }
  delete pkg.scripts[scriptName]
  return { content: JSON.stringify(pkg, null, 2) + '\n', changed: true }
}

/** Toglie l'app dall'indice del listino pricing-as-code. */
export function uneditPricingIndex(content, ctx) {
  const entry = `  - ${ctx.APP_ID}`
  const lines = content.split('\n')
  const idx = lines.findIndex((line) => line.trimEnd() === entry)
  if (idx < 0) return { content, changed: false }
  lines.splice(idx, 1)
  return { content: lines.join('\n'), changed: true }
}

/**
 * Toglie il blocco delle tre code dell'app da dev/elasticmq.conf. Il generatore
 * inserisce, in coda al blocco `queues { … }`, una riga vuota + un commento
 * `# ── code dell'app <id> …` seguiti dalle sei code (purge/export/entitlement +
 * relative code degli scarti), chiuse dalla riga `entitlement-<id>-dlq { }`.
 * Si rimuove esattamente da quella riga vuota fino alla chiusura `-dlq`.
 */
export function uneditElasticMq(content, ctx) {
  const id = ctx.APP_ID
  const lines = content.split('\n')
  const commentAt = lines.findIndex((line) => line.includes(`# ── code dell'app ${id} `))
  if (commentAt < 0) return { content, changed: false }

  const endMarker = `entitlement-${id}-dlq { }`
  let endAt = -1
  for (let i = commentAt; i < lines.length; i += 1) {
    if (lines[i].trim() === endMarker) {
      endAt = i
      break
    }
  }
  if (endAt < 0) {
    throw new Error(
      `dev/elasticmq.conf: trovato il commento delle code di ${id} ma non la chiusura `
      + `\`${endMarker}\`. Nessuna modifica applicata: correggere il file a mano.`,
    )
  }
  // Include la riga vuota che il generatore antepone al commento, se presente.
  const startAt = commentAt > 0 && lines[commentAt - 1].trim() === '' ? commentAt - 1 : commentAt
  lines.splice(startAt, endAt - startAt + 1)
  return { content: lines.join('\n'), changed: true }
}

/**
 * Toglie la landing dal registro del sito vetrina: la riga di import della
 * `Landing` per-app e la voce nell'array LANDINGS (gestendo i separatori a virgola).
 * Inverso di `editLandingsIndex`. La CARTELLA per-app (`<id>/*.ts`) la rimuove
 * `removedTrees` in plan.mjs; qui si disfa solo la modifica al file condiviso.
 */
export function uneditLandingsIndex(content, ctx) {
  const appCamel = ctx.APP_CAMEL ?? toCamelCase(ctx.APP_ID)
  const landingName = `${appCamel}Landing`
  const importLine = `import { ${landingName} } from './${ctx.APP_ID}/index.ts'`

  let next = content
  let changed = false

  if (next.includes(importLine)) {
    next = next.replace(new RegExp(`${escapeRe(importLine)}\\r?\\n`), '')
    changed = true
  }

  const arrayRe = /(export const LANDINGS: Landing\[\] = \[)([^\]]*)(\])/
  const match = next.match(arrayRe)
  if (match) {
    const items = match[2]
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s !== '' && s !== landingName)
    if (items.length !== match[2].split(',').map((s) => s.trim()).filter((s) => s !== '').length) {
      next = next.replace(arrayRe, (_m, open, _inner, close) => `${open}${items.join(', ')}${close}`)
      changed = true
    }
  }

  return { content: next, changed }
}

/** Mappa percorso → funzione inversa. Le CHIAVI devono coprire EDITED_FILES (test di parità). */
export const UNEDITORS = {
  'services/pom.xml': uneditServicesPom,
  'frontend/apps/backoffice/src/registry/registry.ts': uneditRegistry,
  'frontend/apps/backoffice/package.json': uneditBackofficePackageJson,
  'services/core/src/main/resources/pricing/index.yaml': uneditPricingIndex,
  'dev/elasticmq.conf': uneditElasticMq,
  'site/src/content/landings/index.ts': uneditLandingsIndex,
}

/** I file condivisi da disfare, nello stesso ordine del generatore (sorgente unica). */
export const UNEDITED_FILES = EDITED_FILES

function escapeRe(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}
