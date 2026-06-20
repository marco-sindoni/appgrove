# Developer experience / local dev — Decisioni

**Stato**: 🟢 deciso
**Ultimo aggiornamento**: 2026-06-21

## Scope
Come si sviluppa appgrove in locale: setup, esecuzione di frontend + servizi + DB, emulazione di auth/Cognito,
seed data, e gli script che rendono tutto semplice. Border con [12-environments-config](12-environments-config.md)
(tier/ambienti) e [06-infra-iac](06-infra-iac.md) (Terraform/AWS, NON usato in locale).

## Direttiva registrata (2026-06-16)
- **Sviluppo 100% locale, ZERO AWS**: si deve poter sviluppare e provare l'intero sistema senza creare nulla su AWS;
  gli ambienti cloud (test/prod) si creano **solo a sviluppo finito**. _Supera la scelta #12 "pool Cognito dev su AWS"._
- **Setup con uno script**: ambiente di sviluppo avviabile in modo semplice con un singolo script (ben documentato),
  coerente con lo spirito dei wrapper di [06-infra-iac](06-infra-iac.md) §25.

## Topic dell'area (agenda, da discutere)
- **A. Orchestrazione locale** — come girano insieme DB Postgres + servizi Quarkus + 2 frontend (docker compose? processi? Quarkus dev mode?).
- **B. Emulazione auth/Cognito in locale** — emulatore Cognito (es. `cognito-local`) vs OIDC/JWT fittizio vs Quarkus OIDC mock; come si replica il flusso auth Lambda + claim `tenant_id`/`roles` + cookie.
- **C. Setup script** — un comando per: dipendenze, DB locale + schemi/grant, migrazioni Flyway, seed, avvio servizi/frontend. Documentato.
- **D. Seed data** — dati demo coerenti (account, utenti, app catalog, entitlements) per lavorare subito.
- **E. Config locale** — `config.json` runtime puntato al locale; profili Quarkus `%dev`; domini locali + mkcert (da #12 §11).
- **F. Parità & limiti** — cosa NON è replicabile in locale (RDS Proxy, IAM, EventBridge reale?) e come si simula (es. LocalStack mirato? o stub).

## Decisioni prese
1. **Locale 100% offline (zero AWS)** + **setup script unico e documentato** (direttiva sopra).

### A. Orchestrazione locale
2. **Approccio IBRIDO**: **infra in Docker Compose** + **app in dev mode come processi** (per l'hot reload).
   - **Compose** (dipendenze): **Postgres** persistente+seedato, **provider auth locale** (B), **reverse proxy**
     path-routing, **Mailpit** (email), **MinIO** (S3) + **ElasticMQ** (SQS) (F);
   - **Quarkus dev mode** (`mvn quarkus:dev`, hot reload) + **Vite dev server** per le 2 SPA (hot reload), come processi.
3. **Reverse proxy locale** (Caddy/Traefik) **replica il path-routing dell'API Gateway** (`/api/<app_id>/v1/*` →
   porta servizio + route auth) → un solo origin per le SPA, locale fedele a prod.
4. **Avvio selettivo dei servizi** (core + l'app su cui lavori, non tutti gli N).
5. **Postgres esplicito in Compose** (persistente+seedato), **non** Dev Services effimeri (quelli per i test, #10 C).

### B. Emulazione auth/Cognito
6. **Identity provider dietro interfaccia nel BFF auth**, scelto per profilo: **Cognito** (test/prod) vs **Local** (dev).
7. **Provider Local (dev)**: utenti nel **Postgres locale**, **firma JWT in locale** (access/id) con claim `sub`+
   `tenant_id`+`roles` **dal DB** (replica Pre-Token-Gen), espone **JWKS** locale, stesso **refresh cookie**; email su
   **Mailpit**; **2FA TOTP** con lib reale (+ toggle bypass dev). I servizi usano **Quarkus OIDC** sul **JWKS locale** in
   `%dev` → **stesso code path di prod**, cambia solo l'issuer. L'integrazione Cognito reale si valida in **test**.

### C. Setup script
8. **Set di shell script documentati** sotto `dev/` (stile wrapper #06 §25), `--help` ciascuno + **README quickstart**:
   **`dev setup`** (one-time, idempotente: prereq, mkcert+hosts, chiavi JWT, compose, db init+Flyway, seed, config),
   **`dev up`/`dev down`** (daily), **`dev seed`/`dev reset`/`dev migrate`**, **`dev service <app_id>`** (avvio selettivo).
9. **`dev doctor`** (preflight): diagnostica l'ambiente e dà fix **azionabili** (copia-incolla) prima del setup.
10. **README estremamente chiaro** (requisito): comandi copia-incolla, **output atteso**, stima tempi, **troubleshooting**
    (porte, mkcert, permessi `/etc/hosts`, Docker). Script **idempotenti e auto-riparanti**. Setup **senza intoppi**.
11. **Auto-wiring**: una app creata da `new-application` si aggancia da sola all'orchestrazione dev (servizio+schema+seed)
    → `dev up` la prende automaticamente.

### D. Seed data
12. Come **#10 I**: seed **deterministico/idempotente/versionato** (ID stabili), cast **multi-tenant** (Acme B2B
    owner/admin/member + Bob B2C single-user + platform-admin + app in vari stati), **dati sintetici** (no PII),
    **condiviso dev-locale ↔ E2E**, seed-base generato da `new-application`.

### E. Config locale
13. Come **#12**: `config.json` runtime puntato al locale, profili Quarkus `%dev`, domini `*.local.appgrove.app` via
    `/etc/hosts` + **mkcert**; la config locale la **scrive `dev setup`** (C).

### F. Parità & limiti
14. **Principio**: *parità sui **comportamenti applicativi** (logica/flussi/multi-tenancy/auth/dati/routing/email), NON
    sull'infrastruttura*. L'infra-specifico si valida in **test** (coerente con attivazione a fasi).
15. **Sostituiti/simulati in locale**: Cognito→provider locale (B); API GW→reverse proxy (A); SES→**Mailpit**;
    S3→**MinIO**; EventBridge/SQS→**ElasticMQ** (così **export job** #13 D e **purge per-tenant** #06 H **girano davvero**);
    Secrets/SSM→`.env`/config locale (mai segreti reali).
16. **NON in locale** (solo in test): **RDS Proxy** (Quarkus si connette diretto a Postgres), IAM, **CloudFront/WAF/ACM**,
    **CloudWatch** (log su stdout JSON, niente allarmi), **KMS/encryption-at-rest** (infra cloud).
17. **Niente LocalStack** (troppo pesante/generico): si usano strumenti **mirati e leggeri** (MinIO/ElasticMQ/Mailpit).
    LocalStack resta opzione futura se servisse emulare più servizi AWS insieme.

## Questioni aperte
_Nessuna — #11 chiuso._ Implementazione concreta degli script `dev/` + README → a [_BACKLOG](_BACKLOG.md) (con la skill `new-application`).

## Alternative valutate / scartate
- **Pool Cognito dev su AWS per il locale** (#12 originale) — scartato: in locale niente AWS, Cognito si emula.

## Impatti su altre aree
- [02-auth-sicurezza](02-auth-sicurezza.md), [06-infra-iac](06-infra-iac.md), [12-environments-config](12-environments-config.md),
  [10-testing](10-testing.md) (stack locale = base degli E2E F-a), [13-compliance-privacy](13-compliance-privacy.md) (MinIO/ElasticMQ per export/purge),
  [_BACKLOG.md](_BACKLOG.md) (script `dev/`, skill `new-application`)
