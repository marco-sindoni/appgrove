# UC 0046 — skill `new-application` (codifica il pattern: FE+BE+modulo+CI+manifest+landing draft+quota+contratto GDPR+test)

**Area**: 10-skills-tooling · **Fase**: 4 · **Stato**: 🟢 deciso (skill da implementare)
**Dipendenze**: UC [0051](../11-apps/0051-app1-backend.md), UC [0052](../11-apps/0052-app1-modulo-frontend.md) (pattern validato dall'app #1)
**Fonte decisioni**: #07 G (lifecycle/skill), #09 A7/H36 (co-pilota pricing/quota), #13 C/L (manifest/contratto GDPR), #14 9 (landing), #10 (test scaffold)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [07-devops-cicd](../../07-devops-cicd.md), [09-pagamenti](../../09-pagamenti.md), [13-compliance-privacy](../../13-compliance-privacy.md), [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md), [10-testing](../../10-testing.md)

> **Open point da change 0004 (UC 0009).** Gli script `dev/` lasciano tre agganci che l'auto-wiring di questa skill (e i
> primi servizi) devono riempire: `dev/lib/migrate.sh` (**stub** → Flyway sul Postgres locale), `dev/lib/service.sh`
> (**stub** → avvio selettivo core + `<app_id>`), e l'**hook "processi-app"** in `dev/lib/up.sh`. Una nuova app deve
> agganciarsi così che `./dev.sh up`/`migrate`/`service <app_id>` la prendano automaticamente (#11 §11).

## 1. Obiettivo / Scope
Creare la skill **`new-application`** che **codifica il pattern** della prima app (UC 0051-0030) in uno scaffolding automatico,
così l'utente si concentra **solo sul business**.
**Incluso**: scaffold **backend** (modulo Maven, Dockerfile, migrazioni Flyway, logging strutturato by-default) + **frontend**
(modulo + manifest registry) + chiamata **`service-add`** (istanza modulo `microsaas_app`) + workflow **CI** + wiring `config.json`
+ **seed-base** + **test** (unit/integration/security/E2E + harness isolamento) + **co-pilota pricing/quota** (tier, prezzi
mensile/annuale+sconto, freemium, trial, **flow/stock** per metrica, fee effettiva) + **manifesto dati + contratto GDPR**
(export/purge, anonimizzazione guardrail) + **bozza landing** 5 lingue (+SEO/GEO). Segue il workflow `new-change` (branch + PR).
**Escluso**: la pubblicazione landing (è la skill `finalize-landing`, UC 0057); il sync prezzi a Paddle (pipeline, UC 0022); l'implementazione del business specifico dell'app.

## 2. Attori & ruoli
- **Developer**: invoca `/new-application <descrizione>`, risponde al co-pilota (pricing/quota/GDPR), rivede la PR.
- **Skill** (tooling): genera tutto lo scaffolding e lascia la **PR** all'utente (#07 91).

## 3. Precondizioni
- Pattern app validato (UC 0051-0030); modulo `microsaas_app` (UC 0004); pipeline CI (UC 0005); design system (UC 0019);
  catalogo/subscription (UC 0013). Idealmente dopo #08/#10 così lo scaffold nasce con metriche/log/test (#07 "quando implementarla").

## 4. Flusso principale
1. `/new-application <descrizione>` → la skill chiede `app_id`/`user_model` (single/multi) + icona/colore-categoria.
2. **Scaffold BE**: modulo Maven su `commons`, schema `app_<app_id>` (via `service-add`), Flyway, logging by-default, contratto quota+GDPR stub.
3. **Scaffold FE**: modulo React lazy + manifest registry; **handler error-ingest** (`window.onerror`/`onunhandledrejection` → ingest, contesto `app_id`/route/build-SHA, #08 23) by-default; **bozza landing** 5 lingue (copy AI on-brand + placeholder) + SEO/GEO per-app (#14 9/25).
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
contratto GDPR+pricing-as-code+bozza landing+seed+test. **Runbook**: `/new-application` → rispondere ai co-piloti → rivedere/mergiare la PR; poi `finalize-landing` (UC 0057) quando l'app è MVP.

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

## Punti aperti / decisioni differite

> **Stato dopo la change `0041-use-case-0046-…`.** I punti sotto marcati **✅ chiuso** sono stati
> risolti da quella change; restano in elenco (non cancellati) perché il *perché* della soluzione è
> parte della memoria del caso d'uso. I punti senza marcatura sono ancora aperti.

_Tracciato dalla change `0008-use-case-0011-…` (regola CLAUDE.md "Tracciamento delle decisioni differite")._

- ✅ **chiuso dalla change 0041** — la scoperta automatica è reale: `dev/lib/services.sh` deriva la mappa
  servizio → identificativo app → porta → schema dagli `application.properties` già presenti nei servizi, e
  `dev migrate`/`dev service`/`app-start.sh`/`app-stop.sh`/`dev/Caddyfile`/`tools/smoke` la consumano. Il
  generatore **non tocca** quei file. Testo originale del punto:
- **Industrializzazione di `dev migrate` multi-servizio.** La change 0008 rende `dev migrate` reale ma **solo per
  `services/core`** (one-shot del container `flyway/flyway` sulle migrazioni di core), perché è l'unico servizio con
  migrazioni e il seed (UC 0011) ne ha bisogno. La versione **multi-servizio** — scoperta di tutti i `services/<app>`
  con `db/migration`, ordinamento, e l'aggancio automatico quando `new-application` genera una nuova app — va
  industrializzata **qui**. **Proprietario**: UC 0046 (comando `dev migrate` di base: UC 0009).

- ✅ **chiuso dalla change 0041**, con una postura decisa esplicitamente col Platform Engineer: **cache con
  rete di sicurezza**. L'app legge la **proiezione locale** (`app_<id>.entitlement_projection`); core pubblica
  un **evento sottile** di invalidazione (solo "i diritti del tenant T sono cambiati") sulla coda
  `entitlement-<app_id>`, così la derivazione degli entitlement resta in **un solo posto**. Tre situazioni:
  riga fresca → si usa senza soglia di scadenza; riga da rinfrescare → si tenta il rinfresco e, se core non
  risponde, **si serve il valore vecchio** (un guasto di core non blocca i paganti); riga assente → rinfresco
  obbligatorio, e solo se core è irraggiungibile si nega. La chiamata sincrona **non sparisce**: retrocede a
  rete di sicurezza (`@SafetyNet`) per il caso "tenant sconosciuto". Scostamenti strumentati con misure e
  allarmi (`safety_net`, `stale_served`, `denied_unknown`, coda degli scarti) nel modulo `microsaas_app`.
  Testo originale del punto:
- **Disaccoppiamento entitlement: ritirare la chiamata sincrona app→core (da UC 0027).** _Tracciato dalla change
  `0023-use-case-0027-…` (enforcement entitlement/quota); rationale completo in [docs/_BACKLOG.md](../../_BACKLOG.md),
  sezione "Architettura di piattaforma — accoppiamento inter-servizio app↔core"._ UC 0027 sblocca l'enforcement reale
  con una via **semplice e sincrona**: l'app chiama `GET /api/platform/v1/me/entitlements` di core via HTTP (client REST
  isolato in `commons`, propagazione JWT) a ogni gate 402 / risoluzione cap quota. È un **antipattern a regime** (fan-in
  sincrono, accoppiamento a disponibilità/lifecycle di core sul path caldo). **Questo UC è il primo del flusso che
  permette la bonifica**: industrializzando il pattern per-app, `new-application` deve generare per ogni app una
  **proiezione locale read-only di entitlement+limits** (nello schema `app_<id>`) alimentata da **eventi di lifecycle
  subscription pubblicati da core** sul bus interno (ossatura SQS già presente da UC 0025), così l'enforcement legge
  **solo dati propri** — niente chiamata sincrona, niente lettura cross-schema. Il seam attuale (`QuotaLimitSource`
  sostituibile + `EntitlementService`/client in `commons`) è progettato per essere rimpiazzato dalla proiezione **senza
  toccare il codice di dominio dell'app**. UC 0054 (2ª app) **non deve perpetuare** la chiamata sincrona. **Proprietario**:
  UC 0046 (industrializzazione); il publisher di eventi lato core può anticiparsi se serve prima.
- **Generazione del blocco `module "app_<id>"` nel formato della change 0033 (UC 0004)** _(tracciato
  dalla change `0033-use-case-0004-…`)_: il modulo `infra/modules/microsaas_app` e lo script
  `infra/scripts/service-add <app_id>` esistono e fissano il formato del blocco `module` negli env
  `test`/`prod` (input: `app_id`, aggancio agli output di `platform_shared`/`baseline`). La skill
  `new-application` **non deve reinventare il wiring**: genera il blocco riusando `service-add` (o lo
  stesso template) insieme allo scaffold del codice, come da #07 20 ("non si lancia `service-add` a
  mano"). Differito perché la skill è lo scope di questo UC; il mattone infra è già pronto.
- **Reporter errori frontend nello scaffold (#08 23)** _(tracciato dalla change `0035-use-case-0006-…`)_:
  il package condiviso `@appgrove/error-reporter` esiste (UC 0006) ed è cablato in backoffice/admin; lo
  scaffold dei nuovi moduli frontend deve installarlo allo stesso modo (`installErrorReporter` con
  `appId` del modulo, endpoint da `errorIngestUrl` della config runtime). Lato backend, lo scaffold
  eredita gratis MDC/health/EMF/audit da `services/commons` + le risorse per-servizio dal modulo
  `microsaas_app` (che ora genera anche allarmi/widget/subscription filter, invariante #3).

_Tracciato dalla change `0036-use-case-0005-…` (pipeline CI/CD)._

- ✅ **chiuso dalla change 0041** — le liste sono **derivate**, non più duplicate: `tools/ci/services.sh`
  (che riusa la stessa scoperta di `dev/lib/services.sh`) alimenta la matrice di build e i cicli per-servizio
  dei tre workflow. Restano volutamente espliciti i cicli `for app in backoffice admin`: sono le due SPA
  fissate da `platform_shared`, non crescono con le app del marketplace. Testo originale del punto:
- **Liste per-servizio nei workflow CI da aggiornare a ogni nuova app.** I workflow generati dalla change 0036
  hanno i servizi **espliciti** in tre punti: matrix `build` di `deploy-test.yml` (coppie `service`/`app_id`),
  loop `for app in platform fatture` (migrate/gate native/promozione in `deploy-test.yml` e `release-prod.yml`).
  Gli agganci Terraform sono già automatici (`service-add` mantiene `image_tag`, marker `ci-services` in
  `ci_deploy`, marker observability): resta ai workflow. Quando `new-application` scaffolda un'app DEVE
  aggiornare anche queste liste (o, meglio, derivarle dall'output `ci_deploy` per eliminare la duplicazione).
  **Proprietario**: UC 0046.
