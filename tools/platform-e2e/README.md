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
tools/platform-e2e/run.sh --journey J-REG   # un solo journey (grep Playwright + --no-deps)
```

Prerequisiti: Docker (Colima), Java/Maven, Node. Nessun ambiente cloud.

## I journey (UC 0090 + batteria utente UC 0091 + batteria admin/guasti UC 0092)

| ID | Funzionalità coperta | Note |
|---|---|---|
| `J-REG` | Registrazione: signup → email di verifica reale → onboarding → dashboard (+ gate legale del primo ingresso) | UC 0090 |
| `J-BUY` | Acquisto e attivazione: catalogo → tier a pagamento → fake Paddle → webhook in coda → sidebar + modulo montato | Mini-CRM Team |
| `J-QUOTA` | Quota a consumo (fatture): banner, tetto, 429 reale + invito upgrade | il ramo upgrade è in J-MEMBERS (dec. 4, change 0070) |
| `J-MEMBERS` | Inviti B2B con email reale, seconda sessione browser, posti (seat) fino al 429, upgrade che sblocca, ruoli, rimozione, ultimo owner | Mini-CRM |
| `J-SUB` | Ciclo abbonamento: downgrade programmato, disdetta, scadenza via webhook firmato, 402 + dati intatti, riattivazione | Mini-CRM Team |
| `J-PWD` | Reset password con email reale + 2FA TOTP con challenge reale al login (bypass dev spento per l'auth della suite) | |
| `J-PRIVACY` | Diritti GDPR: rettifica, export asincrono reale (ZIP su MinIO, download validato), recesso per-app con purge, eliminazione con grace | |
| `A-GDPR` | Console Diritti GDPR fra due attori: ticket privacy + export → aggregazione in console → risposta visibile al cliente → limitazione art. 18 applicata **e rimossa** | UC 0092 |
| `A-ENTITLE` | Coerenza della matrice dei diritti: tre tenant (acquisto attivo · sola fascia gratuita · account in eliminazione), matrice della console ≡ menu laterale del cliente | UC 0092 |
| `A-CONSOLE` | Disabilitazione applicazione fra due attori: l'admin disabilita → il cliente perde app e rotta ma **non** i dati → riabilita → registro delle transizioni | progetto `admin-serial` |
| `F-DEGRADE` | Guasti veri: servizio **fermato davvero** → errore con "riprova" e mai diniego → riavvio → rientro senza ricaricare → sessione invalidata lato server | progetto `degrade-serial` |
| `J-LEGAL` | Ri-accettazione legale: nuova major via leva d'ambiente → gate bloccante, esenzione `/privacy`, accettazione registrata | progetto `legal-serial`, ultimo |

Stato di catalogo: il **global-setup** attiva l'app `crm` (leva admin del seed platform-admin) per
l'intera esecuzione e il teardown la ripristina `inactive` — è l'unica app del registry frontend con
un tier a pagamento (decisione 3, change 0070).

**Ordine di esecuzione** (progetti in catena, `playwright.config.ts`): i journey confinati al proprio
tenant girano **in parallelo** nel progetto `chromium`; quelli che muovono stato **globale** girano da
soli, in coda, uno alla volta —

```
chromium  →  admin-serial (A-CONSOLE)  →  degrade-serial (F-DEGRADE)  →  legal-serial (J-LEGAL)
```

La dipendenza fra progetti è l'unico meccanismo che garantisce sequenzialità stretta anche fra file
diversi (`fullyParallel: false` serializza i test dentro un file, non i file fra loro). `J-LEGAL`
resta ultimo perché la sua leva è la più invasiva. Con `--journey <id>` la catena viene scavalcata
(`--no-deps`) e gira il solo journey chiesto.

⚠️ **Dopo `degrade-serial` il Mini-CRM è di nuovo disabilitato.** `F-DEGRADE` riavvia il core, e ogni
avvio del core rilancia la sincronizzazione del listino, che riallinea lo stato delle app allo YAML —
dove `crm` è `inactive` di proposito (change 0042). Un journey nuovo che abbia bisogno del Mini-CRM va
quindi collocato **prima** di `degrade-serial`, oppure deve riattivare l'app da sé. Per la stessa
ragione `F-DEGRADE` osserva `fatture`, che nello YAML è attiva e offre una fascia gratuita di base.

**Convivenza**: i servizi girano su porta reale **+12000** (core 8080→20080, auth 9100→21100),
le SPA su **:24173** (backoffice) e **:24174** (admin) — la suite convive con lo stack dev
acceso (`app-start`) e con lo smoke (+10000). Il compose è idempotente; la doppia esecuzione
consecutiva non richiede pulizia (email e tenant unici per run). I container compose restano
su a fine corsa (come per lo smoke); i processi della suite vengono fermati.

## Architettura

- `run.sh` — orchestratore: compose (Postgres+ElasticMQ+Mailpit) → migrate+seed (gli stessi
  passi di `app-start`) → build+avvio dei servizi scoperti (`dev/lib/services.sh`, passi comuni
  in `dev/lib/headless.sh`, condivisi con lo smoke) → build SPA → `serve-spa.mjs` → Playwright.
- `service-ctl.sh` + `.run/services.json` — governo del ciclo di vita dei servizi della suite
  (UC 0092). `run.sh` scrive il **descrittore** (porta, registro, variabili d'ambiente per ruolo)
  nello stesso ciclo in cui accende i servizi, e `service-ctl.sh` è l'**unico esecutore**: primo
  avvio, riavvii di `F-DEGRADE` e spegnimento finale passano di lì. Un solo percorso di codice,
  quindi nessun secondo elenco di variabili che col tempo resta indietro; e poiché il descrittore
  deriva dalla scoperta servizi, una nuova app diventa governabile senza toccare nulla.
- `serve-spa.mjs` — server statico (stdlib Node, zero dipendenze) che serve i `dist/` delle SPA,
  espone il `config.json` di suite e **inoltra `/api/*`** ai servizi della suite: le rotte sono
  derivate dalla stessa scoperta servizi del blocco `api-routes` del `dev/Caddyfile` — una
  nuova app entra nella suite senza toccare nulla qui.
- `helpers/` — libreria condivisa dei journey (per gli UC 0091/0092):
  - `mailbox` — attesa/lettura/estrazione link dalle email via API Mailpit (polling, timeout parlante);
  - `db` — asserzioni leak-detector via `docker exec psql` (sole letture; `dbExec` è riservato
    alle **leve d'ambiente**, oggi solo la pubblicazione della versione legale di J-LEGAL);
  - `api` — `tenant()` (tenant fresco via signup+verifica email reale), `login()`/`loginRaw()`/
    `loginMfa()` via API, `pollUntil()` (attese su condizione), `authedFetch()`;
  - `paddle` — acquisto programmatico via checkout reale + **webhook sintetici firmati** (HMAC
    del contratto UC 0023) inviati all'ingest vero;
  - `totp` — codici RFC 6238 (SHA1/6 cifre/30s) coerenti col servizio auth;
  - `browser` — `browserLogin()`, attraversamento del gate legale del primo ingresso e
    `adminSession()` (seconda sessione browser sulla console admin, contesto isolato);
  - `services` — `stopService()` / `startService()` / `ensureServiceUp()`: la leva che permette a
    `F-DEGRADE` di produrre un guasto **vero** (processo fermo, non risposta finta).
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
