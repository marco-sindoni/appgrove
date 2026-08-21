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

## Persone e appartenenze (UC 0116)

Dalla change 0088 una persona è **due righe**: l'**identità** (`platform.identity`, dato di
piattaforma: chi è la persona) e l'**appartenenza** (`platform.membership`, dato dell'account: che
ruolo ha lì). Gli id delle identità sono gli **stessi** che avevano le righe utente di prima, così
ogni riferimento memorizzato altrove (per esempio `invitations.invited_by`) resta valido.

| Persona | identità (id) | appartenenza (id) | account | cognito_sub | email | ruolo |
|---|---|---|---|---|---|---|
| Acme Owner | `b0000000-…-001` | `d0000000-…-001` | Acme | `seed-acme-owner` | owner@acme.test | owner |
| Acme Admin | `b0000000-…-002` | `d0000000-…-002` | Acme | `seed-acme-admin` | admin@acme.test | member ** |
| Acme Member | `b0000000-…-003` | `d0000000-…-003` | Acme | `seed-acme-member` | member@acme.test | member |
| Bob | `b0000000-…-004` | `d0000000-…-004` | Bob | `seed-bob-owner` | bob@bob.test | owner |
| Platform Admin | `b0000000-…-005` | `d0000000-…-005` | Platform | `seed-platform-admin` | admin@appgrove.test | owner * |

Ogni persona del seme ha **una sola** appartenenza: è il caso di tutti gli utenti di oggi, e il seme
deve restare la fotografia del caso normale. Il caso «una persona, due account» si costruisce nei
collaudi automatici, non qui.

`platform.users` esiste ancora nello schema ma è **fredda** (rete di ritorno del travaso, change
0088): il seme non la popola e nessun codice la legge.

\* La capacità **`platform-admin`** è un **gruppo JWT**, non il ruolo di un'appartenenza: l'auth locale
(UC 0010) assegna il gruppo `platform-admin` al subject `seed-platform-admin`.

\*\* Dalla change 0091 (UC 0098) il ruolo di piattaforma ha **due soli valori** — `owner` e `member` — e il
potere sta sull'**applicazione**. `admin@acme.test` conserva il nome ma è `member` di piattaforma con ruolo
`admin` sul Mini-CRM: **non** vede più le schermate riservate all'owner (Account, Billing, Members). È la
stessa traduzione che UC 0113 applicherà agli account reali.

## Accessi per applicazione (`platform.app_access`, tenant Acme)

| id | persona | applicazione | ruolo |
|---|---|---|---|
| `e0000000-…-001` | admin@acme.test (`b0000000-…-002`) | `crm` | admin |
| `e0000000-…-002` | member@acme.test (`b0000000-…-003`) | `crm` | editor |

L'**owner non ha righe** qui: l'accesso gli è implicito su tutte le applicazioni dell'account, e ogni lettura
di «chi ha accesso» lo aggiunge al risultato. L'`app_id` si legge dal catalogo, che non sta in `seed.sql`:
dove il catalogo non esiste (i servizi di sola identità applicano solo `seed.sql`) il blocco non inserisce
nulla invece di fallire.

## Inviti pending (tenant Acme)

| Invito | id | email | role | token (grezzo) |
|---|---|---|---|---|
| primo | `c0000000-…-001` | invitee-admin@acme.test | member | `seed-invite-acme-admin` |
| secondo | `c0000000-…-002` | invitee-member@acme.test | member | `seed-invite-acme-member` |

Entrambi di ruolo `member`: chi entra **non porta con sé alcun potere** (UC 0098), i poteri si concedono dopo
una applicazione alla volta. Il primo indirizzo conserva il nome storico `invitee-admin` perché i collaudi
lo nominano.

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
> (es. auth) applicano **solo** `seed.sql` (accounts/identity/membership/invitations), non le subscription.

| Tenant | App | Tier | Stato | Note |
|---|---|---|---|---|
| Acme | Teams | team | `active` | multi-user attivo |
| Acme | Notes | pro | `past_due` | |
| Bob | Notes | free | `trialing` | `trial_end` futuro |
| Acme | Legacy | std | `active` | app `inactive` → esercita il gate "app abilitata" |
| Bob | Teams | team | `canceled` | `cancel_at` valorizzato |

## Storico pagamenti (`billing_transaction`, UC 0096)

Stesso file `seed-subscriptions.sql`. Senza queste righe la sezione «Payments & receipts» della pagina Billing
sarebbe vuota su ogni account locale e non sarebbe osservabile senza prima fare un acquisto. Sono coerenti con
gli abbonamenti qui sopra.

| Tenant | App | Esito | Importo | Ricevuta | Note |
|---|---|---|---|---|---|
| Acme | Teams | `paid` | €19,00 | sì | giugno 2024 |
| Acme | Teams | `paid` | €19,00 | sì | maggio 2024 |
| Acme | Notes | `failed` | €9,00 | **no** | è il motivo del `past_due`; esercita il caso «ricevuta non disponibile» |
| Bob | Teams | `paid` | €19,00 | sì | pagato prima della disdetta: lo storico resta |

## Runbook

```bash
./dev.sh migrate   # applica V1+V2 al Postgres locale (Flyway one-shot, idempotente)
./dev.sh seed      # carica questo seed (idempotente)
./dev.sh reset     # wipe volumi + ricrea stack + reseed (stato pulito deterministico)
```
