import { describe, it, expect } from 'vitest'
import {
  BRAND_KEYS,
  BRAND_SLUGS,
  brandPath,
  brandHref,
  brandHreflangPaths,
  slugHreflangPaths,
  brandParams,
} from './routes.ts'
import { LOCALES } from './i18n.ts'

// Percorsi localizzati per lingua (UC 0040): fondamento degli slug brand e degli
// hreflang corretti. La parità di FORMA (una entry per lingua) è garantita dal tipo
// Record<Locale, …>; qui si coprono i VALORI e la coerenza degli hreflang.

describe('slug brand localizzati (UC 0040)', () => {
  it('ogni pagina brand ha uno slug ben formato per ciascuna lingua', () => {
    for (const key of BRAND_KEYS) {
      for (const lang of LOCALES) {
        const slug = BRAND_SLUGS[key][lang]
        expect(slug, `${key}/${lang}`).toMatch(/^[a-z0-9-]+$/)
      }
    }
  })

  it('brandPath e brandHref compongono il percorso localizzato', () => {
    expect(brandPath('why', 'it')).toBe('/perche/')
    expect(brandPath('pricing', 'de')).toBe('/preise/')
    expect(brandHref('why', 'it')).toBe('/it/perche/')
    expect(brandHref('pricing', 'en')).toBe('/en/pricing/')
  })

  it('brandHreflangPaths punta ogni lingua al SUO percorso reale', () => {
    const paths = brandHreflangPaths('why')
    expect(paths.en).toBe('/why/')
    expect(paths.it).toBe('/perche/')
    expect(paths.fr).toBe('/pourquoi/')
    // Una entry per lingua, nessuna mancante.
    expect(Object.keys(paths).sort()).toEqual([...LOCALES].sort())
  })

  it('slugHreflangPaths costruisce la mappa da slug per-lingua (landing)', () => {
    const paths = slugHreflangPaths({ en: 'invoicing', it: 'fatture', fr: 'facturation', es: 'facturacion', de: 'rechnungen' })
    expect(paths.en).toBe('/invoicing/')
    expect(paths.it).toBe('/fatture/')
  })

  it('brandParams emette una pagina per brand × lingua con lo slug localizzato', () => {
    const params = brandParams()
    expect(params.length).toBe(BRAND_KEYS.length * LOCALES.length)
    for (const p of params) {
      expect(p.params.slug).toBe(BRAND_SLUGS[p.props.brandKey][p.params.lang])
      expect(p.props.kind).toBe('brand')
    }
  })
})
