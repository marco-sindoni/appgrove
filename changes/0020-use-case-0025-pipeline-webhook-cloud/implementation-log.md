# Implementation Log — Change 0020: Hardening pipeline webhook Paddle (dedup, out-of-order, set eventi, firma reale, DLQ — L1 esaustivo)

**Branch**: `change/0020-use-case-0025-pipeline-webhook-cloud`
**Aree**: `services/core` (backend)
**Completata**: 2026-06-28

## File modificati

| File | Azione |
|---|---|
| services/core/src/main/resources/db/migration/V3__webhook_dedup.sql | Creato |
| services/core/.../billing/WebhookEventMapping.java | Creato |
| services/core/.../billing/PaddleSignature.java | Modificato (formato `ts/h1` + anti-replay) |
| services/core/.../billing/PaddleWebhookEvent.java | Modificato (`status` nullable + `paddleCustomerId`) |
| services/core/.../billing/SubscriptionWriter.java | Modificato (apply transazionale: dedup + out-of-order + customer + outcome) |
| services/core/.../billing/PaddleWebhookConsumer.java | Modificato (outcome + redrive/DLQ-aware) |
| services/core/.../billing/StubScenarioEmitter.java | Modificato (scenari paused/resumed/renewal/chargeback/customer) |
| services/core/.../billing/LifecycleScenario.java | Modificato (nuovi scenari) |
| services/core/src/main/resources/application.properties | Modificato (`webhook-max-age`) |
| services/core/.../test/TestData.java | Modificato (helper subscriptionStatus/periodEnd/customerId/webhookOutcome) |
| services/core/.../test/billing/InMemoryWebhookQueue.java | Modificato (receive-count + simulazione DLQ) |
| services/core/.../test/billing/PaddleWebhookIngestTest.java | Modificato (test replay) |
| services/core/.../test/billing/WebhookFixtures.java | Creato (builder payload L1) |
| services/core/.../test/billing/WebhookHardeningTest.java | Creato (L1 esaustivo) |
| services/core/.../test/billing/WebhookSignatureTest.java | Creato (firma `ts/h1` + replay) |
| docs/usecases/07-payments/0025-pipeline-webhook.md | Modificato (Punti aperti: differiti tracciati) |
| docs/usecases/_INDEX.md | Modificato (0025 → ✅) |

## Cosa è stato fatto

Irrobustito il livello applicativo della pipeline webhook (consumer minimo di 0018) fino all'L1 esaustivo, tutto in locale:
firma reale `ts=…;h1=…` con **anti-replay**; **dedup** su `event_id` (tabella `webhook_event` + claim transazionale autoritativo
nel consumer); **out-of-order** via `occurred_at` (colonna `subscription.last_event_occurred_at` + guardia nel `DO UPDATE`);
**set di eventi completo** mappato su `subscription`/`accounts.paddle_customer_id` con policy autoritativa (`WebhookEventMapping`,
non si fida del payload per i `transaction.*`); **redrive→DLQ** a livello consumer (non-ack su errore). Tutto applicato in **una
sola transazione** per evento (dedup + mutazione + esito), così un fallimento rolla indietro anche il record di dedup → il
messaggio è ri-consegnato e poi instradato in DLQ.

## Decisioni prese

- **Dedup autoritativo nel consumer (transazionale)**, non all'ingest: `INSERT … ON CONFLICT (event_id) DO NOTHING` come "claim"
  prima dell'applicazione; 0 righe → duplicato no-op. Corretto anche sotto redelivery/concorrenza (il pre-filtro all'ingest/Lambda
  resta ottimizzazione cloud).
- **Out-of-order come `WHERE` sul `DO UPDATE`** dell'upsert (`last_event_occurred_at <= excluded`): 0 righe aggiornate ⇒
  `skipped_stale`, senza eccezioni.
- **Mappatura evento→stato esplicita** (`WebhookEventMapping`): per `transaction.completed/payment_failed/disputed` lo stato è
  **forzato** (active/past_due/past_due) ignorando lo `status` del payload — i test lo provano con status "sbagliato".
- **Simulazione DLQ in `InMemoryWebhookQueue`** (receive-count + lista DLQ, `maxReceiveCount=5` come `dev/elasticmq.conf`) per
  testare il redrive offline.
- **Fix**: `MDC.put` non accetta valori null → gli eventi `customer.*` (senza `app_id`) ora omettono la chiave invece di passare null.

## Invarianti appgrove

- **Tenant ID non manomettibile**: il linkage `(tenant_id, app_id)` viene dai `custom_data` **dentro il payload firmato HMAC**;
  l'anti-replay rafforza la non-falsificabilità. Il consumer scrive con tenant **esplicito** dal payload firmato (gira fuori da
  richiesta autenticata, `TenantResolver` fail-closed).
