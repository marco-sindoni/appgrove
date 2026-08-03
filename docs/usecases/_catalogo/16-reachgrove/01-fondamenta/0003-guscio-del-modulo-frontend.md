# 0003 — Guscio del modulo frontend

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena attivato ReachGrove
> voglio vedere l'app comparire nel mio backoffice, con le sue sezioni e nella mia lingua
> così da capire subito dove si fanno le cose, prima ancora che ci sia qualcosa dentro.

**Contesto.** Il servizio esiste (storia 0001) ma nel backoffice non c'è traccia dell'app. Serve il guscio del
modulo: il manifesto che lo dichiara, la registrazione nel registro delle app, le sezioni della barra laterale, le
traduzioni nelle cinque lingue e le schermate vuote che le sezioni aprono. È la storia che rende l'app
*visitabile*, e va fatta presto perché ogni storia di dominio successiva vi appende una schermata invece di
inventarsi una navigazione propria.

Le sezioni non sono una scelta grafica: ricalcano le epiche, cioè il flusso di lavoro di chi manda una campagna —
prima il pubblico, poi il messaggio, poi l'invio, poi il risultato.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il modulo `frontend/apps/backoffice/src/modules/campaigns/` con il suo manifesto, dichiarato
   nell'elenco dei moduli del registro delle app.
2. **RF-2** — Il manifesto dichiara identificativo `campaigns`, nome visibile, icona, `accentToken` `violet`
   (**lo stesso** valore del campo `category` del listino), le sezioni, le risorse e la metrica di quota
   `messages_sent`.
3. **RF-3** — Le sezioni della barra laterale sono sei e in quest'ordine: **Panoramica**, **Iscritti**,
   **Campagne**, **Automazioni**, **Rapporti**, **Impostazioni di invio**.
4. **RF-4** — Ogni sezione apre una schermata che esiste, con titolo, spiegazione di una riga di cosa ci si farà e
   uno stato vuoto che dice cosa fare: nessuna sezione porta a una pagina bianca.
5. **RF-5** — Il modulo compare nella barra laterale **solo** quando registro delle app e abilitazione dell'account
   dicono di sì; a un account non abilitato non compare affatto.
6. **RF-6** — Tutte le stringhe visibili passano dallo spazio-nomi `campaigns` e sono presenti in tutte e cinque le
   lingue dell'interfaccia (`en, it, fr, es, de`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il modulo **non** conosce l'identificativo dell'account se non
  attraverso il contesto che la shell gli passa, e non lo invia mai nelle richieste: il perimetro lo decide il
  servizio a partire dal token.
- **RT-2 — Interfaccia di programmazione (§2).** Il modulo legge i dati con il client generato dalla definizione
  OpenAPI del servizio; in questa storia l'unica chiamata è la sonda `GET /api/campaigns/v1/ping`, che serve a
  mostrare che il collegamento funziona.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: il modello dati è la storia 0002.
- **RT-4 — Modulo frontend (§3, §5).** Modulo React caricato su richiesta, in TypeScript, con i componenti senza
  stile proprio sopra i token del sistema di design; stato del server con la libreria di interrogazione comune.
  Solo token del sistema di design, **nessun colore scritto a mano**; funziona in tema chiaro e in tema scuro.
- **RT-5 — Cinque lingue (§4).** Traduzioni in
  `frontend/apps/backoffice/src/modules/campaigns/i18n/{en,it,fr,es,de}.ts` sotto lo spazio-nomi `campaigns`;
  nessun testo visibile scritto a mano nei componenti. La storia non è conclusa se manca una lingua.
- **RT-6 — Varchi e quota (§6, §7).** Il guscio mostra il consumo della metrica `messages_sent` (natura `flow`,
  finestra mensile) letto dall'abilitazione; il comportamento a quota esaurita lo definisce la storia 0004. Con
  abbonamento in `past_due` il modulo resta accessibile; con `canceled` la shell non lo mostra.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia. Il guscio prevede però il
  posto in cui la panoramica dirà che le stesse funzioni sono richiamabili dalla chat (epica 07).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: le schermate sono vuote e la sonda restituisce un
  identificativo. Il modulo non registra nulla nel deposito locale del browser oltre alle preferenze di
  interfaccia; **nessun tracciamento** dentro l'app.
