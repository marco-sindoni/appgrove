# Change 0024: Portale cliente & gestione abbonamento self-service

**Branch**: `change/0024-use-case-0028-portale-cliente-self-service`
**Aree**: `services/core` (backend), `services/commons` (DTO/provider condivisi), `frontend` (backoffice SPA)
**Data**: 2026-06-29
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/07-payments/0028-portale-cliente-self-service.md](../../docs/usecases/07-payments/0028-portale-cliente-self-service.md)
**Tocca dati personali?**: Sì (lieve) — espone/gestisce dati di abbonamento già esistenti (categoria *billing/abbonamento = base contratto*, già nel manifesto) e apre la sessione Customer Portal di Paddle (titolare autonomo per carta/fatture). **Nessuna nuova categoria di dato personale** viene introdotta o raccolta → checkpoint privacy/RoPA di step-03 leggero.

## Problema / Obiettivo

Dare all'utente owner la **gestione self-service dell'abbonamento** con l'approccio **ibrido** deciso in #09 G33: nel nostro backoffice ciò che controlliamo e si lega a tier/quota (vedi/upgrade/downgrade/disdici/riattiva, limiti del piano); delega a **Paddle** ciò che è intrinsecamente suo (metodo di pagamento PCI + fatture MoR) tramite una sessione **Customer Portal** generata server-side. Si chiude inoltre il punto aperto ereditato da UC 0027: i **banner azionabili di enforcement** (402/429) che traducono i gate backend in azioni (upgrade/riattiva/esporta dati).

## Scope

### Backend (`services/core`, `services/commons`)

1. **Persistenza cambio tier schedulato** (punto aperto di cui UC 0028 è owner):
   - nuove colonne `subscription.scheduled_tier_id` (UUID, nullable) + `scheduled_change_at` (timestamptz, nullable) via nuova migration Flyway;
   - il consumer webhook ([SubscriptionWriter](../../services/core/src/main/java/app/appgrove/core/billing/SubscriptionWriter.java)/[PaddleWebhookEvent](../../services/core/src/main/java/app/appgrove/core/billing/PaddleWebhookEvent.java)) mappa il downgrade schedulato dall'evento `subscription.updated` (popola/azzera le due colonne); upsert idempotente e out-of-order-safe come l'esistente.

2. **Read-model dedicato `GET /api/platform/v1/me/subscriptions`** (OWNER, tenant dal JWT):
   - elenca **tutte** le subscription del tenant — incluse non-attive (canceled/paused/scadute) — con: `appSlug`, `status`, `tierKey` corrente, `currentPeriodStart/End`, `cancelAt`, `trialEnd`, cambio schedulato (`scheduledTierKey` + `scheduledChangeAt`), `phase` (da [SubscriptionLifecycle](../../services/core/src/main/java/app/appgrove/core/billing/SubscriptionLifecycle.java)), `limits` del tier corrente, e flag azionabili (`canUpgrade`/`canDowngrade`/`canCancel`/`canResume`/`portalAvailable`);
   - distinto da `/me/entitlements` (che resta il read-model del gate e della sidebar).

