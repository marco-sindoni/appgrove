# 0025 — Orologi e violazioni

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 05 — Tempi di risposta e livello di servizio
**Storia**: `0025` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0009`, `0023`, `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come operatore che apre la coda la mattina
> voglio vedere su ogni richiesta entro quando devo rispondere e quanto tempo mi resta davvero
> così da lavorare sulle cose che stanno per scadere e non su quelle che sembrano vecchie ma aspettano il cliente.

**Contesto.** Il calendario c'è (storia `0023`), la promessa c'è (storia `0024`): qui la promessa diventa due date
su ogni richiesta e un numero che si muove. La regola che decide se questa storia è utile o inutile è una sola, ed è
scritta nei rischi noti del [documento capofila](../application-description.md) §11: *se gli orologi non si fermano
quando la palla è al cliente, i numeri sono falsi e gli operatori smettono di guardarli*. Una richiesta in stato «in
attesa del cliente» da sei giorni non è un ritardo dell'azienda — contarla come tale è il modo più rapido per
trasformare la misura del servizio in un giocattolo. La macchina a stati del capofila §4 dice già dove l'orologio si
ferma e dove si mette in pausa: questa storia la implementa senza inventarne un'altra.

## 2. Requisiti funzionali

1. **RF-1** — All'apertura di una richiesta il servizio calcola due scadenze — prima risposta e risoluzione —
   applicando la politica della coda (storia `0024`) sull'orario di servizio dell'account (storia `0023`), e le
   memorizza sulla richiesta insieme all'istante da cui contano.
2. **RF-2** — L'orologio della prima risposta si ferma **definitivamente** al primo messaggio in uscita verso il
   richiedente; una nota interna (storia `0008`) non lo ferma.
3. **RF-3** — L'orologio della risoluzione si mette in pausa all'ingresso nello stato «in attesa del cliente» e
   riprende alla replica del cliente; il tempo trascorso in pausa non si conta e la scadenza si sposta in avanti
   della stessa quantità di ore lavorative.
4. **RF-4** — L'orologio della risoluzione si ferma all'ingresso negli stati «risolta» e «chiusa»; una richiesta
   riaperta è una richiesta nuova (capofila §4) e riceve quindi scadenze nuove.
5. **RF-5** — Quando una scadenza viene superata senza essere stata soddisfatta, il servizio registra una
   violazione con il tipo (prima risposta o risoluzione), l'istante e il ritardo in ore lavorative; la violazione
   resta anche se la richiesta viene poi risolta.
6. **RF-6** — Il cambio di priorità (storia `0021`) ricalcola le scadenze **non ancora soddisfatte** con i nuovi
   obiettivi, contando dall'istante di apertura originale; una violazione già registrata non si cancella.
7. **RF-7** — La richiesta espone in ogni momento, per ciascuno dei due orologi, il proprio stato — in corso, in
   pausa, soddisfatto, violato — e il tempo residuo o il ritardo in ore lavorative.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il calcolo, la lettura e la registrazione delle violazioni filtrano per
  `tenant_id` preso dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri
  viene ignorato. La lavorazione periodica che rileva le scadenze superate (RT-10) elabora account per account con
  filtro `WHERE tenant_id = :tid` e non aggrega mai fra account.
