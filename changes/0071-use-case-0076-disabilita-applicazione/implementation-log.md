# Implementation Log — Change 0071: Disabilita applicazione (feature admin reversibile)

**Branch**: `change/0071-use-case-0076-disabilita-applicazione`
**Aree**: `services/core` · `frontend/apps/admin` · `frontend/apps/backoffice` · `frontend/packages/i18n` ·
`frontend/packages/api-client` · docs
**Completata**: 2026-08-01
**Modalità**: fast (autopilot senza gate di workflow) — tutte le risposte alle domande di approfondimento sono
dell'agente e sono tracciate in [decisions.json](decisions.json), marcate `(autopilot)`.

## File modificati

| File | Azione |
|---|---|
| `services/core/src/main/resources/db/migration/V13__app_status_audit.sql` | Creato |
| `services/core/src/main/java/app/appgrove/core/catalog/AppStatusService.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/platform/AuditRetentionSweeper.java` | Rinominato da `gdpr/GdprAuditRetentionSweeper.java` + esteso |
| `services/core/src/main/java/app/appgrove/core/platform/AdminResource.java` | Modificato |
| `services/core/src/main/java/app/appgrove/core/platform/AdminDtos.java` | Modificato |
| `services/core/src/main/java/app/appgrove/core/billing/SubscriptionDtos.java` | Modificato |
| `services/core/src/main/java/app/appgrove/core/billing/SubscriptionReadModel.java` | Modificato |
| `services/core/src/main/resources/META-INF/openapi/openapi.yaml` · `openapi.json` | Rigenerati (build Quarkus) |
| `services/core/src/test/java/app/appgrove/core/catalog/AppDisableApiTest.java` | Creato |
| `services/core/src/test/java/app/appgrove/core/platform/AuditRetentionSweeperTest.java` | Rinominato + esteso |
| `services/core/src/test/java/app/appgrove/core/TestData.java` | Modificato (helper stato app + registro) |
| `services/core/src/test/java/app/appgrove/core/billing/SubscriptionSelfServiceTest.java` | Modificato |
| `frontend/packages/api-client/src/schema.ts` | Rigenerato da OpenAPI |
| `frontend/packages/i18n/src/resources/{it,en,es,fr,de}.ts` | Modificati (5 lingue) |
| `frontend/apps/admin/src/pages/Apps.tsx` | Modificato |
| `frontend/apps/admin/src/pages/ConfirmDialog.tsx` | Modificato (slot `children`) |
| `frontend/apps/admin/src/shell/QueryState.tsx` | Modificato (`emptyLabel`) |
| `frontend/apps/admin/src/api/hooks.ts` | Modificato |
| `frontend/apps/admin/src/pages/admin.test.tsx` | Modificato |
| `frontend/apps/admin/e2e/admin.spec.ts` | Modificato |
| `frontend/apps/backoffice/src/billing/SubscriptionsPanel.tsx` | Modificato |
| `frontend/apps/backoffice/e2e/subscriptions.spec.ts` | Modificato |
| `docs/usecases/15-supporto-e-piattaforma/0076-disabilita-applicazione.md` | Modificato (punti aperti) |
| `docs/usecases/EPICS-WAVE-2.md` | Modificato (0076 → ✅) |
| `docs/_BACKLOG.md` | Modificato (rimando compliance) |

## Cosa è stato fatto

Metà della leva esisteva già: il campo `app.status`, l'endpoint riservato a `platform-admin`, il pulsante con
conferma nella console, e soprattutto l'applicazione della regola a valle (`EntitlementAccess`, UC 0027/0077, che
scarta ogni app non `active`). Questa change ha completato la Definition of Done dello use case: la **motivazione**
facoltativa dell'operatore, l'**audit persistito e consultabile** di ogni transizione in `platform.app_status_audit`,
l'**idempotenza** esplicita (stesso stato richiesto = nessuna scrittura e nessuna riga di registro), gli **errori
tipizzati** (400 su stato non ammesso, 404 su app inesistente), un **copy** che distingue in modo inequivocabile la
pausa reversibile dalla dismissione definitiva, e il registro visibile in fondo alla sezione App della console.

Lato utente finale è stato chiuso il punto aperto annotato nello use case: `/me/subscriptions` espone ora
`appDisabled` e la card dell'abbonamento mostra un badge "Sospesa" più l'avviso che spiega la situazione — prima la
pagina Billing diceva "Attivo" per un'app che la barra laterale, giustamente, non mostrava più.

