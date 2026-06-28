# Implementation Log — Change 0018: Stub Paddle locale (UC 0023)

**Branch**: `change/0018-use-case-0023-stub-paddle-locale`
**Aree**: `services/core`, `frontend`
**Completata**: 2026-06-27

## File modificati

| File | Azione |
|---|---|
| `services/core/pom.xml` | Modificato — AWS SDK v2 BOM + `sqs` + `url-connection-client`, `quarkus-scheduler` |
| `services/core/src/main/resources/application.properties` | Modificato — selettore provider/queue, secret HMAC test, endpoint SQS `%dev`, dev-endpoints, esclusione OpenAPI |
| `services/core/.../core/billing/Subscription.java` | Creato — entità JPA tenant-scoped (DDL UC 0013) |
| `services/core/.../core/billing/SubscriptionStatus.java` | Creato — enum stati |
| `services/core/.../core/billing/SubscriptionRepository.java` | Creato — repository (read tenant-scoped) |
| `services/core/.../core/billing/PaymentProvider.java` | Creato — port + record comandi |
| `services/core/.../core/billing/StubPaymentProvider.java` | Creato — impl dev/test (API finta, ID plausibili) |
| `services/core/.../core/billing/PaddlePaymentProvider.java` | Creato — placeholder prod (gated #14) |
| `services/core/.../core/billing/PaddleSignature.java` | Creato — firma/verifica HMAC-SHA256 |
| `services/core/.../core/billing/InvalidWebhookSignatureException.java` | Creato |
| `services/core/.../core/billing/WebhookQueue.java` | Creato — astrazione coda (≈ SQS) |
| `services/core/.../core/billing/SqsWebhookQueue.java` | Creato — impl ElasticMQ/SQS (fuori da `test`) |
| `services/core/.../core/billing/WebhookIngestService.java` | Creato — verifica firma → accoda |
| `services/core/.../core/billing/PaddleWebhookResource.java` | Creato — ingest pubblico `POST /webhooks/paddle` (HMAC→401/200) |
| `services/core/.../core/billing/PaddleWebhookEvent.java` | Creato — parsing payload (snapshot) |
| `services/core/.../core/billing/SubscriptionWriter.java` | Creato — upsert idempotente (SQL nativo, tenant esplicito) |
| `services/core/.../core/billing/PaddleWebhookConsumer.java` | Creato — poller/drain del consumer |
| `services/core/.../core/billing/LifecycleScenario.java` | Creato — enum scenari |
| `services/core/.../core/billing/StubScenarioEmitter.java` | Creato — emette webhook firmati per scenario |
| `services/core/.../core/billing/PaddleStubDevResource.java` | Creato — endpoint dev-only (trigger scenari + read subscription) |
| `services/core/.../META-INF/openapi/openapi.{yaml,json}` | Modificato — rigenerati (+`/webhooks/paddle`; dev endpoint esclusi) |
| `services/core/src/test/.../core/billing/InMemoryWebhookQueue.java` | Creato — coda in-memory (`@Mock`) |
| `services/core/src/test/.../core/billing/{PaymentProvider,PaddleWebhookIngest,SubscriptionPipeline}Test.java` | Creato — L1-min |
| `services/core/src/test/.../core/TestData.java` | Modificato — helper `app`/`appTier`/`subscriptionCount` |
| `services/core/src/test/resources/application.properties` | Modificato — dev-endpoints on, scheduler off (coda mock) |
| `frontend/packages/paddle-stub/**` | Creato — pacchetto `@appgrove/paddle-stub` (Fake Paddle.js + test) |
| `frontend/package-lock.json` | Modificato — nuovo workspace |
| `docs/usecases/_INDEX.md` | Modificato — UC 0023 → ✅ |
| `docs/usecases/07-payments/{0022,0023,0025}.md` | Modificato — decisioni differite (commit requirements) |

## Cosa è stato fatto

Implementato lo **stub Paddle locale** (UC 0023) con la **pipeline locale minima** (Approach A): port `PaymentProvider`
(stub in dev/test, placeholder reale in prod), endpoint di **ingest** che verifica la **firma HMAC** e accoda su ElasticMQ,
**consumer** idempotente che applica gli eventi a `subscription` (entità JPA nata qui), e una **libreria di scenari** lifecycle
(happy/past_due/canceled/upgrade/downgrade) emessi come **webhook sintetici firmati** che passano per la **stessa** pipeline.
Lato frontend, il pacchetto `@appgrove/paddle-stub` espone la stessa interfaccia dell'overlay ed emette `checkout.completed`
sintetico. Tutto offline e deterministico; nessun account Paddle, nessun dato di pagamento reale.

## Decisioni prese

- **Consumer a rigore Minimo** (deciso al gate): HMAC + idempotenza (upsert convergente su `(tenant_id, app_id)`). Dedup
  `event_id`, out-of-order `occurred_at`, DLQ/allarmi e packaging Lambda/API GW → **UC 0025** (tracciato).
- **Scrittura `subscription` via SQL nativo con tenant esplicito** dal payload firmato: il consumer gira fuori da una richiesta
  autenticata (niente JWT → resolver fail-closed), stesso pattern di `TestData`/`AdminResource`. L'entità JPA serve la lettura
  tenant-scoped (discriminator), usata dall'endpoint dev e da UC 0027.
- **Client SQS**: AWS SDK v2 (HTTP url-connection) puntato a ElasticMQ in `%dev`, dietro l'astrazione `WebhookQueue` (in test
  sostituita da una coda in-memory `@Mock` → niente LocalStack, test deterministici). UC 0025 valuterà l'estensione Quarkus.
