// Modifiche meccaniche al registro e loro INVERSI (UC 0084).
//
// Il punto di questi test è la simmetria: ogni operazione, seguita dal suo inverso, deve
// riportare il testo identico. È la garanzia su cui poggiano sia il ripristino dopo un
// build rosso, sia il collaudo andata-ritorno.
import { test } from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import {
  addClusterKey,
  addToRegistry,
  publishedAppIds,
  readAllPosts,
  readPost,
  removeClusterKey,
  removeFromRegistry,
} from '../lib/registry.mjs'
import { makeRepo } from './fixture.mjs'

const REGISTRY = `// Registro.
import type { BlogPost } from './types.ts'
import { primo } from './primo/index.ts'

export const BLOG_POSTS: readonly BlogPost[] = [
  primo,
]

export type { BlogPost } from './types.ts'
`

test("l aggiunta al registro mette importazione e voce, e la rimozione le toglie", () => {
  const added = addToRegistry(REGISTRY, 'nuovo-post')
  assert.match(added, /import \{ nuovoPost \} from '\.\/nuovo-post\/index\.ts'/)
  assert.match(added, /\n  nuovoPost,\n\]/)
  assert.equal(removeFromRegistry(added, 'nuovo-post'), REGISTRY)
})

test("l importazione del nuovo post va dopo l ultima importazione di post", () => {
  const added = addToRegistry(REGISTRY, 'nuovo-post')
  const lines = added.split('\n')
  assert.equal(lines[2], "import { primo } from './primo/index.ts'")
  assert.equal(lines[3], "import { nuovoPost } from './nuovo-post/index.ts'")
})

test('aggiungere due post di seguito mantiene ordine e simmetria', () => {
  const a = addToRegistry(REGISTRY, 'uno')
  const b = addToRegistry(a, 'due')
  assert.ok(b.indexOf('  uno,') < b.indexOf('  due,'))
  assert.equal(removeFromRegistry(removeFromRegistry(b, 'due'), 'uno'), REGISTRY)
})

test('aggiungere due volte lo stesso post è un errore, non un doppione silenzioso', () => {
  const a = addToRegistry(REGISTRY, 'uno')
  assert.throws(() => addToRegistry(a, 'uno'), /importa già/)
})

test('togliere un post assente è un errore', () => {
  assert.throws(() => removeFromRegistry(REGISTRY, 'assente'), /non importa/)
})

test('il pilastro senza lista di cluster la riceve, e la perde quando si svuota', () => {
  const pillar = `export const p: BlogPost = {
  key: 'p',
  kind: 'pillar',
  datePublished: '2026-01-01',
  appId: 'fatture',
  content: { en },
}
`
  const withCluster = addClusterKey(pillar, 'figlio')
  assert.match(withCluster, /\n  clusterKeys: \['figlio'\],\n/)
  assert.ok(withCluster.indexOf('clusterKeys') > withCluster.indexOf('appId'))
  assert.equal(removeClusterKey(withCluster, 'figlio'), pillar)
})

test('il pilastro con lista esistente la vede crescere e tornare come prima', () => {
  const pillar = `export const p: BlogPost = {
  key: 'p',
  kind: 'pillar',
  appId: 'fatture',
  clusterKeys: ['a', 'b'],
  content: { en },
}
`
  const grown = addClusterKey(pillar, 'c')
  assert.match(grown, /clusterKeys: \['a', 'b', 'c'\]/)
  assert.equal(removeClusterKey(grown, 'c'), pillar)
})

test('agganciare due volte lo stesso cluster non duplica', () => {
  const pillar = `  appId: 'fatture',\n  clusterKeys: ['a'],\n`
  assert.equal(addClusterKey(addClusterKey(pillar, 'a'), 'a'), pillar)
})

test('la lettura del registro reale restituisce identità, slug e cluster', () => {
  const root = makeRepo()
  const posts = readAllPosts(root)
  assert.equal(posts.length, 1)
  assert.equal(posts[0].key, 'tema-guida')
  assert.equal(posts[0].kind, 'pillar')
  assert.equal(posts[0].appId, 'fatture')
  assert.deepEqual(posts[0].clusterKeys, [])
  assert.equal(posts[0].slugs.en, 'guide-theme')
  assert.equal(posts[0].slugs.de, 'leitthema')
})

test('si riconoscono solo le app con landing PUBBLICATA', () => {
  const root = makeRepo()
  assert.deepEqual(publishedAppIds(root), ['fatture'])
})

test('la lettura di un post con lista di cluster la restituisce', () => {
  const root = makeRepo()
  const file = path.join(root, 'site/src/content/blog/tema-guida/index.ts')
  fs.writeFileSync(file, addClusterKey(fs.readFileSync(file, 'utf8'), 'figlio'))
  assert.deepEqual(readPost(root, 'tema-guida').clusterKeys, ['figlio'])
})
