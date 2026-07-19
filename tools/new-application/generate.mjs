#!/usr/bin/env node
// ─────────────────────────────────────────────────────────────────────────────
// tools/new-application/generate.mjs — la META' MECCANICA della skill
// `new-application` (UC 0046).
//
// Istanzia i modelli-sorgente di `templates/` in una nuova app del marketplace e
// applica le poche modifiche necessarie ai file condivisi. Deterministico e
// collaudabile: la skill conversazionale (.claude/skills/new-application/)
// raccoglie le decisioni e poi chiama questo comando; qui dentro non si chiede
// nulla e non si decide nulla che non arrivi dalle opzioni.
//
// ── Perché Node e non bash ──────────────────────────────────────────────────
// Nel monorepo convivono entrambi (dev/ e tools/ci/ sono bash, tools/compliance
// è Node). Qui pesa Node per tre motivi concreti:
//   1. `frontend/apps/backoffice/package.json` va modificato: leggerlo e
//      riscriverlo come JSON è sicuro, incollarci dentro una riga con `sed` è il
//      modo più rapido per produrre un file non parsabile;
//   2. il lavoro è sostituzione di testo su ~45 file con contenuti che
//      contengono virgolette, backtick, `$`, graffe: in bash ogni passaggio è
//      un'occasione di espansione accidentale, in Node il testo è solo testo;
//   3. `--dry-run` richiede di costruire il piano completo PRIMA di scrivere
//      qualunque cosa: con strutture dati è naturale, in bash diventerebbe una
//      seconda implementazione parallela a quella che scrive davvero — cioè due
//      comportamenti che divergono al primo cambiamento.
// La scoperta dei servizi resta però in bash (`dev/lib/services.sh`): è già la
// sorgente unica e non va duplicata.
//
// ── Convenzione dei segnaposto ──────────────────────────────────────────────
// `@@NOME@@`, in maiuscolo, sia nel contenuto dei file sia nei nomi di file e
// cartelle (es. `@@APP_CLASS@@Main.java`). La sequenza `@@` non compare nella
// sintassi di Java, TypeScript, SQL, YAML, HOCON né XML: la ricerca
// `@@[A-Z0-9_]+@@` non ha né falsi positivi né falsi negativi.
//
// Conseguenza voluta: i file di `templates/` NON sono validi come YAML/JSON
// finché non vengono istanziati (in YAML `@` è un carattere riservato a inizio
// scalare). Gli editor lo segnalano: è rumore atteso su file che nessuno
// consuma come YAML — sono ingressi di una sostituzione testuale. Il guadagno è
// che un segnaposto sopravvissuto nell'OUTPUT rompe subito il parser proprio
// dove il silenzio sarebbe più pericoloso (manifesto dati, listino prezzi).
// In ogni caso, prima di dichiarare fatto, il generatore rilegge tutto ciò che
// ha scritto e ABORTISCE se trova un solo segnaposto residuo.
//
// ── Cosa NON fa (di proposito) ──────────────────────────────────────────────
//   - non tocca `app-start.sh`, `app-stop.sh`, `dev/Caddyfile`, `dev/lib/*.sh`,
//     `tools/smoke/*.sh`, `.github/workflows/*`: sono AUTO-SCOPRENTI, derivano
//     la mappa dei servizi da `dev/lib/services.sh` e `tools/ci/services.sh`. Se
//     un giorno uno di essi tornasse a richiedere una modifica a mano, la cosa
//     giusta è ripristinare la derivazione, non incollarci dentro una riga da
//     qui;
//   - non reimplementa il wiring Terraform: DELEGA a `infra/scripts/service-add`,
//     che è il proprietario del formato del blocco `module "app_<id>"`;
//   - non inventa il dominio dell'app: genera un modello segnaposto (`Item`)
//     che serve a far nascere l'app con la suite verde e gli invarianti già
//     dimostrati. Sostituirlo è il primo lavoro dopo lo scaffolding;
//   - non compila i testi del manifesto dati né i prezzi: quelli restano marcati
//     "DA COMPLETARE" perché li deve decidere una persona, con il co-pilota.
//
// ── Uso ─────────────────────────────────────────────────────────────────────
//   tools/new-application/generate.mjs --app-id note --metric notes \
//       [--port 8082] [--user-model single|multi] [--free-cap 10] \
//       [--app-name "Note"] [--icon sticky_note_2] [--accent cat-violet] \
//       [--dry-run] [--skip-infra]
// ─────────────────────────────────────────────────────────────────────────────
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { execFileSync } from 'node:child_process'
import {
  APP_ID_PATTERN,
  RESERVED_APP_IDS,
  buildContext,
  discoverServices,
  nextFreePort,
} from './lib/context.mjs'
import { EDITED_FILES, EDITORS } from './lib/edits.mjs'

