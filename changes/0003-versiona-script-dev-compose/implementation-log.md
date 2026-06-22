# Implementation Log — Change 0003: Versiona gli script dev start/stop

**Branch**: `change/0003-versiona-script-dev-compose`
**Aree**: script shell di sviluppo in root (tooling)
**Completata**: 2026-06-22

## File modificati

| File | Azione |
|---|---|
| `dev-start.sh` | Creato (rinomina di `dev-compose.sh`) — header aggiornato (versionato, interim → UC 0009) |
| `dev-stop.sh` | Modificato — header aggiornato (versionato, interim → UC 0009) |
| `dev-compose.sh` | Eliminato (rinominato in `dev-start.sh`) |
| `.git/info/exclude` | Modificato — rimosse le voci `dev-compose.sh`/`dev-stop.sh` (non più esclusi) |
| `docs/usecases/03-local-dev/0009-script-dev.md` | Modificato — nota sugli script interim root da sostituire |

> Nota: `.git/info/exclude` è locale al repo (non versionato); la sua modifica non compare nel diff committato ma è
> necessaria perché gli script diventino tracciabili.

## Cosa è stato fatto

I due wrapper di `docker compose` per lo stack locale (creati come comodità non versionata nella change 0002) sono stati
**ricontrollati e versionati**. `dev-compose.sh` è stato rinominato in **`dev-start.sh`** (nome esplicito start/stop, scelta
C dello sviluppatore), gli header ora dichiarano lo stato **versionato/interim** col rimando a **UC 0009**, e le voci sono
state tolte da `.git/info/exclude` così git li traccia. Funzionalità invariata: `dev-start.sh` crea `dev/.env` se manca,
avvia Colima se il daemon è giù, fa `up -d` (o passthrough); `dev-stop.sh` fa `down` (con `-v` per il reset volumi).

## Decisioni prese

- **Scelta C**: script in root con nomi espliciti `dev-start.sh` / `dev-stop.sh` (no spostamento sotto `dev/`, no dispatcher unico).
- Marcati **interim**: gli script ufficiali `dev/` di UC 0009 li sostituiranno e a quel punto i due wrapper root andranno rimossi.

## Invarianti appgrove

Nessuno toccato a runtime — è tooling di sviluppo che orchestra lo stack locale già conforme agli invarianti.

## Note per il revisore

- `dev-compose.sh` → `dev-start.sh` è una rinomina di un file finora **non versionato**: in git appare come **nuovo file**, non come rename.
- Gli script dipendono da `dev/docker-compose.yml` + `dev/.env` (stack della change 0002, già su `main`).

## Test

**Non applicabile come suite automatica** — solo shell/tooling, nessun codice eseguibile applicativo (`mvn`/`npm`/`terraform`
N/A). `shellcheck` non installato sulla macchina → salto (non bloccante). **Verifica funzionale eseguita** (esito verde):
`./dev-start.sh` → stack su (Postgres/Mailpit healthy, proxy `GET /healthz` = `appgrove-dev proxy ok`); `./dev-start.sh ps`
→ container elencati; `./dev-stop.sh` → stack fermato e rete rimossa.

## Stato criteri di accettazione

- [x] `dev-start.sh` e `dev-stop.sh` tracciati da git; `dev-compose.sh` rimosso; voci tolte da `.git/info/exclude`.
- [x] Entrambi eseguibili e funzionanti: `./dev-start.sh` su, `./dev-stop.sh` giù (`-v` per reset).
- [x] Header senza "NON versionato"; segnalano stato interim e rimando a UC 0009.
- [x] `0009-script-dev.md` cita gli script interim root da sostituire.
