# 0025 — Valore gestionale delle giacenze

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 04 — Inventario fisico, rettifiche e valore
**Storia**: `0025` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0023`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una micro-impresa
> voglio sapere quanto vale, in ordine di grandezza, la merce che ho sugli scaffali, e poterla consegnare al mio
> commercialista in un file
> così da rispondere alla domanda «quanto capitale ho fermo?» senza credere di aver fatto la valutazione delle
> rimanenze, che non è affar mio né di questo programma.

**Contesto.** Il costo medio ponderato mobile esiste già: la storia `0014` lo aggiorna a ogni carico nella tabella
`costo_articolo`. Moltiplicarlo per la giacenza è aritmetica di dieci righe. **Il lavoro di questa storia non è il
calcolo: è il confine.** Il numero che produciamo somiglia moltissimo a un dato di bilancio e non lo è, e la
distanza fra le due cose è invisibile a chi legge. Il rischio di posizionamento più insidioso dell'applicazione
arriva proprio sotto forma di richiesta gentile — «me lo fai anche per il bilancio?» — e si respinge una volta
sola, qui, scrivendolo nell'interfaccia e non solo nella documentazione (descrizione dell'applicazione, §2.3, §10
e §11).

**Il confine, per esteso.**

- StockGrove **non produce le scritture ausiliarie di magazzino** previste dall'art. 14, primo comma, lettera d),
  del decreto del Presidente della Repubblica 600/1973. Quelle scritture sono obbligatorie solo per chi supera
  **contemporaneamente**, per due esercizi consecutivi, **5.164.568,99 € di ricavi** e **1.032.913,80 € di
  rimanenze**: soglie che il nostro cliente — da 1 a 50 addetti — non tocca. Il registro dei movimenti che
  costruiamo assomiglia a quello che la norma descrive, e proprio per questo non va spacciato per quello.
- La **valutazione delle rimanenze** ai fini del bilancio e della dichiarazione — la scelta fra i metodi ammessi
  dall'art. 92 del testo unico delle imposte sui redditi (ultimo costo, media ponderata, primo entrato-primo
  uscito), le svalutazioni, il raccordo con le scritture contabili — **è materia del commercialista** e resta
  fuori da questa applicazione.
- Il numero che produciamo risponde a «quanto capitale ho fermo su questo scaffale?». **Non compila un rigo di
  dichiarazione** e non è una base contabile: è una base di conversazione con chi la contabilità la tiene.

## 2. Requisiti funzionali

1. **RF-1** — Per ogni coppia articolo/deposito si calcola il **valore gestionale** come giacenza × costo medio
   ponderato mobile dell'articolo, con la valuta accanto; l'applicazione espone il totale per articolo, per
   deposito e per l'intera impresa.
2. **RF-2** — Il metodo di calcolo è **dichiarato in ogni schermata e in ogni esportazione**: «costo medio
   ponderato mobile, aggiornato a ogni carico». Non è configurabile: un menu con tre metodi suggerirebbe che si
   sta scegliendo un criterio contabile.
3. **RF-3** — Gli articoli **senza costo medio** (mai caricati con un costo, per esempio quelli entrati solo da un
   movimento di rettifica) sono contati a valore zero ed **elencati a parte**, con il loro numero e la loro
   quantità: un totale che nasconde venti articoli senza prezzo è un totale bugiardo.
4. **RF-4** — Ogni schermata e ogni file portano l'etichetta **«valore gestionale»** e un avviso breve e fisso, non
   nascosto in un aiuto contestuale: questo numero non è la valutazione delle rimanenze ai fini di bilancio o
   dichiarazione, che spetta a chi tiene la contabilità. La parola «rimanenze» **non compare mai** in senso fiscale
   in nessuna delle cinque lingue.
5. **RF-5** — L'esportazione produce un **file che l'utente scarica** (per foglio di calcolo) con articolo, codice,
   deposito, quantità, costo medio unitario, valore, valuta, metodo e momento del calcolo, più l'elenco degli
   articoli senza costo. Nessun invio a nessuno: il file lo consegna la persona.
6. **RF-6** — Il calcolo si può chiedere **a una data**: la giacenza a quella data si ricava dal registro dei
   movimenti, il costo medio è quello corrente dell'articolo. La differenza fra i due tempi è dichiarata nel file,
   perché è una semplificazione e va detta.
7. **RF-7** — Il valore è una **lettura**: non scrive nulla, non crea movimenti e non modifica il costo medio, che
   cambia solo per effetto dei carichi (storia `0014`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il calcolo e l'esportazione filtrano per `tenant_id` preso dal token
  verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato. Prova di
  isolamento fra due account sulla rotta del valore.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/magazzino/v1/valore` (con filtri per deposito,
  categoria e data, e raggruppamento) e `GET /api/magazzino/v1/valore/esportazione`; risposta paginata con totali;
  errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** **Nessuna migrazione nuova**: si leggono `giacenza`, `movimento` e `costo_articolo`,
  tutte già esistenti. Il valore non si materializza in una tabella: è un calcolo, e conservarlo creerebbe un
  secondo numero da tenere allineato — lo stesso errore che il modello dei movimenti esiste per evitare.
