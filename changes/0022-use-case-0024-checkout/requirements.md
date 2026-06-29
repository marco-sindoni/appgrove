# Change 0022: Checkout (server-initiated token, overlay stub, polling post-checkout)

**Branch**: `change/0022-use-case-0024-checkout`
**Aree**: `services/core` (backend) · `frontend` (backoffice SPA)
**Data**: 2026-06-29
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/07-payments/0024-checkout.md](../../docs/usecases/07-payments/0024-checkout.md)
**Tocca dati personali?**: No (nessuna nuova categoria). L'email del chiamante è passata al provider al primo
checkout per creare il customer: in locale il provider è lo **stub offline** (nessuna rete); in prod andrebbe a
Paddle, **sub-processor già documentato** (#13) come trattamento "billing/abbonamento". Nessuna nuova entità/colonna
(`accounts.paddle_customer_id` già esiste). Nessun dato di pagamento toccato (MoR).

## Problema / Obiettivo

Implementare il **checkout** d'acquisto di un'app come **vertical slice** locale: scelta tier → checkout avviato
**lato server** (token) → overlay Paddle.js (stub locale) → **attivazione SOLO via webhook** (UC 0025, già
implementato) → UX post-checkout a **polling** rassicurante. Rispetta l'invariante #1: `tenant_id` solo dal JWT, i
`custom_data` impostati server-side e non manomettibili dal client.

## Scope

### Backend (`services/core`)
1. **`GET /api/platform/v1/checkout/apps/{appId}/tiers`** (autenticato) — *endpoint catalogo lato cliente* (opzione A
   concordata): ritorna i tier dell'app (`key`, `name`, `limits`, `features`, `trial_days`) con i relativi **prezzi
   per ciclo** (`monthly`/`annual`, `amount` in minor units, `currency`) da `app_tier` + `app_price`. Read-only,
   contenuto uguale per tutti i tenant (i prezzi vivono nel codice), ma dietro autenticazione (#02).
2. **`POST /api/platform/v1/checkout/apps/{appId}`** (autenticato, **OWNER**) — *checkout server-initiated* (#09 C14):
   body = `{ tierKey | appTierId, billingCycle }`. Il backend legge **`tenant_id` dal JWT** (`CallerContext`), risolve
   `appTier` + `appPrice` → `paddle_price_id`, gestisce il **customer lazy** (usa `accounts.paddle_customer_id` se
   presente, altrimenti lo crea via provider e lo **persiste**), chiama `PaymentProvider.startCheckout` con
   `custom_data = {tenant_id, app_id}` **server-side**, e ritorna `{ checkoutToken }`. **Non** tocca `subscription`
   (l'attivazione è solo via webhook).
3. **`GET /api/platform/v1/checkout/apps/{appId}/subscription`** (autenticato) — *stato minimale per il polling*
   (#09 C17): ritorna `{ status, active }` per `(tenant, app)` via `SubscriptionRepository.findByApp` (tenant-scoped),
   con `active = SubscriptionStatus.grantsAccess()` (mappa canonica UC 0026). Nessuna subscription ⇒ `{ status: null,
   active: false }`.

### Frontend (`frontend/apps/backoffice`)
4. **Schermata scelta tier** sulla pagina `/billing` ([Billing.tsx](../../frontend/apps/backoffice/src/pages/Billing.tsx)
   è il segnaposto riservato): toggle **mensile/annuale con default annuale** + **sconto esplicito** ("2 mesi gratis",
   derivato da `annual` vs `12×monthly`) + badge **trial** (`trial_days`). Dati dal `GET .../tiers`.
5. **Avvio checkout + overlay**: sul tier scelto → `POST .../checkout` → token → apertura overlay via
   **`@appgrove/paddle-stub`** (`createStubPaddle`) col **solo** token. L'evento client (`checkout.completed`) è **solo
   UX** (mai attivazione).
6. **UX post-checkout (polling)**: spinner "attivazione in corso", poll del `GET .../subscription` ogni 1–2s finché
   `active` → "attivato"; oltre ~30–60s **messaggio rassicurante** (pagamento ok, attivazione in arrivo), **mai
   errore**. Errori di rete del **nostro** backend gestiti (problem+json), mai "errore di pagamento" inventato.
7. **i18n** (it/en) per le nuove stringhe; wiring API client esistente (`api/`).

### Test
8. **Backend integration** (`@QuarkusTest` + Testcontainers): `custom_data` impostati server-side dal JWT (tenant **non**
   dal body); risoluzione tier/price corretta; customer lazy persistito; il `POST` **non** attiva la subscription;
   `GET .../subscription` tenant-scoped e isolato; `GET .../tiers` corretto.
9. **Frontend**: unit (vitest) sulla logica di stato/polling (transizioni "in corso"→"attivato"→rassicurante) +
   **L2 Playwright E2E** (backoffice) con **Paddle.js mockato** (paddle-stub): scelta tier → server-initiated → overlay
   → simulazione arrivo webhook → polling "attivazione"→"attivato".

## Fuori scope (differito e tracciato — come richiesto)

- **Entitlement reale del registry FE** (sostituire lo stub `entitled: string[]`, far comparire l'app appena acquistata
  nella sidebar "YOUR APPS") e l'endpoint **entitlement completo** `/me/entitlements` → **UC 0027**. *Conseguenza nota:*
  in questa change un'app appena attivata **non** appare automaticamente nella sidebar finché 0027 non cabla
  l'entitlement reale; il polling di 0024 conferma solo lo stato della singola app.
- **Overlay Paddle.js reale** (loader del vero Paddle.js vs stub) e **`PaddlePaymentProvider.startCheckout` reale** →
  gated #14, smoke **L3** in **UC 0029**.
- **Marketplace / discovery delle app non possedute** (da dove si avvia l'acquisto di un'app non ancora posseduta): qui
  il checkout si aggancia a `/billing`; la vetrina di catalogo delle app acquistabili è un follow-up → da valutare
  (tracciato in UC 0024).
- **Pipeline webhook / lifecycle / enforcement / portal**: UC 0025 (fatto) / 0026 (fatto) / 0027 / 0028.

## Criteri di accettazione

- [ ] `POST .../checkout` ritorna un token; `tenant_id` e `app_id` nei `custom_data` sono impostati **server-side** (dal
      JWT), **ignorando** qualunque tenant nel body; OWNER-only.
- [ ] Il checkout **non** modifica `subscription` (nessuna attivazione lato client/endpoint); l'attivazione resta solo
      webhook.
- [ ] Customer lazy: primo checkout senza `paddle_customer_id` → creato e persistito; checkout successivi lo riusano.
- [ ] `GET .../tiers` ritorna tier+prezzi per ciclo; `GET .../subscription` ritorna `{status, active}` tenant-scoped
      (active dalla mappa canonica UC 0026).
- [ ] FE: scelta tier (default annuale + sconto + trial) → overlay stub col solo token → polling "in corso"→"attivato",
      rassicurante oltre soglia, mai errore di pagamento inventato.
- [ ] Test verdi: backend integration + FE unit + **L2 Playwright E2E**; `./run-tests.sh backend` e `./run-tests.sh
      frontend` verdi.
- [ ] Avvio locale (DoD): la feature è eseguibile in locale (route `/billing`, provider stub, webhook sintetico).
- [ ] UC 0024 → ✅ in `_INDEX.md`; task residui/refactor tracciati negli UC proprietari.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT**: `POST/GET` leggono il tenant da `CallerContext.tenantId()` (claim verificato); i
  `custom_data` del checkout sono impostati server-side; il client non fornisce mai `tenant_id`. Test di regressione
  dedicato (tenant nel body ignorato).
- **Filtro row-level**: `GET .../subscription` usa `SubscriptionRepository` (entità `@TenantId` → `WHERE tenant_id`
  automatico). Il catalogo (`tiers`) non è tenant-scoped (prezzi globali) ma è dietro `@Authenticated`.
- **Modulo `microsaas_app`**: N/A (nessuna infra).
- **Logging strutturato**: i nuovi endpoint loggano `tenant_id`/`app_id`/`user_id` (avvio checkout, esito), pattern
  esistente.

## Requisiti di test

- **Integration**: (a) tenant dai `custom_data` = JWT anche con body ostile; (b) `POST` non crea/aggiorna
  `subscription`; (c) customer lazy create→persist→reuse; (d) risoluzione `(app,tier,cycle)→paddle_price_id`, 404 su
  tier/price inesistente; (e) `GET .../subscription` isolamento cross-tenant.
- **FE unit**: macchina a stati del polling (idle→submitting→activating→active; soglia→reassuring; errori di rete del
  backend ≠ errore pagamento).
- **L2 E2E**: flusso completo con paddle-stub e webhook sintetico (riuso pipeline UC 0023/0025).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (solo nuovi endpoint + nuova UI sulla route già esistente `/billing`) |
| Contratto cross-area | Sì — frontend ↔ API core (`/checkout/*`): i due lati vanno nello stesso commit; OpenAPI core rigenerato |
| Version bump | minor (nuova funzionalità) |
