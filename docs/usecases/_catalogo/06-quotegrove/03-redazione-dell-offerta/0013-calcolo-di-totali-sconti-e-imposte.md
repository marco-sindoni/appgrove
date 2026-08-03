# 0013 — Calcolo di totali, sconti e imposte

**Applicazione**: 06 — QuoteGrove (`preventivi`) · **Epica**: 03 — Redazione dell'offerta
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0011`, `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che firma l'offerta
> voglio che il numero in fondo al preventivo sia sempre giusto, comunque siano combinati sconti e imposte
> così da poterlo mandare senza ricontrollarlo con la calcolatrice.

**Contesto.** Sconti di riga, sconto di documento, aliquote diverse, esenzioni, arrotondamenti: ciascuna cosa da
sola è semplice, insieme sono il punto in cui i fogli di calcolo delle micro-imprese sbagliano. Questa storia
isola il **motore di calcolo** in un pezzo di codice provato da solo, perché è la logica che tutto il resto
dell'applicazione — documento, invio, evento verso la fatturazione — dà per corretta.

## 2. Requisiti funzionali

1. **RF-1** — L'ordine del calcolo è dichiarato e non ambiguo: prezzo unitario × quantità → sconto di riga →
   imponibile di riga → somma per aliquota → sconto di documento ripartito proporzionalmente sulle aliquote →
   imposta per aliquota → totale.
2. **RF-2** — Lo sconto di documento si ripartisce **proporzionalmente** fra le aliquote, così che il riepilogo
   resti coerente; il criterio è mostrato all'utente, non nascosto.
3. **RF-3** — Il totale mostrato è sempre uguale alla somma dei riepiloghi per aliquota, al centesimo.
4. **RF-4** — Il calcolo è **lo stesso** per l'anteprima nell'interfaccia, per il documento generato e per
   l'evento verso le app a valle: una sola implementazione, non tre.
5. **RF-5** — Il preventivo mostra anche il **margine** rispetto al listino, per chi lo prepara e non per il
   cliente: non compare mai sul documento inviato.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il calcolo opera su dati già filtrati per `tenant_id`; nessuna
  interrogazione del motore può leggere dati fuori dall'account.
- **RT-2 — Interfaccia di programmazione (§2).** `POST /api/preventivi/v1/preventivi/{id}/calcolo` restituisce il
  riepilogo senza salvarlo; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** I totali si **memorizzano** sul preventivo al momento del congelamento della
  versione (storia `0015`) e non si ricalcolano mai su un documento già inviato: il cliente ha in mano quei numeri.
- **RT-4 — Modulo frontend (§3, §5).** Il riepilogo si aggiorna mentre si scrive, con la stessa logica del
  servizio richiamata dal client generato: **nessun calcolo duplicato in JavaScript**.
- **RT-5 — Cinque lingue (§4).** Le etichette del riepilogo in `en, it, fr, es, de`; la formattazione dei numeri
  segue la lingua.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-7 — Registrazione eventi (§14).** Nessun evento nuovo: il calcolo non è un fatto, è una funzione.

## 4. Criteri di accettazione

**CA-1 — Sconto di riga e di documento insieme**
- **Dato** tre righe con aliquote diverse, sconti di riga misti e uno sconto di documento del 5 %
- **Quando** si chiede il calcolo
- **Allora** ogni imponibile per aliquota è ridotto in proporzione e il totale è la somma esatta dei riepiloghi

**CA-2 — Nessuna differenza di un centesimo**
- **Dato** importi scelti apposta per generare terzi decimali · **Quando** si somma · **Allora** totale e somma
  dei riepiloghi coincidono al centesimo

**CA-3 — Il documento inviato non si ricalcola**
- **Dato** un preventivo inviato e un listino modificato dopo l'invio · **Quando** si riapre il documento
- **Allora** i numeri sono quelli congelati all'invio, non quelli nuovi

**CA-4 — Il margine non esce**
- **Dato** un preventivo con margine calcolato · **Quando** si genera il documento per il cliente · **Allora** il
  margine non compare da nessuna parte

**CA-5 — Un solo motore**
- **Dato** lo stesso preventivo · **Quando** si confrontano il riepilogo dell'interfaccia, quello del documento e
  quello dell'evento · **Allora** sono identici cifra per cifra

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh`;
- [ ] prove di **unità** estese sul motore di calcolo, con casi limite di arrotondamento, e di **integrazione**
      sulla risorsa;
- [ ] prova di **isolamento fra account** sulla risorsa;
- [ ] **prova end-to-end**: rimando alla storia `0029`, che verifica il totale mostrato al cliente;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato: **l'ordine del calcolo e il criterio di ripartizione dello sconto di
      documento vanno scritti per esteso** — è la decisione che fra due anni nessuno saprà più ricostruire;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0010` | gli sconti |
| storia `0011` | aliquote, esenzioni e valuta |
| storia `0012` | le righe su cui calcolare |

## 7. Fuori ambito

- le regole fiscali per giurisdizione: sono della fatturazione (catalogo 02);
- il calcolo dei costi di cantiere e del ricarico per voce: è di BuildGrove (catalogo 25).

## 8. Punti aperti

Nessuno.
