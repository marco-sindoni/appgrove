// ─────────────────────────────────────────────────────────────────────────────
// tools/new-blog-post/lib/registry.mjs — lettura e modifiche meccaniche del registro
// dei contenuti del blog (UC 0084 su contratto UC 0042).
//
// Sono edit su TESTO, non riscritture del file: il registro è codice scritto da persone e
// deve restare tale. Stessa scelta (e stesse ragioni) delle modifiche meccaniche di
// `finalize-landing`. Ogni operazione ha il suo INVERSO esatto, così il ciclo
// genera→rimuovi riporta i file identici byte a byte — è la garanzia che rende sicuro sia
// il ripristino dopo un build rosso, sia il collaudo andata-ritorno.
// ─────────────────────────────────────────────────────────────────────────────
import fs from 'node:fs'
import path from 'node:path'
import { LOCALES } from './spec.mjs'
import { exportNameFor } from './render.mjs'

/** Cartella dei contenuti del blog nel sito vetrina. */
export function blogDir(repoRoot) {
  return path.join(repoRoot, 'site/src/content/blog')
}
/** Cartella di un singolo post. */
export function postDir(repoRoot, key) {
  return path.join(blogDir(repoRoot), key)
}
/** File del registro (array dei post). */
export function registryFile(repoRoot) {
  return path.join(blogDir(repoRoot), 'index.ts')
}
/** Cartella delle landing per-app (serve per verificare il collegamento interno). */
export function landingsDir(repoRoot) {
  return path.join(repoRoot, 'site/src/content/landings')
}

/** Legge il valore di una proprietà stringa (`nome: '…'` o `nome: "…"`). */
function readStringProp(text, name) {
  const m = text.match(new RegExp(`\\b${name}:\\s*(?:'([^']*)'|"([^"]*)")`))
  if (!m) return undefined
  return m[1] !== undefined ? m[1] : m[2]
}

/** Legge una lista di stringhe su una riga (`clusterKeys: ['a', 'b']`). */
function readStringListProp(text, name) {
  const m = text.match(new RegExp(`\\b${name}:\\s*\\[([^\\]]*)\\]`))
  if (!m) return undefined
  return [...m[1].matchAll(/'([^']*)'|"([^"]*)"/g)].map((x) => (x[1] !== undefined ? x[1] : x[2]))
}

/** Righe di importazione dei post nel registro: `import { x } from './chiave/index.ts'`. */
const POST_IMPORT_RE = /^import \{ ([A-Za-z0-9_$]+) \} from '\.\/([a-z0-9-]+)\/index\.ts'$/gm

/**
 * Legge il registro reale: le chiavi dei post nell'ordine in cui sono importati, con il
 * loro nome di export. L'ordine delle importazioni è l'ordine del registro.
 */
export function readRegistry(repoRoot) {
  const file = registryFile(repoRoot)
  const text = fs.readFileSync(file, 'utf8')
  const imports = [...text.matchAll(POST_IMPORT_RE)].map((m) => ({ exportName: m[1], key: m[2] }))
  return { file, text, imports }
}

/** Legge identità e slug di un post già presente nel registro. */
export function readPost(repoRoot, key) {
  const dir = postDir(repoRoot, key)
  const indexText = fs.readFileSync(path.join(dir, 'index.ts'), 'utf8')
  const slugs = {}
  for (const loc of LOCALES) {
    const f = path.join(dir, `${loc}.ts`)
    if (fs.existsSync(f)) slugs[loc] = readStringProp(fs.readFileSync(f, 'utf8'), 'slug')
  }
  return {
    key,
    kind: readStringProp(indexText, 'kind'),
    appId: readStringProp(indexText, 'appId'),
    pillarKey: readStringProp(indexText, 'pillarKey'),
    clusterKeys: readStringListProp(indexText, 'clusterKeys') ?? [],
    slugs,
  }
}

/** Tutti i post del registro, letti dal disco (identità + slug per lingua). */
export function readAllPosts(repoRoot) {
  return readRegistry(repoRoot).imports.map(({ key }) => readPost(repoRoot, key))
}

