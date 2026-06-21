# Indice di esecuzione — Use Case appgrove

**Ordine ottimale di implementazione** dei 57 use case, ottenuto da un ordinamento topologico su **Fase (0→7) + Dipendenze**
(non per area: per quello vedi [README.md](README.md)). Si implementa **dall'alto verso il basso**: ogni UC ha tutte le sue
dipendenze sopra di sé.

> **Due indici, due assi.** [README.md](README.md) = catalogo **per area** con stato del **drill-down** (🔴 da scrivere /
> 🟡 in corso / 🟢 scritto). **Questo file** = ordine **esecutivo** con stato di **implementazione** (sotto). Sono ortogonali:
> uno dice "la spec è pronta?", l'altro "il codice è in main?".

## Legenda stato implementazione
- ⬜ **da implementare** — nessuna change avviata
- 🟡 **in corso** — esiste una `change/NNNN-use-case-YYYY-…` aperta (non ancora in main)
- ✅ **implementato** — change mergiata in main

## 👉 Prossimo da implementare: **UC 0044** (update `new-change`)

> **Manutenzione automatica.** La skill `new-change`, quando una change implementa uno use case (forma
> `NNNN-use-case-YYYY-…`), aggiorna **da sola** questo indice: `YYYY` → 🟡 all'avvio (step-01) e → ✅ alla chiusura/merge
> (step-04). Tenere coerente l'ordine se si aggiungono nuovi UC (ri-eseguire l'ordinamento Fase+Dipendenze).

---

## Fase 0 — Tooling & local dev
| # | UC | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 1 | [0044](10-skills-tooling/0044-aggiornamento-skill-new-change.md) | `new-change` update (use-case variant + gate test/snapshot + hook privacy/RoPA) | — | ✅ |
| 2 | [0045](10-skills-tooling/0045-skill-new-usecase.md) | skill `new-usecase` (numerazione/scaffolding/indice) | — | ✅ |
| 3 | [0008](03-local-dev/0008-stack-sviluppo-locale.md) | Stack dev locale (Compose: Postgres, proxy, Mailpit, MinIO, ElasticMQ) | — | ⬜ |
| 4 | [0009](03-local-dev/0009-script-dev.md) | Script `dev/` (setup, up/down, seed, reset, migrate, service, doctor) | 0008 | ⬜ |
| 5 | [0010](03-local-dev/0010-provider-auth-locale.md) | Auth provider locale (JWT/JWKS, claim dal DB, refresh, TOTP, Mailpit) | 0008 | ⬜ |
| 6 | [0011](03-local-dev/0011-dati-seed.md) | Seed data deterministico (dev↔E2E) | 0008 | ⬜ |
| 7 | [0019](06-frontend/0019-design-system-brand-kit.md) | Design system & brand kit (token, light/dark, font, icone) | — | ⬜ |

## Fase 1 — Infra & CI/CD
| # | UC | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 8 | [0003](02-devops-infra/0003-fondamenta-terraform.md) | Terraform foundation (state, Route53/ACM/OIDC, VPC no-NAT, KMS/secrets) | — | ⬜ |
| 9 | [0055](02-devops-infra/0055-risorse-condivise-per-ambiente.md) | Risorse condivise per-env (Aurora SsV2+RDS Proxy, ECS cluster, API GW+VPC Link, EventBridge, CloudFront SPA) | 0003 | ⬜ |
| 10 | [0004](02-devops-infra/0004-modulo-microsaas-app.md) | Modulo `microsaas_app` + wrapper scripts | 0003, 0055 | ⬜ |
| 11 | [0005](02-devops-infra/0005-pipeline-cicd.md) | Pipeline CI/CD (OIDC, terraform, build/test, Flyway one-shot, prod gate) | 0003, 0004 | ⬜ |
| 12 | [0006](02-devops-infra/0006-osservabilita-base.md) | Observability baseline (log JSON, Micrometer/EMF, dashboard/alarm/Budgets) | 0003, 0004 | ⬜ |

