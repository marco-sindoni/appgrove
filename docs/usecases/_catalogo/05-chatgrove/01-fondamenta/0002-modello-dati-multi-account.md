# 0002 — Modello dati multi-account

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio
> voglio che i miei contatti e le mie conversazioni siano solo miei
> così da poter usare l'app senza il dubbio che un altro negozio veda i miei clienti.

**Contesto.** Le tre entità centrali dell'app — contatto, conversazione, messaggio — contengono i dati più
delicati che tratteremo: numeri di telefono e contenuto di comunicazioni di persone che non sono nostri
clienti. Vanno create insieme, con l'isolamento fra account già dentro, e insieme al loro posto nel manifesto
dei dati e nel contratto di esportazione e cancellazione. Farlo dopo significherebbe rincorrere: una tabella
con dati personali dimenticata nella cancellazione è il difetto di conformità più probabile di un'app nuova.

## 2. Requisiti funzionali

1. **RF-1** — Esistono le tabelle `contact`, `conversation` e `message` nello schema `app_chat_commerce`, con
   le colonne descritte nel modello di dominio della descrizione dell'applicazione.
2. **RF-2** — Un contatto è identificato dalla coppia (account, numero di telefono) e **non** ha il numero
   come chiave primaria: la chiave è un identificativo tecnico, così che il numero si possa cancellare davvero.
3. **RF-3** — Una conversazione appartiene a un contatto e porta il proprio stato, l'utente che l'ha presa in
   carico e la scadenza della finestra di servizio.
4. **RF-4** — Un messaggio porta direzione (in entrata o in uscita), tipo, corpo, identificativo presso il
   fornitore del canale e stato di consegna.
5. **RF-5** — Il contratto dati dell'app (`ChatCommerceDataContract`) esporta e cancella queste tre tabelle,
   ed è predisposto per accogliere quelle delle epiche successive.
6. **RF-6** — Il manifesto dei dati dichiara ogni campo che riguarda una persona, in italiano e inglese.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `contact`, `conversation` e `message`
  filtra per `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o
  dai parametri viene ignorato.
- **RT-2 — Persistenza (§8).** Migrazione `V1__contatti_conversazioni_messaggi.sql` sullo schema
  `app_chat_commerce`: tabelle con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo
  (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica (`deleted_at`). Nessuna
  chiave esterna verso altri schemi: il riferimento all'utente che prende in carico è **logico**.
- **RT-3 — Dati personali (§10).** Voci nuove nel manifesto `docs/compliance/manifests/chat_commerce.yaml` in
  italiano e inglese per `contact.phone`, `contact.display_name`, `message.body` e
  `conversation.assignee`; campi annotati `@PersonalData`; le tre tabelle aggiunte a `exportData` e
  `purgeData`. La cancellazione è **fisica**: sostituire il numero con un codice non è cancellare.
- **RT-4 — Registrazione eventi (§14).** Gli eventi `contatto creato` e `conversazione aperta` sono registrati
  con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza** numero di telefono e senza
  corpo del messaggio.
- **RT-5 — Prove (§11).** Prova di integrazione con database effimero e migrazioni vere; prova di isolamento
  fra due account su ognuna delle tre entità.
- **RT-6 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica in questa storia oltre a quelle di sola
  lettura minime necessarie alle prove: le rotte del dominio nascono nelle epiche successive.

## 4. Criteri di accettazione

**CA-1 — Le migrazioni si applicano**
- **Dato** un database vuoto
- **Quando** si esegue `dev migrate` per l'app
- **Allora** lo schema `app_chat_commerce` contiene le tre tabelle con tutte le colonne di controllo

**CA-2 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri contatti
- **Quando** un utente di `A` legge l'elenco dei contatti
- **Allora** vede solo i propri, anche se forza l'identificativo dell'account `B` nella richiesta

**CA-3 — Stesso numero, due account**
- **Dato** che la stessa persona scrive a due negozi diversi
- **Quando** entrambi i negozi la registrano come contatto
- **Allora** esistono due contatti distinti, uno per account, senza conflitto di unicità

**CA-4 — La cancellazione è fisica**
- **Dato** un contatto con messaggi
- **Quando** si esegue la cancellazione prevista dal contratto dati per quel contatto
- **Allora** le righe non esistono più nel database e resta una riga di prova nel registro delle purghe;
  nessun campo è stato semplicemente sostituito con un codice

**CA-5 — L'esportazione è completa**
- **Dato** un account con contatti, conversazioni e messaggi
- **Quando** si esegue l'esportazione prevista dal contratto dati
- **Allora** l'esito contiene tutte e tre le tabelle, e ogni campo dichiarato nel manifesto compare

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sulla normalizzazione del numero di telefono e di **integrazione** sulle tre tabelle,
      con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su tutte e tre le entità;
- [ ] **prova end-to-end**: *nessun impatto* — non c'è ancora superficie utente;
- [ ] **traduzioni**: non applicabile (nessun testo visibile);
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, campi annotati, tabelle presenti in esportazione
      e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta della chiave del contatto e la durata di
      conservazione proposta;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione utente introdotta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0001` | Serve il servizio e lo schema |

## 7. Fuori ambito

- la connessione al canale e la ricezione dei messaggi veri: epica 02;
- le schermate: la storia `0003` fa il guscio, l'elenco dei contatti è la storia `0021`;
- prodotti, ordini e pagamenti: epiche 03 e 04, con le loro migrazioni.

## 8. Punti aperti

- **Durata di conservazione del corpo dei messaggi**: proposti 12 mesi (§6 della descrizione
  dell'applicazione), da confermare dallo sviluppatore. Il valore entra nel manifesto e va deciso **prima** di
  andare in produzione, non dopo.
- **Se conservare affatto il corpo dei messaggi** o tenere solo i metadati: è una scelta che cambia l'utilità
  dell'app e il rischio; la decisione è dello sviluppatore.