const TOOL_DIR = path.dirname(fileURLToPath(import.meta.url))
const REPO_ROOT = path.resolve(TOOL_DIR, '../..')
const TEMPLATES = path.join(TOOL_DIR, 'templates')

/** Ricerca dei segnaposto: usata sia per sostituire sia per verificare i residui. */
const PLACEHOLDER_RE = /@@([A-Z0-9_]+)@@/g

// ── Argomenti ───────────────────────────────────────────────────────────────

function usage() {
  return `Uso: generate.mjs --app-id <id> --metric <nome> [opzioni]

Obbligatori
  --app-id <id>        identificativo dell'app: ${APP_ID_PATTERN}
                       È la stessa chiave di rotte, diritti, schema e listino.
  --metric <nome>      metrica di quota (es. "notes"): ciò che si conta per il tetto di piano

Opzioni
  --port <N>           porta HTTP del servizio (default: prima libera dopo quelle esistenti)
  --user-model <m>     single (B2C, default) | multi (B2B, con ruolo membro)
  --free-cap <N>       tetto del livello gratuito (default 10)
  --app-name <nome>    nome leggibile (default: derivato dall'identificativo)
  --icon <nome>        icona Material Symbols (default: widgets)
  --accent <token>     colore-categoria del design system (default: cat-blue)
  --dry-run            mostra il piano senza scrivere nulla
  --skip-infra         non invoca infra/scripts/service-add (da lanciare poi a mano)
  --help               questo testo
`
}

function parseArgs(argv) {
  const opts = {
    userModel: 'single',
    freeCap: 10,
    icon: 'widgets',
    accent: 'cat-blue',
    dryRun: false,
    skipInfra: false,
  }
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    const next = () => {
      const value = argv[i + 1]
      if (value === undefined || value.startsWith('--')) die(`${arg} richiede un valore`)
      i += 1
      return value
    }
    switch (arg) {
      case '--app-id': opts.appId = next(); break
      case '--metric': opts.metric = next(); break
      case '--port': opts.port = Number(next()); break
      case '--user-model': opts.userModel = next(); break
      case '--free-cap': opts.freeCap = Number(next()); break
      case '--app-name': opts.appName = next(); break
      case '--icon': opts.icon = next(); break
      case '--accent': opts.accent = next(); break
      case '--dry-run': opts.dryRun = true; break
      case '--skip-infra': opts.skipInfra = true; break
      case '--help':
      case '-h': process.stdout.write(usage()); process.exit(0); break
      default: die(`opzione sconosciuta: ${arg}\n\n${usage()}`)
    }
  }
  return opts
}

function die(message) {
  process.stderr.write(`\n✗ ${message}\n\n`)
  process.exit(1)
}

// ── Validazione ─────────────────────────────────────────────────────────────

