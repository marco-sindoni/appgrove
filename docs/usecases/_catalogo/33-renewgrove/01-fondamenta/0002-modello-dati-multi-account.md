# 0002 — Modello dati multi-account

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che sta per far entrare in questa app fatti riferiti ai clienti di qualcun altro
> voglio che le tabelle nascano già con l'account addosso e con lo storico impossibile da riscrivere
> così da non dover rincorrere l'isolamento fra account e la tracciabilità quando ci saranno dentro dei dati veri.

**Contesto.** Il servizio esiste (`0001`) ma il suo schema è vuoto. Questa storia posa le tre tabelle da cui parte
tutto il resto — `fonte`, `rapporto`, `segnale` — e, cosa che conta più della forma delle colonne, **posa le regole
di scrittura** che tutte le tabelle successive erediteranno. Ce n'è una che non è la regola generale di piattaforma
e va motivata subito: `segnale` è **in sola aggiunta**, e lo sarà anche `punteggio` quando arriverà (epica 03).
Il motivo è nell'epica 05: uno storico che si riscrive all'indietro non permette di misurare se un intervento è
servito, e non regge una contestazione da parte di chi il giudizio lo subisce (§4.4 della
[descrizione](../application-description.md)). Correggere un segnale sbagliato è possibile, ma è **una riga nuova**,
non un aggiornamento.

## 2. Requisiti funzionali

1. **RF-1** — Esistono le migrazioni Flyway sullo schema `app_fidelizzazione` che creano `fonte`, `rapporto` e
   `segnale`, applicate dalle prove con database effimero e migrazioni vere.
2. **RF-2** — Ogni tabella porta `tenant_id` non nullo, chiave primaria UUID versione 7 generata
   dall'applicazione, colonne di controllo `created_at`, `updated_at`, `created_by`, `updated_by` e cancellazione
   logica `deleted_at`.
3. **RF-3** — `segnale` è **in sola aggiunta**: nessun percorso applicativo aggiorna una riga esistente; la
   correzione di un fatto è una riga nuova che rende superata la precedente. La stessa regola è dichiarata come
   vincolante per `punteggio`, che nascerà nell'epica 03.
4. **RF-4** — `rapporto` è identificato, dentro un account, dalla coppia `(app_origine, riferimento_opaco)`, con
   un vincolo di unicità che comprende `tenant_id`. Il popolamento della coppia è della storia `0009`.
5. **RF-5** — Nessuna chiave esterna verso altri schemi e nessuna interrogazione fra schemi diversi: `tenant_id` e
   i riferimenti alle righe d'origine delle altre app sono riferimenti **logici**, opachi, non risolvibili da qui.
