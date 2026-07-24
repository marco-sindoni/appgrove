#!/usr/bin/env node
// ─────────────────────────────────────────────────────────────────────────────
// tools/drop-application/remove.mjs — la META' MECCANICA della skill
// `drop-application` (UC 0048): l'inverso di tools/new-application/generate.mjs.
//
// Disfa ciò che il generatore ha creato per un'app:
//   • rimuove i DUE alberi dedicati (servizio Quarkus + modulo frontend);
//   • rimuove i TRE file dell'app nelle cartelle condivise (test end-to-end,
//     manifesto dati, listino);
//   • toglie le CINQUE modifiche ai file condivisi (invertendo lib/edits.mjs del
//     generatore — vedi lib/unedits.mjs);
//   • rigenera la RoPA, così il registro dei trattamenti non resta in drift dopo
//     la rimozione del manifesto.
//
// Cosa NON fa, di proposito (change 0043, decisione 4 — la skill non esegue atti
// irreversibili o verso l'esterno):
//   • NON invoca infra/scripts/service-remove: la rimozione dell'infrastruttura
//     tocca gli ambienti test E prod e ha un guardrail di conferma digitata
//     (#06 K) che è deliberato e va eseguito con la persona presente. Lo si
//     stampa nel runbook come primo passo, non lo si automatizza (distruggere
//     non è creare: la simmetria con generate.mjs, che invoca service-add, si
//     ferma qui apposta);
//   • NON lancia la purga dati (comando core `offboard-app`), il destroy
//     Terraform, il DROP SCHEMA né tocca il fornitore di pagamento: tutti passi
//     del runbook, dopo il merge.
//
// È deterministico e collaudato (area `tooling` di run-tests.sh): la skill
// conversazionale raccoglie le decisioni e poi chiama questo comando.
// ─────────────────────────────────────────────────────────────────────────────
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { execFileSync } from 'node:child_process'
import { APP_ID_PATTERN, RESERVED_APP_IDS, toCamelCase } from '../new-application/lib/context.mjs'
import { UNEDITORS, UNEDITED_FILES } from './lib/unedits.mjs'
import { removedTrees, removedFiles } from './lib/plan.mjs'

const TOOL_DIR = path.dirname(fileURLToPath(import.meta.url))
const REPO_ROOT = path.resolve(TOOL_DIR, '../..')

function usage() {
  return `Uso: remove.mjs --app-id <id> [opzioni]

Disfa lo scaffolding di un'app (l'inverso di new-application). Rimuove il servizio,
il modulo frontend, il test end-to-end, il manifesto dati e il listino dell'app, e
toglie le cinque modifiche ai file condivisi. Rigenera la RoPA.

Obbligatori
  --app-id <id>   identificativo dell'app da dismettere (${APP_ID_PATTERN})

Opzioni
  --dry-run       mostra il piano senza toccare nulla
  --skip-ropa     non rigenera la RoPA (utile nei test / offline)
  --help          questo testo

NON fa (restano passi del runbook, dopo il merge — decisione 4 della change 0043):
  · infra:   ./infra/scripts/service-remove <id>   (+ terraform destroy -target, safety #06 K)
  · dati:    core offboard-app <id>                 (purga per tutti i tenant, con audit)
  · DB:      DROP SCHEMA app_<id> CASCADE; DROP ROLE app_<id>; + segreto Secrets Manager
`
}

function die(message) {
  process.stderr.write(`\n✗ ${message}\n\n`)
  process.exit(1)
}

function parseArgs(argv) {
  const opts = { dryRun: false, skipRopa: false }
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    switch (arg) {
      case '--app-id': {
        const value = argv[i + 1]
        if (value === undefined || value.startsWith('--')) die('--app-id richiede un valore')
        opts.appId = value
        i += 1
        break
      }
      case '--dry-run': opts.dryRun = true; break
      case '--skip-ropa': opts.skipRopa = true; break
      case '--help':
      case '-h': process.stdout.write(usage()); process.exit(0); break
      default: die(`opzione sconosciuta: ${arg}\n\n${usage()}`)
    }
  }
  return opts
}

function validate(opts) {
  if (!opts.appId) die(`manca --app-id\n\n${usage()}`)
  if (!APP_ID_PATTERN.test(opts.appId)) {
    die(`identificativo non valido: "${opts.appId}" (minuscole/cifre/underscore, iniziale alfabetica, max 31)`)
  }
  if (RESERVED_APP_IDS.has(opts.appId)) {
    die(`"${opts.appId}" è riservato (servizio di piattaforma o convenzione del monorepo): non si dismette`)
  }
}

