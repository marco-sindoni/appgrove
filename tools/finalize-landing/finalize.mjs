#!/usr/bin/env node
// ─────────────────────────────────────────────────────────────────────────────
// tools/finalize-landing/finalize.mjs — la META' MECCANICA della skill
// `finalize-landing` (UC 0057).
//
// La skill conversazionale (.claude/skills/finalize-landing/) rifinisce la copy nelle 5
// lingue e conduce la review con l'utente; POI chiama questi sottocomandi per i passi che
// una macchina fa in modo deterministico e ripetibile:
//
//   og           genera l'immagine Open Graph on-brand (PNG 1200×630)
//   screenshots  cattura gli screenshot reali del modulo app, uno per lingua (Playwright)
//   wire-assets  cabla i percorsi asset (screenshot.src, meta.ogImage) nei file lingua
//   preflight    verifica che la landing sia pronta per la pubblicazione
//   publish      preflight + transizione draft→published (rifiuta se il preflight è rosso)
//
// NON fa deploy e NON apre la PR: la pubblicazione è la CI al merge (#14 dec.53); la PR la
// apre la skill via il flusso leggero di `new-change`. Lo strumento scrive contenuti/asset.
// ─────────────────────────────────────────────────────────────────────────────
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { LOCALES } from './lib/branding.mjs'
import { renderOgImage } from './lib/og-image.mjs'
import { captureShots } from './lib/screenshot.mjs'
import { preflightDir } from './lib/preflight.mjs'
import {
  landingDir,
  publicAssetDir,
  screenshotAssetPath,
  ogImageAssetPath,
  setScreenshotSrc,
  setOgImage,
  publish as publishLanding,
} from './lib/registry.mjs'

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
  return `Uso: finalize.mjs <comando> [opzioni]

Comandi
  og           --app-id <id> --app-name <nome> --tagline <frase> [--accent cat-blue]
               Genera l'immagine Open Graph on-brand in site/public/landings/<id>/og.png
  screenshots  --app-id <id> --base-url <url> [--metric <m>] [--free-cap <N>] [--locales en,it]
               Cattura hero.<lingua>.png in site/public/landings/<id>/ (app servita a base-url)
  wire-assets  --app-id <id> [--locales en,it]
               Cabla screenshot.src e meta.ogImage nei file lingua (ai percorsi di convenzione)
  preflight    --app-id <id>
               Verifica che la landing sia pronta (esce ≠ 0 se non lo è)
  publish      --app-id <id>
               Preflight + porta lo stato draft→published (rifiuta se il preflight è rosso)

Opzioni comuni
  --repo-root <path>   radice del monorepo (default: dedotta dalla posizione dello strumento)
  --help
`
}

/** Parsing minimale `--chiave valore` / `--flag`. */
function parseArgs(argv) {
  const opts = {}
  for (let i = 0; i < argv.length; i += 1) {
    const a = argv[i]
    if (!a.startsWith('--')) continue
    const key = a.slice(2)
    const next = argv[i + 1]
    if (next === undefined || next.startsWith('--')) {
      opts[key] = true
    } else {
      opts[key] = next
      i += 1
    }
  }
  return opts
}

function resolveRoot(opts) {
  return opts['repo-root'] ? path.resolve(opts['repo-root']) : REPO_ROOT
}

function requireAppDir(root, appId) {
  if (!appId || appId === true) die('manca --app-id')
  const dir = landingDir(root, appId)
  if (!fs.existsSync(dir)) {
    die(`bozza landing non trovata per "${appId}" (${path.relative(root, dir)}).\n`
      + `  La bozza la crea /new-application: esegui prima quello, poi /finalize-landing ${appId}.`)
  }
  return dir
}

function localesOf(opts) {
  if (!opts.locales || opts.locales === true) return LOCALES
  return String(opts.locales).split(',').map((s) => s.trim()).filter(Boolean)
}

// ── Comandi ─────────────────────────────────────────────────────────────────

