# Catalogo Use Case — appgrove

Indice master degli use case implementativi. Ogni use case è una specifica di dettaglio (drill-down) e corrisponde
~1:1 a una `new-change`. Genesi e regole in [piano approvato](../../). Template: [_TEMPLATE.md](_TEMPLATE.md).

## Convenzioni
- **Numerazione `NNNN` a 4 cifre, assoluta/globale**: ID **stabile**, assegnato in ordine di catalogo (≈ ordine di
  implementazione). **Non** relativa alla sottocartella. Nuovi use case si appendono col prossimo `NNNN`.
- **Cartelle per area** `XX-area/`; file `NNNN-slug.md`. L'**ordine esecutivo** autorevole è **Fase + Dipendenze** in questa
  tabella, non il numero.
- **Stato**: 🔴 da scrivere · 🟡 in corso · 🟢 scritto/deciso.
- Skill di gestione: **`new-usecase`** (crea/numerа/indicizza), **`new-change`** (implementa; folder
  `NNNN-use-case-YYYY-…` quando la change nasce da uno use case YYYY).
- Consolidamento: i ~54 use case sotto consolidano i ~209 task atomici dell'inventario (ogni UC, nel suo file, elenca gli
  item che copre).

## Aree
`00-business-legal` · `01-devops-infra` · `02-local-dev` · `03-platform-core` · `04-auth` · `05-frontend` ·
`06-payments` · `07-compliance-gdpr` · `08-marketing-site` · `09-skills-tooling` · `10-apps`

## Fasi (ordine di implementazione)
0 Tooling & local dev · 1 Infra & CI/CD · 2 Core & Auth · 3 Vetrina + legale (prereq Paddle) · 4 Prima app + new-application ·
5 Pagamenti · 6 Compliance/GDPR runtime + admin · 7 Crescita

## Catalogo

### Fase 0 — Tooling & local dev
| UC | Area | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0001 | 09 | `new-change` update (4 cifre + variante use-case + gate test/snapshot + hook privacy/RoPA) | — | 🟡 |
| 0002 | 09 | skill `new-usecase` (numerazione/scaffolding/indice) | — | 🟡 |
| 0003 | 02 | Local dev stack (Compose: Postgres, reverse proxy, Mailpit, MinIO, ElasticMQ) | — | 🔴 |
| 0004 | 02 | Script `dev/` (setup, up/down, seed, reset, migrate, service, doctor) + README | 0003 | 🔴 |
| 0005 | 02 | Local auth provider (JWT/JWKS, claim dal DB, refresh cookie, TOTP, Mailpit) | 0003 | 🔴 |
| 0006 | 02 | Seed data deterministico (condiviso dev↔E2E) | 0003 | 🔴 |
| 0007 | 05 | Design system & brand kit (token dai mockup, light/dark, Material Symbols, font) | — | 🔴 |

### Fase 1 — Infra foundation & CI/CD
| UC | Area | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0008 | 01 | Terraform foundation (state, global Route53/ACM/OIDC, VPC no-NAT, KMS/secrets baseline) | — | 🔴 |
| 0009 | 01 | Modulo `microsaas_app` + wrapper scripts (bootstrap/plan/up/down/service-add\|remove/test-start\|stop) | 0008 | 🔴 |
| 0010 | 01 | Pipeline CI/CD (OIDC, terraform, backend build/test+`[graal]`, frontend, Flyway one-shot, prod gate, path-filter, Infracost) | 0008, 0009 | 🔴 |
| 0011 | 01 | Observability baseline (log JSON+correlation, Micrometer/EMF, dashboard/alarm/SNS/Budgets, retention/archivio) | 0008 | 🔴 |

### Fase 2 — Core & Auth
| UC | Area | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0012 | 03 | Core service + multitenancy (Quarkus, TenantResolver, discriminator, schema `platform`, Flyway, audit/soft-delete) | 0009 | 🔴 |
| 0013 | 03 | Accounts/Users/Invitations + core REST API (problem+json, OpenAPI) | 0012 | 🔴 |
| 0014 | 04 | Cognito + auth BFF (login/refresh/logout, HttpOnly cookie, CORS) | 0008, 0012 | 🔴 |
| 0015 | 04 | Pre-Token-Gen Lambda (claim tenant_id/roles) + JWT validation (Quarkus OIDC) | 0012, 0014 | 🔴 |
| 0016 | 03 | Custom Lambda authorizer (app-abilitata + entitlement grossolano derivato; catena gate) | 0013, 0015 | 🔴 |
| 0017 | 04 | Flussi auth UI (signup/verify/login/reset/invite/2FA/onboarding) | 0014, 0015, 0019 | 🟢 |
| 0018 | 04 | Localizzazione email auth (Custom Message Lambda EN/IT) | 0014 | 🔴 |
| 0019 | 05 | Backoffice SPA shell (sidebar, app registry, routing, auth store, API client, i18n, theme) | 0007, 0013 | 🔴 |

