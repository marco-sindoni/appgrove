# 0013 — Registro dei movimenti e giacenza derivata

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 03 — Registro dei movimenti e giacenze
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0006`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore dell'applicazione
> voglio il registro dei movimenti in sola aggiunta e la giacenza calcolata da esso
> così da poter costruire carichi, scarichi, trasferimenti e inventari sopra un fondamento che non si può falsare.

**Contesto.** Oggi l'app sa cosa conta (`articolo`, storia `0006`) e dove lo tiene (`deposito` e `ubicazione`,
storia `0008`), ma non sa **quanto ce n'è**. Il modo in cui si risponde a quella domanda decide l'intera
applicazione, e va deciso adesso: ogni storia successiva scrive movimenti, e cambiare la forma del registro dopo
significa riscrivere il passato di ogni cliente. Il modello scelto — registro in sola aggiunta, giacenza derivata —
non è una raffinatezza tecnica ma la risposta al problema che la descrizione dell'applicazione mette al centro
(§1, §4): quando i conti non tornano, nessuno sa perché.

**Perché la giacenza non è un numero che si aggiorna.** Se la quantità fosse una colonna che qualcuno riscrive, una
differenza sarebbe un numero sbagliato e basta: non esisterebbe nulla da leggere per capire come ci si è arrivati.
Con il registro, la stessa differenza diventa una domanda con risposta — «il 12 sono entrati 24 pezzi, il 14 ne sono
usciti 30, quindi mancano 6 e li ha scaricati Anna con riferimento all'ordine 118». La stessa forma serve tre volte:
regge l'analisi interna quando un cliente contesta un saldo, regge l'importazione dei movimenti storici (storia
`0018`) e regge la ricostruzione della giacenza dal registro (storia `0024`), che è l'unica prova possibile che la
proiezione dica la verità.

**Perché la proiezione esiste lo stesso.** La domanda «quanti ce ne sono?» si fa mille volte al giorno, e sommare
cinque anni di movimenti a ogni domanda sarebbe assurdo. La riga di `giacenza` è quindi una **comodità di lettura**:
l'autorità resta al registro, e quando le due divergono la verità è il registro. È per questo che la proiezione
porta una **versione** — serve a serializzare le scritture concorrenti (storia `0015`), a riconoscere un
aggiornamento perso e a ordinare gli eventi in uscita (storia `0020`).

**Perché l'aritmetica sta nella base di dati.** Inserimento del movimento e aggiornamento della riga di giacenza
avvengono nella **stessa transazione**, con un aggiornamento condizionato che somma il delta alla colonna. Non si
legge mai la quantità in memoria per poi riscriverla: quella sequenza (leggo 5, calcolo 5−3, scrivo 2) è
esattamente il modo in cui due scarichi da 3 su 5 pezzi lasciano 2 pezzi invece di rifiutarne uno. Qui si mette la
regola; la storia `0015` la mette alla prova.

## 2. Requisiti funzionali

1. **RF-1** — Esiste la tabella `movimento` sullo schema dell'app, con: `tipo` (elenco chiuso: `carico`, `scarico`,
   `trasferimento_uscita`, `trasferimento_entrata`, `rettifica`, `storno`), `articolo_id`, `deposito_id`,
   `ubicazione_id` facoltativo, `quantita` **con segno** (positiva per le entrate, negativa per le uscite),
   `motivo_codice`, `riferimento_documento` facoltativo, `chiave_idempotenza`, `avvenuto_il`, oltre alle colonne di
   controllo che portano l'autore.
2. **RF-2** — Il registro è **in sola aggiunta**: il servizio espone la creazione e la lettura dei movimenti e
   **non** espone modifica né cancellazione; qualunque tentativo di modificare o cancellare un movimento è respinto
   con `405`, con un messaggio che indirizza allo storno (storia `0017`).
3. **RF-3** — Esiste la tabella `giacenza`, con **una sola riga per coppia** `(articolo_id, deposito_id)` per
   account, che porta `quantita`, `versione`, `ultimo_movimento_id` e il momento dell'ultimo aggiornamento; la riga
   nasce al primo movimento della coppia.
4. **RF-4** — Inserimento del movimento e aggiornamento della riga di giacenza avvengono nella **stessa
   transazione**, con un aggiornamento condizionato in forma
   `SET quantita = quantita + :delta, versione = versione + 1`: se l'aggiornamento non tocca alcuna riga, la
   transazione fallisce per intero e nessun movimento resta scritto.
5. **RF-5** — Ogni movimento porta una `chiave_idempotenza` **univoca per account**: un secondo invio con la stessa
   chiave non crea un secondo movimento e restituisce il movimento già registrato con esito `200`, non un errore.
6. **RF-6** — Esiste la tabella `motivo_movimento` (`codice`, `etichetta`, `segno_ammesso` fra `positivo`,
   `negativo`, `entrambi`, `nota_obbligatoria`), popolata alla creazione dell'account con un insieme predefinito:
   acquisto, reso da cliente, produzione interna, vendita, consumo per lavoro, scarto o rottura, furto o
   smarrimento, differenza d'inventario, trasferimento. Un movimento con un motivo di segno incompatibile è
   respinto con `422`.
7. **RF-7** — Esistono le rotte di elenco e lettura dei movimenti (filtrabili per articolo, deposito, periodo, tipo)
   e di lettura delle giacenze, entrambe paginate a pagina/dimensione con totale.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `movimento`, `giacenza` e `motivo_movimento`
  filtra per `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai
  parametri viene ignorato. La chiave di idempotenza è univoca **per account**, non globale. Prova di isolamento fra
  due account su tutte e tre le risorse.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/magazzino/v1/movimenti`,
  `GET /api/magazzino/v1/movimenti/{id}`, `GET /api/magazzino/v1/giacenze`,
  `GET /api/magazzino/v1/motivi-movimento`; oggetti di trasferimento al bordo (le entità non si espongono mai);
  validazione dichiarativa; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso
  commit. Hibernate ORM **bloccante**, accesso ai dati con il modello *repository*.
- **RT-3 — Persistenza (§8).** Migrazione `V8__movimento_giacenza_motivo.sql` sullo schema `app_magazzino`: tre
  tabelle con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e `deleted_at`. Vincolo di unicità
  su `(tenant_id, chiave_idempotenza)` e su `(tenant_id, articolo_id, deposito_id)` per la giacenza; indici su
  `(tenant_id, articolo_id, avvenuto_il)` e `(tenant_id, deposito_id, avvenuto_il)`. Nessuna chiave esterna verso
  altri schemi. **Avvertenza sulla colonna `deleted_at` di `movimento`**: esiste perché è lo standard di
  piattaforma, ma l'applicazione **non la valorizza mai** — cancellare logicamente un movimento significherebbe
  cambiare il passato. Resta disponibile solo per la cancellazione fisica prevista dai diritti dell'interessato e
  dalla chiusura dell'account.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova: la storia si ferma al servizio. Le sezioni che
  mostrano movimenti e giacenze arrivano con le storie `0014` e `0015`.
- **RT-5 — Cinque lingue (§4).** Le etichette dei motivi predefiniti sono **chiavi**, non testo: la traduzione sta
  nello spazio-nomi `magazzino` in tutte e cinque le lingue (`en, it, fr, es, de`) e il cliente può rinominarle;
  un motivo creato dal cliente porta il testo che lui ha scritto e non si traduce.
- **RT-6 — Varchi e quota (§6, §7).** **Nessun consumo di quota**: la metrica `articoli_gestiti` (natura `stock`)
  è consumata solo dalla creazione di articoli (storia `0004`). Registrare un movimento **non consuma quota e non
  viene mai respinto con `429`**, nemmeno a tetto raggiunto: impedire di registrare uno scarico corromperebbe il
  saldo del cliente. Restano attivi gli altri varchi: `401` senza token, `402` con abbonamento `canceled`, `403`
  con ruolo insufficiente.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento dichiarato qui: `leggi_giacenza` e
  `storico_movimenti` sono della storia `0034`, gli strumenti di scrittura della storia `0035`. Il contratto vivrà
  dentro il servizio; il server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo.** L'unico dato che riguarda una persona è
  l'autore del movimento (`created_by`), già dichiarato nel manifesto dalla storia `0010`; la tabella `movimento`
  è già presente in `exportData` e `purgeData` del contratto `MagazzinoDataContract`. Il campo `nota` del movimento
  è testo libero e ricade nella voce già dichiarata. **Nessun indicatore di produttività per persona**: l'autore
  serve alla tracciabilità della merce, non a misurare chi lavora (descrizione dell'applicazione, §6).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `movimento registrato`, `movimento duplicato ignorato per
  idempotenza`, `aggiornamento della giacenza fallito` sono registrati con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione, **senza** note e **senza** il nome dell'autore: si scrive l'identificativo, non
  la persona.

## 4. Criteri di accettazione

**CA-1 — Il primo movimento crea la giacenza**
- **Dato** un articolo attivo e un deposito, senza alcun movimento
- **Quando** si registra un movimento di carico di 12 pezzi con motivo `acquisto`
- **Allora** esiste un movimento con quantità `+12` e una riga di giacenza per la coppia articolo-deposito con
  quantità `12`, `versione` `1` e `ultimo_movimento_id` uguale all'identificativo del movimento appena scritto

**CA-2 — Il registro non si modifica e non si cancella**
- **Dato** un movimento già registrato
- **Quando** si tenta di modificarlo o di cancellarlo tramite l'interfaccia di programmazione
- **Allora** la risposta è `405` in `application/problem+json`, con un messaggio che indica lo storno come unica
  correzione possibile, e il movimento resta identico

**CA-3 — Idempotenza dell'invio ripetuto**
- **Dato** un movimento di carico già registrato con chiave di idempotenza `abc-123`
- **Quando** arriva un secondo invio identico con la stessa chiave
- **Allora** la risposta è `200` con il movimento già esistente, non viene creato un secondo movimento e la
  giacenza resta invariata

**CA-4 — Motivo con segno incompatibile**
- **Dato** il motivo `vendita`, ammesso solo con segno negativo
- **Quando** si registra un movimento di carico con quel motivo
- **Allora** la risposta è `422` con l'indicazione dei motivi ammessi per un'entrata, e nulla viene scritto

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri movimenti sullo stesso codice articolo
- **Quando** un utente di `A` chiede l'elenco dei movimenti e delle giacenze
- **Allora** vede solo i propri, anche se forza l'identificativo dell'account `B` nel corpo della richiesta o in un
  parametro; la stessa chiave di idempotenza usata dai due account non produce alcun conflitto

**CA-6 — La giacenza coincide con la somma del registro**
- **Dato** una sequenza di movimenti su una coppia articolo-deposito (`+12`, `−3`, `+5`, `−4`)
- **Quando** si confronta la riga di giacenza con la somma delle quantità dei movimenti
- **Allora** i due valori coincidono (`10`) e la `versione` della riga è pari al numero di movimenti applicati

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione del segno rispetto al motivo e di **integrazione** sulle tre risorse, con
      database effimero e migrazioni Flyway vere;
- [ ] prova di **isolamento fra account** su `movimento`, `giacenza` e `motivo_movimento`, compresa la chiave di
      idempotenza omonima in due account diversi;
- [ ] **prova end-to-end**: *rimando* — non c'è ancora superficie utente; il percorso `[J-MAGAZZINO]` è di proprietà
      della storia `0036`, e il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce;
- [ ] **traduzioni** delle etichette dei motivi predefiniti presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, verificato che `movimento` sia già in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta del registro in sola aggiunta, dell'aritmetica nella base
      di dati e del `deleted_at` mai valorizzato;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione esposta in questa storia;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0002` | Schema `app_magazzino`, cartella delle migrazioni e colonne di controllo devono esistere |
