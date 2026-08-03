# 0003 — Guscio del modulo frontend

**Applicazione**: 12 — DeskGrove Support (`helpdesk`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un'azienda che ha appena attivato l'assistenza clienti
> voglio vedere DeskGrove nella barra laterale del mio backoffice, con le sue sezioni al posto giusto
> così da capire che l'ho attivata davvero e da avere un posto dove entrare, anche prima che dentro ci sia
> qualcosa.

**Contesto.** Il servizio risponde ma nessuno può vederlo. Questa storia crea il modulo del backoffice: il
manifesto, la registrazione, le sezioni vuote, le cinque lingue e il colore-categoria. È volutamente uno scheletro:
mettere qui anche una sola schermata vera significherebbe scriverla prima che il dominio esista, e riscriverla
subito dopo. Il modulo compare nella barra laterale solo quando **registro e abilitazione** dicono di sì: è la
regola comune a tutte le applicazioni, e la si verifica qui una volta per tutte.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il modulo `frontend/apps/backoffice/src/modules/helpdesk/` con il proprio manifesto, che
   dichiara identificativo, nome, icona, colore d'accento, sezioni, risorse e metrica di quota.
2. **RF-2** — Il modulo è aggiunto all'elenco dei moduli del registro delle app e compare nella barra laterale solo
   quando l'account è abilitato; finché l'abilitazione reale non esiste, è abilitato nello stub locale.
3. **RF-3** — Le sezioni dichiarate sono quattro e corrispondono a ciò che l'app diventerà: **Richieste**, **Base di
   conoscenza**, **Rapporti**, **Impostazioni**. Ognuna mostra per ora uno stato vuoto che dice cosa ci sarà e non
   finge di funzionare.
4. **RF-4** — Tutti i testi visibili passano dallo spazio-nomi `helpdesk` e sono presenti in tutte e cinque le
   lingue dell'interfaccia: inglese, italiano, francese, spagnolo, tedesco.
5. **RF-5** — Il modulo usa il client generato dalla definizione delle interfacce del servizio: non scrive chiamate
   di rete a mano e non accede al token se non attraverso il contesto che la shell gli passa.
6. **RF-6** — Il modulo funziona in tema chiaro e in tema scuro senza alcun colore scritto a mano.

## 3. Requisiti tecnici

- **RT-1 — Modulo frontend (§3).** Applicazione a pagina singola in React e TypeScript con Vite; il modulo è
  caricato **su richiesta**; il manifesto dichiara `{ id, name, icon, accentToken, sections[], resources, quota,
  component }` e viene aggiunto all'elenco `MODULES` del registro. Il modulo **non** gestisce l'autenticazione e
  **non** conosce l'identificativo dell'account se non tramite il contesto della shell.
- **RT-2 — Sistema di design (§5).** Solo i token del sistema di design condiviso; colore-categoria `teal`
  (`accentToken`), che deve coincidere con il valore `category` del listino (storia `0004`). Componenti senza stile
  proprio sopra i token; **vietate** le librerie con un aspetto proprio marcato, che sfondano il tema.
- **RT-3 — Cinque lingue (§4).** Traduzioni accanto al modulo, in
  `frontend/apps/backoffice/src/modules/helpdesk/i18n/{en,it,fr,es,de}.ts`, sotto lo spazio-nomi `helpdesk`.
  **Nessun testo visibile scritto a mano nei componenti.** La storia non è conclusa se manca una lingua.
- **RT-4 — Varchi (§6).** La visibilità del modulo è **registro ∩ abilitazione**: l'abilitazione si legge dalla
  proiezione locale alimentata a eventi, mai con una chiamata di rete sincrona all'app centrale sul percorso caldo.
- **RT-5 — Isolamento fra account (§1).** Non applicabile alla scrittura, perché il modulo non scrive nulla; vale
  però la regola generale: il modulo non costruisce mai una richiesta con un identificativo di account preso
  dall'interfaccia.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo: lo scheletro non mostra dati.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento: non ci sono funzioni.

## 4. Criteri di accettazione

**CA-1 — Il modulo compare quando deve**
- **Dato** un account abilitato all'app `helpdesk`
- **Quando** l'utente apre il backoffice
- **Allora** nella barra laterale, sotto «Le tue app», compare DeskGrove Support con le sue quattro sezioni e il
  colore verde acqua

**CA-2 — Il modulo non compare quando non deve**
- **Dato** un account **non** abilitato all'app
- **Quando** l'utente apre il backoffice
- **Allora** il modulo non compare in nessun punto dell'interfaccia, e la navigazione diretta all'indirizzo della
  sezione non lo mostra

**CA-3 — Cinque lingue**
- **Dato** l'interfaccia impostata di volta in volta su ciascuna delle cinque lingue
- **Quando** si apre ogni sezione del modulo
- **Allora** nessuna etichetta appare nella lingua sbagliata o come chiave di traduzione grezza

**CA-4 — Due temi**
- **Dato** il tema chiaro e poi il tema scuro
- **Quando** si apre il modulo
- **Allora** testo e sfondi restano leggibili e nessun colore è definito fuori dai token del sistema di design

**CA-5 — Stato vuoto onesto**
- **Dato** il modulo appena attivato
- **Quando** si apre la sezione «Richieste»
- **Allora** compare uno stato vuoto che dice che non c'è ancora nulla e cosa succederà, senza pulsanti che non
  fanno niente

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `frontend`; l'intera suite prima del commit), compreso il
      controllo dei tipi, che è parte del cancello;
- [ ] prove di **unità** sul manifesto e sulla visibilità del modulo con abilitazione presente e assente;
- [ ] prova di **isolamento fra account**: non applicabile — il modulo non legge dati;
- [ ] **prova end-to-end**: *rimando* — il percorso `[J-HELPDESK]` nasce con la storia `0037`; qui la superficie
      esiste ma è vuota e non c'è un percorso significativo da fissare;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica — la storia non tratta dati personali;
- [ ] **registro delle decisioni** compilato, con annotate le quattro sezioni scelte e il perché;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta;
- [ ] controllo automatico di **accessibilità** verde sulle schermate introdotte;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0001` di questa app | Il client del frontend si genera dalla definizione delle interfacce del servizio |
| Registro delle app del backoffice e stub locale dell'abilitazione | Il modulo si vede solo se registrato **e** abilitato; in locale l'abilitazione reale non c'è ancora |

## 7. Fuori ambito

- **Le schermate vere**: l'elenco delle richieste è la storia `0010`, il dettaglio la `0007`.
- **La barra del consumo dei posti operatore**: la aggiunge la storia `0004`, insieme alla quota.
- **Le pagine viste dal cliente finale** (portale, collegamento di stato): non sono nel backoffice e stanno
  nell'epica 06.

## 8. Punti aperti

- **Icona del modulo**: la proposta è un salvagente (`life-buoy`), perché dice «qui si viene aiutati» senza bisogno
  di leggere. Va confermata rispetto all'insieme delle icone già in uso, che questo documento non conosce.
- **Nome commerciale contro identificativo tecnico**: nella barra laterale compare «DeskGrove Support», mentre
  l'identificativo tecnico è `helpdesk`. È voluto e va tenuto coerente: se il nome commerciale cambia, non cambia
  nulla nel codice.
