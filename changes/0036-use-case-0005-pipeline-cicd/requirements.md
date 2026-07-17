# Change 0036 — Pipeline CI/CD (use case 0005)

**Fonte**: [UC 0005 — Pipeline CI/CD](../../docs/usecases/02-devops-infra/0005-pipeline-cicd.md)
**Decisioni**: #07 (tutte), #10 34/35/36, #06, #08 23/24, #09 H37/I38/D20
**Perimetro**: change unica (opzione A concordata) — workflow + Dockerfile + adattamenti codice + Terraform + strumenti.
**Aree toccate**: `.github/workflows/` (nuova), `services/*`, `frontend/`, `infra/`, docs.

## Obiettivo

Realizzare la pipeline GitHub Actions completa: PR = solo verifica, merge su `main` = deploy automatico
in test, tag `v*` = produzione con piano salvato + gate di approvazione. Autenticazione **OIDC** (ruoli
`appgrove-github-actions-{test,prod}` già creati da `infra/global/oidc.tf`, prod assumibile solo da tag).
La CI invoca **gli stessi wrapper `infra/scripts/`** del locale (unica fonte di verità).

**Vincolo di verifica accettato**: gli ambienti AWS sono spenti (attivazione per fasi) → la prima
esecuzione dal vivo della pipeline è **differita** all'accensione degli ambienti e tracciata (v. sotto).
Accettazione di questa change = validazione statica (actionlint) + `run-tests.sh` verde sulle aree toccate.

## 1. Workflow GitHub Actions (`.github/workflows/`)

### 1a. `verify-pr.yml` — pull request verso `main`
- `concurrency` per-ref con cancellazione delle run superate; cache Maven `~/.m2` e npm.
- **Path-filter** per area (backend / frontend / infra / compliance / design-system), con invarianti #10 35:
  **security, E2E frontend e oasdiff girano SEMPRE** (mai esclusi dal filtro) e sono bloccanti.
  - backend toccato → `mvn -B test` completo; backend non toccato → esecuzione comunque della parte
    security/multi-tenancy (moduli `commons`/security-core + ArchUnit).
  - frontend → vitest; **E2E Playwright L2 sempre**.
  - infra → `infra/scripts/check` (fmt, validate, terraform test, tflint **con ruleset AWS via `tflint --init`**, checkov).
  - compliance → `tools/compliance` `npm test` + `npm run check`.
- **`terraform plan` di `envs/test` commentato sulla PR** (OIDC ruolo test; nessun apply). Il plan di
  prod NON è possibile in PR (trust policy prod = solo tag): avviene al tag, nel workflow di release.
- **oasdiff**: confronto base↔head dei `services/*/src/main/resources/META-INF/openapi/openapi.yaml`;
  breaking change = rosso bloccante.
- **Infracost** sulla PR quando `infra/` è toccata (commento con delta costi; richiede secret
  `INFRACOST_API_KEY` — passo manuale post-merge).
