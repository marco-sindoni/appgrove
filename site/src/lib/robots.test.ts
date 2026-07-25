import { describe, it, expect } from 'vitest'
import { buildRobotsTxt, AI_CRAWLERS } from './robots.ts'

const SITEMAP = 'https://appgrove.app/sitemap.xml'
const LLMS = 'https://appgrove.app/llms.txt'

describe('robots.txt (UC 0040 gate + UC 0041 crawler AI)', () => {
  it('pre-go-live: Disallow totale, nessun consenso ai crawler AI', () => {
    const body = buildRobotsTxt({ indexable: false, sitemapUrl: SITEMAP, llmsTxtUrl: LLMS })
    expect(body).toContain('User-agent: *')
    expect(body).toContain('Disallow: /')
    expect(body).not.toContain('Allow: /')
    expect(body).not.toContain('Sitemap:')
    expect(body).not.toContain('GPTBot')
  })

  it('go-live: Allow generale + Sitemap + rimando a llms.txt', () => {
    const body = buildRobotsTxt({ indexable: true, sitemapUrl: SITEMAP, llmsTxtUrl: LLMS })
    expect(body).toContain('User-agent: *')
    expect(body).toContain('Allow: /')
    expect(body).not.toContain('Disallow: /')
    expect(body).toContain(`Sitemap: ${SITEMAP}`)
    expect(body).toContain(LLMS)
  })

  it('go-live: un blocco di consenso esplicito per OGNI crawler AI curato', () => {
    const body = buildRobotsTxt({ indexable: true, sitemapUrl: SITEMAP, llmsTxtUrl: LLMS })
    for (const ua of AI_CRAWLERS) {
      expect(body, `manca il consenso a ${ua}`).toContain(`User-agent: ${ua}`)
    }
    // Il numero di "Allow: /" = wildcard (1) + un blocco per crawler AI.
    const allows = (body.match(/^Allow: \/$/gm) || []).length
    expect(allows).toBe(AI_CRAWLERS.length + 1)
  })

  it('include i principali motori AI citati nello use case', () => {
    for (const ua of ['GPTBot', 'ClaudeBot', 'PerplexityBot', 'Google-Extended']) {
      expect(AI_CRAWLERS).toContain(ua)
    }
    // Elenco volutamente ampio.
    expect(AI_CRAWLERS.length).toBeGreaterThanOrEqual(20)
  })
})
