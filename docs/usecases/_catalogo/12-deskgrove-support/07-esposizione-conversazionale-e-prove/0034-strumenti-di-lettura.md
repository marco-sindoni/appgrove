# 0034 — Strumenti di lettura per il livello conversazionale

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0034` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0007`, `0028`, `0029`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un'azienda che ha l'assistenza clienti dentro appgrove
> voglio poter chiedere a voce «quali richieste sono in ritardo?» e «riassumimi la conversazione con quel cliente»
> così da sapere come sta andando l'assistenza senza aprire l'applicazione e senza leggere quaranta messaggi.

**Contesto.** Il catalogo pone a tutte le sessanta applicazioni un requisito trasversale: ogni funzione dev'essere
comandabile da una chat. Nel repository il livello conversazionale **non esiste ancora** — è l'epica
`12-ready-for-ai-mcp` (UC 0061-0066), scritta e non implementata. Il compito di questa storia non è costruire il
server: è **dichiarare il contratto** degli strumenti che leggono, e tenerlo dentro il servizio dell'app,
versionato insieme ad essa. Si comincia dalla lettura perché è la parte libera: leggere non produce effetti, e un
contratto di lettura scritto bene è ciò che rende ovvio, poi, quanto la scrittura vada trattata diversamente
(storia `0035`).

Una avvertenza specifica di questa applicazione: gli strumenti leggono **dati di persone che non sono nostre
utenti** — i clienti finali dell'azienda cliente. Non è un motivo per non esporli, perché chi interroga è l'azienda
titolare di quei dati; è un motivo per **restituire il meno possibile**.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara cinque strumenti di sola lettura: `elenca_richieste`, `leggi_richiesta`,
   `riassumi_richiesta`, `cerca_articoli`, `stato_del_servizio`.
2. **RF-2** — Ogni strumento porta nome stabile, descrizione in lingua naturale, schema dei parametri, schema del
   risultato, marcatura **lettura** e dichiarazione di idempotenza.
3. **RF-3** — `elenca_richieste` accetta stato, coda, operatore, «oltre la scadenza» e periodo, e restituisce un
   elenco **minimizzato**: numero, oggetto, stato, scadenza, operatore assegnato — **mai** il corpo dei messaggi.
4. **RF-4** — `leggi_richiesta` restituisce la conversazione completa di una singola richiesta, note interne
   comprese, e solo su richiesta esplicita di quel numero.
5. **RF-5** — `riassumi_richiesta` restituisce una sintesi, i punti rimasti aperti e il prossimo passo suggerito,
   costruita **dai dati dell'app** e senza mandare il contenuto a nessun servizio esterno di analisi del testo.
6. **RF-6** — `stato_del_servizio` restituisce solo numeri aggregati — richieste aperte, tempo medio di prima
   risposta, scadenze violate, soddisfazione media — e nessun dato che identifichi una persona.

## 3. Requisiti tecnici

- **RT-1 — Esposizione conversazionale (§12).** Gli strumenti sono dichiarati `nome(parametri) → risultato` e
  marcati **lettura**; il contratto vive **dentro** il servizio `helpdesk`, in un pacchetto dedicato, versionato con
  esso. Il server conversazionale è di piattaforma e **non è ancora implementato**: dipendenza dichiarata verso
  UC 0061-0063.
- **RT-2 — Isolamento fra account (§1).** Ogni strumento risolve l'account dal token verificato ottenuto dal
  livello conversazionale; un identificativo di account che arrivasse fra i parametri dello strumento viene
  **ignorato**. Un numero di richiesta che appartiene a un altro account risponde «non trovata», non «non
  autorizzato»: non si conferma nemmeno l'esistenza.
- **RT-3 — Varchi e quota (§6, §7).** Anche le chiamate che arrivano dal livello conversazionale attraversano
  abilitazione e ruolo: con abbonamento non attivo lo strumento risponde con l'errore di abilitazione, non con
  dati. Gli strumenti di lettura non consumano la metrica `agents`, che è a giacenza e conta le persone abilitate a
  lavorare, non le interrogazioni.
- **RT-4 — Minimizzazione dei dati (§10).** È il requisito che distingue questa app: `elenca_richieste` e
  `stato_del_servizio` **non** restituiscono contenuto dei messaggi né recapiti dei richiedenti. Il contenuto esce
  solo da `leggi_richiesta` e `riassumi_richiesta`, cioè quando qualcuno ha chiesto esplicitamente **quella**
  conversazione. Nessuna voce nuova nel manifesto: gli strumenti non creano dati, li leggono.
- **RT-5 — Interfaccia di programmazione (§2).** Gli strumenti si appoggiano ai servizi di dominio già esistenti,
  non a interrogazioni scritte apposta che aggirerebbero i controlli delle risorse.
