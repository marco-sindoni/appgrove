# 0025 — Percorso automatico a passi

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 05 — Automazioni
**Storia**: `0025` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0004`, `0013`, `0014`, `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che vorrebbe dare il benvenuto a chi si iscrive senza doverci pensare ogni volta
> voglio definire una sequenza di passi «aspetta / manda» e attivarla
> così da avere una comunicazione che parte da sola nel momento giusto, senza che io sia davanti al computer.

**Contesto.** È la funzione che il cliente chiede subito dopo aver mandato la prima campagna, e la funzione su cui
la categoria sbaglia di più. L'analisi delle lamentele ricorrenti
([application-description.md](../application-description.md) §2.5) dice una cosa netta: *«le automazioni avanzate
hanno una curva di apprendimento ripida»*, e chi è in due persone non costruisce un diagramma a rami. Perciò qui non
si fa un editore a grafo: si fa una **sequenza lineare**, un passo dopo l'altro, leggibile dall'alto in basso come
un elenco. Un percorso lineare copre il caso reale — benvenuto, seconda comunicazione dopo tre giorni, richiamo dopo
un mese — e non produce quei percorsi impossibili da capire che poi nessuno osa spegnere. La condizione, se serve, è
una sola e semplice: «se accade questo, salta il passo».

## 2. Requisiti funzionali

