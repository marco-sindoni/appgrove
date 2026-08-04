# 0007 — Identificativi e codici a barre

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 02 — Anagrafiche e catalogo prodotti
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che riceve la merce al banco di carico
> voglio trovare l'articolo digitando o leggendo uno qualunque dei codici che gli stanno addosso
> così da non dover ricordare a memoria che il codice del fornitore `X-88/B` è la nostra vite `VT-020`.

**Contesto.** Sulla stessa scatola convivono più codici: quello stampato dal produttore (il codice a barre
commerciale), quello che usa il fornitore sui suoi documenti e quello che l'impresa si è inventata per lo scaffale.
Se l'app ne conosce uno solo, il primo che legge un codice diverso torna al foglio di calcolo. Questa storia
introduce quindi **più codici per lo stesso articolo**, ed è il presupposto tecnico di tutta l'epica 06: senza
questa tabella la scansione con la fotocamera (`0030`) non ha nulla su cui cercare.

Un punto va detto qui e non altrove: **il codice commerciale GTIN si registra, non si genera**. Il prefisso
aziendale da cui nascono i codici GTIN/EAN è **noleggiato** a GS1 — in Italia, per fatturati fino a 500.000 €,
300 € di iscrizione e 95 € l'anno per un pacchetto di 1.000 codici (descrizione dell'applicazione, §2.3 punto 4 e
§2.6 fonte 7) — e non è proprietà di chi lo usa. Un codice inventato dal programma, o copiato da un prodotto
altrui, esce dall'azienda su un'etichetta, entra nei sistemi di qualcun altro e diventa un errore che non si
richiama indietro. Perciò StockGrove ha un campo dove trascrivere il GTIN che il prodotto ha già, e **nessun
bottone che lo generi**. Il codice interno è l'opposto: è dell'impresa, non vale niente fuori, e quello sì si può
stampare su un'etichetta (storia `0033`).

## 2. Requisiti funzionali

1. **RF-1** — Esiste la tabella `codice_articolo` che lega un codice a un articolo, con `tipo`
   (`gtin` | `interno` | `fornitore`), `valore`, `fornitore_id` facoltativo e `principale` sì/no.
2. **RF-2** — Un articolo può avere **più codici**; un codice appartiene a **un solo** articolo. Il vincolo di
   univocità del valore è **per account e per tipo**, e ignora spazi e trattini di formattazione.
3. **RF-3** — Un valore di tipo `gtin` è accettato solo se ha 8, 12, 13 o 14 cifre e la **cifra di controllo**
   torna; altrimenti è respinto con un messaggio che spiega quale delle due condizioni è mancata. Il programma
   **non genera** né suggerisce mai un valore GTIN: l'interfaccia lo dice esplicitamente accanto al campo.
4. **RF-4** — La ricerca per codice restituisce l'articolo a fronte di **uno qualunque** dei suoi codici, compreso
   il codice interno dell'anagrafica (storia `0006`), con corrispondenza esatta dopo la normalizzazione.
5. **RF-5** — Un codice si aggiunge, si marca come principale e si rimuove. La rimozione è una cancellazione
   logica: un codice ritirato non blocca il riuso del valore su un altro articolo, ma resta leggibile nella scheda
   per capire perché una vecchia etichetta non risponde più.
