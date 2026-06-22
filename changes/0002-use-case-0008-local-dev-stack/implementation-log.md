# Implementation Log — Change 0002: Stack di sviluppo locale (Docker Compose: Postgres, Caddy, Mailpit, MinIO, ElasticMQ)

**Branch**: `change/0002-use-case-0008-local-dev-stack`
**Aree**: nuova cartella `dev/` (config: Compose/Caddyfile/HOCON/dotenv) + note negli use case eredi
**Completata**: 2026-06-22

## File modificati

| File | Azione |
|---|---|
| `dev/docker-compose.yml` | Creato — Postgres 17, Caddy, Mailpit, MinIO, ElasticMQ (+ `auth-local` placeholder commentato) |
| `dev/Caddyfile` | Creato — reverse proxy, TLS mkcert, path-routing prod replicato (route placeholder commentate) |
| `dev/.env.example` | Creato — template config locale (Postgres/MinIO/porte), valori fittizi |
| `dev/elasticmq.conf` | Creato — code iniziali `tenant-purge` / `gdpr-export` / `paddle-webhooks` + DLQ |
| `dev/README.md` | Creato — scopo stack, prerequisiti, avvio manuale, smoke test, confini con UC 0009/0010/0011/0023 |
| `dev/certs/.gitkeep` | Creato — cartella certificati mkcert (i `.pem` non sono committati) |
| `.gitignore` | Modificato — ignora `dev/certs/*` (tranne `.gitkeep`); `dev/.env` già coperto dal pattern globale |
| `docs/usecases/03-local-dev/0009-script-dev.md` | Modificato — nota di aggancio (gli script consumano `dev/`) |
| `docs/usecases/03-local-dev/0010-provider-auth-locale.md` | Modificato — nota di aggancio (placeholder `auth-local` da scommentare) |
| `docs/usecases/03-local-dev/0011-dati-seed.md` | Modificato — nota di aggancio (seed gira su Postgres/MinIO dello stack) |
| `docs/usecases/07-payments/0023-stub-paddle-locale.md` | Modificato — nota di aggancio (code `paddle-webhooks` predisposte) |
| `docs/usecases/_INDEX.md` | Modificato — UC 0008 🟡→✅ (e 🟡 a step-01) |

## Cosa è stato fatto

Realizzato lo **stack locale 100% offline (zero AWS)** in `dev/docker-compose.yml`: Postgres 17 (≈Aurora, connessione
diretta), **Caddy** come reverse proxy (≈API GW: TLS mkcert su `*.local.appgrove.app` + path-routing fedele a prod, un solo
origin per SPA), **Mailpit** (≈SES), **MinIO** (≈S3), **ElasticMQ** (≈SQS) con le code per purge/export/webhook già create.
I "buchi" demandati ad altri UC sono lasciati come **placeholder commentati e documentati** (`auth-local`→UC 0010; routing
servizi/SPA→UC 0009) e gli UC eredi (0009/0010/0011/0023) hanno una **nota di aggancio** che punta a questi file.

## Decisioni prese

- **Reverse proxy = Caddy** (vs Traefik): routing statico → `Caddyfile` minimale, TLS mkcert in una riga, HTTPS-by-default in linea con HSTS.
- **Stack sotto `dev/`** (vs repo root): coerente con dove vivranno gli script `dev` (UC 0009).
- **Placeholder documentati** per `auth-local` e routing servizi/SPA, con note di aggancio negli UC eredi (richiesta esplicita dello sviluppatore).
- **Rimosso l'endpoint stats ElasticMQ (porta 9325)**: non si avvia nell'immagine `elasticmq-native` e non è in DoD; la funzionalità SQS reale (9324) è verificata. Eventuale UI demandata al tooling `dev`.
- **Ambiente macchina**: installati Colima (engine open-source) + plugin `docker compose` + `mkcert`; `mkcert -install` (trust CA, richiede sudo interattivo) e `/etc/hosts` restano a carico dello sviluppatore / di `dev setup` (UC 0009). Annotato l'intoppo `JAVA_HOME`/keytool di mkcert in fase di generazione certificati (workaround: `env -u JAVA_HOME TRUST_STORES=system mkcert …`) → da gestire in `dev setup`.

## Invarianti appgrove

- **tenant_id dal JWT / filtro row-level**: non a runtime (infra di sviluppo). Lo stack è *predisposto* a renderli testabili offline — il provider auth locale (UC 0010) emetterà gli stessi claim sul JWKS locale, stesso code path Quarkus OIDC; il proxy a un-solo-origin replica same-site/CORS.
- **Modulo `microsaas_app`**: N/A (nessuna infra AWS).
- **Logging strutturato**: N/A a livello di stack (container loggano su stdout; CloudWatch solo in test).

## Note per il revisore

- **Contratto/convenzione introdotti**: assegnazione porte processi host (`auth-local`=9100, backend `8081+`, Vite `5173`/`5174`) e nomi stabili dei certificati (`dev/certs/local.appgrove.app(.|-key.)pem`) — UC 0009/0010 e `new-application` li consumano; documentati nel `Caddyfile`.
- **Avvio su checkout fresco**: richiede prima i certificati mkcert in `dev/certs/` (li genererà `dev setup`, UC 0009); senza, Caddy non parte (vincolo HTTPS/HSTS voluto).
- `dev/.env` e `dev/certs/*.pem` **non sono committati** (verificato con `git check-ignore`).

## Test

**Non applicabile come suite automatica** — la change tocca solo config (YAML/Caddyfile/HOCON/dotenv), nessun codice eseguibile (niente `mvn`/`npm`/`terraform`). **Verifica funzionale eseguita** con engine Colima (esito tutto verde):

- `docker compose -f dev/docker-compose.yml --env-file dev/.env config` → valido.
- `… up -d` → Postgres **healthy**, Mailpit **healthy**, MinIO/ElasticMQ/proxy **up**.
- Proxy HTTP `GET /healthz` → `appgrove-dev proxy ok`; HTTPS sui 4 host (`app./admin./api./apex local.appgrove.app`) → risposta servita con **certificato mkcert validato contro la rootCA** (no `-k`).
- Mailpit `/livez` 200 · MinIO `/minio/health/live` 200 · Postgres `pg_isready` ok.
- ElasticMQ `ListQueues` → 6 code presenti (`tenant-purge`, `gdpr-export`, `paddle-webhooks` + relative DLQ).

Baseline visiva E2E (#10 F): non toccata (lo stack è prerequisito degli E2E, che arriveranno con le app).

## Stato criteri di accettazione

- [x] `docker compose config` valido; definisce Postgres 17, proxy Caddy, Mailpit, MinIO, ElasticMQ con volumi persistenti e healthcheck dove sensato.
- [x] `up` porta i servizi reali healthy/up senza fallire (placeholder `auth-local` commentato).
- [x] Caddyfile termina TLS con mkcert su `*.local.appgrove.app` e contiene il path-routing prod replicato + placeholder commentati/documentati (`/api/auth/*`, `/api/<app_id>/v1/*`, upstream SPA un-solo-origin).
- [x] Zero-AWS; segreti solo da `.env` (non committato); `.env.example` committato.
- [x] Punti d'aggancio annotati `# UC 0009/0010/0011/0023` e UC eredi aggiornati con la nota ai placeholder.
