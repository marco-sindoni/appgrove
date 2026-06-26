# UC 0009 — Script `dev/` (setup, up/down, seed, reset, migrate, service, doctor) + README

**Area**: 03-local-dev · **Fase**: 0 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0008](0008-stack-sviluppo-locale.md) (stack Compose)
**Fonte decisioni**: #11 C (setup script), #12 (config locale)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [11-developer-experience](../../11-developer-experience.md), [06-infra-iac](../../06-infra-iac.md)

> **Aggancio da change 0002 (UC 0008).** Lo stack Compose è già in `dev/` (`docker-compose.yml`, `Caddyfile`,
> `.env.example`, `elasticmq.conf`, `certs/`). Gli script qui devono **consumare** quei file, non riscriverli:
> `dev setup` → `mkcert -install` + genera i certificati in `dev/certs/` coi **nomi stabili** attesi dal `Caddyfile`
> (`local.appgrove.app.pem` / `-key.pem`), scrive `/etc/hosts`, copia `dev/.env.example`→`dev/.env`, avvia l'engine
> (`colima start`); `dev up/down` → `docker compose -f dev/docker-compose.yml --env-file dev/.env up/down`; `dev doctor`
> → verifica engine/mkcert/hosts/porte. **Routing servizi**: assegna le porte secondo la convenzione documentata nel
> `Caddyfile` e scommenta i blocchi `handle /api/<app_id>/v1/*` (auto-wiring `new-application`, #11 §11).

> **Interim da change 0003.** In root esistono già due wrapper provvisori — `dev-start.sh` (≈`dev up`) e `dev-stop.sh`
> (≈`dev down`, con `-v` per il reset volumi) — versionati come comodità. Gli script ufficiali `dev/` di questo UC li
> **sostituiscono**: a quel punto i due wrapper root vanno rimossi.

## 1. Obiettivo / Scope
Definire l'insieme di **shell script documentati** sotto `dev/` che rendono lo sviluppo locale **senza intoppi** (#11 C),
con uno **stile wrapper** speculare a `infra/scripts/` (#06 §25): nomi espliciti, `--help` ciascuno, idempotenti/auto-riparanti,
README quickstart.
**Incluso**: `setup`, `up`/`down`, `seed`/`reset`, `migrate`, `service <app_id>`, `doctor`, + README.
**Escluso**: la definizione dello stack (UC 0008), il contenuto del seed (UC 0011), il provider auth (UC 0010).

## 2. Attori & ruoli
- **Developer** (anche principiante): esegue gli script copia-incolla.
- **Skill `new-application`** (futuro, UC 0046): una nuova app si **auto-aggancia** all'orchestrazione (`dev up` la prende, #11 11).

## 3. Precondizioni
- Repo clonato; Docker installato; prerequisiti locali (Postgres client, Node, Java, Maven) — verificati da `doctor`.

## 4. Flusso principale (onboarding)
1. `dev doctor` → preflight: diagnostica prerequisiti, porte, mkcert, permessi `/etc/hosts`, Docker; fix **azionabili** (#11 9/10).
2. `dev setup` (one-time, idempotente): prereq, **mkcert + hosts**, chiavi JWT locali, `docker compose`, **db init + Flyway**,
   **seed**, scrittura `config.json` locale (#11 8, #12 6/11).
3. `dev up` → avvia stack (UC 0008) + processi app; `dev down` → arresta tutto.
4. `dev service <app_id>` → avvia selettivamente core + l'app su cui si lavora (#11 A4).
5. Durante il lavoro: `dev migrate` (Flyway), `dev seed` (ricarica), `dev reset` (wipe + reseed).

## 5. Flussi alternativi / edge / errori
- **Setup ripetuto**: idempotente — non duplica, ripara ciò che manca.
- **Ambiente rotto**: `dev doctor` indica il fix esatto (porta, certificato, daemon Docker).
- **Comando senza arg richiesto**: stampa `--help` e si ferma.
- **DB già migrato**: `migrate` è no-op sulle versioni già applicate.

## 6. Risorse & runbook
**Script** (`dev/`, ognuno con `--help`):

| Comando | Cosa fa | Idempotente |
|---|---|---|
| `dev doctor` | preflight ambiente, fix azionabili | sì (read-only) |
| `dev setup` | bootstrap one-time completo | sì |
| `dev up` / `dev down` | start/stop stack + app | sì |
| `dev seed` | carica il seed deterministico (UC 0011) | sì |
| `dev reset` | wipe volumi + reseed | sì |
| `dev migrate` | Flyway sul Postgres locale | sì |
| `dev service <app_id>` | avvio selettivo core + app | sì |

**README quickstart** ("i 5 comandi che userai"): comandi copia-incolla, **output atteso**, stima tempi, troubleshooting
(porte/mkcert/hosts/Docker). Requisito: README **estremamente chiaro** (#11 10).

## 7. Dati toccati
Solo ambiente locale: volume Postgres, file `config.json` locale, `.env` (non committato; `.env.example` committato).
Nessun dato reale/personale. Manifest GDPR N/A.

## 8. Permessi & gate
- **Invarianti**: gli script preparano l'ambiente che li rende veri (chiavi JWT locali → claim `tenant_id`/`roles`; schemi
  per-app). N/A a runtime.
- Nessun gate runtime; `setup`/`reset` operano solo in locale (mai su AWS).

## 9. Requisiti di test
Verifica funzionale: su una macchina pulita, `doctor` → `setup` → `up` → app raggiungibile, senza passaggi manuali nascosti
(setup "senza intoppi", #11 10). `reset` riporta a uno stato deterministico (base degli E2E #10 F). Gli script sono
prerequisito d'esecuzione delle suite E2E in locale.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #11 1/8/9/10/11, #12 6/11/13.
- **DoD**:
  1. Esistono gli script `dev/*` con `--help` e README quickstart.
  2. `dev setup` è one-time, idempotente e porta a un ambiente funzionante senza intoppi.
  3. `seed`/`reset`/`migrate`/`service` funzionano e sono idempotenti.
  4. Una nuova app creata da `new-application` viene presa automaticamente da `dev up`.

## Punti aperti / decisioni differite

_Tracciati dalla change `0006-use-case-0012-…` (regola CLAUDE.md "Tracciamento delle decisioni differite")._

- **`DOCKER_HOST` per i test backend (Testcontainers + colima).** I test d'integrazione dei servizi (Quarkus Dev
  Services) richiedono un Docker daemon; con **colima** il socket non è in `/var/run/docker.sock`, quindi serve
  esportare `DOCKER_HOST=unix://$HOME/.colima/default/docker.sock` (+ `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`).
  Valutare che `dev doctor`/`dev setup` rilevino il socket colima e lo esportino/segnalino, così `mvn test` gira
  senza configurazione manuale.

_Aggiunti dalla change `0012-use-case-0017-…` (avvio/verifica dello stack completo per gli E2E manuali della UI auth)._

- **`app-start.sh` / `app-stop.sh` provvisori → assorbire in `dev`.** Per avviare *tutte* le app insieme
  (auth-local + core + SPA) sono stati creati due script alla radice. Sono un ponte: il comando canonico
  `dev service` (stub → UC 0046) dovrebbe avviare core + app selettivamente, e `dev up` orchestrare anche la SPA.
  Quando arriva quel comando, deprecare gli script radice o renderli wrapper sottili.
- **Wiring Caddy single-origin SPA — atterrato in anticipo.** Il blocco `app.local.appgrove.app` è stato attivato
  (`import api_routes` + `reverse_proxy …:5173`) e a `(api_routes)` è stata aggiunta la rotta
  `/api/platform/* → core :8080`; `config.json` ora punta same-origin a `https://app.local.appgrove.app` e Vite gira
  con `host:true` + `allowedHosts:['.local.appgrove.app']`. Questo è lavoro di UC 0009: qui è stato anticipato il
  minimo per la verifica manuale. Da consolidare in UC 0009 (es. rotte per-app, admin SPA :5174, vetrina apex).
- **core in dev sul Postgres CONDIVISO (non DevServices).** `app-start.sh` lancia `core` con
  `QUARKUS_DATASOURCE_*` + `QUARKUS_HTTP_PORT=8080` verso il Postgres dello stack (:5433, schema `platform` già
  migrato), così core e auth-local condividono i dati seed (necessario per onboarding → `PATCH /accounts/me`). Il
  profilo `%dev` di core resta su DevServices effimero: **da decidere** se renderlo il default di dev (spostando
  l'injection in `application.properties`/`dev service`). Owner naturale: `dev service` (UC 0046) + config core.
