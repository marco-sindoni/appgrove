# Indice di esecuzione — Use Case appgrove

**Ordine di esecuzione reale** dei 59 use case, in **tabella unica**. L'ordine deriva da un ordinamento
topologico sulle **dipendenze effettive** (estratte rileggendo per intero ogni use case, non solo dagli
header `Dipendenze:`) sotto una strategia **local-first**. Si implementa **dall'alto verso il basso**:
ogni UC ha tutti i suoi prerequisiti *hard* sopra di sé.

> **Due indici, due assi.** [README.md](README.md) = catalogo **per area** con stato del **drill-down**
> (🔴 da scrivere / 🟡 in corso / 🟢 scritto). **Questo file** = ordine **esecutivo** con stato di
> **implementazione** (sotto). Sono ortogonali: uno dice "la spec è pronta?", l'altro "il codice è in main?".

## Criterio di ordinamento: local-first

Esiste un **doppio binario**: molte dipendenze dichiarate sono di *deploy in cloud*, non di *sviluppo
locale*. In locale i servizi girano come **processi host sul Postgres locale** (zero AWS) e l'auth reale
(Cognito/Lambda, UC 0015/0016/0014) è **emulata da UC 0010**. L'ordine quindi:

1. costruisce e testa **prima tutto il prodotto offline** (core, schema, seed, auth locale, API, shell,
   app demo, pagamenti via stub, compliance);
2. poi la **messa in cloud** (Terraform, risorse condivise, modulo, CI/CD, Cognito/Lambda reali, observability);
3. poi **vetrina + legali + go-live**; infine **crescita**.

Coerente col principio **cost-min** e con l'**attivazione per fasi** (locale → test → prod): nessun costo
AWS finché il prodotto non gira offline.

## Legenda
- **Stato**: ⬜ da implementare · 🟡 in corso (change `NNNN-use-case-YYYY-…` aperta, non ancora in main) · ✅ implementato (in main)
- **Dipendenze**: i numeri sono i prerequisiti **hard** che devono comparire più in alto; `☁N` = dipendenza
  che si completa solo nella **messa in cloud** (non blocca lo sviluppo offline); `(N)` = relazione soft/non bloccante.

## 👉 Prossimo da implementare: **UC 0017** (flussi auth UI, in corso — change `0012`); poi **UC 0059** (gestione membri & inviti)

> **Manutenzione automatica.** La skill `new-change`, quando una change implementa uno use case (forma
> `NNNN-use-case-YYYY-…`), aggiorna **da sola** questo indice: `YYYY` → 🟡 all'avvio (step-01) e → ✅ alla
> chiusura/merge (step-04). Se si aggiungono nuovi UC, ri-eseguire l'ordinamento dipendenze (local-first).

---