### Fase 3 — Sito vetrina + legale (prereq Paddle #14)
| UC | Area | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0020 | 00 | Setup business/legale (commercialista, P.IVA forfettaria, domiciliazione/entità, account Paddle + Domain Review) | 0021, 0022 | 🔴 |
| 0021 | 00 | Documenti legali reali 5 lingue (Privacy/ToS/Refund/Cookie, IT facente fede, md single-source, versioning + accettazione scoped) | — | 🔴 |
| 0022 | 08 | Vetrina Astro skeleton (SSG, i18n subpath+hreflang, content md, S3+CloudFront static-first, test basic-auth+noindex) | 0007, 0008 | 🔴 |
| 0023 | 08 | Homepage + nav/footer + "Perché appgrove / Privacy & EU" | 0022 | 🔴 |
| 0024 | 08 | Template landing per-app + wiring `finalize-landing` | 0022 | 🔴 |
| 0025 | 08 | Newsletter subscribe + consent log + Plausible (cookieless) | 0022, 0013 | 🔴 |
| 0026 | 08 | SEO technicals (sitemap, Schema.org, meta/OG, hreflang) | 0022 | 🔴 |
| 0027 | 08 | GEO (`llms.txt`, crawler AI consentiti, entità canonica) | 0022 | 🔴 |

### Fase 4 — Prima app + `new-application`
| UC | Area | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0028 | 10 | App #1 (B2C single-user, es. fatture) backend (schema, quota SPI, contratto GDPR, logging) | 0012, 0016 | 🔴 |
| 0029 | 10 | App #1 frontend module (React lazy, manifest registry, UI) | 0019, 0028 | 🔴 |
| 0030 | 10 | App #1 landing (bozza → `finalize-landing`) | 0024, 0029 | 🔴 |
| 0031 | 09 | skill `new-application` (codifica il pattern: FE+BE+modulo+CI+manifest+landing draft+quota+contratto GDPR+test) | 0028, 0029 | 🔴 |
| 0032 | 10 | App #2 (B2B multi-user, es. mini-CRM) via `new-application` (valida skill + inviti/seat) | 0031 | 🔴 |

### Fase 5 — Pagamenti (Paddle)
| UC | Area | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0033 | 06 | Pricing-as-code + sync pipeline (test→sandbox, tag→prod) | 0010, 0013 | 🔴 |
| 0034 | 06 | Stub Paddle locale (port PaymentProvider, fake Paddle.js, webhook sintetici firmati, scenari lifecycle) | 0003, 0013 | 🔴 |
| 0035 | 06 | Checkout (token server-initiated, overlay, polling post-checkout) | 0034, 0019 | 🔴 |
| 0036 | 06 | Pipeline webhook (Lambda ingest HMAC+dedup → SQS → consumer idempotente, out-of-order) | 0013, 0034 | 🔴 |
| 0037 | 06 | Ciclo di vita subscription (stati, upgrade/downgrade, dunning/grace, trial, cancellazione) | 0036 | 🔴 |
| 0038 | 06 | Enforcement entitlement + quota SPI (flow/stock) runtime | 0016, 0037 | 🔴 |
| 0039 | 06 | Customer portal & gestione abbonamento self-service | 0037, 0019 | 🔴 |
| 0040 | 06 | Test pagamenti L1/L2/L3 | 0035, 0036 | 🔴 |
| 0041 | 09 | skill `pricing-change` | 0033 | 🔴 |

### Fase 6 — Compliance/GDPR runtime + admin
| UC | Area | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0042 | 07 | Manifesti dati per-app + RoPA automation (assembla IT+EN, check CI) | 0031 | 🔴 |
| 0043 | 07 | Gate privacy/RoPA in `new-change` (co-pilota classificazione + enforcement ArchUnit) | 0001, 0042 | 🔴 |
| 0044 | 07 | Framework export/erasure (contratto per-app, job async EventBridge/SQS, zip S3 presigned) | 0013, 0028 | 🔴 |
| 0045 | 07 | Self-service GDPR (export, rettifica, elimina account+grace, recedi-app, unsubscribe, consent center) | 0044, 0019 | 🔴 |
| 0046 | 07 | Console "Diritti GDPR" (admin single pane) | 0044, 0048 | 🔴 |
| 0047 | 07 | Job retention/purge (grace 14g, auto-delete inattività, archivio audit) | 0011, 0044 | 🔴 |
| 0048 | 05 | Admin console SPA (accounts, users, matrice entitlement, billing, danger zone, disable-app) | 0007, 0013 | 🔴 |
| 0049 | 09 | skill `drop-application` | 0009, 0031 | 🔴 |
| 0050 | 09 | skill `breach-response` + runbook/registro/`security.txt` | — | 🔴 |

### Fase 7 — Crescita
| UC | Area | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 0051 | 08 | Blog/risorse (pillar-cluster, contenuti SEO/GEO) | 0022, 0026 | 🔴 |
| 0052 | 09 | skill `campaign-guide` | — | 🔴 |
| 0053 | 08 | Lancio paid/social (Product Hunt, directory, LinkedIn, Meta/Google cookieless) | 0023 | 🔴 |
| 0054 | 01 | Observability hardening (canary eu-central-1 prod, tuning Budgets) | 0011 | 🔴 |

---
**Prossimo passo**: scrivere i drill-down in ordine di fase (a batch rivedibili), usando `new-usecase` per lo scaffold.
Solo **0017** è già scritto (migrato).
