// Endpoint per-lingua /<lang>/llms.txt (UC 0041, GEO — llms.txt multilingua). Una variante
// localizzata per ciascuna lingua, col testo attinto dai contenuti marketing tradotti.
// Guscio sottile: la logica sta in src/lib/llms.ts. La variante radice /llms.txt (inglese
// + link alle lingue) è servita da src/pages/llms.txt.ts.
import type { APIRoute } from 'astro'
import { LOCALES, isLocale, type Locale } from '../../lib/i18n.ts'
import { buildLlmsTxt } from '../../lib/llms.ts'

export function getStaticPaths() {
  return LOCALES.map((lang) => ({ params: { lang } }))
}

export const GET: APIRoute = ({ params, site }) => {
  const lang = params.lang
  if (!lang || !isLocale(lang)) return new Response('Not found', { status: 404 })
  const base = (site ?? new URL('https://appgrove.app')).toString()
  const body = buildLlmsTxt({ locale: lang as Locale, siteUrl: base })
  return new Response(body, {
    headers: { 'Content-Type': 'text/plain; charset=utf-8' },
  })
}
