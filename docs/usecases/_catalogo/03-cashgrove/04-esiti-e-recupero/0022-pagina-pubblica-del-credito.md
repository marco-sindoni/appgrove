# 0022 — Pagina pubblica del credito

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 04 — Esiti e recupero
**Storia**: `0022` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come debitore che riceve un sollecito
> voglio vedere in una pagina che cosa mi viene chiesto, per quali documenti, e come pagare
> così da poter saldare subito invece di dover cercare la fattura e telefonare per chiedere l'IBAN.

**Contesto.** Tutti i prodotti della categoria offrono un «portale di pagamento» ([documento capofila](../application-description.md)
§2.1). Il termine però nasconde due cose molto diverse: mostrare al debitore che cosa deve, e **incassare** il suo
denaro. La prima è utile e a costo zero; la seconda farebbe di appgrove un intermediario che maneggia denaro di terzi —
fuori dal perimetro dichiarato dell'app e fuori dal modello della piattaforma, dove il fornitore di pagamento serve gli
abbonamenti, non gli incassi dei clienti dei nostri clienti. Questa storia fa la prima e **dichiara** di non fare la
seconda.

## 2. Requisiti funzionali

1. **RF-1** — Ogni sollecito può contenere un collegamento a una pagina pubblica che mostra il riepilogo di ciò che il
   debitore deve a quel creditore: documenti, importi, scadenze, residuo totale.
2. **RF-2** — La pagina mostra le coordinate di pagamento del **creditore** (conto e causale suggerita), non di
   appgrove; nessun pagamento avviene sulla pagina.
3. **RF-3** — Il debitore può, dalla pagina, dichiarare di aver pagato (indicando data e riferimento) oppure segnalare
   una contestazione: entrambe arrivano al creditore come **segnalazioni da verificare**, mai come fatti.
4. **RF-4** — L'accesso alla pagina avviene con un collegamento non indovinabile, valido per un tempo limitato e
   revocabile; non richiede registrazione e non crea un utente.
5. **RF-5** — La pagina è nella lingua preferita del debitore, fra quelle disponibili.
6. **RF-6** — La pagina mostra solo i crediti di **quel** debitore verso **quel** creditore, e nulla che permetta di
   dedurre l'esistenza di altri.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Questa è la sola superficie dell'app raggiungibile **senza token**: il
  `tenant_id` non arriva da un token ma è **legato al collegamento** e verificato lato servizio. Vale con più forza la
  regola: nessun identificativo di account o di debitore è accettato da parametri o corpo. Prova di isolamento
  obbligatoria: un collegamento di un account non deve mostrare nulla di un altro, nemmeno per errore di indice.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte pubbliche `GET /api/crediti/v1/pubblico/{gettone}` e
  `POST /api/crediti/v1/pubblico/{gettone}/segnalazione`, con limitazione della frequenza per gettone e per indirizzo di
  provenienza; errori in `application/problem+json` **senza rivelare** se un gettone è scaduto o inesistente.
- **RT-3 — Persistenza (§8).** Migrazioni per le tabelle `gettone_pubblico` (valore cifrato o impronta, credito o
  debitore, scadenza, revoca) e `segnalazione_debitore` sullo schema `app_crediti`, con `tenant_id`, chiave UUID
  versione 7, colonne di controllo e cancellazione logica. Il valore del gettone **non si conserva in chiaro**.
- **RT-4 — Modulo frontend (§3, §5).** La pagina pubblica **non** sta nel modulo del backoffice: è una superficie
  separata, minima, senza barra laterale e senza accesso alla shell. Usa gli stessi token del sistema di design e
  funziona in tema chiaro e scuro. Nel backoffice si aggiungono solo l'interruttore che abilita il collegamento nei
  solleciti e l'elenco delle segnalazioni ricevute.
- **RT-5 — Cinque lingue (§4).** La pagina pubblica esiste in `en, it, fr, es, de`, scelta secondo la lingua preferita
  del debitore, con ripiego sulla lingua predefinita dell'account.
- **RT-6 — Varchi e quota (§6, §7).** La pagina pubblica non consuma quota. Se l'abbonamento del creditore è
  `canceled`, i collegamenti smettono di funzionare e la pagina lo dice senza esporre dati.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento: la generazione di un collegamento pubblico è un atto
  che espone dati verso l'esterno e resta fuori dagli strumenti anche dopo la storia `0029`. Scelta esplicita, annotata.
