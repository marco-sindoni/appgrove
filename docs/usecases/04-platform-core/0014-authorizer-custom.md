# UC 0014 — Custom Lambda authorizer (app-abilitata + entitlement grossolano derivato; catena gate)

**Area**: 04-platform-core · **Fase**: 2 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0013](0013-account-utenti-inviti-api.md) (catalogo/subscription), UC [0016](../05-auth/0016-pre-token-gen-jwt.md) (JWT)
**Fonte decisioni**: #04 §7 (enforcement edge), #09 dec.30 (catena gate), #01 (authorizer centralizzato), #02 §8
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [04-services-backend](../../04-services-backend.md), [09-pagamenti](../../09-pagamenti.md), [02-auth-sicurezza](../../02-auth-sicurezza.md)

## 1. Obiettivo / Scope
Implementare il **custom Lambda authorizer** sull'API Gateway: i due check **grossolani** di piattaforma, primi anelli della
catena di gate (#09 dec.30), prima che la richiesta raggiunga il servizio.
**Incluso**: verifica **JWT**; **(2) app abilitata?** (flag disable-admin, ha precedenza, → **403**); **(3) tenant entitled?**
(entitlement **DERIVATO** da `platform.subscription`, status ∈ `{trialing,active,past_due}`, → **402**/deny). Sostituisce
l'authorizer Cognito nativo.
**Escluso**: i gate **fini** nel servizio (ruolo 403, quota 429, sfumature stato/grace 402 — UC 0027); la materializzazione
entitlement (**abolita**, #09 dec.12); la cache authorizer (**opzionale/futura, NON attiva**).

## 2. Attori & ruoli
- **Authorizer Lambda** (edge): esegue i check grossolani.
- **Servizio** (Quarkus): ri-valida JWT e applica i gate fini (UC 0027).
- **platform-admin**: può **disabilitare** un'app (gate 2, UC 0021).

## 3. Precondizioni
- JWT valido con `tenant_id`/`roles` (UC 0016); catalogo+`subscription` nel core (UC 0013); flag disable-app gestibile (UC 0021).

## 4. Flusso principale (per ogni richiesta a `/api/<app_id>/...`)
1. **AuthN**: verifica JWT, estrae `tenant_id` → altrimenti **401**.
2. **(2) App abilitata?** legge il flag disable-admin dell'app; se disabilitata → **403** (precede l'entitlement; rende l'app
   indisponibile a tutti **senza toccare dati/subscription**, reversibile) (#09 dec.30 gate 2).
3. **(3) Tenant entitled?** **deriva** l'entitlement da `subscription` (status ∈ `{trialing,active,past_due}`) — **non** legge
   una tabella `entitlements` (abolita) → se non entitled → **402** "abbonamento richiesto/scaduto" (#09 dec.12/dec.30 gate 3).
4. Se passa → forwarda al servizio, che applica ruolo/quota/grace (UC 0027).

## 5. Flussi alternativi / edge / errori
- **Derivazione economica**: tabelle piccole → **nessuna cache attiva**; cache = opzionale/futura solo se collo di bottiglia (eventuale revoca con lag = TTL) (#04 §7).
- **Diritti GDPR esenti** dai gate (2)/(3): export/erasure non passano dall'API di business gateata (#09 F31) — sono capability di piattaforma (UC 0032).
- **Health/Swagger**: health esclusi dall'authorizer (#08 21); Swagger gated platform-admin (#04 9).
- **Out-of-order/stato**: l'entitlement riflette `subscription` (aggiornata idempotente dal webhook, UC 0025).

## 6. Risorse & runbook
**Risorse**: Lambda authorizer (VPC, accesso DB via RDS Proxy per leggere subscription/flag), associata alle route `/api/<app_id>/*`.
**Runbook**: deploy via UC 0005; osservabilità via log con `tenant_id`/`app_id`/`correlation_id` (#08). Sostituisce l'authorizer nativo Cognito.

## 7. Dati toccati
Legge `platform.subscription` (status) + flag `app.status` per derivare entitlement e app-abilitata. Nessuna scrittura. Nessun
PII in chiaro nei log (identificativi opachi, #08 5). Manifest: rientra nel trattamento billing/account (#13 B).

## 8. Permessi & gate
- **Catena gate (#09 dec.30)**: authN(401) → **app-abilitata(403)** → **entitled(402)** [edge, questo UC] → ruolo(403) → quota(429) [servizio, UC 0027].
- **Invarianti**: `tenant_id` dal JWT; entitlement **derivato** (unica fonte `subscription`); nessuna tabella entitlements; difesa in profondità (il servizio ri-verifica).

## 9. Requisiti di test
- **Integration**: app disabilitata → 403; tenant non entitled (status canceled/paused) → 402; entitled → forward (#10 12).
- **Security/multi-tenancy** (#10 D): l'entitlement è per (tenant, app) corretto; nessun cross-tenant; coerenza con la ri-validazione del servizio.
- Verifica che i **diritti GDPR** non siano bloccati dall'authorizer (#09 F31).

## 10. Riferimenti & Definition of Done
- **Decisioni**: #04 §7, #09 dec.12/dec.29/dec.30, #01 6, #02 8.
- **DoD**:
  1. Authorizer verifica JWT + app-abilitata (403) + entitlement derivato (402); sostituisce l'authorizer Cognito.
  2. Nessuna tabella entitlements; nessuna cache attiva (solo futura/opzionale).
  3. Diritti GDPR esenti dai gate; health/Swagger gestiti correttamente.
  4. Test gate (401/402/403) + isolamento verdi.
