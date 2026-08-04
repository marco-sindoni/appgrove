# 0002 — Modello dati multi-account

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore dell'applicazione
> voglio la prima tabella di dominio — l'articolo — isolata per account e disegnata secondo la regola che governa
> tutta l'app
> così da poter costruire il registro dei movimenti sopra un modello che non dovrà essere rifatto a metà strada.

**Contesto.** Il servizio esiste ed è vuoto. Questa storia mette a terra l'entità di cui si tiene il conto:
`articolo`. Sembra la tabella più banale dell'applicazione ed è invece **il punto in cui si decide se StockGrove
funzionerà o mentirà**, perché è qui che si stabilisce una cosa per omissione: la tabella `articolo` **non ha e non
avrà mai una colonna `quantita`**. La giacenza non è un attributo dell'articolo, è la somma dei movimenti (storia
`0013`). Il resto della storia è ordinaria amministrazione; quella riga no, ed è per questo che va scritta adesso,
mentre la tabella è ancora vuota e cambiare idea costa zero.

**Perché la quantità non sta qui — l'argomento per esteso.** Se l'articolo avesse una colonna `quantita`,
esisterebbe una schermata con una casella modificabile, e quella casella sarebbe il difetto d'origine
dell'applicazione. Non perché qualcuno la userebbe male, ma perché **un numero riscritto cancella la domanda**: un
saldo che passa da 12 a 9 senza un fatto che lo spieghi è un dato di cui nessuno può dire se è giusto. Con il
registro, la stessa differenza è una storia leggibile — «tre pezzi usciti giovedì per il lavoro dal cliente, con il
nome di chi li ha presi» — e una differenza inattesa diventa una domanda a cui si può rispondere invece di un
mistero da accettare. È esattamente il problema che il cliente ha oggi con il suo foglio di calcolo, e riprodurlo
in un programma a pagamento sarebbe la peggiore delle beffe. Corollario che vale per ogni storia successiva:
l'unico modo di cambiare un saldo sarà un movimento di **rettifica con motivo obbligatorio** (storia `0021`) e
l'unico modo di correggere un errore sarà lo **storno** (storia `0017`).

## 2. Requisiti funzionali

1. **RF-1** — Esiste la tabella `articolo` sullo schema `app_magazzino`, con `tenant_id`, chiave primaria UUID
   versione 7, colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione
   logica (`deleted_at`).
2. **RF-2** — L'articolo porta: `codice_interno`, `descrizione`, `unita_misura`, `categoria`, `stato`
   (`attivo` | `archiviato`) e `origine` (`locale` | `condivisa`, per il confine con il catalogo prodotti della
   storia `0012`; qui vale sempre `locale`).
3. **RF-3** — Il `codice_interno` è **univoco per account** fra gli articoli non cancellati: due account possono
   usare lo stesso codice senza interferire, lo stesso account no e riceve un errore parlante.
4. **RF-4** — La tabella `articolo` **non contiene alcuna colonna di quantità, giacenza, saldo o valore**: la
   giacenza è la somma dei movimenti (storia `0013`) e nessuna rotta di questa storia la scrive o la restituisce.
5. **RF-5** — Esistono le rotte di elenco (paginata a pagina/dimensione con totale, filtrabile per stato e
   categoria), lettura singola e creazione dell'articolo.
6. **RF-6** — L'unità di misura è un elenco chiuso e ampliabile (`pezzo`, `metro`, `metro_quadro`, `chilogrammo`,
   `litro`, `confezione`, `ora`), conservato come chiave e tradotto in interfaccia.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `articolo` filtra per `tenant_id` preso dal
  token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato. Prova di
  isolamento fra due account sulla risorsa.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/magazzino/v1/articoli` e
  `GET /api/magazzino/v1/articoli/{id}`; oggetti di trasferimento al bordo (le entità non si espongono mai);
  validazione dichiarativa sui dati in ingresso; errori in `application/problem+json`; paginazione a
  pagina/dimensione con totale; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V1__articolo.sql` sullo schema `app_magazzino`: tabella `articolo` con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e `deleted_at`; indice univoco parziale su
  `(tenant_id, codice_interno)` valido per le sole righe con `deleted_at` nullo; indice su
  `(tenant_id, stato, categoria)` per l'elenco filtrato. **Nessuna chiave esterna** verso altri schemi: il
  riferimento all'utente è logico.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata: la storia si ferma al servizio. L'elenco navigabile è
  della storia `0003`, la gestione completa dell'anagrafica della `0006`.
- **RT-5 — Cinque lingue (§4).** Le unità di misura e le categorie predefinite sono **chiavi**, non testo: la
  traduzione sta nello spazio-nomi `magazzino` in tutte e cinque le lingue (`en, it, fr, es, de`), e la storia non
  è conclusa se ne manca una.
