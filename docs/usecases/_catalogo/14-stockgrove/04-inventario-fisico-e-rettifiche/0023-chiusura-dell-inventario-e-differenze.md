# 0023 — Chiusura dell'inventario e differenze

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 04 — Inventario fisico, rettifiche e valore
**Storia**: `0023` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0021`, `0022`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha finito di contare
> voglio vedere tutte le differenze prima di accettarle e poi confermarle in un colpo solo, dicendo perché
> così da chiudere l'inventario sapendo esattamente cosa sto per cambiare, invece di scoprirlo dopo.

**Contesto.** La sessione della storia `0022` contiene i numeri contati, ma finché non si chiude non è successo
niente: la giacenza è ancora quella di prima. La chiusura è **il momento più pericoloso dell'applicazione**, perché
in un solo gesto può cambiare centinaia di saldi. Per questo qui non si automatizza nulla di nascosto: le
differenze si mostrano tutte, con il segno e il totale, e la conferma è un atto esplicito. È la stessa regola che
il livello conversazionale applicherà allo strumento `chiudi_inventario` — «effetto ampio, conferma obbligatoria
con l'elenco delle differenze mostrato prima» (descrizione dell'applicazione, §7).

## 2. Requisiti funzionali

1. **RF-1** — Prima della conferma si può chiedere l'**anteprima della chiusura**: per ogni riga contata, quantità
   contata, giacenza corrente, differenza con il segno; in coda il numero di righe in eccedenza, in mancanza, senza
   differenza, non contate e saltate. L'anteprima non scrive nulla e si può chiedere quante volte si vuole.
2. **RF-2** — L'anteprima segnala separatamente le righe il cui articolo **si è mosso durante il conteggio** (la
   giacenza corrente è diversa dall'atteso congelato): sono le differenze che meritano un'occhiata prima di
   confermare, perché possono essere effetto del tempo e non del magazzino.
3. **RF-3** — Alla conferma nasce **una rettifica per ogni riga con differenza diversa da zero**, con il motivo
   scelto: un motivo generale per l'intera sessione, sovrascrivibile riga per riga. Le rettifiche riusano
   l'operazione della storia `0021`, con le stesse regole (motivo obbligatorio, `altro` con nota, mai sotto zero).
4. **RF-4** — La chiusura avviene in **una sola transazione**: o tutte le rettifiche sono scritte, o nessuna. Se
   anche una sola riga fallisce, la sessione resta `aperta` e il messaggio dice quale riga ha fallito e perché.
5. **RF-5** — Le righe **non contate** e quelle **saltate** non producono alcuna rettifica: non contare non è
   contare zero, ed è la distinzione che impedisce a un inventario interrotto di azzerare mezzo magazzino.
6. **RF-6** — Una sessione chiusa passa in stato `chiusa`, porta chiusura e autore, **non si riapre** e non si
   modifica. Se il conteggio era sbagliato si stornano le rettifiche prodotte (storia `0017`) oppure si apre una
   sessione nuova: il passato non si riscrive.
7. **RF-7** — L'esito della chiusura è **scaricabile** in un file per foglio di calcolo: articolo, deposito,
   contato, giacenza al momento della chiusura, differenza, motivo, chi ha contato, quando. È il documento che
   resta agli atti dell'impresa.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Anteprima, chiusura e scarico del file filtrano per `tenant_id` preso dal
  token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato. Prova di
  isolamento fra due account sulla chiusura.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/magazzino/v1/inventari/{id}/chiusura` (anteprima),
  `POST /api/magazzino/v1/inventari/{id}/chiusura` (conferma, con motivo generale, eventuali motivi per riga e
  chiave di idempotenza) e `GET /api/magazzino/v1/inventari/{id}/chiusura/esito` per il file; errori in
  `application/problem+json` (`409` se la sessione non è `aperta`, `422` se manca un motivo dovuto); definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: si valorizzano `stato`, `chiusa_il` e `chiusa_da` su
  `inventario` e si scrive su `riga_inventario` il riferimento al movimento di rettifica generato, più la giacenza
  corrente al momento della chiusura, perché il documento scaricato resti riproducibile. Le rettifiche sono righe
  di `movimento`, in sola aggiunta come tutte le altre. La **chiave di idempotenza** della chiusura impedisce che
  un doppio invio produca due volte le stesse rettifiche.
- **RT-4 — Modulo frontend (§3, §5).** Nella sezione `inventari` del modulo `magazzino`: schermata di anteprima con
  le differenze ordinate per scarto assoluto decrescente, il totale in testa, le righe mosse durante il conteggio
  evidenziate, il motivo generale in cima e il motivo per riga apribile; il bottone di conferma dice quante
  rettifiche sta per creare. Solo token del sistema di design, colore-categoria `amber`; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, intestazioni del file scaricato e messaggi di conferma passano dallo
  spazio-nomi `magazzino` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La chiusura **non consuma quota e non viene mai respinta con `429`**, nemmeno
  quando genera trecento rettifiche: il tetto `articoli_gestiti` (natura `stock`) colpisce solo la creazione di
  articoli nuovi. Valgono gli altri varchi: `401`, `402` con abbonamento `canceled`, `403` per ruolo insufficiente.
- **RT-7 — Esposizione conversazionale (§12).** Strumento `chiudi_inventario(id_inventario) → bozza con l'elenco
  completo delle differenze`, marcato **scrittura con effetto ampio**: la bozza mostra tutte le differenze e il
  numero di rettifiche che verrebbero create, e richiede **conferma umana esplicita**. Nessuna scorciatoia che
  chiuda senza mostrare. Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non
  ancora implementato (UC 0061-0063). Dettaglio nella storia `0035`.
