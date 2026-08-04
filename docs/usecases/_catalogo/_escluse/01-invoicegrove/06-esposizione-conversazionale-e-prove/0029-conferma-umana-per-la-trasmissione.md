# 0029 — Conferma umana per la trasmissione

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`, `0018`, `0023`, `0028`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio essere **certo** che nessun assistente possa mandare una fattura all'Agenzia delle Entrate al posto mio
> così da poter usare l'intelligenza artificiale senza avere paura di quello che potrebbe fare mentre non guardo.

**Contesto.** Il catalogo lo dice con parole sue, ed è l'esempio che sceglie per primo: le azioni di scrittura con
effetti irreversibili — **trasmissione di una fattura a un'autorità fiscale**, esecuzione di un pagamento,
cancellazione di dati, accesso a un segreto — «non devono mai essere eseguibili direttamente da un agente senza
conferma umana esplicita» (§8). InvoiceGrove è l'app in cui quell'esempio è letterale: dopo l'accettazione del
Sistema di Interscambio la fattura **esiste giuridicamente** e l'unico rimedio è una nota di credito.

Questa storia costruisce il varco: non una casella da spuntare dentro i parametri dello strumento — che un modello
può riempire da solo — ma un **atto separato, umano, tracciato**. È anche, come nota il catalogo, un buon
argomento di vendita: l'intelligenza artificiale prepara, la persona approva.

## 2. Requisiti funzionali

1. **RF-1** — Gli strumenti con effetti irreversibili sono `submit_document` (trasmissione) e `archive_document`
   (versamento in conservazione); nessun altro strumento produce effetti verso l'esterno.
2. **RF-2** — Invocare uno di questi strumenti **non esegue nulla**: produce una **richiesta di conferma** con un
   riepilogo di cosa sta per accadere, a chi, con quale effetto e con quale irreversibilità.
3. **RF-3** — La conferma è un **atto umano separato**, compiuto nell'interfaccia dell'app, da un utente con ruolo
   sufficiente; il segno di conferma **non** è producibile dallo strumento né dal chiamante.
4. **RF-4** — La richiesta di conferma **scade**: dopo un tempo breve va rifatta, così che non resti in giro
   un'autorizzazione dimenticata.
5. **RF-5** — Ogni conferma è tracciata con chi, quando, cosa e da quale richiesta è nata: è la catena di
   responsabilità, ed è ciò che si esibisce se qualcuno chiede «chi ha mandato questa fattura?».
6. **RF-6** — La stessa richiesta di conferma non può essere usata due volte.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Richiesta e conferma appartengono all'account del documento, filtrato
  per `tenant_id` preso dal token verificato; una conferma non può essere spesa su un documento di un altro
  account. Prova di isolamento obbligatoria e rafforzata.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `POST /api/einvoicing/v1/confirmations` (creazione della richiesta, invocata dallo strumento) e
  `POST /api/einvoicing/v1/confirmations/{id}/approve` (atto umano). L'esecuzione avviene **solo** dopo
  l'approvazione. Errori in `application/problem+json` con un tipo di problema dedicato «richiesta di conferma in
  attesa», perché l'agente deve saperlo riferire all'utente. Definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V23__confirmation_request.sql`: tabella delle richieste di conferma con
  strumento, perimetro, riepilogo, stato, scadenza, autore della richiesta e autore dell'approvazione; `tenant_id`,
  chiave UUID versione 7, colonne di controllo. Nessuna cancellazione logica: è catena di responsabilità.
- **RT-4 — Modulo frontend (§3, §5).** Un riquadro ben visibile «Richieste in attesa di conferma» nella
  panoramica, e una finestra di conferma che dice **cosa succede e che non si può annullare**. Solo token del
  sistema di design; tema chiaro e scuro. Il pulsante di conferma non è il primo su cui cade il dito.
- **RT-5 — Cinque lingue (§4).** ⚠️ Il testo del riepilogo e quello della conferma sono **il presidio**: se sono
  vaghi o tradotti male, la conferma diventa un gesto automatico e il varco non serve a nulla. Dallo spazio-nomi
  `einvoicing`, presenti in `en, it, fr, es, de`, e da rivedere con particolare attenzione.
- **RT-6 — Varchi e quota (§6, §7).** La quota si prenota all'**esecuzione**, non alla richiesta di conferma: una
  richiesta mai approvata non deve consumare nulla. Ruolo richiesto per approvare: `owner` o `admin`. In modalità
  prova la richiesta si può creare ma l'approvazione risponde con l'errore dedicato: si può vedere come funziona
  senza che nulla esca.
- **RT-7 — Esposizione conversazionale (§12).** È la storia che realizza la regola di sicurezza del punto §12:
  `submit_document` e `archive_document` sono marcati **scrittura irreversibile** con **conferma umana
  obbligatoria**, e la loro invocazione produce una richiesta, non un effetto. Dipendenza dichiarata:
  UC 0061-0065 (compreso il tracciamento di sicurezza), non implementati.
