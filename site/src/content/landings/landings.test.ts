import { describe, it, expect } from 'vitest'
import { experimental_AstroContainer as AstroContainer } from 'astro/container'
import LandingSections from '../../components/LandingSections.astro'
import { LANDINGS } from './index.ts'
import {
  publishedLandings,
  landingParams,
  validateLandings,
  reservedSlugs,
} from '../../lib/landings.ts'
import { LOCALES, DEFAULT_LOCALE } from '../../lib/i18n.ts'

// Test del template landing (UC 0038). La parità di FORMA fra lingue è già garantita
// a compile-time dal tipo Record<Locale, LandingLocaleContent>; qui si coprono i
// VALORI (nessuna stringa vuota, stessa forma della sorgente EN), il GATE
// draft/published, gli slug riservati e la RESA del template (8 sezioni).

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
  if (value === null) return 'null'
  if (Array.isArray(value)) return { __len: value.length, items: value.map(shape) }
  if (typeof value === 'object') {
    const o: Record<string, unknown> = {}
    for (const k of Object.keys(value).sort()) o[k] = shape((value as Record<string, unknown>)[k])
    return o
  }
  return typeof value
}

describe('contenuti landing (UC 0038)', () => {
  for (const landing of LANDINGS) {
    describe(`landing "${landing.appId}"`, () => {
      it('espone tutte e 5 le lingue', () => {
        expect(Object.keys(landing.content).sort()).toEqual([...LOCALES].sort())
      })

      for (const lang of LOCALES) {
        it(`[${lang}] nessuna stringa vuota o di soli spazi`, () => {
          const strings: Array<[string, string]> = []
          collectStrings(landing.content[lang], '', strings)
          const empty = strings.filter(([, s]) => s.trim().length === 0).map(([p]) => p)
          expect(empty, `stringhe vuote in ${lang}: ${empty.join(', ')}`).toEqual([])
        })
      }

      const reference = shape(landing.content[DEFAULT_LOCALE])
      for (const lang of LOCALES) {
        if (lang === DEFAULT_LOCALE) continue
        it(`[${lang}] stessa forma della sorgente (${DEFAULT_LOCALE})`, () => {
          expect(shape(landing.content[lang])).toEqual(reference)
        })
      }

      it('ha 3–6 feature e 2–3 step (vincoli #14 25)', () => {
        for (const lang of LOCALES) {
          const c = landing.content[lang]
          expect(c.features.items.length, `feature in ${lang}`).toBeGreaterThanOrEqual(3)
          expect(c.features.items.length, `feature in ${lang}`).toBeLessThanOrEqual(6)
          expect(c.howItWorks.steps.length, `step in ${lang}`).toBeGreaterThanOrEqual(2)
          expect(c.howItWorks.steps.length, `step in ${lang}`).toBeLessThanOrEqual(3)
        }
      })
    })
  }
})

describe('registro landing — validazione e gate (UC 0038)', () => {
  it('il registro è valido (slug ben formati, non riservati, non duplicati)', () => {
    expect(validateLandings()).toEqual([])
  })

  it('nessuno slug coincide con uno riservato (per la sua lingua)', () => {
    for (const landing of LANDINGS) {
      for (const lang of LOCALES) {
        expect(reservedSlugs(lang).has(landing.content[lang].slug)).toBe(false)
      }
    }
  })

  it('validateLandings segnala uno slug riservato (slug brand localizzato)', () => {
    // 'prezzi' è lo slug brand italiano (UC 0040): una landing non può rivendicarlo in it.
    const bad = [
      {
        appId: 'bad',
        status: 'published' as const,
        content: Object.fromEntries(
          LOCALES.map((l) => [l, { ...LANDINGS[0].content[l], slug: l === 'it' ? 'prezzi' : `bad-${l}` }]),
        ) as (typeof LANDINGS)[number]['content'],
      },
    ]
    const errors = validateLandings(bad)
    expect(errors.some((e) => e.includes('riservato'))).toBe(true)
  })

  it('gate: le bozze sono escluse dai path, le pubblicate incluse', () => {
    // Nel registro reale: la fixture "example" è draft (esclusa); la landing dell'app #1
    // "fatture" è published (UC 0053), quindi inclusa in tutte e 5 le lingue. Il gate è
    // verificato sui dati veri, non sull'assenza globale di pubblicate.
    const published = publishedLandings()
    expect(published.some((l) => l.appId === 'fatture'), 'fatture è pubblicata').toBe(true)
    expect(published.some((l) => l.appId === 'example'), 'la bozza example è esclusa').toBe(false)
    // Solo le pubblicate generano path: 5 lingue × numero di landing pubblicate.
    expect(landingParams().length).toBe(LOCALES.length * published.length)
    for (const p of landingParams()) {
      expect(p.props.landing.status, `path di "${p.props.landing.appId}"`).toBe('published')
    }

    // Con tutte le landing marcate published: 5 lingue × N landing.
    // Non si assume UNA sola landing: il generatore new-application (UC 0046) ne
    // aggiunge una per ogni app, e questo test deve reggere il registro cresciuto.
    const asPublished = LANDINGS.map((l) => ({ ...l, status: 'published' as const }))
    const params = landingParams(asPublished)
    expect(params.length).toBe(LOCALES.length * asPublished.length)
    // Ogni landing pubblicata compare in tutte e 5 le lingue.
    for (const landing of asPublished) {
      const langs = params.filter((p) => p.props.landing.appId === landing.appId).map((p) => p.params.lang).sort()
      expect(langs, `lingue di "${landing.appId}"`).toEqual([...LOCALES].sort())
    }
    // Ogni path porta lo slug localizzato della SUA landing.
    for (const p of params) {
      expect(p.params.slug).toBe(p.props.landing.content[p.params.lang].slug)
    }
  })
})

describe('resa del template LandingSections (UC 0038)', () => {
  it('rende le 8 sezioni con i contenuti della fixture', async () => {
    const container = await AstroContainer.create()
    const c = LANDINGS[0].content[DEFAULT_LOCALE]
    const html = await container.renderToString(LandingSections, {
      props: { content: c, lang: DEFAULT_LOCALE },
    })

    // (1) Hero + (8) CTA finale
    expect(html).toContain(c.hero.title)
    expect(html).toContain(c.finalCta.title)
    // (2) Problema→soluzione
    expect(html).toContain(c.problemSolution.problem)
    expect(html).toContain(c.problemSolution.solution)
    // (3) Feature con icone Material Symbols
    expect(html).toContain(c.features.items[0].title)
    expect(html).toContain('material-symbols-outlined')
    expect(html).toContain(c.features.items[0].icon)
    // (4) Come funziona
    expect(html).toContain('id="how-it-works"')
    expect(html).toContain(c.howItWorks.steps[0].title)
    // (5) Pricing
    expect(html).toContain(c.pricing.tiers[0].name)
    expect(html).toContain(c.pricing.trialNote)
    // (6) Privacy
    expect(html).toContain(c.privacy.title)
    // (7) FAQ
    expect(html).toContain(c.faq.items[0].q)
  })

  it('in bozza (screenshot null) usa il placeholder, non un <img>', async () => {
    const container = await AstroContainer.create()
    const c = LANDINGS[0].content[DEFAULT_LOCALE]
    const html = await container.renderToString(LandingSections, {
      props: { content: c, lang: DEFAULT_LOCALE },
    })
    // La fixture è in bozza: niente <img> dell'hero, ma il riquadro placeholder con alt.
    expect(html).not.toContain(`alt="${c.hero.screenshot.alt}"`)
    expect(html).toContain(`aria-label="${c.hero.screenshot.alt}"`)
  })
})