/**
 * Costruisce il piano leggendo lo stato reale del repository: quali alberi/file
 * esistono e, per ogni file condiviso, se porta ancora il segno dell'app.
 */
function buildPlan(ctx) {
  const trees = removedTrees(ctx.APP_ID)
    .filter((rel) => fs.existsSync(path.join(REPO_ROOT, rel)))
  const files = removedFiles(ctx.APP_ID)
    .filter((rel) => fs.existsSync(path.join(REPO_ROOT, rel)))

  const edits = []
  for (const rel of UNEDITED_FILES) {
    const full = path.join(REPO_ROOT, rel)
    if (!fs.existsSync(full)) continue
    const before = fs.readFileSync(full, 'utf8')
    const { content, changed } = UNEDITORS[rel](before, ctx)
    if (changed) edits.push({ path: rel, content })
  }

  return { ctx, trees, files, edits }
}

function printPlan(plan) {
  const out = ['']
  out.push(`  App da dismettere   ${plan.ctx.APP_ID}`)
  out.push('')
  out.push(`  RIMUOVE alberi (${plan.trees.length})`)
  for (const t of plan.trees) out.push(`    - ${t}/`)
  out.push(`  RIMUOVE file (${plan.files.length})`)
  for (const f of plan.files) out.push(`    - ${f}`)
  out.push(`  DISFA modifiche condivise (${plan.edits.length})`)
  for (const e of plan.edits) out.push(`    ~ ${e.path}`)
  out.push('')
  process.stdout.write(out.join('\n'))
}

function printRunbook(appId) {
  process.stdout.write(
    `
  PASSI SUCCESSIVI — restano manuali, dopo il merge (decisione 4 della change 0043)

  1. Abbonati        già gestiti PRIMA di arrivare qui (disdetta a fine periodo /
                     migrazione / comunicazione): nessuna distruzione senza questo.
  2. Dati (purga)    core offboard-app ${appId}
                     accoda la purga per ogni tenant, con audit. Garantire prima l'export.
  3. Infra           ./infra/scripts/service-remove ${appId}
                     poi, mirato e con safety #06 K:
                     terraform -chdir=infra/envs/<env> destroy -target='module.app_${appId}'
  4. DB (fisico)     pg_dump --schema=app_${appId}  (archivio, se serve)
                     DROP SCHEMA app_${appId} CASCADE;  DROP ROLE app_${appId};
                     + elimina il segreto appgrove/<env>/${appId}/db (Secrets Manager)
  5. Listino         l'app è già tolta da pricing/index.yaml: al prossimo sync i price
                     vengono archiviati (soft-delete) se privi di subscription attive.

`,
  )
}

function main() {
  const opts = parseArgs(process.argv.slice(2))
  validate(opts)

  const ctx = { APP_ID: opts.appId, APP_CAMEL: toCamelCase(opts.appId) }
  const plan = buildPlan(ctx)

  if (plan.trees.length === 0 && plan.files.length === 0 && plan.edits.length === 0) {
    die(
      `l'app "${ctx.APP_ID}" non risulta presente: nessun albero, file o modifica condivisa da rimuovere.\n`
      + '  O l\'identificativo è errato, o l\'app è già stata dismessa.',
    )
  }

  printPlan(plan)

  if (opts.dryRun) {
    process.stdout.write('  (--dry-run: non è stato scritto nulla)\n\n')
    return
  }

  // Prima le modifiche condivise (reversibili, testo), poi le rimozioni fisiche.
  for (const edit of plan.edits) {
    fs.writeFileSync(path.join(REPO_ROOT, edit.path), edit.content)
  }
  for (const rel of plan.files) {
    fs.rmSync(path.join(REPO_ROOT, rel), { force: true })
  }
  for (const rel of plan.trees) {
    fs.rmSync(path.join(REPO_ROOT, rel), { recursive: true, force: true })
  }
  process.stdout.write(
    `  ✓ rimossi ${plan.trees.length} alberi, ${plan.files.length} file; disfatte ${plan.edits.length} modifiche condivise\n`,
  )

  // RoPA: senza il manifesto rimosso, i registri vanno rigenerati o il check di
  // freschezza andrebbe rosso. Reversibile e deterministico → di default.
  if (!opts.skipRopa) {
    try {
      execFileSync('npm', ['run', 'assemble'], {
        cwd: path.join(REPO_ROOT, 'tools/compliance'),
        stdio: 'inherit',
      })
      process.stdout.write('  ✓ RoPA rigenerata (tools/compliance)\n')
    } catch {
      process.stderr.write(
        '  ! rigenerazione RoPA fallita: lanciare a mano  ( cd tools/compliance && npm run assemble )\n',
      )
    }
  }

  printRunbook(ctx.APP_ID)
}

main()
