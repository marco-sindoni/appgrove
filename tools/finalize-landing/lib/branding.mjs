// ─────────────────────────────────────────────────────────────────────────────
// tools/finalize-landing/lib/branding.mjs — costanti condivise dell'helper.
//
// Piccole costanti ricopiate oltre il confine dello strumento (stesso schema di
// tools/compliance e site/scripts/postbuild-check.mjs): le 5 lingue, gli slug
// riservati (allineati a site/src/lib/landings.ts) e i colori-categoria del design
// system (allineati a frontend/packages/design-system/src/tokens/tokens.css). Un
// import cross-pacchetto verso `site/` o il design system sarebbe fragile (TypeScript
// non caricabile da Node puro); duplicare qualche costante stabile è la scelta meno
// rischiosa.
// ─────────────────────────────────────────────────────────────────────────────

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
 * Colori-categoria del design system (UC 0019), da `--ag-cat-*` (terne RGB in
 * tokens.css) a esadecimale. Servono all'immagine Open Graph on-brand: ogni app
 * ha il colore della sua categoria. Default: cat-blue.
 */
export const ACCENT_HEX = {
  'cat-blue': '#5b8def',
  'cat-violet': '#8a76f0',
  'cat-green': '#3aae73',
  'cat-amber': '#dd9b34',
  'cat-red': '#e3654f',
  'cat-teal': '#1fb6a6',
}

/** Esadecimale del token colore-categoria (o del default cat-blue se sconosciuto). */
export function accentHex(token) {
  return ACCENT_HEX[token] ?? ACCENT_HEX['cat-blue']
}

/** Dimensioni canoniche dell'immagine Open Graph (rapporto 1.91:1, standard social). */
export const OG_WIDTH = 1200
export const OG_HEIGHT = 630
