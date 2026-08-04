# 0017 — Consumo di cabina per servizio

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 04 — Prodotti, consumi e magazzino
**Storia**: `0017` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0016`, `0010`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un salone
> voglio che il prodotto usato per un colore si scarichi da solo quando chiudo il conto, e poter correggere la
> dose quando l'operatrice ne ha usato di più
> così da sapere finalmente quanto mi costa un colore invece di scoprirlo a fine anno dalla fattura del fornitore.

**Contesto.** È la storia che rende utile tutta l'epica 04, ed è quella con il ritorno economico più diretto: le
fonti di settore riportano riduzioni del **25-40 % del prodotto di cabina** quando il consumo si misura per
servizio (§2.5 della descrizione — numeri di parte, usati come ordine di grandezza). Il meccanismo è semplice e
va tenuto semplice: ogni servizio dichiara le **dosi previste**, la chiusura del conto le scarica, e chi ha usato
di più lo dice prima di chiudere.

## 2. Requisiti funzionali

1. **RF-1** — A un servizio si associano le **dosi previste**: quali prodotti di cabina consuma e quanto, nella
   loro unità di misura.
2. **RF-2** — Alla chiusura del conto (storia `0019`) i prodotti previsti si scaricano dal deposito cabina, con un
   movimento per prodotto che cita il conto come origine.
3. **RF-3** — Prima di chiudere, l'operatore può **correggere le quantità** effettivamente usate, aggiungere un
   prodotto non previsto o toglierne uno: la dose prevista è un punto di partenza, non un obbligo.
4. **RF-4** — Lo **scostamento** fra previsto e usato resta registrato e visibile: è il numero da cui si capisce se
   le dosi previste sono tarate male o se in cabina si spreca.
5. **RF-5** — Il **costo del servizio** si calcola dal prodotto effettivamente scaricato e dal suo costo
   d'acquisto, e compare accanto al prezzo: è la misura del margine che il salone non ha mai avuto.
6. **RF-6** — Un servizio senza dosi previste non scarica niente e non blocca niente: la funzione deve poter
   restare inutilizzata.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Dosi, correzioni e scarichi filtrano per `tenant_id` dal token
  verificato.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|PUT /api/<app>/v1/servizi/{id}/dosi`, e la chiusura del
  conto accetta le quantità effettive; errori in `problem+json`; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Tabella `dose_prevista` con `tenant_id`, UUID versione 7, colonne di controllo e
  cancellazione logica; i consumi sono `movimento_magazzino` **immutabili** che citano il conto (storia `0016`).
- **RT-4 — Atomicità con la chiusura del conto.** Lo scarico è **parte della transazione** che chiude il conto: o
  il conto si chiude e il magazzino si muove, o non succede nulla. Un conto chiuso senza scarico è un margine
  falso che nessuno ritroverà.
- **RT-5 — Varchi e quota (§6, §7).** La funzione è accesa dal piano; a piano insufficiente risponde `402`. Non
  consuma la metrica `postazioni`.
- **RT-6 — Modulo frontend (§3, §5).** Nella chiusura del conto, i prodotti previsti compaiono già compilati con
  le quantità modificabili accanto; lo scostamento si mostra a colpo d'occhio. Il costo del servizio è visibile
  **solo a chi ha il ruolo per vederlo**: non è un numero da mostrare a tutti in sala. Solo token del sistema di
  design.
- **RT-7 — Cinque lingue (§4).** Etichette, unità di misura, avvisi di scostamento e di giacenza insufficiente in
  `en, it, fr, es, de`.
- **RT-8 — Dati personali (§10).** Nessuna voce nuova: le dosi riguardano servizi e prodotti. Il consumo eredita
  la voce `movimento_magazzino.operatore` della storia `0016`.
- **RT-9 — Esposizione conversazionale (§12).** Nessuno strumento nuovo: il consumo si conferma dentro la
  chiusura del conto, che è già uno strumento di scrittura con conferma obbligatoria (storia `0019`).
- **RT-10 — Registrazione eventi (§14).** `consumo registrato`, `scostamento rilevato` con `tenant_id`, `app_id`,
  `user_id` e correlazione, senza il nome del cliente.

## 4. Criteri di accettazione

**CA-1 — Lo scarico automatico**
- **Dato** un colore con dose prevista di 60 ml di tinta e 60 ml di ossidante
- **Quando** si chiude il conto senza toccare le quantità
- **Allora** la cabina ha 60 ml in meno di ciascuno, con due movimenti che citano quel conto

**CA-2 — La correzione prima di chiudere**
- **Dato** lo stesso colore
- **Quando** l'operatrice indica 90 ml di tinta perché i capelli erano lunghi
- **Allora** si scaricano 90 ml, lo scostamento risulta di +30 ml e resta registrato

**CA-3 — Il costo del servizio**
- **Dato** una tinta che costa 12 € ogni 100 ml
- **Quando** si scaricano 60 ml
- **Allora** il costo prodotto di quel servizio risulta 7,20 €, accanto al prezzo praticato

**CA-4 — Giacenza insufficiente non blocca il conto**
- **Dato** una cabina con 20 ml di tinta e un consumo di 60
- **Quando** si chiude il conto
- **Allora** il conto si chiude, la giacenza va a meno 40 e un avviso visibile dice che manca del carico — perché
  il servizio è stato erogato davvero, e negarlo non lo rende falso

**CA-5 — Tutto o niente**
- **Dato** una chiusura di conto che fallisce a metà per un errore
- **Quando** si guarda lo stato dopo
- **Allora** né il conto è chiuso né il magazzino si è mosso

**CA-6 — Isolamento fra account**
- **Dato** due account con lo stesso servizio
- **Quando** un utente del primo chiude un conto
- **Allora** solo la propria cabina si muove

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`; suite intera prima del commit);
- [ ] prove di **unità** sul calcolo del costo e dello scostamento, di **integrazione** sull'atomicità fra
      chiusura del conto e movimento di magazzino;
- [ ] prova di **isolamento fra account** su dosi e consumi;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-SALONGROVE]` (storia `0030`) verifica che la giacenza
      scenda dopo la chiusura del conto; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, e il fatto è dichiarato;
- [ ] **registro delle decisioni**: scarico atomico con la chiusura, giacenza negativa ammessa, costo visibile per
      ruolo, dose prevista come punto di partenza;
- [ ] avvio locale invariato; il salone di prova ha dosi previste su almeno due servizi.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0016` | i movimenti sono lo strumento con cui si scarica |
| storia `0010` | la scheda tecnica dichiara i prodotti usati e alimenta la correzione |
| storia `0019` | è la chiusura del conto a far scattare lo scarico: le due si implementano insieme o in questo ordine |

## 7. Fuori ambito

- la pesatura automatica del prodotto con una bilancia collegata: è ciò che fanno strumenti specializzati citati
  al §2.1 della descrizione, ed è un'integrazione hardware fuori perimetro;
- il costo della manodopera nel costo del servizio: qui si misura il **prodotto**. Il costo del lavoro sfiora la
  materia esclusa dell'app 10, e il margine di contribuzione più fine non è di questa stesura;
- le soglie e il riordino: storia `0018`.

## 8. Punti aperti

**A chi si mostra il costo del servizio.** È un numero che dice quanto guadagna il salone su ogni cliente, e in
una sala con tre operatori non è detto che tutti debbano vederlo. La proposta è legarlo al ruolo (titolare e
amministratore sì, operatore no). Va confermato: è una scelta di prodotto con un risvolto sui rapporti interni,
non una questione tecnica.
