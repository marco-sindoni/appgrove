# Implementation Log — Change 0008: Seed data deterministico (UC 0011)

**Branch**: `change/0008-use-case-0011-dati-seed`
**Aree**: `dev/` (seed + script) + `services/core` (integration test) + docs (`_INDEX`, decisioni differite)
**Completata**: 2026-06-26

## File modificati

| File | Azione |
|---|---|
| dev/seed/seed.sql | Creato (cast deterministico idempotente) |
| dev/seed/README.md | Creato (ID stabili, token inviti, runbook) |
| dev/lib/migrate.sh | Modificato (stub → Flyway one-shot core reale) |
| dev/lib/seed.sh | Modificato (stub → up+migrate+psql del seed) |
| dev/lib/setup.sh | Modificato (step 7/8 → avvio stack + migrazioni + seed) |
| services/core/src/test/java/app/appgrove/core/SeedDataTest.java | Creato |
| docs/usecases/_INDEX.md | Modificato (0011 🟡→✅) |
| docs/usecases/10-skills-tooling/0046-skill-new-application.md | Modificato (differita: migrate multi-servizio) |
| docs/usecases/03-local-dev/0010-provider-auth-locale.md | Modificato (differita: gruppo platform-admin + JWT sui subject del seed) |

## Cosa è stato fatto

Realizzato il seed deterministico in `dev/seed/seed.sql` (UUID e timestamp **fissi**, `INSERT … ON CONFLICT (id)
DO UPDATE` → idempotente): cast multi-tenant (Acme B2B owner/admin/member, Bob B2C, Platform), 2 inviti pending,
catalogo `notes`/`teams`/`legacy(inactive)` con tier+limits+prezzi, e 5 subscription in stati `active`/`past_due`/
`trialing`/`canceled` + app disabilitata. Reso reale `dev migrate` (Flyway one-shot su `services/core` via container
`flyway/flyway`, core-only) e `dev seed` (stack up → migrate → `psql` del seed); `dev setup`/`dev reset` ora migrano e
seedano davvero. Validazione con `SeedDataTest` (Testcontainers) che applica lo stesso `seed.sql` due volte.

## Decisioni prese

- **Righe del seed marcate `created_by = 'seed'`**: permette al test (DB condivisa) e alle query di scoping di isolare
  il cast senza dipendere da conteggi globali.
- **`platform-admin` come gruppo JWT, non colonna**: l'utente piattaforma è seedato con `role=owner` del proprio
  account; la capacità `platform-admin` la assegnerà l'auth locale (UC 0010) sul subject `seed-platform-admin`.
- **App disabilitata = `app.status='inactive'`** (`legacy`) con subscription `active` sopra → esercita il gate
  "app abilitata" (#09 dec.30) senza tabella di enablement per-tenant.
- **`dev migrate` core-only** via Flyway Docker (no tool sull'host, no AWS); multi-servizio → UC 0046.

## Invarianti appgrove

- **Tenant ID**: il seed scrive `tenant_id` (= account id) esplicito su ogni riga tenant-scoped e crea ≥2 tenant
  apposta per la matrice cross-tenant. Nessuna lettura runtime qui (caricamento dati).
- **Filtro row-level**: alimentato (non introdotto) — esercitato da UC 0013 e dai futuri E2E.
- **Logging strutturato / modulo `microsaas_app`**: N/A (script di dati + test, nessun servizio runtime né infra).

## Note per il revisore

- **Cross-area (soft)**: il seed è il **contratto dati** dev↔E2E; UC 0010 dipende dai `cognito_sub`/ID stabili
  documentati in `dev/seed/README.md`. Nessun contratto rotto.
- **Dati personali = No**: cast 100% sintetico (email `*.test`); manifest/RoPA N/A.
- **Decisioni differite tracciate**: **UC 0046** (industrializzazione `dev migrate` multi-servizio + aggancio
  `new-application`); **UC 0010** (minting JWT sui subject del seed + assegnazione gruppo `platform-admin`).
- **Esecuzione**: i test e il runbook richiedono Docker (Testcontainers / container Flyway). Con colima esportare
  `DOCKER_HOST=unix://$HOME/.colima/default/docker.sock`. `dev seed` richiede `psql` sull'host.

## Test

`services/core` (`mvn test`, Testcontainers Postgres 17 + Flyway): **24 test verdi** (incl. `SeedDataTest`):
idempotenza su doppia applicazione, cast multi-tenant (3 account / 5 utenti / 2 inviti pending), catalogo
(single/multi/**inactive**), 5 subscription con stati `active`(2)/`past_due`/`trialing`/`canceled`, ≥2 tenant,
email tutte `*.test`. `services/commons`: 3 test verdi. **BUILD SUCCESS**.

**Runbook reale verificato** contro lo stack locale: `./dev.sh seed` → stack su, Flyway applica V1+V2, seed caricato;
ri-eseguito `./dev.sh seed` → conteggi stabili (3/5/2/3/4/4/5) = idempotente. Gli script `dev/` (bash) non hanno un
framework di unit test nel repo: il comportamento dell'SQL è coperto da `SeedDataTest`, il runbook da verifica manuale.

## Stato criteri di accettazione

- [x] `dev/seed/seed.sql` idempotente con UUID/timestamp fissi; `README.md` documenta cast, ID stabili e token inviti.
- [x] `dev migrate` applica V1+V2 (Flyway one-shot, idempotente); `dev seed` migra poi carica; `dev reset`/`dev setup` reseedano.
- [x] Cast ≥2 tenant (Acme B2B + Bob B2C) + Platform, ruoli owner/admin/member, catalogo single/multi/disabled, subscription trialing/active/past_due/canceled + app disabilitata.
- [x] `SeedDataTest` verde: cast/stati + idempotenza su doppia esecuzione.
