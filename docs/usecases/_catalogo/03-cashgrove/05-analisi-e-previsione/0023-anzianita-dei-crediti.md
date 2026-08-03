# 0023 — Anzianità dei crediti

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 05 — Analisi e previsione
**Storia**: `0023` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0009`, `0010`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio vedere in una schermata quanto denaro è fermo e da quanto tempo
> così da capire in dieci secondi se il problema è grande o piccolo, e su chi conviene insistere per primo.

**Contesto.** È il prospetto per fasce di scaduto, la vista che ogni prodotto della categoria mette al centro
([documento capofila](../application-description.md) §2.5) e che la micro-impresa oggi non ha, perché costruirla a mano
in un foglio di calcolo richiede mezz'ora e va rifatta ogni settimana. Il valore non è la tabella: è il fatto che sia
sempre aggiornata senza che nessuno la aggiorni.

## 2. Requisiti funzionali

1. **RF-1** — La *Panoramica* mostra il totale dei crediti aperti suddiviso in cinque fasce: non ancora scaduto, 1-30,
   31-60, 61-90, oltre 90 giorni di ritardo.
2. **RF-2** — Ogni fascia riporta importo totale e numero di crediti; da ogni fascia si arriva all'elenco filtrato.
3. **RF-3** — Il prospetto è disponibile anche per debitore: chi concentra il denaro fermo e in quale fascia.
4. **RF-4** — I crediti `sospeso` e `in_escalation` compaiono nel totale ma sono distinti visivamente, perché non sono
   crediti «in lavorazione normale».
5. **RF-5** — Il prospetto si può calcolare a una **data passata**, per rispondere a «com'era a fine mese scorso».
6. **RF-6** — Il prospetto è esportabile in formato tabellare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'aggregazione filtra per `tenant_id` preso dal token verificato; nessuna
  interrogazione aggrega su più account, nemmeno per statistiche interne.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/crediti/v1/anzianita` (con parametro di data) e
  `GET /api/crediti/v1/anzianita/per-debitore`; errori in `application/problem+json`; definizione OpenAPI aggiornata
  nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: è una aggregazione. Va verificato che gli indici su
  (`tenant_id`, `stato`, `data_scadenza`) la reggano; se il calcolo a data passata dovesse costare troppo, la strada è
  una vista materializzata, non una tabella di riepilogo aggiornata a mano.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro delle fasce nella *Panoramica*, con collegamento all'elenco filtrato;
  vista per debitore; solo token del sistema di design; funziona in tema chiaro e scuro. Le fasce si distinguono per
  **valore** e non per solo colore, così da restare leggibili anche a chi non distingue le tinte.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili, comprese le etichette delle fasce, passano dallo
  spazio-nomi `crediti` e sono presenti in `en, it, fr, es, de`; gli importi sono formattati secondo la lingua attiva.
- **RT-6 — Varchi e quota (§6, §7).** Non consuma quota. Accessibile anche al ruolo in sola lettura: è la vista che il
  commercialista guarda.
- **RT-7 — Esposizione conversazionale (§12).** `riepilogo_anzianita(alla_data?) → fasce con importi e conteggi` è
  dichiarato qui come strumento di **lettura**, raccolto nel contratto della storia `0028`. È lo strumento che rende
  l'app utile in chat più di quanto lo sia sullo schermo.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo; la vista per debitore mostra denominazioni già presenti.
- **RT-9 — Registrazione eventi (§14).** L'evento «prospetto esportato» è registrato con `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione, senza importi.

## 4. Criteri di accettazione

**CA-1 — Le fasce**
- **Dato** un account con crediti a 5, 40, 75 e 200 giorni di ritardo e uno non ancora scaduto
- **Quando** si apre la *Panoramica*
- **Allora** ciascun credito compare nella fascia giusta, e la somma delle fasce è pari al totale aperto

**CA-2 — Dal totale all'elenco**
- **Dato** la fascia «oltre 90 giorni» con 4 crediti · **Quando** si tocca la fascia · **Allora** si arriva all'elenco
  filtrato con esattamente quei 4 crediti

**CA-3 — Sospesi distinti**
- **Dato** due crediti scaduti di cui uno sospeso per contestazione · **Quando** si guarda il prospetto · **Allora**
  entrambi sono nel totale e quello sospeso è distinto, con la ragione visibile

**CA-4 — Data passata**
- **Dato** un credito incassato il 20 del mese · **Quando** si chiede il prospetto al giorno 15 · **Allora** quel
  credito compare come ancora aperto, nella fascia che aveva quel giorno

**CA-5 — Isolamento fra account**
- **Dato** due account con crediti propri · **Quando** ciascuno apre il prospetto · **Allora** i totali sono quelli del
  proprio account e nessun importo dell'altro vi contribuisce

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend);
- [ ] prove di **unità** sull'assegnazione alle fasce (compresi i confini esatti: 30, 31, 60, 61…) e di **integrazione**
      sul calcolo a data passata;
- [ ] prova di **isolamento fra account** sull'aggregazione;
- [ ] **prova end-to-end**: *coprire ora* — «dopo l'incasso, il prospetto cambia» è un passo osservabile del percorso
      `[J-CREDITI]`; voce registrata nel registro di copertura con proprietaria la storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, dichiarato;
- [ ] **registro delle decisioni** compilato, in particolare sui confini delle fasce e sul trattamento dei sospesi;
- [ ] contratto degli **strumenti conversazionali**: `riepilogo_anzianita` dichiarato come lettura;
- [ ] **accessibilità**: controllo automatico sulla *Panoramica*, con le fasce distinguibili senza dipendere dal colore;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0009` | Il residuo deve essere aggiornato dagli incassi, altrimenti il prospetto è falso |
| storia `0010` | Servono gli stati e il passaggio automatico a `scaduto` |

## 7. Fuori ambito

- Il tempo medio di incasso: storia `0024`.
- La previsione: storia `0026`.
- Il confronto con periodi precedenti in forma di grafico storico: rimandato alla storia `0024`, che introduce
  l'andamento.

## 8. Punti aperti

Nessuno.