La conservazione delle righe di registro riusa il job già esistente, che è stato **generalizzato** da
`GdprAuditRetentionSweeper` a `AuditRetentionSweeper`: applica una regola di piattaforma ("le prove di audit nel
database durano 12 mesi"), non una regola GDPR.

## Decisioni prese

Change condotta in modalità **fast**: ogni domanda di approfondimento ha avuto risposta dall'agente. Le principali:

- **Nome dello stato**: si mantiene l'enum esistente `active`/`inactive`, non si rinomina in `disabled` come recita
  il testo dello use case — il valore vive già nello schema, nei dati e nella suite end-to-end di piattaforma, e la
  parola "disabilitata" serve nel copy, non nel database.
- **Tabella dedicata** `platform.app_status_audit` (migrazione V13): le tabelle di prova esistenti sono tenant-scoped
  e di dominio GDPR, qui il soggetto è il catalogo di piattaforma e non c'è alcun tenant.
- **Idempotenza a 200, non a 409**: rispondere conflitto avrebbe rotto i chiamanti automatici (la suite di
  piattaforma attiva un'app in preparazione senza sapere da quale stato parte). Il vincolo vero dello use case è
  "nessun audit incoerente": nessuna transizione ⇒ nessuna riga.
- **Motivazione facoltativa, 512 caratteri, solo su database**: il testo libero non entra mai nell'evento di audit
  strutturato che confluisce nell'archivio a 12 mesi — stessa prudenza già applicata alla nota della limitazione
  art. 18.
- **Job di conservazione generalizzato** invece di un secondo job gemello.
- **Punto aperto Billing chiuso qui** e non rimandato alla revisione UX catalogo/billing (UC 0096): è l'esperienza
  utente diretta della disabilitazione, cioè il senso della storia.
- **Copertura end-to-end**: estesi i due percorsi L2 esistenti (console admin e backoffice); nessun journey nuovo
  nella suite di piattaforma — quello lato admin appartiene a UC 0092, la storia successiva.

Registro completo e strutturato: [decisions.json](decisions.json) — 21 decisioni, tutte in autopilot tranne la
prima (la modalità, dichiarata dallo sviluppatore all'invocazione).

## Invarianti appgrove

- **`tenant_id` solo dal JWT verificato**: l'operatore è identificato dal `sub` del token via `CallerContext`; il
  corpo della richiesta porta solo stato e motivazione, mai identificativi di tenant o di attore.
- **Filtro row-level `WHERE tenant_id`**: nessuna query tenant-scoped nuova. La scrittura riguarda il catalogo di
  piattaforma (non tenant-scoped) e il registro, che non ha colonna tenant perché l'azione vale per tutti gli
  account insieme; l'eccezione cross-account della console admin resta quella già documentata e gated
  `platform-admin`. Il read-model `/me/subscriptions` continua a leggere le subscription tenant-scoped e aggiunge
  solo un dato di catalogo.
- **Modulo Terraform `microsaas_app`**: nessuna modifica infrastrutturale.
- **Logging strutturato**: la transizione emette `admin.app.status-changed` con `app_id`, stato di partenza e di
  arrivo, attore — più l'evento di audit con soli identificativi opachi (la motivazione ne resta fuori).

## Note per il revisore

- **Contratto cross-area**: l'OpenAPI del core cambia in modo **additivo** — nuova lettura
  `GET /api/platform/v1/admin/apps/audit`, nuovo campo facoltativo `reason` nel corpo del `PATCH`, nuovo campo
  `appDisabled` su `/me/subscriptions`. `frontend/packages/api-client/src/schema.ts` è stato rigenerato di
  conseguenza. Nessun consumatore esistente si rompe: la suite end-to-end di piattaforma continua a usare il
  `PATCH` senza motivazione.
- **Rinomina di classe**: `GdprAuditRetentionSweeper` → `AuditRetentionSweeper`, spostata dal pacchetto `gdpr` a
  `platform`. Nessun chiamante fuori dal suo test.
- **Cambio di interfaccia voluto**: il badge di stato nella console admin non mostra più il valore grezzo del
  database ma un'etichetta tradotta (Attiva/Disabilitata), come chiede lo use case; gli asserti dei test che
  leggevano il valore grezzo sono stati aggiornati per questo, non per farli passare.
- **Gate privacy (UC 0031)**: eseguito. Un solo segnale — la nuova tabella. Classificazione: registro amministrativo
  di audit, l'unico dato riferibile a una persona è l'identificativo opaco dell'operatore di piattaforma; finalità
  sicurezza e tracciabilità; base giuridica legittimo interesse (art. 6.1.f); conservazione 12 mesi nel database;
  categoria ordinaria, nessun dato dell'art. 9, nessun nuovo responsabile esterno, nessun dato di utente finale.
  Classificazione del cambio: **MINOR** (nessuna nuova finalità, base, categoria o conservazione → nessuna
  ri-accettazione dei legali). Nessuna annotazione `@PersonalData` da mettere (la tabella non ha entità JPA e non ha
  campi di dato personale di utente finale) e nessuna voce nuova nel manifesto, coerentemente con le due tabelle di
  prova già esistenti.
- **Gate parità scaffold (UC 0046)**: `source-paths-scan.mjs` → exit 0, nessun percorso-sorgente dei modelli toccato.
- **Promemoria landing**: nessuna superficie feature/pricing di un'app toccata (le modifiche sono su `core`, console
  admin e pannello abbonamenti) → nessuna landing resa stantìa.
- **Decisioni differite tracciate**:
  1. *Rinfresco delle proiezioni entitlement lato app* → punti aperti di UC 0076: disabilitare non pubblica alcun
     evento di invalidazione (il pubblicatore esistente lavora per tenant su cambi di fatturazione, qui il cambio è
     di catalogo e riguarda tutti); rinuncia accettata, coerente con quella già dichiarata per token e cache client.
  2. *Tabelle di prova nel database e manifesto dati* → `docs/_BACKLOG.md` §Compliance & privacy: le tre tabelle di
     audit nel database non hanno una voce dedicata nel manifesto e sono coperte solo implicitamente da
     `logs.structured`; da chiudere in una revisione di compliance prima della revisione legale pre-go-live.
  3. Restano invariati i punti aperti già scritti nello use case: kill-switch all'edge, disabilitazione per singolo
     account, comunicazione automatica agli utenti, finestra di manutenzione programmata.

## Test

Eseguita la **suite completa** `./run-tests.sh` (senza parametri), come impone la modalità fast: **tutte le aree
verdi** — backend, frontend, infra, compliance, tooling, smoke, platform, site.

Test aggiunti/aggiornati:

- **backend** — `AppDisableApiTest` (nuovo, 6 casi): transizione con motivazione e riga di registro con attore e
  istante; simmetrica alla riabilitazione con il più recente in testa; idempotenza che non sporca il registro;
  400 *problem+json* su stato non ammesso senza cambio di stato; 404 su app inesistente; motivazione oltre 512
  caratteri rifiutata; nessun ruolo di account può cambiare lo stato né leggere il registro.
  `AuditRetentionSweeperTest` esteso alla terza tabella (12 mesi: le vecchie spariscono, le recenti restano).
  `SubscriptionSelfServiceTest` + un caso: l'abbonamento porta `appDisabled` quando la piattaforma sospende l'app e
  torna a `false` alla riabilitazione. `AdminApiTest.disableAppTogglesStatusAndDropsEntitlement` invariato e verde —
  è la verifica del raccordo con l'enforcement di UC 0027.
- **frontend (vitest)** — `admin.test.tsx`: il dialogo mostra la distinzione dalla dismissione definitiva, la
  motivazione digitata finisce nel corpo della richiesta, il badge passa a "Disabled" e la transizione compare nel
  registro; lo stato vuoto del registro ha un testo suo. Parità delle 5 lingue verificata dal test di `@appgrove/i18n`.
- **frontend (Playwright L2)** — `admin.spec.ts`: percorso completo overview → app → disabilita con motivazione →
  badge → registro → riabilita → due righe di registro. `subscriptions.spec.ts`: abbonamento su app sospesa dalla
  piattaforma → badge "Suspended" e avviso con la spiegazione.

Nessun aggiornamento di baseline visiva: la change non tocca snapshot.

## Stato criteri di accettazione

- [x] Disabilitare scrive `app.status = inactive` e **una** riga di audit con operatore, istante, stati e motivazione; riabilitare fa il simmetrico
- [x] Ripetere l'azione sullo stato corrente risponde 200 senza scrivere nulla e senza riga di audit aggiuntiva
- [x] 404 su app inesistente, 400 su stato non ammesso, rifiuto per chi non è `platform-admin` (nessuna scrittura)
- [x] Con l'app `inactive` la lettura entitlement non la include; tornata `active`, la include di nuovo (database reale)
- [x] La console admin mostra il registro con la motivazione e il dialogo ospita campo e copy che distinguono dalla dismissione definitiva
- [x] La pagina Billing del backoffice segnala l'app disabilitata dalla piattaforma
- [x] Le righe del registro oltre 12 mesi sono eliminate dal job di conservazione, quelle recenti no
- [x] `./run-tests.sh` (suite completa) verde
