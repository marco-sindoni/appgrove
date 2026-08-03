# 0013 — Caricamento del listino da file

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 03 — Catalogo prodotti in chat
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`, `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio con duecento articoli in un foglio di calcolo
> voglio caricarli tutti insieme e vedere prima cosa entrerà davvero
> così da non passare la prima sera a digitare, che è il momento in cui si smette di usare un'app nuova.

**Contesto.** È la richiesta più banale del segmento e la più presente nelle recensioni (§2.4 dell'analisi): il
catalogo esiste già, da qualche parte, in una tabella. Senza il caricamento, il primo giorno di ChatGrove è
una giornata di digitazione, e la barriera d'adozione del §2.5 vince. Va dopo le due storie del catalogo perché
carica ciò che quelle hanno definito.

## 2. Requisiti funzionali

1. **RF-1** — Il negozio carica un file tabellare (valori separati da virgola o foglio di calcolo) con una
   riga per prodotto o variante.
2. **RF-2** — L'app mostra un'**anteprima** prima di scrivere qualsiasi cosa: quante righe entrano, quante
   aggiornano un prodotto esistente, quante sono scartate e **perché**, riga per riga.
3. **RF-3** — Il caricamento si conferma esplicitamente; senza conferma nulla viene scritto.
4. **RF-4** — L'abbinamento con i prodotti esistenti avviene sul **codice**: stesso codice significa
   aggiornamento, codice nuovo significa creazione.
5. **RF-5** — Le righe scartate si possono scaricare in un file con la colonna del motivo, per correggerle e
   ricaricarle.
6. **RF-6** — Il caricamento è **tutto o niente**: se la scrittura fallisce a metà, il catalogo resta come
   prima.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il caricamento scrive solo nell'account dell'utente, ricavato dal
  `tenant_id` del token verificato; nessuna colonna del file può indicare un account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/chat_commerce/v1/products/import/preview` e
  `POST /api/chat_commerce/v1/products/import/commit`; dimensione massima del file dichiarata; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** La conferma avviene in **una sola transazione**. L'anteprima non scrive nulla:
  vive nella richiesta, non in una tabella di appoggio.
- **RT-4 — Ruoli (§6).** Solo `owner` e `admin` possono caricare un listino: è un'operazione che riscrive il
  catalogo. Un `member` riceve `403`.
- **RT-5 — Modulo frontend (§3, §4, §5).** Schermata di caricamento nella sezione Catalogo, con tabella
  dell'anteprima e conteggi. Tutte le stringhe, **compresi i motivi di scarto**, in `en, it, fr, es, de`.
- **RT-6 — Dati personali (§10).** Nessun dato personale: un listino contiene prodotti. Se il file ne
  contenesse per errore in una colonna non prevista, quella colonna viene **ignorata**, non conservata.
- **RT-7 — Registrazione eventi (§14).** `anteprima caricamento`, `caricamento confermato` con i conteggi,
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione; **mai** il contenuto del file.
- **RT-8 — Sicurezza del file.** Il file caricato è un ingresso non fidato: dimensione massima, numero massimo
  di righe, nessuna interpretazione di formule, nessun percorso di file preso dal contenuto.

## 4. Criteri di accettazione

**CA-1 — Anteprima onesta**
- **Dato** un file di 200 righe di cui 3 senza prezzo
- **Quando** si chiede l'anteprima
- **Allora** si legge «197 righe pronte, 3 scartate» con il motivo su ognuna delle tre, e **nulla** è stato
  scritto nel catalogo

**CA-2 — Conferma**
- **Dato** l'anteprima di cui sopra · **Quando** si conferma · **Allora** il catalogo contiene i 197 prodotti e
  le 3 righe scartate restano scaricabili

**CA-3 — Aggiornamento sul codice**
- **Dato** un catalogo con il prodotto `TORTA-01` a 20,00 €
- **Quando** si carica un file che contiene `TORTA-01` a 22,00 €
- **Allora** dopo la conferma il prodotto esiste **una sola volta**, a 22,00 €

**CA-4 — Tutto o niente**
- **Dato** un file valido · **Quando** la scrittura fallisce a metà per un errore del database
- **Allora** il catalogo è identico a prima del caricamento

**CA-5 — Isolamento fra account**
- **Dato** un file che contiene una colonna con l'identificativo di un altro account
- **Quando** si carica · **Allora** la colonna è ignorata e i prodotti finiscono nell'account di chi carica

**CA-6 — Ruolo insufficiente**
- **Dato** un utente `member` · **Quando** tenta il caricamento · **Allora** riceve `403`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla lettura del file e sui motivi di scarto, e di **integrazione** sulla conferma
      in una sola transazione;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sul caricamento;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`; il caricamento è un passo utile ma non necessario
      al percorso minimo;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, compresi i motivi di scarto;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, con la scelta dell'abbinamento sul codice e del tutto-o-niente;
- [ ] contratto degli **strumenti conversazionali**: il caricamento **non** è esposto come strumento — un
      assistente non carica file, e l'anteprima esiste proprio perché serve un occhio umano;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0011` | Carica prodotti |
| `0012` | Carica anche varianti e disponibilità |

## 7. Fuori ambito

- l'esportazione del catalogo verso un file: utile, ma è una storia a sé;
- il caricamento delle immagini in massa: la prima versione carica testo e prezzi;
- la sincronizzazione ricorrente con un foglio esterno: sarebbe un'integrazione, non un caricamento.

## 8. Punti aperti

- Nessuno.
