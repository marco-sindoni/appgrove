// ─────────────────────────────────────────────────────────────────────────────
// @appgrove/design-system/logo.js — il disegno del logo, in un solo posto (UC 0086).
//
// Perché esiste. Il logo viveva solo dentro il componente React `Logo.tsx`: chiunque
// non compili React — il generatore dell'immagine social, un favicon, un'email — non
// poteva usarlo, e l'unica via era ridisegnarlo. Qui il disegno è definito UNA volta,
// in JavaScript puro (nessun React, nessun accesso al filesystem: importabile sia dal
// browser sia da Node), e sia il componente sia i consumatori non-React lo consumano.
//
// I colori NON sono cablati: si passano come parametri. Chi compila CSS passa i
// riferimenti alle variabili (`rgb(var(--ag-accent))`), chi non lo fa passa gli
// esadecimali letti dai token (`hex('accent')` di tokens.js). Un solo disegno, e
// nessuna copia dei valori colore.
//
// UC 0087 (artwork logo finale) sostituirà l'artwork intervenendo SOLO su questo file:
// è la ragione per cui il disegno è stato isolato qui prima che quel lavoro cominci.
// ─────────────────────────────────────────────────────────────────────────────

/** Riquadro di disegno del mark (quadrato). Le proporzioni dei tracciati vi si riferiscono. */
export const LOGO_VIEWBOX = '0 0 32 32'

/** Raggio degli angoli della piastrella del mark, nelle unità del riquadro di disegno. */
export const LOGO_TILE_RADIUS = 9

/**
 * I tracciati del mark, in ordine di disegno. Ogni tracciato dichiara il RUOLO del suo
 * colore (non il colore), così la stessa figura si adatta a tema chiaro e scuro:
 *  - `accent`   → il colore d'accento vivo (la piastrella, i dettagli);
 *  - `contrast` → il colore che ci sta sopra leggibile (la foglia).
 *
 * Placeholder on-brand: l'artwork definitivo è UC 0087, che sostituisce questi tracciati.
 */
export const LOGO_PATHS = [
  {
    role: 'accent',
    fill: true,
    d: 'M22 9c0 6.5-3.8 11-9.5 11-1 0-1.9-.15-2.5-.4C10.4 13.2 14.8 9.6 22 9Z',
    on: 'contrast',
  },
  {
    role: 'accent',
    fill: false,
    d: 'M10 22c1.2-3.4 3.4-5.8 6.6-7.2',
    strokeWidth: 1.4,
  },
]

/**
 * Genera l'SVG completo del mark come stringa, per i consumatori che non compilano
 * React (immagine social, favicon, anteprime).
 *
 * @param {{ size?: number, accent?: string, contrast?: string, title?: string }} [opts]
 *   `accent` e `contrast` sono i due colori, nella forma che serve al consumatore
 *   (esadecimale per Node, `rgb(var(--ag-accent))` per il browser).
 * @returns {string} SVG autoconsistente
 */
export function logoMarkSvg(opts = {}) {
  const { size = 32, accent = '#ec5a72', contrast = '#ffffff', title = 'appgrove' } = opts
  const paths = LOGO_PATHS.map((p) => {
    const color = p.on === 'contrast' ? contrast : accent
    return p.fill
      ? `<path d="${p.d}" fill="${color}"/>`
      : `<path d="${p.d}" stroke="${color}" stroke-width="${p.strokeWidth}" stroke-linecap="round" fill="none"/>`
  }).join('')

  return (
    `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" ` +
    `viewBox="${LOGO_VIEWBOX}" role="img" aria-label="${title}">` +
    `<rect width="32" height="32" rx="${LOGO_TILE_RADIUS}" fill="${accent}"/>` +
    `${paths}</svg>`
  )
}

/**
 * Il logo completo (mark + wordmark) come SVG, per le superfici larghe: immagine
 * social, intestazioni statiche. Il wordmark usa il carattere del brand con i suoi
 * ripieghi, così resta leggibile anche dove Plus Jakarta Sans non è disponibile.
 *
 * @param {{ height?: number, accent?: string, contrast?: string, text?: string }} [opts]
 *   `text` è il colore del wordmark (di norma il token del testo).
 * @returns {string} SVG autoconsistente
 */
export function logoLockupSvg(opts = {}) {
  const { height = 32, accent = '#ec5a72', contrast = '#ffffff', text = '#262420' } = opts
  const gap = height * 0.28
  const fontSize = height * 0.62
  const width = height + gap + fontSize * 5.1
  const mark = logoMarkSvg({ size: height, accent, contrast })
    .replace(/^<svg[^>]*>/, '')
    .replace(/<\/svg>$/, '')

  return (
    `<svg xmlns="http://www.w3.org/2000/svg" width="${round(width)}" height="${height}" ` +
    `viewBox="0 0 ${round(width)} ${height}" role="img" aria-label="appgrove">` +
    `<g transform="scale(${round(height / 32)})">${mark}</g>` +
    `<text x="${round(height + gap)}" y="${round(height * 0.71)}" ` +
    `font-family="Plus Jakarta Sans, ui-sans-serif, system-ui, sans-serif" ` +
    `font-size="${round(fontSize)}" font-weight="800" letter-spacing="-0.5" fill="${text}">appgrove</text>` +
    `</svg>`
  )
}

/** Arrotonda a due decimali, per non sporcare l'SVG di cifre inutili. */
function round(n) {
  return Math.round(n * 100) / 100
}
