import { describe, it, expect } from 'vitest'
import {
  parsePrice,
  organizationJsonLd,
  breadcrumbJsonLd,
  softwareApplicationJsonLd,
  faqPageJsonLd,
  landingJsonLd,
  serializeJsonLd,
  BRAND_NAME,
} from './seo.ts'
import { LANDINGS } from '../content/landings/index.ts'
import { DEFAULT_LOCALE } from './i18n.ts'

describe('parser prezzo (UC 0040)', () => {
  it('estrae numero e valuta da formati tolleranti', () => {
    expect(parsePrice('€9')).toEqual({ price: '9', priceCurrency: 'EUR' })
    expect(parsePrice('€9 / mo')).toEqual({ price: '9', priceCurrency: 'EUR' })
    expect(parsePrice('€0')).toEqual({ price: '0', priceCurrency: 'EUR' })
    expect(parsePrice('9,99 €')).toEqual({ price: '9.99', priceCurrency: 'EUR' })
    expect(parsePrice('$10 / mo')).toEqual({ price: '10', priceCurrency: 'USD' })
  })

  it('ritorna null se non c’è un numero (nessun JSON-LD malformato)', () => {
    expect(parsePrice('Free')).toBeNull()
    expect(parsePrice('Su richiesta')).toBeNull()
  })
})

describe('costruttori JSON-LD (UC 0040)', () => {
  it('Organization usa il marchio, non la ragione sociale del titolare', () => {
    const org = organizationJsonLd('https://appgrove.app/', 'support@appgrove.app')
    expect(org['@type']).toBe('Organization')
    expect(org.name).toBe(BRAND_NAME)
    expect(org.name).toBe('appgrove')
    expect(org.url).toBe('https://appgrove.app/')
  })

  it('BreadcrumbList numera le tappe da 1', () => {
    const bc = breadcrumbJsonLd([
      { name: 'Home', url: 'https://x/it/' },
      { name: 'Prezzi', url: 'https://x/it/prezzi/' },
    ])
    const items = bc.itemListElement as Array<Record<string, unknown>>
    expect(items.map((i) => i.position)).toEqual([1, 2])
    expect(items[1].name).toBe('Prezzi')
  })

  it('SoftwareApplication: 2 offerte per tier; il prezzo non parsabile omette price', () => {
    const app = softwareApplicationJsonLd({
      name: 'Fatture',
      description: 'x',
      url: 'https://x/it/fatture/',
      tiers: [
        { name: 'Pro', priceMonthly: '€9 / mo', priceYearly: '€90 / yr' },
        { name: 'Enterprise', priceMonthly: 'Su richiesta', priceYearly: 'Su richiesta' },
      ],
    })
    const offers = app.offers as Array<Record<string, unknown>>
    expect(offers.length).toBe(4)
    const pro = offers.filter((o) => String(o.name).startsWith('Pro'))
    expect(pro.every((o) => 'price' in o)).toBe(true)
    const ent = offers.filter((o) => String(o.name).startsWith('Enterprise'))
    expect(ent.every((o) => !('price' in o))).toBe(true)
  })

  it('FAQPage mappa domande e risposte', () => {
    const faq = faqPageJsonLd([{ q: 'Q?', a: 'A.' }])
    const main = faq.mainEntity as Array<Record<string, unknown>>
    expect(main[0].name).toBe('Q?')
    expect((main[0].acceptedAnswer as Record<string, unknown>).text).toBe('A.')
  })

  it('landingJsonLd della fixture: SoftwareApplication + FAQPage, serializzabili e sicuri', () => {
    const c = LANDINGS[0].content[DEFAULT_LOCALE]
    const nodes = landingJsonLd(c, 'https://appgrove.app/en/example/')
    expect(nodes.map((n) => n['@type'])).toEqual(['SoftwareApplication', 'FAQPage'])
    for (const n of nodes) {
      const s = serializeJsonLd(n)
      expect(s).not.toContain('<')
      expect(() => JSON.parse(s)).not.toThrow()
    }
  })
})
