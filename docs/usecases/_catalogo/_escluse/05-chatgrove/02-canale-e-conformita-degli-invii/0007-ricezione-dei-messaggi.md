# 0007 — Ricezione dei messaggi

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 02 — Canale di messaggistica e conformità degli invii
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che risponde ai clienti
> voglio vedere nell'app tutti i messaggi che arrivano al numero del negozio, in ordine e senza doppioni
> così da non dover guardare il telefono e l'app allo stesso tempo.

**Contesto.** Senza la ricezione l'app è cieca: si può configurare tutto, ma non succede niente. Il fornitore
del canale recapita i messaggi con una chiamata verso di noi, che può arrivare **più di una volta** per lo
stesso messaggio: la ripetizione non è un caso limite, è il comportamento normale di questi sistemi. Se non se
ne tiene conto, il negozio vede lo stesso ordine due volte — ed è il genere di errore che fa perdere fiducia
all'istante.

## 2. Requisiti funzionali

1. **RF-1** — Esiste una rotta di ricezione che accetta le notifiche del fornitore del canale e ne ricava
   messaggi in entrata.
2. **RF-2** — Ogni notifica è **verificata**: se la firma non corrisponde alla connessione dell'account, viene
   respinta e registrata come tentativo non valido.
3. **RF-3** — La ricezione è **idempotente**: la stessa notifica recapitata più volte produce **un solo**
   messaggio, riconosciuto dall'identificativo assegnato dal fornitore.
4. **RF-4** — Se il numero mittente non è ancora fra i contatti, il contatto viene creato; altrimenti si usa
   quello esistente.
5. **RF-5** — Il messaggio entra nella conversazione aperta con quel contatto; se non ce n'è una, la
   conversazione viene aperta.
6. **RF-6** — La ricezione aggiorna la **scadenza della finestra di servizio** della conversazione, portandola
   a 24 ore dal messaggio ricevuto.
7. **RF-7** — L'elenco delle conversazioni mostra le più recenti in cima, con l'ultimo messaggio, il nome del
   contatto e se la finestra di servizio è ancora aperta.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La notifica è associata all'account tramite la **connessione**, non
  tramite un identificativo presente nel corpo: il `tenant_id` si ricava dalla connessione riconosciuta, mai da
  ciò che arriva dall'esterno. Ogni lettura successiva filtra per `tenant_id` preso dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta di ricezione `POST /api/chat_commerce/v1/channel/inbound`
  — **non autenticata con il token utente** ma verificata con la firma del fornitore, e per questo trattata come
  superficie esposta: corpo validato, dimensione massima, errori in `application/problem+json`. Rotte di lettura
  `GET /api/chat_commerce/v1/conversations` e `.../conversations/{id}/messages`, paginate.
- **RT-3 — Persistenza (§8).** Vincolo di unicità su (account, identificativo del messaggio presso il
  fornitore) per garantire l'idempotenza a livello di database, non solo di codice.
- **RT-4 — Dati personali (§10).** Voci già dichiarate in `0002` per `contact.phone` e `message.body`: qui si
  verifica che la ricezione non introduca campi non dichiarati (per esempio metadati del dispositivo). Se il
  fornitore ne recapita di ulteriori, **non si conservano**.
- **RT-5 — Registrazione eventi (§14).** Gli eventi `messaggio ricevuto`, `notifica duplicata ignorata` e
  `firma non valida` sono registrati con `tenant_id`, `app_id` e identificativo di correlazione, **senza il
  numero di telefono e senza il corpo del messaggio**: si registrano identificativi, non contenuti.
- **RT-6 — Prove (§11).** Prove di integrazione con il canale simulato della storia `0005`, compresa la
  ripetizione della stessa notifica e la firma errata.

## 4. Criteri di accettazione

**CA-1 — Il messaggio arriva**
- **Dato** un account con canale collegato
- **Quando** il canale recapita un messaggio da un numero sconosciuto
- **Allora** compaiono un contatto nuovo, una conversazione nuova e il messaggio, e la conversazione risulta
  con finestra di servizio aperta fino a 24 ore dopo

**CA-2 — Nessun doppione**
- **Dato** la stessa notifica recapitata tre volte
- **Quando** la ricezione la elabora
- **Allora** esiste **un solo** messaggio e le altre due elaborazioni sono registrate come ripetizioni ignorate

**CA-3 — Firma non valida**
- **Dato** una notifica con firma errata
- **Quando** arriva alla rotta di ricezione
- **Allora** viene respinta, nessun messaggio è creato e l'evento è registrato come tentativo non valido

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B` con canali diversi
- **Quando** arriva una notifica per il canale di `A`
- **Allora** il messaggio finisce solo in `A`, e un utente di `B` non lo vede in alcun modo

**CA-5 — Conversazione riaperta**
- **Dato** una conversazione chiusa con un contatto
- **Quando** quel contatto scrive di nuovo
- **Allora** la conversazione torna aperta con il messaggio nuovo in coda, senza crearne una seconda

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul riconoscimento della ripetizione e sul calcolo della finestra, e di
      **integrazione** sulla rotta di ricezione con il canale simulato;
- [ ] prova di **isolamento fra account** sulle conversazioni e sui messaggi;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per l'elenco delle conversazioni;
- [ ] **manifesto dei dati**: verificato che non entrino campi non dichiarati;
- [ ] **registro delle decisioni** compilato, con la scelta di ricavare l'account dalla connessione e non dal
      corpo della notifica;
- [ ] contratto degli **strumenti conversazionali**: `elenca_conversazioni` e `leggi_conversazione` dichiarati
      come strumenti di **lettura** (contratto completo nella storia `0026`);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0002` | Servono contatti, conversazioni e messaggi |
| `0006` | Serve la connessione da cui si riconosce l'account |

## 7. Fuori ambito

- l'invio di risposte: storia `0008`;
- la presa in carico della conversazione da parte di un addetto: storia `0008`;
- la rubrica dei contatti con scheda e storico: storia `0021`.

## 8. Punti aperti

- **Contenuti non testuali** (immagini, note vocali, posizione): la storia tratta il testo e registra il tipo
  degli altri senza conservarne il contenuto. Se il negozio ne ha bisogno — un cliente che manda la foto del
  prodotto è normale — serve una decisione su dove conservarli e per quanto: è una decisione sui dati
  personali e sui costi, quindi dello sviluppatore.
