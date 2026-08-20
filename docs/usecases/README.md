# Catalogo Use Case — appgrove

Indice master degli use case implementativi (organizzato **per area**). Ogni use case è una specifica di dettaglio
(drill-down) e corrisponde ~1:1 a una `new-change`. Template: [_TEMPLATE.md](_TEMPLATE.md).

> **Per implementare seguendo l'ordine ottimale** (topologico su Fase + Dipendenze) e seguire lo **stato di
> implementazione**, usa [**_INDEX.md**](_INDEX.md) — tenuto in sync automaticamente da `new-change`.

## Convenzioni
- **Numerazione `NNNN` a 4 cifre, assoluta/globale**: ID **stabile**, assegnato seguendo **l'ordine delle cartelle area
  (`01` → `11`)** — la numerazione procede sequenziale da `0001` partendo dall'area `01-business-legal` fino a `11-apps`.
  **Non** riflette l'ordine di implementazione (che è dato da **Fase + Dipendenze**, vedi colonne). Nuovi use case si
  appendono col prossimo `NNNN` libero.
- **Cartelle per area** `XX-area/`; file `NNNN-slug.md`.
- **Ordine esecutivo** autorevole = **Fase + Dipendenze** (colonne sotto), non il numero.
- **Stato**: 🔴 da scrivere · 🟡 in corso · 🟢 scritto/deciso.
- Skill di gestione: **`new-usecase`** (crea/numerа/indicizza), **`new-change`** (implementa; folder
  `NNNN-use-case-YYYY-…` quando la change nasce da uno use case YYYY).
- Consolidamento: i 57 use case sotto consolidano i ~209 task atomici dell'inventario (ogni UC, nel suo file, elenca gli
  item che copre). Gli UC **0055–0057** sono stati aggiunti dopo una revisione di copertura requisiti→use case (gap infra
  condivisa, ri-accettazione legali runtime, skill `finalize-landing`).

## Fasi (ordine di implementazione)
0 Tooling & local dev · 1 Infra & CI/CD · 2 Core & Auth · 3 Vetrina + legale (prereq Paddle) · 4 Prima app + new-application ·
5 Pagamenti · 6 Compliance/GDPR runtime + admin · 7 Crescita

## Catalogo (per area, numerazione 01 → 11)

### 01-business-legal
| UC | Fase | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0001 | 3 | Setup business/legale (commercialista, P.IVA forfettaria, domiciliazione/entità, account Paddle + Domain Review) | 0002, 0036 | 🟢 |
| 0002 | 3 | Documenti legali reali 5 lingue (Privacy/ToS/Refund/Cookie, IT facente fede, md single-source, versioning + accettazione scoped) | — | 🟡 |

### 02-devops-infra
| UC | Fase | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0003 | 1 | Terraform foundation (state, global Route53/ACM/OIDC, VPC no-NAT, KMS/secrets baseline) | — | 🟢 |
| 0004 | 1 | Modulo `microsaas_app` + wrapper scripts (bootstrap/plan/up/down/service-add\|remove/test-start\|stop) | 0003, 0055 | 🟢 |
| 0005 | 1 | Pipeline CI/CD (OIDC, terraform, backend build/test+`[graal]`, frontend, Flyway one-shot, prod gate, path-filter, Infracost) | 0003, 0004 | 🟢 |
| 0006 | 1 | Observability baseline (log JSON+correlation, Micrometer/EMF, dashboard/alarm/SNS/Budgets, retention/archivio) | 0003 | 🟢 |
| 0007 | 7 | Observability hardening (canary eu-central-1 prod, tuning Budgets) | 0006 | 🟢 |
| 0055 | 1 | Risorse condivise per-env (Aurora SsV2+RDS Proxy/PITR, ECS cluster, API GW HTTP+VPC Link+Cloud Map, EventBridge bus, 2 CloudFront SPA) | 0003 | 🟢 |

