# 0035 — Strumenti di scrittura con bozza e conferma umana

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0035` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0034`, `0015`, `0020`, `0022`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come operatore dell'assistenza
> voglio poter dire «prepara una risposta a quella richiesta usando l'articolo sui resi» e poi rileggere e approvare
> io il testo prima che parta
> così da risparmiare il tempo della scrittura senza mai correre il rischio che una risposta sbagliata arrivi a un
> cliente al posto mio.

**Contesto.** La storia `0034` ha esposto la lettura, che è libera. Qui si espone la scrittura, che non lo è. La
regola di sicurezza del catalogo è netta e non negoziabile: gli strumenti di scrittura con **effetti
irreversibili** producono una **bozza** e richiedono una **conferma umana esplicita**. L'intelligenza artificiale
prepara, la persona approva. In questa applicazione l'effetto irreversibile ha un nome preciso: **un messaggio che
arriva a una persona esterna**. Una risposta sbagliata mandata a un cliente non si richiama, non si annulla e non
si spiega: è già stata letta. Per questo la storia separa in due strumenti distinti ciò che altrove sarebbe uno
solo — `prepara_risposta` scrive una bozza che resta dentro l'app, `invia_risposta` la fa uscire. Nessun percorso,
nessuna scorciatoia, nessuna preferenza dell'utente può fondere i due passi.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara sei strumenti di scrittura: `crea_richiesta`, `prepara_risposta`,
   `invia_risposta`, `assegna_richiesta`, `cambia_stato`, `inoltra_richiesta`.
2. **RF-2** — `prepara_risposta` produce una **bozza** collegata alla richiesta, visibile nell'interfaccia
   dell'operatore, che **non** è un messaggio del filo e **non** raggiunge nessuno.
3. **RF-3** — `invia_risposta` accetta solo l'identificativo di una bozza già esistente e richiede una conferma
   umana esplicita: non esiste un percorso che invii un testo passato direttamente fra i parametri.
4. **RF-4** — `crea_richiesta` produce anch'essa una bozza da confermare, perché apre una conversazione a nome di
   una persona reale e fa scattare gli orologi del livello di servizio.
5. **RF-5** — `assegna_richiesta`, `cambia_stato` e `inoltra_richiesta` hanno effetti **interni e reversibili** e
   non richiedono conferma; ognuno lascia però una traccia che dice che l'ha fatto il livello conversazionale e per
   conto di chi.
6. **RF-6** — Una bozza non confermata scade dopo un tempo definito e viene cancellata, così che non resti in giro
   testo scritto e mai letto da nessuno.
7. **RF-7** — L'interfaccia dell'operatore mostra le bozze in attesa dentro la richiesta, con il testo modificabile
   prima dell'approvazione: approvare **non** significa accettare parola per parola.

## 3. Requisiti tecnici

- **RT-1 — Esposizione conversazionale (§12).** Strumenti dichiarati `nome(parametri) → risultato`, marcati
  **scrittura**; `invia_risposta` è marcato **scrittura irreversibile** e produce l'effetto solo dopo conferma
  umana. Il contratto vive dentro il servizio; il server conversazionale è di piattaforma e non ancora
  implementato (UC 0061-0063).
- **RT-2 — Isolamento fra account (§1).** Ogni scrittura filtra per `tenant_id` preso dal token verificato; una
  bozza appartiene all'account e all'utente che l'ha chiesta, e non è confermabile da nessun altro account.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema `app_helpdesk`: tabella delle bozze con `tenant_id`, chiave
  primaria UUID versione 7, riferimento alla richiesta, autore, corpo, data di scadenza, colonne di controllo e
  cancellazione logica.
- **RT-4 — Interfaccia di programmazione (§2).** Rotte `POST /api/helpdesk/v1/tickets/{id}/drafts`,
  `POST /api/helpdesk/v1/drafts/{id}/confirm`, `DELETE /api/helpdesk/v1/drafts/{id}`; corpo validato; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-5 — Varchi e quota (§6, §7).** Le chiamate provenienti dal livello conversazionale attraversano gli stessi
  varchi delle chiamate dall'interfaccia: con abbonamento non attivo `402`, con ruolo insufficiente `403`. Solo chi
  occupa un posto operatore può confermare l'invio di una risposta.
- **RT-6 — Modulo frontend (§3, §5).** Le bozze in attesa compaiono nel dettaglio della richiesta, distinte a colpo
  d'occhio dai messaggi veri; solo token del sistema di design; tema chiaro e scuro.
- **RT-7 — Cinque lingue (§4).** Tutte le stringhe della sezione delle bozze passano dallo spazio-nomi `helpdesk` e
  sono presenti in `en, it, fr, es, de`.
