# Implementation Log — Change 0084: Ticketing nativo in-house (UC 0075)

**Branch**: `change/0084-use-case-0075-ticketing-nativo-in-house`
**Aree**: `services/core`, `shared/email-templates`, `frontend/packages/{i18n,api-client}`,
`frontend/apps/{backoffice,admin}`, `tools/platform-e2e`, `docs`
**Completata**: 2026-08-02
**Modalità**: **fast** — i tre gate di workflow sono stati rinunciati all'invocazione; le risposte alle
domande di approfondimento sono dell'agente e sono tracciate in [decisions.json](decisions.json) (20 voci).

## File modificati

| File | Azione |
|---|---|
| `services/core/src/main/resources/db/migration/V16__support_ticket_source_review.sql` | Creato |
| `services/core/src/main/java/app/appgrove/core/support/TicketSource.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/support/SpecialCategoryScreening.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/support/AdminTicketResource.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/support/TicketEmailRenderer.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/support/TicketNotifier.java` | Riscritto |
| `services/core/src/main/java/app/appgrove/core/support/{SupportTicket,TicketStatus,TicketStore,TicketDtos,TicketResource}.java` | Modificati |
| `services/core/src/main/java/app/appgrove/core/gdpr/{AdminGdprResource,AdminGdprDtos,GdprExportResultsConsumer}.java` | Modificati |
| `services/core/src/main/resources/application.properties` | Modificato (basi pubbliche per i collegamenti delle email) |
| `services/core/src/main/resources/META-INF/openapi/openapi.{yaml,json}` | Rigenerati dal build |
| `shared/email-templates/{it,en}.json` | Modificati (4 messaggi nuovi del ticketing) |
| `frontend/packages/i18n/src/resources/{en,it,fr,es,de}.ts` | Modificati (blocco `admin.tickets`, voce di menu, chiavi backoffice) |
| `frontend/packages/api-client/src/schema.ts` | Rigenerato |
| `frontend/apps/admin/src/pages/{Tickets,TicketDetail}.tsx`, `ticketLabels.ts` | Creati |
| `frontend/apps/admin/src/pages/GdprTicketDetail.tsx` | Eliminato (sostituito da `TicketDetail.tsx`) |
| `frontend/apps/admin/src/pages/GdprRights.tsx`, `api/hooks.ts`, `routing/routes.tsx`, `shell/{Sidebar,Topbar}.tsx` | Modificati |
| `frontend/apps/backoffice/src/pages/support/SupportPage.tsx` | Modificato |
| `services/core/src/test/java/app/appgrove/core/support/{SpecialCategoryScreeningTest,TicketEmailRendererTest}.java` | Creati |
| `services/core/src/test/java/app/appgrove/core/support/{TicketApiTest,TicketAdminApiTest}.java`, `gdpr/AutoTicketOnExportFailureTest.java`, `TestData.java` | Modificati |
| `frontend/apps/admin/src/pages/tickets.test.tsx`, `frontend/apps/admin/e2e/tickets.spec.ts` | Creati |
| `frontend/apps/admin/src/pages/gdpr.test.tsx`, `frontend/apps/admin/e2e/gdpr.spec.ts` | Modificati (il ticketing esce di lì) |
| `frontend/apps/backoffice/src/pages/support/SupportPage.test.tsx`, `e2e/support.spec.ts` | Modificati |
| `tools/platform-e2e/journeys/J-SUPPORT-TICKETING.spec.ts` | Creato |
| `tools/platform-e2e/journeys/A-GDPR.spec.ts` | Modificato (naviga al ticket dalla nuova sezione) |
| `docs/testing/copertura-e2e.yaml` | Modificato |
| `docs/compliance/manifests/platform.yaml`, `docs/compliance/ropa.{it,en}.md` | Modificati / rigenerati |
| `docs/usecases/15-supporto-e-piattaforma/0075-ticketing-nativo-in-house.md` | Modificato (rimandi) |
| `docs/_REVISIONE-LEGALE.md` | Modificato (punto L17) |

## Cosa è stato fatto

