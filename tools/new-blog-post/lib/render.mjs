// ─────────────────────────────────────────────────────────────────────────────
// tools/new-blog-post/lib/render.mjs — resa dei file TypeScript di un post (UC 0084).
//
// I contenuti del blog sono TypeScript tipizzato, non markdown libero (UC 0042): è il
// tipo `Record<Locale, PostLocaleContent>` a garantire la parità delle 5 lingue a tempo di
// compilazione. Quindi lo scaffolding deve produrre codice, e produrlo nello stile dei
// file scritti a mano — stesse virgolette, stessa indentazione, stessa densità di commenti
// — altrimenti ogni post generato si riconosce a occhio e il registro diventa disomogeneo.
//
// Il modello di riferimento è il pilastro `fatturazione-pmi-ue` scritto a mano nella change
// che ha costruito il motore del blog.
// ─────────────────────────────────────────────────────────────────────────────
import { LOCALES } from './spec.mjs'

/** Nome dell'export di un post a partire dalla sua chiave: `fattura-a-norma` → `fatturaANorma`. */
export function exportNameFor(key) {
  return key.replace(/-([a-z0-9])/g, (_, c) => c.toUpperCase())
}

/**
 * Rende una stringa come letterale TypeScript nello stile del repository (virgolette
 * singole). Se il testo contiene un apostrofo dritto si passa alle virgolette doppie
 * invece di riempire la riga di sequenze di escape: si legge meglio, ed è quello che
 * farebbe una persona. L'escape resta come ultima risorsa.
 */
export function tsString(value) {
  const s = String(value).replace(/\\/g, '\\\\').replace(/\n/g, '\\n')
  if (!s.includes("'")) return `'${s}'`
  if (!s.includes('"')) return `"${s}"`
  return `'${s.replace(/'/g, "\\'")}'`
}

const NL = '\n'

/** Nome della lingua in italiano, per i commenti di intestazione. */
const LOCALE_LABEL = {
  en: 'inglese (lingua sorgente)',
  it: 'italiano',
  fr: 'francese',
  es: 'spagnolo',
  de: 'tedesco',
}

/**
 * Rende il file di UNA lingua di un post. La descrizione va a capo (è quasi sempre lunga:
 * è la meta description), tutto il resto segue la forma dei file scritti a mano.
 */
export function renderLocaleFile(post, locale) {
  const c = post.content[locale]
  const kind = post.kind === 'pillar' ? 'Pilastro' : 'Articolo'
  const out = []
  out.push(`// ${kind} "${post.content.en.title}" — ${LOCALE_LABEL[locale]} (UC 0042).`)
  out.push(`// Generato dalla skill \`new-blog-post\` (UC 0084); la copy è del co-pilota, non del generatore.`)
  out.push(`import type { PostLocaleContent } from '../types.ts'`)
  out.push('')
  out.push(`export const ${locale}: PostLocaleContent = {`)
  out.push(`  slug: ${tsString(c.slug)},`)
  out.push(`  title: ${tsString(c.title)},`)
  out.push(`  description:`)
  out.push(`    ${tsString(c.description)},`)
  out.push(`  question: ${tsString(c.question)},`)
  out.push(`  intro: [`)
  for (const p of c.intro) out.push(`    ${tsString(p)},`)
  out.push(`  ],`)
  out.push(`  sections: [`)
  for (const s of c.sections) {
    out.push(`    {`)
    out.push(`      heading: ${tsString(s.heading)},`)
    out.push(`      paragraphs: [`)
    for (const p of s.paragraphs) out.push(`        ${tsString(p)},`)
    out.push(`      ],`)
    out.push(`    },`)
  }
  out.push(`  ],`)
  out.push(`  faq: {`)
  out.push(`    title: ${tsString(c.faq.title)},`)
  out.push(`    items: [`)
  for (const item of c.faq.items) {
    out.push(`      {`)
    out.push(`        q: ${tsString(item.q)},`)
    out.push(`        a: ${tsString(item.a)},`)
    out.push(`      },`)
  }
  out.push(`    ],`)
  out.push(`  },`)
  out.push(`  ctaText: ${tsString(c.ctaText)},`)
  out.push(`}`)
  return out.join(NL) + NL
}

/**
 * Rende il file di identità del post (`index.ts` della cartella): chiave, tipo, data, app
 * collegata, eventuale pilastro di appartenenza, e il contenuto delle 5 lingue.
 *
 * Un pilastro nasce SENZA il campo `clusterKeys`: l'aggancio dei riferimenti reciproci è
 * un'operazione a parte (lib/registry.mjs), ed è anche ciò che rende la generazione e la
 * rimozione esattamente simmetriche.
 */
export function renderPostIndex(post) {
  const name = exportNameFor(post.key)
  const out = []
  if (post.kind === 'pillar') {
    out.push(`// Pilastro "${post.content.en.title}" (UC 0042): testa di serie del suo tema. Gli articoli`)
    out.push(`// cluster che vi rimandano compaiono in \`clusterKeys\`, agganciati alla generazione.`)
  } else {
    out.push(`// Articolo cluster "${post.content.en.title}" (UC 0042): rimanda al pilastro`)
    out.push(`// "${post.pillarKey}" (\`pillarKey\`), che a sua volta lo elenca fra i suoi cluster.`)
  }
  out.push(`// Rimanda alla landing dell'app \`${post.appId}\` (collegamento risolto per lingua). Il tipo`)
  out.push(`// Record<Locale, …> garantisce la parità delle 5 lingue a tempo di compilazione.`)
  out.push(`// Generato dalla skill \`new-blog-post\` (UC 0084).`)
  out.push(`import type { BlogPost } from '../types.ts'`)
  for (const loc of LOCALES) out.push(`import { ${loc} } from './${loc}.ts'`)
  out.push('')
  out.push(`export const ${name}: BlogPost = {`)
  out.push(`  key: ${tsString(post.key)},`)
  out.push(`  kind: ${tsString(post.kind)},`)
  out.push(`  datePublished: ${tsString(post.datePublished)},`)
  out.push(`  appId: ${tsString(post.appId)},`)
  if (post.kind === 'article') out.push(`  pillarKey: ${tsString(post.pillarKey)},`)
  out.push(`  content: { ${LOCALES.join(', ')} },`)
  out.push(`}`)
  return out.join(NL) + NL
}

/** Campi che il generatore scrive davvero: base dell'allarme di deriva del contratto. */
export const RENDERED_POST_FIELDS = ['key', 'kind', 'datePublished', 'appId', 'pillarKey', 'clusterKeys', 'content']
export const RENDERED_LOCALE_FIELDS = [
  'slug',
  'title',
  'description',
  'question',
  'intro',
  'sections',
  'faq',
  'ctaText',
]
