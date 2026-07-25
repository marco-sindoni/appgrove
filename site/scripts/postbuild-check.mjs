#!/usr/bin/env node
// Controlli sull'output statico della vetrina (UC 0036), da lanciare DOPO
// `astro build`. Verifica ciò che riguarda la RESA del sito (la parità dei
// markdown legali resta anche in tools/compliance, UC 0030 — qui non si duplica
// quella logica, si controlla l'HTML generato):
//   1. parità 5 lingue di home e documenti legali;
//   2. nessun token {{ residuo nell'HTML dei legali;
//   3. hreflang completo (5 lingue + x-default) sulle pagine localizzate;
//   4. meta noindex presente (salvo SITE_INDEXABLE=true);
//   5. nessun link interno rotto.
// Exit code ≠ 0 su qualsiasi violazione.

import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = path.resolve(fileURLToPath(new URL('.', import.meta.url)), '..')
const DIST = path.join(ROOT, 'dist')

const LOCALES = ['en', 'it', 'fr', 'es', 'de']
const LEGAL_COMPONENTS = ['privacy', 'terms', 'refund', 'cookie', 'subprocessors']
const indexable = process.env.SITE_INDEXABLE === 'true'

const errors = []
const fail = (msg) => errors.push(msg)

if (!fs.existsSync(DIST)) {
  console.error('✗ dist/ assente: lancia prima `astro build`.')
  process.exit(1)
}

function walk(dir) {
  const out = []
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) out.push(...walk(full))
    else out.push(full)
  }
  return out
}

const allFiles = walk(DIST)
const htmlFiles = allFiles.filter((f) => f.endsWith('.html'))
const relFromDist = (f) => '/' + path.relative(DIST, f).split(path.sep).join('/')

// Percorso URL "pulito" di un file HTML: dist/it/legal/privacy/index.html → /it/legal/privacy/
function urlOf(file) {
  let rel = relFromDist(file)
  rel = rel.replace(/index\.html$/, '')
  if (!rel.endsWith('/')) rel += '/'
  return rel
}

// 1. Parità 5 lingue: home + legali.
for (const lang of LOCALES) {
  const home = path.join(DIST, lang, 'index.html')
  if (!fs.existsSync(home)) fail(`parità: manca la home per lingua "${lang}" (${relFromDist(home)})`)
  for (const c of LEGAL_COMPONENTS) {
    const p = path.join(DIST, lang, 'legal', c, 'index.html')
    if (!fs.existsSync(p)) fail(`parità: manca il legale "${c}" in lingua "${lang}" (${relFromDist(p)})`)
  }
}

// Pagine localizzate (sotto /<lang>/…): oggetto dei controlli hreflang/noindex.
const localizedPages = htmlFiles.filter((f) => {
  const seg = relFromDist(f).split('/')[1]
  return LOCALES.includes(seg)
})

for (const file of localizedPages) {
  const html = fs.readFileSync(file, 'utf8')
  const url = urlOf(file)

  // 2. Nessun token residuo nei legali.
  if (url.includes('/legal/') && /\{\{/.test(html)) {
    fail(`token: {{…}} non risolto in ${url}`)
  }

  // 3. hreflang completo.
  const hreflangs = [...html.matchAll(/hreflang="([^"]+)"/g)].map((m) => m[1])
  for (const expected of [...LOCALES, 'x-default']) {
    if (!hreflangs.includes(expected)) fail(`hreflang: manca "${expected}" in ${url}`)
  }

  // 4. noindex (salvo indicizzazione esplicita).
  const hasNoindex = /<meta[^>]+name="robots"[^>]+noindex/i.test(html)
  if (!indexable && !hasNoindex) fail(`noindex: meta robots noindex assente in ${url}`)
  if (indexable && hasNoindex) fail(`noindex: SITE_INDEXABLE=true ma ${url} è ancora noindex`)
}

// 5. Link interni: ogni <a href="/…"> deve risolvere a un file in dist.
const pageExists = (href) => {
  const clean = href.split('#')[0].split('?')[0]
  if (clean === '' || clean === '/') return fs.existsSync(path.join(DIST, 'index.html'))
  const rel = clean.replace(/^\//, '').replace(/\/$/, '')
  const asDir = path.join(DIST, rel, 'index.html')
  const asFile = path.join(DIST, rel)
  return fs.existsSync(asDir) || fs.existsSync(asFile)
}

for (const file of htmlFiles) {
  const html = fs.readFileSync(file, 'utf8')
  const hrefs = [...html.matchAll(/<a\s[^>]*href="([^"]+)"/g)].map((m) => m[1])
  for (const href of hrefs) {
    if (!href.startsWith('/')) continue // esterni, mailto, anchor
    if (href.startsWith('//')) continue // protocol-relative → esterno
    if (!pageExists(href)) fail(`link rotto: ${href} referenziato da ${urlOf(file)}`)
  }
}

if (errors.length) {
  console.error(`✗ controllo vetrina: ${errors.length} problemi`)
  for (const e of errors) console.error('  - ' + e)
  process.exit(1)
}
console.log(`✓ controllo vetrina: ${htmlFiles.length} pagine, ${LOCALES.length} lingue — tutto verde`)