- **RT-8 — Dati personali (§10).** Voci nuove nel manifesto `docs/compliance/manifests/helpdesk.yaml` in italiano e
  inglese per il corpo della bozza, che è testo libero e può contenere qualsiasi cosa; campo annotato
  `@PersonalData`; tabella delle bozze aggiunta a `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** «Bozza creata», «bozza confermata», «bozza scaduta» e «risposta inviata» si
  registrano con `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e origine della chiamata
  (interfaccia o livello conversazionale), **senza** il testo.

## 4. Criteri di accettazione

**CA-1 — Preparare non è inviare**
- **Dato** una richiesta aperta
- **Quando** si chiama `prepara_risposta`
- **Allora** compare una bozza nel dettaglio della richiesta, **nessun** messaggio viene aggiunto al filo e
  **nessun** messaggio di posta parte

**CA-2 — L'invio richiede una conferma umana**
- **Dato** una bozza esistente
- **Quando** si chiama `invia_risposta` senza che una persona abbia confermato
- **Allora** l'operazione è respinta e la bozza resta tale

**CA-3 — Non esiste una scorciatoia**
- **Dato** il livello conversazionale
- **Quando** si tenta di chiamare `invia_risposta` passando direttamente un testo invece dell'identificativo di una
  bozza
- **Allora** la chiamata è respinta con un errore di validazione: lo strumento non accetta testo libero

**CA-4 — L'operatore può correggere prima di approvare**
- **Dato** una bozza il cui testo non convince
- **Quando** l'operatore la modifica e poi conferma
- **Allora** il messaggio che parte è quello corretto, e la traccia registra che la bozza era stata preparata dal
  livello conversazionale e modificata da una persona

**CA-5 — Le azioni interne non chiedono conferma**
- **Dato** una richiesta assegnata all'operatore A
- **Quando** si chiama `assegna_richiesta` verso l'operatore B
- **Allora** l'assegnazione cambia subito, senza conferma, ed è reversibile con un'altra chiamata

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, e una bozza creata in `A`
- **Quando** un utente di `B` tenta di confermarla usandone l'identificativo
- **Allora** riceve «non trovata» e la bozza di `A` resta intatta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul ciclo di vita della bozza e sulla scadenza, e di **integrazione** sulle rotte con
      database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulle bozze e sulla conferma;
- [ ] **prova end-to-end**: la storia `0037` estende il percorso `[J-HELPDESK]` con il passo «bozza preparata,
      corretta, approvata, inviata» e possiede la voce del registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con il corpo della bozza, campo annotato, tabella in
      esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotata in modo esplicito la separazione fra `prepara_risposta` e
      `invia_risposta` e il perché non è negoziabile;
- [ ] contratto degli **strumenti conversazionali** dichiarato per tutte le funzioni di scrittura dell'applicazione;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0034` di questa app | Il contratto degli strumenti nasce lì: qui si aggiunge la parte che scrive |
| Storia `0015` di questa app | `invia_risposta` fa partire un messaggio di posta vero: senza quella storia non c'è niente da inviare |
| Storia `0020` di questa app | `assegna_richiesta` presuppone che l'assegnazione esista |
| Storia `0022` di questa app | Una bozza costruita a partire da una risposta predefinita è il caso d'uso più frequente |
| Epica di piattaforma `12-ready-for-ai-mcp`, UC 0061-0063 | Il server conversazionale non esiste ancora: il contratto si scrive e si prova dentro il servizio, il presidio della bozza funziona comunque perché è dell'app, non del server |

## 7. Fuori ambito

- **La pubblicazione automatica di un articolo della base di conoscenza**: anche quella è visibile al pubblico e
  richiede conferma; la governa la storia `0029` e nessuno strumento la esegue da solo.
- **Risposte automatiche al cliente finale senza persona nel mezzo**: non è rimandato, è **escluso**. Se un giorno
  qualcuno lo volesse, sarebbe una decisione di direzione di prodotto e cambierebbe la natura dell'applicazione.
- **La cancellazione di richieste o di dati** dal livello conversazionale: non è esposta come strumento. La
  cancellazione dei dati di una persona è la storia `0036` e passa da un percorso deliberato, non da una chat.

## 8. Punti aperti

- **Quanto vive una bozza non confermata**: la proposta è pochi giorni, poi cancellazione automatica. Il numero
  esatto è una scelta di prodotto che tocca anche la conservazione (storia `0036`): lo chiude lo sviluppatore.
- **Chi può confermare l'invio**: la proposta è «chiunque occupi un posto operatore». Se il cliente volesse che le
  risposte preparate dal livello conversazionale fossero approvate solo da un responsabile, servirebbe un ruolo in
  più — e i ruoli sono materia di piattaforma. Punto aperto per lo sviluppatore.
- ⚠️ **Il confine fra «preparare» e «decidere»**: uno strumento che prepara una risposta a partire dallo storico
  del cliente sta usando dati personali di terzi per produrre testo. Chi sia il titolare di quella elaborazione e
  con quale base giuridica è una domanda che appartiene alla revisione legale e a UC 0061-0062, non a questa
  storia. Va posta, non risolta qui.
