import { describe, it, expect } from 'vitest'
import { buildLlmsTxt } from './llms.ts'
import { LOCALES, DEFAULT_LOCALE } from './i18n.ts'
import { MARKETING } from '../content/marketing/index.ts'
import type { Landing } from '../content/landings/types.ts'

const SITE = 'https://appgrove.app'

describe('llms.txt multilingua (UC 0041)', () => {
  for (const locale of LOCALES) {
    it(`[${locale}] titolo, sommario localizzato e link assoluti risolvibili`, () => {
      const body = buildLlmsTxt({ locale, siteUrl: SITE })
      // Titolo di marca.
      expect(body.startsWith('# appgrove')).toBe(true)
      // Sommario = sottotitolo hero della lingua (localizzato).
      expect(body).toContain(MARKETING[locale].hero.subtitle)
      // I link puntano al percorso della SUA lingua, assoluti.
      expect(body).toContain(`${SITE}/${locale}/`)
      // Nessun link a un'altra lingua nelle sezioni pagine (a parte la sezione lingue, qui assente).
      const otherLocales = LOCALES.filter((l) => l !== locale)
      for (const other of otherLocales) {
        expect(body).not.toContain(`${SITE}/${other}/`)
      }
    })
  }

  it('la variante radice elenca tutte le lingue', () => {
    const body = buildLlmsTxt({ locale: DEFAULT_LOCALE, siteUrl: SITE, includeLanguages: true })
    for (const loc of LOCALES) {
      expect(body).toContain(`${SITE}/${loc}/llms.txt`)
    }
  })

  it('senza landing pubblicate: riga onesta "nessuna app"', () => {
    // Registro esplicitamente senza pubblicate (solo bozze) → la sezione App è onesta.
    const body = buildLlmsTxt({ locale: DEFAULT_LOCALE, siteUrl: SITE, landings: [] })
    expect(body).toMatch(/No apps are published yet/i)
  })

  it('il registro reale espone la landing pubblicata dell\'app #1 "fatture" (UC 0053)', () => {
    // Dopo UC 0053 la landing di fatture è published → compare fra le App di llms.txt.
    const body = buildLlmsTxt({ locale: 'it', siteUrl: SITE })
    expect(body).not.toMatch(/No apps are published yet/i)
    expect(body).toContain(`${SITE}/it/fatture/`)
  })

  it('con una landing pubblicata: compare fra le App', () => {
    const published: Landing = {
      appId: 'demo',
      status: 'published',
      content: Object.fromEntries(
        LOCALES.map((l) => [
          l,
          {
            slug: `demo-${l}`,
            meta: { title: `Demo ${l}`, description: `Descr ${l}`, ogImage: null },
            hero: { badge: '', title: '', subtitle: '', ctaPrimary: '', ctaSecondary: '', screenshot: { src: null, alt: '' } },
            problemSolution: { title: '', problem: '', solution: '' },
            features: { title: '', subtitle: '', items: [] },
            howItWorks: { title: '', steps: [] },
            pricing: { title: '', subtitle: '', monthlyLabel: '', yearlyLabel: '', trialNote: '', tiers: [] },
            privacy: { title: '', body: '', points: [] },
            faq: { title: '', items: [] },
            finalCta: { title: '', body: '', primary: '', secondary: '' },
          },
        ]),
      ) as Landing['content'],
    }
    const body = buildLlmsTxt({ locale: 'it', siteUrl: SITE, landings: [published] })
    expect(body).toContain(`${SITE}/it/demo-it/`)
    expect(body).toContain('Demo it')
  })
})
