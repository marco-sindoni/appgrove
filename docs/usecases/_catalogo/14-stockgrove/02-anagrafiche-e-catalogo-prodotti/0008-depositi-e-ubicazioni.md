# 0008 — Depositi e ubicazioni

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 02 — Anagrafiche e catalogo prodotti
**Storia**: `0008` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come installatore che gira con un furgone
> voglio distinguere quello che è in magazzino da quello che ho già caricato sul mezzo
> così da non tornare in sede a prendere un pezzo che ho sotto il sedile da tre giorni.

**Contesto.** «Quanti ce ne sono?» è una domanda mal posta finché non si dice **dove**. Il deposito è la seconda
metà della chiave di ogni giacenza — la coppia `(articolo, deposito)` — e va introdotto **prima** dell'epica 03,
altrimenti il registro dei movimenti nascerebbe con una dimensione in meno e aggiungerla dopo sarebbe una
migrazione di tutte le righe già scritte. Il caso più frequente nel nostro segmento non è la multinazionale con
dieci capannoni: è l'artigiano con un magazzino e un furgone, e il negozio con il retrobottega e la sala. Dentro il
deposito, l'**ubicazione** è volutamente una semplice etichetta libera — «scaffale C», «ripiano 3», «cassetta
blu» — perché nessuna micro-impresa mantiene una mappa a coordinate, e chiederle di farlo è il modo migliore per
far abbandonare il programma (descrizione dell'applicazione, §2.5: «la profondità inutile è il rifiuto più netto»).

## 2. Requisiti funzionali

1. **RF-1** — Esiste la tabella `deposito` con `codice`, `nome`, `tipo` (`magazzino` | `negozio` | `furgone`),
   `indirizzo` facoltativo, `predefinito` sì/no e `stato` (`attivo` | `archiviato`).
2. **RF-2** — Ogni account ha **esattamente un** deposito predefinito. Il primo deposito nasce predefinito;
   marcarne un altro sposta il contrassegno; il deposito predefinito non si può archiviare finché non se ne elegge
   un altro.
3. **RF-3** — Esiste la tabella `ubicazione`, che appartiene a un deposito e ha una sola **etichetta libera**
   univoca dentro quel deposito. L'ubicazione è **facoltativa** ovunque: un magazzino senza ubicazioni funziona.
4. **RF-4** — Il `codice` del deposito è univoco per account; il tipo serve a mostrare l'icona giusta e a filtrare,
   non cambia il comportamento del registro: un furgone si carica e si scarica come un magazzino.
5. **RF-5** — Un deposito si archivia solo se la somma delle giacenze in esso è zero; altrimenti l'operazione è
   respinta indicando quanti articoli hanno ancora pezzi lì dentro e suggerendo il trasferimento (storia `0016`).
   È il caso opposto all'articolo (storia `0006`), e la ragione è che archiviare un deposito pieno renderebbe
   invisibile merce che esiste, senza nessun modo di ritrovarla.
6. **RF-6** — Il **numero di depositi ammessi è una caratteristica del piano**, non la metrica di quota: la mappa
   `features` del listino dichiara `depositi: 1` per `free`, `3` per `pro`, illimitati per `business`. Al
   superamento la creazione è respinta con un messaggio che indica il limite del piano.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `deposito` e `ubicazione` filtra per
  `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri
  viene ignorato. Prova di isolamento fra due account su entrambe le risorse.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/magazzino/v1/depositi`,
  `GET|PATCH /api/magazzino/v1/depositi/{id}`, `POST /api/magazzino/v1/depositi/{id}/archiviazione`,
  `GET|POST /api/magazzino/v1/depositi/{id}/ubicazioni`,
  `DELETE /api/magazzino/v1/depositi/{id}/ubicazioni/{ubicazioneId}`; oggetti di trasferimento al bordo;
  validazione dichiarativa; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-3 — Persistenza (§8).** Migrazione `V4__deposito_ubicazione.sql` sullo schema `app_magazzino`: due tabelle
  con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e `deleted_at`; indice unico su
  `tenant_id, lower(codice)` per il deposito e su `tenant_id, deposito_id, lower(etichetta)` per l'ubicazione;
  indice parziale che garantisce **un solo** deposito predefinito per account. Nessuna chiave esterna verso altri
  schemi.
- **RT-4 — Modulo frontend (§3, §5).** Sotto-sezione «Depositi» dentro la sezione `impostazioni` del modulo
  `magazzino`: elenco con il tipo, modulo di inserimento, gestione delle ubicazioni, contrassegno del predefinito.
  In tutte le schermate operative il deposito è un **selettore in testa** che ricorda l'ultima scelta della
  persona. Dati letti con il client generato; solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — nomi dei tipi di deposito, errore di archiviazione
  con giacenza residua, messaggio del limite di piano — passano dallo spazio-nomi `magazzino` e sono presenti in
  `en, it, fr, es, de`. I tipi di deposito sono **chiavi**, non testo.
- **RT-6 — Varchi e quota (§6, §7).** Il deposito **non consuma** la metrica `articoli_gestiti`: il tetto è sugli
  articoli attivi e nient'altro (descrizione, §3). Il limite sui depositi si legge dalla mappa `features` del piano
  pubblicata dall'abilitazione e, se superato, la creazione risponde `403` con il limite e il rimedio. Con
  abbonamento `canceled` il servizio risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo. Questa storia però **rende sensato** il
  parametro `deposito?` di quasi tutti gli strumenti dichiarati nella descrizione (§7): quando è omesso, gli
  strumenti di lettura rispondono per tutti i depositi con il totale, e quelli di scrittura usano il deposito
  predefinito. La regola è dichiarata qui e realizzata nelle storie `0034` e `0035`.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. L'`indirizzo` del deposito è l'indirizzo di un
  luogo dell'impresa, non di una persona; va però dichiarato nel registro delle decisioni che il campo **non deve
  essere usato** per l'indirizzo di casa di un collaboratore che tiene merce, caso in cui diventerebbe un dato
  personale a tutti gli effetti e andrebbe messo a manifesto.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `deposito creato`, `deposito predefinito cambiato`,
  `archiviazione respinta per giacenza residua` e `creazione respinta per limite di piano` sono registrati con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza l'indirizzo.

## 4. Criteri di accettazione

**CA-1 — Primo deposito e deposito predefinito**
- **Dato** un account senza depositi
- **Quando** crea il deposito `MAG` «Magazzino» di tipo `magazzino`
- **Allora** il deposito esiste, risulta **predefinito**, e le schermate operative lo propongono come selezione
  iniziale

**CA-2 — Un solo predefinito**
- **Dato** un account con i depositi `MAG` (predefinito) e `FUR` «Furgone Ducato»
- **Quando** si marca `FUR` come predefinito
- **Allora** `FUR` risulta predefinito, `MAG` non lo è più, e in nessun momento ne esistono due

**CA-3 — Archiviazione di un deposito con merce dentro**
- **Dato** il deposito `FUR` con 3 articoli che hanno giacenza diversa da zero
- **Quando** si tenta di archiviarlo
- **Allora** la risposta è `409` in `application/problem+json`, il messaggio dice quanti articoli hanno ancora
  pezzi e suggerisce di trasferirli, e il deposito resta attivo

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con un deposito di codice `MAG`
- **Quando** un utente di `A` chiede l'elenco dei depositi o le ubicazioni di un deposito di `B`
- **Allora** vede solo i propri e riceve `404` per il deposito altrui, anche forzando l'identificativo dell'account
  `B` nella richiesta

**CA-5 — Limite di depositi del piano**
- **Dato** un account sul piano `free`, con `depositi: 1` nelle caratteristiche del piano, che ha già un deposito
- **Quando** tenta di crearne un secondo
- **Allora** riceve `403` con il limite del piano e l'indicazione del rimedio, e nulla viene creato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sull'invariante «un solo predefinito» e sul controllo di giacenza residua in
      archiviazione; prove di **integrazione** su depositi e ubicazioni, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su `deposito` e `ubicazione`;
- [ ] **prova end-to-end**: *rimando* — il selettore del deposito è un passo del percorso `[J-MAGAZZINO]` di
      proprietà della storia `0036`; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) riceve lì la voce;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), tipi di deposito compresi;
