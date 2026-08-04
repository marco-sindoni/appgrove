# 0005 — Avvio locale e dati di prova

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 01 — Fondamenta
**Storia**: `0005` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`, `0004`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore che riprende in mano l'applicazione fra tre settimane
> voglio avviare tutto lo stack in locale con un comando e trovarci dentro qualche richiesta di assistenza già
> pronta
> così da vedere l'app funzionare in trenta secondi invece di passare mezz'ora a inventarmi dei dati per capire se
> qualcosa è rotto.

**Contesto.** Le storie precedenti hanno prodotto un servizio, uno schema, un modulo e un varco. Manca la cosa che
rende tutto ciò utilizzabile davvero: che l'app parta in locale **senza passi manuali impliciti** e che l'elenco
delle richieste non sia vuoto. Un'app vuota non permette di vedere se l'ordinamento è giusto, se lo stato vuoto è
scritto bene, se la barra del consumo dei posti si muove. È anche il momento in cui si verifica che la scoperta
automatica dei servizi abbia funzionato: se viene voglia di modificare a mano uno script di avvio, quello è un
difetto della scoperta automatica, non un passo del lavoro.

## 2. Requisiti funzionali

1. **RF-1** — `./app-start.sh` avvia il servizio `helpdesk` insieme agli altri, senza che nessuno abbia modificato
   uno script a mano, e `./app-stop.sh` lo ferma.
2. **RF-2** — `./dev.sh services` mostra l'app con il suo identificativo, la porta `8112` e lo schema
   `app_helpdesk`, ricavati dal **solo** file delle proprietà del servizio.
3. **RF-3** — `dev migrate` applica le migrazioni dell'app sullo schema `app_helpdesk`, e il proxy locale espone le
   rotte `/api/helpdesk/v1/*` dal blocco rigenerato, non da righe scritte a mano.
4. **RF-4** — Esiste un comando che riempie un account di prova con dati **inventati**: due o tre richiedenti,
   una decina di richieste in stati diversi con qualche messaggio ciascuna, e almeno una vicina al tetto dei posti
   operatore per poter vedere l'avviso di quota.
5. **RF-5** — I dati di prova sono **deterministici** (stessi dati a ogni esecuzione) e riconoscibili come finti:
   nomi inventati e indirizzi di posta sul dominio riservato alle prove.
6. **RF-6** — Il comando è ripetibile: eseguirlo due volte non crea doppioni e non lascia l'account a metà.

## 3. Requisiti tecnici

- **RT-1 — Avvio locale automatico (§15).** La mappa servizio → identificativo dell'app → porta → schema si ricava
  dal **solo** `services/helpdesk/src/main/resources/application.properties`. Da lì discendono da soli gli script di
  avvio e di arresto, le migrazioni, le rotte del proxy locale e gli avvii di collaudo. Il dovere della storia è
  dichiarare bene quelle proprietà, **non** incollare righe negli script.
- **RT-2 — Isolamento fra account (§1).** I dati di prova nascono dentro un account preciso e il comando lo
  richiede in modo esplicito: non esiste una via per riempire «tutti gli account».
- **RT-3 — Persistenza (§8).** Nessuna migrazione nuova: il comando scrive attraverso i depositi dell'app, così che
  i dati di prova rispettino le stesse regole dei dati veri (chiavi UUID versione 7, colonne di controllo,
  cancellazione logica).
- **RT-4 — Dati personali (§10).** I dati di prova **non sono dati personali di nessuno**: nomi inventati,
  indirizzi di posta su dominio riservato alle prove, nessun dato reale, mai. È la stessa regola che vale per le
  prove end-to-end. Nessuna voce nuova nel manifesto.
- **RT-5 — Cinque lingue (§4).** Se il comando produce testi visibili (per esempio l'oggetto delle richieste
  inventate), quelli sono contenuto di prova e non passano dalle traduzioni; ogni messaggio dell'interfaccia
  introdotto dalla storia sì.
- **RT-6 — Registrazione eventi (§14).** L'esecuzione del comando si registra con `tenant_id` e `app_id`, e dichiara
  quanti elementi ha creato.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento: è uno strumento per chi sviluppa, non una
  funzione dell'applicazione, e non va esposto al livello conversazionale.

## 4. Criteri di accettazione

**CA-1 — Parte senza cablaggi**
- **Dato** un repository appena clonato e le dipendenze installate
- **Quando** si esegue `./app-start.sh`
- **Allora** il servizio `helpdesk` risponde sulla porta `8112`, e nel ramo della change **nessuno** script di
  avvio è stato modificato a mano

**CA-2 — La scoperta automatica vede l'app**
- **Dato** lo stack locale in esecuzione
- **Quando** si esegue `./dev.sh services`
- **Allora** l'elenco riporta `helpdesk`, porta `8112`, schema `app_helpdesk`

**CA-3 — I dati di prova compaiono**
- **Dato** un account di prova vuoto
- **Quando** si esegue il comando di riempimento su quell'account
- **Allora** l'elenco delle richieste nel backoffice mostra una decina di richieste in stati diversi, con
  richiedenti dal nome inventato e indirizzi su dominio di prova

**CA-4 — Ripetibile senza doppioni**
- **Dato** un account già riempito
- **Quando** si esegue di nuovo lo stesso comando
- **Allora** il numero di richieste resta lo stesso e non compaiono duplicati

**CA-5 — Isolamento fra account**
- **Dato** due account di prova `A` e `B`, e il comando eseguito solo su `A`
- **Quando** si guarda l'elenco delle richieste di `B`
- **Allora** è vuoto

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `smoke`; l'intera suite prima del commit);
- [ ] prova di **avvio reale** nell'area `smoke`: l'artefatto parte fuori dal profilo di prova e risponde;
- [ ] prova di **isolamento fra account** sul comando di riempimento;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-HELPDESK]` nasce con la storia `0037`, che si appoggerà
      proprio a questi dati di prova; la voce del registro di copertura è di quella storia;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per i messaggi dell'interfaccia eventualmente introdotti;
- [ ] **manifesto dei dati**: nessuna modifica — i dati di prova sono inventati;
- [ ] **registro delle decisioni** compilato, con annotata la forma dei dati di prova e la loro ripetibilità;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta;
- [ ] `./dev.sh services` mostra la mappa scoperta e l'avvio locale funziona senza passi manuali — è la verifica che
      chiude l'epica delle fondamenta.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0001` e `0002` di questa app | Servono il servizio e le tabelle da riempire |
| Storia `0003` di questa app | I dati di prova si guardano dall'interfaccia: senza modulo non si vede niente |
| Storia `0004` di questa app | Perché l'avviso di quota si veda, i dati di prova devono poter avvicinare il tetto dei posti |

## 7. Fuori ambito

- **I dati di prova dei canali, delle code, degli articoli**: li aggiungeranno le rispettive storie, quando quelle
  entità esisteranno. Inventarli adesso significherebbe scrivere righe per tabelle che non ci sono.
- **Il percorso end-to-end**: la storia `0037`.
- **Gli ambienti diversi da quello locale**: qui si parla solo di sviluppo.

## 8. Punti aperti

- **Quanti dati di prova sono «abbastanza»**: dieci richieste bastano a vedere l'ordinamento e la paginazione, ma
  non a vedere il comportamento con diecimila. Se in futuro servisse una prova di carico, sarà un comando diverso e
  una decisione diversa.
- Nessun altro: la storia non tocca prezzi, dati personali reali né effetti verso l'esterno.
