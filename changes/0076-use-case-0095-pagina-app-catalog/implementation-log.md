# Implementation Log — Change 0076: Pagina "App catalog" del backoffice

**Branch**: `change/0076-use-case-0095-pagina-app-catalog`
**Aree**: `services/core`, `frontend`, `tools/platform-e2e`, `tools/new-application`, documentazione
**Completata**: 2026-08-01
**Modalità**: **fast** (autopilot senza gate di workflow, dichiarata all'invocazione dalla skill `go-fast`) —
le risposte alle domande di approfondimento e ogni scelta tecnica sono dell'agente e sono tracciate in
[decisions.json](decisions.json) (26 voci, tutte marcate `(autopilot)` tranne la prima).

## File modificati

| File | Azione |
|---|---|
| `services/core/src/main/java/app/appgrove/core/catalog/CatalogReadModel.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/catalog/CatalogAppState.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/catalog/CatalogDtos.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/catalog/MeCatalogResource.java` | Creato |
| `services/core/src/main/java/app/appgrove/core/catalog/PricingDefinition.java` | Modificato (2 campi facoltativi) |
| `services/core/src/main/java/app/appgrove/core/catalog/AppPriceRepository.java` | Modificato (`existsForTier`) |
| `services/core/src/main/java/app/appgrove/core/billing/EntitlementReadModel.java` | Modificato (fascia gratuita per esistenza) |
| `services/core/src/main/resources/pricing/{fatture,crm}.yaml` | Modificato (categoria + 5 descrizioni) |
| `services/core/src/main/resources/pricing/fixtures/{notes,teams,legacy}.yaml` | Modificato (idem) |
| `services/core/src/main/resources/META-INF/openapi/openapi.{yaml,json}` | Rigenerato |
| `services/core/src/test/java/app/appgrove/core/catalog/CatalogApiTest.java` | Creato |
| `services/core/src/test/java/app/appgrove/core/catalog/CatalogAppStateTest.java` | Creato |
| `frontend/apps/backoffice/src/pages/catalog/AppCatalogPage.tsx` (+ `.test.tsx`) | Creato |
| `frontend/apps/backoffice/src/catalog/catalogApi.ts` (+ `.test.ts`) | Creato |
| `frontend/apps/backoffice/src/billing/CheckoutFlow.tsx` | Creato (estratto da `pages/Billing.tsx`) |
| `frontend/apps/backoffice/src/pages/Billing.tsx` | Modificato (usa il componente estratto) |
| `frontend/apps/backoffice/src/routing/routes.tsx` | Modificato (rotta `/catalog`) |
| `frontend/apps/backoffice/src/shell/Sidebar.tsx` (+ `.test.tsx`) | Modificato (voce di menu + invito) |
| `frontend/packages/i18n/src/resources/{en,it,fr,es,de}.ts` | Modificato (sezione `catalog` + `nav.appCatalog`) |
| `frontend/packages/api-client/src/schema.ts` | Rigenerato |
| `frontend/apps/backoffice/e2e/catalog.spec.ts` | Creato (`[L2-CATALOG]`, 6 test) |
| `tools/platform-e2e/journeys/J-BUY.spec.ts` | Modificato (parte dalla vetrina) |
| `tools/new-application/templates/pricing/@@APP_ID@@.{flow,stock}.yaml` | Modificato (varco parità scaffold) |
| `docs/testing/copertura-e2e.yaml` | Modificato (percorso `L2-CATALOG`, J-BUY, superficie 0095) |
| `docs/usecases/21-catalogo-app-backoffice/0095-pagina-app-catalog.md` | Modificato (punti aperti) |
| `docs/usecases/10-skills-tooling/0046-skill-new-application.md` | Modificato (punto aperto) |
| `docs/usecases/EPICS-WAVE-2.md` | Modificato (0095 ⬜ → ✅) |
| `docs/_BACKLOG.md` | Modificato (fragilità residua) |

## Cosa è stato fatto

Il backoffice ha una **pagina "App catalog"** (`/catalog`, voce di menu sotto Dashboard) che mostra il
**catalogo reale servito dal backend** invece dei moduli impacchettati nel frontend. Un nuovo read-model
`GET /api/platform/v1/me/catalog` restituisce, per ogni app visibile a questo account, nome, categoria,
descrizione nelle 5 lingue, prezzo di partenza e **stato per l'account** fra i sei del dominio; la pagina lo
rende in una griglia di card fedele al riferimento visivo approvato (change `0066`), con ricerca, conteggio
dei risultati, paginazione a 6 e stati di caricamento/errore/vuoto distinti.

L'acquisto **parte dalla vetrina**: il flusso di checkout è stato estratto da `pages/Billing.tsx` in un
componente condiviso e viene reso dentro la pagina del catalogo, così che dopo l'attivazione la card torni
sotto gli occhi dell'utente già aggiornata. La descrizione e la categoria sono un dato di **listino**
(pricing-as-code), non di frontend: il catalogo deve saper descrivere anche un'app senza modulo impacchettato.

## Decisioni prese

Change in **fast**: tutte le domande hanno avuto risposta dall'agente. Le principali, in prosa (registro
completo e strutturato in [decisions.json](decisions.json)):

