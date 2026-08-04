# 0017 — Ricalcolo tracciato dello storico

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 03 — Listino dei fornitori e calcolo del costo
**Storia**: `0017` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0015`, `0016`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha scoperto che per due settimane i conti sono stati fatti con un prezzo sbagliato
> voglio poter rifare quei conti, sapendo esattamente cosa cambia e potendo mostrare la versione di prima
> così da correggere un errore senza perdere la prova di quello che avevo visto e mandato ai miei clienti.

**Contesto.** Il congelamento del costo (storia `0014`) è la regola; il ricalcolo è la sua unica eccezione, e va
costruita in modo che l'eccezione non distrugga la regola. Le occasioni sono tre e sono tutte legittime: un prezzo
del catalogo era sbagliato, un modello prima sconosciuto ha ora un prezzo (storia `0015`), uno sconto negoziato va
applicato al passato (storia `0016`). In tutti e tre i casi il cliente può aver già mandato quei numeri a
qualcuno: l'operazione è **irreversibile nei suoi effetti pratici** anche se non distrugge dati, e va trattata come
tale.

## 2. Requisiti funzionali

1. **RF-1** — Il ricalcolo è un'azione **esplicita**, mai automatica, delimitata da un intervallo di date e
   facoltativamente da un fornitore o da un modello.
2. **RF-2** — Prima di eseguire, il ricalcolo produce un'**anteprima**: quante misure sarebbero toccate, qual era
   il totale, quale sarebbe, e la differenza in valore e in percentuale. Nulla cambia finché non si conferma.
3. **RF-3** — Il ricalcolo **non modifica** le righe esistenti: produce una nuova valorizzazione, con il proprio
   numero di revisione e la propria versione di catalogo, e la valorizzazione precedente resta consultabile.
4. **RF-4** — Ogni ricalcolo lascia una riga di registro con: chi, quando, intervallo, motivo scritto (obbligatorio),
   versione di catalogo di partenza e di arrivo, numero di misure toccate, differenza complessiva.
5. **RF-5** — Le schermate che mostrano un periodo ricalcolato lo dichiarano, con la data del ricalcolo e la
   possibilità di vedere la valorizzazione precedente.
6. **RF-6** — Un ricalcolo su un periodo per cui l'account ha già esportato o ribaltato dei costi (storie `0022` e
   `0030`) mostra un avvertimento aggiuntivo prima della conferma: quei numeri sono già usciti dall'app.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il ricalcolo agisce sul solo `tenant_id` preso dal gettone verificato;
  il ricalcolo di piattaforma per un singolo account, eseguito dall'assistenza, resta comunque circoscritto a
  quell'account ed è tracciato ([estensioni-admin.md](../estensioni-admin.md) §5).
- **RT-2 — Persistenza (§8).** Migrazione sullo schema `app_spesa_modelli`: tabella `valorizzazione` con
  `tenant_id`, riferimento alla misura, revisione, costo, versione di catalogo, origine, colonne di controllo; e
  tabella `ricalcolo` con il registro dell'operazione. Il costo «corrente» di una misura è quello della revisione
  più alta; le precedenti non si cancellano.
- **RT-3 — Interfaccia di programmazione (§2).** Rotte `POST /api/spesa_modelli/v1/ricalcoli/anteprima` e
  `POST /api/spesa_modelli/v1/ricalcoli`; corpo validato con motivo obbligatorio; errori in `problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-4 — Varchi, ruoli e quota (§6, §7).** Riservato a `owner` e `admin`. Un ricalcolo **non consuma** la
  metrica `misure_registrate`: non sta registrando misure nuove, sta rivalorizzando quelle esistenti. Va scritto,
  perché la lettura ingenua porterebbe a contarle due volte e a bloccare il cliente proprio mentre corregge un
  errore.
- **RT-5 — Modulo frontend (§3, §5).** Il ricalcolo si avvia dalla scheda «I miei prezzi» o dalla schermata dei
  modelli sconosciuti; l'anteprima è una finestra di conferma che mostra il prima e il dopo. Solo token del
  sistema di design; tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Tutte le stringhe, compreso l'avvertimento sui numeri già usciti dall'app, sono
  presenti in `en, it, fr, es, de`.
