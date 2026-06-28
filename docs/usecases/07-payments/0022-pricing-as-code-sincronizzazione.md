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

## Punti aperti / decisioni differite

_Tracciato dalla change `0007-use-case-0013-…` (regola CLAUDE.md "Tracciamento delle decisioni differite")._

- **Entità JPA del catalogo (`app`/`app_tier`/`app_price`).** UC 0013 crea solo il **DDL Flyway** delle tabelle di
  catalogo (decisione di scope della change 0007: niente mapping speculativo, #09 H34 = nessun editor runtime). Le
  **entità JPA + repository** del catalogo vanno modellate **qui** (UC 0022), che è il primo consumatore reale (sync
  pricing-as-code le scrive). **Proprietario**: UC 0022.
- **Mapping `paddle_price_id` → `app_tier_id` per lo stub locale.** Tracciato dalla change `0018-use-case-0023-…`: lo stub
  Paddle (UC 0023), negli scenari upgrade/downgrade, passa per ora l'`app_tier_id` target **esplicito** nei `custom_data`
  perché il mapping `paddle_price_id`→tier richiede le **entità JPA del catalogo** (sopra). **La change 0019 fornisce le
  entità + il lookup** (`AppPriceRepository.findTierIdByPaddlePriceId`), quindi il mapping è ora **disponibile**.
  **Residuo (non fatto in 0019, tracciato qui):** ri-cablare emitter+consumer dello stub perché il webhook porti il
  `paddle_price_id` e il consumer (`SubscriptionWriter`) risolva il tier via catalogo **come in prod**, eliminando
  l'`app_tier_id` esplicito dai `custom_data`. Non fatto in 0019 per non destabilizzare il consumer webhook (territorio di
  **UC 0025**) e perché richiede i price già sincronizzati. **Proprietario**: UC 0022, in coordinamento con UC 0025.

_Residui tracciati dalla change `0019-use-case-0022-…` (confine deciso: slice offline completo ora; reale Paddle + CI differiti)._

- **Client Paddle reale per la sync (`PaddlePaymentProvider.syncPricing`).** La change 0019 implementa il **motore di sync**
  contro il **port `PaymentProvider`** e lo esercita offline con lo **stub** (`StubPaymentProvider` ritorna ID Paddle
  plausibili e li scrive nel catalogo DB). Il `PaddlePaymentProvider` reale — chiamate REST a **Product/Price API di Paddle**
  (crea mancanti, archivia rimossi, mai mutare importo vivo) + lettura secret API key per-env da Secrets Manager (#09 I38) —
  resta **placeholder**: è **bloccato da #14** (nessun account Paddle, nemmeno sandbox, finché sito+ToU/PP non sono pubblicati).
  Va completato **quando #14 sblocca l'account**. **Proprietario**: UC 0022. *(Il `startCheckout` reale è invece di UC 0024,
  la robustezza webhook prod di UC 0025: stesso `PaddlePaymentProvider` riempito per-metodo dai rispettivi UC.)*
- **Cablaggio dello step di sync nel deploy CI** → tracciato in **UC 0005** ("Punti aperti"): la pipeline non esiste ancora,
  quindi la change 0019 fornisce l'**entrypoint runnable** in command-mode (`sync-pricing`), non il job GitHub Actions.
  **Proprietario** del cablaggio: UC 0005.
- **Trattamento delle app "fixture" (`notes`/`teams`/`legacy`) nel pricing-as-code centrale.** Solo `fatture` è un'app reale
  con cartella `services/fatture`; `notes`/`teams`/`legacy` sono **fixture** del seed E2E senza service folder. Nella change
  0019 il pricing-as-code centrale (`services/core/.../pricing/<slug>.yaml`) le ospita comunque (file YAML per ciascuna) per
  preservare gli asset E2E con la Strada 1 (loader fonte unica, UUID deterministici da chiave). **Da ridiscutere** (sollevato
  dal Platform Engineer): se le fixture debbano restare definizioni pricing-as-code di prima classe o essere isolate come
  semplici fixture di test separate dal catalogo "reale" — si lega al discovery per-cartella-service di **UC 0046**.
  **Proprietario**: UC 0022 (in coordinamento con UC 0046).
- **Co-piloti che producono gli YAML** (`new-application` scrive il pricing iniziale, `pricing-change` gestisce i cambi con
  immutabilità/grandfathering) → **UC 0046/0047**: la change 0019 definisce e congela il **contratto/formato YAML** che quelle
  skill scriveranno, ma **non** le implementa. **Proprietario**: UC 0046/0047.
- **Migrazione esplicita di subscription esistenti a un nuovo price** (oltre il grandfathering di default) → gestita da
  `pricing-change` (**UC 0047**); la change 0019 implementa solo il **default grandfathering** (la sync non archivia/cancella
  un price con subscription attive). **Proprietario**: UC 0047.
