// ─────────────────────────────────────────────────────────────────────────────
// Collaudo dei DERIVATI del logo (UC 0087) — icone e anteprima social.
//
// Perché sta qui e non fra i test del pacchetto: questi non sono test di unità, sono un
// controllo sul REPOSITORY VERO — «i file committati corrispondono ancora al disegno?».
// È lo stesso mestiere di `check.mjs`, e va eseguito in Node puro: i moduli del brand kit
// leggono i token dal disco, cosa che l'esecutore di test del frontend (che serve i moduli
// via HTTP) non consente.
//
// Come leggere un rosso: NON aggiornare l'atteso. Rilanciare
//   cd frontend/packages/design-system && npm run brand:assets
// e ricommittare gli artefatti: il disegno è la sorgente, i file sono la sua ombra.
// ─────────────────────────────────────────────────────────────────────────────
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync, statSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  APPLE_TOUCH_SIZE,
  OG_HEIGHT,
  OG_WIDTH,
  appIconSvg,
  faviconSvg,
  platformOgSvg,
  rasterAssets,
  svgAssets,
} from '../../../frontend/packages/design-system/src/brand/assets.mjs'
import { LOGO_PATHS } from '../../../frontend/packages/design-system/src/brand/logo.mjs'
import { allBrandHexes } from '../../../frontend/packages/design-system/src/tokens/tokens.mjs'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..')

/** Larghezza e altezza di un PNG, lette dall'intestazione (nessuna libreria). */
function dimensioniPng(percorso) {
  const testa = readFileSync(percorso).subarray(0, 33)
  assert.equal(testa.subarray(1, 4).toString('ascii'), 'PNG', `${percorso} non è un PNG`)
  return { width: testa.readUInt32BE(16), height: testa.readUInt32BE(20) }
}

test('i file vettoriali committati coincidono con la generazione dal disegno unico', () => {
  for (const asset of svgAssets()) {
    const scritto = readFileSync(join(ROOT, asset.path), 'utf8')
    assert.equal(
      scritto,
      `${asset.content}\n`,
      `${asset.path} non è aggiornato: lancia \`npm run brand:assets\` nel design-system`,
    )
  }
})

test('gli artefatti dipingono solo con colori del brand', () => {
  const brand = allBrandHexes()
  for (const contenuto of [...svgAssets().map((a) => a.content), appIconSvg(), platformOgSvg()]) {
    for (const colore of contenuto.match(/#[0-9a-f]{6}/gi) ?? []) {
      assert.ok(brand.has(colore.toLowerCase()), `colore fuori palette negli artefatti: ${colore}`)
    }
  }
})

test("l'icona della scheda del browser usa il disegno compatto", () => {
  const svg = faviconSvg()
  for (const p of LOGO_PATHS.filter((p) => p.detail === 'full')) {
    assert.ok(!svg.includes(p.d), 'il dettaglio fine non deve comparire a 16 px')
  }
  for (const p of LOGO_PATHS.filter((p) => p.detail === 'always')) assert.ok(svg.includes(p.d))
})

test("l'icona applicativa è piena fino ai bordi e tiene il segno nell'area sicura", () => {
  const svg = appIconSvg({ size: 512 })
  assert.ok(svg.includes('viewBox="0 0 512 512"'))
  assert.ok(svg.includes('<rect width="512" height="512"'), 'il fondo deve arrivare ai bordi')
  const [, sx, sy] = svg.match(/translate\(([\d.]+) ([\d.]+)\)/) ?? []
  assert.equal(sx, sy, 'il segno deve essere centrato')
  assert.ok(Number(sx) > 0, 'il segno deve stare dentro un margine di sicurezza')
})

test("l'anteprima social ha il rapporto atteso e nessun testo tradotto", () => {
  const svg = platformOgSvg()
  assert.ok(svg.includes(`width="${OG_WIDTH}" height="${OG_HEIGHT}"`))
  assert.ok(Math.abs(OG_WIDTH / OG_HEIGHT - 1.91) < 0.02, 'rapporto 1.91:1 richiesto dai social')
  const testi = [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map((m) => m[1])
  assert.equal(testi.length, 2, 'solo nome del marchio e payoff neutro')
  assert.equal(testi[0], 'appgrove')
})

test('i file a griglia di pixel esistono e hanno le dimensioni dichiarate', () => {
  for (const asset of rasterAssets()) {
    const percorso = join(ROOT, asset.path)
    assert.ok(statSync(percorso).size > 0, `${asset.path} è vuoto`)
    assert.deepEqual(dimensioniPng(percorso), { width: asset.width, height: asset.height })
  }
  assert.deepEqual(
    rasterAssets().map((a) => a.width),
    [APPLE_TOUCH_SIZE, OG_WIDTH],
  )
})
