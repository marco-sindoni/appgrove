#!/usr/bin/env node
// Controllo meccanico del registro di copertura end-to-end (UC 0093).
//
//   node tools/e2e-coverage/check.mjs [radice]
//
// Esce con codice 0 se il registro `docs/testing/copertura-e2e.yaml` rispecchia la realtà del
// repository, con codice 1 (e messaggi che dicono QUALE voce sistemare) altrimenti.
// Girato nell'area `tooling` di ./run-tests.sh.
//
// Cosa NON misura: la QUALITÀ dei test. Misura che la mappa sia vera. Vedi docs/testing/README.md.

import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { validate, REGISTRY_PATH } from './lib.mjs'

const qui = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(process.argv[2] ?? path.join(qui, '..', '..'))

const violazioni = validate(root)

if (violazioni.length === 0) {
  console.log(`✓ copertura e2e: ${REGISTRY_PATH} coerente con i test presenti nel repository.`)
  process.exit(0)
}

console.error(`✗ copertura e2e: ${violazioni.length} incoerenza/e fra ${REGISTRY_PATH} e il repository.\n`)
const perRegola = new Map()
for (const v of violazioni) {
  if (!perRegola.has(v.regola)) perRegola.set(v.regola, [])
  perRegola.get(v.regola).push(v.messaggio)
}
for (const [regola, messaggi] of perRegola) {
  console.error(`  [${regola}]`)
  for (const m of messaggi) console.error(`    • ${m}`)
  console.error('')
}
console.error('Come rimediare: docs/testing/README.md ("Come leggere un rosso").')
process.exit(1)
