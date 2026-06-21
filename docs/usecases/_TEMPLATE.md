# UC NNNN — Titolo breve

**Area**: XX-area · **Fase**: N · **Stato**: 🔴 da scrivere / 🟡 in corso / 🟢 deciso
**Dipendenze**: UC NNNN (…), UC NNNN (…) — cosa deve esistere prima
**Fonte decisioni**: #NN (…), #NN (…)
**Ultimo aggiornamento**: AAAA-MM-GG

> Use case scaffoldato da `new-usecase`. Compilare le sezioni; le UC non-UI/devops adattano "Schermate & stati" →
> "Risorse & runbook". Cancellare questa nota a stesura conclusa.

## 1. Obiettivo / Scope
_Cosa realizza questo use case e perché (1-3 frasi). Cosa è incluso / escluso._

## 2. Attori & ruoli
_Chi interagisce: utente B2C, owner/admin/member B2B, platform-admin, sistema/cron, terzi (Paddle/SES…)._

## 3. Precondizioni
_Stato richiesto prima del flusso (auth, entitlement, dati esistenti, env attivo…)._

## 4. Flusso principale
1. …
2. …

## 5. Flussi alternativi / edge / errori
- **Edge**: …
- **Errore**: … → comportamento (problem+json / messaggio / retry).

## 6. Schermate & stati  _(UI)_  ·  oppure  ·  Risorse & runbook  _(devops/infra)_
_UI: schermate, stati (loading/empty/error/success), copy chiave, banner._
_DevOps: risorse create/modificate, comandi/script, runbook, rollback._

## 7. Dati toccati
_Entità/tabelle/schema. **Se dati personali** → categoria + finalità + base giuridica + retention (manifest GDPR #13 C);
marcare i campi `@PersonalData`._

## 8. Permessi & gate
_Entitlement (accesso app), ruolo (`@RolesAllowed`), quota (flow/stock). Invarianti multi-tenancy: `tenant_id` solo da
JWT, filtro row-level. Diritti GDPR esenti dai gate dove applicabile (#09 F31)._

## 9. Requisiti di test
_Unit / integration (Testcontainers) / security-isolamento cross-tenant / E2E Playwright / L1-L3 pagamenti — secondo #10.
Cosa DEVE essere verde prima del merge._

## 10. Riferimenti & Definition of Done
- **Decisioni**: #NN …
- **DoD**: criteri oggettivi di completamento (funzionale + test verdi + doc/manifest aggiornati).
