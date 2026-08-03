# 0027 — Avviso di sotto scorta

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 05 — Scorte minime e riordino
**Storia**: `0027` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0026`, `0013`, `0015`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare o addetto al magazzino
> voglio sapere che cosa è sceso sotto la soglia che ho impostato, e saperlo una volta sola per volta che accade
> così da poter rimediare prima della rottura di scorta senza dover sfogliare tutti i giorni un elenco di cose
> che già sapevo.

**Contesto.** Le soglie esistono (`0026`) ma nessuno le guarda. Questa storia le confronta con la giacenza
corrente e produce due cose: un **elenco di ciò che è sotto soglia**, sempre consultabile, e un **riepilogo
periodico** che arriva senza doverlo chiedere. Il punto delicato non è il calcolo — è banale — ma la disciplina
degli avvisi. Le fonti sul segmento sono concordi su due difetti che fanno abbandonare questa funzione: l'avviso
che si ripete ogni giorno finché il problema non è risolto, e l'avviso che non porta a nulla di operativo
(descrizione dell'applicazione, §2.5; guida agli avvisi di scorta bassa, §2.6 fonte 9). Il primo si affronta
qui, con la regola del «niente molestie»; il secondo nella storia `0028`, che trasforma l'elenco in una lista
della spesa — e per questo `0027` e `0028` vanno lette come una coppia, anche se si implementano una per volta.

## 2. Requisiti funzionali

1. **RF-1** — Una coppia articolo e deposito è **sotto scorta** quando ha una regola con `scorta_minima`
   valorizzata e la sua giacenza corrente è minore o uguale a quella soglia. Le coppie senza regola non sono mai
   sotto scorta (`0026`, RF-2).
2. **RF-2** — L'elenco delle coppie sotto scorta è consultabile in qualunque momento, ordinato per gravità
   (quanto manca alla soglia, in proporzione), filtrabile per deposito, categoria e fornitore preferito, con
   giacenza corrente, soglia e scarto su ogni riga.
3. **RF-3** — L'attraversamento della soglia genera un **avviso** registrato: quando una coppia passa da sopra a
   sotto soglia nasce un avviso `aperto`; quando risale sopra la soglia l'avviso si chiude da solo, indicando
   quando e per effetto di quale movimento.
4. **RF-4** — **Niente molestie.** Finché un avviso resta aperto non se ne genera un altro per la stessa coppia.
   Si notifica di nuovo in due soli casi: la giacenza **peggiora** scendendo sotto una soglia di gravità
   ulteriore (per esempio da «sotto scorta minima» a «esaurito», giacenza zero), oppure l'avviso si è chiuso e la
   coppia riscende sotto soglia più tardi.
5. **RF-5** — Il **riepilogo periodico** è una sola comunicazione con tutte le coppie sotto scorta e le novità dal
   riepilogo precedente; la cadenza è scelta dal cliente fra `giornaliera`, `settimanale` (con giorno) e
   `nessuna`, e il valore predefinito è `settimanale`. Se non ci sono novità e nessuna coppia è sotto scorta, il
   riepilogo **non viene inviato**: un messaggio che dice «va tutto bene» ogni settimana è la prima cosa che
   qualcuno filtra.
6. **RF-6** — La sezione `riordino` del modulo mostra il conteggio delle coppie sotto scorta come indicatore
   sempre visibile, e da lì si arriva all'elenco in un tocco.
7. **RF-7** — Da una riga dell'elenco si vede lo storico dei movimenti di quell'articolo in quel deposito
   (`0013`), perché la prima domanda davanti a un articolo sotto scorta è «dove è finito?».

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il calcolo delle coppie sotto scorta, gli avvisi e i riepiloghi filtrano
  per `tenant_id` preso dal token verificato; il lavoro programmato che produce i riepiloghi itera per account e
  non esegue mai una interrogazione che attraversi più di un account. Prova di isolamento fra due account su
  `avviso_scorta` e sull'elenco.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/magazzino/v1/avvisi-scorta` (elenco delle coppie
  sotto scorta, con i filtri di RF-2), `GET /api/magazzino/v1/avvisi-scorta/storico` e
  `GET|PUT /api/magazzino/v1/avvisi-scorta/impostazioni` per la cadenza del riepilogo; errori in
  `application/problem+json`; paginazione a pagina e dimensione con totale; definizione OpenAPI aggiornata nello
  stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V17__avviso_scorta.sql` sullo schema `app_magazzino`: tabella
  `avviso_scorta` con `tenant_id`, chiave primaria UUID versione 7, `articolo_id`, `deposito_id`, `livello`
  (`sotto_scorta`, `esaurito`), `stato` (`aperto`, `chiuso`), `giacenza_alla_apertura`, `soglia_alla_apertura`,
  `movimento_scatenante_id`, `chiuso_il`, colonne di controllo e `deleted_at`; tabella
  `impostazione_avvisi_scorta` con la cadenza per account. Indice su `(tenant_id, stato, articolo_id,
  deposito_id)`. Nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** L'elenco vive nella sezione `riordino` del modulo `magazzino`; il conteggio
  compare come indicatore nella sezione `giacenze`; la cadenza del riepilogo si imposta nella sezione
  `impostazioni`. Dati letti con il client generato; solo token del sistema di design, con il colore-categoria
  `amber` per lo stato di attenzione; funziona in tema chiaro e scuro; l'elenco è leggibile da telefono, perché è
  la schermata che si guarda davanti allo scaffale.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili e i testi del riepilogo periodico passano dallo
  spazio-nomi `magazzino` e sono presenti in `en, it, fr, es, de`; i livelli `sotto_scorta` ed `esaurito` sono
  chiavi tradotte, non testo scritto nel codice.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: la metrica `articoli_gestiti` (natura `stock`)
  conta gli articoli attivi e la tocca solo la loro creazione. Consultare l'elenco, ricevere un riepilogo o
  cambiare la cadenza **non viene mai respinto con `429`**. Restano i varchi precedenti: `402` con abbonamento non
  attivo — e con abbonamento in `past_due` la funzione resta accessibile (§13 dei principi) — `403` per ruolo
  insufficiente sulla modifica della cadenza, riservata a `owner` e `admin`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento dichiarato qui: `elenca_sotto_scorta(deposito?)`
  è dichiarato dalla storia `0034` e leggerà esattamente l'elenco di RF-2. È uno strumento di **lettura**, quindi
  libero e senza conferma. Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non
  ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. L'elenco può mostrare il **fornitore preferito**
  della regola, la cui anagrafica è già dichiarata nel manifesto `docs/compliance/manifests/magazzino.yaml` dalla
  storia `0010`; la tabella `avviso_scorta` non contiene dati di persone oltre alle colonne di controllo e va
  comunque aggiunta a `exportData` e `purgeData` del contratto `MagazzinoDataContract` insieme alle altre.
  Il riepilogo periodico è indirizzato a utenti dell'account e non contiene dati di terzi.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `avviso di scorta aperto`, `avviso di scorta aggravato`,
  `avviso di scorta chiuso` e `riepilogo periodico prodotto` (con il numero di righe e la cadenza) sono registrati
  con `tenant_id`, `app_id`, `user_id` — o l'indicazione del lavoro programmato quando non c'è un utente — e
  identificativo di correlazione, senza descrizioni di articoli e senza ragioni sociali.

## 4. Criteri di accettazione

**CA-1 — Un articolo scende sotto soglia**
- **Dato** l'articolo `RIC-014` nel deposito `MAG` con soglia 5 e giacenza 8
- **Quando** viene registrato uno scarico di 4 pezzi
- **Allora** la giacenza è 4, la coppia compare nell'elenco delle coppie sotto scorta con scarto 1, ed esiste un
  avviso `aperto` di livello `sotto_scorta` che riferisce il movimento che l'ha scatenato

**CA-2 — Niente molestie**
- **Dato** la coppia dello scenario precedente con un avviso già aperto
- **Quando** vengono registrati altri scarichi che la portano da 4 a 3 e poi a 2, senza mai raggiungere lo zero
- **Allora** non nasce nessun avviso nuovo, l'avviso esistente resta uno solo e il riepilogo periodico riporta la
  coppia una volta sola

**CA-3 — Aggravamento e rientro**
- **Dato** la stessa coppia con giacenza 2 e un avviso aperto di livello `sotto_scorta`
- **Quando** uno scarico porta la giacenza a 0, e più tardi un carico la riporta a 12
- **Allora** all'esaurimento nasce un avviso di livello `esaurito`, e al carico entrambi gli avvisi si chiudono con
  la data e il movimento che li ha chiusi; la coppia sparisce dall'elenco

**CA-4 — Riepilogo silenzioso**
- **Dato** un account con cadenza `settimanale` e nessuna coppia sotto scorta, né novità dal riepilogo precedente
- **Quando** scatta il momento del riepilogo
- **Allora** nessuna comunicazione viene prodotta, e l'evento registrato dice che il riepilogo è stato saltato
  perché vuoto

**CA-5 — Coppia non sorvegliata**
- **Dato** un articolo con giacenza 0 in un deposito e **nessuna** regola di scorta per quella coppia
- **Quando** si consulta l'elenco delle coppie sotto scorta
- **Allora** la coppia non compare e nessun avviso viene generato, oggi né mai, finché non si imposta una regola

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, entrambi con articoli sotto scorta
- **Quando** un utente di `A` chiede l'elenco o lo storico degli avvisi
- **Allora** vede solo i propri, anche forzando l'identificativo dell'altro account nella richiesta, e il riepilogo
  periodico di `A` non contiene mai righe di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, frontend e compliance; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla regola del «niente molestie» (apertura, aggravamento, chiusura, riapertura) e di
      **integrazione** sulla risorsa `avvisi-scorta` e sul lavoro programmato del riepilogo, con database effimero
      e migrazioni vere;
- [ ] prova di **isolamento fra account** su `avviso_scorta` e sul riepilogo periodico;
- [ ] **prova end-to-end**: *rimando* — la catena soglia → avviso → proposta è coperta dal percorso
      `[J-MAGAZZINO]` esteso dalla storia `0028`, proprietaria della voce nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), compresi i testi del riepilogo
      periodico;
- [ ] **manifesto dei dati** invariato quanto ai campi, con `avviso_scorta` e `impostazione_avvisi_scorta`
      presenti in esportazione e cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con la regola del «niente molestie»
      e la scelta di non inviare riepiloghi vuoti;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione esposta qui, `elenca_sotto_scorta` è della
      storia `0034`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0026` — soglie di scorta | senza una soglia impostata non esiste il concetto di «sotto scorta» |
