# UC 0075 — Ticketing nativo in-house

**Area**: 15-supporto-e-piattaforma · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0012 (Core service + multitenancy), UC 0013 (Accounts/Users/Invitations + core REST API), UC 0020 (Backoffice SPA shell), UC 0021 (Admin console SPA), UC 0018 (Localizzazione email auth / invio via SES), UC 0034 (Console "Diritti GDPR")
**Fonte**: R6 (Tabella residui _INDEX.md) · docs/_BACKLOG.md §Compliance/privacy (#13 D/I)
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope

Costruire un **sistema di ticket di assistenza fatto in casa**, dentro la piattaforma, che sostituisca strumenti esterni
(tipo Jira o servizi di help desk in cloud). La ragione è di purezza sulla privacy: **zero responsabili esterni del
trattamento**, dati personali tenuti in casa, tutto entro l'Unione Europea. Un ticket è una conversazione tracciata fra
chi ha un problema (l'utente di un account cliente) e chi risponde (l'agente o il fondatore, dal lato piattaforma).

Copriamo **due tipi** di ticket con regole diverse:
- **privacy** — tipo speciale, con **obbligo di legge di rispondere entro un mese** (istanza di esercizio dei diritti);
  può essere **creato automaticamente** da eventi (per esempio un'esportazione dati fallita o un'istanza arrivata via
  email);
- **supporto generico** — assistenza *best-effort* (per quanto possibile, senza tempi di legge), aperto dall'utente da
  dentro l'applicazione.

**Incluso (versione minima e volutamente essenziale)**: entità `support_ticket` nel core; **interfaccia backoffice** dove
l'utente apre e segue i propri ticket; **console admin** dove l'agente/fondatore risponde; campi tipo / priorità / stato;
un **filo di messaggi** utente ↔ admin; **notifiche email** via SES; **inneschi automatici** (form dentro l'app, caselle
`privacy@` e `support@` instradate via SES → funzione Lambda, evento di esportazione **fallita**, promozione a priorità
alta quando entrano in gioco categorie particolari di dati — articolo 9 del Regolamento europeo sulla protezione dei
dati). Integrazione con la Console "Diritti GDPR" (UC 0034), che **aggrega** anche i ticket di tipo privacy.

**Escluso in questa versione**: **allegati** (nessun caricamento di file), risposte automatiche evolute, valutazioni di
soddisfazione, portale pubblico. Sono candidati a un'evoluzione successiva (vedi Punti aperti).

## 2. Attori & ruoli

- **Utente dell'account cliente** (proprietario/amministratore/membro, secondo UC 0013): apre e segue i propri ticket dal
  backoffice.
- **Agente / fondatore** (ruolo `platform-admin`): legge, risponde, cambia stato dalla console admin, **cross-account**.
- **Sistema / automazioni**: crea ticket a partire da eventi (form in-app, email in ingresso, esportazione fallita) e
  invia notifiche.
- **Terzi**: **SES** come vettore email (invio notifiche e ricezione delle caselle `privacy@`/`support@`), interno
  all'infrastruttura del progetto, non un responsabile esterno del help desk.

## 3. Precondizioni

- Core e multitenancy attivi — UC 0012 (Core service + multitenancy).
- Modello account/utenti disponibile — UC 0013 (Accounts/Users/Invitations + core REST API).
- Shell backoffice e console admin esistenti — UC 0020 (Backoffice SPA shell) e UC 0021 (Admin console SPA).
- Invio email via SES disponibile — UC 0018 (Localizzazione email auth / invio via SES).
- L'utente è autenticato (token verificato con `tenant_id` e `user_id`).

## 4. Flusso principale

1. L'utente apre il **form di supporto** nel backoffice: sceglie il tipo (privacy o generico), scrive oggetto e messaggio.
2. Il core crea un `support_ticket` con `tenant_id` (dal token), `user_id`, tipo, priorità di partenza, stato `open`, e
   il **primo messaggio** nel filo.
3. Il sistema invia una notifica email all'utente (conferma di apertura) e all'agente (nuovo ticket) via SES.
4. L'agente apre la **console admin**, legge il ticket, **risponde**: la risposta diventa un nuovo messaggio nel filo e
   fa passare lo stato a `waiting_user`; l'utente riceve la notifica email.
5. L'utente replica dal backoffice (stato torna a `open`/`in_progress`); il filo cresce alternando i due lati.
6. Quando la questione è chiusa, l'agente porta lo stato a `resolved`/`closed`; l'utente riceve l'avviso.
7. Ogni passaggio è tracciato con **log strutturato** (`tenant_id`, `app_id` dove pertinente, `user_id`).

Per i ticket **privacy** valgono in più: la **scadenza di un mese** è calcolata alla creazione e mostrata; il ticket
compare **anche** nella Console "Diritti GDPR" (UC 0034), che ne aggrega scadenze e stato.

## 5. Flussi alternativi / edge / errori

- **Creazione automatica da email**: un messaggio arrivato a `privacy@`/`support@` viene consegnato da SES a una funzione
  Lambda che apre (o aggiorna) il ticket associando il mittente all'utente noto quando riconoscibile; se il mittente non
  è associabile a un account, il ticket resta a carico della piattaforma con provenienza email registrata.
- **Creazione automatica da esportazione fallita**: l'evento di **export FALLITO** (framework di esportazione/cancellazione)
  apre automaticamente un ticket di tipo privacy con priorità alta, così che l'obbligo di risposta non venga mancato.
- **Escalation categorie particolari**: se il contenuto tocca categorie particolari (articolo 9: salute, convinzioni,
  ecc.) il ticket è promosso a **priorità alta** e marcato per attenzione umana.
- **Edge — utente rimosso**: se l'utente che ha aperto il ticket viene rimosso, il ticket resta visibile lato admin con
  l'associazione storica; il filo non si perde.
- **Errore — invio email non riuscito**: il fallimento SES non blocca il ticket (il ticket esiste comunque); viene
  registrato e ritentato secondo la policy di invio; l'utente vede lo stato aggiornato in interfaccia anche senza email.
- **Errore — validazione**: oggetto/messaggio vuoti o troppo lunghi → risposta tipizzata (*problem+json*) con messaggio
  chiaro in italiano e inglese.
- **Edge — nessun allegato**: se l'utente prova a incollare qualcosa che richiederebbe un file, l'interfaccia spiega che
  in questa versione gli allegati non sono supportati.

## 6. Schermate & stati

Due superfici, entrambe descritte perché questo use case porta interfaccia sia lato utente sia lato piattaforma.

- **Backoffice — "I miei ticket"** (UC 0020, Backoffice SPA shell):
  - lista dei ticket dell'utente (oggetto, tipo, stato, ultimo aggiornamento);
  - dettaglio con il **filo di messaggi** e la casella per rispondere;
  - pulsante "Apri un ticket" con scelta del tipo;
  - **stati**: caricamento, vuoto ("nessun ticket"), errore (messaggio tipizzato), successo (ticket creato/aggiornato).
- **Console admin — "Ticket"** (UC 0021, Admin console SPA):
  - **coda cross-account** dei ticket (account, utente, tipo, priorità, stato, scadenza per i privacy);
  - filtri per tipo/stato/priorità e ordinamento per scadenza (i privacy più urgenti in cima);
  - dettaglio con filo, cambio stato e priorità, risposta;
  - evidenza visiva della **scadenza del mese** per i ticket privacy (avviso quando si avvicina).
- **Copy**: italiano e inglese, linguaggio chiaro senza sigle non spiegate; per i ticket privacy, testo che ricorda il
  termine di legge. Conferme esplicite per il cambio a `closed`.

## 7. Dati toccati

- **`support_ticket`** (schema `platform`, nel core): `tenant_id`, `user_id`, `type` (`privacy`/`generic`), `priority`,
  `status` (`open`/`in_progress`/`waiting_user`/`resolved`/`closed`), `subject`, `source` (form/email/evento),
  `due_at` (solo privacy), timestamp di audit e soft-delete (secondo UC 0012).
- **`support_ticket_message`**: righe del filo (`ticket_id`, `author_side` utente/admin, `body`, timestamp).
- **Dati personali** (contenuto dei messaggi, email, nome):
  - *categoria*: dati di contatto/identificativi e **contenuto libero** che può includere, per i ticket privacy,
    categorie particolari (articolo 9);
  - *finalità*: fornire assistenza e dare seguito alle istanze di esercizio dei diritti;
  - *base giuridica*: esecuzione del contratto (supporto) e **obbligo di legge** (istanze privacy);
  - *conservazione*: legata alla gestione della richiesta e agli obblighi di prova; da fissare nel manifesto dati e
    allineare con il job di conservazione/purga (UC 0035, Job retention/purge).
  I campi che contengono dati personali vanno marcati `@PersonalData` e dichiarati nel manifesto dati (registro dei
  trattamenti).

## 8. Permessi & gate

- **`tenant_id` solo dal token verificato**: lato backoffice ogni lettura/scrittura è filtrata sull'account del claim; un
  utente vede **solo** i propri ticket.
- **Filtro riga per riga** `WHERE tenant_id = :tid` su tutte le query lato utente.
- **Eccezione controllata cross-account**: la coda della **console admin** legge i ticket di **tutti** gli account: è
  l'eccezione esplicita all'invariante di filtro, ammessa **solo** per il ruolo `platform-admin` e in **sola lettura**
  sui dati di account (le scritture riguardano stato/priorità/risposta del ticket, non i dati dell'account). Ogni accesso
  admin è loggato.
- **Ruoli**: gli utenti dell'account aprono/seguono i propri ticket; solo `platform-admin` risponde e cambia stato.
- **Esenzione dai gate di prodotto**: aprire un ticket **privacy** non dipende da entitlement o quota — l'esercizio dei
  diritti non si può bloccare per motivi commerciali (coerente con la linea GDPR del progetto).

## 9. Requisiti di test

- **Unità**: transizioni di stato valide; calcolo della scadenza di un mese per i privacy; promozione a priorità alta su
  categorie particolari.
- **Integrazione** (database reale, tipo Testcontainers): apertura da form, filo di messaggi, notifiche accodate;
  creazione automatica da evento di esportazione fallita.
- **Isolamento fra account**: un utente non legge **mai** ticket di un altro account (test di sicurezza esplicito); la
  coda admin invece li vede tutti (verifica del gate `platform-admin`).
- **End-to-end** (Playwright): utente apre ticket → admin risponde → utente vede la risposta → chiusura; e il caso privacy
  con scadenza e aggregazione in Console "Diritti GDPR".
- **Email**: invio notifiche via SES simulato; un fallimento di invio non perde il ticket.
- Verde su `run-tests.sh` per le aree toccate (backend, frontend) prima del merge.

## 10. Riferimenti & Definition of Done

- **Fonte**: R6 della tabella dei residui in `_INDEX.md`; `docs/_BACKLOG.md` §Compliance/privacy (#13 D/I).
- **Use case sorelle**: [UC 0076 (Disabilita applicazione)](0076-disabilita-applicazione.md),
  [UC 0077 (Provider entitlement reale del backoffice/admin)](0077-provider-entitlement-reale.md).
- **Definition of Done**:
  1. entità `support_ticket` e filo messaggi nel core, con filtro riga per riga e soft-delete/audit;
  2. interfaccia backoffice "I miei ticket" e console admin "Ticket" (coda cross-account, sola lettura sui dati account);
  3. i due tipi (privacy con scadenza di un mese, generico best-effort) con relativi stati e priorità;
  4. inneschi automatici (form, email via SES → Lambda, esportazione fallita, escalation categorie particolari);
  5. i ticket privacy compaiono nella Console "Diritti GDPR" (UC 0034);
  6. manifesto dati aggiornato (contenuto messaggi come dato personale, retention allineata a UC 0035);
  7. `run-tests.sh` verde sulle aree toccate.

## Punti aperti / decisioni differite

- **Allegati**: esclusi da questa versione; se servissero (schermate del cliente, documenti a corredo di un'istanza
  privacy), progettare caricamento sicuro, antivirus e retention dedicata. Possiede questo punto l'epica 15.
- **Instradamento email in ingresso**: i dettagli della ricezione SES → Lambda (verifica del dominio, associazione
  mittente ↔ utente, gestione dello spam) vanno definiti in implementazione insieme a UC 0018.
- **Retention fine dei ticket privacy**: la conservazione del contenuto (specie categorie particolari) va fissata con la
  revisione legale pre-go-live e allineata al job di purga (UC 0035); qui si vincola solo il principio di minimizzazione.
- **Confine con la Console "Diritti GDPR"**: decidere se le istanze formali di esercizio dei diritti nascano sempre come
  ticket privacy o se esistano flussi che la Console (UC 0034) gestisce senza ticket; da coordinare con chi possiede UC 0034.
- **Assegnazione**: con un solo agente/fondatore non serve; se il team cresce, aggiungere assegnatario e regole di presa
  in carico.
