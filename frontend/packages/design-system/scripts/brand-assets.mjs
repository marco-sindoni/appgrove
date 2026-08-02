#!/usr/bin/env node
// ─────────────────────────────────────────────────────────────────────────────
// scripts/brand-assets.mjs — scrive i derivati del logo (UC 0087).
//
// Uso:  npm run brand:assets           (dal pacchetto design-system)
//
// Rigenera icone e anteprima social A PARTIRE dal disegno unico. Va eseguito ogni volta
// che l'artwork in `src/brand/logo.mjs` cambia: il test `assets.test.ts` confronta i file
// vettoriali committati con la generazione e diventa rosso se qualcuno se ne dimentica.
//
// I due file a griglia di pixel (icona iOS, anteprima social) richiedono la libreria di
// rasterizzazione `sharp`, che NON è una dipendenza di questo pacchetto: è già installata
// nel sito (Astro) e nello strumento delle landing. Se non si trova, lo script scrive
// comunque i vettoriali e lo dice — non fallisce, perché i vettoriali sono la parte che
// la suite sorveglia.
//   Per averla a disposizione:  NODE_PATH=../../../site/node_modules npm run brand:assets
// ─────────────────────────────────────────────────────────────────────────────
import { writeFileSync, mkdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { svgAssets, rasterAssets } from '../src/brand/assets.mjs'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..', '..')

for (const asset of svgAssets()) {
  const dest = join(ROOT, asset.path)
  mkdirSync(dirname(dest), { recursive: true })
  writeFileSync(dest, `${asset.content}\n`, 'utf8')
  console.log(`  ✓ ${asset.path}`)
}

/**
 * Cerca la libreria di rasterizzazione dove il monorepo già ce l'ha. I moduli ES non
 * leggono `NODE_PATH`, quindi il percorso va tentato per esteso: prima come dipendenza
 * normale, poi nei due posti in cui esiste davvero.
 */
async function caricaSharp() {
  const candidati = [
    'sharp',
    pathToFileURL(join(ROOT, 'site/node_modules/sharp/lib/index.js')).href,
    pathToFileURL(join(ROOT, 'tools/finalize-landing/node_modules/sharp/lib/index.js')).href,
  ]
  for (const candidato of candidati) {
    try {
      return (await import(candidato)).default
    } catch {
      /* si prova il prossimo */
    }
  }
  return null
}

const sharp = await caricaSharp()
if (!sharp) {
  console.warn(
    `\n⚠ libreria di rasterizzazione non trovata: i file PNG non sono stati rigenerati.` +
      `\n  Installa le dipendenze del sito (cd site && npm ci) e riesegui.`,
  )
}

if (sharp) {
  for (const asset of rasterAssets()) {
    const dest = join(ROOT, asset.path)
    mkdirSync(dirname(dest), { recursive: true })
    const buffer = await sharp(Buffer.from(asset.svg), { density: 200 })
      .resize(asset.width, asset.height)
      .png()
      .toBuffer()
    writeFileSync(dest, buffer)
    console.log(`  ✓ ${asset.path} (${asset.width}×${asset.height})`)
  }
}

console.log(`\nDerivati del logo rigenerati dal disegno unico (src/brand/logo.mjs).`)
