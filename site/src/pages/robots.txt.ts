// Endpoint robots.txt (UC 0040). Rispetta il gate di indicizzazione (#14 54):
// finché SITE_INDEXABLE !== 'true' il sito è pre-go-live → Disallow totale (coerente
// col meta noindex). Al go-live (SITE_INDEXABLE=true) → Allow + riga Sitemap.
// Il consenso ai crawler AI (llms.txt / GEO) è di UC 0041: qui NON si decide.
import type { APIRoute } from 'astro'

export const GET: APIRoute = ({ site }) => {
  const indexable = process.env.SITE_INDEXABLE === 'true'
  const base = site ?? new URL('https://appgrove.app')
  const body = indexable
    ? `User-agent: *\nAllow: /\n\nSitemap: ${new URL('/sitemap.xml', base).toString()}\n`
    : `User-agent: *\nDisallow: /\n`
  return new Response(body, {
    headers: { 'Content-Type': 'text/plain; charset=utf-8' },
  })
}
