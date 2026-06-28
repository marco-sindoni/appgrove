# UC 0025 — Pipeline webhook (Lambda ingest HMAC+dedup → SQS → consumer idempotente, out-of-order)

**Area**: 07-payments · **Fase**: 5 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0013](../04-platform-core/0013-account-utenti-inviti-api.md) (subscription), UC [0023](0023-stub-paddle-locale.md) (webhook sintetici)
**Fonte decisioni**: #09 D (webhook & source of truth), #06 H (SQS/DLQ), #04 (consumer billing)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [09-pagamenti](../../09-pagamenti.md), [06-infra-iac](../../06-infra-iac.md), [04-services-backend](../../04-services-backend.md)

## 1. Obiettivo / Scope
Implementare la **pipeline di ingest dei webhook Paddle**, source of truth dello stato di billing su `subscription`.
**Incluso**: **Lambda di ingest** (verifica **firma HMAC** `Paddle-Signature`, **dedup su `event_id`**, accoda su **SQS**,
risponde 200 subito); **consumer** idempotente che aggiorna `subscription` con gestione **out-of-order** (`occurred_at`); **retry
+ DLQ + allarme** (#08); set di eventi sottoscritti mappato su `subscription`.
**Escluso**: il lifecycle/semantica stati (UC 0026), l'enforcement entitlement (UC 0027), il checkout (UC 0024); lo stub che genera i webhook in dev (UC 0023).

## 2. Attori & ruoli
- **Paddle**: invia webhook (ri-invia su mancato 200).
- **Lambda ingest**: verifica/dedup/accoda.
- **Consumer** (capability billing core): applica idempotente a `subscription`.

## 3. Precondizioni
- Core con `subscription` (UC 0013); SQS+DLQ (UC 0004/0011 infra); webhook signing secret in Secrets Manager (#09 I38); in dev i webhook arrivano dallo stub (UC 0023).

## 4. Flusso principale
1. **Ingest** (API GW → Lambda): verifica **firma HMAC**; firma non valida → **401**, niente processing (#09 D18a).
2. **Dedup su `event_id`**: Paddle ri-invia → applicato una volta sola (#09 D18b).
3. Accoda su **SQS** e risponde **200** subito (disaccoppia ricezione da elaborazione; utile col core scale-to-0 in test) (#09 D19).
4. **Consumer** legge da SQS → aggiorna `subscription` in modo **idempotente**, con **out-of-order** via `occurred_at` (un evento più vecchio non sovrascrive uno più recente) (#09 D18c/D19).
5. **Set eventi** → `subscription`: created (linkage tenant/app via custom_data), activated, **updated** (catch-all: status/price/period/cancel_at/past_due), canceled, paused/resumed, transaction.completed (attivazione+rinnovi), payment_failed (→dunning), **chargeback/dispute** (`transaction.payment_failed`/dispute → reagiamo via webhook: stato subscription `past_due`/sospeso, divisione responsabilità MoR #09 J42), customer.created/updated (paddle_customer_id) (#09 D21).
6. **Retry + DLQ + allarme** sui fallimenti (#09 D19, #08).

## 5. Flussi alternativi / edge / errori
- **Firma non valida** → 401, scartato (#09 D18a).
- **Evento out-of-order** → ignorato se più vecchio dello stato corrente (`occurred_at`) (#09 D18c).
- **Duplicato** → no-op (dedup) (#09 D18b).
- **Elaborazione fallita** → retry → DLQ + allarme (#09 D19).
- **Eventi non sottoscritti** → ignorati (meno rumore/superficie) (#09 D21).

## 6. Risorse & runbook
**Risorse**: Lambda ingest (API GW), coda **SQS webhook + DLQ** (#06 19bis), consumer (core). **Runbook**: in dev i webhook
sintetici firmati (UC 0023) passano per la **stessa** pipeline (ElasticMQ); osservabilità via log+allarmi DLQ (#08). **Secret**:
signing secret per env in Secrets Manager.

## 7. Dati toccati
Scrive **solo** `subscription` (unica fonte di verità billing); `accounts.paddle_customer_id` da customer.* (#09 B/D). Nessun
dato carta (MoR). Manifest: trattamento billing/abbonamento (base contratto); fiscale in capo a Paddle. No-PII nei log (#08 5).

## 8. Permessi & gate
- **Invarianti**: il linkage tenant viene dai `custom_data` impostati server-side al checkout (UC 0024) → `subscription` per
  (tenant, app) corretta; entitlement **derivato** da `subscription` (nessuna tabella, #09 dec.12).
- Endpoint webhook fuori dall'authorizer di business (è ingest pubblico firmato HMAC).

## 9. Requisiti di test
- **L1 (esaustivo, per-PR, BLOCCANTE, #09 D20)**: payload sintetici firmati → evoluzione `subscription`; copre firma valida/errata,
  idempotenza, out-of-order, ogni evento, linkage tenant, derivazione entitlement, enforcement quota; Testcontainers Postgres.
- **Security**: firma errata → 401; nessun cross-tenant. **DLQ/allarme** verificati.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #09 D18/D19/D20/D21, J42 (chargeback/dispute via webhook), B12, I38, #06 19bis, #08.
- **DoD**:
  1. Lambda ingest (HMAC + dedup `event_id` → SQS, 200 subito).
  2. Consumer idempotente con out-of-order (`occurred_at`); set eventi mappato su `subscription`.
  3. Retry+DLQ+allarme; `subscription` unica fonte di verità (entitlement derivato).
  4. L1 esaustivo verde (firma/idempotenza/out-of-order/eventi/linkage).

## Punti aperti / decisioni differite

_Tracciato dalla change `0007-use-case-0013-…` (regola CLAUDE.md "Tracciamento delle decisioni differite")._

- **Entità JPA `subscription` — SPOSTATA a UC 0023 (change `0018`).** ~~Va modellata qui~~. Il gate di chiarimento della change
  `0018-use-case-0023-…` ha scelto **Approach A**: l'entità JPA `Subscription` + repository (tenant-scoped, `@TenantId`) e un
  **consumer locale minimo** (HMAC + idempotenza) **nascono in 0018**, perché lo stub 0023 deve mutare davvero `subscription`
  offline (DoD 0023). UC 0027 la legge per l'entitlement derivato. **Nuovo proprietario dell'entità**: UC 0023 (change 0018).
- **Ri-scopo di UC 0025 → hardening cloud/prod (residuo dopo la pipeline locale minima di 0018).** Restano a UC 0025:
  (a) **Lambda ingest** dietro API GW + packaging **Terraform**; (b) **dedup su `event_id`** (richiede una migrazione: tabella
  `webhook_event` o colonna dedicata — il DDL attuale non ce l'ha); (c) **out-of-order via `occurred_at`** (idem, colonna da
  aggiungere); (d) **DLQ + allarmi** (#08); (e) set eventi completo. Il consumer minimo di 0018 va **irrobustito**, non riscritto.
  Lo **stub-emettitore** (0018) sa già generare firma errata/duplicato/out-of-order: qui si completa la **gestione** lato consumer
  per chiudere l'**L1 esaustivo**. **Proprietario**: UC 0025.

### Stato dopo la change `0020-use-case-0025-…` (hardening APPLICATIVO completato)

La change **0020** ha implementato in `services/core` (tutto verificabile in locale, L1 esaustivo verde):
**firma reale `ts=…;h1=…` + anti-replay**; **dedup su `event_id`** (migrazione `V3`, tabella `platform.webhook_event`, claim
transazionale autoritativo nel consumer); **out-of-order via `occurred_at`** (colonna `subscription.last_event_occurred_at`,
guardia nel `DO UPDATE`); **set eventi completo** (created/activated/updated/canceled/paused/resumed, transaction.completed→rinnovo,
payment_failed/disputed→`past_due`, customer.*→`accounts.paddle_customer_id`) con mappatura autoritativa lato consumer
(`WebhookEventMapping`); **redrive→DLQ** a livello consumer (non-ack su errore) + simulazione DLQ nei test. **L1**: `WebhookSignatureTest`,
`WebhookHardeningTest`, `PaddleWebhookIngestTest`, `SubscriptionPipelineTest`.

**Resta differito (proprietà UC 0025) — packaging CLOUD, gated sulle fondamenta infra (0003/0004/0006/0055), `phased-env-activation`:**
- **Lambda di ingest** dietro **API Gateway** + **packaging Terraform** (istanziando il modulo `microsaas_app`, invariante #3 —
  oggi `infra/` non ha `.tf`). Il codice locale `WebhookIngestService`/`PaddleWebhookResource` è già il corpo della futura Lambda.
- **Secret per-ambiente in Secrets Manager** (signing secret + `webhook-max-age`): oggi in config `%dev` con valore di test (#09 I38).
- **DLQ reale (SQS) + allarme CloudWatch** sulla profondità DLQ (#08): oggi la DLQ è ElasticMQ (`dev/elasticmq.conf`, maxReceiveCount=5)
  e l'allarme è solo un log strutturato d'errore lato consumer.
- **Dedup best-effort all'ingest (pre-filtro nella Lambda)**: ottimizzazione cloud; la correttezza è già garantita dal claim
  transazionale nel consumer.

**Differito ad altri UC (tracciato qui per non perderlo):**
- **Semantica del ciclo di vita** (dunning/grace, accesso fino a fine periodo, suspend-vs-`past_due` su chargeback, gating
  downgrade `flow`/`stock`) → **UC 0026**. La change 0020 porta solo `subscription` allo **stato** corretto.
- **Enforcement entitlement + quota SPI** (derivazione entitlement, gate 402/429) → **UC 0027**.
- **Validazione del contratto firma/eventi reali Paddle** (formato `ts/h1` definitivo, secret reali, shape eventi) → **L3 smoke**,
  **UC 0029** (gated #14 + account attivato). Qui è validato solo contro payload sintetici.
- **Creazione lazy del customer + `custom_data` server-side al checkout** (l'altra metà di `paddle_customer_id`) → **UC 0024**.
