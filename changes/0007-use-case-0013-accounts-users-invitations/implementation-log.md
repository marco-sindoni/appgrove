# Implementation Log — Change 0007: Accounts/Users/Invitations + API REST core (UC 0013)

**Branch**: `change/0007-use-case-0013-accounts-users-invitations`
**Aree**: services (`services/core`, `services/commons`) + docs (`_INDEX`, tracciamenti differiti)
**Completata**: 2026-06-26

## File modificati

| File | Azione |
|---|---|
| services/core/src/main/resources/db/migration/V2__core_domain.sql | Creato |
| services/core/src/main/java/app/appgrove/core/platform/Account.java, User.java, Invitation.java | Creato |
| services/core/src/main/java/app/appgrove/core/platform/{AccountStatus,UserRole,UserStatus,InvitationStatus}.java | Creato |
| services/core/src/main/java/app/appgrove/core/platform/{AccountRepository,UserRepository,InvitationRepository}.java | Creato |
| services/core/src/main/java/app/appgrove/core/platform/{AccountResource,UserResource,InvitationResource}.java | Creato |
| services/core/src/main/java/app/appgrove/core/platform/{AccountDtos,UserDtos,InvitationDtos}.java | Creato |
| services/core/src/main/java/app/appgrove/core/platform/{Roles,CallerContext,InvitationTokens}.java | Creato |
| services/commons/src/main/java/app/appgrove/commons/persistence/AuditListener.java | Creato |
| services/commons/src/main/java/app/appgrove/commons/persistence/BaseEntity.java | Modificato (@EntityListeners) |
| services/core/pom.xml | Modificato (quarkus-smallrye-openapi) |
| services/core/src/main/resources/application.properties | Modificato (config OpenAPI) |
| services/core/src/main/resources/META-INF/openapi/openapi.{yaml,json} | Creato (spec committato) |
| services/core/src/main/java/app/appgrove/core/example/Widget*.java | Eliminato (harness UC 0012) |
| services/core/src/test/resources/db/migration/V2__example_widget.sql | Eliminato |
| services/core/src/test/java/app/appgrove/core/{TestData,RolesTest,AccountUserApiTest,InvitationLifecycleTest,OpenApiContractTest}.java | Creato |
| services/core/src/test/java/app/appgrove/core/{MultiTenancyTest,FailClosedTest,TestTokens}.java | Modificato (entità reali) |
| services/core/src/test/resources/application.properties | Modificato (commento V2) |
| docs/usecases/_INDEX.md | Modificato (0013 🟡→✅) |
| docs/usecases/{0011,0022,0025,0030}-*.md | Modificato (decisioni differite) |

## Cosa è stato fatto

Implementato il modello dati del core (`accounts`/`users`/`invitations` con entità JPA + repository; catalogo e
`subscription` come **solo DDL** Flyway `V2`) e le API REST `/api/platform/v1/*` (CRUD account/utenti/inviti) in
problem+json con matrice ruoli `@RolesAllowed`, paginazione e **OpenAPI committato** (Swagger UI libera in dev).
Aggiunto l'**audit-attore** (`created_by`/`updated_by` dal `sub` JWT, via `AuditListener` in commons) e rimosso
l'harness `example.Widget`, spostando le suite multi-tenancy/fail-closed sulle entità reali.

## Decisioni prese

