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
**File**: `.github/workflows/` — `verify-pr.yml`, `deploy-test.yml` (il toggle native è la checkbox del suo
`workflow_dispatch`, #07 9), `release-prod.yml`, `env-ops.yml` (cron test-stop + dispatch start/stop); task one-shot:
`infra/scripts/oneshot`.
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

_Sezione aggiornata dalla change `0036-use-case-0005-…` (implementazione della pipeline): tutti i punti che questo UC
possedeva sono stati **risolti a codice**; la lista storica è riassunta sotto con l'esito. La pipeline però **non è mai
girata dal vivo** (ambienti spenti, attivazione per fasi #12): la prima esecuzione live + la configurazione del repo
GitHub sono tracciate in [docs/_BACKLOG.md](../../_BACKLOG.md), sezione "Attivazione ambienti cloud"._

**Risolti dalla change `0036` (in sintesi):**

- **Sync pricing in CI** (da UC 0022): `sync-pricing` come task one-shot dopo il Flyway migrate — deploy test → Paddle
  sandbox, tag→prod → Paddle production — **gated sull'esistenza del secret** `appgrove/<env>/paddle` (convenzione
  fissata; la creazione dei secret è di UC 0001, vedi i suoi "Punti aperti"). Senza secret: skip con warning.
- **L1/L2 per-PR bloccanti + job L3 in release** (da UC 0029): `verify-pr.yml` esegue le aree di `run-tests.sh` con
  path-filter e invarianti #10 35 (security/E2E/oasdiff sempre); `release-prod.yml` ha il job `l3-smoke`
  pre-approvazione con auto-skip senza secret e override motivato registrato (audit). Secret reali → UC 0001.
- **Cron `test-stop` + `test-start` dispatch** (da UC 0004): workflow `env-ops.yml` (cron 20:00 UTC ≈ 21:00 IT;
  dispatch start/stop), stessi wrapper `infra/scripts/`.
- **Adeguamento cloud dei client AWS** (da UC 0004): `SqsMessageQueues`, `SqsWebhookQueue` e `S3ExportStorage` usano la
  **catena di credenziali di default** senza endpoint override (task role IAM), prefisso code da
  `appgrove.sqs.queue-prefix` (← `APPGROVE_SQS_QUEUE_PREFIX`), regioni da `APPGROVE_SQS_REGION`/`APPGROVE_S3_REGION`
  (quest'ultima aggiunta alla task definition del modulo). Test JUnit dedicati.
- **Promozione ECR + ARM64 + tagging** (da UC 0004): schema di tagging **fissato per-SHA** (`<sha>` JVM,
  `<sha>-native` GraalVM; `TF_VAR_image_tag` pilota i root env — niente `latest` mobile); build su runner
  `ubuntu-24.04-arm`; promozione test→prod = retag/push dello **stesso** manifest, nessun rebuild; gate prod
  bloccante sui soli tag `-native` col messaggio guidato di #07 23.
- **`curl` nell'immagine** (da UC 0006): Dockerfile JVM (ubi9/openjdk-21-runtime) e native (ubi9-minimal + microdnf)
  con **verifica al build** (`RUN curl --version`): un cambio di base image rompe in build, non al deploy.
- **`VITE_BUILD_SHA` + source map artifact** (da UC 0006/#08 24): build Vite con `VITE_BUILD_SHA=<sha>`, source map
  `hidden` caricate come artifact privato CI (90 giorni) e rimosse prima del sync S3.
- **`errorIngestUrl` nel `config.json`** (da UC 0006): l'output Terraform `spa_config` (envs test/prod) genera il
  `config.json` per-SPA con `errorIngestUrl = <api_url>/ingest/errors`; la pipeline lo riscrive a ogni deploy.
- **Flusso prod "piano salvato → approvazione → apply"**: wrapper estesi (`plan <env> --out`, `up <env> --plan`) +
  environment GitHub `prod` con required reviewer; ordine `build → test → migrate → deploy` garantito dal task
  one-shot `infra/scripts/oneshot` (clona la task definition del service con l'immagine nuova, senza toccare il service).

**Restano aperti (proprietà altrui):**

- **Campi Cognito nel `config.json`** → UC 0015 (placeholder vuoti oggi; vedi i suoi "Punti aperti").
- **Secret Paddle per-env + secret GitHub `APPGROVE_L3_*`** → UC 0001.
- **Liste per-servizio nei workflow** (matrix/loop `platform fatture`) da aggiornare a ogni nuova app → UC 0046
  (gli agganci Terraform sono già automatici via `service-add`: `image_tag`, marker `ci-services`, observability).
- **Promozione del job `smoke` di verify-pr a check bloccante** _(tracciato dalla change `0037-use-case-0015-…`)_ —
  il job (area `smoke` di `run-tests.sh`: boot artefatti nei profili di spedizione + stack headless dev, nato dalla
  regressione `queue-prefix` di questo UC, #10 37bis) parte **non bloccante** (`continue-on-error`): dopo un periodo
  di run stabili (indicativamente 2–3 settimane senza falsi rossi) va promosso a **required** in branch protection e
  tolto il `continue-on-error`. **Proprietario**: UC 0005 (configurazione repo, vedi anche _BACKLOG "Attivazione").
- **Prima esecuzione live + configurazione repo GitHub** (variabile `AWS_ACCOUNT_ID`, environment `prod`, branch
  protection/check required, `INFRACOST_API_KEY`, verifica fatturazione runner ARM64 su repo privato) →
  [docs/_BACKLOG.md](../../_BACKLOG.md), "Attivazione ambienti cloud".