- **Fonte della parte descrittiva**: pricing-as-code, con `category` e `descriptions` facoltativi; **non**
  persistite nel database né sincronizzate (contenuto di presentazione, non dato transazionale). Il
  read-model le legge in-processo e serve **tutte e cinque le lingue**, così il cambio lingua non ricarica nulla.
- **Chi entra in vetrina**: le app pubblicate più quelle spente dalla piattaforma per cui l'account ha un
  abbonamento — chi paga deve leggere il perché; un'app spenta e mai sottoscritta resta invisibile.
- **Stati**: un tipo di dominio con derivazione pura, che *consuma* la regola unica di accesso (UC 0077) e il
  ciclo di vita dell'abbonamento (UC 0026) invece di ri-derivarli. Caso deciso esplicitamente: un'app con
  fascia gratuita e nessun abbonamento è `active`, non `available`.
- **Ruoli**: il pulsante d'acquisto è abilitato ai soli `owner`, perché quello è il divieto reale del backend.
- **Ricerca e paginazione lato client**, pagina da 6: il catalogo è piccolo e la ricerca deve filtrarlo tutto.
- **Copertura end-to-end**: coperta ora — nuovo percorso `L2-CATALOG` e journey `J-BUY` esteso.

**Guasto trovato e corretto in corsa** (dalla suite completa, non dai test mirati): una singola riga di
listino con un ciclo di fatturazione fuori dall'enum faceva fallire l'intera lettura del catalogo, perché i
prezzi venivano caricati come entità. Ora prezzo di partenza e fascia gratuita si leggono in SQL nativo — il
ciclo qui è un'etichetta, non un valore su cui decidere — con un test di regressione dedicato e, in più, due
letture per l'intera pagina invece di una per fascia.

Il test di regressione ha poi reso **attiva** la stessa fragilità, fino a quel momento latente, in
`EntitlementReadModel` (faceva fallire `AccountDeletionApiTest`): anche lì la ricerca della fascia gratuita
chiede ora l'**esistenza** di un prezzo invece di caricarne le entità. È il percorso che governa l'accesso:
non poteva restare esposto a una riga malformata. I tre punti in cui il ciclo *è* l'informazione cercata
(avvio del checkout, elenco delle fasce, cambio fascia) restano come sono.

## Invarianti appgrove

- **`tenant_id` solo dal JWT verificato** — il read-model ricava l'account da `CallerContext` (claim
  verificato) e **non espone alcun parametro** con cui indicarne uno: non c'è superficie da abusare. Provato
  dal test `tenantMancanteEFailClosed` (token senza `tenant_id` → 403 fail-closed).
- **Filtro row-level `WHERE tenant_id = :tid`** — gli abbonamenti si leggono via `SubscriptionRepository`,
  soggetto al discriminator di multi-tenancy. Il catalogo, le fasce e i prezzi sono dati di piattaforma e le
  due letture native aggiunte non contengono né potrebbero contenere un `tenant_id`. Provato da
  `vetrinaIsolataPerAccount` (stessa app, due account, due stati).
- **Modulo Terraform `microsaas_app`** — non toccato: nessuna infrastruttura nuova.
- **Logging strutturato** — `catalog.read apps=N` con `tenant_id`/`user_id`/`app_id` iniettati dal filtro MDC
  di `commons`.

## Note per il revisore

- **Contratto cross-area**: nuovo endpoint `GET /api/platform/v1/me/catalog` (sola aggiunta). Spec OpenAPI e
  tipi del frontend rigenerati nello stesso commit.
