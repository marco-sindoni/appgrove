# Change 0007: Accounts/Users/Invitations + API REST core (UC 0013)

**Branch**: `change/0007-use-case-0013-accounts-users-invitations`
**Aree**: services (`services/core` — entità di dominio, API REST, migrazioni Flyway; tocca `services/commons` solo se serve estrarre helper riusabili)
**Data**: 2026-06-26
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/04-platform-core/0013-account-utenti-inviti-api.md](../../docs/usecases/04-platform-core/0013-account-utenti-inviti-api.md)
**Tocca dati personali?**: **Sì** — `users.email`, `users.display_name`, `users.cognito_sub` (e `invitations.email`). Finalità: erogazione/gestione account; base giuridica **contratto** (GDPR 6.1.b); retention "finché attivo" + grace 14gg (#13 E25). Si applica il checkpoint privacy/RoPA di step-03 (classifica → manifesto/RoPA → bump PP/ToS); il gate CI bloccante è UC 0031, l'automazione manifest/RoPA è UC 0030 (non ancora presenti → tracciati, vedi sotto).

## Problema / Obiettivo

UC 0012 ha consegnato lo scheletro del core e lo schema `platform` **vuoto**. Questa change implementa il **modello dati e le API del core**: account (tenant), utenti (membership foldata), inviti, più la **struttura** (solo DDL) di catalogo e `subscription`. È prerequisito diretto del **seed deterministico (UC 0011)**, che popolerà queste tabelle, e della shell/flussi auth (UC 0020/0017/0010) che consumano queste API.

## Scope

### 1. Schema dati — migrazione Flyway di produzione `V2` (schema `platform`)

Nuova migration `services/core/src/main/resources/db/migration/V2__core_domain.sql`:

- **`accounts`** (radice tenant; **non** tenant-scoped via discriminator — `accounts.id` **è** il `tenant_id`): `id` (uuid PK, = tenant_id), `name`, `status` (`active`/`suspended`), `paddle_customer_id` (nullable, lazy #09 C15) + colonne audit/soft-delete della `BaseEntity`.
- **`users`** (tenant-scoped, **membership foldata** 1 utente→1 tenant, nessuna tabella memberships): `id` (uuid PK), `tenant_id` (discriminator), `cognito_sub` (**unique**), `email` (**unique** globale, lower/citext), `display_name`, `role` (`owner`/`admin`/`member`), `status` (`active`/`suspended`) + audit/soft-delete.
- **`invitations`** (tenant-scoped): `id`, `tenant_id`, `email`, `role`, `token_hash` (single-use), `status` (`pending`/`accepted`/`revoked`/`expired`), `expires_at`, `invited_by` (uuid), `accepted_user_id` (uuid nullable) + audit/soft-delete.
- **Catalogo (solo DDL, platform-level, no entità JPA)**: `app` (`id`, `slug` unique, `name`, `user_model` `single_user`/`multi_user`, `status`, `paddle_product_id` nullable), `app_tier` (`id`, `app_id` FK, `key`, `name`, `limits` jsonb, `features` jsonb, `trial_days`), `app_price` (`id`, `app_tier_id` FK, `billing_cycle` `monthly`/`annual`, `paddle_price_id` nullable, `amount` minor units, `currency`).
- **`subscription` (solo DDL, tenant-scoped via colonna `tenant_id` + indice)**: `id`, `tenant_id`, `app_id` FK, `app_tier_id` FK nullable, `status` (`trialing`/`active`/`past_due`/`canceled`/`paused`), `current_period_start`/`current_period_end`, `cancel_at` nullable, `trial_end` nullable, `paddle_subscription_id` nullable + audit/soft-delete.

I tipi/colonne esatti di catalogo e `subscription` sono **strutturali**: potranno essere affinati dagli UC proprietari (0022/0025) al primo uso reale. Indici su tutte le FK e su ogni colonna `tenant_id`.

### 2. Entità JPA + repository — solo `accounts` / `users` / `invitations`

- `Account` estende `BaseEntity` (la radice tenant: accesso filtrato per `id = tenant_id` dal JWT, equivalente row-level dell'invariante #2); `User` e `Invitation` estendono `BaseTenantEntity` (discriminator automatico) con `@SQLRestriction("deleted_at is null")`.
- Repository Panache; **nessun filtro tenant manuale** su `User`/`Invitation` (lo aggiunge il discriminator); su `Account` l'accesso è per `id = tenant_id`.
- **Audit-attore**: valorizzare `created_by`/`updated_by` dal `sub` del JWT (es. listener JPA CDI-aware) — risolve la decisione differita di UC 0012.

### 3. API REST `/api/platform/v1/*` (problem+json, Bean Validation, paginazione)

- **accounts**: `GET /accounts/me` (account del tenant corrente); `PATCH /accounts/me` (`name`) — owner.
- **users**: `GET /users` (lista membri del tenant, paginata) owner/admin; `GET /users/me` (proprio profilo) tutti; `GET /users/{id}` owner/admin; `PATCH /users/{id}` (`role`/`status`) owner/admin; `DELETE /users/{id}` (soft-delete) owner/admin.
- **invitations**: `POST /invitations` (`email`,`role`) owner/admin; `GET /invitations` (pending, paginata) owner/admin; `DELETE /invitations/{id}` (revoca) owner/admin.
- **Ruoli** via `@RolesAllowed` letti dai gruppi del JWT (`owner`/`admin`/`member` tenant-level; `platform-admin` platform-level). Anti-override `tenant_id`: mai da body/param.
- Riuso del layer `commons` esistente (`ProblemDetail`, `Page`/`PageRequest`, mapper, `MdcRequestFilter`).

### 4. OpenAPI + Swagger

- Aggiungere `quarkus-smallrye-openapi`; **OpenAPI generato e committato** nel repo (`services/core/src/main/resources/META-INF/openapi.yaml`); annotazioni minime sulle resource.
- **Swagger UI** sempre generata; in **dev locale libera** (il gating platform-admin via authorizer è UC 0014, ☁ → tracciato).

### 5. Rimozione harness UC 0012 + riallineamento test

- Eliminare `example.Widget`/`WidgetRepository`/`WidgetResource`/`WidgetDtos` e la migration **di test** `src/test/.../V2__example_widget.sql`.
- Riscrivere `MultiTenancyTest`/`FailClosedTest`/`ArchitectureTest` sulle **entità reali** (`User`/`Invitation`).

## Fuori scope

- **Entità JPA + endpoint di catalogo/`subscription`** (solo DDL qui) → UC 0022 (catalogo) / UC 0025 (subscription) / UC 0027 (lettura entitlement). **Tracciato** nei rispettivi file.
- **Popolamento** di catalogo/subscription (pricing-sync UC 0022, webhook UC 0025) e **entitlement** (derivato, no tabella → UC 0014/0027).
- **Flusso di accettazione invito → creazione utente** (legato a signup) → UC 0017/0015/0010. Qui solo create/list/revoke.
- **UI** (shell/flussi/console) → UC 0020/0017/0021. **Seed** dei dati → UC 0011 (prossima change).
- **Auth reale** (Cognito/Pre-Token-Gen/authorizer, JWKS reale) → UC 0015/0016/0014 (☁); in locale emulata da UC 0010, nei test firmata dall'harness `TestTokens`.
- **`oasdiff` bloccante** sui breaking change OpenAPI → CI UC 0005 (☁). **Gating** Swagger in test/prod → authorizer UC 0014 (☁). **Annotazione `@PersonalData` + manifest/RoPA** → UC 0030. Tutti **tracciati** sotto.

## Criteri di accettazione

- [ ] `V2__core_domain.sql` crea `accounts`/`users`/`invitations` + struttura catalogo + `subscription` nello schema `platform`; vincoli (unique `email`/`cognito_sub`, FK, `1 utente→1 tenant`) e indici su `tenant_id`/FK presenti; `mvn -q -DskipTests package` verde.
- [ ] Entità/repository per `accounts`/`users`/`invitations` con audit-attore (`created_by`/`updated_by` dal `sub` JWT) e soft-delete; nessuna entità JPA per catalogo/subscription.
- [ ] API `/api/platform/v1/*` (CRUD account/utenti/inviti) in problem+json con matrice ruoli applicata; OpenAPI committato + Swagger UI disponibile in dev.
- [ ] Harness `example.Widget` rimosso; suite multi-tenancy/fail-closed/architettura riscritte sulle entità reali e **verdi** (`mvn test`).
- [ ] Punti differiti (oasdiff/Swagger-gating/@PersonalData/manifest/entità catalogo-subscription/accept-invito) tracciati nei file UC corretti.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT**: `User`/`Invitation` ricevono `tenant_id` dal discriminator (JWT); `Account` è acceduto per `id = tenant_id`. I DTO non espongono mai `tenant_id` scrivibile (anti-override).
- **Filtro row-level**: automatico via discriminator su `users`/`invitations`/`subscription`; su `accounts` l'equivalente è `id = :tid`. Test cross-tenant lo verificano.
- **Logging strutturato**: invariato — `MdcRequestFilter` (commons) già popola `tenant_id`/`user_id`/`request_id`; le nuove resource ereditano.
- **Modulo `microsaas_app`**: N/A (nessuna infra in questa change).

## Requisiti di test

- **Integration (Testcontainers + Flyway reale)**: CRUD account/utenti/inviti; vincolo `1 utente→1 tenant` (email duplicata → rifiuto problem+json); invito single-use/scadenza.
- **Security/multi-tenancy**: tenant A non legge utenti/inviti di B (leak detector via GET); anti-override `tenant_id` da body; matrice ruoli (member non può gestire utenti/inviti → 403; owner/admin sì; no token → 401; token senza `tenant_id` → 403 fail-closed).
- **Contract/OpenAPI**: test di **drift** che asserisce che lo spec servito (`/q/openapi`) coincide con `openapi.yaml` committato (oasdiff bloccante resta CI/UC 0005).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (rimuove solo l'endpoint harness `/api/_demo/widgets`, non pubblico) |
| Contratto cross-area | Sì — nuove API `/api/platform/v1/*` consumate da frontend/auth locali (UC 0020/0017/0010), contratto = OpenAPI committato |
| Version bump | minor (nuove tabelle + API; nessuna rottura di contratti esistenti) |
