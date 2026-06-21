# UC 0051 — App #1 (B2C single-user, es. fatture) backend (schema, quota SPI, contratto GDPR, logging)

**Area**: 11-apps · **Fase**: 4 · **Stato**: 🟢 deciso (esempio concreto: app "fatture")
**Dipendenze**: UC [0012](../04-platform-core/0012-servizio-core-multitenancy.md) (core/commons), UC [0014](../04-platform-core/0014-authorizer-custom.md) (authorizer/gate)
**Fonte decisioni**: #04 (backend), #05 (per-app), #13 L (contratto GDPR), #09 A (quota SPI)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [04-services-backend](../../04-services-backend.md), [05-persistenza-dati](../../05-persistenza-dati.md), [13-compliance-privacy](../../13-compliance-privacy.md), [09-pagamenti](../../09-pagamenti.md)

## 1. Obiettivo / Scope
Implementare il **backend della prima app** (B2C **single-user**, es. fatturazione privati) come verticale Quarkus che eredita
tutte le invarianti dal `commons`.
**Incluso**: modulo servizio `services/<app_id>/`; schema `app_<app_id>` (tabelle tenant-scoped con `tenant_id` discriminator);
implementazione del **contratto quota SPI** (`flow`/`stock`, metrica per-app, es. "n. fatture/mese"); **contratto GDPR per-app**
(`exportData`/`purgeData` + manifesto dati); logging strutturato di default; API `/api/<app_id>/v1/*` problem+json + OpenAPI.
**Escluso**: il frontend modulo (UC 0052), la landing (UC 0053), l'enforcement runtime entitlement/quota di piattaforma (UC 0027),
la skill che lo scaffolderà (UC 0046 — questa app #1 valida il pattern manualmente).

## 2. Attori & ruoli
- **Utente B2C** (owner del proprio tenant single-user): usa l'app.
- **Sistema**: enforcement gate (authorizer UC 0014 + servizio UC 0027); purge orchestrata dal core (UC 0032).

## 3. Precondizioni
- Core/commons (UC 0012/0013); modulo infra `microsaas_app` per l'app (UC 0004); authorizer (UC 0014); catalogo+pricing (UC 0013/0022).

## 4. Flusso principale
1. **Scaffold** modulo Maven `services/<app_id>/` dipendente da `commons` (#04 2); package-by-layer (#04 5).
2. **Schema** `app_<app_id>`: tabelle con colonna **`tenant_id`** (discriminator), audit + soft-delete, PK UUID v7; Flyway possiede le proprie migrazioni (#05 7bis/8, #04).
3. **Quota SPI**: dichiara metrica + natura (**flow** per fatture/mese) + finestra + tetto nei `app_tier.limits`; implementa la faccia runtime (`quota.checkAndReserve(metric)`) chiamata **prima** dell'azione che consuma quota (#09 A5/E23/F30 gate 5).
4. **Contratto GDPR**: implementa `exportData(scope)` (con step dichiarati per il progress) e `purgeData(scope)` + **manifesto dati** (categorie/finalità/base/retention) (#13 L69, C15).
5. **API** `/api/<app_id>/v1/*` problem+json + OpenAPI; logging con `tenant_id`/`app_id`/`user_id` (#04 6/9, #08).

## 5. Flussi alternativi / edge / errori
- **Quota raggiunta** → hard-limit → **429** (aspetta reset o upgrade) (#09 A6).
- **Tenant non entitled / app disabilitata** → bloccato a monte (authorizer 402/403, UC 0014).
- **Single-user**: un solo utente per tenant; identico isolamento `tenant_id` (#05 10).
- **Erasure**: `purgeData` cancella tutto (incl. cache/derivati) con audit; invocato dall'EventBridge purge (UC 0032) (#13 L70).

## 6. Risorse & runbook  _(backend)_
**Risorse**: modulo servizio + schema `app_<app_id>` (via modulo microsaas_app UC 0004) + migrazioni Flyway. **Runbook (dev)**:
`dev service <app_id>` (UC 0009) contro Postgres locale + auth locale (UC 0010) + seed (UC 0011). Deploy via UC 0005.

## 7. Dati toccati
Schema `app_<app_id>`: i contenuti dell'app (es. fatture → nominativi/importi). **Dati personali** = "contenuti dell'app"
(postura uniforme #13 A2: dell'utente, trattati per erogare il servizio, base **contratto**), dichiarati nel **manifesto**
(categorie/finalità/base/retention). `@PersonalData` sui campi pertinenti → enforcement gate privacy (UC 0031). Retention per-app (#13 E24).

## 8. Permessi & gate
- **Invarianti**: `tenant_id` dal JWT; **filtro row-level** automatico (discriminator commons); ruolo DB least-privilege sul solo `app_<app_id>` (#05 11); logging strutturato.
- **Catena gate (#09 dec.30)**: authorizer (app-abilitata/entitlement) + servizio (ruolo `@RolesAllowed` 403, **quota** 429). Diritti GDPR esenti (#09 F31).

## 9. Requisiti di test
- **Integration** (Testcontainers + Flyway): CRUD app, quota flow (reset finestra), export/purge senza dati orfani.
- **Security/multi-tenancy** (#10 D, harness ereditato): A non vede dati di B; anti-override `tenant_id`; leak detector.
- **Compliance** (#10/#13 L74): export+purge coprono ogni entità con dati personali; manifesto allineato.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #04 2/5/6/9, #05 7bis/8/10/11, #09 A5/A6/E23/F30/F31, #13 A2/C15/L69/L70/L74.
- **DoD**:
  1. Backend app #1 verticale su `commons`, schema `app_<app_id>` con discriminator.
  2. Quota SPI (flow) + contratto GDPR (export/purge) + manifesto dati.
  3. API problem+json + OpenAPI; logging strutturato.
  4. Suite security/multi-tenancy + compliance verdi.
