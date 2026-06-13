# Recap — Marketplace micro-SaaS su AWS

Sto costruendo una piattaforma marketplace di micro-SaaS su AWS. Riprendo il progetto da dove l'avevo lasciato. Ecco tutto il contesto necessario.

---

## Concept del prodotto

Un **backoffice shell** multi-tenant in cui l'utente si autentica una volta e attiva le mini-applicazioni che vuole. Ogni app appare come nuova voce di menù nella sidebar del backoffice e si apre nell'area centrale. Il marketplace ha anche una vetrina pubblica con tutte le app disponibili.

L'obiettivo immediato è un **PoC** con due app demo per validare il wiring end-to-end: auth → multi-tenancy → microfrontend → microservice → DB.

---

## Stack tecnologico (decisioni bloccate)

**Frontend**
- Shell backoffice: **React SPA**
- Micro-applicazioni: **Lit Web Components** (microfrontend), caricati dinamicamente via DOM custom events / dynamic import
- Hosting: **S3 + CloudFront**

**Backend**
- Un microservice **Quarkus** (GraalVM native) per ogni app
- API REST, con Cognito authorizer su ogni endpoint
- Deployment: **ECS Fargate**
- Routing: **API Gateway v2**

**Auth & Multi-tenancy**
- **AWS Cognito** — OAuth2/OIDC, JWT
- `tenant_id` = `sub` Cognito (UUID v4) estratto **esclusivamente dal JWT verificato**, mai da parametri request o body
- Pattern Quarkus:
  ```java
  @Inject JsonWebToken jwt;
  String tenantId = jwt.getClaim("sub"); // mai dal request body
  ```

**Persistenza**
- Libertà di scelta DBMS per ogni microservice
- Per il PoC: **Aurora Serverless v2 PostgreSQL**, istanza condivisa con **schema separato per app** (`schema: app_notes`, `schema: app_dashboard`, ecc.) — ottimizzazione costi iniziale, path di migrazione chiaro verso istanze dedicate
- Row-level filter `WHERE tenant_id = :tid` obbligatorio in ogni query

**Infrastruttura**
- **AWS CDK (TypeScript)** con construct riusabile `MicroSaasApp`
- Aggiungere una nuova app = instanziare il construct con nome, porta, schema DB
- **CloudWatch** per logging centralizzato (structured logging con MDC in Quarkus: ogni log porta `tenant_id`, `app_name`)
- **GitHub Actions** per CI/CD → ECR → ECS

**Admin panel**
- Stessa shell React, Cognito group `admin`
- Visibilità su tutti gli utenti, app attive, stato pagamenti
- Possibilità di attivare/disattivare servizi per tenant

---

## Roadmap PoC (4 fasi)

| Fase | Contenuto | Stima |
|---|---|---|
| 1 | Infrastruttura base: VPC, Cognito, CloudFront+S3, API GW, ECR, RDS, CDK construct `MicroSaasApp` | ~1 settimana |
| 2 | Backoffice shell React: vetrina pubblica, login OAuth, sidebar dinamica da App Registry, caricamento microfrontend | 3–4 giorni |
| 3 | Due app demo: App1 (note-taking CRUD per tenant), App2 (mini-dashboard con metriche), validazione multi-tenancy end-to-end | ~1 settimana |
| 4 | Admin dashboard, structured logging frontend+backend, CloudWatch MDC | 2–3 giorni |

---

## Dominio del marketplace

TLD scelto: **`.app`**

Nomi in valutazione (disponibilità da verificare su instantdomainsearch.com):
- `appnest.app`
- `apptiles.app`
- `nestly.app`
- `appforge.app`
- `appcraft.app`
- `appblocks.app`
- `saaslet.app`
- `appshelf.app`

Nessun nome finale scelto ancora.

---

## Pagamenti

- **Paddle** come Merchant of Record (nessuna gestione fiscale/IVA diretta)
- Ogni app ha il suo modello di costo
- RemindPinoy (progetto precedente, filippine) in pausa — GCash non supportato da Paddle

---

## Principi architetturali chiave

1. **Tenant ID solo da JWT** — vincolo di sicurezza non negoziabile
2. **Persistenza cost-first** — RDS condiviso con schema-per-app è il punto di partenza legittimo
3. **Construct pattern** — `MicroSaasApp` CDK è il building block di tutto; scalare = instanziare
4. **Isolation incrementale** — dall'istanza condivisa all'istanza dedicata è una modifica CDK, non un refactor applicativo
5. **Logging strutturato ovunque** — ogni log deve portare `tenant_id`, `app_name`, `user_id`

---

## Stato attuale

- Architettura definita e validata ✅
- TLD `.app` scelto per il dominio, nome in valutazione ✅
- Implementazione non ancora iniziata — nessun codice scritto
- Prossimo passo da decidere: da dove si inizia (CDK infra, shell React, o primo microservice Quarkus)