| `0006` | Un movimento si riferisce a un articolo: senza anagrafica non c'è nulla da muovere |
| `0008` | Un movimento si riferisce a un deposito e facoltativamente a un'ubicazione |

## 7. Fuori ambito

- Le rotte di carico, scarico e trasferimento: sono delle storie `0014`, `0015` e `0016`. Qui esiste la scrittura
  generica del movimento, che quelle storie specializzano.
- La rettifica con motivo obbligatorio: storia `0021`; il tipo `rettifica` è previsto nell'elenco chiuso ma non ha
  ancora una rotta propria.
- Lo storno: storia `0017`, che aggiunge il riferimento al movimento stornato.
- La ricostruzione della giacenza dal registro e il confronto con la proiezione: storia `0024`.
- Il costo medio ponderato mobile: storia `0014`.

## 8. Punti aperti

- **Lotti, date di scadenza e numeri di matricola** (descrizione dell'applicazione, §11 punto 2): oggi un movimento
  è riferito all'articolo. Riferirlo al lotto cambierebbe la chiave della giacenza e sarebbe una migrazione, non una
  funzione. La decisione va presa **prima** che questa storia venga implementata, non dopo: lo chiude lo
  sviluppatore, direzione di prodotto.
- **Giacenza negativa**: qui il modello la ammette tecnicamente (la colonna non ha vincolo di non negatività), ma la
  politica di quando è lecita si decide nella storia `0015` per i movimenti fatti da una persona e nella storia
  `0019` per quelli generati da un fatto già avvenuto. È un punto aperto di prodotto (§11 punto 3).
