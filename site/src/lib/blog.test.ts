import { describe, it, expect } from 'vitest'
import {
  blogPosts,
  pillars,
  clusterArticles,
  pillarOf,
  postsByDateDesc,
  blogPath,
  blogHref,
  blogHreflangPaths,
  landingHref,
  blogParams,
  validateBlog,
} from './blog.ts'
import { LOCALES } from './i18n.ts'
import { publishedLandings } from './landings.ts'
import type { BlogPost } from '../content/blog/types.ts'

// Logica del registro blog (UC 0042): coerenza pilastro↔cluster, slug/hreflang localizzati,
// risoluzione del link interno alle landing e validazione. È il presidio su cui poggerà la
// futura skill `new-blog-post`.

describe('registro blog (UC 0042)', () => {
  it('il registro reale è valido (parità, slug, coerenza pilastro↔cluster)', () => {
    expect(validateBlog()).toEqual([])
  })

  it('ha almeno un pilastro e ogni articolo appartiene a un pilastro', () => {
    const ps = pillars()
    expect(ps.length).toBeGreaterThan(0)
    for (const post of blogPosts()) {
      if (post.kind === 'article') expect(pillarOf(post)).toBeDefined()
    }
  })

  it('i riferimenti pilastro↔cluster sono reciproci', () => {
    for (const pillar of pillars()) {
      for (const article of clusterArticles(pillar)) {
        expect(article.kind).toBe('article')
        expect(article.pillarKey).toBe(pillar.key)
      }
    }
  })

  it('blogParams: una entry per ogni post × lingua, con lo slug localizzato', () => {
    const params = blogParams()
    expect(params.length).toBe(blogPosts().length * LOCALES.length)
    for (const post of blogPosts()) {
      for (const lang of LOCALES) {
        const entry = params.find((p) => p.props.post.key === post.key && p.params.lang === lang)
        expect(entry, `${post.key}/${lang}`).toBeDefined()
        expect(entry!.params.slug).toBe(post.content[lang].slug)
      }
    }
  })

  it('blogPath/blogHref compongono il percorso localizzato', () => {
    const post = blogPosts()[0]
    expect(blogPath(post, 'it')).toBe(`/blog/${post.content.it.slug}/`)
    expect(blogHref(post, 'en')).toBe(`/en/blog/${post.content.en.slug}/`)
  })

  it('blogHreflangPaths punta al percorso reale di ogni lingua', () => {
    const post = blogPosts()[0]
    const paths = blogHreflangPaths(post)
    for (const lang of LOCALES) {
      expect(paths[lang]).toBe(`/blog/${post.content[lang].slug}/`)
    }
  })

  it('landingHref risolve dallo slug per-lingua della landing pubblicata', () => {
    const fatture = publishedLandings().find((l) => l.appId === 'fatture')
    expect(fatture, 'la landing fatture deve essere pubblicata per questo test').toBeDefined()
    for (const lang of LOCALES) {
      expect(landingHref('fatture', lang)).toBe(`/${lang}/${fatture!.content[lang].slug}/`)
    }
  })

  it('landingHref ripiega su /<lang>/#apps per un appId senza landing pubblicata', () => {
    expect(landingHref('app-inesistente', 'it')).toBe('/it/#apps')
  })

  it('postsByDateDesc ordina per data decrescente', () => {
    const ordered = postsByDateDesc()
    for (let i = 1; i < ordered.length; i++) {
      expect(ordered[i - 1].datePublished >= ordered[i].datePublished).toBe(true)
    }
  })
})

describe('validateBlog rileva i registri incoerenti', () => {
  const base = blogPosts()[0]

  it('segnala uno slug malformato', () => {
    const bad: BlogPost = {
      ...base,
      key: 'bad',
      kind: 'pillar',
      clusterKeys: [],
      pillarKey: undefined,
      content: { ...base.content, it: { ...base.content.it, slug: 'Non Valido' } },
    }
    expect(validateBlog([bad]).some((e) => e.includes('slug non valido'))).toBe(true)
  })

  it('segnala un articolo il cui pilastro non lo elenca', () => {
    const pillar: BlogPost = { ...base, key: 'p', kind: 'pillar', pillarKey: undefined, clusterKeys: [] }
    const article: BlogPost = { ...base, key: 'a', kind: 'article', clusterKeys: undefined, pillarKey: 'p' }
    const errs = validateBlog([pillar, article])
    expect(errs.some((e) => e.includes('non lo elenca fra i cluster'))).toBe(true)
  })

  it('segnala un appId senza landing pubblicata', () => {
    const orphan: BlogPost = {
      ...base,
      key: 'orphan',
      kind: 'pillar',
      pillarKey: undefined,
      clusterKeys: [],
      appId: 'non-esiste',
    }
    expect(validateBlog([orphan]).some((e) => e.includes('non corrisponde a nessuna landing'))).toBe(true)
  })
})
