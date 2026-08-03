# 0012 — Calendario dei rinnovi

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 03 — Rinnovi e scadenze
**Storia**: `0012` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0011`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare
> voglio che i rinnovi si aprano da soli il giorno giusto e generino ciò che è dovuto
> così da non dover ricordare a fine mese chi va rifatturato, che è esattamente la cosa che dimentico.

**Contesto.** È la storia che fa vivere l'app da sola. Ogni giorno una lavorazione guarda quali periodi finiscono,
chiude quello in corso, apre il successivo e genera la **scadenza** — quanto è dovuto, per quale periodo, entro
quando. Ci sono tre trappole, e vanno affrontate qui una volta per tutte. La prima è il **doppione**: se la
lavorazione gira due volte, o se qualcuno la rilancia a mano, non devono nascere due scadenze per lo stesso
periodo. La seconda è il **giorno che non esiste**: un abbonamento mensile decorso il 31 gennaio si rinnova il 28
febbraio, non il 3 marzo, e chi lo scrive per la prima volta lo sbaglia. La terza è il **giorno saltato**: se la
lavorazione non gira per due giorni, al terzo deve recuperare, non ripartire da oggi.

**Riuso di piattaforma da valutare qui, e non altrove.** L'aritmetica del calendario ricorrente — dato un ciclo e
una decorrenza, quando finisce il periodo — è la stessa che il servizio centrale usa per gli abbonamenti di
appgrove. Il §10.1 della descrizione propone di estrarla in una libreria condivisa **quando la seconda
implementazione esiste davvero**, cioè adesso. È una decisione di piattaforma: chi implementa questa storia deve
**sollevare la mano**, non decidere da solo.

## 2. Requisiti funzionali

1. **RF-1** — Una lavorazione giornaliera individua gli abbonamenti il cui periodo in corso finisce, chiude quel
   periodo e ne apre uno nuovo secondo il ciclo del piano.
2. **RF-2** — Per ogni periodo aperto nasce una **scadenza** con: periodo coperto, importo dalla versione di
   prezzo agganciata, data di esigibilità, stato iniziale `in_attesa`.
3. **RF-3** — La lavorazione è **idempotente**: eseguirla due volte sullo stesso giorno non crea doppioni, e la
   chiave di unicità è la coppia abbonamento + periodo.
4. **RF-4** — La lavorazione **recupera i giorni saltati**: se non gira per N giorni, alla ripresa elabora tutti
   i rinnovi maturati nel frattempo, nell'ordine giusto.
5. **RF-5** — Il calcolo della data di fine periodo gestisce i mesi corti: un ciclo mensile decorso il 31 finisce
   l'ultimo giorno del mese successivo, e il giorno di ancoraggio **non** si perde ai mesi successivi.
6. **RF-6** — Gli abbonamenti in stato `sospeso`, `in_ritardo` e `cessato` **non** generano scadenze nuove; per
   `disdetto_a_scadenza` si genera l'ultima e poi si passa a `cessato`.
7. **RF-7** — Lo stato della lavorazione (ultima esecuzione, quante scadenze create, quanti errori) è visibile
   nella panoramica e alla console di amministrazione.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La lavorazione elabora un account alla volta e ogni scrittura filtra
  per `tenant_id`; un errore su un account non ferma gli altri.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/abbonati/v1/scadenze` con filtri per periodo e
  stato; rotta amministrativa per rilanciare la lavorazione di un account; errori in `problem+json`; OpenAPI
  aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V7__scadenza.sql`: tabella `scadenza` con `tenant_id`, chiave UUID
  versione 7, colonne di controllo, cancellazione logica, e **vincolo di unicità** su abbonamento + periodo, che
  è il presidio vero contro i doppioni — non la logica applicativa.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Scadenze*: elenco con filtro per stato e per periodo, e un
  riquadro nella panoramica con «prossimi rinnovi» e lo stato dell'ultima lavorazione; solo token del sistema di
  design.
- **RT-5 — Cinque lingue (§4).** Etichette, stati delle scadenze e messaggi in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6).** La lavorazione **non** consuma quota: il tetto è sugli abbonamenti vivi, non
  sui rinnovi. Con abbonamento di piattaforma non attivo la lavorazione **si ferma** per quell'account e lo
  registra: non si continua a elaborare per chi non è abbonato.
- **RT-7 — Esposizione conversazionale (§12).** Strumento dichiarato:
  `prossimi_rinnovi(giorni) → elenco con importo e data`, marcato **lettura**, libero.
- **RT-8 — Dati personali (§10).** La scadenza contiene importi, non persone, ma è riferita a un abbonamento:
  va aggiunta al manifesto, a `exportData` e a `purgeData`.
- **RT-9 — Registrazione eventi (§14).** `lavorazione avviata`, `scadenza creata`, `periodo chiuso`,
  `lavorazione fallita (con causa)`, con `tenant_id`, `app_id` e correlazione, senza nomi.
- **RT-10 — Prove (§11).** Prove di unità sull'aritmetica delle date — mesi corti, anni bisestili, cicli
  trimestrali e annuali — che sono il genere di errore che nessuno vede finché non è febbraio.

## 4. Criteri di accettazione

**CA-1 — Rinnovo che apre il periodo e crea la scadenza**
- **Dato** un abbonamento mensile a 39 € il cui periodo finisce oggi
- **Quando** gira la lavorazione
- **Allora** il periodo in corso è chiuso, ne è aperto uno nuovo di un mese, ed esiste una scadenza da 39 € per
  quel periodo, in stato `in_attesa`

**CA-2 — Idempotenza**
- **Dato** la lavorazione già eseguita oggi · **Quando** la si rilancia
- **Allora** non nasce alcuna scadenza in più e l'esecuzione termina con successo

**CA-3 — Mese corto**
- **Dato** un abbonamento mensile decorso il 31 gennaio · **Quando** si rinnova
- **Allora** il periodo finisce il 28 febbraio (29 se bisestile), e il rinnovo successivo torna al 31 marzo

**CA-4 — Giorni saltati**
- **Dato** una lavorazione ferma da tre giorni, con cinque rinnovi maturati
- **Quando** riparte
- **Allora** crea tutte e cinque le scadenze con le date corrette dei rispettivi periodi, non con quella di oggi

**CA-5 — Stati che non rinnovano**
- **Dato** abbonamenti in `sospeso`, `in_ritardo` e `cessato` in scadenza oggi
- **Quando** gira la lavorazione
- **Allora** non nasce alcuna scadenza per nessuno dei tre

**CA-6 — Isolamento fra account**
- **Dato** due account, uno dei quali provoca un errore
- **Quando** gira la lavorazione
- **Allora** l'altro account viene elaborato regolarmente e l'errore è registrato con il proprio account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend` e `frontend`);
- [ ] prove di **unità** sull'aritmetica delle date, sull'idempotenza e sul recupero dei giorni saltati; prova di
      **integrazione** sulla lavorazione con database effimero;