| `0013` — registro dei movimenti e giacenza derivata | l'avviso si valuta sulla giacenza corrente e cita il movimento che lo ha scatenato |
| `0015` — scarico della merce | è il movimento che nella pratica fa scendere sotto soglia, ed è il punto in cui si valuta l'attraversamento |
| meccanismo comune di comunicazione della piattaforma | il riepilogo periodico usa il canale già in uso per le altre app; se non fosse disponibile resta consultabile in applicazione — vedi punti aperti |

## 7. Fuori ambito

- **La lista della spesa raggruppata per fornitore**: storia `0028`. Questa storia si ferma all'elenco e
  all'avviso; senza la `0028` la funzione è ancora quella che le fonti chiamano «un avviso che non porta a
  niente», ed è una carenza dichiarata, non una svista.
- **Il suggerimento che una soglia è invecchiata**: storia `0029`.
- **Le notifiche immediate a ogni attraversamento** (messaggio istantaneo, notifica sul telefono): fuori perimetro.
  La cadenza periodica è deliberata: un magazzino attivo attraversa soglie tutto il giorno.
- **La previsione di quando l'articolo finirà**: è la copertura in giorni della storia `0029`.

## 8. Punti aperti

- **Canale del riepilogo periodico** — posta elettronica, notifica in applicazione o entrambe: dipende da quale
  meccanismo comune la piattaforma mette a disposizione. Va verificato al momento dello scaffolding; se non
  esistesse nulla di comune, la proposta è di limitarsi alla notifica in applicazione e di **non** costruire un
  invio di posta dedicato per questa sola app.
- **A chi arriva il riepilogo**: la proposta è «a tutti gli utenti dell'account con ruolo `owner` o `admin`»,
  perché è chi decide gli acquisti. La preferenza per singola persona è una raffinatezza da confermare.
- **Soglia di gravità ulteriore diversa da zero** — oggi l'aggravamento scatta all'esaurimento; un cliente
  potrebbe volere «metà della soglia». Non l'ho trovato discusso in nessuna fonte (§2.7) e non lo invento.
