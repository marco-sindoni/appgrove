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
// figura dell'hero, catturata a parte da screenshot.mjs).
//
// Dalla change 0080 (UC 0086) i colori arrivano DAVVERO dai token: il fondo usa i neutri
// scuri e caldi del brand e il logo è quello del pacchetto, disegnato in un solo posto.
// Prima il commento diceva «usa i token del design system» mentre il fondo era un
// blu-navy freddo inesistente nella palette — il classico drift che nessun test coglie.
// La rasterizzazione usa `sharp` (già dipendenza del sito via Astro).
// ─────────────────────────────────────────────────────────────────────────────
import sharp from 'sharp'
import { logoMarkSvg } from '../../../frontend/packages/design-system/src/brand/logo.mjs'
import { OG_WIDTH, OG_HEIGHT, OG_PALETTE, accentHex } from './branding.mjs'

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
  const { bgFrom, bgTo, text, textMuted, markContrast } = OG_PALETTE
  const titleLines = wrap(appName, 22, 2)
  const taglineLines = wrap(tagline ?? '', 46, 3)

  const titleStartY = 300 - (titleLines.length - 1) * 45
  const titleTspans = titleLines
    .map((line, i) => `<tspan x="90" y="${titleStartY + i * 96}">${escapeXml(line)}</tspan>`)
    .join('')
  const taglineTspans = taglineLines
    .map((line, i) => `<tspan x="90" y="${430 + i * 44}">${escapeXml(line)}</tspan>`)
    .join('')

  // Il mark del logo viene dal brand kit (un solo disegno per tutto il progetto): qui è
  // dipinto col colore-categoria dell'app, così l'anteprima resta riconoscibile e insieme
  // distinta per app. UC 0087 sostituirà l'artwork e questa immagine lo seguirà da sola.
  const mark = logoMarkSvg({ size: 44, accent: color, contrast: markContrast })
    .replace(/^<svg[^>]*>/, '')
    .replace(/<\/svg>$/, '')

  const font = 'Plus Jakarta Sans, Helvetica, Arial, sans-serif'

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${OG_WIDTH}" height="${OG_HEIGHT}" viewBox="0 0 ${OG_WIDTH} ${OG_HEIGHT}">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="${bgFrom}"/>
      <stop offset="1" stop-color="${bgTo}"/>
    </linearGradient>
  </defs>
  <rect width="${OG_WIDTH}" height="${OG_HEIGHT}" fill="url(#bg)"/>
  <rect x="0" y="0" width="16" height="${OG_HEIGHT}" fill="${color}"/>
  <circle cx="1050" cy="150" r="220" fill="${color}" opacity="0.14"/>
  <g transform="translate(90 108) scale(1.375)">${mark}</g>
  <text x="148" y="140" font-family="${font}" font-size="34" font-weight="800" fill="${text}" letter-spacing="-0.5">appgrove</text>
  <text font-family="${font}" font-size="82" font-weight="800" fill="${text}">${titleTspans}</text>
  <text font-family="${font}" font-size="34" font-weight="500" fill="${textMuted}">${taglineTspans}</text>
  <text x="90" y="590" font-family="${font}" font-size="26" font-weight="600" fill="${color}">all-EU · GDPR-first</text>
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
