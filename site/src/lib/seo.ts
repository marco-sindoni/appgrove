// Costruttori dei dati strutturati Schema.org (JSON-LD) del sito vetrina (UC 0040).
//
// Nessun testo cablato: i nodi sono derivati dai contenuti già strutturati
// (marketing UC 0037, landing UC 0038) e dai dati del titolare (entity.yaml). Il
// rendering come <script type="application/ld+json"> avviene nel BaseLayout, che
// aggiunge SEMPRE Organization e riceve dalle pagine i nodi specifici (BreadcrumbList,
// SoftwareApplication, FAQPage, Article). Article è aggiunto dal blog (UC 0042).

import type { LandingLocaleContent } from '../content/landings/types.ts'
import { BRAND } from './brand.ts'

/** Marchio del marketplace: NON la ragione sociale del titolare (che è "DA COMPILARE"). */
export const BRAND_NAME = 'appgrove'

/** Simbolo di valuta → codice ISO 4217. */
const CURRENCY_BY_SYMBOL: Record<string, string> = { '€': 'EUR', $: 'USD', '£': 'GBP' }

/**
 * Estrae prezzo e valuta da una stringa di listino tollerante ('€9', '€9 / mo',
 * '€0', '9,99 €'). Ritorna null se non contiene un numero: in quel caso l'offerta
 * viene emessa SENZA prezzo, mai come JSON-LD malformato. La virgola decimale è
 * normalizzata a punto. Valuta di default EUR (mercato UE).
 */
export function parsePrice(raw: string): { price: string; priceCurrency: string } | null {
  const match = raw.match(/\d+(?:[.,]\d+)?/)
  if (!match) return null
  const symbol = Object.keys(CURRENCY_BY_SYMBOL).find((s) => raw.includes(s))
  return { price: match[0].replace(',', '.'), priceCurrency: symbol ? CURRENCY_BY_SYMBOL[symbol] : 'EUR' }
}

/**
 * Organization del marchio appgrove: presente su ogni pagina (aggiunta dal layout).
 * Porta la descrizione CANONICA (boilerplate `brand.ts`, UC 0041): uno statement fattuale
 * machine-readable, identico ovunque, che aiuta gli assistenti AI a citare correttamente.
 */
export function organizationJsonLd(siteUrl: string, supportEmail: string): Record<string, unknown> {
  return {
    '@context': 'https://schema.org',
    '@type': 'Organization',
    name: BRAND_NAME,
    url: siteUrl,
    slogan: BRAND.tagline,
    description: BRAND.description,
    contactPoint: {
      '@type': 'ContactPoint',
      email: supportEmail,
      contactType: 'customer support',
    },
  }
}

export interface Crumb {
  name: string
  /** URL assoluto della tappa. */
  url: string
}

/** Breadcrumb (Home → pagina) delle pagine interne. */
export function breadcrumbJsonLd(crumbs: Crumb[]): Record<string, unknown> {
  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: crumbs.map((c, i) => ({
      '@type': 'ListItem',
      position: i + 1,
      name: c.name,
      item: c.url,
    })),
  }
}

/** SoftwareApplication di una landing, con le offerte derivate dai tier di prezzo. */
export function softwareApplicationJsonLd(opts: {
  name: string
  description: string
  url: string
  tiers: { name: string; priceMonthly: string; priceYearly: string }[]
}): Record<string, unknown> {
  const offers: Array<Record<string, unknown>> = []
  for (const tier of opts.tiers) {
    for (const cycle of ['monthly', 'yearly'] as const) {
      const raw = cycle === 'monthly' ? tier.priceMonthly : tier.priceYearly
      const parsed = parsePrice(raw)
      const offer: Record<string, unknown> = {
        '@type': 'Offer',
        name: `${tier.name} (${cycle})`,
        category: cycle,
      }
      if (parsed) {
        offer.price = parsed.price
        offer.priceCurrency = parsed.priceCurrency
      }
      offers.push(offer)
    }
  }
  return {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    name: opts.name,
    description: opts.description,
    url: opts.url,
    applicationCategory: 'BusinessApplication',
    operatingSystem: 'Web',
    offers,
  }
}

/** FAQPage dalla sezione FAQ del template landing. */
export function faqPageJsonLd(items: { q: string; a: string }[]): Record<string, unknown> {
  return {
    '@context': 'https://schema.org',
    '@type': 'FAQPage',
    mainEntity: items.map((it) => ({
      '@type': 'Question',
      name: it.q,
      acceptedAnswer: { '@type': 'Answer', text: it.a },
    })),
  }
}

/** Grafo JSON-LD specifico di una landing: SoftwareApplication (+offerte) + FAQPage. */
export function landingJsonLd(content: LandingLocaleContent, url: string): Record<string, unknown>[] {
  return [
    softwareApplicationJsonLd({
      name: content.meta.title,
      description: content.meta.description,
      url,
      tiers: content.pricing.tiers,
    }),
    faqPageJsonLd(content.faq.items),
  ]
}

/**
 * Article di una pagina blog (UC 0042): il nodo Schema.org che gli assistenti AI e i
 * motori usano per capire e citare un contenuto editoriale. `headline`, `inLanguage` e
 * `datePublished` sono i campi portanti; `publisher` riusa l'identità del marchio, `author`
 * è l'organizzazione (contenuti AI-generati on-brand, dec. #14 35). Le FAQ della pagina
 * restano un nodo FAQPage separato (faqPageJsonLd), come per landing e pagine brand.
 */
export function articleJsonLd(opts: {
  headline: string
  description: string
  url: string
  inLanguage: string
  datePublished: string
}): Record<string, unknown> {
  return {
    '@context': 'https://schema.org',
    '@type': 'Article',
    headline: opts.headline,
    description: opts.description,
    inLanguage: opts.inLanguage,
    datePublished: opts.datePublished,
    mainEntityOfPage: { '@type': 'WebPage', '@id': opts.url },
    author: { '@type': 'Organization', name: BRAND_NAME },
    publisher: { '@type': 'Organization', name: BRAND_NAME },
  }
}

/**
 * Serializza un nodo JSON-LD per l'inserimento in <script type="application/ld+json">.
 * Neutralizza `<` (→ <) così una stringa di contenuto non può chiudere il tag.
 */
export function serializeJsonLd(node: unknown): string {
  return JSON.stringify(node).replace(/</g, '\\u003c')
}
