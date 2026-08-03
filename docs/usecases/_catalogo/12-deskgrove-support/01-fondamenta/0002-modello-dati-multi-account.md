# 0002 — Modello dati multi-account

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio le tabelle portanti dell'assistenza — richiesta, messaggio, richiedente — con l'isolamento fra account
> già cucito dentro
> così da non doverlo aggiungere dopo su venti tabelle, che è il modo in cui si dimentica una riga e si mostrano i
> dati di un cliente a un altro.

**Contesto.** Il servizio esiste ma è vuoto. Questa storia posa le tre tabelle su cui poggia tutto il resto e, con
esse, le regole che non si potranno più cambiare a costo basso: la chiave, il filtro per account, le colonne di
controllo, la cancellazione logica. È anche il momento in cui si guarda ciò che il repository ha già: nel servizio
`core`, sotto `app.appgrove.core.support`, esiste il sistema di richieste di assistenza **della piattaforma**
(UC 0075, change `0084`). Non è la stessa cosa — quello serve ad appgrove per assistere i propri clienti, questo
serve al cliente per assistere i suoi — e per cinque ragioni non può condividere né tabelle né rotte (§10 del
documento capofila). Ma le sue **idee** sono già state discusse e collaudate: la forma della macchina a stati, la
semantica di «in attesa del cliente», la colonna della provenienza, il contrassegno per la revisione umana. Vanno
lette prima di scrivere, non dopo.

## 2. Requisiti funzionali

1. **RF-1** — Esiste la tabella delle **richieste** con: numero progressivo per account, oggetto, stato, priorità,
   canale d'ingresso, riferimento al richiedente, data di apertura, data della prima risposta, data di chiusura.
2. **RF-2** — Esiste la tabella dei **messaggi** del filo con: riferimento alla richiesta, verso (in ingresso, in
   uscita, interno), autore, corpo, data.
3. **RF-3** — Esiste la tabella dei **richiedenti** con: nome, indirizzo di posta elettronica, numero di telefono,
   lingua preferita, e un campo — oggi vuoto — per il riferimento all'anagrafica clienti condivisa della suite,
   quando esisterà.
4. **RF-4** — Il **numero della richiesta** è progressivo **per account** e parte da 1 per ogni account: due
   account diversi hanno entrambi la richiesta numero 1, e nessun cliente deduce dal numero quante richieste
   ricevono gli altri.
5. **RF-5** — Esiste il contrassegno «da guardare con attenzione» sulla richiesta, alimentato dal riconoscitore
   deterministico delle categorie particolari: un valore vero/falso che **non registra quale** categoria sarebbe
   stata riconosciuta.
