# Implementation Log — Change 0019: Pricing-as-code + motore di sync

**Branch**: `change/0019-use-case-0022-pricing-as-code-sync`
**Aree**: `services/core`, `dev/` (script seed), `dev/seed`, `docs/usecases`, root (`run-tests.sh`, `CLAUDE.md`)
**Completata**: 2026-06-28

## File modificati

| File | Azione |
|---|---|
| `services/core/src/main/java/app/appgrove/core/catalog/CatalogIds.java` | Creato — UUID deterministici dalla chiave |
| `services/core/src/main/java/app/appgrove/core/catalog/App.java` · `AppTier.java` · `AppPrice.java` | Creati — entità JPA del catalogo (read-model) |
| `services/core/src/main/java/app/appgrove/core/catalog/AppStatus.java` · `AppUserModel.java` · `BillingCycle.java` | Creati — enum |
| `services/core/src/main/java/app/appgrove/core/catalog/AppRepository.java` · `AppTierRepository.java` · `AppPriceRepository.java` | Creati — repository (`findTierIdByPaddlePriceId` in SQL nativo) |
| `services/core/src/main/java/app/appgrove/core/catalog/PricingDefinition.java` · `PricingCatalogLoader.java` | Creati — modello YAML + loader |
| `services/core/src/main/java/app/appgrove/core/catalog/PricingSyncService.java` | Creato — motore di sync (idempotenza/immutabilità/grandfathering/archivio + riconciliazione port) |
| `services/core/src/main/java/app/appgrove/core/catalog/PricingSyncStartup.java` | Creato — sync allo startup in dev/test |
| `services/core/src/main/java/app/appgrove/core/CoreMain.java` | Creato — entrypoint command-mode `sync-pricing` |
| `services/core/src/main/resources/pricing/{index,fatture}.yaml` | Creati — pricing-as-code app **reali** (vanno in prod) |
| `services/core/src/main/resources/pricing/fixtures/{index,notes,teams,legacy}.yaml` | Creati — app **fixture** (solo dev/test/E2E, mai prod) |
| `services/core/src/main/java/app/appgrove/core/billing/PaymentProvider.java` | Modificato — aggiunto `syncPricing` + record |
| `services/core/src/main/java/app/appgrove/core/billing/StubPaymentProvider.java` | Modificato — `syncPricing` (ID deterministici offline) |
| `services/core/src/main/java/app/appgrove/core/billing/PaddlePaymentProvider.java` | Modificato — `syncPricing` placeholder (gated #14) |
| `services/core/pom.xml` · `application.properties` (main+test) | Modificati — dep YAML + `sync-on-startup` |
| `services/core/src/test/java/app/appgrove/core/catalog/{CatalogIdsTest,PricingCatalogLoaderTest,PricingSyncServiceTest,PricingCatalogRealOnlyTest}.java` | Creati — test UC 0022 (incl. guardia prod-only) |
| `services/core/src/test/.../SeedDataTest.java` · `AdminApiTest.java` | Modificati — catalogo ora dal loader (`created_by='sync'`, UUID deterministici) |
| `dev/seed/seed.sql` | Modificato — rimossi catalogo **e** subscription; resta l'identità (accounts/users/invitations) |
| `dev/seed/seed-subscriptions.sql` | Creato — subscription (FK sul catalogo), applicato solo dove il catalogo esiste |
| `dev/lib/seed.sh` · `dev/seed/README.md` | Modificati — step `sync-pricing` (sempre-rebuild dev + watchdog) + applica i due file seed |
| `run-tests.sh` | Creato — entrypoint unico "tutti i test di tutti i moduli" (backend/frontend/infra) |
| `CLAUDE.md` | Modificato — sezione non-negoziabile "Esecuzione dei test" (mantenere `run-tests.sh` aggiornato) |
| `services/core/.../SeedDataTest.java` · `AdminApiTest.java` | Modificati — applicano `seed.sql` + `seed-subscriptions.sql` |
| `docs/usecases/07-payments/0022-*.md` · `docs/usecases/02-devops-infra/0005-*.md` · `_INDEX.md` | Modificati — tracciamento differiti + stato ✅ |

## Cosa è stato fatto

Implementato il **pricing-as-code** (definizione YAML del catalogo in `services/core/.../pricing/`, fonte di verità) e il
**motore di sync** (`PricingSyncService`): carica gli YAML, fa **upsert** idempotente di `app`/`app_tier`/`app_price` nel
catalogo DB con **UUID deterministici** dalla chiave (`CatalogIds`), applica **immutabilità prezzi** (rifiuta la mutazione
dell'importo di un price vivo), **grandfathering** (non archivia price/tier con subscription attive), **archivio** dei rimossi,
e **riconcilia** gli ID Paddle per-ambiente via il port `PaymentProvider` (stub offline). UC 0022 porta anche le **entità JPA +
repository** del catalogo (finora solo DDL, UC 0013), incluso `findTierIdByPaddlePriceId`. La sync gira **allo startup** in
dev/test e via l'entrypoint **command-mode `sync-pricing`** (che la CI invocherà — UC 0005). Seed "Strada 1": il catalogo non è
più in `seed.sql` ma prodotto dal loader; le subscription del seed referenziano gli UUID deterministici.

## Decisioni prese

- **Scritture in SQL nativo** (come `SubscriptionWriter`): la sync e `findTierIdByPaddlePriceId` girano **fuori da una richiesta
  autenticata** (consumer webhook UC 0025, command-mode), dove il `TenantResolver` (DISCRIMINATOR) è fail-closed e una query
  Hibernate fallirebbe. Le entità JPA restano il read-model in-richiesta.
- **Build-time provider** (`@IfBuildProperty`): `dev seed` builda il core in **profilo dev** per includere lo stub (un build
  prod selezionerebbe il `PaddlePaymentProvider` placeholder). Il jar è **sempre ricostruito** (un jar stantio senza command-mode
  girerebbe come server e bloccherebbe il seed) + **watchdog** 180s di sicurezza.
- **Archivio scoped per-app**: rimuovere un price/tier dallo YAML di un'app lo archivia; la rimozione di un'**intera app** è fuori
  scope (tracciata).
- **App fixture non-prod** (risolto il punto sollevato dal PE): `notes`/`teams`/`legacy` sono fixture sintetiche in
  `pricing/fixtures/`, caricate solo con `include-fixtures=true` (`%dev`/`%test`); le app reali in `pricing/` vanno in prod. Così
  la sync di **produzione crea Product Paddle solo per app reali** (oggi `fatture`). Guardia: `PricingCatalogRealOnlyTest`.
- **Seed identità vs billing** (fix regressione auth-local): le subscription dipendono dal catalogo (FK), quindi sono in
  `seed-subscriptions.sql` separato, applicato **solo** dove il catalogo esiste (core test, dev/E2E). I servizi di sola identità
  (auth-local) applicano solo `seed.sql` → niente FK rotta. La regressione è stata **scoperta da `run-tests.sh`** (eseguendo
  tutti i moduli, non solo core).
- **`run-tests.sh` + regola in CLAUDE.md** (richiesta del PE in questa change): entrypoint unico per tutti i test di tutti i
  moduli, da mantenere aggiornato (DoD di `new-change`).

## Invarianti appgrove

- **Tenant_id dal JWT / filtro row-level**: il catalogo è **platform-level, non tenant-scoped** → entità senza `BaseTenantEntity`,
  nessun `WHERE tenant_id` (corretto). Il grandfathering legge `subscription` (tenant-scoped) solo in aggregato.
- **Logging strutturato**: la sync logga `app_id` + counts (creati/archiviati); non c'è `tenant_id`/`user_id` (op di piattaforma).
- **Modulo Terraform `microsaas_app`**: non toccato.

## Note per il revisore

- **Cross-area**: nessun contratto runtime frontend↔API. La change **congela il formato YAML** che UC 0046/0047 consumeranno.
- **Scope ampliato su richiesta del PE** (oltre il `requirements.md` iniziale): split app fixture non-prod, `run-tests.sh` +
  regola CLAUDE.md, split seed identità/billing (che ha sanato una regressione auth-local emersa con `run-tests.sh`).
- **Migrazione dev**: un DB dev con catalogo legacy (UUID vecchi) richiede `./dev.sh reset` una tantum (slug duplicato altrimenti).
- **Decisioni differite tracciate** (regola CLAUDE.md):
  - UC 0022 "Punti aperti": client Paddle reale (`syncPricing`, bloccato #14) · ri-cablaggio stub→`paddle_price_id` (abilitato,
    esecuzione con UC 0025) · invocazione one-shot industrializzata (UC 0005/0046) · co-piloti `new-application`/`pricing-change`
    (UC 0046/0047) · migrazione subscription esplicita (UC 0047). *(Il trattamento delle app fixture è stato **risolto** in
    questa change: fixture non-prod gated `include-fixtures`.)*
  - UC 0005 "Punti aperti": cablaggio dello step `sync-pricing` nel workflow CI.

## Test

- **services/core** (`mvn test`): **50/50 verdi** (50 run, 0 fail, 0 error). Nuovi: `CatalogIdsTest` (contratto UUID = FK seed),
  `PricingCatalogLoaderTest` (load YAML reali+fixture in test), `PricingSyncServiceTest` (idempotenza, mappa chiave→ID,
  immutabilità, grandfathering, archivio), `PricingCatalogRealOnlyTest` (guardia: in prod solo app reali, niente fixture).
  Aggiornati `SeedDataTest`/`AdminApiTest` per il catalogo da loader.
- **Verifica locale (DoD)**: `./dev.sh seed` end-to-end OK — `sync-pricing` completata `apps=4 tiers=5 prices=4 archived=0`,
  catalogo popolato (`created_by='sync'`, UUID = `CatalogIds`), `paddle_*_id` riempiti dallo stub, 5 subscription del seed con FK
  risolte, lookup `paddle_price_id`→tier funzionante.
- **dev/seed.sh** (shell) e **YAML**: nessuna suite dedicata; validati dall'esecuzione end-to-end sopra + `SeedDataTest`.
- **Suite completa** (`./run-tests.sh`): **verde** — backend `services/*` (commons 3 · core 50 · auth-local 20 · fatture 18,
  BUILD SUCCESS) + frontend (50, 16 file vitest) + infra (saltata: terraform non installato in locale, gira in CI). auth-local
  era rosso prima del fix del seed split → ora verde.

## Stato criteri di accettazione

- [x] Entità JPA + repository per `app`/`app_tier`/`app_price` sul DDL esistente (nessuna nuova migrazione).
- [x] Pricing-as-code YAML (1 file/app, centrale) per le 4 app, con chiave interna stabile.
- [x] Sync idempotente; crea mancanti, archivia rimossi, rifiuta mutazione importo vivo, riempie `paddle_*_id`.
- [x] Catalogo popolato automaticamente allo startup in dev/test; entrypoint command-mode `sync-pricing`.
- [x] Porzione catalogo rimossa da `seed.sql`; UUID deterministici coerenti coi riferimenti del seed (E2E/`SeedDataTest` verdi).
- [x] `mvn test` di `services/core` verde, inclusi i test di sync.
