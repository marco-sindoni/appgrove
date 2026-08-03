# 0010 — Elenco, ricerca e viste di lavoro

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 02 — Casella condivisa e conversazioni
**Storia**: `0010` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0003`, `0006`, `0009`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che apre l'app la mattina
> voglio una coda che mi dica subito che cosa non ha ancora ricevuto risposta e che cosa è mio
> così da cominciare a lavorare senza dover scorrere tutto, e da ritrovare in due secondi la conversazione di cui
> il cliente mi sta parlando al telefono.

**Contesto.** Le richieste esistono, hanno un filo e uno stato, ma si raggiungono solo se si conosce il loro
identificativo: manca la coda, che è il posto dove l'operatore passa la giornata. Questa storia è anche il punto in
cui si esercita la scelta di prodotto più caratterizzante dell'app: **la sottrazione** (§2.5 della descrizione). I
prodotti concorrenti offrono un costruttore di viste personalizzate con filtri salvabili e condivisibili; il segmento
micro non lo usa e ne paga la complessità. Qui si consegnano **tre viste fisse** scelte perché rispondono alle tre
domande che l'operatore si fa davvero — che cosa è scoperto, che cosa è mio, che cosa è ancora vivo — e nessun
costruttore.

## 2. Requisiti funzionali

1. **RF-1** — L'elenco è paginato a pagina e dimensione con il totale, e ordinabile per data di apertura o per data
   dell'ultimo messaggio; l'ordinamento predefinito è **l'ultimo messaggio prima**, con criterio di parità
   deterministico perché la paginazione sia stabile.
2. **RF-2** — La ricerca è a **testo libero** su oggetto della richiesta, nome e posta elettronica del richiedente e
   **numero** della richiesta; è insensibile a maiuscole e accenti e cerca per porzione di parola. Un testo
   composto di sole cifre viene inteso anche come numero di richiesta.
3. **RF-3** — Il filtro per **stato** accetta più stati insieme e si combina con la ricerca; il risultato è
   l'intersezione, e i filtri attivi sono visibili e rimovibili uno per uno.
4. **RF-4** — Esistono **tre viste di lavoro** predefinite e non modificabili: «Da prendere in carico» (stato
   `aperta` e nessun operatore assegnato), «Mie» (assegnate all'utente che guarda), «Tutte le aperte» (tutto ciò che
   non è `risolta` né `chiusa`). Finché l'assegnazione esplicita non esiste (storia `0020`) il campo dell'operatore
   resta vuoto e la vista «Mie» è legittimamente vuota: è un comportamento atteso, non un difetto.
5. **RF-5** — Accanto al nome di ciascuna vista compare il **conteggio** delle richieste che vi ricadono, coerente
   con il contenuto della vista quando la si apre.
6. **RF-6** — **Non esiste** un costruttore di viste personalizzate: nessun salvataggio di filtri, nessuna
   condivisione di viste, nessun elenco di viste da amministrare. È una sottrazione deliberata e va detta anche
   nella pagina del prodotto, non nascosta.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni interrogazione dell'elenco, della ricerca e dei conteggi filtra per
  `tenant_id` preso dal token verificato, **prima** di qualunque altro filtro; un `tenant_id` che arrivasse dal
  corpo o dai parametri viene ignorato. Cercare il numero esatto di una richiesta di un altro account non ne rivela
  neppure l'esistenza: l'elenco torna vuoto.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/helpdesk/v1/tickets` con i parametri `q` (testo),
  `status` (ripetibile), `view` (una delle tre viste), `page`, `size`, `sort`; risposta paginata con il totale;
  `GET /api/helpdesk/v1/tickets/counts` per i conteggi delle viste. Parametri validati (dimensione massima della
  pagina, lunghezza massima del testo cercato); errori in `application/problem+json`; definizione OpenAPI aggiornata
  nello stesso commit. La risposta dell'elenco è **minimizzata**: numero, oggetto, stato, richiedente, data
  dell'ultimo messaggio — **mai** il corpo dei messaggi.
