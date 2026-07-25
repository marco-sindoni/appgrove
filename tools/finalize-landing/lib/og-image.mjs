// ─────────────────────────────────────────────────────────────────────────────
// tools/finalize-landing/lib/og-image.mjs — immagine Open Graph on-brand (UC 0057).
//
// Genera l'anteprima social (1200×630, rapporto 1.91:1) di una landing: un'immagine
// PNG on-brand con il nome dell'app, una frase-beneficio e il colore della categoria
// del design system (UC 0019). La bozza ha `meta.ogImage: null`; `finalize-landing`
// la riempie con l'immagine prodotta qui.
//
// Perché un template SVG rasterizzato e non una cattura di pagina: l'anteprima social
// è un artefatto grafico stabile e on-brand, non uno screenshot dell'app (quello è la
// figura dell'hero, catturata a parte da screenshot.mjs). L'SVG usa i token del design
// system ed è coerente con lo stile illustrazioni del sito (memo di stile del progetto).
// La rasterizzazione usa `sharp` (già dipendenza del sito via Astro).
// ─────────────────────────────────────────────────────────────────────────────
import sharp from 'sharp'
import { OG_WIDTH, OG_HEIGHT, accentHex } from './branding.mjs'

/** Escapa i caratteri speciali XML nel testo che finisce dentro l'SVG. */
export function escapeXml(text) {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}

/**
 * Manda a capo un testo su più righe rispettando un numero massimo di caratteri
 * per riga (stima a caratteri, sufficiente per una frase-beneficio breve) e un tetto
 * di righe. Evita di dipendere dalla misura reale del font in fase di rasterizzazione.
 */
export function wrap(text, maxChars, maxLines) {
  const words = String(text).trim().split(/\s+/)
  const lines = []
  let current = ''
  for (const word of words) {
    const candidate = current ? `${current} ${word}` : word
    if (candidate.length > maxChars && current) {
      lines.push(current)
      current = word
    } else {
      current = candidate
    }
    if (lines.length === maxLines - 1 && current.length > maxChars) break
  }
  if (current) lines.push(current)
  return lines.slice(0, maxLines)
}

/**
 * Costruisce l'SVG dell'immagine Open Graph. Funzione pura (nessun I/O): la si può
 * testare senza rasterizzare. Fondo scuro on-brand con una banda del colore-categoria,
 * il wordmark "appgrove", il nome dell'app e la frase-beneficio.
 */
export function ogSvg({ appName, tagline, accent = 'cat-blue' }) {
  const color = accentHex(accent)
  const titleLines = wrap(appName, 22, 2)
  const taglineLines = wrap(tagline ?? '', 46, 3)

  const titleStartY = 300 - (titleLines.length - 1) * 45
  const titleTspans = titleLines
    .map((line, i) => `<tspan x="90" y="${titleStartY + i * 96}">${escapeXml(line)}</tspan>`)
    .join('')
  const taglineTspans = taglineLines
    .map((line, i) => `<tspan x="90" y="${430 + i * 44}">${escapeXml(line)}</tspan>`)
    .join('')

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${OG_WIDTH}" height="${OG_HEIGHT}" viewBox="0 0 ${OG_WIDTH} ${OG_HEIGHT}">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#0b1020"/>
      <stop offset="1" stop-color="#141a30"/>
    </linearGradient>
  </defs>
  <rect width="${OG_WIDTH}" height="${OG_HEIGHT}" fill="url(#bg)"/>
  <rect x="0" y="0" width="16" height="${OG_HEIGHT}" fill="${color}"/>
  <circle cx="1050" cy="150" r="220" fill="${color}" opacity="0.14"/>
  <text x="90" y="150" font-family="Plus Jakarta Sans, Helvetica, Arial, sans-serif" font-size="34" font-weight="700" fill="#ffffff" letter-spacing="0.5">appgrove</text>
  <text x="90" y="150" font-family="Plus Jakarta Sans, Helvetica, Arial, sans-serif" font-size="34" font-weight="700" fill="${color}" opacity="0"> </text>
  <text font-family="Plus Jakarta Sans, Helvetica, Arial, sans-serif" font-size="82" font-weight="800" fill="#ffffff">${titleTspans}</text>
  <text font-family="Plus Jakarta Sans, Helvetica, Arial, sans-serif" font-size="34" font-weight="500" fill="#c7cfe2">${taglineTspans}</text>
  <text x="90" y="590" font-family="Plus Jakarta Sans, Helvetica, Arial, sans-serif" font-size="26" font-weight="600" fill="${color}">all-EU · GDPR-first</text>
</svg>`
}

/**
 * Rasterizza l'immagine Open Graph in un buffer PNG 1200×630. `density` alto per
 * mantenere il testo nitido nella rasterizzazione dell'SVG.
 */
export async function renderOgImage(opts) {
  const svg = ogSvg(opts)
  return sharp(Buffer.from(svg), { density: 144 })
    .resize(OG_WIDTH, OG_HEIGHT)
    .png()
    .toBuffer()
}
