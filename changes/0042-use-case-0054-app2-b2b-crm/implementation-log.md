# Implementation Log — Change 0042: App #2 (B2B multi-utente) «Mini-CRM» via `new-application`

**Branch**: `change/0042-use-case-0054-app2-b2b-crm`
**Aree**: `services/crm` (nuovo), `services/core`, `services/commons`, `frontend`, `infra`, `tools/new-application`, `tools/scaffold-parity`, `docs/compliance`, `docs/usecases`
**Completata**: 2026-07-24
**Modalità**: **autopilot** — le risposte alle domande di approfondimento sono dell'agente e sono tracciate in [decisions.json](decisions.json) (29 decisioni, 26 in autopilot). I co-piloti riservati allo sviluppatore — prezzi/quote e dati personali — sono stati proposti e approvati esplicitamente; l'ultima decisione (29) è dello sviluppatore: **il Mini-CRM nasce disabilitato di default** (vedi sotto).

## File modificati (sintesi per area; dettaglio in git)

| Area | Azione |
|---|---|
| `tools/new-application/` (generatore + modelli) | Modificato — nuova opzione `--quota-nature flow\|stock`, varianti dei modelli, correzioni |
| `tools/scaffold-parity/` | Modificato — collaudo di parità consapevole delle varianti + test |
| `services/crm/` | Creato (generato) poi dominio reale — contatti, interazioni, posti |
| `services/commons/usage/UsageEvents.java` | Creato — contratto del canale d'uso app→core |
| `services/core/` (billing) | Modificato — `AppUsageStore`/`AppUsageConsumer`, gate downgrade reale, migrazione V9 |
| `services/core/.../pricing/` | Modificato — listino `crm.yaml` + `index.yaml` |
| `frontend/apps/backoffice/src/modules/crm/` | Modificato — dominio reale (contatti/interazioni/membri), registry |
| `infra/modules/platform_shared`, `infra/modules/microsaas_app`, `infra/envs/*` | Modificato — coda condivisa `app-usage` + IAM |
| `docs/compliance/manifests/crm.yaml`, `docs/compliance/ropa.*.md` | Creato/rigenerato |
| `dev/elasticmq.conf` | Modificato — code locali dell'app + `app-usage` |

## Cosa è stato fatto

L'app #2 del marketplace («Mini-CRM», B2B multi-utente) è stata generata **interamente dalla skill `new-application`**
e poi portata al dominio reale: contatti (con stato della trattativa), interazioni datate e **posti** (`seat`) come quota
a **giacenza**. La generazione ha validato la skill end-to-end e fatto emergere alcune lacune del generatore, corrette nei
**modelli-sorgente** (non nella copia). Il blocco del downgrade è stato reso reale con un canale asincrono app→core.

## Decisioni prese (registro completo e strutturato: [decisions.json](decisions.json))

Condotta in **autopilot**; scelte principali dell'agente:

1. **Generatore** — nuova opzione `--quota-nature flow|stock`, materializzata come **varianti** dei modelli-sorgente
   (`QuotaService`, `QuotaTest`, listino) con conteggio e listino coerenti alla natura. Due lacune del generatore trovate
   usandolo e corrette nei modelli: copia di `target/`/`node_modules` (ora saltati) e `MockEntitlementService.natureOf()`
   che ignorava la natura; più una lacuna dei test (campioni derivati dal tetto, tenant per-test). Istruzioni della skill
   corrette (comando `generate.mjs`, natura decisa prima di generare).
2. **Dominio** — `Contact` + `Interaction` al posto del segnaposto; il **posto** è un utente abilitato *a questa app*,
   registrato nello schema dell'app. Il varco d'accesso al dominio è il **possesso di un posto** (403); l'assegnazione è
   l'unico punto che consuma quota (429 a tetto raggiunto); la revoca libera subito la giacenza. Gestione posti riservata
   a owner/admin e fuori dal varco posto (un account pieno deve poter liberarne uno).
3. **Downgrade gated reale** — l'app pubblica l'uso a giacenza sulla coda condivisa `app-usage`; core lo materializza
   (`app_usage_stock`) e `TierChangePolicy` smette di ricevere una mappa vuota. Chiude, per il solo varco del downgrade,
   il punto aperto di UC 0028. Nessuna nuova chiamata sincrona (verso opposto e disaccoppiato).
4. **Prezzi/quote** (approvati) — `free` 2 posti · `team` 10 posti a 19,00 €/mese o 190,00 €/anno, prova 14 giorni,
   listino `active`.
5. **Dati personali** (approvati) — contatti = dati di terzi immessi dal tenant, base contratto, appgrove responsabile,
   nessuna categoria art. 9; i campi nota (testo libero) dichiarati come ingresso non presidiato. Cambio **MINOR**.
