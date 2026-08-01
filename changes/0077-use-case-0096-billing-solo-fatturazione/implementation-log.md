# Implementation Log — Change 0077: Billing solo-fatturazione (abbonamenti + storico pagamenti/ricevute)

**Branch**: `change/0077-use-case-0096-billing-solo-fatturazione`
**Aree**: `services/core`, `frontend` (backoffice + catalogo traduzioni), `tools/platform-e2e`, documentazione e compliance
**Completata**: 2026-08-01
**Modalità**: **fast** (autopilot senza gate di workflow, dichiarata all'invocazione dalla skill `go-fast`) —
le risposte alle domande di approfondimento e ogni scelta tecnica sono dell'agente e sono tracciate in
[decisions.json](decisions.json) (24 voci, tutte marcate `(autopilot)` tranne la prima).

## File modificati

| File | Azione |
|---|---|
| `services/core/src/main/resources/db/migration/V14__billing_transaction.sql` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/BillingTransaction.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/BillingTransactionStatus.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/BillingTransactionRepository.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/PaymentDtos.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/PaymentReadModel.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/PaymentsResource.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/billing/PaddleWebhookEvent.java` | Modificato (dati economici + esito della transazione) |
| `services/core/src/main/java/app/appgrove/core/billing/SubscriptionWriter.java` | Modificato (scrittura della transazione nella stessa transazione) |
| `services/core/src/main/java/app/appgrove/core/billing/StubScenarioEmitter.java` | Modificato (dati economici, pagamento nel percorso felice, orologio monotòno) |
| `services/core/src/main/java/app/appgrove/core/catalog/CatalogDtos.java` · `CatalogReadModel.java` | Modificato (`upgradeAvailable`) |
| `services/core/src/main/java/app/appgrove/core/gdpr/PlatformDataContract.java` | Modificato (esportazione, cancellazione, manifesto) |
| `services/core/src/main/resources/META-INF/openapi/openapi.{yaml,json}` | Rigenerato |
| `services/core/src/test/java/app/appgrove/core/billing/PaymentsApiTest.java` | Creato (11 test) |
| `services/core/src/test/java/app/appgrove/core/billing/WebhookFixtures.java` | Modificato (fixture degli eventi di transazione) |
| `services/core/src/test/java/app/appgrove/core/TestData.java` | Modificato (fixture `billingTransaction`) |
| `services/core/src/test/java/app/appgrove/core/gdpr/{PlatformGdprContractTest,GdprExportApiTest}.java` | Modificato |
| `frontend/apps/backoffice/src/pages/Billing.tsx` (+ `Billing.test.tsx`) | Riscritto / test creato |
| `frontend/apps/backoffice/src/billing/PaymentsPanel.tsx` | Creato |
| `frontend/apps/backoffice/src/billing/paymentsApi.ts` (+ `.test.ts`) | Creato |
| `frontend/apps/backoffice/src/billing/SubscriptionsPanel.tsx` | Modificato (stato vuoto verso il catalogo, assistenza) |
| `frontend/apps/backoffice/src/pages/catalog/AppCatalogPage.tsx` (+ `.test.tsx`) | Modificato (via al piano a pagamento) |
| `frontend/packages/i18n/src/resources/{en,it,fr,es,de}.ts` | Modificato (`billing`, `payments`, chiavi nuove; via i testi di vetrina) |
| `frontend/packages/api-client/src/schema.ts` | Rigenerato |
| `frontend/apps/backoffice/e2e/billing.spec.ts` | Creato (`[L2-BILLING]`, 5 test) |
| `frontend/apps/backoffice/e2e/{checkout,catalog,subscriptions}.spec.ts` | Modificato |
| `tools/platform-e2e/journeys/{J-BUY,J-QUOTA,J-MEMBERS}.spec.ts` | Modificato |
| `docs/testing/copertura-e2e.yaml` | Modificato (`L2-BILLING`, J-BUY, superficie 0096) |
| `docs/compliance/manifests/platform.yaml` · `docs/compliance/ropa.{it,en}.md` | Modificato / rigenerato |
| `docs/usecases/21-catalogo-app-backoffice/0096-*.md` | Modificato (punti aperti) |
| `docs/usecases/21-catalogo-app-backoffice/0095-*.md` | Modificato (punto aperto chiuso) |
| `docs/usecases/15-supporto-e-piattaforma/0076-*.md` | Modificato (punto aperto sull'addebito) |
| `docs/_BACKLOG.md` · `docs/_PARITA-SCAFFOLD.md` · `docs/usecases/EPICS-WAVE-2.md` | Modificato |

## Cosa è stato fatto

La pagina **Billing** è ora di **sola fatturazione**. Si intitola "Billing — Manage your plans, payments and
receipts", mostra gli abbonamenti del workspace con le azioni self-service già esistenti e — sezione che
nel prodotto **non esisteva affatto** — lo **storico dei pagamenti con le ricevute**. Sono spariti il titolo
"Get an app" e la griglia delle app acquistabili: la scoperta e l'acquisto vivono nel catalogo (UC 0095), e
un workspace senza abbonamenti legge uno stato vuoto che lo manda lì.

Lo storico è un **dato del prodotto**, non una lettura al volo dal fornitore: la nuova tabella
tenant-scoped `platform.billing_transaction` è alimentata dalla **stessa pipeline webhook** che tiene
aggiornati gli abbonamenti — nella stessa transazione di database, idempotente sul riferimento della
transazione e con la stessa guardia contro gli eventi fuori ordine. Entrano tutte le transazioni,
**comprese quelle fallite e contestate**, che sono proprio ciò che l'utente deve poter vedere.

In più la change chiude il punto aperto che UC 0095 le aveva assegnato: un'app **freemium** risultava già
`active` in vetrina e il suo piano a pagamento non era comprabile da nessuna parte del prodotto. Ora la
card `active` senza abbonamento offre, accanto ad "Apri", la via all'acquisto — ed è la via che il journey
di piattaforma usa davvero per comprare il Mini-CRM.

## Decisioni prese

Change in **fast**: tutte le domande hanno avuto risposta dall'agente. Le principali, in prosa (registro
completo e strutturato in [decisions.json](decisions.json)):

- **Fonte dello storico**: **persistenza** via webhook, non lettura on-demand dal fornitore. Il requisito di
  completezza chiede tutte le transazioni (anche fallite); la lettura on-demand richiederebbe un
  identificativo cliente che è nullo finché non si è comprato nulla e metterebbe una chiamata verso
  l'esterno dentro il rendering di una pagina.
- **Contratto**: nuova lettura `GET /api/platform/v1/me/payments`, senza parametri, ristretta a `owner` e
  `admin` come vuole UC 0096 §2. Al member la sezione non viene nemmeno chiesta.
- **Il checkout resta in Billing solo per la riattivazione** di un abbonamento scaduto: riattivare non è
  scoprire, ed è un'azione che parte da una card già in pagina.
- **Punto aperto freemium**: risolto con `upgradeAvailable` nel read-model della vetrina — vero solo quando
  la card è `active` **senza** abbonamento e un prezzo vivo esiste ancora.
- **Il simulatore locale porta gli importi veri** (letti dal listino) e il percorso felice emette anche il
  pagamento: senza, lo storico in locale sarebbe sempre vuoto e la pagina non sarebbe verificabile a mano.
- **Nessun importo inventato dal backend**: un evento di transazione senza importo o valuta non produce
  alcuna riga. In una pagina di fatturazione un numero finto è peggio di un dato mancante.
- **Copertura end-to-end**: coperta ora — nuovo percorso `L2-BILLING` e `J-BUY` esteso fino allo storico.

**Due guasti trovati e corretti in corsa**, entrambi dalla suite completa e non dai test mirati:

1. **L'orologio compresso del simulatore.** Aggiungendo il pagamento, uno scenario è passato da due a tre
   eventi: due scenari lanciati nello stesso secondo facevano scartare come "vecchio" il secondo, per la
   guardia out-of-order del consumer. Corretto alla radice — il simulatore tiene ora un orologio
   **monotòno**, lo stesso accorgimento già usato per le mutazioni self-service, esteso a tutti gli scenari.
2. **Il conteggio degli step di esportazione GDPR**, salito da 6 a 7 con l'aggiunta dello storico: il test è
   stato aggiornato al numero vero, non alleggerito.

## Invarianti appgrove

- **`tenant_id` solo dal token verificato** — la lettura dello storico non espone alcun parametro con cui
  indicare un conto: il tenant arriva dal discriminator di multi-tenancy, che lo prende dal token. Provato
  da `senzaContoNelTokenSiRifiutaInveceDiIndovinare` (403 fail-closed). La **scrittura** gira fuori da una
  richiesta autenticata e usa il tenant dei dati personalizzati del payload **firmato**, esattamente come
  già fa la subscription: la fiducia viene dalla firma, non da un input del client.
- **Filtro row-level `WHERE tenant_id = :tid`** — `BillingTransaction` estende `BaseTenantEntity`, quindi
  ogni lettura è filtrata da Hibernate. Provato da `loStoricoEIsolatoPerConto` (due conti, un pagamento).
- **Modulo Terraform `microsaas_app`** — non toccato: nessuna infrastruttura nuova.
- **Logging strutturato** — `payments.read transazioni=N` sulla lettura; la registrazione della transazione
  passa dal log del webhook, che porta già `tenant_id`/`app_id`/`user_id` via MDC.

## Note per il revisore

- **Contratto cross-area**: nuova lettura `GET /api/platform/v1/me/payments` e nuovo campo
  `upgradeAvailable` nella vetrina (sola aggiunta). Spec OpenAPI e tipi del frontend rigenerati nello stesso
  commit.
- **Migrazione di database**: `V14__billing_transaction.sql`, sola creazione di tabella e indici. Nessuna
  colonna esistente toccata, nessun dato riscritto.
- **Gate privacy (UC 0031)**: segnali presenti e **classificati**. Tre voci nuove a manifesto
  (`billing_transactions.paddle_transaction_id`, `.amount`, `.receipt_url`) nelle due lingue, registro dei
  trattamenti rigenerato, campi annotati `@PersonalData`. Categoria **ordinaria**, base giuridica
  **contratto (art. 6.1.b)**, conservazione **account attivo + 14 giorni di grazia** (la conservazione
  fiscale resta in capo a Paddle). Nessun dato dell'art. 9, **nessun nuovo responsabile esterno del
  trattamento**, nessun nuovo host o dipendenza. Classificazione del cambio: **MINOR** — notizia agli
  interessati, non ri-accettazione. Nota: nessun dato di carta o di conto transita o è conservato da
  appgrove; l'ambito PCI resta di Paddle.
- **Diritti dell'interessato**: la nuova tabella è **esportata** e **cancellata fisicamente** con il conto.
- **Decisioni differite tracciate**: nei punti aperti di **UC 0096** (paginazione e filtri dello storico;
  rimborsi e note di credito non modellati — tema di UC 0025; formato reale del payload del fornitore da
  confermare in ambiente di prova, UC 0029; risoluzione dei nomi riga per riga); nei punti aperti di
  **UC 0076** (se una sospensione di piattaforma debba sospendere o rimborsare l'addebito — **direzione di
  prodotto e prezzo**, deliberatamente non decisa qui); in **`docs/_BACKLOG.md`** (gli abbonamenti restano
  fuori dall'esportazione mentre le transazioni ora ci sono: asimmetria da sciogliere, tema di UC 0033/0032).
- **Testo deliberatamente NON scritto**: la card dell'app sospesa **non** dice «l'abbonamento non viene
  addebitato», come suggerisce il mockup della change `0066`. Nulla nel prodotto sospende l'addebito: sarebbe
  una promessa non mantenuta. Resta il testo onesto già in produzione più la via verso l'assistenza.
- **Gate parità scaffold**: percorso-sorgente toccato (`tools/platform-e2e/journeys/J-QUOTA.spec.ts`) →
  **deroga registrata** in `docs/_PARITA-SCAFFOLD.md`: il tocco è il solo adeguamento di un testo
  dell'interfaccia, e il modello del journey è scritto apposta per non dipendere dai testi. Collaudo di
  parità verde.
- **Copertura e2e**: voce aggiunta (`L2-BILLING`), voce estesa (`J-BUY`); UC 0096 non è più esente.
- **Promemoria landing**: **non scatta** — nessun listino di app, nessun servizio di app, nessun modulo
  frontend di app toccato.
- **Ricadute su percorsi esistenti**: L2-CHECKOUT parte ora dalla vetrina; J-QUOTA e J-MEMBERS asseriscono
  «Billing» invece di «Get an app» dopo l'invito all'upgrade (l'invito continua a portare a `/billing`, dove
  vivono i cambi di piano: nessuna destinazione è stata cambiata).
- **Nessuna istantanea visiva ri-registrata**: la suite non ne usa per queste pagine.

## Test

- **Backend** (`services/core`): `PaymentsApiTest` (11 test) — riga completa di uno storico, esiti fallito e
  contestato, ricevuta assente, stesso evento due volte, due eventi diversi sulla stessa transazione, evento
  fuori ordine che non regredisce l'esito, transazione senza valuta che non sporca lo storico, evento di
  abbonamento che non produce righe, isolamento fra conti, ruoli (owner/admin sì, member 403, anonimo 401),
  token senza conto (403 fail-closed). Aggiornati `PlatformGdprContractTest` (l'esportazione copre la nuova
  entità) e `GdprExportApiTest` (7 step invece di 6).
- **Frontend** (unit/componente): `Billing.test.tsx` (9 test: titolo e assenza di elementi di vetrina, le due
  sezioni, tabella con importo/esito/ricevuta, pagamento fallito senza ricevuta, stato vuoto della sola
  sezione, guasto che non spegne gli abbonamenti, rimando al catalogo, app sospesa con via all'assistenza,
  member senza sezione), `paymentsApi.test.ts` (4 test sulla descrizione della riga),
  `AppCatalogPage.test.tsx` (2 test nuovi sulla via al piano a pagamento).
- **End-to-end livello 2**: `frontend/apps/backoffice/e2e/billing.spec.ts`, 5 test etichettati `[L2-BILLING]`;
  un test nuovo in `catalog.spec.ts`; `checkout.spec.ts` spostato sulla vetrina.
- **Journey di piattaforma**: `J-BUY` esteso — l'app freemium si compra dalla vetrina, il pagamento compare
  nello storico di Billing e il database contiene due transazioni riuscite del solo tenant di prova.
- **Suite completa** `./run-tests.sh` (backend, frontend, infra, compliance, tooling, smoke, platform, site):
  **VERDE**.

## Stato criteri di accettazione

- [x] Billing senza alcun elemento di vetrina; titolo "Billing" con il sottotitolo su piani, pagamenti e ricevute
- [x] Workspace senza abbonamenti: stato vuoto con rimando esplicito al catalogo
- [x] Sezione "Your subscriptions" con piano, quota inclusa, rinnovo/scadenza, azioni self-service e avviso
      esplicito per l'app sospesa dalla piattaforma
- [x] Sezione "Payments & receipts" con lo storico reale, comprese le righe fallite
- [x] Riga senza ricevuta disponibile presente, senza collegamento
- [x] Guasto dello storico confinato alla sua sezione, con riprova; abbonamenti visibili
- [x] Dopo un acquisto vero in locale la transazione compare nello storico (provato da `J-BUY`)
- [x] Transazioni lette con il filtro riga per riga; nessun parametro del client indica un conto
- [x] La card `active` di un'app freemium offre la via all'acquisto del piano a pagamento
- [x] Testi nuovi in tutte e 5 le lingue
- [x] `./run-tests.sh` verde e registro di copertura end-to-end coerente
