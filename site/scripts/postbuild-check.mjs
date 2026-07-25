#!/usr/bin/env node
// Controlli sull'output statico della vetrina (UC 0036), da lanciare DOPO
// `astro build`. Verifica ciò che riguarda la RESA del sito (la parità dei
// markdown legali resta anche in tools/compliance, UC 0030 — qui non si duplica
// quella logica, si controlla l'HTML generato):
//   1. parità 5 lingue di home e documenti legali;
//   2. nessun token {{ residuo nell'HTML dei legali;
//   3. hreflang completo (5 lingue + x-default) sulle pagine localizzate;
//   4. meta noindex presente (salvo SITE_INDEXABLE=true);
//   5. nessun link interno rotto;
//   6. landing per-app (UC 0038): parità 5 lingue + Open Graph per ogni app pubblicata.
// SEO tecnico (UC 0040):
//   8. ogni pagina localizzata ha <title> e <meta name="description"> non vuoti;
//   9. ogni hreflang RISOLVE a una pagina reale in dist (non solo presenza) — cattura
//      gli slug localizzati che non combaciano fra lingue;
//  10. sitemap.xml e robots.txt presenti; sitemap XML ben formato e non vuota;
//  11. dati strutturati JSON-LD validi: ogni pagina ha Organization; le landing hanno
//      SoftwareApplication e FAQPage.
// Exit code ≠ 0 su qualsiasi violazione.

import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = path.resolve(fileURLToPath(new URL('.', import.meta.url)), '..')
const DIST = path.join(ROOT, 'dist')

const LOCALES = ['en', 'it', 'fr', 'es', 'de']
const LEGAL_COMPONENTS = ['privacy', 'terms', 'refund', 'cookie', 'subprocessors']
// Slug brand localizzati per lingua (UC 0040): home (senza slug) + why + pricing con
// slug tradotto. DEVE restare allineato a src/lib/routes.ts (BRAND_SLUGS): un
// disallineamento fa fallire questo controllo (parità), quindi il drift è rumoroso.
const BRAND_SLUGS = {
  why: { en: 'why', it: 'perche', fr: 'pourquoi', es: 'por-que', de: 'warum' },
  pricing: { en: 'pricing', it: 'prezzi', fr: 'tarifs', es: 'precios', de: 'preise' },
}
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

// Estrae e parsa i blocchi JSON-LD di una pagina. Ritorna { types, invalid }:
// `types` = i @type trovati (stringa o array), `invalid` = numero di blocchi che NON
// parsano come JSON (SEO tecnico UC 0040: un JSON-LD malformato è peggio che assente).
function jsonLdTypes(html) {
  const types = []
  let invalid = 0
  const re = /<script[^>]+type="application\/ld\+json"[^>]*>([\s\S]*?)<\/script>/g
  let m
  while ((m = re.exec(html)) !== null) {
    try {
      const parsed = JSON.parse(m[1])
      for (const node of Array.isArray(parsed) ? parsed : [parsed]) {
        const t = node && node['@type']
        if (Array.isArray(t)) types.push(...t)
        else if (t) types.push(t)
      }
    } catch {
      invalid += 1
    }
  }
  return { types, invalid }
}

// Percorso URL "pulito" di un file HTML: dist/it/legal/privacy/index.html → /it/legal/privacy/
function urlOf(file) {
  let rel = relFromDist(file)
  rel = rel.replace(/index\.html$/, '')
  if (!rel.endsWith('/')) rel += '/'
  return rel
}

// Un percorso interno (href/hreflang) risolve a un file reale in dist? Usato sia dal
// controllo link (5) sia dalla risoluzione degli hreflang (9, UC 0040), perciò definito qui.
const pageExists = (href) => {
  const clean = href.split('#')[0].split('?')[0]
  if (clean === '' || clean === '/') return fs.existsSync(path.join(DIST, 'index.html'))
  const rel = clean.replace(/^\//, '').replace(/\/$/, '')
  const asDir = path.join(DIST, rel, 'index.html')
  const asFile = path.join(DIST, rel)
  return fs.existsSync(asDir) || fs.existsSync(asFile)
}

// 1. Parità 5 lingue: home + pagine brand (slug localizzati) + legali.
for (const lang of LOCALES) {
  const home = path.join(DIST, lang, 'index.html')
  if (!fs.existsSync(home)) fail(`parità: manca la home per lingua "${lang}" (${relFromDist(home)})`)
  for (const [key, byLang] of Object.entries(BRAND_SLUGS)) {
    const slug = byLang[lang]
    const p = path.join(DIST, lang, slug, 'index.html')
    if (!fs.existsSync(p)) {
      fail(`parità: manca la pagina brand "${key}" (${slug}) per lingua "${lang}" (${relFromDist(p)})`)
    }
  }
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

  // 8. title + description non vuoti (SEO tecnico UC 0040).
  const titleMatch = html.match(/<title>([\s\S]*?)<\/title>/i)
  if (!titleMatch || !titleMatch[1].trim()) fail(`meta: <title> assente o vuoto in ${url}`)
  const descMatch = html.match(/<meta[^>]+name="description"[^>]+content="([^"]*)"/i)
  if (!descMatch || !descMatch[1].trim()) fail(`meta: description assente o vuota in ${url}`)

  // 9. hreflang che RISOLVONO a pagine reali (non solo presenti). Cattura gli slug
  //    localizzati che non combaciano fra lingue (bug corretto in UC 0040).
  const altHrefs = [...html.matchAll(/rel="alternate"[^>]+href="([^"]+)"|href="([^"]+)"[^>]+hreflang=/g)]
    .map((m) => m[1] || m[2])
    .filter(Boolean)
  for (const href of altHrefs) {
    let pathname
    try {
      pathname = new URL(href, 'https://appgrove.app').pathname
    } catch {
      fail(`hreflang: href non valido "${href}" in ${url}`)
      continue
    }
    if (!pageExists(pathname)) fail(`hreflang: alternate "${pathname}" non risolve (da ${url})`)
  }

  // 11. Dati strutturati: JSON-LD valido + Organization su ogni pagina (UC 0040).
  const { types, invalid } = jsonLdTypes(html)
  if (invalid > 0) fail(`json-ld: ${invalid} blocco/i non parsano come JSON in ${url}`)
  if (!types.includes('Organization')) fail(`json-ld: manca Organization in ${url}`)

  // 4. noindex (salvo indicizzazione esplicita).
  const hasNoindex = /<meta[^>]+name="robots"[^>]+noindex/i.test(html)
  if (!indexable && !hasNoindex) fail(`noindex: meta robots noindex assente in ${url}`)
  if (indexable && hasNoindex) fail(`noindex: SITE_INDEXABLE=true ma ${url} è ancora noindex`)
}

