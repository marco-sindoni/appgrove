# UC 0008 — Local dev stack (Compose: Postgres, reverse proxy, Mailpit, MinIO, ElasticMQ)

**Area**: 03-local-dev · **Fase**: 0 · **Stato**: 🟢 deciso
**Dipendenze**: — (è la base degli UC 0009/0010/0011)
**Fonte decisioni**: #11 (orchestrazione locale, parità/limiti), #12 (tier local, domini, Postgres 17)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [11-developer-experience](../../11-developer-experience.md), [12-environments-config](../../12-environments-config.md), [06-infra-iac](../../06-infra-iac.md)

## 1. Obiettivo / Scope
Definire lo **stack locale 100% offline (zero AWS)** che riproduce i **comportamenti applicativi** di prod con strumenti
leggeri e gratuiti, così da sviluppare e provare l'intero sistema senza creare nulla su AWS (#11 direttiva).
**Incluso**: le dipendenze in **Docker Compose** — **Postgres** (≈Aurora), **reverse proxy** (≈API Gateway, path-routing),
**Mailpit** (≈SES), **MinIO** (≈S3), **ElasticMQ** (≈SQS), **provider auth locale** (porta dedicata, dettaglio in UC 0010).
**Escluso**: gli script che orchestrano (UC [0009](0009-script-dev.md)), il provider auth (UC [0010](0010-provider-auth-locale.md)),
il seed (UC [0011](0011-dati-seed.md)); l'infra AWS (test/prod), che non gira in locale (#11 F).

## 2. Attori & ruoli
- **Developer**: avvia/arresta lo stack, ci sviluppa contro.
- **Sistema (Compose)**: orchestra i container delle dipendenze.
- App (Quarkus dev mode + Vite) girano **come processi**, non in Compose (hot reload) — approccio **ibrido** #11 A2.

## 3. Precondizioni
- Docker in esecuzione; Postgres 17 come major allineato (#12 F); `/etc/hosts` con i domini `*.local.appgrove.app`;
  TLS locale via **mkcert** (`.app` è HSTS preload → HTTPS obbligatorio, #12 13). Il wiring lo fa `dev setup` (UC 0009).

## 4. Flusso principale
1. `dev up` (UC 0009) lancia `docker compose up` con i servizi-dipendenza.
2. **Postgres** parte persistente; schemi/ruoli locali creabili via `psql` (#12 2); Flyway crea le tabelle (UC 0009 `migrate`).
3. Il **reverse proxy** (Caddy/Traefik) replica il path-routing dell'API GW (`/api/<app_id>/v1/*` → porta servizio;
   `/api/auth/*` → provider locale) → **un solo origin** per le SPA, fedele a prod (#11 A3).
4. **Mailpit/MinIO/ElasticMQ** espongono SMTP/S3/SQS locali: così **export job** (#13 D) e **purge per-tenant** (#06 H)
   **girano davvero** in locale (#11 F15).
5. Le app si avviano come processi (`mvn quarkus:dev`, `vite`) puntando alle dipendenze Compose.

## 5. Flussi alternativi / edge / errori
- **Porta occupata / Docker spento** → diagnosticato da `dev doctor` (UC 0009) con fix azionabili.
- **Avvio selettivo**: si avviano core + l'app su cui si lavora, non tutti gli N servizi (#11 A4).
- **Niente LocalStack** (troppo pesante): strumenti mirati MinIO/ElasticMQ/Mailpit (#11 17).
- **Limiti non replicati in locale**: RDS Proxy (connessione diretta), IAM, CloudFront/WAF/ACM, CloudWatch (log su stdout
  JSON), KMS/encryption-at-rest → si validano in **test** (#11 16).

## 6. Risorse & runbook
**Servizi Compose** (`docker-compose.yml` a repo root o `dev/`):

| Servizio | Immagine (indicativa) | Sostituisce (AWS) | Note |
|---|---|---|---|
| postgres | `postgres:17` | Aurora Serverless v2 | persistente + seedato, volume locale |
| proxy | Caddy/Traefik | API Gateway v2 | path-routing `/api/<app_id>/v1/*` + `/api/auth/*` |
| auth-local | (UC 0010) | Cognito + auth Lambda | JWT/JWKS, claim dal DB, cookie refresh |
| mailpit | `axllent/mailpit` | SES | UI per le email transazionali |
| minio | `minio/minio` | S3 | bucket export GDPR/asset |
| elasticmq | `softwaremill/elasticmq` | SQS | code purge + webhook Paddle |

**Runbook**: `dev up` (start) · `dev down` (stop) · log via `docker compose logs`. **Reset** dati = volume drop (UC 0009 `reset`).

## 7. Dati toccati
Solo dati **locali sintetici** (seed, UC 0011); nessun dato reale/personale, nessuna risorsa AWS. Mai segreti reali
(config locale da `.env` non committato, #12 7). Manifest GDPR N/A (ambiente di sviluppo).

## 8. Permessi & gate
- **Invarianti**: lo stack è fedele a prod proprio sugli invarianti applicativi — il provider auth locale emette gli
  **stessi claim** (`tenant_id`/`roles`), i servizi usano lo **stesso code path** Quarkus OIDC sul JWKS locale (UC 0010),
  così il **filtro row-level** e il fail-closed sono testabili offline.
- Nessun entitlement/ruolo/quota a livello di stack (è infrastruttura di sviluppo).

## 9. Requisiti di test
Lo stack è **prerequisito degli E2E** (#10 F): gli E2E Playwright girano contro questo stack reale (backend + Postgres +
auth emulato). DEVE risultare: `dev up` porta su tutte le dipendenze in stato healthy; le SPA raggiungono i servizi via un
solo origin; email su Mailpit, file su MinIO, code su ElasticMQ funzionano (export/purge eseguibili in locale).

## 10. Riferimenti & Definition of Done
- **Decisioni**: #11 1/2/3/4/5/14/15/16/17, #12 1/2/3/8/11/13.
- **DoD**:
  1. `docker-compose.yml` definisce Postgres + proxy + Mailpit + MinIO + ElasticMQ (+ aggancio provider auth UC 0010).
  2. Path-routing locale identico a prod (un solo origin per SPA).
  3. Export job e purge per-tenant eseguibili in locale (ElasticMQ + MinIO).
  4. Zero dipendenze AWS; HTTPS locale via mkcert sui domini `*.local.appgrove.app`.