function validate(opts, services) {
  if (!opts.appId) die(`manca --app-id\n\n${usage()}`)
  if (!APP_ID_PATTERN.test(opts.appId)) {
    die(
      `identificativo non valido: "${opts.appId}"\n`
      + '  Ammesso: minuscole, cifre e underscore, iniziale alfabetica, max 31 caratteri.\n'
      + "  È la stessa regola di infra/scripts/service-add: le due DEVONO restare allineate.",
    )
  }
  if (RESERVED_APP_IDS.has(opts.appId)) {
    die(`"${opts.appId}" è riservato (servizio di piattaforma o convenzione del monorepo)`)
  }
  if (!opts.metric) die(`manca --metric\n\n${usage()}`)
  if (!/^[a-z][a-z0-9_]{0,30}$/.test(opts.metric)) {
    die(`metrica non valida: "${opts.metric}" (minuscole/cifre/underscore, iniziale alfabetica)`)
  }
  if (!['single', 'multi'].includes(opts.userModel)) {
    die(`--user-model deve essere "single" o "multi" (ricevuto: "${opts.userModel}")`)
  }
  if (!Number.isInteger(opts.freeCap) || opts.freeCap < 1) {
    die('--free-cap deve essere un intero positivo')
  }

  const taken = services.find((s) => s.appId === opts.appId || s.svc === opts.appId)
  if (taken) {
    die(`esiste già un servizio con questo identificativo: services/${taken.svc} (app_id "${taken.appId}")`)
  }

  if (opts.port === undefined) {
    opts.port = nextFreePort(services)
  } else {
    if (!Number.isInteger(opts.port) || opts.port < 1024 || opts.port > 65535) {
      die(`--port non valida: ${opts.port}`)
    }
    const clash = services.find((s) => s.port === opts.port)
    if (clash) {
      die(
        `la porta ${opts.port} è già di services/${clash.svc}.\n`
        + `  Porte assegnate: ${services.map((s) => `${s.svc}:${s.port}`).join(', ')}\n`
        + `  Prima libera: ${nextFreePort(services)}`,
      )
    }
  }
}

/**
 * Il generatore non sovrascrive MAI. Se anche uno solo dei bersagli esiste,
 * l'app è già (parzialmente) presente: si elencano TUTTI i conflitti, così chi
 * legge capisce se si tratta di un'app vera o dei resti di un tentativo fallito.
 */
function checkConflicts(plan) {
  const existing = plan.creates.filter((c) => fs.existsSync(path.join(REPO_ROOT, c.path)))
  if (existing.length > 0) {
    die(
      `l'app "${plan.ctx.APP_ID}" sembra già esistere: ${existing.length} file/cartelle da creare sono già presenti.\n`
      + existing.slice(0, 10).map((c) => `    ${c.path}`).join('\n')
      + (existing.length > 10 ? `\n    … e altri ${existing.length - 10}` : '')
      + '\n  Il generatore non sovrascrive nulla. Rimuovere gli artefatti residui e ripetere.',
    )
  }
}

// ── Modelli-sorgente ────────────────────────────────────────────────────────

function substitute(text, ctx) {
  return text.replace(PLACEHOLDER_RE, (match, name) => {
    if (!(name in ctx)) {
      die(`segnaposto sconosciuto nei modelli-sorgente: ${match} (aggiungerlo a lib/context.mjs)`)
    }
    return ctx[name]
  })
}

/** File binari o comunque da copiare senza sostituzione (le chiavi JWT di test). */
const VERBATIM = new Set(['.pem'])

function walk(dir) {
  const out = []
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) out.push(...walk(full))
    else out.push(full)
  }
  return out
}

/**
 * Espande una cartella di modelli verso una destinazione, sostituendo i
 * segnaposto sia nei percorsi sia nei contenuti.
 */
function expand(templateDir, destDir, ctx) {
  return walk(templateDir).map((file) => {
    const relative = path.relative(templateDir, file)
    const destRelative = substitute(relative, ctx)
    const verbatim = VERBATIM.has(path.extname(file))
    const raw = fs.readFileSync(file, verbatim ? null : 'utf8')
    return {
      path: path.join(destDir, destRelative),
      content: verbatim ? raw : substitute(raw, ctx),
      verbatim,
    }
  })
}

function buildPlan(ctx) {
  const id = ctx.APP_ID
  const creates = [
    ...expand(path.join(TEMPLATES, 'service'), `services/${id}`, ctx),
    ...expand(
      path.join(TEMPLATES, 'frontend-module'),
      `frontend/apps/backoffice/src/modules/${id}`,
      ctx,
    ),
    ...expand(path.join(TEMPLATES, 'frontend-e2e'), 'frontend/apps/backoffice/e2e', ctx),
    ...expand(path.join(TEMPLATES, 'compliance'), 'docs/compliance/manifests', ctx),
    ...expand(path.join(TEMPLATES, 'pricing'), 'services/core/src/main/resources/pricing', ctx),
  ]
  return { ctx, creates, edits: EDITED_FILES }
}

// ── Controllo dei residui ───────────────────────────────────────────────────