- **Nessuna migrazione di database**, nessuna colonna nuova: la vetrina si deriva da ciò che esiste.
- **Billing è rimasta com'era** (griglia d'acquisto compresa): la sua pulizia è UC 0096. Le due vie
  all'acquisto convivono per una change.
- **Decisioni differite tracciate**: nei punti aperti di **UC 0095** (nessuna via all'acquisto dalla vetrina
  per un'app freemium — la decisione è di UC 0096; ricerca/paginazione lato server; persistenza della parte
  descrittiva se il catalogo diventasse modificabile da console, tema di UC 0022); nei punti aperti di
  **UC 0046** (il generatore non compila categoria e descrizioni); in **`docs/_BACKLOG.md`** (resta aperta
  la sola domanda se il ciclo fuori catalogo vada **rifiutato in scrittura** con un vincolo di dominio, tema
  di UC 0022).
- **Gate parità scaffold**: percorso-sorgente toccato (`pricing/fatture.yaml`) → **modelli aggiornati**
  (esempio commentato dei due campi nei due listini-modello), collaudo di parità verde.
- **Gate privacy**: nessun segnale. Nessun dato personale nuovo, nessuna classificazione, nessun cambio
  MAJOR/MINOR.
- **Copertura e2e**: voce aggiunta (`L2-CATALOG`) e voce estesa (`J-BUY`); UC 0095 non è più esente.
- **Promemoria landing**: la landing di `fatture` è pubblicata e la change tocca il suo file di listino, ma
  solo nella parte descrittiva della vetrina interna (non feature, non prezzi): **non** è resa stantìa.
- **Costo del journey**: `J-BUY` compra ora due app (Teams dalla vetrina per provare la transizione della
  card, Mini-CRM da Billing per provare il montaggio del modulo) — un giro di webhook in più nel journey più
  lungo della suite. Il perché sta nei punti aperti di UC 0095: il Mini-CRM ha una fascia gratuita, quindi in
  vetrina non è acquistabile.

## Test

- **Backend** (`services/core`): `CatalogAppStateTest` (derivazione pura dei sei stati, tutte le
  combinazioni) e `CatalogApiTest` (9 test: stati contro il seed multi-stato, app spenta e mai sottoscritta
  fuori dalla vetrina, isolamento fra account, baseline gratuita, prezzo di partenza e "gratis", parte
  descrittiva nelle 5 lingue, ciclo di fatturazione fuori catalogo, fail-closed sul tenant, vetrina aperta a
  ogni ruolo ma non agli anonimi). **`mvn test` verde** su tutti i servizi.
- **Frontend** (unit/componente): `catalogApi.test.ts` (descrizione localizzata con ripiego, tinta dichiarata
  e derivata, ricerca, paginazione con pagina fuori intervallo) e `AppCatalogPage.test.tsx` (8 test: card e
  prezzo, "Free", i sei stati con la loro azione, member senza acquisto, ricerca e stato vuoto, paginazione,
  errore ≠ vuoto, avvio del checkout). Aggiornata l'asserzione del menu (`Sidebar.test.tsx`).
- **End-to-end livello 2**: `frontend/apps/backoffice/e2e/catalog.spec.ts`, 6 test etichettati `[L2-CATALOG]`.
- **Journey di piattaforma**: `J-BUY` esteso — l'acquisto parte dalla vetrina e la card passa da proposta
  d'acquisto ad app in uso, con il webhook reale.
- **Suite completa** `./run-tests.sh` (backend, frontend, infra, compliance, tooling, smoke, platform, site):
  **verde**.

## Stato criteri di accettazione

- [x] Voce **App catalog** nel menu sotto Dashboard per ogni utente autenticato, con l'elenco dal backend
- [x] Card con nome, descrizione localizzata, tinta di categoria, badge di stato e azione contestuale; prezzo
      di partenza sulle card acquistabili
- [x] I sei stati resi correttamente contro dati veri (compresa l'app spenta dalla piattaforma)
- [x] Ricerca su nome e descrizione, conteggio, ritorno alla prima pagina, stato vuoto ≠ errore con "riprova"
- [x] Paginazione con "Page N of M" e bordi disabilitati
- [x] Member senza azione d'acquisto e con la spiegazione; backend che rifiuta comunque il checkout
- [x] Acquisto completato dalla vetrina: la card della stessa app non è più acquistabile
- [x] Testi dell'interfaccia in tutte e 5 le lingue
- [x] `./run-tests.sh` verde e registro di copertura end-to-end coerente
