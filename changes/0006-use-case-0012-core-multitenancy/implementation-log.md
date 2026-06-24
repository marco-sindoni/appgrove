# Implementation Log — Change 0006: Core service + multitenancy

**Branch**: `change/0006-use-case-0012-core-multitenancy`
**Aree**: services (`services/` parent + `commons` + `core`), docs (`_INDEX` + note differite)
**Completata**: 2026-06-24

## File modificati (principali)

| File | Azione |
|---|---|
| `services/pom.xml` | Creato (parent: Java 21, Quarkus BOM 3.20.6, moduli commons+core) |
| `services/.gitignore` | Creato (`target/`) |
| `services/commons/pom.xml` + `src/main/java/app/appgrove/commons/**` | Creato (TenantResolver, BaseEntity/BaseTenantEntity, problem+json mappers, Page/PageRequest, MdcRequestFilter) |
| `services/commons/src/test/java/.../JwtTenantResolverTest.java` | Creato (unit: legge-da-JWT + fail-closed) |
| `services/core/pom.xml`, `src/main/resources/application.properties`, `db/migration/V1__create_platform_schema.sql` | Creato (app Quarkus, datasource/multitenant/flyway, schema platform) |
| `services/core/src/main/java/app/appgrove/core/example/**` | Creato (harness `Widget` + repository + resource + DTO) |
| `services/core/src/test/**` (application.properties, `V2__example_widget.sql`, chiavi PEM, TestTokens, MultiTenancyTest, FailClosedTest, ArchitectureTest) | Creato |
| `docs/usecases/_INDEX.md` | 0012 → 🟡 → ✅ |
| `docs/usecases/04-platform-core/0012,0013` · `03-local-dev/0009` | Note "Punti aperti / decisioni differite" |

## Cosa è stato fatto

Creato il primo backend del monorepo: progetto Maven multi-module `services/` (parent + `commons` + `core`), **Java 21**,
**Quarkus 3.20.6 (LTS)**. Il `commons` rende esecutivi gli invarianti multi-tenancy per tutti i servizi: `TenantResolver`
che legge `tenant_id` **solo dal JWT** (fail-closed), **Hibernate multitenancy DISCRIMINATOR** (filtro row-level
automatico via `@TenantId`), base entity (**UUID v7** + audit + soft-delete `@SQLRestriction`), exception mapper
**problem+json (RFC 9457)**, paginazione offset, **MDC logging** (request_id/tenant_id/user_id). Il `core` è l'app Quarkus
con datasource Postgres, **Flyway** sullo schema `platform` (migrate-at-start off in prod), profili dev/test/prod.

## Decisioni prese

- **Quarkus 3.20.6 LTS** + **Java 21 LTS** (Temurin/openjdk@21) come convenzione backend.
- **Verifica JWT via smallrye-jwt** (public key + issuer placeholder) per essere testabili senza issuer attivo; swap a
  Quarkus OIDC quando esiste (UC 0010 dev / 0015-0016 prod) — tracciato.
- **Opzione A** (da requisiti): schema `platform` di produzione vuoto (solo `CREATE SCHEMA`); l'entity/endpoint **harness**
  (`example.Widget`) ha la tabella creata **solo da una migration di test** (Quarkus non mappa entity di solo-test, quindi
  la classe vive in `main` ma in prod non esiste la tabella). Rimovibile con UC 0013 — tracciato.

## Invarianti appgrove

- **Tenant ID solo dal JWT**: `JwtTenantResolver` legge il claim `tenant_id`; test anti-override prova che il body è
  ignorato. **Cuore del change.**
- **Filtro row-level**: automatico via discriminator `@TenantId`; test cross-tenant (leak detector) verde; ArchUnit vieta
  setter del tenant e impone la base entity.
- **Logging strutturato**: `MdcRequestFilter` popola MDC (request_id/tenant_id/user_id).
- **Modulo `microsaas_app`**: non toccato (infra differita a UC 0004).

## Note per il revisore

- **Decisioni differite tracciate** (regola CLAUDE.md): in
  [0012](../../docs/usecases/04-platform-core/0012-servizio-core-multitenancy.md) (verifica JWT smallrye→OIDC, native in
  CI, harness Widget da rimuovere, audit-attore, datasource prod), in
  [0013](../../docs/usecases/04-platform-core/0013-account-utenti-inviti-api.md) (rimuovere l'harness con le entità reali) e
  in [0009](../../docs/usecases/03-local-dev/0009-script-dev.md) (`DOCKER_HOST` per Testcontainers con colima).
- **Prerequisito test**: Docker attivo. Con **colima** servono `DOCKER_HOST=unix://$HOME/.colima/default/docker.sock` +
  `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE` (vedi nota in UC 0009).
- Le chiavi PEM in `core/src/test/resources` sono **chiavi di test** (firma JWT locale), non segreti reali.

## Test

Area **services** — `mvn test` (Java 21): **BUILD SUCCESS, 9 test, 0 failures**.
- `commons` (3): `JwtTenantResolver` legge il tenant dal JWT, fail-closed se assente, default = sentinella.
- `core` (6): **MultiTenancyTest** (isolamento cross-tenant via GET = leak detector; anti-override del tenant dal body),
  **FailClosedTest** (401 senza token, 403 senza claim `tenant_id`), **ArchitectureTest** (ArchUnit: entity ⊂ BaseEntity,
  nessun setter del tenant). Integrazione con **Flyway reale** (V1+V2) su **Postgres 17 via Testcontainers**.

## Stato criteri di accettazione

- [x] `services/` multi-module compila (JVM, Java 21).
- [x] `commons`: TenantResolver (JWT), discriminator, base entity (UUID v7 + audit + soft-delete), problem+json, paginazione, MDC.
- [x] Flyway crea `platform` (migrate-at-start off in prod); nei test gira su Testcontainers PG17 con migrazioni reali.
- [x] Suite security/multi-tenancy verde (isolamento ≥2 tenant, fail-closed, anti-override, soft-delete).
- [x] ArchUnit verde.
- [x] `mvn test` verde con Java 21.