/**
 * Nessun segnaposto deve sopravvivere. È l'ultimo cancello prima di dichiarare
 * fatto: gira su TUTTO ciò che è stato prodotto — contenuti e nomi di percorso —
 * e blocca la generazione se trova anche una sola occorrenza. Un segnaposto che
 * arriva in un file generato non è un refuso: significa che il modello-sorgente
 * usa un nome che il contesto non conosce, e la stessa app nascerebbe sbagliata
 * ogni volta.
 */
function assertNoLeftovers(artifacts) {
  const leftovers = []
  for (const artifact of artifacts) {
    if (PLACEHOLDER_RE.test(artifact.path)) {
      leftovers.push(`${artifact.path} (nel nome del file)`)
    }
    PLACEHOLDER_RE.lastIndex = 0
    if (typeof artifact.content === 'string') {
      for (const match of artifact.content.matchAll(PLACEHOLDER_RE)) {
        leftovers.push(`${artifact.path}: ${match[0]}`)
      }
    }
  }
  if (leftovers.length > 0) {
    die(
      'segnaposto NON risolti nell\'output — generazione annullata:\n'
      + leftovers.map((l) => `    ${l}`).join('\n'),
    )
  }
}

// ── Esecuzione ──────────────────────────────────────────────────────────────

function printPlan(plan, opts) {
  const { ctx } = plan
  const out = []
  out.push('')
  out.push(`  App             ${ctx.APP_ID}  (${ctx.APP_NAME})`)
  out.push(`  Modello utente  ${ctx.USER_MODEL}`)
  out.push(`  Porta HTTP      ${ctx.HTTP_PORT}   (debug JVM ${ctx.DEBUG_PORT}, derivata da dev/lib/services.sh)`)
  out.push(`  Schema          ${ctx.SCHEMA}`)
  out.push(`  Metrica quota   ${ctx.METRIC}  (tetto gratuito ${ctx.FREE_CAP}/mese, natura flow)`)
  out.push('')
  out.push(`  CREA (${plan.creates.length} file)`)
  for (const c of plan.creates) out.push(`    + ${c.path}`)
  out.push('')
  out.push(`  MODIFICA (${plan.edits.length} file)`)
  for (const e of plan.edits) out.push(`    ~ ${e}`)
  out.push('')
  out.push('  DELEGA')
  out.push(
    opts.skipInfra
      ? '    · infra/scripts/service-add — SALTATO (--skip-infra)'
      : `    → infra/scripts/service-add ${ctx.APP_ID} --port ${ctx.HTTP_PORT}`,
  )
  out.push('')
  out.push('  NON TOCCA (auto-scoperti da dev/lib/services.sh e tools/ci/services.sh)')
  out.push('    · app-start.sh, app-stop.sh, dev/Caddyfile, dev/lib/*.sh')
  out.push('    · tools/smoke/*.sh, .github/workflows/*')
  out.push('')
  process.stdout.write(out.join('\n'))
}

/**
 * Ciò che resta da fare a una persona. Non è cortesia: sono i due punti in cui
 * il repository è volutamente lasciato in stato ROSSO, e tacerli significherebbe
 * consegnare un lavoro che sembra finito e non lo è.
 */
function printHandoff(ctx) {
  process.stdout.write(
    `
  DA COMPLETARE — lo scaffolding si ferma qui apposta

  1. Manifesto dati  docs/compliance/manifests/${ctx.APP_ID}.yaml
     Contiene voci "DA COMPLETARE": finalità, interessati e conservazione non si
     indovinano. Il registro dei trattamenti (RoPA) è ORA disallineato, e resta
     rosso finché non si rigenera — di proposito, DOPO aver compilato il manifesto:
         cd tools/compliance && npm run assemble
     Rigenerarlo prima pubblicherebbe i segnaposto in un documento di compliance.

  2. Listino  services/core/src/main/resources/pricing/${ctx.APP_ID}.yaml
     Solo il livello gratuito, e \`status: inactive\`: l'app non è vendibile finché
     i livelli a pagamento non sono decisi (co-pilota prezzi/quota).

  3. Dominio  services/${ctx.APP_ID}/…/Item.java e migrazione V2
     È un modello SEGNAPOSTO: serve a far nascere l'app con la suite verde e gli
     invarianti già dimostrati. Sostituirlo è il primo lavoro vero.

  4. Tipi del client  npm run gen:${ctx.APP_ID}  (dopo la prima compilazione del backend)

  Verifica subito che l'app nasca verde:
      cd services && mvn -B -pl ${ctx.APP_ID} -am test

`,
  )
}