- Coverage **riportata, non bloccante** (#10 36).

### 1b. `deploy-test.yml` — push su `main` + `workflow_dispatch`
- `workflow_dispatch` con checkbox **"build native"** (decisione #07 9: il toggle native è integrato qui,
  niente quinto file separato).
- Build **native** se: messaggio del commit di squash-merge contiene `[graal]` (case-insensitive) oppure
  checkbox del dispatch; altrimenti **JVM** (default).
- Backend (servizi deployati: `core`, `fatture`; `auth-local` è solo locale, **esclusa**):
  `mvn test` → build immagine **ARM64** (JVM o native) → push su ECR `appgrove-test-<app>` **taggata SHA**.
  Si buildano **tutte** le immagini a ogni deploy (tag per-SHA coerente per tutti i servizi; la native,
  costosa, resta on-demand). Runner `ubuntu-24.04-arm` per le build immagine (native sotto QEMU è impraticabile).
- `infra/scripts/up test --yes` con `TF_VAR_image_tag=<SHA>` (lo schema di tagging si fissa qui: **per-SHA**,
  niente `latest` mobile).
- **Flyway migrate**: task ECS one-shot **in VPC** per servizio (immagine del servizio, comando dedicato,
  connessione **diretta** Agroal — il Proxy è solo Lambda), **prima** del deploy (`build → test → migrate → deploy`).
- **`sync-pricing`** (UC 0022): task one-shot del core verso **Paddle sandbox**, dopo il migrate. Se i
  secret Paddle non esistono ancora (account sandbox = UC 0001) → **skip con warning esplicito**.
- Deploy ECS rolling (l'apply aggiorna la task definition al nuovo tag) → attesa `services-stable` = health check.
- Frontend (se toccati `frontend/` o `packages/design-system` — il design-system ripubblica **entrambe** le SPA):
  `vite build` con **`VITE_BUILD_SHA=<SHA>`**, source map **hidden** generate → caricate come **artifact
  privato CI** (#08 24) → rimosse dal bundle → sync S3 (asset `immutable`; `index.html`/`config.json` no-cache) →
  **`config.json` generato dagli output Terraform** (incluso `errorIngestUrl`) → invalidation CloudFront mirata.
  Il `config.json` viene riscritto a ogni deploy anche senza rebuild del bundle.

### 1c. `release-prod.yml` — tag `v*` (+ `workflow_dispatch` con tag e `override_reason`)
- Verifiche preliminari: lo SHA del tag è su `main`; **gate native bloccante** — esistono immagini
  **native** su ECR test per quello SHA per **tutti** i servizi, altrimenti fail col **messaggio guidato**
  di #07 (preview in `docs/07-devops-cicd.md`, valori dinamici).
- **Smoke L3 Paddle** (UC 0029) contro l'ambiente test deployato: auto-skip se i secret `APPGROVE_L3_*`
  mancano (warning registrato); **override manuale con motivazione** via `workflow_dispatch`
  (`override_reason` obbligatoria, registrata nel summary della run = audit).
- `terraform plan` di `envs/prod` **salvato** (`-out`, caricato come artifact) → **gate di approvazione**
  (GitHub Environment `prod`, required reviewer) → **apply del plan salvato** (si applica esattamente ciò
  che è stato approvato).
- **Promozione immagini** (stesso SHA, **niente rebuild**): retag/copy ECR `appgrove-test-*` → `appgrove-prod-*`
  (crane/skopeo) → Flyway migrate prod (one-shot) → apply (deploy ECS) → `sync-pricing` verso **Paddle production**.
- Frontend: **stesso bundle** promosso — sync S3 bucket test → bucket prod (escluso `config.json`),
  `config.json` prod dagli output Terraform, invalidation mirata.

### 1d. `env-ops.yml` — start/stop ambiente test
- **Cron giornaliero UTC fisso `0 20 * * *`** (~21:00 IT, deriva ora legale accettata) → `infra/scripts/test-stop`
  (idempotente).
- `workflow_dispatch` → `test-start` (e `test-stop` manuale). OIDC ruolo test.

## 2. Dockerfile dei servizi (oggi assenti)

Per `core` e `fatture` (pattern replicabile da `new-application`, UC 0046): `Dockerfile.jvm` e
`Dockerfile.native`, **ARM64**, con **`curl` incluso** (health check ECS del modulo `microsaas_app`:
`curl -fsS http://localhost:<porta>/q/health/live`). Build native via profilo `native` di Quarkus
(container-build non necessario: il runner è già Linux ARM64).

## 3. Adattamenti al codice eseguibile

- **`SqsMessageQueues`** (`services/commons`) — profilo cloud: (a) **catena di credenziali di default**
  quando manca l'endpoint override (task role IAM); creds statiche "local/local" solo con endpoint locale;
  (b) **prefisso nomi coda** da `appgrove.sqs.queue-prefix` (← `APPGROVE_SQS_QUEUE_PREFIX`, vuoto in locale);
  (c) regione da `APPGROVE_SQS_REGION`. **Con test JUnit** delle tre varianti.
- **Comando `migrate`**: entrypoint command-mode (stesso pattern di `sync-pricing` in `CoreMain`) che esegue
  le migrazioni Flyway e termina, per il task one-shot — per core e fatture.
- **Frontend**: source map `hidden` nella build di produzione; `VITE_BUILD_SHA` già consumato dal codice
  (`main.tsx` → error-reporter), va solo passato dalla CI.

## 4. Terraform

- **Task one-shot Flyway/sync-pricing**: quanto serve nel modulo `microsaas_app` (o env root) per lanciare
  `aws ecs run-task` con command override in VPC (riuso della task definition del servizio dove possibile).
- **`config.json` per-ambiente come output** degli env root (per SPA: `env`, `authBaseUrl`, `coreBaseUrl`,
  `errorIngestUrl` = rotta di `platform_shared/error_ingest.tf`; i campi **Cognito** restano placeholder →
  differiti a UC 0015).
- **Wrapper estesi** (restano l'unica fonte di verità): `plan <env> --out <file>` e `up <env> --plan <file>`
  per il flusso prod "piano salvato → apply del salvato"; guardrail esistenti invariati.
- Aggiornare i test `terraform test` dei moduli se il modulo cambia.

## 5. Strumenti

- **actionlint** sui workflow: aggiunto a `infra/scripts/check` (se installato) → entra in `run-tests.sh infra`.
- tflint ruleset AWS (`--init` in CI), checkov e Infracost cablati come sopra; oasdiff installato in CI.

## Fuori scope

Definizione infra (UC 0003/0004/0055), observability (UC 0006), skill `new-application` (UC 0046),
Cognito/auth reale (UC 0015/0016), account Paddle sandbox/production (UC 0001), attivazione ambienti.

## Requisiti di test

- `actionlint` verde su tutti i workflow.
- Test JUnit per `SqsMessageQueues` (credenziali/prefisso/regione) e per il comando `migrate`.
- Build locale dell'immagine JVM di `core` verificata (`docker build` + presenza `curl`).
- `./run-tests.sh` verde su **tutte** le aree toccate (backend, frontend, infra; compliance per il gate).
- Nessun test eseguibile per i workflow in sé (ambienti spenti): la prima run live è differita e tracciata.

## Decisioni differite da tracciare (gate di chiusura — nulla si perde)

1. **Prima esecuzione live della pipeline + configurazione repo GitHub** (Environment `prod` con required
   reviewer, branch protection con check required su `verify-pr`, secret `INFRACOST_API_KEY`, verifica
   fatturazione runner ARM64 su repo privato) → UC di attivazione ambienti (individuare in `_INDEX.md`;
   altrimenti `docs/_BACKLOG.md`).
2. **Campi Cognito nel `config.json`** → UC 0015.
3. **Secret Paddle per-env** (sandbox/production) → UC 0001; finché assenti, `sync-pricing` in CI = skip con warning.
4. **Secret/ambiente L3** (`APPGROVE_L3_*`) → UC 0001/0029; finché assenti, smoke L3 = auto-skip registrato.
5. **Generazione workflow per nuove app** (matrix servizi da aggiornare a ogni `service-add`) → UC 0046.

Inoltre: spuntare nel file di UC 0005 i punti aperti che questa change risolve; aggiornare
`docs/_COSTI-AWS.md` (CI ≈ $0) se necessario.

## Gate privacy (UC 0031)

La pipeline muove artefatti, non dati personali (spec §7): atteso **nessun segnale**; `privacy-scan`
eseguito comunque prima del commit.