| #  | UC | Titolo | Area | Dipendenze | Stato |
|----|------|--------|------|-----------|-------|
| 1  | [0044](10-skills-tooling/0044-aggiornamento-skill-new-change.md) | `new-change` update (use-case + gate test/snapshot + hook privacy) | Skills & Tooling | — | ✅ |
| 2  | [0045](10-skills-tooling/0045-skill-new-usecase.md) | skill `new-usecase` | Skills & Tooling | — | ✅ |
| 3  | [0008](03-local-dev/0008-stack-sviluppo-locale.md) | Stack dev locale (Compose) | Local Dev | — | ✅ |
| 4  | [0009](03-local-dev/0009-script-dev.md) | Script `dev/` (setup/up/seed/migrate/doctor) | Local Dev | 0008 | ✅ |
| 5  | [0019](06-frontend/0019-design-system-brand-kit.md) | Design system & brand kit | Frontend | — | ✅ |
| 6  | [0012](04-platform-core/0012-servizio-core-multitenancy.md) | Core service + multitenancy + schema `platform` (Flyway) | Platform Core | 0008, 0009 · ☁0004 | ✅ |
| 7  | [0011](03-local-dev/0011-dati-seed.md) | Seed data deterministico | Local Dev | 0012 | ✅ |
| 8  | [0010](03-local-dev/0010-provider-auth-locale.md) | Auth provider locale — security-core (JWT/JWKS, claim DB, refresh, fail-closed) | Local Dev | 0009, 0011, 0012, 0013 | ✅ |
| 9  | [0013](04-platform-core/0013-account-utenti-inviti-api.md) | Accounts/Users/Invitations + core REST API | Platform Core | 0012 | ✅ |
| 10 | [0058](03-local-dev/0058-flussi-auth-locali-completi.md) | Flussi auth locali completi (signup/verify/invite/reset/2FA/Mailpit) | Local Dev | 0010, 0013, 0011 | ✅ |
| 11 | [0020](06-frontend/0020-shell-spa-backoffice.md) | Backoffice SPA shell | Frontend | 0019, 0013, 0010 · ☁0015 | ✅ |
| 12 | [0017](05-auth/0017-flussi-auth.md) | Flussi auth UI (signup/login/reset/invite/2FA) | Auth | 0020, 0058, 0013, 0010 · ☁0015,0016,0018 | ✅ |
| 13 | [0059](06-frontend/0059-gestione-membri-inviti.md) | Gestione membri & inviti (UI backoffice) | Frontend | 0020, 0013, 0017 | ✅ |
| 14 | [0021](06-frontend/0021-console-admin-spa.md) | Admin console SPA | Frontend | 0019, 0013, 0020, 0010 · ☁0015,0016 | ✅ |
| 15 | [0051](11-apps/0051-app1-backend.md) | App #1 (B2C) backend | Apps | 0012, 0013 · ☁0004,0014 | 🟡 |
| 16 | [0052](11-apps/0052-app1-modulo-frontend.md) | App #1 frontend module | Apps | 0020, 0051 | ⬜ |
| 17 | [0023](07-payments/0023-stub-paddle-locale.md) | Stub Paddle locale (port PaymentProvider, webhook sintetici) | Payments | 0008, 0013 | ⬜ |
| 18 | [0022](07-payments/0022-pricing-as-code-sincronizzazione.md) | Pricing-as-code + sync pipeline | Payments | 0013, 0023 · ☁0005 | ⬜ |
| 19 | [0025](07-payments/0025-pipeline-webhook.md) | Pipeline webhook (ingest → SQS → consumer idempotente) | Payments | 0013, 0023 | ⬜ |
| 20 | [0026](07-payments/0026-ciclo-vita-abbonamento.md) | Ciclo di vita subscription | Payments | 0025, 0013 | ⬜ |
| 21 | [0024](07-payments/0024-checkout.md) | Checkout (overlay, polling) | Payments | 0023, 0022, 0025, 0020 | ⬜ |
| 22 | [0027](07-payments/0027-applicazione-entitlement-quota.md) | Enforcement entitlement + quota SPI | Payments | 0026, 0051 · ☁0014 | ⬜ |
| 23 | [0028](07-payments/0028-portale-cliente-self-service.md) | Customer portal self-service | Payments | 0026, 0020 | ⬜ |
| 24 | [0029](07-payments/0029-test-pagamenti-l1-l2-l3.md) | Test pagamenti L1/L2/L3 | Payments | 0024, 0025, 0023 | ⬜ |
| 25 | [0030](08-compliance-gdpr/0030-manifesti-dati-ropa.md) | Manifesti dati per-app + RoPA automation | Compliance & GDPR | 0051 · (0046 industrializza) | ⬜ |
| 26 | [0031](08-compliance-gdpr/0031-gate-privacy-ropa-new-change.md) | Gate privacy/RoPA in `new-change` | Compliance & GDPR | 0044, 0030 | ⬜ |
| 27 | [0032](08-compliance-gdpr/0032-framework-esportazione-cancellazione.md) | Framework export/erasure | Compliance & GDPR | 0013, 0051 | ⬜ |
| 28 | [0033](08-compliance-gdpr/0033-self-service-gdpr.md) | Self-service GDPR | Compliance & GDPR | 0032, 0020 | ⬜ |
| 29 | [0034](08-compliance-gdpr/0034-console-diritti-gdpr.md) | Console "Diritti GDPR" (admin) | Compliance & GDPR | 0032, 0021 | ⬜ |
| 30 | [0003](02-devops-infra/0003-fondamenta-terraform.md) | Terraform foundation | DevOps & Infra | — | ⬜ |
| 31 | [0055](02-devops-infra/0055-risorse-condivise-per-ambiente.md) | Risorse condivise per-ambiente | DevOps & Infra | 0003 | ⬜ |
| 32 | [0004](02-devops-infra/0004-modulo-microsaas-app.md) | Modulo `microsaas_app` | DevOps & Infra | 0003, 0055 | ⬜ |
| 33 | [0006](02-devops-infra/0006-osservabilita-base.md) | Observability baseline | DevOps & Infra | 0003, 0004 | ⬜ |
| 34 | [0005](02-devops-infra/0005-pipeline-cicd.md) | Pipeline CI/CD | DevOps & Infra | 0003, 0004, 0055, 0006 | ⬜ |
| 35 | [0015](05-auth/0015-cognito-auth-bff.md) | Cognito + auth BFF | Auth | 0003, 0012 | ⬜ |
| 36 | [0016](05-auth/0016-pre-token-gen-jwt.md) | Pre-Token-Gen Lambda + JWT validation | Auth | 0012, 0015 | ⬜ |
| 37 | [0014](04-platform-core/0014-authorizer-custom.md) | Custom Lambda authorizer | Platform Core | 0013, 0016 | ⬜ |
| 38 | [0018](05-auth/0018-localizzazione-email-auth.md) | Localizzazione email auth (Custom Message Lambda) | Auth | 0015 | ⬜ |
| 39 | [0046](10-skills-tooling/0046-skill-new-application.md) | skill `new-application` | Skills & Tooling | 0051, 0052, 0004, 0005, 0013, 0019, 0022, 0030, 0032 | ⬜ |
| 40 | [0054](11-apps/0054-app2-b2b-via-new-application.md) | App #2 (B2B) via `new-application` | Apps | 0046, 0013, 0027 | ⬜ |
| 41 | [0048](10-skills-tooling/0048-skill-drop-application.md) | skill `drop-application` | Skills & Tooling | 0004, 0046 | ⬜ |
| 42 | [0047](10-skills-tooling/0047-skill-pricing-change.md) | skill `pricing-change` | Skills & Tooling | 0022 | ⬜ |
| 43 | [0002](01-business-legal/0002-documenti-legali-multilingua.md) | Documenti legali reali 5 lingue | Business & Legal | — | ⬜ |
| 44 | [0036](09-marketing-site/0036-vetrina-astro-scheletro.md) | Vetrina Astro skeleton (S3+CloudFront) | Marketing Site | 0019, 0003 | ⬜ |
| 45 | [0037](09-marketing-site/0037-homepage-nav-footer.md) | Homepage + nav/footer | Marketing Site | 0036 | ⬜ |
| 46 | [0038](09-marketing-site/0038-template-landing-per-app.md) | Template landing per-app | Marketing Site | 0036 · (0046,0057) | ⬜ |
| 47 | [0057](10-skills-tooling/0057-skill-finalize-landing.md) | skill `finalize-landing` | Skills & Tooling | 0038, 0046, 0036 | ⬜ |
| 48 | [0040](09-marketing-site/0040-seo-tecnico.md) | SEO tecnico | Marketing Site | 0036 | ⬜ |
| 49 | [0041](09-marketing-site/0041-geo-llms.md) | GEO (`llms.txt`, crawler AI) | Marketing Site | 0036 | ⬜ |
| 50 | [0039](09-marketing-site/0039-newsletter-consenso-plausible.md) | Newsletter + consent log + Plausible | Marketing Site | 0036, 0013 | ⬜ |
| 51 | [0053](11-apps/0053-app1-landing.md) | App #1 landing | Apps | 0038, 0052, 0057 | ⬜ |
| 52 | [0056](04-platform-core/0056-riaccettazione-legali-runtime.md) | Ri-accettazione ToU/PP a runtime | Platform Core | 0002, 0013, 0020 | ⬜ |
| 53 | [0001](01-business-legal/0001-setup-business-legale.md) | Setup business/legale + account Paddle | Business & Legal | 0002, 0036 | ⬜ |
| 54 | [0007](02-devops-infra/0007-osservabilita-irrobustimento.md) | Observability hardening | DevOps & Infra | 0006 | ⬜ |
| 55 | [0035](08-compliance-gdpr/0035-job-conservazione-purga.md) | Job retention/purge | Compliance & GDPR | 0032, 0006 | ⬜ |
| 56 | [0042](09-marketing-site/0042-blog-risorse.md) | Blog/risorse | Marketing Site | 0036, 0040 | ⬜ |
| 57 | [0043](09-marketing-site/0043-lancio-paid-social.md) | Lancio paid/social | Marketing Site | 0037, 0039, 0041 · (0050) | ⬜ |
| 58 | [0050](10-skills-tooling/0050-skill-campaign-guide.md) | skill `campaign-guide` | Skills & Tooling | 0039 | ⬜ |
| 59 | [0049](10-skills-tooling/0049-skill-breach-response.md) | skill `breach-response` | Skills & Tooling | — (0006, 0030) | ⬜ |

