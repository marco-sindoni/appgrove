# 0026 — Rapporto periodico della reputazione

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 05 — Reputazione e vetrina
**Storia**: `0026` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0022`, `0023`, `0016`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che apre l'app solo quando succede qualcosa
> voglio ricevere ogni mese un riepilogo di come sta andando la reputazione di ogni mia sede
> così da accorgermi di uno scivolamento lento senza dover ricordarmi di andare a guardare.

**Contesto.** Le tre storie precedenti hanno costruito i numeri (0022), la lettura dei temi (0023) e la prova di
equità (0016). Sono tutte informazioni che stanno **dentro** l'app, e il cliente tipo di RepGrove entra nell'app
quando arriva un avviso, cioè quando è già successo qualcosa di brutto (storia 0020). Il danno che questa app deve
evitare è però l'altro: la media che scende di un decimo al mese senza che accada niente di notiziabile
(descrizione §1). È il caso in cui nessun avviso scatta e nessuno guarda.

Il rapporto periodico è la risposta, ed è anche il momento in cui l'app dice le cose che non ha altro modo di
dire: che un collegamento è scaduto, che la dichiarazione di trasparenza è da riconfermare, che ci sono
diciassette recensioni senza risposta da più di due settimane. La rassegna di mercato indica che il cliente vuole
«solo chiedere ai clienti e vedere le risposte» (descrizione §2.5): il rapporto è il modo di portargli il secondo
pezzo senza chiedergli di venire a prenderselo.

## 2. Requisiti funzionali

1. **RF-1** — A inizio periodo l'app genera per ciascuna sede attiva il rapporto del periodo chiuso, con: media e
   volume del periodo e del precedente, distribuzione dei voti, recensioni ricevute per piattaforma, recensioni
   negative e quante sono state prese in carico, quota di recensioni senza risposta e tempo medio di risposta,
   temi ricorrenti in crescita e in calo, riepilogo di equità (quanti clienti serviti, quanti invitati, quanti no
   e per quale motivo, in forma aggregata).
2. **RF-2** — Il rapporto porta in evidenza le **cose da sistemare**: collegamento scaduto o revocato,
   dichiarazione di trasparenza da riconfermare, recensioni negative senza risposta oltre la soglia, modelli di
   messaggio respinti dal controllo delle pratiche vietate.
3. **RF-3** — Il rapporto arriva per posta elettronica ai destinatari scelti fra gli utenti dell'account; ciascuno
   può disattivarlo per sé. Il responsabile di una sola sede riceve solo la propria.
4. **RF-4** — Il messaggio di posta contiene **solo dati aggregati** e un collegamento all'app: nessun testo di
   recensione, nessun nome di autore, nessun recapito di cliente. Quello che è personale si guarda dentro l'app,
   dove ci sono i controlli di accesso.
5. **RF-5** — Il rapporto si consulta anche a schermo per qualunque periodo passato e si scarica in un file
   leggibile con gli stessi contenuti, per essere girato al commercialista o al socio.
6. **RF-6** — Il periodo è **mensile** e si chiude secondo il fuso orario della sede; una sede attivata a metà mese
   riceve il primo rapporto alla chiusura del mese successivo, con l'indicazione che il periodo è parziale.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La generazione gira per account e per sede; ogni interrogazione filtra
  per `tenant_id`. La lavorazione programmata **non** è un'eccezione: prende il `tenant_id` dal contesto del lotto,
  mai da un parametro esterno, e un rapporto di un account non può essere letto da un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/recensioni/v1/rapporti?sede&periodo`,
  `GET …/rapporti/{id}` e `GET …/rapporti/{id}/file`; impostazione dei destinatari su
  `PUT /api/recensioni/v1/sedi/{id}/rapporto`. Errori in `application/problem+json`; definizione OpenAPI aggiornata
  nello stesso commit. Nessuna chiamata sincrona ad altre app.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__rapporto_periodico.sql` sullo schema `app_recensioni`: tabella
  `rapporto_periodico` con `tenant_id`, sede, periodo, istantanea dei valori calcolati, momento di generazione e
  di invio; unicità su `(tenant_id, sede_id, periodo)` perché una lavorazione ripetuta non deve produrre doppioni.
  Chiave primaria a identificativo universale versione 7, colonne di controllo, `deleted_at`. I destinatari si
  memorizzano come **identificativi di utente**, non come indirizzi.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Reputazione* → «Rapporti»: elenco dei periodi, rapporto a schermo
  con gli stessi blocchi del messaggio, pulsante di scaricamento, impostazione dei destinatari. Solo token del
  sistema di design; tema chiaro e scuro; i grafici restano leggibili anche in scala di grigi, perché finiscono
  stampati.
- **RT-5 — Cinque lingue (§4).** Interfaccia **e** corpo del messaggio in `en, it, fr, es, de` sotto lo
  spazio-nomi `recensioni`; il messaggio usa la lingua del destinatario, non quella della sede.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: il rapporto riguarda sedi già conteggiate dalla
  metrica `sedi_monitorate`. Con abbonamento `canceled` la generazione si ferma e i rapporti già prodotti restano
  consultabili finché l'accesso dura; il ruolo `member` vede solo le sedi che gli competono.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento proprio: la domanda «come sta andando» è già
  servita da `punteggio_reputazione` e `stato_delle_richieste` (storia 0027), e aggiungere uno strumento che spedisce
  un messaggio significherebbe dare a un assistente un effetto verso l'esterno per una funzione che non ne ha
  bisogno. Esclusione deliberata, annotata nel contratto.
- **RT-8 — Dati personali (§10).** Nessun campo personale nuovo. Il punto rilevante è **negativo e va scritto**: il
  messaggio di posta non contiene dati personali, quindi il rapporto non estende il perimetro del manifesto. La
  tabella `rapporto_periodico` contiene aggregati e va comunque elencata fra quelle esaminate dalla storia 0031,
  con la motivazione dell'esclusione.
- **RT-9 — Registrazione eventi (§14).** `rapporto generato`, `rapporto inviato`, `invio fallito` con il codice,
  con `tenant_id`, `app_id`, sede, periodo e identificativo di correlazione; **mai** l'indirizzo del destinatario.

## 4. Criteri di accettazione

**CA-1 — Il rapporto arriva e i numeri tornano**
- **Dato** una sede con recensioni distribuite su due mesi e servizi erogati registrati
- **Quando** la lavorazione chiude il mese
- **Allora** esiste un rapporto con media, volume, distribuzione, confronto con il mese precedente e riepilogo di
  equità, e i valori coincidono con quelli mostrati dalle schermate delle storie 0022 e 0016

**CA-2 — Niente dati personali nel messaggio**
- **Dato** un rapporto con recensioni che contengono nomi e testi
- **Quando** si ispeziona il messaggio di posta prodotto
- **Allora** non contiene testi di recensione, nomi di autori né recapiti: solo numeri, avvisi e un collegamento

**CA-3 — Nessun doppione**
- **Dato** un rapporto già generato per il mese di marzo
- **Quando** la lavorazione viene ripetuta per lo stesso periodo
- **Allora** il rapporto resta uno solo e non parte un secondo messaggio

**CA-4 — Cose da sistemare in evidenza**
- **Dato** una sede con collegamento `scaduto` e dichiarazione di trasparenza `da_riconfermare`
- **Quando** si apre il rapporto
- **Allora** i due avvisi compaiono in testa, con il collegamento al punto dell'app in cui si risolvono

**CA-5 — Periodo parziale**
- **Dato** una sede attivata il 20 del mese
- **Quando** si chiude quel mese
- **Allora** il rapporto è marcato come periodo parziale e non confronta con un periodo precedente inesistente

**CA-6 — Isolamento fra account e ruoli**
- **Dato** due account, e nel secondo un utente `member` associato a una sola sede
- **Quando** ciascuno consulta i rapporti
- **Allora** non si vedono rapporti dell'altro account, e il `member` vede solo quelli della propria sede

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo dei blocchi del rapporto e sul confronto fra periodi (compreso il periodo
      parziale) e di **integrazione** sulla lavorazione programmata con database effimero, verificando l'assenza di
      doppioni alla seconda esecuzione;
- [ ] prova di **isolamento fra account** sulla generazione e sulla lettura dei rapporti;
- [ ] **prova end-to-end**: *rimando* alla storia 0030, con voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) — il rapporto dipende da una
      lavorazione a calendario e si prova meglio con prove di integrazione;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, interfaccia e corpo del messaggio;
- [ ] **manifesto dei dati**: nessuna voce nuova, con la verifica **esplicita** che il messaggio non contenga dati
      personali (è una prova, non una promessa);
- [ ] **registro delle decisioni** compilato, con la scelta di tenere i dati personali fuori dal messaggio e con la
      periodicità mensile;
- [ ] contratto degli **strumenti conversazionali**: esclusione deliberata, annotata;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0022` | media, volume, distribuzione e andamento sono il corpo del rapporto |
| storia `0023` | i temi ricorrenti sono uno dei blocchi |
| storia `0016` | il riepilogo di equità viene dal registro, in forma aggregata |
| storia `0014` | il canale di recapito dei messaggi è già impostato lì; qui si riusa, non si duplica |

## 7. Fuori ambito

- la periodicità settimanale o su richiesta: si può aggiungere, ma moltiplicare i messaggi è il modo più rapido di
  farsi disattivare;
- il rapporto comparativo fra sedi di clienti diversi o con i concorrenti: è il punto aperto n. 3 della
  descrizione §11;
- l'invio del rapporto a destinatari esterni all'account (il commercialista, l'agenzia): si scarica il file e lo si
  gira a mano, perché mandare dati a un indirizzo che non è di un utente aprirebbe un canale da governare.

## 8. Punti aperti

- **Quali avvisi meritano il rapporto e quali una notifica immediata**: la soglia «recensioni negative senza
  risposta» esiste già come avviso (storia 0020) e qui torna come riepilogo. Il rischio è la doppia comunicazione
  sullo stesso fatto; la proposta è che il rapporto riepiloghi ciò che è rimasto aperto, non ciò che è già stato
  gestito. **Da confermare.**
- **Il rapporto in un file impaginato** (per la stampa) invece che leggibile a schermo: è una scelta di prodotto con
  un costo tecnico non banale; qui si propone il formato leggibile e si rimanda.
