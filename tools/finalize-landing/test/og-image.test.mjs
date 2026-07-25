// Test del generatore immagine Open Graph (UC 0057).
import { test } from 'node:test'
import assert from 'node:assert/strict'
import sharp from 'sharp'
import { ogSvg, renderOgImage, wrap, escapeXml } from '../lib/og-image.mjs'
import { OG_WIDTH, OG_HEIGHT, accentHex } from '../lib/branding.mjs'

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

test('renderOgImage produce un PNG valido 1200×630', async () => {
  const png = await renderOgImage({ appName: 'Esempio', tagline: 'Fai il lavoro, prima la privacy', accent: 'cat-teal' })
  assert.equal(png.slice(0, 4).toString('hex'), '89504e47', 'firma PNG')
  const meta = await sharp(png).metadata()
  assert.equal(meta.width, OG_WIDTH)
  assert.equal(meta.height, OG_HEIGHT)
  assert.equal(meta.format, 'png')
})
