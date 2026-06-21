# UC 0022 — Pricing-as-code + sync pipeline (test→sandbox, tag→prod)

**Area**: 07-payments · **Fase**: 5 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0005](../02-devops-infra/0005-pipeline-cicd.md) (pipeline), UC [0013](../04-platform-core/0013-account-utenti-inviti-api.md) (catalogo)
**Fonte decisioni**: #09 B/H (catalogo/pricing-as-code/sync), #07 (deploy)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [09-pagamenti](../../09-pagamenti.md), [07-devops-cicd](../../07-devops-cicd.md), [04-platform-core/0013-account-utenti-inviti-api](../04-platform-core/0013-account-utenti-inviti-api.md)

## 1. Obiettivo / Scope
Implementare il **pricing-as-code** (definizione del "cosa si vende" nel repo) e la **pipeline di sync** verso Paddle, agganciata
al deploy.
**Incluso**: definizione versionata (app/tier/limiti/ciclo/prezzi) nel repo, prodotta dal co-pilota `new-application`/`pricing-change`;
**sync idempotente** che mappa **chiave interna stabile** → ID Paddle dell'ambiente (`paddle_product_id`/`paddle_price_id` nel DB
catalogo dell'env); **deploy test → sync sandbox**, **tag→prod → sync production**; immutabilità prezzi (nuovo Price + archivia,
grandfathering). Mapping **1 app = 1 Product**, **(tier × ciclo) = 1 Price**.
**Escluso**: il checkout (UC 0024), i webhook (UC 0025), le skill co-pilota in sé (UC 0046/0047), lo stub locale (UC 0023).

## 2. Attori & ruoli
- **Pipeline CI** (UC 0005): esegue lo step di sync per ambiente.
- **Paddle** (sandbox/production): riceve product/price.
- **Co-pilota** (`new-application`/`pricing-change`): produce/aggiorna la definizione.

## 3. Precondizioni
- Catalogo nel core (UC 0013) con chiavi interne; account Paddle attivo per l'env (UC 0001) — sandbox per test, production per prod;
  secret Paddle per env (Secrets Manager, #09 I38). In assenza di account → solo stub locale (UC 0023).

## 4. Flusso principale
1. La **definizione pricing** (importi/tier/limiti/ciclo) vive nel repo, uguale per tutti gli env; ogni price ha una **chiave interna stabile** (#09 H37).
2. **Deploy su test** → step di sync verso **Paddle sandbox**: crea i mancanti, **archivia** i rimossi, **mai** muta importo di un price vivo, non cancella price con subscription attive (grandfathering) (#09 H37).
3. Il sync **riempie** `app_price.paddle_price_id` (e `app.paddle_product_id`) nel **DB catalogo dell'env** (ID per-ambiente, prezzo no) (#09 H37, B10).
4. **Tag→prod** → stesso sync verso **Paddle production** (#09 H37, #07).
5. Mapping: **1 app = 1 Product**, **(tier × ciclo) = 1 Price**; limiti/feature **non** stanno in Paddle (vivono nel DB) (#09 B8).

## 5. Flussi alternativi / edge / errori
- **Cambio prezzo**: **nuovo Price + archivia il vecchio**; esistenti **grandfathered** salvo migrazione esplicita (#09 H35) — gestito da `pricing-change` (UC 0047).
- **Immutabilità violata**: il sync **rifiuta** di mutare l'importo di un price vivo (#09 H37).
- **Idempotenza**: ri-sync non duplica; allinea solo i delta.
- **Niente editor runtime**: il pannello admin è read-only/observability sul drift (#09 H34, UC 0021).

## 6. Risorse & runbook
**Artefatti**: definizione pricing-as-code nel repo; step di sync nella pipeline (UC 0005); colonne ID Paddle nel catalogo (UC 0013).
**Runbook**: modifica pricing via `new-application`/`pricing-change` (branch+PR) → merge: sync sandbox → tag: sync production.
Stesso flusso PR→test→tag→prod di tutto il resto.

## 7. Dati toccati
Catalogo `app`/`app_tier`/`app_price` (+ ID Paddle per env). Nessun dato personale (è "cosa si vende"). Secret Paddle per env
(mai in codice/log). Manifest: N/A (catalogo, non dati utente).

## 8. Permessi & gate
- **Invarianti**: catalogo platform-level (non tenant-scoped); ID Paddle per env nel DB di quell'env.
- **Gate**: la sync rispetta immutabilità/grandfathering; cambi prezzo passano da PR (review/audit/rollback) (#09 H35).

## 9. Requisiti di test
- **Integration (L1-adiacente)**: sync idempotente; crea mancanti/archivia rimossi; rifiuta mutazione importo vivo; mappa chiave→ID env.
- Con **stub locale** (UC 0023) la sync si esercita offline (API Paddle finta).
- Verifica grandfathering (subscription esistenti restano sul vecchio price).

## 10. Riferimenti & Definition of Done
- **Decisioni**: #09 B8/B10, H34/35/36/37, I38, #07.
- **DoD**:
  1. Pricing-as-code nel repo (chiave interna stabile); 1 app=1 Product, (tier×ciclo)=1 Price.
  2. Sync idempotente test→sandbox, tag→prod; riempie ID Paddle per env nel catalogo.
  3. Immutabilità prezzi + grandfathering rispettati; mai mutare importo vivo.
  4. Test sync (idempotenza/immutabilità) verdi (anche via stub locale).
