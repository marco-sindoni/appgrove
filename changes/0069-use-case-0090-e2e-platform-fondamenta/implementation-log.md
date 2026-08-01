# Implementation Log — Change 0069: Fondamenta suite end-to-end di piattaforma

**Branch**: `change/0069-use-case-0090-e2e-platform-fondamenta`
**Aree**: tools/platform-e2e (nuova), tools/smoke, dev/lib, run-tests.sh, .github/workflows
**Completata**: 2026-08-01
**Modalità**: fast (autopilot senza gate di workflow, dichiarata dal chiamante `go-fast`) — tutte le
risposte alle domande di approfondimento sono dell'agente e sono tracciate in [decisions.json](decisions.json)

## File modificati

| File | Azione |
|---|---|
| `dev/lib/headless.sh` | Creato — libreria condivisa dell'avvio headless (env, compose, migrate+seed, build, chiavi, avvio jar, readiness) |
| `tools/smoke/stack-headless.sh` | Modificato — rifattorizzato sulla libreria condivisa, comportamento invariato |
| `tools/platform-e2e/run.sh` | Creato — orchestratore della suite (stack vero → SPA costruite → journey → verdetto) |
| `tools/platform-e2e/serve-spa.mjs` | Creato — server statico + inoltro `/api/*` (stdlib Node, rotte derivate dalla scoperta servizi) |
| `tools/platform-e2e/playwright.config.ts` | Creato — retries 1, trace/screenshot/video su fallimento |
| `tools/platform-e2e/helpers/{mailbox,db,api}.ts` | Creati — libreria condivisa journey (Mailpit, leak-detector DB, tenant/login via API) |
| `tools/platform-e2e/journeys/J-REG.spec.ts` | Creato — journey registrazione + collaudo helper `tenant()` |
| `tools/platform-e2e/{README.md,.gitignore,package.json,package-lock.json}` | Creati — runbook diagnosi + progetto npm |
| `run-tests.sh` | Modificato — nuova area `platform` (inclusa nell'esecuzione completa) |
| `.github/workflows/verify-pr.yml` | Modificato — job `platform` dedicato (path-filter, non bloccante come `smoke`) |
| `docs/_PARITA-SCAFFOLD.md` | Modificato — 2 deviazioni consapevoli (stack-headless.sh, verify-pr.yml) |
| `docs/usecases/20-test-e2e-piattaforma/0090-e2e-platform-fondamenta.md` | Modificato — punti aperti: rimandi paddle()/totp() → UC 0091/0092, nota baseline freemium |
| `docs/usecases/02-devops-infra/0005-pipeline-cicd.md` | Modificato — punto aperto: promozione job `platform` a required |
| `docs/usecases/EPICS-WAVE-2.md` | Modificato — UC 0090 → ✅ |

## Cosa è stato fatto

Costruito il quarto livello di test del monorepo (`./run-tests.sh platform`): browser vero (Playwright)
sulle SPA **costruite davvero**, contro lo stack backend **vero** (Postgres + ElasticMQ + **Mailpit** dal
compose dev, tutti i `services/*` scoperti automaticamente in profilo `dev` su porte dedicate +12000),
con le email transazionali **realmente spedite e verificate** via Mailpit. Le SPA sono servite da un
server statico Node senza dipendenze che inoltra le rotte `/api/*` derivate dalla stessa scoperta
servizi (equivalente headless del blocco `api-routes` del Caddyfile). Primo journey **J-REG** completo:
signup dal browser → email di verifica ricevuta e cliccata → gate legali (UC 0056) → onboarding
workspace → dashboard con sidebar coerente con gli entitlement reali → leak-detector a livello DB.
I passi comuni con lo smoke headless sono estratti in `dev/lib/headless.sh` (smoke invariato).

## Decisioni prese

Change condotta in **autopilot fast**: tutte le decisioni sono dell'agente, registro completo e
strutturato in [decisions.json](decisions.json) (16 voci, 15 in autopilot). Le principali:
SPA servite da server statico stdlib con inoltro API derivato dalla scoperta servizi (opzione
raccomandata dallo stesso UC); estrazione dei passi comuni smoke↔suite in `dev/lib/headless.sh`;
offset porte +12000 e SPA su :24173/:24174 (convivenza con dev e smoke); helper `mailbox`/`db`/
`tenant`/`login` implementati e esercitati, `paddle()`/`totp()` differiti ai primi consumatori
(UC 0091/0092); override di suite `MP_JWT_VERIFY_PUBLICKEY_LOCATION` e CORS (`_DEV_QUARKUS_HTTP_CORS_ORIGINS`)
per i servizi non-auth; J-REG attraversa il gate legali e asserta sidebar ≡ entitlements API
(baseline freemium UC 0027, che supera l'attesa "senza app attive" dello use case — drift annotato).

## Invarianti appgrove

La suite **verifica** gli invarianti invece di introdurre superfici: J-REG controlla che `tenant_id`
del JWT emesso coincida con le righe DB (`platform.users`/`accounts`) e che nessun dato del tenant
sintetico esista fuori dal suo `tenant_id` (leak-detector via `docker exec psql`, sole letture,
query parametrizzate). Nessun codice di produzione modificato.

## Note per il revisore

- **Gate privacy (UC 0031)**: lo scanner segnala la nuova dipendenza `@playwright/test`
  (tools/platform-e2e). Classificata: **non** è un sub-processor — dipendenza di collaudo locale/CI,
  nessun trattamento di dati personali (dati solo sintetici su dominio non recapitabile). Nessun
  manifesto/RoPA da aggiornare, nessun bump legali.
- **Gate parità scaffold (UC 0046)**: toccati 2 percorsi-sorgente (`verify-pr.yml`,
  `stack-headless.sh`) → deroga registrata in `docs/_PARITA-SCAFFOLD.md` (nessun modello replica quei
  file; modifiche neutre per le app generate). `parity-check` verde.
- **Decisioni differite tracciate**: helper `paddle()`/`totp()` → UC 0091/0092 (punti aperti di
  UC 0090); promozione del job CI `platform` a check required → punti aperti di UC 0005; nota baseline
  freemium per i journey figli → punti aperti di UC 0090.
- **Landing stale**: non applicabile (nessuna superficie feature/pricing di app toccata).
- Il job CI `platform` è **non bloccante** (`continue-on-error`) come lo smoke alla sua introduzione.

## Test

La suite **è** il test. Esiti:

- `./run-tests.sh platform` — **verde** (J-REG + J-REG-API, 2 passed); collaudo idempotenza: **4
  esecuzioni consecutive** senza pulizia manuale (email/tenant unici per run); convivenza porte
  garantita dagli offset (+12000 vs +10000 smoke vs stack dev) e collaudata con smoke+platform sullo
  stesso compose/DB nella suite completa.
- `./run-tests.sh` completo (tutte le aree, nessun parametro) — **verde** (evidenza di non
  regressione della modalità fast; lo smoke rifattorizzato su `dev/lib/headless.sh` resta verde).
- Diagnosi su rosso: trace/screenshot/video in `tools/platform-e2e/test-results/`, log servizi in
  `tools/platform-e2e/.run/`, casella Mailpit su :8025 (runbook in `tools/platform-e2e/README.md`).

## Stato criteri di accettazione

- [x] `./run-tests.sh platform` esiste e ritorna verdetto unico (exit ≠ 0 su journey rosso)
- [x] J-REG verde: email reale ricevuta/cliccata, onboarding, dashboard coerente, assert DB
- [x] Convivenza porte + doppia esecuzione consecutiva senza pulizia (idempotenza)
- [x] `./run-tests.sh` completo non regredisce (smoke invariato sul lib condiviso)
- [x] Job CI `platform` in `verify-pr.yml` (non bloccante, come smoke)
