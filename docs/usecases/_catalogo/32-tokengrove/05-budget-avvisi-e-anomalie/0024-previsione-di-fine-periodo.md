# 0024 — Previsione di fine periodo

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 05 — Budget, avvisi e anomalie
**Storia**: `0024` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0023`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare con un budget di 800 € al mese
> voglio sapere il 9 del mese che di questo passo chiuderò a 1.240 €
> così da avere ventun giorni per fare qualcosa, invece di scoprirlo il 30.

**Contesto.** È la storia che rende l'avviso **preventivo** invece che postumo, cioè che risponde al requisito di
sostanza dell'app: un avviso di superamento serve *prima* che il conto arrivi. Un avviso al 100% del budget è una
constatazione: quando arriva, i soldi sono già spesi. La previsione sposta l'avviso di due o tre settimane
all'indietro, e senza costare quasi nulla in complessità — purché il metodo sia **dichiarato**, perché una
previsione che non dice come è stata fatta è un numero che nessuno può contestare e quindi nessuno crede.

## 2. Requisiti funzionali

1. **RF-1** — Per ogni budget attivo si calcola la **spesa prevista a fine periodo**, aggiornata almeno una volta
   al giorno.
2. **RF-2** — Il metodo di previsione è **dichiarato accanto al numero**, in parole comprensibili: media del
   consumo giornaliero degli ultimi giorni completi, proiettata sui giorni rimanenti, con la distinzione fra giorni
   feriali e festivi se i dati la giustificano.
3. **RF-3** — La previsione dichiara anche la propria **affidabilità**: con meno di alcuni giorni di dati, o con
   una serie molto irregolare, si dice «troppo presto per una previsione» invece di dare un numero.
4. **RF-4** — Un avviso preventivo scatta quando la **previsione** supera il budget, anche se il consumato è
   ancora sotto: è l'avviso che arriva prima, e il suo testo dice esplicitamente che parla del futuro, non del
   presente.
5. **RF-5** — La previsione tiene conto dei dati mancanti: se una fonte è ferma (storia `0012`), la previsione lo
   dichiara e non la spaccia per un consumo calato.
6. **RF-6** — La scheda del budget mostra il percorso: consumato giorno per giorno, tetto, e la linea della
   proiezione fino a fine periodo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il calcolo avviene per budget e quindi per `tenant_id`; nessuna serie
  aggregata fra account.
- **RT-2 — Persistenza (§8).** Migrazione sullo schema `app_spesa_modelli`: tabella `previsione` con `tenant_id`,
  budget, giorno del calcolo, valore previsto, metodo, affidabilità, colonne di controllo. Conservare le previsioni
  passate permette di mostrare quanto la previsione era giusta, che è il modo con cui il cliente impara a fidarsi.
- **RT-3 — Interfaccia di programmazione (§2).** La previsione è compresa nella risposta della scheda del budget e
  in `GET /api/spesa_modelli/v1/budget/{id}/previsione`; errori in `problem+json`; definizione OpenAPI aggiornata.
- **RT-4 — Modulo frontend (§3, §5).** Il numero previsto sta accanto al consumato, con il metodo in una riga
  sotto; il grafico mostra la proiezione con un tratto diverso da quello dei dati reali, perché non si confonda una
  previsione con un fatto. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe presenti in `en, it, fr, es, de`, compresa la descrizione del
  metodo e la formula «troppo presto per una previsione».
- **RT-6 — Esposizione conversazionale (§12).** Lo strumento `stato_budget` (storia `0023`) restituisce la
  previsione insieme al metodo e all'affidabilità: un assistente che riportasse il numero senza il metodo darebbe
  una certezza che non abbiamo.
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-8 — Registrazione eventi (§14).** Evento «previsione oltre il budget» con `tenant_id`, `app_id`, budget e
  scostamento percentuale, con identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Previsione e metodo**
- **Dato** un budget mensile di 800 € e nove giorni di consumo pari a 40 € al giorno
- **Quando** si apre la scheda del budget
- **Allora** la previsione indica circa 1.240 € e accanto è scritto con quale metodo è stata ottenuta

**CA-2 — Avviso preventivo**
- **Dato** lo stesso caso, con consumato pari a 360 € su 800
- **Quando** la previsione viene aggiornata
- **Allora** scatta l'avviso preventivo, e il suo testo dice che riguarda la previsione di fine mese, non il
  consumo attuale

**CA-3 — Troppo presto**
- **Dato** un budget attivo da due giorni
- **Quando** si apre la scheda
- **Allora** al posto del numero compare «troppo presto per una previsione», con l'indicazione di quando sarà
  disponibile

**CA-4 — Dati mancanti**
- **Dato** una fonte ferma da due giorni
- **Quando** si guarda la previsione
- **Allora** è dichiarato che alcuni dati mancano e che la previsione è probabilmente sottostimata

**CA-5 — Isolamento fra account**
- **Dato** due account con budget dello stesso importo e consumi diversi
- **Quando** ciascuno legge la propria previsione
- **Allora** i numeri sono indipendenti

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo della previsione, sull'affidabilità e sul caso dei dati mancanti, e di
      **integrazione** sull'aggiornamento giornaliero;
- [ ] prova di **isolamento fra account** sulle previsioni;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «consumo costante, la
      previsione supera il budget, arriva l'avviso preventivo», e aggiornare il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, in particolare sul metodo scelto e sul perché è dichiarato accanto al
      numero;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0023` | La previsione esiste rispetto a un budget |
| Storia `0012` | Serve sapere se una fonte è ferma, altrimenti la previsione mente |

## 7. Fuori ambito

- metodi di previsione più elaborati (stagionalità, andamento settimanale complesso): rimandati. Un metodo semplice
  e dichiarato vale più di uno raffinato e opaco, e con periodi di un mese la differenza è piccola. Se emergesse
  come bisogno, sarebbe una storia dell'epica 06;
- il recapito dell'avviso: è la storia `0025`.

## 8. Punti aperti

Nessuno.
