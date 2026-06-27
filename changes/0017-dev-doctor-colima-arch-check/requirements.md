# Change 0017: affidabilità suite test backend su Colima/Docker (doctor + pin Docker API + fix tenant)

**Branch**: `change/0017-dev-doctor-colima-arch-check`
**Aree**: `dev/` (`dev/lib/doctor.sh`), `services/` (`pom.xml` surefire, `fatture` test)
**Data**: 2026-06-27
**Autore**: Platform Engineering
**Use case sorgente**: Nessuno (change ad-hoc, follow-up DevX/affidabilità test)
**Tocca dati personali?**: No

## Problema / Obiettivo

La suite backend (`mvn test`, `@QuarkusTest` → Testcontainers/Dev Services) non era eseguibile in modo
affidabile in locale. Debug end-to-end ha trovato una catena di cause, dalla più profonda:
1. **VM Colima `arch=x86_64` + `vmType=vz` su Apple Silicon**: combinazione impossibile (vz esegue solo guest
   arm64) → il guest non completa il boot (`cloud-final.service` fallito → guest agent assente → `docker.sock`
   non inoltrato → `docker info` rifiuta, `colima status` = `empty value`).
2. **docker-java (bundle Testcontainers) parla API 1.32**, rifiutata da Docker Engine recenti (29.x → min 1.40).
3. **ryuk** montava il socket col **path host** (`~/.colima/.../docker.sock`, inesistente dentro la VM).
4. **Collisione di tenant** fra `QuotaStatusTest` e `GdprContractTest` (stesso `33333333-…-a3`) → conteggio
   quota sporcato nel DB condiviso quando gira l'intera suite.

Obiettivo: rendere `mvn test` **affidabile in locale e CI** e **prevenire** la ricaduta della VM mal configurata.

## Scope

- `dev/lib/doctor.sh`: nuovo step "Colima VM" che, **solo** se host arm64 + Colima con VM creata, legge
  `~/.colima/default/colima.yaml` e: `arch` x86_64/amd64 → **errore bloccante** con fix; nativo ma `vmType=qemu`
  → **warning** (suggerisce vz); nativo + vz → `ok`; VM assente/host non-arm64 → nessun rumore.
- `services/pom.xml` (surefire, vale per tutti i servizi): pin **`DOCKER_API_VERSION`** (default `1.41`,
  proprietà `docker.api.version`) come env + system property `api.version`, e
  **`TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`** — iniettati nel JVM forkato (l'env della
  shell non viene ereditato). Universali: su CI Linux/Docker Desktop sono no-op corretti.
- `services/fatture` test `QuotaStatusTest`: tenant spostati su prefisso univoco `55555555` per eliminare la
  collisione con `GdprContractTest`.

## Fuori scope

- Auto-fix/ricreazione automatica della VM (doctor resta read-only).
- Config **per-macchina** di Testcontainers (`~/.testcontainers.properties`: `docker.host` verso il socket
  Colima): non versionabile, resta sulla macchina dello sviluppatore (documentata nelle note).
- Cambi al lifecycle Colima in `up`/`setup`; logica multi-profilo Colima.

## Criteri di accettazione

- [ ] `dev doctor`: VM `x86_64` su arm64 → errore + fix + exit ≠ 0; VM nativa `aarch64`+`vz` → `ok` senza blocchi;
      nessun falso allarme senza VM / host non-arm64; `bash -n` pulito.
- [ ] `cd services && mvn -pl fatture -am test` → **BUILD SUCCESS** su Docker recente (29.x) via Colima, senza
      variabili d'ambiente manuali nella shell (config nel pom + `~/.testcontainers.properties`).
- [ ] Suite `fatture` verde e **deterministica** rieseguendo l'intera batteria (nessuna collisione di tenant).

## Invarianti appgrove toccati

Nessuno (tooling/test; nessun codice applicativo, tenant a runtime, infra Terraform o logging applicativo).
Il pin dell'API Docker e il socket override riguardano solo l'esecuzione dei test.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A |
| Version bump | nessuno (tooling/test) |
