# 0018 — Soglie e lista di riordino

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 04 — Prodotti, consumi e magazzino
**Storia**: `0018` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0017`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che il martedì fa l'ordine al rappresentante
> voglio aprire una lista di quello che sta finendo, con quanto ne consumo di solito
> così da smettere di ordinare a occhio e di scoprire il venerdì che manca l'ossidante a 20 volumi.

**Contesto.** È la chiusura naturale dell'epica: misurare il consumo (storia `0017`) serve a poco se poi il
riordino resta a memoria. Le fonti di settore descrivono soglie di riavviso e liste di riordino automatiche come
funzione di riferimento (§2.5 della descrizione). Qui la funzione si ferma deliberatamente a **una lista**: non
trasmette ordini a nessuno, perché trasmettere un ordine è un effetto verso l'esterno e un fornitore nuovo (§2.4
della descrizione, punto 4).

## 2. Requisiti funzionali

1. **RF-1** — Ogni prodotto può avere una **soglia di riavviso** per deposito; sotto quella soglia risulta «in
   esaurimento».
2. **RF-2** — La soglia si può **proporre dal consumo osservato**: il programma suggerisce un valore a partire da
   quanto se ne è consumato negli ultimi mesi, e chi decide è il titolare.
3. **RF-3** — La **lista di riordino** elenca i prodotti sotto soglia, con giacenza attuale, consumo medio per
   settimana, quantità suggerita e costo stimato dell'ordine.
4. **RF-4** — La lista si può esportare in un formato tabellare e stampare: il rappresentante non ha accesso al
   nostro programma, e il foglio è ancora lo strumento di scambio.
5. **RF-5** — La lista si può **congelare** in un documento datato («ordine del 12 marzo»), così da poterla
   confrontare con quello che poi è arrivato al momento del carico.
6. **RF-6** — Nessun ordine viene trasmesso a nessuno, e il programma lo dice apertamente.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Soglie, consumi medi e liste filtrano per `tenant_id` dal token
  verificato.
- **RT-2 — Interfaccia di programmazione (§2).** `PUT /api/<app>/v1/prodotti/{id}/soglia`,
  `GET /api/<app>/v1/riordino`, `POST /api/<app>/v1/riordino/documenti`; errori in `problem+json`; OpenAPI
  aggiornata.
- **RT-3 — Persistenza (§8).** La soglia è un campo sulla giacenza; il documento di riordino è una tabella con
  `tenant_id`, UUID versione 7, colonne di controllo e cancellazione logica. Il consumo medio si **calcola**, non
  si conserva.
- **RT-4 — Varchi e quota (§6, §7).** Funzione accesa dal piano; `402` a piano insufficiente.
- **RT-5 — Modulo frontend (§3, §5).** La lista è una tabella ordinata per urgenza, con l'esportazione e la stampa
  a portata di mano; il suggerimento della soglia si accetta o si scrive a mano. Solo token del sistema di design,
  tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Etichette, unità, testo che chiarisce che nessun ordine viene trasmesso, in
  `en, it, fr, es, de`.
- **RT-7 — Dati personali (§10).** **Nessun dato personale**: la lista riguarda prodotti. Dichiarato.
- **RT-8 — Esposizione conversazionale (§12).** Lo strumento `giacenza_prodotti(deposito?, sotto_soglia?)` della
  storia `0016` risponde già a «che cosa sta finendo»; questa storia **non aggiunge strumenti di scrittura**,
  perché congelare un documento di riordino è un atto che si guarda prima di fare.
- **RT-9 — Registrazione eventi (§14).** `soglia impostata`, `documento di riordino creato` con `tenant_id`,
  `app_id`, `user_id` e correlazione.

## 4. Criteri di accettazione

**CA-1 — Sotto soglia compare in lista**
- **Dato** una tinta con soglia 3 e giacenza 2
- **Quando** si apre la lista di riordino
- **Allora** la tinta c'è, con giacenza 2, il suo consumo medio e una quantità suggerita

**CA-2 — La soglia proposta**
- **Dato** un prodotto con tre mesi di consumi registrati
- **Quando** si chiede il suggerimento di soglia
- **Allora** il programma propone un valore coerente con quel consumo e lo si può cambiare prima di salvare

**CA-3 — La lista si congela**
- **Dato** la lista di riordino di oggi
- **Quando** si crea il documento
- **Allora** resta una fotografia datata che non cambia più, anche se domani le giacenze cambiano

**CA-4 — Niente ordini trasmessi**
- **Dato** un documento di riordino creato
- **Quando** si cerca un modo per inviarlo a un fornitore dal programma
- **Allora** non ce n'è nessuno, e il testo lo dice: si esporta e si manda come si vuole

**CA-5 — Isolamento fra account**
- **Dato** due account con lo stesso prodotto sotto soglia
- **Quando** un utente del primo apre la lista
- **Allora** vede solo la propria

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sul calcolo del consumo medio e della quantità suggerita, di **integrazione** sulla lista;
- [ ] prova di **isolamento fra account** su soglie, lista e documenti;
- [ ] **prova end-to-end**: *nessun impatto* — la lista è una lettura derivata; il percorso `[J-SALONGROVE]` copre
      già il movimento di magazzino che la alimenta (storia `0017`);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, e il fatto è dichiarato;
- [ ] **registro delle decisioni**: nessuna trasmissione di ordini e il perché, consumo medio calcolato e non
      conservato, documento di riordino come fotografia immutabile;
- [ ] avvio locale invariato.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0017` | senza consumi misurati non c'è consumo medio, e la soglia resta a occhio |

## 7. Fuori ambito

- la trasmissione dell'ordine al fornitore: effetto verso l'esterno e fornitore nuovo, **fuori** da questa
  stesura;
- i listini dei fornitori e i prezzi d'acquisto negoziati: non in questa stesura;
- la previsione della domanda oltre la media osservata: sarebbe un modello, e non ho elementi per giustificarlo su
  volumi così piccoli.

## 8. Punti aperti

Nessuno.