3. **Endpoint di mutazione abbonamento** (OWNER, tenant dal JWT, tenant-scoped), modello **command → provider → webhook → read-model** (nessuna scrittura diretta della `subscription` dall'endpoint):
   - `POST /me/subscriptions/{appSlug}/change-tier` (body `{ targetTierKey, billingCycle? }`): valida ruolo + gating flow/stock (E23) + semantica tier; **upgrade = immediato**, **downgrade = schedulato a fine periodo**; chiama il provider;
   - `POST /me/subscriptions/{appSlug}/cancel` (disdetta a fine periodo, `cancel_at`) e `POST /me/subscriptions/{appSlug}/resume` (annulla disdetta) — E25;
   - tutte chiamano nuove operazioni del `PaymentProvider`; lo **StubPaymentProvider** emette il webhook corrispondente (immediato vs schedulato) così la pipeline UC 0025 aggiorna lo stato.

4. **Customer Portal Paddle server-side**:
   - nuovo metodo `PaymentProvider.generateCustomerPortalSession(...)` → restituisce un URL/sessione; stub locale ritorna un URL fittizio, impl Paddle reale chiama l'API (placeholder, gated #14);
   - endpoint `POST /api/platform/v1/me/portal-session` (OWNER) che lo genera per il `paddleCustomerId` del tenant.

5. **Gating downgrade stock** (E23): rifiuto azionabile (con remediation) se il downgrade verso un tier con cap `stock` inferiore allo stato corrente — risposta problem+json.

### Frontend (`frontend/apps/backoffice`)

6. **Pagina Billing** (route `/billing`, già presente in sidebar ma senza pagina): pannello abbonamenti che consuma `/me/subscriptions` — lista app con status/tier/fine periodo/cambio schedulato; azioni **upgrade/downgrade/disdici/riattiva** (con conferme per azioni a impatto); **banner limiti piano** (cap/natura/finestra dal read-model); pulsante **"Gestisci pagamento e fatture"** che apre la sessione portal Paddle; stati loading/empty/error.
7. **Schermata subscription scaduta**: offre **riattiva** (reale) + CTA **"Esporta / elimina i tuoi dati"** che rimanda ai diritti GDPR (route placeholder UC 0033, es. `/account/privacy`).
8. **Banner azionabili di enforcement** (chiude il punto aperto di UC 0027): interceptor sugli status **402** (entitlement/stato scaduto → "riattiva oppure esporta/elimina i dati") e **429** (quota → "limite raggiunto, fai upgrade") nel client API ([apiClient.tsx](../../frontend/apps/backoffice/src/api/apiClient.tsx)); il banner mappa il `type` del problem+json al CTA giusto (link a upgrade/riattiva nel pannello, o ai diritti GDPR).
9. **API client billing**: hook per `/me/subscriptions`, le mutazioni, la portal-session; polling/refetch post-azione come il checkout (UC 0024).

## Fuori scope

- **Consumo quota in tempo reale** (es. "73/100 usate"): mostrato solo come **limiti del piano**; il consumo reale richiede un contratto usage per-app → **differito** (vedi Punti aperti UC 0028).
- **Motore reale di export/cancellazione GDPR**: solo CTA/link; implementazione → **UC 0033** (vedi Punti aperti UC 0028).
- Semantica lifecycle (UC 0026), enforcement backend gate (UC 0027 — già fatto), checkout iniziale (UC 0024), pannello admin (UC 0021).
- Impl reale Paddle del portal/cambi (gated #14): solo metodo + stub locale; il path reale resta placeholder.
- Modifica prezzi/tier a runtime (pricing-as-code, #09 H — fuori scope).

## Criteri di accettazione

- [ ] `GET /me/subscriptions` ritorna **tutte** le subscription del tenant (anche scadute/cancellate), con cambio schedulato e flag azionabili, **filtrato per tenant dal JWT**.
- [ ] Upgrade applicato **immediato**, downgrade **schedulato a fine periodo** (colonne `scheduled_tier_id`/`scheduled_change_at` popolate via webhook); downgrade stock oltre capacità → **bloccato + remediation**.
- [ ] Disdici imposta `cancel_at` a fine periodo; riattiva la annulla — entrambe via provider→webhook.
- [ ] Pulsante "Gestisci pagamento e fatture" apre la sessione portal **generata server-side** (mockata in dev).
- [ ] Pannello Billing mostra abbonamenti/limiti/azioni; schermata scaduta offre riattiva + CTA diritti GDPR.
- [ ] Banner enforcement: una 402 e una 429 producono il CTA azionabile corretto.
- [ ] Nessun accesso cross-tenant in nessun endpoint nuovo (security test).

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT**: ogni endpoint nuovo ricava il tenant da [CallerContext.tenantId()](../../services/core/src/main/java/app/appgrove/core/platform/CallerContext.java); `appSlug` dal path serve solo a risolvere l'app, mai il tenant.
- **Filtro row-level `WHERE tenant_id`**: i read/scritture passano da `SubscriptionRepository` (Panache `@SQLRestriction` tenant-scoped); il cambio schedulato dal webhook resta vincolato al tenant del payload firmato.
- **Modulo `microsaas_app`**: N/A — nessuna nuova app/infra; `core` e `/api/platform/v1/*` già cablati (nessuna modifica a `app-start.sh`/`Caddyfile`/`run-tests.sh`).
- **Logging strutturato**: i nuovi endpoint/azioni loggano con `tenant_id`/`app_id`/`user_id`.
- **Webhook = fonte di verità**: le mutazioni non scrivono direttamente la `subscription`; passano da provider→webhook→read-model (coerente con UC 0024/0025).

## Requisiti di test

- **Backend (`mvn test`)**: unit su mapping cambio schedulato (webhook → colonne), su gating downgrade stock (blocco), su lifecycle/flag azionabili del read-model; **security** che `/me/subscriptions` e le mutazioni non vedano/tocchino altri tenant (cross-tenant negato); integration sul giro command→stub→webhook→read-model.
- **Frontend (`npm test` / Playwright)**: unit sulla mappa problem+json→CTA del banner enforcement (402/429) e sulla logica di rendering del pannello; **E2E** vedi/upgrade/downgrade/disdici/riattiva + apertura portal mockata (pattern di [checkout.spec.ts](../../frontend/apps/backoffice/e2e/checkout.spec.ts)). Baseline visiva: non ri-registrare snapshot alla cieca.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (solo aggiunte: colonne nullable, nuovi endpoint, nuova pagina) |
| Contratto cross-area | Sì — frontend ↔ API `core` (nuovi endpoint `/me/subscriptions`, mutazioni, portal-session) |
| Version bump | minor |
