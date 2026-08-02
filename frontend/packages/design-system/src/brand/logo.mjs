// ─────────────────────────────────────────────────────────────────────────────
// @appgrove/design-system/logo.js — il disegno del logo, in un solo posto (UC 0086/0087).
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
// L'ARTWORK (UC 0087). Il segnaposto è stato sostituito dal disegno definitivo:
// una foglia dentro una piastrella ad angoli morbidi. La foglia è costruita con due
// archi di cerchio simmetrici rispetto alla diagonale — geometria pura, nessun punto
// «a occhio» — e la nervatura parte dalla punta bassa e si ferma ai due terzi, che è
// ciò che rende la forma leggibile come foglia e non come lente.
//
// Tre letture dello stesso segno, non tre disegni:
//  • completa   — foglia + nervatura, per le dimensioni normali (≥ 22 px);
//  • compatta   — sola foglia, per gli spazi piccoli (icona del browser, elenchi): sotto
//                 i 22 px un tratto da 1.6 unità è meno di un pixel e sporca invece di
//                 descrivere;
//  • monocroma  — un colore solo, per gli sfondi difficili (fotografie, stampa a un
//                 colore): la piastrella diventa un contorno e la foglia resta piena.
// ─────────────────────────────────────────────────────────────────────────────

/** Riquadro di disegno del mark (quadrato). Le proporzioni dei tracciati vi si riferiscono. */
export const LOGO_VIEWBOX = '0 0 32 32'

/** Raggio degli angoli della piastrella del mark, nelle unità del riquadro di disegno. */
export const LOGO_TILE_RADIUS = 9

/**
 * Soglia (in pixel) sotto la quale si usa il disegno compatto. Non è un gusto: è il
 * punto in cui la nervatura scende sotto il pixel e smette di descrivere la forma.
 */
export const LOGO_COMPACT_BELOW = 22

/**
 * I tracciati del mark, in ordine di disegno. Ogni tracciato dichiara il RUOLO del suo
 * colore (non il colore), così la stessa figura si adatta a tema chiaro e scuro:
 *  - `accent`   → il colore d'accento vivo (la piastrella, la nervatura incisa);
 *  - `contrast` → il colore che ci sta sopra leggibile (il corpo della foglia).
 *
 * `detail: 'always'` = presente a ogni dimensione; `detail: 'full'` = solo nel disegno
 * completo, cioè quello che si toglie negli spazi piccoli.
 */
export const LOGO_PATHS = [
  {
    // Corpo della foglia: due archi di cerchio (r = 13) fra le punte (7.6, 24.4) e
    // (24.4, 7.6). Simmetrica rispetto alla diagonale, centrata in (16, 16). Il raggio
    // è la sola leva sulla forma: più stretto la gonfia fino al cerchio, più largo la
    // assottiglia fino alla lente. 13 è il punto in cui resta una foglia — verificato
    // guardandola a 4× e a 24 px, non calcolato.
    role: 'accent',
    fill: true,
    d: 'M7.6 24.4A13 13 0 0 1 24.4 7.6 13 13 0 0 1 7.6 24.4Z',
    on: 'contrast',
    detail: 'always',
  },
  {
    // Nervatura incisa nel corpo: parte dalla punta bassa e si ferma ai due terzi.
    role: 'accent',
    fill: false,
    d: 'M9.4 22.6 19.3 12.7',
    strokeWidth: 1.7,
    detail: 'full',
  },
]

/** I tracciati effettivamente disegnati alla dimensione richiesta. */
export function logoPathsFor(size = 32) {
  return size < LOGO_COMPACT_BELOW ? LOGO_PATHS.filter((p) => p.detail === 'always') : LOGO_PATHS
}

