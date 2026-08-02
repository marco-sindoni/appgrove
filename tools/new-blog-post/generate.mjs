#!/usr/bin/env node
// ─────────────────────────────────────────────────────────────────────────────
// tools/new-blog-post/generate.mjs — la META' MECCANICA della skill `new-blog-post`
// (UC 0084), gemella di tools/finalize-landing (UC 0057) e tools/new-application (UC 0046).
//
// La skill conversazionale (.claude/skills/new-blog-post/) conduce l'intervista editoriale
// e scrive la copy nelle 5 lingue in un file di specifica JSON; POI chiama questi
// sottocomandi per i passi che una macchina fa in modo deterministico e ripetibile:
//
//   list       elenca pilastri e articoli esistenti (è la mappa su cui si sceglie dove collocare il pezzo)
//   check      valida una specifica SENZA scrivere nulla (rifiuto pulito)
//   scaffold   valida e materializza: cartella + 5 file-lingua + identità, entry nel registro,
//              riferimenti reciproci pilastro↔cluster agganciati
//   remove     inverso della generazione (ripristino dopo un build rosso, collaudo andata-ritorno)
//
// NON pubblica e NON apre PR: la pubblicazione è l'integrazione continua al merge (UC 0036);
// la change la apre la skill via `new-change`.
// ─────────────────────────────────────────────────────────────────────────────
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { outline, remove as removePosts, scaffold, validate } from './lib/scaffold.mjs'

const TOOL_DIR = path.dirname(fileURLToPath(import.meta.url))
const REPO_ROOT = path.resolve(TOOL_DIR, '../..')

function die(msg) {
  process.stderr.write(`\n✗ ${msg}\n\n`)
  process.exit(1)
}
function info(msg) {
  process.stdout.write(`  ${msg}\n`)
}

function usage() {
  return `Uso: generate.mjs <comando> [opzioni]

Comandi
  list                        Elenca i pilastri e i loro articoli cluster
                              [--json] output leggibile da un programma
  check    --spec <file>      Valida la specifica senza scrivere nulla (esce ≠ 0 se non va)
  scaffold --spec <file>      Valida e crea i post: cartella + 5 file-lingua + identità,
                              entry nel registro, aggancio pilastro↔cluster
  remove   --key <chiave>     Toglie un post (ripetibile). Inverso esatto di scaffold

Opzioni comuni
  --repo-root <dir>           Radice del repository (default: quella di questo strumento)

Formato della specifica (JSON): un post, un array di post, oppure { "posts": [ … ] }.
Un post:
  {
    "key": "chiave-stabile",              // nome della cartella, non tradotto
    "kind": "pillar" | "article",         // pilastro o articolo cluster
    "datePublished": "AAAA-MM-GG",
    "appId": "fatture",                   // app con landing PUBBLICATA a cui rimanda
    "pillarKey": "chiave-del-pilastro",   // solo per gli articoli
    "content": { "en": { … }, "it": { … }, "fr": { … }, "es": { … }, "de": { … } }
  }
Il contenuto di ogni lingua: slug, title, description, question, intro[], sections[{heading,
paragraphs[]}], faq{title, items[{q,a}]}, ctaText. Le traduzioni devono avere la STESSA forma
della sorgente inglese (stesso numero di paragrafi, sezioni e voci FAQ).
`
}

function parseArgs(argv) {
  const out = { _: [], keys: [] }
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i]
    if (a === '--json') out.json = true
    else if (a === '--spec') out.spec = argv[++i]
    else if (a === '--repo-root') out.repoRoot = argv[++i]
    else if (a === '--key') out.keys.push(argv[++i])
    else if (a === '-h' || a === '--help') out.help = true
    else out._.push(a)
  }
  return out
}

function readSpec(file) {
  if (!file) die('manca --spec <file> (la specifica JSON scritta dal co-pilota)')
  if (!fs.existsSync(file)) die(`specifica non trovata: ${file}`)
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'))
  } catch (err) {
    die(`la specifica non è JSON valido: ${err.message}`)
  }
}

