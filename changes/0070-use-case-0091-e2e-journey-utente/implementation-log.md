# Implementation Log — Change 0070: Batteria journey end-to-end lato utente

**Branch**: `change/0070-use-case-0091-e2e-journey-utente`
**Aree**: tools/platform-e2e · docs
**Completata**: 2026-08-01
**Modalità**: fast (autopilot senza gate di workflow, dichiarata dall'orchestratore all'invocazione) — le
risposte alle domande di approfondimento sono dell'agente e sono tracciate in [decisions.json](decisions.json)

## File modificati

| File | Azione |
|---|---|
| tools/platform-e2e/journeys/J-BUY.spec.ts | Creato |
| tools/platform-e2e/journeys/J-QUOTA.spec.ts | Creato |
| tools/platform-e2e/journeys/J-MEMBERS.spec.ts | Creato |
| tools/platform-e2e/journeys/J-SUB.spec.ts | Creato |
| tools/platform-e2e/journeys/J-PWD.spec.ts | Creato |
| tools/platform-e2e/journeys/J-PRIVACY.spec.ts | Creato |
| tools/platform-e2e/journeys/J-LEGAL.spec.ts | Creato |
| tools/platform-e2e/helpers/paddle.ts | Creato (webhook sintetici firmati + acquisto via checkout reale) |
| tools/platform-e2e/helpers/totp.ts | Creato (codici RFC 6238 coerenti col servizio auth) |
| tools/platform-e2e/helpers/browser.ts | Creato (login browser + attraversamento gate legale) |
| tools/platform-e2e/helpers/api.ts | Modificato (loginRaw/loginMfa, pollUntil, authedFetch) |
| tools/platform-e2e/helpers/db.ts | Modificato (dbExec riservato alle leve d'ambiente) |
| tools/platform-e2e/global-setup.ts | Creato (attivazione crm via leva admin, una volta per run) |
| tools/platform-e2e/global-teardown.ts | Creato (ripristino crm inactive, best-effort) |
| tools/platform-e2e/playwright.config.ts | Modificato (globalSetup/teardown, progetto legal-serial, timeout 180s) |
| tools/platform-e2e/run.sh | Modificato (MinIO, bypass 2FA spento per l'auth, override core-api, --no-deps) |
| tools/platform-e2e/README.md | Modificato (tabella journey ↔ funzionalità, helper, runbook) |
| docs/usecases/20-test-e2e-piattaforma/0091-e2e-platform-journey-utente.md | Modificato (punti aperti: ramo upgrade fatture, refresh al reset) |
| docs/usecases/20-test-e2e-piattaforma/0090-e2e-platform-fondamenta.md | Modificato (punto aperto paddle/totp chiuso) |
| docs/usecases/03-local-dev/0058-flussi-auth-locali-completi.md | Modificato (punto aperto: reset non invalida i refresh) |
| docs/usecases/EPICS-WAVE-2.md | Modificato (UC 0091 → ✅) |
| changes/0070-…/requirements.md · decisions.json · implementation-log.md | Creati |

## Cosa è stato fatto

Costruita la batteria dei sette journey utente della suite end-to-end di piattaforma (UC 0091) sulle
fondamenta di UC 0090: acquisto e attivazione (J-BUY), quota a consumo con 429 reale (J-QUOTA), inviti B2B
con email vera e seconda sessione browser + posti fino al 429 e upgrade che sblocca (J-MEMBERS), ciclo di
vita abbonamento con scadenza simulata da webhook firmato (J-SUB), reset password con email vera + 2FA TOTP
con challenge reale (J-PWD), diritti GDPR con export asincrono reale scaricato e validato, recesso con purge
e eliminazione con grace (J-PRIVACY), ri-accettazione legale a runtime (J-LEGAL, serializzato). Consegnati
gli helper differiti da UC 0090 (`paddle`, `totp`) più `browser`, `pollUntil`, `authedFetch`, `dbExec`.
L'orchestratore avvia anche MinIO, spegne il bypass 2FA per l'auth della suite e corregge l'URL del
rest-client `core-api` dei servizi app verso il core della suite.

## Decisioni prese

Registro completo e strutturato in [decisions.json](decisions.json) (12 decisioni, tutte `(autopilot)` tranne
la dichiarazione di modalità). Le principali: Mini-CRM attivato a runtime con la leva admin come unica app
comprabile del registry (dec. 3); ramo upgrade di J-QUOTA spostato sulla metrica a giacenza dentro J-MEMBERS
perché `fatture` non ha tier a pagamento (dec. 4, rimando in UC 0091); webhook sintetici firmati sull'ingest
reale come leva di fine-periodo (dec. 5); bypass 2FA spento per la sola auth della suite (dec. 6); assert
sull'invalidazione dei refresh escluso e tracciato in UC 0058 (dec. 7); J-LEGAL serializzato con leva
d'ambiente su `platform.legal_version` (dec. 8); MinIO nell'orchestratore (dec. 9); nessuna spec L2 rimossa
per il criterio di riparto (dec. 10); override `core-api` scoperto alla prima esecuzione reale (dec. 12).

## Invarianti appgrove

La change non introduce superfici runtime: i journey **verificano** gli invarianti. Tenant ID solo dal JWT
(J-REG-API e J-BUY confrontano claim ↔ DB; i webhook sintetici portano il tenant nei `custom_data` firmati
come lo stub reale); filtro row-level provato dai leak detector di ogni journey (nessuna riga fuori dal
tenant, canarino di J-PRIVACY escluso dall'export); gate entitlement/ruolo/quota osservati dal browser
(402/429/route guard). Nessun codice di prodotto modificato.

## Note per il revisore

- **Stato di catalogo per la run**: il global-setup attiva `crm` (PATCH admin col platform-admin del seed) e
  il teardown la ripristina; la sync pricing a ogni avvio riparte comunque dallo YAML. `pricing/crm.yaml`
  non è toccato.
- **Override d'ambiente della suite** (run.sh): JWKS e CORS (change 0069) più, da questa change,
  `QUARKUS_REST_CLIENT_CORE_API_URL` (i servizi app in profilo dev puntano il core dello stack dev: per un
  tenant fresco il fetch-on-miss della proiezione entitlement fallirebbe chiuso → 402 ovunque) e
  `AUTH_LOCAL_TOTP_BYPASS=false` (challenge 2FA reale in J-PWD).
- **Decisioni differite tracciate**: ramo upgrade di J-QUOTA su fatture → punti aperti di UC 0091;
  invalidazione dei refresh al reset → punti aperti di UC 0058; helper paddle/totp segnati consegnati in
  UC 0090. Il journey di localizzazione resta differito (già nei punti aperti di UC 0091).
- **Gate parità scaffold**: nessun percorso-sorgente toccato (scan verde). **Landing stale**: nessuna
  superficie feature/pricing di app toccata.
- Nessun impatto su contratti cross-area.

## Test

La suite È il test. `./run-tests.sh platform`: 9 test verdi (J-REG ×2 + 7 journey; J-LEGAL nel progetto
serializzato `legal-serial`). Valutazione end-to-end L2 (frontend): nessuna spec aggiunta o rimossa — la
change non tocca superficie frontend; il criterio di riparto è documentato (dec. 10). Suite completa
`./run-tests.sh` senza parametri: verde (evidenza di non regressione della modalità fast).
Gate privacy (UC 0031): nessun segnale.

## Stato criteri di accettazione

- [x] 7 journey con ID stabile, verdi nel comando unico `./run-tests.sh platform`
- [x] Tenant fresco per journey, parallelismo (J-LEGAL serializzato), leak detector sul tenant_id
- [x] Assert esterne distintive: email reali (invito, reset), webhook firmati, download export validato, assert DB
- [x] Nessuna attesa a tempo fisso; fallimenti parlanti; retry ≤ 1
- [x] Doppia esecuzione consecutiva senza pulizia manuale (tenant/email unici; sync legale allo startup)
- [x] Criterio di riparto piattaforma/L2 applicato e documentato (nessuna rimozione)
- [x] Suite completa verde; EPICS-WAVE-2.md → ✅ per UC 0091
