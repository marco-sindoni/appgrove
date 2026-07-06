# UC 0005 — Pipeline CI/CD (OIDC, terraform, backend build/test+`[graal]`, frontend, Flyway one-shot, prod gate, path-filter, Infracost)

**Area**: 02-devops-infra · **Fase**: 1 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0003](0003-fondamenta-terraform.md), UC [0004](0004-modulo-microsaas-app.md)
**Fonte decisioni**: #07 (CI/CD), #06 (infra eseguita), #10 (gate test)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [07-devops-cicd](../../07-devops-cicd.md), [06-infra-iac](../../06-infra-iac.md), [10-testing](../../10-testing.md)

## 1. Obiettivo / Scope
Realizzare la **pipeline GitHub Actions** che porta il codice in infra e servizi in modo automatico, ripetibile, cost-min.
**Incluso**: autenticazione **OIDC** (no chiavi); **PR = solo verifica** (terraform plan commentato + suite completa con
path-filter); **merge→test = apply automatico**; **tag→prod = plan salvato + gate di approvazione + apply**; build backend
**JVM di default**, **native GraalVM on-demand** (`[graal]` o workflow_dispatch) e **sempre in prod**; build **2 SPA**
(stesso bundle, cambia `config.json`); **Flyway one-shot** (task ECS in VPC); **gate prod bloccante** (solo immagini native
già su ECR); **Infracost** sulle PR.
**Escluso**: definizione infra (UC 0003/0004), strategia di test in sé (#10), observability (UC 0006).

## 2. Attori & ruoli
- **Developer**: apre PR, fa squash-merge (può mettere `[graal]`), crea tag di release.
- **CI (GitHub Actions)**: esegue verifica/deploy con ruoli OIDC per env.
- **Approver**: dà l'OK al gate prod (GitHub Environments/required reviewer).

## 3. Precondizioni
- Foundation (UC 0003) con ruoli OIDC e state; modulo `microsaas_app` (UC 0004); repo con convenzione `new-change` (#07 B2).

## 4. Flusso principale
1. **PR** (`change/NNNN-*` → `main`): `terraform plan` commentato; suite completa (BE unit+integration+security+ArchUnit, FE
   component+**E2E**, contract+drift+**oasdiff**, infra fmt/validate/tflint/checkov/terraform test/**Infracost**) con **path-filter**
   (#10 34; security/E2E/oasdiff **sempre** bloccanti, #10 35). Nessuna risorsa AWS toccata.
2. **Merge su `main`** (squash): deploy **automatico in test** — `apply` su `envs/test`, `mvn test` → build immagine (JVM, o
   native se `[graal]`) → push **ECR taggata SHA** → **Flyway migrate** (task ECS one-shot in VPC, connessione **diretta** Agroal — il Proxy è solo per le Lambda, #05 dec.3) → **deploy ECS** rolling → health check (#07 10/14/15).
3. Frontend: build Vite → sync S3 (asset immutable, `index.html`/`config.json` no-cache) → **invalidation CloudFront** mirata;
   **`config.json` generato come output Terraform** (#07 11/12). Le **source map** sono prodotte ma **non pubblicate**: caricate come **artifact privato CI** per la de-minificazione offline degli errori frontend (#08 24, UC 0006 §5). Una modifica al `design-system` (path-filter) **ripubblica entrambe le SPA** (#07 dec.3).
4. **Tag `v*`** → prod: `plan` salvato → **approvazione** → `apply` del plan salvato; promozione **stesso SHA** (frontend
   stesso bundle, immagini native già su ECR) dietro **gate bloccante** (#07 21/23).
5. La CI **invoca gli stessi wrapper `infra/scripts/`** del locale (unica fonte di verità, #07 6).

## 5. Flussi alternativi / edge / errori
- **Gate prod senza immagine native**: pipeline del tag **fallisce** col messaggio guidato (run workflow native su stesso SHA,
  poi ri-tag; fallback PR `[graal]`) (#07 23).
- **Ordine migrazioni**: sempre `build → test → flyway migrate → deploy` (#07 15); disciplina **expand/contract** (#07 16).
- **Cron spegnimento test**: `test-stop` giornaliero (desired→0), idempotente, UTC fisso (#07 28).
- **Costi CI**: free tier via path-filter + caching `~/.m2`/npm + `concurrency` cancel + native solo on-demand (#07 27).

## 6. Risorse & runbook
**File**: `.github/workflows/*.yml` (verifica PR, deploy test, release prod, native toggle dispatch, cron test-stop).
**Runbook**: aprire PR → verde → squash-merge (deploy test) → quando pronto, **`[graal]` o native dispatch** sullo stesso
commit → **tag `v*`** → approvare il gate → prod. **Secrets**: la CI **non legge mai** i segreti app; al più li gestisce via
Terraform (genera password in Secrets Manager senza esporle) (#07 26).

## 7. Dati toccati
Nessun dato personale: la CI muove artefatti (immagini, bundle, plan). Flyway tocca **struttura** DB, non dati. Encryption/UE
residency ereditate dall'infra. Manifest GDPR N/A; il **gate privacy** (UC 0031) è un check CI separato sul contenuto del cambio.

## 8. Permessi & gate
- **Invarianti**: la **suite security/multi-tenancy gira SEMPRE** (mai esclusa dal path-filter) ed è bloccante (#10 35) →
  protegge tenant_id-da-JWT, filtro row-level. **OIDC least-privilege**, prod solo da tag (#07 25).
- **Gate**: approvazione prod; gate native bloccante; oasdiff/E2E/security bloccanti.

## 9. Requisiti di test
La pipeline **è** l'esecutore della strategia #10: deve far girare e **bloccare** sui rossi (security, E2E, oasdiff, contract
drift). Coverage **riportata, non bloccante** (#10 36). Smoke E2E opzionale su test al merge; **L3 Paddle smoke** pre-release
(UC 0029). Verifica: una PR con violazione cross-tenant **non passa**.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #07 1/2/3/4/6/7/10/11/12/14/15/21/23/25/26/27/28, #10 34/35/36/37, #06 (infra), #08 24 (source map artifact CI).
- **DoD**:
  1. OIDC attivo; PR = verifica, merge = test, tag = prod con gate.
  2. Backend JVM default + native `[graal]`/dispatch + sempre prod; gate prod bloccante senza native.
  3. Flyway one-shot in VPC prima del deploy; frontend stesso bundle + `config.json` da Terraform.
  4. Path-filter + Infracost + suite #10 bloccante sui rossi; CI ≈ $0.

## Punti aperti / decisioni differite

_Tracciato dalla change `0019-use-case-0022-…` (regola CLAUDE.md "Tracciamento delle decisioni differite")._

- **Cablaggio dello step di sync pricing nei workflow CI.** UC 0022 (change `0019`) implementa il **motore di sync**
  pricing-as-code → catalogo DB → Paddle (esercitabile offline contro lo stub) **e ne espone un entrypoint runnable in
  command-mode Quarkus** (`sync-pricing`, non avvia il server HTTP). Quel che resta a UC 0005 è **solo** invocare quel comando
  nel workflow **dopo il Flyway migrate**: **deploy su test → `sync-pricing` verso Paddle sandbox**, **tag→prod →
  `sync-pricing` verso Paddle production** (#09 H37, #07), con i secret Paddle per-env (Secrets Manager, #09 I38). Non c'è
  ancora `.github/workflows/`, quindi il cablaggio non è realizzabile finché UC 0005 non costruisce la pipeline.
  **Proprietario** del cablaggio CI: UC 0005.

_Tracciato dalla change `0025-use-case-0029-…` (test pagamenti L1/L2/L3)._

- **Cablaggio per-PR bloccante di L1/L2 (UC 0029).** La change `0025` ha reso `run-tests.sh` il gate canonico completo:
  l'area `frontend` esegue vitest **e** gli E2E Playwright L2 (browser auto-installato), il backend include la catena L1
  `WebhookEntitlementChainTest`. Quel che resta a UC 0005 è **solo** eseguire `./run-tests.sh` per-PR e marcarlo required —
  a quel punto la clausola "L1+L2 bloccanti per-PR" (#09 D20, #10 35) è soddisfatta per costruzione.
- **Job L3 nella pipeline di release tag→prod + gate con override motivato (UC 0029).** La suite smoke sandbox esiste già
  (`frontend/apps/backoffice/playwright.l3.config.ts` + `e2e-l3/`, auto-skip senza env `APPGROVE_L3_*`; runbook in
  `e2e-l3/README.md`). Restano a UC 0005: il job di release che la esegue contro l'ambiente deployato, l'esito che
  confluisce nel **gate di approvazione manuale** prod (#07 b1) e il meccanismo di **override manuale con motivazione
  registrata** (audit) quando il sandbox Paddle è down (#09 D20 L3). Prerequisito esterno: account sandbox (UC 0001).
- **Cron giornaliero `test-stop` + `test-start` via `workflow_dispatch`** _(tracciato dalla change
  `0033-use-case-0004-…`)_: la change 0033 implementa gli script `infra/scripts/test-stop` (desired
  count → 0, idempotente) e `test-start` (→ 1) previsti da #07 28. Resta a UC 0005 il **cablaggio nei
  workflow CI**: cron giornaliero a orario UTC fisso (~21:00 IT) che lancia `test-stop` sull'ambiente
  test, e avvio manuale (`workflow_dispatch`) che lancia `test-start`. Differito perché i workflow
  GitHub Actions sono lo scope di UC 0005; gli script sono già pronti per essere invocati dalla CI.
- **Adeguamento cloud del client SQS dei servizi (`services/commons`)** _(tracciato dalla change
  `0033-use-case-0004-…`)_: `SqsMessageQueues` oggi è cablato per il solo locale — credenziali statiche
  "local/local", regione default `eu-south-1`, nessun prefisso sui nomi coda. Per girare nel cloud deve:
  (a) usare la catena di credenziali di default quando manca l'endpoint override (i task hanno il task
  role IAM); (b) leggere il **prefisso dei nomi coda** da config (`appgrove.sqs.queue-prefix`, ← env var
  `APPGROVE_SQS_QUEUE_PREFIX` già impostata dalla task definition del modulo `microsaas_app`, vuoto in
  locale); (c) regione da `APPGROVE_SQS_REGION` (idem). Differito perché i servizi girano nel cloud solo
  col deploy: è il primo momento utile e naturale per il profilo cloud. Proprietà: UC 0005.
- **Promozione immagini tra repo ECR per-ambiente + architettura ARM64** _(tracciato dalla change
  `0033-use-case-0004-…`)_: il modulo `microsaas_app` crea un repo ECR **per ambiente**
  (`appgrove-<env>-<app_id>`, nomi unici nello stesso account) e task definition **ARM64/Graviton**
  (~-20% di costo; Fargate Spot supporta ARM64). La pipeline deve: buildare immagini native ARM64,
  pushare su `appgrove-test-*` al merge e **promuovere** (retag/copy) su `appgrove-prod-*` al tag —
  senza rebuild, stessa immagine. Lo schema di tagging (per-SHA vs `latest`, oggi tag mobile di
  default con `image_tag` sovrascrivibile) si fissa qui. Proprietà: UC 0005 (già correlato a #07 H/E9).

_Tracciato dalla change `0035-use-case-0006-…` (osservabilità di base)._

- **`curl` nell'immagine dei servizi (health check ECS).** La change 0035 (UC 0006) definisce nella task
  definition l'health check del container (`curl -fsS http://localhost:<porta>/q/health/live`): l'immagine
  costruita dalla pipeline DEVE includere `curl` (o un binario di probe equivalente — in tal caso adeguare
  il comando in `infra/modules/microsaas_app/ecs.tf`). Nessun deploy esiste ancora, quindi il vincolo è
  solo sul build dell'immagine. Proprietà: UC 0005.
- **`VITE_BUILD_SHA` nel build frontend.** Il reporter errori (`frontend/packages/error-reporter`, #08 23)
  marca ogni evento con lo SHA di build (`import.meta.env.VITE_BUILD_SHA`, fallback `dev` in locale): la
  pipeline FE deve passarlo al build Vite (si sposa con l'artifact privato delle source map, #08 24, già
  in carico a questo UC). Proprietà: UC 0005.
- **`errorIngestUrl` nel `config.json` per-ambiente delle SPA.** Le app leggono l'endpoint di ingest
  errori dalla config runtime (`errorIngestUrl`, vuoto in locale = reporter spento): il deploy FE deve
  scrivere nel `config.json` per-env il valore `https://api.<env-prefix><domain>/ingest/errors` (rotta
  creata da `platform_shared/error_ingest.tf`). Proprietà: UC 0005.
