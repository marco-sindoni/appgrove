# 0025 — Prospetto delle provvigioni

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 06 — Operatori, provvigioni e rendimento
**Storia**: `0025` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0023`, `0024`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che il primo del mese deve dire a ciascuno quanto gli spetta
> voglio un prospetto per persona e per periodo, che si chiuda e non cambi più
> così da consegnare un numero che regge, invece di uno che si muove ogni volta che qualcuno tocca un conto.

**Contesto.** Le regole (storia `0024`) dicono *come* si calcola; questa storia dice *quanto* e *per quando*. Il
prospetto è il documento su cui il salone e la persona si mettono d'accordo: perché serva a quello, deve avere una
**chiusura** — un momento dopo il quale il numero non si muove più — e una **rettifica** visibile per quando ci si
accorge di un errore. È la stessa logica della chiusura del conto (storia `0019`): si storna, non si gomma.

⚠️ **Il confine, di nuovo e in modo esplicito.** Il prospetto è **per persona** perché è un fatto retributivo: si
paga una persona, non un salone. È l'unico posto dell'applicazione in cui un numero è intestato a un operatore, e
questo è deliberato — gli **indicatori** della storia `0026` sono per salone e per servizio, mai una classifica di
persone (§6 della [descrizione](../application-description.md), avviso sul lavoro; punto aperto n. 6). Un prospetto
serve a pagare; una classifica serve a confrontare, ed è un'altra cosa.

## 2. Requisiti funzionali

1. **RF-1** — Per un periodo scelto (di norma il mese) e per un operatore, il prospetto mostra: la **base
   maturata** distinta per tipo (servizi, rivendita), l'**importo calcolato** con la regola vigente in quel
   periodo, e l'elenco navigabile dei conti che lo compongono.
2. **RF-2** — Il prospetto si **chiude**: da quel momento base e importo sono congelati e non cambiano più, nemmeno
   se un conto del periodo viene rettificato dopo.
3. **RF-3** — Una rettifica che tocca un periodo **già chiuso** non modifica il prospetto chiuso: produce una
   **voce di rettifica sul periodo aperto successivo**, con il riferimento al periodo di origine.
4. **RF-4** — Il prospetto si esporta come tabella con gli stessi numeri che si vedono a schermo, per essere
   consegnato alla persona o al consulente del lavoro. **Non è un cedolino** e lo dichiara.
5. **RF-5** — Chi **amministra** vede i prospetti di tutti; ogni operatore vede **solo il proprio**, chiuso o
   aperto che sia. Non esiste una vista che affianchi i prospetti di più persone per confrontarle.
6. **RF-6** — Un prospetto con base zero esiste comunque e vale zero: un mese senza lavoro è un'informazione, non
   un buco.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni aggregazione filtra per `tenant_id` preso dal token verificato e,
  **prima di aggregare**, applica il filtro di visibilità per ruolo: un totale calcolato su righe che l'utente non
  potrebbe vedere è una fuga di informazione anche se il dettaglio resta nascosto.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/<app>/v1/prospetti?periodo=&operatore=`,
  `POST /api/<app>/v1/prospetti/{id}/chiusura`, `GET /api/<app>/v1/prospetti/{id}/export`; corpo validato; errori
  in `application/problem+json`; **nessuna rotta di riapertura**; definizione OpenAPI aggiornata nello stesso
  commit.
- **RT-3 — Persistenza (§8).** Migrazione sullo schema dell'app: tabella `prospetto_provvigioni` (periodo,
  operatore, basi per tipo, importo, stato, chi ha chiuso e quando) e `prospetto_voce` per il dettaglio e per le
  rettifiche riportate, con `tenant_id`, UUID versione 7, colonne di controllo e cancellazione logica; importi in
  **centesimi interi**; macchina a stati `aperto → chiuso`, senza ritorno.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Provvigioni*: scelta del periodo, elenco dei prospetti che il
  ruolo consente di vedere, dettaglio navigabile fino al conto. La chiusura mostra **prima** che cosa sta per
  congelare («3 operatori, 1.240 € in tutto, 47 conti»), perché una conferma che non dice cosa conferma non è una
  conferma. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, stati, testo dell'avvertenza «non è un cedolino» e messaggi della
  chiusura presenti in `en, it, fr, es, de`, con i formati numerici della lingua.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Funzione accesa dal piano che comprende le
  provvigioni; a piano insufficiente `402`. Con abbonamento in `past_due` la chiusura resta possibile: un salone in
  tolleranza deve poter pagare le persone.
