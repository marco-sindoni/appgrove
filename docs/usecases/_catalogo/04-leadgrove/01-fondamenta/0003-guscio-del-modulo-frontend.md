# 0003 — Guscio del modulo frontend

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una micro-impresa abbonata a LeadGrove
> voglio vedere l'app comparire nel mio backoffice, con le sue sezioni e nella mia lingua
> così da capire che l'ho attivata davvero e da poterci entrare.

**Contesto.** Un servizio senza modulo frontend è invisibile: il cliente non ha modo di sapere che l'app esiste.
Questa storia crea il **guscio** — manifesto, registrazione, voci della barra laterale, cinque lingue, colore —
con una sola schermata di panoramica ancora vuota. Le schermate vere arrivano dalle epiche di dominio; farle prima
del guscio significherebbe scriverle senza un posto dove metterle.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il modulo `frontend/apps/backoffice/src/modules/sales/` con `manifest.ts` che dichiara
   identificativo `sales`, nome, icona, `accentToken` `blue`, le sezioni, le risorse e la quota `seats`.
2. **RF-2** — Il modulo è aggiunto all'elenco dei moduli del registro delle app e compare nella barra laterale
   **solo** quando registro e abilitazione dicono entrambi di sì.
3. **RF-3** — Le sezioni dichiarate sono: Panoramica, Contatti, Aziende, Trattative, Attività, Rapporti,
   Impostazioni. Le sezioni non ancora implementate mostrano uno stato vuoto che dice cosa arriverà, non una
   pagina bianca.
4. **RF-4** — La schermata di panoramica mostra il nome dell'app, il consumo dei posti e lo stato di primo avvio
   («non c'è ancora nulla qui») con l'azione per cominciare.
5. **RF-5** — Tutte le stringhe visibili passano dallo spazio-nomi `sales` e sono presenti in `en, it, fr, es, de`.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il modulo non conosce l'identificativo dell'account se non attraverso il
  contesto che la shell gli passa, e non accede mai al token direttamente.
- **RT-2 — Interfaccia di programmazione (§2).** I dati si leggono con il client generato dalla definizione
  OpenAPI del servizio `sales`; nessuna chiamata scritta a mano.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova.
- **RT-4 — Modulo frontend (§3, §5).** Modulo React caricato su richiesta; solo token del sistema di design;
  funziona in tema chiaro e scuro; nessun colore scritto a mano; vietate le librerie con aspetto proprio.
- **RT-5 — Cinque lingue (§4).** Traduzioni in `frontend/apps/backoffice/src/modules/sales/i18n/{en,it,fr,es,de}.ts`
  sotto lo spazio-nomi `sales`; nessun testo visibile scritto a mano nei componenti. La storia non è conclusa se
  manca una lingua.
- **RT-6 — Varchi e quota (§6, §7).** La barra laterale mostra il modulo su registro ∩ abilitazione;
  l'abilitazione si legge dalla proiezione locale, mai con una chiamata sincrona all'app centrale.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: la panoramica mostra conteggi.
- **RT-9 — Registrazione eventi (§14).** Nessun evento applicativo nuovo lato servizio.

## 4. Criteri di accettazione

**CA-1 — Il modulo compare a chi è abilitato**
- **Dato** un account abilitato a `sales`
- **Quando** l'utente apre il backoffice
- **Allora** vede LeadGrove nella barra laterale con il colore-categoria blu e le sette sezioni

**CA-2 — Il modulo non compare a chi non è abilitato**
- **Dato** un account senza abbonamento a `sales`
- **Quando** l'utente apre il backoffice
- **Allora** LeadGrove non compare, e una chiamata diretta all'indirizzo del modulo non mostra dati

**CA-3 — Cinque lingue**
- **Dato** l'interfaccia impostata a turno su `en, it, fr, es, de`
- **Quando** si apre la panoramica
- **Allora** nessuna chiave di traduzione compare grezza e nessun testo resta in una lingua diversa da quella scelta

**CA-4 — Tema chiaro e scuro**
- **Dato** il tema chiaro e poi il tema scuro
- **Quando** si apre la panoramica
- **Allora** testi e sfondi restano leggibili e nessun colore risulta scritto a mano fuori dai token

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`frontend`, compreso il controllo dei tipi; l'intera suite prima del
      commit);
- [ ] prove di **unità** sui componenti con strato di rete finto;
- [ ] prova di **isolamento fra account**: non applicabile lato modulo, coperta lato servizio;
- [ ] **prova end-to-end**: rimando — il percorso `[J-SALES]` nasce nella storia 0037, che è la storia
      proprietaria; qui si verifica con prove di componente;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotata la scelta delle sette sezioni;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia;
- [ ] controllo automatico di **accessibilità** verde sulla panoramica;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare, con il modulo abilitato nello stub locale.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0001` | Serve la definizione OpenAPI da cui si genera il client |
| Stub locale dell'abilitazione | Finché l'abilitazione reale non esiste, il modulo va acceso nello stub, altrimenti non è visibile in locale |

## 7. Fuori ambito

- le schermate di contatti, aziende, trattative, attività e rapporti: epiche 02-06;
- la lavagna a colonne: storia 0014;
- l'avviso di quota raggiunta nella sua forma definitiva: storia 0004.

## 8. Punti aperti

- **Icona del modulo** — proposta `users`; se il sistema di design non la prevede, la scelta ricade su chi cura i
  token. Non è una decisione di questa storia.
