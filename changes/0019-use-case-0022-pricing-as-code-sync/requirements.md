# Change 0019: Pricing-as-code + motore di sync (catalogo JPA, loader, sync contro stub)

**Branch**: `change/0019-use-case-0022-pricing-as-code-sync`
**Aree**: `services/core`, `dev/seed` (rimozione porzione catalogo da `seed.sql`)
**Data**: 2026-06-28
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/07-payments/0022-pricing-as-code-sincronizzazione.md](../../docs/usecases/07-payments/0022-pricing-as-code-sincronizzazione.md)
**Tocca dati personali?**: No — il catalogo è "cosa si vende" (app/tier/prezzi), dato di piattaforma non tenant-scoped, nessun PII. Nessun checkpoint privacy/RoPA.

## Problema / Obiettivo

Realizzare il **pricing-as-code**: la definizione versionata del catalogo (app/tier/limiti/feature/ciclo/prezzi, con **chiave
interna stabile**) vive nel repo ed è la **fonte di verità** del "cosa si vende"; un **motore di sync idempotente** la riconcilia
nel **catalogo DB** dell'env e poi verso **Paddle** (crea mancanti, archivia rimossi, **mai** muta l'importo di un price vivo,
grandfathering), riempiendo gli **ID Paddle per-ambiente** (`paddle_product_id`/`paddle_price_id`) nel DB. UC 0022 è anche il
**primo consumatore reale** del catalogo: porta le **entità JPA + repository** di `app`/`app_tier`/`app_price` (finora solo DDL,
UC 0013), sbloccando il mapping `paddle_price_id`→tier rimasto aperto in UC 0023.

