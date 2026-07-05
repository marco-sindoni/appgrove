# Implementation Log — Change 0033: Modulo `microsaas_app` + wrapper scripts

**Branch**: `change/0033-use-case-0004-modulo-microsaas-app`
**Aree**: infra (+ `run-tests.sh`, docs)
**Completata**: 2026-07-05

## File modificati

| File | Azione |
|---|---|
| `infra/modules/microsaas_app/{versions,variables,data,ecr,logs,sqs,eventbridge,db,iam,ecs,api,outputs}.tf` | Creato — il modulo per-servizio (invariante #3) |
| `infra/modules/microsaas_app/tests/plan.tftest.hcl` + `tests/fixtures/double/main.tf` | Creato — `terraform test` (provider mock, offline) |
| `infra/modules/platform_shared/db_bootstrap.tf` + `lambda/db_bootstrap.py` | Creato — Lambda bootstrap ruolo+schema via Data API |
| `infra/modules/platform_shared/sqs.tf` | Creato — coda condivisa `gdpr-export-results` + DLQ |
| `infra/modules/platform_shared/{aurora,versions,outputs}.tf` | Modificato — Data API abilitata, provider archive, output di aggancio |
| `infra/envs/{test,prod}/main.tf` | Modificato — `locals.microsaas_shared` + istanze `app_platform`/`app_fatture` (con marker per gli script) |
| `infra/envs/{test,prod}/.terraform.lock.hcl` | Modificato — provider `random`/`archive` |
| `infra/scripts/{service-add,service-remove,test-start,test-stop,_scale-test}` | Creato — wrapper con `--help` e guardrail (#06 25) |
| `infra/scripts/check` | Modificato — nuovo passo `terraform test` sui moduli (3/5) |
| `run-tests.sh` | Modificato — descrizione area infra aggiornata |
| `infra/README.md`, `docs/_COSTI-AWS.md`, `docs/_EVOLUZIONI-DEVOPS.md` (E21) | Modificato — docs |
| `docs/usecases/…/{0004,0005,0006}-*.md`, `10-skills-tooling/0046-*.md`, `_INDEX.md` | Modificato — decisioni differite tracciate; 0004 → ✅ |
| `.gitignore` | Modificato — esclusi gli zip Lambda generati da `archive_file` |

## Cosa è stato fatto

Realizzato il **modulo Terraform `microsaas_app`** (invariante #3): un'istanza crea ECR (lifecycle 10
immagini, scan on push), service/task ECS Fargate 0.25 vCPU/0.5 GB ARM64 (Spot test / on-demand prod)
registrato su Cloud Map (record SRV), route `ANY /api/<app_id>/v1/{proxy+}` sull'API condivisa via VPC
Link, ruolo+schema Postgres least-privilege (password in Secrets Manager, SQL idempotente via Lambda
`db-bootstrap` con Data API), code `gdpr-export-<app_id>`/`tenant-purge-<app_id>` + DLQ cifrate, regola
EventBridge `tenant.offboarded` → coda purge (payload = solo `detail`, come in locale), log group con
retention 7/30 gg. `platform_shared` è stato esteso con la Lambda `db-bootstrap` e la coda condivisa
`gdpr-export-results`+DLQ. Istanziati **`platform`** (schema `platform`, ruolo orchestratore GDPR) e
**`fatture`** in test e prod (nessun apply: attivazione differita). Aggiunti i 4 wrapper script e i
`terraform test` del modulo, agganciati a `check`/`run-tests.sh`.

## Decisioni prese

1. **Prefisso `appgrove-<env>-` sui nomi fisici delle code SQS** — vincolo AWS scoperto in
   implementazione: test e prod convivono nello stesso account/regione e i nomi SQS devono essere
   unici. I nomi logici restano quelli locali (`GdprQueues`); il prefisso arriva ai servizi come env
   var `APPGROVE_SQS_QUEUE_PREFIX` (vuota in locale). Requirements aggiornati (commit di revisione).
2. **Lambda `db-bootstrap` fuori VPC con Data API Aurora** (`enable_http_endpoint`, gratuita): SQL via
   API firmata, niente ENI/driver Postgres impacchettati; retry integrato sul risveglio del cluster in
   pausa. Ruolo = nome schema; `REVOKE CREATE ON SCHEMA public` come difesa in profondità.
3. **ECR per-ambiente** (`appgrove-<env>-<app_id>`): stesso vincolo di unicità nomi nell'account; la
   promozione test→prod delle immagini è tracciata su UC 0005.
4. **ARM64/Graviton** nella task definition (~-20% di costo; Fargate Spot supporta ARM64): la pipeline
   dovrà buildare immagini native ARM64 (tracciato su UC 0005).
5. **Cloud Map con record SRV** (ip+porta): è il formato che l'API Gateway usa via DiscoverInstances;
   le porte restano quelle del profilo `%dev` (platform 8080, fatture 8081) per simmetria col locale.
6. **`ignore_changes = [desired_count]`** sul service ECS: `test-start`/`test-stop` pilotano lo scale
   fuori da Terraform e un apply non deve riaccendere ciò che il cron ha spento.
7. **`service-remove` non cancella schema/ruolo/segreto DB**: pulizia manuale documentata nel
   `--help` (scelta prudente concordata al gate di chiarimento).

## Invarianti appgrove

- **Modulo `microsaas_app`**: la change lo crea e lo esercita con due istanze reali; `service-add`
  genera blocchi `module`, mai infra su misura.
- **Isolamento dati**: ruolo Postgres per-servizio con privilegi solo sul proprio schema (#05 11) —
  difesa in profondità sotto il filtro row-level applicativo (non toccato).
- **Logging strutturato**: log group per-servizio con retention esplicita e cifratura di default; le
  convenzioni MDC restano in `services/commons` (non toccate).
- **Tenant dal JWT**: non toccato (nessun codice applicativo in questa change).

## Note per il revisore

- **Contratto servizio ↔ infra** (cross-area, da tenere d'occhio in UC 0005): la task definition fissa
  le env var `QUARKUS_DATASOURCE_{JDBC_URL,USERNAME,PASSWORD}` (password iniettata dal segreto),
  `APPGROVE_SQS_QUEUE_PREFIX`, `APPGROVE_SQS_REGION`, `APPGROVE_GDPR_EXPORT_BUCKET`. `services/commons`
  oggi NON legge prefisso/regione/credenziali cloud: adeguamento tracciato su UC 0005.
- **Decisioni differite tracciate in questa change**: cron `test-stop` + `workflow_dispatch`
  `test-start` (→ UC 0005); adeguamento cloud client SQS di commons (→ UC 0005); promozione immagini
  ECR per-env + tagging + build ARM64 (→ UC 0005); widget/allarmi su log group e DLQ (→ UC 0006);
  generazione blocco `module` dalla skill `new-application` (→ UC 0046); contratto evento
  `tenant.offboarded` — `source`/shape del `detail` (→ UC 0032/0035, annotato in UC 0004); rotation
  automatica credenziali DB per-app (→ `_EVOLUZIONI-DEVOPS` E21). Il punto aperto "code SQS + bus"
  di UC 0004 è chiuso (annotato ✅ nel file dello use case). CORS e IAM-auth proxy restano differite
  come già tracciato (UC 0004+0015, UC 0014).
- La regola EventBridge filtra il solo `detail-type = "tenant.offboarded"`; quando nascerà il
  publisher cloud (UC 0032/0035) valutare se stringere su `source`.
- `aws_lambda_invocation` ri-esegue il bootstrap quando cambia l'input (idempotente); il primo apply
  con cluster in pausa può impiegare ~15-20 s in più (retry nella Lambda, timeout 150 s).
- **Gate privacy (UC 0031): nessun segnale** (`npm run privacy-scan` → exit 0). La change crea solo
  infrastruttura: nessun dato personale, nessun nuovo sub-processor (tutti servizi AWS già in uso).

## Test

- **`terraform test` sul modulo** (`tests/plan.tftest.hcl`, provider AWS mock → offline): 3 run verdi —
  (1) app di default in test: nomi code con prefisso ambiente, DLQ, SSE, retention 7 gg, route
  `ANY /api/demo/v1/{proxy+}`, FARGATE_SPOT, 1 task 256/512, schema `app_demo`, segreto
  `appgrove/test/demo/db`, regola `tenant.offboarded`; (2) core in prod: schema `platform`, retention
  30 gg, FARGATE on-demand; (3) due istanze nello stesso env: risorse disgiunte per app_id (safety di
  `service-remove`).
- **Suite area infra** (`./run-tests.sh infra` → `scripts/check`): fmt ✓, validate ✓ su
  bootstrap/global/envs/test/envs/prod, terraform test ✓ (3/3), checkov ✓ (364 passed, 0 failed,
  soppressioni documentate inline), tflint non installato (salto già previsto, CI in UC 0005).
- **Prova a secco degli script**: `service-add note --port 8082` → blocchi generati in entrambi gli
  env + validate verde → `service-remove note` (conferma digitata) → albero identico all'originale.
  `bash -n` su tutti i nuovi script; `--help` funzionanti. `test-start`/`test-stop` non eseguibili
  end-to-end senza infra accesa: degradano a no-op documentato (cluster assente → exit 0).

## Stato criteri di accettazione

- [x] `terraform validate` verde su `envs/test` e `envs/prod` con le istanze `platform` e `fatture`; `check` verde.
- [x] `terraform test` sul modulo verde e agganciato a `run-tests.sh` (area infra, via `scripts/check`).
- [x] Nomi logici code = locale (`dev/elasticmq.conf`); nome fisico = prefisso `appgrove-<env>-` + logico.
- [x] I 4 wrapper rispettano il pattern UC 0003 (`--help`, guardrail); `test-stop` idempotente/no-op a lista vuota.
- [x] Ruolo DB per-servizio least-privilege (grant solo sul proprio schema, `REVOKE CREATE` su public); schema creato vuoto (tabelle a Flyway).
