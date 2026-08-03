# 0002 — Magazzino dei fatti e modello dati multi-account

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio che i numeri che arrivano dalle altre applicazioni abbiano un posto dove stare, intestato all'account e
> impossibile da leggere per chi non è quell'account
> così da poter costruire cruscotti e risposte sopra una base che non può mescolare i dati di due clienti.

**Contesto.** InsightGrove è l'unica app della suite che raccoglie numeri prodotti altrove: se il suo magazzino
fosse mal disegnato, sarebbe **la scorciatoia che aggira l'isolamento fra account** — il rischio dichiarato al
§4.2 della [descrizione](../application-description.md). Questa storia fissa la forma del magazzino: una tabella
`fatto` **in sola aggiunta**, con `tenant_id` su ogni riga, letta solo con il filtro per account preso dal
gettone verificato. La forma della tabella decide tutto ciò che viene dopo: gli indicatori si calcolano su di
essa, e la tracciabilità del numero (storia 0016) risale i suoi record.

## 2. Requisiti funzionali

1. **RF-1** — Esiste la tabella `fatto` con: `tenant_id`, app d'origine, chiave della misura, periodo di
   competenza (inizio e fine), dimensioni, valore numerico, unità, momento dell'evento, chiave di idempotenza,
   riferimento alla riga d'origine.
2. **RF-2** — La tabella `fatto` è **in sola aggiunta**: nessuna operazione dell'applicazione la modifica o la
   cancella riga per riga. Una correzione si esprime pubblicando un fatto nuovo con la stessa chiave di
   idempotenza; la cancellazione esiste solo per la revoca di una fonte e per i diritti dell'interessato.
3. **RF-3** — Esiste il vincolo di unicità su `(tenant_id, app_origine, chiave_idempotenza)`: lo stesso fatto
   consegnato due volte non produce due righe.
4. **RF-4** — Esistono le tabelle `fonte` (una fonte collegata per account) e `etichetta_dimensione`, entrambe
   con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
5. **RF-5** — Esiste un livello di accesso ai dati (modello *repository*) che **non espone alcun metodo di
   lettura senza `tenant_id`**: la firma stessa dei metodi richiede il contesto del tenant.
6. **RF-6** — Gli indici sostengono le due letture che questa app fa sempre: per account, misura e periodo; e per
   account, fonte e momento dell'evento.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura delle entità `fatto`, `fonte` e
  `etichetta_dimensione` filtra per `tenant_id` preso dal gettone verificato; un `tenant_id` che arrivasse dal
  corpo della richiesta o dai parametri viene ignorato. **Nota specifica di questa app**: sulla scrittura dei
  fatti il `tenant_id` proviene dal messaggio dell'evento (storia 0007), mai da una richiesta HTTP — e le due
  strade sono separate nel codice, così che non ci sia un solo punto in cui un `tenant_id` di provenienza esterna
  possa entrare nel percorso di lettura.
- **RT-3 — Persistenza (§8).** Migrazione `V2__magazzino_dei_fatti.sql` sullo schema `app_insights`: tabelle
  `fatto`, `fonte`, `etichetta_dimensione` con `tenant_id`, chiave primaria UUID versione 7 generata
  dall'applicazione, colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e
  cancellazione logica (`deleted_at`) — con l'**eccezione motivata** della tabella `fatto`, che non ha
  `updated_at`, `updated_by` né `deleted_at` perché è in sola aggiunta. L'eccezione va scritta in
  `decisions.json`: è una deviazione consapevole dalla forma standard, non una dimenticanza.
- **RT-8 — Dati personali (§10).** La tabella `etichetta_dimensione` **potrebbe** contenere dati personali
  (il nome di un cliente dell'account): la storia la crea, ma la decisione se popolarla è aperta e appartiene
  alla storia 0006 (punto aperto 2 della descrizione). Finché la via non è scelta, la tabella resta vuota e la
  voce nel manifesto è scritta **in bozza**, non pubblicata. Le tabelle `fatto` e `fonte` non contengono dati
  personali oltre a `created_by`/`updated_by`.
- **RT-14 — Registrazione eventi (§14).** Le operazioni sul magazzino registrano `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione; **mai** il contenuto delle dimensioni né le etichette.
- **RT-11 — Prove (§11).** Prove di integrazione con database effimero e migrazioni vere; **prova di isolamento
  fra due account** su tutte e tre le tabelle, compreso il tentativo di forzare un `tenant_id` altrui.

## 4. Criteri di accettazione

**CA-1 — Le migrazioni creano il magazzino**
- **Dato** un database effimero vuoto
- **Quando** si applicano le migrazioni Flyway
- **Allora** esistono nello schema `app_insights` le tabelle `fatto`, `fonte` ed `etichetta_dimensione`, ciascuna
  con la colonna `tenant_id` non nulla

**CA-2 — Lo stesso fatto due volte resta uno**
- **Dato** un fatto già scritto per l'account `A` con chiave di idempotenza `k`
- **Quando** si tenta di scrivere un secondo fatto con lo stesso account, la stessa app d'origine e la stessa `k`
- **Allora** la riga resta una sola e la seconda scrittura non genera errore verso il chiamante

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri fatti sulla stessa misura e sullo stesso periodo
- **Quando** un utente di `A` interroga il magazzino
- **Allora** vede solo i fatti di `A`, anche se forza l'identificativo dell'account `B` nel corpo della richiesta
  o in una intestazione

**CA-4 — Il fatto non si modifica**
- **Dato** un fatto già scritto
- **Quando** si prova a modificarlo attraverso il livello di accesso ai dati
- **Allora** l'operazione non esiste: il *repository* dei fatti non espone alcun metodo di aggiornamento o
  cancellazione riga per riga

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla generazione della chiave di idempotenza e di **integrazione** sulle migrazioni,
      con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su tutte e tre le tabelle introdotte;
- [ ] **prova end-to-end**: *nessun impatto* — la storia non tocca superficie utente;
- [ ] **traduzioni**: non applicabile;
- [ ] **manifesto dei dati** aggiornato con la voce **in bozza** per `etichetta_dimensione.etichetta`, in
      italiano e inglese, e con il punto aperto scritto;
- [ ] tabelle aggiunte a `exportData` e `purgeData` del contratto `InsightsDataContract`;
- [ ] **registro delle decisioni** compilato, con l'eccezione della tabella in sola aggiunta e il suo perché;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | il servizio e lo schema devono esistere |

## 7. Fuori ambito

- il contratto dell'evento che porta il fatto: è la storia 0006 — qui si definisce **dove il fatto atterra**,
  non **come arriva**;
- il consumo dalla coda: storia 0007;
- il calcolo degli indicatori sopra i fatti: epica 03.

## 8. Punti aperti

- **La forma delle dimensioni**: coppie chiave-valore in una colonna semi-strutturata oppure tabella dedicata.
  La prima è più semplice e sufficiente ai volumi di una micro-impresa; la seconda regge meglio le
  interrogazioni per dimensione. Raccomandazione: colonna semi-strutturata con indice, rivedibile senza cambiare
  il contratto esterno. Chiude: **sviluppatore**, in sede di implementazione.
- **Le etichette di dimensione si popolano o no** — punto aperto 2 della descrizione, chiuso dallo sviluppatore
  prima della storia 0006.
