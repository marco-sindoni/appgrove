# 0021 — Priorità ed etichette

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 04 — Organizzazione del lavoro
**Storia**: `0021` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0009`, `0010`, `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che risponde ai clienti
> voglio marcare quanto una richiesta è urgente e attaccarle una parola mia per ritrovarla
> così da non trattare allo stesso modo un reclamo e una domanda sulla fattura, e da ritrovare a colpo d'occhio
> tutte le richieste sullo stesso problema.

**Contesto.** La priorità non è un'etichetta di comodo: è il **parametro d'ingresso** degli obiettivi di servizio
dell'epica 05 — la politica di servizio (storia `0024`) promette tempi diversi per priorità diverse, e senza un
valore di priorità sulla richiesta non ha nulla su cui calcolare. Per questo la priorità va introdotta adesso, in
epica 04, e non insieme agli orologi: se arrivasse dopo, la storia `0024` dovrebbe tornare indietro. Le priorità
sono **quattro e fisse** — bassa, normale, alta, urgente — e il cliente non le configura: una scala che ogni
account ridisegna a modo suo rende impossibile spiegare cosa promette il prodotto. Le **etichette** sono l'esatto
opposto, e di proposito: parole libere che gli operatori scrivono, servono a ritrovare le cose («resi natale»,
«bug pagamenti») e non pilotano nulla.

## 2. Requisiti funzionali

1. **RF-1** — Ogni richiesta ha una priorità fra quattro valori fissi — bassa, normale, alta, urgente — con
   «normale» come valore predefinito all'apertura. L'elenco non è configurabile dal cliente.
2. **RF-2** — Un operatore può cambiare la priorità dal dettaglio della richiesta e su più richieste insieme
   dall'elenco; ogni cambio lascia traccia (chi, quando, da quale valore a quale) visibile sul dettaglio.
3. **RF-3** — La priorità è **esposta** come attributo della richiesta e come criterio di filtro e di ordinamento
   nell'elenco (storia `0010`); il calcolo delle scadenze a partire da essa **non** è di questa storia.
4. **RF-4** — Un operatore può attaccare una o più etichette a una richiesta, scrivendo una parola nuova al volo o
   scegliendone una già usata da un elenco di suggerimenti che si restringe mentre si digita.
5. **RF-5** — L'elenco e la ricerca (storia `0010`) filtrano per priorità e per etichetta, anche combinandole.
6. **RF-6** — La sezione Impostazioni → Etichette permette di rinominare un'etichetta (il nuovo nome si propaga a
   tutte le richieste che la portano), di **unire** due etichette in una sola e di disattivarne una (sparisce dai
   suggerimenti, resta leggibile dove è già attaccata). Il numero di etichette attive per account è limitato
   (proposta: **cinquanta**); al superamento la creazione è rifiutata con `422` e una spiegazione.