**Traguardi (milestone) lungo la tabella:**
- riga **28**: prodotto applicativo completo e testato **offline** (zero AWS);
- righe **29–37**: **messa in cloud** (infra + auth reale + CI/CD + observability);
- riga **52**: **go-live** commerciale (Paddle attivo dopo vetrina + legali);
- righe **53–58**: crescita / hardening.

---

## Eccezioni note (dipendenze non lineari — valutate e accettate)

1. **0027 → 0014** (☁): in locale 0027 verifica la quota SPI nel servizio Quarkus; il gate entitlement
   grossolano dell'authorizer si integra quando 0014 atterra (riga 36).
2. **0030 ↔ 0046**: 0030 nasce come framework manifesto/RoPA per l'app #1 (manuale); 0046 (riga 38) lo
   *industrializza* per le app successive. Non è un ciclo: 0030 precede, 0046 riusa.
3. **0043 → 0050** (soft): il lancio è guidato da `campaign-guide`; relazione soft, non blocca.
4. **0012 ↔ 0016** (solo test): 0012 cita 0016 per i test d'integrazione JWT; in locale i test di 0012
   usano l'auth locale (0010) / unit test → ciclo rotto sulla dipendenza di test.

## Note

- L'ordine è governato dalle **dipendenze reali** (local-first), non dal raggruppamento per area o per
  fase. La colonna **Area** resta come riferimento; per il catalogo per-area vedi [README.md](README.md).
- Gli header `Dipendenze:` dei singoli file use case possono essere ancora quelli originali (incompleti):
  l'ordine autorevole è questa tabella. Allinearli è un follow-up opzionale (un change dedicato).
- `0045` è ✅ perché la skill `new-usecase` è già presente in `.claude/skills/`; `0044/0008/0009` sono ✅
  per le change già mergiate in `main`.
