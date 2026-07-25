#!/usr/bin/env node
// ─────────────────────────────────────────────────────────────────────────────
// tools/pricing-change/change.mjs — la META' MECCANICA della skill `pricing-change`
// (UC 0047): i cambi di pricing SUCCESSIVI al lancio (#09 H36).
//
// Due responsabilità, entrambe deterministiche e collaudate (area `tooling`):
//   • fee            → calcola la fee effettiva + netto (avviso soft >10%, #09 K46/K47);
//   • le 4 modifiche → nuovo tier / aggiungi ciclo / cambia limiti / cambia prezzo,
//     sul pricing-as-code congelato dalla change 0019, rispettando l'immutabilità.
//
// Cosa NON fa, di proposito (change 0044): non parla col fornitore di pagamento (lo
// fa la sync, UC 0022, comunque bloccata da #14), non esegue migrazioni di abbonati
// (runbook, riusa changeSubscriptionTier), non fa commit/merge (gate di new-change).
// La skill conversazionale raccoglie le decisioni (grandfathering, quale via per il
// cambio prezzo) e poi chiama questo comando.
// ─────────────────────────────────────────────────────────────────────────────
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { computeFee, formatEur, DEFAULT_USD_TO_EUR, WARN_THRESHOLD_PCT } from './lib/fee.mjs'
import {
  load,
  serialize,
  validate,
  addTier,
  addCycle,
  setLimits,
  changePriceInPlace,
  changePriceNewTier,
} from './lib/pricing.mjs'

const TOOL_DIR = path.dirname(fileURLToPath(import.meta.url))
const REPO_ROOT = path.resolve(TOOL_DIR, '../..')
const PRICING_DIR = 'services/core/src/main/resources/pricing'

function usage() {
  return `Uso: change.mjs <comando> [opzioni]

Comandi
  fee            calcola la fee effettiva + netto per uno o più importi (avviso soft >10%)
  add-tier       aggiunge un nuovo tier a un listino
  add-cycle      aggiunge un ciclo (monthly/annual) a un tier esistente
  set-limits     cambia i limiti di un tier
  change-price   cambia un prezzo rispettando l'immutabilità (in loco o via nuovo tier)

Selezione del listino (per le modifiche)
  --slug <slug>  app del catalogo → ${PRICING_DIR}/<slug>.yaml
  --file <path>  percorso esplicito a un file YAML (per test/fixture)
  --dry-run      mostra il risultato senza scrivere

Esempi
  change.mjs fee --amount 500 --cycle monthly --amount 5000 --cycle annual
  change.mjs add-cycle --slug crm --tier team --cycle annual --amount 19000
  change.mjs change-price --slug crm --tier team --cycle monthly --amount 2200 --new-tier team_2026
  change.mjs change-price --slug crm --tier free --cycle monthly --amount 300 --in-place
`
}

function die(message) {
  process.stderr.write(`\n✗ ${message}\n\n`)
  process.exit(1)
}

// ── parsing degli argomenti ─────────────────────────────────────────────────────

function parseArgs(argv) {
  // Raccoglie le opzioni; --amount/--cycle possono ripetersi (comando fee).
  const opts = { amounts: [], cycles: [], dryRun: false, inPlace: false, fx: DEFAULT_USD_TO_EUR }
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    const val = () => {
      const v = argv[i + 1]
      if (v === undefined || v.startsWith('--')) die(`${arg} richiede un valore`)
      i += 1
      return v
    }
    switch (arg) {
      case '--slug': opts.slug = val(); break
      case '--file': opts.file = val(); break
      case '--tier': opts.tier = val(); break
      case '--new-tier': opts.newTier = val(); break
      case '--name': opts.name = val(); break
      case '--cycle': opts.cycles.push(val()); break
      case '--amount': opts.amounts.push(Number(val())); break
      case '--currency': opts.currency = val(); break
      case '--metric': opts.metric = val(); break
      case '--cap': opts.cap = Number(val()); break
      case '--type': opts.limitType = val(); break
      case '--window': opts.window = val(); break
      case '--trial-days': opts.trialDays = Number(val()); break
      case '--fx': opts.fx = Number(val()); break
      case '--in-place': opts.inPlace = true; break
      case '--dry-run': opts.dryRun = true; break
      case '--help': case '-h': process.stdout.write(usage()); process.exit(0); break
      default: die(`opzione sconosciuta: ${arg}\n\n${usage()}`)
    }
  }
  return opts
}

// ── risoluzione e scrittura del file ────────────────────────────────────────────