- **RT-4 — Modulo frontend (§3, §5).** Vista «valore gestionale» dentro la sezione `giacenze` del modulo
  `magazzino`: totale in testa con l'etichetta e l'avviso sempre visibili, ripartizione per deposito, elenco degli
  articoli senza costo, azione di esportazione. Solo token del sistema di design, colore-categoria `amber`;
  funziona in tema chiaro e scuro. Gli importi si mostrano nella valuta dell'account, in centesimi interi
  internamente.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe passano dallo spazio-nomi `magazzino` e sono presenti in
  `en, it, fr, es, de`. **Questa è la storia in cui la traduzione è un rischio di prodotto, non un dettaglio**: le
  cinque etichette e i cinque avvisi vanno scelti da chi conosce il termine contabile locale, perché una parola
  infelice — *inventory value* reso come *closing stock*, *Warenbestand* reso come *Vorratsbewertung* — ricrea
  esattamente la promessa che stiamo evitando. Fino a quella revisione, i testi restano marcati come da validare
  (descrizione, §11 punto 4).
- **RT-6 — Varchi e quota (§6, §7).** La lettura del valore e l'esportazione **non consumano quota e non vengono
  mai respinte con `429`**: il tetto `articoli_gestiti` (natura `stock`) colpisce solo la creazione di articoli
  nuovi. Valgono gli altri varchi: `401`, `402` con abbonamento `canceled`, `403` per ruolo insufficiente — con
  l'avvertenza di piattaforma che l'esportazione dei propri dati per i diritti dell'interessato resta accessibile
  in ogni caso, ed è cosa diversa da questa.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura. Un eventuale strumento di
  **lettura** `valore_gestionale(deposito?, data?) → totale e ripartizione` è ammissibile e resta fuori da questa
  storia: se verrà dichiarato nella storia `0034`, la sua descrizione in lingua naturale dovrà portare lo stesso
  avviso dell'interfaccia, perché un assistente che rispondesse «le tue rimanenze valgono 42.000 €» rifarebbe il
  danno in una frase. Il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: articoli, quantità, costi e depositi sono dati
  su cose. Il file esportato non contiene dati di persone; se in futuro vi si aggiungesse il fornitore, la voce
  andrebbe dichiarata nel manifesto perché il fornitore può essere una ditta individuale (descrizione, §6).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `valore calcolato` (con ambito e numero di righe) e
  `esportazione del valore scaricata` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di
  correlazione, **senza** importi: il valore del magazzino di un cliente non finisce in un registro tecnico.

## 4. Criteri di accettazione

**CA-1 — Valore per articolo, deposito e totale**
- **Dato** un articolo con giacenza 10 nel deposito centrale e costo medio 3,50 €, e un secondo articolo con
  giacenza 4 nel furgone e costo medio 12,00 €
- **Quando** si chiede il valore gestionale dell'impresa
- **Allora** il primo vale 35,00 €, il secondo 48,00 €, il totale è 83,00 €, e la ripartizione per deposito riporta
  35,00 € e 48,00 €

**CA-2 — Articoli senza costo elencati a parte**
- **Dato** tre articoli con giacenza positiva, di cui uno mai caricato con un costo
- **Quando** si chiede il valore
- **Allora** il totale considera i due valorizzati, e la risposta riporta esplicitamente «1 articolo senza costo
  medio, quantità 6», che non è sommato al totale e non è nascosto

**CA-3 — L'etichetta e l'avviso ci sono davvero**
- **Dato** la vista del valore e il file esportato
- **Quando** si leggono l'una e l'altro
- **Allora** entrambi portano l'etichetta «valore gestionale», il metodo «costo medio ponderato mobile» e l'avviso
  che non si tratta della valutazione delle rimanenze ai fini di bilancio o dichiarazione; la parola «rimanenze»
  non compare in senso fiscale in nessuna delle cinque lingue

