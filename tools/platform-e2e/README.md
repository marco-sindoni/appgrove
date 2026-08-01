# platform-e2e — suite end-to-end di piattaforma (UC 0090)

Il **quarto livello di test** del monorepo: un browser vero (Playwright) naviga i frontend
**costruiti davvero** contro lo **stack backend vero** (Postgres + ElasticMQ + **Mailpit** dal
compose dev, tutti i `services/*` scoperti automaticamente, in profilo `dev`), con le email
transazionali **realmente spedite e verificate** via Mailpit. Nessuna rotta intercettata,
nessun fornitore esterno: tutto offline e deterministico.

| Livello | Backend | Email | Dove |
|---|---|---|---|
| L2 Playwright | simulato (`page.route`) | no | `frontend/apps/*/e2e/` |
| Smoke headless | vero | — | `tools/smoke/` |
| **Piattaforma (qui)** | **vero** | **vera (Mailpit)** | `tools/platform-e2e/` |
| L3 sandbox | vero (cloud test) | vera | `frontend/apps/*/e2e-l3/` |

## Esecuzione

```bash
./run-tests.sh platform            # via l'entrypoint canonico
tools/platform-e2e/run.sh          # diretta: tutti i journey
tools/platform-e2e/run.sh --journey J-REG   # un solo journey (grep Playwright)
```

Prerequisiti: Docker (Colima), Java/Maven, Node. Nessun ambiente cloud.

**Convivenza**: i servizi girano su porta reale **+12000** (core 8080→20080, auth 9100→21100),
le SPA su **:24173** (backoffice) e **:24174** (admin) — la suite convive con lo stack dev
acceso (`app-start`) e con lo smoke (+10000). Il compose è idempotente; la doppia esecuzione
consecutiva non richiede pulizia (email e tenant unici per run). I container compose restano
su a fine corsa (come per lo smoke); i processi della suite vengono fermati.

## Architettura

- `run.sh` — orchestratore: compose (Postgres+ElasticMQ+Mailpit) → migrate+seed (gli stessi
  passi di `app-start`) → build+avvio dei servizi scoperti (`dev/lib/services.sh`, passi comuni
  in `dev/lib/headless.sh`, condivisi con lo smoke) → build SPA → `serve-spa.mjs` → Playwright.
- `serve-spa.mjs` — server statico (stdlib Node, zero dipendenze) che serve i `dist/` delle SPA,
  espone il `config.json` di suite e **inoltra `/api/*`** ai servizi della suite: le rotte sono
  derivate dalla stessa scoperta servizi del blocco `api-routes` del `dev/Caddyfile` — una
  nuova app entra nella suite senza toccare nulla qui.
- `helpers/` — libreria condivisa dei journey (per gli UC 0091/0092):
  - `mailbox` — attesa/lettura/estrazione link dalle email via API Mailpit (polling, timeout parlante);
  - `db` — asserzioni leak-detector via `docker exec psql` (sole letture, query parametrizzate);
  - `api` — `tenant()` (tenant fresco via signup+verifica email reale) e `login()` via API.
  - `paddle()` e `totp()` arrivano coi loro primi consumatori (UC 0091/0092).
- `journeys/` — un file per journey; il nome del file/test è l'ID (registro di copertura: UC 0093).

## Diagnosi dopo un rosso

- **Trace / screenshot / video Playwright**: `tools/platform-e2e/test-results/` (conservati su
  fallimento; `npx playwright show-trace <file.zip>` per il replay).
- **Log dei servizi e dei server SPA**: `tools/platform-e2e/.run/*.log` (build frontend inclusa).
- **Casella email**: interfaccia web di Mailpit su <http://localhost:8025> — le email della
  suite restano lì finché non si azzera la casella.
- **Rilancio mirato**: `tools/platform-e2e/run.sh --journey <id>`; un ritento che passa viene
  comunque segnalato dal reporter come "flaky" — va indagato, non ignorato (retry ≤ 1 di config).
- **Database**: lo stack scrive sul Postgres del compose dev (porta da `dev/.env`, default 5433):
  `docker exec -it $(docker ps --format '{{.Names}}' | grep -m1 postgres) psql -U appgrove -d appgrove`.

## Vincoli di qualità (dallo use case)

- Determinismo: zero dipendenze esterne, dominio email non recapitabile (`test.appgrove.local`).
- Journey **indipendenti e paralleli**: ogni journey crea da zero il proprio tenant.
- Niente attese a tempo fisso: solo polling su condizioni (API Mailpit, stati UI, DB).
- Fallimenti parlanti: mai un timeout anonimo del browser per un'email mancata.
- La suite **verifica** gli invarianti appgrove (tenant_id dal JWT, isolamento row-level) —
  non introduce superfici runtime.
