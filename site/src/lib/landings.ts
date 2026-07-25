// Logica del registro delle landing per-app (UC 0038): gate draft/published,
// parametri statici delle rotte, validazione. È il punto in cui si applica il
// GATE DI PUBBLICAZIONE (#14 52): solo le landing `published` diventano pagine —
// una bozza non compare in getStaticPaths, quindi non finisce mai in dist/.

import { LANDINGS } from '../content/landings/index.ts'
import type { Landing } from '../content/landings/types.ts'
import { LOCALES, type Locale } from './i18n.ts'

/**
 * Slug riservati: percorsi già occupati da pagine statiche del sito. Una landing
 * NON può usarli (validazione), così lo slug localizzato alla radice della lingua
 * (dec. #14 31, es. /en/invoicing/) non collide con /why/, /pricing/, /legal/…
 * In Astro le rotte statiche hanno comunque priorità sulla dinamica [slug]: la
 * lista rende il vincolo esplicito e testato invece che implicito.
 */
export const RESERVED_SLUGS: ReadonlySet<string> = new Set([
  'why',
  'pricing',
  'legal',
  'apps',
  'blog',
  'support',
  'coming-soon',
])

/** Le sole landing pubblicate (gate #14 52). */
export function publishedLandings(landings: readonly Landing[] = LANDINGS): Landing[] {
  return landings.filter((l) => l.status === 'published')
}

/**
 * Parametri statici per la rotta [lang]/[slug].astro: una entry per ogni landing
 * PUBBLICATA × lingua, con lo slug localizzato come parametro e la landing + lingua
 * come props. Le bozze sono escluse per costruzione.
 */
export function landingParams(landings: readonly Landing[] = LANDINGS): Array<{
  params: { lang: Locale; slug: string }
  props: { landing: Landing; lang: Locale }
}> {
  const out = []
  for (const landing of publishedLandings(landings)) {
    for (const lang of LOCALES) {
      out.push({
        params: { lang, slug: landing.content[lang].slug },
        props: { landing, lang },
      })
    }
  }
  return out
}

/**
 * Valida il registro delle landing: presenza di tutte le lingue, slug ben formati,
 * non riservati e non duplicati (per lingua). Ritorna la lista degli errori (vuota
 * se tutto a posto). Chiamata dal test; è il presidio contro slug che collidono con
 * le pagine statiche o fra due landing.
 */
export function validateLandings(landings: readonly Landing[] = LANDINGS): string[] {
  const errors: string[] = []
  const seen = new Map<string, string>() // `${lang}:${slug}` → appId

  for (const l of landings) {
    for (const lang of LOCALES) {
      const c = l.content[lang]
      if (!c) {
        errors.push(`${l.appId}: manca la lingua "${lang}"`)
        continue
      }
      const slug = c.slug
      if (!slug || !/^[a-z0-9-]+$/.test(slug)) {
        errors.push(`${l.appId}/${lang}: slug non valido "${slug}" (atteso minuscolo [a-z0-9-])`)
        continue
      }
      if (RESERVED_SLUGS.has(slug)) {
        errors.push(`${l.appId}/${lang}: slug riservato "${slug}" (collide con una pagina statica)`)
      }
      const key = `${lang}:${slug}`
      const prev = seen.get(key)
      if (prev && prev !== l.appId) {
        errors.push(`slug duplicato "${slug}" in lingua "${lang}" fra ${prev} e ${l.appId}`)
      }
      seen.set(key, l.appId)
    }
  }
  return errors
}
