# 0029 — Consumi e giorni di copertura

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 05 — Scorte minime e riordino
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0015`, `0026`, `0027`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha impostato le soglie mesi fa e non le ha più toccate
> voglio sapere quanto consumo davvero di ogni articolo e per quanti giorni mi basta quello che ho
> così da accorgermi che una soglia è diventata sbagliata prima che sia la rottura di scorta a dirmelo.

**Contesto.** Le storie `0026`-`0028` funzionano solo se le soglie sono giuste, e le soglie invecchiano: quella
impostata un anno fa descrive le vendite di un anno fa, non quelle di oggi. È il rilievo più insistito delle fonti
sugli avvisi di scorta bassa (descrizione dell'applicazione, §2.5; guida agli avvisi di scorta bassa, §2.6 fonte
9). Questa storia chiude l'epica dando all'applicazione il solo dato che permette di accorgersene: il **consumo
medio** ricavato dai movimenti che già esistono, e da lì i **giorni di copertura** e la segnalazione delle soglie
diventate incoerenti con il consumo.

Il registro dei movimenti c'è già ed è vero per costruzione (`0013`): questa storia non aggiunge fatti, li
**legge**. Due limiti stanno scritti nel prodotto e non si negoziano. Primo: **non si misurano le persone**. Il
registro sa chi ha fatto ogni movimento, ma qui si contano gli articoli, mai gli operatori — niente «movimenti per
addetto», niente classifiche: in Italia gli strumenti da cui può derivare un controllo a distanza dell'attività
ricadono nell'articolo 4 della legge 300/1970 e il dato dell'autore serve alla tracciabilità della merce, non alla
sorveglianza di chi la muove (§6 della descrizione). Secondo: **non si confronta il cliente con medie di settore**.
Le cifre in circolazione sulle rotture di scorta nel segmento micro sono citate da fornitori senza indagine
indipendente (§2.7): gli indicatori mostrano il dato del cliente e basta.

## 2. Requisiti funzionali

1. **RF-1** — Per ogni coppia articolo e deposito si calcola il **consumo medio giornaliero** sui soli movimenti
   di **scarico** (compresi quelli generati dagli eventi delle altre app, `0019`) in una finestra scelta fra 30,
   90 e 365 giorni; carichi, trasferimenti, rettifiche e storni non entrano nel consumo, e il motivo è scritto in
   interfaccia.
2. **RF-2** — Dai consumi si ricavano i **giorni di copertura**: giacenza corrente diviso consumo medio
   giornaliero, arrotondato per difetto. Con consumo nullo la copertura non è un numero: è «nessun consumo nel
   periodo», e non si scrive «infinito».
3. **RF-3** — Un articolo con **poca storia** (meno movimenti di scarico di una soglia minima, o registrato da
   meno tempo della finestra) è marcato «dato non significativo» e non produce segnalazioni: una media costruita
   su due scarichi è rumore travestito da informazione.
4. **RF-4** — L'applicazione segnala le **soglie diventate sbagliate** confrontando la scorta minima con il
   consumo del periodo, in due direzioni: soglia **troppo bassa** (copre meno giorni di quanti ne servono per
   riapprovvigionarsi, e l'articolo rischia di finire prima) e soglia **troppo alta** (giacenza ferma molto sopra
   la soglia con consumo quasi nullo, cioè denaro immobilizzato). Ogni segnalazione dice il valore attuale, quello
   che i consumi suggerirebbero e su quale periodo è stato calcolato.
5. **RF-5** — Da una segnalazione si arriva in un tocco alla modifica della regola di scorta (`0026`);
   l'applicazione **non modifica mai una soglia da sola**: propone, la persona decide.
6. **RF-6** — Al livello dell'account si pubblicano due indicatori di sintesi: la **rotazione** (quanto del
   magazzino si muove in un periodo) e il **capitale immobilizzato** negli articoli senza consumo, quest'ultimo
   espresso con il valore gestionale della storia `0025` e con la sua stessa etichetta.
7. **RF-7** — Gli indicatori sono esposti come evento per le app a valle — InsightGrove (20) — nella stessa forma
   asincrona dell'evento `giacenza.variata` (`0020`), e **non contengono alcun dato riferito a una persona**.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il calcolo dei consumi, le segnalazioni e gli indicatori filtrano per
  `tenant_id` preso dal token verificato; il lavoro programmato che aggiorna i consumi itera per account e non
  esegue mai una interrogazione che attraversi più di un account; l'evento pubblicato porta il proprio
  `tenant_id`. Prova di isolamento fra due account su `consumo_articolo` e sulle rotte di lettura.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/magazzino/v1/consumi` (elenco per articolo e
  deposito, con la finestra come parametro), `GET /api/magazzino/v1/consumi/segnalazioni` (le soglie da rivedere)
  e `GET /api/magazzino/v1/consumi/indicatori` (rotazione e capitale immobilizzato dell'account); sole letture,
  nessuna scrittura esposta al cliente; errori in `application/problem+json`; paginazione a pagina e dimensione
  con totale; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V19__consumo_articolo.sql` sullo schema `app_magazzino`: tabella
  `consumo_articolo` con `tenant_id`, chiave primaria UUID versione 7, `articolo_id`, `deposito_id`, `finestra`,
  `consumo_medio_giornaliero`, `movimenti_considerati`, `significativo`, `calcolato_il`, colonne di controllo e
  `deleted_at`. È una **proiezione ricalcolabile**: si può cancellare e ricostruire interamente dal registro dei
  movimenti, che resta l'unica autorità — la stessa disciplina della giacenza (`0013`, `0024`). Nessuna chiave
  esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** I consumi e la copertura compaiono nella scheda dell'articolo e come
  colonne ordinabili nella sezione `giacenze`; le segnalazioni delle soglie da rivedere stanno nella sezione
  `riordino`, accanto all'elenco delle coppie sotto scorta; gli indicatori di sintesi nella sezione `giacenze`.
  Dati letti con il client generato; solo token del sistema di design; funziona in tema chiaro e scuro. Nessuna
  schermata, nessun filtro e nessun ordinamento per **persona**.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `magazzino` e sono presenti
  in `en, it, fr, es, de`, comprese «dato non significativo», «nessun consumo nel periodo» e l'etichetta del
  valore gestionale, che va tradotta con la stessa cura richiesta dalla storia `0025`: una traduzione infelice
  ricrea la promessa di valutazione fiscale che l'applicazione evita.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: la metrica `articoli_gestiti` (natura `stock`)
  conta gli articoli attivi e la tocca solo la loro creazione. Consultare consumi, copertura, segnalazioni o
  indicatori **non viene mai respinto con `429`**. Restano i varchi precedenti: `402` con abbonamento non attivo,
  `403` per ruolo insufficiente.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo dichiarato qui: gli strumenti di lettura
  della storia `0034` possono arricchire la risposta di `elenca_sotto_scorta` con i giorni di copertura, ed è lì
  che il contratto si scrive. Sono strumenti di **lettura**, quindi liberi. Il contratto vive dentro il servizio;
  il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo, e un divieto esplicito da mettere per iscritto:
  il calcolo **ignora** la colonna `created_by` dei movimenti e nessuna aggregazione è per utente. La tabella
  `consumo_articolo` non contiene dati di persone; va comunque aggiunta a `exportData` e `purgeData` del contratto
  `MagazzinoDataContract` per completezza. L'evento pubblicato verso le app a valle contiene solo quantità e
  identificativi di articoli e depositi.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `consumi ricalcolati` (con il numero di coppie interessate e
  la finestra), `segnalazione di soglia da rivedere prodotta` e `indicatori pubblicati` sono registrati con
  `tenant_id`, `app_id`, `user_id` — o l'indicazione del lavoro programmato quando non c'è un utente — e
  identificativo di correlazione, senza descrizioni di articoli.

## 4. Criteri di accettazione

**CA-1 — Consumo e copertura**
- **Dato** l'articolo `RIC-014` nel deposito `MAG` con 90 pezzi scaricati in 90 giorni e giacenza corrente 12
- **Quando** si consultano i consumi con finestra 90 giorni
- **Allora** il consumo medio giornaliero è 1 e i giorni di copertura sono 12

**CA-2 — Solo gli scarichi contano**
- **Dato** lo stesso articolo, a cui nel periodo si aggiungono un carico di 100, un trasferimento di 20 verso un
  altro deposito e una rettifica di −5
- **Quando** si ricalcolano i consumi
- **Allora** il consumo medio resta 1: carico, trasferimento e rettifica non entrano nel calcolo, e l'interfaccia
  dichiara che il consumo è misurato sui soli scarichi

**CA-3 — Dato non significativo e consumo nullo**
- **Dato** un articolo registrato dieci giorni fa con due soli scarichi, e un secondo articolo con giacenza 40 e
  nessuno scarico nel periodo
- **Quando** si consultano i consumi con finestra 90 giorni
- **Allora** il primo è marcato «dato non significativo» e non produce segnalazioni; il secondo mostra «nessun
  consumo nel periodo» invece di una copertura infinita, e concorre al capitale immobilizzato

**CA-4 — Soglia diventata sbagliata**
- **Dato** un articolo con scorta minima 5 impostata mesi fa e un consumo medio salito a 3 pezzi al giorno
- **Quando** si consultano le segnalazioni
- **Allora** compare una segnalazione di soglia troppo bassa, con il valore attuale, quello suggerito dai consumi e
  la finestra usata; la regola **non** è stata modificata, e dalla segnalazione si arriva alla schermata di
  modifica

**CA-5 — Nessun indicatore per persona**
- **Dato** un account con movimenti registrati da tre utenti diversi
- **Quando** si esplorano le rotte dei consumi, le schermate e l'evento pubblicato per le app a valle
- **Allora** non esiste alcun modo di ottenere un conteggio, un filtro, un ordinamento o una classifica per
  utente: nessuna risposta contiene l'identificativo di chi ha registrato i movimenti

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con articoli omonimi e consumi diversi
- **Quando** un utente di `A` consulta consumi, segnalazioni e indicatori
- **Allora** ottiene solo i dati del proprio account, anche forzando l'identificativo dell'altro nella richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, frontend e compliance; l'intera suite prima del
      commit);
- [ ] prove di **unità** sul calcolo del consumo medio (esclusione di carichi, trasferimenti, rettifiche e
      storni), sulla soglia di significatività e sulle due direzioni della segnalazione, e di **integrazione**
      sulle rotte dei consumi e sul ricalcolo, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su `consumo_articolo`, sulle rotte di lettura e sull'evento pubblicato;
- [ ] **prova** che nessuna risposta e nessun evento contenga l'identificativo dell'autore dei movimenti: è un
      requisito di prodotto, quindi è un test, non una raccomandazione;
- [ ] **prova end-to-end**: *rimando* — la catena delle scorte è coperta dal percorso `[J-MAGAZZINO]` esteso dalla
      storia `0028`, proprietaria della voce nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml); qui non si aggiunge superficie
      nuova al percorso;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), con l'etichetta del valore
      gestionale rivista lingua per lingua;
- [ ] **manifesto dei dati** invariato quanto ai campi, con `consumo_articolo` presente in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con il divieto di aggregare per
      persona e il rifiuto dei confronti con medie di settore;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione nuova esposta qui, l'arricchimento è della
      storia `0034`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0013` — registro dei movimenti e giacenza derivata | il consumo si legge dal registro, che resta l'unica autorità |
| `0015` — scarico della merce | è il movimento su cui il consumo si misura |
| `0026` — soglie di scorta | senza soglia non esiste una soglia da segnalare come sbagliata |
| `0027` — avviso di sotto scorta | le segnalazioni vivono accanto all'elenco delle coppie sotto scorta |
| `0025` — valore gestionale delle giacenze | il capitale immobilizzato usa quel valore e la sua stessa etichetta |
| `0020` — evento «giacenza variata» | gli indicatori per InsightGrove (20) escono con lo stesso meccanismo a eventi |

## 7. Fuori ambito

- **La modifica automatica delle soglie**: l'applicazione propone, non decide. Un programma che cambia da solo le
  soglie toglie all'imprenditore l'unica leva che ha su questa funzione.
- **La previsione della domanda** (stagionalità, tendenza, modelli statistici): fuori perimetro. Il consumo medio
  su una finestra è una media, ed è dichiarata come tale.
- **Il tempo di consegna del fornitore** come componente del calcolo: non è nel modello di dominio (§4 della
  descrizione); finché non esiste, la segnalazione di soglia troppo bassa usa un orizzonte dichiarato in
  interfaccia, non un dato del fornitore.
- **L'analisi trasversale fra app** (rotazione confrontata con le vendite, marginalità): è di InsightGrove (20).
  StockGrove pubblica i propri indicatori e si ferma lì.
- **Qualunque misura sull'attività delle persone**: non è un rimando, è un divieto di prodotto.

## 8. Punti aperti

- **Quale orizzonte usare per giudicare «soglia troppo bassa»** in assenza del tempo di consegna del fornitore:
  la proposta è un valore predefinito di sette giorni, modificabile per account nelle impostazioni. È una scelta
  di prodotto e va confermata dallo sviluppatore.
- **La soglia di significatività** (quanti scarichi servono perché la media valga qualcosa): la proposta è almeno
  cinque scarichi e una storia lunga almeno quanto la finestra. Non l'ho trovata illuminata da nessuna fonte
  (§2.7) ed è una convenzione, dichiarata come tale.
- **Frequenza del ricalcolo** — una volta al giorno o su richiesta: dipende dal costo del calcolo su magazzini
  grandi, che non è stato misurato. Proposta: una volta al giorno, con ricalcolo su richiesta per un singolo
  articolo.