- [ ] prova di **isolamento fra account** sulla lavorazione;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-ABBONATI]` della storia `0033` fa maturare un
      rinnovo e verifica la scadenza; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la tabella `scadenza`;
- [ ] **registro delle decisioni** compilato: idempotenza per vincolo di unicità, regola dei mesi corti, e
      **la domanda sull'estrazione dell'aritmetica in libreria condivisa, posta e non decisa**;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0010` | servono abbonamenti con periodo e prezzo |
| storia `0011` | la macchina a stati decide chi rinnova e chi no |
| **decisione di piattaforma** sull'estrazione dell'aritmetica ricorrente (§10.1) | va posta qui: è il momento in cui la seconda implementazione nasce |

## 7. Fuori ambito

- l'avviso all'abbonato prima del rinnovo: storia `0013`, ed è un obbligo di legge;
- l'incasso e il suo esito: epica 04;
- il conguaglio di un cambio di piano a metà periodo: storia `0014`;
- l'uscita verso il documento fiscale: storia `0016`.

## 8. Punti aperti

**Estrazione dell'aritmetica del ricorrente in `services/commons`.** È il punto aperto n. 1 della descrizione e
va sollevato **in questa storia**, perché è qui che il codice viene scritto la seconda volta. La proposta è:
estrarre calcolo del periodo, conguaglio proporzionale e normalizzazione a mese in una libreria condivisa **con
le proprie prove**, senza persistenza né fornitore. Non lo decide chi implementa: lo decide la piattaforma.
Chiude: lo sviluppatore.
