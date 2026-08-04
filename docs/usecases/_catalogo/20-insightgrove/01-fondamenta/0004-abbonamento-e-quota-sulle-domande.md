# 0004 — Abbonamento e quota sulle domande

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 01 — Fondamenta
**Storia**: `0004` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che paga un abbonamento
> voglio sapere quante domande al copilota mi restano questo mese, e trovarmi bloccato con una spiegazione invece
> che con una fattura a sorpresa
> così da poter decidere io se mi serve un piano più grande.

**Contesto.** InsightGrove ha una sola cosa che costa davvero: la domanda al copilota, che è una chiamata a un
modello linguistico. Tutto il resto — cruscotti, riquadri, avvisi, rapporti, fonti collegate — è calcolo su dati
già in casa e resta **illimitato in ogni piano** (§3 e §5 della [descrizione](../application-description.md)).
Questa storia mette in piedi la catena dei varchi e il contatore, **prima** che esista il copilota: così quando
la storia 0026 lo collegherà, il meccanismo sarà già collaudato e non ci sarà la tentazione di lasciarlo per
dopo.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il file di listino `services/core/src/main/resources/pricing/insights.yaml`, registrato in
   `pricing/index.yaml`, con `userModel: multi_user`, `category: blue` e tre piani (`free`, `pro`, `business`),
   ciascuno con `limits: { metric: questions, cap: <N>, type: flow }`.
2. **RF-2** — Il servizio tiene un contatore delle domande **per account e per finestra mensile**, che si azzera
   all'inizio di ogni periodo di fatturazione.
3. **RF-3** — Esiste una risorsa di lettura che restituisce, per l'account chiamante, quante domande sono state
   consumate, quale sia il tetto e quando la finestra si azzera.
4. **RF-4** — Esiste il punto di prenotazione della quota che ogni funzione che consuma domande dovrà chiamare:
   prenota una unità, e se il tetto è raggiunto risponde `429` con un messaggio che dice **cosa è successo, cosa
   non si può più fare e come si rimedia**.
5. **RF-5** — L'interfaccia mostra il consumo nella sezione Cruscotto e un avviso quando il consumo supera
   l'80 % del tetto.
6. **RF-6** — La catena dei varchi è rispettata nell'ordine: gettone non valido `401`; app spenta dalla
   piattaforma `403`; account non abilitato `402`; ruolo insufficiente `403`; quota esaurita `429`.

## 3. Requisiti tecnici

- **RT-6 — Varchi e quota (§6, §7).** L'abilitazione si legge dalla **proiezione locale** dell'app, alimentata a
  eventi: mai una chiamata di rete sincrona all'app centrale sul percorso caldo. Prima di eseguire una domanda il
  servizio prenota una unità della metrica `questions` (natura `flow`); a quota esaurita risponde `429`. Con
  abbonamento in `past_due` la funzione resta accessibile; con `canceled` risponde `402`. I **diritti
  dell'interessato** restano accessibili in ogni caso.
- **RT-7 — Listino come codice (§7).** La storia **non fissa i prezzi**: consuma il tetto pubblicato
  dall'abilitazione per la metrica `questions`. I valori del listino sono una proposta da confermare
  (§5 della descrizione, fermata di escalation).
- **RT-1 — Isolamento fra account (§1).** Il contatore è per account, letto con `tenant_id` dal gettone
  verificato; un account non può leggere né consumare la quota di un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/insights/v1/quota`; errori in
  `application/problem+json` con il codice della catena dei varchi; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-5 — Cinque lingue (§4).** I messaggi di quota vicina e di quota esaurita esistono in `en, it, fr, es, de`.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: il contatore è per account, non per persona.
- **RT-14 — Registrazione eventi (§14).** Gli eventi «quota prenotata» e «domanda respinta per quota» sono
  registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati personali.

## 4. Criteri di accettazione

**CA-1 — Il consumo si vede**
- **Dato** un account sul piano `pro` che ha consumato 42 domande nel mese in corso
- **Quando** apre la sezione Cruscotto
- **Allora** vede «42 di 300 domande — questo mese» e la data in cui il contatore si azzera

**CA-2 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto di `questions` del proprio piano
- **Quando** tenta di prenotare una unità di quota
- **Allora** riceve `429`, un messaggio che spiega come rimediare (passare di piano o attendere l'azzeramento) e
  **nulla viene consumato né eseguito**

**CA-3 — La finestra si azzera**
- **Dato** un account che ha esaurito la quota nel mese precedente
- **Quando** inizia il nuovo periodo di fatturazione
- **Allora** il contatore riparte da zero e la prenotazione torna a riuscire

**CA-4 — Abbonamento non attivo**
- **Dato** un account con abbonamento `canceled`
- **Quando** chiama una funzione protetta di InsightGrove
- **Allora** riceve `402`; ma la richiesta di esportazione dei propri dati continua a funzionare

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` con consumi diversi
- **Quando** un utente di `A` legge la propria quota forzando l'identificativo di `B` nella richiesta
- **Allora** vede il consumo di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sul contatore e sull'azzeramento della finestra, e di **integrazione** sulla risorsa,
      con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sul contatore e sulla risorsa di lettura;
- [ ] prove sulla **matrice dei ruoli** e sull'abilitazione negata (`402`);
- [ ] **prova end-to-end**: *rimando* alla storia 0034, che possiede il percorso `[J-INSIGHTS]`; voce
      `da-coprire` nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per i messaggi di quota;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato, con la natura `flow` della metrica e il perché;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione introdotta qui, ma va annotato che la quota
      si applicherà **anche** alle chiamate dell'assistente (storia 0033);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | serve il servizio |
| storia `0003` | serve la sezione Cruscotto dove mostrare il consumo |
| decisione sul listino (§5 della descrizione) | prezzi, tetti e durata della prova sono una fermata di escalation dello sviluppatore |

## 7. Fuori ambito

- il copilota che consuma la quota: storia 0026;
- l'applicazione della quota alle chiamate dell'assistente esterno: storia 0033;
- la deroga temporanea concessa dalla console di amministrazione: [estensioni-admin.md](../estensioni-admin.md).

## 8. Punti aperti

- **I tetti proposti (20 / 300 / 1.500 domande al mese) non sono validabili** finché non si misura il costo medio
  di una domanda su un prototipo (punto aperto 9 della descrizione). Chiude: **sviluppatore**.
- **Quanto vale una domanda?** Se una richiesta complessa richiede più chiamate al modello, conta ancora una
  unità? La raccomandazione è **sì, una domanda = una unità**, perché è la sola unità che il cliente capisce; il
  costo variabile si governa con il tetto, non con la tariffazione. Chiude: **sviluppatore**.