- **RT-2 — Interfaccia di programmazione (§2).** Le due scadenze e lo stato degli orologi viaggiano dentro l'oggetto
  di trasferimento della richiesta già esposto da `GET /api/helpdesk/v1/richieste/{id}` e dall'elenco
  `GET /api/helpdesk/v1/richieste`: nessuna rotta nuova per leggerli. Si aggiunge la sola
  `GET /api/helpdesk/v1/richieste/{id}/violazioni` per la storia delle violazioni della singola richiesta; errori
  in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__service_clocks.sql` sullo schema `app_helpdesk`: colonne sulla
  tabella della richiesta (`first_response_due_at`, `resolution_due_at`, `first_response_at`, `clock_paused_at`,
  `paused_working_minutes`, `service_policy_id` fotografata all'apertura) e tabella `service_breach` (tipo,
  istante, ritardo in minuti lavorativi) con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e
  cancellazione logica. Le ore lavorative si conservano in **minuti interi** per non accumulare errori di
  arrotondamento; nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** Nel dettaglio della richiesta (storia `0007`) compaiono le due scadenze con
  il tempo residuo o il ritardo e l'indicazione esplicita quando l'orologio è in pausa; nell'elenco (storia `0010`)
  compare la sola scadenza più vicina. Dati letti con il client generato; solo token del sistema di design; funziona
  in tema chiaro e scuro. Nessun colore da solo porta l'informazione: accanto all'indicatore c'è sempre il testo.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — «in pausa: la palla è al cliente», «scaduta da 3 ore
  lavorative», «prima risposta», «risoluzione» — passano dallo spazio-nomi `helpdesk` e sono presenti in
  `en, it, fr, es, de`, con le forme al singolare e al plurale corrette per ciascuna lingua.
- **RT-6 — Varchi e quota (§6, §7).** Il calcolo delle scadenze non consuma la metrica `agents` (natura `stock`):
  è una conseguenza dell'apertura di una richiesta, non un'azione dell'utente. Con abbonamento `canceled` il
  servizio risponde `402` alle letture della richiesta e la lavorazione periodica salta l'account, registrandolo.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo, ma un **contributo obbligato** ai contratti
  già dichiarati: `elenca_richieste(stato?, coda?, operatore?, oltre_scadenza?, periodo?)` (storia `0034`) deve
  poter filtrare per `oltre_scadenza` grazie ai campi introdotti qui, e restituisce la scadenza senza il corpo dei
  messaggi. Entrambi di sola lettura, nessuna conferma umana. Dipendenza di piattaforma dichiarata: UC 0061-0063,
  non ancora implementati.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: scadenze, pause e violazioni sono metadati
  temporali e riferimenti. La tabella `service_breach` va comunque aggiunta a `exportData` e `purgeData` del
  contratto `HelpdeskDataContract`, perché è legata alla richiesta: dimenticarla lascerebbe righe orfane dopo una
  cancellazione per singolo richiedente (storia `0036`) — che è il difetto di conformità più facile da commettere.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «scadenze calcolate», «prima risposta soddisfatta», «orologio in
  pausa», «orologio ripreso», «scadenza violata», «scadenze ricalcolate per cambio di priorità» sono registrati con
  `tenant_id`, `app_id`, `user_id` (o «sistema» per la lavorazione periodica) e identificativo di correlazione,
  senza dati personali e senza alcun frammento del contenuto della conversazione.
- **RT-10 — Rilevazione delle scadenze superate.** Una lavorazione periodica frequente (proposta: ogni cinque
  minuti) individua le richieste con scadenza superata e non ancora violata e registra la violazione. È
  **idempotente**: la stessa richiesta e lo stesso tipo di scadenza non producono due violazioni, garantito da un
  vincolo di unicità su (`tenant_id`, richiesta, tipo). Modello di riferimento: lo spazzino della conservazione
  dell'assistenza interna della piattaforma (capofila §10, punto 5).

## 4. Criteri di accettazione

**CA-1 — Scadenze calcolate all'apertura fuori orario**
- **Dato** un account con orario 09:00-18:00 dal lunedì al venerdì e una politica che promette 4 ore lavorative di
  prima risposta per la priorità «normale»
- **Quando** arriva una richiesta il sabato alle 20:00
- **Allora** la scadenza di prima risposta è il lunedì alle 13:00 e il tempo residuo mostrato la domenica è ancora
  di 4 ore lavorative piene

**CA-2 — La nota interna non ferma l'orologio**
- **Dato** una richiesta aperta con orologio di prima risposta in corso
- **Quando** un operatore scrive una nota interna e nessun messaggio esce verso il richiedente
- **Allora** l'orologio della prima risposta è ancora in corso; scrivendo poi una risposta al cliente si ferma e
  risulta soddisfatto con l'istante di quella risposta

**CA-3 — Pausa quando la palla è al cliente**
- **Dato** una richiesta con scadenza di risoluzione fra 10 ore lavorative, portata in stato «in attesa del cliente»
- **Quando** il cliente replica dopo sei giorni di calendario
- **Allora** il tempo residuo alla ripresa è ancora di 10 ore lavorative meno quelle consumate prima della pausa, e
  la scadenza risulta spostata in avanti della durata lavorativa dell'attesa

**CA-4 — Violazione registrata e non cancellabile**
- **Dato** una richiesta la cui scadenza di prima risposta è superata senza alcun messaggio in uscita
- **Quando** la lavorazione periodica viene eseguita, anche due volte di seguito
- **Allora** esiste **una sola** violazione di tipo «prima risposta» con il ritardo in ore lavorative, e resta
  registrata anche dopo che la richiesta viene risolta

**CA-5 — Cambio di priorità**
- **Dato** una richiesta di priorità «normale» aperta due ore lavorative fa, con prima risposta promessa a 8 ore e
  non ancora soddisfatta
- **Quando** un operatore la porta a priorità «urgente», la cui promessa è di 2 ore
- **Allora** la scadenza di prima risposta viene ricalcolata dall'istante di apertura e risulta già superata: la
  violazione viene registrata alla prima esecuzione successiva della lavorazione

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, con politiche e orari diversi e richieste aperte nello stesso istante
- **Quando** un utente di `A` legge le scadenze e le violazioni, anche forzando l'identificativo di una richiesta di
  `B`
- **Allora** vede solo le proprie, la richiesta di `B` risponde come se non esistesse, e le scadenze di `B` sono
  calcolate sull'orario di `B` e non su quello di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla macchina degli orologi — pausa, ripresa, pause ripetute, prima risposta soddisfatta,
      cambio di priorità dopo una violazione — e di **integrazione** sulla lavorazione periodica, con database
      effimero, migrazioni vere e verifica dell'idempotenza;
- [ ] prova di **isolamento fra account** su scadenze, violazioni e lavorazione periodica;
- [ ] **prova end-to-end**: *rimando* alla storia `0037`, proprietaria del percorso `[J-HELPDESK]`, che percorre
      apertura → attesa del cliente → risposta → scadenza; motivo e storia proprietaria annotati nel registro di
      copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), forme plurali comprese;
- [ ] **manifesto dei dati** aggiornato: nessuna voce nuova di persone, `service_breach` presente in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, in particolare sulla semantica della
      pausa e sul comportamento al cambio di priorità;
- [ ] contratto degli **strumenti conversazionali**: il filtro `oltre_scadenza` di `elenca_richieste` diventa
      realizzabile e viene annotato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0023` (orario di servizio) | Le ore lavorative si contano solo su un calendario |
