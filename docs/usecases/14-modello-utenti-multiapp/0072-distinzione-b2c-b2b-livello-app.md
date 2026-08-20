# UC 0072 — Distinzione B2C/B2B a livello app + semantica gestione utenti

**Area**: 14-modello-utenti-multiapp · **Fase**: evo · **Stato**: 🗄️ **SUPERATA dall'epica 22** — archivio della decisione precedente, **non da implementare**
**Dipendenze**: UC 0013 (Accounts/Users/Invitations + core REST API), UC 0059 (Gestione membri & inviti UI backoffice), UC 0051 (App #1 B2C single-user), UC 0054 (App #2 B2B multi-user via new-application), UC 0017 (Flussi auth UI)
**Fonte**: R3 (Tabella residui _INDEX.md) · docs/_BACKLOG.md §"Modello di gestione utenti — tenant-level vs per-app (B2B/B2C)"
**Ultimo aggiornamento**: 2026-07-26


> **Questa storia è superata e non va implementata.** L'epica **22 — rifacimento del modello di appartenenza**
> (change `0087`) adotta il modello **opposto** a quello di questa epica: appartenenza **centralizzata** sulla
> piattaforma, ruolo della persona **sull'applicazione**, posti **di piattaforma** a listino unico — cioè
> esattamente l'opzione che l'epica 14 registrava come scartata. Lo sviluppatore ha cambiato direzione, e la
> decisione è registrata in `changes/0087-epica-22-refactor-membership-model/decisions.json`.
>
> **Sostituita da**: [0114](../22-refactor-membership-model/story/0114-ritiro-categoria-b2c-b2b.md) la ritira e [0115](../22-refactor-membership-model/story/0115-ambito-dati-applicazione.md) ne prende il posto con l'ambito dei dati.
>
> Il documento resta come **archivio**: dice cosa si era pensato e perché non si è fatto. Cancellarlo farebbe
> perdere il ragionamento, che è la parte che serve a chi un giorno riaprirà la questione.

## 1. Obiettivo / Scope

Oggi la gestione utenti del marketplace vive **a livello di account** (in gergo tecnico *tenant*): la tabella
`platform.users` e gli inviti sono legati all'account, non alla singola app. Questo impone a **tutti** la semantica di
un prodotto "a più persone" (in gergo *B2B*, cioè azienda con più utenti), anche quando un'app è pensata per l'**uso del
solo proprietario** (in gergo *B2C*, un singolo professionista che lavora da solo).

Questo use case stabilisce che la distinzione **B2C** (solo proprietario, nessun invito) contro **B2B** (utenti
invitati) **appartiene alla singola app**, non all'account, e che il campo già esistente `App.user_model`
(l'enumerazione `AppUserModel`) è la sorgente di verità di questa distinzione. Ne discende la **semantica di gestione
utenti** che ogni app espone: un'app B2C non ha inviti né schermata "Membri" completa; un'app B2B sì.

**Incluso**: definizione del significato dei valori di `App.user_model`; cosa cambia — a livello di comportamento
osservabile e di dati — fra un'app B2C e un'app B2B; il contratto che le UC sorelle (invito per-app, directory
cross-app) assumono. **Escluso**: il meccanismo dei "posti" come quota per-app e il loro prezzo → UC 0073 (Invito
utenti per-app con "posti" come metrica quota stock); la directory cross-app e il ridisegno concreto della schermata
"Membri" → UC 0074 (Directory cross-app + UI "Membri" ripensata per-app).

## 2. Attori & ruoli

- **Proprietario dell'account** (in gergo *owner*): l'unica persona presente in un'app B2C; in un'app B2B è chi può
  invitare e amministrare gli altri.
- **Amministratore** (*admin*): in un'app B2B, delega dell'owner per la gestione utenti.
- **Membro** (*member*): utente invitato di un'app B2B; non presente nel modello B2C.
- **Chi definisce l'app** (Platform Engineer, tramite la skill `new-application`): sceglie `user_model` alla nascita
  dell'app — UC 0046 (skill `new-application`).
- **Sistema**: legge `App.user_model` per abilitare o nascondere inviti, schermate e gate.

## 3. Precondizioni

- Esiste il catalogo delle app con l'entità `App` e il campo `user_model`/`AppUserModel` già valorizzato per ogni app.
- Il nucleo account/utenti/inviti è disponibile — UC 0013 (Accounts/Users/Invitations + core REST API).
- L'account ha almeno un'app attiva (ha un *entitlement*, cioè il diritto d'accesso derivato dall'abbonamento).
- Esistono i due modelli di riferimento: un'app B2C — UC 0051 (App #1 B2C single-user) — e un'app B2B — UC 0054
  (App #2 B2B multi-user via new-application).

## 4. Flusso principale

1. Il proprietario apre un'app del proprio account nel backoffice.
2. Il sistema legge `App.user_model` per quell'app.
3. **Se `user_model = B2C`**: l'app si comporta come mono-utente. Non esiste alcuna funzione "invita"; la sezione
   "Membri" è assente o ridotta alla sola riga del proprietario (in sola lettura). Nessun invito viene mai creato per
   quest'app.
4. **Se `user_model = B2B`**: l'app espone la gestione utenti completa — lista membri dell'app, invito, revoca, cambio
   ruolo — come da UC 0074 (Directory cross-app + UI "Membri" ripensata per-app), con il gate sui "posti" di UC 0073
   (Invito utenti per-app con "posti" come metrica quota stock).
5. Ogni azione di gestione utenti resta filtrata per account (invariante `tenant_id` dal token verificato) e, in più,
   **riferita alla singola app**: un utente è "membro dell'app X", non genericamente "membro dell'account".
6. Le operazioni scrivono un log strutturato con `tenant_id`, `app_id`, `user_id` e l'esito.

## 5. Flussi alternativi / edge / errori

- **Edge — app B2C con più di un utente storico**: un account potrebbe avere, per ragioni storiche, utenti legati a
  un'app poi classificata B2C. Il sistema non deve cancellarli in automatico: mostra la riga del proprietario e tratta
  gli eventuali altri come dato legacy da migrare (annotato nei punti aperti).
- **Edge — stessa persona su un'app B2C e su un'app B2B**: legittimo. La persona è proprietario dell'app B2C e, ad
  esempio, membro invitato dell'app B2B; le due appartenenze sono indipendenti.
- **Errore — tentativo di invito su un'app B2C**: la chiamata di invito deve essere rifiutata con errore tipizzato
  (formato *problem+json*, cioè l'errore in JSON standard) perché l'operazione non è ammessa dal modello dell'app.
- **Edge — cambio di `user_model` da B2C a B2B (o viceversa)**: è una decisione di prodotto rara e delicata; non è un
  self-service. Da B2C a B2B si abilitano inviti e posti; da B2B a B2C serve una strategia per gli utenti già invitati
  (annotato nei punti aperti). Il cambio non è parte di questo use case.

## 6. Schermate & stati

La distinzione tocca direttamente la schermata "Membri" del backoffice descritta in UC 0059 (Gestione membri & inviti
UI backoffice), oggi legata all'account.

- **App B2C**: la voce "Membri" non appare nel menu dell'app, oppure appare come pannello informativo con **una sola
  riga** — il proprietario, in sola lettura — e una nota che spiega che l'app è a utente singolo. Copy suggerito:
  "Questa applicazione è pensata per un solo utente. Per lavorare in più persone serve un'app multi-utente."
- **App B2B**: la voce "Membri" appare e si comporta come da UC 0074 (Directory cross-app + UI "Membri" ripensata
  per-app).
- **Stati comuni**: caricamento, vuoto (nessun membro oltre al proprietario), errore (con messaggio tipizzato),
  successo. Testi in italiano e inglese, con attenzione all'accessibilità come per il resto del backoffice.

Il ridisegno di dettaglio della schermata appartiene a UC 0074; qui si fissa solo **quale semantica** la schermata deve
riflettere in base a `user_model`.

## 7. Dati toccati

- **`App.user_model`** (enumerazione `AppUserModel`): sorgente di verità della distinzione B2C/B2B; già presente. Nessun
  nuovo campo introdotto da questo use case.
- **`platform.users`** e **`invitations`**: nessuna modifica di semantica introdotta qui; questo use case ne prepara la
  ri-lettura "per app" che UC 0073 e UC 0074 formalizzano.
- **Dati personali degli utenti** (email e nome dei membri): trattamento **già dichiarato** in UC 0013
  (Accounts/Users/Invitations + core REST API). Per completezza del manifesto dati:
  - *categoria*: dati di contatto e identificativi (email, nome);
  - *finalità*: consentire l'accesso di più persone a un'app B2B dell'account;
  - *base giuridica*: esecuzione del contratto con l'account titolare;
  - *conservazione*: legata al ciclo di vita dell'appartenenza (finché la persona è membro dell'app o invito pendente).
  Questo use case **non apre un nuovo trattamento**: chiarisce che l'appartenenza è per-app, non per-account.

## 8. Permessi & gate

- **`tenant_id` solo dal token verificato**: l'account è sempre il claim `tenant_id` del token, mai da corpo o parametri
  della richiesta. Invariante non negoziabile.
- **Filtro riga per riga**: ogni lettura/scrittura di utenti e inviti porta `WHERE tenant_id = :tid`; con la semantica
  per-app si aggiunge il riferimento all'`app_id`.
- **Ruoli**: `owner` e `admin` gestiscono gli utenti di un'app B2B; `member` ha sola lettura o nessun accesso alla
  sezione. In un'app B2C esiste di fatto il solo `owner`.
- **Gate di modello**: l'esistenza stessa delle operazioni di invito è un gate — sono ammesse **solo** se
  `App.user_model = B2B`. Su un'app B2C ogni tentativo di invito è rifiutato.
- Il gate quantitativo sui "posti" (quanti membri sono ammessi) è definito in UC 0073.

## 9. Requisiti di test

- **Unità**: la funzione che decide "questa app ammette inviti?" restituisce vero solo per `user_model = B2B`.
- **Integrazione** (con database reale, tipo *Testcontainers*): un invito su un'app B2C viene rifiutato con
  *problem+json*; su un'app B2B viene accettato (fino al gate posti di UC 0073).
- **Isolamento fra account**: un owner dell'account A non vede né gestisce membri o inviti dell'account B, in nessun
  modello.
- **End-to-end** (test di interfaccia, tipo *Playwright*): aprendo un'app B2C la voce "Membri" è assente/ridotta;
  aprendo un'app B2B è presente e operativa.
- Deve essere verde prima del merge: la suite unica `run-tests.sh` per le aree toccate (backend, frontend).

## 10. Riferimenti & Definition of Done

- **Fonte**: R3 della tabella dei residui in `_INDEX.md`; sezione dedicata di `docs/_BACKLOG.md`.
- **Use case sorelle**: [UC 0073 (Invito utenti per-app con "posti" come metrica quota stock)](0073-invito-utenti-per-app-posti-quota.md),
  [UC 0074 (Directory cross-app + UI "Membri" ripensata per-app)](0074-directory-cross-app-ui-membri.md).
- **Definition of Done**:
  1. È scritta e implementata la regola "gli inviti dipendono da `App.user_model`";
  2. la schermata "Membri" riflette il modello dell'app (assente/ridotta per B2C, completa per B2B);
  3. gli inviti su app B2C sono rifiutati in modo tipizzato;
  4. il manifesto dati resta coerente (nessun nuovo trattamento);
  5. `run-tests.sh` verde sulle aree toccate.

## Punti aperti / decisioni differite

- **Direzione preferita, non ancora decisa**: l'intero impianto "gestione utenti per-app" (questa epica 14) è la
  **direzione preferita** emersa dal backlog, ma **non è ancora una decisione presa**. Va confermato in una **sessione
  dedicata** di piattaforma, come richiesto dall'utente subito dopo UC 0028. Finché non è decisa, questi use case
  restano specifiche "da implementare".
- **Opzione scartata**: la gestione utenti **centralizzata di piattaforma** — con un'offerta/costo dei posti unico e
  slegato dalle singole app — è stata **scartata dall'utente**: eviterebbe di re-invitare la stessa persona su ogni
  app, ma richiederebbe un listino "posti" centrale indipendente dalle app, non desiderato. Va tenuta come alternativa
  documentata, non come piano.
- **Cambio di `user_model` a posteriori**: come gestire il passaggio B2B→B2C (utenti già invitati da dismettere) e
  B2C→B2B (abilitazione posti) è fuori scope; da approfondire quando/se serve.
- **Utenti legacy su app poi classificate B2C**: strategia di migrazione da definire.
- **Proprietà**: questa epica 14 è una **decisione di piattaforma trasversale** con impatti su UC 0059 (Gestione membri
  & inviti UI backoffice), UC 0017 (Flussi auth UI), UC 0013 (Accounts/Users/Invitations + core REST API) e sul
  catalogo/prezzi delle app. Non è di un singolo use case: l'epica la possiede.
