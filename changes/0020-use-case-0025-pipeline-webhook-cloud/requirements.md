# Change 0020: Hardening pipeline webhook Paddle — dedup, out-of-order, set eventi completo, firma reale, DLQ-handling (L1 esaustivo)

**Branch**: `change/0020-use-case-0025-pipeline-webhook-cloud`
**Aree**: `services/core` (backend)
**Data**: 2026-06-28
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/07-payments/0025-pipeline-webhook.md](../../docs/usecases/07-payments/0025-pipeline-webhook.md)
**Tocca dati personali?**: No — payload 100% sintetici (L1, seed UC 0011); nessun dato di pagamento (MoR Paddle). L'unico campo "personale" toccato è `accounts.paddle_customer_id` (identificativo opaco lato Paddle, già esistente in V2, base contratto billing) — nessuna nuova categoria di dato, nessun dato carta. Manifest/RoPA: nessuna variazione → nessun checkpoint privacy/RoPA.

## Problema / Obiettivo

Irrobustire la **pipeline di ingest dei webhook Paddle** (creata a rigore **Minimo** in 0018/UC 0023) fino a chiudere
l'**L1 esaustivo bloccante** (#09 D20): firma reale `ts/h1` + anti-replay, **dedup su `event_id`**, **out-of-order via
`occurred_at`**, **set di eventi completo** mappato su `subscription` (+ `accounts.paddle_customer_id`), e **comportamento
corretto di redrive → DLQ** a livello consumer. `subscription` resta l'**unica fonte di verità** del billing; l'entitlement
è derivato (nessuna tabella).

