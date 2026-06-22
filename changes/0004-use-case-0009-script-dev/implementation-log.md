# Implementation Log — Change 0004: Script `dev/` (dispatcher + comandi) — UC 0009

**Branch**: `change/0004-use-case-0009-script-dev`
**Aree**: `dev/` (script shell di orchestrazione) + launcher/alias root + open-point negli UC eredi
**Completata**: 2026-06-22

## File modificati

| File | Azione |
|---|---|
| `dev/dev` | Creato — dispatcher `dev <comando>` (router + `--help` globale) |
| `dev/lib/common.sh` | Creato — helper condivisi (log/colori, `compose()`, `ensure_engine`/`ensure_env`, `gen_certs`, check porte/host); **port-aware**: legge le porte da `dev/.env` |
| `dev/lib/doctor.sh` | Creato — preflight read-only (engine, mkcert, certs, hosts, porte, prerequisiti) |
| `dev/lib/setup.sh` | Creato — bootstrap idempotente in 8 passi (con stub auth/seed/migrazioni) |
| `dev/lib/up.sh` / `down.sh` | Creati — start/stop stack (con hook processi-app vuoto) |
| `dev/lib/reset.sh` | Creato — wipe volumi + riavvio + reseed (reseed stub) |
| `dev/lib/seed.sh` / `migrate.sh` / `service.sh` | Creati — **stub** con `--help` e rimando allo UC proprietario |
| `dev.sh` | Creato — launcher root → `dev/dev` (`./dev.sh <comando>`) |
| `dev-start.sh` / `dev-stop.sh` | Modificati — ora **alias** di `dev up` / `dev down` |
| `dev/README.md` | Modificato — quickstart `./dev.sh` + troubleshooting |
| `docs/usecases/03-local-dev/0010-…md` | Modificato — open point (JWT in `setup`, `auth-local` in `up`) |
| `docs/usecases/03-local-dev/0011-…md` | Modificato — open point (`dev/lib/seed.sh` reale) |
| `docs/usecases/10-skills-tooling/0046-…md` | Modificato — open point (`migrate`/`service`/hook processi-app) |
| `docs/usecases/_INDEX.md` | Modificato — UC 0009 🟡→✅ |

## Cosa è stato fatto

Realizzato il **dispatcher unico `dev/dev`** (scelta A) con i comandi `doctor/setup/up/down/reset/seed/migrate/service`,
ognuno con `--help`, logica comune in `dev/lib/common.sh`, lanciabile dalla root con `./dev.sh <comando>` (alias `dev`
suggerito). I comandi **pieni** (`doctor`, `setup`, `up`, `down`, `reset`-wipe) funzionano contro lo stack UC 0008; quelli
**dipendenti** (`seed`, `migrate`, `service`, e i passi auth/config/migrazioni dentro `setup`/`reset`) sono **stub
idempotenti** che stampano un rimando allo UC proprietario senza far fallire lo script. `dev-start.sh`/`dev-stop.sh` sono
stati ridotti ad **alias** (scelta B).

## Decisioni prese

- **(A)** Superficie CLI completa con stub graziosi (vs solo-oggi); **(A)** dispatcher unico `dev/dev` (vs N script); **(B)** `dev-start/stop` come alias (vs rimozione).
- **Launcher `dev.sh` (non `./dev`)**: in root non può esistere un file `dev` perché c'è già la **cartella `dev/`** (conflitto di nome) → entrypoint root `./dev.sh`, con alias `dev="$(pwd)/dev/dev"` documentato per la UX `dev <comando>`.
- **Open point** registrati in UC 0010/0011/0046 per non perdere i task degli stub.
- Generazione certificati con `env -u JAVA_HOME` (evita lo snag keytool di mkcert); lo store Java si popola con `mkcert -install`.

## Invarianti appgrove

Nessuno a runtime — tooling che **prepara** l'ambiente (chiavi JWT locali → claim `tenant_id`/`roles` con UC 0010; schemi
per-app coi servizi). Gli stub esplicitano dove tali invarianti verranno resi veri.

## Note per il revisore

- **Entrypoint**: `./dev.sh <comando>` (o `dev/dev`); `./dev` non è possibile per il conflitto col folder `dev/`.
- **Hook lasciati**: passo JWT in `setup` (UC 0010), `dev/lib/up.sh` processi-app (UC 0010/0046), `seed.sh` (UC 0011), `migrate.sh`/`service.sh` (UC 0046) — tutti annotati come open point negli UC.
- **Ambiente (risolto)**: `doctor` ha fatto emergere un **Postgres host su 5432** che ombreggiava il container. Risolto su questa macchina: `brew services stop postgresql@18` (fermato + niente auto-start al login) e `dev/.env` → `POSTGRES_PORT=5433` (file gitignored, non nel commit). `common.sh` è stato reso **port-aware** (legge le porte da `dev/.env`), così `doctor` controlla la porta reale. Verificato: `psql -h 127.0.0.1 -p 5433` raggiunge il container (PostgreSQL 17.10). Il template committato `dev/.env.example` resta a 5432 (default del team).
- `mkcert -install` in `setup` può richiedere sudo interattivo: a CA già installata è no-op; in caso di errore lo step **avvisa** e prosegue (non blocca il setup).

## Test

**Non applicabile come suite automatica** — solo shell/tooling, nessun codice eseguibile applicativo (`mvn`/`npm`/`terraform`
N/A). `shellcheck` non installato sulla macchina → salto (non bloccante). **Verifica funzionale eseguita** (esito verde):

- `./dev.sh --help` e `./dev.sh <cmd> --help` → output corretto per ogni comando.
- `./dev.sh doctor` → exit 0, tutti i check verdi (segnala 5432 occupata, informativo).
- `./dev.sh up` → stack healthy/up; `./dev.sh down` → fermato; `./dev-start.sh`/`./dev-stop.sh` (alias) → ok.
- `./dev.sh setup` ri-eseguito → **idempotente** (no duplicazioni; certs/hosts/.env già presenti rilevati; stack up no-op).
- `./dev.sh seed|migrate|service` → stub stampano il rimando; `service` senza arg → exit 2 con uso.

## Stato criteri di accettazione

- [x] Dispatcher `dev/dev` con `dev --help` e `dev <cmd> --help`; launcher root `./dev.sh` funzionante.
- [x] `./dev.sh doctor` read-only con fix azionabili; verde su questa macchina.
- [x] `./dev.sh up` porta lo stack up; `down`/`down -v`/`reset` fermano/resettano.
- [x] `setup` idempotente (verificato sui passi non-sudo).
- [x] Comandi stub stampano rimando allo UC senza far fallire lo script.
- [x] `dev-start.sh`/`dev-stop.sh` alias di `up`/`down`; `dev/README.md` aggiornato.
- [x] Open point registrati in UC 0010, 0011, 0046.
