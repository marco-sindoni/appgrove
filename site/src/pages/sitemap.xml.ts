// Endpoint sitemap.xml (UC 0040): involucro sottile sui costruttori puri di
// lib/sitemap.ts. Generata al build (SSG). Elenca home, pagine brand (slug
// localizzati), legali pubblicati e landing pubblicate, ciascuna con gli hreflang
// alternates verso i percorsi reali di ogni lingua.
import type { APIRoute } from 'astro'
import { collectSitemapGroups, renderSitemap } from '../lib/sitemap.ts'

export const GET: APIRoute = ({ site }) => {
  const base = site ?? new URL('https://appgrove.app')
  const xml = renderSitemap(base, collectSitemapGroups())
  return new Response(xml, {
    headers: { 'Content-Type': 'application/xml; charset=utf-8' },
  })
}
