# 0012 — Confine con il catalogo prodotti condiviso

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 02 — Anagrafiche e catalogo prodotti
**Storia**: `0012` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che usa anche i preventivi e la fatturazione della stessa suite
> voglio che un prodotto censito una volta sia lo stesso prodotto ovunque
> così da non ritrovarmi con «Vite 8×20» in tre programmi, tre codici diversi e nessuno che sappia quale è vero.

**Contesto.** Il catalogo indica «catalogo prodotti e listini» fra le entità condivise della suite, comuni a
preventivi (06), fatturazione (02), magazzino (14) e retail (29). QuoteGrove lo ha già impostato: nella sua storia
`0008` il catalogo è **anagrafico e di prezzo**, e dichiara espressamente che «giacenze e movimenti di magazzino
sono di StockGrove». Questa storia accetta e completa quel confine dalla parte del magazzino, e lo fa **adesso**
per un motivo preciso: la colonna che distingue un articolo proprio da un articolo che arriva da fuori va messa
prima che il registro dei movimenti si riempia, altrimenti la si aggiunge a schema pieno e senza sapere quali
righe siano quali.

La regola sta in una riga: **il catalogo condiviso possiede l'identità commerciale del prodotto; StockGrove
possiede la sua quantità** (descrizione dell'applicazione, §10).

| Dato | Chi lo possiede | Chi lo legge |
|---|---|---|
| codice, descrizione, unità di misura, categoria, tipo (prodotto/servizio) | **catalogo condiviso** (oggi QuoteGrove `0008`, domani l'anagrafica di suite) | StockGrove, in proiezione locale |
| prezzo di vendita, listini, sconti, aliquota d'imposta | **catalogo condiviso** — StockGrove non li vede e non li vuole | 06, 02, 29 |
| codice a barre, ubicazione, deposito, giacenza, movimenti, costo medio d'acquisto, soglie di scorta | **StockGrove** | chiunque, tramite l'evento `giacenza.variata` (storia `0020`) |
| disponibilità mostrata in un preventivo o su un negozio online | nessuno dei due la «possiede»: è la giacenza di StockGrove **letta** da altri | 06, 29 |

## 2. Requisiti funzionali

1. **RF-1** — L'articolo porta il campo `origine` con due soli valori: `locale` (nato in StockGrove) e `condivisa`
   (proiezione di un prodotto del catalogo di suite), più `riferimento_esterno` per l'identificativo d'origine.
   Finché il catalogo condiviso non esiste, **tutti** gli articoli sono `locale` e il campo è un predisposto
   dichiarato, non una funzione a metà.
2. **RF-2** — Sugli articoli con `origine = condivisa` i **campi di identità** (codice, descrizione, unità di
   misura, categoria) sono in **sola lettura** nell'interfaccia e nelle rotte di modifica: si cambiano dove sono
   nati. I campi che appartengono al magazzino (codici a barre, deposito, ubicazione, soglie, fornitore preferito)
   restano modificabili.
3. **RF-3** — L'aggiornamento degli articoli `condivisa` avviene **solo per eventi** asincroni: una app non chiama
   mai un'altra app. La proiezione locale si aggiorna quando arriva l'evento e non interroga nessuno sul percorso
   caldo.
4. **RF-4** — Un articolo `locale` **può restare locale per sempre**: un ricambio che nessuno vende, un materiale di
   consumo interno, un imballaggio. Non tutto ciò che si conta si vende, e l'app non spinge a «promuovere» nulla.
5. **RF-5** — StockGrove **non conserva e non mostra** il prezzo di vendita, in nessuna schermata e in nessuna
   esportazione: se comparisse per comodità, il giorno dopo qualcuno chiederebbe di modificarlo da lì. L'unico dato
   di valore che l'app tiene è il costo medio d'acquisto, che nasce dai propri carichi (storia `0025`).
6. **RF-6** — Se un evento del catalogo condiviso arriva per un prodotto il cui codice esiste già come articolo
   `locale`, l'app **non fonde nulla in automatico**: segnala il conflitto e lascia la riconciliazione a una
   decisione umana. Un'unione sbagliata di due articoli fonderebbe due giacenze e sarebbe irreparabile.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Anche gli articoli in proiezione filtrano per `tenant_id` preso dal token
  verificato; l'evento in ingresso porta il proprio `tenant_id` e viene applicato **solo** a quell'account, con
  scarto e registrazione se l'account non esiste o non è abilitato. Prova di isolamento fra due account
  sull'applicazione degli eventi.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova: si estendono gli oggetti di trasferimento
  dell'articolo con `origine` e `riferimento_esterno` (in sola lettura) e la modifica respinge con `409` i campi di
  identità di un articolo `condivisa`, in `application/problem+json`. Definizione OpenAPI aggiornata nello stesso
  commit. **Nessuna chiamata sincrona verso un'altra app**, mai: è l'invariante di comunicazione della piattaforma.
- **RT-3 — Persistenza (§8).** Migrazione `V7__articolo_origine.sql` sullo schema `app_magazzino`: colonne
  `origine` (valore predefinito `locale`, elenco chiuso), `riferimento_esterno` e `sincronizzato_al` su `articolo`;
  indice unico su `tenant_id, riferimento_esterno` quando valorizzato. Il riferimento all'app d'origine è
  **logico**: nessuna chiave esterna verso altri schemi e nessuna interrogazione fra schemi.
- **RT-4 — Modulo frontend (§3, §5).** Nella sezione `articoli` del modulo `magazzino`, gli articoli `condivisa`
  mostrano un contrassegno di provenienza, i campi di identità disabilitati e un rimando che spiega **dove** si
  modificano. Solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Le stringhe nuove — contrassegno di provenienza, messaggio «questo campo si
  modifica nel catalogo condiviso», avviso di conflitto — passano dallo spazio-nomi `magazzino` e sono presenti in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Un articolo in proiezione **consuma quota come gli altri** se è attivo: il
  tetto della metrica `articoli_gestiti` conta gli articoli che l'app tiene in ordine, a prescindere da dove sono
  nati. Se un evento portasse l'account sopra il tetto, l'articolo entra **archiviato** con una segnalazione, e non
  viene scartato: scartare un dato altrui perché il piano è pieno produrrebbe una lacuna silenziosa.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo. Gli strumenti di lettura della storia
  `0034` restituiscono l'articolo **senza distinguere** l'origine: a chi chiede «quante ne ho?» non interessa da
  quale programma è nata la scheda. Gli strumenti di scrittura non modificano mai i campi di identità di un
  articolo `condivisa`.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: l'identità di un prodotto non è un dato di persona.
  Se un domani l'evento condiviso portasse campi liberi popolati altrove, valgono le stesse cautele dei campi
  liberi già dichiarate (storia `0010`).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `articolo in proiezione creato`, `proiezione aggiornata`,
  `conflitto di codice rilevato` e `modifica respinta su campo di identità` sono registrati con `tenant_id`,
  `app_id`, `user_id` (o l'origine dell'evento), identificativo di correlazione e identificativi degli articoli,
  senza descrizioni.

## 4. Criteri di accettazione

**CA-1 — Tutti gli articoli nascono locali**
- **Dato** un account che crea un articolo dall'interfaccia
- **Quando** lo si rilegge
- **Allora** `origine` vale `locale`, `riferimento_esterno` è vuoto e tutti i campi sono modificabili

**CA-2 — Campi di identità in sola lettura sugli articoli condivisi**
- **Dato** un articolo con `origine = condivisa` · **Quando** si tenta di cambiarne la descrizione
- **Allora** la risposta è `409` in `application/problem+json` con l'indicazione di dove si modifica, la descrizione
  resta invariata, e la modifica della **soglia di scorta** dello stesso articolo riesce

**CA-3 — Aggiornamento per evento**
- **Dato** un articolo in proiezione e un evento del catalogo condiviso che ne cambia la descrizione
- **Quando** l'evento viene consumato
- **Allora** la descrizione locale si aggiorna, `sincronizzato_al` avanza, la giacenza **non cambia** e nessuna
  chiamata sincrona verso l'altra app compare nelle prove

**CA-4 — Conflitto di codice**
- **Dato** un articolo `locale` di codice `VT-020` e un evento condiviso per un prodotto con lo stesso codice
- **Quando** l'evento viene consumato
- **Allora** i due articoli restano **distinti**, il conflitto è segnalato all'utente per la riconciliazione, e
  nessuna giacenza viene sommata o spostata

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` e un evento condiviso destinato a `B`
- **Quando** l'evento viene consumato
- **Allora** l'articolo compare solo in `B`, l'elenco di `A` è invariato, e un evento con un `tenant_id`
  sconosciuto viene scartato e registrato senza scrivere nulla

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla regola di sola lettura dei campi di identità e sul rilevamento del conflitto di
      codice; prove di **integrazione** sull'applicazione di un evento simulato, con database effimero e
      migrazioni vere;
- [ ] prova di **isolamento fra account** sull'applicazione degli eventi in ingresso;
- [ ] **prova end-to-end**: *rimando* — oggi non esiste un catalogo condiviso da cui far partire l'evento, quindi
      il percorso non è dimostrabile dalla superficie; la voce nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) si aggiunge quando l'anagrafica di
      suite esisterà, ed è di proprietà di quell'epica. Il caso è coperto a livello di integrazione con un evento
      sintetico;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna voce nuova, e il fatto è dichiarato nel registro delle decisioni;
- [ ] **registro delle decisioni** compilato, con la regola «identità al catalogo, quantità a StockGrove», il
      divieto di chiamate sincrone e il rifiuto della fusione automatica in caso di conflitto;
- [ ] contratto degli **strumenti conversazionali**: comportamento degli strumenti sugli articoli in proiezione
      documentato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0006` | Il campo `origine` si aggiunge all'anagrafica esistente |
| catalogo prodotti condiviso (QuoteGrove `0008`, o l'anagrafica di suite) | **Non esiste ancora**: finché non esiste, questa storia realizza il predisposto e il consumo dell'evento resta verificato con eventi sintetici |

## 7. Fuori ambito

- **Costruire il catalogo condiviso**: non è di questa app. StockGrove ne è un consumatore.
- **Varianti, attributi, schede prodotto ricche e contenuti per canale**: sono di PimGrove (catalogo 43). Qui una
  taglia o un colore restano **articoli distinti con codice proprio** (descrizione, §1).
- **Pubblicare la giacenza verso le altre app**: è il verso opposto e ha la sua storia, la `0020` («evento giacenza
  variata»).
- **Migrazione degli articoli `locale` esistenti verso il catalogo condiviso** quando nascerà: è una migrazione e
  un criterio di riconciliazione dei codici duplicati, ed è punto aperto §11 punto 9 della descrizione.

## 8. Punti aperti

- **Chi possiede l'unità di misura** quando lo stesso prodotto si vende a confezione e si conta a pezzo: la
  proposta è che l'identità stia al catalogo e che StockGrove tenga un fattore di conversione **solo se** il caso
  si presenta davvero; non l'ho trovato discusso nelle fonti. Chiude lo sviluppatore, direzione di prodotto.
- **Forma e nome dell'evento in ingresso** dal catalogo condiviso: dipendono dall'epica della suite che non esiste;
  qui si dichiara il comportamento atteso, non il contratto del messaggio.
- **Criterio di riconciliazione dei codici duplicati** al momento della nascita dell'anagrafica di suite: punto
  aperto §11 punto 9 della descrizione, chiude l'epica della suite.