# Change 0004: Script `dev/` — dispatcher `dev` (doctor/setup/up/down/seed/reset/migrate/service) + README

**Branch**: `change/0004-use-case-0009-script-dev`
**Aree**: `dev/` (script shell di orchestrazione locale) + launcher root `./dev` + alias `dev-start.sh`/`dev-stop.sh` + note open-point negli UC eredi — solo tooling, nessun codice eseguibile applicativo
**Data**: 2026-06-22
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/03-local-dev/0009-script-dev.md](../../docs/usecases/03-local-dev/0009-script-dev.md)
**Tocca dati personali?**: No (tooling di sviluppo; opera solo in locale)

## Problema / Obiettivo

Realizzare l'insieme di **script `dev/` documentati** che rendono lo sviluppo locale "senza intoppi" (#11 C, #06 §25):
un **dispatcher unico `dev`** con sottocomandi `dev <comando>`, ognuno con `--help`, idempotente/auto-riparante, + README
quickstart. Poiché diverse dipendenze non esistono ancora (auth/JWT → UC 0010, seed → UC 0011, servizi/Flyway, frontend/
`config.json`), si adotta la **strategia (A)**: **superficie CLI completa subito**, con i comandi pronti **pienamente
funzionanti** e quelli dipendenti come **stub idempotenti che rimandano allo UC proprietario**. Gli **open point** vengono
registrati negli UC che finalizzeranno gli stub, così nessun task si perde.

## Scope

**Struttura** (dispatcher unico, scelta A; stile wrapper #06 §25):
- `dev/dev` — dispatcher/router: `dev <comando> [args]`, `dev --help` (elenca i comandi), `dev <cmd> --help`.
- `dev/lib/common.sh` — helper condivisi: logging/colori, repo root, `compose()` (= `docker compose -f dev/docker-compose.yml --env-file dev/.env`), `ensure_engine` (Colima), `ensure_env` (`dev/.env` da template), `require_cmd`, check porte/host.
- `dev/lib/<cmd>.sh` — un file per comando (sourcato dal dispatcher).
- `./dev` (root) — launcher sottile → `exec dev/dev "$@"` (così digiti `./dev up`).
- `dev-start.sh`/`dev-stop.sh` — ridotti ad **alias** (scelta B): chiamano `./dev up` / `./dev down`.

**Comandi PIENI (funzionanti oggi contro lo stack UC 0008):**
- `dev doctor` — preflight **read-only**: docker CLI + engine raggiungibile, plugin compose, mkcert + CA installata, certificati in `dev/certs/`, voci `/etc/hosts` dei 4 domini, porte libere, prerequisiti (node/java/mvn/psql). Stampa **fix azionabili** copia-incolla; exit ≠0 se ci sono blocchi.
- `dev setup` — bootstrap one-time **idempotente**: `ensure_engine`; `mkcert -install` (rileva già-fatto; workaround `JAVA_HOME`/keytool); genera i certificati in `dev/certs/` coi nomi stabili attesi dal Caddyfile se mancano; aggiunge le voci `/etc/hosts` mancanti (via `sudo`, con conferma/azionabile); crea `dev/.env`; porta su lo stack (`compose up -d`). I passi non ancora disponibili sono **stub** (sotto).
- `dev up` / `dev down` — start/stop stack (`compose up -d` / `down`; `down -v` per reset volumi). `up` predispone l'**hook** per avviare i processi-app (vuoto finché non esistono app).

**Comandi STUB (presenti, `--help`, idempotenti, rimando allo UC):**
- `dev seed` — rimanda a **UC 0011** (nessun seed ancora definito).
- `dev migrate` — Flyway sul Postgres locale: rimanda ai **servizi/UC 0046** (nessuna migrazione/servizio ancora).
- `dev service <app_id>` — avvio selettivo core+app: valida l'arg e rimanda ai **servizi/UC 0046** (nessuna app ancora).
- `dev reset` — la parte **wipe** funziona (`compose down -v` + `up`); il **reseed** è stub (UC 0011).
- passi **stub dentro `setup`**: chiavi **JWT locali** → UC 0010; **`config.json`** runtime → frontend; **db init + Flyway/seed** → UC 0011/servizi.

**Open point da registrare negli UC eredi** (così nessun task si perde):
- UC 0010 (auth): riempire lo step JWT in `setup` + agganciare `auth-local` in `up` + route `/api/auth/*`.
- UC 0011 (seed): implementare `dev seed` (`dev/lib/seed.sh`) + il reseed in `setup`/`reset`.
- UC 0046 (`new-application`): riempire `dev migrate` (Flyway), `dev service <app_id>`, l'avvio processi-app in `up` e l'auto-wiring (#11 §11).

**README**: aggiornare `dev/README.md` con il quickstart "i comandi che userai" (copia-incolla, output atteso, troubleshooting porte/mkcert/hosts/Docker), sostituendo le istruzioni manuali con `./dev …`.

## Fuori scope

- Definizione dello **stack** (UC 0008, già su `main`) — gli script lo **consumano**.
- **Contenuto** del seed (UC 0011), **provider auth**/JWT (UC 0010), **servizi**/Flyway e **auto-wiring** (UC 0046): qui solo stub + open point.
- Frontend/`config.json` reale (non esiste il frontend).
- Codice eseguibile in `infra/`, `frontend/`, `services/`.

## Criteri di accettazione

- [ ] Esiste il dispatcher `dev/dev` con `dev --help` (elenco comandi) e `dev <cmd> --help`; `./dev` (root) funziona come launcher.
- [ ] `./dev doctor` gira read-only e segnala fix azionabili; su questa macchina (già configurata) passa verde.
- [ ] `./dev up` porta lo stack healthy/up; `./dev down` lo ferma; `./dev down -v` / `./dev reset` resettano i volumi.
- [ ] `dev setup` è idempotente: ri-eseguito non duplica e ripara solo ciò che manca (verificato sui passi non-sudo).
- [ ] I comandi stub (`seed`/`migrate`/`service`, e i passi auth/config in `setup`) stampano un messaggio chiaro col rimando allo UC e non falliscono lo script.
- [ ] `dev-start.sh`/`dev-stop.sh` sono alias di `./dev up`/`./dev down`; `dev/README.md` aggiornato al quickstart `./dev`.
- [ ] Gli open point sono registrati in UC 0010, 0011, 0046.

## Invarianti appgrove toccati

Nessuno a runtime (tooling). Gli script **preparano** l'ambiente che renderà veri gli invarianti (chiavi JWT locali → claim
`tenant_id`/`roles` quando arriva UC 0010; schemi per-app col `microsaas_app`/servizi quando arriveranno) — qui solo predisposizione.

## Requisiti di test

Nessun codice eseguibile applicativo → `mvn`/`npm`/`terraform` **non applicabili**. Verifica funzionale: `./dev doctor`
(verde sulla macchina configurata), `./dev up`→stack healthy, `./dev down`, idempotenza di `setup` (ri-run no-op sui passi
non-sudo), e ogni comando stub stampa il rimando senza errore. `shellcheck` se disponibile.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (nuovo tooling; `dev-start.sh`/`dev-stop.sh` diventano alias, comportamento invariato) |
| Contratto cross-area | N/A oggi; definisce gli **hook** che UC 0010/0011/0046 riempiranno |
| Version bump | nessuno (tooling) |
