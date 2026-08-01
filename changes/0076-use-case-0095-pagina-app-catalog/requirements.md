# Change 0076: Pagina "App catalog" del backoffice

**Branch**: `change/0076-use-case-0095-pagina-app-catalog`
**Aree**: `services/core`, `frontend`
**Data**: 2026-08-01
**Autore**: Platform Engineering (modalità fast, skill `go-fast`)
**Use case sorgente**: [`docs/usecases/21-catalogo-app-backoffice/0095-pagina-app-catalog.md`](../../docs/usecases/21-catalogo-app-backoffice/0095-pagina-app-catalog.md)
**Tocca dati personali?**: No — il catalogo espone dati di listino (già pubblici per definizione) e lo stato di
abbonamento dell'account, che è già esposto oggi dalle viste `/me/entitlements` e `/me/subscriptions`. Nessun
nuovo trattamento, nessuna nuova categoria di dati, nessun nuovo destinatario.

## Problema / Obiettivo

Oggi l'elenco delle app che un cliente può attivare **non viene dal catalogo reale**: la pagina Billing lo
costruisce dai moduli impacchettati nel frontend (il registro `MODULES`). Ne discendono tre difetti:

1. **Il catalogo mente.** Un'app che esiste nel listino del backend ma non ha (ancora) un modulo impacchettato
   non è acquistabile; un modulo impacchettato ma non pubblicato a listino appare comunque.
2. **La scoperta è annegata nella fatturazione.** "Quali app esistono" e "quanto pago" sono due domande
   diverse, e oggi vivono nella stessa pagina.
3. **Lo stato dell'app per questo account è invisibile o incoerente.** Un'app spenta dalla piattaforma
   (UC 0076) sparisce dal menu laterale ma resta "Active" in Billing, senza spiegazione.

Obiettivo: una **pagina "App catalog"** dedicata nel backoffice — voce di menu sotto Dashboard — che mostra
**il catalogo reale servito dal backend**, con per ogni app il suo **stato effettivo per questo account**, e da
cui parte l'attivazione riusando il flusso di checkout esistente.

## Scope

### Backend (`services/core`)

- **Read-model "catalogo per questo account"**: una nuova lettura autenticata che, per ogni app del catalogo,
  restituisce nome, descrizione breve **nelle 5 lingue**, categoria (tinta/icona), **prezzo di partenza** e
  **stato per l'account chiamante**.
- **Quali app compaiono**: le app **pubblicate** (attive nel catalogo) più le app **spente dalla piattaforma
  per le quali questo account ha un abbonamento vivo** — chi paga deve vedere perché non può usarla. Un'app
  spenta e mai sottoscritta **non** si mostra: non si fa vetrina di ciò che la piattaforma ha deciso di non
  vendere.
- **I sei stati** di UC 0095, derivati dalle **stesse fonti** già in uso per la regola unica di accesso
  (UC 0077) e per il ciclo di vita dell'abbonamento (UC 0026), senza riderivarne la logica:
  `available` · `active` · `trial` · `payment pending` · `cancellation scheduled` · `disabled by platform`.
  Il caso "app con fascia gratuita e nessun abbonamento" è **`active`**, non `available`: l'app è già usabile e
  compare nel menu laterale; offrire "Subscribe" sarebbe una bugia.
- **Prezzo di partenza**: il prezzo più basso fra quelli vivi dell'app (preferendo il ciclo mensile). Un'app
  con sole fasce gratuite non ha prezzo di partenza e va presentata come tale, non come "€0".
- **Descrizione e categoria** diventano un dato di **listino** (pricing-as-code): il catalogo è codice, e
  questa è la sua parte descrittiva. Sono **facoltative**: un'app che non le dichiara resta presentabile.
- Nessuna modifica al checkout, agli entitlement, alle subscription o al loro schema dati.

### Frontend (`frontend`)

- **Nuova pagina "App catalog"** raggiungibile da **ogni utente autenticato** dell'account (è la vetrina:
  nessun entitlement richiesto), con **voce di menu nel gruppo PLATFORM subito sotto Dashboard**.
- **Griglia di card** fedele al riferimento visivo approvato (change 0066): testata con tinta di categoria,
  titolo, badge di stato, descrizione breve, riga finale con azione contestuale e prezzo. 2–3 card per riga su
  desktop, 1 su mobile.
- **Azione contestuale per stato**: `available` → *Subscribe* (flusso di checkout esistente); `active`/`trial`
  → *Open* (rotta del modulo) con piano o scadenza della prova; `payment pending` → *Fix payment* (Billing);
  `cancellation scheduled` → *Undo cancellation* (Billing); `disabled by platform` → *Contact support*, mai
  un'azione d'acquisto.
- **Ricerca** su nome e descrizione, con **conteggio dei risultati visibile**; **paginazione** con
  *Previous / Next* e "Page N of M"; una ricerca riporta alla prima pagina.
- **Stati della pagina**: caricamento, errore leggibile con "riprova", risultato vuoto della ricerca — e
  l'errore **non** deve mai essere confuso con "nessuna app".
- **Member**: le card `available` mostrano il prezzo ma l'azione d'acquisto è **disabilitata con la
  spiegazione** ("chiedi a un owner"); il divieto vero resta sul backend.
- **Localizzazione a 5 lingue** di tutti i testi dell'interfaccia; la descrizione dell'app segue la lingua
  attiva dell'interfaccia senza dover ricaricare i dati.

