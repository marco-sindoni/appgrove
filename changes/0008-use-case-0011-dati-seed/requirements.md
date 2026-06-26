# Change 0008: Seed data deterministico (condiviso dev↔E2E) — UC 0011

**Branch**: `change/0008-use-case-0011-dati-seed`
**Aree**: `dev/` (script `migrate`/`seed`/`setup` + nuovo `dev/seed/`) + `services/core` (integration test del seed)
**Data**: 2026-06-26
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/03-local-dev/0011-dati-seed.md](../../docs/usecases/03-local-dev/0011-dati-seed.md)
**Tocca dati personali?**: **No** — dati **100% sintetici** (email `*.test`, nessun PII reale), coerente #08/#13/#10 I33. Manifest GDPR N/A (sintetico).

## Problema / Obiettivo

UC 0013 ha consegnato le tabelle del core ma vuote. Questa change realizza il **seed deterministico, idempotente, versionato** (ID stabili) condiviso tra **dev locale** ed **E2E** (#11 D12, #10 I32/33): un unico cast multi-tenant + catalogo + subscription in stati di lifecycle vari, così i flussi sono riproducibili. Sblocca l'auth locale (UC 0010, che minta JWT sui subject del seed) e gli E2E.

## Scope

### 1. Artefatto seed versionato — `dev/seed/seed.sql` (+ `dev/seed/README.md`)

SQL **idempotente** (`INSERT … ON CONFLICT (id) DO UPDATE`), **UUID fissi e documentati**, **timestamp audit fissi** (no `now()` → determinismo). Cast:

| Entità | Contenuto |
|---|---|
| **accounts** | **Acme** (B2B multi-user), **Bob** (B2C single-user), **Appgrove Platform** (home del platform-admin) — tutti `active` |
| **users** | Acme: owner+admin+member; Bob: owner; Platform: 1 utente (capacità `platform-admin` data dal gruppo JWT in UC 0010) — `cognito_sub` stabili, email `*.test` |
| **invitations** | Acme: 2 inviti **pending** (un admin, un member), `token_hash` di token fissi documentati |
| **catalogo** (`app`/`app_tier`/`app_price`) | `notes` (single_user, `active`), `teams` (multi_user, `active`), `legacy` (multi_user, **`inactive`** = disabilitata dall'admin); tier con `limits` jsonb (metrica/finestra/tetto, esempi **flow** e **stock**) e `trial_days`; `app_price` monthly+annual per i tier a pagamento (#05 A7, #09 B10) |
| **subscription** | Acme→teams **active**; Acme→notes **past_due**; Bob→notes **trialing** (`trial_end` futuro); Acme→legacy **active** ma su app `inactive` (esercita il gate "app abilitata"); Bob→teams **canceled** (`cancel_at`) |

Copre ≥2 tenant (matrice cross-tenant #10 D8), B2C+B2B+ruoli, e gli stati che alimentano l'**entitlement derivato** e la **catena di gate** (#09 dec.29/30): app abilitata / entitlement / ruolo / quota. `README.md` documenta gli **ID stabili** (gli E2E ci asseriscono sopra) e i token degli inviti.

### 2. `dev migrate` reale (solo core) — `dev/lib/migrate.sh`

Applica le migrazioni Flyway di `services/core` al Postgres dello stack via **one-shot del container `flyway/flyway`** (monta `services/core/src/main/resources/db/migration`, `-schemas=platform`, `-connectRetries`), idempotente (no-op sulle versioni già applicate). Zero AWS, nessun tool sull'host. Il multi-servizio è tracciato a **UC 0046**.

### 3. `dev seed` reale + `dev reset` + `dev setup` — `dev/lib/seed.sh`, `setup.sh`

- `dev seed`: assicura lo stack su, esegue `dev migrate` (idempotente), poi carica `dev/seed/seed.sql` via `psql` (prereq host) con `ON_ERROR_STOP=1`.
- `dev reset`: già richiama `cmd_seed` al passo 3/3 → ora reseeda davvero (deterministico).
- `dev setup`: step "DB init + migrazioni + seed" passa da stub a reale (stack `up --wait` → migrate → seed).

### 4. Validazione automatica — integration test in `services/core`

`SeedDataTest` (Testcontainers + Flyway già attivi nel profilo `%test`): applica lo **stesso** `dev/seed/seed.sql` (letto dal repo), **due volte**, e asserisce: cast presente con gli ID/stati attesi; **idempotenza** (ri-esecuzione = stesso stato, nessun duplicato); copertura B2C+B2B+ruoli+stati subscription. Asserzioni **scoped agli ID/tenant del seed** (la DB di test è condivisa con le altre suite).

## Fuori scope

- **`dev migrate` multi-servizio** (scoperta di tutti i servizi, ordinamento, aggancio `new-application`) → **UC 0046** (tracciato).
- **Minting JWT locali sui subject del seed** + assegnazione gruppo **`platform-admin`** → **UC 0010** (tracciato).
- **Suite E2E Playwright** che consuma il seed (#10 F) → use case E2E dedicati; qui il seed è solo *prodotto* e validato via integration test.
- **Popolamento "vero" di catalogo/subscription** dalla pipeline pricing-sync/webhook (UC 0022/0025): qui sono **dati di seed sintetici**, non il flusso runtime.
- **Seed-base auto-generato da `new-application`** (#11 11, #10 I32) → UC 0046.
- **Entità JPA** di catalogo/subscription: restano assenti (UC 0022/0025); il seed scrive via SQL.

## Criteri di accettazione

- [ ] `dev/seed/seed.sql` idempotente con UUID/timestamp fissi; `dev/seed/README.md` documenta cast, ID stabili e token inviti.
- [ ] `dev migrate` applica V1+V2 al Postgres locale (Flyway one-shot), idempotente; `dev seed` carica il seed dopo aver migrato; `dev reset`/`dev setup` reseedano.
- [ ] Il cast copre ≥2 tenant (Acme B2B + Bob B2C) + platform account, ruoli owner/admin/member, catalogo (single/multi/disabled), subscription in trialing/active/past_due/canceled + app disabilitata.
- [ ] `SeedDataTest` verde: cast/stati attesi + idempotenza su doppia esecuzione (`mvn test` in `services/core`).

## Invarianti appgrove toccati

- **Tenant ID**: il seed scrive `tenant_id` (= account id) esplicito su ogni riga tenant-scoped (`users`/`invitations`/`subscription`), creando ≥2 tenant apposta per testare **filtro row-level** e fail-closed (UC 0011 §8). Nessuna lettura runtime qui: è caricamento dati.
- **Filtro row-level**: il seed **alimenta** i test cross-tenant (A non vede i dati di B) ma non introduce query; l'invariante è esercitato altrove (UC 0013 + futuri E2E).
- **Logging strutturato**: N/A (script di caricamento dati, non codice di servizio runtime).
- **Modulo `microsaas_app`**: N/A (nessuna infra).

## Requisiti di test

- **Integration (`services/core`, Testcontainers)**: idempotenza (riesecuzione = stesso stato, nessun duplicato), ID stabili, copertura cast (B2C+B2B+ruoli) e stati subscription (trialing/active/past_due/canceled + app disabled) per esercitare gate ed entitlement derivato; dati sintetici senza PII.
- **Script `dev/` (bash)**: nessun framework di unit test bash nel repo → validazione del comportamento via il test di integrazione sopra (che copre l'SQL) + verifica manuale del runbook (`dev migrate`/`dev seed`/`dev reset`); annotato nel log.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (nuovo artefatto + comandi dev prima stub) |
| Contratto cross-area | Sì (soft) — il seed è il **contratto dati** condiviso dev↔E2E e consumato da UC 0010 (subject/ID stabili) |
| Version bump | minor (nuovo seed + comandi dev resi reali) |
