import { describe, it, expect } from 'vitest'
import { createI18n, resources, LANGUAGES } from './index'

describe('createI18n', () => {
  it('risolve le chiavi in EN di default e cambia a IT a runtime', async () => {
    const i18n = createI18n()
    expect(i18n.t('nav.platform')).toBe('Platform')
    await i18n.changeLanguage('it')
    expect(i18n.t('nav.platform')).toBe('Piattaforma')
  })

  it('EN e IT espongono lo stesso insieme di chiavi (nessuna mancante)', () => {
    const keys = (obj: object, prefix = ''): string[] =>
      Object.entries(obj).flatMap(([k, v]) =>
        typeof v === 'object' && v !== null ? keys(v, `${prefix}${k}.`) : [`${prefix}${k}`],
      )
    const en = keys(resources.en.translation).sort()
    const it = keys(resources.it.translation).sort()
    expect(it).toEqual(en)
  })

  it('espone EN e IT', () => {
    expect(LANGUAGES).toEqual(['en', 'it'])
  })
})
