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
- Il **core** possiede: `accounts` (+ `paddle_customer_id`, #09 B), `users` (**membership foldata su users**, niente tabella `memberships`), `invitations`, **catalogo** (`app`/`app_tier`/`app_price`, modello autorevole in [09-pagamenti](09-pagamenti.md) B), e **`subscription`** (tenant↔app). **L'entitlement è DERIVATO** da `subscription` (niente tabella `entitlements`, #09 dec.12).
- La **Pre-Token-Gen Lambda** legge `platform` in **DB diretto**.

## Topic dell'area (agenda)
- **A. Data model del core (`platform`)** — tabelle, chiavi, relazioni (accounts/users/invitations/catalogo `app`+`app_tier`+`app_price`/`subscription`; entitlement derivato).
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
   - **accounts**: `id` (= `tenant_id`), `name`, `status` (active/suspended), **`paddle_customer_id`** (un customer Paddle per account, #09 B). Il tenant.
   - **users**: `id`, `cognito_sub` (unique), `email`, `display_name`, `tenant_id`→accounts, `role`
     (owner/admin/member), `status`. **La membership è foldata su `users`** (1 utente→1 tenant): nessuna tabella memberships.
   - **invitations**: `id`, `tenant_id`→accounts, `email`, `role`, `token_hash` (single-use), `status`
     (pending/accepted/expired/revoked), `expires_at`, `invited_by`, `accepted_user_id`.
   - **Catalogo** (modello autorevole in [09-pagamenti](09-pagamenti.md) B, dec.10): **`app`** (`app_id` PK varchar kebab,
     `name`, `description`, `user_model` single/multi, `status`, **`paddle_product_id`**) + **`app_tier`** (tier, `limits`
     JSON con metrica/finestra/tetto, `features`, `trial_days`) + **`app_price`** (`billing_cycle` monthly/annual,
     **`paddle_price_id`**, importo, valuta). _Supera la singola tabella `apps` originaria._
   - **subscription** (tenant-scoped): `tenant_id`→accounts, `app_id`, **`paddle_subscription_id`**, `paddle_price_id`
     corrente (→ risolve il tier), **`status`** (`trialing/active/past_due/paused/canceled`), `current_period_start/end`,
     `cancel_at`, `trial_end`. Una per (tenant, app). _**L'entitlement NON è una tabella**: è **derivato** da `subscription`
     (status nell'access-set) + `app_price`/`app_tier`, fonte unica `subscription` — #09 dec.12. Sostituisce la vecchia
     tabella materializzata `entitlements`._

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
