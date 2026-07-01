# Implementation Log — Change 0024: Portale cliente & gestione abbonamento self-service

**Branch**: `change/0024-use-case-0028-portale-cliente-self-service`
**Aree**: `services/core` (backend), `frontend` (backoffice + package i18n/api-client)
**Completata**: 2026-07-01

## File modificati

| File | Azione |
|---|---|
| services/core/.../db/migration/V4__subscription_scheduled_change.sql | Creato — colonne `scheduled_tier_id` + `scheduled_change_at` |
| services/core/.../billing/Subscription.java | Modificato — campi cambio schedulato + `last_event_occurred_at` (read) |
| services/core/.../billing/PaddleWebhookEvent.java | Modificato — parse `scheduled_tier_id`/`scheduled_change_at` |
| services/core/.../billing/SubscriptionWriter.java | Modificato — upsert persiste le 2 colonne |
| services/core/.../billing/StubScenarioEmitter.java | Modificato — emit `subscription.updated` self-service (occurred_at monotòno) |
| services/core/.../billing/PaymentProvider.java | Modificato — `changeSubscriptionTier`/`cancel`/`resume`/`createCustomerPortalSession` |
| services/core/.../billing/StubPaymentProvider.java | Modificato — no-op mutazioni + portal URL stub |
| services/core/.../billing/PaddlePaymentProvider.java | Modificato — placeholder reali (UnsupportedOperationException, gated #14) |
| services/core/.../billing/SubscriptionChangeRequested.java | Creato — evento di dominio (snapshot risultante) |
| services/core/.../billing/StubSubscriptionActivation.java | Creato — osservatore dev che emette il webhook |
| services/core/.../billing/TierChangePolicy.java | Creato — gating puro (direzione + blocco stock) |
| services/core/.../billing/SubscriptionDtos.java | Creato — DTO read-model + richieste |
| services/core/.../billing/SubscriptionReadModel.java | Creato — costruzione `/me/subscriptions` |
| services/core/.../billing/SubscriptionResource.java | Creato — GET + change-tier/cancel/resume |
| services/core/.../billing/PortalResource.java | Creato — POST `/me/portal-session` |
| services/core/.../META-INF/openapi/openapi.{yaml,json} | Rigenerato (build Quarkus) |
| services/core/.../test/.../TierChangePolicyTest.java | Creato — unit gating |
| services/core/.../test/.../SubscriptionSelfServiceTest.java | Creato — integration endpoint |
| frontend/packages/i18n/src/resources/{it,en}.ts | Modificato — blocchi `subscriptions`/`enforcement` |
| frontend/packages/api-client/src/schema.ts | Rigenerato dai tipi OpenAPI |
| frontend/apps/backoffice/src/api/queryClient.ts | Modificato — cache onError → banner enforcement |
| frontend/apps/backoffice/src/billing/{enforcement,enforcementStore,subscriptionsApi,subscriptionsView}.ts | Creati |
| frontend/apps/backoffice/src/billing/{EnforcementBanner,SubscriptionsPanel}.tsx | Creati |
| frontend/apps/backoffice/src/billing/{enforcement,subscriptionsView}.test.ts | Creati |
| frontend/apps/backoffice/src/pages/Billing.tsx | Modificato — pannello self-service sopra il checkout |
| frontend/apps/backoffice/src/shell/ShellLayout.tsx | Modificato — monta `EnforcementBanner` |
| frontend/apps/backoffice/e2e/subscriptions.spec.ts | Creato — E2E L2 |
| docs/usecases/07-payments/0028-*.md | Modificato — Punti aperti (risolti + residui) |
| docs/usecases/_INDEX.md | Modificato — UC 0028 🟡 → ✅ |

## Cosa è stato fatto

Portale cliente self-service ibrido (#09 G33): read-model dedicato `GET /me/subscriptions` (tutte le subscription
del tenant, anche non-attive); mutazioni **command → provider → webhook → read-model** (upgrade immediato /
downgrade schedulato a fine periodo via nuove colonne, disdici/riattiva) — nessuna scrittura diretta della
subscription; sessione **Customer Portal** Paddle server-side; pannello Billing (piano/limiti/azioni/portal +
schermata scaduta con riattiva e CTA GDPR); **banner azionabili 402/429** intercettati nel client API.

## Decisioni prese

- **`occurred_at` monotòno** per i webhook self-service dello stub: derivato da `subscription.last_event_occurred_at`
  (+1s) per non essere scartato dalla guardia out-of-order (UC 0025) quando arriva nello stesso secondo dell'attivazione.
- **Direzione upgrade/downgrade per importo** del ciclo (proxy stabile della semantica tier che conosciamo solo noi).
- **CTA GDPR → `/account`** (entry point esistente): il motore reale export/elimina è di UC 0033 (differito).

## Invarianti appgrove

- **tenant_id dal JWT**: ogni endpoint usa `CallerContext.tenantId()`; `appSlug` dal path risolve solo l'app.
- **Filtro row-level**: letture/azioni via `SubscriptionRepository` (discriminator tenant-scoped); il webhook resta
  vincolato al tenant del payload firmato.
- **Modulo `microsaas_app`**: N/A (nessuna nuova app/infra). Nessuna modifica a `app-start.sh`/`Caddyfile` necessaria:
  `core` e `/api/platform/v1/*` già cablati; nessun nuovo modulo → `run-tests.sh` invariato per lo scope UC 0028.
- **Logging strutturato**: i nuovi endpoint/azioni loggano con `tenant_id`/`app_id`/`user_id`.
- **Webhook = fonte di verità**: le mutazioni passano da provider→webhook→read-model, mai scrittura diretta.

## Note per il revisore

- **Contratto cross-area**: nuovi endpoint core consumati dal backoffice (`/me/subscriptions`, `/me/subscriptions/{appSlug}/{change-tier,cancel,resume}`, `/me/portal-session`); tipi FE rigenerati da OpenAPI (schema.ts) + rebuild dei package `@appgrove/api-client` e `@appgrove/i18n` (consumati da `dist`).
- **Decisioni differite tracciate** in `docs/usecases/07-payments/0028-*.md` §Punti aperti: (1) consumo quota in tempo reale nel banner; (2) gate stock downgrade contro l'uso reale per-app (la logica `TierChangePolicy` è reale+testata, ma il resource passa uso vuoto → non blocca a runtime); (3) motore reale export/elimina GDPR → **UC 0033**; (4) client Paddle reale → gated **#14**.
- **Fuori scope UC 0028 (incluso per decisione del committente)**: `run-tests.sh` porta un blocco `ensure_colima` (auto-avvio di Colima prima dei test backend) non pertinente a UC 0028; incluso in questo commit su richiesta esplicita (risolve l'auto-avvio di Colima).
- **Verifica manuale**: guidata la SPA reale con Playwright (login `owner@acme.test` → `/billing`): `GET /me/subscriptions` 200 con dati di seed; pannello che mostra abbonamenti (Teams/Legacy/Notes) con fasi, limiti e azioni. Screenshot ok.
- **E2E**: `subscriptions.spec.ts` non è nel gate `npm test` (Playwright separato, come `checkout.spec.ts`); verificato a parte insieme al non-regresso del checkout. Nessuna baseline visiva ri-registrata.

## Test

- **Backend (`mvn test`, core)**: **104 verdi**. Nuovi: `TierChangePolicyTest` (direzione + blocco stock/flow + uso vuoto), `SubscriptionSelfServiceTest` (read-model include non-attive, upgrade immediato vs downgrade schedulato, cancel→resume, same-tier 400, OWNER-only 403, isolamento tenant, portal 409→200).
- **Frontend (`npm test`)**: **73 + 8 (i18n) verdi**. Nuovi: `enforcement.test.ts` (402→entitlement, 429→quota, altri null), `subscriptionsView.test.ts` (priorità status line, descrittori limiti flow/stock, formatDate).
- **E2E (Playwright, fuori gate)**: `subscriptions.spec.ts` (downgrade programmato; banner 429 con CTA) + `checkout.spec.ts` (non-regresso) verdi.
- `./run-tests.sh backend frontend` → tutte le suite verdi.

## Stato criteri di accettazione

- [x] `GET /me/subscriptions` ritorna tutte le subscription del tenant (anche scadute/cancellate), con cambio schedulato e flag azionabili, filtrato per tenant dal JWT.
- [x] Upgrade immediato, downgrade schedulato a fine periodo (colonne popolate via webhook); blocco stock oltre capacità reale+testato in `TierChangePolicy` (wiring uso per-app differito).
- [x] Disdici imposta `cancel_at` a fine periodo; riattiva la annulla — entrambe via provider→webhook.
- [x] "Gestisci pagamento e fatture" apre la sessione portal generata server-side (mockata in dev).
- [x] Pannello Billing mostra abbonamenti/limiti/azioni; schermata scaduta offre riattiva + CTA diritti GDPR.
- [x] Banner enforcement: una 402 e una 429 producono il CTA azionabile corretto.
- [x] Nessun accesso cross-tenant nei nuovi endpoint (security test verde).
