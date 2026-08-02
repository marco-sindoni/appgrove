// ─────────────────────────────────────────────────────────────────────────────
// @appgrove/design-system — i DERIVATI del logo (UC 0087).
//
// Perché esiste. Il disegno del logo vive in `logo.mjs`, ma i consumatori non chiedono
// «il disegno»: chiedono un file. Il browser vuole un'icona di scheda, il telefono
// un'icona da schermata iniziale, i social un rettangolo 1200×630. Finché quei file si
// disegnano a mano sono copie, e una copia diverge appena l'artwork cambia.
//
// Qui i file sono FUNZIONI del disegno: `scripts/brand-assets.mjs` le esegue e scrive gli
// artefatti, un test confronta gli artefatti scritti con la generazione. Se qualcuno
// ritocca l'artwork e non rigenera, la suite diventa rossa — che è tutto il punto.
//
// I colori arrivano dai token veri (`tokens.mjs`), mai scritti a mano: gli artefatti sono
// file statici, quindi il colore va COTTO dentro, e cuocerlo è legittimo solo se il valore
// viene dalla fonte unica.
// ─────────────────────────────────────────────────────────────────────────────
import { hex } from '../tokens/tokens.mjs'
import { logoMarkSvg, logoPathsFor } from './logo.mjs'

/** Dimensioni canoniche degli artefatti. */
export const FAVICON_SIZE = 32
export const APP_ICON_SIZE = 512
export const APPLE_TOUCH_SIZE = 180
export const OG_WIDTH = 1200
export const OG_HEIGHT = 630

/** Payoff dell'anteprima social: neutro rispetto alla lingua (il sito ne parla cinque). */
export const OG_PAYOFF = 'micro-SaaS · all-EU · GDPR-first'

/**
 * Icona della scheda del browser (favicon). Disegno COMPATTO: a 16 px la nervatura è
 * meno di un pixel e sporcherebbe la foglia invece di descriverla. La piastrella in
 * accento la rende leggibile sia sulle barre chiare sia su quelle scure, quindi non
 * serve una seconda icona per il tema scuro.
 */
export function faviconSvg() {
  return logoMarkSvg({
    size: FAVICON_SIZE,
    compact: true,
    accent: hex('accent'),
    contrast: hex('accent-contrast'),
  })
}

/**
 * Icona applicativa quadrata (512×512), pensata anche per il ritaglio: il fondo è pieno
 * fino ai bordi — gli angoli li arrotonda il sistema operativo — e il segno sta nel 72%
 * centrale, cioè dentro l'area che nessun ritaglio circolare mangia.
 */
export function appIconSvg({ size = APP_ICON_SIZE } = {}) {
  const accent = hex('accent')
  const contrast = hex('accent-contrast')
  const scala = (size * 0.72) / 32
  const offset = (size - size * 0.72) / 2

  const paths = logoPathsFor(32)
    .map((p) =>
      p.fill
        ? `<path d="${p.d}" fill="${p.on === 'contrast' ? contrast : accent}"/>`
        : `<path d="${p.d}" stroke="${accent}" stroke-width="${p.strokeWidth}" stroke-linecap="round" fill="none"/>`,
    )
    .join('')

  return (
    `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" ` +
    `viewBox="0 0 ${size} ${size}" role="img" aria-label="appgrove">` +
    `<rect width="${size}" height="${size}" fill="${accent}"/>` +
    `<g transform="translate(${arrotonda(offset)} ${arrotonda(offset)}) scale(${arrotonda(scala)})">${paths}</g>` +
    `</svg>`
  )
}

/**
 * Anteprima social di PIATTAFORMA (1200×630): quella che si vede quando si condivide un
 * indirizzo della vetrina che non è la pagina di un'app. Le landing per-app hanno la
 * propria, generata da `tools/finalize-landing` col colore della categoria.
 *
 * Fondo scuro nei neutri caldi del brand — un rettangolo scuro risalta nelle bacheche —
 * e nessun testo tradotto: solo il nome del marchio e un payoff neutro, perché la stessa
 * immagine serve tutte e cinque le lingue.
 */
export function platformOgSvg() {
  const accent = hex('accent')
  const contrast = hex('accent-contrast')
  const bg = hex('bg', { theme: 'dark' })
  const surface = hex('surface-3', { theme: 'dark' })
  const text = hex('text', { theme: 'dark' })
  const textMuted = hex('text-muted', { theme: 'dark' })
  const font = 'Plus Jakarta Sans, Helvetica, Arial, sans-serif'

  const mark = logoMarkSvg({ size: 32, accent, contrast })
    .replace(/^<svg[^>]*>/, '')
    .replace(/<\/svg>$/, '')

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${OG_WIDTH}" height="${OG_HEIGHT}" viewBox="0 0 ${OG_WIDTH} ${OG_HEIGHT}" role="img" aria-label="appgrove">
  <defs>
    <linearGradient id="ag-og-bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="${bg}"/>
      <stop offset="1" stop-color="${surface}"/>
    </linearGradient>
  </defs>
  <rect width="${OG_WIDTH}" height="${OG_HEIGHT}" fill="url(#ag-og-bg)"/>
  <rect x="0" y="0" width="16" height="${OG_HEIGHT}" fill="${accent}"/>
  <circle cx="1010" cy="140" r="230" fill="${accent}" opacity="0.14"/>
  <circle cx="1120" cy="520" r="150" fill="${accent}" opacity="0.08"/>
  <g transform="translate(96 210) scale(4.6)">${mark}</g>
  <text x="264" y="290" font-family="${font}" font-size="104" font-weight="800" fill="${text}" letter-spacing="-2">appgrove</text>
  <text x="100" y="400" font-family="${font}" font-size="38" font-weight="500" fill="${textMuted}">${OG_PAYOFF}</text>
</svg>`
}

/**
 * Gli artefatti VETTORIALI e la loro destinazione, in percorsi relativi alla radice del
 * monorepo. È l'elenco che lo script scrive e che il test riconfronta: aggiungere un
 * consumatore significa aggiungere una riga qui, non ricopiare un file.
 */
export function svgAssets() {
  const favicon = faviconSvg()
  const icon = appIconSvg()
  const og = platformOgSvg()
  return [
    { path: 'site/public/favicon.svg', content: favicon },
    { path: 'site/public/icon.svg', content: icon },
    { path: 'site/public/og-appgrove.svg', content: og },
    { path: 'frontend/apps/backoffice/public/favicon.svg', content: favicon },
    { path: 'frontend/apps/admin/public/favicon.svg', content: favicon },
  ]
}

/**
 * Gli artefatti a GRIGLIA DI PIXEL: esistono solo perché i formati di destinazione non
 * accettano il vettore (iOS vuole un PNG per l'icona da schermata iniziale, i social un
 * PNG per l'anteprima). Li produce lo script con la libreria di rasterizzazione, non la
 * suite: qui si dichiarano sorgente e dimensioni attese.
 */
export function rasterAssets() {
  return [
    { path: 'site/public/apple-touch-icon.png', svg: appIconSvg({ size: APPLE_TOUCH_SIZE }), width: APPLE_TOUCH_SIZE, height: APPLE_TOUCH_SIZE },
    { path: 'site/public/og-appgrove.png', svg: platformOgSvg(), width: OG_WIDTH, height: OG_HEIGHT },
  ]
}

/** Arrotonda a due decimali, per non sporcare l'SVG di cifre inutili. */
function arrotonda(n) {
  return Math.round(n * 100) / 100
}
