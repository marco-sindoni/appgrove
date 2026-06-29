# Change 0021: Semantica ciclo di vita subscription (mappa status→accesso + derivazione lifecycle)

**Branch**: `change/0021-use-case-0026-ciclo-vita-abbonamento`
**Aree**: `services/core` (backend)
**Data**: 2026-06-29
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/07-payments/0026-ciclo-vita-abbonamento.md](../../docs/usecases/07-payments/0026-ciclo-vita-abbonamento.md)
**Tocca dati personali?**: No — pura derivazione dallo stato di `subscription` (status/period/cancel_at/trial_end), nessuna nuova categoria di dato, nessuna PII, nessuno schema nuovo.

## Problema / Obiettivo

UC 0026 possiede la **semantica del ciclo di vita** della subscription: la **mappa status→accesso** (#09 E29) che
alimenta l'entitlement *derivato*, più la semantica di cancellazione "fino a fine periodo", trial e dunning/grace.

Oggi questa regola **non ha una fonte di verità unica**: è **duplicata e incompleta** in `AdminResource`
(`ENTITLEMENTS_SQL` calcola `entitled` come `s.status in ('active','trialing')`, **omettendo `past_due`** — cioè la
grace del dunning, che invece **mantiene l'accesso**). Manca inoltre una derivazione di dominio che incarni la
semantica lifecycle (cancel schedulato/riattivabile, trial, grace) che UC 0027 (enforcement) e UC 0028 (self-service/UX)
consumeranno.

Obiettivo: introdurre il **livello di semantica di dominio** come unica fonte di verità, sanare il drift in
`AdminResource`, e coprirlo con test lifecycle L1 — senza enforcement runtime, senza self-service, senza frontend, senza
nuove tabelle.

## Scope

Solo `services/core` (backend), pura logica di dominio + refactor letture admin:

1. **Mappa status→accesso canonica** (#09 E29) come unica fonte di verità: accesso **sse** `status ∈ {trialing,
   active, past_due}`; **no accesso** se `∈ {paused, canceled}`. Realizzata come metodo di dominio su
   `SubscriptionStatus` (es. `grantsAccess()`), pura, senza tabelle.
2. **Derivazione lifecycle** da uno snapshot `subscription` (+ "now"): un value object di dominio (record/enum) che
   classifica la **fase di accesso** *derivabile dallo schema attuale* —
   `TRIAL` (trialing) · `ACTIVE` · `CANCELING` (active con `cancel_at` valorizzato → accesso fino a `cancel_at`,
   riattivabile azzerando `cancel_at`) · `GRACE` (past_due, dunning → accesso mantenuto) · `ENDED` (canceled/paused →
   no accesso) — esponendo `access` (bool) e `accessUntil` (= `cancel_at` se in `CANCELING`, altrimenti
   `current_period_end`). È la lettura che UC 0027/0028 consumeranno; qui resta pura derivazione testata (nessun
   endpoint pubblico nuovo → nessun drift OpenAPI).
3. **Refactor `AdminResource`**: `entitled` calcolato in Java dalla mappa canonica (`grantsAccess()` ∧ `app.status =
   'active'`), non più dal predicato SQL duplicato/incompleto → sanata l'omissione di `past_due`.
4. **Test L1**: tabella status→accesso completa (5 stati); transizioni per evento già verificate dalla pipeline (0025)
   estese con asserzioni sull'accesso derivato; fase `CANCELING` (accesso fino a `cancel_at`, riattivazione),
   `GRACE` (accesso in past_due), `TRIAL`, `ENDED`; isolamento per (tenant, app).

## Fuori scope (differito e tracciato negli UC proprietari)

- **Enforcement runtime** dei gate (entitled 402) + quota SPI → **UC 0027**.
- **Self-service/portal** (avvio upgrade/downgrade/disdetta/riattivazione) e **UX downgrade** "schedulato, attivo dal
  giorno X" → **UC 0028**. La *surfacing* del cambio tier schedulato richiede anche **persistenza del cambio
  schedulato** (colonna `scheduled_tier_id`/effective_at + mapping webhook): non derivabile dallo schema attuale →
  differita a 0028.
- **Gating `stock`** del downgrade + nozione di **metrica flow/stock** (oggi inesistente nel codice) → **UC 0054/0027**.
- **Checkout** (overlay/polling) → **UC 0024**. Tempistica esatta dei **2 settimane** di dunning: la temporizza
  **Paddle** (poi flippa lo status a canceled/paused); noi ci fidiamo dello status (nessun timer locale).
- **Frontend**: nessuna modifica (la UX non ha dove vivere senza il portale di 0028).
- **Packaging cloud / migrazioni**: nessuna (nessuno schema nuovo).

## Criteri di accettazione

- [ ] Esiste **un'unica** definizione della mappa status→accesso (`SubscriptionStatus.grantsAccess()`): accesso per
      `{trialing, active, past_due}`, no per `{paused, canceled}`.
- [ ] `AdminResource` calcola `entitled` dalla mappa canonica (∧ `app.status='active'`); il predicato SQL duplicato è
      rimosso; `past_due` ora risulta `entitled` (a parità di app attiva).
- [ ] La derivazione lifecycle classifica correttamente `TRIAL/ACTIVE/CANCELING/GRACE/ENDED` ed espone `access` e
      `accessUntil` coerenti (cancel → fino a `cancel_at`, riattivabile).
- [ ] Test L1 verdi su tutta la mappa e sulle transizioni lifecycle; isolamento per (tenant, app) preservato.
- [ ] `./run-tests.sh backend` verde.
- [ ] UC 0026 → ✅ in `_INDEX.md`; differiti tracciati negli UC proprietari (0027/0028/0054/0024) e/o nel file UC 0026.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT**: invariato. Le letture admin sono cross-tenant *by design* (console UC 0021,
  `@RolesAllowed` admin) e restano tali; la derivazione di dominio è una funzione pura su uno snapshot già caricato,
  non legge tenant da input.
- **Filtro row-level**: invariato. `SubscriptionRepository`/`Subscription` restano `BaseTenantEntity` (filtro
  automatico); nessuna query tenant-scoped nuova. La derivazione non bypassa il discriminator.
- **Modulo `microsaas_app`**: N/A (nessuna infra).
- **Logging strutturato**: invariato; nessun nuovo punto di log con tenant/app/user richiesto (logica pura). Eventuali
  log restano col pattern esistente.

## Requisiti di test

- **L1**: copertura esaustiva della tabella status→accesso (5 stati) come unit test puro sull'enum.
- **L1 lifecycle**: `CANCELING` (status active + `cancel_at` futuro → access, `accessUntil = cancel_at`; `cancel_at`
  azzerato → torna `ACTIVE`), `GRACE` (past_due → access), `TRIAL` (trialing → access, `accessUntil = period_end`),
  `ENDED` (canceled/paused → no access).
- **Regression**: l'entitlement admin per `past_due` passa da non-entitled a entitled (a parità di app attiva); nessuna
  regressione sugli scenari pipeline esistenti (`SubscriptionPipelineTest`).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (cambia solo, in meglio, la correttezza di `entitled` per `past_due`; nessun contratto pubblico modificato) |
| Contratto cross-area | N/A (nessun endpoint nuovo/modificato; nessun drift OpenAPI) |
| Version bump | patch |
