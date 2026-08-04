# 0015 — Periodi, calendario e confronti

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 03 — Catalogo delle metriche e tracciabilità
**Storia**: `0015` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che guarda il fatturato di luglio
> voglio sapere se è andato meglio o peggio di giugno, e di luglio dell'anno scorso
> così da capire se è un problema o è solo agosto che si avvicina.

**Contesto.** Un numero da solo non dice niente: dice qualcosa solo confrontato. Questa storia costruisce il
motore di calcolo dei valori su un periodo, il calendario dell'account (in che giorno comincia la settimana, in
che mese comincia l'anno fiscale, quale fuso orario) e i due confronti che servono davvero: **periodo
precedente** e **stesso periodo dell'anno prima**. È anche la storia che decide una trappola classica: il
confronto fra un mese finito e un mese in corso.

## 2. Requisiti funzionali

1. **RF-1** — Un valore si calcola su un **periodo** scelto fra: giorno, settimana, mese, trimestre, anno,
   ultimi N giorni, intervallo esplicito.
2. **RF-2** — L'account configura il proprio calendario: primo giorno della settimana, primo mese dell'anno
   fiscale, fuso orario. Il calendario vale per tutti i calcoli dell'account e compare nella scheda del numero.
3. **RF-3** — Ogni periodo può essere confrontato con il **periodo precedente della stessa lunghezza** e con lo
   **stesso periodo dell'anno precedente**; il confronto restituisce differenza assoluta e percentuale.
4. **RF-4** — Un **periodo in corso** è marcato come tale, e il confronto con un periodo concluso è
   automaticamente **parziale**: si confrontano i primi N giorni con i primi N giorni, non un mese intero con
   mezzo mese. La scelta è dichiarata nella scheda del numero.
5. **RF-5** — I valori calcolati sono **memorizzati** con il riferimento alla versione della definizione, al
   periodo e al calendario usato, così che ricalcolare produca lo stesso numero (e se non lo producesse, si
   possa capire perché).
6. **RF-6** — Un periodo per cui **non esiste alcun fatto** non produce zero: produce «nessun dato per questo
   periodo». Zero e assenza sono cose diverse, e confonderle è il modo più comune di mentire con un cruscotto.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni calcolo legge i fatti con `WHERE tenant_id = :tid` e `:tid`
  preso dal gettone verificato; il calendario è per account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/insights/v1/valori` che accetta un piano
  (metrica, periodo, dimensioni, filtri, confronto) e restituisce valore, unità, completezza e riferimento alla
  traccia; corpo validato; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-3 — Persistenza (§8).** Tabella `valore_metrica` con `tenant_id`, chiave della metrica, versione,
  periodo, dimensioni, valore, conteggio dei fatti, completezza, momento del calcolo; chiave primaria UUID
  versione 7 e colonne di controllo.
- **RT-4 — Modulo frontend (§3, §5).** Il selettore di periodo e di confronto è un componente condiviso da
  cruscotto, metriche ed esportazione; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I nomi dei periodi, le formule di confronto e le date sono formattati secondo
  la lingua scelta, in `en, it, fr, es, de`.
- **RT-6 — Varchi e ruoli (§6).** Il calcolo rispetta la classe di riservatezza della metrica (storia 0014).
  **Non consuma quota**: il calcolo non è una domanda al copilota.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-11 — Prove (§11).** Prove di unità sul calendario e sul confronto parziale, con **tempo controllato**:
  nessuna attesa reale, nessuna dipendenza dalla data di esecuzione della prova.

## 4. Criteri di accettazione

**CA-1 — Confronto con il periodo precedente**
- **Dato** un fatturato di 42.300 € a luglio e di 38.900 € a giugno
- **Quando** si guarda luglio con confronto «periodo precedente»
- **Allora** si legge «42.300 €, +3.400 € (+8,7 %) rispetto a giugno»

**CA-2 — Periodo in corso confrontato correttamente**
- **Dato** che siamo l'11 del mese e il mese in corso ha 10 giorni di dati
- **Quando** si guarda il mese in corso con confronto «anno precedente»
- **Allora** il confronto è sui primi 10 giorni di entrambi i mesi, ed è dichiarato come parziale nella scheda
  del numero

**CA-3 — Assenza non è zero**
- **Dato** un periodo in cui nessun fatto è mai arrivato per quella misura
- **Quando** si calcola
- **Allora** il risultato è «nessun dato per questo periodo», e nessun grafico disegna una linea a zero

**CA-4 — Il calendario dell'account conta**
- **Dato** un account con anno fiscale che comincia a luglio
- **Quando** si guarda «anno in corso»
- **Allora** il periodo va da luglio a oggi, e la scheda del numero dichiara il calendario usato

**CA-5 — Il ricalcolo dà lo stesso numero**
- **Dato** un valore calcolato ieri
- **Quando** lo si ricalcola oggi, senza che siano arrivati fatti nuovi né cambiate definizioni
- **Allora** il numero è identico

**CA-6 — Isolamento fra account**
- **Dato** due account con fatti sulla stessa misura e periodo
- **Quando** un utente di `A` calcola forzando l'identificativo di `B`
- **Allora** ottiene il valore di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul calendario, sul confronto parziale e sulla distinzione fra zero e assenza; prove di
      **integrazione** sul calcolo con dati inventati di valore noto;
- [ ] prova di **isolamento fra account** sulla risorsa di calcolo;
- [ ] **prova end-to-end**: *rimando* alla storia 0034; voce `da-coprire` nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, comprese le formattazioni di data e numero;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con la regola del confronto parziale e la distinzione zero/assenza;
- [ ] contratto degli **strumenti conversazionali**: `interroga_metrica` accetta periodo e confronto
      (storia 0031);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | serve una definizione da calcolare |
| storia `0013` | le derivate si calcolano sugli stessi periodi |

## 7. Fuori ambito

- la scheda che spiega il valore: storia 0016 — qui il calcolo produce i dati che la scheda mostrerà;
- la scomposizione di uno scostamento per dimensione: storia 0029;
- la proiezione sul futuro: storia 0030.

## 8. Punti aperti

- **I valori si ricalcolano al volo o si materializzano?** Ai volumi di una micro-impresa il calcolo al volo è
  sostenibile; materializzare rende i cruscotti istantanei ma introduce il problema dell'invalidazione quando
  arriva un fatto vecchio. Raccomandazione: **calcolo al volo con memoria di breve durata**, rivedibile se i
  volumi lo richiedono. Chiude: **sviluppatore**.
- **Che cosa succede se arriva un fatto con periodo di competenza molto vecchio?** Cambia un numero che qualcuno
  ha già letto e magari esportato. Raccomandazione: si accetta (il dato corretto vince) e la scheda del numero
  mostra il momento dell'ultimo fatto ricevuto, così che una discrepanza sia spiegabile. Chiude:
  **sviluppatore**.
