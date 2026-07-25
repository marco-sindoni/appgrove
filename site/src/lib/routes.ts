// Registro dei percorsi localizzati per lingua del sito vetrina (UC 0040, SEO tecnico).
//
// Nasce per due esigenze legate fra loro:
//   1. gli hreflang devono puntare al percorso REALE di ogni lingua — non allo
//      stesso percorso per tutte (difetto corretto in questa change: le landing
//      hanno slug diversi per lingua, quindi un unico percorso genera hreflang rotti);
//   2. le pagine brand (why, pricing) hanno slug LOCALIZZATI (#14 31), es.
//      /it/perche/ vs /en/why/, legati fra loro dagli hreflang.
//
// Le pagine brand non possono restare file statici (il loro slug varia per lingua):
// sono servite dal dispatcher [lang]/[slug].astro insieme alle landing. Questo modulo
// è la fonte unica dei loro slug, dei parametri statici e delle mappe hreflang.

import { LOCALES, normalizePath, type Locale } from './i18n.ts'

/** Pagine brand con slug localizzato per lingua. La home resta senza slug (/<lang>/). */
export type BrandPageKey = 'why' | 'pricing'

export const BRAND_KEYS: readonly BrandPageKey[] = ['why', 'pricing'] as const

/**
 * Slug localizzato per lingua di ciascuna pagina brand (#14 31). Minuscolo, [a-z0-9-].
 * Approvati nella revisione dei requisiti della change 0050.
 */
export const BRAND_SLUGS: Record<BrandPageKey, Record<Locale, string>> = {
  why: { en: 'why', it: 'perche', fr: 'pourquoi', es: 'por-que', de: 'warum' },
  pricing: { en: 'pricing', it: 'prezzi', fr: 'tarifs', es: 'precios', de: 'preise' },
}

/** Percorso DENTRO la lingua (slash iniziale e finale) di una pagina brand, es. `/perche/`. */
export function brandPath(key: BrandPageKey, lang: Locale): string {
  return normalizePath(`/${BRAND_SLUGS[key][lang]}/`)
}

/** Link interno (con segmento di lingua) a una pagina brand, es. `/it/perche/`. */
export function brandHref(key: BrandPageKey, lang: Locale): string {
  return `/${lang}${brandPath(key, lang)}`
}

/** Mappa `Record<Locale, path>` degli hreflang di una pagina brand (slug per-lingua). */
export function brandHreflangPaths(key: BrandPageKey): Record<Locale, string> {
  return Object.fromEntries(LOCALES.map((loc) => [loc, brandPath(key, loc)])) as Record<Locale, string>
}

/**
 * Mappa hreflang da uno slug localizzato per lingua (usata dalle landing:
 * `landing.content[loc].slug`), es. `{ en: '/invoicing/', it: '/fatture/' }`.
 */
export function slugHreflangPaths(slugByLocale: Record<Locale, string>): Record<Locale, string> {
  return Object.fromEntries(
    LOCALES.map((loc) => [loc, normalizePath(`/${slugByLocale[loc]}/`)]),
  ) as Record<Locale, string>
}

/** Props di una pagina brand emessa dal dispatcher [lang]/[slug].astro. */
export interface BrandRouteProps {
  kind: 'brand'
  brandKey: BrandPageKey
  lang: Locale
}

/**
 * Parametri statici delle pagine brand per il dispatcher [lang]/[slug].astro:
 * una entry per ciascuna pagina brand × lingua, con lo slug localizzato come parametro.
 */
export function brandParams(): Array<{ params: { lang: Locale; slug: string }; props: BrandRouteProps }> {
  const out: Array<{ params: { lang: Locale; slug: string }; props: BrandRouteProps }> = []
  for (const key of BRAND_KEYS) {
    for (const lang of LOCALES) {
      out.push({ params: { lang, slug: BRAND_SLUGS[key][lang] }, props: { kind: 'brand', brandKey: key, lang } })
    }
  }
  return out
}