## Fase 2 — Core & Auth
| # | UC | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 13 | [0012](04-platform-core/0012-servizio-core-multitenancy.md) | Core service + multitenancy (Quarkus, TenantResolver, discriminator, Flyway) | 0004 | ⬜ |
| 14 | [0013](04-platform-core/0013-account-utenti-inviti-api.md) | Accounts/Users/Invitations + core REST API (problem+json, OpenAPI) | 0012 | ⬜ |
| 15 | [0015](05-auth/0015-cognito-auth-bff.md) | Cognito + auth BFF (login/refresh/logout, cookie HttpOnly, CORS) | 0003, 0012 | ⬜ |
| 16 | [0016](05-auth/0016-pre-token-gen-jwt.md) | Pre-Token-Gen Lambda (claim tenant_id/roles) + JWT validation | 0012, 0015 | ⬜ |
| 17 | [0014](04-platform-core/0014-authorizer-custom.md) | Custom Lambda authorizer (app-abilitata + entitlement derivato; catena gate) | 0013, 0016 | ⬜ |
| 18 | [0020](06-frontend/0020-shell-spa-backoffice.md) | Backoffice SPA shell (sidebar, app registry, routing, auth store, i18n, theme) | 0019, 0013 | ⬜ |
| 19 | [0017](05-auth/0017-flussi-auth.md) | Flussi auth UI (signup/verify/login/reset/invite/2FA/onboarding) | 0015, 0016, 0020 | ⬜ |
| 20 | [0018](05-auth/0018-localizzazione-email-auth.md) | Localizzazione email auth (Custom Message Lambda EN/IT) | 0015 | ⬜ |

## Fase 3 — Vetrina + legale (prereq Paddle)
| # | UC | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 21 | [0002](01-business-legal/0002-documenti-legali-multilingua.md) | Documenti legali reali 5 lingue (PP/ToS/Refund/Cookie/subprocessors) | — | ⬜ |
| 22 | [0036](09-marketing-site/0036-vetrina-astro-scheletro.md) | Vetrina Astro skeleton (SSG, i18n, S3+CloudFront static-first) | 0019, 0003 | ⬜ |
| 23 | [0037](09-marketing-site/0037-homepage-nav-footer.md) | Homepage + nav/footer + "Perché appgrove / Privacy & EU" | 0036 | ⬜ |
| 24 | [0038](09-marketing-site/0038-template-landing-per-app.md) | Template landing per-app + wiring `finalize-landing` | 0036 | ⬜ |
| 25 | [0039](09-marketing-site/0039-newsletter-consenso-plausible.md) | Newsletter subscribe + consent log + Plausible (cookieless) | 0036, 0013 | ⬜ |
| 26 | [0040](09-marketing-site/0040-seo-tecnico.md) | SEO technicals (sitemap, Schema.org, meta/OG, hreflang) | 0036 | ⬜ |
| 27 | [0041](09-marketing-site/0041-geo-llms.md) | GEO (`llms.txt`, crawler AI, entità canonica) | 0036 | ⬜ |
| 28 | [0056](04-platform-core/0056-riaccettazione-legali-runtime.md) | Ri-accettazione ToU/PP a runtime (derivazione login + schermata bloccante + log) | 0002, 0013, 0020 | ⬜ |
| 29 | [0001](01-business-legal/0001-setup-business-legale.md) | Setup business/legale (P.IVA, domiciliazione, account Paddle + Domain Review) | 0002, 0036 | ⬜ |

## Fase 4 — Prima app + new-application
| # | UC | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 30 | [0051](11-apps/0051-app1-backend.md) | App #1 (B2C single-user) backend (schema, quota SPI, contratto GDPR, logging) | 0012, 0014 | ⬜ |
| 31 | [0052](11-apps/0052-app1-modulo-frontend.md) | App #1 frontend module (React lazy, manifest registry, UI) | 0020, 0051 | ⬜ |
| 32 | [0046](10-skills-tooling/0046-skill-new-application.md) | skill `new-application` (FE+BE+modulo+CI+manifest+landing+quota+GDPR+test) | 0051, 0052 | ⬜ |
| 33 | [0053](11-apps/0053-app1-landing.md) | App #1 landing (bozza → `finalize-landing`) | 0038, 0052 | ⬜ |
| 34 | [0054](11-apps/0054-app2-b2b-via-new-application.md) | App #2 (B2B multi-user) via `new-application` (valida skill + inviti/seat) | 0046 | ⬜ |
| 35 | [0057](10-skills-tooling/0057-skill-finalize-landing.md) | skill `finalize-landing` (bozza → published + CI deploy) | 0038, 0046 | ⬜ |

