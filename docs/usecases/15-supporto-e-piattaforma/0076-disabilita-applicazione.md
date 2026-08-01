# UC 0076 — Disabilita applicazione (feature admin reversibile)

**Area**: 15-supporto-e-piattaforma · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0021 (Admin console SPA), UC 0027 (Enforcement entitlement + quota SPI), UC 0014 (Authorizer all'edge), UC 0035 (Job retention/purge — archivio audit), UC 0006 (Observability baseline)
**Fonte**: R7 (Tabella residui _INDEX.md) · docs/_BACKLOG.md §Configurazione admin
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope

Dare al fondatore/agente una **leva reversibile** per rendere un'applicazione del marketplace **indisponibile a tutti gli
account contemporaneamente**, agendo su catalogo ed entitlement, **senza toccare dati né infrastruttura**. È l'interruttore
"metti in pausa questa app per tutti" — utile per un incidente, una manutenzione, un problema legale o commerciale — che si
**annulla** riportando l'app disponibile.

Va tenuta ben distinta dalla skill **distruttiva** `drop-application` (UC 0048), che **dismette** un'app rimuovendo servizio,
modulo frontend, dati e infrastruttura: quella è irreversibile e definitiva, questa è un semplice **toggle di stato**.

Punto chiave, già vero oggi: l'**applicazione della regola** è **già presente nel servizio**. Il modello di lettura degli
entitlement (`EntitlementReadModel`, UC 0027, Enforcement entitlement + quota SPI) **scarta** le app con stato diverso da
`active` — decisione presa nella change 0039. Questo use case **non** reintroduce l'enforcement: aggiunge la **leva
amministrativa** (il comando che scrive `app.status` dalla console admin), il **modello** che quel comando aggiorna, e
l'**audit** dell'azione.

**Incluso**: il toggle `app.status` (`active` ⇄ `disabled`) nella console admin; il modello e la sua persistenza; l'audit
dell'azione con chi/quando/perché; i testi e le conferme. **Escluso**: qualsiasi cancellazione o migrazione di dati; ogni
modifica infrastrutturale; la dismissione definitiva (→ UC 0048 via skill `drop-application`).

## 2. Attori & ruoli

- **Fondatore / agente** (ruolo `platform-admin`): esegue e annulla la disabilitazione dalla console admin.
- **Sistema (servizio)**: legge `app.status` e, tramite `EntitlementReadModel` (UC 0027), esclude l'app disabilitata dagli
  entitlement effettivi — comportamento **già esistente**.
- **Utenti degli account**: subiscono l'effetto (l'app sparisce dai loro entitlement); non hanno alcun comando qui.

## 3. Precondizioni

- Console admin disponibile — UC 0021 (Admin console SPA).
- Enforcement entitlement attivo con l'esclusione delle app non `active` — UC 0027 (Enforcement entitlement + quota SPI),
  come deciso nella change 0039.
- Esiste almeno un'app nel catalogo con un `app.status` gestibile.
- L'operatore è autenticato come `platform-admin`.

## 4. Flusso principale

1. Il `platform-admin` apre la sezione app della console admin e individua l'app da disabilitare.
2. Preme "Disabilita app"; l'interfaccia chiede **conferma esplicita** spiegando l'effetto (l'app diventa indisponibile a
   **tutti** gli account, operazione **reversibile**, i **dati restano intatti**) e può chiedere una **nota/motivazione**.
3. Il core imposta `app.status = disabled` e **registra un evento di audit** (chi, quando, azione, app, motivazione).
4. Da quel momento `EntitlementReadModel` (UC 0027) **scarta** l'app: alla successiva lettura degli entitlement, gli account
   non la vedono più fra le app attive; i tentativi di uso applicativo cadono nella catena di enforcement.
5. Per riattivarla, il `platform-admin` preme "Riabilita app": `app.status` torna `active`, si registra un nuovo evento di
   audit, e l'app ricompare negli entitlement effettivi.
6. Ogni azione è tracciata con **log strutturato** (`app_id`, `user_id` dell'operatore; `tenant_id` dell'operatore di
   piattaforma).

## 5. Flussi alternativi / edge / errori