- **Endpoint dev esclusi dal contratto OpenAPI** (`mp.openapi.scan.exclude.classes`) per evitare drift tra `mvn test` (dev-endpoints
  attivi) e build di package.

## Invarianti appgrove

- **tenant_id non dal client**: il linkage viene dai `custom_data` **dentro il payload firmato HMAC**; l'ingest è un canale
  server-to-server firmato (eccezione esplicita all'authorizer, #09 D / UC 0025 §8). Gli endpoint dev leggono il tenant dal JWT.
- **Filtro row-level**: `Subscription` estende `BaseTenantEntity` (`@TenantId`) → letture auto-filtrate; la scrittura imposta
  `tenant_id` esplicito dal payload firmato. Verificato dal test di isolamento (tenant B non vede la subscription di A).
- **Modulo `microsaas_app`**: N/A (nessuna infra).
- **Logging strutturato**: ingest e consumer popolano MDC `tenant_id`/`app_id`/`user_id` (`system/webhook`).

## Note per il revisore

- **Contratto cross-area**: il payload webhook firmato è il contratto tra `StubScenarioEmitter` e il consumer; il Fake Paddle.js
  (`@appgrove/paddle-stub`) definisce l'interfaccia `PaddleClient` che il loader Paddle.js reale (UC 0024) dovrà rispettare. Non
  ancora montato nella SPA (la schermata checkout è UC 0024) → nessuna modifica all'App Registry/avvio locale.
- **Avvio locale**: il consumer gira dentro il processo `core` esistente (nessun nuovo processo → `app-start.sh`/`Caddyfile`
  invariati); la coda `paddle-webhooks` è già in `dev/elasticmq.conf`. In `%dev` il poller drena ogni 2s; senza ElasticMQ logga in
  debug, niente crash.
- **`api-client/schema.ts` non rigenerato**: l'ingest webhook è server-to-server, non un'API consumata dalla SPA → volutamente
  fuori dal client tipizzato.
- **Decisioni differite tracciate** (regola costituzionale): UC 0025 (entità `Subscription` spostata a 0023 + ri-scopo hardening),
  UC 0023 (PaddlePaymentProvider reale per-metodo a 0024/0028/0022; rigore L1; secret per-env), UC 0022 (mapping
  `paddle_price_id`→`app_tier_id`).
- **Dati personali**: nessuno (dati sintetici, nessun dato di pagamento — MoR). Nessun checkpoint privacy/RoPA.

## Test

- **`services/core` (`mvn test`)**: **41 verdi** (di cui nuovi: `SubscriptionPipelineTest` 6 — scenari→stato, idempotenza,
  isolamento tenant, cambio tier su upgrade; `PaddleWebhookIngestTest` 3 — firma valida 200 / errata|assente 401 senza
  processing; `PaymentProviderTest` 2 — stub ID plausibili/distinti). `ArchitectureTest` resta verde (l'entità rispetta gli
  invarianti). Testcontainers Postgres 17.
- **`frontend` (`npm test`)**: tutti i workspace verdi, incluso `@appgrove/paddle-stub` (4 test: emissione asincrona di
  `checkout.completed`, propagazione `custom_data`, ID distinti, assenza callback) + typecheck `tsc --noEmit` ok.

## Stato criteri di accettazione

- [x] Port `PaymentProvider` (stub in `%dev`/test, `PaddlePaymentProvider` placeholder in prod).
- [x] Entità JPA `Subscription` + repository tenant-scoped sul DDL esistente; nessuna nuova migrazione.
- [x] `POST /webhooks/paddle`: firma valida → 200 + accoda; errata → 401 e nessuna scrittura.
- [x] Consumer idempotente per `(tenant_id, app_id)` dai `custom_data`; re-delivery convergente (count = 1).
- [x] Scenari lifecycle (happy/past_due/canceled/upgrade/downgrade) attivabili offline e verificati su `subscription`.
- [x] Pacchetto Fake Paddle.js (frontend) con interfaccia overlay + `checkout.completed` sintetico, coperto da test.
- [x] Logging strutturato (`tenant_id`/`app_id`/`user_id`) su ingest e consumer.
- [x] `mvn test` (core) e `npm test` (frontend) verdi.
