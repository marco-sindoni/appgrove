# Implementation Log — Change 0021: Semantica ciclo di vita subscription (mappa status→accesso + derivazione lifecycle)

**Branch**: `change/0021-use-case-0026-ciclo-vita-abbonamento`
**Aree**: `services/core` (backend)
**Completata**: 2026-06-29

## File modificati

| File | Azione |
|---|---|
| services/core/.../billing/SubscriptionStatus.java | Modificato (`grantsAccess()` — mappa canonica #09 E29) |
| services/core/.../billing/SubscriptionLifecycle.java | Creato (derivazione lifecycle pura: fasi + access + accessUntil) |
| services/core/.../platform/AdminResource.java | Modificato (`entitled` derivato in Java dalla mappa canonica; rimosso predicato SQL duplicato) |
| services/core/.../test/billing/SubscriptionLifecycleTest.java | Creato (L1 puro: mappa completa + fasi) |
| services/core/.../test/AdminApiTest.java | Modificato (regressione: `past_due`/`trialing` → entitled) |
| docs/usecases/07-payments/0026-ciclo-vita-abbonamento.md | Modificato (sezione "Punti aperti": stato post-change + differiti) |
| docs/usecases/07-payments/0027-applicazione-entitlement-quota.md | Modificato (nota: read-model entitlement disponibile + natura stock differita) |
| docs/usecases/07-payments/0028-portale-cliente-self-service.md | Modificato (sezione "Punti aperti": persistenza cambio tier schedulato) |
| docs/usecases/_INDEX.md | Modificato (0026 → ✅) |

## Cosa è stato fatto

Implementato il **livello di semantica di dominio** del ciclo di vita subscription (UC 0026), backend-only e
senza schema nuovo. La **mappa status→accesso** (#09 E29) è ora la *fonte di verità unica*
`SubscriptionStatus.grantsAccess()` (accesso `∈ {trialing, active, past_due}`), e `SubscriptionLifecycle`
deriva da uno snapshot `subscription` la fase di accesso (`TRIAL/ACTIVE/CANCELING/GRACE/ENDED`) con `access` e
`accessUntil` (cancel → fino a `cancel_at`, riattivabile; altrimenti fine periodo; null se ENDED) — read-model
per UC 0027/0028. `AdminResource` calcola `entitled` da questa mappa (∧ app attiva) invece del predicato SQL
duplicato, **sanando l'omissione di `past_due`** che escludeva erroneamente l'accesso durante il dunning/grace.

## Decisioni prese

- **`entitled` derivato in Java, non in SQL**: il predicato `s.status in ('active','trialing')` di
  `ENTITLEMENTS_SQL` era una *seconda* copia (incompleta) della regola; ora il SQL restituisce solo lo status
  grezzo + `app_active` e l'accesso si calcola con `grantsAccess()` → una sola definizione, niente drift.
- **`accessUntil` semantica**: `cancel_at` solo in `CANCELING` (cutoff definitivo); altrove `current_period_end`
  (confine rinnovabile); null in `ENDED`. La distinzione access (booleano canonico) vs phase (classificazione
  ricca) tiene `access == grantsAccess()` per costruzione (testato come invariante).
- **Nessun timer locale per dunning/trial**: i 2 settimane di grace e la conversione trial li temporizza Paddle
  (poi flippa lo status); il sistema si fida dello status corrente. Coerente con UC 0025 (transizioni via webhook).
- **Niente endpoint pubblico nuovo**: il read-model resta di dominio (consumato da 0027/0028) → nessun drift OpenAPI.

## Invarianti appgrove

- **Tenant ID dal JWT**: invariato. La derivazione è una funzione pura su uno snapshot già caricato; non legge
  tenant da input. Le letture admin restano cross-tenant *by design* (console UC 0021, ruolo admin).
- **Filtro row-level**: invariato. `Subscription`/`SubscriptionRepository` restano `BaseTenantEntity` (filtro
  automatico); nessuna query tenant-scoped nuova; la derivazione non bypassa il discriminator.
- **Modulo `microsaas_app`**: N/A (nessuna infra).
- **Logging strutturato**: invariato (logica pura, nessun nuovo punto di log richiesto).

## Note per il revisore

- **Confine di scope (gate di chiarimento)**: change **backend-only** sul livello di *semantica di dominio*.
  Enforcement runtime (402 + quota SPI), self-service/UX, gating `stock` e checkout sono fuori scope per accordo.
- **Decisioni differite tracciate** (constitution): nessuna resta in chat —
  - **UC 0026** (`0026-…md` → "Punti aperti"): record primario di fatto/differito post-change.
  - **UC 0027**: il read-model `grantsAccess()`/`SubscriptionLifecycle` è il building block da consumare per il
    402; la **natura `stock`** del gate è differita lì (nessuna nozione flow/stock nel codice oggi).
  - **UC 0028**: la **persistenza del cambio tier schedulato** (downgrade "attivo dal giorno X") richiede colonna
    `scheduled_tier_id`/`scheduled_change_at` + mapping `subscription.updated`, non derivabile dallo schema attuale.
- **Contratto cross-area**: nessuno. Nessuna modifica a frontend/infra; nessun endpoint/contratto pubblico cambiato.
- **Avvio locale (DoD CLAUDE.md)**: nessun nuovo processo/modulo/route → `app-start.sh`/`app-stop.sh`/`Caddyfile`
  invariati. Nessuna migrazione (derivazione su schema esistente).
- **Privacy/RoPA**: nessuna nuova categoria di dato né integrazione esterna; nessun checkpoint.
- **Nota test cross-classe**: eseguendo un *sottoinsieme* di `@QuarkusTest` via `-Dtest=` può emergere una FK su
  `subscription_app_id_fkey` per interferenza di stato sul Postgres condiviso tra classi (preesistente, non
  introdotta qui); il runner canonico `./run-tests.sh backend` è verde.

## Test

- **`services/core`** — `SubscriptionLifecycleTest` (L1 puro, 13): mappa status→accesso completa (5 stati),
  fasi `TRIAL/ACTIVE/CANCELING/GRACE/ENDED`, `accessUntil` per fase, riattivazione (cancel_at azzerato → ACTIVE),
  invariante `access == grantsAccess()` per ogni stato.
- **`AdminApiTest`** (6, integration Testcontainers): aggiunte asserzioni di regressione — Acme→notes (`past_due`,
  app attiva) **ora entitled**, Bob→notes (`trialing`) entitled; invariati teams/legacy/canceled.
- **Regressione**: `SubscriptionPipelineTest` (6) e suite billing/webhook verdi (nessun impatto).
- **Suite canonica**: `./run-tests.sh backend` → **BUILD SUCCESS** (commons/core/auth-local/fatture tutte verdi).
- Frontend/infra: non toccati.

## Stato criteri di accettazione

- [x] Unica definizione mappa status→accesso (`SubscriptionStatus.grantsAccess()`): accesso `{trialing, active, past_due}`.
- [x] `AdminResource` deriva `entitled` dalla mappa canonica (∧ app attiva); predicato SQL duplicato rimosso; `past_due` ora entitled.
- [x] `SubscriptionLifecycle` classifica `TRIAL/ACTIVE/CANCELING/GRACE/ENDED` con `access`/`accessUntil` coerenti (cancel → fino a `cancel_at`, riattivabile).
- [x] Test L1 verdi su mappa + transizioni lifecycle; isolamento (tenant, app) preservato.
- [x] `./run-tests.sh backend` verde.
- [x] UC 0026 → ✅ in `_INDEX.md`; differiti tracciati negli UC proprietari (0026/0027/0028) e nessuno lasciato in chat.
