# 0006 — Caricamento della ricevuta

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 02 — Cattura e lettura della ricevuta
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come tecnico che ha appena pagato il pranzo in trasferta
> voglio fotografare lo scontrino dal telefono e sapere che è arrivato
> così da poter buttare il pezzo di carta nella tasca senza più pensarci, invece di ritrovarmelo stropicciato a
> fine mese.

**Contesto.** Oggi lo scontrino resta nel portafoglio e, quando arriva sulla scrivania dell'amministrazione, è
sbiadito o perso. È la prima storia del ciclo vero: senza il file caricato non c'è nulla da leggere, nulla da
approvare e nulla da consegnare al commercialista. Si fa adesso, prima della lettura automatica, perché il
caricamento deve funzionare **anche quando la lettura non funziona**: il documento è la prova, l'estrazione è solo
una comodità.

## 2. Requisiti funzionali

1. **RF-1** — Si può caricare un'immagine (JPEG, PNG, HEIC) o un file PDF di una sola pagina; il limite di
   dimensione è dichiarato e l'errore, quando si sfora, dice qual è.
2. **RF-2** — Il caricamento crea una `Ricevuta` e una `Spesa` collegata in stato `caricata`, subito visibile
   nell'elenco anche se non contiene ancora nessun dato.
3. **RF-3** — Del file si conservano l'impronta del contenuto, il tipo, la dimensione e la data di acquisizione;
   l'impronta serve al riconoscimento dei doppioni (storia `0011`) e alla preparazione della conservazione
   (storia `0026`).
4. **RF-4** — Si possono caricare più file in una volta sola (fino a un numero dichiarato per richiesta), e ognuno
   produce la propria ricevuta: se uno fallisce, gli altri passano lo stesso e l'esito lo dice per ciascuno.
5. **RF-5** — L'immagine si può rivedere a schermo intero e ruotare; l'originale non viene mai sovrascritto — la
   rotazione è un dato di visualizzazione.
6. **RF-6** — Un file che non è né immagine né PDF, o che è illeggibile come file, viene respinto **prima** di
   essere archiviato, con un messaggio che spiega cosa fare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `ricevuta` filtra per `tenant_id` preso dal
  token verificato; l'indirizzo con cui si scarica l'immagine è **firmato, a scadenza breve e verificato contro
  l'account**: conoscere l'identificativo di una ricevuta di un altro account non deve bastare per vederla.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/notespese/v1/ricevute` con corpo multiparte,
  `GET /api/notespese/v1/ricevute/{id}/contenuto` per il recupero; errori in `application/problem+json` con
  distinzione fra tipo non ammesso, dimensione eccessiva e file corrotto; definizione OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V3__ricevuta_file.sql` sullo schema `app_notespese`: colonne per
  riferimento all'oggetto archiviato, impronta, tipo, dimensione, orientamento di visualizzazione; `tenant_id`,
  chiave UUID versione 7, colonne di controllo e cancellazione logica. **Il file non entra nel database**: nel
  database va il riferimento.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Spese*: pulsante di caricamento con trascinamento, anteprima,
  barra di avanzamento per file, gestione dell'errore per singolo file. Solo token del sistema di design; tema
  chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutti i messaggi (tipi ammessi, limite di dimensione, esito per file) passano
  dallo spazio-nomi `notespese` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Il caricamento **non consuma quota** (storia `0004`): si consuma alla
  conferma. Vale però un limite tecnico di sicurezza sul numero di caricamenti per intervallo, per evitare che un
  cliente riempia l'archivio senza mai confermare nulla.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia: `leggi_ricevuta` (storia `0029`)
  presuppone un file già caricato, perché un'immagine non si passa in una conversazione.