- **RT-3 — Persistenza (§8).** Migrazione `V7__ticket_search_indexes.sql` sullo schema `app_helpdesk`: indici
  `(tenant_id, status, last_message_at, id)` per l'ordinamento predefinito, `(tenant_id, number)` per la ricerca
  numerica, e un indice testuale su oggetto e sui campi del richiedente che regga la ricerca per porzione di parola
  insensibile ad accenti. Se la colonna `last_message_at` non esiste, la aggiunge questa migrazione e la mantiene
  aggiornata la registrazione del messaggio (storia `0007`), **nella stessa transazione**. La ricerca resta dentro
  PostgreSQL: **nessun motore di ricerca esterno**, che sarebbe un fornitore in più su contenuto altrui.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Richieste» del modulo `helpdesk`: le tre viste come voci fisse con
  il loro conteggio, campo di ricerca, filtri di stato rimovibili, elenco paginato con stato vuoto che spiega cosa
  fare invece di limitarsi a dire «nessun risultato». Dati letti con il client generato e stato del server con
  TanStack Query; solo token del sistema di design con colore-categoria `teal`; tema chiaro e scuro; controllo
  automatico di accessibilità sulla schermata, che è la principale dell'app.
- **RT-5 — Cinque lingue (§4).** Nomi delle tre viste, etichette dei filtri, testi degli stati vuoti e messaggi di
  errore passano dallo spazio-nomi `helpdesk` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Consultare la coda **non consuma quota**: la metrica unica dell'app è `agents`
  (posti operatore, natura `stock`), consumata dalla storia `0018`. Restano i varchi a monte: `401`, `402` con
  abbonamento non attivo, `403` per ruolo insufficiente; tutti i ruoli abilitati possono leggere la coda, compreso
  chi non occupa un posto operatore. La storia non fissa prezzi: consuma il tetto pubblicato dall'abilitazione.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `elenca_richieste(stato?, operatore?, testo?, periodo?) → elenco minimizzato di richieste`, marcato **lettura**,
  che restituisce numero, oggetto, stato e data e **non** il corpo dei messaggi (§7 della descrizione). Il nome
  dello strumento è **stabile**: i parametri `coda` e `oltre_scadenza` previsti dal contratto completo si
  aggiungeranno con le storie `0019` e `0025` senza cambiarne il nome né rompere le chiamate esistenti. Il
  contratto vive dentro il servizio; il server conversazionale è di piattaforma e non ancora implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun campo nuovo e nessuna tabella nuova: la storia **legge** i dati dichiarati
  dalla storia `0006`. Due vincoli però nascono qui e vanno scritti nel manifesto
  `docs/compliance/manifests/helpdesk.yaml` come nota di finalità, in italiano e inglese: la risposta dell'elenco è
  **minimizzata** e non veicola mai il corpo dei messaggi, e il **testo cercato non viene conservato** in alcuna
  tabella di cronologia — una ricerca è spesso il nome o l'indirizzo di una persona.
- **RT-9 — Registrazione eventi (§14).** L'evento `elenco richieste consultato` — se registrato — porta `tenant_id`,
  `app_id`, `user_id`, identificativo di correlazione, i filtri di stato e la **sola lunghezza** del testo cercato:
  **mai il testo**, che può contenere il nome o l'indirizzo di una persona.

## 4. Criteri di accettazione

**CA-1 — Ricerca a testo libero**
- **Dato** un account con una richiesta il cui oggetto è «Reso della lavatrice» e un utente abilitato
- **Quando** cerca `lavatr` in minuscolo
- **Allora** la richiesta compare fra i risultati, con numero, oggetto, stato e richiedente, e senza il corpo dei
  messaggi

**CA-2 — Ricerca per numero**
- **Dato** un account con la richiesta numero `42` · **Quando** l'operatore cerca `42`
- **Allora** la richiesta numero `42` è fra i risultati, in evidenza rispetto alle eventuali corrispondenze testuali

**CA-3 — Vista «Da prendere in carico»**
- **Dato** un account con due richieste `aperta` senza operatore assegnato, una `in lavorazione` e una `chiusa`
- **Quando** l'operatore apre la vista «Da prendere in carico»
- **Allora** vede esattamente le due richieste `aperta`, e il conteggio accanto al nome della vista dice `2`

