# 0030 — Previsione con metodo dichiarato

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 06 — Rapporti, esportazione e previsioni
**Storia**: `0030` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0015`, `0016`, `0027`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che il 12 del mese vuole sapere dove andrà a finire il mese
> voglio vedere una proiezione **dichiarata come tale**, con scritto come è stata ottenuta e quanto è incerta
> così da usarla per decidere, sapendo che è una stima e non una misura.

**Contesto.** È la storia più pericolosa dell'applicazione, e va detto in apertura: **una previsione è un numero
che non viene da nessun dato osservato**. Tutta l'architettura di InsightGrove è costruita per rendere ogni numero
risalibile alla sua fonte (§4.3 della [descrizione](../application-description.md)); una proiezione non ha una
fonte da mostrare, ha un **metodo**. Se le due cose finiscono nella stessa colonna, nello stesso colore e con lo
stesso peso visivo, l'app perde in un colpo solo la proprietà per cui esiste. Da qui la regola che governa ogni
requisito di questa storia: **rilevato e previsto non si toccano mai** — non nel modello dati, non nel disegno,
non nel file esportato, non nelle risposte del copilota, non negli avvisi.

## 2. Requisiti funzionali

1. **RF-1** — Si chiede la proiezione di **una** metrica su un periodo futuro fra tre soli casi: **fine del
   periodo in corso**, **prossimo periodo**, **prossimi tre periodi**. Nessun orizzonte libero.
2. **RF-2** — I metodi ammessi sono due, entrambi elementari e spiegabili in una riga: **proporzione sul periodo
   in corso** (i giorni trascorsi proiettati sul periodo intero, corretti per i giorni lavorativi) e **media
   mobile** sugli ultimi N periodi conclusi. Il metodo usato è **scritto accanto al numero**, non in una nota.
3. **RF-3** — Ogni previsione porta un **intervallo** (valore minimo e massimo) e il numero di periodi su cui è
   costruita. Una previsione senza intervallo non si mostra: un valore singolo si legge come una promessa.
4. **RF-4** — La distinzione da un valore rilevato è **quadrupla e ridondante**: (a) nei dati, il campo
   `tipo_valore` vale `previsto` e la previsione vive nella propria entità, mai dentro `ValoreMetrica`; (b) nel
   disegno, la linea è **tratteggiata** con la banda dell'intervallo e la cifra porta l'etichetta testuale
   «stima», non affidata al solo colore; (c) nell'esportazione, la colonna `tipo_valore` della storia 0027 più
   metodo e intervallo; (d) nel copilota, la parola «stima» e il metodo compaiono **nella prima frase** della
   risposta.
5. **RF-5** — Una previsione **non fa scattare avvisi** (storia 0019-0020), **non viene pubblicata come fatto**,
   **non alimenta altre metriche** e **non entra nei totali** di un cruscotto. È un numero terminale: si legge,
   non si usa come materia prima.
6. **RF-6** — Quando i dati non bastano — meno di tre periodi conclusi, fonte richiesta silente, periodo in corso
   con meno di un terzo dei giorni trascorsi — la previsione **non si produce**: compare «non prevedibile» con il
   motivo, esattamente come il «non lo so» del copilota (storia 0024).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La serie storica su cui si costruisce la proiezione si legge con
  `tenant_id` dal gettone verificato; nessun dato di altri account concorre mai a una previsione — nessuna media
  di settore, nessun modello addestrato su più clienti (§2.3, punto 2 della descrizione).
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/insights/v1/previsioni` con metrica, orizzonte
  e metodo; la risposta porta valore centrale, intervallo, metodo, periodi usati e riferimento alla traccia;
  errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__previsioni.sql` sullo schema `app_insights`: tabella
  `previsione` **separata** da `valore_metrica`, con `tenant_id`, metrica e versione, periodo proiettato, metodo,
  valore centrale, minimo, massimo, numero di periodi usati, momento del calcolo; chiave primaria UUID versione 7,
  colonne di controllo, cancellazione logica. La separazione delle due tabelle è il presidio strutturale: una
  interrogazione sui valori rilevati **non può** restituire una previsione per distrazione.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro e grafico con tratto pieno per il rilevato e tratteggio più banda
  per il previsto; etichetta testuale «stima» accanto alla cifra; solo token del sistema di design; la
  distinzione **non si affida al colore** e supera il controllo automatico di accessibilità; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe — nomi dei metodi, parola «stima», motivi del «non
  prevedibile» — esistono in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** La previsione è un calcolo deterministico e **non consuma** la metrica
  `questions`; la consuma solo la sua spiegazione a parole chiesta al copilota. Rispetta la classe di
  riservatezza della metrica proiettata.
- **RT-7 — Esposizione conversazionale (§12).** La previsione **non ha uno strumento proprio**: `interroga_metrica`
  (storia 0031) restituisce previsioni solo se la richiesta le chiede esplicitamente, e in tal caso il risultato
  porta `tipo_valore = previsto`, metodo e intervallo. Un assistente non deve poter ottenere una stima credendo
  di aver ottenuto una misura.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: la previsione è un numero aggregato. La tabella
  `previsione` entra comunque in `purgeData` per account (è dato dell'account) e porta `created_by`.
- **RT-14 — Registrazione eventi (§14).** «Previsione calcolata», «previsione non producibile» con `tenant_id`,
  `app_id`, `user_id`, metrica, metodo e motivo; senza valori.

## 4. Criteri di accettazione

**CA-1 — La stima si vede come stima**
- **Dato** «fatturato emesso» con dodici mesi conclusi e il mese in corso al giorno 12
- **Quando** l'utente chiede la proiezione a fine mese
- **Allora** vede un valore centrale con intervallo, l'etichetta «stima», il metodo «proporzione sul periodo in
  corso, 12 giorni su 22 lavorativi» e, nel grafico, un tratto **tratteggiato** con la banda dell'intervallo

**CA-2 — Nei dati non si confonde**
- **Dato** una tavola con storico e proiezione
- **Quando** viene esportata (storia 0027)
- **Allora** le righe di proiezione hanno `tipo_valore = previsto` con metodo e intervallo compilati, e una
  interrogazione dei soli valori rilevati **non le restituisce**

**CA-3 — La previsione non suona**
- **Dato** un avviso «crediti scaduti sopra 10.000 €» e una proiezione che supera quella soglia
- **Quando** la valutazione periodica degli avvisi gira
- **Allora** l'avviso **non scatta**: gli avvisi si valutano solo su valori rilevati

**CA-4 — Dati insufficienti**
- **Dato** una metrica con due soli periodi conclusi
- **Quando** si chiede la media mobile
- **Allora** compare «non prevedibile — servono almeno tre periodi conclusi, ce ne sono due» e nessun numero

**CA-5 — Il copilota lo dice per primo**
- **Dato** la domanda «quanto chiuderò questo mese?»
- **Quando** il copilota risponde
- **Allora** la **prima frase** contiene la parola «stima» e il metodo, e la risposta porta l'intervallo insieme
  al valore centrale

**CA-6 — Isolamento fra account**
- **Dato** due account con storici diversi sulla stessa metrica
- **Quando** entrambi chiedono la stessa proiezione
- **Allora** ciascuno riceve un valore costruito **solo** sul proprio storico, e una prova lo verifica con serie
  volutamente diverse

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sui due metodi (proporzione con giorni lavorativi, media mobile), sull'intervallo e su
      tutte le condizioni di «non prevedibile»; prova che una interrogazione dei valori rilevati non restituisce
      previsioni;
- [ ] prova di **isolamento fra account** sulla serie storica usata per la proiezione;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-INSIGHTS]` include «chiedi la proiezione e verifica
      che sia marcata come stima nel disegno e nell'esportazione»; registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** verificato: nessun dato personale nuovo; `previsione` presente in `purgeData`;