- **RT-9 — Registrazione eventi (§14).** Nessun evento di dominio. Gli errori di rete del modulo si riportano con
  l'identificativo di correlazione della richiesta, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — L'app compare a chi è abilitato**
- **Dato** un account abilitato a ReachGrove
- **Quando** l'utente apre il backoffice
- **Allora** nella barra laterale, sotto «Le tue app», compare ReachGrove con le sue sei sezioni

**CA-2 — L'app non compare a chi non è abilitato**
- **Dato** un account senza abilitazione a ReachGrove
- **Quando** l'utente apre il backoffice
- **Allora** il modulo non compare, e raggiungere il suo indirizzo a mano non mostra la sua interfaccia

**CA-3 — Nessuna sezione porta a una pagina bianca**
- **Dato** l'app appena attivata, senza dati
- **Quando** l'utente apre a turno le sei sezioni
- **Allora** ognuna mostra titolo, spiegazione e uno stato vuoto con l'azione da fare

**CA-4 — Cinque lingue e due temi**
- **Dato** l'interfaccia impostata a turno su ciascuna delle cinque lingue e su ciascuno dei due temi
- **Quando** si percorrono le sezioni
- **Allora** non compare nessuna chiave di traduzione mancante e nessun testo in una lingua diversa da quella
  scelta, e i colori restano leggibili in entrambi i temi

**CA-5 — Il colore-categoria coincide**
- **Dato** il manifesto del modulo e il file di listino dell'app
- **Quando** si confrontano `accentToken` e `category`
- **Allora** valgono entrambi `violet`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `frontend`, compreso il controllo dei tipi; l'intera suite prima
      del commit);
- [ ] prove di **unità** con la libreria di prova dei componenti sul manifesto e sulla comparsa condizionata del
      modulo, con lo strato di rete finto;
- [ ] prova di **isolamento fra account**: non applicabile al modulo, che non decide perimetri; la prova sta nel
      servizio (storia 0002);
- [ ] **prova end-to-end**: rimando — il percorso `[J-CAMPAIGNS]` nasce nella storia 0037, che è la sua
      proprietaria; un percorso che apre sei sezioni vuote non proverebbe niente che le prove dei componenti non
      provino già;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`);
- [ ] **manifesto dei dati**: nessuna voce nuova, la storia non tratta dati di persone;
- [ ] **registro delle decisioni** compilato, con annotato perché le sezioni sono sei e in quell'ordine;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia;
- [ ] controllo automatico di **accessibilità** verde sulle schermate introdotte;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare; il modulo è abilitato nello stub locale
      dell'abilitazione, finché quella reale non esiste.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0001` | Servono il servizio e la sua definizione OpenAPI, da cui si genera il client |
| Stub locale dell'abilitazione | Finché l'abilitazione reale non esiste, il modulo va acceso nello stub, altrimenti in locale non si vede |

## 7. Fuori ambito

- ogni schermata di dominio: iscritti, campagne, automazioni e rapporti arrivano con le rispettive epiche e
  riempiono queste sezioni;
- il blocco a quota esaurita: è la storia 0004;
- i dati di prova che rendono le schermate non vuote: è la storia 0005.

## 8. Punti aperti

- **Nome visibile dell'app nell'interfaccia** — «ReachGrove» è il nome commerciale del catalogo; se il nome
  cambiasse in fase di lancio cambierebbe solo una stringa di traduzione, non l'identificativo tecnico. Chiude lo
  sviluppatore, quando decide il nome definitivo.
- **Sezione «Impostazioni di invio»** — raccoglie domini mittenti e canali collegati, cioè materia dell'epica 04.
  Se in corso d'opera risultasse più naturale portarla dentro le impostazioni comuni dell'account, la scelta è di
  prodotto e non di questa storia.
