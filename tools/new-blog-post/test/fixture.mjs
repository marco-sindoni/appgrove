// Copia di prova del sito vetrina per i test del generatore (UC 0084).
//
// Riproduce a mano la forma REALE dei file di UC 0042 — registro, pilastro scritto a mano,
// landing pubblicata e landing in bozza — perché il valore dei test sta proprio nel provare
// che le modifiche meccaniche funzionano su codice scritto da persone, non solo su codice
// che lo strumento ha appena generato da sé.
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { LOCALES } from '../lib/spec.mjs'

const REGISTRY = `// Registro dei contenuti del blog/risorse (UC 0042).
import type { BlogPost } from './types.ts'
import { temaGuida } from './tema-guida/index.ts'

export const BLOG_POSTS: readonly BlogPost[] = [
  temaGuida,
]

export type { BlogPost } from './types.ts'
`

const PILLAR_INDEX = `// Pilastro di prova (UC 0042).
import type { BlogPost } from '../types.ts'
import { en } from './en.ts'
import { it } from './it.ts'
import { fr } from './fr.ts'
import { es } from './es.ts'
import { de } from './de.ts'

export const temaGuida: BlogPost = {
  key: 'tema-guida',
  kind: 'pillar',
  datePublished: '2026-01-01',
  appId: 'fatture',
  content: { en, it, fr, es, de },
}
`

function pillarLocale(locale, slug) {
  return `// Pilastro di prova — ${locale}.
import type { PostLocaleContent } from '../types.ts'

export const ${locale}: PostLocaleContent = {
  slug: '${slug}',
  title: 'Tema guida',
  description:
    'Descrizione del pilastro di prova.',
  question: 'Come funziona il tema guida?',
  intro: ['Introduzione.'],
  sections: [
    {
      heading: 'Sezione',
      paragraphs: ['Paragrafo.'],
    },
  ],
  faq: {
    title: 'Domande',
    items: [
      {
        q: 'Domanda?',
        a: 'Risposta.',
      },
    ],
  },
  ctaText: 'Vai alla app',
}
`
}

const LANDING_PUBLISHED = `import type { Landing } from '../types.ts'
import { en } from './en.ts'

export const fattureLanding: Landing = {
  appId: 'fatture',
  status: 'published',
  content: { en },
}
`

const LANDING_DRAFT = `import type { Landing } from '../types.ts'
import { en } from './en.ts'

export const bozzaLanding: Landing = {
  appId: 'bozza',
  status: 'draft',
  content: { en },
}
`

/** Crea una copia di prova del sito e ne ritorna la radice. */
export function makeRepo() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'new-blog-post-'))
  const blog = path.join(root, 'site/src/content/blog')
  fs.mkdirSync(path.join(blog, 'tema-guida'), { recursive: true })
  fs.writeFileSync(path.join(blog, 'index.ts'), REGISTRY)
  fs.writeFileSync(path.join(blog, 'tema-guida/index.ts'), PILLAR_INDEX)
  const slugs = { en: 'guide-theme', it: 'tema-guida', fr: 'theme-guide', es: 'tema-guia', de: 'leitthema' }
  for (const loc of LOCALES) {
    fs.writeFileSync(path.join(blog, `tema-guida/${loc}.ts`), pillarLocale(loc, slugs[loc]))
  }
  const landings = path.join(root, 'site/src/content/landings')
  fs.mkdirSync(path.join(landings, 'fatture'), { recursive: true })
  fs.mkdirSync(path.join(landings, 'bozza'), { recursive: true })
  fs.writeFileSync(path.join(landings, 'fatture/index.ts'), LANDING_PUBLISHED)
  fs.writeFileSync(path.join(landings, 'bozza/index.ts'), LANDING_DRAFT)
  return root
}

/** Contenuto di una lingua, valido, con lo slug richiesto. */
export function localeContent(slug, n = 1) {
  return {
    slug,
    title: `Titolo ${slug}`,
    description: "Descrizione dell'articolo di prova, abbastanza lunga da andare a capo.",
    question: `Come si fa ${slug}?`,
    intro: Array.from({ length: n }, (_, i) => `Paragrafo di introduzione ${i + 1}.`),
    sections: [
      { heading: 'Prima sezione', paragraphs: ['Un paragrafo.', 'Un altro paragrafo.'] },
      { heading: 'Seconda sezione', paragraphs: ['Ancora un paragrafo.'] },
    ],
    faq: {
      title: 'Domande frequenti',
      items: [
        { q: 'Prima domanda?', a: 'Prima risposta.' },
        { q: 'Seconda domanda?', a: 'Seconda risposta.' },
      ],
    },
    ctaText: 'Prova appgrove Fatture',
  }
}

/** Un articolo cluster valido, agganciato al pilastro di prova. */
export function articleSpec(overrides = {}) {
  const key = overrides.key ?? 'articolo-prova'
  return {
    key,
    kind: 'article',
    datePublished: '2026-08-02',
    appId: 'fatture',
    pillarKey: 'tema-guida',
    content: Object.fromEntries(LOCALES.map((loc) => [loc, localeContent(`${key}-${loc}`)])),
    ...overrides,
  }
}

/** Un pilastro valido (per il caso "il pilastro non esiste ancora"). */
export function pillarSpec(overrides = {}) {
  const key = overrides.key ?? 'nuovo-pilastro'
  return {
    key,
    kind: 'pillar',
    datePublished: '2026-08-02',
    appId: 'fatture',
    content: Object.fromEntries(LOCALES.map((loc) => [loc, localeContent(`${key}-${loc}`)])),
    ...overrides,
  }
}

/** Istantanea (percorso → contenuto) di tutti i file sotto una cartella. */
export function snapshot(dir) {
  const out = new Map()
  const walk = (d) => {
    for (const entry of fs.readdirSync(d, { withFileTypes: true })) {
      const p = path.join(d, entry.name)
      if (entry.isDirectory()) walk(p)
      else out.set(p, fs.readFileSync(p, 'utf8'))
    }
  }
  walk(dir)
  return out
}