| storia `0024` (politiche di servizio) | Senza obiettivi non c'è nulla da calcolare |
| storia `0009` (ciclo di vita degli stati) | La pausa e la ripresa sono agganciate agli stati «in attesa del cliente» e «risolta» |
| storia `0007` (filo dei messaggi e risposta) | Il primo messaggio in uscita è ciò che ferma l'orologio della prima risposta |
| epica di piattaforma non implementata (UC 0061-0063) | Il filtro `oltre_scadenza` degli strumenti sarà utilizzabile solo quando il livello conversazionale esisterà |

## 7. Fuori ambito

- **L'avviso all'operatore prima che la scadenza passi**: storia `0026`. Qui la scadenza è calcolata e visibile, ma
  nessuno la ricorda attivamente.
- **I numeri aggregati** (quante violazioni, tempo medio di prima risposta): storia `0028`.
- **Un secondo livello di promessa dopo la violazione** («recuperiamo entro X»): rimandato perché è una funzione da
  centro assistenza grande e il capofila §2.5 la esclude dal perimetro.
- **La sospensione manuale dell'orologio da parte dell'operatore** («metti in pausa perché aspettiamo il
  fornitore»): rimandata: è comoda ma è anche il modo più semplice per truccare la misura, e va decisa insieme a
  chi guarderà i numeri.
- **La pubblicazione della scadenza verso il cliente finale**: le storie del portale, epica 06.

## 8. Punti aperti

- **La frequenza della lavorazione periodica** (proposta: cinque minuti) è un compromesso fra prontezza della
  violazione e costo: su obiettivi misurati in ore, un ritardo di rilevazione di cinque minuti è irrilevante.
  **Decide lo sviluppatore.**
- **Cosa fare delle richieste già aperte quando l'orario di servizio viene modificato**: la proposta è di non
  ricalcolare nulla, per lo stesso principio della storia `0024` RF-7 — cambiare il righello a metà misura rende
  incomprensibili i numeri già visti. **Decide lo sviluppatore.**
- **Se la sospensione manuale dell'orologio vada concessa** (vedi «Fuori ambito») è una decisione di direzione di
  prodotto: **decide lo sviluppatore**.