6. **RF-6** — Il servizio si collega al database con un ruolo che ha privilegi **solo** su `app_fidelizzazione`:
   un tentativo di leggere lo schema di un'altra app fallisce per permessi mancanti, non per convenzione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `fonte`, `rapporto` e `segnale` filtra per
  `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri
  viene ignorato. Il caso della scrittura a partire da un evento — dove il `tenant_id` arriva da un'altra strada —
  è della storia `0007`, e questa storia gli prepara il terreno rendendo la colonna obbligatoria.
- **RT-2 — Persistenza (§8).** Migrazioni `V2__fonte.sql`, `V3__rapporto.sql`, `V4__segnale.sql` sullo schema
  `app_fidelizzazione`, in SQL, versionate, **non** applicate all'avvio in produzione. Indici su
  `(tenant_id, app_origine, riferimento_opaco)` per `rapporto` e su
  `(tenant_id, app_origine, chiave_idempotenza)` per `segnale`, con unicità.
- **RT-3 — Interfaccia di programmazione (§2).** Nessuna rotta nuova: la storia si ferma allo strato di
  persistenza e ai repository. Le entità non escono mai dal bordo, che arriverà con le storie di dominio.
- **RT-4 — Dati personali (§10).** La storia crea le colonne che **conterranno** dati riferiti a persone anche se
  restano vuote fino alla storia `0009`: `rapporto.etichetta` (di norma un nome o una ragione sociale) e la coppia
  `rapporto.app_origine` + `rapporto.riferimento_opaco` (identificativo opaco del cliente finale). Poiché un campo
  annotato `@PersonalData` e non dichiarato fa fallire la compilazione, le voci corrispondenti entrano **adesso**
  nel manifesto `docs/compliance/manifests/fidelizzazione.yaml` in italiano e inglese, e le tabelle `rapporto` e
  `segnale` entrano **adesso** in `exportData` e `purgeData` di `FidelizzazioneDataContract`. `fonte` non contiene
  dati riferiti a clienti finali e resta fuori da entrambi.
- **RT-5 — Registrazione eventi (§14).** Le migrazioni e gli scarti per violazione di vincolo si registrano con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza contenuti.
- **RT-6 — Esposizione conversazionale (§12).** Nessuno strumento introdotto: la storia non espone funzioni.
  Dipendenza dichiarata comunque per le storie a valle: UC 0061-0063, non ancora implementati.
- **RT-7 — Prove (§11).** Integrazione con PostgreSQL 17 effimero e migrazioni Flyway vere; prova di isolamento
  fra due account su ciascuna delle tre tabelle; prova che un aggiornamento di una riga di `segnale` non è
  possibile attraverso il repository.

## 4. Criteri di accettazione

**CA-1 — Le migrazioni girano su un database vuoto**
- **Dato** un database effimero senza lo schema
- **Quando** si eseguono le migrazioni
- **Allora** `app_fidelizzazione` contiene `fonte`, `rapporto` e `segnale`, ciascuna con `tenant_id`, chiave
  primaria UUID versione 7, le quattro colonne di controllo e `deleted_at`

**CA-2 — Isolamento fra due account**
- **Dato** due account `A` e `B`, ciascuno con i propri rapporti e segnali
- **Quando** un utente di `A` chiede l'elenco dei propri rapporti
- **Allora** vede solo i propri, anche se forza l'identificativo dell'account di `B` nel corpo della richiesta

**CA-3 — Un segnale non si aggiorna**
- **Dato** un segnale già scritto
- **Quando** un percorso applicativo prova a modificarne l'intensità
- **Allora** l'operazione è respinta e la correzione è possibile solo scrivendo una riga nuova che rende superata
  la precedente

**CA-4 — Rapporto univoco dentro l'account**
- **Dato** un account con un rapporto `(app_origine = abbonati, riferimento_opaco = r-1042)`
- **Quando** si prova a inserirne un secondo con la stessa coppia
- **Allora** l'inserimento è respinto dal vincolo di unicità, mentre lo stesso inserimento in un altro account
  riesce

**CA-5 — Nessun accesso allo schema altrui**
- **Dato** il ruolo del database del servizio `fidelizzazione`
- **Quando** si tenta una lettura su uno schema di un'altra app
- **Allora** il database la rifiuta per permessi mancanti

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla generazione delle chiavi e sulla regola di sola aggiunta, e di **integrazione**
      sulle migrazioni, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su tutte e tre le tabelle introdotte;
- [ ] **prova end-to-end**: *rimando* — nessuna superficie utente ancora; il percorso `[J-FIDELIZZAZIONE]` nasce
      con la storia `0030` e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta la voce `da-coprire`;
- [ ] **traduzioni**: nessun testo visibile in questa storia;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con le voci `rapporto.etichetta` e
      `rapporto.riferimento_origine`, campi annotati `@PersonalData`, tabelle `rapporto` e `segnale` presenti in
      `exportData` e `purgeData`;
- [ ] **registro delle decisioni** compilato: la sola aggiunta di `segnale` e `punteggio` come eccezione motivata,
      e la scelta di dichiarare subito nel manifesto colonne ancora vuote;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | serve il servizio, il suo schema e il suo ruolo di database |
| classificazione dei dati personali (§6 della descrizione, punto aperto n. 4) | l'etichetta leggibile è un dato personale e la sua base giuridica è una fermata di escalation: va chiusa **prima** che la colonna si popoli (storia `0009`) |

## 7. Fuori ambito

- il contratto del segnale, cioè che cosa un segnale può contenere: storia `0006`;
- la scrittura dei segnali a partire dagli eventi: storia `0007`;
- il popolamento dell'etichetta e gli stati di sorveglianza: storia `0009`;
- le tabelle di punteggio, intervento ed esito: epiche 03, 04 e 05, che le creano quando servono. Qui si dichiara
  soltanto la regola di sola aggiunta che `punteggio` dovrà rispettare.

## 8. Punti aperti

- **La finestra di conservazione di 24 mesi su `segnale`** (§6 della descrizione, punto aperto n. 9) è una
  proposta prudente, non un dato: dipende dalla base giuridica scelta e dalla durata tipica dei rapporti nel
  settore del cliente. La migrazione non la cabla: la conservazione si applica con la purga, non con un vincolo di
  tabella. Chiude: revisione legale.
- **`fonte` resta davvero fuori da esportazione e cancellazione?** Contiene chi ha collegato la fonte e quando —
  un dato di un **nostro** utente, già trattato dalla piattaforma, non del cliente finale. La proposta è di sì.
  Chiude: lo sviluppatore, insieme al manifesto.