**CA-4 — Paginazione stabile**
- **Dato** un account con più richieste di quante ne stiano in una pagina, alcune con la stessa data di ultimo
  messaggio · **Quando** si chiedono la prima e la seconda pagina
- **Allora** nessuna richiesta compare in entrambe e nessuna manca: l'ordinamento ha un criterio di parità
  deterministico

**CA-5 — Nessun risultato**
- **Dato** un account con richieste · **Quando** si cerca un testo che non corrisponde a nulla
- **Allora** la risposta è un elenco vuoto con totale zero — **non** un errore — e l'interfaccia mostra uno stato
  vuoto tradotto che suggerisce di togliere un filtro

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, dove `B` ha la richiesta numero `7` con oggetto «Reso della lavatrice»
- **Quando** un utente di `A` cerca `lavatr`, poi `7`, poi forza il `tenant_id` di `B` nei parametri
- **Allora** in tutti e tre i casi non vede alcuna richiesta di `B`, e i conteggi delle viste restano quelli di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla costruzione dei filtri e sull'interpretazione del testo cercato, e di
      **integrazione** su ricerca, ordinamento, paginazione e conteggi, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su elenco, ricerca, viste e conteggi, compresa la ricerca del numero
      esatto di una richiesta altrui;
- [ ] **prova end-to-end**: *coprire ora* — passo «trova la richiesta nella vista Da prendere in carico e aprila»
      del percorso `[J-HELPDESK]`, con l'etichetta in testa al titolo del test; registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna voce nuova, ma nota di finalità aggiornata in italiano e inglese su
      minimizzazione dell'elenco e non conservazione del testo cercato;
- [ ] **registro delle decisioni** compilato, con annotate la scelta delle **tre viste fisse senza costruttore** e
      la scelta di restare dentro PostgreSQL per la ricerca;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `elenca_richieste`, con il nome stabile e i
      parametri estendibili;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0006` | Servono richieste da elencare, con numero e richiedente su cui cercare |
| storia `0009` | Servono gli stati: senza di essi il filtro per stato e due delle tre viste non hanno significato |
| storia `0003` | Serve il guscio del modulo `helpdesk` per appendere la sezione «Richieste» con le sue viste |
| epica di piattaforma non implementata (UC 0061-0063) | Il livello conversazionale non esiste: qui si dichiara solo il contratto di `elenca_richieste` |

## 7. Fuori ambito

- il **costruttore di viste personalizzate**, i filtri salvabili e condivisibili: **non li fa nessuna storia** ed è
  una scelta di prodotto, non un rimando (§2.5 della descrizione);
- il filtro per **coda**: storia `0019`, che aggiunge la dimensione senza cambiare la forma dell'elenco;
- il filtro per **priorità** ed **etichette**: storia `0021`;
- il filtro «**oltre la scadenza**» e l'ordinamento per scadenza: storia `0025`, che introduce le scadenze;
- l'**assegnazione** che riempie la vista «Mie»: storia `0020`;
- i **numeri di sintesi** del servizio (carico, tempi medi, violazioni, soddisfazione): storia `0028`, che è un
  cruscotto e non una coda;
- l'**esportazione dell'elenco** in un file: non prevista in questa stesura; l'esportazione dei dati per obblighi di
  protezione dei dati è la storia `0036` ed è un'altra cosa.

## 8. Punti aperti

- **La ricerca deve entrare anche nel corpo dei messaggi?** È la funzione che gli operatori chiedono per seconda, e
  qui è deliberatamente esclusa: cercare dentro il testo libero di terzi richiede un indice sul contenuto più
  delicato dell'app e cambia il rapporto fra utilità e rischio descritto al §6 della descrizione. La proposta è di
  valutarla **dopo** che la conservazione sarà governata dal cliente (storia `0036`), non prima. Chiude lo
  **sviluppatore**.
- **Quanto in là si può paginare senza degradare?** Il punto 6 del §11 della descrizione ricorda che nulla limita il
  numero di richieste accumulate da un account. Qui si propone un tetto esplicito alla dimensione della pagina e
  nessun tetto al numero di pagine, rimandando la questione del volume alla storia `0036`. Chiude lo
  **sviluppatore**.