- **RT-6 — Varchi e quota (§6, §7).** La creazione di un articolo è l'atto che consumerà la metrica
  `articoli_gestiti` (natura `stock`), ma **il conteggio si aggancia nella storia `0004`**: qui le rotte esistono
  senza applicare il tetto. La storia non fissa prezzi.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento dichiarato qui; l'elenco e la ricerca degli
  articoli diventano `elenca_articoli` e `trova_articolo` nella storia `0034`, entrambi di sola lettura. Il server
  conversazionale è di piattaforma e non è ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** L'articolo non è una persona e i suoi campi non lo sono, con **una eccezione da
  dichiarare**: `descrizione` è testo libero e può accogliere il nome di chiunque venga nominato per sbaglio. Va
  quindi già dichiarata come voce del manifesto `docs/compliance/manifests/magazzino.yaml` in italiano e inglese
  (categoria «testo libero, contenuto imprevedibile») e la tabella `articolo` va aggiunta a `exportData` e
  `purgeData`. Le colonne `created_by` e `updated_by` sono trattate come dato sull'attività di un lavoratore e
  seguono la stessa via (storia `0010` per il quadro completo).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `articolo creato`, `articolo archiviato`, `codice duplicato
  respinto` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza** la
  descrizione dell'articolo, che è testo libero.

## 4. Criteri di accettazione

**CA-1 — Creazione e lettura di un articolo**
- **Dato** un utente autenticato di un account abilitato
- **Quando** crea un articolo con codice interno, descrizione, unità di misura e categoria
- **Allora** l'articolo esiste in stato `attivo`, ha un identificativo UUID versione 7 e compare nell'elenco
  paginato del suo account

**CA-2 — Codice interno duplicato**
- **Dato** un account che ha già un articolo con codice `VT-M6-30` · **Quando** ne crea un altro con lo stesso codice
- **Allora** la risposta è `409` in `application/problem+json` con un messaggio che indica il codice in conflitto, e
  nessun articolo viene creato

**CA-3 — Lo stesso codice in due account non è un conflitto**
- **Dato** due account `A` e `B`
- **Quando** entrambi creano un articolo con codice `VT-M6-30`
- **Allora** entrambe le creazioni riescono e ciascun account vede **solo** il proprio

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri articoli
- **Quando** un utente di `A` chiede l'elenco degli articoli o legge un articolo di `B` per identificativo
- **Allora** vede solo i propri e riceve `404` sull'articolo altrui, anche se forza l'identificativo dell'account
  `B` nel corpo della richiesta o in un parametro

**CA-5 — Nessuna quantità nell'articolo**
- **Dato** la definizione OpenAPI e la migrazione di questa storia
- **Quando** si cercano campi di quantità, giacenza, saldo o valore nella tabella `articolo` e nei suoi oggetti di
  trasferimento
- **Allora** non ne esiste nessuno, e una prova automatica lo verifica in modo che una aggiunta futura faccia
  fallire la suite

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione del codice interno e di **integrazione** sulla risorsa `articoli`, con
      database effimero e migrazioni Flyway vere;
- [ ] prova di **isolamento fra account** sulla risorsa `articoli`, compreso il tentativo di forzare l'account
      dall'esterno;
- [ ] **prova end-to-end**: *rimando* — non c'è ancora superficie utente; le voci del registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) arrivano con le storie `0036` e
      `0037`, proprietarie del percorso `[J-MAGAZZINO]`;
- [ ] **traduzioni** di unità di misura e categorie predefinite presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la voce del testo libero e le colonne di
      controllo, campi annotati `@PersonalData`, tabella `articolo` presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta di **non** avere una colonna di quantità e il perché;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione esposta in questa storia;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0001` | Servizio, schema `app_magazzino` e cartella delle migrazioni devono esistere |

## 7. Fuori ambito

- **Giacenze e movimenti**: sono l'epica 03 e nascono con la storia `0013`. Qui non esiste alcun saldo.
- Codici a barre e codice GTIN dell'articolo: storia `0007`.
- Depositi e ubicazioni: storia `0008`.
- Modifica, archiviazione e riattivazione dalla interfaccia, con tutte le regole d'anagrafica: storia `0006`.
- Applicazione del tetto di `articoli_gestiti`: storia `0004`.
- Importazione dell'anagrafica da file: storia `0011`.

## 8. Punti aperti

- **Elenco delle categorie predefinite**: quali proporre a un account nuovo è una scelta di prodotto minore ma
  visibile (un installatore e un negozio di abbigliamento non hanno le stesse categorie). La proposta è un insieme
  neutro e modificabile; la chiude lo sviluppatore nella storia `0006`.
- **Articoli archiviati e codice interno**: se un codice liberato da un articolo archiviato debba poter essere
  riusato da un articolo nuovo non è deciso. La proposta è **no** finché l'articolo esiste, perché riusare un codice
  rende ambigua la lettura dello storico dei movimenti; va confermato nella storia `0006`.
