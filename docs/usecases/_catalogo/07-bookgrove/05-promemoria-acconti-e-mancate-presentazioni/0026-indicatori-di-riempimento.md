# 0026 — Indicatori di riempimento

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 05 — Promemoria, acconti e mancate presentazioni
**Storia**: `0026` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0015`, `0020`, `0022`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio vedere in una schermata quanto è piena l'agenda, quante persone non si presentano e quanto recupero con
> la lista d'attesa
> così da sapere se questo programma mi sta facendo guadagnare qualcosa oppure no.

**Contesto.** È la storia che rende visibile il ritorno dell'investimento, e serve a due cose: al cliente per
decidere se rinnovare, e a noi per sapere se la promessa dell'app è vera. Deve restare piccola: quattro numeri
letti bene valgono più di un cruscotto che nessuno guarda. I numeri nascono tutti da dati già esistenti — stati
della prenotazione, esiti dei messaggi, offerte della lista d'attesa — quindi la storia non introduce tabelle.

## 2. Requisiti funzionali

1. **RF-1** — Quattro indicatori su un periodo scelto: **tasso di riempimento** (tempo prenotato su tempo
   disponibile), **tasso di mancata presentazione**, **disdette entro e fuori dalla finestra**, **spazi
   recuperati** dalla lista d'attesa.
2. **RF-2** — Gli indicatori si scompongono per risorsa e per servizio, perché il valore sta nel capire **dove**
   si perde.
3. **RF-3** — Ogni numero dichiara come è calcolato, in una riga: un indicatore che non si sa da dove viene non
   viene creduto e quindi non viene usato.
4. **RF-4** — Gli appuntamenti mai chiusi (storia `0015`) sono contati a parte e **non** falsano né il tasso di
   presentazione né quello di mancata presentazione.
5. **RF-5** — Il periodo predefinito è l'ultimo mese chiuso, con confronto sul periodo precedente.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni aggregazione filtra per `tenant_id` preso dal token verificato;
  nessun confronto con altri account, nemmeno anonimo.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/prenotazioni/v1/indicatori` con periodo,
  risorsa e servizio facoltativi; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** **Nessuna tabella nuova**: gli indicatori si calcolano dai dati esistenti. Se il
  calcolo dovesse diventare pesante, la via è un indice, non una tabella di riepilogo che può divergere.
- **RT-4 — Modulo frontend (§3, §5).** Una schermata con quattro numeri grandi e la scomposizione sotto; solo
  token del sistema di design; tema chiaro e scuro; leggibile da telefono.
- **RT-5 — Cinque lingue (§4).** Etichette, spiegazioni del calcolo e formati numerici in `en, it, fr, es, de`.
- **RT-6 — Dati personali (§10).** Nessun dato personale: gli indicatori sono conteggi. Nessuna schermata deve
  scendere fino al singolo cliente da qui — chi vuole il dettaglio passa dall'agenda o dalla scheda cliente, dove
  l'accesso è quello che è.
- **RT-7 — Registrazione eventi (§14).** Nessun evento applicativo nuovo.
- **RT-8 — Esposizione conversazionale (§12).** Base dello strumento di lettura
  `riepilogo_mancate_presentazioni`, dichiarato nella storia `0031`.

## 4. Criteri di accettazione

**CA-1 — I quattro numeri**
- **Dato** un mese con dieci appuntamenti, uno non presentato e due disdetti
- **Quando** si aprono gli indicatori
- **Allora** i quattro numeri sono coerenti con i dati e ciascuno dichiara come è calcolato

**CA-2 — Scomposizione**
- **Dato** due risorse con comportamenti diversi · **Quando** si scompone per risorsa · **Allora** la differenza
  si vede

**CA-3 — Appuntamenti mai chiusi**
- **Dato** tre appuntamenti passati mai chiusi · **Quando** si guardano gli indicatori · **Allora** sono contati a
  parte e non spostano né il tasso di presentazione né quello di mancata presentazione

**CA-4 — Spazi recuperati**
- **Dato** una disdetta seguita da un'offerta accettata dalla lista d'attesa · **Quando** si guardano gli
  indicatori · **Allora** lo spazio risulta recuperato

**CA-5 — Isolamento fra account**
- **Dato** due account · **Quando** uno chiede gli indicatori · **Allora** i numeri riguardano solo i suoi dati

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sui calcoli, compresi i casi limite (nessun appuntamento, tempo disponibile nullo) e di
      **integrazione** sulla rotta;
- [ ] prova di **isolamento fra account** sulle aggregazioni;
- [ ] **prova end-to-end**: *rimando* — gli indicatori sono lettura pura; motivo e storia proprietaria dichiarati
      in [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue, compresi i formati numerici;
- [ ] **manifesto dei dati**: nessuna voce nuova, con la ragione scritta;
- [ ] **registro delle decisioni** compilato: definizione esatta dei quattro indicatori e trattamento degli
      appuntamenti mai chiusi;
- [ ] contratto degli **strumenti conversazionali** predisposto per `riepilogo_mancate_presentazioni`;
- [ ] avvio locale invariato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0015` | gli stati finali sono la materia prima |
| storia `0020` | gli spazi recuperati vengono dalla lista d'attesa |
| storia `0022` | per collegare i promemoria all'effetto sulle mancate presentazioni |

## 7. Fuori ambito

- il confronto con altre attività dello stesso settore: richiederebbe di usare i dati dei clienti per una
  finalità secondaria, che la piattaforma esclude;
- previsioni e suggerimenti automatici: non richiesti, e senza abbastanza dati sarebbero indovinelli.

## 8. Punti aperti

**Il tempo disponibile come denominatore.** Il tasso di riempimento cambia molto a seconda di cosa si mette al
denominatore: le ore di apertura, le ore delle risorse, o le ore al netto di ferie e chiusure. La proposta è la
terza, perché è quella che il titolare riconosce come «le ore che avrei potuto vendere». Da confermare: è una
definizione, e le definizioni degli indicatori vanno decise una volta sola.
