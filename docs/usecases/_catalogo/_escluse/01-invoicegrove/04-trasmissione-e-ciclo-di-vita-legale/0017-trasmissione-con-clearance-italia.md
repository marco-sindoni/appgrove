# 0017 — Trasmissione a liberatoria (Italia)

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 04 — Trasmissione e ciclo di vita legale
**Storia**: `0017` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0016`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile amministrativo italiano
> voglio premere «trasmetti» e sapere che la fattura è partita verso il Sistema di Interscambio
> così da avere un documento che esiste giuridicamente, non un file sul mio computer.

**Contesto.** È la prima realizzazione concreta del contratto della storia `0016` e la prima azione dell'app con
**effetti irreversibili verso l'esterno**. In Italia il modello è a liberatoria: la fattura non esiste
giuridicamente finché il Sistema di Interscambio non l'ha accettata, e una volta accettata l'unico rimedio è una
nota di credito. La trasmissione avviene tramite un **fornitore certificato** (descrizione dell'applicazione §2.3:
l'accreditamento diretto del canale richiede prove di interoperabilità e certificati rilasciati dall'Agenzia): il
costo variabile rilevato è di circa €0,074 a documento a volume, ed è la ragione per cui la quota esiste.

## 2. Requisiti funzionali

1. **RF-1** — Esiste l'adattatore italiano che serializza il documento canonico nel formato ufficiale e lo
   consegna al fornitore di trasmissione.
2. **RF-2** — La trasmissione è consentita **solo** se la validazione preventiva (storia `0014`) non ha violazioni
   bloccanti; il documento passa da `validato` a `in_trasmissione`.
3. **RF-3** — La trasmissione è **idempotente**: un secondo tentativo sullo stesso documento non produce una
   seconda consegna, ma restituisce l'esito del primo.
4. **RF-4** — Il documento riceve l'**identificativo assegnato dal Sistema di Interscambio** quando disponibile, e
   solo allora passa a `accettato_dall_autorita`.
5. **RF-5** — Se il fornitore non risponde o risponde con un errore tecnico, il documento resta in
   `in_trasmissione` con l'ultimo tentativo registrato, e viene ritentato con un ritmo crescente e un numero
   massimo di tentativi.
6. **RF-6** — Ogni trasmissione consuma **una** unità della metrica `documenti`; la prenotazione avviene **prima**
   della consegna e viene rilasciata se la consegna non parte.
7. **RF-7** — In **modalità prova** la trasmissione è negata con il messaggio dedicato: nulla esce.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il documento trasmesso e la trasmissione filtrano per `tenant_id` preso
  dal token verificato. Prova di isolamento: un utente non può trasmettere il documento di un altro account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta
  `POST /api/einvoicing/v1/documents/{id}/submit` con corpo che porta la **conferma esplicita**; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit. La rotta è protetta anche a
  livello di ruolo: `owner` o `admin`.
- **RT-3 — Persistenza (§8).** Migrazione `V14__transmission.sql`: tabella `transmission` con canale, fornitore,
  identificativo esterno, stato, tentativi, istanti e riferimento all'artefatto; `tenant_id`, chiave UUID versione
  7, colonne di controllo. Nessuna cancellazione logica su `transmission`: è una prova di ciò che è uscito.
- **RT-4 — Modulo frontend (§3, §5).** Sulla scheda del documento, il pulsante «Trasmetti» apre una **finestra di
  conferma** che dice esattamente cosa sta per succedere e che non si potrà annullare. Solo token del sistema di
  design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Il testo della conferma, i nomi degli stati e i messaggi di errore dallo
  spazio-nomi `einvoicing`, presenti in `en, it, fr, es, de`. Il testo della conferma è quello che protegge
  l'utente da un errore irreversibile: una traduzione debole qui ha conseguenze.
- **RT-6 — Varchi e quota (§6, §7).** Prima di trasmettere, il servizio prenota una unità della metrica
  `documenti` (natura `flow`, finestra mensile); a quota esaurita risponde `429` con l'indicazione del rimedio e
  **nulla viene trasmesso**. Con abbonamento non attivo risponde `402`; in `past_due` la funzione resta
  accessibile.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `submit_to_authority(id, conferma_esplicita) → esito della trasmissione`, marcato **scrittura irreversibile**,
  con **conferma umana obbligatoria**. Il varco e la sua forma sono della storia `0029`: qui si dichiara il
  contratto e si rende impossibile invocarlo senza il segno di conferma. Contratto dentro il servizio; server
  conversazionale non implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Il carico trasmesso contiene tutti i dati personali del documento e viene
  consegnato a un **fornitore esterno**, che è un responsabile del trattamento: va dichiarato nell'elenco dei
  fornitori e nell'informativa, e va verificato che i dati stiano a riposo in regioni europee. La tabella
  `transmission` (che riferisce il carico) va dichiarata nel manifesto in italiano e inglese e inserita in
  `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `trasmissione avviata`, `quota prenotata`, `esito ricevuto`,
  `tentativo fallito` sono registrati con `tenant_id`, `app_id`, `user_id`, identificativo di correlazione,
  identificativo del documento e identificativo esterno — **mai** il carico, il numero di documento o le
  denominazioni.

