# 0020 — Pacchetti di sedute prepagate

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 05 — Conto, pacchetti e fedeltà
**Storia**: `0020` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come estetista che vende un ciclo di dieci sedute
> voglio registrare il pacchetto e vedere le sedute scalarsi da sole man mano che la cliente viene
> così da sapere sempre quante ne restano, senza il quadernetto con le crocette che si perde e su cui si discute.

**Contesto.** Il pacchetto prepagato è una funzione centrale del beauty e ha una particolarità che nessun'altra
funzione dell'applicazione ha: **è normata**. Il decreto legislativo 141/2018, che recepisce la direttiva UE
2016/1065, distingue il **buono monouso** — quando al momento della vendita si conoscono già natura della
prestazione, aliquota e luogo — dal **buono multiuso**, quando non si conoscono; il primo è rilevante
all'emissione, il secondo all'utilizzo (§2.3, punto 4 della descrizione). «Dieci sedute di pressoterapia» e
«200 € di credito da spendere come vuoi» **non sono la stessa entità**, e il programma deve saperlo: non per
calcolare imposte — non le calcola — ma perché è ciò che a valle decide quando l'operazione conta.

## 2. Requisiti funzionali

1. **RF-1** — Si vende un pacchetto a un cliente scegliendo fra due **specie**: **a sedute determinate** (n sedute
   di un servizio o di un gruppo di servizi noti) oppure **a valore** (un credito spendibile su servizi diversi).
   La specie è obbligatoria e non si cambia dopo.
2. **RF-2** — Il pacchetto registra prezzo pagato, data di vendita, sedute o valore totale e residuo, scadenza
   facoltativa, e stato (`venduto`, `in uso`, `esaurito`, `scaduto`, `annullato`).
3. **RF-3** — Alla chiusura di un conto, se il cliente ha un pacchetto compatibile, il programma **propone** di
   scalarlo mostrando quante sedute o quanto valore restano dopo: propone, non decide.
4. **RF-4** — Ogni utilizzo lascia un **movimento immutabile** che cita il conto: chi contesta le sedute residue
   deve poter vedere quando ognuna è stata usata.
5. **RF-5** — Un pacchetto si annulla **solo** se nessuna seduta è stata usata; se ne è stata usata almeno una, la
   sola via è una rettifica sul conto, che resta visibile.
6. **RF-6** — La scadenza fa passare il pacchetto a `scaduto` e lo segnala **prima**, non dopo: un avviso quando
   manca poco, sulla scheda del cliente e nell'elenco.
7. **RF-7** — L'elenco dei pacchetti si legge per cliente e per stato, con il valore residuo complessivo: è
   l'importo che il salone deve ancora erogare, e il titolare ha diritto di conoscerlo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Pacchetti e utilizzi filtrano per `tenant_id` dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST /api/<app>/v1/pacchetti`,
  `GET /api/<app>/v1/clienti/{id}/pacchetti`, `POST /api/<app>/v1/pacchetti/{id}/utilizzi` (invocata dalla
  chiusura del conto), `POST /api/<app>/v1/pacchetti/{id}/annullamento`; corpo validato (specie obbligatoria,
  sedute o valore positivi); errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Tabelle `pacchetto` e `utilizzo_pacchetto` con `tenant_id`, UUID versione 7,
  colonne di controllo e cancellazione logica sul pacchetto; **`utilizzo_pacchetto` è immutabile** (storia
  `0002`). Importi in **centesimi interi**; la specie è un valore chiuso, non testo libero.
- **RT-4 — Atomicità con la chiusura del conto (storia `0019`).** La decurtazione è dentro la stessa transazione:
  un conto chiuso senza decurtazione è una seduta regalata che nessuno ritroverà.
- **RT-5 — Concorrenza.** Due chiusure simultanee che scalano lo stesso pacchetto **non possono** portarlo sotto
  zero: il vincolo sta nel database.
- **RT-6 — Varchi e quota (§6, §7).** Funzione accesa dal piano; `402` a piano insufficiente. Il pacchetto **non
  consuma** la metrica `postazioni`.
- **RT-7 — Modulo frontend (§3, §5).** La vendita chiede la specie con due opzioni **spiegate in parole semplici**
  («sedute di un trattamento preciso» / «credito da spendere come vuole»), perché è la scelta che il salone
  sbaglierà se gliela si presenta con i termini della norma. Solo token del sistema di design.
- **RT-8 — Cinque lingue (§4).** Nomi delle specie, stati, avvisi di scadenza in `en, it, fr, es, de`. ⚠️ La
  distinzione fra le due specie è giuridica e **la traduzione va verificata**, non calcata sull'italiano.
- **RT-9 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese: `pacchetto.cliente` (economico,
  finalità «sapere quante sedute restano e a chi», base «esecuzione del contratto», durata «fino a esaurimento o
  scadenza, poi 24 mesi»). Campi annotati; tabelle in esportazione e cancellazione — **con il caso scomodo del
  pacchetto non consumato descritto alla storia `0014`**.