- **RT-8 — Dati personali (§10).** Nessun campo personale nuovo: chi ha contato e chi ha chiuso sono già dichiarati
  nel manifesto dalla storia `0010`. Il file scaricato riporta chi ha contato ogni riga perché serve alla
  tracciabilità della merce, e per questo motivo **non** esiste alcuna vista che aggreghi le differenze per
  persona: sarebbe una misura di prestazione individuale (art. 4 della legge 300/1970; descrizione, §6).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `anteprima di chiusura richiesta`, `inventario chiuso` (con il
  numero di rettifiche generate), `chiusura fallita` sono registrati con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione, senza note a testo libero.

## 4. Criteri di accettazione

**CA-1 — Anteprima con differenze in entrambi i sensi**
- **Dato** una sessione aperta con tre righe: contata 12 su giacenza 10, contata 3 su giacenza 5, contata 7 su
  giacenza 7
- **Quando** si chiede l'anteprima della chiusura
- **Allora** si vedono `+2`, `−3` e `0`, con una riga in eccedenza, una in mancanza e una senza differenza, e nulla
  è stato scritto: le giacenze restano 10, 5 e 7

**CA-2 — Conferma: una rettifica per riga con differenza**
- **Dato** l'anteprima precedente e il motivo generale `merce_mancante` con `merce_trovata` scelto sulla prima riga
- **Quando** si conferma la chiusura
- **Allora** nascono **due** movimenti di rettifica (`+2` con `merce_trovata`, `−3` con `merce_mancante`), le
  giacenze diventano 12, 3 e 7, la sessione è `chiusa` con autore e momento, e la riga senza differenza non ha
  generato alcun movimento

**CA-3 — Righe non contate e saltate**
- **Dato** una sessione con due righe contate e cinque mai toccate, di cui una dichiarata saltata
- **Quando** si conferma la chiusura
- **Allora** nascono rettifiche solo per le righe contate con differenza; le cinque righe restano senza movimento e
  le loro giacenze sono immutate

**CA-4 — Fallimento parziale: nessuna scrittura**
- **Dato** una sessione la cui riga numero sette porterebbe la giacenza sotto zero
- **Quando** si conferma la chiusura
- **Allora** la risposta è `422`, indica la riga sette e il motivo, **nessuna** rettifica è stata scritta e la
  sessione è ancora `aperta`

**CA-5 — Una sessione chiusa non si riapre**
- **Dato** una sessione in stato `chiusa` · **Quando** si tenta di contare una riga o di chiuderla di nuovo
- **Allora** la risposta è `409` in `application/problem+json`, con l'indicazione che la correzione si fa con uno
  storno o con una sessione nuova

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie sessioni
- **Quando** un utente di `A` tenta di chiudere una sessione di `B`
- **Allora** riceve `404`, la sessione di `B` resta aperta e nessuna sua giacenza cambia

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo delle differenze e sulla distinzione fra riga non contata e riga a zero, di
      **integrazione** sulla chiusura in una sola transazione, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla chiusura;
- [ ] prova che una chiusura fallita a metà **non lascia nessuna rettifica** e prova che un doppio invio con la
      stessa chiave di idempotenza non raddoppia le rettifiche;
- [ ] **prova end-to-end**: *rimando* — la chiusura è il cuore del percorso `[J-MAGAZZINO]` dell'inventario, di
      proprietà della storia `0037`; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), comprese le intestazioni del file
      scaricato;
- [ ] **manifesto dei dati**: nessun campo nuovo; verificato che non esistano viste aggregate per persona;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con la scelta della transazione unica
      e della sessione non riapribile;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `chiudi_inventario`, con bozza e conferma;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0022` | La sessione, le righe, il congelato e le condizioni di riga devono esistere |
| `0021` | La rettifica con motivo obbligatorio è l'operazione che la chiusura ripete riga per riga: qui non si scrive una seconda via per cambiare i saldi |
| `0017` (a valle) | Lo storno è l'unico rimedio a una chiusura sbagliata, dato che la sessione non si riapre |

## 7. Fuori ambito

- **Il conteggio in sé**: storia `0022`.
- **La riparazione della proiezione divergente dal registro**: storia `0024`, che non è una rettifica e non passa
  di qui.
- **Il valore in denaro delle differenze trovate**: storia `0025`. Sapere che mancano tre pezzi e sapere quanto
  valgono sono due domande diverse e la seconda tocca il confine con la contabilità.
- **L'approvazione a due mani** della chiusura (chi conta ≠ chi conferma): non prevista, vedi i punti aperti.

## 8. Punti aperti

- **Chi può chiudere.** La proposta è di riservare la chiusura a `owner` e `admin`, lasciando il conteggio anche a
  `member`: contare è lavoro di magazzino, cambiare trecento saldi è una decisione. È una scelta di prodotto e va
  confermata dallo sviluppatore; se venisse aperta anche ai `member`, la storia non cambia, cambia la matrice dei
  ruoli.
- **Soglia di allarme sulle differenze.** Una chiusura che sposta il 40 % della giacenza è quasi sempre un errore di
  conteggio, non un magazzino evaporato. Un avviso in anteprima sarebbe utile, ma la soglia oltre la quale
  avvisare non ha una fonte: è una decisione di prodotto, tracciata qui e non inventata.
- **Il file scaricato come documento dell'impresa.** Il nostro cliente è sotto le soglie che rendono obbligatorie le
  scritture ausiliarie di magazzino (descrizione, §2.3): il file è un documento gestionale, **non** un registro
  fiscale, e il testo dell'interfaccia deve dirlo senza ambiguità in tutte e cinque le lingue. Il confine e la sua
  formulazione sono di competenza della storia `0025`.
