# 0004 — Abbonamento e quota

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio
> voglio sapere quanti messaggi con modello mi restano questo mese, e vederlo prima di finirli
> così da non scoprire il limite proprio mentre sto rispondendo a un cliente.

**Contesto.** La metrica di quota di ChatGrove è `messaggi_template`: i messaggi inviati con un modello
approvato, cioè quelli che si pagano e che raggiungono un cliente fuori dalla finestra di risposta. Il
contatore va costruito **prima** delle funzioni che lo consumano, perché ogni invio delle epiche successive
deve passare da qui. Il §2.5 dell'analisi dice che il costo imprevedibile è la lamentela numero uno del
segmento: un contatore sempre visibile è la risposta più semplice a quel problema.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio legge l'abilitazione dell'account dalla propria **proiezione locale**, alimentata a
   eventi, e ne ricava il tetto della metrica `messaggi_template` per il mese in corso.
2. **RF-2** — Esiste un contatore per account e per mese, che si azzera all'inizio del mese di calendario
   (natura `flow`).
3. **RF-3** — Esiste un punto unico di prenotazione della quota che ogni invio delle epiche successive deve
   attraversare: prenota una unità, e la rilascia se l'invio non parte.
4. **RF-4** — A quota esaurita il servizio risponde `429` con un messaggio che dice **cosa è successo**, **cosa
   non si può più fare** e **come si rimedia**.
5. **RF-5** — Il consumo (usato, tetto, giorni al ripristino) è leggibile da una rotta e mostrato nella pagina
   d'atterraggio del modulo, con un avviso visibile oltre l'80 %.
6. **RF-6** — Le risposte inviate **dentro** la finestra di servizio **non** consumano quota: il contatore
   distingue i due casi.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il contatore e la proiezione dell'abilitazione filtrano per
  `tenant_id` preso dal token verificato; nessun account può leggere o consumare la quota di un altro.
- **RT-2 — Varchi e quota (§6, §7).** Prima di ogni invio con modello il servizio prenota una unità della
  metrica `messaggi_template` (natura `flow`); a quota esaurita risponde `429`. Con abbonamento non attivo
  risponde `402`. L'abilitazione si legge **solo** dalla proiezione locale: mai una chiamata di rete sincrona
  all'app centrale sul percorso caldo.
- **RT-3 — Abbonamento (§13).** Con abbonamento in `past_due` la funzione resta accessibile (periodo di
  tolleranza); con `canceled` risponde `402`. I diritti dell'interessato (esportazione, cancellazione) restano
  accessibili in ogni caso.
- **RT-4 — Persistenza (§8).** Migrazione `V2__contatore_quota.sql` sullo schema `app_chat_commerce`: tabella
  del contatore con `tenant_id`, periodo, valore, colonne di controllo. La prenotazione è atomica: due invii
  simultanei non possono superare il tetto.
- **RT-5 — Modulo frontend (§3, §4, §5).** La pagina d'atterraggio mostra la barra di consumo; tutte le
  stringhe passano dallo spazio-nomi `chat_commerce` in cinque lingue; solo token del sistema di design.
- **RT-6 — Registrazione eventi (§14).** Gli eventi `quota prenotata`, `quota rilasciata` e `invio respinto
  per quota` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati
  personali.
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo: il contatore conta, non conosce persone.

## 4. Criteri di accettazione

**CA-1 — Il contatore si vede**
- **Dato** un account sul piano `pro` che ha inviato 120 messaggi con modello questo mese
- **Quando** apre la pagina d'atterraggio del modulo
- **Allora** legge «120 di 2.000 messaggi con modello — questo mese» e la barra è sotto la soglia d'avviso

**CA-2 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto di `messaggi_template`
- **Quando** tenta un invio con modello
- **Allora** riceve `429` con un messaggio che spiega come rimediare, **nulla viene inviato** e il contatore
  non aumenta

**CA-3 — La risposta dentro la finestra non consuma**
- **Dato** un account a un solo messaggio dal tetto, con una conversazione la cui finestra di servizio è
  aperta
- **Quando** l'addetto risponde in quella conversazione
- **Allora** il messaggio parte e il contatore **non** aumenta

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B` sullo stesso piano
- **Quando** `A` esaurisce la propria quota
- **Allora** `B` continua a inviare senza restrizioni, e `A` non può leggere il contatore di `B` nemmeno
  forzando l'identificativo nella richiesta

**CA-5 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled`
- **Quando** tenta un invio · **Allora** riceve `402`; se invece l'abbonamento è `past_due`, l'invio funziona

**CA-6 — Il mese si azzera**
- **Dato** un account che ha esaurito la quota a fine mese
- **Quando** inizia il mese successivo
- **Allora** il contatore riparte da zero senza alcun intervento

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo del periodo e sulla prenotazione atomica, e di **integrazione** sulla
      rotta del consumo, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sul contatore;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`, che percorre il blocco a quota esaurita dentro il
      percorso `[J-CHAT-COMMERCE]`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, compresi i messaggi di errore mostrati all'utente;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, con la scelta di non far consumare quota alle risposte dentro la
      finestra e il perché;
- [ ] contratto degli **strumenti conversazionali**: la quota vale anche per le chiamate dell'assistente
      (dipendenza dichiarata verso UC 0064);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0002` | Serve lo schema in cui vive il contatore |
| `0003` | Serve la pagina d'atterraggio in cui mostrarlo |
| UC 0064 (applicazione della quota alle chiamate dell'assistente) | Non implementato: qui si costruisce il punto unico di prenotazione perché domani l'assistente passi da lì |

## 7. Fuori ambito

- l'invio vero dei messaggi: epica 02;
- la stima del costo in denaro di una campagna: storia `0023`;
- la deroga temporanea concessa dall'assistenza: [estensioni-admin.md](../estensioni-admin.md).

## 8. Punti aperti

- **I valori dei tetti** (100 / 2.000 / 10.000) sono una proposta del §5.2 della descrizione: fermata di
  escalation dello sviluppatore.