## Fase 5 — Pagamenti
| # | UC | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 36 | [0022](07-payments/0022-pricing-as-code-sincronizzazione.md) | Pricing-as-code + sync pipeline (test→sandbox, tag→prod) | 0005, 0013 | ⬜ |
| 37 | [0023](07-payments/0023-stub-paddle-locale.md) | Stub Paddle locale (port PaymentProvider, fake Paddle.js, webhook sintetici) | 0008, 0013 | ⬜ |
| 38 | [0024](07-payments/0024-checkout.md) | Checkout (token server-initiated, overlay, polling post-checkout) | 0023, 0020 | ⬜ |
| 39 | [0025](07-payments/0025-pipeline-webhook.md) | Pipeline webhook (Lambda ingest HMAC+dedup → SQS → consumer idempotente) | 0013, 0023 | ⬜ |
| 40 | [0026](07-payments/0026-ciclo-vita-abbonamento.md) | Ciclo di vita subscription (stati, upgrade/downgrade, dunning/grace, trial) | 0025 | ⬜ |
| 41 | [0027](07-payments/0027-applicazione-entitlement-quota.md) | Enforcement entitlement + quota SPI (flow/stock) runtime | 0014, 0026 | ⬜ |
| 42 | [0028](07-payments/0028-portale-cliente-self-service.md) | Customer portal & gestione abbonamento self-service | 0026, 0020 | ⬜ |
| 43 | [0029](07-payments/0029-test-pagamenti-l1-l2-l3.md) | Test pagamenti L1/L2/L3 | 0024, 0025 | ⬜ |
| 44 | [0047](10-skills-tooling/0047-skill-pricing-change.md) | skill `pricing-change` | 0022 | ⬜ |

## Fase 6 — Compliance/GDPR runtime + admin
| # | UC | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 45 | [0021](06-frontend/0021-console-admin-spa.md) | Admin console SPA (accounts, users, matrice entitlement, billing, danger zone) | 0019, 0013 | ⬜ |
| 46 | [0030](08-compliance-gdpr/0030-manifesti-dati-ropa.md) | Manifesti dati per-app + RoPA automation (assembla IT+EN, check CI) | 0046 | ⬜ |
| 47 | [0031](08-compliance-gdpr/0031-gate-privacy-ropa-new-change.md) | Gate privacy/RoPA in `new-change` (classificazione + enforcement ArchUnit) | 0044, 0030 | ⬜ |
| 48 | [0032](08-compliance-gdpr/0032-framework-esportazione-cancellazione.md) | Framework export/erasure (contratto per-app, job async EventBridge/SQS, zip S3) | 0013, 0051 | ⬜ |
| 49 | [0033](08-compliance-gdpr/0033-self-service-gdpr.md) | Self-service GDPR (export, rettifica, elimina account+grace, consent center) | 0032, 0020 | ⬜ |
| 50 | [0034](08-compliance-gdpr/0034-console-diritti-gdpr.md) | Console "Diritti GDPR" (admin single pane) | 0032, 0021 | ⬜ |
| 51 | [0035](08-compliance-gdpr/0035-job-conservazione-purga.md) | Job retention/purge (grace 14g, auto-delete inattività, archivio audit) | 0006, 0032 | ⬜ |
| 52 | [0048](10-skills-tooling/0048-skill-drop-application.md) | skill `drop-application` | 0004, 0046 | ⬜ |
| 53 | [0049](10-skills-tooling/0049-skill-breach-response.md) | skill `breach-response` + runbook/registro/`security.txt` | — | ⬜ |

## Fase 7 — Crescita
| # | UC | Titolo | Dipendenze | Stato |
|---|---|---|---|---|
| 54 | [0007](02-devops-infra/0007-osservabilita-irrobustimento.md) | Observability hardening (canary eu-central-1 prod, tuning Budgets) | 0006 | ⬜ |
| 55 | [0042](09-marketing-site/0042-blog-risorse.md) | Blog/risorse (pillar-cluster, contenuti SEO/GEO) | 0036, 0040 | ⬜ |
| 56 | [0043](09-marketing-site/0043-lancio-paid-social.md) | Lancio paid/social (Product Hunt, directory, LinkedIn, Meta/Google cookieless) | 0037 | ⬜ |
| 57 | [0050](10-skills-tooling/0050-skill-campaign-guide.md) | skill `campaign-guide` | — | ⬜ |

---
**Note**: l'ordine entro una fase rispetta le dipendenze interne; UC nella stessa fase senza dipendenze reciproche sono
parallelizzabili. `0045` è ✅ perché la skill `new-usecase` è già presente in `.claude/skills/`. La capacità di sync di
questo indice è stata aggiunta a `new-change` insieme alla sua stesura; il resto di **0044** (gate snapshot + hook
privacy/RoPA) resta da implementare.
