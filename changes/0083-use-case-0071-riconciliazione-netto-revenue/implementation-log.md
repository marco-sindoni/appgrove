# Change 0083 — Log di implementazione

**Branch**: `change/0083-use-case-0071-riconciliazione-netto-revenue`
**Use case sorgente**: [UC 0071 — Riconciliazione netto/revenue](../../docs/usecases/13-abbonamenti-self-service/0071-riconciliazione-netto-revenue.md)
**Modalità**: fast (gate di workflow rinunciati all'invocazione; suite completa verde obbligatoria)
**Registro decisioni**: [decisions.json](decisions.json) — 24 voci, 23 in autopilot

## Cosa è stato fatto

### `services/core` — acquisizione, riconciliazione, osservabilità

- **Migrazione `V15__payout_reconciliation.sql`**: tre colonne nuove sulla riga di transazione
  (`fee_amount`, `net_amount`, `fee_source`) e due tabelle di piattaforma per gli accrediti
  (`platform.payout`, `platform.payout_line`). Gli accrediti **non hanno tenant**: raccolgono transazioni di
  conti diversi. Le righe già registrate restano senza commissione — vedi «Scelte non ovvie».
- **`PaymentFees`**: commissione e netto di una transazione. Il valore dichiarato dal fornitore vince sempre;
  in sua assenza si stima con la formula del listino (percentuale + quota fissa, entrambe configurabili) e la
  riga resta marcata `estimated`.
- **`PaddleWebhookEvent`**: il payload della transazione può ora portare `fee`; nuovo blocco `payout` con il
  dettaglio per transazione; `transaction.refunded` mappato sul nuovo stato `refunded`.
- **`SubscriptionWriter`**: calcola commissione e netto nella scrittura dello storico, e registra gli accrediti
  con la **stessa** postura degli altri eventi — una sola transazione, deduplicazione sull'identificativo
  dell'evento, guardia contro gli eventi vecchi (`returning id`: se l'accredito è stale il dettaglio non viene
  toccato).
- **`ReconciliationService` / `ReconciliationDtos`**: totali, righe per mese di **addebito** ed elenco degli
  accrediti con lo scostamento e l'esito della quadratura (`matched` / `mismatch` / `mixed_currency`). Query
  native cross-tenant, come tutta la console amministrativa.
- **`ReconciliationMetrics`**: rilevazione oraria delle misure `appgrove.billing.reconciliation.*` e
  segnalazione — misura più log di avvertimento — dell'**accredito atteso che non arriva**.
- **`AdminResource`**: `GET /api/platform/v1/admin/reconciliation`, dietro il ruolo di piattaforma.
- **Stub locale**: scenari `payout` e `refund`, che passano dalla stessa pipeline firmata degli altri.
- **Contratto**: `openapi.yaml` rigenerato dalla build e tipi del client frontend riallineati.

### `frontend/apps/admin` — la pagina «Riconciliazione»

Nuova pagina nel gruppo *Revenue* (rotta, voce di menu, etichetta della barra superiore): quattro riquadri di
totale, avviso di accredito in ritardo, avviso di commissioni sopra soglia, nota su quante righe hanno la
commissione stimata, tabella per mese e tabella degli accrediti con la quadratura. Diciture nelle **5 lingue**.

### Compliance e registri

Manifesto dati aggiornato (due voci nuove, entrambe le lingue), registro dei trattamenti rigenerato, campi
inclusi nell'esportazione prevista dai diritti dell'interessato, punto **L16** aperto in
`docs/_REVISIONE-LEGALE.md`, registro di copertura end-to-end aggiornato, UC 0071 marcato implementato in
`docs/usecases/EPICS-WAVE-2.md`.

## Scelte non ovvie (il resto è in `decisions.json`)

- **La quadratura è congelata nella riga di dettaglio.** Il netto accreditato per una transazione è quello
  comunicato *allora*, mai ricalcolato: uno storno successivo non deve far apparire sbagliato un accredito che
  al momento era corretto. Il denaro restituito riappare come riga **negativa** in un accredito successivo,
  esattamente come lo comunica il fornitore.