- **RT-8 — Dati personali (§10).** Il riepilogo della richiesta di conferma **cita il documento** — controparte,
  importo — quindi contiene dati personali: la tabella va dichiarata nel manifesto in italiano e inglese e
  inserita in `exportData` e `purgeData`. È l'ennesima tabella che sembra «solo tracciamento» e non lo è.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `richiesta di conferma creata`, `conferma approvata`,
  `conferma scaduta`, `tentativo di riuso di una conferma` sono registrati con `tenant_id`, `app_id`, `user_id`,
  identificativo di correlazione, strumento e identificativi — senza il riepilogo.

## 4. Criteri di accettazione

**CA-1 — Lo strumento non esegue**
- **Dato** un documento validato e un agente che invoca `submit_document`
- **Quando** l'invocazione è completata
- **Allora** **nulla è stato trasmesso**, esiste una richiesta di conferma in attesa, e il riquadro in panoramica
  la mostra

**CA-2 — La conferma esegue**
- **Dato** la richiesta in attesa
- **Quando** un utente con ruolo `admin` la approva dall'interfaccia
- **Allora** la trasmissione parte, la quota viene prenotata, e la traccia riporta chi ha approvato e quando

**CA-3 — Il segno di conferma non è producibile dallo strumento**
- **Dato** un'invocazione di `submit_document` che tenta di includere un segno di conferma nei parametri
- **Quando** la si esegue
- **Allora** il segno è ignorato e viene comunque creata una richiesta in attesa

**CA-4 — Ruolo insufficiente**
- **Dato** un utente con ruolo `member`
- **Quando** tenta di approvare una richiesta
- **Allora** riceve `403` e nulla viene trasmesso

**CA-5 — Scadenza**
- **Dato** una richiesta oltre la sua scadenza
- **Quando** si tenta di approvarla
- **Allora** l'approvazione è rifiutata e va rifatta la richiesta

**CA-6 — Nessun riuso**
- **Dato** una richiesta già approvata ed eseguita
- **Quando** si tenta di approvarla di nuovo
- **Allora** l'operazione è rifiutata e nulla esce una seconda volta

**CA-7 — Isolamento fra account**
- **Dato** due account con richieste in attesa
- **Quando** un utente dell'uno tenta di approvare la richiesta dell'altro
- **Allora** riceve `404` e nulla esce

**CA-8 — Modalità prova**
- **Dato** un account in stato `trialing` con una richiesta in attesa
- **Quando** la si approva
- **Allora** l'operazione è negata con il messaggio dedicato e nulla esce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance, smoke);
- [ ] prove di **unità** su scadenza, non riusabilità e impossibilità di produrre il segno dal chiamante;
      **integrazione** sull'intero percorso richiesta → approvazione → esecuzione;
- [ ] prova di **isolamento fra account** e matrice dei ruoli sull'approvazione;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-EINVOICING]` (storia `0030`) attraverserà una
      richiesta di conferma e la sua approvazione dall'interfaccia, che è il modo di verificare che il varco esista
      davvero;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, riviste con attenzione perché **i testi sono il
      presidio**;
- [ ] **manifesto dei dati** aggiornato con la tabella delle richieste di conferma;
- [ ] controllo automatico di **accessibilità** sulla finestra di conferma;
- [ ] **registro delle decisioni** compilato, con la scelta «conferma come atto separato, non come parametro» e il
      motivo;
- [ ] contratto degli **strumenti conversazionali** aggiornato: i due strumenti irreversibili marcati come tali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0017`, `0018` | Sono le trasmissioni da mettere dietro il varco |
| `0023` | Il versamento in conservazione è il secondo effetto irreversibile |
| `0028` | Serve il contratto degli strumenti di scrittura su cui innestare il varco |
| UC 0065 (sicurezza e tracciamento del livello conversazionale), non implementato | Il tracciamento lato server è di piattaforma; qui si traccia lato app |

## 7. Fuori ambito

- La conferma per azioni **non** irreversibili: non serve, e aggiungerla svaluterebbe quella che serve.
- La conferma a più persone: non c'è, e non serve in un'impresa da dieci persone.
- La cancellazione e la restituzione integrale dell'archivio: **non sono esposte** al livello conversazionale
  (storia `0026`), quindi non passano da questo varco: non ci arrivano proprio.

## 8. Punti aperti

- **Quanto deve durare una richiesta di conferma.** Troppo poco è fastidioso, troppo trasforma il varco in una
  formalità. Proposta: minuti, non giorni. Da confermare.
- **Se la conferma debba poter avvenire anche dalla chat** con un secondo messaggio dell'utente. Sarebbe comodo,
  ma un messaggio in chat è ancora dentro il canale che l'agente controlla: la proposta è che la conferma avvenga
  **fuori** da quel canale, nell'app. È una decisione di piattaforma (UC 0062, consenso delegato) e va presa lì,
  non qui.
