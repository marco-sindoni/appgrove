# 0013 — Metriche derivate da formula

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 03 — Catalogo delle metriche e tracciabilità
**Storia**: `0013` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una micro-impresa
> voglio vedere quanti giorni ci metto a farmi pagare, e quanto margine mi resta
> così da accorgermi che sto finanziando i miei clienti prima che la banca me lo faccia notare.

**Contesto.** Gli indicatori che una micro-impresa cerca davvero non sono somme: sono **rapporti fra somme
provenienti da fonti diverse**. I giorni medi di incasso mettono insieme crediti e fatturato; il margine mette
insieme ricavi e spese; il ciclo del contante somma tre indicatori derivati. Il materiale consultato è concorde
nel metterli al centro, con la soglia d'allarme dei 60-90 giorni di incasso (§2.5 della
[descrizione](../application-description.md), fonti 5 e 6). Sono anche **la valvola di sfogo** al catalogo
chiuso: non si scrivono interrogazioni libere, ma si compongono indicatori esistenti.

## 2. Requisiti funzionali

1. **RF-1** — Una definizione di metrica può essere **derivata**: invece di aggregare una misura, calcola una
   espressione su altre metriche pubblicate, con le quattro operazioni, una costante e la moltiplicazione per il
   numero di giorni del periodo.
2. **RF-2** — L'espressione riferisce le metriche **per chiave e versione**: se una metrica di base cambia
   versione, la derivata non cambia da sola — va aggiornata esplicitamente, creando una versione nuova.
3. **RF-3** — Una derivata dichiara le proprie **fonti richieste** come unione delle fonti delle metriche che
   compone: se ne manca una, non produce valori (regola della storia 0012).
4. **RF-4** — La divisione per zero e i periodi senza dati non producono zero: producono **nessun valore**, con
   il motivo. Un margine «0 %» perché non ci sono ricavi è un numero falso.
5. **RF-5** — L'app nasce con le derivate predefinite più utili al segmento: **giorni medi di incasso**, **margine
   lordo percentuale**, **giorni di giacenza di magazzino**, **ciclo del contante**, **valore medio del
   documento**, **tasso di accettazione dei preventivi**.
6. **RF-6** — Una derivata **non può riferire sé stessa**, direttamente o attraverso una catena: i cicli sono
   rifiutati alla pubblicazione, non scoperti al calcolo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Le derivate seguono la stessa regola delle metriche di base: quelle di
  sistema sono uguali per tutti, quelle del cliente hanno `tenant_id` e si leggono con il filtro per account
  preso dal gettone verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta nuova: si estende la risorsa delle metriche.
  Il corpo è validato — l'espressione è **analizzata**, non eseguita come testo — e gli errori escono in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** L'espressione è salvata in forma **strutturata** (albero di operazioni con
  riferimenti a chiave e versione), non come stringa da interpretare: una stringa da interpretare è un motore di
  esecuzione mascherato.
- **RT-4 — Modulo frontend (§3, §5).** La sezione Metriche mostra l'espressione in forma leggibile, non in
  codice; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Titoli, descrizioni e messaggi di errore delle derivate predefinite esistono in
  `en, it, fr, es, de`.
- **RT-6 — Varchi e ruoli (§6).** Creare e pubblicare una derivata richiede `owner` o `admin`; nessun consumo di
  quota.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo.
- **RT-11 — Prove (§11).** Prove di unità sull'analisi dell'espressione, sul rifiuto dei cicli, sulla divisione
  per zero e sulla propagazione delle fonti richieste.

## 4. Criteri di accettazione

**CA-1 — Giorni medi di incasso**
- **Dato** un account con le fonti fatturazione e incassi collegate, un fatturato di 120.000 € nell'anno e
  crediti aperti medi per 20.000 €
- **Quando** si guarda la metrica «giorni medi di incasso» sull'anno
- **Allora** mostra il valore calcolato secondo la formula dichiarata, con l'espressione leggibile nella scheda
  del numero

**CA-2 — Fonte mancante, nessun valore**
- **Dato** una derivata che compone una metrica sulla fonte magazzino, non collegata
- **Quando** si prova a calcolarla
- **Allora** non produce un valore e dice quale fonte manca

**CA-3 — Divisione per zero**
- **Dato** un periodo in cui il denominatore della derivata è zero
- **Quando** si calcola
- **Allora** il risultato è «non calcolabile per questo periodo», **non** zero e **non** un errore tecnico
  mostrato all'utente

**CA-4 — I cicli sono rifiutati**
- **Dato** una derivata `A` che riferisce `B`, e si tenta di pubblicare `B` riferendo `A`
- **Quando** si chiede la pubblicazione
- **Allora** la pubblicazione è rifiutata con «l'espressione contiene un ciclo», e nessuna versione nuova viene
  creata

**CA-5 — Isolamento fra account**
- **Dato** due account con derivate personalizzate omonime
- **Quando** un utente di `A` calcola la propria
- **Allora** ottiene il risultato della propria definizione, non di quella di `B`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sull'analisi dell'espressione, sui cicli, sulla divisione per zero e sulla propagazione
      delle fonti; prove di **integrazione** sul calcolo delle derivate predefinite con dati inventati noti;
- [ ] prova di **isolamento fra account** sulle definizioni derivate;
- [ ] **prova end-to-end**: *rimando* alla storia 0034; voce `da-coprire` nel registro di copertura;
- [ ] **traduzioni** delle derivate predefinite presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con l'espressione strutturata invece della stringa e il perché;
- [ ] contratto degli **strumenti conversazionali**: le derivate compaiono in `elenca_metriche` come tutte le
      altre, con l'espressione leggibile;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | una derivata compone metriche pubblicate |

## 7. Fuori ambito

- il calcolo su un periodo e il confronto fra periodi: storia 0015;
- la scomposizione di uno scostamento: storia 0029;
- formule libere scritte dall'utente in un linguaggio proprio: **fuori ambito per scelta** — è precisamente ciò
  che il catalogo chiuso evita (§4.3 della descrizione).

## 8. Punti aperti

- **Quante operazioni servono davvero?** Le quattro operazioni più la moltiplicazione per i giorni del periodo
  coprono tutti gli indicatori delle fonti 5 e 6. Aggiungere funzioni (radici, logaritmi, condizioni) trasforma
  l'espressione in un linguaggio, e allora tanto vale ammettere le formule libere — con tutto ciò che comporta.
  Raccomandazione: **restare alle quattro operazioni** e rivalutare solo su richiesta rilevata. Chiude:
  **sviluppatore**.
- **La derivata non segue la versione della metrica di base**: è voluto (RF-2), ma significa che un cliente può
  ritrovarsi una derivata «vecchia» che compone una versione superata. Serve un avviso nella sezione Metriche.
  Raccomandazione: sì, avviso non bloccante. Chiude: **sviluppatore**.