- **RT-8 — Dati personali (§10).** È il punto in cui i dati del debitore sono **accessibili senza autenticazione**: la
  protezione è tutta nel gettone. Conseguenze obbligate: gettone lungo e casuale, scadenza breve, revoca, limitazione
  della frequenza, nessun contenuto indicizzabile dai motori di ricerca, nessuna informazione di più di quanto serva.
  Voci nuove nel manifesto per `gettone_pubblico` e `segnalazione_debitore`, tabelle presenti in `exportData` e
  `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «pagina pubblica aperta», «segnalazione ricevuta», «gettone
  scaduto» sono registrati con `tenant_id`, `app_id`, identificativi e identificativo di correlazione; **mai** il
  gettone, mai l'indirizzo di provenienza in chiaro oltre il tempo necessario alla limitazione della frequenza.

## 4. Criteri di accettazione

**CA-1 — La pagina mostra il dovuto**
- **Dato** un sollecito inviato con collegamento e un debitore con tre documenti scaduti
- **Quando** il debitore apre il collegamento
- **Allora** vede i tre documenti, il totale, le coordinate del creditore e la causale suggerita

**CA-2 — Nessun incasso sulla pagina**
- **Dato** la pagina pubblica · **Quando** il debitore cerca un modo di pagare lì · **Allora** non esiste: la pagina
  mostra come pagare il creditore e dichiara che appgrove non riceve denaro

**CA-3 — Gettone scaduto o revocato**
- **Dato** un collegamento scaduto · **Quando** lo si apre · **Allora** si ottiene la stessa risposta di un collegamento
  inesistente, senza distinguere i due casi e senza mostrare alcun dato

**CA-4 — Segnalazione di pagamento**
- **Dato** un debitore sulla pagina · **Quando** dichiara di aver pagato il 12/09 con riferimento «bonifico 4471» ·
  **Allora** il creditore riceve la segnalazione nel backoffice come **da verificare**, e nessun incasso viene
  registrato in automatico

**CA-5 — Isolamento fra account**
- **Dato** due account con debitori omonimi e collegamenti attivi · **Quando** si apre il collegamento del primo ·
  **Allora** si vedono solo i suoi crediti, e nessuna manipolazione del percorso mostra quelli del secondo

**CA-6 — Nessuna indicizzazione**
- **Dato** la pagina pubblica · **Quando** la si esamina · **Allora** dichiara di non voler essere indicizzata e non
  espone contenuti a chi non ha il gettone

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sulla generazione e verifica del gettone, di **integrazione** sulla rotta pubblica compresa la
      limitazione della frequenza;
- [ ] prova di **isolamento fra account** rafforzata: è la sola superficie senza token dell'app;
- [ ] **prova end-to-end**: *coprire ora* — «il debitore apre il collegamento e vede il dovuto» entra nel percorso
      `[J-CREDITI]` della storia `0031`; voce registrata nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, compresa la pagina pubblica;
- [ ] **manifesto dei dati** aggiornato con le due tabelle nuove, presenti in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta di non incassare e sulle protezioni del
      gettone;
- [ ] contratto degli **strumenti conversazionali**: esclusione deliberata, annotata con il motivo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0014` | Il collegamento viaggia dentro il sollecito |
| storia `0017` | L'apertura della pagina è un fatto che entra nella cronologia del credito |

## 7. Fuori ambito

- **L'incasso vero sulla pagina** (carta, addebito diretto, bollettino): renderebbe appgrove un intermediario nel
  denaro di terzi. Fuori dal perimetro dell'app e dal modello della piattaforma. Se lo sviluppatore volesse cambiarlo,
  è una decisione di direzione di prodotto con conseguenze regolamentari, non una storia.
- La riconciliazione automatica della segnalazione di pagamento con il movimento bancario: punto aperto n. 10 del
  documento capofila §11.
- Un'area riservata permanente per il debitore: non serve, e creerebbe utenti che non sono nostri clienti.

## 8. Punti aperti

**La durata di validità del collegamento** (proposta: 30 giorni, rinnovabile a ogni sollecito) è una scelta di
sicurezza da confermare: troppo corta rende la pagina inutile, troppo lunga allarga la finestra di esposizione.
**Decide lo sviluppatore.**