- **Le transazioni antecedenti restano senza commissione.** La migrazione non le riempie. Calcolare a
  posteriori una commissione mai osservata scriverebbe un numero falso proprio nella vista che esiste per dire
  la verità sui soldi.
- **Valute diverse nello stesso accredito ⇒ nessuno scostamento.** Sommare valute diverse produrrebbe un
  numero che *sembra* una differenza. La riga è marcata «non quadrabile» e si aspettano i dati reali sul cambio.
- **Lo stub non dichiara la commissione**, quindi in locale tutto risulta stimato: è il caso conservativo e
  quello che si vedrà davvero finché non sapremo cosa manda il fornitore. Deviazione consapevole dal punto 7
  dei requisiti, registrata (decisione 18).
- **La vista si legge dall'endpoint anche nei test**: le query native passano dal gestore delle entità, che
  fuori da una richiesta autenticata non ha tenant e nega la lettura (fail-closed). Passare dall'endpoint prova
  anche che la superficie sia servita e protetta.

## Gate privacy/RoPA (UC 0031)

Scanner eseguito: **14 segnali**, tutti attesi. Commissione e netto per transazione sono **dati di
fatturazione** della stessa natura dell'importo già trattato — finalità: riconciliare il ricavo con il denaro
accreditato; base giuridica: esecuzione del contratto; conservazione: account attivo + 14 giorni di tolleranza;
categoria ordinaria; nessun destinatario esterno nuovo. **Classificazione MINOR**: nessun aggiornamento di
versione di informativa o condizioni, nessuna ri-accettazione. Le tabelle degli accrediti sono dati economici
della piattaforma e non entrano nell'esportazione del cliente. Nessun nuovo responsabile esterno.
Manifesto aggiornato nelle due lingue e registro dei trattamenti rigenerato (`npm run assemble`).

Un punto è stato **tracciato e non deciso**: il dettaglio degli accrediti conserva il riferimento della
transazione presso il fornitore in una tabella che la cancellazione dei dati del cliente non tocca → punto
**L16** di `docs/_REVISIONE-LEGALE.md`.

## Test

- **`services/core`** — `ReconciliationTest` (18 test): commissione dichiarata contro stimata, tentativo
  fallito senza commissione, quadratura, scostamento, valuta mista, evento duplicato e fuori ordine, rimborso e
  contestazione con restituzione nell'accredito successivo, accredito a cavallo di due mesi, netto non
  accreditato, scenari dello stub, accredito in ritardo, peso delle commissioni. `AdminApiTest` estende il
  gating al nuovo endpoint (200 per l'amministratore, 403 per un titolare di conto).
- **`frontend`** — `reconciliation.test.tsx` (9 test): totali, righe per mese, i tre esiti di quadratura,
  i due avvisi, la nota sulle stime, stato vuoto, errore con riprova, accessibilità.
- **End-to-end livello 2** — `[L2-ADMIN-RECON]` in `frontend/apps/admin/e2e/riconciliazione.spec.ts`, dal menu
  alla pagina fino alla quadratura. Nessuna base di riferimento visiva toccata.
- **Copertura end-to-end**: 0071 esce dalle esenzioni ed entra fra gli use case con superficie, collegato al
  nuovo percorso; `tools/e2e-coverage` verde.

**`./run-tests.sh` completa: verde** (backend, frontend, infra, compliance, tooling, smoke, platform, site).

## Fuori scope, tracciato

Ricavo ricorrente mensile e tasso di abbandono (UC 0021 / backlog), rimborsi parziali, tasso di cambio,
taratura reale delle soglie, allarmi infrastrutturali sulle nuove misure, implementazione reale del fornitore
(bloccata da #14). Tutto nei punti aperti di UC 0071.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No — colonne e stato nuovi sono additivi; nessuna interfaccia esistente modificata |
| Contratto cross-area | Sì — nuova lettura amministrativa: `openapi.yaml` e tipi del client rigenerati nello stesso commit |
| Version bump | minor |