6. **App disabilitata di default** (decisione dello sviluppatore, non autopilot) — il Mini-CRM è un **veicolo di
   validazione** della skill `new-application` e della meccanica multi-utente/posti, **non un prodotto in vendita**. Il
   codice resta nel repo per il valore riutilizzabile (tempra del generatore, canale d'uso app→core, gate di downgrade
   reale), ma l'app è **spenta per tutti** via `status: inactive` in `crm.yaml`: il read-model entitlement salta le app
   non-attive (decisione #09.30), quindi niente modulo nel registry frontend e API `/api/crm/v1/*` che negano l'accesso.
   Riaccensione: toggle admin `PATCH /api/admin/v1/apps/{id}` (temporaneo, la sync lo riscrive) o `status: active` nel
   listino (durevole). Intento fissato da due test (`PricingCatalogLoaderTest`, `SeedDataTest`).

## Invarianti appgrove

- **Tenant dal solo JWT** — contesto del chiamante ereditato da `commons`; posti/contatti/interazioni non accettano mai
  un tenant da body/param (test `MultiTenancyTest.tenantIdInBodyIsIgnored`).
- **Filtro per account** — discriminatore su ogni entità; `contact`/`interaction`/`seat` indicizzate su `tenant_id`.
- **Modulo Terraform `microsaas_app`** — l'infra dell'app è un'istanza del modulo, prodotta da `service-add`; nessun
  blocco scritto a mano.
- **Logging strutturato** — assegnazione/revoca posto e pubblicazione uso portano `tenant_id`/`user_id`.
- **Diritti dalla proiezione locale** (UC 0046) — nessuna lettura sincrona verso core sul percorso caldo; l'unico canale
  nuovo è asincrono e in direzione app→core.

## Note per il revisore

- **Contratti cross-area**: (1) frontend ↔ `/api/crm/v1/*`; (2) **nuovo canale asincrono** app→core `app-usage`
  (commons `UsageEvents`, consumato solo da core); (3) servizio ↔ infra (nuova istanza `microsaas_app` + coda condivisa in
  `platform_shared`). Infra **validata** (`fmt`+`validate` su test e prod), **non applicata**.
- **Gate privacy (UC 0031)**: segnali attesi (nuovo manifesto/campi personali, dipendenze standard, host
  `local.appgrove.app` = emittente JWT locale condiviso, non un nuovo responsabile). Classificazione **MINOR**, nessun
  nuovo sub-processor (decisioni 23, 25).
- **Gate parità scaffold (UC 0046)**: percorsi-sorgente toccati = `tools/new-application/templates/` (aggiornati: opzione
  natura + varianti), `infra/modules/microsaas_app` e `pricing/index.yaml` (infrastruttura/registro condivisi che il
  generatore istanzia/edita dal vivo → le app nuove li ereditano da soli). `npm run parity` **verde**; nessuna deviazione
  da registrare.
- **Decisioni differite** tracciate in [UC 0054 §Punti aperti](../../docs/usecases/11-apps/0054-app2-b2b-via-new-application.md):
  generalizzazione di posti+canale d'uso nello scaffold (owner UC 0046), consumo quota nel pannello di fatturazione
  (UC 0028), ruolo applicativo del posto, presidio del testo libero (compliance).

## Test

Aggiunti/aggiornati e verdi (`./run-tests.sh` per area):

- **backend** (`mvn test`, reattore completo): app `crm` (43 test — isolamento, ruoli, varco posto 403, quota a giacenza
  con liberazione immediata, gate entitlement, GDPR export/erasure, manifesto); core (`DowngradeStockGateTest` end-to-end:
  downgrade bloccato con posti > tetto, consentito rientrando, nessun report → nessun blocco); tre test di catalogo/seed
  del core aggiornati per la seconda app reale, più due asserzioni che **fissano `crm` come disabilitata di default**
  (`PricingCatalogLoaderTest` a livello di catalogo, `SeedDataTest` end-to-end su `platform.app`).
- **frontend** (`npm test` + Playwright L2): 8 test unità del modulo crm (incluso lo stato «senza posto» → messaggio
  azionabile) + 3 e2e (ciclo contatti, assegnazione posto, route guard non-entitled). Nessun aggiornamento cieco di
  baseline visive.
- **tooling**: parità modelli-sorgente ↔ app #1 verde (varianti gestite) + collaudo di livello 3 (genera un'app e ne
  esegue l'intera suite: verde).
- **infra**: `terraform fmt`/`validate` (test+prod) + test del modulo/Lambda.
- **compliance**: parità lingue manifesti + RoPA allineato.
- **smoke**: avvio reale degli artefatti in profilo dev — `crm` risponde su `:18082/q/health/ready` e login end-to-end ok.

## Stato criteri di accettazione

- [x] `./dev.sh services` elenca `crm` (porta 8082, schema `app_crm`) senza modifiche manuali a script/proxy/CI.
- [x] App generata dal generatore; le differenze rispetto all'output sono solo dominio reale + decisioni dei co-piloti.
- [x] Il generatore accetta `--quota-nature stock`: conteggio a giacenza + listino `type: stock`; parità verde.
- [x] Isolamento fra account su contatti/interazioni/posti (verificato da test).
- [x] (N+1)-esima assegnazione → 429 con rimedio; revocato un posto, l'assegnazione riesce **senza finestra temporale**.
- [x] Membro senza posto → 403; owner/admin gestiscono i posti anche a posti esauriti.
- [x] Downgrade verso un piano con meno posti di quelli occupati → rifiutato con rimedio; liberati i posti → accettato
      (test end-to-end).
- [x] Manifesto dati in italiano e inglese; controllo `@PersonalData`↔manifesto verde; export/erasure coprono le tre tabelle.
- [x] Modulo Mini-CRM nel backoffice per un account entitled, con stati vuoto/caricamento/errore.
- [x] `./run-tests.sh` verde su tutte le aree; l'app è inclusa senza modifiche manuali oltre a quelle del generatore.