- **RT-8 — Dati personali (§10).** La storia introduce **il dato più delicato dell'app**: l'immagine di un documento,
  che contiene ciò che contiene. Voce nuova nel manifesto in italiano e inglese (`ricevuta.file`: dove vive, di chi
  è, che dato è, a cosa serve, perché è lecito, per quanto si tiene), campi annotati `@PersonalData`, tabella
  `ricevuta` **e l'oggetto archiviato** aggiunti a `exportData` e `purgeData` — l'oggetto è il candidato più facile
  da dimenticare, perché non è una riga di tabella. Archiviazione **in regione europea**. Nessuna analisi del
  contenuto oltre l'estrazione dei campi dichiarati (storia `0007`).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `ricevuta caricata`, `caricamento respinto` sono registrati con
  `tenant_id`, `app_id`, `user_id`, identificativo di correlazione, tipo e dimensione del file — **mai** il nome del
  file scelto dall'utente, che spesso contiene informazioni.

## 4. Criteri di accettazione

**CA-1 — Caricamento riuscito**
- **Dato** un utente autenticato di un account abilitato
- **Quando** carica la foto di uno scontrino
- **Allora** compare una nuova voce nell'elenco in stato `caricata`, con l'anteprima visibile, e la quota consumata
  **non** è cambiata

**CA-2 — Formato non ammesso**
- **Dato** un utente autenticato · **Quando** carica un foglio di calcolo
- **Allora** riceve `415` con un messaggio che elenca i formati ammessi, e **nulla** viene archiviato

**CA-3 — Caricamento multiplo con un errore in mezzo**
- **Dato** tre file di cui uno oltre il limite di dimensione
- **Quando** si caricano insieme
- **Allora** due ricevute esistono, la terza è respinta, e la risposta dice per ciascun file com'è andata

**CA-4 — Isolamento fra account sull'immagine**
- **Dato** due account `A` e `B`, ciascuno con le proprie ricevute
- **Quando** un utente di `A` chiede il contenuto di una ricevuta di `B` conoscendone l'identificativo
- **Allora** riceve `404` — non `403`, che confermerebbe l'esistenza dell'oggetto

**CA-5 — L'originale non si tocca**
- **Dato** una ricevuta caricata · **Quando** l'utente la ruota di 90 gradi nell'anteprima
- **Allora** l'impronta del file archiviato resta identica: è cambiato solo il dato di visualizzazione

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, frontend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sul controllo del tipo e sul calcolo dell'impronta; di **integrazione** sul caricamento con
      database effimero, migrazioni vere e archivio degli oggetti simulato;
- [ ] prova di **isolamento fra account** sulla risorsa `ricevute` e sull'indirizzo di recupero del contenuto;
- [ ] **prova end-to-end**: *coprire ora* il primo passo del percorso `[J-NOTESPESE]` (caricamento di una ricevuta
      finta) e registrare la voce in
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la voce del file della ricevuta, e l'oggetto
      archiviato presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta di non far consumare quota al caricamento;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia, con la motivazione scritta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0002` | Servono `spesa` e `ricevuta` e la macchina a stati |
| `0003` | Serve la sezione dove mettere il pulsante di caricamento |
| Archivio degli oggetti della piattaforma | Il file vive lì, in regione europea; in locale è simulato |

## 7. Fuori ambito

- La lettura automatica dei dati: storia `0007`. Qui il file è solo archiviato.
- L'ingresso via posta elettronica: punto aperto n. 5 della descrizione dell'applicazione.
- I documenti a più pagine (per esempio la fattura di un albergo su due fogli): rimandati, perché richiedono un
  modello di ricevuta diverso. Se serviranno, sarà una storia a sé.

## 8. Punti aperti

- **Ritenzione dei file caricati e mai confermati**: una ricevuta rimasta in `caricata` per mesi occupa spazio e
  contiene dati personali senza servire a niente. Serve una regola («si cancella dopo N giorni»), che è una
  decisione di prodotto e di conformità insieme: la propone la storia `0011`, la conferma lo sviluppatore.
- **Dimensione massima e numero di file per richiesta**: vanno tarati sul costo dell'archivio e sulla rete dei
  telefoni, non a intuito.