7. **RF-7** — Accanto alla casella delle etichette compare l'avviso che le etichette sono **testo libero visibile a
   tutti gli operatori** e non vanno usate per il nome di una persona né per informazioni delicate sulla sua
   situazione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura delle entità `Label` e delle associazioni con la
  richiesta filtra per `tenant_id` preso dal token verificato; un'etichetta di un account non compare mai nei
  suggerimenti di un altro, nemmeno se il nome coincide. Un `tenant_id` che arrivasse dal corpo viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `PATCH /api/helpdesk/v1/tickets/{id}` (priorità),
  `POST /api/helpdesk/v1/tickets/bulk-priority` (cambio su più richieste),
  `GET|POST /api/helpdesk/v1/labels`, `PATCH /api/helpdesk/v1/labels/{id}` (rinomina, disattivazione),
  `POST /api/helpdesk/v1/labels/{id}/merge` (unione) e `PUT /api/helpdesk/v1/tickets/{id}/labels`; corpo validato
  con la priorità ristretta ai quattro valori ammessi; errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__priority_and_labels.sql` sullo schema `app_helpdesk`: colonna
  `priority` su `ticket` con valore predefinito «normale» e indice a partire da `tenant_id`, tabella
  `ticket_priority_change` per la traccia, tabelle `label` e `ticket_label` per le etichette. Tutte con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica; unicità del nome
  dell'etichetta per account fra le righe non cancellate, confrontata **senza distinzione fra maiuscole e
  minuscole** perché «Resi» e «resi» non devono diventare due etichette. **Nessuna chiave esterna verso altri
  schemi**.
- **RT-4 — Modulo frontend (§3, §5).** Selettore di priorità sul dettaglio e azione in blocco sull'elenco, casella
  delle etichette con suggerimenti, sezione Impostazioni → Etichette. La priorità si distingue con i token del
  sistema di design (nessun colore scritto a mano) e **mai** con il solo colore: serve anche il testo, altrimenti
  chi non distingue i colori non la legge. Dati letti con il client generato; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I nomi delle quattro priorità e tutte le stringhe dell'interfaccia passano dallo
  spazio-nomi `helpdesk` e sono presenti in `en, it, fr, es, de`. Il **nome delle etichette** lo scrive il cliente e
  resta nella sua lingua: non si traduce e non si tenta di indovinarne la lingua.
- **RT-6 — Varchi e quota (§6, §7).** Né la priorità né le etichette consumano la metrica `agents`: il tetto di
  RF-6 è un limite di prodotto e produce `422`, non `429`. La catena dei varchi resta quella di piattaforma
  (`401 → 403 → 402 → 403 → 429`): con abbonamento `canceled` o `paused` le rotte rispondono `402`.
- **RT-7 — Esposizione conversazionale (§12).** Questa storia dà fondamento allo strumento
  `inoltra_richiesta(numero, motivo) → esito` del §7 della descrizione dell'applicazione, che **alza la priorità** e
  avvisa il responsabile: marcato **scrittura reversibile e interna**, senza conferma umana obbligatoria. Le
  etichette **non** introducono strumenti di scrittura: farle scrivere a un assistente moltiplicherebbe le varianti
  della stessa parola e distruggerebbe l'unico scopo dell'etichetta, che è ritrovare. Il contratto vive dentro il
  servizio; il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** La storia introduce un campo che può riguardare una persona: il **nome
  dell'etichetta** (`label.name`) è testo libero scritto dagli operatori e nulla impedisce di scriverci «Mario
  Rossi» o peggio. Voce nuova nel manifesto `docs/compliance/manifests/helpdesk.yaml` in **italiano e inglese**,
  campo annotato `@PersonalData`, tabelle `label` e `ticket_label` aggiunte **sia** a `exportData` **sia** a
  `purgeData` del contratto `HelpdeskDataContract`. L'avviso di RF-7 è il presidio proposto, ed è lo stesso
  ragionamento del testo libero al §6 della descrizione dell'applicazione. La priorità è un valore fra quattro:
  nessun dato personale. Su DeskGrove appgrove è **responsabile del trattamento** per conto dell'azienda cliente,
  non titolare.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «priorità cambiata» (con il valore vecchio e quello nuovo, che
  sono fra quattro costanti), «etichetta creata», «etichetta rinominata», «etichette unite» e «creazione respinta
  per tetto raggiunto» sono registrati con `tenant_id`, `app_id`, `user_id`, identificativo della richiesta e
  identificativo di correlazione, **senza** il nome dell'etichetta, che è testo libero.

## 4. Criteri di accettazione

**CA-1 — Priorità cambiata e tracciata**
- **Dato** una richiesta aperta con priorità «normale»
- **Quando** un operatore la porta a «urgente»
- **Allora** la richiesta mostra la nuova priorità e il dettaglio registra chi l'ha cambiata, quando e da quale
  valore

**CA-2 — Etichetta creata al volo e poi suggerita**
- **Dato** un account senza etichette
- **Quando** un operatore scrive «resi natale» su una richiesta e poi comincia a digitare «res» su un'altra
- **Allora** la prima richiesta porta l'etichetta e sulla seconda «resi natale» compare fra i suggerimenti, senza
  creare un doppione

**CA-3 — Filtro combinato**
- **Dato** un elenco con richieste di priorità diversa ed etichette diverse
- **Quando** un operatore filtra per priorità «alta» **e** etichetta «bug pagamenti»
- **Allora** vede solo le richieste che soddisfano entrambi i criteri, e il conteggio del filtro lo conferma

**CA-4 — Rinomina che si propaga e unione**
- **Dato** venti richieste con l'etichetta «resi» e cinque con «reso»
- **Quando** l'amministratore unisce «reso» dentro «resi»
- **Allora** tutte e venticinque le richieste portano «resi», l'etichetta «reso» sparisce dai suggerimenti e nessuna
  associazione va perduta

**CA-5 — Tetto delle etichette raggiunto**
- **Dato** un account con cinquanta etichette attive
- **Quando** un operatore prova a crearne una nuova al volo
- **Allora** riceve `422` con un messaggio che spiega il limite e propone di riusare o unire un'etichetta esistente,
  e nulla viene creato

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, entrambi con un'etichetta chiamata «urgente cliente»
- **Quando** un operatore di `A` digita nella casella delle etichette e poi prova ad attaccare l'etichetta di `B`
  forzandone l'identificativo nel corpo
- **Allora** vede solo la propria fra i suggerimenti e l'associazione forzata è rifiutata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla normalizzazione del nome dell'etichetta (spazi e maiuscole) e sull'unione, e di
      **integrazione** sulle risorse di priorità ed etichette, con database effimero e migrazioni Flyway vere;
- [ ] prova di **isolamento fra account** su etichette, suggerimenti e associazioni;
- [ ] **prova end-to-end**: **nessun impatto** sul percorso `[J-HELPDESK]` — priorità ed etichette non stanno nel
      percorso minimo dal modulo di contatto alla soddisfazione; la copertura resta alle prove d'integrazione, e la
      risposta va scritta nel **registro di copertura**
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`) per i nomi delle priorità e per
      l'interfaccia, con la nota che i nomi delle etichette restano nella lingua del cliente;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `label.name`, campo annotato `@PersonalData`,
      tabelle `label` e `ticket_label` presenti in esportazione e cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotato perché le priorità sono
      quattro e fisse e perché le etichette non sono configurabili come tassonomia;
