# 0015 — Scarico della merce

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 03 — Registro dei movimenti e giacenze
**Storia**: `0015` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0014`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che preleva merce per un lavoro o per una spedizione
> voglio togliere dalla giacenza quello che sto portando via, e sapere subito se non c'è
> così da non promettere a un cliente un articolo che sulla carta c'è e sullo scaffale no.

**Contesto.** Il carico esiste (storia `0014`), ma un magazzino che si riempie e non si svuota non è un magazzino.
Lo scarico è il movimento più frequente e anche il più esposto: **due persone che scaricano lo stesso articolo nello
stesso istante sono il caso normale, non il caso limite** — uno prende i pezzi in magazzino mentre l'altro li carica
sul furgone, entrambi con il telefono in mano. Se il programma sbaglia qui, sbaglia il saldo, e un saldo sbagliato è
il rischio esistenziale di questa applicazione (descrizione dell'applicazione, §11): il cliente non se ne accorge
subito, se ne accorge il giorno in cui promette merce che non ha, e a quel punto smette di fidarsi per sempre.

**Come si risolve la concorrenza.** In una sola transazione si inserisce il movimento e si esegue un aggiornamento
**condizionato** della riga di giacenza — `SET quantita = quantita + :delta, versione = versione + 1` con la
condizione `quantita + :delta >= 0`. Le due transazioni si serializzano sul blocco della riga: la seconda vede il
valore già aggiornato dalla prima, non quello che aveva letto all'inizio. Chi perde la corsa non ottiene un saldo
sbagliato: ottiene `409` con la quantità residua, e **nulla viene scritto**. Il difetto che questa regola esclude ha
un nome preciso ed è la sequenza *leggo 5, calcolo 5 − 3, scrivo 2*: fatta da due richieste insieme, lascia 2 pezzi
invece di rifiutarne una, e il magazzino ha appena consegnato merce che non aveva.

## 2. Requisiti funzionali

1. **RF-1** — Esiste la registrazione di uno scarico: articolo, deposito, ubicazione facoltativa, quantità
   **strettamente positiva** indicata dall'utente e registrata con segno negativo, motivo fra quelli ammessi con
   segno negativo, riferimento al documento d'origine facoltativo, nota facoltativa.
2. **RF-2** — Lo scarico produce **un** movimento di tipo `scarico` e aggiorna la riga di giacenza nella stessa
   transazione, con l'aggiornamento condizionato descritto sopra: se la condizione non è soddisfatta l'intera
   transazione fallisce e non resta scritto né il movimento né l'aggiornamento.
3. **RF-3** — Uno scarico che porterebbe la giacenza sotto zero è respinto con `409` e un messaggio che riporta la
   **quantità residua reale** al momento del rifiuto («giacenza insufficiente: ne restano 2»), non un errore
   generico.
4. **RF-4** — Due scarichi concorrenti sullo stesso articolo e deposito non possono mai lasciare un saldo diverso
   dalla somma dei movimenti effettivamente registrati: o passano entrambi, o passa quello per cui c'è merce.
5. **RF-5** — Un secondo invio con la stessa `chiave_idempotenza` non crea un secondo movimento e restituisce il
   movimento già registrato: il telefono che ritenta perché la rete è andata via non deve contare due volte lo
   stesso prelievo.
6. **RF-6** — La schermata di scarico mostra la giacenza disponibile **prima** di confermare e la ricarica dopo ogni
   registrazione, così che chi scarica una serie di articoli veda sempre il valore aggiornato.
7. **RF-7** — La giacenza **non può andare negativa per uno scarico registrato da una persona**. I movimenti
   generati da un fatto già avvenuto (una vendita già battuta) seguono una regola diversa, che è della storia `0019`.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `movimento` e `giacenza` filtra per
  `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri
  viene ignorato. Prova di isolamento fra due account sulla risorsa dello scarico.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/magazzino/v1/movimenti` con tipo `scarico`;
  il rifiuto per giacenza insufficiente è `409` in `application/problem+json`, con un campo che riporta la quantità
  residua; validazione dichiarativa sul corpo; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** **Nessuna migrazione nuova**: le tabelle sono quelle della storia `0013`.
  L'aggiornamento condizionato è scritto come singola istruzione sul database, con il conteggio delle righe toccate
  usato come esito: zero righe toccate significa merce insufficiente. **Vietato** leggere la quantità in Java e
  riscriverla; il collaudo deve dimostrare che la sequenza leggi-calcola-scrivi non esiste nel codice del percorso
  di scrittura.
- **RT-4 — Modulo frontend (§3, §5).** Nuova sezione «Scarico» del modulo `magazzino`; la giacenza disponibile è
  letta con il client generato e ricaricata dopo ogni conferma; l'errore `409` è mostrato con la quantità residua e
  non come messaggio tecnico; solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili, compreso il messaggio di giacenza insufficiente con il
  numero residuo interpolato, passano dallo spazio-nomi `magazzino` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** **Nessun consumo di quota**: registrare uno scarico **non consuma quota e non
  viene mai respinto con `429`**, nemmeno a tetto di `articoli_gestiti` raggiunto. È la regola più importante del
  listino di questa app: impedire a un'impresa di registrare uno scarico perché ha finito il piano significa
  corrompere il suo saldo e restituirle un dato falso quando tornerà a pagare (descrizione dell'applicazione, §5).
  Restano gli altri varchi: `402` con abbonamento `canceled`, `403` con ruolo insufficiente.
- **RT-7 — Esposizione conversazionale (§12).** Lo strumento `registra_scarico(articolo, deposito, quantità,
  motivo?, riferimento?) → bozza di movimento` è dichiarato qui come contratto e implementato nella storia `0035`:
  è di **scrittura**, produce una bozza con conferma umana e può essere rifiutato per giacenza insufficiente
  esattamente come la rotta. Il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo.** L'autore dello scarico (`created_by`) è già
  dichiarato nel manifesto dalla storia `0010`. **Nessun indicatore di produttività per persona**: il dato serve a
  spiegare una differenza di merce, non a misurare chi la muove (descrizione dell'applicazione, §6).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `scarico registrato`, `scarico respinto per giacenza
  insufficiente` e `scarico duplicato ignorato per idempotenza` sono registrati con `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione, senza note e senza nomi.