/**
 * Genera l'SVG completo del mark come stringa, per i consumatori che non compilano
 * React (immagine social, favicon, anteprime).
 *
 * @param {{ size?: number, accent?: string, contrast?: string, title?: string,
 *           mono?: string, compact?: boolean }} [opts]
 *   `accent` e `contrast` sono i due colori, nella forma che serve al consumatore
 *   (esadecimale per Node, `rgb(var(--ag-accent))` per il browser).
 *   `mono` = variante a un colore solo per gli sfondi difficili: il valore È il colore,
 *   la piastrella diventa un contorno e la foglia resta piena.
 *   `compact` forza (o vieta) il disegno compatto; se assente decide la dimensione.
 * @returns {string} SVG autoconsistente
 */
export function logoMarkSvg(opts = {}) {
  const { size = 32, accent = '#ec5a72', contrast = '#ffffff', title = 'appgrove', mono, compact } = opts
  const usaCompatto = compact ?? size < LOGO_COMPACT_BELOW
  const disegnati = usaCompatto ? LOGO_PATHS.filter((p) => p.detail === 'always') : LOGO_PATHS

  // In monocromia c'è un colore solo: la piastrella si svuota (contorno) perché una
  // foglia dello stesso colore su un pieno dello stesso colore sarebbe invisibile.
  const tile = mono
    ? `<rect x="1" y="1" width="30" height="30" rx="${LOGO_TILE_RADIUS - 1}" fill="none" stroke="${mono}" stroke-width="2"/>`
    : `<rect width="32" height="32" rx="${LOGO_TILE_RADIUS}" fill="${accent}"/>`

  const paths = disegnati
    .map((p) => {
      const color = mono ?? (p.on === 'contrast' ? contrast : accent)
      return p.fill
        ? `<path d="${p.d}" fill="${color}"/>`
        : `<path d="${p.d}" stroke="${mono ?? accent}" stroke-width="${p.strokeWidth}" stroke-linecap="round" fill="none"/>`
    })
    .join('')

  return (
    `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" ` +
    `viewBox="${LOGO_VIEWBOX}" role="img" aria-label="${title}">` +
    `${tile}${paths}</svg>`
  )
}

/**
 * Il logo completo (mark + wordmark) come SVG, per le superfici larghe: immagine
 * social, intestazioni statiche. Il wordmark usa il carattere del brand con i suoi
 * ripieghi, così resta leggibile anche dove Plus Jakarta Sans non è disponibile.
 *
 * @param {{ height?: number, accent?: string, contrast?: string, text?: string, mono?: string }} [opts]
 *   `text` è il colore del wordmark (di norma il token del testo); in monocromia lo
 *   sovrascrive `mono`, perché la variante a un colore vale per tutto il segno.
 * @returns {string} SVG autoconsistente
 */
export function logoLockupSvg(opts = {}) {
  const { height = 32, accent = '#ec5a72', contrast = '#ffffff', text = '#262420', mono } = opts
  const gap = height * 0.28
  const fontSize = height * 0.62
  const width = height + gap + fontSize * 5.1
  const mark = logoMarkSvg({ size: height, accent, contrast, mono })
    .replace(/^<svg[^>]*>/, '')
    .replace(/<\/svg>$/, '')

  return (
    `<svg xmlns="http://www.w3.org/2000/svg" width="${round(width)}" height="${height}" ` +
    `viewBox="0 0 ${round(width)} ${height}" role="img" aria-label="appgrove">` +
    `<g transform="scale(${round(height / 32)})">${mark}</g>` +
    `<text x="${round(height + gap)}" y="${round(height * 0.71)}" ` +
    `font-family="Plus Jakarta Sans, ui-sans-serif, system-ui, sans-serif" ` +
    `font-size="${round(fontSize)}" font-weight="800" letter-spacing="-0.5" fill="${mono ?? text}">appgrove</text>` +
    `</svg>`
  )
}

/** Arrotonda a due decimali, per non sporcare l'SVG di cifre inutili. */
function round(n) {
  return Math.round(n * 100) / 100
}