- **RT-7 — Dati personali (§10).** Voce di manifesto `prospetto_provvigioni` in italiano e inglese: interessato =
  chi lavora nel salone; finalità «il conteggio chiuso del periodo»; base «esecuzione del contratto di lavoro o di
  collaborazione», per conto del salone; durata proposta 24 mesi. Il manifesto dichiara che il prospetto **non**
  è usato per valutare la persona e che non esistono viste comparative. Campi annotati `@PersonalData`; tabelle in
  esportazione e cancellazione (storie `0014` e `0032`).
- **RT-8 — Esposizione conversazionale (§12).** `provvigioni_periodo(periodo, operatore?) → base e importo`,
  marcato **lettura**, ristretto al ruolo che amministra e — per la propria — all'interessato; **non** ordina né
  confronta operatori (storia `0028`). `chiudi_prospetto_provvigioni(periodo) → bozza`, **scrittura irreversibile
  con conferma umana obbligatoria** (storia `0029`).
- **RT-9 — Registrazione eventi (§14).** `prospetto chiuso`, `prospetto esportato` con `tenant_id`, `app_id`,
  `user_id`, correlazione, periodo e conteggi — **mai** nomi né importi individuali.

## 4. Criteri di accettazione

**CA-1 — Il conteggio del mese**
- **Dato** Sara con regola 50 % sui servizi e 10 % sulla rivendita, e nel mese 1.800 € di servizi e 300 € di
  rivendita attribuiti a lei
- **Quando** si apre il prospetto del mese
- **Allora** mostra base 1.800 € / 300 €, importo 930 €, e l'elenco dei conti che li compongono

**CA-2 — La chiusura congela**
- **Dato** il prospetto del mese chiuso a 930 €
- **Quando** si rettifica un conto di quel mese per 20 €
- **Allora** il prospetto chiuso resta a 930 € e sul periodo aperto successivo compare una voce di rettifica con il
  riferimento al mese di origine

**CA-3 — Non si riapre**
- **Dato** un prospetto chiuso
- **Quando** si tenta di riaprirlo, anche interrogando direttamente l'interfaccia di programmazione
- **Allora** l'operazione fallisce e l'unica via offerta è la rettifica sul periodo successivo

**CA-4 — Ciascuno vede il proprio**
- **Dato** tre operatori con prospetti diversi
- **Quando** uno di loro apre la sezione *Provvigioni*
- **Allora** vede solo il proprio prospetto, e la richiesta diretta del prospetto di un collega risponde come per
  un prospetto inesistente

**CA-5 — Nessuna vista comparativa**
- **Dato** un utente che amministra
- **Quando** esamina tutte le viste e le rotte dei prospetti
- **Allora** non esiste alcun elenco che ordini gli operatori per importo o per base presentandoli come confronto:
  i prospetti si aprono uno per uno

**CA-6 — Isolamento fra account**
- **Dato** due account con prospetti dello stesso periodo
- **Quando** l'uno chiude il proprio
- **Allora** nessun valore dell'altro contribuisce ad alcun totale e nulla cambia nell'altro account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul conteggio del periodo, sugli scaglioni a cavallo del periodo e sulle rettifiche
      riportate; di **integrazione** sulla chiusura, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** **sugli aggregati**, non solo sui dettagli;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-SALONGROVE-PKG]` (storia `0031`) verifica che una
      seduta di pacchetto scalata faccia maturare la provvigione attesa; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** in tutte e cinque le lingue, con i formati numerici corretti;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con il limite d'uso dichiarato;
- [ ] **registro delle decisioni**: chiusura irreversibile, rettifica sul periodo successivo, visibilità per ruolo,
      assenza di viste comparative;
- [ ] avvio locale invariato; il salone di prova ha un prospetto chiuso e uno aperto.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0023` | la base viene dalle righe attribuite |
| storia `0024` | l'importo viene dalla regola vigente nel periodo |
| storia `0019` | le rettifiche dei conti chiusi sono la sorgente delle voci di rettifica |

## 7. Fuori ambito

- il **pagamento** di quanto spetta: appgrove non muove denaro, in nessun caso;
- il **cedolino** e gli adempimenti del rapporto di lavoro: perimetro escluso (app 10 PayGrove, esclusa);
- gli **indicatori** del salone: storia `0026`, che ha un confine diverso e più delicato;
- l'approvazione del prospetto da parte dell'operatore (un «visto» firmato): utile, ma è un flusso di
  accettazione che merita una storia propria se lo si vorrà.

## 8. Punti aperti

**Chi può chiudere un prospetto.** La proposta è: solo chi amministra l'account. Va confermato insieme al punto
precedente sui permessi (storia `0023`).

**Se l'operatore debba poter vedere il proprio prospetto ancora aperto.** Mostrarlo è trasparente e riduce le
discussioni; però un numero che si muove fino alla chiusura può essere letto come una promessa. La proposta è
mostrarlo con l'etichetta «in corso, non definitivo». Da confermare.
