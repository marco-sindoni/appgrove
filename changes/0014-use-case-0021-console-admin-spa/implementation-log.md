# Implementation Log — Change 0014: Console Admin SPA + API admin cross-tenant (UC 0021)

**Branch**: `change/0014-use-case-0021-console-admin-spa`
**Aree**: frontend (nuova app `apps/admin`; +`packages/i18n`, +`packages/api-client` rigenerato), services/core (`AdminResource`), dev (Caddyfile, `app-start.sh`/`app-stop.sh`)
**Completata**: 2026-06-27

## File principali

| File | Azione |
|---|---|
| `services/core/.../platform/AdminResource.java`, `AdminDtos.java` | Creato — `/api/platform/v1/admin/*` gated `platform-admin`: read-only **cross-tenant** via query native + `PATCH apps/{id}` (disable-app, logging strutturato) |
| `services/core/src/test/.../AdminApiTest.java` | Creato — gating 403, liste cross-tenant, matrice entitlement derivata, disable-app (sui dati seed) |
| `services/core/.../META-INF/openapi/openapi.{yaml,json}` | Rigenerato — path/DTO admin |
| `frontend/packages/api-client/src/schema.ts` | Rigenerato (`gen`) — endpoint admin **tipizzati** |
| `frontend/apps/admin/**` | Creato — nuova SPA `@appgrove/admin` (:5174): config build, auth/sessione, api hooks tipizzati, guard `requireRole('platform-admin')`, shell (badge PLATFORM ADMIN), pagine Overview/Accounts(+dettaglio)/Users/Entitlements/Billing/DangerZone, Login(+gating), test+E2E |
| `frontend/packages/i18n/src/resources/en.ts`, `it.ts` | Modificato — namespace `admin` (EN+IT, parità) |
| `dev/Caddyfile` | Modificato — attivato `admin.local.appgrove.app` (single-origin SPA :5174 + `/api/*`) |
| `app-start.sh`, `app-stop.sh` | Modificato — avvio/stop SPA admin (:5174) + riepilogo |
| `dev/lib/migrate.sh` | Modificato — `JAVA_ARGS=-Xint` nel container Flyway (evita il SIGSEGV del JIT sotto emulazione x86_64 di colima) |
| `docs/_BACKLOG.md`, `docs/usecases/{0009,0013,0014}` | Modificato — registrazione dei 18 rinvii (registro canonico + pointer) |

## Cosa è stato fatto

Slice verticale MVP della console admin (UC 0021): **SPA separata** `apps/admin` (gated `platform-admin`) sopra una **nuova
superficie API admin** nel core. La console amministra **cross-tenant**: Overview (KPI), Accounts (lista + dettaglio
read-only con utenti + entitlement derivato), Users, **matrice Entitlements** (tenant×app derivata da `subscription` +
`app.status`), Billing read-only (stato locale), **Danger zone** (disable-app). In locale, login `admin@appgrove.test`
(claim `platform-admin` da auth-local); single-origin via Caddy `admin.local.appgrove.app`.