### 03-local-dev
| UC | Fase | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0008 | 0 | Local dev stack (Compose: Postgres, reverse proxy, Mailpit, MinIO, ElasticMQ) | — | 🟢 |
| 0009 | 0 | Script `dev/` (setup, up/down, seed, reset, migrate, service, doctor) + README | 0008 | 🟢 |
| 0010 | 0 | Local auth provider — security-core (JWT/JWKS, claim dal DB, refresh cookie, fail-closed) | 0008, 0011, 0013 | 🟢 |
| 0011 | 0 | Seed data deterministico (condiviso dev↔E2E) | 0008 | 🟢 |
| 0058 | 0 | Flussi auth locali completi (signup/verifica, accept invito, reset password, 2FA TOTP, Mailpit) | 0010, 0013, 0011 | 🟢 |

### 04-platform-core
| UC | Fase | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0012 | 2 | Core service + multitenancy (Quarkus, TenantResolver, discriminator, schema `platform`, Flyway, audit/soft-delete) | 0004 | 🟢 |
| 0013 | 2 | Accounts/Users/Invitations + core REST API (problem+json, OpenAPI) | 0012 | 🟢 |
| 0014 | 2 | Authorizer all'edge (gate 1: token verificato prima del servizio; JWT nativo) | 0013, 0016 | 🟢 |
| 0056 | 3 | Ri-accettazione ToU/PP a runtime (derivazione al login + schermata bloccante + log accettazione) | 0002, 0013, 0020 | 🟡 |

### 05-auth
| UC | Fase | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0015 | 2 | Cognito + auth BFF (login/refresh/logout, HttpOnly cookie, CORS) | 0003, 0012 | 🟢 |
| 0016 | 2 | Pre-Token-Gen Lambda (claim tenant_id/roles) + JWT validation (Quarkus OIDC) | 0012, 0015 | 🟢 |
| 0017 | 2 | Flussi auth UI (signup/verify/login/reset/invite/2FA/onboarding) | 0015, 0016, 0020 | 🟢 |
| 0018 | 2 | Localizzazione email auth (Custom Message Lambda EN/IT) | 0015 | 🟢 |

### 06-frontend
| UC | Fase | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0019 | 0 | Design system & brand kit (token dai mockup, light/dark, Material Symbols, font) | — | 🟢 |
| 0020 | 2 | Backoffice SPA shell (sidebar, app registry, routing, auth store, API client, i18n, theme) | 0019, 0013 | 🟢 |
| 0021 | 6 | Admin console SPA (accounts, users, matrice entitlement, billing, danger zone, disable-app) | 0019, 0013 | 🟢 |
| 0059 | 2 | Gestione membri & inviti (UI backoffice: lista, invita, revoca, cambia ruolo) | 0020, 0013, 0017 | 🟡 |
| 0060 | 6 | Localizzazione UI app a 5 lingue (shell i18n en/it/fr/es/de + moduli via i18n) → sblocca screenshot landing per-lingua | 0020, 0052, 0037 | 🔴 |

### 07-payments
| UC | Fase | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0022 | 5 | Pricing-as-code + sync pipeline (test→sandbox, tag→prod) | 0005, 0013 | 🟢 |
| 0023 | 5 | Stub Paddle locale (port PaymentProvider, fake Paddle.js, webhook sintetici firmati, scenari lifecycle) | 0008, 0013 | 🟢 |
| 0024 | 5 | Checkout (token server-initiated, overlay, polling post-checkout) | 0023, 0020 | 🟢 |
| 0025 | 5 | Pipeline webhook (Lambda ingest HMAC+dedup → SQS → consumer idempotente, out-of-order) | 0013, 0023 | 🟢 |
| 0026 | 5 | Ciclo di vita subscription (stati, upgrade/downgrade, dunning/grace, trial, cancellazione) | 0025 | 🟢 |
| 0027 | 5 | Enforcement entitlement + quota SPI (flow/stock) runtime | 0014, 0026 | 🟢 |
| 0028 | 5 | Customer portal & gestione abbonamento self-service | 0026, 0020 | 🟢 |
| 0029 | 5 | Test pagamenti L1/L2/L3 | 0024, 0025 | 🟢 |

