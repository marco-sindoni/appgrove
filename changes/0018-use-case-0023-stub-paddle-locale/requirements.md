# Change 0018: Stub Paddle locale (port `PaymentProvider`, webhook sintetici firmati, pipeline locale minima)

**Branch**: `change/0018-use-case-0023-stub-paddle-locale`
**Aree**: `services/core`, `frontend` (config `dev/` in core `%dev`)
**Data**: 2026-06-27
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/07-payments/0023-stub-paddle-locale.md](../../docs/usecases/07-payments/0023-stub-paddle-locale.md)
**Tocca dati personali?**: No — dati 100% sintetici (seed UC 0011); nessun dato di pagamento reale (MoR). Manifest N/A (ambiente dev/test). Niente checkpoint privacy/RoPA.

## Problema / Obiettivo

Far girare **offline e in modo deterministico** l'intero flusso pagamenti senza account Paddle (unica via non bloccata da #14),
dietro la **stessa interfaccia (port)** del provider reale. È l'abilitatore dei test **L1/L2 offline** (#10 L, #09 D20).

**Confine deciso (gate di chiarimento):** **Approach A** — costruiamo lo stub *e* la **pipeline locale minima** che lo rende
auto-dimostrabile (entità `Subscription` + consumer idempotente), con consumer a rigore **Minimo** (HMAC + idempotenza). Il
rigore L1 esaustivo (dedup `event_id`, out-of-order `occurred_at`, DLQ/allarmi) e il packaging Lambda/API GW restano a **UC 0025**
(tracciati sotto). "Pipeline reale" della DoD 0023 = *stesso percorso di codice su ElasticMQ*, non un mock separato; la pipeline
**prod** (Lambda/infra) è esplicitamente esclusa da 0023.

## Scope

**Backend (`services/core`, nuovo package `app.appgrove.core.billing`):**
- **Port `PaymentProvider`** (interfaccia): l'astrazione che il codice di prodotto usa. Selezione per profilo: `%dev` →
  `StubPaymentProvider` (default); test/prod → `PaddlePaymentProvider` reale, qui **placeholder non implementato** (gated #14,
  riempito per-metodo dagli UC consumatori — tracciato sotto).
- **`StubPaymentProvider`**: **API Paddle finta** (ritorna ID plausibili `paddle_customer_id`/`paddle_transaction_id`/
  `paddle_subscription_id`) + **emettitore di webhook sintetici firmati** (HMAC con secret di test) + **libreria scenari**
  (happy path, payment_failed/past_due, canceled, upgrade/downgrade), inclusa la capacità di generare **casi limite**
  (firma errata, duplicato, out-of-order) per L1.
- **Endpoint ingest webhook** `POST /api/platform/v1/webhooks/paddle` (pubblico, **fuori authorizer** di business): verifica la
  **firma HMAC** dell'header `Paddle-Signature` → firma errata = **401**, niente processing; firma valida → accoda su ElasticMQ
  `paddle-webhooks` e risponde **200** subito.
- **Consumer** (poller SQS su ElasticMQ): legge da `paddle-webhooks`, applica gli eventi a `subscription` in modo **idempotente**
  per `(tenant_id, app_id)` letti dai `custom_data` del payload firmato. Set eventi mappati su `subscription` (created/activated/
  updated/canceled/transaction.completed/payment_failed) limitato a quanto serve agli scenari.
- **Entità JPA `Subscription` + repository** (tenant-scoped, `@TenantId` su `BaseTenantEntity`), mappate sul **DDL esistente**
  (`V2__core_domain.sql`, nessuna nuova migrazione). *(Nasce qui per Approach A; era assegnata a UC 0025 — vedi tracce.)*
- **Endpoint dev-only** per attivare uno scenario (`%dev`): es. `POST /api/platform/v1/dev/paddle/scenarios/{scenario}` che fa
  emettere allo stub i webhook firmati del lifecycle scelto.

**Frontend (`frontend/packages/`):**
- **Fake Paddle.js**: pacchetto che espone la **stessa interfaccia** dell'overlay Paddle.js ed emette un `checkout.completed`
  **sintetico** (solo UX). Nessun montaggio nella SPA in questa change (la schermata checkout è UC 0024); qui solo il pacchetto
  + i suoi test.

**Wiring locale:** lo stub gira **dentro il processo `core` esistente** via profilo `%dev` (nessun nuovo processo →
`app-start.sh`/`app-stop.sh` invariati); l'ingest è raggiungibile dalla route Caddy esistente `/api/platform/*` (nessuna modifica
a `dev/Caddyfile`); la coda `paddle-webhooks` + DLQ è già in `dev/elasticmq.conf`. Config endpoint ElasticMQ nel client SQS in
`application.properties` `%dev` (`http://localhost:9324`).