**Confine deciso (gate di chiarimento):** si costruisce lo **slice offline completo ed esercitabile contro lo stub** (UC 0023);
il **client Paddle reale** (bloccato da #14) e il **cablaggio nel workflow CI** (UC 0005 non ancora realizzato) sono **fuori
scope** e tracciati con precisione (vedi "Fuori scope / differiti").

## Scope

Tutto in `services/core` salvo `dev/seed`.

**1. Entità JPA + repository del catalogo** (nuovo package `app.appgrove.core.catalog`), mappate sul DDL esistente
(`V2__core_domain.sql`, **nessuna nuova migrazione**), platform-level (**non** tenant-scoped):
- `App` (`platform.app`), `AppTier` (`platform.app_tier`), `AppPrice` (`platform.app_price`) + repository Panache.
- Repository con i lookup necessari, incluso `AppPriceRepository.findTierIdByPaddlePriceId(String)` (abilita il mapping
  `paddle_price_id`→tier per UC 0023/0025; il ri-cablaggio dello stub è differito — vedi sotto).

**2. Definizione pricing-as-code (YAML, posizione centrale nel core):**
- `services/core/src/main/resources/pricing/<slug>.yaml`, un file per app. Schema esplicito: `app` (slug, name, user_model,
  status, `paddle_product_id` **assente** dal codice) → `tiers[]` (key stabile, name, limits, features, trial_days) →
  `prices[]` (billing_cycle, amount in minor units, currency). I `paddle_*_id` **non** stanno nel codice (sono per-env, nel DB).
- File per le 4 app correnti (`notes`, `teams`, `legacy`, `fatture`) coerenti coi dati oggi in `seed.sql`.

**3. Motore di sync** (`PricingSyncService` nel package `catalog`), flusso orchestrato idempotente:
- **(a) load**: legge gli YAML del catalogo;
- **(b) upsert catalogo DB**: riconcilia `app`/`app_tier`/`app_price` (campi env-agnostici), con **UUID deterministici** derivati
  dalla chiave (UUIDv5 da `slug` / `slug:tierKey` / `slug:tierKey:cycle`) → ID stabili per E2E;
- **(c) riconcilia Paddle** via il **port `PaymentProvider`**: crea i mancanti, **archivia** i rimossi, **rifiuta** la mutazione
  dell'importo di un price vivo, **non** archivia price con subscription attive (grandfathering), e **scrive**
  `paddle_product_id`/`paddle_price_id` nel catalogo DB.

**4. Estensione del port `PaymentProvider`** con `syncPricing(...)`:
- `StubPaymentProvider`: ritorna ID Paddle **plausibili** (deterministici/offline) per product e price → la sync li persiste.
- `PaddlePaymentProvider`: **placeholder** non implementato (gated #14), coerente con `startCheckout`.

**5. Invocazione:**
- **`%dev`/`%test`: esecuzione automatica allo startup** (`@Observes StartupEvent`) → il catalogo locale è sempre =
  pricing-as-code (idempotente); rispetta l'invariante CLAUDE.md "Avvio locale … senza passi manuali".
- **Entrypoint runnable in command-mode** Quarkus (`sync-pricing`, non avvia il server HTTP) — ciò che il workflow CI
  invocherà (cablaggio a UC 0005).

**6. Seed (`dev/seed/seed.sql`) — Strada 1:** rimuovere la porzione **catalogo** (`app`/`app_tier`/`app_price`), ora prodotta dal
loader; restano account/users/invitations/subscription. Le righe `subscription` del seed referenziano gli UUID di tier/app →
garantire che gli UUID deterministici del loader **coincidano** con quelli oggi attesi (aggiornare i riferimenti se necessario;
aggiornare `dev/seed/README.md` se documenta gli ID di catalogo).

## Fuori scope / differiti (tracciati con precisione)

- **Client Paddle reale** (`PaddlePaymentProvider.syncPricing`: REST Product/Price API, secret per-env da Secrets Manager) —
  **bloccato da #14**. → UC 0022 "Punti aperti".
- **Cablaggio dello step `sync-pricing` nel workflow CI** (test→sandbox, tag→prod, dopo Flyway) — la pipeline non esiste. → UC
  0005 "Punti aperti".
- **Ri-cablaggio stub→`paddle_price_id`** (emitter porta il price id, consumer risolve il tier via catalogo, elimina
  l'`app_tier_id` esplicito) — abilitato qui (entità+lookup), non eseguito (territorio consumer UC 0025). → UC 0022 "Punti aperti".
- **Co-piloti `new-application`/`pricing-change`** che scrivono/aggiornano gli YAML (immutabilità/grandfathering interattivo,
  migrazione subscription) → UC 0046/0047; qui si congela solo il **formato YAML**.
- **Trattamento app fixture** (`notes`/`teams`/`legacy` senza service folder) **da ridiscutere** → UC 0022 "Punti aperti"
  (in coordinamento con il discovery per-cartella di UC 0046).

## Criteri di accettazione

- [ ] Esistono entità JPA + repository per `app`/`app_tier`/`app_price` mappate sul DDL esistente (nessuna nuova migrazione).
- [ ] La definizione pricing-as-code YAML (1 file/app, posizione centrale) descrive le 4 app correnti con chiave interna stabile.
- [ ] `PricingSyncService` è **idempotente** (ri-sync = stesso stato, nessun duplicato), **crea i mancanti**, **archivia i
      rimossi**, **rifiuta** la mutazione dell'importo di un price vivo, e **riempie** `paddle_product_id`/`paddle_price_id` nel
      catalogo DB (via stub).
- [ ] In `%dev`/`%test` il catalogo è popolato automaticamente allo startup; esiste l'entrypoint command-mode `sync-pricing`.
- [ ] La porzione catalogo è rimossa da `seed.sql` e gli UUID deterministici del loader sono coerenti con i riferimenti del seed
      (E2E/`SeedDataTest` verdi).
- [ ] `mvn test` di `services/core` verde, inclusi i test di sync (idempotenza, immutabilità, mappa chiave→ID, grandfathering).

## Invarianti appgrove toccati

- **Tenant_id dal JWT / filtro row-level**: il catalogo è **platform-level, non tenant-scoped** → le entità `App`/`AppTier`/
  `AppPrice` **non** estendono `BaseTenantEntity` e non portano `WHERE tenant_id` (corretto: non sono dati di tenant). Il
  grandfathering legge `subscription` (tenant-scoped) solo in aggregato per decidere se archiviare un price.
- **Modulo Terraform `microsaas_app`**: non toccato (nessuna nuova infra in questa change).
- **Logging strutturato**: la sync logga con `app_id` (e price/tier key) gli esiti (creati/archiviati/rifiutati); non c'è
  `tenant_id`/`user_id` (operazione di piattaforma, non per-tenant).

## Requisiti di test

- **Integration (L1-adiacente, offline via stub):** sync idempotente; crea mancanti/archivia rimossi; **rifiuta** mutazione
  importo di price vivo; `findTierIdByPaddlePriceId` mappa chiave→ID env; **grandfathering** (un price con subscription attiva
  non viene archiviato).
- **Loader/UUID determinismo:** stessi YAML → stessi UUID e stesso stato DB; coerenza con i riferimenti del seed.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (catalogo prima vuoto di logica JPA; seed riallineato nello stesso commit) |
| Contratto cross-area | N/A in runtime; **congela** il contratto formato YAML che UC 0046/0047 consumeranno |
| Version bump | minor (nuova capability di piattaforma, nessuna API pubblica rotta) |
