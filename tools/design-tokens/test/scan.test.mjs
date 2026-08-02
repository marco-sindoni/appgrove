// Test del rilevatore di divergenza dal brand kit (UC 0086).
// Il collaudo verifica il VERIFICATORE: su cartelle di prova costruite qui, deve trovare
// il colore inventato e assolvere quello che viene dai token. Un controllo che non sa
// diventare rosso non è un presidio.
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { mkdtempSync, mkdirSync, writeFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { scan, extractColors, normalizeHex } from '../lib.mjs'

/** Costruisce una cartella di prova con i file dati (percorsi relativi). */
function fixture(files) {
  const root = mkdtempSync(join(tmpdir(), 'design-tokens-'))
  for (const [path, content] of Object.entries(files)) {
    const full = join(root, path)
    mkdirSync(join(full, '..'), { recursive: true })
    writeFileSync(full, content, 'utf8')
  }
  return root
}

const BRAND = new Set(['#ec5a72', '#f4f4f1', '#262420'])
const WATCHED = [{ path: 'app', why: 'cartella di prova' }]

test('assolve un consumatore che usa solo colori del brand', () => {
  const root = fixture({
    'app/ok.css': '.a { color: #ec5a72; background: #f4f4f1; }',
  })
  const { findings, scanned } = scan({ root, brandHexes: BRAND, watched: WATCHED })
  assert.equal(scanned, 1)
  assert.deepEqual(findings, [])
  rmSync(root, { recursive: true, force: true })
})

test('trova un colore inventato e ne riporta file e riga', () => {
  const root = fixture({
    'app/drift.tsx': 'const a = 1\nconst style = { color: "#0b1020" }\n',
  })
  const { findings } = scan({ root, brandHexes: BRAND, watched: WATCHED })
  assert.equal(findings.length, 1)
  assert.equal(findings[0].color, '#0b1020')
  assert.equal(findings[0].line, 2)
  assert.equal(findings[0].file, 'app/drift.tsx')
  rmSync(root, { recursive: true, force: true })
})

test('riconosce il colore del brand anche scritto in maiuscolo o a tre cifre', () => {
  const root = fixture({
    'app/varianti.css': '.a { color: #EC5A72; }\n.b { color: #fff; }',
  })
  const brand = new Set([...BRAND, '#ffffff'])
  const { findings } = scan({ root, brandHexes: brand, watched: WATCHED })
  assert.deepEqual(findings, [])
  rmSync(root, { recursive: true, force: true })
})

test('non segnala i colori presi da una variabile: quelli vengono dal pacchetto', () => {
  const root = fixture({
    'app/ok.css': '.a { background: rgb(var(--ag-accent) / 0.1); color: rgb(var(--ag-text)); }',
  })
  const { findings } = scan({ root, brandHexes: BRAND, watched: WATCHED })
  assert.deepEqual(findings, [])
  rmSync(root, { recursive: true, force: true })
})

test('segnala una scrittura rgb() con numeri veri fuori dal brand', () => {
  const root = fixture({
    'app/drift.css': '.a { color: rgb(11, 16, 32); }',
  })
  const { findings } = scan({ root, brandHexes: BRAND, watched: WATCHED })
  assert.equal(findings.length, 1)
  assert.equal(findings[0].color, '#0b1020')
  rmSync(root, { recursive: true, force: true })
})

test('non guarda dentro le dipendenze né gli esiti di compilazione', () => {
  const root = fixture({
    'app/node_modules/lib/index.js': 'const c = "#123456"',
    'app/dist/bundle.js': 'const c = "#654321"',
    'app/src/ok.ts': 'const c = "#ec5a72"',
  })
  const { findings, scanned } = scan({ root, brandHexes: BRAND, watched: WATCHED })
  assert.equal(scanned, 1)
  assert.deepEqual(findings, [])
  rmSync(root, { recursive: true, force: true })
})

test('ignora i file senza estensione interessante', () => {
  const root = fixture({
    'app/note.md': 'il colore era #0b1020',
    'app/dati.yaml': 'colore: "#0b1020"',
  })
  const { findings, scanned } = scan({ root, brandHexes: BRAND, watched: WATCHED })
  assert.equal(scanned, 0)
  assert.deepEqual(findings, [])
  rmSync(root, { recursive: true, force: true })
})

test('segnala una radice sorvegliata che non esiste più', () => {
  const root = fixture({ 'app/ok.css': '.a { color: #ec5a72; }' })
  const { missingRoots } = scan({
    root,
    brandHexes: BRAND,
    watched: [...WATCHED, { path: 'sparita', why: 'radice rimossa' }],
  })
  assert.deepEqual(missingRoots, ['sparita'])
  rmSync(root, { recursive: true, force: true })
})

test('normalizeHex espande le tre cifre e scarta il canale di trasparenza', () => {
  assert.equal(normalizeHex('#FFF'), '#ffffff')
  assert.equal(normalizeHex('#EC5A72'), '#ec5a72')
  assert.equal(normalizeHex('#ec5a7280'), '#ec5a72')
})

test('extractColors trova più colori sulla stessa riga', () => {
  const colori = extractColors('border:1px solid #e9e7e1; color:#262420;')
  assert.deepEqual(
    colori.map((c) => c.color),
    ['#e9e7e1', '#262420'],
  )
  assert.equal(colori[0].line, 1)
})