function main() {
  const opts = parseArgs(process.argv.slice(2))
  const services = discoverServices(REPO_ROOT)
  validate(opts, services)

  const ctx = buildContext(opts)
  const plan = buildPlan(ctx)

  // Il controllo sui residui gira PRIMA di scrivere: se i modelli hanno un
  // segnaposto sconosciuto, non si sporca il repository per scoprirlo dopo.
  assertNoLeftovers(plan.creates)
  checkConflicts(plan)

  printPlan(plan, opts)

  if (opts.dryRun) {
    process.stdout.write('  (--dry-run: non è stato scritto nulla)\n\n')
    return
  }

  // ── Scrittura, con ripristino in caso di errore ───────────────────────────
  // Un'app generata a metà è peggio di nessuna app: i conflitti al tentativo
  // successivo sarebbero incomprensibili. Si tiene traccia di ciò che è stato
  // creato e del contenuto originale dei file modificati.
  const written = []
  const originals = new Map()
  try {
    for (const artifact of plan.creates) {
      const full = path.join(REPO_ROOT, artifact.path)
      fs.mkdirSync(path.dirname(full), { recursive: true })
      fs.writeFileSync(full, artifact.content)
      written.push(full)
    }

    const editedContents = []
    for (const relative of plan.edits) {
      const full = path.join(REPO_ROOT, relative)
      if (!fs.existsSync(full)) {
        throw new Error(`file da modificare non trovato: ${relative}`)
      }
      const before = fs.readFileSync(full, 'utf8')
      originals.set(full, before)
      const after = EDITORS[relative](before, ctx)
      editedContents.push({ path: relative, content: after })
      fs.writeFileSync(full, after)
    }

    // Anche le modifiche passano dal controllo: un segnaposto incollato in un
    // file condiviso è ancora più difficile da notare che in un file nuovo.
    assertNoLeftovers(editedContents)
  } catch (err) {
    for (const [full, before] of originals) fs.writeFileSync(full, before)
    for (const full of written.reverse()) fs.rmSync(full, { force: true })
    fs.rmSync(path.join(REPO_ROOT, `services/${ctx.APP_ID}`), { recursive: true, force: true })
    fs.rmSync(
      path.join(REPO_ROOT, `frontend/apps/backoffice/src/modules/${ctx.APP_ID}`),
      { recursive: true, force: true },
    )
    die(`generazione fallita e ANNULLATA (nulla è rimasto a metà): ${err.message}`)
  }

  process.stdout.write(`  ✓ scritti ${plan.creates.length} file, modificati ${plan.edits.length}\n`)
  printHandoff(ctx)

  // ── Delega dell'infrastruttura ───────────────────────────────────────────
  // Ultimo passo perché è l'unico che può fallire per motivi d'ambiente
  // (terraform non installato). Se fallisce NON si annulla il codice già
  // generato — sarebbe una perdita sproporzionata — ma si esce con errore e si
  // stampa il comando da rilanciare, così nessuno crede che sia finita.
  if (opts.skipInfra) {
    process.stdout.write(
      `  · infrastruttura saltata: lanciare a mano\n`
      + `      ./infra/scripts/service-add ${ctx.APP_ID} --port ${ctx.HTTP_PORT}\n\n`,
    )
    return
  }
  try {
    execFileSync(
      path.join(REPO_ROOT, 'infra/scripts/service-add'),
      [ctx.APP_ID, '--port', String(ctx.HTTP_PORT)],
      { stdio: 'inherit', cwd: REPO_ROOT },
    )
    process.stdout.write('  ✓ istanza del modulo microsaas_app generata (test + prod)\n\n')
  } catch (err) {
    process.stderr.write(
      `\n✗ il codice dell'app è stato generato, ma service-add è fallito.\n`
      + `  Il wiring Terraform MANCA: l'app non ha ancora ECR, servizio ECS, rotta né code nel cloud.\n`
      + `  Rilanciare quando l'ambiente è a posto (serve terraform):\n`
      + `      ./infra/scripts/service-add ${ctx.APP_ID} --port ${ctx.HTTP_PORT}\n\n`,
    )
    process.exit(1)
  }
}

main()
