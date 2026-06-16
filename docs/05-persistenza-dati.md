# Persistenza & dati — Decisioni

**Stato**: 🟢 deciso (retention backup, ruoli DB e RDS Proxy → #06)
**Ultimo aggiornamento**: 2026-06-14

## Scope
Modello dati e persistenza: data model del core (`platform`), modello per-app, strategia schema-per-app,
discriminator multitenancy, migrations, connection management su Aurora, ID/keys, audit e data lifecycle.
Non copre l'authz/verifica JWT (→ [02-auth-sicurezza](02-auth-sicurezza.md)) né il provisioning RDS/networking
(→ [06-infra-iac](06-infra-iac.md)).

## Vincoli ereditati (#01/#02, già decisi)
- **Aurora Serverless v2 PostgreSQL**, istanza **condivisa**; **schema-per-app** (`app_<app_id>`), core su schema `platform`.
- **Hibernate multitenancy `DISCRIMINATOR`** + `TenantResolver` da JWT; filtro `tenant_id` automatico, **fail-closed**.
- `tenant_id` = account id; `user_id` = `sub` Cognito. **1 utente → 1 tenant**.
- Il **core** possiede: `accounts`, `memberships` (utente↔tenant + ruolo), `invitations`, `catalog` (app + capability single/multi-user), `entitlements` (tenant↔app).
- La **Pre-Token-Gen Lambda** legge `platform` in **DB diretto**.

## Topic dell'area (agenda)
- **A. Data model del core (`platform`)** — tabelle, chiavi, relazioni (accounts/memberships/invitations/catalog/entitlements).
- **B. Modello dati per-app & discriminator** — colonna `tenant_id` su ogni tabella tenant-scoped; implicazioni single vs multi-user.
- **C. Strategia schema & isolamento** — schema-per-app sull'istanza condivisa; divieto cross-schema; path verso DB dedicato.
- **D. Migrations** — Flyway vs Liquibase; per-schema; chi le esegue (startup servizio / CI/CD); versioning.
- **E. Connection management** — pooling (RDS Proxy vs Agroal/Quarkus), connessioni da Fargate + Lambda, scaling Aurora v2.
- **F. ID & keys** — UUID (v4/v7) vs bigint; generazione `account_id`; relazione con le PK.
- **G. Audit & data lifecycle** — colonne audit, soft vs hard delete, offboarding tenant (GDPR), backup/PITR.

## Decisioni prese

### Migrations (topic D)
1. **Flyway** (SQL-first, Quarkus-native), **migrate-at-start disabilitato**. Le migration girano come
   **task one-shot in CI/CD** prima del deploy (→ orchestrazione in [07-devops-cicd](07-devops-cicd.md)).
2. **Ogni servizio possiede le migration del proprio schema**: il core per `platform`, ogni app per `app_<app_id>`.

### Connection management (topic E)
3. **Fargate → Agroal diretto** (pool Quarkus, connessioni long-lived). **Lambda (auth + pre-token-gen) → RDS Proxy**
   per evitare connection storm. Provisioning RDS Proxy → [06-infra-iac](06-infra-iac.md).

### ID & keys (topic F)
4. **PK = UUID v7** (ordinato nel tempo: unicità/non-enumerabilità + buona località d'indice). Generazione
   app-side (Hibernate `@UuidGenerator(style=TIME)`). `account_id` (UUID v7) generato dal core al signup.
   _Supera il "UUID v4" citato nel recap._

### Audit & lifecycle (topic G)
5. **Colonne audit ovunque**: `created_at`, `updated_at`, `created_by`, `updated_by`.
6. **Soft-delete** (`deleted_at` nullable) per le entità di business, filtro auto-applicato accanto al
   discriminator tenant. **Hard-delete** riservato a erasure GDPR / offboarding tenant.

### Data model del core — schema `platform` (topic A)
7. Tabelle (tutte con audit + `deleted_at`; PK UUID v7 salvo `apps`):
   - **accounts**: `id` (= `tenant_id`), `name`, `status` (active/suspended). Il tenant.
   - **users**: `id`, `cognito_sub` (unique), `email`, `display_name`, `tenant_id`→accounts, `role`
     (owner/admin/member), `status`. **La membership è foldata su `users`** (1 utente→1 tenant): nessuna tabella memberships.
   - **invitations**: `id`, `tenant_id`→accounts, `email`, `role`, `token_hash` (single-use), `status`
     (pending/accepted/expired/revoked), `expires_at`, `invited_by`, `accepted_user_id`.
   - **apps** (catalog): `app_id` (PK varchar kebab), `name`, `description`, `user_model` (single/multi),
     `status`, `paddle_product_id`.
   - **entitlements**: `id`, `tenant_id`→accounts, `app_id`→apps, `status` (active/trial/suspended/cancelled),
     `activated_at`, `expires_at`, `paddle_subscription_id`, UNIQUE(`tenant_id`,`app_id`).

### Modello dati per-app & isolamento (topic B, C)
8. Ogni tabella tenant-scoped in `app_<app_id>` ha la colonna **`tenant_id`** (UUID = account id) = **discriminator** Hibernate.
9. **`tenant_id` è un riferimento logico** a `platform.accounts`, **mai** una FK cross-schema. Niente query/FK
   tra schemi → schemi indipendenti, passaggio a DB dedicato = modifica Terraform, non refactor.
10. App **multi-user**: tabelle possono portare `created_by`/owner per authz *intra-tenant*; il confine
    d'isolamento resta sempre `tenant_id`. App **single-user**: identico, un solo utente per tenant.
11. **Isolamento a livello DB**: **un ruolo Postgres per servizio**, privilegi solo sul proprio schema
    (least privilege); il core solo su `platform`. Difesa in profondità oltre al discriminator. (ruoli → [06-infra-iac](06-infra-iac.md))

### Offboarding & backup (topic G, dettaglio infra → #06)
12. **Erasure GDPR / offboarding** = hard-delete **orchestrato dal core**: niente FK cross-schema, quindi
    ogni app espone una **purge per `tenant_id`** che il core invoca per cancellare i dati del tenant in tutti gli schemi.
13. **Backup**: Aurora automated backups + **PITR**; retention e finestre definite in [06-infra-iac](06-infra-iac.md).

## Questioni aperte
_Nessuna — #05 chiuso (retention backup e provisioning ruoli/RDS Proxy demandati a #06)._

## Alternative valutate / scartate
- **Flyway migrate-at-start** — scartato: si preferisce uno step CI/CD controllato (no migrazioni concorrenti all'avvio dei task).
- **RDS Proxy per tutto / nessun Proxy** — scartati: Proxy solo dove serve (Lambda), Agroal diretto da Fargate (equilibrio costo/robustezza).
- **UUID v4 / bigint** — scartati a favore di UUID v7.

## Impatti su altre aree
- [02-auth-sicurezza](02-auth-sicurezza.md), [04-services-backend](04-services-backend.md), [06-infra-iac](06-infra-iac.md), [12-environments-config](12-environments-config.md)
