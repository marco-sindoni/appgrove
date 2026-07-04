# Change 0028: Framework export/erasure GDPR (contratto per-app, job async, ZIP S3 presigned, purge con audit)

**Branch**: `change/0028-use-case-0032-framework-export-erasure`
**Aree**: `services/commons`, `services/core`, `services/fatture`, `dev/` (stack locale: ElasticMQ/MinIO/config)
**Data**: 2026-07-04
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/08-compliance-gdpr/0032-framework-esportazione-cancellazione.md](../../docs/usecases/08-compliance-gdpr/0032-framework-esportazione-cancellazione.md)
**Tocca dati personali?**: Sì — il framework legge/cancella dati personali di piattaforma e app; introduce
nuove entità (job di export, audit delle purge) che contengono riferimenti a tenant/utente. Si applica il
gate privacy/RoPA di step-03 (UC 0031: scanner + co-pilota → manifesto/RoPA + classificazione MAJOR/MINOR).

## Problema / Obiettivo

Implementare il framework GDPR di **esportazione** (diritto d'accesso/portabilità, art. 15/20) e
**cancellazione** (art. 17) deciso in #13 D22, #06 H-19, #13 L70/L71 e #09 F31: contratto obbligatorio
per-app, orchestrazione asincrona con avanzamento consultabile, consegna sicura via ZIP cifrato con link
firmato a scadenza, cancellazione completa con prova di audit. Tutto funzionante **in locale** su
MinIO/ElasticMQ (il runbook dello UC lo richiede esplicitamente).

## Scope

Decisioni prese al gate di chiarimento (dialogo change 0028):

1. **Architettura export = code per-servizio + coda risultati** (simmetrica alla purge #06 H-19; niente
   chiamate HTTP dirette tra microservizi). Flusso:
   - il core, alla richiesta, crea il record **`export_job`** (id, tipo *account completo* | *singola app*,
     `requested_at`, `requested_by`, stato `QUEUED→RUNNING→COMPLETED/FAILED`, progress "step X di N") e
     pubblica un messaggio sulla coda export di ogni servizio coinvolto (`gdpr-export-<app_id>`);
   - ogni servizio (core compreso, per i dati di piattaforma) ha un **worker riusabile** (in `commons`,
     stesso pattern di `PaddleWebhookConsumer`: `@Scheduled` + AWS SDK v2) che consuma la propria coda,
     esegue il **proprio** `AppDataContract.exportData(scope)` in-process, carica il frammento su
     S3/MinIO (`jobs/<job_id>/<app_id>.json`) e notifica esito/step sulla coda **`gdpr-export-results`**;
   - il core consuma i risultati, aggiorna il progress e, quando tutti i servizi hanno finito, assembla lo
     **ZIP** sul bucket export (cifrato lato server; auto-cancellazione a 7 giorni via lifecycle nel cloud,
     rimandata a UC 0003) e genera il **presigned URL con scadenza 7 giorni**.
2. **Il contratto resta a livello di account**: `GdprScope(tenantId)` invariato. L'export del profilo del
   singolo utente è capability del core, rimandata a UC 0033 (tracciato).
3. **Niente Terraform in questa change**: tutto gira in locale su MinIO/ElasticMQ; le risorse cloud
   (bucket SSE+lifecycle, code+DLQ, bus EventBridge+regola) sono tracciate su UC 0003/0004 con i nomi
   definitivi delle code.
4. **`manifest()` resta derivato dalle annotazioni `@PersonalData`** (inventario tecnico; coerenza con i
   manifesti YAML già garantita dal check bloccante di UC 0030). Diventa il metro del test di copertura
   (nessun dato orfano). L'eventuale sfoltimento degli attributi duplicati è tracciato su UC 0030.
5. **Endpoint HTTP esposti: solo quelli dell'export**, sotto `/api/platform/v1/gdpr/*` nel core:
   richiesta export (account completo o singola app), stato/progress del job, recupero del link firmato.
   Solo autenticazione + ownership (job visibili solo al tenant del JWT), **senza** `@RequiresEntitlement`
   (esenzione #09 F31). **Nessun endpoint di cancellazione**: trigger utente rimandati a UC 0033 (recesso
   per-app) e UC 0035 (eliminazione account con grazia 14 giorni), entrambi tracciati.

Completano lo scope:

6. **Purge guidata dall'evento**: consumer per-servizio sulla coda `tenant-purge-<app_id>` che invoca il
   proprio `purgeData(scope)` e registra l'**audit** (record persistito: tenant, app, quando, conteggio
   cancellati per entità — la prova richiesta da #13 L70 — oltre al log strutturato). **Orchestrazione
   account-level** nel core: funzione interna che esegue la purge di piattaforma e pubblica l'evento
   `tenant.offboarded`; in locale il publisher invia direttamente alle code purge dei servizi (il bus
   EventBridge arriva con UC 0004, stessa astrazione).
7. **Client S3 in `commons`** (upload frammenti/ZIP, presigned URL, cifratura lato server) con dipendenze
   AWS SDK `s3`; config `%dev` verso MinIO. Stack locale aggiornato: code per-servizio + DLQ in
   `dev/elasticmq.conf` (sostituiscono `gdpr-export`/`tenant-purge` singole), creazione bucket export
   all'avvio, variabili in `dev/.env`.
8. **Test di copertura compliance (#13 L74)** per core e fatture: export e purge coprono **ogni** entità
   con campi `@PersonalData` (metro: `manifest()`), così una futura entità dimenticata fa fallire i test.

## Fuori scope

- UI self-service e qualunque schermata (UC 0033); console admin diritti (UC 0034).
- Endpoint di cancellazione account/recesso per-app, grazia 14 giorni, job schedulati (UC 0033/0035).
- Terraform/risorse cloud (UC 0003/0004); auto-cancellazione a 7 giorni su MinIO locale (vale il vincolo
  vero: la scadenza del link firmato).
- Estensione per-utente di `GdprScope` (rimandata; vedi UC 0033).
- Sfoltimento attributi `@PersonalData` / verifier YAML (UC 0030).
- Ticket privacy automatico su export FAILED (#13 D21) → dipende dal ticketing (UC 0034): il fallimento
  resta visibile su stato job + log/DLQ; tracciare il collegamento in UC 0034 se non già presente.

## Criteri di accettazione

- [ ] In locale (stack dev avviato): richiesta di export via endpoint → job `QUEUED→RUNNING→COMPLETED` con
      progress consultabile → ZIP su MinIO → link firmato scaricabile che scade dopo 7 giorni, contenente i
      dati di piattaforma e di fatture del solo tenant richiedente.
- [ ] Invocando l'orchestrazione account-level: dati di piattaforma e app del tenant cancellati (hard),
      record di audit persistiti per ogni servizio, nessun dato orfano (test di copertura sul manifesto).
- [ ] Test anti-regressione F31: gli endpoint export rispondono anche con subscription non attiva
      (nessun `@RequiresEntitlement`) — il guard rimandato da UC 0027.
- [ ] Ownership: un tenant non vede né scarica i job di un altro tenant (test negativo).
- [ ] `./run-tests.sh backend` (e `compliance`) verdi; nessun aggiornamento a `run-tests.sh` necessario
      (nessun modulo aggiunto/rimosso).

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT**: gli endpoint export ricavano il tenant dal claim `tenant_id`; il job e i
  messaggi in coda portano lo scope esplicito **derivato da quel claim** (i worker girano senza richiesta
  utente, come l'esistente `FattureDataContract` via JDBC diretto).
- **Filtro row-level**: ogni query dei contratti e dei repository dei job filtra per tenant (`WHERE
  tenant_id = ?`); i contratti bypassano Hibernate ma applicano il filtro esplicito (pattern esistente).
- **Modulo `microsaas_app`**: N/A qui (niente infra); i fabbisogni sono tracciati su UC 0003/0004.
- **Logging strutturato**: worker, orchestratore e consumer loggano sempre `tenant_id`, `app_id` e, dove
  esiste, `user_id` (richiedente).

## Requisiti di test

- Integration (core): ciclo di vita export job (stati, progress, aggregazione, ZIP, presigned, FAILED se
  un frammento fallisce); orchestrazione account-level; consumer purge con audit. Code e storage mockati
  in-memory nei test (pattern `InMemoryWebhookQueue`), niente ElasticMQ/MinIO nei test automatici.
- Integration (fatture): worker export su coda + purge su evento, scoped al tenant.
- Compliance (#13 L74): copertura export+purge di ogni entità `@PersonalData` (core e fatture).
- Security: F31 (risposta senza entitlement) + ownership cross-tenant negata.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (nuove capability; le code locali `gdpr-export`/`tenant-purge` vengono sostituite dalle varianti per-servizio, non ancora consumate da nessuno) |
| Contratto cross-area | Sì — nuovo contratto asincrono tra servizi (formato messaggi export/purge/risultati e layout frammenti su S3), documentato in `commons` |
| Version bump | minor |
