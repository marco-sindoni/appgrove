# 0023 — Pagina dell'abbonato con collegamento firmato

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 05 — Portale dell'abbonato
**Storia**: `0023` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona abbonata a una palestra
> voglio aprire un collegamento dalla mail e vedere subito a cosa sono abbonata, quanto pago e fino a quando
> così da non dover telefonare in reception per una domanda a cui potrei rispondermi da sola.

**Contesto.** L'abbonato **non è un nostro utente**: non ha credenziali di appgrove, non ne avrà mai, e chiedergli
di crearne sarebbe insensato oltre che controproducente. Ma qualcosa deve poter vedere, per due ragioni: perché
le comunicazioni di rinnovo devono portare da qualche parte, e perché — è il punto della storia `0024` — la
disdetta dev'essere raggiungibile **senza dover inserire credenziali**. Da qui il disegno: una pagina pubblica
per abbonamento, raggiungibile solo attraverso un **collegamento firmato** che l'app manda al recapito
dell'abbonato. Chi ha il collegamento vede quel solo abbonamento; chi non ce l'ha non ha modo di indovinarlo.

È una superficie pubblica: il confine di ciò che mostra va deciso con parsimonia, e le difese sono nella storia
`0026`.

## 2. Requisiti funzionali

1. **RF-1** — Ogni abbonamento ha un collegamento firmato, che l'app include negli avvisi di rinnovo e nei
   solleciti, e che il cliente può copiare dalla scheda per mandarlo a mano.
2. **RF-2** — La pagina mostra: piano, canone, periodo in corso, data del prossimo rinnovo, ultimo giorno utile
   per disdire, e l'elenco delle scadenze con il loro stato. Nient'altro.
3. **RF-3** — La pagina **non** mostra: dati di altri abbonamenti dello stesso abbonato, note interne, nome
   dell'operatore, riferimenti dell'autorizzazione all'addebito, alcun dato di altri abbonati.
4. **RF-4** — Il collegamento **scade**; scaduto, la pagina spiega cosa è successo e offre di ricevere un
   collegamento nuovo al recapito già noto — mai a un recapito indicato lì per lì.
5. **RF-5** — La pagina dichiara chi è il titolare del rapporto (il **cliente**, non appgrove) e come contattarlo.
6. **RF-6** — La pagina funziona su telefono, in tema chiaro e scuro, e nella lingua dell'abbonato quando è nota.

## 3. Requisiti tecnici

- **RT-1 — Superficie pubblica.** La pagina sta **fuori** dal backoffice: nessuna sessione, nessun token di
  accesso della piattaforma. L'autorizzazione è **solo** il gettone firmato, verificato dal servizio.
- **RT-2 — Isolamento fra account (§1).** Il gettone identifica account e abbonamento insieme: non esiste un
  gettone che possa risolvere su un abbonamento di un altro account, e la verifica lo controlla.
- **RT-3 — Interfaccia di programmazione (§2).** Rotte pubbliche `GET /api/abbonati/v1/pubblico/{gettone}` e
  `POST /api/abbonati/v1/pubblico/{gettone}/nuovo-collegamento`; errori in `problem+json` che **non** rivelano
  se un abbonamento esiste; OpenAPI aggiornata.
- **RT-4 — Persistenza (§8).** Migrazione `V17__gettone_abbonato.sql`: la tabella dei gettoni con `tenant_id`,
  colonne di controllo, scadenza e momento d'uso. Il gettone si conserva **cifrato o come impronta**, mai in
  chiaro.
- **RT-5 — Modulo frontend (§3, §5).** Pagina autonoma, con i soli token del sistema di design; nessuna
  dipendenza dal guscio del backoffice; funziona senza che l'utente sia autenticato.
- **RT-6 — Cinque lingue (§4).** Tutte le stringhe della pagina in `en, it, fr, es, de`; la lingua si sceglie da
  quella dell'abbonato, con quella dell'account come ripiego.
- **RT-7 — Varchi e quota (§6).** La pagina pubblica **non** attraversa la catena dei varchi dell'utente, ma
  **rispetta** lo stato dell'account: se l'abbonamento di piattaforma del cliente è scaduto, la pagina risponde
  in modo neutro, senza esporre dati.