`AdminResource` usa **query native read-only** (che non passano dal filtro `@TenantId` di Hibernate): è la **prima**
superficie non-tenant-scoped, ammessa solo perché gated `platform-admin`. Unico write: `PATCH /admin/apps/{id}` su
`app.status` (disable-app, gate 2 — l'enforcement runtime è di UC 0014/0027), con **logging strutturato** dell'azione.

## Decisioni prese

- **Approccio A** (slice verticale): SPA admin **+** minimo di API admin nel core, per una console funzionante end-to-end.
- **Scelta (i)**: unico write = **disable-app platform-wide** (`app.status`). Il toggle entitlement **per-tenant** non è
  implementato — nel modello attuale l'unica leva per-tenant è la `subscription` → dettaglio Account **read-only** (rinvii #16/#17).
- **Native queries** per il cross-tenant (no entità JPA per `app`/`subscription`: sono di UC 0022/0025) — niente pre-emption del dominio.
- **`InvitationView`/DTO admin** tipizzati dallo schema generato (response body annotati → client tipizzato, niente cast).
- **Auth duplicata** (sottoinsieme minimo) in `apps/admin`; estrazione di un runtime condiviso rimandata (#18).

## Invarianti appgrove

- **tenant_id solo dal JWT** — gli endpoint admin **non** leggono `tenant_id` da request; i read sono **cross-tenant by
  design** e gated `platform-admin` (**eccezione esplicita e documentata** all'invariante #2). Le SPA non inviano `tenant_id`.
- **Filtro row-level** — gli altri endpoint (tenant) restano invariati e filtrati; l'admin è non-tenant-scoped solo dove
  previsto. Test: non-`platform-admin` → **403**.
- **Logging strutturato** — disable-app logga `app_id` + actor `sub` + esito.
- **Modulo `microsaas_app`** — N/A (nessuna infra; hosting prod → UC 0055).

## Note per il revisore

- **Pattern cross-tenant**: nuovo e sensibile. Mitigazioni: gating `@RolesAllowed(platform-admin)`, **solo letture** + il
  solo write `app.status`, query native esplicite. Formalizzazione del pattern → **UC 0013 / #02** (tracciato).
- **18 rinvii tracciati** (regola costituzione): registro canonico owner-tagged in `_BACKLOG.md` ("Console admin (UC 0021)")
  + pointer in UC 0013 (cross-tenant + schema override), 0014 (enforcement disable-app), 0009 (dev admin). Owner principali:
  UC 0022/0024/0025 (dominio catalogo/subscription/checkout), 0014/0027 (enforcement + override per-tenant), 0015/0016
  (auth cloud), 0055/0005/0003 (hosting/pipeline), 0035 (audit persistito), 0019 (Table base), 0020 (provider entitlement + runtime condiviso).
- **Privacy/RoPA**: vista admin di email/nome utenti (cross-tenant) — base "amministrazione piattaforma", **nessun nuovo
  trattamento** (lettura di dati già dichiarati in UC 0013); **audit persistito** rimandato (UC 0035).
- **E2E visual baseline (#10 F)**: asserzioni `getByRole`/`getByText`, nessuno snapshot visivo.
- **Ambiente**: durante lo sviluppo colima è andato in stato incoerente (daemon morto) → riavviato (`colima restart`) per
  i Testcontainers del core; i test core richiedono `DOCKER_HOST` sul socket colima (rinvio già noto in UC 0009).

## Test

Tutte le suite delle aree toccate **verdi**.

- **core** (`mvn -pl core test`, Testcontainers): **30** test, di cui **6** nuovi in `AdminApiTest` (403 gating,
  accounts/users cross-tenant, overview KPI, matrice entitlement derivata, disable-app + ripristino). Nessuna regressione.
- **frontend** (`npm test`): api-client 6 · design-system 25 · i18n 3 (parità incl. namespace `admin`) · **admin 8** ·
  backoffice 43. **Typecheck** `tsc` verde su tutti i workspace.
- **E2E Playwright**: **admin 1** (overview → apps → disabilita → riga "inactive") + backoffice 5 (regressione, verdi).
- **build**: `@appgrove/admin` build ok.

## Stato criteri di accettazione

- [x] SPA `apps/admin` (:5174, single-origin via Caddy `admin.local`); login `platform-admin` → console; non-admin → `/forbidden`.
- [x] `AdminResource` gated `platform-admin`: liste cross-tenant corrette; non-admin → **403**.
- [x] disable-app: `PATCH /admin/apps/{id}` toggla `app.status`; matrice esclude le app `inactive`; azione **loggata**.
- [x] Schermate Overview/Accounts(+dettaglio read-only)/Users/Entitlements/Billing/DangerZone; stati; EN/IT; a11y axe.
- [x] Test core + frontend + E2E + typecheck verdi.
- [x] 18 rinvii registrati (_BACKLOG owner-tagged + UC 0013/0014/0009); UC 0021 → ✅ in _INDEX.