## 4. Criteri di accettazione

**CA-1 — Scarico che fa scendere la giacenza**
- **Dato** un articolo con giacenza `10` nel deposito «Furgone»
- **Quando** un addetto registra uno scarico di 3 pezzi con motivo `consumo per lavoro`
- **Allora** la giacenza diventa `7`, esiste un movimento di tipo `scarico` con quantità `−3` e la `versione` della
  riga di giacenza è aumentata di uno

**CA-2 — Due scarichi simultanei sullo stesso articolo**
- **Dato** un articolo con giacenza `5` in un deposito
- **Quando** due richieste di scarico da 3 pezzi ciascuna arrivano nello stesso istante sulla stessa coppia
  articolo-deposito
- **Allora** una sola va a buon fine, l'altra riceve `409` con «giacenza insufficiente: ne restano 2»; la giacenza
  finale è `2`; i movimenti di scarico registrati sono **uno solo**

**CA-3 — Scarico maggiore della giacenza**
- **Dato** un articolo con giacenza `2`
- **Quando** si tenta uno scarico di 5 pezzi
- **Allora** la risposta è `409` con la quantità residua `2`, nessun movimento viene scritto e la giacenza resta `2`

**CA-4 — Idempotenza dell'invio ripetuto**
- **Dato** uno scarico di 3 pezzi già registrato con chiave di idempotenza `xyz-9`
- **Quando** il telefono ritenta l'invio identico dopo un'interruzione di rete
- **Allora** la risposta è `200` con il movimento già esistente, la giacenza non scende una seconda volta e i
  movimenti restano uno

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con lo stesso codice articolo e giacenze diverse
- **Quando** un utente di `A` registra uno scarico
- **Allora** cambia solo la giacenza dell'account `A`, anche se forza l'identificativo dell'account `B` nel corpo
  della richiesta o in un parametro

**CA-6 — La quota non blocca lo scarico**
- **Dato** un account che ha raggiunto il tetto di `articoli_gestiti`
- **Quando** registra uno scarico su un articolo esistente
- **Allora** lo scarico va a buon fine e non viene restituito `429`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo del delta e sulla validazione del segno, e di **integrazione** sulla rotta
      dello scarico con database effimero e migrazioni vere;
- [ ] prova di **concorrenza** dedicata: due transazioni parallele di scarico sulla stessa coppia articolo-deposito,
      con verifica che il saldo finale sia coerente e che i movimenti scritti siano esattamente quelli riusciti;
- [ ] prova di **isolamento fra account** sullo scarico;
- [ ] **prova end-to-end**: *rimando* — il passo «due scarichi simultanei» del percorso `[J-MAGAZZINO]` è di
      proprietà della storia `0036`, e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, compreso il messaggio con la quantità residua;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con la scelta dell'aggiornamento condizionato nella base di dati e il
      motivo per cui la sequenza leggi-calcola-scrivi è vietata;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `registra_scarico` (scrittura, con conferma);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0013` | Registro, giacenza con versione, idempotenza e aggiornamento condizionato |
| `0014` | La merce deve poter entrare prima di poter uscire; le due schermate condividono la forma |

## 7. Fuori ambito

- Lo spostamento fra depositi, che è un'uscita e un'entrata insieme: storia `0016`.
- La correzione di uno scarico sbagliato: si fa **solo** con lo storno, storia `0017`.
- Il cambio di saldo dichiarando che il registro era sbagliato: è la rettifica con motivo obbligatorio, storia
  `0021`, e non ha nulla a che vedere con lo scarico.
- Gli scarichi generati automaticamente da una vendita o da un documento di trasporto: storia `0019`, che ha una
  regola diversa sul negativo.
- Lo scarico avviato inquadrando un codice a barre: storia `0031`.

## 8. Punti aperti

- **La giacenza può andare negativa?** La proposta di questa storia è **no** per i movimenti registrati da una
  persona (`409` con la quantità residua), perché una persona davanti allo scaffale può contare; **sì** per quelli
  generati da un fatto già avvenuto (storia `0019`), perché rifiutare un fatto accaduto corromperebbe il registro.
  È una scelta di prodotto con conseguenze visibili in interfaccia e non è stata trovata discussa in nessuna fonte
  (descrizione dell'applicazione, §2.7 e §11 punto 3): la chiude lo sviluppatore.
- **Prenotazione della merce** (impegnare pezzi per un ordine senza ancora scaricarli): richiesta prevedibile e
  fuori perimetro; introdurrebbe una seconda quantità accanto alla giacenza e va decisa come direzione di prodotto,
  non aggiunta come campo.
