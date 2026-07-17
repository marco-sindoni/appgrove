# Seed deterministico (UC 0011)

Dataset **unico, deterministico, idempotente e versionato** condiviso tra **dev locale** ed **E2E**.
Caricato da `./dev.sh seed` (psql, dopo `./dev.sh migrate`) e validato da
`services/core` → `SeedDataTest` (Testcontainers). Dati **100% sintetici** (email `*.test`, nessun PII).

- Idempotente: `INSERT … ON CONFLICT (id) DO UPDATE` → ri-eseguire = stesso stato.
- Deterministico: UUID e timestamp **fissi** (gli E2E asseriscono su questi ID).

## Tenant / accounts

| Account | id | Tipo |
|---|---|---|
| Acme Corp | `a0000000-0000-4000-8000-000000000001` | B2B multi-user |
| Bob Personal | `a0000000-0000-4000-8000-000000000002` | B2C single-user |
| Appgrove Platform | `a0000000-0000-4000-8000-000000000003` | home del platform-admin |

## Utenti (`cognito_sub` stabili → usati dall'auth locale UC 0010 per mintare i JWT)

| Utente | id | tenant | cognito_sub | email | role |
|---|---|---|---|---|---|
| Acme Owner | `b0000000-…-001` | Acme | `seed-acme-owner` | owner@acme.test | owner |
| Acme Admin | `b0000000-…-002` | Acme | `seed-acme-admin` | admin@acme.test | admin |
| Acme Member | `b0000000-…-003` | Acme | `seed-acme-member` | member@acme.test | member |
| Bob | `b0000000-…-004` | Bob | `seed-bob-owner` | bob@bob.test | owner |
| Platform Admin | `b0000000-…-005` | Platform | `seed-platform-admin` | admin@appgrove.test | owner * |

\* La capacità **`platform-admin`** è un **gruppo JWT**, non una colonna `users.role`: l'auth locale (UC 0010)
assegna il gruppo `platform-admin` al subject `seed-platform-admin`.

## Inviti pending (tenant Acme)

| Invito | id | email | role | token (grezzo) |
|---|---|---|---|---|
| admin | `c0000000-…-001` | invitee-admin@acme.test | admin | `seed-invite-acme-admin` |
| member | `c0000000-…-002` | invitee-member@acme.test | member | `seed-invite-acme-member` |

Su DB è salvato solo `token_hash = SHA-256(hex)` del token grezzo (single-use).

## Catalogo (pricing-as-code, UC 0022)

Il catalogo (`app`/`app_tier`/`app_price`) **non è più in `seed.sql`**: è la **definizione pricing-as-code** in
`services/core/src/main/resources/pricing/<slug>.yaml` (fonte di verità del "cosa si vende"), caricata dal **loader**
del core. `dev seed` esegue `sync-pricing` (loader, YAML → DB) **dopo** il migrate e **prima** del seed; in `@QuarkusTest`
il loader gira allo startup. Gli ID sono **deterministici** dalla chiave stabile (`CatalogIds`: UUIDv3 name-based su
`app:<slug>` / `tier:<slug>:<key>` / `price:<slug>:<key>:<cycle>`), così le FK delle subscription del seed restano stabili.

| App | id (`CatalogIds.appId`) | slug | user_model | status |
|---|---|---|---|---|
| Notes | `e8b95b18-…-9eb` | notes | single_user | active |
| Teams | `1c4ea96d-…-779` | teams | multi_user | active |
| Legacy | `52fbfc15-…-232` | legacy | multi_user | **inactive** (disabilitata dall'admin) |
| Fatture | `c46a39d9-…-4c6` | fatture | single_user | active (app #1, UC 0051) |

Tier (`app_tier`) con `limits` jsonb (flow/stock) e prezzi (`app_price`) monthly+annual EUR per i tier a pagamento
(Notes Pro, Teams). I `paddle_product_id`/`paddle_price_id` (per-ambiente) li riempie la sync (stub in locale). Vedi gli
YAML in `services/core/.../pricing/` per i valori.

## Subscription (stati di lifecycle → entitlement derivato + catena di gate)

> File separato: **`seed-subscriptions.sql`** (dipende dal catalogo via FK). Applicato **solo** dove il catalogo
> esiste — core `@QuarkusTest` (loader allo startup) e dev/E2E (dopo `sync-pricing`). I servizi di sola identità
> (es. auth) applicano **solo** `seed.sql` (accounts/users/invitations), non le subscription.

| Tenant | App | Tier | Stato | Note |
|---|---|---|---|---|
| Acme | Teams | team | `active` | multi-user attivo |
| Acme | Notes | pro | `past_due` | |
| Bob | Notes | free | `trialing` | `trial_end` futuro |
| Acme | Legacy | std | `active` | app `inactive` → esercita il gate "app abilitata" |
| Bob | Teams | team | `canceled` | `cancel_at` valorizzato |

## Runbook

```bash
./dev.sh migrate   # applica V1+V2 al Postgres locale (Flyway one-shot, idempotente)
./dev.sh seed      # carica questo seed (idempotente)
./dev.sh reset     # wipe volumi + ricrea stack + reseed (stato pulito deterministico)
```
