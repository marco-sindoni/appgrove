# 0007 — Filo dei messaggi e risposta

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 02 — Casella condivisa e conversazioni
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0003`, `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde ai clienti
> voglio vedere tutta la conversazione con quel cliente in un posto solo e scrivere lì la mia risposta
> così da capire in dieci secondi cosa è già stato detto, e da non far ripartire il cliente da zero per la terza
> volta.

**Contesto.** Con la storia `0006` una richiesta si apre, ma resta muta: c'è un messaggio e non c'è modo di
aggiungerne un altro. Il filo è **il prodotto**: è ciò che la casella di posta condivisa non sa dare, perché la
conversazione lì è sparsa fra tre caselle personali e nessuno la vede intera. Questa storia costruisce il filo e la
casella di risposta, e si ferma un passo prima della spedizione: **il messaggio in uscita resta dentro l'app**,
perché mandare posta a una persona esterna è irreversibile e ha requisiti propri — identità del mittente,
recapitabilità, autenticazione del dominio — che sono la storia `0015`. Qui si valorizza però la **data della prima
risposta**, che è il dato senza il quale, più avanti, nessuna misura del servizio potrà essere vera (§2.5 della
descrizione dell'applicazione).

## 2. Requisiti funzionali

1. **RF-1** — Il dettaglio della richiesta mostra **tutti** i messaggi del filo in ordine cronologico crescente, con
   verso, autore e data, e l'intestazione della richiesta (numero, oggetto, stato, richiedente).
2. **RF-2** — Dalla stessa schermata l'operatore scrive una risposta e la registra: nasce un messaggio con verso
   «in uscita», autore l'utente autenticato, data del momento della registrazione.
3. **RF-3** — L'interfaccia dichiara in modo esplicito e tradotto che il messaggio è **registrato nel filo e non
   ancora recapitato al cliente**: nessun messaggio esce dall'app in questa storia, e l'operatore non deve poterlo
   credere.
4. **RF-4** — Il **primo** messaggio in uscita valorizza la data di prima risposta della richiesta; il valore si
   scrive **una sola volta** e non viene mai riscritto dai messaggi successivi.
5. **RF-5** — Il corpo del messaggio è obbligatorio, non vuoto e non più lungo del limite dichiarato; a messaggio
   rifiutato nulla viene registrato e la data di prima risposta resta intatta.
6. **RF-6** — Un messaggio registrato è **immutabile**: non esiste modifica né cancellazione dal filo. Una
   correzione è un messaggio nuovo, e la storia della conversazione non si riscrive mai.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lettura del filo e la registrazione di un messaggio filtrano per
  `tenant_id` preso dal token verificato; una richiesta di un altro account risponde `404` e non `403`, per non
  rivelare che quel numero esiste altrove. Un `tenant_id` che arrivasse dal corpo o dai parametri viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/helpdesk/v1/tickets/{id}` restituisce la richiesta con il
  filo paginato; `POST /api/helpdesk/v1/tickets/{id}/messages` registra il messaggio in uscita. Corpo validato in
  modo dichiarativo sugli oggetti di trasferimento; errori in `application/problem+json` (`400` validazione, `404`
  richiesta inesistente o altrui, `405` su ogni tentativo di modifica o cancellazione di un messaggio); paginazione
  a pagina e dimensione con totale; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Sullo schema `app_helpdesk`: se la colonna `first_response_at` di `ticket` non nasce
  già con la storia `0002`, la aggiunge la migrazione `V4__ticket_first_response.sql`, insieme all'indice
  `(tenant_id, ticket_id, created_at, id)` su `ticket_message` che rende l'ordinamento del filo **stabile e
  deterministico** anche a parità di istante. Chiavi primarie UUID versione 7, colonne di controllo, cancellazione
  logica; nessuna chiave esterna verso altri schemi. La valorizzazione della data di prima risposta e la scrittura
  del messaggio stanno nella **stessa transazione**: non deve esistere il caso «messaggio registrato, orologio non
  fermato».
- **RT-4 — Modulo frontend (§3, §5).** Schermata di dettaglio della sezione «Richieste» del modulo `helpdesk`: filo
  a scorrimento con distinzione visiva fra verso in ingresso e in uscita, casella di risposta in coda, stato di
  invio in corso e messaggio d'errore leggibile. Dati letti e scritti con il client generato; solo token del sistema
  di design; funziona in tema chiaro e scuro; controllo automatico di accessibilità sulla schermata.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — compresi l'avviso «registrata, non ancora inviata al
  cliente» e i messaggi di rifiuto — passano dallo spazio-nomi `helpdesk` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Rispondere **non consuma quota**: la metrica unica dell'app è `agents` (posti
  operatore, natura `stock`), consumata dalla storia `0018`. Restano i varchi a monte: `401` senza token valido,
  `402` con abbonamento non attivo, `403` per ruolo insufficiente; il ruolo `member` può rispondere. La storia non
  fissa prezzi: consuma il tetto pubblicato dall'abilitazione.
- **RT-7 — Esposizione conversazionale (§12).** Strumenti dichiarati: `leggi_richiesta(numero) → richiesta con il
  filo dei messaggi`, marcato **lettura**; `prepara_risposta(numero, indicazioni) → bozza di messaggio, non
  inviata`, marcato **scrittura** con **conferma umana**. La separazione fra preparare e inviare è il cuore della
  sicurezza dell'app (§7 della descrizione) e qui è gratuita, perché `invia_risposta` non esiste ancora: nasce con
  la storia `0015`. Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non ancora
  implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessuna tabella nuova e nessun campo nuovo di persone: la voce `message.body` del
  manifesto `docs/compliance/manifests/helpdesk.yaml`, scritta dalla storia `0006`, copre già il corpo dei messaggi
  in entrambi i versi — va però **estesa in italiano e inglese** per dire che comprende anche il testo scritto
  dall'operatore, che può contenere dati del cliente finale. `ticket_message` è già in `exportData` e `purgeData` di
  `HelpdeskDataContract` e ci resta. Anche il testo scritto dall'operatore attraversa il riconoscitore delle
  categorie particolari con il solo **contrassegno booleano**, senza registrare quale categoria: un operatore che
  trascrive «il cliente dice di essere in terapia» crea lo stesso problema del cliente che lo scrive.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `messaggio registrato` (con identificativo della richiesta e
  verso, **mai** il corpo) e `prima risposta registrata` sono scritti con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Risposta registrata nel filo**
- **Dato** una richiesta aperta con un solo messaggio in ingresso e un utente abilitato con ruolo `member`
- **Quando** scrive una risposta e la registra
- **Allora** il filo contiene due messaggi in ordine cronologico, il secondo con verso «in uscita» e autore l'utente
  che ha scritto, e l'interfaccia dichiara che il messaggio non è ancora stato recapitato al cliente

**CA-2 — L'orologio della prima risposta si ferma una volta sola**
- **Dato** una richiesta senza data di prima risposta
- **Quando** l'operatore registra una prima risposta e poi, un minuto dopo, una seconda
- **Allora** la data di prima risposta è quella del **primo** messaggio in uscita e non viene riscritta dal secondo

**CA-3 — Corpo vuoto**
- **Dato** una richiesta esistente · **Quando** si tenta di registrare un messaggio con corpo vuoto
- **Allora** la risposta è `400` in `application/problem+json`, nessun messaggio viene registrato e la data di prima
  risposta resta intatta

**CA-4 — Messaggio immutabile**
- **Dato** un messaggio già registrato nel filo · **Quando** si tenta di modificarlo o cancellarlo
- **Allora** la risposta è `405` con la spiegazione che una correzione si fa scrivendo un messaggio nuovo, e il filo
  resta invariato

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie richieste
- **Quando** un utente di `A` chiede il filo di una richiesta di `B` o tenta di registrarvi un messaggio, anche
  forzando il `tenant_id` nel corpo della richiesta
- **Allora** riceve `404` in entrambi i casi e nulla viene scritto nel filo di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla regola «la data di prima risposta si scrive una volta sola» e di **integrazione**
      sulla risorsa dei messaggi e sull'ordinamento stabile del filo, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su lettura e scrittura del filo, con tentativo di forzatura del
      `tenant_id`;
- [ ] **prova end-to-end**: *coprire ora* — passo «rispondi alla richiesta» del percorso `[J-HELPDESK]`, con
      l'etichetta in testa al titolo del test; registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese: la voce `message.body` estesa al testo scritto
      dall'operatore; `ticket_message` presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotate la scelta di **non** spedire nulla in questa storia e la
      transazione unica messaggio + data di prima risposta;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `leggi_richiesta` e `prepara_risposta`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | Serve una richiesta con il suo primo messaggio: senza di essa non c'è filo a cui aggiungere niente |
| storia `0003` | Serve il guscio del modulo `helpdesk` per appendere la schermata di dettaglio |
| epica di piattaforma non implementata (UC 0061-0063) | Il livello conversazionale non esiste: qui si dichiarano solo i contratti di `leggi_richiesta` e `prepara_risposta` |

## 7. Fuori ambito

- la **spedizione vera** del messaggio al cliente per posta elettronica, l'identità del mittente e la
  recapitabilità: storia `0015`;
- le **note interne** (verso «interno») e il filtro del filo: storia `0008`;
- il **cambio di stato automatico** alla risposta («in attesa del cliente») e il divieto di rispondere a una
  richiesta chiusa: storia `0009` — qui la richiesta resta `aperta` qualunque cosa accada, ed è una scelta di
  perimetro, non una dimenticanza;
- il **calcolo delle scadenze** in ore lavorative e la loro violazione: storie `0023`-`0025`; qui si registra
  soltanto il **momento** della prima risposta, che è il dato grezzo su cui quel calcolo poggerà;
- gli **allegati** al messaggio: storia `0016`;
- le **risposte predefinite** e l'inserimento di articoli mentre si scrive: storie `0022` e `0030`.

## 8. Punti aperti

- **Quanto può essere lungo il corpo di un messaggio?** Qui si propone un limite generoso ma esplicito, perché un
  campo di testo senza tetto è una via d'ingresso per l'abuso e un problema di archiviazione (punto 6 del §11 della
  descrizione). Il valore preciso lo chiude lo **sviluppatore** insieme alla decisione sulla conservazione (storia
  `0036`).
- **Se un operatore scrive per errore un dato che non doveva, come si rimedia**, visto che i messaggi sono
  immutabili? La proposta è che il rimedio sia la **cancellazione per singolo richiedente** della storia `0036` e
  non una modifica del filo, che falsificherebbe la storia della conversazione. Chiude lo **sviluppatore**.
