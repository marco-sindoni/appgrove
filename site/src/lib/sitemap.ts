// Generazione della sitemap multilingua del sito vetrina (UC 0040).
//
// Perché una generazione PROPRIA e non @astrojs/sitemap: gli slug sono localizzati e
// NON uniformi fra lingue (brand: /en/why/ ↔ /it/perche/; landing: /en/invoicing/ ↔
// /it/fatture/). L'integrazione ufficiale assume lo stesso percorso per tutte le
// lingue, quindi produrrebbe alternates hreflang errati. Costruendola dal nostro
// registro di percorsi otteniamo alternates corretti, senza aggiungere dipendenze.
//
// Funzioni pure (testabili): `collectSitemapGroups` raccoglie i percorsi, `renderSitemap`
// produce l'XML. L'endpoint pages/sitemap.xml.ts è un involucro sottile su queste.

import { LOCALES, DEFAULT_LOCALE, normalizePath, type Locale } from './i18n.ts'
import { BRAND_KEYS, BRAND_SLUGS } from './routes.ts'
import { publishedLegalPaths } from './legal.ts'
import { publishedLandings } from './landings.ts'
import type { Landing } from '../content/landings/types.ts'

/** Una pagina in tutte le lingue in cui esiste: `path` è il percorso dentro la lingua. */
export type SitemapGroup = Array<{ locale: Locale; path: string }>

/**
 * Raccoglie i gruppi di pagine pubbliche localizzate: home, brand (slug localizzati),
 * legali `published` (slug non tradotto, per le lingue presenti) e landing `published`
 * (slug per-lingua). Ogni gruppo tiene insieme le varianti di lingua di UNA pagina,
 * così `renderSitemap` può scrivere gli hreflang alternates coerenti.
 */
export function collectSitemapGroups(landings?: readonly Landing[]): SitemapGroup[] {
  const groups: SitemapGroup[] = []

  // Home: /<lang>/ per ogni lingua.
  groups.push(LOCALES.map((locale) => ({ locale, path: '/' })))

  // Brand: slug localizzato per lingua.
  for (const key of BRAND_KEYS) {
    groups.push(LOCALES.map((locale) => ({ locale, path: normalizePath(`/${BRAND_SLUGS[key][locale]}/`) })))
  }

  // Legali: stesso slug per tutte le lingue, ma solo per le lingue pubblicate.
  const legalLangs = new Map<string, Set<Locale>>()
  for (const { component, lang } of publishedLegalPaths()) {
    if (!legalLangs.has(component)) legalLangs.set(component, new Set())
    legalLangs.get(component)!.add(lang)
  }
  for (const [component, langs] of legalLangs) {
    groups.push(
      LOCALES.filter((l) => langs.has(l)).map((locale) => ({ locale, path: `/legal/${component}/` })),
    )
  }

  // Landing pubblicate: slug per-lingua.
  for (const landing of publishedLandings(landings)) {
    groups.push(LOCALES.map((locale) => ({ locale, path: normalizePath(`/${landing.content[locale].slug}/`) })))
  }

  return groups
}

const xmlEscape = (s: string): string =>
  s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')

/** URL assoluto di una variante (`/<lang><path>`) a partire dalla base del sito. */
function urlFor(base: URL, locale: Locale, path: string): string {
  return new URL(`/${locale}${normalizePath(path)}`, base).toString()
}

/**
 * Rende l'XML della sitemap. Ogni variante di lingua è una `<url>`, con gli
 * `<xhtml:link rel="alternate" hreflang="…">` verso tutte le varianti presenti più
 * `x-default` (lingua di default se presente, altrimenti la prima). Gli alternates
 * puntano ai percorsi REALI di ogni lingua — è qui che gli slug localizzati diventano
 * hreflang corretti.
 */
export function renderSitemap(base: URL, groups: SitemapGroup[]): string {
  const lines: string[] = [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">',
  ]
  for (const group of groups) {
    if (group.length === 0) continue
    const xDefault = group.find((v) => v.locale === DEFAULT_LOCALE) ?? group[0]
    for (const variant of group) {
      lines.push('  <url>')
      lines.push(`    <loc>${xmlEscape(urlFor(base, variant.locale, variant.path))}</loc>`)
      for (const alt of group) {
        lines.push(
          `    <xhtml:link rel="alternate" hreflang="${alt.locale}" href="${xmlEscape(urlFor(base, alt.locale, alt.path))}" />`,
        )
      }
      lines.push(
        `    <xhtml:link rel="alternate" hreflang="x-default" href="${xmlEscape(urlFor(base, xDefault.locale, xDefault.path))}" />`,
      )
      lines.push('  </url>')
    }
  }
  lines.push('</urlset>')
  return lines.join('\n') + '\n'
}