6. **RF-6** — I campi che riguardano una persona sono annotati e dichiarati nel manifesto dei dati, e le tre
   tabelle compaiono sia nell'esportazione sia nella cancellazione del contratto dati dell'app.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura delle richieste, dei messaggi e dei richiedenti
  filtra per `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai
  parametri viene **ignorato**. Se il filtro manca il comportamento corretto è negare, non mostrare tutto.
- **RT-2 — Persistenza (§8).** Migrazione `V1__helpdesk_core.sql` sullo schema `app_helpdesk`: tabelle `ticket`,
  `ticket_message`, `requester`, ciascuna con `tenant_id`, chiave primaria UUID versione 7 generata
  dall'applicazione, colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione
  logica (`deleted_at`). **Vietate** le chiavi esterne verso altri schemi e le interrogazioni fra schemi: `tenant_id`
  è un riferimento logico. Indice unico su (`tenant_id`, `number`) per il numero progressivo, e su
  (`tenant_id`, `email`) per il richiedente.
- **RT-3 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica nuova in questa storia: le risorse arrivano
  con l'epica 02. Le entità **non** si espongono mai: gli oggetti di trasferimento nascono con le rotte.
- **RT-4 — Dati personali (§10).** Voci nuove nel manifesto `docs/compliance/manifests/helpdesk.yaml`, in italiano
  e inglese, per `requester.name`, `requester.email`, `requester.phone`, `requester.locale`, `ticket.subject`,
  `message.body`; campi annotati `@PersonalData` (un campo annotato e non dichiarato fa fallire la compilazione);
  `ticket`, `ticket_message` e `requester` aggiunte a `exportData` e `purgeData` di `HelpdeskDataContract`. Le voci
  dichiarano che il titolare del trattamento è **l'azienda cliente** e che appgrove agisce per suo conto.
- **RT-5 — Riuso invece di riscrittura.** Il riconoscitore deterministico delle categorie particolari esiste già in
  `services/core/src/main/java/app/appgrove/core/support/SpecialCategoryScreening.java`: si **sposta** in
  `services/commons` (area `privacy`) e lo usano entrambe le applicazioni, con l'elenco delle radici estendibile per
  lingua. È un cambio di casa, non una riscrittura, e va concordato con chi possiede UC 0075.
- **RT-6 — Registrazione eventi (§14).** La creazione di una richiesta e l'aggiunta di un messaggio si registrano
  con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione. **Mai il corpo del messaggio nei
  registri**: qui la regola «nessun dato personale nei registri» è più stringente che altrove, perché il contenuto
  non è nemmeno nostro.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento: non c'è ancora nessuna operazione da esporre.

## 4. Criteri di accettazione

**CA-1 — Le tabelle nascono con le migrazioni vere**
- **Dato** un database effimero appena creato
- **Quando** si applicano le migrazioni Flyway del servizio
- **Allora** lo schema `app_helpdesk` contiene `ticket`, `ticket_message` e `requester`, ognuna con `tenant_id`,
  colonne di controllo e `deleted_at`

**CA-2 — Numero progressivo per account**
- **Dato** due account `A` e `B`, entrambi senza richieste
- **Quando** ciascuno apre la propria prima richiesta
- **Allora** entrambe portano il numero `1`, e la seconda richiesta di `A` porta il numero `2` indipendentemente da
  quante ne abbia aperte `B`

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie richieste
- **Quando** un componente di `A` interroga il deposito delle richieste
- **Allora** vede solo le proprie, anche se l'identificativo dell'account di `B` viene forzato nel corpo della
  richiesta o in un parametro

**CA-4 — Cancellazione logica**
- **Dato** una richiesta esistente
- **Quando** viene cancellata logicamente
- **Allora** non compare più nelle letture ordinarie ma la riga esiste ancora, con `deleted_at` valorizzato

**CA-5 — Contrassegno senza classificazione**
- **Dato** un messaggio il cui testo contiene una parola dell'elenco del riconoscitore
- **Quando** la richiesta viene creata
- **Allora** il contrassegno «da guardare con attenzione» è vero, e **nessun campo** registra quale categoria
  particolare sia stata riconosciuta

**CA-6 — Il manifesto è coerente col codice**
- **Dato** i campi annotati come dati personali
- **Quando** si compila il servizio ed esegue l'area `compliance` della suite
- **Allora** l'esito è verde: nessun campo annotato manca dal manifesto, e ogni voce del manifesto è presente in
  italiano e in inglese

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla generazione del numero progressivo e sul riconoscitore delle categorie particolari,
      e di **integrazione** sulle tabelle con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sui tre depositi introdotti;
- [ ] **prova end-to-end**: *rimando* — nessuna superficie utente; il percorso `[J-HELPDESK]` nasce con la storia
      `0037`, che possiede la voce del registro di copertura;
- [ ] **traduzioni**: non applicabile — nessun testo visibile;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, campi annotati, tabelle presenti in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, con annotate la forma della macchina a stati ereditata
      dall'assistenza interna e lo spostamento del riconoscitore in `services/commons`;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] `docs/usecases/15-supporto-e-piattaforma/0075-ticketing-nativo-in-house.md` aggiornato nel punto aperto sul
      riconoscitore, se lo spostamento in `services/commons` viene eseguito.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0001` di questa app | Serve il servizio e lo schema in cui applicare le migrazioni |
| `services/commons` (colonne di controllo, contesto dell'account) | Le entità di base e il contesto del `tenant_id` non si riscrivono |
| UC 0075 / change `0084` (assistenza interna della piattaforma) | **Non** come dipendenza tecnica ma come lettura obbligata: da lì si prendono le idee già discusse e da lì si sposta il riconoscitore delle categorie particolari |

## 7. Fuori ambito

- **Le rotte pubbliche** su richieste e messaggi: le apre la storia `0006`.
- **Le tabelle di canale, coda, operatore, politica di servizio, articolo, allegato, soddisfazione**: nascono nelle
  rispettive storie, non tutte insieme qui. Anticiparle significherebbe scrivere colonne che nessuno usa e che
  qualcuno userà male.
- **La macchina a stati**: qui c'è la colonna dello stato, non le transizioni. Le regola la storia `0009`.

## 8. Punti aperti

- **Spostare il riconoscitore delle categorie particolari in `services/commons`** tocca codice esistente del
  servizio `core`, fuori dal perimetro di questa applicazione: va concordato con chi possiede UC 0075. Se
  l'accordo non arriva in tempo, l'alternativa **non** è duplicarlo: è dichiarare la storia bloccata su quel punto.
- **Durata di conservazione** delle tre tabelle: la fissa il **titolare**, cioè il cliente, e diventa un parametro
  dell'account nella storia `0036`. Qui non si scrive alcuna costante.
- **Il campo di riferimento all'anagrafica condivisa** resta vuoto finché quell'anagrafica non esiste: è una
  colonna preparata, non una integrazione. Chi la riempirà lo farà per eventi, mai con una chiamata a un'altra app.