function resolvePricingPath(opts) {
  if (opts.file) return path.resolve(REPO_ROOT, opts.file)
  if (opts.slug) return path.join(REPO_ROOT, PRICING_DIR, `${opts.slug}.yaml`)
  die('serve --slug <slug> oppure --file <path> per individuare il listino')
}

function withDoc(opts, mutate) {
  const file = resolvePricingPath(opts)
  if (!fs.existsSync(file)) die(`listino non trovato: ${path.relative(REPO_ROOT, file)}`)
  const doc = load(fs.readFileSync(file, 'utf8'))
  mutate(doc)
  validate(doc)
  const out = serialize(doc)
  if (opts.dryRun) {
    process.stdout.write(`\n${out}\n  (--dry-run: non è stato scritto nulla)\n\n`)
    return
  }
  fs.writeFileSync(file, out)
  process.stdout.write(`  ✓ aggiornato ${path.relative(REPO_ROOT, file)}\n`)
}

// ── comando: fee ─────────────────────────────────────────────────────────────────

function runFee(opts) {
  if (opts.amounts.length === 0) die('fee richiede almeno un --amount (in centesimi)')
  process.stdout.write('\n  Fee effettiva del fornitore di pagamento (segnale-guida, avviso soft >' + WARN_THRESHOLD_PCT + '%)\n\n')
  for (let i = 0; i < opts.amounts.length; i += 1) {
    const amountMinor = opts.amounts[i]
    const cycle = opts.cycles[i] ?? '—'
    const r = computeFee({ amountMinor, fxUsdToEur: opts.fx })
    const pct = r.effectivePct.toFixed(1)
    const flag = r.warn ? `  ⚠ oltre il ${WARN_THRESHOLD_PCT}% — valuta un prezzo più alto o spingi l'annuale` : ''
    process.stdout.write(
      `  ${cycle.padEnd(8)} ${formatEur(amountMinor).padStart(9)}  →  fee ${pct}% (${formatEur(r.feeMinor)})  netto ${formatEur(r.netMinor)}${flag}\n`,
    )
  }
  process.stdout.write('\n')
}

// ── comando: modifiche ────────────────────────────────────────────────────────────

function buildLimits(opts) {
  if (!opts.metric) die('set-limits richiede almeno --metric')
  const limits = { metric: opts.metric }
  if (opts.window) limits.window = opts.window
  if (opts.cap != null && !Number.isNaN(opts.cap)) limits.cap = opts.cap
  if (opts.limitType) limits.type = opts.limitType
  return limits
}

function main() {
  const [command, ...rest] = process.argv.slice(2)
  if (!command || command === '--help' || command === '-h') {
    process.stdout.write(usage())
    process.exit(command ? 0 : 1)
  }
  const opts = parseArgs(rest)

  switch (command) {
    case 'fee':
      runFee(opts)
      break
    case 'add-tier':
      withDoc(opts, (doc) => addTier(doc, {
        key: opts.tier,
        name: opts.name,
        trialDays: opts.trialDays,
        limits: buildLimits(opts),
        prices: opts.amounts.map((amount, i) => ({
          billingCycle: opts.cycles[i], amount, currency: opts.currency ?? 'EUR',
        })),
      }))
      break
    case 'add-cycle':
      withDoc(opts, (doc) => addCycle(doc, {
        tierKey: opts.tier, billingCycle: opts.cycles[0], amount: opts.amounts[0], currency: opts.currency,
      }))
      break
    case 'set-limits':
      withDoc(opts, (doc) => setLimits(doc, { tierKey: opts.tier, limits: buildLimits(opts) }))
      break
    case 'change-price':
      if (opts.inPlace && opts.newTier) die('scegliere UNA via: --in-place (bozza) oppure --new-tier <key> (prezzo vivo)')
      if (!opts.inPlace && !opts.newTier) {
        die('change-price richiede la via esplicita: --new-tier <key> (prezzo vivo, immutabilità-safe) o --in-place (prezzo non ancora sincronizzato)')
      }
      withDoc(opts, (doc) => (opts.inPlace
        ? changePriceInPlace(doc, { tierKey: opts.tier, billingCycle: opts.cycles[0], amount: opts.amounts[0] })
        : changePriceNewTier(doc, {
          tierKey: opts.tier, newTierKey: opts.newTier, billingCycle: opts.cycles[0], amount: opts.amounts[0], newName: opts.name,
        })))
      break
    default:
      die(`comando sconosciuto: ${command}\n\n${usage()}`)
  }
}

main()