- **RT-8 — Esposizione conversazionale (§12).** Nessuno strumento: la superficie pubblica non si comanda da chat.
- **RT-9 — Dati personali (§10).** La pagina espone dati di una persona a chi ha il collegamento: va dichiarato
  nel manifesto **cosa** è esposto e con quale presidio. Il gettone stesso è un dato da proteggere.
- **RT-10 — Registrazione eventi (§14).** `pagina pubblica aperta`, `collegamento scaduto`, `nuovo collegamento
  richiesto`, con `tenant_id`, `app_id` e correlazione, **senza** recapito e senza gettone.
- **RT-11 — Prove (§11).** Prove che la pagina non espone nulla oltre il consentito e che un gettone di un altro
  account non risolve; controllo automatico di accessibilità sulla pagina.

## 4. Criteri di accettazione

**CA-1 — Apertura con collegamento valido**
- **Dato** un abbonato che riceve l'avviso di rinnovo
- **Quando** apre il collegamento **senza inserire alcuna credenziale**
- **Allora** vede piano, canone, periodo, prossimo rinnovo, ultimo giorno utile per disdire e le sue scadenze

**CA-2 — Nulla di più del dovuto**
- **Dato** un abbonato con due abbonamenti e una nota interna sulla sua scheda
- **Quando** apre il collegamento di uno dei due
- **Allora** vede solo quell'abbonamento, e la nota interna non compare da nessuna parte

**CA-3 — Collegamento scaduto**
- **Dato** un collegamento scaduto · **Quando** lo si apre
- **Allora** la pagina spiega e offre di ricevere un collegamento nuovo al recapito già noto, senza chiedere un
  indirizzo

**CA-4 — Gettone di un altro account**
- **Dato** un gettone valido dell'account `A` · **Quando** lo si usa contro l'account `B`
- **Allora** la risposta è neutra e non conferma l'esistenza di nulla

**CA-5 — Telefono e tema scuro**
- **Dato** uno schermo largo 390 punti in tema scuro · **Quando** si apre la pagina
- **Allora** è leggibile, non scorre in orizzontale e i contrasti reggono

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`);
- [ ] prove di **unità** sulla verifica e sulla scadenza del gettone; **integrazione** sulla rotta pubblica;
- [ ] prova di **isolamento fra account** sul gettone;
- [ ] **prova end-to-end**: *coprire ora* — è il primo passo del percorso `[J-ABBONATI-PUBBLICO]` della storia
      `0034`; registro [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato: cosa la pagina espone, a chi, con quale presidio;
- [ ] **registro delle decisioni** compilato: nessuna credenziale per l'abbonato, gettone firmato, elenco chiuso
      di ciò che si mostra;
- [ ] controllo di accessibilità verde sulla pagina.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0010` | serve un abbonamento da mostrare |
| storia `0013` | è l'avviso di rinnovo a portare il collegamento all'abbonato |

## 7. Fuori ambito

- la disdetta: storia `0024` — è la ragione per cui questa pagina esiste, ma è una storia a sé;
- la richiesta di cambio piano: storia `0025`;
- le difese contro l'abuso della superficie pubblica: storia `0026`, e **le due vanno rilasciate insieme**;
- il pagamento dell'abbonato dalla pagina: **mai** (§5.2 della descrizione);
- l'iscrizione di chi non è ancora abbonato: fuori (punto aperto della storia `0010`).

## 8. Punti aperti

**Durata del collegamento.** Troppo breve e l'abbonato che apre la mail una settimana dopo trova una pagina
scaduta; troppo lunga e un collegamento inoltrato per sbaglio resta valido per mesi. **Proposta**: durata
dell'ordine di alcune settimane, allineata al preavviso del piano, così che il collegamento dell'avviso di
rinnovo sia ancora valido nell'ultimo giorno utile per disdire — altrimenti si crea un ostacolo alla disdetta,
che è precisamente ciò che la legge vieta. Chiude: lo sviluppatore, con la revisione legale.
