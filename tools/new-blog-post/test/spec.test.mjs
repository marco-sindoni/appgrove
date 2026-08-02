// Validazione della specifica: ogni motivo di rifiuto ha il suo caso (UC 0084).
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { normalizeSpec, validateSpecShape } from '../lib/spec.mjs'
import { articleSpec, localeContent, pillarSpec } from './fixture.mjs'

/** Ritorna true se almeno un errore contiene il frammento atteso. */
function has(errors, fragment) {
  return errors.some((e) => e.includes(fragment))
}

test('la specifica si accetta nelle tre forme comode (oggetto, array, { posts })', () => {
  const p = articleSpec()
  assert.equal(normalizeSpec(p).length, 1)
  assert.equal(normalizeSpec([p, p]).length, 2)
  assert.equal(normalizeSpec({ posts: [p] }).length, 1)
  assert.deepEqual(normalizeSpec(null), [])
})

test('un articolo ben formato non produce errori', () => {
  assert.deepEqual(validateSpecShape([articleSpec()]), [])
})

test('un pilastro ben formato non produce errori', () => {
  assert.deepEqual(validateSpecShape([pillarSpec()]), [])
})

test('specifica vuota: rifiutata', () => {
  assert.equal(validateSpecShape([]).length, 1)
})

test('lingua mancante: rifiutata', () => {
  const p = articleSpec()
  delete p.content.de
  assert.ok(has(validateSpecShape([p]), 'lingue mancanti — de'))
})

test('lingua non prevista: rifiutata', () => {
  const p = articleSpec()
  p.content.pt = localeContent('artigo')
  assert.ok(has(validateSpecShape([p]), 'lingue non previste — pt'))
})

test('stringa vuota nella copy: rifiutata', () => {
  const p = articleSpec()
  p.content.it.title = '   '
  assert.ok(has(validateSpecShape([p]), 'title mancante o vuoto'))
})

test('slug malformato: rifiutato', () => {
  const p = articleSpec()
  p.content.fr.slug = 'Slug Con Maiuscole'
  assert.ok(has(validateSpecShape([p]), 'non valido'))
})

test('slug riservato: rifiutato', () => {
  const p = articleSpec()
  p.content.en.slug = 'index'
  assert.ok(has(validateSpecShape([p]), 'riservato'))
})

test('due post della stessa specifica con lo stesso slug: rifiutati', () => {
  const a = articleSpec({ key: 'primo' })
  const b = articleSpec({ key: 'secondo' })
  b.content.en.slug = a.content.en.slug
  assert.ok(has(validateSpecShape([a, b]), 'usato due volte nella stessa specifica'))
})

test('chiave duplicata nella stessa specifica: rifiutata', () => {
  const a = articleSpec({ key: 'stesso' })
  assert.ok(has(validateSpecShape([a, articleSpec({ key: 'stesso' })]), 'key duplicata'))
})

test('data non nel formato AAAA-MM-GG: rifiutata', () => {
  assert.ok(has(validateSpecShape([articleSpec({ datePublished: '2/8/2026' })]), 'datePublished'))
})

test('articolo senza pilastro: rifiutato', () => {
  const p = articleSpec()
  delete p.pillarKey
  assert.ok(has(validateSpecShape([p]), 'senza pillarKey'))
})

test('pilastro con pillarKey: rifiutato', () => {
  assert.ok(has(validateSpecShape([pillarSpec({ pillarKey: 'altro' })]), 'un pilastro non ha pillarKey'))
})

test('clusterKeys dichiarati a mano: rifiutati (li aggancia il generatore)', () => {
  assert.ok(has(validateSpecShape([pillarSpec({ clusterKeys: ['x'] })]), 'clusterKeys non si dichiara'))
})

test('tipo di post sconosciuto: rifiutato', () => {
  assert.ok(has(validateSpecShape([articleSpec({ kind: 'guida' })]), 'kind deve essere'))
})

test('campo non previsto nel contenuto di una lingua: rifiutato', () => {
  const p = articleSpec()
  p.content.es.author = 'Chi scrive'
  assert.ok(has(validateSpecShape([p]), 'campi non previsti — author'))
})

test('traduzione con una sezione in meno della sorgente inglese: rifiutata', () => {
  const p = articleSpec()
  p.content.de.sections = [p.content.de.sections[0]]
  assert.ok(has(validateSpecShape([p]), 'forma diversa dalla sorgente en'))
})

test('traduzione con una voce FAQ in meno: rifiutata', () => {
  const p = articleSpec()
  p.content.fr.faq.items = [p.content.fr.faq.items[0]]
  assert.ok(has(validateSpecShape([p]), 'forma diversa dalla sorgente en'))
})

test('sezione senza paragrafi: rifiutata', () => {
  const p = articleSpec()
  p.content.en.sections[0].paragraphs = []
  assert.ok(has(validateSpecShape([p]), 'paragraphs deve essere una lista non vuota'))
})
