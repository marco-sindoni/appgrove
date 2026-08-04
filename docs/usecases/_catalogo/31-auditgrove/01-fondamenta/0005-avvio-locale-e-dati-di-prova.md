# 0005 — Avvio locale e dati di prova

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che prende in mano AuditGrove per la prima volta
> voglio avviare l'app in locale con un comando e trovarla già piena di azioni di prova, con la catena che cresce
> mentre guardo
> così da capire in dieci minuti come funziona il prodotto, invece di doverlo dedurre dal codice.

**Contesto.** Le storie 0001-0004 hanno costruito un'app che parte ed è vuota. Un registro vuoto non insegna
niente: la cosa che va vista è la **catena che cresce** e le righe che si incatenano. Questa storia chiude
l'epica delle fondamenta rendendo l'app eseguibile in locale senza passi manuali impliciti — è parte della
definizione di fatto imposta dal repository — e aggiunge un **agente finto** che dichiara azioni, perché senza una
sorgente che parla il prodotto non si vede. I dati sono inventati: mai realistici al punto da sembrare dati veri
di un cliente.

## 2. Requisiti funzionali

1. **RF-1** — `./dev.sh services` mostra l'app `agentaudit` con la porta `8131` e lo schema `app_agentaudit`,
   ricavati dalla sola dichiarazione delle proprietà del servizio.
2. **RF-2** — `./app-start.sh` avvia l'app, `dev migrate` applica le migrazioni e le rotte `/api/agentaudit/v1/*`
   sono raggiungibili attraverso il proxy locale, **senza modifiche manuali a nessuno script**.
3. **RF-3** — Esiste un insieme di dati di prova **inventati**: due account di prova, tre sorgenti, alcune decine
   di azioni distribuite su strumenti diversi, con esiti diversi.
4. **RF-4** — Esiste un **agente finto** avviabile in locale che dichiara azioni a intervalli, così che il
   registro si riempia e la catena cresca mentre lo sviluppatore guarda la schermata.
5. **RF-5** — Un comando ripopola i dati di prova da zero: catena azzerata e ricostruita, in modo deterministico e
   ripetibile.
6. **RF-6** — I dati di prova sono chiaramente riconoscibili come tali: nomi di fantasia, indirizzi di posta nel
   dominio riservato alle prove (`*.test`), nessun riferimento a persone o aziende reali.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I dati di prova popolano **due** account, non uno: è la condizione
  minima per accorgersi a occhio di una perdita di isolamento mentre si sviluppa.
- **RT-2 — Interfaccia di programmazione (§2).** L'agente finto passa dal **servizio di accodamento** della storia
  0002, che è ciò che esiste quando questa storia si esegue: le righe di prova nascono incatenate come le vere.
  Quando la rotta di ingresso pubblica esisterà (storia 0008), l'agente finto **va ripuntato su quella** — è un
  compito esplicito della 0008, non un rimando generico: un generatore che continua a scrivere per una via che i
  clienti non useranno smetterebbe di provare qualcosa.
- **RT-3 — Persistenza (§8).** Le migrazioni girano davvero anche in locale; il ripopolamento non aggira il
  livello di accodamento — le righe di prova nascono incatenate come quelle vere.
- **RT-4 — Modulo frontend (§3, §5).** Il modulo `agentaudit` è abilitato nello stub locale di abilitazione
  finché quella reale non esiste, così che compaia nella barra laterale sullo stack di sviluppo.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile nuovo; i dati di prova non introducono stringhe
  d'interfaccia.
- **RT-6 — Varchi e quota (§6, §7).** I dati di prova comprendono un account **vicino al proprio tetto**, così
  che l'avviso di quota e la banda di cortesia (storia 0004) siano visibili senza doverli provocare a mano.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia.
- **RT-8 — Dati personali (§10).** I dati di prova **non contengono dati personali veri**: identificativi
  inventati, indirizzi `*.test`. Nessuna voce nuova nel manifesto.
- **RT-9 — Registrazione eventi (§14).** In sviluppo il formato del registro tecnico è testo leggibile; ogni riga
  porta comunque `tenant_id`, `app_id`, `user_id` e identificativo di correlazione.
