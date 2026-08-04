# 0032 — Collegamento di stato per il richiedente

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 06 — Base di conoscenza e portale del richiedente
**Storia**: `0032` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0009`, `0015`, `0027`
**Ultimo aggiornamento**: 2026-08-03

> ⚠️ **Storia con una deviazione architetturale da approvare prima dell'implementazione.** Tocca l'invariante
> numero uno della piattaforma («`tenant_id` solo dal token verificato»). La proposta di conciliazione è scritta in
> RT-1 e il punto è aperto in §8: **la conferma spetta allo sviluppatore**, non a un agente.

## 1. Narrazione

> Come cliente finale che ha scritto all'assistenza e sta aspettando
> voglio vedere a che punto è la mia richiesta e rispondere, senza creare un account da nessuna parte
> così da smettere di scrivere «ci sono novità?» e da non dover cercare il messaggio originale nella mia posta.

**Contesto.** «Che il cliente finale non debba registrarsi per seguire la propria richiesta» è la quarta aspettativa
del segmento nella ricerca di mercato (§2.5 della descrizione dell'applicazione), ed è anche la sola compatibile con
la realtà: nessuno apre un account per chiedere dov'è il suo pacco. Ma questa persona **non è un utente di
appgrove**: non ha un token, non ha un ruolo, non appartiene a un account. È il punto in cui l'applicazione incontra
l'invariante numero uno della piattaforma e deve dire come lo rispetta, invece di aggirarlo in silenzio. La storia
`0027` ha già aperto la strada con un gettone monouso per il voto di soddisfazione; qui il meccanismo diventa
**generale e formalizzato**, e resta **uno solo** in tutta l'applicazione.

## 2. Requisiti funzionali

1. **RF-1** — Ogni richiesta ha un **collegamento di stato riservato**, firmato dalla piattaforma, che viene incluso
   nei messaggi in uscita verso il richiedente. Il collegamento non è indovinabile e non contiene il numero della
   richiesta in chiaro.
2. **RF-2** — Il collegamento apre una pagina che mostra **una sola** richiesta: numero, oggetto, stato, data di
   apertura, data dell'ultimo aggiornamento e i messaggi scambiati con il cliente. **Le note interne non compaiono
   mai**, né nel testo della pagina né nei dati che la alimentano.
3. **RF-3** — Dalla pagina il richiedente può **replicare**: la replica entra nel filo come messaggio in ingresso,
   avvisa l'operatore assegnato e riapre la richiesta se era risolta o chiusa, seguendo la macchina a stati della
   storia `0009`.
4. **RF-4** — Il gettone ha una **scadenza** (proposta: 90 giorni dall'ultimo messaggio, rinnovata a ogni nuovo
   messaggio in uscita) e si può **revocare** in qualsiasi momento dall'operatore, con effetto immediato.
5. **RF-5** — Gettone scaduto, revocato, manomesso o inesistente portano tutti alla **stessa** pagina neutra, che
   non rivela se la richiesta esista e invita a scrivere all'indirizzo dell'assistenza.
6. **RF-6** — La pagina non è indicizzabile dai motori di ricerca e non contiene nulla che permetta di raggiungere
   un'altra richiesta, un altro richiedente o un altro account.
7. **RF-7** — Nel backoffice l'operatore vede lo stato del collegamento della richiesta (attivo, scaduto, revocato)
   e può revocarlo o rigenerarlo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1) — ⚠️ deviazione dichiarata, con proposta di conciliazione.** Il richiedente
  non ha un token di accesso. **La proposta è che il gettone firmato *sia* un token verificato**, non un'eccezione
  all'invariante: emesso e firmato dalla piattaforma, verificato a ogni richiesta come qualsiasi altro token, con
  **un solo permesso** (`ticket:read+reply`) e **un solo oggetto** (una richiesta, identificata dentro il gettone).
  Il servizio ne ricava `tenant_id` e identificativo della richiesta **esattamente come dagli altri token**: mai da
  un parametro, mai dal corpo, mai da un'intestazione. Un `ticket_id` che arrivasse dall'indirizzo viene ignorato.
  Il gettone **non è un'identità** e **non diventa mai una sessione**: non concede nessun'altra lettura, non elenca
  richieste, non si scambia con un token di accesso ordinario. Con questa forma l'invariante non viene deviato ma
  **esteso** a un emittente e a un profilo di permessi nuovi. **Va approvato dallo sviluppatore prima di scrivere
  codice** (§8, e punto 5 dei rischi della descrizione dell'applicazione).
- **RT-2 — Interfaccia di programmazione (§2).** Rotte pubbliche separate e riconoscibili:
  `GET /api/helpdesk/v1/public/tickets/{gettone}` e `POST /api/helpdesk/v1/public/tickets/{gettone}/replies`.
  Nessun'altra rotta accetta il gettone. Corpo della replica validato (lunghezza massima, niente allegati — vedi §7);
  errori in `application/problem+json` che non distinguono «non esiste» da «non autorizzato»; limitazione di
  frequenza per gettone e per indirizzo di rete; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__ticket_access_token.sql` sullo schema `app_helpdesk`: tabella
  `ticket_access_token` con `tenant_id`, chiave primaria UUID versione 7, riferimento logico alla richiesta,
  **impronta** del gettone (mai il gettone in chiaro), data di scadenza, data di revoca, colonne di controllo e
  cancellazione logica. La replica riusa la tabella `ticket_message` esistente, con verso «in ingresso» e autore
  «richiedente».
