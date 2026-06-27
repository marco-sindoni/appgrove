# Change 0015: Backend App #1 (`fatture`) — verticale Quarkus su `commons`

**Branch**: `change/0015-use-case-0051-app1-backend`
**Aree**: `services/commons` (contratti SPI), `services/fatture` (nuovo modulo), `services/pom.xml`, `dev/seed/seed.sql`
**Data**: 2026-06-27
**Autore**: Platform Engineering
**Use case sorgente**: [`docs/usecases/11-apps/0051-app1-backend.md`](../../docs/usecases/11-apps/0051-app1-backend.md)
**Tocca dati personali?**: **Sì** — i contenuti dell'app (nominativo/email cliente sulle fatture) sono dati personali dell'utente (postura uniforme #13 A2, base giuridica **contratto**). Si applica il checkpoint privacy/RoPA di step-03 (classifica → manifesto/RoPA → bump PP/ToS); il gate CI bloccante è UC 0031 (non ancora attivo).

## Problema / Obiettivo

Implementare il **backend della prima app reale** (`fatture`, B2C **single-user**) come verticale Quarkus che eredita tutte le invarianti dal `commons`. Questa è l'app che **valida manualmente il pattern** poi industrializzato dalla skill `new-application` (UC 0046). Oltre al verticale, la change **introduce in `commons` i tre contratti SPI** che oggi non esistono ancora e che gli UC a valle consumeranno: quota, GDPR per-app, e l'annotazione `@PersonalData` per il manifesto dati.

## Scope

### A. Contratti SPI in `commons` (solo interfacce/annotazioni, nessuna logica applicativa)
1. **Quota SPI**: interfaccia `QuotaService` con `checkAndReserve(metric)` (natura **flow**/**stock**), più una `QuotaLimitSource` (seam per il tetto) e un'eccezione `QuotaExceededException`. Mapper problem+json → **429** in `commons`.
2. **GDPR SPI per-app**: interfaccia `AppDataContract` con `exportData(scope)` (con step dichiarati per il progress) e `purgeData(scope)`.
3. **Manifesto dati**: annotazione `@PersonalData` (campo) + un piccolo tipo `DataManifest` (categorie/finalità/base/retention) esposto dall'app.

### B. Nuovo modulo `services/fatture/` (verticale, package-by-layer)
4. **Scaffold** modulo Maven dipendente da `commons`; registrazione in `services/pom.xml` (`<modules>`); `quarkus.application.name=fatture`, `quarkus.http.port=8081`; OpenAPI store in `META-INF/openapi`; schema Flyway **`app_fatture`**; `app_id` aggiunto all'MDC (logging strutturato).
5. **Schema `app_fatture`** (Flyway `V1__…`, di proprietà del servizio): due entità tenant-scoped con discriminator `tenant_id`, audit + soft-delete, PK UUID v7:
   - **`Invoice`** (`invoice`): `number` (progressivo per-tenant per-anno, generato server-side, es. `2026-0001`), `customer_name` **@PersonalData**, `customer_email` **@PersonalData** (nullable), `issue_date`, `status` (`draft|issued|paid|void`), `currency` (default `EUR`), `total_amount`.
   - **`InvoiceLine`** (`invoice_line`): FK → `invoice`, `description`, `quantity`, `unit_amount`, `line_amount`.
6. **Quota (flow)**: implementazione `QuotaService` per la metrica `fatture`; l'**uso** = conteggio `Invoice` create nella finestra (mese di calendario corrente) in `app_fatture`; il **tetto** arriva da `QuotaLimitSource` con implementazione **locale config-driven** (`app.fatture.quota.fatture.cap`, default **10**). `checkAndReserve("fatture")` è invocato **prima della creazione** della fattura (`POST`).
7. **Contratto GDPR**: `AppDataContract` implementato per `fatture` → `exportData` (Invoice + righe del tenant, con step di progress), `purgeData` (cancellazione **fisica** di Invoice + InvoiceLine del tenant, **senza orfani**, con audit log) + **manifesto dati** dichiarato.
8. **API** `/api/fatture/v1/invoices` problem+json + OpenAPI: `POST` (consuma quota), `GET` (lista paginata), `GET /{id}`, `PATCH /{id}`, `DELETE /{id}` (soft-delete). `@Authenticated`; identità caller solo dal JWT.

### C. Catalogo & seed
9. Aggiungere l'app **`fatture`** (`user_model = single_user`, `status = active`) a `platform.app` e un `app_tier` `free` con `limits = {"metric":"fatture","window":"month","cap":10,"type":"flow"}` in `dev/seed/seed.sql` (UUID di seed stabili, idempotente `ON CONFLICT`).

## Fuori scope

- **Frontend modulo** (UC 0052), **landing** (UC 0053).
- **Enforcement runtime entitlement/quota di piattaforma** (UC 0027): la **risoluzione del tetto dall'entitlement** (`subscription → tier → limits`) e il wiring del `QuotaLimitSource` "reale" sono di UC 0027 → **decisione differita registrata** in `docs/usecases/07-payments/0027-*.md`. In 0051 il tetto è config-driven locale.
- **Authorizer/gate app-abilitata** (UC 0014): in locale non c'è il 402/403 a monte; il servizio applica solo ruolo + quota.
- **RoPA automation / gate privacy CI** (UC 0030/0031): qui si forniscono `@PersonalData` + manifesto; l'industrializzazione è a valle.
- **Skill `new-application`** (UC 0046): questa app valida il pattern manualmente.
- **Letture cross-schema** da `fatture` verso `platform`: vietate (isolamento di schema, ruolo least-privilege su `app_fatture`).

## Criteri di accettazione

- [ ] `services/fatture` compila ed è registrato nel reactor; `mvn test` verde; `commons` invariato a valle (le SPI sono additive).
- [ ] Le tre SPI (`QuotaService`/`QuotaLimitSource`/`QuotaExceededException`, `AppDataContract`, `@PersonalData`+`DataManifest`) vivono in `commons` e sono implementate in `fatture`.
- [ ] Schema `app_fatture` via Flyway con `Invoice`/`InvoiceLine` tenant-scoped (discriminator), audit, soft-delete, PK UUID v7.
- [ ] `POST` fattura: alla 11ª fattura del mese (cap 10) → **429** problem+json; il conteggio rispetta la finestra mensile.
- [ ] `exportData` copre Invoice + righe del tenant; `purgeData` cancella tutto senza lasciare righe orfane; entrambe solo dati del tenant chiamante.
- [ ] API in problem+json + spec OpenAPI committato; ogni log porta `tenant_id`/`app_id`/`user_id`.
- [ ] App `fatture` + `app_tier` free (cap 10) presenti nel seed.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT**: entità estendono `BaseTenantEntity` (`@TenantId`); identità caller via `CallerContext`/JWT; mai da body/params. Test anti-override.
- **Filtro row-level** `WHERE tenant_id`: automatico dal discriminator Hibernate del `commons`; nessun filtro manuale; quota/export/purge operano solo sul tenant corrente.
- **Modulo Terraform `microsaas_app`**: N/A in questa change (cloud/UC 0004). Lo schema `app_fatture` e il ruolo least-privilege sono allineati al pattern del modulo, ma l'infra è fuori scope locale.
- **Logging strutturato**: `MdcRequestFilter` del `commons` valorizza `tenant_id`/`user_id`/`request_id`; `fatture` aggiunge `app_id` all'MDC.

## Requisiti di test

- **Integration (Testcontainers + Flyway)**: CRUD fatture; quota flow con reset finestra (10 ok → 429 → conteggio per mese); export/purge senza dati orfani.
- **Security/multi-tenancy** (harness ereditato): tenant A non vede dati di B (lista, get, quota, export); anti-override `tenant_id` nel body; il purge di A non tocca B.
- **Compliance**: export+purge coprono **ogni** entità con dati personali (Invoice + righe); test che verifica che ogni `@PersonalData` sia coperto dall'export (allineamento manifesto).
- **ArchUnit**: le entità di `fatture` estendono `BaseTenantEntity`/`BaseEntity` (riuso delle regole esistenti).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (tutto additivo: nuovo modulo + SPI nuove in `commons`) |
| Contratto cross-area | Sì — nuove SPI in `commons` consumate a valle (UC 0027/0030/0031/0032); nuova API `/api/fatture/v1/*` (frontend UC 0052) |
| Version bump | minor (nuove SPI in `commons`, nuovo modulo) |