### 08-compliance-gdpr
| UC | Fase | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0030 | 6 | Manifesti dati per-app + RoPA automation (assembla IT+EN, check CI) | 0046 | 🟢 |
| 0031 | 6 | Gate privacy/RoPA in `new-change` (co-pilota classificazione + enforcement ArchUnit) | 0044, 0030 | 🟢 |
| 0032 | 6 | Framework export/erasure (contratto per-app, job async EventBridge/SQS, zip S3 presigned) | 0013, 0051 | 🟢 |
| 0033 | 6 | Self-service GDPR (export, rettifica, elimina account+grace, recedi-app, unsubscribe, consent center) | 0032, 0020 | 🟢 |
| 0034 | 6 | Console "Diritti GDPR" (admin single pane) | 0032, 0021 | 🟢 |
| 0035 | 6 | Job retention/purge (grace 14g, auto-delete inattività, archivio audit) | 0006, 0032 | 🟢 |

### 09-marketing-site
| UC | Fase | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0036 | 3 | Vetrina Astro skeleton (SSG, i18n subpath+hreflang, content md, S3+CloudFront static-first, test basic-auth+noindex) | 0019, 0003 | 🟢 |
| 0037 | 3 | Homepage + nav/footer + "Perché appgrove / Privacy & EU" | 0036 | 🟢 |
| 0038 | 3 | Template landing per-app + wiring `finalize-landing` | 0036 | 🟢 |
| 0039 | 3 | Newsletter subscribe + consent log + Plausible (cookieless) | 0036, 0013 | 🟢 |
| 0040 | 3 | SEO technicals (sitemap, Schema.org, meta/OG, hreflang) | 0036 | 🟢 |
| 0041 | 3 | GEO (`llms.txt`, crawler AI consentiti, entità canonica) | 0036 | 🟢 |
| 0042 | 7 | Blog/risorse (pillar-cluster, contenuti SEO/GEO) | 0036, 0040 | 🟢 |
| 0043 | 7 | Lancio paid/social (Product Hunt, directory, LinkedIn, Meta/Google cookieless) | 0037 | 🟢 |

### 10-skills-tooling
| UC | Fase | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0044 | 0 | `new-change` update (4 cifre + variante use-case + gate test/snapshot + hook privacy/RoPA) | — | 🟡 |
| 0045 | 0 | skill `new-usecase` (numerazione/scaffolding/indice) | — | 🟢 |
| 0046 | 4 | skill `new-application` (codifica il pattern: FE+BE+modulo+CI+manifest+landing draft+quota+contratto GDPR+test) | 0051, 0052 | 🟢 |
| 0047 | 5 | skill `pricing-change` | 0022 | 🟢 |
| 0048 | 6 | skill `drop-application` | 0004, 0046 | 🟢 |
| 0049 | 6 | skill `breach-response` + runbook/registro/`security.txt` | — | 🟢 |
| 0050 | 7 | skill `campaign-guide` | — | 🟢 |
| 0057 | 4 | skill `finalize-landing` (bozza → landing pubblicata: rifinitura 5 lingue + flag `published` + CI deploy) | 0038, 0046 | 🟡 |

### 11-apps
| UC | Fase | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0051 | 4 | App #1 (B2C single-user, es. fatture) backend (schema, quota SPI, contratto GDPR, logging) | 0012, 0014 | 🟢 |
| 0052 | 4 | App #1 frontend module (React lazy, manifest registry, UI) | 0020, 0051 | 🟢 |
| 0053 | 4 | App #1 landing (bozza → `finalize-landing`) | 0038, 0052 | 🟢 |
| 0054 | 4 | App #2 (B2B multi-user, es. mini-CRM) via `new-application` (valida skill + inviti/seat) | 0046 | 🟢 |

---

## Epiche evolutive (evo) — numerazione delle aree da `12`