function reportErrors(errors) {
  process.stderr.write(`\n✗ specifica rifiutata — ${errors.length} problem${errors.length === 1 ? 'a' : 'i'}:\n\n`)
  for (const e of errors) process.stderr.write(`  • ${e}\n`)
  process.stderr.write(
    `\nNiente è stato scritto: correggi la specifica e rilancia.\n` +
      `Le regole autorevoli del registro blog stanno in site/src/lib/blog.ts (UC 0042).\n\n`,
  )
  process.exit(1)
}

const args = parseArgs(process.argv.slice(2))
const command = args._[0]
const repoRoot = args.repoRoot ? path.resolve(args.repoRoot) : REPO_ROOT

if (args.help || !command) {
  process.stdout.write(usage())
  process.exit(command ? 0 : 2)
}

switch (command) {
  case 'list': {
    const view = outline(repoRoot)
    if (args.json) {
      process.stdout.write(`${JSON.stringify(view, null, 2)}\n`)
      break
    }
    process.stdout.write(`\nBlog — ${view.total} post in ${view.blogDir}\n\n`)
    if (view.pillars.length === 0) info('(nessun pilastro: il primo articolo dovrà aprirne uno)')
    for (const p of view.pillars) {
      process.stdout.write(`  ▸ PILASTRO ${p.key}  [app: ${p.appId}]  /en/blog/${p.slugEn}/\n`)
      if (p.clusters.length === 0) process.stdout.write(`      (nessun articolo cluster)\n`)
      for (const c of p.clusters) {
        process.stdout.write(`      · ${c.key}  [app: ${c.appId}]  /en/blog/${c.slugEn}/\n`)
      }
    }
    if (view.orphans.length > 0) {
      process.stdout.write(`\n  Articoli senza pilastro valido (da sistemare):\n`)
      for (const o of view.orphans) process.stdout.write(`      · ${o.key}\n`)
    }
    process.stdout.write('\n')
    break
  }

  case 'check': {
    const raw = readSpec(args.spec)
    const { posts, errors } = validate(repoRoot, raw)
    if (errors.length > 0) reportErrors(errors)
    process.stdout.write(`\n✓ specifica valida — ${posts.length} post pronti da generare:\n\n`)
    for (const p of posts) info(`${p.kind === 'pillar' ? 'pilastro' : 'articolo'} ${p.key} → app ${p.appId}`)
    process.stdout.write('\n')
    break
  }

  case 'scaffold': {
    const raw = readSpec(args.spec)
    const { posts, errors } = validate(repoRoot, raw)
    if (errors.length > 0) reportErrors(errors)
    let touched
    try {
      touched = scaffold(repoRoot, posts)
    } catch (err) {
      die(`generazione fallita (ripristinato lo stato precedente): ${err.message}`)
    }
    process.stdout.write(`\n✓ generati ${posts.length} post:\n\n`)
    for (const p of posts) {
      info(`${p.kind === 'pillar' ? 'pilastro' : 'articolo'} ${p.key}${p.pillarKey ? ` → pilastro ${p.pillarKey}` : ''}`)
    }
    process.stdout.write(`\n  File toccati:\n`)
    for (const f of touched) info(path.relative(repoRoot, f))
    process.stdout.write(
      `\nProssimo passo: ./run-tests.sh site (vitest + astro build + controllo post-build).\n` +
        `Se diventa rosso: generate.mjs remove --key <chiave> riporta tutto com'era.\n\n`,
    )
    break
  }

  case 'remove': {
    if (args.keys.length === 0) die('manca --key <chiave> (ripetibile)')
    let touched
    try {
      touched = removePosts(repoRoot, args.keys)
    } catch (err) {
      die(`rimozione fallita (ripristinato lo stato precedente): ${err.message}`)
    }
    process.stdout.write(`\n✓ rimossi ${args.keys.length} post: ${args.keys.join(', ')}\n\n`)
    for (const f of touched) info(path.relative(repoRoot, f))
    process.stdout.write('\n')
    break
  }

  default:
    die(`comando sconosciuto: ${command}\n\n${usage()}`)
}
