# Implementation Log — Change 0035: Osservabilità di base (UC 0006)

**Branch**: `change/0035-use-case-0006-osservabilita-base`
**Aree**: infra · services/commons · services/core · services/fatture · frontend
**Completata**: 2026-07-06

## File modificati

| File | Azione |
|---|---|
| `infra/modules/platform_shared/observability.tf` | Creato — topic SNS critical/warning (+email), archivio audit S3 (SSE, Glacier 30gg, scadenza 12 mesi) + Firehose + ruoli (firehose→S3, logs→firehose) |
| `infra/modules/platform_shared/error_ingest.tf` + `lambda/error_ingest.py` | Creati — rotta `POST /ingest/errors` → Lambda (python, concorrenza riservata 2) → log group dedicato; payload non fidato con allowlist |
| `infra/modules/platform_shared/ingress.tf` | Modificato — access log JSON dello stage (senza IP, minimizzazione) su log group con retention; metriche dettagliate per-rotta; rimossa soppressione `CKV_AWS_76` |
| `infra/modules/platform_shared/{variables,outputs}.tf`, `db_bootstrap.tf` | Modificati — var `alert_email`, 7 output nuovi; motivazione skip X-Ray → E10 |
| `infra/modules/microsaas_app/observability.tf` | Creato — per OGNI servizio: metric filter ERROR→allarme, allarmi 5xx/latenza p95 route, allarmi DLQ, subscription filter audit→Firehose, widget dashboard (output `observability`) |
| `infra/modules/microsaas_app/ssm.tf` | Creato — parametro `/appgrove/<env>/<app_id>/log-level` (INFO, `ignore_changes` sul valore) |
| `infra/modules/microsaas_app/{api,ecs,iam,data,variables,outputs}.tf` | Modificati — parameter mapping `overwrite:` di `X-Correlation-Id`=`$context.requestId` (un header fornito dal client viene SOSTITUITO, mai preservato); secret `LOG_LEVEL` da SSM + env `APPGROVE_ENV`; health check container su `/q/health/live` (richiede `curl` nell'immagine → punto UC 0005); `alarms_enabled` (default: prod pieno / test silenziato); 4 campi nuovi in `shared`; output DLQ/ARN/`observability` |
| `infra/modules/observability/*` (nuovo modulo) | Creato — dashboard UNICA per env (overview + sezioni per-servizio generate + auth/sicurezza), allarmi Aurora (ACU/connessioni/storage), allarme Lambda ingest, regola EventBridge "task che non parte"→SNS critical, 4 query Logs Insights salvate (drill-down per-tenant/correlazione/errori/audit) |
| `infra/global/budgets.tf` | Creato — AWS Budgets $100/mese, soglie 75/90/100% reale + forecast >100% |
| `infra/envs/{test,prod}/main.tf` | Modificati — `alert_email`, 4 campi nuovi in `microsaas_shared`, blocco `module "observability"` con lista servizi (marker `obs-services`) |
| `infra/scripts/{service-add,service-remove,check}` | Modificati — gli script mantengono la lista `services` del modulo observability; `TESTED_MODULES` += observability |
| `infra/modules/microsaas_app/tests/*`, `infra/modules/observability/tests/plan.tftest.hcl` | Modificati/Creato — 2 run nuovi microsaas_app + suite nuova observability (2 run) |
| `services/commons/.../logging/MdcRequestFilter.java` | Modificato — MDC `correlation_id` (header `X-Correlation-Id`, fallback request_id), `user_id` dal claim `sub`, `app_id` dalla config `appgrove.app-id` (unica fonte, condivisa con le metriche EMF), `trace_id`/`span_id` dallo span OTel |
| `services/commons/.../audit/{AuditLogger,AuditOutcome}.java` (+test) | Creati — eventi audit = log JSON con `log_type=audit` nell'MDC (ripristinato dopo la call); regola "nessun dato personale" a javadoc |
| `services/commons/.../metrics/{EmfMeterRegistry,EmfMeterRegistryProducer}.java` (+test) | Creati — Micrometer→EMF su stdout diretto (niente `PutMetricData`); whitelist dimensioni `app_id/env/endpoint/status/service` applicata due volte; `uri`→`endpoint`; timer = `.count/.sum/.max`; i meter SENZA attività nello step non emettono righe (niente zeri pagati in ingestione) |
| `services/{core,fatture}/pom.xml`, `services/commons/pom.xml` | Modificati — `quarkus-logging-json`, `quarkus-opentelemetry`, `quarkus-micrometer`, `quarkus-smallrye-health`; commons: micrometer-core + opentelemetry-api |
| `services/{core,fatture}/src/{main,test}/resources/application.properties` | Modificati — JSON on (`%dev` off), livelli via `${LOG_LEVEL:INFO}`, exporter OTel `none`, EMF on in cloud / off in dev+test |
| `services/{core,fatture}/.../AppIdMdcFilter.java` | Eliminati — l'`app_id` nell'MDC arriva ora dalla config `appgrove.app-id` letta da commons: niente letterali per-servizio che possono divergere dalle metriche |
| `services/core/.../gdpr/*.java`, `platform/{AdminResource,UserResource,InvitationResource}.java` | Modificati — 11 azioni strumentate con `AuditLogger` (export/erasure/offboarding/restrizioni GDPR, stato app, membri/inviti — mai email nei details) |
| Test backend nuovi (commons/core/fatture) | Creati — wiring MDC (4 campi + riuso header), forma reale del log JSON, health live/ready, EMF, audit GDPR |
| `frontend/packages/error-reporter/*` (nuovo package) | Creato — `installErrorReporter`: `onerror`+`onunhandledrejection`, no-op senza endpoint, dedupe+cap 10/sessione, POST `text/plain` fire-and-forget (CORS differito UC 0004); 17 unit test |
| `frontend/apps/{backoffice,admin}/src/{main.tsx,config.ts}` (+fixture test) | Modificati — chiave config `errorIngestUrl` (vuota in locale), wiring reporter con contesto opaco da `useAuthStore` |
| `docs/compliance/manifests/platform.yaml` + `ropa.{it,en}.md` | Modificati — nuova voce `logs.frontend_errors`; RoPA rigenerato |
| `docs/usecases/…/{0004,0005,0014,0032,0046}` + `0006` + `_INDEX.md` + `docs/_COSTI-AWS.md` | Modificati — decisioni differite tracciate, punto aperto 0006 chiuso ✅, indice ✅, changelog costi |

## Cosa è stato fatto

Baseline di osservabilità completa (#08 1–33): log JSON con MDC dal JWT (invariante #4 garantita dal framework in
commons), correlation id generato all'edge (parameter mapping API GW) e propagato via header→MDC, metriche business
Micrometer→EMF dentro i log (zero `PutMetricData`), tracing OTel strumentato con export spento ($0), health
liveness/readiness, `AuditLogger` (`log_type=audit`) con 11 azioni già strumentate. Lato infra, tutto as-code: SNS
critical/warning, set minimo di allarmi (pieni in prod, silenziati in test), dashboard unica per ambiente con sezione
per-servizio generata dal modulo `microsaas_app`, Budgets $100, archivio audit subscription-filter→Firehose→S3→Glacier
(12 mesi, SOLO eventi audit), access log dell'edge e pipeline di ingest per gli errori JavaScript del frontend
(handler globale nelle due SPA). Nessun `apply`: solo fmt/validate/test/checkov.

## Decisioni prese

- **Collocazione risorse (anti-ciclo)**: punti di aggancio a monte delle app in `platform_shared` (SNS, Firehose,
  ingest, access log), consumatori a valle in un nuovo modulo `observability` (dashboard/allarmi env/query salvate);
  per-servizio nel modulo `microsaas_app` (invariante #3). Evita il ciclo platform_shared↔app↔observability.
- **Ingest cross-origin senza CORS** (CORS = punto differito UC 0004): il reporter invia una "simple request"
  `text/plain` (corpo JSON), risposta mai letta; la Lambda parsa a prescindere dal Content-Type.
- **Allarmi silenziati ≠ assenti in test**: `actions_enabled=false` (e regola EventBridge `DISABLED`) — gli allarmi
  restano visibili in console, non notificano (#08 18); override possibile (`alarms_enabled`).
- **Access log senza IP sorgente** (minimizzazione #13): il triage passa da `requestId`/`correlation_id`.
- **Timer EMF** come tre metriche `.count/.sum/.max`; EMF scritto su stdout diretto (il documento EMF deve essere
  l'oggetto JSON radice dell'evento log, incompatibile col wrapping del logging framework).
- **`aws_ssm_parameter` con `ignore_changes` sul valore**: il toggle DEBUG è operativo, l'apply non lo riporta a INFO.
- **Cifratura con chiavi gestite AWS** (SNS/Firehose/S3/log group): CMK dedicate rimandate (#06 §20bis, soppressioni
  checkov documentate inline).

## Invarianti appgrove

- **#4 logging strutturato**: garantita dal framework — `MdcRequestFilter` (commons) mette `tenant_id` (dal JWT
  verificato), `user_id` (claim `sub`, mai upn/email), `app_id` (config `appgrove.app-id`), `correlation_id`,
  `request_id`, `trace_id`/`span_id`. Test di wiring in core e fatture.
- **#1 tenant_id solo dal JWT**: l'MDC legge il claim dal JWT verificato; l'ingest errori tratta `user_id`/`tenant_id`
  del payload come contesto non fidato, solo loggato, mai usato per autorizzare.
- **#3 modulo `microsaas_app`**: filtri/allarmi/widget/subscription filter per-servizio generati dal modulo; la lista
  della dashboard è mantenuta da `service-add`/`service-remove` (marker `obs-services`).
- **#2 filtro row-level**: non toccato (nessuna query nuova).

## Verifica end-to-end in locale (e bug trovato e corretto)

Demo su stack locale reale (`app-start.sh --no-spa`, log JSON forzato sul core): login con utente seed,
`POST /invitations` con header `X-Correlation-Id: demo-e2e-cid-…` → riga JSON reale su `appgrove.audit` con
`mdc.log_type=audit`, `tenant_id`, `app_id=platform`, `correlation_id` riusato dall'header, `trace_id`/`span_id`.
La prova ha scovato una **violazione della regola "nessun dato personale nei log" (#08 5)**: `user_id` nell'MDC era l'**email** (`owner@acme.test`),
perché `MdcRequestFilter` usava `jwt.getName()` (risolve `upn`, che nei token reali è l'email); i test non lo
vedevano perché asserivano proprio il valore dell'upn. **Corretto**: `user_id` = claim **`sub`** (invariante #1),
con guardia di regressione nei test MDC di core e fatture (il token di test ha `upn≠sub` apposta). Verificato di
nuovo dal vivo: `user_id=seed-acme-owner` (opaco). Dati demo rimossi (inviti revocati), config dev ripristinata,
suite backend ri-eseguita verde dopo la correzione.

## Revisione sistematica pre-commit e fix

Su richiesta, prima del commit è girata una code review multi-agente del diff (8 angoli indipendenti:
scan riga-per-riga, guardie rimosse, contratti cross-file, riuso, semplificazione, efficienza, altitudine,
convenzioni CLAUDE.md): 35 candidati, 10 sopravvissuti alla verifica. L'angolo cross-file ha confermato
coerenti tutti i contratti tra i pezzi (pattern filtri ↔ forma log, env var ↔ config, payload ↔ allowlist,
`shared` ↔ envs ↔ fixture, marker ↔ script). **Fix applicati** (tutte le suite ri-eseguite verdi):

- **Correlation id non avvelenabile**: parameter mapping da `append:` a `overwrite:` — un header
  `X-Correlation-Id` del client viene sostituito dall'id dell'edge (test Terraform aggiornato).
- **EMF senza righe a zero** (cost-min): i meter senza attività nello step non pubblicano più (i gauge sì);
  test dedicato.
- **Reporter frontend**: `message` troncato a 4KB (un body >32KB veniva scartato in blocco dalla Lambda);
  `String(reason)` protetto da try (una reason con toString ostile non fa più lanciare l'handler globale);
  2 test nuovi.
- **Lambda ingest**: i booleani non passano più il filtro di `line`/`col` (in Python bool è int);
  `import base64` a livello modulo.
- **`app_id` centralizzato**: commons lo legge dalla config `appgrove.app-id` (stessa chiave delle metriche
  EMF) — eliminati i due `AppIdMdcFilter` per-servizio con letterale hardcoded (log e metriche non possono
  più divergere sull'attribuzione).
- **`service-remove`**: pattern di rimozione ancorato all'intera riga (non può più eliminare commenti).
- **Lingua**: sigla "PII" sostituita con "dati personali" nei testi nuovi (regola CLAUDE.md).

**Residui noti, non bloccanti**: al primo apply in test verificare che le metriche per-rotta dell'API GW
popolino davvero la dimensione `Route` (se il formato differisse, gli allarmi 5xx/latenza per-servizio
resterebbero silenziosamente senza dati) e che gli access log arrivino nel log group; duplicazioni tollerate
e annotate (retention 7/30 in due moduli, policy bucket solo-TLS, fixture di test gemelle core/fatture,
wiring reporter nei due main.tsx, `alarms_enabled` derivato in due moduli) — candidate a una change di
pulizia, non a questa.

## Note per il revisore

- **Contratti cross-area**: edge→servizi header `X-Correlation-Id` (API GW → MDC); infra→servizi env `LOG_LEVEL`
  (SSM) e `APPGROVE_ENV` (dimensione EMF `env`); frontend→infra rotta `POST /ingest/errors` + chiave config
  `errorIngestUrl` (vuota in locale = reporter spento; il valore cloud lo scrive la pipeline, punto UC 0005).
- **Gate privacy (UC 0031)**: 10 segnali = sole librerie in-process → **nessun nuovo sub-responsabile**; nuova voce
  manifesto `logs.frontend_errors` + RoPA rigenerato; log/audit 12 mesi già dichiarati (`logs.structured`).
  **Classificazione: MINOR** (platform core) — dettaglio in `requirements.md`.
- **Decisioni differite tracciate**: UC 0005 (curl nell'immagine per l'health check ECS; `VITE_BUILD_SHA`;
  `errorIngestUrl` nel config.json per-env), UC 0014 (eventi audit auth con convenzione `AuditLogger`; esclusione
  health/Swagger dall'authorizer; propagazione correlation id), UC 0004 (nota text/plain finché manca CORS),
  UC 0032 (audit log dell'esecuzione purge nel consumer condiviso), UC 0046 (reporter errori nello scaffold).
  Chiuso ✅ il punto aperto di UC 0006 (widget/allarmi per-servizio, ereditato dalla change 0033).
- **Subscription email SNS e Budgets**: la subscription email va confermata dal destinatario al primo apply;
  default `marcosindoni@gmail.com` (variabili `alert_email`/`budget_alert_email`).
- Note minori pre-esistenti, fuori scope: typecheck backoffice rosso su `e2e/privacy.spec.ts` (`.at()` vs lib
  ES2021 — non fa parte di `npm test`); warning `auth-local` su chiave `quarkus.log.console.json` senza estensione;
  Quarkus OTel duplica `traceId`/`spanId` camelCase accanto alle nostre chiavi snake_case (innocuo; valutare
  `quarkus.otel.mdc.enabled=false` in futuro).

## Test

- **infra**: `./run-tests.sh infra` verde — fmt, validate (4 root), `terraform test` 5 run microsaas_app (2 nuovi:
  pattern filtri, allarmi silenziati/pieni, SSM, correlation mapping) + 2 run del nuovo modulo observability
  (dashboard per-servizio, regola ECS DISABLED/ENABLED, query salvate); checkov 0 rilievi (2 soppressioni nuove
  documentate: SNS `CKV_AWS_26`, Firehose `CKV_AWS_241`).
- **backend**: `mvn test` dal reactor verde (commons 21, core 157, auth-local 20, fatture 28) — nuovi: wiring MDC
  (4 campi, riuso header), forma reale del log JSON (conferma pattern `{ $.level = "ERROR" }` e
  `{ $.mdc.log_type = "audit" }` col vero JsonFormatter), health live senza DB / ready col DB, EMF (forma documento,
  whitelist dimensioni: un tag `tenant_id` NON diventa dimensione), eventi audit GDPR.
- **frontend**: `./run-tests.sh frontend` verde — 154 unit (17 nuovi del reporter: no-op senza endpoint, payload,
  text/plain, dedupe/cap, unhandledrejection) + e2e Playwright (17 backoffice, 2 admin). Nessuna baseline visiva toccata.
- **compliance**: verde (parità lingue manifesti + RoPA allineato + test scanner).
- `./run-tests.sh` completo: **TUTTE le suite verdi**.

## Stato criteri di accettazione

- [x] Log JSON con `tenant_id`/`app_id`/`user_id`/`correlation_id` (+`trace_id`/`span_id`) verificati da test; nessun dato personale in chiaro (id opachi; regola a javadoc su `AuditLogger`)
- [x] `/q/health/live` senza DB, `/q/health/ready` col DB; health check ECS configurato (vincolo `curl` tracciato su UC 0005)
- [x] Risorse Terraform nuove verdi (validate/test/checkov); allarmi pieni in prod e silenziati in test; ARN DLQ esportati
- [x] Evento `AuditLogger` → JSON con `log_type=audit`; pattern del subscription filter verificato nel test Terraform; 11 azioni GDPR/admin strumentate
- [x] Handler frontend cattura e invia il payload atteso (unit test); suite frontend verde (unit + e2e)
- [x] `run-tests.sh` verde su tutte le aree; `infra/scripts/check` aggiornato (modulo observability nei TESTED_MODULES)
