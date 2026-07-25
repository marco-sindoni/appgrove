import preset from '@appgrove/design-system/preset'

/**
 * Il sito riusa il preset del design system (UC 0019): stessa palette, stessi
 * raggi/ombre/tipografia delle SPA. I token (CSS custom properties) arrivano da
 * `@appgrove/design-system/tokens.css`, importato nel layout.
 * @type {import('tailwindcss').Config}
 */
export default {
  presets: [preset],
  content: ['./src/**/*.{astro,ts,tsx,js,jsx,md,mdx}'],
}
