// Test della risoluzione del seed screenshot per-app (UC 0057 — fix seed generico).
//
// Il comando `screenshots` deriva le rotte mock dal seed risolto per appId: un'app con
// descrittore dedicato (seeds/<appId>.mjs) usa la sua risorsa di lista reale; un'app
// senza descrittore ricade sul default generico (risorsa `items`). Questi test coprono
// esattamente quel bivio — la parte deterministica del fix, indipendente dal browser.
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { resolveSeed, normalizeSeed, DEFAULT_SEED } from '../lib/seeds.mjs'

test("resolveSeed('fatture') usa il descrittore reale (risorsa invoices)", async () => {
  const seed = await resolveSeed('fatture')
  assert.equal(seed.listPath, 'invoices')
  assert.equal(seed.metric, 'fatture')
  assert.equal(seed.freeCap, 10)
  assert.ok(seed.records.length > 0, 'ha record d\'esempio')
  // I record hanno la forma della lista fatture reale (number/customerName/status/totale).
  const r = seed.records[0]
  for (const k of ['number', 'customerName', 'issueDate', 'status', 'currency', 'totalAmount']) {
    assert.ok(k in r, `il record espone "${k}"`)
  }
})

test('un appId senza descrittore ricade sul default generico (risorsa items)', async () => {
  const seed = await resolveSeed('app-inesistente-xyz')
  assert.equal(seed.listPath, DEFAULT_SEED.listPath)
  assert.equal(seed.listPath, 'items')
  assert.equal(seed.metric, DEFAULT_SEED.metric)
  assert.ok(seed.records.length > 0)
})

test('normalizeSeed riempie i campi mancanti coi default', () => {
  const seed = normalizeSeed({ listPath: 'invoices' })
  assert.equal(seed.listPath, 'invoices')
  assert.equal(seed.metric, DEFAULT_SEED.metric) // non fornito → default
  assert.equal(seed.freeCap, DEFAULT_SEED.freeCap)
  assert.deepEqual(seed.records, DEFAULT_SEED.records)
  // normalizeSeed(undefined) è il default puro (nessun crash).
  assert.deepEqual(normalizeSeed(undefined), normalizeSeed(DEFAULT_SEED))
})
