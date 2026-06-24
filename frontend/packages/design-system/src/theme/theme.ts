export const THEMES = ['light', 'dark'] as const
export type Theme = (typeof THEMES)[number]

export const ACCENTS = ['coral', 'violet', 'teal', 'blue'] as const
export type Accent = (typeof ACCENTS)[number]

export const DEFAULT_THEME: Theme = 'light'
export const DEFAULT_ACCENT: Accent = 'coral'

/**
 * Applica tema e accent scrivendo gli attributi sul nodo dato (default: <html>).
 * I token in tokens.css reagiscono a `data-theme` / `data-accent` → cambio istantaneo e globale.
 */
export function applyTheme(
  { theme, accent }: { theme: Theme; accent: Accent },
  node: HTMLElement = document.documentElement,
): void {
  node.setAttribute('data-theme', theme)
  node.setAttribute('data-accent', accent)
}
