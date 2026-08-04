# 0021 — Report incassato e da incassare

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 04 — Incassi e solleciti
**Storia**: `0021` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`, `0018`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che a fine mese vuole sapere come sta andando
> voglio tre numeri: quanto ho fatturato, quanto ho incassato, quanto mi devono
> così da capire in dieci secondi se il mese è stato buono, senza aprire un foglio di calcolo e senza chiamare il
> commercialista.

**Contesto.** Sono i numeri che la scheda di catalogo chiama «report incassato / da incassare» e che la Panoramica
del modulo (storia `0003`) mostra ancora grezzi. Vanno fatti dopo incassi e scadenzario, che ne sono la materia
prima, e chiudono l'epica: da qui in poi l'app risponde da sola alle domande per cui il titolare la apre.

## 2. Requisiti funzionali

1. **RF-1** — Esiste un riepilogo per periodo con: fatturato (documenti emessi al netto delle note di credito),
   incassato, residuo da incassare e scaduto.
2. **RF-2** — Il periodo si sceglie fra mese, trimestre e anno, e si confronta con il periodo precedente.
3. **RF-3** — Il riepilogo si può leggere anche **per cliente**, ordinato per residuo.
4. **RF-4** — I numeri del riepilogo coincidono sempre con quelli dello scadenzario e delle schede dei documenti:
   una sola fonte di verità.
5. **RF-5** — Il riepilogo si può scaricare in formato tabellare.
6. **RF-6** — Il riepilogo distingue i documenti in valuta estera riportandoli alla valuta di conto (storia `0022`).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni aggregazione filtra per `tenant_id` preso dal token verificato: è il
  punto in cui una interrogazione aggregata scritta male somma i dati di tutti. Prova di isolamento obbligatoria e
  mirata sulle somme.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/billing/v1/reports/revenue` con periodo e
  raggruppamento; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: si aggrega su `document`, `payment` e `due_date`. Se le
  interrogazioni risultassero lente, la soluzione è un indice, non una tabella di riepilogo scritta a parte: due
  fonti di verità sui soldi sono un difetto, non una ottimizzazione.
- **RT-4 — Modulo frontend (§3, §5).** La Panoramica mostra i tre numeri con il confronto sul periodo precedente;
  una sezione «Report» permette di scegliere periodo e raggruppamento. Solo token del sistema di design; tema chiaro
  e scuro; il controllo automatico di accessibilità passa anche sui riquadri numerici.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `billing` e sono presenti in
  `en, it, fr, es, de`; importi e date usano il formato della lingua scelta.
- **RT-6 — Varchi e quota (§6).** Nessun consumo di quota: è una lettura. L'accesso al report richiede ruolo
  `admin`: sono i numeri dell'azienda, non tutti in azienda devono vederli. **Va confermato**: è una scelta
  organizzativa, non tecnica.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `riepilogo_incassi(periodo) → fatturato, incassato, da incassare, scaduto`, marcato **lettura**. È la domanda che
  il titolare fa più spesso e il motivo per cui vorrà parlare con l'app invece di aprirla. Dipendenza dichiarata:
  UC 0061-0063.
- **RT-8 — Dati personali (§10).** Il riepilogo **per cliente** espone nomi accanto a importi: nessun campo nuovo,
  ma il risultato dello strumento conversazionale va **minimizzato** e il raggruppamento per cliente non deve
  uscire dalla chat se non richiesto esplicitamente.
- **RT-9 — Registrazione eventi (§14).** L'evento `report scaricato` è registrato con `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — I tre numeri**
- **Dato** un mese con 10.000 € di fatture emesse, 6.000 € incassati e 1.000 € di note di credito
- **Quando** si apre il riepilogo del mese
- **Allora** il fatturato è 9.000 €, l'incassato 6.000 €, il residuo 3.000 €

**CA-2 — Confronto con il periodo precedente**
- **Dato** un mese e il precedente con valori diversi
- **Quando** si apre il riepilogo · **Allora** compare la variazione rispetto al mese precedente

**CA-3 — Coerenza con lo scadenzario**
- **Dato** lo stesso periodo
- **Quando** si sommano i residui dello scadenzario
- **Allora** il totale coincide al centesimo con il «da incassare» del riepilogo

**CA-4 — Raggruppamento per cliente**
- **Dato** tre clienti con residui diversi · **Quando** si chiede il riepilogo per cliente
- **Allora** l'elenco è ordinato per residuo decrescente

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con documenti
- **Quando** un utente di `A` chiede il riepilogo, anche forzando l'identificativo di `B`
- **Allora** le somme comprendono solo i documenti di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sull'aggregazione (note di credito, incassi parziali, valuta estera) e di **integrazione**
      sulla rotta, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** mirata sulle somme aggregate;
- [ ] **prova end-to-end**: *coprire ora* — passo finale del percorso `[J-BILLING]`: dopo l'incasso il riepilogo
      mostra i valori attesi; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, dichiarato;
- [ ] **registro delle decisioni** compilato, con annotata la scelta di aggregare invece di mantenere riepiloghi;
- [ ] contratto degli **strumenti conversazionali** dichiarato per `riepilogo_incassi`;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0017` | L'incassato viene dagli incassi |
| storia `0018` | Il residuo e lo scaduto vengono dalle scadenze |

## 7. Fuori ambito

- la previsione di cassa: è di CashGrove (3);
- l'analisi della redditività per prodotto o per cliente: rimandata, non è un bisogno rilevato nel segmento micro;
- i registri e la liquidazione dell'imposta: sono del commercialista, non di BillGrove.

## 8. Punti aperti

Se il report debba essere riservato al ruolo `admin` o visibile anche a `member` è una decisione organizzativa che
spetta allo sviluppatore: la proposta qui è `admin`, perché sono i numeri dell'azienda, ma in una micro-impresa di
due persone la distinzione può risultare artificiosa.
