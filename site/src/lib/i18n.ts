// i18n del sito vetrina (UC 0036, #14 14): le 5 lingue del marketplace, con EN
// come default/sorgente. È indipendente dal pacchetto @appgrove/i18n delle SPA
// (react-i18next, runtime): qui l'i18n è statico e guidato dai contenuti.

export const LOCALES = ['en', 'it', 'fr', 'es', 'de'] as const
export type Locale = (typeof LOCALES)[number]

export const DEFAULT_LOCALE: Locale = 'en'

/** Nome nativo di ciascuna lingua, per il selettore. */
export const LOCALE_LABELS: Record<Locale, string> = {
  en: 'English',
  it: 'Italiano',
  fr: 'Français',
  es: 'Español',
  de: 'Deutsch',
}

export function isLocale(value: string): value is Locale {
  return (LOCALES as readonly string[]).includes(value)
}

/**
 * Costruisce le coppie hreflang di una pagina: una entry per ciascuna lingua più
 * `x-default` (che punta alla lingua di default). `pathWithinLocale` è il percorso
 * DOPO il segmento di lingua, con slash iniziale e finale (es. `/legal/privacy/`).
 * Gli URL sono relativi alla radice; `Astro.site` li rende assoluti negli hreflang.
 */
export function hreflangAlternates(pathWithinLocale: string): Array<{ hreflang: string; href: string }> {
  const path = normalizePath(pathWithinLocale)
  const alternates = LOCALES.map((loc) => ({ hreflang: loc, href: `/${loc}${path}` }))
  alternates.push({ hreflang: 'x-default', href: `/${DEFAULT_LOCALE}${path}` })
  return alternates
}

/**
 * Come `hreflangAlternates`, ma con un percorso DIVERSO per lingua (slug localizzati,
 * UC 0040): per ogni lingua l'hreflang punta al SUO percorso reale, e `x-default`
 * al percorso della lingua di default. `pathsByLocale[loc]` è il percorso dentro la
 * lingua `loc` (con slash iniziale/finale). È il presidio contro gli hreflang rotti
 * quando lo slug cambia da lingua a lingua (es. /en/why/ ↔ /it/perche/).
 */
export function hreflangAlternatesByLocale(
  pathsByLocale: Record<Locale, string>,
): Array<{ hreflang: string; href: string }> {
  const alternates = LOCALES.map((loc) => ({
    hreflang: loc as string,
    href: `/${loc}${normalizePath(pathsByLocale[loc])}`,
  }))
  alternates.push({ hreflang: 'x-default', href: `/${DEFAULT_LOCALE}${normalizePath(pathsByLocale[DEFAULT_LOCALE])}` })
  return alternates
}

/** Garantisce slash iniziale e finale (coerente con trailingSlash: 'always'). */
export function normalizePath(path: string): string {
  let p = path.startsWith('/') ? path : `/${path}`
  if (!p.endsWith('/')) p = `${p}/`
  return p
}
