/**
 * appgrove design system — Tailwind preset (UC 0019).
 *
 * Mappa i token (CSS custom properties di `src/tokens/tokens.css`) sul theme Tailwind,
 * così SPA e — cross-progetto — la vetrina Astro condividono la stessa fonte. I colori usano
 * `rgb(var(--ag-*) / <alpha-value>)` per supportare l'opacità (es. bg-accent/10). Tema (light/dark)
 * e accent cambiano a runtime via attributi sul nodo radice, senza ricompilare.
 *
 * @type {Partial<import('tailwindcss').Config>}
 */
const withAlpha = (cssVar) => `rgb(var(${cssVar}) / <alpha-value>)`

export default {
  darkMode: ['class', '[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
        bg: withAlpha('--ag-bg'),
        surface: {
          DEFAULT: withAlpha('--ag-surface'),
          2: withAlpha('--ag-surface-2'),
          3: withAlpha('--ag-surface-3'),
        },
        line: {
          DEFAULT: withAlpha('--ag-border'),
          strong: withAlpha('--ag-border-strong'),
        },
        fg: {
          DEFAULT: withAlpha('--ag-text'),
          muted: withAlpha('--ag-text-muted'),
          faint: withAlpha('--ag-text-faint'),
        },
        accent: {
          DEFAULT: withAlpha('--ag-accent'),
          contrast: withAlpha('--ag-accent-contrast'),
        },
        success: withAlpha('--ag-success'),
        warning: withAlpha('--ag-warning'),
        danger: withAlpha('--ag-danger'),
        cat: {
          green: withAlpha('--ag-cat-green'),
          amber: withAlpha('--ag-cat-amber'),
          red: withAlpha('--ag-cat-red'),
          blue: withAlpha('--ag-cat-blue'),
          violet: withAlpha('--ag-cat-violet'),
          teal: withAlpha('--ag-cat-teal'),
        },
      },
      borderRadius: {
        sm: 'var(--ag-radius-sm)',
        md: 'var(--ag-radius-md)',
        lg: 'var(--ag-radius-lg)',
        xl: 'var(--ag-radius-xl)',
        pill: 'var(--ag-radius-pill)',
      },
      boxShadow: {
        sm: 'var(--ag-shadow-sm)',
        DEFAULT: 'var(--ag-shadow)',
        lg: 'var(--ag-shadow-lg)',
      },
      fontFamily: {
        sans: 'var(--ag-font-sans)',
        mono: 'var(--ag-font-mono)',
      },
    },
  },
}
