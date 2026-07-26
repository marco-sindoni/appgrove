import { describe, it, expect } from 'vitest'
import { BLOG_POSTS } from './index.ts'
import { BLOG_UI } from './ui.ts'
import { LOCALES, DEFAULT_LOCALE } from '../../lib/i18n.ts'

// La parità di FORMA è già garantita a compile-time dal tipo Record<Locale, …>. Questi
// test coprono i VALORI a runtime: nessuna stringa vuota e stessa forma (chiavi + lunghezza
// delle liste) di ogni lingua rispetto alla sorgente EN — così una traduzione con una
// sezione o una voce FAQ in meno non passa. Mirror del test marketing (UC 0037).

function collectStrings(value: unknown, prefix: string, out: Array<[string, string]>): void {
  if (typeof value === 'string') {
    out.push([prefix, value])
  } else if (Array.isArray(value)) {
    value.forEach((v, i) => collectStrings(v, `${prefix}[${i}]`, out))
  } else if (value && typeof value === 'object') {
    for (const [k, v] of Object.entries(value)) collectStrings(v, prefix ? `${prefix}.${k}` : k, out)
  }
}

function shape(value: unknown): unknown {
  if (Array.isArray(value)) return { __len: value.length, items: value.map(shape) }
  if (value && typeof value === 'object') {
    const o: Record<string, unknown> = {}
    for (const k of Object.keys(value).sort()) o[k] = shape((value as Record<string, unknown>)[k])
    return o
  }
  return typeof value
}

describe('contenuti blog (UC 0042)', () => {
  it('ci sono post nel registro', () => {
    expect(BLOG_POSTS.length).toBeGreaterThan(0)
  })

  for (const post of BLOG_POSTS) {
    it(`[${post.key}] espone tutte e 5 le lingue`, () => {
      expect(Object.keys(post.content).sort()).toEqual([...LOCALES].sort())
    })

    for (const lang of LOCALES) {
      it(`[${post.key}/${lang}] nessuna stringa vuota o di soli spazi`, () => {
        const strings: Array<[string, string]> = []
        collectStrings(post.content[lang], '', strings)
        const empty = strings.filter(([, s]) => s.trim().length === 0).map(([p]) => p)
        expect(empty, `stringhe vuote in ${post.key}/${lang}: ${empty.join(', ')}`).toEqual([])
        expect(strings.length).toBeGreaterThan(10)
      })
    }

    const reference = shape(post.content[DEFAULT_LOCALE])
    for (const lang of LOCALES) {
      if (lang === DEFAULT_LOCALE) continue
      it(`[${post.key}/${lang}] stessa forma della sorgente (${DEFAULT_LOCALE})`, () => {
        expect(shape(post.content[lang])).toEqual(reference)
      })
    }
  }

  // Stringhe di contorno del blog (indice + cornice pagina): parità 5 lingue.
  describe('stringhe di contorno (BLOG_UI)', () => {
    it('espone tutte e 5 le lingue', () => {
      expect(Object.keys(BLOG_UI).sort()).toEqual([...LOCALES].sort())
    })
    for (const lang of LOCALES) {
      it(`[${lang}] nessuna stringa vuota`, () => {
        const strings: Array<[string, string]> = []
        collectStrings(BLOG_UI[lang], '', strings)
        const empty = strings.filter(([, s]) => s.trim().length === 0).map(([p]) => p)
        expect(empty).toEqual([])
      })
    }
    const reference = shape(BLOG_UI[DEFAULT_LOCALE])
    for (const lang of LOCALES) {
      if (lang === DEFAULT_LOCALE) continue
      it(`[${lang}] stessa forma della sorgente`, () => {
        expect(shape(BLOG_UI[lang])).toEqual(reference)
      })
    }
  })
})
