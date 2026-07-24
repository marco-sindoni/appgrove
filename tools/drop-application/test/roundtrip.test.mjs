// Round-trip end-to-end: generare un'app e poi de-generarla deve riportare il
// repository allo stato di partenza, byte per byte sui file condivisi e senza
// lasciare alberi/file dell'app. È il test più forte di simmetria fra generatore
// e de-generatore: se new-application aggiunge un file/una modifica che il
// de-generatore non toglie, questo test se ne accorge.
import { test } from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { execFileSync } from 'node:child_process'
import { EDITED_FILES } from '../../new-application/lib/edits.mjs'
import { removedTrees, removedFiles } from '../lib/plan.mjs'

const REPO_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..')
const APP = 'dropit'

const abs = (rel) => path.join(REPO_ROOT, rel)
const node = (script, args) =>
  execFileSync('node', [script, ...args], { cwd: REPO_ROOT, encoding: 'utf8' })

test('genera → de-genera: il repository torna identico', () => {
  const shared = EDITED_FILES.map((rel) => ({ rel, before: fs.readFileSync(abs(rel), 'utf8') }))
  const trees = removedTrees(APP)
  const files = removedFiles(APP)

  // Precondizione: l'app non esiste ancora.
  for (const rel of [...trees, ...files]) {
    assert.ok(!fs.existsSync(abs(rel)), `precondizione: ${rel} non deve esistere prima del test`)
  }

  try {
    // 1. genera (senza toccare l'infrastruttura)
    node('tools/new-application/generate.mjs', ['--app-id', APP, '--metric', 'things', '--skip-infra'])
    for (const rel of [...trees, ...files]) {
      assert.ok(fs.existsSync(abs(rel)), `dopo generate: ${rel} deve esistere`)
    }
    for (const { rel } of shared) {
      assert.ok(fs.readFileSync(abs(rel), 'utf8').includes(APP), `dopo generate: ${rel} deve citare ${APP}`)
    }

    // 2. de-genera (senza rigenerare la RoPA: la verifica è sui file condivisi)
    node('tools/drop-application/remove.mjs', ['--app-id', APP, '--skip-ropa'])

    // 3. alberi e file dell'app spariti
    for (const rel of [...trees, ...files]) {
      assert.ok(!fs.existsSync(abs(rel)), `dopo remove: ${rel} deve essere rimosso`)
    }
    // 4. file condivisi identici all'originale, byte per byte
    for (const { rel, before } of shared) {
      assert.equal(fs.readFileSync(abs(rel), 'utf8'), before, `dopo remove: ${rel} deve tornare identico`)
    }
  } finally {
    // Ripristino incondizionato: il test non deve lasciare il repo sporco, nemmeno se fallisce.
    for (const { rel, before } of shared) fs.writeFileSync(abs(rel), before)
    for (const rel of files) fs.rmSync(abs(rel), { force: true })
    for (const rel of trees) fs.rmSync(abs(rel), { recursive: true, force: true })
  }
})

test('remove su app inesistente esce con errore', () => {
  assert.throws(() =>
    node('tools/drop-application/remove.mjs', ['--app-id', 'nonesiste_xyz', '--skip-ropa']),
  )
})

test('remove --dry-run non tocca nulla', () => {
  const shared = EDITED_FILES.map((rel) => ({ rel, before: fs.readFileSync(abs(rel), 'utf8') }))
  try {
    node('tools/new-application/generate.mjs', ['--app-id', APP, '--metric', 'things', '--skip-infra'])
    const out = node('tools/drop-application/remove.mjs', ['--app-id', APP, '--dry-run'])
    assert.ok(out.includes('dry-run'))
    // l'albero del servizio deve esistere ancora dopo il dry-run
    assert.ok(fs.existsSync(abs(`services/${APP}`)), 'dry-run non deve rimuovere nulla')
  } finally {
    for (const { rel, before } of shared) fs.writeFileSync(abs(rel), before)
    for (const rel of removedFiles(APP)) fs.rmSync(abs(rel), { force: true })
    for (const rel of removedTrees(APP)) fs.rmSync(abs(rel), { recursive: true, force: true })
  }
})
