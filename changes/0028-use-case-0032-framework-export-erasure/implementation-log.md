# Implementation Log — Change 0028: Framework export/erasure GDPR (UC 0032)

**Branch**: `change/0028-use-case-0032-framework-export-erasure`
**Aree**: `services/commons`, `services/core`, `services/fatture`, `dev/` (stack locale)
**Completata**: 2026-07-04

## File modificati

| File | Azione |
|---|---|
| `services/commons/pom.xml` | Modificato — dipendenze quarkus-scheduler + AWS SDK (sqs, s3, url-connection) |
| `services/commons/.../messaging/MessageQueues.java` + `SqsMessageQueues.java` | Creati — accesso alle code per nome (SQS/ElasticMQ) |
| `services/commons/.../storage/ExportStorage.java` + `S3ExportStorage.java` | Creati — storage bucket export + link firmati (S3/MinIO) |
| `services/commons/.../gdpr/GdprQueues.java` | Creato — naming code/chiavi (`gdpr-export-<app>`, `tenant-purge-<app>`, `gdpr-export-results`) |
| `services/commons/.../gdpr/ExportRequestMessage.java`, `ExportResultMessage.java`, `TenantPurgeMessage.java` | Creati — contratto messaggi cross-servizio |
| `services/commons/.../gdpr/GdprExportWorker.java` | Creato — worker riusabile: consuma la coda export del servizio, esegue `exportData`, carica il frammento, notifica l'esito |
| `services/commons/.../gdpr/TenantPurgeConsumer.java` | Creato — consumer riusabile: consuma la coda purge, esegue `purgeData`, registra l'audit |
| `services/commons/.../gdpr/GdprPurgeAuditWriter.java` | Creato — scrittura JDBC dell'audit purge (tabella da config, nome validato) |
| `services/commons/.../gdpr/DataManifests.java` | Creato — helper condiviso manifesto ← annotazioni `@PersonalData` |
| `services/commons/src/test/.../InMemoryMessageQueues.java`, `InMemoryExportStorage.java` | Creati — mock riusabili nel test-jar (con simulazione redrive/DLQ) |
| `services/core/.../db/migration/V5__gdpr_export_erasure.sql` | Creato — tabelle `gdpr_export_job`, `gdpr_export_job_item`, `gdpr_purge_audit` |
| `services/core/.../gdpr/GdprExportJob.java`, `GdprExportJobItem.java`, enum, repository | Creati — record del job (tenant-scoped, discriminator) |
| `services/core/.../gdpr/GdprExportService.java` | Creato — crea job+item e pubblica sulle code per-servizio (contesto REST, tenant dal JWT) |
| `services/core/.../gdpr/GdprJobStore.java` | Creato — scritture JDBC del consumer (fuori richiesta, come `SubscriptionWriter`) |
| `services/core/.../gdpr/GdprExportResultsConsumer.java` | Creato — aggiorna progress, aggrega lo ZIP finale, chiude il job |
| `services/core/.../gdpr/PlatformDataContract.java` | Creato — contratto GDPR della piattaforma (accounts/users/invitations, JDBC) |
| `services/core/.../gdpr/TenantOffboarding.java` | Creato — orchestrazione account-level: fan-out `tenant.offboarded` → code purge |
| `services/core/.../gdpr/GdprResource.java` + `GdprDtos.java` | Creati — `POST/GET /api/platform/v1/gdpr/exports` + `/download` (senza gate, F31) |
| `services/core/src/main/resources/application.properties` | Modificato — config audit-table + S3/MinIO `%dev` |
| `services/core/.../META-INF/openapi/openapi.{yaml,json}` | Rigenerati — nuovi endpoint export |
| `services/fatture/.../db/migration/V3__gdpr_purge_audit.sql` | Creato — tabella audit purge nello schema dell'app |
| `services/fatture/.../FattureDataContract.java` | Modificato — usa l'helper condiviso `DataManifests` |
| `services/fatture/src/main/resources/application.properties` | Modificato — config code/S3/audit `%dev` |
| `dev/elasticmq.conf` | Modificato — code per-servizio + risultati (+DLQ); rimosse le code singole mai consumate |
| `dev/docker-compose.yml` | Modificato — healthcheck MinIO + job one-shot `minio-init` (bucket idempotente) |
| `dev/.env.example` (e `dev/.env` locale) | Modificato — `GDPR_EXPORT_BUCKET` |
| Test: `core/.../gdpr/*Test.java` (4 classi + 2 mock), `fatture/GdprPipelineTest.java` (+2 mock), `TestData.java` | Creati/Modificato — vedi sezione Test |
| `CLAUDE.md`, `.claude/skills/new-change/SKILL.md`, `docs/usecases/*` (rimandi), `requirements.md` | Già committati con i requirements (regola lingua + punti differiti) |

