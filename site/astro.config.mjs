// @ts-check
import { fileURLToPath } from 'node:url'
import { defineConfig } from 'astro/config'
import react from '@astrojs/react'
import tailwind from '@astrojs/tailwind'
import { LOCALES, DEFAULT_LOCALE } from './src/lib/i18n.ts'

// Vetrina appgrove (UC 0036). SSG puro (nessun backend): output statico su
// S3+CloudFront. i18n a subpath per lingua (#14 14): /en|it|fr|es|de/… con EN
// come default, prefissato anch'esso per uniformità degli URL e degli hreflang.
// `site` è il dominio pubblico: serve a generare URL assoluti negli hreflang.
export default defineConfig({
  site: process.env.SITE_URL || 'https://appgrove.app',
  trailingSlash: 'always',
  build: { format: 'directory' },
  integrations: [
    react(),
    tailwind({ applyBaseStyles: false }),
  ],
  i18n: {
    defaultLocale: DEFAULT_LOCALE,
    locales: [...LOCALES],
    routing: {
      prefixDefaultLocale: true,
      redirectToDefaultLocale: false,
    },
  },
  vite: {
    server: {
      fs: {
        // Il design system è collegato come dipendenza `file:` in
        // frontend/packages/design-system: i font @fontsource si risolvono dalla
        // loro posizione reale in frontend/node_modules, FUORI da site/. In dev
        // Vite serve solo file dentro la radice del progetto: allarghiamo il
        // permesso alla radice del monorepo (solo dev; la build li impacchetta).
        allow: [fileURLToPath(new URL('..', import.meta.url))],
      },
    },
  },
})
