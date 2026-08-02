// Test del generatore immagine Open Graph (UC 0057).
import { test } from 'node:test'
import assert from 'node:assert/strict'
import sharp from 'sharp'
import { ogSvg, renderOgImage, wrap, escapeXml } from '../lib/og-image.mjs'
import { OG_WIDTH, OG_HEIGHT, OG_PALETTE, ACCENT_HEX, accentHex } from '../lib/branding.mjs'
import {
  allBrandHexes,
  hex,
  colorHexes,
} from '../../../frontend/packages/design-system/src/tokens/tokens.mjs'
import { LOGO_PATHS } from '../../../frontend/packages/design-system/src/brand/logo.mjs'

test('ogSvg è un SVG 1200×630 col nome app e il colore-categoria', () => {
  const svg = ogSvg({ appName: 'Note Pro', tagline: 'Prendi appunti in pochi clic', accent: 'cat-violet' })
  assert.match(svg, /^<svg[^>]*width="1200"[^>]*height="630"/)
  assert.ok(svg.includes('Note Pro'), 'contiene il nome app')
  assert.ok(svg.includes(accentHex('cat-violet')), 'usa il colore della categoria')
  assert.ok(svg.includes('appgrove'), 'porta il wordmark')
})

test('ogSvg escapa i caratteri XML nel nome app', () => {
  const svg = ogSvg({ appName: 'Fatture & Co <b>', tagline: '', accent: 'cat-blue' })
  assert.ok(svg.includes('Fatture &amp; Co &lt;b&gt;'))
  assert.ok(!svg.includes('<b>'), 'nessun tag non escapato')
})

test('accentHex ricade su cat-blue per token sconosciuti', () => {
  assert.equal(accentHex('cat-inesistente'), accentHex('cat-blue'))
})

test('wrap manda a capo entro il numero di righe e caratteri', () => {
  const lines = wrap('una frase piuttosto lunga da mandare a capo su piu righe', 20, 3)
  assert.ok(lines.length <= 3)
  for (const l of lines) assert.ok(l.length <= 24, `riga troppo lunga: "${l}"`)
})

test('escapeXml gestisce apici e e-commerciale', () => {
  assert.equal(escapeXml(`a & b "c" 'd'`), 'a &amp; b &quot;c&quot; &apos;d&apos;')
})

// ── on-brand per davvero (UC 0086) ───────────────────────────────────────────
// Prima della change 0080 il fondo di questa immagine era un blu-navy freddo che nella
// palette appgrove non è mai esistito, e nessun test se ne accorgeva: l'anteprima social
// è l'unico artefatto del progetto che nessuno guarda finché non finisce su un social.

test('i colori-categoria arrivano dai token, non da una copia', () => {
  const daiToken = Object.fromEntries(
    Object.entries(colorHexes()).filter(([name]) => name.startsWith('cat-')),
  )
  assert.deepEqual(ACCENT_HEX, daiToken)
  assert.equal(Object.keys(ACCENT_HEX).length, 6, 'le sei categorie del design system')
})

test('la palette dell immagine social è quella scura CALDA del brand', () => {
  assert.equal(OG_PALETTE.bgFrom, hex('bg', { theme: 'dark' }))
  assert.equal(OG_PALETTE.bgTo, hex('surface-3', { theme: 'dark' }))
  assert.equal(OG_PALETTE.text, hex('text', { theme: 'dark' }))
  assert.equal(OG_PALETTE.textMuted, hex('text-muted', { theme: 'dark' }))
})

test('ogSvg non contiene nessun colore fuori dal brand kit', () => {
  const svg = ogSvg({ appName: 'Note Pro', tagline: 'Prendi appunti in pochi clic', accent: 'cat-violet' })
  const brand = allBrandHexes()
  const usati = new Set((svg.match(/#[0-9a-f]{6}/gi) ?? []).map((c) => c.toLowerCase()))
  const fuori = [...usati].filter((c) => !brand.has(c))
  assert.deepEqual(fuori, [], `colori fuori dal brand nell immagine social: ${fuori.join(', ')}`)
  assert.ok(usati.size > 0, 'l immagine usa dei colori')
})

test('ogSvg porta il mark del logo del pacchetto, non un disegno suo', () => {
  const svg = ogSvg({ appName: 'Esempio', tagline: 'x', accent: 'cat-blue' })
  // I tracciati si CHIEDONO al pacchetto, non si ricopiano qui: un tracciato scritto a mano
  // nel test è una seconda copia del disegno, cioè il difetto che il brand kit combatte —
  // e infatti l'artwork definitivo (UC 0087) lo aveva già reso falso.
  const attesi = LOGO_PATHS.filter((p) => p.detail === 'always')
  assert.ok(attesi.length > 0, 'il pacchetto deve dichiarare almeno un tracciato')
  for (const p of attesi) {
    assert.ok(svg.includes(p.d), `contiene il tracciato del mark condiviso: ${p.d.slice(0, 24)}…`)
  }
})

test('renderOgImage produce un PNG valido 1200×630', async () => {
  const png = await renderOgImage({ appName: 'Esempio', tagline: 'Fai il lavoro, prima la privacy', accent: 'cat-teal' })
  assert.equal(png.slice(0, 4).toString('hex'), '89504e47', 'firma PNG')
  const meta = await sharp(png).metadata()
  assert.equal(meta.width, OG_WIDTH)
  assert.equal(meta.height, OG_HEIGHT)
  assert.equal(meta.format, 'png')
})
