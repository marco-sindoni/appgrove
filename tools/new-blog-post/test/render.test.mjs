// Resa dei file TypeScript del post (UC 0084).
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { exportNameFor, renderLocaleFile, renderPostIndex, tsString } from '../lib/render.mjs'
import { articleSpec, pillarSpec } from './fixture.mjs'

test('il nome dell export segue la chiave in forma cammello', () => {
  assert.equal(exportNameFor('fatturazione-pmi-ue'), 'fatturazionePmiUe')
  assert.equal(exportNameFor('semplice'), 'semplice')
  assert.equal(exportNameFor('con-2-numeri'), 'con2Numeri')
})

test('i letterali di stringa usano le virgolette singole, doppie se c è un apostrofo', () => {
  assert.equal(tsString('senza apostrofi'), "'senza apostrofi'")
  assert.equal(tsString("l'articolo"), '"l\'articolo"')
  assert.equal(tsString(`l'articolo "vero"`), "'l\\'articolo \"vero\"'")
})

test('il file di una lingua dichiara il tipo e tutti i campi del contenuto', () => {
  const out = renderLocaleFile(articleSpec(), 'it')
  assert.match(out, /import type \{ PostLocaleContent \} from '\.\.\/types\.ts'/)
  assert.match(out, /export const it: PostLocaleContent = \{/)
  for (const field of ['slug', 'title', 'description', 'question', 'intro', 'sections', 'faq', 'ctaText']) {
    assert.match(out, new RegExp(`\\n  ${field}:`), `manca il campo ${field}`)
  }
  assert.ok(out.endsWith('}\n'))
})

test("il file di identità di un articolo dichiara il pilastro e non i cluster", () => {
  const out = renderPostIndex(articleSpec())
  assert.match(out, /export const articoloProva: BlogPost = \{/)
  assert.match(out, /kind: 'article',/)
  assert.match(out, /pillarKey: 'tema-guida',/)
  assert.ok(!/\n {2}clusterKeys:/.test(out), 'i riferimenti reciproci li aggancia il registro, non la resa')
  assert.match(out, /content: \{ en, it, fr, es, de \},/)
})

test('il file di identità di un pilastro non dichiara né pilastro né cluster', () => {
  const out = renderPostIndex(pillarSpec())
  assert.match(out, /kind: 'pillar',/)
  assert.ok(!/\n {2}pillarKey:/.test(out))
  assert.ok(!/\n {2}clusterKeys:/.test(out))
})

test('la copy con apostrofi resta leggibile e valida', () => {
  const p = articleSpec()
  p.content.it.title = "L'articolo dell'anno"
  const out = renderLocaleFile(p, 'it')
  assert.match(out, /title: "L'articolo dell'anno",/)
})
