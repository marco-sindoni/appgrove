// Endpoint radice /llms.txt (UC 0041, GEO). Variante nella lingua di default (inglese,
// lingua franca degli assistenti AI), con in coda i link alle varianti per-lingua. Guscio
// sottile: la logica sta in src/lib/llms.ts (pura e testabile). Le varianti localizzate
// sono servite da src/pages/[lang]/llms.txt.ts.
import type { APIRoute } from 'astro'
import { DEFAULT_LOCALE } from '../lib/i18n.ts'
import { buildLlmsTxt } from '../lib/llms.ts'

export const GET: APIRoute = ({ site }) => {
  const base = (site ?? new URL('https://appgrove.app')).toString()
  const body = buildLlmsTxt({ locale: DEFAULT_LOCALE, siteUrl: base, includeLanguages: true })
  return new Response(body, {
    headers: { 'Content-Type': 'text/plain; charset=utf-8' },
  })
}