> **Base ↔ evo.** Le aree `01`–`11` sopra sono l'**implementazione base** del prodotto. Le epiche qui sotto (`12`+)
> raccolgono il **lavoro evolutivo** formalizzato dal backlog nella change `0064` (dai residui R1–R21 di
> [_INDEX.md](_INDEX.md) e dai temi di [docs/_BACKLOG.md](../_BACKLOG.md)). Sono storie numerate con la **stessa
> convenzione globale** (use case a 4 cifre, che qui **continua da `0061`**), raggruppate per epica in cartelle-area
> numerate a partire da `12`. **Fase = evo**: **non ancora schedulate** nell'ordine di esecuzione topologico principale
> ([_INDEX.md](_INDEX.md)) — vi entrano quando maturano. **Stato drill-down**: 🟢 scritto (implementazione da avviare).
> **Ordine di esecuzione dell'onda 2** (topologico sulle dipendenze evo): [EPICS-WAVE-2.md](EPICS-WAVE-2.md).

### 12-ready-for-ai-mcp — Ready for AI (MCP) _(direzione di prodotto, nodi ancora da decidere)_
| UC | Titolo | Dipendenze | Stato |
|---|---|---|---|
| [0061](12-ready-for-ai-mcp/0061-architettura-server-mcp.md) | Architettura & collocazione del server MCP (per-app vs gateway centrale) | 0004, 0055, 0051, 0014 | 🟢 |
| [0062](12-ready-for-ai-mcp/0062-auth-consenso-delegato-ai.md) | Autenticazione e consenso delegato (assistente AI → tenant) | 0015, 0016, 0013, 0061 | 🟢 |
| [0063](12-ready-for-ai-mcp/0063-mappatura-operazioni-strumenti-mcp.md) | Mappatura operazioni app → strumenti MCP (contratto per-app) | 0061, 0051, 0046 | 🟢 |
| [0064](12-ready-for-ai-mcp/0064-enforcement-quota-entitlement-ai.md) | Enforcement entitlement/quota sulle chiamate AI | 0027, 0026, 0061 | 🟢 |
| [0065](12-ready-for-ai-mcp/0065-sicurezza-audit-invocazioni-ai.md) | Sicurezza & audit invocazioni AI + postura privacy | 0006, 0030, 0061 | 🟢 |
| [0066](12-ready-for-ai-mcp/0066-industrializzazione-mcp-newapp.md) | Industrializzazione in `new-application`/`microsaas_app` + riconciliazione claim sito | 0046, 0004, 0037, 0061–0065 | 🟢 |

### 13-abbonamenti-self-service — Abbonamenti self-service & leve billing
| UC | Titolo | Dipendenze | Stato |
|---|---|---|---|
| [0067](13-abbonamenti-self-service/0067-gestione-abbonamento-self-service.md) | Gestione abbonamento self-service (backoffice "Abbonamenti") | 0026, 0028, 0027, 0024, 0020 | 🟢 |
| [0068](13-abbonamenti-self-service/0068-pausa-ripresa-subscription.md) | Pausa/ripresa subscription self-service _(priorità bassissima)_ | 0026, 0028, 0020 | 🟢 |
| [0069](13-abbonamenti-self-service/0069-trial-una-tantum-tenant-app.md) | Trial una-tantum per tenant×app _(decisione di prodotto da confermare)_ | 0026, 0024, 0027 | 🟢 |
| [0070](13-abbonamenti-self-service/0070-bundling-abbonamento-multi-app.md) | Bundling: più app in un unico abbonamento _(priorità bassissima)_ | 0022, 0026 | 🟢 |
| [0071](13-abbonamenti-self-service/0071-riconciliazione-netto-revenue.md) | Riconciliazione netto/revenue | 0025, 0006, 0021 | 🟢 |

### 14-modello-utenti-multiapp — Modello utenti multi-app (B2B/B2C) _(🗄️ **SUPERATA dall'epica 22**: archivio, non da implementare)_

