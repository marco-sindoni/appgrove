# 0029 — Strumenti di scrittura con conferma

**Applicazione**: 33 — RenewGrove (`fidelizzazione`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`, `0022` — la macchina a stati dell'intervento e l'offerta di trattenuta sono ciò che qui si comanda
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che dalla chat dice «preparami una mail per il cliente Aurora, che sta scivolando»
> voglio ottenere **una bozza** e nient'altro, e dover confermare io, con un gesto separato, se e quando quella mail
> deve uscire
> così da non scoprire il giorno dopo che un assistente ha scritto a un mio cliente al posto mio.

**Contesto.** Questa è la storia in cui il presidio più importante dell'app incontra la superficie in cui è più
facile violarlo. La [descrizione](../application-description.md) al §7 lo dice con una frase che vale come requisito:
la chat è «la superficie in cui è più facile confondere *scrivimi una bozza* con *mandagliela*». La simmetria da
tenere ferma è netta: **preparare** un intervento è una scrittura ordinaria, **farlo uscire** è irreversibile e non
si concede mai senza una persona. La macchina a stati del §4.4 non ha alcun passaggio automatico da `bozza`, e
nessuna configurazione lo abilita; qui quella regola va portata pari pari sugli strumenti, invece di essere
riscritta più permissiva perché «tanto la chat è comoda». Il livello conversazionale **non esiste ancora** nel
repository (UC 0061-0066): si dichiara il contratto, che vive dentro il servizio.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara i **sette strumenti di scrittura** della tabella §7 della descrizione:
   `marca_segnale_non_pertinente(segnale, motivo)`, `escludi_rapporto(rapporto, motivo)`,
   `prepara_intervento(rapporto, piano?)`, `conferma_intervento(intervento)`,
   `autorizza_offerta(intervento, tipo, valore, validità)`, `collega_fonte(app)`, `scollega_fonte(app)` — ciascuno
   con nome stabile, descrizione in lingua naturale, schema dei parametri, schema del risultato e marcatura
   **scrittura**.
2. **RF-2** — **Tutti producono una bozza** e nessuno esegue: il risultato di ogni strumento è un identificativo di
   bozza, l'effetto esatto che avrà, e una scadenza breve oltre la quale la bozza decade. Nessuno strumento di
   scrittura ha una variante «esegui subito», in nessuna configurazione.
3. **RF-3** — **Quattro** hanno **conferma obbligatoria**, perché irreversibili o con effetti verso l'esterno:
   `conferma_intervento` (fa uscire qualcosa verso una persona che non è nostro utente), `autorizza_offerta`
   (impegna denaro del cliente verso un terzo), `collega_fonte` e `scollega_fonte` (aprono o chiudono un flusso di
   dati personali, e lo scollegamento **cancella**). Per questi quattro la conferma è un **atto separato compiuto da
   una persona identificata**, e non esiste impostazione, ruolo o parametro che la disattivi.
4. **RF-4** — Prima della conferma la bozza mostra **l'effetto esatto**, non una descrizione generica: per
   `conferma_intervento` il rapporto destinatario, il canale e il testo che uscirà; per `autorizza_offerta` tipo,
   valore, validità e chi autorizza; per `scollega_fonte` **quanti segnali verranno cancellati** e quali punteggi
   smetteranno di essere calcolabili; per `marca_segnale_non_pertinente` il punteggio ricalcolato **accanto** a
   quello attuale.
5. **RF-5** — La simmetria prepara/fa-uscire è strutturale: `prepara_intervento` lascia l'intervento in `bozza` e
   **non può** portarlo oltre; solo la conferma di `conferma_intervento` compie il passaggio a `confermato`. Ogni
   tentativo, per qualunque via, di far uscire un intervento dallo stato `bozza` senza un identificativo utente
   fallisce (la prova che lo dimostra è della storia `0031`).
6. **RF-6** — Le conferme sono **idempotenti**: confermare due volte la stessa bozza produce un solo effetto; una
   bozza **scaduta**, **già confermata** o **annullata** risponde con un errore che dice quale dei tre casi è, e non
   esegue nulla.
7. **RF-7** — Gli strumenti attraversano gli stessi varchi delle rotte web: `tenant_id` dal token verificato della
   chiamata, `402` con abbonamento non attivo, `403` per ruolo insufficiente — `collega_fonte` e `scollega_fonte`
   sono riservati a `owner` e `admin`, un `member` vede le fonti in sola lettura — e `429` quando l'azione farebbe
   superare il tetto di `rapporti_sorvegliati`. `escludi_rapporto`, alla conferma, **libera** una unità della
   metrica.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Bozza e conferma appartengono allo stesso `tenant_id`, preso dal token
  verificato; una bozza creata in un account non è confermabile da un altro, nemmeno conoscendone l'identificativo,
  e il tentativo produce «non trovato». Nessun parametro di strumento accetta un identificativo di account.
- **RT-2 — Interfaccia di programmazione (§2).** Le bozze riusano le rotte esistenti
  `/api/fidelizzazione/v1/*` per l'effetto, e aggiungono `POST /api/fidelizzazione/v1/bozze/{id}/conferma` e
  `POST /api/fidelizzazione/v1/bozze/{id}/annullamento`; corpo validato; errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit. Nessuna logica di dominio duplicata dentro lo strato degli
  strumenti.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__bozza_di_strumento.sql` sullo schema `app_fidelizzazione`: tabella
  `bozza_di_strumento` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione
  logica; porta lo strumento, i parametri validati, l'effetto calcolato, la scadenza, lo stato (in attesa /
  confermata / annullata / scaduta) e **chi ha confermato**. La conferma è la sola transizione che scrive un
  identificativo utente, ed è obbligatoria per i quattro strumenti del **RF-3**.
- **RT-4 — Modulo frontend (§3, §5).** Sezione **Bozze in attesa** del modulo `fidelizzazione`: l'elenco delle bozze
  aperte con il loro effetto, i pulsanti di conferma e annullamento; solo token del sistema di design; tema chiaro e
  scuro. È la schermata che rende visibile ciò che un assistente ha preparato: una bozza che esiste solo dentro una
  conversazione è una bozza che nessuno rivede.
- **RT-5 — Cinque lingue (§4).** Descrizioni degli strumenti, testi dell'effetto, messaggi di errore e schermata
  delle bozze passano dallo spazio-nomi `fidelizzazione` e sono presenti in `en, it, fr, es, de`. I nomi degli
  strumenti restano stabili e non tradotti.
- **RT-6 — Varchi e quota (§6, §7).** Come al **RF-7**. La verifica di quota avviene **alla conferma**, non alla
  creazione della bozza: una bozza non è un rapporto sorvegliato, e rifiutare di preparare qualcosa che poi non si
  potrà comunque eseguire va detto **prima**, mostrando nell'effetto che la quota è insufficiente.
- **RT-7 — Esposizione conversazionale (§12).** Sette nomi dichiarati, tutti marcati **scrittura**, tutti con
  **bozza**; quattro con **conferma umana obbligatoria** (`conferma_intervento`, `autorizza_offerta`,
  `collega_fonte`, `scollega_fonte`). `conferma_intervento` mostra **sempre i tre fatti principali** che hanno
  formato il punteggio, così che confermare significhi almeno averli visti — attenuazione dichiarata come
  **parziale** al §11 della descrizione, non come difesa completa. Il contratto vive dentro il servizio; il server
  conversazionale è di piattaforma e **non ancora implementato** (UC 0061-0063), perciò le prove esercitano bozza e
  conferma **chiamando il servizio direttamente**: va scritto nel registro delle decisioni che il collegamento andrà
  rifatto passando dal server.
- **RT-8 — Dati personali (§10).** La storia **non introduce categorie nuove**, ma introduce una tabella che
  contiene dati riferiti a persone: `bozza_di_strumento` conserva i parametri e l'effetto, e per
  `conferma_intervento` questo comprende il **contenuto proposto** verso un cliente finale. Voce nuova nel manifesto
  `docs/compliance/manifests/fidelizzazione.yaml` in **italiano e inglese**; campi annotati `@PersonalData`; tabella
  aggiunta a `exportData` e `purgeData` del contratto `FidelizzazioneDataContract`. Le bozze **scadute o annullate**
  si cancellano fisicamente alla scadenza dichiarata: una bozza mai eseguita che resta per sempre è una copia di
  dati personali senza scopo.
- **RT-9 — Registrazione eventi (§14).** `bozza creata (strumento)`, `bozza confermata (strumento)`, `bozza
  annullata`, `bozza scaduta`, `conferma negata per ruolo`, `conferma negata per quota`, con `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione, **senza** il contenuto della bozza e senza etichette di rapporti.
- **RT-10 — Prove (§11).** Unità sull'idempotenza della conferma e sul calcolo dell'effetto mostrato; integrazione
  su tutti e sette i nomi con database effimero e migrazioni vere; prova che nessuno dei quattro strumenti a
  conferma obbligatoria esegua senza identificativo utente; prova di isolamento fra due account su bozza e conferma.

## 4. Criteri di accettazione

**CA-1 — Preparare non è mandare**
- **Dato** un rapporto in fascia alta e un piano di intervento
- **Quando** si invoca `prepara_intervento`
- **Allora** nasce un intervento in stato `bozza`, nulla esce verso il cliente finale, e il risultato è una bozza di
  strumento con il proprio identificativo e la propria scadenza

**CA-2 — Far uscire richiede una persona**
- **Dato** l'intervento in `bozza` del caso precedente
- **Quando** si invoca `conferma_intervento` e si tenta di completarla senza il passaggio di conferma di un utente
  identificato
- **Allora** l'intervento resta in `bozza`, nulla è consegnato, e la risposta dice che serve una conferma umana

**CA-3 — L'effetto è mostrato prima, per intero**
- **Dato** una fonte collegata con 1.240 segnali
- **Quando** si invoca `scollega_fonte`
- **Allora** la bozza dichiara quanti segnali verranno cancellati e quali punteggi smetteranno di essere
  calcolabili, e solo dopo la conferma esplicita di un `owner` o `admin` la cancellazione avviene

**CA-4 — Conferma idempotente e bozze morte**
- **Dato** una bozza di `autorizza_offerta` già confermata e una bozza scaduta
- **Quando** si confermano entrambe
- **Allora** la prima risponde che è già stata confermata e non produce una seconda offerta; la seconda risponde che
  è scaduta; in nessuno dei due casi qualcosa cambia

**CA-5 — Ruolo e quota**
- **Dato** un utente `member` e un account che ha raggiunto il tetto di `rapporti_sorvegliati`
- **Quando** il `member` invoca `collega_fonte`, e un `owner` conferma una bozza che aggiungerebbe un rapporto
  sorvegliato
- **Allora** il primo riceve `403`, il secondo `429` con l'indicazione del rimedio, e in nessuno dei due casi
  qualcosa viene creato

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con bozze in attesa
- **Quando** un utente di `B` tenta di confermare una bozza di `A` conoscendone l'identificativo
- **Allora** riceve «non trovato», la bozza di `A` resta in attesa e nessun effetto si produce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sull'idempotenza e sul calcolo dell'effetto, e di **integrazione** su tutti e sette i nomi,
      con database effimero e migrazioni vere;
- [ ] prova che i quattro strumenti a conferma obbligatoria **non** eseguano senza un identificativo utente;
- [ ] prova di **isolamento fra account** su creazione e conferma delle bozze;
- [ ] **prova end-to-end**: *rimando* — il server conversazionale non esiste (UC 0061-0063); il percorso
      `[J-FIDELIZZAZIONE]` della storia `0030` esercita bozza e conferma chiamando il servizio direttamente, e il
      registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta la voce `da-coprire`
      con motivo («livello conversazionale di piattaforma non implementato») e storia proprietaria `0030`;
- [ ] **traduzioni** di descrizioni, effetti, errori e schermata delle bozze in `en, it, fr, es, de`;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `bozza_di_strumento`, campi annotati
      `@PersonalData`, tabella in `exportData` e `purgeData`, e la cancellazione fisica delle bozze morte;
- [ ] registro dei trattamenti rigenerato dal manifesto nello stesso commit;
- [ ] **registro delle decisioni** compilato: quali quattro nomi hanno conferma obbligatoria e perché, scadenza
      delle bozze, verifica di quota alla conferma e non alla creazione, nota che il collegamento andrà rifatto
      quando il server esisterà;
- [ ] contratto degli **strumenti conversazionali** dichiarato per tutti e sette i nomi, con prova di coerenza
      contratto↔implementazione;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la descrizione elenca gli strumenti (§7) e la macchina a stati (§4.4).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` (intervento con conferma umana) | la macchina a stati e il presidio «da `bozza` non si esce senza una persona» nascono là: qui si espongono, non si riscrivono |
| storia `0022` (offerte di trattenuta e loro limiti) | `autorizza_offerta` deve rispettare tetti, autorizzazione e il divieto di frapporsi al percorso di disdetta |
| storie `0008`, `0015` | collegamento/revoca di una fonte e contestazione del punteggio sono gli effetti degli altri strumenti |
| UC 0061-0063 (livello conversazionale di piattaforma) | **non implementati**: qui si dichiara solo il contratto, e le prove chiamano il servizio direttamente |

## 7. Fuori ambito

- l'applicazione di **abilitazione e quota alle chiamate dell'assistente** come strato di piattaforma (UC 0064):
  qui i varchi si applicano dentro l'app, come per le rotte web;
- la **sicurezza e il tracciamento** delle sessioni conversazionali (UC 0065): di piattaforma;
- gli **strumenti di lettura**: storia `0028`;
- qualunque strumento sui **diritti dell'interessato**: esportare o cancellare i dati di una persona su richiesta di
  una chat, anche con conferma, è il modo più veloce di cancellare i dati sbagliati. Si fa dall'interfaccia, con
  l'anteprima davanti (storia `0032`);
- la **generazione del testo** dell'intervento: la scrive il livello conversazionale o la persona; qui si garantisce
  che quel testo resti in bozza finché qualcuno non lo guarda.

## 8. Punti aperti

- **La durata di validità di una bozza.** Troppo corta e il titolare la trova scaduta quando torna dal cantiere;
  troppo lunga e diventa un magazzino di messaggi pronti a partire, con dentro dati di clienti finali. La proposta
  è una scadenza breve dichiarata a schermo, ma il valore è di prodotto. Chiude: **sviluppatore**.
- **Il rischio che la conferma diventi una formalità.** È scritto al §11 della
  [descrizione](../application-description.md) come rischio noto: se il titolare conferma tutto a occhi chiusi,
  l'intervento umano c'è sulla carta e non nei fatti. Mostrare i tre fatti principali (**RT-7**) è
  un'attenuazione **parziale e dichiarata come tale**; la difesa completa è organizzativa, non tecnica, e non
  fingiamo il contrario. Chiude: **sviluppatore**, con la **revisione legale** sul punto aperto n. 4.
