# 0006 — Apertura manuale di una richiesta

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 02 — Casella condivisa e conversazioni
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde ai clienti in una micro-impresa
> voglio poter aprire io una richiesta di assistenza per un cliente che mi ha telefonato o mi ha scritto altrove
> così da avere anche quella conversazione tracciata, con un numero, uno stato e una storia, invece che su un
> foglietto sulla scrivania.

**Contesto.** Alla fine dell'epica 01 l'app esiste, è accesa ed è vuota: c'è lo schema, c'è il modulo nella barra
laterale, c'è la quota dei posti operatore, ma non c'è ancora un modo per far entrare una richiesta. I canali
automatici — modulo del sito, posta elettronica, WhatsApp — sono l'epica 03: farli prima significherebbe costruire
l'ingresso di una casa che non ha ancora le stanze. Questa storia è **la prima scrittura del dominio** e apre la via
d'ingresso che nessun canale potrà mai coprire: il cliente che telefona, quello che ferma il titolare in negozio,
quello che scrive sul profilo social. È anche il punto in cui i primi dati di persone esterne entrano nel database,
e va progettato sapendolo (§6 della descrizione dell'applicazione).

## 2. Requisiti funzionali

1. **RF-1** — Un operatore apre una richiesta indicando **oggetto**, **testo iniziale**, **nome** e **posta
   elettronica** del richiedente; la richiesta nasce nello stato `aperta`, con canale d'ingresso `manuale` e nessun
   operatore assegnato.
2. **RF-2** — Il testo iniziale viene registrato come **primo messaggio del filo** con verso «in ingresso» e
   attribuito al **richiedente**, non all'operatore che l'ha trascritto: chi legge la conversazione sei mesi dopo
   deve vedere che quelle parole sono del cliente.
3. **RF-3** — Ogni richiesta riceve un **numero progressivo per account**, univoco, senza salti né riuso, assegnato
   al momento della creazione anche quando più operatori dello stesso account creano nello stesso istante.
4. **RF-4** — Se nell'account esiste già un richiedente con la stessa posta elettronica (confronto sull'indirizzo
   normalizzato a minuscole e ripulito degli spazi), la richiesta si aggancia a quello; altrimenti ne viene creato
   uno nuovo con nome e indirizzo forniti. Nessun altro criterio di riconoscimento in questa storia.
5. **RF-5** — La validazione è dichiarativa e il messaggio d'errore dice **quale campo** è sbagliato: oggetto
   obbligatorio e non più lungo del limite dichiarato, testo iniziale obbligatorio e non vuoto, nome obbligatorio,
   posta elettronica obbligatoria e sintatticamente valida. A richiesta rifiutata **nulla viene creato**, né la
   richiesta, né il messaggio, né il richiedente, né il numero.