La direzione è stata decisa **in senso opposto**: l'epica **22** adotta l'appartenenza **centralizzata** con ruolo
per applicazione e posti di piattaforma, cioè l'opzione che questa epica registrava come scartata. Le tre storie
restano come **archivio della decisione precedente** — dicono cosa si era pensato e perché non si è fatto — e non
compaiono più nell'ordine di esecuzione di [EPICS-WAVE-2.md](EPICS-WAVE-2.md).

| UC | Titolo | Superata da | Stato |
|---|---|---|---|
| [0072](14-modello-utenti-multiapp/0072-distinzione-b2c-b2b-livello-app.md) | Distinzione B2C/B2B a livello app (`App.user_model`) + semantica gestione utenti | 0114 (la ritira) + 0115 (ambito dei dati) | 🗄️ |
| [0073](14-modello-utenti-multiapp/0073-invito-utenti-per-app-posti-quota.md) | Invito utenti per-app con "posti" come metrica quota `stock` | 0098 + 0102–0103 | 🗄️ |
| [0074](14-modello-utenti-multiapp/0074-directory-cross-app-ui-membri.md) | Directory cross-app + UI "Membri" ripensata per-app | 0100 + 0111 | 🗄️ |

### 15-supporto-e-piattaforma — Supporto & piattaforma
| UC | Titolo | Dipendenze | Stato |
|---|---|---|---|
| [0075](15-supporto-e-piattaforma/0075-ticketing-nativo-in-house.md) | Ticketing nativo in-house (`support_ticket` + UI backoffice + console admin) | 0012, 0013, 0020, 0021, 0018, 0034 | 🟢 |
| [0076](15-supporto-e-piattaforma/0076-disabilita-applicazione.md) | Disabilita applicazione (feature admin reversibile) | 0021, 0027, 0014, 0035 | 🟢 |
| [0077](15-supporto-e-piattaforma/0077-provider-entitlement-reale.md) | Provider entitlement reale del backoffice/admin (sostituire lo stub) | 0013, 0020, 0021, 0027, 0025, 0024 | 🟢 |

### 16-messa-in-cloud-golive — Messa in cloud & go-live operativo _(operazioni ☁, non codice)_
| UC | Titolo | Dipendenze | Stato |
|---|---|---|---|
| [0078](16-messa-in-cloud-golive/0078-uscita-ses-sandbox.md) | Uscita di SES dalla modalità di prova (sandbox) _(bloccante go-live)_ | 0018, 0079 | 🟢 |
| [0079](16-messa-in-cloud-golive/0079-gestione-rimbalzi-reclami-ses.md) | Gestione rimbalzi/reclami SES (notifiche, soppressione, allarme) | 0018, 0006 | 🟢 |
| [0080](16-messa-in-cloud-golive/0080-prima-esecuzione-live-pipeline.md) | Prima esecuzione live pipeline + configurazione repo GitHub | 0005, 0003, 0004, 0055 | 🟢 |
| [0081](16-messa-in-cloud-golive/0081-smoke-reali-cloud-test.md) | Smoke reali cloud alla prima accensione di `test` | 0015, 0016, 0014, 0018, 0055, 0005 | 🟢 |
| [0082](16-messa-in-cloud-golive/0082-script-attivazione-ambienti-fasi.md) | Script attivazione ambienti per fasi (`test-start`/`test-stop` + cron) | 0004, 0055, 0005, 0006 | 🟢 |
| [0083](16-messa-in-cloud-golive/0083-drift-regione-e-casella-security.md) | Correzione drift regione `eu-south-1`→`eu-west-1` + casella `security@` | 0005, 0037, 0049 | 🟢 |

