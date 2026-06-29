# Implementation Log — Change 0022: Checkout (server-initiated token, overlay stub, polling)

**Branch**: `change/0022-use-case-0024-checkout`
**Aree**: `services/core` (backend) · `frontend` (backoffice + packages api-client/i18n)
**Completata**: 2026-06-29

## File modificati

| File | Azione |
|---|---|
| services/core/.../billing/CheckoutResource.java | Creato — 3 endpoint (tiers / start / subscription), slug-based |
| services/core/.../billing/CheckoutDtos.java | Creato — DTO catalogo/checkout/stato |
| services/core/.../billing/CheckoutStarted.java | Creato — evento CDI di dominio |
| services/core/.../billing/StubCheckoutActivation.java | Creato — osservatore dev/test che simula il webhook |
| services/core/.../billing/PaymentProvider.java | Modificato — command esteso (paddlePriceId, existingPaddleCustomerId) |
| services/core/.../billing/StubPaymentProvider.java | Modificato — riuso customer lazy |
| services/core/.../platform/CallerContext.java | Modificato — `email()` (claim upn) |
| services/core/.../META-INF/openapi/openapi.{yaml,json} | Rigenerati (+`/checkout/*`; dev endpoint esclusi) |
| services/core/.../test/billing/CheckoutResourceTest.java | Creato — integration L1 (10 test) |
| services/core/.../test/billing/PaymentProviderTest.java | Modificato — nuovo command + reuse customer |
| services/core/.../test/TestData.java | Modificato — helper `appPrice` |
| frontend/apps/backoffice/src/pages/Billing.tsx | Riscritto — UI scelta tier + overlay + polling |
| frontend/apps/backoffice/src/billing/{checkoutMachine,checkoutApi,paddle}.ts | Creati — logica pura, hooks, factory overlay |
| frontend/apps/backoffice/src/billing/checkoutMachine.test.ts | Creato — vitest unit (8 test) |
| frontend/apps/backoffice/e2e/checkout.spec.ts | Creato — L2 Playwright (Paddle.js mockato) |
| frontend/apps/backoffice/package.json | Modificato — dep `@appgrove/paddle-stub` |
| frontend/packages/i18n/src/resources/{en,it}.ts | Modificato — namespace `checkout` |
| frontend/packages/api-client/src/schema.ts | Rigenerato dall'OpenAPI |
| docs/usecases/07-payments/{0024,0027}.md | Modificato — Punti aperti / differiti |
| docs/usecases/_INDEX.md | Modificato — 0024 → ✅ |

## Cosa è stato fatto

Slice verticale del checkout (UC 0024), eseguibile in locale end-to-end. **Backend** (`services/core`):
`CheckoutResource` con catalogo lato cliente (`GET .../tiers`), checkout **server-initiated** (`POST .../{appSlug}`,
OWNER) che legge `tenant_id` dal JWT, risolve `paddle_price_id` da `(app, tier, ciclo)`, gestisce il **customer lazy**
e ritorna il **token** senza toccare `subscription`, e stato minimale per il polling (`GET .../subscription`).
**Frontend** (`backoffice`, `/billing`): scelta app→tier (default annuale + sconto + trial), overlay via
`@appgrove/paddle-stub` col solo token, UX a polling rassicurante (mai errore di pagamento).

## Decisioni prese

- **Endpoint slug-based**: i tre endpoint usano `app.slug` (chiave stabile = id del registry FE) invece dell'UUID,
  risolvendo internamente `slug→App`. Evita di esporre/gestire UUID lato client. La riconciliazione slug↔UUID
  dell'entitlement è di UC 0027 (tracciata).
- **Attivazione via evento CDI + bean stub-only**: `CheckoutResource` resta **prod-fedele** (nessuna auto-attivazione)
  e spara `CheckoutStarted`; in dev/test `StubCheckoutActivation` (gate `provider=stub`) emette il webhook `happy_path`
  via la pipeline firmata esistente → l'attivazione passa **sempre** dal webhook (#09 C16), il locale "si attiva da
  solo" per la DX. In prod nessun osservatore.
