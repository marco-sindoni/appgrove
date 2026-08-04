# 0026 — Rilevazione delle impennate

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 05 — Budget, avvisi e anomalie
**Storia**: `0026` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0025`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile tecnico il cui programma è entrato in un ciclo di ritentativi alle due di notte
> voglio saperlo entro un'ora, e sapere subito quale funzione e quale modello lo stanno causando
> così da spegnerlo prima che mi costi il budget di tre mesi.

**Contesto.** È il caso che il catalogo cita per nome, ed è la vera differenza fra un cruscotto e uno strumento
utile. Il consumo dei modelli ha una caratteristica pericolosa: un difetto che ricomincia la stessa chiamata mille
volte non genera un errore visibile da nessuna parte, genera **una spesa**. Il budget mensile non aiuta (una notte
di ciclo può bruciarlo tutto prima della prima soglia) e la previsione neanche (è tarata sul giorno). Serve un
controllo su una finestra breve, che confronti l'adesso con il comportamento abituale.

## 2. Requisiti funzionali

1. **RF-1** — Su una finestra breve (proposta: l'ora) si confronta la spesa con quella tipica della stessa ora nei
   giorni precedenti; uno scostamento oltre una soglia dichiarata genera un avviso di impennata.
2. **RF-2** — L'avviso di impennata dice **dove** sta l'impennata, non solo che c'è: modello, valore di
   dimensione, chiave del fornitore o fonte che contribuiscono di più allo scostamento.
3. **RF-3** — Il caso del **ciclo di ritentativi** è riconosciuto e chiamato con il suo nome: molte chiamate quasi
   identiche per conteggi e modello, molto ravvicinate, spesso con lo stesso valore di dimensione. Il testo
   dell'avviso lo suggerisce come causa probabile, senza affermarlo come certo.
4. **RF-4** — Le impennate legittime non devono diventare rumore: un'impennata che si ripete nello stesso momento
   ogni settimana (per esempio una elaborazione del lunedì mattina) viene riconosciuta come abituale dopo alcune
   ripetizioni e smette di generare avvisi, dichiarandolo.
5. **RF-5** — La rilevazione dichiara la propria affidabilità: con meno di alcuni giorni di storico non si
   pronuncia, invece di scambiare per anomalia il primo giorno di uso normale.
6. **RF-6** — L'utente può regolare la sensibilità con una scelta a tre livelli, spiegati in parole («avvisami
   solo se è grosso» / «equilibrato» / «avvisami presto, anche a costo di qualche falso allarme»), e non con un
   numero da tarare a occhio.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La linea di riferimento del comportamento abituale si calcola sul solo
  `tenant_id` dell'account: **mai** una linea di riferimento condivisa fra account, che farebbe dipendere
  l'avviso di un cliente dal comportamento di un altro.
- **RT-2 — Persistenza (§8).** Migrazione sullo schema `app_spesa_modelli`: tabella `impennata` con `tenant_id`,
  finestra, valore osservato, valore atteso, scostamento, contributi principali, stato, colonne di controllo e
  cancellazione logica; e la linea di riferimento oraria conservata per account.
- **RT-3 — Interfaccia di programmazione (§2).** Rotte `GET /api/spesa_modelli/v1/impennate` e
  `GET /api/spesa_modelli/v1/impennate/{id}` con la scomposizione; errori in `problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-4 — Modulo frontend (§3, §5).** Le impennate compaiono nella panoramica e nella sezione Budget; la scheda
  mostra il confronto fra osservato e atteso e i contributi. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe presenti in `en, it, fr, es, de`, compresi i tre livelli di
  sensibilità espressi a parole.
- **RT-6 — Esposizione conversazionale (§12).** Strumento `spiega_impennata(periodo) → scomposizione del salto per
  modello, etichetta e ora`, marcato **lettura**: è la domanda «perché martedì è costato il triplo?», che in chat
  si risponde molto meglio che con un cruscotto.
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo. La scomposizione può contenere valori di
  dimensione, già dichiarati.
- **RT-8 — Registrazione eventi (§14).** Evento «impennata rilevata» con `tenant_id`, `app_id`, finestra e
  scostamento percentuale, senza i valori delle etichette.

## 4. Criteri di accettazione

**CA-1 — Impennata rilevata entro l'ora**
- **Dato** un account con consumo tipico di 2 € l'ora e un'ora in cui ne spende 60
- **Quando** la rilevazione gira
- **Allora** genera un avviso di impennata entro la finestra dichiarata, con osservato, atteso e scostamento

**CA-2 — Dove sta l'impennata**
- **Dato** l'impennata concentrata su un solo modello e su una sola funzionalità
- **Quando** si apre la scheda
- **Allora** modello e funzionalità compaiono in cima ai contributi

**CA-3 — Ciclo di ritentativi riconosciuto**
- **Dato** duemila chiamate quasi identiche per conteggi e modello in dieci minuti
- **Quando** la rilevazione gira
- **Allora** l'avviso suggerisce il ciclo di ritentativi come causa probabile, con la formula del sospetto e non
  dell'accertamento

**CA-4 — Impennata abituale**
- **Dato** un'elaborazione che ogni lunedì alle 6 consuma dieci volte il normale, ripetuta per quattro settimane
- **Quando** si ripete la quinta volta
- **Allora** non genera avviso, e la scheda dichiara che quel picco è riconosciuto come abituale

**CA-5 — Troppo poco storico**
- **Dato** un account attivo da due giorni
- **Quando** la rilevazione gira
- **Allora** non genera avvisi e dichiara che serve più storico

**CA-6 — Isolamento fra account**
- **Dato** un account con consumo altissimo e uno con consumo bassissimo
- **Quando** entrambe le rilevazioni girano
- **Allora** ciascuna usa solo la propria linea di riferimento

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul confronto con la linea di riferimento, sul riconoscimento del picco abituale e sui
      tre livelli di sensibilità, con serie di dati costruite apposta;
- [ ] prova di **isolamento fra account** sulla linea di riferimento;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «arriva un lotto anomalo,
      compare l'impennata con la sua scomposizione», e aggiornare il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, in particolare sulla scelta di una linea di riferimento per account e
      sulla sensibilità espressa a parole;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `spiega_impennata`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0025` | Riusa il recapito e la disciplina anti-tempesta degli avvisi |
| Storia `0012` | Una fonte ferma produrrebbe un'impennata al ritorno: va distinta |

## 7. Fuori ambito

- il riconoscimento di chiamate identiche per **contenuto**: impossibile per costruzione, perché il contenuto non
  entra nell'app (§6 del documento capofila). Il riconoscimento si basa su conteggi, modello e ritmo;
- il blocco automatico delle chiamate durante un'impennata: è la storia `0027`, e non è un blocco nostro.

## 8. Punti aperti

- **La finestra e la soglia predefinite.** Un'ora e uno scostamento che vale più volte il tipico sono una
  proposta ragionevole, ma vanno tarate su dati veri: con clienti a basso consumo, un'ora può essere troppo corta
  per avere una linea di riferimento sensata. Da rivedere dopo i primi clienti; la chiude lo sviluppatore.
