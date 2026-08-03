# 0015 — Anagrafica prodotti e depositi

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 04 — Prodotti, consumi e magazzino
**Storia**: `0015` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un salone
> voglio un elenco dei prodotti che compro, distinguendo quelli che uso in cabina da quelli che rivendo
> così da poter sapere, più avanti, quanto mi costa un colore e quanto guadagno su uno shampoo.

**Contesto.** È la prima delle quattro storie che portano il salone dal fatturato al margine, e da sola non fa
niente di visibile: serve alle tre che seguono. La distinzione **cabina / rivendita** non è una classificazione
burocratica, è la struttura del problema: il prodotto di cabina si consuma durante un servizio e diventa un
costo, quello di rivendita si vende e diventa un ricavo — e lo stesso prodotto fisico può stare in tutti e due i
posti, con quantità diverse.

## 2. Requisiti funzionali

1. **RF-1** — Si crea, modifica e archivia un prodotto con: codice interno, marca, linea, nome, formato con la sua
   **unità di misura**, costo d'acquisto e prezzo di vendita.
2. **RF-2** — Ogni prodotto dichiara in quali **depositi** vive: cabina, rivendita, o entrambi. I depositi sono
   due e sono fissi: non è un magazzino generale.
3. **RF-3** — L'elenco si cerca per nome, marca e codice, e si filtra per deposito e per stato di scorta.
4. **RF-4** — Un prodotto archiviato smette di comparire nelle scelte ma resta nei movimenti passati: la storia
   non si riscrive.
5. **RF-5** — I prodotti si possono caricare da un file tabellare, perché nessun salone inserisce trecento
   referenze a mano: se l'importazione non c'è, l'epica non parte.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura dei prodotti filtra per `tenant_id` dal token
  verificato; anche la ricerca e l'importazione.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST /api/<app>/v1/prodotti`,
  `GET|PUT|DELETE /api/<app>/v1/prodotti/{id}`, `POST /api/<app>/v1/prodotti/importazione`; corpo validato
  (unità di misura fra quelle ammesse, importi non negativi); errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Tabella `prodotto` con `tenant_id`, UUID versione 7, colonne di controllo e
  cancellazione logica; importi in **centesimi interi**; unità di misura obbligatoria (storia `0002`).
- **RT-4 — Varchi e quota (§6, §7).** La sezione è accesa dal piano: con un piano che non la prevede risponde
  `402` con l'indicazione del rimedio. Il prodotto **non consuma** la metrica `postazioni`: la metrica è una sola
  e non è questa.
- **RT-5 — Modulo frontend (§3, §5).** Sezione «Cabina e prodotti»: elenco con ricerca istantanea e filtro per
  deposito, modulo di inserimento breve, importazione con anteprima e scarto delle righe non valide. Solo token
  del sistema di design, tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Etichette, unità di misura, messaggi di errore e testi dell'importazione in
  `en, it, fr, es, de`.
- **RT-7 — Dati personali (§10).** **Nessun dato personale**: un prodotto non riguarda nessuno. Va dichiarato,
  non taciuto.
- **RT-8 — Registrazione eventi (§14).** `prodotto creato`, `importazione eseguita` con conteggi, `tenant_id`,
  `app_id`, `user_id` e correlazione.

## 4. Criteri di accettazione

**CA-1 — Un prodotto in due depositi**
- **Dato** uno shampoo che si usa in cabina e si rivende
- **Quando** lo si crea dichiarando entrambi i depositi
- **Allora** compare in entrambi i filtri, con giacenze separate

**CA-2 — Importazione con scarti**
- **Dato** un file di cento righe di cui tre con unità di misura sconosciuta
- **Quando** lo si importa
- **Allora** entrano novantasette prodotti, le tre righe scartate sono elencate con il motivo, e nulla di parziale
  resta a metà

**CA-3 — L'archiviato non sparisce dal passato**
- **Dato** un prodotto con movimenti registrati
- **Quando** lo si archivia
- **Allora** non compare più nelle scelte, ma i movimenti passati continuano a mostrarlo

**CA-4 — Piano insufficiente**
- **Dato** un account sul piano gratuito
- **Quando** apre la sezione dei prodotti
- **Allora** riceve `402` con un messaggio che dice quale piano la accende

**CA-5 — Isolamento fra account**
- **Dato** due account con lo stesso codice prodotto
- **Quando** un utente del primo cerca quel codice
- **Allora** trova solo il proprio

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sulla validazione dell'importazione, di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account** su elenco, ricerca e importazione;
- [ ] **prova end-to-end**: *rimando* — passo del percorso `[J-SALONGROVE]` della storia `0030`;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, e il fatto è dichiarato;
- [ ] **registro delle decisioni**: due depositi fissi e non un magazzino generale, unità di misura obbligatoria,
      importi in centesimi, importazione tutto-o-niente per riga;
- [ ] avvio locale invariato; il salone di prova ha prodotti in entrambi i depositi.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | la tabella |
| storia `0003` | la sezione «Cabina e prodotti» |
| storia `0004` | la sezione è accesa dal piano |

## 7. Fuori ambito

- le giacenze e i movimenti: storia `0016`;
- il consumo per servizio: storia `0017`;
- lotti, scadenze e inventari fisici: sono di un magazzino generale, cioè dell'app 14 StockGrove. È il confine
  dichiarato al §10 della descrizione, e questa storia lo rispetta;
- la vendita al banco: storia `0021`.

## 8. Punti aperti

**Due depositi bastano?** Un centro estetico con più cabine potrebbe volerne uno per cabina. La proposta è
tenerne due e vedere: aggiungere depositi dopo è additivo, toglierli quando qualcuno li usa non lo è. Se
emergesse il bisogno, è il segnale che quel cliente ha bisogno di StockGrove, non di più magazzino qui.
