# Implementation Log — Change 0036: Pipeline CI/CD (use case 0005)

**Branch**: `change/0036-use-case-0005-pipeline-cicd`
**Aree**: `.github/workflows/` (nuova), `services/commons|core|fatture`, `frontend/`, `infra/`, docs
**Completata**: 2026-07-17

## File modificati

| File | Azione |
|---|---|
| `.github/workflows/verify-pr.yml` | Creato — PR: path-filter, suite `run-tests.sh` per area, security/E2E/oasdiff sempre, plan test commentato, Infracost |
| `.github/workflows/deploy-test.yml` | Creato — merge→test: build JVM/native ARM64, push ECR per-SHA, migrate→apply→health, sync-pricing, frontend |
| `.github/workflows/release-prod.yml` | Creato — tag `v*`: gate native, L3 smoke, piano salvato→approvazione→apply, promozione immagini/bundle |
| `.github/workflows/env-ops.yml` | Creato — cron 20:00 UTC `test-stop`; dispatch `test-start`/`test-stop` |
| `services/core/src/main/docker/Dockerfile.{jvm,native}` | Creati — ARM64, `curl` verificato al build |
| `services/fatture/src/main/docker/Dockerfile.{jvm,native}` | Creati — idem |
| `services/commons/.../aws/AwsClientCredentials.java` (+test) | Creato — catena credenziali di default senza endpoint override (task role IAM) |
| `services/commons/.../messaging/SqsMessageQueues.java` (+test) | Modificato — prefisso code per-ambiente + credenziali cloud |
| `services/commons/.../storage/S3ExportStorage.java` | Modificato — credenziali cloud (stesso pattern) |
| `services/core/.../billing/SqsWebhookQueue.java` | Modificato — prefisso code + credenziali cloud |
| `services/core/.../CoreMain.java` | Modificato — command-mode `migrate` (Flyway one-shot) |
| `services/fatture/.../FattureMain.java` | Creato — entrypoint con command-mode `migrate` |
| `services/{core,fatture}/.../MigrateCommandTest.java` | Creati — `@QuarkusMainTest`: `migrate` applica Flyway ed esce 0 |
| `infra/modules/microsaas_app/ecs.tf` | Modificato — env `APPGROVE_S3_REGION` nella task definition |
| `infra/modules/microsaas_app/outputs.tf` | Modificato — output `task_definition_family`, `security_group_id` (task one-shot) |
| `infra/envs/{test,prod}/main.tf` | Modificati — variabile `image_tag` (per-SHA dalla pipeline) cablata nei moduli app |
| `infra/envs/{test,prod}/outputs.tf` | Modificati — output `spa_config` (config.json per-SPA) e `ci_deploy` (marker `ci-services`) |
| `infra/scripts/plan` | Modificato — opzione `--out` (piano salvato, flusso prod) |
| `infra/scripts/up` | Modificato — opzione `--plan` (apply del piano approvato; guardrail prod conservato in terminale) |
| `infra/scripts/output` | Modificato — opzione `--json` |
| `infra/scripts/oneshot` | Creato — task ECS one-shot in VPC (migrate/sync-pricing): clona la task definition col nuovo tag, run-task, exit code |
| `infra/scripts/check` | Modificato — `tflint --init` (ruleset AWS) + step actionlint |
| `infra/scripts/service-add` / `service-remove` | Modificati — mantengono `image_tag` e le righe `ci-services` dell'output `ci_deploy` |
| `infra/.tflint.hcl` | Modificato — plugin ruleset AWS |
| `frontend/apps/{backoffice,admin}/vite.config.ts` | Modificati — source map `hidden` (#08 24) |
| `run-tests.sh` | Modificato — solo commenti (actionlint nel check infra) |
| `docs/usecases/02-devops-infra/0005-pipeline-cicd.md` | Modificato — punti aperti risolti/rimandati, file §6 |
| `docs/usecases/{0001,0015,0029,0046}` + `docs/_BACKLOG.md` + `docs/_COSTI-AWS.md` | Modificati — tracciamento decisioni differite |
| `docs/usecases/_INDEX.md` | Modificato — UC 0005 → stato implementazione |

## Cosa è stato fatto

Pipeline GitHub Actions completa (UC 0005): PR = sola verifica (path-filter #10 34 con security/E2E/oasdiff
sempre bloccanti #10 35, plan di test commentato, Infracost); merge su `main` = deploy automatico in test
(build ARM64 JVM di default / native con `[graal]` o checkbox dispatch, push ECR per-SHA, **Flyway migrate via
task one-shot in VPC prima del deploy**, apply con gli stessi wrapper del locale, health check, `sync-pricing`,
frontend con `config.json` dagli output Terraform e source map come artifact privato); tag `v*` = release prod
(gate native bloccante col messaggio guidato #07 23, smoke L3 con override motivato, **piano salvato →
approvazione → apply di quel piano**, promozione stesso SHA di immagini e bundle). Più i pezzi che gli use case
precedenti avevano differito qui: Dockerfile con `curl`, profilo cloud dei client AWS, comando `migrate`,
`VITE_BUILD_SHA`, `errorIngestUrl`, cron `test-stop`.

## Decisioni prese

1. **4 workflow, non 5**: il toggle native è la checkbox del `workflow_dispatch` di `deploy-test` (#07 9).
2. **Tagging fissato**: `<sha>` (JVM) / `<sha>-native` (GraalVM); `TF_VAR_image_tag` pilota i root env; niente
   `latest` mobile. Il gate prod controlla i soli tag `-native`.
3. **Ordine migrate→deploy garantito senza toccare il service**: `infra/scripts/oneshot` clona l'ultima task
   definition del service in una family `-oneshot` sostituendo solo l'immagine → la migrazione gira con
   l'immagine nuova PRIMA che l'apply porti il service sul nuovo tag. Al primissimo deploy di un servizio
   (nessuna task definition) la migrazione gira subito dopo l'apply (schema vuoto).
4. **Plan prod impossibile in PR per costruzione** (trust OIDC prod solo su ref di tag): in PR si commenta il
   plan di test; il plan prod è quello salvato del workflow di release.
5. **Frontend prod = copia S3 test→prod** (escluso `config.json`): "stesso bundle" senza dipendere dalla
   retention degli artifact GitHub.
6. **Workflow inerti senza `vars.AWS_ACCOUNT_ID`**: attivazione per fasi (#12), zero costi e zero rossi finché
   gli ambienti sono spenti.
7. **Adeguamento cloud esteso a tutti e tre i client AWS** (`SqsMessageQueues`, `SqsWebhookQueue`,
   `S3ExportStorage`): il requirement citava il primo, ma il pattern "credenziali local cablate" era identico
   negli altri due — stessa motivazione del punto differito ("i servizi devono girare nel cloud"), stesso
   helper condiviso (`AwsClientCredentials`). Aggiunta coerente `APPGROVE_S3_REGION` alla task definition.
8. **Suite security sempre in PR** = job dedicato quando il backend non è toccato
   (`ArchitectureTest,MultiTenancyTest,FailClosedTest,JwtTenantResolverTest,PersonalDataManifestVerifierTest`).
9. **Secret Paddle: convenzione `appgrove/<env>/paddle`** — la pipeline salta `sync-pricing` con warning finché
   il secret non esiste (creazione → UC 0001).

## Invarianti appgrove

- **Tenant dal JWT / filtro row-level**: non toccati dal codice; la pipeline li **protegge** (suite
  security/multi-tenancy mai esclusa dal path-filter e bloccante, #10 35).
- **Modulo `microsaas_app`**: esteso (output per la pipeline, `APPGROVE_S3_REGION`), nessuna infra bespoke;
  `service-add`/`service-remove` mantengono automaticamente i nuovi agganci (`image_tag`, `ci-services`).
- **Logging strutturato**: invariato; i task one-shot ereditano la task definition del service (stessi log group/MDC).
- **Secrets**: la CI non legge mai segreti applicativi (#07 26) — verifica solo l'esistenza del secret Paddle;
  OIDC senza chiavi AWS.

## Note per il revisore

- **La pipeline non è mai girata dal vivo**: ambienti spenti per scelta (attivazione per fasi). Accettazione =
  actionlint + suite verdi + build Docker locale (fatta: immagine core JVM ARM64 con `curl 7.76.1` e fast-jar).
  Il runbook di attivazione (variabile `AWS_ACCOUNT_ID`, environment `prod` con required reviewer, branch
  protection, `INFRACOST_API_KEY`, verifica fatturazione runner ARM, prima run live) è in `docs/_BACKLOG.md`
  ("Attivazione ambienti cloud").
- **Decisioni differite tracciate**: campi Cognito nel `config.json` → UC 0015; secret Paddle per-env + secret
  GitHub `APPGROVE_L3_*` → UC 0001; liste per-servizio nei workflow a ogni nuova app → UC 0046; prima run live +
  configurazione repo → `docs/_BACKLOG.md`. I punti aperti che UC 0005 possedeva sono marcati risolti nel suo file.
- **Contratti cross-area**: `ci_deploy`/`spa_config` sono il contratto Terraform↔pipeline; `oneshot` è il
  contratto pipeline↔ECS; il tagging per-SHA è il contratto build↔deploy↔promozione.
- Il job `oasdiff` usa `go install ...@latest` (runner con Go): da pinnare a una versione alla prima run live se
  si vuole riproducibilità stretta.

## Test

- **Nuovi**: `AwsClientCredentialsTest` (statiche con endpoint locale, catena di default senza),
  `SqsMessageQueuesTest` (nome fisico = prefisso+logico; locale senza prefisso), `MigrateCommandTest` in core e
  fatture (`@QuarkusMainTest @Launch("migrate")`: Flyway + exit 0).
- **Build Docker locale**: immagine `core` JVM costruita e ispezionata (`curl --version` OK aarch64,
  `/deployments/quarkus-run.jar` presente).
- **actionlint**: verde su tutti e 4 i workflow.
- **`./run-tests.sh` (tutte le aree)**: **VERDE** — backend ✓ (incl. i 6 test nuovi: surefire
  `MigrateCommandTest` core/fatture 1+1, `SqsMessageQueuesTest` 2, `AwsClientCredentialsTest` 2, 0 failure),
  frontend ✓ (vitest + Playwright e2e), infra ✓ (`scripts/check`: fmt/validate/terraform test moduli con i
  nuovi output), compliance ✓ (manifesti + RoPA allineati)
- **Gate privacy (UC 0031)**: 1 segnale dal `privacy-scan` (`queuePrefix` in `SqsMessageQueues`) —
  **classificato: non è un dato personale** (configurazione tecnica: prefisso dei nomi coda SQS). Nessun
  manifesto/RoPA da aggiornare, nessuna `@PersonalData`, nessun bump PP/ToS (né MAJOR né MINOR).

## Stato criteri di accettazione

- [x] 4 workflow (`verify-pr`, `deploy-test` con toggle native, `release-prod`, `env-ops`) — actionlint verde
- [x] OIDC senza chiavi; prod solo da tag (per costruzione della trust policy); piano salvato→approvazione→apply
- [x] Backend JVM default + native `[graal]`/dispatch; gate prod bloccante coi tag `-native` e messaggio guidato
- [x] Flyway one-shot in VPC prima del deploy (`oneshot` + comando `migrate`); ordine build→test→migrate→deploy
- [x] Frontend stesso bundle test→prod; `config.json` da output Terraform (`spa_config`, con `errorIngestUrl`)
- [x] `sync-pricing` cablata post-migrate (sandbox/production), gated sui secret
- [x] Path-filter + security/E2E/oasdiff sempre + Infracost + coverage riportata non bloccante
- [x] Dockerfile ARM64 con `curl` verificato al build; client AWS pronti per il cloud (+ test)
- [x] Cron `test-stop` 20:00 UTC + `test-start` dispatch
- [x] Decisioni differite tracciate (UC 0001/0015/0046 + backlog); prima run live tracciata come attivazione
- [ ] Prima esecuzione live della pipeline — **differita all'attivazione ambienti** (fuori scope, tracciata)
