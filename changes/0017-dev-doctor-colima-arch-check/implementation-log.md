# Implementation Log — Change 0017: affidabilità suite test backend su Colima/Docker

**Branch**: `change/0017-dev-doctor-colima-arch-check`
**Aree**: `dev/` (`dev/lib/doctor.sh`), `services/` (`pom.xml`, `fatture` test)
**Completata**: 2026-06-27

## File modificati

| File | Azione |
|---|---|
| dev/lib/doctor.sh | Modificato — nuovo step "Colima VM (host arm64)" |
| services/pom.xml | Modificato — surefire: pin `DOCKER_API_VERSION`/`api.version` + `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`; proprietà `docker.api.version` |
| services/fatture/.../QuotaStatusTest.java | Modificato — tenant su prefisso univoco `55555555` (no collisione con GdprContractTest) |

## Cosa è stato fatto

Reso `mvn test` dei servizi **affidabile in locale e CI** risolvendo una catena di cause emersa eseguendo la
suite backend su Apple Silicon + Colima + Docker 29:
- **doctor**: nuovo step read-only che, su host arm64, legge `~/.colima/default/colima.yaml` e segnala come
  **errore bloccante** una VM `arch=x86_64`/`amd64` (instabile / impossibile con `vz`), con il comando di fix;
  `warning` se nativo ma `vmType=qemu`; `ok` se nativo+vz; nessun rumore se la VM non esiste o host non-arm64.
- **pom (surefire, tutti i servizi)**: pin dell'API Docker per il client docker-java di Testcontainers
  (`DOCKER_API_VERSION`/`api.version` = `${docker.api.version}`, default `1.41`) — il bundle parla 1.32, rifiutata
  da Docker ≥29 (min 1.40); e `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` perché ryuk montava il
  path host del socket (assente dentro la VM Colima). Iniettati come **env del fork** (la shell non li propaga).
- **test**: tenant di `QuotaStatusTest` spostati da `33333333-…` a `55555555-…` per togliere la collisione con
  `GdprContractTest` (stesso `…a3`), che falsava il conteggio quota nel DB condiviso.

In parallelo (azione di ambiente, non versionata): VM Colima ricreata nativa **aarch64 + vz**; `docker.host`
verso il socket Colima aggiunto a `~/.testcontainers.properties` (config per-macchina).

## Decisioni prese

- Doctor resta **read-only** (diagnostica + comando di fix, nessun auto-fix).
- I parametri Docker/Testcontainers vivono nel **pom** (validi ovunque, niente env manuali); la sola
  `docker.host` (path del socket, specifico della macchina) resta in `~/.testcontainers.properties`.
- `DOCKER_API_VERSION=1.41`: ≥ minimo richiesto dai Docker recenti e supportato da Engine ≥20.10 → sicuro in CI.

## Invarianti appgrove

Nessuno toccato (tooling/test). Nessun codice applicativo, tenant a runtime, infra Terraform o logging applicativo.

## Note per il revisore

- Setup **per-macchina** necessario una volta (non versionabile): VM Colima nativa
  (`colima start --arch aarch64 --vm-type vz`) e `docker.host` in `~/.testcontainers.properties`. Il nuovo step
  `dev doctor` intercetta la VM mal configurata.
- `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`/`DOCKER_API_VERSION` nel pom sono no-op corretti su CI Linux/Docker
  Desktop (socket già `/var/run/docker.sock`, API negoziabile) → nessun rischio di regressione in pipeline.

## Test

- `bash -n dev/lib/doctor.sh` pulito; `./dev/dev doctor` → `✓ VM Colima nativa (aarch64/vz)`, nessun blocco
  (il ramo bloccante x86_64 è stato osservato sulla VM rotta, prima della ricreazione).
- `cd services && mvn -pl fatture -am test` → **BUILD SUCCESS**, `Tests run: 18, Failures: 0, Errors: 0,
  Skipped: 0` su Postgres reale (Testcontainers) via Colima Docker 29, **senza env manuali** nella shell.
  Verde e deterministico sull'intera batteria (collisione tenant risolta).

## Stato criteri di accettazione

- [x] `dev doctor`: VM x86_64 → errore + fix + exit ≠ 0; VM nativa aarch64+vz → `ok`; nessun falso allarme; `bash -n` pulito.
- [x] `mvn -pl fatture -am test` → BUILD SUCCESS via Colima/Docker 29 senza env manuali (pom + properties per-macchina).
- [x] Suite `fatture` verde e deterministica (no collisione di tenant).