### 17-skill-e-tooling-contenuto — Skill & tooling di contenuto/manutenzione
| UC | Titolo | Dipendenze | Stato |
|---|---|---|---|
| [0084](17-skill-e-tooling-contenuto/0084-skill-new-blog-post.md) | Skill `new-blog-post` (scaffold 5 lingue + registro + pilastro↔cluster + copy on-brand) | 0042, 0057, 0046, 0040, 0041 | 🟢 |
| [0085](17-skill-e-tooling-contenuto/0085-unificazione-renderer-email-commons.md) | Unificazione in `services/commons` dei renderer dei template email | 0018, 0039 | 🟢 |

### 18-brand-e-design-system — Brand & design system condiviso
| UC | Titolo | Dipendenze | Stato |
|---|---|---|---|
| [0086](18-brand-e-design-system/0086-brand-kit-token-condiviso.md) | Pacchetto brand kit / token condiviso (fonte unica SPA + vetrina + landing) | 0019, 0036, 0046 | 🟢 |
| [0087](18-brand-e-design-system/0087-artwork-logo-e-illustrazioni.md) | Artwork logo finale + stile illustrazioni on-brand | 0019, 0086, 0037 | 🟢 |

### 19-debito-tecnico — Debito tecnico & feature deprioritizzate
| UC | Titolo | Dipendenze | Stato |
|---|---|---|---|
| [0088](19-debito-tecnico/0088-search-globale-workspace.md) | Search globale dal workspace del backoffice _(deprioritizzata)_ | 0020, 0013 | 🟢 |
| [0089](19-debito-tecnico/0089-rimozione-legacy-peer-deps.md) | Rimozione `legacy-peer-deps` nel frontend | 0020, 0019 | 🟢 |

### 20-test-e2e-piattaforma — Test end-to-end di piattaforma (suite su stack reale + processo di copertura)
| UC | Titolo | Dipendenze | Stato |
|---|---|---|---|
| [0090](20-test-e2e-piattaforma/0090-e2e-platform-fondamenta.md) | Fondamenta suite e2e di piattaforma: stack reale + browser + Mailpit + helper + area `platform` in `run-tests.sh` + primo journey (registrazione con email vera) | 0058, 0018, 0020, 0023, 0029 | 🟢 |
| [0091](20-test-e2e-piattaforma/0091-e2e-platform-journey-utente.md) | Batteria journey lato utente: acquisto, uso app+quota, membri/inviti, ciclo abbonamento, password/2FA, privacy, gate legale | 0090, 0024, 0027, 0059, 0028, 0033, 0056 | 🟢 |
| [0092](20-test-e2e-piattaforma/0092-e2e-platform-journey-admin.md) | Batteria journey lato amministratore + guasti di piattaforma (disabilita app cross-tenant, console GDPR, entitlement illeggibili) | 0090, 0021, 0034, 0076, 0077 | 🟢 |
| [0093](20-test-e2e-piattaforma/0093-e2e-platform-registro-copertura.md) | Registro di copertura e2e leggibile da programma (UC ↔ journey ↔ test) + check meccanico in area `tooling` | 0090, 0091, 0092, 0045 | 🟢 |
| [0094](20-test-e2e-piattaforma/0094-e2e-platform-workflow-skill.md) | Integrazione nel workflow delle skill: `new-usecase`/`new-change`/`new-application` identificano e registrano i journey a ogni change | 0093, 0044, 0045, 0046 | 🟢 |

### 21-catalogo-app-backoffice — Catalogo app & UX backoffice (proposta approvata nella change `0066`)
| UC | Titolo | Dipendenze | Stato |
|---|---|---|---|
| [0095](21-catalogo-app-backoffice/0095-pagina-app-catalog.md) | Pagina "App catalog" (read-model catalogo per-tenant, card con stati, ricerca, paginazione → checkout) | 0020, 0022, 0024, 0026, 0077 | 🟢 |
| [0096](21-catalogo-app-backoffice/0096-billing-solo-fatturazione.md) | Billing solo-fatturazione (abbonamenti + storico pagamenti/ricevute; avviso app disabilitata) | 0095, 0028, 0025, 0026, 0077, 0076 | 🟢 |
| [0097](21-catalogo-app-backoffice/0097-dashboard-operativa.md) | Dashboard operativa del workspace (+ Workspace ID in Account) | 0095, 0027, 0028, 0059, 0056, 0077 | 🟢 |

