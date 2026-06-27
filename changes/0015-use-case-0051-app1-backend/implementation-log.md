# Implementation Log — Change 0015: Backend App #1 (`fatture`)

**Branch**: `change/0015-use-case-0051-app1-backend`
**Aree**: `services/commons`, `services/fatture` (nuovo modulo), `services/pom.xml`, `services/core` (test seed), `dev/seed/seed.sql`, `docs`
**Completata**: 2026-06-27

## File modificati

| File | Azione |
|---|---|
| `services/commons/.../quota/{QuotaNature,QuotaExceededException,QuotaLimitSource,QuotaService}.java` | Creato |
| `services/commons/.../web/QuotaExceededMapper.java` | Creato (429 problem+json) |
| `services/commons/.../privacy/PersonalData.java` | Creato (annotazione) |
| `services/commons/.../gdpr/{AppDataContract,GdprScope,DataManifest,ExportResult,PurgeResult}.java` | Creato |
| `services/fatture/pom.xml` + `services/pom.xml` (`<module>`) | Creato / Modificato |
| `services/fatture/src/main/resources/application.properties` | Creato |
| `services/fatture/.../db/migration/V1__create_app_fatture_schema.sql`, `V2__fatture_domain.sql` | Creato |
| `services/fatture/.../{Invoice,InvoiceLine,InvoiceStatus,CallerContext,Roles}.java` | Creato |
| `services/fatture/.../{InvoiceRepository,InvoiceDtos,InvoiceResource}.java` | Creato |
| `services/fatture/.../{ConfigQuotaLimitSource,FattureQuotaService,FattureDataContract,AppIdMdcFilter}.java` | Creato |
| `services/fatture/src/main/resources/META-INF/openapi/openapi.{yaml,json}` | Creato (generato) |
| `services/fatture/src/test/...` (5 classi + risorse + chiavi JWT) | Creato |
| `dev/seed/seed.sql` | Modificato (app `fatture` + tier free cap 10) |
| `services/core/.../SeedDataTest.java` | Modificato (conteggi app 3→4, tier 4→5 + assert `fatture`) |
| `docs/usecases/07-payments/0027-*.md`, `11-apps/0051-*.md`, `_INDEX.md` | Modificato |

## Cosa è stato fatto

Implementato il backend della prima app reale (`fatture`, B2C single-user) come verticale Quarkus su
`commons` (schema `app_fatture`, entità `Invoice`/`InvoiceLine` tenant-scoped, API `/api/fatture/v1/*`
problem+json + OpenAPI). Introdotti in `commons` i tre contratti SPI mancanti — **quota**
(`QuotaService`/`QuotaLimitSource`/`QuotaExceededException` → 429), **GDPR per-app** (`AppDataContract`
export/purge), **privacy** (`@PersonalData` + `DataManifest`) — e implementati in `fatture`. Quota
flow (10 fatture/mese, tetto config-driven locale); export/erasure GDPR via JDBC diretto (tenant
esplicito); manifesto derivato dalle annotazioni.

## Decisioni prese

- **SPI in `commons`, implementazione in `fatture`** (concordato): contratto stabile per i consumatori a
  valle (UC 0027/0030/0031/0032).
- **Quota**: meccanismo completo in `fatture`, **tetto** da `ConfigQuotaLimitSource` config-driven
  (`app.fatture.quota.fatture.cap=10`); la risoluzione dall'entitlement è differita a **UC 0027**.
- **GDPR export/purge via JDBC diretto** (`AgroalDataSource`), non via `EntityManager`: girano fuori da
  una richiesta (tenant esplicito), dove il discriminator `@TenantId` sarebbe fail-closed senza JWT.
  Stesso razionale di `TestData` nel core.
- **Numerazione** `number` per-tenant/anno dal max suffisso (query nativa, monotòna).

## Invarianti appgrove

- **tenant_id solo dal JWT**: entità su `BaseTenantEntity` (`@TenantId`); identità via `CallerContext`;
  test anti-override (`tenant_id` nel body ignorato) e fail-closed (token senza tenant → 403).
- **Filtro row-level**: automatico dal discriminator; quota/CRUD operano solo sul tenant corrente;
  export/purge filtrano per `tenant_id` esplicito.
- **Modulo `microsaas_app`**: N/A (infra cloud, UC 0004); schema `app_fatture` + ruolo least-privilege
  allineati al pattern del modulo.
- **Logging strutturato**: `MdcRequestFilter` (commons) per `tenant_id`/`user_id`/`request_id`;
  `AppIdMdcFilter` aggiunge `app_id=fatture`.

## Note per il revisore

- **Contratto cross-area**: nuove SPI in `commons` (consumate da UC 0027/0030/0031/0032) e nuova API
  `/api/fatture/v1/*` (consumata dal frontend modulo UC 0052). `QuotaExceededMapper` (429) è un provider
  generico aggiunto a `commons`: innocuo per `core` (non lancia mai `QuotaExceededException`).
- **Seed condiviso**: aggiunta app `fatture` + tier free a `dev/seed/seed.sql`; aggiornati i conteggi in
  `SeedDataTest` di core (app 3→4, app_tier 4→5; app_price/subscription invariati).
- **Decisioni differite tracciate**:
  - UC **0027** — risoluzione del tetto di quota dall'entitlement + gate 402/stock (seam `QuotaLimitSource`
    già pronto).
  - UC **0051** (punti aperti) — semplificazioni quota (conteggio non-deleted) e numerazione; PP/RoPA
    bump di competenza UC 0030/0002/0056.
- **Privacy/RoPA checkpoint**: la change introduce dati personali (`customer_name`/`customer_email`, base
  contratto) classificati con `@PersonalData` + manifesto. Bump PP/ToS e RoPA non azionabili ora (UC 0002
  legali e UC 0030 RoPA non implementati, Paddle non attivo) → tracciato.

## Test

Tutte le suite delle aree toccate sono **verdi** (Quarkus Dev Services + Testcontainers, Postgres 17):

- **`services/commons`** — `mvn test`: 3/3 ✅ (SPI additive, nessuna regressione).
- **`services/fatture`** — `mvn test`: 11/11 ✅
  - `InvoiceApiTest` (3): CRUD, numerazione server-side, validazione 400.
  - `QuotaTest` (1): 10 creazioni ok → 11ª **429** problem+json.
  - `MultiTenancyTest` (3): isolamento A/B, anti-override `tenant_id`, fail-closed 403.
  - `GdprContractTest` (2): export copre `invoice`+`invoice_line` allineato al manifesto; purge fisico
    scoped al tenant, senza orfani, B intatto.
  - `ArchitectureTest` (2): entità su `BaseTenantEntity`, nessun setter di tenant.
- **`services/core`** — `mvn test`: 30/30 ✅ (incl. `SeedDataTest` aggiornato per `fatture`).

## Stato criteri di accettazione

- [x] `services/fatture` registrato nel reactor; `mvn test` verde; `commons` additivo (nessuna regressione).
- [x] SPI quota/GDPR/`@PersonalData` in `commons`, implementate in `fatture`.
- [x] Schema `app_fatture` via Flyway con `Invoice`/`InvoiceLine` tenant-scoped, audit, soft-delete, UUID v7.
- [x] `POST` oltre il tetto mensile → 429 problem+json; conteggio sulla finestra mensile.
- [x] `exportData` copre Invoice + righe; `purgeData` senza orfani; solo dati del tenant.
- [x] API problem+json + spec OpenAPI committato; log con `tenant_id`/`app_id`/`user_id`.
- [x] App `fatture` + tier free (cap 10) nel seed.