## Cosa è stato fatto

Implementato il framework GDPR di UC 0032 secondo l'architettura decisa al gate di chiarimento
(code per-servizio, nessuna chiamata HTTP tra microservizi): il core crea il record `gdpr_export_job`
e pubblica le richieste sulle code `gdpr-export-<app_id>`; ogni servizio con `AppDataContract`
(piattaforma inclusa) le consuma con un worker riusabile di `commons`, carica il frammento JSON su
S3/MinIO e notifica `gdpr-export-results`; il core aggrega gli esiti, assembla lo ZIP e lo serve con
un link firmato che scade a 7 giorni, solo in-app. L'erasure è guidata dall'evento `tenant.offboarded`
(`TenantOffboarding` = purge piattaforma + `purgeData` di ogni app attivata via code `tenant-purge-*`),
con audit persistito per servizio. Tutto gira in locale su ElasticMQ + MinIO (bucket creato da
`minio-init` all'avvio dello stack).

## Decisioni prese

1. **Chi ha richiesto l'export = `created_by`** (colonna di audit standard, valorizzata dal `sub` del
   JWT): nessuna colonna dedicata → nessun nuovo dato personale, riusa il precedente UC 0030 (le
   colonne di audit non entrano nei manifesti).
2. **Anche la purge della piattaforma passa dalla sua coda** (`tenant-purge-platform`): un solo
   percorso di codice per tutti i servizi, audit uniforme.
