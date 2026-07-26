# UC 0077 — Provider entitlement reale del backoffice/admin

**Area**: 15-supporto-e-piattaforma · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0013 (Accounts/Users/Invitations + core REST API), UC 0020 (Backoffice SPA shell), UC 0021 (Admin console SPA), UC 0027 (Enforcement entitlement + quota SPI), UC 0025 (Pipeline webhook → subscription materializzata), UC 0024 (Checkout — polling post-checkout)
**Fonte**: R11 (Tabella residui _INDEX.md) · docs/_BACKLOG.md §UC0020/UC0021 #5
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope

Sostituire lo **stub** degli entitlement del backoffice con la loro **fonte reale**. Oggi la shell del backoffice usa uno
`StubEntitlementsProvider` con un insieme di app **statico**, perché il core **non** espone un endpoint che dica quali app
sono davvero abilitate per l'account che sta usando l'interfaccia. Il risultato è che il backoffice "sa" quali app mostrare
solo per finta.

Questo use case introduce l'**endpoint core mancante** — per esempio `GET /api/platform/v1/entitlements` — che restituisce
l'elenco degli `app_id` abilitati per l'account **derivato dal token**, calcolato dalla stessa logica già usata altrove:
abbonamento materializzato (`platform.subscription`) **incrociato** con lo stato dell'app (`app.status`). Lato interfaccia, si
consuma l'endpoint tramite il client API e la libreria di gestione delle richieste (TanStack Query), e si **rimpiazza solo**
`StubEntitlementsProvider` con l'implementazione reale: nessun'altra parte della shell cambia.

**Incluso**: l'endpoint core degli entitlement dell'account; il consumo lato backoffice via client API + TanStack Query; la
sostituzione del solo provider; il riuso della derivazione entitlement (account × app) già introdotta per la console admin.
**Escluso**: cambi alla catena di enforcement (resta di UC 0027); cambi al modello dati dell'abbonamento (UC 0025); la
definizione della disabilitazione app (UC 0076, sorella di questo use case, che alimenta `app.status`).

## 2. Attori & ruoli

- **Utente dell'account** (proprietario/amministratore/membro, UC 0013): il backoffice, per suo conto, chiede gli
  entitlement reali per decidere quali app mostrare.
- **Sistema (core)**: espone l'endpoint e calcola l'elenco `app_id` derivando da `platform.subscription` e `app.status`.
- **Backoffice (shell)**: chiama l'endpoint tramite il provider reale al posto dello stub.
- **Console admin** (UC 0021): fonte della **derivazione entitlement** già esistente (account × app), da riusare — non da
  reinventare.

## 3. Precondizioni

- Core con API REST disponibile — UC 0013 (Accounts/Users/Invitations + core REST API).
- Shell backoffice con lo stub attuale — UC 0020 (Backoffice SPA shell) — e la sua astrazione `EntitlementsProvider`.
- Abbonamento materializzato disponibile come fonte — UC 0025 (Pipeline webhook → subscription materializzata).
- Stato app (`app.status`) come secondo ingrediente della derivazione — coerente con UC 0076 (Disabilita applicazione) e
  con l'esclusione delle app non `active` di UC 0027 (Enforcement entitlement + quota SPI).
- La derivazione entitlement (account × app) è **già** stata introdotta lato admin e riusabile.

## 4. Flusso principale

1. Il backoffice si avvia; il provider entitlement reale chiede al core `GET /api/platform/v1/entitlements`.
2. Il core ricava l'`account` (`tenant_id`) **dal token verificato**, calcola l'elenco `app_id` abilitati incrociando
   `platform.subscription` (abbonamenti attivi) con `app.status` (solo app `active`), e restituisce l'elenco.
3. Il backoffice, tramite TanStack Query, memorizza il risultato e mostra **solo** le app effettivamente abilitate; le app
   non abilitate non compaiono nel menu.
4. Al cambiare degli entitlement (nuovo acquisto, disdetta, disabilitazione app), una **nuova lettura** dell'endpoint
   aggiorna l'interfaccia.
5. Tutte le chiamate sono loggate con `tenant_id`, `user_id` (e `app_id` dove pertinente).

## 5. Flussi alternativi / edge / errori