### 22-refactor-membership-model — Rifacimento del modello di appartenenza (analisi della change `0087`)

**Struttura diversa dalle altre aree** (per volontà dello sviluppatore): sottocartelle
[epic/](22-refactor-membership-model/epic/), [story/](22-refactor-membership-model/story/),
[task/](22-refactor-membership-model/task/) e [prototype/](22-refactor-membership-model/prototype/README.md)
con cinque prototipi navigabili. **Le prime tre storie** (`0116`–`0118`, sotto-epica E22.5) sciolgono il
vincolo «una persona appartiene a un solo account», oggi imposto da indici unici globali su una tabella
interna all'account: aprono l'epica pur essendo state scritte per ultime, perché `0098` crea la tabella
degli accessi con un riferimento alla persona e cambiarne la forma dopo costringerebbe a rifare due
migrazioni. Altre due storie nascono dalla **riverifica della categorizzazione B2C/B2B** delle applicazioni:
`0114` la **ritira** (il nuovo modello la rende falsa) e `0115` mette al suo posto l'**ambito dei dati**
(del gruppo di lavoro contro della singola persona), che ha conseguenze verificabili nel codice. L'uso di
«B2C/B2B» in senso **giuridico** — titolare verso i consumatori, responsabile verso i clienti-azienda —
resta valido e non viene toccato. Indice dell'area e decisioni già prese:
[22-refactor-membership-model/README.md](22-refactor-membership-model/README.md).

**Supera l'epica 14** (`0072`–`0074`): l'appartenenza torna **centralizzata** (elenco unico di persone,
ruolo per applicazione, posti di piattaforma a listino unico), cioè l'opzione che l'epica 14 registrava
come scartata. Le tre storie dell'epica 14 restano come archivio della decisione precedente.