### Test

- Unit backend sulla derivazione dei sei stati dalle combinazioni (stato dell'app × abbonamento × fascia
  gratuita) e sul prezzo di partenza; test di contratto dell'endpoint (autenticazione, `tenant_id` solo dal
  token).
- Unit frontend su ricerca, paginazione e mappatura stato → azione.
- **End-to-end di livello 2** (browser vero, backend simulato): nuova batteria dedicata al catalogo —
  ricerca con e senza risultati, paginazione, le azioni per ciascuno stato, member senza azione d'acquisto,
  errore di lettura distinto dal catalogo vuoto.
- **Journey di piattaforma**: estensione di **J-BUY** — l'acquisto parte **dal catalogo** e, dopo
  l'attivazione, la card della stessa app passa da `available` ad `active`.
- Registro di copertura end-to-end aggiornato: UC 0095 esce dalle esenzioni ed entra fra gli use case con
  superficie.

## Fuori scope

- **Pulizia della pagina Billing** (rimozione della griglia d'acquisto "Get an app"): è **UC 0096**. In questa
  change Billing resta com'è — le due vie all'acquisto convivono per una change.
- **Nuova Dashboard operativa** e Workspace ID in Account: è **UC 0097**.
- **Modifiche al flusso di checkout** (UC 0024): il catalogo ne è un nuovo ingresso, non una riprogettazione.
- **Ricerca e paginazione lato server**: il catalogo è un elenco piccolo e limitato (decine di app al
  massimo); portarle sul backend ora sarebbe complessità senza beneficio. Rimando scritto nei punti aperti di
  UC 0095.
- **Autorare descrizione e categoria per le app generate dalla skill `new-application`**: il generatore non può
  produrre cinque traduzioni da solo. Rimando scritto nei punti aperti di UC 0046.
- Icone di categoria disegnate: la tinta di categoria e l'iniziale dell'app bastano al riferimento visivo
  approvato.

## Criteri di accettazione

- [ ] Con lo stack locale avviato, la voce **App catalog** compare nel menu sotto Dashboard per ogni utente
      autenticato e apre una pagina che elenca **le app del catalogo del backend** (non i moduli impacchettati).
- [ ] Ogni card mostra nome, descrizione nella lingua attiva, tinta di categoria, badge di stato corretto e
      l'azione prevista per quello stato; il prezzo di partenza compare sulle card `available` con prezzo.
- [ ] I sei stati sono resi correttamente contro dati veri: app spenta dalla piattaforma con abbonamento vivo →
      *Disabled by platform* senza azione d'acquisto; app in prova → *Trial* con la scadenza; abbonamento
      scaduto di pagamento → *Payment pending* con *Fix payment*; disdetta programmata → *Undo cancellation*.
- [ ] La ricerca filtra su nome e descrizione, mostra il conteggio, riporta alla prima pagina e ha uno stato
      vuoto dedicato distinto dall'errore di lettura, che offre "riprova".
- [ ] La paginazione mostra "Page N of M" e disabilita *Previous*/*Next* ai bordi.
- [ ] Un utente `member` vede prezzi e stati ma non può avviare l'acquisto, e legge il perché; il backend
      rifiuta comunque l'avvio del checkout a chi non è owner.
- [ ] Un acquisto completato dal catalogo riporta la card della stessa app da `available` ad `active`.
- [ ] I testi dell'interfaccia esistono in tutte e 5 le lingue.
- [ ] `./run-tests.sh` (suite completa) verde, registro di copertura end-to-end coerente.

## Invarianti appgrove toccati

- **`tenant_id` solo dal JWT verificato** — il read-model del catalogo ricava l'account dal token verificato e
  **non accetta alcun identificativo di account dal client**: non c'è nessun parametro che lo consenta.
- **Filtro row-level `WHERE tenant_id = :tid`** — la parte tenant-scoped della lettura (gli abbonamenti) passa
  dal filtro automatico di multi-tenancy già in vigore; il catalogo in sé è dato di piattaforma, non
  tenant-scoped, e non deve introdurre letture incrociate.
- **Modulo Terraform `microsaas_app`** — non pertinente: nessuna infrastruttura nuova.
- **Logging strutturato** — la lettura del catalogo e l'avvio del checkout dal catalogo emettono log con
  `tenant_id`, `app_id`, `user_id` secondo la convenzione in uso.

## Requisiti di test

- **Guardia di isolamento**: due account con abbonamenti diversi alla stessa app devono ottenere stati di card
  diversi dalla stessa lettura, e nessuno dei due deve vedere l'abbonamento dell'altro.
- **Guardia di coerenza**: lo stato mostrato dal catalogo per un'app spenta dalla piattaforma deve essere
  `disabled by platform` **anche** quando l'abbonamento sottostante è formalmente attivo — è esattamente
  l'incoerenza che la storia esiste per chiudere.
- **Guardia di ruolo**: l'avvio del checkout resta rifiutato dal backend a un utente non owner, a prescindere
  da ciò che l'interfaccia mostra.
- **Errore ≠ vuoto**: un guasto di lettura del catalogo deve produrre lo stato di errore con "riprova", mai lo
  stato "nessuna app".

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | Sì — nuova lettura HTTP frontend ↔ `services/core` (solo aggiunta; contratto OpenAPI rigenerato) |
| Version bump | minor |