- **Edge — traffico che raggiunge comunque il container**: il blocco **non** è all'edge. L'authorizer (UC 0014, Authorizer
  all'edge) verifica solo il token e **non legge il database**, quindi non conosce `app.status`: le richieste verso un'app
  disabilitata **arrivano comunque** al servizio, che le respinge tramite l'enforcement (UC 0027). È una **rinuncia
  consapevole**: risparmia complessità all'edge accettando che il rifiuto avvenga nel servizio. Se in futuro servisse un
  interruttore a monte (kill-switch all'edge), è tracciato nei Punti aperti.
- **Edge — sessioni/token già emessi**: gli entitlement possono essere già presenti in un token/cache lato client fino alla
  successiva rilettura; l'effetto pieno si vede al refresh degli entitlement. Va spiegato nel copy dell'admin.
- **Edge — abbonamenti attivi su un'app disabilitata**: disabilitare **non** annulla gli abbonamenti (i dati restano); alla
  riabilitazione l'accesso torna. La gestione commerciale (rimborsi, comunicazioni) è fuori scopo qui.
- **Errore — app inesistente o già nello stato richiesto**: risposta tipizzata (*problem+json*); il toggle è **idempotente**
  (disabilitare un'app già `disabled` non fa danni e non duplica audit incoerente).
- **Errore — permesso mancante**: un chiamante senza ruolo `platform-admin` riceve un rifiuto; nessuna modifica di stato.

## 6. Schermate & stati

- **Console admin — sezione app** (UC 0021, Admin console SPA):
  - elenco app del catalogo con **badge di stato** (`Attiva` / `Disabilitata`);
  - per ciascuna, il pulsante contestuale "Disabilita" oppure "Riabilita";
  - **dialogo di conferma** che descrive l'effetto ("indisponibile a tutti gli account", "reversibile", "i dati restano")
    e ospita la **nota/motivazione** opzionale;
  - dopo l'azione, il badge cambia stato e compare una conferma; l'elenco riflette il nuovo `app.status`.
- **Stati**: caricamento (lista app), errore (messaggio tipizzato, nessun cambio di stato), successo (badge aggiornato),
  stato "già in quel valore" gestito senza errore visibile fastidioso.
- **Copy**: italiano e inglese, chiaro; distinzione netta e visibile fra "Disabilita" (reversibile, questo use case) e la
  ben più grave dismissione definitiva (skill `drop-application`, UC 0048), per evitare confusioni pericolose.
- **Vista audit**: la lista/archivio degli eventi di disabilitazione/riabilitazione è consultabile (chi, quando, app,
  motivazione), coerente con l'audit di piattaforma.

## 7. Dati toccati

- **`App.status`** (catalogo, schema `platform`): unico campo scritto (`active` ⇄ `disabled`). Nessun'altra tabella dati è
  toccata: **niente** dati di account, di abbonamento o applicativi.
- **Audit dell'azione** (registro di audit di piattaforma): riga con operatore, timestamp, app, azione, motivazione;
  confluisce nell'**archivio audit** gestito dal job di conservazione (UC 0035, Job retention/purge) e nell'osservabilità
  (UC 0006, Observability baseline).
- **Dati personali**: nessun trattamento nuovo di dati personali degli **utenti finali**; l'audit registra l'identità
  dell'**operatore di piattaforma** (già trattata per finalità di sicurezza/tracciabilità). Nessun campo dati di account
  viene letto o scritto.

## 8. Permessi & gate

- **Solo `platform-admin`**: l'azione è ristretta al ruolo di piattaforma; nessun ruolo di account può disabilitare un'app.
- **`tenant_id` solo dal token verificato**: pur essendo un'azione **cross-account per effetto** (tocca tutti gli account),
  la scrittura riguarda il **catalogo** (`app.status`), non i dati di un tenant; non si accede a dati tenant-scoped, quindi
  non si viola il filtro riga per riga — non c'è lettura di righe di account. L'eccezione cross-account è quindi **limitata
  al catalogo** e gated dal ruolo `platform-admin`.
- **Enforcement a valle immutato**: l'esclusione dell'app dagli entitlement resta responsabilità di `EntitlementReadModel`
  (UC 0027); questo use case non duplica quel controllo, lo **alimenta** cambiando `app.status`.
- **Idempotenza e audit obbligatorio**: ogni transizione di stato produce **una** voce di audit; l'assenza di audit su una
  transizione è un difetto.

## 9. Requisiti di test

- **Unità**: transizione `active` ⇄ `disabled` idempotente; scrittura dell'audit a ogni transizione; rifiuto senza ruolo.
- **Integrazione** (database reale, tipo Testcontainers): dopo `disabled`, la lettura entitlement **non** include l'app;
  dopo `active`, torna a includerla — verifica del raccordo con UC 0027.
- **Confine edge (documentazione + test di comportamento)**: una richiesta verso un'app disabilitata **raggiunge** il
  servizio e viene respinta dall'enforcement, **non** dall'authorizer (UC 0014) — a conferma della rinuncia consapevole.
- **Sicurezza**: nessun ruolo di account può cambiare `app.status`; solo `platform-admin`.
- **End-to-end** (Playwright): dalla console admin, disabilita → l'app sparisce dagli entitlement di un account di prova →
  riabilita → ricompare; audit visibile.
- Verde su `run-tests.sh` per le aree toccate (backend, frontend) prima del merge.

## 10. Riferimenti & Definition of Done

- **Fonte**: R7 della tabella dei residui in `_INDEX.md`; `docs/_BACKLOG.md` §Configurazione admin.
- **Use case sorelle**: [UC 0075 (Ticketing nativo in-house)](0075-ticketing-nativo-in-house.md),
  [UC 0077 (Provider entitlement reale del backoffice/admin)](0077-provider-entitlement-reale.md).
- **Definition of Done**:
  1. toggle `app.status` (`active` ⇄ `disabled`) nella console admin, con conferma esplicita e motivazione opzionale;
  2. transizione **idempotente** e **reversibile**, che **non** tocca dati né infrastruttura;
  3. **audit** persistito di ogni transizione (operatore, timestamp, app, motivazione), in archivio (UC 0035) e
     osservabilità (UC 0006);
  4. raccordo verificato con l'enforcement già esistente (UC 0027): l'app disabilitata è esclusa dagli entitlement;
  5. distinzione netta, anche nel copy, dalla dismissione definitiva (skill `drop-application`, UC 0048);
  6. `run-tests.sh` verde sulle aree toccate.

## Punti aperti / decisioni differite

- **Kill-switch all'edge**: se in futuro servisse bloccare le richieste verso un'app disabilitata **prima** che raggiungano
  il container (per esempio per protezione da carico o per sicurezza), servirebbe far conoscere lo stato all'authorizer
  (UC 0014) o un livello a monte — oggi impossibile perché l'authorizer non legge il database. Da valutare come evoluzione;
  possiede il punto l'epica 15, in coordinamento con chi possiede UC 0014.
- **Disabilitazione per singolo account**: questo use case disabilita l'app **per tutti**. Una sospensione mirata su un
  singolo account (per esempio per morosità o abuso) è un'altra funzione, da progettare a parte se necessaria.
- **Comunicazione agli utenti**: se disabilitare un'app debba innescare una comunicazione automatica agli account impattati
  (email, banner) è una scelta di prodotto da fare separatamente; qui si registra solo l'audit interno.
- **Programmazione/finestra**: disabilitazione pianificata (manutenzione programmata con finestra oraria) non è coperta;
  eventuale evoluzione.
- ~~**Fix — pagina Billing incoerente per app disabilitata**~~ — **chiuso** nella change
  `0071-use-case-0076-disabilita-applicazione`: `/me/subscriptions` espone `appDisabled` e la card dell'abbonamento
  mostra badge "Sospesa" più l'avviso "app sospesa dalla piattaforma — abbonamento valido, dati intatti". L'abbonamento
  continua a essere elencato (per disegno UC 0028 li mostra tutti), ma non contraddice più la barra laterale.
- **Rinfresco delle proiezioni entitlement lato app** (tracciato 2026-08-01, change `0071`): disabilitare un'app
  **non** pubblica alcun evento di invalidazione degli entitlement. L'unico pubblicatore esistente
  (`EntitlementInvalidationPublisher`, UC 0027) lavora **per tenant** su un cambio di stato di fatturazione; qui il
  cambio è di catalogo e riguarda **tutti** i tenant insieme, quindi servirebbe una diffusione a tappeto che oggi non
  esiste e che, con pochi account, non ripaga. Conseguenza accettata: un'app che tiene una propria proiezione dei
  diritti può restare indietro fino al primo rinfresco utile — la stessa rinuncia già dichiarata per token e cache
  lato client. Da rivedere se e quando la disabilitazione dovrà avere effetto **immediato** ovunque.