**Confine deciso (gate di chiarimento, 2026-06-28):** questa change copre **solo l'hardening del livello applicativo**
(`services/core`), interamente verificabile in locale (Testcontainers Postgres + ElasticMQ/in-memory). Il **packaging cloud**
(Lambda ingest @ API GW, Terraform, Secrets Manager wiring, allarmi CloudWatch sulla DLQ) è **differito e tracciato** in
UC 0025 "Punti aperti": non costruibile ora perché `infra/` non ha alcun `.tf`, il modulo `microsaas_app` (invariante #3) non
esiste, e l'indice di esecuzione colloca le fondamenta Terraform (0003/0004/0006/0055) **dopo** il cluster pagamenti —
costruirle a mano violerebbe l'invariante #3 e `phased-env-activation`. `WebhookIngestService` + `PaddleWebhookResource`
**sono già** l'equivalente locale della Lambda (HMAC → enqueue → 200): in cloud quel codice diventerà il corpo della Lambda.

## Scope

Tutto in **`services/core`**, package `app.appgrove.core.billing` (+ nuova migrazione Flyway).

**1. Firma HMAC reale (`ts/h1`) + anti-replay** — `PaddleSignature`:
- Formato header `Paddle-Signature: ts=<unix>;h1=<hmac-sha256(ts + ":" + body)>` (al posto dell'hex puro attuale); parsing
  robusto di `ts`/`h1`, confronto a tempo costante (invariato).
- **Finestra anti-replay** configurabile (`appgrove.payments.webhook-max-age`, default 5 min): `ts` fuori finestra → firma
  rifiutata (401). In `%dev`/test la finestra è disattivabile/larga per i payload deterministici.
- Lo **stub-emettitore** (`StubScenarioEmitter`) e i test producono firme nello **stesso** formato (coerenza garantita dal
  riuso di `PaddleSignature`).

**2. Dedup su `event_id` (autoritativo nel consumer, transazionale)** — nuova tabella + logica:
- Nuova migrazione `V3__webhook_events.sql`: tabella `platform.webhook_event` (`event_id` **unique**, `event_type`,
  `occurred_at`, `processed_at`, `outcome` `processed|skipped_stale|failed`, audit). Non tenant-scoped (è log di pipeline).
- Il consumer, **nella stessa transazione** in cui applica l'evento, esegue `INSERT INTO webhook_event(event_id…)`: violazione
  del unique constraint → evento già processato → **no-op** e conferma del messaggio (idempotenza corretta anche sotto
  redelivery/concorrenza). Eventuale pre-filtro all'ingest resta ottimizzazione futura (cloud), non necessario alla correttezza.

**3. Out-of-order via `occurred_at`** — colonna + guardia:
- Migrazione: `ALTER TABLE platform.subscription ADD COLUMN last_event_occurred_at timestamptz`.
- Il consumer applica la **mutazione di `subscription`** solo se `event.occurred_at >= subscription.last_event_occurred_at`;
  evento più vecchio → **non muta** lo stato ma **registra comunque** l'evento in `webhook_event` (`outcome=skipped_stale`),
  così resta dedotto e non viene rilavorato.

**4. Set di eventi completo** mappato su `subscription` (e `accounts`):
- `subscription.created` (upsert + linkage `(tenant_id, app_id)` dai `custom_data` firmati), `.activated`, `.updated`
  (catch-all: status / tier via `app_tier_id` / period / `cancel_at` / `past_due`), `.canceled`, **`.paused`**, **`.resumed`**.
- **`transaction.completed`** (attivazione + **rinnovi**: avanza `current_period_end`), **`transaction.payment_failed`** (→ `past_due`).
- **`chargeback`/`dispute`** (→ `past_due`/sospeso; reazione MoR #09 J42).
- **`customer.created`/`.updated`** → scrive `accounts.paddle_customer_id` (colonna **già esistente** in V2; nessuna migrazione).
- Eventi non sottoscritti → ignorati (registrati `processed` no-op, meno rumore/superficie).
- **Solo mapping evento → stato.** La **semantica** lifecycle (dunning/grace, accesso fino a fine periodo, gating downgrade
  `flow`/`stock`) è **UC 0026**; l'**enforcement** entitlement/quota è **UC 0027** — fuori scope, tracciati a valle.

**5. Redrive/DLQ a livello consumer:**
- Su fallimento di elaborazione il consumer **non conferma/elimina** il messaggio → la coda lo ri-consegna; dopo
  `maxReceiveCount` (ElasticMQ già a 5) → **DLQ** (config già in `dev/elasticmq.conf`). Log strutturato dell'errore
  (`tenant_id`/`app_id`) come aggancio osservabilità locale.
- `InMemoryWebhookQueue` (test) arricchita per **simulare receive-count + DLQ**, così l'L1 può asserire "messaggio velenoso →
  finisce in DLQ, non perso né rilavorato all'infinito".
- L'**allarme CloudWatch** sulla DLQ è **differito al cloud** (UC 0025 "Punti aperti").

## Fuori scope (tutto tracciato in UC 0025 "Punti aperti")

- **Packaging cloud**: Lambda ingest @ API Gateway, packaging **Terraform**, secret per-env in **Secrets Manager**, **allarme
  CloudWatch** sulla DLQ → **differito** (gated su fondamenta infra 0003/0004/0006/0055, `phased-env-activation`).
- **Semantica del ciclo di vita** (dunning/grace, accesso fino a fine periodo, gating downgrade `flow`/`stock`) → **UC 0026**.
- **Enforcement entitlement + quota SPI** (gate 402/429, derivazione entitlement a runtime) → **UC 0027**.
- **Checkout / overlay / UX polling** (impostazione `custom_data` server-side, `paddle_customer_id` lazy) → **UC 0024**.
- **`PaddlePaymentProvider` reale** verso Paddle (sandbox/prod) → gated #14, riempito per-metodo dagli UC consumatori.
- **L3 smoke reale** contro Paddle sandbox, **tunnel cloudflared** opt-in → **UC 0029**.
- Validazione del **contratto di firma reale** contro il vero Paddle (formato/segreto definitivi) → L3 (UC 0029), gated #14.

## Criteri di accettazione

- [ ] `PaddleSignature` verifica il formato reale `ts=…;h1=…` con **anti-replay** (ts fuori finestra → 401); stub e test usano lo stesso formato.
- [ ] Migrazione `V3`: tabella `platform.webhook_event` (`event_id` unique, audit) + colonna `subscription.last_event_occurred_at`. `mvn test` applica V3 su Postgres reale (Testcontainers).
- [ ] **Dedup**: lo stesso `event_id` consegnato due volte → applicato **una sola volta** (no-op sul duplicato, `outcome` tracciato).
- [ ] **Out-of-order**: un evento con `occurred_at` più vecchio dello stato corrente **non** sovrascrive `subscription` (`outcome=skipped_stale`), ma è dedotto.
- [ ] **Set eventi completo**: created/activated/updated/canceled/paused/resumed, transaction.completed (avanza periodo)/payment_failed, chargeback/dispute → `past_due`/sospeso, customer.created/updated → `accounts.paddle_customer_id`.
- [ ] **Redrive/DLQ**: un messaggio che fallisce ripetutamente finisce in **DLQ** (simulata in test) e **non** viene perso né processato all'infinito; errore loggato strutturato.
- [ ] Firma errata → **401** e **nessuna scrittura** (regression guard 0018 mantenuto); nessun cross-tenant.
- [ ] Logging strutturato (`tenant_id`/`app_id`/`user_id="system/webhook"`) su ingest e consumer.
- [ ] `./run-tests.sh backend` verde (core).
- [ ] UC 0025 "Punti aperti" aggiornata con **tutti** i task differiti (packaging cloud, semantica 0026/0027, validazione firma L3); indice esecuzione `_INDEX.md` 0025 → ✅.

## Invarianti appgrove toccati

- **Tenant ID non manomettibile dal client**: il linkage `(tenant_id, app_id)` viene dai `custom_data` **dentro il payload
  firmato HMAC** (canale server-to-server firmato, eccezione esplicita all'authorizer #09 D / UC 0025 §8). L'anti-replay
  rafforza la non-falsificabilità. Coerente con l'invariante #1 nello spirito.
- **Filtro row-level**: `Subscription` resta `BaseTenantEntity` (`@TenantId`); il consumer scrive via `SubscriptionWriter` con
  tenant **esplicito dal payload firmato** (gira fuori da richiesta autenticata). `webhook_event` è log di pipeline non
  tenant-scoped (nessun dato tenant sensibile; dedup globale per `event_id`).
- **Modulo Terraform `microsaas_app`**: N/A in questa change (packaging cloud differito) — anzi, è **proprio** il rispetto di
  questo invariante a imporre il differimento (niente infra bespoke prima del modulo).
- **Logging strutturato**: ingest, consumer e percorso d'errore/DLQ popolano MDC con `tenant_id`/`app_id` (+ `user_id="system/webhook"`).

## Requisiti di test (L1 esaustivo — #09 D20, BLOCCANTE)

- **Firma**: valida `ts/h1` → 200; **errata** → 401 nessuna scrittura; **replay** (ts fuori finestra) → 401.
- **Idempotenza/dedup**: stesso `event_id` ×2 → una sola applicazione; `webhook_event` registra il duplicato come no-op.
- **Out-of-order**: sequenza con `occurred_at` decrescente → lo stato non regredisce (`skipped_stale`).
- **Ogni evento**: un test per ciascun tipo del set completo verifica la transizione attesa di `subscription` (e `customer.*` →
  `accounts.paddle_customer_id`).
- **Linkage tenant / no cross-tenant**: il tenant deriva dai `custom_data` firmati; firma non valida non scrive nulla.
- **DLQ/redrive**: messaggio velenoso → ri-consegna fino a `maxReceiveCount` → DLQ; non perso, non in loop.
- Testcontainers Postgres reale; nessuna rete esterna; deterministico.

## Scelte tecniche (dettaglio in step-03)

- Dedup **autoritativo nel consumer** in **una transazione** con l'upsert di `subscription` (constraint unique su `event_id`).
- Out-of-order via confronto `occurred_at` ≥ `subscription.last_event_occurred_at`.
- DLQ a livello coda (ElasticMQ/SQS redrive); `InMemoryWebhookQueue` estesa con receive-count + lista DLQ per i test.
- Secret HMAC e finestra anti-replay da config (`%dev` valori locali; per-env in Secrets Manager → differito cloud).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (formato firma cambia, ma stub+test allineati nello stesso commit; nessun consumatore esterno reale ancora) |
| Contratto cross-area | Sì — payload webhook firmato (`ts/h1`) ↔ consumer; nessun contratto frontend/infra in questa change |
| Version bump | minor |