Il ticketing esisteva già in forma minima dalla change `0030`, come strumento interno della Console "Diritti
GDPR". Questa change ne ha implementato il **delta** che la storia 0075 chiede, senza riscrivere ciò che
funzionava: stato **`waiting_user`** con il rimpallo automatico fra i due lati, colonna **`source`**
(provenienza), **riconoscimento delle categorie particolari** che alza la priorità e accende un contrassegno
«da rivedere», **sezione «Ticket» autonoma** nella console di amministrazione (coda cross-account, filtri per
tipo/stato/priorità, ordinamento per scadenza, evidenza di scaduto/in scadenza, chiusura sotto conferma), e
**notifiche email rese dal renderer unificato** di `services/commons` (UC 0085) nelle due lingue, con la
conferma di apertura che prima non esisteva.

Nel backoffice il cliente vede ora lo stato «in attesa di una tua risposta», il termine di legge detto a
parole sulle istanze privacy e la nota esplicita che gli allegati non sono ancora supportati.

## Decisioni prese

Registro completo e strutturato: [decisions.json](decisions.json) — 20 voci, tutte prese dall'agente in
modalità fast. Le principali:

- **Delta, non riscrittura** (dec. 3): la base di UC 0034 resta, con la sua copertura di test.
- **`waiting_user` è la sola transizione automatica della risposta** (dec. 5); `in_progress` resta lo stato
  che l'operatore mette a mano.
- **Il contrassegno delle categorie particolari è un booleano operativo** (dec. 7): dice *che* serve un
  essere umano, mai *quale* categoria sarebbe stata riconosciuta. Registrare l'inferenza avrebbe creato un
  dato di categoria particolare derivato dove prima c'era solo testo libero.
- **Gli endpoint di amministrazione si spostano** da `/admin/gdpr/tickets` a `/admin/tickets` (dec. 8):
  rottura di contratto interna e voluta, perché il percorso vecchio raccontava una falsità.
- **Le email non trasportano più il contenuto della conversazione** (dec. 14): dicono che c'è un
  aggiornamento e portano alla pagina protetta. È un miglioramento di riservatezza rispetto a prima.
- **Il tipo `support` non viene rinominato in `generic`** (dec. 10): sarebbe una migrazione di dati e una
  rottura di contratto per un sinonimo.

## Invarianti appgrove

- **Tenant dal solo token verificato** — le API utente continuano a leggere conto e utente dal
  `CallerContext`; nessun identificativo di conto arriva da corpo o parametri. La coda di amministrazione è
  l'eccezione documentata, ristretta al ruolo `platform-admin` e registrata nei log a ogni operazione.
- **Filtro riga per riga** — le letture utente restano governate dal discriminator Hibernate (un ticket
  altrui è un 404, provato dal test); le scritture cross-tenant passano solo da `TicketStore`, che porta il
  conto in modo esplicito.
- **Modulo Terraform `microsaas_app`** — non toccato (nessuna app nuova).
- **Log strutturato** — ogni operazione registra `ticket_id`, `tenant_id`, l'attore e, all'apertura, anche
  provenienza ed esito del riconoscitore.

## Note per il revisore

- **Rottura di contratto interna**: `/api/platform/v1/admin/gdpr/tickets*` non esiste più. Nessun
  consumatore fuori dal monorepo; la console è stata riallineata nello stesso commit.