| UC | Titolo | Dipendenze | Stato |
|---|---|---|---|
| [0116](22-refactor-membership-model/story/0116-identita-e-appartenenze.md) | Identità della persona e appartenenze agli account: scioglie «una persona, un solo account» | 0013 | 🟢 |
| [0117](22-refactor-membership-model/story/0117-account-attivo-e-selettore.md) | Account attivo nella sessione, selettore e parità dei fornitori di identità | 0116, 0010, 0017 | 🟢 |
| [0118](22-refactor-membership-model/story/0118-inviti-e-registrazione-con-identita-esistente.md) | Inviti e registrazione quando l'identità esiste già (esiti indistinguibili) | 0116, 0117, 0058, 0059 | 🟢 |
| [0098](22-refactor-membership-model/story/0098-modello-dati-accesso-per-applicazione.md) | Modello dati dell'accesso per applicazione (`platform.app_access`) + ruolo di piattaforma a due valori | 0116, 0013, 0059 | 🟢 |
| [0099](22-refactor-membership-model/story/0099-autorizzazione-per-applicazione.md) | Autorizzazione per applicazione: claim del token, propagazione del ruolo, varco riusabile in `commons` | 0098, 0016, 0010, 0027 | 🟢 |
| [0100](22-refactor-membership-model/story/0100-sezione-members-elenco-unico.md) | «Members» come elenco unico di utenti, senza ruolo, invito riservato all'owner | 0098, 0099, 0059 | 🟢 |
| [0101](22-refactor-membership-model/story/0101-semantica-ruoli-viewer-editor-admin.md) | Semantica dei tre ruoli (viewer/editor/admin) come contratto di piattaforma verificabile | 0098, 0099 | 🟢 |
| [0102](22-refactor-membership-model/story/0102-listino-posti-a-fasce.md) | Listino dei posti a fasce: modello versionato e calcolo del dovuto | 0098, 0022 | 🟢 |
| [0103](22-refactor-membership-model/story/0103-acquisto-anticipato-posto-invito.md) | Acquisto anticipato del posto all'invito (abbonamento di piattaforma) | 0100, 0102, 0024, 0025, 0026 | 🟢 |
| [0104](22-refactor-membership-model/story/0104-riduzione-posti-in-attesa.md) | Riduzione dei posti in attesa: scelta delle persone, blocco, annullamento, scadenza | 0103, 0026, 0028 | 🟢 |
| [0105](22-refactor-membership-model/story/0105-governo-listino-console-piattaforma.md) | Governo del listino dei posti dalla console di piattaforma (dal ciclo successivo) | 0102, 0021, 0047 | 🟢 |
| [0106](22-refactor-membership-model/story/0106-posti-in-billing.md) | I posti nella sezione «Billing»: righe, storico, prossimo rinnovo | 0103, 0105, 0096, 0067 | 🟢 |
| [0107](22-refactor-membership-model/story/0107-menu-rotte-visibilita-per-ruolo.md) | Menu, rotte e visibilità per ruolo (intersezione a tre; Account/Billing/Members all'owner) | 0099, 0100, 0020, 0077 | 🟢 |
| [0108](22-refactor-membership-model/story/0108-cruscotto-collaboratore.md) | Cruscotto del collaboratore, senza azioni dispositive | 0107, 0097 | 🟢 |
| [0109](22-refactor-membership-model/story/0109-catalogo-sola-lettura-richiesta-owner.md) | Catalogo in sola lettura + richiesta «chiedi all'owner di installare» (email) | 0107, 0095, 0018, 0085 | 🟢 |
| [0110](22-refactor-membership-model/story/0110-miei-dati-forma-ridotta.md) | «I miei dati» in forma ridotta per il collaboratore (diritti della persona) | 0107, 0033 | 🟢 |
| [0111](22-refactor-membership-model/story/0111-schermata-gestione-utenti-app.md) | Schermata «Gestione utenti» dentro ogni applicazione (+ ritiro dei posti del Mini-CRM) | 0098, 0099, 0101, 0107 | 🟢 |
| [0112](22-refactor-membership-model/story/0112-copilota-ruoli-new-application.md) | Copilota dei ruoli nella skill `new-application` + parità dello scaffolding | 0101, 0099, 0111, 0046 | 🟢 |
| [0114](22-refactor-membership-model/story/0114-ritiro-categoria-b2c-b2b.md) | Ritiro della categoria B2C/B2B delle app (`App.user_model`): il nuovo modello la rende falsa | 0099, 0101, 0112 | 🟢 |
| [0115](22-refactor-membership-model/story/0115-ambito-dati-applicazione.md) | Ambito dei dati di un'app: del gruppo di lavoro o della persona che li ha creati (dichiarazione + guardia; il filtro va con la prima app che lo usa) | 0114, 0101, 0032 | 🟢 |
| [0113](22-refactor-membership-model/story/0113-migrazione-account-e-copertura-e2e.md) | Migrazione degli account esistenti + copertura end-to-end per ruolo | 0098–0112, 0114, 0115, 0090, 0093 | 🟢 |

---
**Numerazione**: segue l'ordine delle aree (`01` → `11`) per `0001`–`0054`; **0055–0060** appesi col prossimo `NNNN`
libero (ID stabili, non riflettono l'area). Le **epiche evolutive** (aree `12`+) continuano la stessa sequenza globale da
**`0061`** (change `0064`). **Ordine di implementazione**: dato dalla colonna **Fase** + **Dipendenze** (non dal numero;
le evo restano fuori dall'ordine topologico finché non maturano).
**Stato**: i drill-down sono **scritti** (🟢) tranne **0044** (`new-change`: hook privacy/RoPA + snapshot da wire-are in UC 0031),
**0002** (documenti legali: impianto deciso, testi ancora da redigere) e i nuovi **0056/0057** (🟡, drill-down scritto,
implementazione da avviare). Implementazione successiva: una `new-change` per use case (folder `NNNN-use-case-YYYY-…`).