- **RT-10 — Esposizione conversazionale (§12).** `stato_pacchetti(cliente?) → pacchetti aperti con residuo e
  scadenza` in lettura. La decurtazione **non ha uno strumento proprio**: avviene dentro `chiudi_conto`, che è già
  scrittura irreversibile con conferma obbligatoria.
- **RT-11 — Registrazione eventi (§14).** `pacchetto venduto`, `seduta scalata`, `pacchetto scaduto` con
  `tenant_id`, `app_id`, `user_id`, correlazione e specie — mai il nome del cliente.

## 4. Criteri di accettazione

**CA-1 — Vendita e specie**
- **Dato** il modulo di vendita di un pacchetto
- **Quando** si vendono dieci sedute di pressoterapia
- **Allora** il pacchetto nasce di specie «a sedute determinate», con residuo dieci, e la specie non è più
  modificabile

**CA-2 — La decurtazione è proposta, non imposta**
- **Dato** una cliente con un pacchetto compatibile e un conto aperto per quel trattamento
- **Quando** si chiude il conto
- **Allora** il programma propone di scalare una seduta mostrando «restano 9», e si può rifiutare pagando in
  contanti

**CA-3 — Ogni utilizzo lascia traccia**
- **Dato** un pacchetto con tre sedute usate
- **Quando** si apre la sua scheda
- **Allora** ci sono tre movimenti, ciascuno con data e riferimento al conto

**CA-4 — Non si scende sotto zero**
- **Dato** un pacchetto con una sola seduta residua
- **Quando** due conti tentano di scalarla nello stesso istante
- **Allora** uno solo riesce e l'altro riceve un errore chiaro; il residuo è zero, mai meno uno

**CA-5 — Annullamento solo se intatto**
- **Dato** un pacchetto con due sedute usate
- **Quando** si tenta di annullarlo
- **Allora** l'operazione è rifiutata e il programma indica la via della rettifica

**CA-6 — La scadenza si annuncia**
- **Dato** un pacchetto che scade fra dieci giorni
- **Quando** si apre la scheda della cliente o l'elenco dei pacchetti
- **Allora** l'avviso è visibile prima della scadenza, non dopo

**CA-7 — Isolamento fra account**
- **Dato** due account con pacchetti
- **Quando** un utente del primo tenta di scalare un pacchetto dell'altro
- **Allora** la richiesta è respinta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (suite intera prima del commit);
- [ ] prove di **unità** sulle transizioni di stato e sul residuo; di **integrazione** su concorrenza e atomicità
      con la chiusura del conto;
- [ ] prova di **isolamento fra account** su vendita, lettura e utilizzo;
- [ ] **prova end-to-end**: *coprire ora* — è il cuore del percorso `[J-SALONGROVE-PKG]` (storia `0031`); registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** in tutte e cinque le lingue, con la distinzione fra le specie verificata e non calcata;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per il pacchetto e i suoi utilizzi;
- [ ] **registro delle decisioni**: le due specie e la loro ragione normativa (con la fonte), specie immutabile,
      decurtazione proposta e non imposta, annullamento solo se intatto, nessun calcolo di imposta;
- [ ] avvio locale invariato; il salone di prova ha un pacchetto di ciascuna specie.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` | la decurtazione avviene alla chiusura del conto |

## 7. Fuori ambito

- **il calcolo dell'imposta**: SalonGrove registra la specie e i fatti, non calcola niente. L'imposta è delle app
  01 e 02, che ricevono l'evento;
- il rimborso di un pacchetto non consumato: è un movimento di denaro, e appgrove non ne muove;
- il diritto di ripensamento su un pacchetto **venduto a distanza**: un pacchetto venduto in salone non è un
  contratto a distanza, ma uno venduto da un collegamento sì. **Non ho approfondito** questo caso e non lo
  invento: è un punto aperto qui sotto;
- i pacchetti trasferibili ad altre persone (regalo): fuori da questa stesura.

## 8. Punti aperti

**Il pacchetto venduto a distanza.** Se un giorno un pacchetto si potesse comprare dalla pagina pubblica, si
entrerebbe nella disciplina dei contratti a distanza, con il ripensamento e le sue eccezioni. BookGrove ha già
esaminato il tema per gli appuntamenti a data fissa (§2.3 di quell'app) e ha trovato un'esclusione che **non è
detto valga per un pacchetto**, che non ha una data. Non lo decido e non lo assumo: in questa stesura il pacchetto
si vende **solo in salone**, ed è una limitazione voluta.

**La scadenza è ammessa dovunque?** Un pacchetto con scadenza è, dal punto di vista del cliente, denaro che può
evaporare. Non ho verificato se e come i diversi ordinamenti europei limitino la durata minima dei buoni. È un
punto per la revisione legale, e nel frattempo la scadenza resta **facoltativa** e va scritta dal salone, mai
proposta da noi.
