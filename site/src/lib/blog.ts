// Logica del registro del blog/risorse (UC 0042): relazioni pilastro↔cluster, parametri
// statici delle rotte, mappe hreflang dagli slug localizzati, risoluzione del link interno
// alle landing e validazione. Come per le landing (lib/landings.ts), le funzioni sono pure
// e testabili; le rotte in pages/[lang]/blog/* sono gusci sottili su queste.

import { BLOG_POSTS } from '../content/blog/index.ts'
import type { BlogPost } from '../content/blog/types.ts'
import { LOCALES, normalizePath, type Locale } from './i18n.ts'
import { publishedLandings } from './landings.ts'

/** Slug riservati DENTRO la sezione blog: `index` è la rotta dell'indice (/<lang>/blog/). */
const RESERVED_BLOG_SLUGS: readonly string[] = ['index']

/** Tutti i post del blog (pilastri + articoli). */
export function blogPosts(landings = BLOG_POSTS): readonly BlogPost[] {
  return landings
}

/** Solo i pilastri, in ordine di registro. */
export function pillars(posts: readonly BlogPost[] = BLOG_POSTS): BlogPost[] {
  return posts.filter((p) => p.kind === 'pillar')
}

/** Articoli cluster di un pilastro, nell'ordine dichiarato da `clusterKeys`. */
export function clusterArticles(pillar: BlogPost, posts: readonly BlogPost[] = BLOG_POSTS): BlogPost[] {
  const byKey = new Map(posts.map((p) => [p.key, p]))
  return (pillar.clusterKeys ?? []).map((k) => byKey.get(k)).filter((p): p is BlogPost => Boolean(p))
}

/** Pilastro a cui appartiene un articolo (o `undefined` se non ne ha uno valido). */
export function pillarOf(article: BlogPost, posts: readonly BlogPost[] = BLOG_POSTS): BlogPost | undefined {
  if (!article.pillarKey) return undefined
  return posts.find((p) => p.key === article.pillarKey && p.kind === 'pillar')
}

/** Post ordinati per data di pubblicazione decrescente (ISO → confronto lessicografico). */
export function postsByDateDesc(posts: readonly BlogPost[] = BLOG_POSTS): BlogPost[] {
  return [...posts].sort((a, b) => (a.datePublished < b.datePublished ? 1 : a.datePublished > b.datePublished ? -1 : 0))
}

/** Percorso DENTRO la lingua di un post, es. `/blog/fattura-elettronica-a-norma/`. */
export function blogPath(post: BlogPost, lang: Locale): string {
  return normalizePath(`/blog/${post.content[lang].slug}/`)
}

/** Link interno (con segmento di lingua) a un post, es. `/it/blog/…/`. */
export function blogHref(post: BlogPost, lang: Locale): string {
  return `/${lang}${blogPath(post, lang)}`
}

/** Link interno all'indice blog di una lingua, es. `/it/blog/`. */
export function blogIndexHref(lang: Locale): string {
  return `/${lang}/blog/`
}

/** Mappa `Record<Locale, path>` degli hreflang di un post (slug per-lingua). */
export function blogHreflangPaths(post: BlogPost): Record<Locale, string> {
  return Object.fromEntries(LOCALES.map((loc) => [loc, blogPath(post, loc)])) as Record<Locale, string>
}

/** Mappa hreflang dell'indice blog: stesso segmento `/blog/` in tutte le lingue. */
export function blogIndexHreflangPaths(): Record<Locale, string> {
  return Object.fromEntries(LOCALES.map((loc) => [loc, '/blog/'])) as Record<Locale, string>
}

/**
 * Link interno alla landing collegata a un post, risolto PER LINGUA dal registro
 * `LANDINGS` (mai cablato): così resta corretto in tutte le lingue e non si rompe se lo
 * slug della landing cambia. Se la landing non è pubblicata, ripiega sull'ancora della
 * vetrina app della home (`/<lang>/#apps`), che esiste sempre — nessun link rotto.
 */