- **RT-7 — Esposizione conversazionale (§12).** Lo strumento `applica_regola_allo_storico` della storia `0033` è
  il gemello di questa azione sul versante dell'attribuzione; il **ricalcolo del costo non è esposto** come
  strumento, nemmeno con conferma, perché richiede di leggere un'anteprima numerica per decidere. Il motivo va nel
  registro delle decisioni.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. La tabella `valorizzazione` va comunque aggiunta a
  `exportData` e `purgeData` insieme a `misura`: la cancellazione di una misura deve portarsi via le sue
  valorizzazioni.
- **RT-9 — Registrazione eventi (§14).** Evento «ricalcolo eseguito» con `tenant_id`, `app_id`, `user_id`,
  intervallo, numero di misure e differenza complessiva, con identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Anteprima prima di tutto**
- **Dato** un intervallo con 5.000 misure e un prezzo cambiato
- **Quando** si chiede il ricalcolo
- **Allora** si vede quante misure sarebbero toccate, il totale prima, il totale dopo e la differenza, e nulla è
  cambiato finché non si conferma

**CA-2 — Le righe vecchie restano**
- **Dato** un ricalcolo confermato su luglio
- **Quando** si apre la scheda del costo di una misura di luglio
- **Allora** si vede la valorizzazione corrente e, accanto, quella precedente con la sua versione di catalogo

**CA-3 — Motivo obbligatorio**
- **Dato** una richiesta di ricalcolo senza motivo scritto
- **Quando** viene inviata
- **Allora** è respinta con `400` e nulla viene eseguito

**CA-4 — Avvertimento sui numeri già usciti**
- **Dato** un periodo per cui l'account ha già esportato la spesa attribuita
- **Quando** si chiede il ricalcolo di quel periodo
- **Allora** l'anteprima contiene l'avvertimento aggiuntivo, con la data dell'esportazione

**CA-5 — Il ricalcolo non consuma quota**
- **Dato** un account vicino al tetto di `misure_registrate`
- **Quando** esegue un ricalcolo su 100.000 misure
- **Allora** la quota consumata non cambia e nessun `429` viene restituito

**CA-6 — Isolamento fra account**
- **Dato** due account con misure sugli stessi modelli e periodo
- **Quando** uno esegue un ricalcolo
- **Allora** i costi dell'altro restano invariati

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla costruzione dell'anteprima e sulla revisione delle valorizzazioni, e di
      **integrazione** su un ricalcolo completo con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sul ricalcolo;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «prezzo sbagliato,
      ricalcolo con anteprima, il vecchio valore resta consultabile», e aggiornare il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato: la tabella `valorizzazione` compare in esportazione e cancellazione
      insieme a `misura`;
- [ ] **registro delle decisioni** compilato, in particolare sul perché il ricalcolo non consuma quota e sul
      perché non è uno strumento conversazionale;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0014` | Il ricalcolo è l'eccezione al congelamento: senza il congelamento non ha senso |
| Storia `0015` | È una delle tre occasioni di ricalcolo (prezzo prima ignoto) |
| Storia `0016` | È la seconda occasione (sconto retroattivo) |

## 7. Fuori ambito

- la **riattribuzione** dello storico (cambiare a chi è imputata una spesa, non quanto costa): è la storia `0021`
  e ha una macchina diversa;
- l'annullamento di un ricalcolo: non esiste come funzione, perché si ottiene ricalcolando di nuovo con la versione
  di catalogo desiderata — e in questo modo resta tracciato anche il ritorno indietro.

## 8. Punti aperti

- **Per quanto conservare le valorizzazioni superate.** Tenerle tutte per sempre fa crescere la tabella più
  popolosa dell'app; cancellarle dopo un periodo toglie la prova di ciò che il cliente aveva visto. Proposta:
  conservarle per la stessa durata dello storico del piano, che è la stessa durata per cui il cliente può
  consultare le misure. La chiude lo sviluppatore insieme alla politica di conservazione (storia `0035`).
