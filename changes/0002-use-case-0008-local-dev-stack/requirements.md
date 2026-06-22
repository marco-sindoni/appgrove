# Change 0002: Stack di sviluppo locale (Docker Compose: Postgres, Caddy, Mailpit, MinIO, ElasticMQ)

**Branch**: `change/0002-use-case-0008-local-dev-stack`
**Aree**: nuova cartella `dev/` (Docker Compose + reverse proxy Caddy + `.env.example`) — solo config (YAML/Caddyfile/dotenv), nessun codice eseguibile (Terraform/Java/TS)
**Data**: 2026-06-22
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/03-local-dev/0008-stack-sviluppo-locale.md](../../docs/usecases/03-local-dev/0008-stack-sviluppo-locale.md)
**Tocca dati personali?**: No (ambiente di sviluppo; solo dipendenze infrastrutturali locali, nessun dato reale/PII — manifesto GDPR N/A, UC 0008 §7)

## Problema / Obiettivo

Realizzare lo **stack locale 100% offline (zero AWS)** che riproduce i **comportamenti applicativi** di prod con strumenti
leggeri e gratuiti (UC 0008; decisioni #11, #12). Concretamente: un `docker-compose.yml` che porta su le dipendenze —
**Postgres 17** (≈Aurora), **reverse proxy Caddy** (≈API Gateway, path-routing + TLS), **Mailpit** (≈SES), **MinIO** (≈S3),
**ElasticMQ** (≈SQS) — più i **punti d'aggancio documentati** per il provider auth locale (UC 0010) e per il routing verso i
servizi/SPA (che girano come processi host, UC 0009).

Le app (Quarkus dev mode + Vite) girano **come processi sull'host**, non in Compose (modello ibrido #11 §2): il proxy le
raggiunge via `host.docker.internal`.

## Scope

Tutto sotto la nuova cartella **`dev/`** (coerente con dove vivranno gli script `dev` di UC 0009, #11 §8):

- **`dev/docker-compose.yml`** — definisce i servizi:
  - `postgres` — immagine `postgres:17` (#12 §8), volume persistente nominato, healthcheck, porta esposta su host; credenziali/DB da `.env`.
  - `proxy` — Caddy, monta il `Caddyfile` e la cartella certificati mkcert (read-only); espone 80/443; `extra_hosts: host-gateway` per raggiungere i processi host.
  - `mailpit` — `axllent/mailpit`, porte SMTP + UI.
  - `minio` — `minio/minio`, porte API + console, volume persistente, credenziali da `.env`.
  - `elasticmq` — `softwaremill/elasticmq`, porta SQS-compatibile; monta `dev/elasticmq.conf` con le code iniziali (purge per-tenant + webhook Paddle, UC 0008 §6/§4.4).
  - `auth-local` — **blocco commentato** con nota `# UC 0010` (placeholder: immagine/JWT/JWKS li riempie UC 0010).
- **`dev/Caddyfile`** — reverse proxy che replica il path-routing prod e dà **un solo origin** alle SPA (#11 §3, #12 §11):
  - TLS via certificati mkcert sui domini `*.local.appgrove.app` (direttiva `tls`).
  - Route **attive/dimostrabili oggi**: salute del proxy.
  - Route **placeholder commentate e documentate**: `/api/auth/*` → `auth-local` (`# UC 0010`); `/api/<app_id>/v1/*` →
    `host.docker.internal:<porta>` come template con la **convenzione di assegnazione porte** (`# UC 0009 + new-application auto-wiring`);
    upstream SPA `app.local.appgrove.app` / `admin.local.appgrove.app` / vetrina `local.appgrove.app` → Vite host (`# UC 0009`).
- **`dev/.env.example`** — template committato (credenziali Postgres/MinIO locali, nomi DB/coda) con valori fittizi; il `.env` reale non è committato (#12 §7).
- **`dev/elasticmq.conf`** — configurazione code iniziali per ElasticMQ.
- **`dev/certs/.gitkeep`** + voce in `.gitignore` — cartella dove `dev setup` (UC 0009) genererà i certificati mkcert; i certificati non sono committati.
- **`dev/README.md`** — nota breve sullo scopo dello stack, comando di avvio manuale (`docker compose -f dev/docker-compose.yml up`) e rimando agli script `dev` (UC 0009) per l'orchestrazione completa.
- **`.gitignore`** — esclusione di `dev/.env` e `dev/certs/*` (tranne `.gitkeep`).
- **Note di aggancio negli use case eredi** — aggiungere in UC 0009 / 0010 / 0011 (e dove rilevante 0023) un riferimento ai
  placeholder lasciati qui, così i punti d'aggancio non si perdono.
- **`docs/usecases/_INDEX.md`** — riga UC 0008 → 🟡 (già fatto a step-01) → ✅ a chiusura (step-04).

## Fuori scope

- **Script `dev/` di orchestrazione** (`dev setup/up/down/seed/reset/migrate/service/doctor`) → **UC 0009**.
- **Provider auth locale** (immagine, firma JWT, JWKS, claim dal DB, refresh cookie, TOTP) → **UC 0010** (qui solo placeholder commentato).
- **Seed data** deterministico → **UC 0011**.
- **Stub Paddle locale** → **UC 0023** (qui solo, eventualmente, la coda ElasticMQ predisposta).
- **Migrazioni Flyway / creazione schemi-ruoli** → girano via gli script di UC 0009 contro questo Postgres.
- **Qualsiasi infra AWS** (test/prod, Terraform) — non gira in locale (UC 0008 §1, #11 §F).
- **Codice eseguibile** in `infra/`, `frontend/`, `services/`.

## Criteri di accettazione

- [ ] `docker compose -f dev/docker-compose.yml config` è valido (parsing OK) e definisce `postgres` (17), `proxy` (Caddy), `mailpit`, `minio`, `elasticmq`, con volumi persistenti e healthcheck dove sensato.
- [ ] `docker compose -f dev/docker-compose.yml up` porta i servizi reali (Postgres/proxy/Mailpit/MinIO/ElasticMQ) in stato **healthy** senza fallire su immagini inesistenti (il placeholder `auth-local` è commentato).
- [ ] Il `Caddyfile` termina il TLS con i certificati mkcert sui domini `*.local.appgrove.app` e contiene il path-routing prod replicato: route attive oggi + placeholder commentati e **documentati** per `/api/auth/*`, `/api/<app_id>/v1/*` e gli upstream SPA (un solo origin).
- [ ] Lo stack è **zero-AWS**: nessuna immagine/dipendenza AWS; segreti solo da `.env` (non committato); `.env.example` committato come template.
- [ ] I punti d'aggancio (`auth-local`, routing servizi/SPA, certs, code) hanno una nota `# UC 0009/0010/0011/0023` e gli use case eredi citano i placeholder lasciati qui.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT / filtro row-level** — non a runtime in questa change (è infra di sviluppo), ma lo stack è
  predisposto a renderli **testabili offline**: il provider auth locale (UC 0010) emetterà gli **stessi claim** e i servizi
  useranno lo **stesso code path Quarkus OIDC** sul JWKS locale; il proxy a un-solo-origin replica same-site/CORS di prod (UC 0008 §8).
- **Modulo Terraform `microsaas_app`** — N/A (nessuna infra AWS qui).
- **Logging strutturato** — N/A a livello di stack (i container delle dipendenze loggano su stdout; CloudWatch è solo in test, #11 §16).

## Requisiti di test

Nessun codice eseguibile → `mvn`/`npm`/`terraform` **non applicabili**. Verifica funzionale (step-03/04):
`docker compose -f dev/docker-compose.yml config` valido; `up` porta le dipendenze healthy; smoke check che Mailpit (UI),
MinIO (console/API) ed ElasticMQ (endpoint code) rispondano. Lo stack è **prerequisito degli E2E Playwright** (#10 F, UC 0008 §9):
la baseline visiva non è toccata qui.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (nuovo apparato, nessun consumatore esistente) |
| Contratto cross-area | N/A oggi; definisce la convenzione di path-routing/porte che UC 0009/0010 e `new-application` consumeranno |
| Version bump | nessuno (infra di sviluppo) |
