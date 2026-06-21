# UC 0046 — skill `new-application` (codifica il pattern: FE+BE+modulo+CI+manifest+landing draft+quota+contratto GDPR+test)

**Area**: 10-skills-tooling · **Fase**: 4 · **Stato**: 🟢 deciso (skill da implementare)
**Dipendenze**: UC [0051](../11-apps/0051-app1-backend.md), UC [0052](../11-apps/0052-app1-modulo-frontend.md) (pattern validato dall'app #1)
**Fonte decisioni**: #07 G (lifecycle/skill), #09 A7/H36 (co-pilota pricing/quota), #13 C/L (manifest/contratto GDPR), #14 9 (landing), #10 (test scaffold)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [07-devops-cicd](../../07-devops-cicd.md), [09-pagamenti](../../09-pagamenti.md), [13-compliance-privacy](../../13-compliance-privacy.md), [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md), [10-testing](../../10-testing.md)

## 1. Obiettivo / Scope
Creare la skill **`new-application`** che **codifica il pattern** della prima app (UC 0051-0030) in uno scaffolding automatico,
così l'utente si concentra **solo sul business**.
**Incluso**: scaffold **backend** (modulo Maven, Dockerfile, migrazioni Flyway, logging strutturato by-default) + **frontend**
(modulo + manifest registry) + chiamata **`service-add`** (istanza modulo `microsaas_app`) + workflow **CI** + wiring `config.json`
+ **seed-base** + **test** (unit/integration/security/E2E + harness isolamento) + **co-pilota pricing/quota** (tier, prezzi
mensile/annuale+sconto, freemium, trial, **flow/stock** per metrica, fee effettiva) + **manifesto dati + contratto GDPR**
(export/purge, anonimizzazione guardrail) + **bozza landing** 5 lingue (+SEO/GEO). Segue il workflow `new-change` (branch + PR).
**Escluso**: la pubblicazione landing (è `finalize-landing`, UC 0053); il sync prezzi a Paddle (pipeline, UC 0022); l'implementazione del business specifico dell'app.

## 2. Attori & ruoli
- **Developer**: invoca `/new-application <descrizione>`, risponde al co-pilota (pricing/quota/GDPR), rivede la PR.
- **Skill** (tooling): genera tutto lo scaffolding e lascia la **PR** all'utente (#07 91).

## 3. Precondizioni
- Pattern app validato (UC 0051-0030); modulo `microsaas_app` (UC 0004); pipeline CI (UC 0005); design system (UC 0019);
  catalogo/subscription (UC 0013). Idealmente dopo #08/#10 così lo scaffold nasce con metriche/log/test (#07 "quando implementarla").

## 4. Flusso principale
1. `/new-application <descrizione>` → la skill chiede `app_id`/`user_model` (single/multi) + icona/colore-categoria.
2. **Scaffold BE**: modulo Maven su `commons`, schema `app_<app_id>` (via `service-add`), Flyway, logging by-default, contratto quota+GDPR stub.
3. **Scaffold FE**: modulo React lazy + manifest registry; **bozza landing** 5 lingue (copy AI on-brand + placeholder) + SEO/GEO per-app (#14 9/25).
4. **Co-pilota pricing/quota** (#09 A7/E23/K47): guida tier/prezzi (mensile+annuale, sconto ~17%), freemium, trial (default 14gg disattivabile), **flow vs stock** per ogni metrica, mostra **fee effettiva** + warning soft >10%.
5. **Co-pilota GDPR** (#13 C/L72): compila il **manifesto dati** (IT+EN), genera lo snippet privacy pubblico, definisce export/purge; guardrail anonimizzazione (blocca pseudonimizzazione spacciata per erasure).
6. **CI/seed/test**: genera workflow, seed-base, suite (incl. harness isolamento #10 15). Segue `new-change` → branch + **PR** all'utente (#07 91).

## 5. Flussi alternativi / edge / errori
- **Categorie particolari (art. 9)**: avviso forte + DPIA (#13 K) — non procede in sordina.
- **App multi-user (B2B)**: scaffold con inviti/seat (stock) (validato da UC 0054).
- **Fee >10%**: warning soft, non blocco; spinge verso annuale (#09 K47/48).
- **Niente deploy/Paddle diretto**: la skill scrive codice/config; sync prezzi = pipeline (UC 0022), pubblicazione landing = `finalize-landing`.

## 6. Risorse & runbook
**File skill** `.claude/skills/new-application/`. **Output per invocazione**: branch + PR con BE+FE+modulo infra+CI+manifest+
contratto GDPR+pricing-as-code+bozza landing+seed+test. **Runbook**: `/new-application` → rispondere ai co-piloti → rivedere/mergiare la PR; poi `finalize-landing` quando l'app è MVP.

## 7. Dati toccati
Genera **codice e dichiarazioni** (manifesto dati, pricing-as-code), non dati runtime. Il manifesto dichiara i dati personali
della nuova app (categorie/finalità/base/retention) → alimenta RoPA (UC 0030) + privacy snippet (UC 0002) + export/purge (UC 0032). Manifest GDPR: **è** lo strumento che lo crea.

## 8. Permessi & gate
- **Invarianti**: lo scaffold nasce **già conforme** — `tenant_id` dal JWT, filtro row-level (commons), modulo `microsaas_app`,
  logging strutturato, harness isolamento, contratto GDPR obbligatorio ("no contratto = no produzione", #13 L74).
- **Gate**: workflow `new-change` (requisiti/commit/merge); co-pilota GDPR/pricing fa **confermare** le scelte.

## 9. Requisiti di test
Skill di tooling: l'app generata deve nascere con suite verde (unit/integration/**security**/E2E + compliance export/purge,
#10/#13 L74). Verifica: scaffold completo (BE+FE+CI+manifest+landing+seed+test); pricing-as-code coerente; manifesto completo IT+EN.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #07 G18/20/85-94, #09 A7/E23/H36/K47, #13 C15/16/L69/L72/L74, #14 9/25, #10 15/32.
- **DoD**:
  1. `new-application` scaffolda BE+FE+modulo+CI+manifest+contratto GDPR+pricing-as-code+bozza landing+seed+test.
  2. Co-pilota pricing/quota (flow/stock, fee effettiva) e GDPR (manifesto IT+EN, anonimizzazione guardrail).
  3. Segue `new-change` (branch+PR); app generata con suite verde e invarianti by-default.
  4. UC 0054 valida la skill creando l'app #2 (B2B).
