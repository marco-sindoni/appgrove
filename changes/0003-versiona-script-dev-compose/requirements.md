# Change 0003: Versiona gli script dev start/stop (wrapper Docker Compose)

**Branch**: `change/0003-versiona-script-dev-compose`
**Aree**: script shell di sviluppo in root (`dev-start.sh`, `dev-stop.sh`) — solo tooling, nessun codice eseguibile applicativo
**Data**: 2026-06-22
**Autore**: Platform Engineering
**Use case sorgente**: Nessuno (change ad-hoc) — anticipo provvisorio di UC 0009
**Tocca dati personali?**: No (tooling di sviluppo)

## Problema / Obiettivo

Nella change 0002 i due wrapper di `docker compose` per lo stack locale erano stati creati come **comodità personale,
esclusi dal versionamento** (`.git/info/exclude`). Ripensandoci, vanno **versionati** così sono condivisi/riproducibili.
Prima del commit i due script vanno **ricontrollati** (header, guardie, coerenza) e, per chiarezza d'uso, **rinominati** in
forma esplicita start/stop. Restano **provvisori**: gli script ufficiali `dev/` (`dev up`/`dev down`/`setup`/`doctor`/…)
arriveranno con **UC 0009** e li sostituiranno.

## Scope

- **Rinomina** `dev-compose.sh` → **`dev-start.sh`** (resta un wrapper passthrough di `docker compose`, default `up -d`); `dev-stop.sh` mantiene il nome.
- **Rimozione dall'esclusione git**: togliere `dev-compose.sh`/`dev-stop.sh` da `.git/info/exclude`; aggiungere `dev-start.sh`/`dev-stop.sh` come file **tracciati**.
- **Revisione/hardening** (entrambi):
  - header aggiornato: **ora versionati**, marcati **interim → superati da UC 0009**, riferimento allo stack UC 0008;
  - `set -eu`, `cd` alla dir dello script, guardia engine (Colima auto-start in start / "niente da fermare" in stop), auto-creazione `dev/.env` da `dev/.env.example` in start;
  - invocazione corretta `docker compose -f dev/docker-compose.yml --env-file dev/.env …` con passthrough degli argomenti.
- **Aggiornamento riferimenti use case**: nota in `docs/usecases/03-local-dev/0009-script-dev.md` che esistono gli script interim root e che gli script ufficiali `dev/` li rimpiazzeranno.
- **Push** del branch / merge secondo i gate.

## Fuori scope

- Gli **script ufficiali** `dev/` (`dev up/down/setup/seed/reset/migrate/service/doctor` + README) → **UC 0009**.
- Modifiche allo **stack Compose** (`dev/docker-compose.yml`, `Caddyfile`, ecc.) — invariato.
- Spostare gli script sotto `dev/` (scelta C: restano in root) o introdurre un dispatcher unico `dev`.
- Codice eseguibile in `infra/`, `frontend/`, `services/`.

## Criteri di accettazione

- [ ] `dev-start.sh` e `dev-stop.sh` sono **tracciati** da git (non più in `.git/info/exclude`); `dev-compose.sh` non esiste più.
- [ ] Entrambi sono eseguibili (`chmod +x`) e funzionano: `./dev-start.sh` porta su lo stack, `./dev-stop.sh` lo ferma (e `-v` resetta i volumi).
- [ ] Gli header non dichiarano più "NON versionato"; segnalano lo stato **interim** e il rimando a **UC 0009**.
- [ ] `docs/usecases/03-local-dev/0009-script-dev.md` cita gli script interim root da sostituire.

## Invarianti appgrove toccati

Nessuno a runtime (tooling di sviluppo). Gli script si limitano a orchestrare lo stack locale che già rispetta gli invarianti.

## Requisiti di test

Nessun codice eseguibile applicativo → `mvn`/`npm`/`terraform` **non applicabili**. Verifica funzionale: `./dev-start.sh`
porta i container healthy/up, `./dev-stop.sh` li ferma; se disponibile, `shellcheck` sui due file senza errori bloccanti.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (rinomina di uno script finora non versionato) |
| Contratto cross-area | N/A |
| Version bump | nessuno (tooling) |
