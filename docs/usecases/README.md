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
| 0055 | 1 | Risorse condivise per-env (Aurora SsV2+RDS Proxy/PITR, ECS cluster, API GW HTTP+VPC Link+Cloud Map, EventBridge bus, 2 CloudFront SPA) | 0003 | 🟡 |

### 03-local-dev
| UC | Fase | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0008 | 0 | Local dev stack (Compose: Postgres, reverse proxy, Mailpit, MinIO, ElasticMQ) | — | 🟢 |
| 0009 | 0 | Script `dev/` (setup, up/down, seed, reset, migrate, service, doctor) + README | 0008 | 🟢 |
| 0010 | 0 | Local auth provider (JWT/JWKS, claim dal DB, refresh cookie, TOTP, Mailpit) | 0008 | 🟢 |
| 0011 | 0 | Seed data deterministico (condiviso dev↔E2E) | 0008 | 🟢 |

### 04-platform-core
| UC | Fase | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0012 | 2 | Core service + multitenancy (Quarkus, TenantResolver, discriminator, schema `platform`, Flyway, audit/soft-delete) | 0004 | 🟢 |
| 0013 | 2 | Accounts/Users/Invitations + core REST API (problem+json, OpenAPI) | 0012 | 🟢 |
| 0014 | 2 | Custom Lambda authorizer (app-abilitata + entitlement grossolano derivato; catena gate) | 0013, 0016 | 🟢 |
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
**Numerazione**: segue l'ordine delle aree (`01` → `11`) per `0001`–`0054`; **0055–0057** sono appesi col prossimo `NNNN` libero
(ID stabili, non riflettono l'area). **Ordine di implementazione**: dato dalla colonna **Fase** + **Dipendenze** (non dal numero).
**Stato**: i drill-down sono **scritti** (🟢) tranne **0044** (`new-change`: hook privacy/RoPA + snapshot da wire-are in UC 0031),
**0002** (documenti legali: impianto deciso, testi ancora da redigere) e i nuovi **0055/0056/0057** (🟡, drill-down scritto,
implementazione da avviare). Implementazione successiva: una `new-change` per use case (folder `NNNN-use-case-YYYY-…`).