## 4. Criteri di accettazione

**CA-1 — Trasmissione riuscita**
- **Dato** un documento italiano validato senza violazioni bloccanti, su un account con quota disponibile
- **Quando** l'utente conferma la trasmissione
- **Allora** il documento passa a `in_trasmissione`, il fornitore riceve l'artefatto, e all'arrivo
  dell'identificativo del Sistema di Interscambio lo stato diventa `accettato_dall_autorita`

**CA-2 — Trasmissione negata per violazione bloccante**
- **Dato** un documento con una violazione bloccante
- **Quando** si tenta di trasmetterlo
- **Allora** l'operazione è rifiutata con l'elenco delle violazioni, **nulla esce** e nessuna quota è consumata

**CA-3 — Secondo tentativo sullo stesso documento**
- **Dato** un documento già trasmesso
- **Quando** si preme di nuovo «Trasmetti»
- **Allora** non parte una seconda consegna e viene restituito l'esito della prima

**CA-4 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto di `documenti`
- **Quando** tenta una trasmissione
- **Allora** riceve `429` con il rimedio, e nulla viene trasmesso

**CA-5 — Fornitore non disponibile**
- **Dato** il fornitore che non risponde
- **Quando** si trasmette
- **Allora** il documento resta in `in_trasmissione`, il tentativo è registrato, il ritentativo è pianificato con
  ritmo crescente, e la quota **non** viene consumata due volte

**CA-6 — Modalità prova**
- **Dato** un account in stato `trialing`
- **Quando** tenta una trasmissione
- **Allora** riceve l'errore dedicato che spiega che in prova non si trasmette, e nulla esce

**CA-7 — Isolamento fra account**
- **Dato** due account con documenti propri
- **Quando** un utente dell'uno tenta di trasmettere un documento dell'altro
- **Allora** riceve `404` e nulla esce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance, smoke);
- [ ] **suite di conformità del contratto** (storia `0016`) verde su questa realizzazione;
- [ ] prove di **unità** sulla serializzazione e sull'idempotenza, di **integrazione** con il fornitore
      **simulato**, compresi i casi di indisponibilità, lentezza ed esito negativo;
- [ ] prova di **isolamento fra account** e matrice dei ruoli sulla rotta di trasmissione;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-EINVOICING]` (storia `0030`) attraverserà una
      trasmissione riuscita contro il fornitore simulato, e il registro di copertura va aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con attenzione al testo della conferma;
- [ ] **manifesto dei dati** aggiornato con `transmission` e con il fornitore esterno dichiarato;
- [ ] **registro delle decisioni** compilato, con il fornitore scelto, il motivo, e la scelta «prenotazione della
      quota prima della consegna»;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `submit_to_authority`, marcato irreversibile.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0016` | Serve il contratto dell'adattatore e la suite di conformità |
| `0014` | La trasmissione richiede una validazione senza violazioni bloccanti |
| `0004` | Serve la metrica di quota e la modalità prova |
| Contratto con il fornitore di trasmissione | Nessun codice sostituisce un accordo con un responsabile del trattamento |

## 7. Fuori ambito

- L'**acquisizione delle notifiche** successive (consegna, mancata consegna, scarto): storia `0019`. Qui si arriva
  fino alla consegna al fornitore e all'identificativo dell'autorità.
- La **gestione dello scarto** e il rinvio del documento corretto: storia `0020`.
- La rete a quattro angoli: storia `0018`.
- L'accreditamento **diretto** del canale presso l'Agenzia delle Entrate: deliberatamente escluso; si usa un
  fornitore. Se un giorno si volesse accreditarsi, è un progetto, non una storia.

## 8. Punti aperti

- 🛑 **Quale fornitore, a quali condizioni e con quale responsabilità in caso di scarto.** Il prezzo pubblico
  rilevato è di circa €0,074 a documento a volume (descrizione dell'applicazione §2.2), ma il modello di
  responsabilità non l'ho trovato dichiarato su nessuno dei fornitori esaminati (§2.7). È una fermata di
  escalation: tocca contratti ed effetti verso l'esterno.
- **Dove stanno a riposo i dati presso il fornitore.** Va verificato contrattualmente, non presunto.
- **Se il ritentativo automatico sia sempre desiderabile.** Su un'azione con effetti giuridici, ritentare da soli
  è comodo ma non ovvio: la proposta è ritentare solo sugli errori **tecnici** di trasporto, mai su un esito
  applicativo negativo. Va confermato.