- **Command provider esteso** (paddlePriceId, existingPaddleCustomerId) per fedeltà alla creazione checkout reale e per
  il customer lazy (#09 C15).
- **Overlay FE = stub** (`billing/paddle.ts`): loader Paddle.js reale gated #14 → differito (UC 0029).

## Invarianti appgrove

- **Tenant ID solo dal JWT**: `POST/GET` leggono `caller.tenantId()`; lo `StartCheckoutRequest` **non** ha campo
  tenant; i `custom_data={tenant_id,app_id}` sono impostati server-side. Test: l'attivazione finisce solo sul tenant
  del JWT, non su altri (isolamento).
- **Filtro row-level**: `GET .../subscription` usa `SubscriptionRepository` (`@TenantId` → `WHERE tenant_id`). Catalogo
  (tiers) platform-level dietro `@Authenticated`.
- **Modulo `microsaas_app`**: N/A.
- **Logging strutturato**: `checkout.start` con `app_id` nel messaggio; `tenant_id`/`user_id` da `MdcRequestFilter`.

## Note per il revisore

- **Contratto cross-area** (frontend ↔ API core): nuovi endpoint `/checkout/*`; OpenAPI core rigenerato e
  `@appgrove/api-client` ri-generato nello **stesso commit** → i due lati restano coerenti.
- **Avvio locale (DoD)**: nessun nuovo processo/modulo/route → `app-start.sh`/`app-stop.sh`/`Caddyfile` invariati
  (gli endpoint stanno sotto `/api/platform/v1/*` del core già instradato). `run-tests.sh` invariato (nessun modulo
  nuovo). Nessuna migrazione (usa `app`/`app_tier`/`app_price`/`subscription`/`accounts` esistenti).
- **Privacy/RoPA**: nessuna nuova categoria/colonna; l'email (claim `upn`) è passata al provider solo per il customer —
  in locale stub offline, in prod Paddle (sub-processor già documentato #13). Nessun checkpoint.
- **Decisioni differite tracciate** (constitution + richiesta esplicita): in `0024-…md` (Punti aperti: overlay/provider
  reale → #14/UC 0029; marketplace app non possedute; price-locale polish) e `0027-…md` (entitlement reale del registry
  + `/me/entitlements` + slug↔UUID). Nulla resta solo in chat.
- **E2E**: `e2e/checkout.spec.ts` gira con `npm run e2e` (Paddle.js mockato + `page.route`), **non** in `npm test`
  (coerente con gli altri E2E del backoffice); nessun baseline visivo ri-registrato.

## Test

- **`services/core`** (`mvn test`, Testcontainers): `CheckoutResourceTest` (10) — catalogo tiers; token; OWNER-only
  (403); tier/ciclo invalidi (404/400); **il POST non attiva** (subscription assente finché il webhook non è drenato);
  attivazione **solo sul tenant del JWT** (isolamento); **customer lazy** create→persist→reuse; 401 anonimo.
  `PaymentProviderTest` (2) aggiornato (command + reuse customer).
- **`frontend`** (`vitest`): `checkoutMachine.test.ts` (8) — fasi/polling (activating→active, ritardo webhook mai
  errore), soglia rassicurante, sconto annuale, formato prezzo. Suite backoffice **58/58**.
- **L2 Playwright** (`npm run e2e`): `checkout.spec.ts` — scelta tier → overlay stub → polling "attivazione"→"attivato".
- **Canoniche**: `./run-tests.sh backend` → BUILD SUCCESS; `./run-tests.sh frontend` → verde.

## Stato criteri di accettazione

- [x] `POST .../checkout` ritorna token; `custom_data` (tenant/app) server-side dal JWT; OWNER-only; body senza tenant.
- [x] Il checkout **non** modifica `subscription` (attivazione solo via webhook drenato).
- [x] Customer lazy create→persist→reuse.
- [x] `GET .../tiers` (tier+prezzi per ciclo) e `GET .../subscription` (`{status, active}` tenant-scoped, mappa #09 E29).
- [x] FE: scelta tier (default annuale + sconto + trial) → overlay col solo token → polling rassicurante, mai errore.
- [x] Backend integration + FE unit + **L2 Playwright** verdi; `run-tests.sh backend|frontend` verdi.
- [x] Avvio locale ok (route `/billing`, provider stub, webhook sintetico auto-emesso in dev).
- [x] UC 0024 → ✅ in `_INDEX.md`; task residui/refactor tracciati (0024/0027).