- [ ] **registro delle decisioni** compilato, con i due metodi ammessi, la separazione della tabella
      `previsione` da `valore_metrica` e il divieto di far scattare avvisi su una stima;
- [ ] contratto degli **strumenti conversazionali**: `interroga_metrica` marca il risultato `previsto` e non
      restituisce mai una stima al posto di una misura (storia 0031);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0015` | i periodi e il calendario definiscono l'orizzonte proiettabile |
| storia `0016` | anche una previsione porta la sua scheda: qui la scheda dice il **metodo**, non le fonti del numero |
| storia `0027` | la colonna `tipo_valore` dell'esportazione nasce insieme a questa storia |

## 7. Fuori ambito

- metodi statistici avanzati (stagionalità, regressione, modelli appresi): due metodi elementari e spiegabili
  valgono più di uno bravo e opaco, in un'app la cui promessa è la verificabilità;
- la previsione di liquidità a partire da scadenze note: è materia dell'app 03 CashGrove, che ha i dati di
  dettaglio; qui arriverebbe come misura già calcolata;
- il confronto della proiezione con un obiettivo: è l'app 54 BudgetGrove (§11, punto 8 della descrizione);
- l'uso della previsione dentro una metrica derivata (storia 0013): vietato da RF-5.

## 8. Punti aperti

- **Mostrare una previsione è una scelta di prodotto, non solo tecnica.** Un titolare che legge «chiuderai a
  38.000 €» decide su quel numero anche se scritto «stima». La domanda onesta è se il valore aggiunto superi il
  rischio di fiducia mal riposta. Raccomandazione: **sì, con i presidi di RF-4 e con la proiezione spenta in modo
  predefinito** sui cruscotti, accesa da chi la vuole. Chiude: **sviluppatore**.
- **La correzione per i giorni lavorativi dipende dal calendario dell'account** (settimana corta, chiusure
  stagionali) e la piattaforma oggi non ha un calendario delle chiusure. Proposta: giorni lavorativi da
  lunedì a venerdì, dichiarato nel metodo. Chiude: **sviluppatore**.
- **L'intervallo di quale ampiezza?** Con due metodi elementari non c'è una teoria che lo fissi: si può usare la
  dispersione dei periodi passati. Va scelto un criterio **scritto e sempre lo stesso**, mai un valore fissato a
  occhio. Chiude: **sviluppatore**.