export function landingHref(appId: string, lang: Locale): string {
  const landing = publishedLandings().find((l) => l.appId === appId)
  return landing ? `/${lang}/${landing.content[lang].slug}/` : `/${lang}/#apps`
}

/** Parametri statici per la rotta [lang]/blog/[slug].astro: una entry per post × lingua. */
export function blogParams(posts: readonly BlogPost[] = BLOG_POSTS): Array<{
  params: { lang: Locale; slug: string }
  props: { post: BlogPost; lang: Locale }
}> {
  const out = []
  for (const post of posts) {
    for (const lang of LOCALES) {
      out.push({ params: { lang, slug: post.content[lang].slug }, props: { post, lang } })
    }
  }
  return out
}

/**
 * Valida il registro del blog: presenza delle lingue, slug ben formati/non riservati/non
 * duplicati per lingua, e coerenza pilastro↔cluster (riferimenti reciproci esistenti e del
 * giusto tipo) più appId collegato a una landing pubblicata. Ritorna la lista degli errori
 * (vuota se tutto a posto). È il presidio su cui si appoggerà la skill `new-blog-post`.
 */
export function validateBlog(posts: readonly BlogPost[] = BLOG_POSTS): string[] {
  const errors: string[] = []
  const byKey = new Map(posts.map((p) => [p.key, p]))
  const seen = new Map<string, string>() // `${lang}:${slug}` → key

  for (const p of posts) {
    // Slug ben formati, non riservati, non duplicati per lingua.
    for (const lang of LOCALES) {
      const c = p.content[lang]
      if (!c) {
        errors.push(`${p.key}: manca la lingua "${lang}"`)
        continue
      }
      const slug = c.slug
      if (!slug || !/^[a-z0-9-]+$/.test(slug)) {
        errors.push(`${p.key}/${lang}: slug non valido "${slug}" (atteso minuscolo [a-z0-9-])`)
        continue
      }
      if (RESERVED_BLOG_SLUGS.includes(slug)) {
        errors.push(`${p.key}/${lang}: slug riservato "${slug}" (collide con l'indice blog)`)
      }
      const mapKey = `${lang}:${slug}`
      const prev = seen.get(mapKey)
      if (prev && prev !== p.key) {
        errors.push(`slug blog duplicato "${slug}" in lingua "${lang}" fra ${prev} e ${p.key}`)
      }
      seen.set(mapKey, p.key)
    }

    // appId collegato a una landing pubblicata (l'internal linking deve risolvere davvero).
    if (!publishedLandings().some((l) => l.appId === p.appId)) {
      errors.push(`${p.key}: appId "${p.appId}" non corrisponde a nessuna landing pubblicata`)
    }

    // Coerenza pilastro↔cluster.
    if (p.kind === 'pillar') {
      for (const ck of p.clusterKeys ?? []) {
        const child = byKey.get(ck)
        if (!child) errors.push(`pilastro ${p.key}: articolo cluster inesistente "${ck}"`)
        else if (child.kind !== 'article') errors.push(`pilastro ${p.key}: "${ck}" non è un articolo`)
        else if (child.pillarKey !== p.key) {
          errors.push(`pilastro ${p.key}: l'articolo "${ck}" non rimanda indietro (pillarKey="${child.pillarKey}")`)
        }
      }
    } else {
      if (!p.pillarKey) {
        errors.push(`articolo ${p.key}: pillarKey mancante`)
      } else {
        const parent = byKey.get(p.pillarKey)
        if (!parent) errors.push(`articolo ${p.key}: pilastro inesistente "${p.pillarKey}"`)
        else if (parent.kind !== 'pillar') errors.push(`articolo ${p.key}: "${p.pillarKey}" non è un pilastro`)
        else if (!(parent.clusterKeys ?? []).includes(p.key)) {
          errors.push(`articolo ${p.key}: il pilastro "${p.pillarKey}" non lo elenca fra i cluster`)
        }
      }
    }
  }
  return errors
}