6. **RF-6** — Se il codice letto o digitato non corrisponde a nessun articolo, la risposta lo dice in modo
   parlante e propone di **creare l'articolo** con quel codice già compilato: è il caso normale del primo carico
   di una referenza nuova.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `codice_articolo` filtra per `tenant_id`
  preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene
  ignorato. L'univocità del valore è **per account**: due imprese diverse rivendono lo stesso prodotto e hanno
  legittimamente lo stesso GTIN. Prova di isolamento fra due account sulla risorsa e sulla ricerca per codice.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/magazzino/v1/articoli/{id}/codici`,
  `DELETE /api/magazzino/v1/articoli/{id}/codici/{codiceId}` e la ricerca
  `GET /api/magazzino/v1/articoli?codice=<valore>`; oggetti di trasferimento al bordo; validazione dichiarativa
  della cifra di controllo; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-3 — Persistenza (§8).** Migrazione `V3__codice_articolo.sql` sullo schema `app_magazzino`: tabella
  `codice_articolo` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e `deleted_at`; indice
  unico su `tenant_id, tipo, valore_normalizzato` con `deleted_at is null`; indice di ricerca su
  `tenant_id, valore_normalizzato` per la lettura da scansione. Il riferimento all'articolo è interno allo schema;
  nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro «Codici» nella scheda dell'articolo, dentro la sezione `articoli`
  del modulo `magazzino`: elenco dei codici con il tipo, aggiunta, marcatura del principale, ritiro. Dati letti con
  il client generato; solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — nomi dei tipi di codice, errore di cifra di
  controllo, nota «il codice commerciale si registra, non si genera» — passano dallo spazio-nomi `magazzino` e sono
  presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Aggiungere un codice **non consuma quota**: la metrica `articoli_gestiti`
  conta gli articoli attivi, non i loro identificativi. Un articolo con dieci codici occupa un posto come uno con
  zero. Con abbonamento `canceled` il servizio risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo qui, ma questa storia **estende il
  parametro** degli strumenti di lettura `trova_articolo` e `leggi_giacenza` (storia `0034`), che accettano
  indifferentemente il codice interno o il codice commerciale — è la ragione per cui la firma dichiarata nella
  descrizione dell'applicazione (§7) si chiama `codice_o_gtin`.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: un codice a barre è un identificativo di una cosa,
  non di una persona. Il collegamento facoltativo al fornitore è un riferimento all'anagrafica della storia `0009`
  e viene dichiarato lì.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `codice aggiunto`, `codice ritirato`, `codice rifiutato per
  cifra di controllo` e `ricerca per codice senza esito` sono registrati con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione. Il valore del codice **si può registrare**: identifica una merce, non una
  persona.

## 4. Criteri di accettazione

**CA-1 — Più codici per lo stesso articolo**
- **Dato** l'articolo `VT-020` già esistente
- **Quando** si aggiungono il codice commerciale `8001234567895` e il codice del fornitore `X-88/B`
- **Allora** la ricerca per `8001234567895`, per `X-88/B` e per `VT-020` restituisce tutte e tre lo stesso articolo

**CA-2 — Codice commerciale con cifra di controllo sbagliata**
- **Dato** un utente che compila il campo del codice commerciale · **Quando** inserisce `8001234567890`
- **Allora** la risposta è `400` in `application/problem+json`, il messaggio dice che la cifra di controllo non
  torna, nulla viene salvato e l'interfaccia **non propone alcun valore alternativo**

**CA-3 — Lo stesso codice su due articoli dello stesso account**
- **Dato** l'articolo `VT-020` con il codice commerciale `8001234567895`
- **Quando** si tenta di attribuire lo stesso valore all'articolo `VT-030`
- **Allora** la risposta è `409`, il messaggio nomina l'articolo che possiede già il codice e nulla viene creato

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B` che rivendono lo stesso prodotto con codice commerciale `8001234567895`
- **Quando** un utente di `A` cerca quel codice
- **Allora** trova il proprio articolo e nessun riferimento a quello di `B`, anche forzando l'identificativo
  dell'altro account nella richiesta; e l'inserimento in `B` dello stesso valore riesce

**CA-5 — Codice sconosciuto**
- **Dato** un codice mai registrato · **Quando** lo si cerca
- **Allora** la risposta è `404` con un messaggio che invita a creare l'articolo, e l'interfaccia apre il modulo di
  creazione con il codice già compilato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo della cifra di controllo (tutte e quattro le lunghezze ammesse) e sulla
      normalizzazione del valore; prove di **integrazione** sulla risorsa dei codici e sulla ricerca, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su `codice_articolo`, compreso lo stesso valore in due account;
- [ ] **prova end-to-end**: *rimando* — la lettura di un codice fa parte del percorso `[J-MAGAZZINO]` di proprietà
      della storia `0036`, e la scansione vera è dell'epica 06; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna voce nuova, e il fatto è dichiarato nel registro delle decisioni;
- [ ] **registro delle decisioni** compilato, con la scelta di non generare mai codici commerciali e il motivo
      (prefisso GS1 noleggiato, §2.3 punto 4);
- [ ] contratto degli **strumenti conversazionali**: il parametro `codice_o_gtin` degli strumenti di lettura è
      documentato in questa storia e realizzato nella `0034`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0006` | L'articolo deve esistere prima di poter essere identificato in più modi |
| `0009` (in avanti, facoltativa) | Il collegamento `fornitore_id` sul codice del fornitore resta vuoto finché l'anagrafica dei fornitori non esiste; il campo si popola con quella storia |

## 7. Fuori ambito

- **Lettura del codice con la fotocamera**: storia `0030`. Qui il codice si digita o si incolla, ed è già
  sufficiente per i lettori esterni che si comportano come una tastiera (descrizione, §2.4 punto 5).
- **Stampa delle etichette con il codice interno**: storia `0033`.
- **Generazione di codici commerciali**: non è fuori ambito, è **fuori prodotto**: non si farà in nessuna storia.
- **Lotti, date di scadenza e numeri di matricola**: non sono identificativi dell'articolo ma dimensioni del
  movimento; sono fuori perimetro (descrizione, §11 punto 2).

## 8. Punti aperti

- **Tipi di codice oltre i tre previsti** (per esempio il codice interno di un cliente che impone il proprio
  riferimento): il modello lo reggerebbe aggiungendo un valore all'elenco chiuso, ma non ho riscontrato la domanda
  nell'analisi in rete. Si aggiunge quando qualcuno la chiede.
- **Cosa fare quando un fornitore riusa lo stesso proprio codice per un prodotto diverso** dopo qualche anno: il
  ritiro logico del codice lo consente, ma il comportamento atteso dall'utente non è dimostrato. Chiude lo
  sviluppatore alla prima segnalazione reale.
