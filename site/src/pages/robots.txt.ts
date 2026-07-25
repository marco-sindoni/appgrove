// Endpoint robots.txt (UC 0040 gate indicizzazione + UC 0041 consenso crawler AI).
// Guscio sottile: la logica sta in src/lib/robots.ts (pura e testabile). Rispetta il
// gate di indicizzazione (#14 54): finché SITE_INDEXABLE !== 'true' il sito è pre-go-live
// → Disallow totale (coerente col meta noindex). Al go-live → Allow + consenso ai crawler
// AI + riga Sitemap + rimando a /llms.txt.
import type { APIRoute } from 'astro'
import { buildRobotsTxt } from '../lib/robots.ts'

export const GET: APIRoute = ({ site }) => {
  const base = site ?? new URL('https://appgrove.app')
  const body = buildRobotsTxt({
    indexable: process.env.SITE_INDEXABLE === 'true',
    sitemapUrl: new URL('/sitemap.xml', base).toString(),
    llmsTxtUrl: new URL('/llms.txt', base).toString(),
  })
  return new Response(body, {
    headers: { 'Content-Type': 'text/plain; charset=utf-8' },
  })
}