- **Account non tenant-scoped via discriminator**: `accounts.id` **è** il `tenant_id`; l'accesso usa `id = tenant_id`
  (equivalente row-level dell'invariante #2). `users`/`invitations` restano tenant-scoped col discriminator automatico.
- **Fixture di test via JDBC diretto** (`TestData`, Agroal): fuori da una richiesta il `TenantResolver` è fail-closed,
  quindi i dati di base si inseriscono con `tenant_id` esplicito; le letture restano filtrate dal discriminator via REST.
- **Tenant id = UUID nei test**: poiché `accounts.id` è UUID e coincide col `tenant_id`, i token di test usano UUID.
- **Inviti**: solo create/list/revoke; l'accettazione (→ creazione utente) è UC 0017. Ruoli invitabili = admin/member.

## Invarianti appgrove

- **Tenant ID solo dal JWT**: `User`/`Invitation` ricevono `tenant_id` dal discriminator (JWT); `Account` per `id =
  tenant_id` via `CallerContext` (fail-closed). I DTO non accettano `tenant_id` (anti-override, test dedicato).
- **Filtro row-level**: automatico (discriminator) su `users`/`invitations`; su `accounts` è `id = :tid`. Test
  cross-tenant (`MultiTenancyTest`, `AccountUserApiTest`) verificano l'assenza di leak.
- **Logging strutturato**: invariato — `MdcRequestFilter` (commons) popola `tenant_id`/`user_id`/`request_id`.
- **Modulo `microsaas_app`**: non toccato (nessuna infra).

## Note per il revisore

- **Contratto cross-area**: nuove API `/api/platform/v1/*`; il contratto è `openapi.yaml` committato. Rimosso l'endpoint
  harness `/api/_demo/widgets` (non pubblico). Lo spec è rigenerato a ogni build (`store-schema-directory`): il drift è
  un git diff; l'**oasdiff bloccante** sui breaking change è in CI (UC 0005).
- **Dati personali = Sì**: `users.email/display_name/cognito_sub`, `invitations.email`. Classificazione in
  requirements §7; manifesto/RoPA + annotazione `@PersonalData` tracciati a **UC 0030** (gate CI bloccante UC 0031).
  Bump PP/ToS N/A ora (testi legali non ancora redatti, UC 0002). Nessun nuovo sub-processor.
- **Decisioni differite tracciate**:
  - **UC 0011** — fix ordinamento indice: il seed (0011) deve seguire 0013.
  - **UC 0022** — entità JPA del catalogo (`app`/`app_tier`/`app_price`); qui solo DDL.
  - **UC 0025** — entità JPA `subscription`; qui solo DDL (lettura entitlement: UC 0027).
  - **UC 0030** — entry manifesto piattaforma + `@PersonalData` per i PII di UC 0013.
  - **UC 0014** — gating Swagger UI in test/prod (authorizer); in locale libera.
  - **UC 0017** — flusso di accettazione invito → creazione utente.
- **Esecuzione test**: richiede Docker (Testcontainers). In locale con colima esportare
  `DOCKER_HOST=unix://$HOME/.colima/default/docker.sock` (+ `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`).

## Test

`services/core` (`mvn test`, Testcontainers Postgres 17 + Flyway reale): **23 test verdi**.
- `MultiTenancyTest` (3): isolamento per tenant + anti-override `tenant_id` (su inviti reali).
- `FailClosedTest` (2): no token → 401; ruolo senza `tenant_id` → 403.
- `RolesTest` (5): member non gestisce inviti/utenti (403); owner/admin sì; member legge il proprio profilo.
- `AccountUserApiTest` (6): account corrente, isolamento utenti, profilo proprio, patch ruolo, soft-delete, email unica globale.
- `InvitationLifecycleTest` (4): create→list→revoke, duplicato pending (409), ruolo non invitabile (400), token grezzo solo in creazione.
- `OpenApiContractTest` (1): lo spec servito espone le API platform.
- `ArchitectureTest` (2): entità ⇒ BaseEntity; nessun setter del tenant.

`services/commons` (`mvn test`): **3 test verdi** (`JwtTenantResolverTest`). Build complessivo: **BUILD SUCCESS**.

## Stato criteri di accettazione

- [x] `V2__core_domain.sql` crea accounts/users/invitations + struttura catalogo + subscription; vincoli/indici presenti; package verde.
- [x] Entità/repository per accounts/users/invitations con audit-attore e soft-delete; nessuna entità JPA per catalogo/subscription.
- [x] API `/api/platform/v1/*` in problem+json con matrice ruoli; OpenAPI committato + Swagger UI in dev.
- [x] Harness `example.Widget` rimosso; suite multi-tenancy/fail-closed/architettura riscritte sulle entità reali e verdi.
- [x] Punti differiti tracciati nei file UC corretti (0011/0022/0025/0030/0014/0017).