6. **RF-6** — Accanto al campo del testo compare l'avviso, tradotto, che il contenuto viene conservato secondo la
   durata stabilita per l'account e che non vanno trascritti dati non necessari a fornire l'assistenza.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `ticket`, `ticket_message` e `requester`
  filtra per `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai
  parametri viene ignorato. Anche il **contatore dei numeri** è per `tenant_id`: due account non condividono mai un
  contatore. Prova di isolamento fra due account su tutte e tre le tabelle.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/helpdesk/v1/tickets` (apertura) e
  `GET /api/helpdesk/v1/tickets/{id}` (dettaglio minimo, con il filo). Corpo validato in modo dichiarativo sugli
  oggetti di trasferimento, mai sulle entità; errori in `application/problem+json` (`400` per validazione, `402`
  senza abbonamento attivo, `404` per richiesta inesistente o di un altro account); definizione OpenAPI generata e
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V3__ticket_numbering.sql` sullo schema `app_helpdesk`: tabella
  `ticket_number` con `tenant_id` e ultimo numero assegnato, colonne di controllo, e vincolo di unicità su
  `(tenant_id, number)` in `ticket`. **L'assegnazione del numero avviene dentro la stessa transazione della
  creazione, con blocco sulla riga del contatore**: è il punto in cui un'implementazione ingenua produce doppioni
  sotto carico concorrente. Chiavi primarie UUID versione 7, colonne di controllo e cancellazione logica su tutte le
  tabelle toccate; nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Richieste» del modulo `helpdesk`, con l'azione «Nuova richiesta»:
  modulo di inserimento con React Hook Form e validazione condivisa, dati letti e scritti con il client generato
  dalla definizione OpenAPI. Solo token del sistema di design con colore-categoria `teal`; funziona in tema chiaro e
  scuro; il modulo non conosce il `tenant_id` se non attraverso il contesto della shell.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — etichette, messaggi di errore, avviso sulla
  conservazione — passano dallo spazio-nomi `helpdesk` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** L'apertura di una richiesta **non consuma quota**: la metrica dell'app è una
  sola, `agents` (posti operatore, natura `stock`), e si consuma quando una persona riceve il diritto di lavorare
  sulle richieste (storia `0018`). Restano i varchi a monte: `401` senza token valido, `402` con abbonamento non
  attivo, `403` per ruolo insufficiente. Il ruolo `member` può aprire una richiesta. La storia non fissa prezzi:
  consuma il tetto pubblicato dall'abilitazione.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `crea_richiesta(canale, richiedente, oggetto, testo) → bozza di richiesta`, marcato **scrittura**, con **conferma
  umana obbligatoria**: lo strumento produce una bozza e la richiesta nasce solo dopo l'approvazione della persona.
  Il contratto vive dentro il servizio `helpdesk`, versionato con esso; il server conversazionale è di piattaforma e
  non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **È la storia che fa entrare i primi dati di persone esterne nell'app.** Voci nel
  manifesto `docs/compliance/manifests/helpdesk.yaml`, in italiano e inglese: `requester.name`, `requester.email`,
  `ticket.subject`, `message.body`. Campi Java annotati `@PersonalData`; tabelle `requester`, `ticket` e
  `ticket_message` presenti **sia** in `exportData` **sia** in `purgeData` del contratto `HelpdeskDataContract`.
  Base giuridica dichiarata: **trattamento per conto del titolare** — qui appgrove è **responsabile del
  trattamento** per conto dell'azienda cliente, non titolare, e la voce del manifesto deve dirlo. Il corpo del
  messaggio e l'oggetto sono **testo libero scritto da persone esterne** e possono contenere per accidente
  categorie particolari (articolo 9): la richiesta creata attraversa il riconoscitore deterministico spostato in
  `services/commons` dalla storia `0002` e viene marcata con un **contrassegno booleano** «da guardare con
  attenzione», **senza registrare quale** categoria sarebbe stata riconosciuta e **senza** alcun servizio esterno di
  analisi del testo.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `richiesta creata` (con il **numero**, mai l'oggetto),
  `richiedente creato` (con l'identificativo, mai nome né indirizzo) e `apertura respinta per validazione` sono
  registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza dati personali**.

## 4. Criteri di accettazione

**CA-1 — Apertura di una richiesta**
- **Dato** un utente abilitato con ruolo `member` e un account senza richieste
- **Quando** apre una richiesta con oggetto, testo, nome e posta elettronica del richiedente
- **Allora** la richiesta esiste nello stato `aperta` con numero `1`, ha esattamente un messaggio nel filo con verso
  «in ingresso» attribuito al richiedente, e il richiedente è stato creato

**CA-2 — Validazione**
- **Dato** un modulo con la posta elettronica del richiedente sintatticamente non valida · **Quando** si tenta di
  salvare · **Allora** la risposta è `400` in `application/problem+json` con l'indicazione del campo, e **nulla**
  viene creato: nessuna richiesta, nessun messaggio, nessun richiedente, nessun numero consumato

**CA-3 — Numerazione senza doppioni sotto concorrenza**
- **Dato** due operatori dello stesso account che aprono una richiesta nello stesso istante
- **Quando** entrambe le aperture vanno a buon fine
- **Allora** i numeri assegnati sono distinti e consecutivi, senza salti né riuso

**CA-4 — Richiedente già noto**
- **Dato** un account che ha già un richiedente con l'indirizzo `mario.rossi@esempio.test`
- **Quando** un operatore apre una nuova richiesta indicando lo stesso indirizzo scritto con maiuscole diverse
- **Allora** la richiesta si aggancia al richiedente esistente e **non** viene creato un doppione

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie richieste
- **Quando** un utente di `A` apre una richiesta e poi tenta di leggere una richiesta di `B`
- **Allora** la numerazione di `A` riparte dal proprio contatore indipendentemente da `B`, e la lettura della
  richiesta altrui risponde `404`, anche forzando il `tenant_id` nel corpo o nei parametri

**CA-6 — Contrassegno per le categorie particolari**
- **Dato** un testo iniziale che contiene una radice riconosciuta dal riconoscitore delle categorie particolari
- **Quando** la richiesta viene aperta
- **Allora** la richiesta porta il contrassegno «da guardare con attenzione» valorizzato, e **da nessuna parte** è
  registrata quale categoria sia stata riconosciuta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sull'assegnazione del numero e sulla normalizzazione dell'indirizzo, e di **integrazione**
      sulla risorsa e sull'apertura concorrente, con database effimero e migrazioni Flyway vere;
- [ ] prova di **isolamento fra account** su `ticket`, `ticket_message`, `requester` e sul contatore dei numeri, con
      tentativo di forzatura del `tenant_id`;
- [ ] **prova end-to-end**: *coprire ora* — passo «apri una richiesta a mano» del percorso `[J-HELPDESK]`, con
      l'etichetta in testa al titolo del test; registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con le quattro voci nuove, campi annotati
      `@PersonalData`, tabelle presenti in esportazione e cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotate la scelta della
      transazione unica numero + creazione e la regola di attribuzione del primo messaggio al richiedente;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `crea_richiesta`, con conferma obbligatoria;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | Servono le tabelle `ticket`, `ticket_message` e `requester` sullo schema `app_helpdesk`, e il riconoscitore delle categorie particolari già spostato in `services/commons` |
| storia `0003` | Serve il guscio del modulo `helpdesk` a cui appendere la sezione «Richieste» |
| storia `0004` | Servono i varchi dell'abbonamento: senza di essi la risorsa nuova nascerebbe senza `402` |
| epica di piattaforma non implementata (UC 0061-0063) | Il livello conversazionale non esiste: qui si dichiara solo il contratto di `crea_richiesta`, che vive dentro il servizio |

## 7. Fuori ambito

- **rispondere** alla richiesta e leggere il filo in forma completa: storia `0007` (qui il dettaglio mostra il solo
  messaggio iniziale);
- il **cambio di stato** e la macchina a stati completa: storia `0009` (qui la richiesta nasce `aperta` e ci resta);
- **elenco, ricerca e viste** di lavoro: storia `0010` (qui si arriva alla richiesta appena creata, non la si cerca);
- il **riconoscimento avanzato del richiedente** e la fusione delle schede doppie: storia `0012` (qui il criterio è
  uno solo, la corrispondenza esatta dell'indirizzo normalizzato);
- l'**assegnazione a un operatore** e la presa in carico: storia `0020`;
- i **canali automatici** che aprono richieste da soli: epica 03, storie `0013`-`0017`;
- gli **allegati** al messaggio iniziale: storia `0016`;
- la **coda di revisione** delle richieste contrassegnate: qui si valorizza il contrassegno, non si costruisce la
  schermata che lo consulta — la consultazione arriva con le viste di lavoro (storia `0010`) e con la conservazione
  governata (storia `0036`).

## 8. Punti aperti

- **Il numero progressivo riparte ogni anno o è continuo per sempre?** Qui si propone il contatore **continuo per
  account**, perché una richiesta di assistenza non ha alcun obbligo di numerazione annuale (§2.3 della descrizione:
  il dominio non è regolato) e la ripartenza annuale creerebbe numeri ambigui nelle conversazioni fra operatori
  («la 12» quale?). Chiude lo **sviluppatore**.
- **La classificazione dei dati personali resta da confermare** (§6 della descrizione, punto 2 del §11): questa
  storia scrive le prime quattro voci del manifesto secondo la proposta, ma il manifesto si compila **insieme** allo
  sviluppatore, e il ruolo di responsabile del trattamento richiede un contratto di nomina che oggi la piattaforma
  non ha per questa fattispecie (punto 4 del §11). Chiudono lo **sviluppatore** e la **revisione legale pre-go-live**.
