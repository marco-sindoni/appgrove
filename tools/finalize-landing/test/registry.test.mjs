// Test delle modifiche meccaniche ai file landing (UC 0057).
import { test } from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import {
  publish,
  unpublish,
  readStatus,
  setScreenshotSrc,
  setOgImage,
  screenshotAssetPath,
  ogImageAssetPath,
} from '../lib/registry.mjs'

/** Crea una cartella landing di prova (index draft + un file lingua bozza) e la ritorna. */
function makeDraft() {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'finalize-landing-'))
  fs.writeFileSync(
    path.join(dir, 'index.ts'),
    "export const noteLanding = { appId: 'note', status: 'draft', content: {} }\n",
  )
  fs.writeFileSync(
    path.join(dir, 'en.ts'),
    "export const en = { meta: { ogImage: null }, hero: { screenshot: { src: null, alt: 'x' } } }\n",
  )
  return dir
}

test('publish porta lo stato draft→published', () => {
  const dir = makeDraft()
  assert.equal(readStatus(dir), 'draft')
  const { changed } = publish(dir)
  assert.equal(changed, true)
  assert.equal(readStatus(dir), 'published')
})

test('publish è idempotente (già published → nessun cambiamento)', () => {
  const dir = makeDraft()
  publish(dir)
  const { changed } = publish(dir)
  assert.equal(changed, false)
  assert.equal(readStatus(dir), 'published')
})

test('unpublish è l\'inverso di publish', () => {
  const dir = makeDraft()
  publish(dir)
  const { changed } = unpublish(dir)
  assert.equal(changed, true)
  assert.equal(readStatus(dir), 'draft')
})

test('publish solleva errore su stato non riconosciuto', () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'finalize-landing-'))
  fs.writeFileSync(path.join(dir, 'index.ts'), 'export const x = { status: 42 }\n')
  assert.throws(() => publish(dir), /stato non riconosciuto/)
})

test('setScreenshotSrc cabla il percorso al posto di src: null', () => {
  const dir = makeDraft()
  const src = screenshotAssetPath('note', 'en')
  assert.equal(setScreenshotSrc(dir, 'en', src), true)
  const text = fs.readFileSync(path.join(dir, 'en.ts'), 'utf8')
  assert.ok(text.includes(`src: '${src}'`))
  // Idempotente: seconda volta non c'è più `src: null` da sostituire.
  assert.equal(setScreenshotSrc(dir, 'en', src), false)
})

test('setOgImage cabla il percorso al posto di ogImage: null', () => {
  const dir = makeDraft()
  const og = ogImageAssetPath('note')
  assert.equal(setOgImage(dir, 'en', og), true)
  const text = fs.readFileSync(path.join(dir, 'en.ts'), 'utf8')
  assert.ok(text.includes(`ogImage: '${og}'`))
  assert.equal(setOgImage(dir, 'en', og), false)
})

test('i percorsi di convenzione sono sotto /landings/<appId>/', () => {
  assert.equal(screenshotAssetPath('note', 'it'), '/landings/note/hero.it.png')
  assert.equal(ogImageAssetPath('note'), '/landings/note/og.png')
})
