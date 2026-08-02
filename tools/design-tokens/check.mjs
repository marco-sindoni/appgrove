#!/usr/bin/env node
// ─────────────────────────────────────────────────────────────────────────────
// tools/design-tokens/check.mjs — il controllo anti-drift sul repository VERO (UC 0086).
//
// Verde = ogni colore scritto per esteso nei consumatori del brand kit corrisponde a un
// token reale (o a un'eccezione dichiarata con motivo). Rosso = qualcuno ha ricopiato o
// inventato un colore, ed è esattamente il difetto che nessun altro test coglie.
//
// Come leggere un rosso: NON aggiungere il colore alle eccezioni per far tornare verde.
// Le due riparazioni giuste sono (1) usare il token che esiste già, oppure (2) se il
// colore serve davvero al brand, aggiungerlo a tokens.css — cioè alla fonte unica.
// ─────────────────────────────────────────────────────────────────────────────
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'
import { allBrandHexes } from '../../frontend/packages/design-system/src/tokens/tokens.mjs'
import { scan, ALLOWED, EXCLUDED_FILES, WATCHED } from './lib.mjs'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..')

const brandHexes = allBrandHexes()
const { findings, scanned, missingRoots } = scan({ root: ROOT, brandHexes })

console.log(`design-tokens — anti-drift del brand kit (UC 0086)`)
console.log(`  colori del brand riconosciuti: ${brandHexes.size} (da tokens.css, tutti i temi e gli accenti)`)
console.log(`  consumatori sorvegliati: ${WATCHED.length} · file scandagliati: ${scanned}`)
console.log(`  eccezioni dichiarate: ${ALLOWED.length} colori, ${EXCLUDED_FILES.length} file`)

if (missingRoots.length) {
  console.error(`\n✗ radici sorvegliate inesistenti (WATCHED da aggiornare): ${missingRoots.join(', ')}`)
  process.exit(1)
}

if (findings.length === 0) {
  console.log(`\n✓ nessuna divergenza: i consumatori dipingono solo con i colori del brand.`)
  process.exit(0)
}

console.error(`\n✗ ${findings.length} color${findings.length === 1 ? 'e' : 'i'} fuori dal brand kit:\n`)
for (const f of findings) {
  console.error(`  ${f.file}:${f.line} → ${f.raw} (normalizzato ${f.color})`)
}
console.error(
  `\nRiparazione: usa il token che già esiste (frontend/packages/design-system/src/tokens/tokens.css),` +
    `\noppure aggiungi il colore ALLA FONTE UNICA se appartiene davvero al brand.` +
    `\nAggiungerlo alle eccezioni di tools/design-tokens/lib.mjs è l'ultima risorsa, e vuole un motivo scritto.`,
)
process.exit(1)
