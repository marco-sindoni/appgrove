# UC 0004 — Modulo `microsaas_app` + wrapper scripts (bootstrap/plan/up/down/service-add|remove/test-start|stop)

**Area**: 02-devops-infra · **Fase**: 1 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0003](0003-fondamenta-terraform.md) (foundation)
**Fonte decisioni**: #06 J (modulo), #06 §25 (wrapper), #01 (invariante modulo), #07 (lifecycle servizi)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [06-infra-iac](../../06-infra-iac.md), [01-architettura](../../01-architettura.md), [07-devops-cicd](../../07-devops-cicd.md)

## 1. Obiettivo / Scope
Realizzare il **modulo Terraform `microsaas_app`** — l'**invariante #3**: una nuova app = istanziare il modulo, non infra
bespoke — e i **wrapper script** che lo pilotano.
**Incluso**: un'istanza del modulo crea **ECR repo**, **ECS service/task** (Fargate + Cloud Map), **route API** `/api/<app_id>/v1/*`,
**ruolo DB + schema `app_<app_id>` vuoto**, **coda SQS** (purge), **SSM/Secrets** (cred DB), **log group** (+ widget/allarmi base via UC 0006);
gli script `service-add`/`service-remove`, `test-start`/`test-stop`.
**Escluso**: lo scaffold del codice del servizio (UC 0046 `new-application`), la pipeline (UC 0005), le tabelle (Flyway, UC 0005/0012).

