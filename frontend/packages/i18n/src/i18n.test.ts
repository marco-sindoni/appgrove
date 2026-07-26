import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import {
  createI18n,
  resources,
  LANGUAGES,
  DEFAULT_LANGUAGE,
  detectLanguage,
  persistLanguage,
  LANGUAGE_STORAGE_KEY,
} from './index'

const flatKeys = (obj: object, prefix = ''): string[] =>
  Object.entries(obj).flatMap(([k, v]) =>
    typeof v === 'object' && v !== null ? flatKeys(v, `${prefix}${k}.`) : [`${prefix}${k}`],
  )

describe('createI18n', () => {
  it('risolve le chiavi in EN di default e cambia a IT a runtime', async () => {
    const i18n = createI18n()
    expect(i18n.t('nav.platform')).toBe('Platform')
    await i18n.changeLanguage('it')
    expect(i18n.t('nav.platform')).toBe('Piattaforma')
  })

  it('parte dalla lingua richiesta', () => {
    expect(createI18n('fr').language).toBe('fr')
  })
})

describe('parità dei cataloghi', () => {
  it('espone le 5 lingue del sito', () => {
    expect(LANGUAGES).toEqual(['en', 'it', 'fr', 'es', 'de'])
  })

  it('ogni lingua espone lo stesso insieme di chiavi di EN (nessuna mancante o in eccesso)', () => {
    const enKeys = flatKeys(resources.en.translation).sort()
    for (const lng of LANGUAGES) {
      const keys = flatKeys(resources[lng].translation).sort()
      expect(keys, `lingua ${lng}`).toEqual(enKeys)
    }
  })

  it('nessun valore vuoto in nessuna lingua', () => {
    for (const lng of LANGUAGES) {
      const empty = Object.entries(
        // valori foglia appiattiti con la loro chiave
        flatKeys(resources[lng].translation).reduce<Record<string, unknown>>((acc, key) => {
          const value = key
            .split('.')
            .reduce<unknown>((o, k) => (o as Record<string, unknown>)?.[k], resources[lng].translation)
          acc[key] = value
          return acc
        }, {}),
      ).filter(([, v]) => typeof v !== 'string' || v.trim() === '')
      expect(empty, `lingua ${lng}`).toEqual([])
    }
  })
})

describe('detectLanguage / persistLanguage', () => {
  // Simula un ambiente browser (il pacchetto gira in env `node`): localStorage su Map + navigator.
  function stubWindow(navigatorLanguage: string | undefined) {
    const store = new Map<string, string>()
    vi.stubGlobal('window', {
      localStorage: {
        getItem: (k: string) => store.get(k) ?? null,
        setItem: (k: string, v: string) => void store.set(k, v),
        clear: () => store.clear(),
      },
      navigator: { language: navigatorLanguage },
    })
  }

  beforeEach(() => stubWindow('en-US'))
  afterEach(() => vi.unstubAllGlobals())

  it('usa la preferenza salvata quando è supportata', () => {
    persistLanguage('de')
    expect(window.localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('de')
    expect(detectLanguage()).toBe('de')
  })

  it('ignora una preferenza non supportata e ricade sulla lingua del browser', () => {
    stubWindow('fr-FR')
    window.localStorage.setItem(LANGUAGE_STORAGE_KEY, 'xx')
    expect(detectLanguage()).toBe('fr')
  })

  it('ricade sul default quando browser e preferenza non sono supportati', () => {
    stubWindow('ja-JP')
    expect(detectLanguage()).toBe(DEFAULT_LANGUAGE)
    expect(DEFAULT_LANGUAGE).toBe('en')
  })

  it('senza window (test/SSR) torna il default senza errori', () => {
    vi.unstubAllGlobals()
    vi.stubGlobal('window', undefined)
    expect(detectLanguage()).toBe(DEFAULT_LANGUAGE)
    expect(() => persistLanguage('it')).not.toThrow()
  })
})
