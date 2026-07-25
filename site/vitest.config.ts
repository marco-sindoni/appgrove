/// <reference types="vitest" />
// Config vitest Astro-aware (UC 0038): `getViteConfig` aggiunge i plugin di Astro
// così i test possono importare e RENDERE componenti `.astro` (Container API), oltre
// ai moduli TypeScript puri già coperti (contenuti marketing/legali). Trasparente
// per i test esistenti, che restano TypeScript puro.
import { getViteConfig } from 'astro/config'

export default getViteConfig({
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
  },
})
