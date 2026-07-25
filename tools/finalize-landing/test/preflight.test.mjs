// Test del preflight di pubblicazione (UC 0057).
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { preflightFiles, EXPECTED_FILES } from '../lib/preflight.mjs'
import { LOCALES, DRAFT_SENTINEL } from '../lib/branding.mjs'

/** File lingua "pronto": screenshot cablato, ogImage cablata, nessun sentinella. */
function readyLocale(locale) {
  return {
    name: `${locale}.ts`,
    text: `export const ${locale} = { slug: 'note', meta: { ogImage: '/landings/note/og.png' }, hero: { badge: 'all-EU · GDPR-first', screenshot: { src: '/landings/note/hero.${locale}.png' } } }`,
  }
}
/** Cartella landing pronta: 5 lingue pronte + index published. */
function readyFiles() {
  return [...LOCALES.map(readyLocale), { name: 'index.ts', text: "status: 'published'" }]
}

test('preflight verde su una landing pronta (5 lingue, asset cablati, niente sentinella)', () => {
  const { ok, errors } = preflightFiles(readyFiles())
  assert.deepEqual(errors, [])
  assert.equal(ok, true)
})

test('EXPECTED_FILES = 5 lingue + index', () => {
  assert.deepEqual(new Set(EXPECTED_FILES), new Set([...LOCALES.map((l) => `${l}.ts`), 'index.ts']))
})

test('preflight rosso: manca una lingua', () => {
  const files = readyFiles().filter((f) => f.name !== 'de.ts')
  const { ok, errors } = preflightFiles(files)
  assert.equal(ok, false)
  assert.ok(errors.some((e) => e.includes('de.ts')))
})

test('preflight rosso: sentinella «DA RIFINIRE» ancora presente', () => {
  const files = readyFiles()
  files[0].text = files[0].text.replace('all-EU', `${DRAFT_SENTINEL} — all-EU`)
  const { ok, errors } = preflightFiles(files)
  assert.equal(ok, false)
  assert.ok(errors.some((e) => e.includes('copy non rifinita')))
})

test('preflight rosso: screenshot ancora placeholder (src: null)', () => {
  const files = readyFiles()
  files[1].text = files[1].text.replace(/src: '[^']*'/, 'src: null')
  const { ok, errors } = preflightFiles(files)
  assert.equal(ok, false)
  assert.ok(errors.some((e) => e.includes('screenshot non catturato')))
})

test('preflight rosso: immagine Open Graph mancante (ogImage: null)', () => {
  const files = readyFiles()
  files[2].text = files[2].text.replace(/ogImage: '[^']*'/, 'ogImage: null')
  const { ok, errors } = preflightFiles(files)
  assert.equal(ok, false)
  assert.ok(errors.some((e) => e.includes('Open Graph')))
})

test('preflight cumula più problemi insieme', () => {
  const files = readyFiles().filter((f) => f.name !== 'fr.ts')
  files[0].text = files[0].text.replace('all-EU', `${DRAFT_SENTINEL} — all-EU`)
  const { errors } = preflightFiles(files)
  assert.ok(errors.length >= 2)
})
