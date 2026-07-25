import { describe, it, expect } from 'vitest'
import { MARKETING } from './index.ts'
import { LOCALES, DEFAULT_LOCALE } from '../../lib/i18n.ts'

// La parità di FORMA è già garantita a compile-time dal tipo Record<Locale,
// MarketingContent>. Questi test coprono i VALORI a runtime: nessuna stringa vuota
// e stessa forma (chiavi + lunghezza delle liste) di ogni lingua rispetto alla
// sorgente EN — così una traduzione con una voce di lista in meno non passa.

/** Percorsi di tutte le stringhe foglia (per messaggi d'errore leggibili). */
function collectStrings(value: unknown, prefix: string, out: Array<[string, string]>): void {
  if (typeof value === 'string') {
    out.push([prefix, value])
  } else if (Array.isArray(value)) {
    value.forEach((v, i) => collectStrings(v, `${prefix}[${i}]`, out))
  } else if (value && typeof value === 'object') {
    for (const [k, v] of Object.entries(value)) collectStrings(v, prefix ? `${prefix}.${k}` : k, out)
  }
}

/** Firma di forma: chiavi ordinate e lunghezze degli array, ricorsivamente. */
function shape(value: unknown): unknown {
  if (Array.isArray(value)) return { __len: value.length, items: value.map(shape) }
  if (value && typeof value === 'object') {
    const o: Record<string, unknown> = {}
    for (const k of Object.keys(value).sort()) o[k] = shape((value as Record<string, unknown>)[k])
    return o
  }
  return typeof value // le foglie contano come "string", non il loro valore
}

describe('contenuti marketing (UC 0037)', () => {
  it('espone tutte e 5 le lingue', () => {
    expect(Object.keys(MARKETING).sort()).toEqual([...LOCALES].sort())
  })

  for (const lang of LOCALES) {
    it(`[${lang}] nessuna stringa vuota o di soli spazi`, () => {
      const strings: Array<[string, string]> = []
      collectStrings(MARKETING[lang], '', strings)
      const empty = strings.filter(([, s]) => s.trim().length === 0).map(([p]) => p)
      expect(empty, `stringhe vuote in ${lang}: ${empty.join(', ')}`).toEqual([])
      expect(strings.length).toBeGreaterThan(50)
    })
  }

  const reference = shape(MARKETING[DEFAULT_LOCALE])
  for (const lang of LOCALES) {
    if (lang === DEFAULT_LOCALE) continue
    it(`[${lang}] stessa forma della sorgente (${DEFAULT_LOCALE}) — chiavi e lunghezze liste`, () => {
      expect(shape(MARKETING[lang])).toEqual(reference)
    })
  }
})