- [ ] **manifesto dei dati**: nessuna voce nuova, con la nota sull'uso improprio del campo indirizzo scritta nel
      registro delle decisioni;
- [ ] **registro delle decisioni** compilato, con la scelta dell'ubicazione come etichetta libera e dei depositi
      come caratteristica del piano invece che come metrica;
- [ ] contratto degli **strumenti conversazionali**: regola del parametro `deposito?` omesso documentata qui;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0006` | Il controllo sulla giacenza residua in archiviazione presuppone che esistano articoli; finché l'epica 03 non c'è, il controllo passa banalmente perché le giacenze sono tutte a zero |
| `0004` | La lettura della mappa `features` del piano passa dalla proiezione dell'abilitazione |

## 7. Fuori ambito

- **Trasferimento della merce fra depositi**: storia `0016`. Qui si dichiarano i luoghi, non si sposta niente.
- **Giacenza per deposito**: epica 03. Questa storia introduce la seconda metà della chiave, non il saldo.
- **Mappe, corsie, coordinate e percorsi di prelievo**: fuori prodotto. Sono il magazzino di chi ha un magazziniere
  a tempo pieno, non il nostro cliente.
- **Depositi di terzi (conto lavorazione, merce presso il cliente)**: fuori perimetro; se servisse, il tipo di
  deposito è il punto in cui si aggancerebbe, ed è una decisione di prodotto.

## 8. Punti aperti

- **L'applicazione a runtime della mappa `features`** non è verificata: la descrizione dell'applicazione la segnala
  come punto aperto §11 punto 1. Se le caratteristiche del piano non fossero applicate dalla piattaforma, la via
  onesta è **dare i depositi illimitati a tutti i piani a pagamento** e togliere il messaggio di limite, non
  fingere un limite che nessuno fa rispettare. Chiude lo sviluppatore al momento dello scaffolding, e la scelta
  cambia il criterio di accettazione CA-5.
- **Se il furgone debba essere un tipo di deposito o un concetto a parte** («mezzo»): la proposta è il tipo, perché
  il comportamento è identico e un concetto in più costerebbe una schermata. Se emergessero comportamenti diversi
  (per esempio la riconsegna automatica del non usato a fine giornata), la scelta va rivista. Chiude lo
  sviluppatore, direzione di prodotto.