## 2. Attori & ruoli
- **`new-application`** (UC 0046): genera il blocco `module "app_<id>"` + scaffold; **non** si lancia `service-add` a mano (#07 20).
- **CI**: applica (plan in PR, apply su test al merge, apply con gate su prod al tag) (#07 G18).
- **Platform Engineer**: usa i wrapper in casi mirati.

## 3. Precondizioni
- Foundation attiva (UC 0003): VPC, state, OIDC, Route53/ACM, baseline secrets/KMS.

## 4. Flusso principale
1. Si aggiunge un blocco `module "app_<id>" { source = "../../modules/microsaas_app" ... }` con gli input (app_id, cpu/mem,
   ecc.) — generato da `new-application`.
2. `terraform apply` (via CI) crea: **ECR**, **ECS service/task** (0.25 vCPU/0.5 GB, 1 task; Spot in test, on-demand in prod #06 9/10),
   **Cloud Map** + route API GW, **ruolo DB + schema `app_<app_id>` vuoto** (#06 23), **coda SQS purge**, **SSM/Secrets** cred DB, **log group** con retention (#08 26).
3. **Flyway/CI** crea poi le tabelle nello schema (UC 0005/0012) — il modulo crea solo ruolo+grant+schema vuoto (#06 23).
4. **Rimozione**: `service-remove` toglie il blocco → `terraform destroy -target` mirato con le safety #06 K (#07 19).

## 5. Flussi alternativi / edge / errori
- **Test scale-to-0**: `test-stop` (cron giornaliero + manuale) porta desired count a 0; `test-start` (manuale) a 1 (#07 28).
- **Destroy prod**: snapshot + conferma esplicita (#06 K); test = teardown libero (`force_destroy`).
- **App single vs multi-user**: il modulo è uguale; la capability vive nel catalogo (#01 4), non nell'infra.
- **HA / ALB**: 1 task, no ALB ora (cost-min); HA = E3, ALB = E2.

## 6. Risorse & runbook
**Modulo** `infra/modules/microsaas_app/`. **Wrapper** `infra/scripts/`: `service-add <app_id>` / `service-remove <app_id>`
(istanzia/rimuove il modulo), `test-start` / `test-stop` (scale 0↔1 servizi test), oltre a quelli di UC 0003.
**Output**: risorse per-servizio coerenti e ripetibili. **Runbook**: il flusso normale è **PR→CI** (lo script genera, la CI
applica, #07 18); i wrapper restano per operazioni mirate. **Rollback**: `service-remove` + destroy mirato con safety.

## 7. Dati toccati
Nessun dato applicativo direttamente: crea lo **schema `app_<app_id>`** (vuoto) e il ruolo DB least-privilege (solo sul proprio schema, #05 11).
Log group cifrati con retention (#08 26). Manifest GDPR: i dati personali nascono nelle tabelle dell'app (UC 0051/0046), non qui.

## 8. Permessi & gate
- **Invariante #3**: il modulo **è** il pattern; nessuna infra bespoke per le app.
- **Isolamento DB**: un **ruolo per servizio**, privilegi solo sul proprio schema (#05 11) → difesa in profondità oltre al discriminator.
- **Logging strutturato**: il log group + le convenzioni MDC (#08) sono ereditate dallo scaffold (UC 0046).

## 9. Requisiti di test
- **`terraform test` sul modulo `microsaas_app`** (mattone centrale, #10 29): dato un input, il plan produce le risorse attese.
- fmt/validate/tflint/checkov + Infracost sul delta (#10 H). Verifica che `service-remove`→destroy mirato non tocchi altri servizi.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #06 22/23/§25, #01 (invariante 3), #07 18/19/20/28, #05 11, #08 26.
- **DoD**:
  1. Un'istanza del modulo crea ECR+ECS+route+ruolo/schema+SQS+SSM/Secrets+log group.
  2. Wrapper `service-add/remove`, `test-start/stop` funzionano con i guardrail #06 K.
  3. `terraform test` sul modulo verde; aggiungere/togliere un'app = aggiungere/togliere un blocco `module`.
  4. Ruolo DB per-servizio least-privilege; schema vuoto (tabelle via Flyway).

## Punti aperti / decisioni differite

- **Code SQS + bus EventBridge per il framework export/erasure (UC 0032)** — ✅ **implementato dalla change
  `0033-use-case-0004-…`** con una deviazione motivata sul naming: test e prod convivono nello stesso
  account/regione AWS, quindi i nomi fisici delle code non possono coincidere con quelli locali — sono
  **`appgrove-<env>-` + nome logico** (es. `appgrove-test-gdpr-export-fatture`). I nomi **logici** restano
  quelli di `GdprQueues`/`dev/elasticmq.conf`; il prefisso arriva ai servizi a runtime (env var
  `APPGROVE_SQS_QUEUE_PREFIX` nella task definition, vuoto in locale). Realizzato: code export+purge+DLQ
  per-app (modulo `microsaas_app`), coda condivisa `gdpr-export-results`+DLQ (`platform_shared`), regola
  EventBridge per-app su `tenant.offboarded` → coda purge, con `input_transformer` che consegna il solo
  `detail` (stesso corpo del messaggio locale).
- **Contratto dell'evento `tenant.offboarded` sul bus** _(tracciato dalla change `0033-use-case-0004-…`)_:
  la regola EventBridge filtra oggi il solo `detail-type = "tenant.offboarded"`; `source` e struttura del
  `detail` vanno confermati quando nasce il publisher cloud del core (l'astrazione che in locale invia
  direttamente alle code purge). Differito perché il publisher è codice applicativo del framework
  export/erasure. **Proprietà**: UC 0032/0035.
- **CORS sulla HTTP API condivisa** _(tracciato dalla change `0032-use-case-0055-…`)_: le SPA girano su
  `app.`/`admin.<env>` e chiamano `api.<env>` — origini diverse, quindi il browser richiede una configurazione
  CORS (`cors_configuration` sull'API creata dal modulo `platform_shared`, UC 0055). Va decisa quando esistono
  le route e il modello cookie/credenziali dell'auth BFF (UC 0015): origini ammesse = i due domini SPA,
  credenziali sì/no, header. Differita per non fissare un contratto browser↔API prima dell'auth. Proprietà:
  questo UC (wiring route) insieme a UC 0015.
  - _Aggiunta dalla change `0035-use-case-0006-…`_: nel frattempo l'**ingest errori frontend** (UC 0006,
    rotta `POST /ingest/errors` su `api.<env>`) aggira l'assenza di CORS inviando una "simple request"
    (`Content-Type: text/plain`, corpo JSON, risposta mai letta — commentato in
    `frontend/packages/error-reporter/src/reporter.ts`). Quando il CORS verrà configurato qui, valutare il
    ritorno a `application/json`.
