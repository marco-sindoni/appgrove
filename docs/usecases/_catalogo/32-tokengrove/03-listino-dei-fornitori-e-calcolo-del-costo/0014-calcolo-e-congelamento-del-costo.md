# 0014 — Calcolo e congelamento del costo

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 03 — Listino dei fornitori e calcolo del costo
**Storia**: `0014` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha chiuso il mese e mandato le note di addebito ai clienti
> voglio che il totale di giugno resti quello di giugno, per sempre
> così da non dover rispiegare a un cliente perché il numero che gli ho mandato oggi non è più lo stesso.

**Contesto.** È la regola dura del modello di dominio (§4 del documento capofila) e la contropartita necessaria di
un catalogo dei prezzi che si aggiorna in continuazione: se il costo si ricalcolasse a ogni lettura, il totale del
mese scorso cambierebbe ogni volta che un fornitore cambia il proprio listino. Sarebbe corretto in un solo senso —
i prezzi di oggi — e sbagliato in tutti gli altri: contabilmente, contrattualmente e nella fiducia di chi legge.
La scelta è quindi il congelamento: **si calcola una volta, all'ingresso, e non si tocca più**.

## 2. Requisiti funzionali

1. **RF-1** — Al momento in cui una misura entra, il servizio calcola il suo costo usando il prezzo valido
   **all'istante della chiamata**, non a quello dell'ingresso: una misura arrivata in ritardo di due giorni usa il
   prezzo di due giorni fa.
2. **RF-2** — Il costo calcolato e il **numero di versione del catalogo** usato vengono scritti sulla riga della
   misura e non vengono più modificati da nessuna lettura, aggregazione o pubblicazione successiva.
3. **RF-3** — Il calcolo tiene conto separatamente dei quattro conteggi (ingresso, uscita, ingresso servito da
   cache, scrittura in cache) e dell'eventuale sconto per l'elaborazione differita, perché ignorarli produce
   errori del 50% e oltre su chi usa la cache.
4. **RF-4** — Se la misura porta già un importo dichiarato dal fornitore (caso del rendiconto), quello **prevale**
   sul nostro calcolo, e la riga lo dichiara: la fonte del costo è sempre visibile.
5. **RF-5** — Ogni riga di misura sa dire, se interrogata, **come** è stato ottenuto il suo costo: quale prezzo per
   unità, quale versione di catalogo, quale conteggio ha pesato di più. È la scheda che serve quando un cliente
   chiede «perché questa chiamata mi è costata così tanto».
6. **RF-6** — I totali per periodo sono somme dei costi congelati: nessuna aggregazione ricalcola nulla.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il calcolo avviene nel contesto della misura e quindi del suo
  `tenant_id`; le letture dei totali filtrano per `tenant_id` preso dal gettone verificato.
- **RT-2 — Persistenza (§8).** Le colonne `costo_congelato`, `versione_listino` e `origine_del_costo` della
  tabella `misura` (introdotte nella storia `0002`) diventano obbligatorie e non nulle per ogni riga registrata.
  Nessuna interrogazione di lettura scrive su quelle colonne.
- **RT-3 — Interfaccia di programmazione (§2).** Rotta `GET /api/spesa_modelli/v1/misure/{id}/scheda-costo` che
  restituisce la scomposizione del calcolo; errori in `problem+json`; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-4 — Modulo frontend (§3, §5).** La scheda del costo si apre dalla riga di una misura; mostra prezzi,
  conteggi e versione del catalogo. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** I nomi dei quattro conteggi sono i termini più difficili da tradurre bene di
  tutta l'app: vanno tradotti in `en, it, fr, es, de` con parole comprensibili a chi non è tecnico, non traslitterati.
- **RT-6 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: la scheda del costo è compresa nel
  risultato di `leggi_spesa` quando si chiede il dettaglio di una singola chiamata (storia `0032`).
- **RT-7 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-8 — Registrazione eventi (§14).** Evento «misura senza prezzo noto» (che rimanda alla storia `0015`) con
  `tenant_id`, `app_id`, chiave del modello e identificativo di correlazione.

## 4. Criteri di accettazione

**CA-1 — Il costo si congela all'ingresso**
- **Dato** una misura registrata quando era valida la versione 7 del catalogo
- **Quando** si pubblica la versione 8 con un prezzo diverso per quel modello
- **Allora** il costo della misura e il totale del suo periodo restano invariati, e la riga continua a dichiarare
  la versione 7

**CA-2 — Prezzo valido all'istante della chiamata, non dell'ingresso**
- **Dato** una misura avvenuta il 30 luglio e arrivata il 2 agosto, e un prezzo cambiato il 1° agosto
- **Quando** viene registrata
- **Allora** il costo è calcolato con il prezzo di luglio

**CA-3 — I conteggi contano tutti**
- **Dato** una chiamata con 10.000 unità in ingresso di cui 9.000 servite da cache, e 500 in uscita
- **Quando** si calcola il costo
- **Allora** le 9.000 unità servite da cache sono valorizzate al proprio prezzo ridotto e non a quello pieno, e la
  scheda del costo mostra le tre voci separate

**CA-4 — L'importo del fornitore prevale**
- **Dato** una riga importata dal rendiconto che porta già l'importo dichiarato dal fornitore
- **Quando** viene registrata
- **Allora** il costo è quello dichiarato, e la riga indica come origine del costo il fornitore, non il nostro
  catalogo

**CA-5 — Isolamento fra account**
- **Dato** due account con misure sullo stesso modello e istante
- **Quando** si leggono i totali
- **Allora** ciascuno vede solo i propri, e un prezzo negoziato di uno non influenza il costo dell'altro

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo con tutte le combinazioni di conteggi e sui confini di validità del prezzo, e
      di **integrazione** sul congelamento con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sui totali;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «pubblico un listino
      nuovo e il totale del periodo precedente non cambia», e aggiornare il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con cura particolare per i nomi dei conteggi;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, in particolare sul congelamento e sul prezzo valido all'istante della
      chiamata;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0013` | Serve il catalogo dei prezzi con validità nel tempo |
| Storia `0010` | Il costo si calcola su misure già deduplicate, altrimenti si congela due volte lo stesso importo |
| Punto aperto P7 del documento capofila | La regola di conversione della valuta va decisa prima: il cambio si congela come il prezzo |

## 7. Fuori ambito

- il caso del modello senza prezzo noto: è la storia `0015`;
- i prezzi negoziati: sono la storia `0016`;
- il ricalcolo esplicito dello storico: è la storia `0017`, ed è l'unica via ammessa per cambiare un costo già
  congelato.

## 8. Punti aperti

- **Il cambio da dollari a euro** (punto P7 del documento capofila). Va congelato insieme al costo, con la stessa
  logica: il cambio del giorno della chiamata. Ma serve una fonte dei tassi, che è un fornitore esterno nuovo e
  quindi una decisione dello sviluppatore. Finché non è deciso, la proposta di ripiego è mostrare gli importi nella
  valuta del fornitore dichiarandolo, invece di convertire con un tasso arbitrario.
