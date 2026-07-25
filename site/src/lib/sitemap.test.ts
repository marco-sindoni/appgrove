import { describe, it, expect } from 'vitest'
import { collectSitemapGroups, renderSitemap } from './sitemap.ts'
import { LOCALES } from './i18n.ts'

const BASE = new URL('https://appgrove.app')

describe('sitemap multilingua (UC 0040)', () => {
  it('raccoglie home + brand + legali (nessuna landing: la fixture è draft)', () => {
    const groups = collectSitemapGroups()
    // La home ha tutte e 5 le lingue.
    const home = groups.find((g) => g.every((v) => v.path === '/'))
    expect(home?.length).toBe(LOCALES.length)
    // Esiste il gruppo brand "why" con slug localizzati (perche, warum, …).
    const paths = groups.flatMap((g) => g.map((v) => v.path))
    expect(paths).toContain('/why/')
    expect(paths).toContain('/perche/')
    expect(paths).toContain('/prezzi/')
    // Almeno un gruppo legale.
    expect(paths.some((p) => p.startsWith('/legal/'))).toBe(true)
  })

  it('renderSitemap produce XML valido con alternates hreflang localizzati', () => {
    const groups = collectSitemapGroups()
    const xml = renderSitemap(BASE, groups)
    expect(xml).toContain('<urlset')
    expect(xml).toContain('</urlset>')
    // Una <url> per ogni variante di lingua di ogni gruppo.
    const totalVariants = groups.reduce((n, g) => n + g.length, 0)
    expect((xml.match(/<url>/g) || []).length).toBe(totalVariants)
    // x-default presente.
    expect(xml).toContain('hreflang="x-default"')
    // La pagina brand italiana punta al suo slug e lega la variante inglese reale.
    expect(xml).toContain('<loc>https://appgrove.app/it/perche/</loc>')
    expect(xml).toContain('hreflang="en" href="https://appgrove.app/en/why/"')
  })
})
