# 0003 — Guscio del modulo frontend

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha appena attivato SubGrove
> voglio vederla comparire nella barra laterale con le sue sezioni, nella mia lingua
> così da capire subito dove si lavora, anche prima che ci sia dentro qualcosa.

**Contesto.** Il modulo frontend è un modulo caricato su richiesta dentro il backoffice: esiste quando il
registro delle app e l'abilitazione dell'account dicono entrambi di sì. Questa storia mette in piedi il guscio —
manifesto, registrazione, sezioni vuote, cinque lingue, colore-categoria — perché tutte le schermate successive
abbiano un posto dove nascere. C'è una cura particolare da avere qui, e riguarda le **parole**: il backoffice ha
già una sezione «I tuoi abbonamenti» che è la piattaforma che parla dei propri contratti col cliente. Le sezioni
di questo modulo devono parlare di **abbonati**, **piani** e **scadenze**, mai di «i tuoi abbonamenti», altrimenti
il cliente non capisce più in quale delle due sta guardando (§10.1 della descrizione).

## 2. Requisiti funzionali

1. **RF-1** — Esiste il modulo `frontend/apps/backoffice/src/modules/abbonati/` con il suo manifesto, che dichiara
   identificativo, nome, icona, colore-categoria, sezioni, risorse e metrica di quota.
2. **RF-2** — Il modulo è registrato nell'elenco dei moduli e compare nella barra laterale **solo** quando registro
   e abilitazione concordano.
3. **RF-3** — Le sezioni dichiarate sono cinque: *Panoramica*, *Abbonati*, *Piani*, *Scadenze*, *Andamento*.
   In questa storia sono gusci vuoti con uno stato «non c'è ancora nulla qui» che dice cosa fare.
4. **RF-4** — Tutti i testi visibili passano dallo spazio-nomi `abbonati` e sono presenti in `en, it, fr, es, de`.
5. **RF-5** — Nessuna sezione si chiama «abbonamenti» al singolare o al plurale nella lingua dell'interfaccia:
   la parola resta alla piattaforma.

## 3. Requisiti tecnici

- **RT-1 — Modulo frontend (§3).** React con TypeScript e Vite, modulo caricato su richiesta; il modulo **non**
  gestisce l'autenticazione e non conosce l'account se non attraverso il contesto che la shell gli passa; i dati
  si leggono con il client generato dalla definizione delle interfacce.
- **RT-2 — Sistema di design (§5).** Solo i token condivisi: neutri caldi, accento a runtime, tema chiaro e
  scuro; colore-categoria `red` dichiarato nel manifesto (`accentToken`) **e** nel listino (`category`), che
  devono coincidere. Niente librerie con un aspetto proprio marcato.
- **RT-3 — Cinque lingue (§4).** Traduzioni accanto al modulo, in
  `.../modules/abbonati/i18n/{en,it,fr,es,de}.ts`, sotto lo spazio-nomi `abbonati`. **Nessun testo visibile
  scritto a mano nei componenti**: la storia non è conclusa se ne manca una lingua.
- **RT-4 — Varchi (§6).** La barra laterale mostra il modulo solo con l'abilitazione; il confine di applicazione
  resta comunque il backend — il frontend è solo esperienza d'uso.
- **RT-5 — Dati personali (§10).** Nessun dato personale nuovo: le sezioni sono vuote.
- **RT-6 — Prove (§11).** Vitest con Testing Library e strato di rete finto; controllo dei tipi `tsc --noEmit`;
  controllo automatico di accessibilità sulle schermate introdotte.

## 4. Criteri di accettazione

**CA-1 — Il modulo compare a chi è abilitato**
- **Dato** un account abilitato a `abbonati`
- **Quando** l'utente apre il backoffice
- **Allora** vede la voce SubGrove con le sue cinque sezioni e il colore-categoria assegnato

**CA-2 — Il modulo non compare a chi non è abilitato**
- **Dato** un account senza abilitazione · **Quando** l'utente apre il backoffice · **Allora** non vede nulla di
  SubGrove, e un accesso diretto all'indirizzo della sezione riceve comunque `402` dal backend

**CA-3 — Cinque lingue**
- **Dato** l'interfaccia in ciascuna delle cinque lingue · **Quando** si aprono le cinque sezioni
- **Allora** nessuna stringa resta nella lingua sbagliata e nessuna chiave appare al posto del testo

**CA-4 — Tema chiaro e scuro**
- **Dato** il tema scuro attivo · **Quando** si aprono le sezioni · **Allora** i contrasti restano leggibili e
  nessun colore è scritto a mano

**CA-5 — Nessuna confusione di parole**
- **Dato** l'interfaccia in italiano · **Quando** si legge la barra laterale
- **Allora** «I tuoi abbonamenti» compare **una** volta sola, nella parte di piattaforma, e non dentro il modulo

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh frontend`, controllo dei tipi compreso;
- [ ] prove di **unità** sul manifesto e sulla comparsa condizionata del modulo;
- [ ] prova di **isolamento fra account**: *nessun impatto* — il modulo non legge dati;
- [ ] **prova end-to-end**: *rimando* — la comparsa del modulo entra nel percorso `[J-ABBONATI]` della storia
      `0033`, con voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato: nomi delle sezioni e divieto della parola «abbonamenti»;
- [ ] modulo abilitato nello stub locale, così che si veda subito dopo l'unione del ramo.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | serve il client generato dalla definizione delle interfacce |
| decisione sul colore-categoria (§3 della descrizione, punto aperto n. 6) | manifesto e listino devono concordare |

## 7. Fuori ambito

- il contenuto delle sezioni: le storie delle epiche 02-06;
- l'indicatore di consumo della quota: storia `0004`;
- la pagina pubblica per l'abbonato, che **non** sta nel backoffice: storia `0023`.

## 8. Punti aperti

**Nome commerciale a schermo.** Nel manifesto conviene mostrare «SubGrove» o una parola descrittiva come
«Abbonati»? La prima costruisce il marchio, la seconda si capisce senza spiegazioni. Proposta: nome descrittivo
nella barra laterale, marchio nella pagina di panoramica e nel catalogo delle app. Chiude: lo sviluppatore, con
la decisione di prodotto sui nomi del catalogo.