- **Polling post-checkout (raccordo con UC 0024)**: dopo un acquisto, il checkout deve **attendere** che l'entitlement
  compaia (l'abbonamento si materializza in modo asincrono via webhook). Occorre **valutare se è lo stesso endpoint**: se
  `GET /api/platform/v1/entitlements` serve anche al polling post-checkout (UC 0024, Checkout — polling post-checkout) si
  ha un'unica fonte di verità; altrimenti i due endpoint vanno tenuti coerenti. La decisione è tracciata nei Punti aperti.
- **Edge — nessun entitlement**: un account senza abbonamenti attivi riceve un elenco **vuoto**; il backoffice mostra lo
  stato "nessuna app attiva" con invito all'acquisto, senza errori.
- **Edge — app disabilitata dalla piattaforma**: se un'app è `disabled` (UC 0076), la derivazione la **esclude** anche se
  l'abbonamento esiste; l'interfaccia non la mostra. Coerente con l'enforcement di UC 0027.
- **Errore — endpoint non raggiungibile**: TanStack Query gestisce ritentativi e stato di errore; il backoffice mostra un
  messaggio chiaro e **non** cade sullo stub (lo stub resta solo per lo sviluppo locale, non è un ripiego in produzione).
- **Coerenza con l'enforcement (gate 402)**: gli entitlement mostrati dal backoffice devono **combaciare** con ciò che il
  servizio effettivamente concede; se il backoffice mostrasse un'app che poi l'enforcement (UC 0027) nega con un rifiuto
  tipizzato (per esempio "pagamento richiesto"), l'utente vivrebbe un'incoerenza. La stessa derivazione da entrambi i lati
  evita questo scarto.

## 6. Schermate & stati

Questo use case è **soprattutto infrastrutturale** (un endpoint + la sostituzione di un provider), ma ha effetti visibili
sulla shell del backoffice e tocca la console admin come fonte della derivazione.

- **Backoffice — menu/registro app** (UC 0020, Backoffice SPA shell):
  - il menu delle app riflette gli entitlement **reali** invece dell'insieme statico dello stub;
  - **stati**: caricamento (mentre si attende l'endpoint), vuoto ("nessuna app attiva" con azione verso l'acquisto),
    errore (messaggio chiaro con ritentativo), pronto (menu con le sole app abilitate).
- **Console admin — matrice entitlement** (UC 0021, Admin console SPA):
  - resta la vista `platform-admin` sulla derivazione account × app; questo use case **riusa** quella derivazione lato core
    per servire il backoffice, quindi le due viste devono restare coerenti (stessa logica, stessa fonte).
- **Copy**: italiano e inglese, chiaro; i messaggi degli stati vuoto/errore spiegano cosa fare senza gergo.

## 7. Dati toccati

- **`platform.subscription`** (materializzata, UC 0025): fonte degli abbonamenti attivi dell'account — **sola lettura** qui.
- **`App.status`** (catalogo): secondo ingrediente della derivazione (solo `active`) — **sola lettura** qui, alimentato da
  UC 0076 (Disabilita applicazione).
- **Nessuna nuova tabella**: l'endpoint **calcola** l'elenco `app_id`, non introduce persistenza nuova.
- **Dati personali**: nessun nuovo trattamento; l'endpoint restituisce un elenco di identificatori tecnici di app per
  l'account del token. L'account e l'utente sono già trattati (UC 0013). Log strutturato con `tenant_id`, `user_id`.

## 8. Permessi & gate

- **`tenant_id` solo dal token verificato**: l'endpoint del backoffice ricava l'account **esclusivamente** dal claim
  verificato; **mai** da corpo o parametri. È il presidio che impedisce a un utente di chiedere gli entitlement di un altro
  account.
- **Filtro riga per riga** `WHERE tenant_id = :tid` sulla lettura di `platform.subscription`.
- **Eccezione controllata cross-account (solo lato admin)**: la **matrice entitlement** della console admin (UC 0021) legge
  la derivazione per **tutti** gli account — è l'eccezione esplicita all'invariante di filtro, ammessa **solo** per il
  ruolo `platform-admin` e in **sola lettura**. L'endpoint del **backoffice**, invece, resta rigorosamente tenant-scoped.
- **Ruoli**: l'endpoint backoffice è accessibile a qualsiasi utente autenticato dell'account (per popolare il proprio menu);
  la vista cross-account resta a `platform-admin`.
- **Coerenza con i gate a valle**: la derivazione mostrata deve allinearsi con la catena di enforcement (UC 0027), incluso
  il rifiuto tipizzato "pagamento richiesto", per non mostrare app che il servizio poi nega.

## 9. Requisiti di test

- **Unità**: la derivazione = `platform.subscription` attivo **∩** app `active`; un'app disabilitata è esclusa anche con
  abbonamento presente; account senza abbonamenti → elenco vuoto.
- **Integrazione** (database reale, tipo Testcontainers): l'endpoint restituisce gli `app_id` corretti dal token; cambiando
  `app.status` a `disabled` (UC 0076) l'app sparisce dalla risposta.
- **Isolamento fra account**: l'endpoint backoffice dell'account A non restituisce **mai** entitlement dell'account B (test
  di sicurezza esplicito); la matrice admin invece li vede tutti (gate `platform-admin`).
