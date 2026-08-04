# 0032 — Coda delle scansioni e invio idempotente

**Applicazione**: 14 — StockGrove (`magazzino`) · **Epica**: 06 — Scansione e lavoro sul campo
**Storia**: `0032` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0013`, `0030`, `0031`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetto che lavora in fondo al magazzino, dove il telefono non prende
> voglio che le mie letture si accodino e partano da sole quando la rete torna, senza mai contare due volte lo
> stesso pezzo
> così da non dover scegliere fra registrare la merce e finire il lavoro.

**Contesto.** Con la storia `0031` ogni conferma pretende una risposta del servizio: basta un corridoio senza
copertura o un magazzino interrato perché il lavoro si fermi, e chi lavora fa l'unica cosa sensata — registra dopo,
a memoria, cioè male. È il momento giusto adesso perché la via rapida esiste e mostra il problema; farlo prima
avrebbe significato progettare una coda per un gesto che non c'era.

**Il pericolo che questa storia deve chiudere.** Una coda che ritenta è una macchina per contare due volte. Il
telefono invia, la rete cade **dopo** che il servizio ha scritto ma **prima** che la risposta torni, il telefono
ritenta, e il pezzo risulta scaricato due volte: la giacenza mente e nessuno sa perché. La difesa è la **chiave di
idempotenza** già prevista dal modello (`0013`): ogni lettura nasce sul dispositivo con la propria chiave univoca
per account, e il servizio che riceve una chiave già vista non crea niente — risponde con il movimento che aveva
già registrato. La seconda regola, altrettanto importante: **niente sparisce in silenzio**. Una lettura rifiutata
resta in coda con l'errore accanto, visibile, finché una persona non decide cosa farne.

## 2. Requisiti funzionali

1. **RF-1** — Ogni lettura confermata in sessione rapida entra in una **coda sul dispositivo** con la propria
   chiave di idempotenza, generata al momento della conferma; se la rete c'è, la coda si svuota subito e
   l'esperienza è indistinguibile da quella della storia `0031`.
2. **RF-2** — Con la rete assente il lavoro **prosegue**: le letture si accumulano, la schermata mostra quante
   sono in attesa, e la giacenza mostrata accanto all'articolo è dichiarata come «ultimo valore noto», non
   spacciata per aggiornata.
3. **RF-3** — Quando la rete torna la coda si invia **in ordine di conferma**, una lettura per volta, così che due
   movimenti sullo stesso articolo si applichino nella sequenza in cui sono avvenuti.
4. **RF-4** — Un invio ripetuto della **stessa** chiave non produce un secondo movimento: il servizio riconosce la
   chiave già vista e risponde con il movimento esistente, e la coda tratta quella risposta come un successo.
5. **RF-5** — Una lettura **rifiutata** (giacenza insufficiente, articolo archiviato, deposito non più valido)
   resta in coda **con l'errore accanto**, in chiaro; si può correggere la quantità e reinviarla, oppure scartarla
   con una conferma esplicita. Non si ritenta all'infinito e non si scarta da sola.
6. **RF-6** — La coda **si vede** in ogni momento: elenco delle letture in attesa, di quelle inviate e di quelle
   rifiutate, con articolo, quantità, ora di conferma e stato; si può svuotare la parte già inviata.
7. **RF-7** — **Nessuna scansione va persa in silenzio**: chiudere la schermata o l'applicazione con la coda non
   vuota avvisa; alla riapertura la coda è ancora lì, con i suoi elementi e i suoi errori.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La rotta di invio filtra per `tenant_id` preso dal token verificato; la
  chiave di idempotenza è univoca **per account**, quindi la stessa stringa inviata da due account produce due
  movimenti distinti e nessuna collisione. Prova di isolamento fra due account sulla risorsa delle scansioni.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `POST /api/magazzino/v1/scansioni` (accoda e applica una lettura, con la chiave di idempotenza nel corpo),
  `GET /api/magazzino/v1/scansioni` (elenco per stato, paginato) e
  `DELETE /api/magazzino/v1/scansioni/{id}` (scarto esplicito di una lettura rifiutata). Corpo validato; errori in
  `application/problem+json`; risposta `200` con il movimento già esistente in caso di chiave ripetuta, `201` alla
  prima applicazione; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V20__scansione_in_coda.sql` sullo schema `app_magazzino`: tabella
  `scansione_in_coda` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e `deleted_at`; campi
  `codice_letto`, `articolo_id`, `deposito_id`, `tipo_movimento`, `quantita`, `chiave_idempotenza`, `stato`
  (`in_attesa`, `applicata`, `rifiutata`, `scartata`), `errore`, `movimento_id`, `confermata_il`. **Indice univoco
  su `(tenant_id, chiave_idempotenza)`**: è il vincolo che rende impossibile il doppio conteggio anche in caso di
  richieste contemporanee, e va nella base di dati, non nel codice. Nessuna chiave esterna verso altri schemi.