## Fuori scope

- **`PaddlePaymentProvider` reale** verso Paddle (sandbox/prod) — gated #14, riempito per-metodo da UC 0024/0028/0022 (tracciato).
- **Pipeline webhook prod** (Lambda ingest @ API GW, packaging Terraform) — **UC 0025**.
- **Rigore L1 esaustivo del consumer**: dedup su `event_id`, out-of-order via `occurred_at`, DLQ + allarmi — **UC 0025**.
- **Schermata di checkout / overlay montato nella SPA** e UX polling — **UC 0024**.
- **Entità JPA del catalogo** (`app`/`app_tier`/`app_price`) e mapping `paddle_price_id`→`app_tier_id` — **UC 0022** (lo stub passa
  l'`app_tier_id` target esplicito negli scenari upgrade/downgrade finché 0022 non fornisce il mapping — tracciato).
- Enforcement entitlement/quota (UC 0027), customer portal (UC 0028), L3 smoke reale (UC 0029), tunnel cloudflared opt-in.

## Criteri di accettazione

- [ ] Port `PaymentProvider` in `core`; `StubPaymentProvider` selezionato in `%dev` (default), `PaddlePaymentProvider` placeholder in test/prod.
- [ ] Entità JPA `Subscription` + repository (tenant-scoped via `@TenantId`) sul DDL esistente; nessuna nuova migrazione.
- [ ] `POST /api/platform/v1/webhooks/paddle`: firma HMAC valida → 200 + accoda; firma errata → **401** e **nessuna scrittura** su `subscription`.
- [ ] Consumer idempotente: applica gli scenari a `subscription` per `(tenant_id, app_id)` dai `custom_data`; ri-consegna dello stesso evento = stato convergente (no doppione).
- [ ] Scenari lifecycle (happy/past_due/canceled/upgrade/downgrade) attivabili offline e verificabili sullo stato `subscription`.
- [ ] Pacchetto Fake Paddle.js (frontend) espone l'interfaccia overlay ed emette `checkout.completed` sintetico; coperto da test.
- [ ] Logging strutturato (`tenant_id`/`app_id`/`user_id`) su ingest e consumer.
- [ ] `mvn test` (core) e `npm test` (frontend) verdi.

## Invarianti appgrove toccati

- **Tenant ID non manomettibile dal client**: il linkage tenant viene dai `custom_data={tenant_id, app_id}` **dentro il payload
  firmato HMAC**; l'ingest webhook è un canale server-to-server firmato (eccezione esplicita all'authorizer, #09 D / UC 0025 §8).
  Nessun input client non firmato determina il tenant. Coerente con l'invariante #1 nello spirito (tenant non falsificabile).
- **Filtro row-level**: `Subscription` estende `BaseTenantEntity` (`@TenantId`) → query auto-filtrate `WHERE tenant_id`. Il consumer
  scrive impostando il tenant dal payload firmato.
- **Modulo Terraform `microsaas_app`**: N/A (nessuna infra in questa change).
- **Logging strutturato**: ingest e consumer popolano MDC con `tenant_id`/`app_id` (e `user_id="system/webhook"`).

## Requisiti di test

- **L1-minimo (backend, `mvn test`, Testcontainers Postgres)**: per ogni scenario lifecycle, payload **firmati** → evoluzione di
  `subscription`; copre **firma valida vs errata** (errata → 401, nessuna scrittura), **idempotenza** su re-delivery, **linkage
  tenant** dai `custom_data`. *(Dedup `event_id` / out-of-order `occurred_at` → coperti da UC 0025.)*
- **Regression guard**: un test che verifica che il tenant della `subscription` derivi dal payload firmato e che una firma non
  valida non produca alcuna scrittura (no cross-tenant da input non firmato).
- **Frontend (`npm test`)**: il Fake Paddle.js emette `checkout.completed` sintetico conforme all'interfaccia attesa.

## Scelte tecniche (dettaglio in step-03)

- Client SQS: estensione `quarkus-amazon-sqs` puntata a ElasticMQ in `%dev` (riusata in prod da UC 0025); poller via
  `quarkus-scheduler`. Secret HMAC di test in config `%dev` (per-env in Secrets Manager → UC 0025/infra).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | Sì — Fake Paddle.js ↔ futura checkout UI (UC 0024); payload webhook firmato ↔ consumer. Esterno: N/A |
| Version bump | minor |
