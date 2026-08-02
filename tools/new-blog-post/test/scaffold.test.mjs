// Generazione completa su una copia di prova del sito (UC 0084).
import { test } from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { LOCALES } from '../lib/spec.mjs'
import { outline, remove, scaffold, validate } from '../lib/scaffold.mjs'
import { articleSpec, makeRepo, pillarSpec, snapshot } from './fixture.mjs'

const blog = (root, ...p) => path.join(root, 'site/src/content/blog', ...p)

/** Valida e genera, fallendo il test se la validazione rifiuta. */
function generate(root, raw) {
  const { posts, errors } = validate(root, raw)
  assert.deepEqual(errors, [], `validazione inattesa: ${errors.join(' | ')}`)
  return scaffold(root, posts)
}

test('un articolo cluster nasce completo e agganciato al suo pilastro', () => {
  const root = makeRepo()
  generate(root, articleSpec())

  for (const loc of [...LOCALES, 'index']) {
    assert.ok(fs.existsSync(blog(root, 'articolo-prova', `${loc}.ts`)), `manca ${loc}.ts`)
  }
  const registry = fs.readFileSync(blog(root, 'index.ts'), 'utf8')
  assert.match(registry, /import \{ articoloProva \} from '\.\/articolo-prova\/index\.ts'/)
  assert.match(registry, /\n  articoloProva,\n\]/)

  const article = fs.readFileSync(blog(root, 'articolo-prova/index.ts'), 'utf8')
  assert.match(article, /pillarKey: 'tema-guida',/)

  const pillar = fs.readFileSync(blog(root, 'tema-guida/index.ts'), 'utf8')
  assert.match(pillar, /clusterKeys: \['articolo-prova'\],/)
})

test('pilastro assente: pilastro e primo articolo nascono coerenti nella stessa esecuzione', () => {
  const root = makeRepo()
  generate(root, {
    posts: [pillarSpec({ key: 'tema-nuovo' }), articleSpec({ key: 'primo-cluster', pillarKey: 'tema-nuovo' })],
  })

  const pillar = fs.readFileSync(blog(root, 'tema-nuovo/index.ts'), 'utf8')
  assert.match(pillar, /kind: 'pillar',/)
  assert.match(pillar, /clusterKeys: \['primo-cluster'\],/)
  const article = fs.readFileSync(blog(root, 'primo-cluster/index.ts'), 'utf8')
  assert.match(article, /pillarKey: 'tema-nuovo',/)

  const view = outline(root)
  const created = view.pillars.find((p) => p.key === 'tema-nuovo')
  assert.deepEqual(
    created.clusters.map((c) => c.key),
    ['primo-cluster'],
  )
})

test('andata e ritorno: generare e poi rimuovere riporta i file identici', () => {
  const root = makeRepo()
  const before = snapshot(path.join(root, 'site'))
  generate(root, articleSpec())
  remove(root, ['articolo-prova'])
  const after = snapshot(path.join(root, 'site'))

  assert.deepEqual([...after.keys()].sort(), [...before.keys()].sort())
  for (const [file, content] of before) assert.equal(after.get(file), content, `${file} è cambiato`)
  assert.ok(!fs.existsSync(blog(root, 'articolo-prova')))
})

test('chiave già usata: rifiuto, e il repository resta identico', () => {
  const root = makeRepo()
  const before = snapshot(path.join(root, 'site'))
  const { errors } = validate(root, pillarSpec({ key: 'tema-guida' }))
  assert.ok(errors.some((e) => e.includes('esiste già un post con questa chiave')))
  const after = snapshot(path.join(root, 'site'))
  for (const [file, content] of before) assert.equal(after.get(file), content)
})

test('slug già usato da un altro post nella stessa lingua: rifiuto', () => {
  const root = makeRepo()
  const p = articleSpec()
  p.content.it.slug = 'tema-guida' // lo slug italiano del pilastro esistente
  const { errors } = validate(root, p)
  assert.ok(errors.some((e) => e.includes('già usato dal post "tema-guida"')))
})

test('app senza landing pubblicata: rifiuto con l elenco delle app ammesse', () => {
  const root = makeRepo()
  const { errors } = validate(root, articleSpec({ appId: 'bozza' }))
  assert.ok(errors.some((e) => e.includes('non ha una landing pubblicata') && e.includes('fatture')))
})

test('pilastro inesistente: rifiuto che suggerisce come rimediare', () => {
  const root = makeRepo()
  const { errors } = validate(root, articleSpec({ pillarKey: 'inesistente' }))
  assert.ok(errors.some((e) => e.includes('non esiste') && e.includes('stessa specifica')))
})

test("un articolo che punta a un altro articolo invece che a un pilastro: rifiuto", () => {
  const root = makeRepo()
  generate(root, articleSpec())
  const { errors } = validate(root, articleSpec({ key: 'secondo', pillarKey: 'articolo-prova' }))
  assert.ok(errors.some((e) => e.includes('non è un pilastro')))
})

test('rieseguire la stessa generazione: rifiuto, registro intatto', () => {
  const root = makeRepo()
  generate(root, articleSpec())
  const afterFirst = snapshot(path.join(root, 'site'))
  const { errors } = validate(root, articleSpec())
  assert.ok(errors.length > 0)
  const afterSecond = snapshot(path.join(root, 'site'))
  for (const [file, content] of afterFirst) assert.equal(afterSecond.get(file), content)
})

test('una scrittura che fallisce a metà non lascia il repository a metà', () => {
  const root = makeRepo()
  const before = snapshot(path.join(root, 'site'))
  // Il pilastro dichiarato esiste per la validazione, ma il suo file di identità viene
  // reso illeggibile subito prima della scrittura: la transazione deve tornare indietro.
  const { posts } = validate(root, articleSpec())
  fs.rmSync(blog(root, 'tema-guida/index.ts'))
  assert.throws(() => scaffold(root, posts))
  fs.writeFileSync(blog(root, 'tema-guida/index.ts'), before.get(blog(root, 'tema-guida/index.ts')))

  const after = snapshot(path.join(root, 'site'))
  assert.deepEqual([...after.keys()].sort(), [...before.keys()].sort())
  for (const [file, content] of before) assert.equal(after.get(file), content, `${file} è cambiato`)
})

test('la vista del registro elenca i pilastri con i loro cluster', () => {
  const root = makeRepo()
  generate(root, articleSpec())
  const view = outline(root)
  assert.equal(view.total, 2)
  assert.equal(view.pillars.length, 1)
  assert.deepEqual(
    view.pillars[0].clusters.map((c) => c.key),
    ['articolo-prova'],
  )
  assert.deepEqual(view.orphans, [])
})
