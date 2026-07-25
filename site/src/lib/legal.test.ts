import { describe, it, expect } from 'vitest'
import {
  LEGAL_COMPONENTS,
  isPublished,
  loadEntity,
  substituteTokens,
  loadLegalDoc,
  publishedLegalPaths,
} from './legal.ts'
import { LOCALES, hreflangAlternates, DEFAULT_LOCALE } from './i18n.ts'

describe('substituteTokens', () => {
  const entity = { 'titolare.ragione_sociale': 'ACME', 'titolare.forma': 'ditta individuale' }

  it('sostituisce i token noti con il valore', () => {
    expect(substituteTokens('X {{titolare.ragione_sociale}} Y', entity)).toBe('X ACME Y')
  })

  it('gestisce spazi interni al token', () => {
    expect(substituteTokens('{{ titolare.forma }}', entity)).toBe('ditta individuale')
  })

  it('lancia su token orfano (integrità referenziale)', () => {
    expect(() => substituteTokens('{{titolare.inesistente}}', entity)).toThrow(/non risolto/)
  })
})

describe('isPublished (gate di pubblicazione)', () => {
  it('pubblicato quando status assente (legali)', () => {
    expect(isPublished({})).toBe(true)
  })
  it('pubblicato quando status === published', () => {
    expect(isPublished({ status: 'published' })).toBe(true)
  })
  it('non pubblicato quando status === draft', () => {
    expect(isPublished({ status: 'draft' })).toBe(false)
  })
})

describe('hreflangAlternates', () => {
  it('emette una entry per lingua più x-default', () => {
    const alts = hreflangAlternates('/legal/privacy/')
    expect(alts.map((a) => a.hreflang)).toEqual([...LOCALES, 'x-default'])
    expect(alts.find((a) => a.hreflang === 'x-default')!.href).toBe(`/${DEFAULT_LOCALE}/legal/privacy/`)
    expect(alts.find((a) => a.hreflang === 'it')!.href).toBe('/it/legal/privacy/')
  })
})

describe('loadEntity', () => {
  it('appiattisce entity.yaml in chiavi puntate titolare.*', () => {
    const flat = loadEntity()
    expect(flat['titolare.dominio']).toBe('appgrove.app')
    expect(typeof flat['titolare.email_privacy']).toBe('string')
  })
})

describe('contenuti legali reali', () => {
  it('ogni componente esiste in tutte le 5 lingue (parità)', () => {
    const paths = publishedLegalPaths()
    for (const component of LEGAL_COMPONENTS) {
      const langs = paths.filter((p) => p.component === component).map((p) => p.lang).sort()
      expect(langs).toEqual([...LOCALES].sort())
    }
  })

  it('ogni documento renderizza senza token {{ residui', () => {
    for (const { component, lang } of publishedLegalPaths()) {
      const doc = loadLegalDoc(component, lang)
      expect(doc.html, `${component}.${lang}`).not.toMatch(/\{\{/)
      expect(doc.frontmatter.lang).toBe(lang)
    }
  })
})