- **RT-10 — Avvio locale automatico (§15).** La mappa servizio → identificativo app → porta → schema deriva dal
  **solo** file delle proprietà del servizio. Se venisse voglia di modificare a mano uno script di avvio, è un
  difetto della scoperta automatica, non un passo del lavoro.

## 4. Criteri di accettazione

**CA-1 — L'app si scopre da sola**
- **Dato** il repository dopo l'unione della storia
- **Quando** si esegue `./dev.sh services`
- **Allora** compare `agentaudit` con porta `8131` e schema `app_agentaudit`, senza che nessuno script sia stato
  modificato a mano

**CA-2 — Lo stack parte e serve**
- **Dato** lo stack locale spento
- **Quando** si esegue `./app-start.sh`
- **Allora** il servizio risponde sulle rotte `/api/agentaudit/v1/*` attraverso il proxy locale e il modulo
  compare nella barra laterale del backoffice

**CA-3 — La catena cresce sotto gli occhi**
- **Dato** lo stack locale avviato e l'agente finto in esecuzione
- **Quando** si apre la sezione Cronologia e si attende
- **Allora** compaiono azioni nuove, i numeri di sequenza sono consecutivi e la verifica interna della catena
  risponde «integra»

**CA-4 — Il ripopolamento è ripetibile**
- **Dato** un ambiente locale con dati già presenti
- **Quando** si esegue il comando di ripopolamento due volte
- **Allora** si ottiene lo stesso stato entrambe le volte, senza doppioni e con catene integre

**CA-5 — I due account restano separati**
- **Dato** i due account di prova popolati
- **Quando** si guarda il registro con l'utente del primo
- **Allora** non compare nessuna azione del secondo, nemmeno forzando l'identificativo dell'altro account nella
  richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul generatore dei dati di prova (determinismo) e di **integrazione** sul ripopolamento,
      con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sui dati di prova, che sono costruiti apposta su due account;
- [ ] **prova end-to-end**: risposta «rimando» — il percorso `[J-AGENTAUDIT]` nasce alla storia 0037, che userà
      proprio questi dati di prova; il registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) porta l'esenzione motivata fino ad
      allora;
- [ ] **traduzioni**: nessun testo visibile introdotto;
- [ ] **manifesto dei dati**: nessuna voce nuova; i dati di prova sono inventati e la cosa è dichiarata;
- [ ] **registro delle decisioni** compilato, con la scelta di far parlare l'agente finto attraverso la rotta
      pubblica invece che con una scorciatoia interna;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia, dichiarato;
- [ ] `./dev.sh services` e l'avvio locale funzionano senza passi manuali, ed è questa storia a dimostrarlo;
- [ ] `run-tests.sh` esegue anche l'area di collaudo di avvio reale se l'app vi rientra.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie `0001`-`0004` | Servono servizio, catena, modulo frontend e quota: i dati di prova li attraversano tutti |
| — | **Nessuna dipendenza in avanti**: l'agente finto scrive attraverso il servizio di accodamento della 0002. Il passaggio alla rotta pubblica è un compito della storia 0008, non un prerequisito di questa |

## 7. Fuori ambito

- il collegamento di una sorgente vera dall'interfaccia: storia 0006;
- i dati di prova per il percorso end-to-end di piattaforma: storia 0037, che li erediterà da qui;
- qualunque dato di prova che assomigli a dati veri di un cliente: vietato, non rimandato.

## 8. Punti aperti

- **Ricucitura con la storia 0008, tracciata perché non si perda.** Qui l'agente finto scrive attraverso il
  servizio di accodamento; quando la rotta di ingresso pubblica esisterà, va ripuntato su quella, altrimenti lo
  strumento di sviluppo smette di esercitare il percorso che i clienti useranno davvero. L'alternativa — spostare
  questa storia dopo la 0008 — lascerebbe le fondamenta senza avvio locale, cosa che il repository non ammette.
  Il compito è assegnato alla 0008; qui resta scritto perché è qui che se ne accorgerebbe chi legge.
- **Quante azioni di prova.** Poche non fanno vedere la paginazione, troppe rallentano l'avvio. Propongo qualche
  decina per account, con un comando che ne genera molte di più su richiesta per le prove di carico.