- [ ] contratto degli **strumenti conversazionali**: `inoltra_richiesta` dichiarato, e la scelta di non esporre
      scrittura sulle etichette motivata;
- [ ] controllo automatico di **accessibilità** verde, con verifica che la priorità non sia distinguibile dal solo
      colore;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove (i dati di prova della
      storia `0005` nascono con priorità e qualche etichetta).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0009` — ciclo di vita degli stati | La priorità convive con lo stato: sono due attributi distinti della stessa richiesta e non devono confondersi |
| Storia `0010` — elenco, ricerca e viste | Serve l'elenco su cui aggiungere filtro, ordinamento e azione in blocco |
| Storia `0018` — operatori e posti | Solo un operatore può cambiare priorità ed etichette |
| Storia `0024` — politiche di servizio (epica 05) | **Dipendenza inversa**: è `0024` a dipendere da questa. Qui si espone il valore, là si calcolano gli obiettivi |

## 7. Fuori ambito

- **Il calcolo delle scadenze a partire dalla priorità**: lo fanno le storie `0024` e `0025` dell'epica 05. Questa
  storia consegna il **parametro**, non l'orologio.
- **L'aumento automatico di priorità al passare del tempo** («da alta a urgente dopo due giorni»): non si fa qui. È
  un automatismo che ha senso solo insieme agli orologi e appartiene all'epica 05.
- **La deduzione automatica della priorità dal testo del messaggio**: esclusa, e non per pigrizia — analizzare il
  contenuto con un servizio esterno significherebbe aggiungere un responsabile del trattamento sul dato più
  delicato dell'app (§1 e §6 della descrizione dell'applicazione).
- **Le etichette come tassonomia con gerarchie, colori obbligatori o regole di applicazione automatica**: fuori
  perimetro. È l'inizio della complessità che il §2.5 della descrizione dell'applicazione dice di evitare.
- **L'avviso a chi presidia quando una richiesta diventa urgente**: appartiene alla storia `0026`, che possiede gli
  avvisi.

## 8. Punti aperti

- **Tetto di cinquanta etichette per account** — proposta di prodotto, non vincolo tecnico. La chiude lo
  sviluppatore, e va decisa prima: abbassarla dopo significa togliere qualcosa a chi ci sta sopra.
- **Chi può cambiare la priorità?** La proposta è «ogni operatore», perché in una squadra di tre persone un
  permesso in più costa più di quanto protegga. Se la priorità pilota una promessa contrattuale verso il cliente
  finale, però, è ragionevole riservarla a `owner` e `admin`: è una scelta di direzione di prodotto e la chiude lo
  sviluppatore.
- **La priorità la vede il richiedente?** Oggi no. Mostrarla nel portale (storia `0032`) trasformerebbe un
  strumento di lavoro interno in una promessa esplicita verso il cliente finale. Decisione di direzione di
  prodotto: la chiude lo sviluppatore, insieme alla storia `0032`.