// 6. Landing per-app (UC 0038): riconosciute dal marcatore <meta name="ag:app-id">.
//    Difesa a valle del gate strutturale (getStaticPaths pubblica solo `published`,
//    quindi qui NON deve mai comparire una bozza). Per ogni app pubblicata: parità
//    delle 5 lingue e presenza dei meta Open Graph. La lingua è il primo segmento URL.
const landingByApp = new Map() // appId → Map(lang → { url, html })
for (const file of localizedPages) {
  const html = fs.readFileSync(file, 'utf8')
  const m = html.match(/<meta[^>]+name="ag:app-id"[^>]+content="([^"]+)"/i)
  if (!m) continue
  const appId = m[1]
  const url = urlOf(file)
  const lang = url.split('/')[1]
  if (!landingByApp.has(appId)) landingByApp.set(appId, new Map())
  landingByApp.get(appId).set(lang, { url, html })
}

for (const [appId, byLang] of landingByApp) {
  // Parità 5 lingue.
  for (const lang of LOCALES) {
    if (!byLang.has(lang)) fail(`landing "${appId}": manca la lingua "${lang}"`)
  }
  // Open Graph + dati strutturati landing (UC 0040) su ogni pagina landing.
  for (const [lang, { url, html }] of byLang) {
    for (const prop of ['og:title', 'og:description', 'og:url']) {
      if (!new RegExp(`property="${prop}"`).test(html)) {
        fail(`landing "${appId}" [${lang}]: manca il meta Open Graph "${prop}" in ${url}`)
      }
    }
    const { types } = jsonLdTypes(html)
    for (const expected of ['SoftwareApplication', 'FAQPage']) {
      if (!types.includes(expected)) {
        fail(`landing "${appId}" [${lang}]: manca il JSON-LD "${expected}" in ${url}`)
      }
    }
  }
}

// 7. Illustrazioni della homepage (UC 0037 / change 0048): ogni home localizzata
//    deve portare le illustrazioni on-brand (marcate data-illustration). Rete di
//    regressione: se qualcuno le rimuove per errore, il sito torna "solo testo".
const MIN_HOME_ILLUSTRATIONS = 4
for (const lang of LOCALES) {
  const home = path.join(DIST, lang, 'index.html')
  if (!fs.existsSync(home)) continue // la mancanza è già segnalata dal controllo di parità
  const html = fs.readFileSync(home, 'utf8')
  const count = (html.match(/data-illustration=/g) || []).length
  if (count < MIN_HOME_ILLUSTRATIONS) {
    fail(`illustrazioni: la home "${lang}" ne ha ${count} (attese ≥ ${MIN_HOME_ILLUSTRATIONS})`)
  }
}

// 5. Link interni: ogni <a href="/…"> deve risolvere a un file in dist.
for (const file of htmlFiles) {
  const html = fs.readFileSync(file, 'utf8')
  const hrefs = [...html.matchAll(/<a\s[^>]*href="([^"]+)"/g)].map((m) => m[1])
  for (const href of hrefs) {
    if (!href.startsWith('/')) continue // esterni, mailto, anchor
    if (href.startsWith('//')) continue // protocol-relative → esterno
    if (!pageExists(href)) fail(`link rotto: ${href} referenziato da ${urlOf(file)}`)
  }
}

// 10. sitemap.xml + robots.txt (SEO tecnico UC 0040).
const sitemapPath = path.join(DIST, 'sitemap.xml')
if (!fs.existsSync(sitemapPath)) {
  fail('sitemap: dist/sitemap.xml assente')
} else {
  const xml = fs.readFileSync(sitemapPath, 'utf8')
  if (!xml.includes('<urlset')) fail('sitemap: manca <urlset> (XML non ben formato)')
  if (!/<loc>/.test(xml)) fail('sitemap: nessun <loc> (sitemap vuota)')
}
const robotsPath = path.join(DIST, 'robots.txt')
if (!fs.existsSync(robotsPath)) {
  fail('robots: dist/robots.txt assente')
} else {
  const robots = fs.readFileSync(robotsPath, 'utf8')
  if (!/User-agent:/i.test(robots)) fail('robots: manca la direttiva User-agent')
}

if (errors.length) {
  console.error(`✗ controllo vetrina: ${errors.length} problemi`)
  for (const e of errors) console.error('  - ' + e)
  process.exit(1)
}
console.log(`✓ controllo vetrina: ${htmlFiles.length} pagine, ${LOCALES.length} lingue — tutto verde`)