- **RT-4 — Modulo frontend (§3, §5).** La coda si vede da un pannello della sezione `movimenti` del modulo
  `magazzino` — non una sezione nuova del manifesto — pensato per lo schermo stretto, sui soli token del sistema di
  design, in tema chiaro e scuro. La persistenza sul dispositivo usa l'archivio locale del browser: nessuna
  applicazione da installare, nessuna risorsa caricata dalla rete.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe — stati della coda, testi degli errori di rifiuto, avviso di
  uscita con coda non vuota, etichetta «ultimo valore noto» — passano dallo spazio-nomi `magazzino` e sono
  presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** **L'invio della coda non consuma quota e non risponde mai `429`**: una coda
  respinta per quota sarebbe merce movimentata e mai registrata, cioè un saldo falso (descrizione §5). Con
  abbonamento in `past_due` l'invio resta accessibile; con `canceled` risponde `402` e la coda **resta sul
  dispositivo** invece di essere buttata.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo esposto: la coda è un dettaglio del
  dispositivo e non ha senso da una chat. Gli strumenti di scrittura restano quelli della storia `0035`, che
  passano dalla registrazione ordinaria con bozza e conferma. Server conversazionale di piattaforma, non ancora
  implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo** nei campi. La tabella `scansione_in_coda` porta
  però l'autore (`created_by`) e va quindi aggiunta a `exportData` e `purgeData` del contratto
  `MagazzinoDataContract` e citata nel manifesto `docs/compliance/manifests/magazzino.yaml` in italiano e inglese,
  in coerenza con la voce già dichiarata dalla storia `0010`. **Nessuna immagine della fotocamera viene inviata al
  servizio né conservata**, né sul dispositivo né nella coda: si accoda il codice letto, non la fotografia.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `scansione applicata`, `scansione riconosciuta come già
  applicata`, `scansione rifiutata` e `scansione scartata` sono registrati con `tenant_id`, `app_id`, `user_id` e
  identificativo di correlazione, senza dati personali e senza il codice letto. Nessun conteggio per persona
  (descrizione §6).

## 4. Criteri di accettazione

**CA-1 — La rete manca, il lavoro continua**
- **Dato** una sessione rapida in scarico e il dispositivo senza rete
- **Quando** si confermano tre letture
- **Allora** le tre letture sono in coda in stato `in_attesa`, la schermata dichiara «3 in attesa» e la giacenza
  mostrata è etichettata come ultimo valore noto

**CA-2 — La rete torna e la coda si svuota in ordine**
- **Dato** la coda di CA-1 e un articolo con giacenza 10
- **Quando** la rete torna
- **Allora** le tre letture sono inviate nell'ordine di conferma, esistono tre movimenti nel registro, la giacenza
  è 7 e le tre righe passano allo stato `applicata`

**CA-3 — Invio ripetuto con la stessa chiave**
- **Dato** una lettura già applicata, la cui risposta era andata persa
- **Quando** il dispositivo la reinvia con la **stessa** chiave di idempotenza
- **Allora** il servizio risponde `200` con il movimento già registrato, **non** crea un secondo movimento, la
  giacenza non cambia una seconda volta e la riga risulta `applicata`

**CA-4 — Rifiuto che resta visibile**
- **Dato** una lettura in coda che scarica 4 pezzi di un articolo la cui giacenza, nel frattempo, è scesa a 1
- **Quando** la coda la invia
- **Allora** il servizio risponde `409` con la quantità residua 1, la riga passa a `rifiutata` **con l'errore
  accanto**, resta in coda, non viene ritentata da sola, e si può correggere la quantità e reinviarla oppure
  scartarla con una conferma esplicita