- **Filtro row-level**: `Subscription` resta `BaseTenantEntity` (`@TenantId`); `webhook_event` è log di pipeline non tenant-scoped
  (dedup globale per `event_id`, no PII).
- **Modulo `microsaas_app`**: N/A — anzi è il rispetto dell'invariante a imporre il **differimento** del packaging cloud (niente
  infra bespoke prima del modulo).
- **Logging strutturato**: ingest/consumer/errore popolano MDC con `tenant_id`/`app_id` (+ `user_id="system/webhook"`); `app_id`
  omesso sugli eventi customer.

## Note per il revisore

- **Confine di scope (gate di chiarimento)**: questa change copre **solo l'hardening applicativo**; il **packaging cloud**
  (Lambda @ API GW, Terraform/`microsaas_app`, Secrets Manager per-env, allarme CloudWatch DLQ) è **differito** perché `infra/`
  non ha ancora `.tf` e le fondamenta (0003/0004/0006/0055) vengono dopo nel piano (`phased-env-activation`).
- **Decisioni differite tracciate** in `docs/usecases/07-payments/0025-pipeline-webhook.md` → "Punti aperti / decisioni differite",
  sezione "Stato dopo la change 0020": packaging cloud (proprietà UC 0025, gated infra); semantica lifecycle → **UC 0026**;
  enforcement entitlement/quota → **UC 0027**; validazione firma/eventi reali → **L3/UC 0029**; customer lazy + custom_data
  server-side → **UC 0024**. Nulla resta solo in chat.
- **Contratto cross-area**: il formato firma webhook passa da hex puro a `ts=…;h1=…`; stub e test sono allineati nello stesso
  commit; nessun consumatore esterno reale ancora (gated #14). Nessun impatto su frontend/infra.
- **Avvio locale (DoD CLAUDE.md)**: nessun nuovo processo/modulo/route → `app-start.sh`/`app-stop.sh`/`Caddyfile` invariati. La
  migrazione `V3` è additiva e applicata da Flyway (`%dev` migrate-at-start, e su Postgres reale Testcontainers in test).
- **Privacy/RoPA**: nessuna nuova categoria di dato; `accounts.paddle_customer_id` (identificativo opaco) già esistente. Nessun
  checkpoint.
- **OpenAPI**: nessun drift (`PaddleStubDevResource` escluso dallo scan; nessun endpoint pubblico nuovo).

## Test

- **`services/core` (`mvn test`, Testcontainers Postgres 17)** — suite billing/webhook (21 test) verde:
  - `WebhookSignatureTest` (4): round-trip `ts/h1`, manomissione, **replay** fuori finestra, header malformato.
  - `WebhookHardeningTest` (7): **dedup** (event_id ripetuto = no-op), **out-of-order** (evento vecchio ignorato), **ogni evento**
    del set → stato atteso (con mappatura autoritativa sui `transaction.*`), **rinnovo** (periodo avanzato), **customer.*** →
    `paddle_customer_id`, evento non sottoscritto = no-op, **poison→DLQ** (non perso, non in loop).
  - `PaddleWebhookIngestTest` (4): firma valida→200/accodato, errata/assente→401, **replay→401**.
  - `SubscriptionPipelineTest` (6): scenari lifecycle (rigenerati con firma `ts/h1`) — regressione verde.
- **Suite backend completa** (`./run-tests.sh backend`): commons/core/auth-local/fatture → **BUILD SUCCESS** (tutte verdi).
- Frontend/infra: non toccati da questa change.

## Stato criteri di accettazione

- [x] Firma reale `ts=…;h1=…` con anti-replay (ts fuori finestra → 401); stub e test allineati.
- [x] Migrazione `V3` (`webhook_event` unique + `subscription.last_event_occurred_at`) applicata su Postgres reale.
- [x] Dedup: stesso `event_id` ×2 → applicato una sola volta.
- [x] Out-of-order: evento più vecchio non sovrascrive (`skipped_stale`) ma resta dedotto.
- [x] Set eventi completo (sub.*, transaction.completed/payment_failed/disputed, customer.* → paddle_customer_id).
- [x] Redrive/DLQ: messaggio velenoso → DLQ, non perso né in loop; errore loggato strutturato.
- [x] Firma errata/assente → 401 senza scrittura (regression guard 0018); nessun cross-tenant.
- [x] Logging strutturato (`tenant_id`/`app_id`/`user_id`).
- [x] `./run-tests.sh backend` verde.
- [x] UC 0025 "Punti aperti" aggiornata con tutti i differiti; `_INDEX.md` 0025 → ✅.