- **Gate privacy (UC 0031)**: 9 segnali, tutti classificati (dec. 15). Classificazione complessiva **MINOR**
  — nessuna nuova finalità, base giuridica, categoria di dati, destinatario o durata di conservazione. Le due
  voci di manifesto dei ticket sono state estese per descrivere la possibilità di categorie particolari nel
  testo libero e la salvaguardia adottata; RoPA rigenerato. I quattro «host esterni» segnalati sono le nostre
  stesse superfici (falso positivo dell'euristica).
- **Punto per la revisione legale**: aggiunto **L17** in `docs/_REVISIONE-LEGALE.md` — far confermare che il
  booleano `flagged_for_review` non vada trattato esso stesso come dato di categoria particolare, e se serva
  una conservazione più corta per i ticket contrassegnati. Non blocca il go-live.
- **Gate parità scaffold**: nessun percorso-sorgente dei modelli toccato.
- **Copertura end-to-end**: 0075 esce dalle esenzioni ed entra fra gli use case con superficie;
  J-SUPPORT-TICKETING passa a coperto con un journey di piattaforma nuovo; nasce L2-ADMIN-TICKETS; L2-SUPPORT
  e L2-ADMIN-GDPR riallineate. Registro verde (`node tools/e2e-coverage/check.mjs`).
- **Decisioni differite tracciate** (dec. 19), nei punti aperti di UC 0075: ricezione delle email in ingresso
  (SES → Lambda), manutenzione dell'elenco di parole-spia del riconoscitore, promemoria attivo della scadenza
  di legge, conferma dell'esclusione degli allegati. Nessun punto lasciato solo in conversazione.
- **Nessuna landing pubblicata toccata**: la change riguarda la piattaforma, non una singola applicazione.
- **`docs/usecases/_INDEX.md` non è stato toccato**: 0075 è una storia evolutiva e le storie evo non
  compaiono nella tabella di esecuzione topologica (dec. 4, coerente con le change 0082 e 0083).

## Test

- **Backend (`services/core`)** — nuovi: `SpecialCategoryScreeningTest` (riconoscimento delle categorie
  particolari, con i falsi positivi da escludere: «terrazza» non è «razza»), `TicketEmailRendererTest`
  (quattro messaggi in due lingue, nessun segnaposto irrisolto, l'avviso di aggiornamento non trasporta il
  filo). Estesi: `TicketApiTest` (provenienza `form`, escalation art. 9, conferma di apertura nella lingua
  dell'utente col termine di legge, rimpallo `waiting_user` → `open`), `TicketAdminApiTest` (percorso nuovo,
  filtro per priorità, **ordinamento della coda**, risposta che porta in attesa dell'utente),
  `AutoTicketOnExportFailureTest` (provenienza `event`).
- **Frontend** — nuovi: `tickets.test.tsx` (coda, filtri, `dueState`, dettaglio, conferma di chiusura) e
  `e2e/tickets.spec.ts` (L2-ADMIN-TICKETS). Estesi: `SupportPage.test.tsx` (nota allegati, stato in attesa,
  termine di legge) e `e2e/support.spec.ts`. `gdpr.test.tsx` e `e2e/gdpr.spec.ts` alleggeriti del ticketing.
- **Piattaforma** — `J-SUPPORT-TICKETING.spec.ts`: stack vero, due sessioni browser, email vere lette da
  Mailpit (con la prova che la conferma **non** contiene il testo del messaggio), escalation art. 9,
  chiusura sotto conferma, prove su database e tenant canarino per l'isolamento. `A-GDPR` aggiornato.
- **Esito**: `./run-tests.sh` (suite completa, senza parametri) — **verde su tutte e otto le aree**
  (backend, frontend, infra, compliance, tooling, smoke, platform, site). La prima esecuzione era rossa
  sull'area `platform` per un difetto del journey nuovo (il collegamento alla coda esiste in due punti
  quando si è nel dettaglio: menu e ritorno); corretto restringendo il selettore alla navigazione della
  console (dec. 20) e suite rilanciata per intero.

## Stato criteri di accettazione

- [x] Provenienza registrata alla nascita: `form` dal modulo, `event` dall'esportazione fallita.
- [x] Risposta della piattaforma → `waiting_user`; replica del cliente → `open`; scrittura su chiuso → 409.
- [x] Testo che tocca categorie particolari → priorità `high` + contrassegno; testo ordinario → invariato.
- [x] Sezione «Ticket» nella console con coda cross-account, filtri e scadenze evidenziate e ordinate.
- [x] Conferma di apertura e avviso di aggiornamento resi dal renderer unificato, in italiano e inglese.
- [x] Isolamento fra conti provato; coda cross-account riservata a `platform-admin` (403 altrimenti).
- [x] `./run-tests.sh` completa verde, percorso J-SUPPORT-TICKETING compreso.
