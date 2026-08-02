// ─────────────────────────────────────────────────────────────────────────────
// tools/finalize-landing/lib/branding.mjs — costanti condivise dell'helper.
//
// Le 5 lingue e gli slug riservati restano ricopiati oltre il confine dello strumento
// (stesso schema di tools/compliance e site/scripts/postbuild-check.mjs): stanno in
// sorgenti TypeScript, che Node puro non carica.
//
// I COLORI invece no: dalla change 0080 (UC 0086) arrivano dal brand kit, letti dai
// token veri tramite `@appgrove/design-system/tokens.js` — un modulo JavaScript puro
// nato apposta perché i consumatori non-CSS non debbano più ricopiarli. Prima erano
// duplicati qui in esadecimale, ed è così che nasce la divergenza silenziosa.
// ─────────────────────────────────────────────────────────────────────────────
import { colorHexes, hex } from '../../../frontend/packages/design-system/src/tokens/tokens.mjs'

/** Le 5 lingue del sito vetrina (UC 0036). EN è la sorgente marketing. */
export const LOCALES = ['en', 'it', 'fr', 'es', 'de']

// Slug riservati del sito — allineato a site/src/lib/routes.ts (BRAND_SLUGS) e
// landings.ts. Include gli slug brand LOCALIZZATI per lingua (UC 0040): perche/prezzi/
// pourquoi/tarifs/… Over-reserve innocuo (il gate preciso per-lingua è validateLandings()).
export const RESERVED_SLUGS = new Set([
  'why', 'perche', 'pourquoi', 'por-que', 'warum',
  'pricing', 'prezzi', 'tarifs', 'precios', 'preise',
  'legal',
  'apps',
  'blog',
  'support',
  'coming-soon',
])

/**
 * Sentinella della bozza: `new-application` lo mette nel badge dell'hero di ogni
 * lingua. Finché è presente, la copy non è stata rifinita → il preflight rifiuta la
 * pubblicazione. `finalize-landing` lo rimuove sostituendo il badge con quello vero.
 */
export const DRAFT_SENTINEL = 'DA RIFINIRE'

/**
 * Colori-categoria del design system, LETTI dai token (`--ag-cat-*` in tokens.css):
 * ogni app ha il colore della sua categoria nell'immagine Open Graph. Default: cat-blue.
 * Nessun valore è scritto qui — se un colore-categoria cambia nel brand kit, cambia qui
 * da solo.
 */
export const ACCENT_HEX = Object.fromEntries(
  Object.entries(colorHexes()).filter(([name]) => name.startsWith('cat-')),
)

/** Esadecimale del token colore-categoria (o del default cat-blue se sconosciuto). */
export function accentHex(token) {
  return ACCENT_HEX[token] ?? ACCENT_HEX['cat-blue']
}

/**
 * Palette dell'immagine Open Graph, dal brand kit. L'anteprima social è scura — un
 * rettangolo scuro risalta nelle bacheche dei social — ma scura NEI NEUTRI CALDI di
 * appgrove: prima di UC 0086 questo generatore disegnava su un blu-navy freddo che nella
 * palette non è mai esistito, e nessun test se ne accorgeva.
 */
export const OG_PALETTE = {
  bgFrom: hex('bg', { theme: 'dark' }),
  bgTo: hex('surface-3', { theme: 'dark' }),
  text: hex('text', { theme: 'dark' }),
  textMuted: hex('text-muted', { theme: 'dark' }),
  markContrast: hex('accent-contrast'),
}

/** Dimensioni canoniche dell'immagine Open Graph (rapporto 1.91:1, standard social). */
export const OG_WIDTH = 1200
export const OG_HEIGHT = 630