async function cmdOg(opts) {
  const root = resolveRoot(opts)
  const appId = opts['app-id']
  requireAppDir(root, appId)
  const appName = opts['app-name'] && opts['app-name'] !== true ? opts['app-name'] : appId
  const tagline = opts.tagline && opts.tagline !== true ? opts.tagline : ''
  const accent = opts.accent && opts.accent !== true ? opts.accent : 'cat-blue'

  const png = await renderOgImage({ appName, tagline, accent })
  const dir = publicAssetDir(root, appId)
  fs.mkdirSync(dir, { recursive: true })
  const out = path.join(dir, 'og.png')
  fs.writeFileSync(out, png)
  info(`✓ immagine Open Graph: ${path.relative(root, out)} (${png.length} byte)`)
}

async function cmdScreenshots(opts) {
  const root = resolveRoot(opts)
  const appId = opts['app-id']
  requireAppDir(root, appId)
  const baseURL = opts['base-url']
  if (!baseURL || baseURL === true) die('manca --base-url (es. http://localhost:4173, l\'app in preview)')
  const outDir = publicAssetDir(root, appId)
  const produced = await captureShots({
    baseURL,
    appId,
    locales: localesOf(opts),
    outDir,
    metric: opts.metric && opts.metric !== true ? opts.metric : 'items',
    freeCap: opts['free-cap'] && opts['free-cap'] !== true ? Number(opts['free-cap']) : 10,
  })
  for (const p of produced) info(`✓ screenshot: ${path.relative(root, p)}`)
}

function cmdWireAssets(opts) {
  const root = resolveRoot(opts)
  const appId = opts['app-id']
  const appDir = requireAppDir(root, appId)
  for (const locale of localesOf(opts)) {
    const s = setScreenshotSrc(appDir, locale, screenshotAssetPath(appId, locale))
    const o = setOgImage(appDir, locale, ogImageAssetPath(appId))
    info(`${locale}: screenshot ${s ? 'cablato' : 'già cablato'}, ogImage ${o ? 'cablata' : 'già cablata'}`)
  }
}

function cmdPreflight(opts) {
  const root = resolveRoot(opts)
  const appId = opts['app-id']
  const appDir = requireAppDir(root, appId)
  const { ok, errors } = preflightDir(appDir)
  if (ok) {
    info(`✓ preflight verde: "${appId}" è pronta per la pubblicazione`)
    return
  }
  process.stderr.write(`\n✗ preflight rosso per "${appId}" (${errors.length} problemi):\n`)
  for (const e of errors) process.stderr.write(`    - ${e}\n`)
  process.stderr.write('\n')
  process.exit(1)
}

function cmdPublish(opts) {
  const root = resolveRoot(opts)
  const appId = opts['app-id']
  const appDir = requireAppDir(root, appId)
  const { ok, errors } = preflightDir(appDir)
  if (!ok) {
    process.stderr.write(`\n✗ pubblicazione rifiutata: preflight rosso per "${appId}" (${errors.length} problemi):\n`)
    for (const e of errors) process.stderr.write(`    - ${e}\n`)
    process.stderr.write('\n  Risolvi i problemi (cattura screenshot, genera immagine OG, rifinisci la copy) e riprova.\n\n')
    process.exit(1)
  }
  const { changed } = publishLanding(appDir)
  info(changed
    ? `✓ "${appId}" portata a published — al merge la CI la pubblica (UC 0036)`
    : `· "${appId}" era già published (nessun cambiamento)`)
}

async function main() {
  const [command, ...rest] = process.argv.slice(2)
  const opts = parseArgs(rest)
  if (!command || command === '--help' || opts.help) {
    process.stdout.write(usage())
    process.exit(command ? 0 : 1)
  }
  switch (command) {
    case 'og': return cmdOg(opts)
    case 'screenshots': return cmdScreenshots(opts)
    case 'wire-assets': return cmdWireAssets(opts)
    case 'preflight': return cmdPreflight(opts)
    case 'publish': return cmdPublish(opts)
    default: die(`comando sconosciuto: ${command}\n${usage()}`)
  }
}

main().catch((err) => die(err.message))
