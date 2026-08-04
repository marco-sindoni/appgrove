# 0009 — Ciclo di vita degli stati

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 02 — Casella condivisa e conversazioni
**Storia**: `0009` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che guarda la coda una volta al giorno
> voglio che ogni richiesta dica da sola a che punto è, e che ci arrivi seguendo regole sempre uguali
> così da distinguere in un colpo d'occhio ciò che aspetta noi da ciò che aspetta il cliente, e da non trovarmi
> richieste ferme in uno stato che non vuol dire niente.

**Contesto.** Fino a qui una richiesta nasce `aperta` e ci resta per sempre: il filo cresce, ma lo stato non si
muove. La macchina a stati del §4 della descrizione dell'applicazione è la regola che tutte le storie successive
devono rispettare — le viste di lavoro (`0010`) filtrano per stato, gli orologi del servizio si mettono in pausa in
`in attesa del cliente` (`0025`), l'indagine di soddisfazione parte alla chiusura (`0027`). Costruirla adesso, prima
che qualcuno cominci a dipenderne, costa una giornata; costruirla dopo significa correggere cinque storie. La forma
è ripresa da una decisione già presa e collaudata nell'assistenza interna della piattaforma (UC 0075, change
`0084`): si copiano l'idea e la semantica di «la palla è al cliente», **non** i dati né le tabelle (§10 della
descrizione).

## 2. Requisiti funzionali

1. **RF-1** — Gli stati sono esattamente cinque — `aperta`, `in lavorazione`, `in attesa del cliente`, `risolta`,
   `chiusa` — e nessun altro valore è ammesso: un valore fuori elenco è rifiutato dal servizio e dal vincolo del
   database.