**CA-4 — Valore a una data passata**
- **Dato** un articolo con giacenza 10 oggi e un carico di 4 registrato ieri
- **Quando** si chiede il valore alla data di due giorni fa
- **Allora** la quantità usata è 6, ricavata dal registro, il costo medio è quello corrente, e il file dichiara che
  quantità e costo si riferiscono a due momenti diversi

**CA-5 — Il calcolo non scrive nulla**
- **Dato** un magazzino con movimenti e costi medi assestati
- **Quando** si chiede il valore dieci volte di seguito e si esporta
- **Allora** nessun movimento è stato creato, nessun costo medio è cambiato e la `versione` di nessuna riga di
  giacenza si è mossa

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con articoli dallo stesso codice interno
- **Quando** un utente di `A` chiede il valore o l'esportazione
- **Allora** ottiene solo i propri articoli e i propri totali, anche forzando l'identificativo dell'altro account
  nella richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo del valore, sugli arrotondamenti in centesimi interi e sulla ricostruzione
      della quantità a una data, di **integrazione** sulla rotta del valore e su quella di esportazione;
- [ ] prova di **isolamento fra account** sul valore e sull'esportazione;
- [ ] prova che verifica la **presenza dell'etichetta e dell'avviso** nel file esportato e nella vista, in tutte e
      cinque le lingue: è un requisito di prodotto, quindi è una prova, non una raccomandazione;
- [ ] **prova end-to-end**: *rimando* — la vista del valore è una lettura; il percorso `[J-MAGAZZINO]` è di
      proprietà delle storie `0036` e `0037`, e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì le voci;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`) e **marcate come da validare** in
      attesa della revisione dei termini contabili locali;
- [ ] **manifesto dei dati**: nessun campo nuovo, e verificato che il file esportato non contenga dati di persone;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con il confine verso la contabilità e
      la scelta di non rendere configurabile il metodo;
- [ ] contratto degli **strumenti conversazionali**: nessuno strumento in questa storia, con la nota per la `0034`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata: il confine è scritto anche in
      [application-description.md](../application-description.md), §2.3 e §10, e deve restare coerente.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0014` | Il costo medio ponderato mobile nella tabella `costo_articolo` è il moltiplicatore: senza carichi con costo, non c'è valore |
| `0023` | Il valore ha senso dopo un conteggio: è il numero che si guarda a inventario chiuso, ed è il file che si consegna |
| `0013` | Il registro dei movimenti è la sorgente della quantità a una data passata |

## 7. Fuori ambito

- **La valutazione delle rimanenze ai fini di bilancio e dichiarazione**: è del commercialista, e non entra in
  questa applicazione né in nessun'altra del catalogo. È il fuori ambito più importante della scheda.
- **Le scritture ausiliarie di magazzino** dell'art. 14, primo comma, lettera d), del decreto del Presidente della
  Repubblica 600/1973: non le produciamo, né oggi né come funzione a pagamento.
- **La scelta del metodo di valorizzazione** (ultimo costo, primo entrato-primo uscito, svalutazioni): non
  configurabile, per costruzione.
- **L'invio del file al commercialista**: l'app non manda niente a nessuno fuori dall'azienda. Il file si scarica e
  lo consegna la persona; non si aggiunge nessun fornitore esterno che tratti dati per nostro conto
  (descrizione, §6).
- **Il valore delle differenze d'inventario** in denaro: sarebbe utile e va valutato dopo, perché mette insieme due
  numeri delicati (una differenza e un costo) proprio nel punto in cui stiamo tracciando un confine.
- **Il costo medio a una data passata**: qui si usa quello corrente. Ricostruirlo storicamente richiede di
  conservare la serie dei costi ed è una storia a sé.

## 8. Punti aperti

- **Come si chiama nelle cinque lingue.** È il punto aperto numero 4 della descrizione dell'applicazione e non lo
  chiude un agente: serve una revisione dei testi con qualcuno che sappia come suona il termine in francese,
  spagnolo e tedesco. Fino ad allora le traduzioni restano marcate da validare.
- **Il costo medio a una data**: la semplificazione «quantità storica × costo corrente» è dichiarata nel file, ma se
  un commercialista la usasse davvero come base di partenza potrebbe essere fuorviante su magazzini con costi molto
  variabili. Se il caso d'uso «file di fine anno» diventasse importante, la serie storica dei costi va conservata:
  decisione di prodotto, con un costo di persistenza da valutare.
- **Se mostrare il valore anche nella vista principale delle giacenze**, accanto alle quantità. Utile per il
  titolare, rischioso perché normalizza un numero che vogliamo tenere qualificato. La proposta è: no, resta una
  vista propria con la sua etichetta.
