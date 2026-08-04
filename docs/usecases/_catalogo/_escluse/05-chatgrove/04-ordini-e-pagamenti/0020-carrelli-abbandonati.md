# 0020 — Carrelli abbandonati

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 04 — Ordini e pagamenti
**Storia**: `0020` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0015`, `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio
> voglio vedere chi aveva scelto qualcosa e poi è sparito, e potergli scrivere una volta sola
> così da recuperare vendite che oggi si perdono nel silenzio.

**Contesto.** È la funzione che il catalogo indica esplicitamente fra i casi d'uso principali, ed è anche la
più coerente con il difetto individuato dall'analisi (§2.5: si perde per attrito e ritardo, non per prezzo).
Sta in fondo all'epica perché ha bisogno di tutto ciò che viene prima: il carrello per sapere cosa era stato
scelto, il modello approvato per poter scrivere fuori dalla finestra, il consenso per sapere a chi è lecito
scrivere.

## 2. Requisiti funzionali

1. **RF-1** — Un carrello senza movimenti da più di N ore (soglia configurabile dal negozio, predefinita 24)
   e non convertito passa ad `abbandonato`.
2. **RF-2** — Esiste un elenco dei carrelli abbandonati con contatto, valore, prodotti e da quanto tempo sono
   fermi, ordinabile per valore.
3. **RF-3** — Da un carrello abbandonato si invia un **promemoria**: se la finestra di servizio è chiusa serve
   un modello approvato, e il promemoria consuma quota.
4. **RF-4** — Il promemoria produce una **bozza** con il testo esatto e richiede una conferma esplicita prima
   di partire.
5. **RF-5** — Su uno stesso carrello si può inviare **un solo** promemoria: il secondo tentativo è rifiutato e
   spiegato. È un limite voluto — insistere fa scendere il punteggio di qualità del numero.
6. **RF-6** — Se il contatto ha revocato il consenso e il modello è di categoria promozionale, il promemoria è
   bloccato.
7. **RF-7** — Un carrello abbandonato **torna attivo** se il cliente scrive di nuovo o se si modifica una riga.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'elenco e l'invio filtrano per `tenant_id` preso dal token
  verificato; il calcolo periodico dell'abbandono opera **per account**, mai globalmente.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/chat_commerce/v1/carts/abandoned` e
  `POST /api/chat_commerce/v1/carts/{id}/reminder` (bozza + conferma); paginazione; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Varchi e quota (§6, §7).** Il promemoria fuori dalla finestra prenota una unità della metrica
  `messaggi_template`; a quota esaurita risponde `429` e nulla parte. Con abbonamento `canceled` risponde `402`.
- **RT-4 — Persistenza (§8).** Migrazione `V13__promemoria_carrello.sql`: colonne di stato e di promemoria su
  `cart`. Il passaggio ad `abbandonato` è una lavorazione periodica idempotente: eseguirla due volte non cambia
  nulla e non invia nulla.
- **RT-5 — Consenso (storia `0010`).** Prima di produrre la bozza, il servizio verifica il consenso: se manca
  e il modello è promozionale, la bozza **non si crea affatto** — meglio spiegare prima che rifiutare dopo.
- **RT-6 — Modulo frontend (§3, §4, §5).** Sezione «Da recuperare» dentro Ordini; finestra di conferma con il
  testo esatto. Tutte le stringhe in `en, it, fr, es, de`.
- **RT-7 — Dati personali (§10).** Nessun campo nuovo che riguardi una persona oltre a quelli già dichiarati;
  il fatto che un promemoria sia stato inviato è un dato riferito al contatto e rientra nella tabella `cart`,
  già presente in esportazione e cancellazione.
- **RT-8 — Registrazione eventi (§14).** `carrello marcato abbandonato`, `promemoria inviato`, `promemoria
  bloccato per consenso` con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza nomi né
  numeri.
- **RT-9 — Esposizione conversazionale (§12).** `elenca_carrelli_abbandonati` è **lettura**; l'invio del
  promemoria è **scrittura verso l'esterno** con bozza e conferma umana obbligatoria.

## 4. Criteri di accettazione

**CA-1 — Il carrello si marca da solo e torna attivo da solo**
- **Dato** un carrello fermo da 30 ore con soglia a 24
- **Quando** la lavorazione periodica gira
- **Allora** il carrello risulta `abbandonato` e compare nell'elenco da recuperare, **senza** che nulla sia
  stato inviato
- **Quando poi** il cliente scrive di nuovo in quella conversazione
- **Allora** il carrello torna aperto ed esce dall'elenco da recuperare

**CA-2 — Promemoria con conferma**
- **Dato** un carrello abbandonato di un contatto con consenso e un modello approvato
- **Quando** l'addetto chiede il promemoria · **Allora** vede la bozza; **solo dopo** la conferma il messaggio
  parte e il contatore `messaggi_template` aumenta di uno

**CA-3 — Un solo promemoria**
- **Dato** un carrello a cui è già stato inviato il promemoria
- **Quando** si tenta il secondo · **Allora** la richiesta è respinta con `409` e la spiegazione, e nulla parte

**CA-4 — Consenso revocato**
- **Dato** un contatto con consenso `revocato` e un modello promozionale
- **Quando** si chiede il promemoria · **Allora** la bozza non viene creata e l'app spiega il motivo

**CA-5 — Quota esaurita**
- **Dato** un account al tetto di `messaggi_template` · **Quando** si conferma un promemoria fuori finestra
- **Allora** riceve `429`, nulla parte e il carrello resta da recuperare

**CA-6 — Isolamento fra account**
- **Dato** due account · **Quando** un utente di `A` chiede l'elenco dei carrelli abbandonati · **Allora** vede
  solo i propri

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla soglia di abbandono e sull'idempotenza della lavorazione, e di **integrazione**
      sull'invio del promemoria con il canale simulato;
- [ ] prova di **isolamento fra account** sull'elenco e sull'invio;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`; il recupero è un ramo del percorso, non il tronco;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: verificato che il promemoria non introduca campi non dichiarati;
- [ ] **registro delle decisioni** compilato, con il limite di un solo promemoria e il perché (punteggio di
      qualità del numero);
- [ ] contratto degli **strumenti conversazionali** dichiarato: lettura per l'elenco, scrittura con conferma
      per l'invio;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0010` | Il consenso decide a chi è lecito scrivere |
| `0015` | Serve il carrello con il suo ultimo movimento |
| `0018` | Riusa la disciplina della bozza con conferma introdotta lì |

## 7. Fuori ambito

- il promemoria **automatico** senza intervento umano: escluso di proposito, vedi i punti aperti;
- le sequenze di più promemoria a distanza di giorni: sarebbero campagne, e le campagne sono l'epica 05;
- lo sconto di recupero: non c'è un motore promozionale in questa versione.

## 8. Punti aperti

- **Invio automatico del promemoria.** Sarebbe la funzione più richiesta e la più pericolosa: un invio
  automatico verso l'esterno, che costa denaro e può far sospendere il numero del cliente. La proposta è di
  **non** farlo nella prima versione e di tenere la conferma umana. Se lo sviluppatore vorrà l'automatismo,
  serve almeno un tetto giornaliero, un interruttore per account e una prova che il consenso sia verificato a
  ogni invio: è una decisione con effetti verso l'esterno, quindi sua.