- **Coerenza con enforcement**: ciò che l'endpoint mostra combacia con ciò che l'enforcement (UC 0027) concede/nega.
- **Frontend**: la shell consuma l'endpoint via TanStack Query; sostituito il solo provider, gli stati caricamento/vuoto/
  errore/pronto funzionano; lo stub resta confinato allo sviluppo locale.
- **End-to-end** (Playwright): login → il menu app riflette gli entitlement reali; dopo una disabilitazione l'app sparisce.
- Verde su `run-tests.sh` per le aree toccate (backend, frontend) prima del merge.

## 10. Riferimenti & Definition of Done

- **Fonte**: R11 della tabella dei residui in `_INDEX.md`; `docs/_BACKLOG.md` §UC0020/UC0021 #5.
- **Use case sorelle**: [UC 0075 (Ticketing nativo in-house)](0075-ticketing-nativo-in-house.md),
  [UC 0076 (Disabilita applicazione)](0076-disabilita-applicazione.md).
- **Definition of Done**:
  1. endpoint core degli entitlement dell'account (es. `GET /api/platform/v1/entitlements`), account **dal token**,
     derivazione `subscription` attiva ∩ app `active`;
  2. backoffice consuma l'endpoint via client API + TanStack Query, con `StubEntitlementsProvider` **sostituito** dal
     provider reale (stub confinato allo sviluppo locale);
  3. riuso della derivazione entitlement già introdotta lato console admin (una sola logica, coerente fra le due viste);
  4. coerenza verificata con la catena di enforcement (UC 0027) e con l'esclusione delle app disabilitate (UC 0076);
  5. isolamento fra account verificato da un test di sicurezza dedicato;
  6. decisione tracciata su "stesso endpoint del polling post-checkout (UC 0024)" o due endpoint coerenti;
  7. `run-tests.sh` verde sulle aree toccate.

## Punti aperti / decisioni differite

- **Unico endpoint per menu e polling post-checkout**: valutare se `GET /api/platform/v1/entitlements` serva **anche** il
  polling post-checkout di UC 0024 (Checkout — polling post-checkout). Un'unica fonte di verità è preferibile; se i due usi
  richiedono forme diverse (per esempio il polling vuole sapere "questa specifica app è già attiva?" con una risposta più
  snella), tenerli **coerenti** sulla stessa derivazione. Decisione da prendere in implementazione; possiede il punto
  l'epica 15 in coordinamento con chi possiede UC 0024.
- **Freschezza vs cache**: TanStack Query mette in cache; definire la strategia di invalidazione dopo eventi che cambiano gli
  entitlement (acquisto, disdetta, disabilitazione app) per evitare menu stantii — dettaglio di implementazione.
- **Forma della risposta**: se restituire solo l'elenco `app_id` o anche metadati (fascia, stato dell'abbonamento) utili
  alla shell; default proposto: elenco `app_id`, arricchibile in seguito senza rompere i consumatori.
- **Confine con la disabilitazione app**: la correttezza di questo use case dipende dal fatto che `app.status` sia gestito
  (UC 0076); i due use case dell'epica 15 vanno implementati in modo coerente.
