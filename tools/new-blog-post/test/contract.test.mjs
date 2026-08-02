// ALLARME DI DERIVA fra il contratto del blog (UC 0042) e ciò che il generatore scrive.
//
// È il presidio che lo use case 0084 chiede esplicitamente fra i suoi punti aperti: il
// generatore CONSUMA il registro, i tipi e la validazione di UC 0042, e se quel contratto
// evolve senza che lo scaffolding sia riallineato non succede nulla di visibile — si
// continuano a produrre post di forma vecchia, che compilano finché il campo nuovo è
// facoltativo. Qui si legge la fonte vera e la si confronta con i campi resi: contratto
// cambiato senza riallineamento = suite rossa.
//
// Come leggere un rosso: NON allargare gli elenchi qui sotto per far tornare verde.
// La riparazione giusta è aggiornare lib/render.mjs e lib/spec.mjs perché il generatore
// scriva davvero il campo nuovo, e questi elenchi lo seguono.
import { test } from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { RENDERED_LOCALE_FIELDS, RENDERED_POST_FIELDS } from '../lib/render.mjs'
import { LOCALES, RESERVED_BLOG_SLUGS } from '../lib/spec.mjs'

const REPO_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..')
const TYPES = path.join(REPO_ROOT, 'site/src/content/blog/types.ts')
const BLOG_LIB = path.join(REPO_ROOT, 'site/src/lib/blog.ts')
const I18N = path.join(REPO_ROOT, 'site/src/lib/i18n.ts')

/** Campi dichiarati da un'interfaccia TypeScript (righe `  nome?: tipo`). */
function fieldsOf(source, interfaceName) {
  const start = source.indexOf(`export interface ${interfaceName} {`)
  assert.ok(start >= 0, `interfaccia ${interfaceName} non trovata in ${TYPES}`)
  const end = source.indexOf('\n}', start)
  const body = source.slice(start, end)
  return [...body.matchAll(/^ {2}(\w+)\??:/gm)].map((m) => m[1])
}

test('i campi di BlogPost sono esattamente quelli che il generatore sa scrivere', () => {
  const declared = fieldsOf(fs.readFileSync(TYPES, 'utf8'), 'BlogPost')
  assert.deepEqual(
    [...declared].sort(),
    [...RENDERED_POST_FIELDS].sort(),
    'il contratto dei post di UC 0042 è cambiato: riallinea lib/render.mjs e lib/spec.mjs',
  )
})

test('i campi di PostLocaleContent sono esattamente quelli che il generatore sa scrivere', () => {
  const declared = fieldsOf(fs.readFileSync(TYPES, 'utf8'), 'PostLocaleContent')
  assert.deepEqual(
    [...declared].sort(),
    [...RENDERED_LOCALE_FIELDS].sort(),
    'il contratto del contenuto per-lingua di UC 0042 è cambiato: riallinea lib/render.mjs e lib/spec.mjs',
  )
})

test('i tipi di post ammessi sono ancora pilastro e articolo', () => {
  const source = fs.readFileSync(TYPES, 'utf8')
  assert.match(source, /export type PostKind = 'pillar' \| 'article'/)
})

test('le 5 lingue del generatore sono quelle del sito', () => {
  const m = fs.readFileSync(I18N, 'utf8').match(/export const LOCALES = \[([^\]]*)\]/)
  assert.ok(m, 'elenco delle lingue non trovato in site/src/lib/i18n.ts')
  const declared = [...m[1].matchAll(/'([a-z-]+)'/g)].map((x) => x[1])
  assert.deepEqual(declared, LOCALES)
})

test('gli slug riservati del generatore sono quelli della validazione del sito', () => {
  const m = fs.readFileSync(BLOG_LIB, 'utf8').match(/RESERVED_BLOG_SLUGS: readonly string\[\] = \[([^\]]*)\]/)
  assert.ok(m, 'elenco degli slug riservati non trovato in site/src/lib/blog.ts')
  const declared = [...m[1].matchAll(/'([a-z0-9-]+)'/g)].map((x) => x[1])
  assert.deepEqual(declared, RESERVED_BLOG_SLUGS)
})

test('il registro reale del blog è leggibile dal generatore', async () => {
  const { readAllPosts, publishedAppIds } = await import('../lib/registry.mjs')
  const posts = readAllPosts(REPO_ROOT)
  assert.ok(posts.length > 0, 'nessun post letto dal registro reale')
  for (const p of posts) {
    assert.ok(['pillar', 'article'].includes(p.kind), `${p.key}: tipo non riconosciuto`)
    assert.ok(p.appId, `${p.key}: appId non letto`)
    for (const loc of LOCALES) assert.ok(p.slugs[loc], `${p.key}: slug ${loc} non letto`)
  }
  assert.ok(publishedAppIds(REPO_ROOT).length > 0, 'nessuna landing pubblicata riconosciuta')
})