/** Identificativi delle app la cui landing è PUBBLICATA (il collegamento interno deve risolvere). */
export function publishedAppIds(repoRoot) {
  const dir = landingsDir(repoRoot)
  if (!fs.existsSync(dir)) return []
  const out = []
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (!entry.isDirectory()) continue
    const f = path.join(dir, entry.name, 'index.ts')
    if (!fs.existsSync(f)) continue
    const text = fs.readFileSync(f, 'utf8')
    if (readStringProp(text, 'status') !== 'published') continue
    const appId = readStringProp(text, 'appId')
    if (appId) out.push(appId)
  }
  return out
}

// ── modifiche al registro (ognuna col suo inverso esatto) ────────────────────

/** Riga di importazione di un post nel registro. */
function importLine(key) {
  return `import { ${exportNameFor(key)} } from './${key}/index.ts'`
}

/**
 * Appende un post al registro: importazione dopo l'ultima importazione di post, e voce in
 * coda all'array. Inverso: `removeFromRegistry`.
 */
export function addToRegistry(text, key) {
  const line = importLine(key)
  if (text.includes(`${line}\n`)) throw new Error(`il registro importa già "${key}"`)

  const matches = [...text.matchAll(POST_IMPORT_RE)]
  let withImport
  if (matches.length > 0) {
    const last = matches[matches.length - 1]
    const at = last.index + last[0].length
    withImport = `${text.slice(0, at)}\n${line}${text.slice(at)}`
  } else {
    const anchor = text.indexOf("import type { BlogPost } from './types.ts'")
    if (anchor < 0) throw new Error('registro in formato inatteso: manca l\'importazione del tipo BlogPost')
    const at = anchor + "import type { BlogPost } from './types.ts'".length
    withImport = `${text.slice(0, at)}\n${line}${text.slice(at)}`
  }

  const marker = 'export const BLOG_POSTS: readonly BlogPost[] = ['
  const start = withImport.indexOf(marker)
  if (start < 0) throw new Error('registro in formato inatteso: manca l\'array BLOG_POSTS')
  const close = withImport.indexOf('\n]', start)
  if (close < 0) throw new Error('registro in formato inatteso: array BLOG_POSTS non chiuso')
  return `${withImport.slice(0, close + 1)}  ${exportNameFor(key)},\n${withImport.slice(close + 1)}`
}

/** Toglie un post dal registro (inverso esatto di `addToRegistry`). */
export function removeFromRegistry(text, key) {
  const line = `${importLine(key)}\n`
  const entry = `  ${exportNameFor(key)},\n`
  if (!text.includes(line)) throw new Error(`il registro non importa "${key}"`)
  if (!text.includes(entry)) throw new Error(`il registro non elenca "${key}" fra i post`)
  return text.replace(line, '').replace(entry, '')
}

/**
 * Aggiunge la chiave di un articolo alla lista dei cluster del suo pilastro. Se il pilastro
 * non ha ancora la lista (è appena nato) la crea subito dopo `appId`. Inverso:
 * `removeClusterKey`, che toglie la voce e — se resta vuota — l'intera riga.
 */
export function addClusterKey(pillarIndexText, key) {
  const current = readStringListProp(pillarIndexText, 'clusterKeys')
  if (current === undefined) {
    const m = pillarIndexText.match(/^(\s*)appId:\s*(?:'[^']*'|"[^"]*"),$/m)
    if (!m) throw new Error("file del pilastro in formato inatteso: manca la riga appId")
    const at = m.index + m[0].length
    return `${pillarIndexText.slice(0, at)}\n${m[1]}clusterKeys: ['${key}'],${pillarIndexText.slice(at)}`
  }
  if (current.includes(key)) return pillarIndexText
  const next = [...current, key]
  return pillarIndexText.replace(
    /\bclusterKeys:\s*\[[^\]]*\]/,
    `clusterKeys: [${next.map((k) => `'${k}'`).join(', ')}]`,
  )
}

/** Toglie la chiave di un articolo dai cluster del pilastro (inverso di `addClusterKey`). */
export function removeClusterKey(pillarIndexText, key) {
  const current = readStringListProp(pillarIndexText, 'clusterKeys')
  if (current === undefined || !current.includes(key)) return pillarIndexText
  const next = current.filter((k) => k !== key)
  if (next.length === 0) {
    return pillarIndexText.replace(/\n\s*clusterKeys:\s*\[[^\]]*\],/, '')
  }
  return pillarIndexText.replace(
    /\bclusterKeys:\s*\[[^\]]*\]/,
    `clusterKeys: [${next.map((k) => `'${k}'`).join(', ')}]`,
  )
}
