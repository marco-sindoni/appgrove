# Change 0006: Core service + multitenancy (`services/` Quarkus multi-module)

**Branch**: `change/0006-use-case-0012-core-multitenancy`
**Aree**: services (`services/` parent + `services/commons` + `services/core`), docs (`_INDEX`)
**Data**: 2026-06-24
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/04-platform-core/0012-servizio-core-multitenancy.md](../../docs/usecases/04-platform-core/0012-servizio-core-multitenancy.md)
**Tocca dati personali?**: No — qui nasce solo l'infrastruttura (multitenancy, audit, soft-delete, schema `platform` vuoto). Le entità con PII (users/email) sono UC 0013; manifest/RoPA → UC 0013/0030.

## Problema / Obiettivo

Realizzare lo **scheletro del core/platform service** Quarkus e il **`commons`** condiviso che rende **esecutive** le
invarianti di multi-tenancy per **tutti** i servizi appgrove. È il **primo servizio backend** del monorepo: fissa le
convenzioni Maven/Quarkus/test che ogni app (anche via `new-application`) erediterà.

## Scope

- **Maven multi-module**: `services/pom.xml` (parent) + `services/commons/` + `services/core/`. `groupId` **`app.appgrove`**,
  package `app.appgrove.commons` / `app.appgrove.core`. **Java 21 LTS**, **Quarkus 3.x** stabile più recente.
- **`commons`** (libreria condivisa):
  - `TenantResolver` request-scoped che legge `tenant_id` **solo dal JWT verificato** (`@TenantId` resolver per Hibernate),
    **fail-closed** se assente;
  - **Hibernate multitenancy `DISCRIMINATOR`** → filtro row-level automatico su ogni query tenant-scoped;
  - **base entity**: `BaseEntity` (PK **UUID v7** via `@UuidGenerator(style=TIME)`, `created_at/updated_at/created_by/updated_by`,
    `deleted_at` soft-delete con filtro automatico) e `BaseTenantEntity` (col discriminator `tenant_id`);
  - **exception mapper centralizzati → problem+json (RFC 9457)**; **Bean Validation** al boundary; **paginazione offset**;
  - **MDC logging** strutturato (correlation id da `X-Request-Id`/`traceparent`; campi `tenant_id`/`user_id`).
- **`core`** (app Quarkus skeleton): wiring Quarkus REST + Hibernate ORM (Panache, repository pattern) + Agroal datasource;
  **Quarkus OIDC** configurato per profilo (issuer/JWKS/audience via config, valorizzati per `%dev`/`%test`/`%prod`);
  **Flyway** per schema **`platform`**, **migrate-at-start off** in prod (gira in CI one-shot, UC 0005); profili
  `%dev` (JVM) / `%test` / `%prod`. Migration di **produzione** `V1`: solo `CREATE SCHEMA platform` (struttura base).
- **Test** (opzione A — schema di produzione pulito): entity concreta + migration Flyway **solo in `core/src/test`**
  (profilo `%test`) per esercitare il pattern; harness identità che **firma JWT locali con smallrye-jwt** (claim
  `tenant_id`/`roles`) → nessuna dipendenza da Cognito/UC 0016.

## Fuori scope

- **Entità/API di dominio** (accounts/users/invitations/catalogo/subscription) → UC 0013.
- **Auth/JWT issuance** (Cognito/BFF/Pre-Token-Gen) → UC 0015/0016; **authorizer** → UC 0014.
- **Build native GraalVM**: configurata per `%test`/`%prod` ma **non eseguita ora** (richiede GraalVM/CI → UC 0005, ☁).
- **Infra AWS** (modulo `microsaas_app`, ruolo DB per-servizio, RDS Proxy) → UC 0004 (☁). In locale: processo host su
  Postgres locale; nei test: Testcontainers.
- **Dev-mode end-to-end** contro un issuer reale: richiede l'auth locale (UC 0010); per ora il core è validato via test
  (Testcontainers + JWT firmati localmente).

## Criteri di accettazione

- [ ] `services/` è un progetto Maven multi-module (`commons` + `core`) che **compila** (`mvn -q -DskipTests package`, JVM).
- [ ] `commons` fornisce `TenantResolver` (dal JWT), discriminator multitenancy, `BaseEntity`/`BaseTenantEntity`
      (UUID v7 + audit + soft-delete), exception mapper problem+json, paginazione, MDC.
- [ ] Flyway crea lo schema `platform` (migrate-at-start **off** in prod); nei test gira su **Testcontainers Postgres 17**
      con migrazioni reali.
- [ ] **Suite security/multi-tenancy verde**: matrice cross-tenant (≥2 tenant) isola le righe; **fail-closed** senza
      `tenant_id`; **anti-override** (`tenant_id` da body/param ignorato); soft-delete filtra `deleted_at`.
- [ ] **ArchUnit** verde: nessun bypass del filtro tenant, nessuna lettura di `tenant_id` dalla request.
- [ ] `mvn test` (area services/core) verde con Java 21.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT verificato**: `TenantResolver` lo legge esclusivamente dai claim del JWT; mai da body/param
  (test anti-override lo prova). **Cuore di questo change.**
- **Filtro row-level**: garantito automaticamente dal discriminator Hibernate per ogni entity tenant-scoped (niente `WHERE`
  manuale); ArchUnit ne impedisce il bypass.
- **Logging strutturato**: MDC con `tenant_id`/`user_id`/correlation id nel `commons` (campo `app_id` valorizzato dai servizi).
- **Modulo `microsaas_app`**: non toccato qui (infra differita a UC 0004).

## Requisiti di test

- **Integration** `@QuarkusTest` + **Testcontainers Postgres 17** + **Flyway reale** (schemi e migrazioni veri).
- **Identità**: harness che firma **JWT reali con smallrye-jwt** (claim `tenant_id`/`roles`) + `@TestSecurity`; copre
  fail-closed e matrice ruoli senza Cognito.
- **Security/multi-tenancy** (sempre): matrice ≥2 tenant (nessun leak cross-tenant), fail-closed, anti-override `tenant_id`,
  verifica soft-delete.
- **ArchUnit**: guardie statiche degli invarianti (no bypass filtro, no `tenant_id` dalla request). Harness di isolamento
  riusabile dalle app.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (primo servizio backend) |
| Contratto cross-area | N/A ora (le API di dominio sono UC 0013; consumo infra → UC 0004 ☁) |
| Version bump | minor (nuovo modulo `services/` 0.x) |