**CA-5 — Niente si perde chiudendo**
- **Dato** una coda con due letture in attesa e una rifiutata
- **Quando** si chiude la schermata e la si riapre
- **Allora** compare l'avviso alla chiusura, e alla riapertura le tre righe sono ancora lì con i loro stati e il
  testo dell'errore

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` che inviano una lettura con la **stessa** stringa di chiave di idempotenza
- **Quando** entrambe le richieste arrivano
- **Allora** ciascun account ottiene il proprio movimento, nessuna delle due richieste vede l'altra, e un utente di
  `A` che chiede l'elenco delle scansioni vede solo le proprie anche forzando l'identificativo di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend, frontend e compliance; l'intera suite prima del
      commit);
- [ ] prove di **unità** sulla macchina degli stati della coda e sull'ordinamento dell'invio; prove di
      **integrazione** sulla rotta delle scansioni con database effimero e migrazioni vere, compresa la prova di
      due invii contemporanei con la stessa chiave, che devono produrre **un** solo movimento;
- [ ] prova di **isolamento fra account** su `scansione_in_coda`, con la stessa chiave di idempotenza su due
      account;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-MAGAZZINO]` è di proprietà della storia `0036`, che
      include il passo «lo stesso movimento inviato due volte non conta due volte»; la voce nel registro di
      copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) si scrive lì. Motivo
      del rimando: simulare l'assenza di rete in una prova automatica è possibile ma appartiene al percorso, non a
      questa storia;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, compresi i testi degli errori di rifiuto;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per la tabella `scansione_in_coda`, con la tabella
      presente in `exportData` e `purgeData`;
- [ ] **registro delle decisioni** compilato, con la scelta dell'indice univoco su `(tenant_id,
      chiave_idempotenza)` e della coda che non scarta mai da sola;
- [ ] contratto degli **strumenti conversazionali**: nessuno introdotto, con il motivo scritto;
- [ ] verifica manuale con la rete disattivata e riattivata, su schermo stretto, in tema chiaro e scuro;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0013` | La chiave di idempotenza sul movimento è definita lì: questa storia la usa, non la inventa |
| `0030` | La lettura del codice è l'origine di ciò che si accoda |
| `0031` | La sessione rapida è il gesto che alimenta la coda |
| `0015` | Il rifiuto per giacenza insufficiente, che qui diventa una riga rifiutata invece di un errore a schermo |

## 7. Fuori ambito

- **Consultare le giacenze senza rete**: la coda serve a **scrivere**, non a leggere. Un catalogo scaricato sul
  dispositivo per la consultazione fuori linea è un lavoro a sé, non richiesto da nessuna fonte, e resta fuori.
- **Risolvere un codice sconosciuto senza rete**: la risoluzione passa dal servizio; senza rete la lettura si
  accoda solo se l'articolo era già stato risolto.
- **Applicare le letture in un ordine diverso da quello di conferma**: nessuna riorganizzazione furba, perché
  cambierebbe il significato della sequenza.
- **La coda per l'inventario fisico**: la sessione di conteggio (`0022`) ha uno stato proprio sul servizio e un
  atteso congelato; il suo comportamento fuori linea, se servirà, è di quella storia.

## 8. Punti aperti

- **Per quanto tempo una coda può restare non inviata.** Una lettura confermata tre settimane fa e inviata oggi
  applica un fatto vecchio a un saldo nuovo. La proposta è di non scadere nulla e di mostrare l'età della riga,
  perché scartare da soli il lavoro di una persona è peggio; ma è una scelta di prodotto e non spetta a questa
  storia chiuderla.
- **Dispositivo condiviso fra più addetti.** Se il telefono del magazzino è uno solo e passa di mano, la coda
  contiene letture di persone diverse mentre l'autore registrato è quello della sessione attiva al momento
  dell'invio. È un difetto di tracciabilità reale: va risolto imponendo che la coda si svuoti al cambio di utente,
  oppure accettato e dichiarato. Serve una decisione dello sviluppatore.