- **RT-6 — Registrazione eventi (§14).** Ogni chiamata a uno strumento si registra con `tenant_id`, `app_id`,
  `user_id`, identificativo di correlazione e **nome dello strumento**; mai i parametri liberi né il risultato,
  che conterrebbero il contenuto delle conversazioni.
- **RT-7 — Cinque lingue (§4).** Le descrizioni in lingua naturale degli strumenti sono rivolte al modello, non
  all'interfaccia: restano in inglese come identificatori tecnici, mentre ogni testo che l'utente legge come
  risposta passa dalle traduzioni. La distinzione va scritta, perché è la fonte di equivoci più probabile.

## 4. Criteri di accettazione

**CA-1 — L'elenco è minimizzato**
- **Dato** un account con richieste che contengono messaggi lunghi
- **Quando** si chiama `elenca_richieste` con stato «aperta»
- **Allora** il risultato contiene numero, oggetto, stato, scadenza e operatore, e **nessun** corpo di messaggio né
  indirizzo di posta del richiedente

**CA-2 — La sintesi non esce dall'applicazione**
- **Dato** una richiesta con dodici messaggi
- **Quando** si chiama `riassumi_richiesta`
- **Allora** si ottiene una sintesi con i punti aperti, e nessuna chiamata verso un servizio esterno di analisi del
  testo compare nelle chiamate in uscita del servizio

**CA-3 — Isolamento fra account**
- **Dato** due account `A` e `B`, e la richiesta numero `7` che esiste in entrambi
- **Quando** un utente di `A` chiama `leggi_richiesta` con il numero `7` e forza fra i parametri l'identificativo di
  `B`
- **Allora** riceve la propria richiesta numero `7`: il parametro forzato viene ignorato

**CA-4 — Richiesta inesistente**
- **Dato** un account `A`
- **Quando** si chiama `leggi_richiesta` con un numero che esiste solo nell'account `B`
- **Allora** la risposta è «non trovata», identica a quella di un numero che non esiste da nessuna parte

**CA-5 — Abbonamento non attivo**
- **Dato** un account con abbonamento disdetto e periodo concluso
- **Quando** si chiama un qualunque strumento di lettura
- **Allora** la risposta è l'errore di abilitazione e nessun dato viene restituito

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla forma dei risultati e sulla minimizzazione dei campi, e di **integrazione** sugli
      strumenti con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su ogni strumento introdotto;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-HELPDESK]` e la voce del registro di copertura sono della
      storia `0037`; il livello conversazionale non è percorribile da un browser perché non esiste ancora;
- [ ] **traduzioni**: non applicabile ai nomi e alle descrizioni degli strumenti, che sono identificatori tecnici;
- [ ] **manifesto dei dati**: nessuna voce nuova — gli strumenti leggono dati già dichiarati;
- [ ] **registro delle decisioni** compilato, con annotata la regola di minimizzazione e il perché `elenca_richieste`
      non restituisce il contenuto;
- [ ] contratto degli **strumenti conversazionali** dichiarato per tutte le funzioni di lettura introdotte fin qui
      dall'applicazione;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0007` e `0010` di questa app | Servono il filo dei messaggi e l'elenco con i filtri: gli strumenti li interrogano, non li reimplementano |
| Storia `0028` di questa app | `stato_del_servizio` restituisce gli stessi numeri del cruscotto: una sola fonte, due presentazioni |
| Storia `0029` di questa app | `cerca_articoli` ha senso solo se esistono articoli |
| Epica di piattaforma `12-ready-for-ai-mcp`, UC 0061-0063 | Il server conversazionale, l'autenticazione delegata e la mappatura operazioni → strumenti **non esistono ancora**. Nel frattempo il contratto si scrive, si versiona e si prova dentro il servizio: quando il server arriverà, troverà l'app già pronta |

## 7. Fuori ambito

- **Gli strumenti che scrivono**: sono la storia `0035`, e la separazione è deliberata.
- **Il server conversazionale, l'autenticazione delegata, il consenso**: sono di piattaforma (UC 0061-0062).
- **Qualunque forma di risposta automatica al cliente finale**: non è un'omissione, è un divieto — vedi `0035`.

## 8. Punti aperti

- **Come si comporta `riassumi_richiesta` con una conversazione molto lunga**: se il riassunto lo produce il
  modello del livello conversazionale, il contenuto passa comunque da lui. È una questione che appartiene a
  UC 0061-0062 (dove vive il modello, con quali garanzie, con quale contratto), non a questa storia: qui si
  dichiara soltanto che **l'applicazione** non manda nulla a servizi terzi di propria iniziativa.
- **Quota delle chiamate conversazionali**: la piattaforma prevede di applicare abilitazione e quota anche alle
  chiamate dell'assistente (UC 0064). La metrica di questa app è a giacenza sui posti operatore e non si presta a
  contare interrogazioni: se servisse un tetto sulle chiamate, sarebbe una decisione di piattaforma, non dell'app.