3. **La purge non tocca** `gdpr_purge_audit` (è la prova, #13 L70; senza dati personali) né
   `webhook_event` (audit pipeline dichiarato no-PII da UC 0025); cancella anche subscription e job
   di export del tenant. L'export include le righe soft-deleted (art. 15 = tutto ciò che è conservato).
4. **SSE e lifecycle 7gg sono proprietà del bucket cloud** (tracciate su UC 0003); in locale MinIO non
   le applica, il vincolo esercitabile (presigned 7gg) è nel codice ed è testato.
5. **Export fallito → job FAILED e messaggio confermato** (niente retry infinito; errori infra →
   redrive/DLQ); il ticket privacy automatico su FAILED dipende dal ticketing (UC 0034).
6. **Endpoint export riservati a `owner`/`admin`** (diritto dell'account); l'export del profilo del
   singolo utente è tracciato su UC 0033.
7. **Iniezione lazy (`Instance<>`) di code/storage nei worker di commons**: i servizi senza
   `AppDataContract` (auth-local) restano inerti e non richiedono i bean (fix emerso dalla suite).
8. **Punto aperto `DataManifest`↔YAML risolto**: `manifest()` resta derivato dalle annotazioni
   (inventario tecnico; coerenza col YAML garantita dal check UC 0030); registrato su UC 0032/0030.

## Invarianti appgrove

- **Tenant ID solo dal JWT**: gli endpoint leggono il tenant da `CallerContext` (claim verificato);
  i messaggi in coda portano lo scope derivato da quel claim; i worker (senza JWT) usano tenant
  esplicito dallo scope, mai da input client.
- **Filtro row-level**: letture REST tenant-filtered dal discriminator (job altrui = 404, testato);
  ogni query JDBC dei contratti/store filtra esplicitamente per tenant.
- **Modulo `microsaas_app`**: nessuna infra creata qui; fabbisogni tracciati su UC 0003/0004.
- **Logging strutturato**: worker/consumer/orchestratore loggano `tenant_id`, `app_id`, `job_id` e,
  dove esiste, `user_id`.

## Note per il revisore

- **Contratto cross-servizio nuovo**: formato dei messaggi (`ExportRequestMessage`,
  `ExportResultMessage`, `TenantPurgeMessage`) e layout dei frammenti S3
  (`jobs/<job_id>/<app_id>.json`, ZIP `jobs/<job_id>/export.zip`) definiti in `commons.gdpr` —
  qualunque futura app li eredita dal worker riusabile.
- **Nomi delle code cambiati** in `dev/elasticmq.conf` (per-servizio): le vecchie `gdpr-export` /
  `tenant-purge` non erano consumate da nessuno; serve un `docker compose up` (o `dev up`) per
  ricreare ElasticMQ con le code nuove.
- **Decisioni differite tracciate** (regola CLAUDE.md): UC 0003 (bucket SSE+lifecycle), UC 0004
  (code+DLQ per-servizio, bus EventBridge, regola `tenant.offboarded`), UC 0030 (sfoltimento
  attributi `@PersonalData`), UC 0033 (export per-utente del profilo, trigger recesso per-app, UI
  export), UC 0035 (eliminazione account con grace 14gg sopra `TenantOffboarding.offboard`).
  Punto aperto F31 di UC 0027: chiuso qui con `GdprGateExemptionTest`.
- **Gate privacy (UC 0031)**: scanner segnala 10 segnali (dipendenze AWS SDK in commons + host
  `minio` nel compose) + 3 tabelle nuove valutate manualmente. Classificazione confermata dallo
  sviluppatore: **MINOR**, componente piattaforma core — nessuna nuova categoria/finalità/base/
  retention (il framework attua diritti già dichiarati; export ZIP già disciplinato da #13 E23),
  nessuna modifica a manifesti/RoPA, **nessun nuovo sub-processor** (fornitore = AWS, già previsto;
  nota per UC 0002: includere AWS S3/SQS nella lista sub-processor quando nascerà).
- `run-tests.sh` invariato: nessun modulo aggiunto/rimosso, i nuovi test girano in `mvn test`.

## Test

- **core** (`mvn test`, Dev Services Postgres 17): `GdprExportApiTest` (ciclo job: richiesta → worker
  → ZIP → presigned ~7gg; fan-out alle app attivate anche con subscription canceled; FAILED su esito
  negativo; download 409 se non pronto; ownership cross-tenant = 404; validazioni 400/404),
  `GdprGateExemptionTest` (guardia F31: nessun `@RequiresEntitlement` + export risponde senza alcuna
  subscription), `PlatformGdprContractTest` (copertura #13 L74: export copre ogni entità del
  manifesto; purge fisica scoped senza orfani), `TenantOffboardingTest` (fan-out piattaforma+app,
  purge con audit, idempotenza).
- **fatture** (`mvn test`): `GdprPipelineTest` (worker: frammento su storage + esito con step;
  purge: erasure fisica + audit nello schema app); `GdprContractTest` esistente resta verde.
- **compliance** (`./run-tests.sh compliance`): manifesti + freshness RoPA verdi (nessuna modifica).
- Suite completa: `./run-tests.sh backend compliance` → prima esecuzione rossa su **auth-local**
  (dipendenze insoddisfatte dei worker commons nel profilo test) → fix con iniezione lazy
  (decisione 7) → **verde**.

## Stato criteri di accettazione

- [x] Export in locale end-to-end: job `QUEUED→RUNNING→COMPLETED`, progress consultabile, ZIP su
      storage, link firmato con scadenza 7 giorni, dati del solo tenant richiedente (integration test;
      in dev reale gira su ElasticMQ/MinIO con le stesse classi, mock solo nei test).
- [x] Orchestrazione account-level: purge piattaforma + app con audit persistito, nessun dato orfano
      (test copertura manifesto).
- [x] Guardia F31: endpoint export senza gate entitlement, risponde senza subscription.
- [x] Ownership: job invisibili agli altri tenant (404).
- [x] `./run-tests.sh backend` e `compliance` verdi; `run-tests.sh` invariato (nessun modulo nuovo).
