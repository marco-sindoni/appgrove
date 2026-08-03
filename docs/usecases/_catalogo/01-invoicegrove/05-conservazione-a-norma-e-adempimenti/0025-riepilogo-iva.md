# 0025 — Riepilogo dell'imposta

**Applicazione**: 01 — InvoiceGrove (`einvoicing`) · **Epica**: 05 — Conservazione a norma e adempimenti
**Storia**: `0025` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0019`, `0021`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che deve mandare i numeri al commercialista ogni trimestre
> voglio un riepilogo per periodo con imponibile e imposta per aliquota, separato per paese
> così da non dover sommare a mano le fatture e da non litigare su un totale che non torna.

**Contesto.** È la voce «reportistica IVA» della scheda di catalogo, ed è deliberatamente la storia più modesta
dell'epica. InvoiceGrove **non fa contabilità** e non calcola le imposte dovute (descrizione dell'applicazione §1):
produce i numeri che ha, cioè quelli dei documenti che sono passati da lei, e li presenta in una forma che chi fa
la dichiarazione possa usare. Il confine è netto e va tenuto: appena si comincia a interpretare, si diventa un
software fiscale, che è un altro prodotto con altri obblighi.

## 2. Requisiti funzionali

1. **RF-1** — Per un periodo e un soggetto emittente, il riepilogo mostra imponibile e imposta **per aliquota**,
   separando documenti attivi e passivi.
2. **RF-2** — Il riepilogo è separato per **giurisdizione**: sommare un paese con un altro non ha senso e sarebbe
   un errore da cui il cliente non si accorgerebbe.
3. **RF-3** — Il riepilogo conta **solo i documenti in stato definitivo**: le bozze e gli scartati non entrano, e
   il riepilogo dice quanti ne ha esclusi e perché.
4. **RF-4** — Il riepilogo è scaricabile in un formato tabellare leggibile da un foglio di calcolo.
5. **RF-5** — Ogni riepilogo porta la data e l'ora del calcolo e l'avvertenza che è un riepilogo dei documenti
   transitati da InvoiceGrove, **non** una dichiarazione né un calcolo dell'imposta dovuta.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il calcolo aggrega **solo** i documenti dell'account, filtrati per
  `tenant_id` preso dal token verificato. È una funzione di aggregazione: una perdita di isolamento qui
  produrrebbe numeri di un altro cliente dentro i propri, ed è il caso più difficile da accorgersene. Prova di
  isolamento obbligatoria e rafforzata.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta
  `GET /api/einvoicing/v1/vat-summary?periodo=&soggetto=&giurisdizione=`; errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: il riepilogo si calcola dai documenti. Se le prestazioni lo
  richiedessero, la memorizzazione va introdotta con la sua invalidazione, non improvvisata.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Panoramica → Riepilogo imposta» con selettore di periodo, tabella
  per aliquota e pulsante di scarico. Solo token del sistema di design; tema chiaro e scuro; i numeri usano il
  carattere a spaziatura fissa del sistema di design.
- **RT-5 — Cinque lingue (§4).** Etichette, nomi dei periodi e **l'avvertenza** dallo spazio-nomi `einvoicing`,
  presenti in `en, it, fr, es, de`. L'avvertenza è la riga che separa un riepilogo da una consulenza: va tradotta
  con precisione.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: leggere i propri numeri non costa nulla.
  Disponibile anche con abbonamento in `past_due`.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `get_vat_report(periodo, giurisdizione) → imponibile e imposta per aliquota`, marcato **lettura**, nessuna
  conferma. È uno strumento che ha senso da una chat: «quanto ho fatturato in Belgio nel secondo trimestre?».
  Contratto dentro il servizio; server conversazionale non implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Il riepilogo è **aggregato** e non contiene dati personali: nessuna voce nuova
  nel manifesto. È una proprietà da preservare deliberatamente — basterebbe aggiungere una colonna «per
  controparte» per trasformarlo in un elenco di dati personali, e quella sarebbe una storia diversa con una
  classificazione diversa.
- **RT-9 — Registrazione eventi (§14).** L'evento `riepilogo calcolato` è registrato con `tenant_id`, `app_id`,
  `user_id`, identificativo di correlazione, periodo e giurisdizione — senza importi.

## 4. Criteri di accettazione

**CA-1 — Riepilogo per aliquota**
- **Dato** un trimestre con documenti a due aliquote diverse
- **Quando** si chiede il riepilogo
- **Allora** compaiono due righe con imponibile e imposta corretti, e il totale corrisponde alla somma

**CA-2 — Separazione per giurisdizione**
- **Dato** un account con documenti italiani e belgi nello stesso periodo
- **Quando** si chiede il riepilogo
- **Allora** i due paesi sono separati e **non** esiste un totale che li somma

**CA-3 — Esclusione dei non definitivi**
- **Dato** un periodo con dieci documenti definitivi, due bozze e uno scartato
- **Quando** si chiede il riepilogo
- **Allora** entrano i dieci definitivi e il riepilogo dichiara che tre sono stati esclusi, con il motivo

**CA-4 — Attivi e passivi separati**
- **Dato** un periodo con documenti emessi e ricevuti
- **Quando** si chiede il riepilogo
- **Allora** le due direzioni sono su sezioni distinte

**CA-5 — Isolamento fra account**
- **Dato** due account con documenti nello stesso periodo
- **Quando** un utente dell'uno chiede il riepilogo
- **Allora** i numeri sono solo i propri, e nessun documento dell'altro entra nell'aggregato

**CA-6 — Avvertenza sempre presente**
- **Dato** un riepilogo, a schermo o scaricato
- **Quando** lo si legge
- **Allora** porta la data del calcolo e l'avvertenza che non è una dichiarazione né un calcolo dell'imposta
  dovuta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend);
- [ ] prove di **unità** sull'aggregazione per aliquota, sull'esclusione dei non definitivi e sull'arrotondamento;
      **integrazione** sulla rotta;
- [ ] prova di **isolamento fra account** rafforzata sull'aggregato;
- [ ] **prova end-to-end**: *rimando* — non è nel percorso principale; il registro di copertura riporta la voce
      `da-coprire` con motivo «funzione di sola lettura, coperta da prove di integrazione» e storia proprietaria
      `0030`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con cura sull'avvertenza;
- [ ] **manifesto dei dati**: nessuna voce nuova, e la proprietà «aggregato senza dati personali» è verificata;
- [ ] **registro delle decisioni** compilato, con il confine «riepilogo, non dichiarazione» dichiarato;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `get_vat_report`.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0019` | Servono gli stati definitivi per sapere cosa contare |
| `0021` | Servono i documenti passivi per la sezione dei ricevuti |

## 7. Fuori ambito

- Il **calcolo dell'imposta dovuta**, le compensazioni, i regimi speciali, il meccanismo dell'inversione contabile
  applicato al calcolo: non è contabilità, e diventarlo è una decisione di prodotto diversa.
- La **comunicazione fiscale periodica** verso l'amministrazione: in Francia farebbe parte del modello a cinque
  angoli, che non è implementato; altrove è un adempimento del commercialista.
- Il riepilogo **per controparte**: escluso deliberatamente, perché trasformerebbe un aggregato senza dati
  personali in un elenco di dati personali.

## 8. Punti aperti

- **Se il riepilogo debba includere i documenti passivi conservati ma non approvati.** Dipende da cosa il
  commercialista si aspetta: è una domanda da fare a un professionista, non da risolvere qui.
- **Fino a che punto il riepilogo possa somigliare a un registro fiscale** senza diventarne uno. Il confine è
  sottile e ha conseguenze: la proposta è restare su un aggregato per aliquota e periodo, e non introdurre mai una
  numerazione progressiva propria.