- **RT-4 — Modulo frontend (§3, §5).** Due superfici distinte. Dentro il backoffice: lo stato del collegamento e i
  comandi di revoca e rigenerazione sulla scheda della richiesta. Fuori: la **pagina del richiedente non vive dentro
  il backoffice** — è una superficie a sé, che non carica nulla che richieda autenticazione, usa gli stessi token del
  sistema di design con il colore-categoria `teal`, funziona in tema chiaro e scuro ed è leggibile da telefono,
  perché è da lì che la aprono.
- **RT-5 — Cinque lingue (§4).** **Due elenchi da non confondere.** L'interfaccia dell'operatore (stato del
  collegamento, revoca) passa dallo spazio-nomi `helpdesk` ed è presente in `en, it, fr, es, de`. La pagina del
  richiedente è invece resa nella **lingua del richiedente** (`requester.locale`, storia `0012`), con ricaduta sulla
  lingua predefinita `en` se manca: qui il criterio non è l'elenco dell'interfaccia, è la persona che legge.
- **RT-6 — Varchi e quota (§6, §7).** Il collegamento **non occupa un posto operatore** e non consuma la metrica
  `agents`: chi lo usa non lavora sulle richieste, ne segue una sola. Non attraversa la catena dei varchi
  dell'utente, perché non c'è un utente dell'account: al suo posto valgono la firma del gettone, la sua scadenza, la
  sua revoca e il limite di frequenza. **Proposta**: con abbonamento `canceled` la pagina si spegne e mostra la
  pagina neutra — ma è un effetto verso l'esterno e la decisione è dello sviluppatore (§8).
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato: `revoca_collegamento(numero) → esito`, marcato
  **scrittura**, reversibile (il collegamento si rigenera) e interno, quindi senza conferma obbligatoria. Nessuno
  strumento **genera** né **manda** un collegamento: il collegamento esce solo dentro un messaggio, e i messaggi
  escono solo da `invia_risposta`, che è scrittura irreversibile con conferma umana obbligatoria (storia `0035`). Il
  contratto vive dentro il servizio; il server conversazionale è di piattaforma e non ancora implementato
  (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **La storia introduce dati nuovi che riguardano una persona.** Il gettone è
  legato a un richiedente e alla sua richiesta: è un dato con **scadenza** e **revoca**, e va dichiarato come tale.
  Voci nuove nel manifesto `docs/compliance/manifests/helpdesk.yaml` **in italiano e inglese** per
  `ticket_access_token` (dove vive, di chi è, che dato è, a cosa serve, perché è lecito, per quanto si tiene: la
  proposta è «fino alla scadenza, poi cancellazione automatica, comunque non oltre la conservazione della
  richiesta»); campi che riferiscono la persona annotati `@PersonalData`; tabella `ticket_access_token` aggiunta a
  `exportData` e `purgeData` del contratto `HelpdeskDataContract` — **cancellare un richiedente deve cancellare i
  suoi gettoni**, altrimenti resta in giro una chiave verso dati cancellati. L'indirizzo di rete di chi apre la
  pagina serve **solo** al limite di frequenza e **non si conserva**; se dovesse conservarsi, vale la stessa regola
  di `webform.ip` (30 giorni, cancellazione automatica) e diventa una decisione da registrare. **Nessun
  tracciamento**: niente strumenti di analisi, niente cookie non tecnici, nessun banner. Su questa app appgrove è
  **responsabile del trattamento** per conto dell'azienda cliente, non titolare: la pagina deve dirlo, con
  l'informativa fornita dal cliente (stessa regola della storia `0031`).
- **RT-9 — Registrazione eventi (§14).** Gli eventi `collegamento generato`, `collegamento revocato`, `pagina del
  richiedente aperta`, `replica ricevuta dal collegamento` e `gettone rifiutato` sono registrati con `tenant_id`,
  `app_id`, identificativo della richiesta e identificativo di correlazione. Non c'è `user_id`: il campo resta vuoto
  e **non** viene sostituito né dall'indirizzo di posta del richiedente né dal suo indirizzo di rete. Il **testo
  della replica non entra nei registri**.

## 4. Criteri di accettazione

**CA-1 — Il richiedente segue la propria richiesta senza registrarsi**
- **Dato** una richiesta aperta e un messaggio in uscita già mandato al richiedente, con lingua preferita spagnola
- **Quando** il richiedente apre il collegamento dal telefono
- **Allora** vede numero, oggetto, stato, data dell'ultimo aggiornamento e i messaggi scambiati, in spagnolo, senza
  alcuna registrazione

**CA-2 — Le note interne non compaiono mai**
- **Dato** una richiesta che contiene due messaggi con il cliente e una nota interna
- **Quando** il richiedente apre la pagina e ne ispeziona i dati ricevuti
- **Allora** la nota interna non è presente né nel testo mostrato né nella risposta del servizio

**CA-3 — La replica riapre la richiesta**
- **Dato** una richiesta in stato `risolta` · **Quando** il richiedente replica dalla pagina
- **Allora** la replica entra nel filo come messaggio in ingresso, la richiesta torna aperta secondo la macchina a
  stati e l'operatore assegnato viene avvisato

**CA-4 — Isolamento fra account con manipolazione del gettone**
- **Dato** due account `A` e `B`, ciascuno con le proprie richieste, e un gettone valido dell'account `A`
- **Quando** si presenta il gettone di `A` provando a raggiungere una richiesta di `B` (cambiando l'identificativo
  nell'indirizzo, aggiungendo un `tenant_id` nei parametri o alterando la firma del gettone)
- **Allora** il parametro forzato è **ignorato**, la firma alterata è respinta, e in tutti i casi si ottiene la
  stessa pagina neutra: `tenant_id` e richiesta vengono **solo** dal gettone verificato

**CA-5 — Scaduto, revocato, manomesso e inesistente sono indistinguibili**
- **Dato** quattro collegamenti: uno scaduto, uno revocato, uno manomesso e uno mai esistito
- **Quando** si aprono tutti e quattro
- **Allora** si ottiene la **stessa** risposta neutra, senza differenze di testo, di codice o di tempo di risposta
  apprezzabili, e nulla lascia capire se la richiesta esista

**CA-6 — Limite di frequenza sulle repliche**
- **Dato** un gettone valido che supera la soglia di repliche nella finestra
- **Quando** manda la replica successiva
- **Allora** riceve `429`, **nessun messaggio viene creato** e la richiesta non cambia stato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`; l'intera suite prima del
      commit);
- [ ] prove di **unità** sull'emissione, la verifica, la scadenza e la revoca del gettone, e di **integrazione** sulle
      due rotte pubbliche, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** in forma di **prova di sicurezza sul gettone**: gettone di un altro
      account, gettone scaduto, gettone revocato, gettone manomesso, `tenant_id` e identificativo della richiesta
      forzati dall'esterno, tentativo di leggere una nota interna — tutti respinti allo stesso modo;
- [ ] **prova end-to-end**: *coprire ora* — passi «apri il collegamento di stato» e «rispondi dalla pagina, la
      richiesta si riapre» del percorso `[J-HELPDESK]`, e registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni**: interfaccia dell'operatore in tutte e cinque le lingue (`en, it, fr, es, de`); pagina del
      richiedente resa nella lingua del richiedente con ricaduta su `en`;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con `ticket_access_token`, campi annotati
      `@PersonalData`, tabella presente in `exportData` e `purgeData`;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato con **la forma esatta del gettone, il
      suo unico permesso, il suo unico oggetto e il perché questo rispetta l'invariante numero uno**: è la decisione
      più importante dell'applicazione, e va scritta anche se sembra ovvia a chi la prende;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `revoca_collegamento`, con la nota che nessuno
      strumento manda un collegamento;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali: le rotte `/public/` sono
      raggiungibili dal proxy locale grazie alla sola scoperta automatica dei servizi;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` — filo dei messaggi e risposta | La pagina mostra il filo, e la replica ci entra dentro |
| storia `0009` — ciclo di vita degli stati | La riapertura su replica è una transizione della macchina a stati, non una regola nuova |
| storia `0015` — posta elettronica in uscita | Il collegamento viaggia dentro i messaggi in uscita: senza quel canale nessuno lo riceve |
| storia `0027` — indagine di soddisfazione | Ha già introdotto un gettone monouso per il voto: **il meccanismo dev'essere uno solo**. Se `0027` arriva prima, questa storia lo generalizza; se arriva dopo, riusa questo |
| approvazione dello sviluppatore sulla forma del gettone | È una deviazione da un invariante di piattaforma e va approvata **prima** di scrivere codice |
| epica di piattaforma non implementata (UC 0061-0063) | Il livello conversazionale non esiste: si dichiara il contratto di `revoca_collegamento`, non lo si espone |

## 7. Fuori ambito

- **L'elenco di tutte le richieste dello stesso richiedente in un'unica pagina**: fuori ambito, e non per taglia. Un
  gettone che apre più di un oggetto smette di essere un permesso e diventa un'identità: è esattamente la cosa che
  RT-1 esclude. Se un giorno servisse, sarà una storia propria con una decisione architetturale propria.
- **L'invio di allegati dalla pagina del richiedente**: rimandato. Accettare file da una superficie pubblica senza
  autenticazione è una questione di antivirus e di abuso, già aperta nella storia `0016` (punto 9 dei rischi della
  descrizione dell'applicazione). Qui la replica è solo testo.
- **La lettura degli articoli della base di conoscenza dalla stessa pagina**: la fa il portale della storia `0031`,
  con la sua superficie; qui al più si mette un rimando.
- **Il voto di soddisfazione alla chiusura**: storia `0027`.
- **La notifica al richiedente quando lo stato cambia**: fuori ambito. Il collegamento serve a guardare quando si
  vuole; mandare un messaggio a ogni cambio di stato è una decisione sul volume di posta verso l'esterno, che
  appartiene alla storia `0015`.

## 8. Punti aperti

- **🛑 Fermata di escalation — la conciliazione con l'invariante numero uno.** La proposta di RT-1 è che il gettone
  firmato **sia** un token verificato, con un solo permesso e un solo oggetto, e che `tenant_id` e identificativo
  della richiesta si ricavino da lì e **mai** da un parametro. La proposta è motivata ma **non è approvata**: la
  chiude **lo sviluppatore**, prima dell'implementazione, e la risposta va scritta nel registro delle decisioni della
  change insieme alla forma esatta del gettone (chi lo emette, con quale chiave, come si revoca).
- **La durata del gettone.** La proposta è 90 giorni dall'ultimo messaggio, rinnovati a ogni messaggio in uscita. È
  un equilibrio fra comodità del cliente finale e superficie esposta, e va allineato con la **durata di conservazione
  come parametro dell'account** della storia `0036`: non ha senso un gettone che sopravvive alla richiesta che apre.
  **La chiude lo sviluppatore** insieme alla politica di conservazione.
- **La pagina deve spegnersi quando l'abbonamento non è attivo?** Stessa domanda della storia `0031`, con un peso
  diverso: qui a restare fuori è un cliente finale che sta aspettando una risposta. **La chiude lo sviluppatore.**
- **Una sola forma di gettone in tutta l'applicazione.** Fra questa storia e la `0027` non devono nascere due
  meccanismi paralleli. Chi implementa la seconda delle due **deve** unificare, e annotarlo nel registro delle
  decisioni. Se la `0027` fosse già stata implementata con una forma diversa, l'unificazione è parte di questa
  storia e non un lavoro futuro.