1. **RF-1** — Un percorso automatico ha un nome, un canale e una **sequenza ordinata di passi**. I tipi di passo
   ammessi sono tre: **aspetta** (una durata, oppure un momento del giorno), **manda** (un messaggio dell'account,
   storia 0014) e **condizione semplice** («se l'iscritto è nel segmento X, salta il prossimo passo»). Non esistono
   diramazioni: la sequenza è una sola.
2. **RF-2** — I passi si aggiungono, si riordinano e si eliminano finché il percorso è in bozza. Un percorso senza
   almeno un passo «manda» non si attiva.
3. **RF-3** — Il percorso ha due stati soli, **attivo** e **fermo**. Si attiva e si ferma da un unico comando, e
   l'effetto è dichiarato prima: attivandolo, chi ha già le condizioni d'avvio non entra retroattivamente
   (l'avvio è la storia 0026); fermandolo, chi è già dentro esce con il motivo scritto (la storia 0027).
4. **RF-4** — **Ogni invio del percorso consuma quota come una campagna**: una unità della metrica `messages_sent`
   per destinatario. Il percorso mostra una stima di consumo mensile basata su quanti iscritti sono entrati nel
   mese precedente, così che nessuno scopra il tetto a percorso attivo.
5. **RF-5** — Modificare un percorso **attivo** è possibile solo sui passi che nessuno degli iscritti dentro ha già
   superato; per il resto si ferma, si modifica e si riattiva. La regola è dichiarata nell'interfaccia, non
   scoperta a errore.
6. **RF-6** — Le automazioni sono una funzionalità del piano alto ([descrizione](../application-description.md) §5):
   se il piano dell'account non le comprende, la sezione è visibile ma inerte e il servizio risponde `402` con
   l'indicazione del piano che le comprende.
7. **RF-7** — Un percorso si duplica; la copia nasce **ferma** e senza nessuno dentro.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Percorsi, passi ed esecuzioni filtrano per `tenant_id` preso dal token
  verificato; un `tenant_id` dal corpo della richiesta o dai parametri viene ignorato. La lavorazione periodica che
  fa avanzare i passi lavora **per account**, e non esiste nessuna interrogazione che attraversi più account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/campaigns/v1/automations`,
  `GET|PUT /api/campaigns/v1/automations/{id}`, `POST /api/campaigns/v1/automations/{id}/activate` e
  `/deactivate`, `POST /api/campaigns/v1/automations/{id}/duplicate`. Corpo validato: i tipi di passo sono un elenco
  chiuso. Errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__automation.sql` sullo schema `app_campaigns`: tabelle `automation`
  e `automation_step` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
  La tabella `automation_run` — il percorso del singolo iscritto — nasce con la storia 0026, che è quella che ci
  mette dentro le persone.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Percorsi automatici» del modulo `campaigns`: elenco dei percorsi con
  stato, editore a elenco verticale dei passi, comando di attivazione con il riepilogo degli effetti. Solo token del
  sistema di design (colore-categoria `violet`); tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe — nomi dei tipi di passo, unità di durata, testo del riepilogo
  degli effetti dell'attivazione — passano dallo spazio-nomi `campaigns` in `en, it, fr, es, de`. I nomi dei
  percorsi scritti dal cliente non si traducono.
- **RT-6 — Varchi e quota (§6, §7).** Prima di ogni invio di un passo il servizio prenota una unità della metrica
  `messages_sent` (natura `flow`); a quota esaurita l'invio non parte e il percorso si sospende con il motivo
  (storia 0027), invece di perdere il passo in silenzio. Il varco di **abilitazione alla funzionalità** vale per
  tutta la sezione: piano senza automazioni → `402`.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato: `elenca_percorsi() → percorsi con stato,
  numero di passi e quanti iscritti sono dentro`, marcato **lettura**. **Attivare** un percorso non è esposto alla
  chat: è un comando che mette in moto invii ripetuti nel tempo verso persone, cioè esattamente il tipo di effetto
  che non deve poter partire da una frase. La scelta è dichiarata. Il contratto vive dentro il servizio; il server
  conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo in questa storia: il percorso è configurazione. Le
  persone entrano con la storia 0026, che dichiara la voce `automation_run.*` nel manifesto
  `docs/compliance/manifests/campaigns.yaml`. Va comunque scritto adesso, nel manifesto, che le tabelle
  `automation` e `automation_step` contengono **contenuti del cliente** e rientrano in `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** «Percorso creato», «percorso attivato», «percorso fermato», «passo
  eseguito», «invio del passo respinto per quota» con `tenant_id`, `app_id`, `user_id` (o l'indicazione che
  l'esecuzione è automatica), identificativo di correlazione e identificativi delle entità. Mai nomi, mai recapiti,
  mai contenuti.

## 4. Criteri di accettazione

**CA-1 — Sequenza lineare, non un grafo**
- **Dato** un percorso in bozza
- **Quando** l'utente aggiunge tre passi: aspetta 1 giorno, manda il messaggio di benvenuto, aspetta 5 giorni,
  manda il secondo messaggio
- **Allora** il percorso si salva come una sequenza ordinata di quattro passi, e non esiste nessun modo di creare
  una diramazione

**CA-2 — Attivazione con effetti dichiarati**
- **Dato** un percorso con almeno un passo «manda»
- **Quando** l'utente lo attiva
- **Allora** prima della conferma legge che chi ha già le condizioni d'avvio **non** entra retroattivamente e che
  gli invii consumeranno quota; dopo la conferma il percorso è in stato attivo

**CA-3 — Percorso senza invii non si attiva**
- **Dato** un percorso composto di soli passi «aspetta»
- **Quando** l'utente prova ad attivarlo
- **Allora** riceve `400` con la spiegazione che serve almeno un passo «manda»

**CA-4 — Piano senza automazioni**
- **Dato** un account con un piano che non comprende le automazioni
- **Quando** un utente prova a creare o attivare un percorso
- **Allora** riceve `402` con l'indicazione del piano che le comprende, e nulla viene creato

**CA-5 — Quota esaurita durante un passo**
- **Dato** un account che ha esaurito il tetto mensile di `messages_sent` e un percorso attivo con iscritti dentro
- **Quando** arriva il momento di un passo «manda»
- **Allora** l'invio non parte, il percorso di quell'iscritto si sospende con il motivo «quota esaurita» e l'evento
  è registrato; nessun messaggio parte e nessun passo viene saltato in silenzio

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri percorsi
- **Quando** un utente di `A` chiede o prova ad attivare un percorso di `B`
- **Allora** riceve `404` e nessuna attivazione avviene

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione della sequenza dei passi e sul calcolo della stima di consumo, e di
      **integrazione** sulle rotte del percorso, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su percorsi e attivazione;
- [ ] prova che il varco di **abilitazione alla funzionalità** risponde `402` con il piano d'ingresso;
- [ ] **prova end-to-end**: rimando — il percorso `[J-CAMPAIGNS]` (storia 0037) copre la catena consenso → campagna
      → invio → disiscrizione; l'automazione entra come voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml), con motivo (richiede l'attesa fra
      due passi, che va simulata) e storia proprietaria 0037;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `automation` e `automation_step`, tabelle in
      esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotato perché la sequenza è lineare e non a rami;
- [ ] contratto degli **strumenti conversazionali**: `elenca_percorsi` in lettura, attivazione **non** esposta con
      la motivazione scritta;
- [ ] controllo automatico di **accessibilità** verde sull'editore dei passi;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0004` | Il consumo di quota e i varchi esistono da lì; qui si aggiunge il varco sulla funzionalità del piano |
| Storia `0013` | La condizione semplice si esprime come appartenenza a un segmento |
| Storia `0014` | Un passo «manda» manda un messaggio, che deve esistere |
| Storia `0019` | L'invio vero passa dalla stessa coda di spedizione: un percorso non si costruisce una via propria |

## 7. Fuori ambito

- **come** un iscritto entra nel percorso: è la storia 0026;
- come e perché ne esce: è la storia 0027;
- la vista di chi è dentro e cosa è fallito: è la storia 0028;
- le diramazioni, i punti d'incontro e i percorsi a grafo: **esclusi per scelta**, non rimandati (§2.5 della
  descrizione dell'applicazione);
- i percorsi automatici sui canali aggiuntivi: possibili solo se il canale è attivo con il contratto del cliente
  (storie 0022-0024); la verifica sta nella storia 0027.

## 8. Punti aperti

- **Se un percorso attivo debba poter essere modificato del tutto.** La proposta (modificare solo i passi non ancora
  superati) è prudente ma può sembrare arbitraria a chi la incontra. L'alternativa — versionare il percorso e far
  finire chi è dentro sulla versione con cui è entrato — è più corretta e costa una storia in più. Chiude lo
  sviluppatore.
- **Durata massima di un passo «aspetta».** Un'attesa di sei mesi tiene aperte esecuzioni per sei mesi e le rende
  ingestibili. Proposta: un tetto dichiarato, per esempio 90 giorni. È una scelta di prodotto.