2. **RF-2** — Le transizioni **ammesse** sono soltanto queste: `aperta → in lavorazione`;
   `aperta | in lavorazione → in attesa del cliente`; `in attesa del cliente → in lavorazione`;
   `in lavorazione | in attesa del cliente → risolta`; `risolta → chiusa`; `risolta → in lavorazione` (ripensamento
   dell'operatore). Ogni altra transizione è rifiutata con `409` e un messaggio che elenca quelle possibili da lì.
3. **RF-3** — La registrazione di un messaggio in **uscita** (storia `0007`) porta la richiesta in `in attesa del
   cliente`; un messaggio in **ingresso** su una richiesta in `in attesa del cliente` o `risolta` la riporta in
   `in lavorazione`. Una **nota interna** (storia `0008`) non cambia mai lo stato.
4. **RF-4** — Una richiesta ferma in `risolta` da **7 giorni** senza nuovi messaggi passa a `chiusa` da sola,
   per lavorazione periodica; l'arrivo di un messaggio fa ripartire il conteggio da capo.
5. **RF-5** — Una richiesta `chiusa` **non si modifica più**: non accetta messaggi né cambi di stato. Un messaggio
   in ingresso su una richiesta chiusa **apre una richiesta nuova**, con numero proprio, collegata a quella chiusa;
   la richiesta chiusa resta esattamente com'era, e le due si rimandano l'una all'altra.
6. **RF-6** — Ogni cambio di stato registra **chi**, **quando**, **da quale stato a quale** ed eventuale motivo, ed
   è consultabile nel filo come evento di sistema, distinto dai messaggi; i cambi automatici sono attribuiti al
   sistema, non a una persona.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il cambio di stato filtra per `tenant_id` preso dal token verificato; una
  richiesta di un altro account risponde `404`. La **lavorazione periodica** di chiusura automatica non è esente: la
  sua interrogazione porta il `tenant_id` riga per riga e non chiude mai richieste fuori dal proprio account. Prova
  di isolamento sia sulla rotta sia sulla lavorazione periodica.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/helpdesk/v1/tickets/{id}/status` con lo stato di
  destinazione e un motivo facoltativo; corpo validato in modo dichiarativo; errori in `application/problem+json`
  (`400` stato non riconosciuto, `409` transizione non ammessa o richiesta chiusa, `404` richiesta altrui);
  definizione OpenAPI aggiornata nello stesso commit. La macchina a stati vive in **un solo posto** dentro il
  servizio: nessuna rotta e nessun canale futuro può cambiare lo stato aggirandola.
- **RT-3 — Persistenza (§8).** Migrazione `V6__ticket_lifecycle.sql` sullo schema `app_helpdesk`: tabella
  `ticket_event` (`tenant_id`, richiesta, tipo, stato di partenza, stato di arrivo, autore, motivo, data) con chiave
  primaria UUID versione 7, colonne di controllo e cancellazione logica; colonne `resolved_at`, `closed_at` e
  `reopened_from_ticket_id` su `ticket`, dove il collegamento alla richiesta precedente è un **riferimento logico**
  senza chiave esterna. Vincolo di controllo sull'elenco degli stati ammessi. Indice `(tenant_id, status,
  resolved_at)` per la lavorazione periodica.
- **RT-4 — Modulo frontend (§3, §5).** Nella schermata di dettaglio del modulo `helpdesk`: indicatore di stato,
  azioni che mostrano **solo** le transizioni ammesse da lì (una transizione vietata non deve essere cliccabile e
  poi rifiutata), gli eventi di sistema intercalati nel filo con stile distinto dai messaggi, e il rimando alla
  richiesta collegata quando c'è. Solo token del sistema di design; tema chiaro e scuro; controllo automatico di
  accessibilità.
- **RT-5 — Cinque lingue (§4).** I nomi dei cinque stati, i messaggi di rifiuto delle transizioni e il testo degli
  eventi di sistema passano dallo spazio-nomi `helpdesk` e sono presenti in `en, it, fr, es, de`. Lo stato si
  memorizza come **valore tecnico stabile**, mai come testo tradotto.
- **RT-6 — Varchi e quota (§6, §7).** Cambiare stato **non consuma quota**: la metrica unica dell'app è `agents`
  (posti operatore, natura `stock`), consumata dalla storia `0018`. Restano i varchi a monte: `401`, `402` con
  abbonamento non attivo, `403` per ruolo insufficiente; il ruolo `member` può cambiare stato. La **chiusura
  automatica continua a funzionare anche con abbonamento in `past_due`**, come previsto dalla piattaforma (§13), e
  non è un'azione dell'utente. La storia non fissa prezzi: consuma il tetto pubblicato dall'abilitazione.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `cambia_stato(numero, stato, motivo?) → esito`, marcato **scrittura reversibile e interna**, **senza** conferma
  umana: non produce effetti verso l'esterno e si disfa cambiando di nuovo stato. Lo strumento passa dalla stessa
  macchina a stati e riceve gli stessi `409`. Il contratto vive dentro il servizio; il server conversazionale è di
  piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: gli eventi di stato contengono identificativi,
  date e stati, non testo di persone. La tabella `ticket_event` va però comunque aggiunta a `exportData` e
  `purgeData` di `HelpdeskDataContract`, perché è legata al richiedente e la cancellazione per singolo richiedente
  (storia `0036`) non deve lasciarne indietro le righe. Il motivo facoltativo del cambio di stato è **testo libero
  scritto da un operatore**: attraversa il riconoscitore delle categorie particolari con il solo contrassegno
  booleano, e il manifesto `docs/compliance/manifests/helpdesk.yaml` dichiara la voce in italiano e inglese.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `stato cambiato` (con stato di partenza e di arrivo),
  `transizione rifiutata`, `richiesta chiusa automaticamente` e `richiesta riaperta` sono registrati con
  `tenant_id`, `app_id`, `user_id` (o l'indicazione del sistema per i cambi automatici) e identificativo di
  correlazione, **senza** il motivo scritto dall'operatore e senza altri dati personali.

## 4. Criteri di accettazione

**CA-1 — Presa in carico**
- **Dato** una richiesta `aperta` e un utente abilitato con ruolo `member`
- **Quando** porta la richiesta in `in lavorazione`
- **Allora** lo stato cambia, e nel filo compare un evento di sistema con chi, quando e la transizione compiuta

**CA-2 — Gli stati seguono la conversazione**
- **Dato** una richiesta `in lavorazione`
- **Quando** l'operatore registra una risposta al cliente, poi scrive una nota interna, poi arriva un messaggio del
  cliente
- **Allora** la richiesta passa a `in attesa del cliente` con la risposta, **resta** `in attesa del cliente` dopo la
  nota interna, e torna `in lavorazione` con il messaggio del cliente

**CA-3 — Transizione non ammessa**
- **Dato** una richiesta `chiusa` · **Quando** si tenta di portarla in `in lavorazione`
- **Allora** la risposta è `409` in `application/problem+json` con l'elenco delle transizioni possibili, e lo stato
  resta `chiusa`

**CA-4 — Chiusura automatica dopo sette giorni**
- **Dato** una richiesta in `risolta` da otto giorni senza nuovi messaggi, e una seconda in `risolta` da otto giorni
  ma con un messaggio ricevuto ieri
- **Quando** la lavorazione periodica gira, con l'orologio pilotato dal test e senza attese a tempo
- **Allora** la prima passa a `chiusa` con evento attribuito al sistema, e la seconda resta `risolta`

**CA-5 — Riapertura che collega e non riscrive**
- **Dato** una richiesta `chiusa` con tre messaggi nel filo
- **Quando** arriva un messaggio in ingresso da quel richiedente su quella richiesta
- **Allora** nasce una **richiesta nuova** con numero proprio, collegata alla precedente e con il rimando visibile in
  entrambe; la richiesta chiusa conserva i suoi tre messaggi, il suo stato e le sue date, invariati

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, entrambi con richieste in `risolta` da otto giorni
- **Quando** un utente di `A` tenta di cambiare lo stato di una richiesta di `B`, e la lavorazione periodica gira
- **Allora** il tentativo riceve `404`, e ciascuna richiesta viene chiusa automaticamente soltanto dentro il proprio
  account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla tabella delle transizioni ammesse (ogni coppia stato-di-partenza / stato-di-arrivo,
      ammessa e vietata) e di **integrazione** sulla rotta e sulla lavorazione periodica, con database effimero,
      migrazioni vere e **orologio iniettabile**: nessuna attesa a tempo nei test;
- [ ] prova di **isolamento fra account** sulla rotta di cambio stato **e** sulla lavorazione periodica;
- [ ] **prova end-to-end**: *coprire ora* — passi «prendi in carico», «risolvi» e «chiudi» del percorso
      `[J-HELPDESK]`, con l'etichetta in testa al titolo del test; registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), con lo stato memorizzato come
      valore tecnico e mai come testo tradotto;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per la tabella `ticket_event` e per il motivo del
      cambio di stato, con la tabella presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotate la tabella delle transizioni ammesse e la scelta della
      **riapertura per collegamento** invece della riscrittura;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `cambia_stato`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove — in particolare il §4
      della descrizione dell'applicazione, se l'implementazione ne raffina la macchina a stati.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | Serve la richiesta con il suo numero: la riapertura ne crea una nuova e deve poterla numerare |
| storia `0007` | Servono i messaggi in ingresso e in uscita, che sono ciò che muove gli stati |
| storia `0008` | Serve il verso «interno», perché la regola «la nota non cambia lo stato» va verificata, non assunta |
| epica di piattaforma non implementata (UC 0061-0063) | Il livello conversazionale non esiste: qui si dichiara solo il contratto di `cambia_stato` |

## 7. Fuori ambito

- la **pausa dell'orologio** in `in attesa del cliente` e il calcolo delle scadenze in ore lavorative: storia `0025`
  — qui si costruisce lo stato che quella pausa userà, non il calcolo;
- l'**assegnazione a un operatore** e l'automatismo «prendo in carico e la richiesta diventa mia»: storia `0020`;
- l'**indagine di soddisfazione** all'arrivo in `chiusa`: storia `0027`;
- la **cancellazione** definitiva delle richieste chiuse per scadenza della conservazione: storia `0036` — qui la
  chiusura automatica è una transizione, non una cancellazione;
- l'**unione di due richieste**, che chiude una delle due con un motivo particolare: storia `0011`, che si appoggia
  alla macchina a stati costruita qui;
- il **messaggio in ingresso proveniente da un canale reale**, che è ciò che scatena riaperture e ritorni in
  lavorazione nel mondo vero: epica 03 — qui il messaggio in ingresso è quello trascritto a mano dall'operatore.

## 8. Punti aperti

- **I sette giorni sono una costante di prodotto o un parametro dell'account?** Qui si propone la **costante**, per
  la stessa ragione per cui l'app non ha un motore di regole (§2.5 della descrizione): un parametro in più è una
  domanda in più a cui il cliente micro non sa rispondere. Il posto naturale in cui diventerebbe parametro è la
  storia `0036`, insieme alla durata di conservazione, che è già destinata a essere governata dal cliente. Chiude lo
  **sviluppatore** come decisione di direzione di prodotto.
- **Una richiesta riaperta deve ereditare priorità, coda e operatore della precedente?** Oggi quei campi non sono
  ancora popolati (storie `0019`-`0021`) e la domanda non è matura. Va ripresa dalla storia `0020`, che introduce
  l'assegnazione: qui la richiesta nuova nasce come una richiesta qualsiasi, con il solo collegamento alla
  precedente. Chiude chi implementa la storia `0020`.
