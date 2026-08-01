# UC 0096 — Billing solo-fatturazione (abbonamenti + storico pagamenti/ricevute)

**Area**: 21-catalogo-app-backoffice · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0095 (App catalog — ospita l'acquisto), UC 0028 (self-service abbonamento), UC 0025 (pipeline webhook), UC 0026 (ciclo di vita subscription), UC 0077 (regola unica di accesso), UC 0076 (disabilita applicazione — punto aperto Billing)
**Fonte decisioni**: change `0066` (proposta UX approvata), #09 (pagamenti — Paddle merchant of record)
**Ultimo aggiornamento**: 2026-08-01

## 1. Obiettivo / Scope

Fare della pagina **Billing** una pagina di **sola fatturazione**: gli abbonamenti del workspace (cambio piano,
disdetta, riattivazione — le azioni self-service esistenti) e lo **storico dei pagamenti con le ricevute**. Sparisce
tutto ciò che è "scoperta/acquisto": il titolo "Get an app" e la griglia delle app acquistabili (che migrano nel
catalogo, UC 0095).

**Incluso**: ristrutturazione della pagina; storico pagamenti/ricevute; stato "disabilitata dalla piattaforma"
sulle card degli abbonamenti (chiude il punto aperto tracciato in UC 0076). **Escluso**: modifiche a checkout
(UC 0024) e alle azioni self-service (UC 0028) — cambiano posto, non comportamento.

## 2. Attori & ruoli

- **Owner/admin**: gestisce piani e vede pagamenti/ricevute.
- **Member**: accesso alla pagina secondo le regole esistenti; nessuna azione di modifica piano.
- **Paddle** (merchant of record): fonte delle transazioni e delle ricevute.

## 3. Precondizioni

Tenant autenticato; UC 0095 disponibile (il rimando "attiva un'app" punta al catalogo).

## 4. Flusso principale

1. L'utente apre **Billing**: intestazione "Billing — Manage your plans, payments and receipts" (i18n 5 lingue).
2. Sezione **Your subscriptions**: una card per subscription del tenant (fonte `/me/subscriptions`, UC 0028) con
   piano, quota inclusa, prossimo rinnovo/scadenza e le azioni self-service esistenti; badge di stato coerenti con
   il catalogo (UC 0095). **Se l'app è disabilitata dalla piattaforma**, la card lo dice con un avviso esplicativo
   (e l'eventuale effetto sull'addebito) invece di mostrare un "Active" muto — è il fix del punto aperto UC 0076.
3. Sezione **Payments & receipts**: tabella dello storico — data, app, descrizione, importo, esito, collegamento
   alla **ricevuta Paddle**. La change decide la fonte tecnica (persistenza delle transazioni ricevute via webhook
   vs lettura on-demand dal fornitore), con requisito di completezza: tutte le transazioni del tenant, anche
   fallite.
4. Un workspace **senza subscription** mostra lo stato vuoto con rimando al catalogo (non una griglia d'acquisto).

## 5. Flussi alternativi / edge / errori

- **Pagamento fallito**: la riga di storico lo mostra (esito "Failed") e la card della subscription espone l'azione
  di aggiornamento del metodo di pagamento (comportamento dunning esistente, UC 0026).
- **Storico non leggibile**: errore con riprova nella sola sezione pagamenti; gli abbonamenti restano visibili
  (guasti indipendenti, niente pagina tutta rossa).
- **Ricevuta non disponibile** (ritardo del fornitore): la riga esiste senza link, con stato onesto.

## 6. Schermate & stati

Riferimento visivo vincolante: vista "Billing" dell'artefatto approvato (change 0066) — card subscription con
badge; avviso rosso tenue per l'app disabilitata; tabella pagamenti con importi in carattere mono e link
"Receipt ↗"; stati loading/empty/error per sezione.

## 7. Dati toccati

- Se la change sceglie la **persistenza delle transazioni**: nuova tabella tenant-scoped (importo, valuta, esito,
  riferimento Paddle, `app_id`) alimentata dalla pipeline webhook (UC 0025). Dati di fatturazione riferiti
  all'account, non nuove categorie di dati personali; verificare col gate privacy/RoPA della change (UC 0031) se il
  manifesto va integrato.

## 8. Permessi & gate

- Azioni di modifica piano: stessi gate di UC 0028 (ruolo abilitato al billing; backend fa fede).
- Storico pagamenti: row-level `WHERE tenant_id` dal JWT; nessun identificatore dal client.
- Logging strutturato su lettura storico e azioni.

## 9. Requisiti di test

- Unit: composizione della card subscription (stati × disabilitazione piattaforma); mappatura transazione → riga.
- Integrazione backend: se si persiste, consumer webhook → transazione registrata (idempotente, out-of-order).
- L2 Playwright: pagina senza elementi di catalogo; card disabilitata-da-piattaforma con avviso; storico con
  pagamento fallito; stato vuoto con rimando al catalogo.
- **Journey di piattaforma** (epica 20): estensione di J-SUB/J-BUY — dopo un acquisto la transazione compare nello
  storico; registro di copertura (UC 0093) quando disponibile.

## 10. Riferimenti & Definition of Done

- **Decisioni**: change 0066; UC 0076 (punto aperto Billing che questo UC chiude); #09 (Paddle merchant of record).
- **DoD**: Billing senza elementi di catalogo; storico pagamenti reale in locale (webhook stub); avviso
  disabilitazione; i18n 5 lingue; test verdi; punto aperto UC 0076 marcato risolto con rimando a questa change.

## Punti aperti / decisioni differite

Aperti dalla change `0077` (implementazione di questo use case).

- **Lo storico pagamenti non è paginato né filtrabile.** La tabella mostra tutte le transazioni del
  conto, dalla più recente. Per un conto giovane è la cosa giusta — vederle tutte è il punto — ma un
  conto con anni di rinnovi su più app produrrà centinaia di righe in un colpo solo. Quando succederà
  servirà una paginazione lato server (l'indice `(tenant_id, billed_at desc)` c'è già) e, probabilmente,
  un filtro per app e per anno. Non anticipato qui perché sarebbe complessità senza un problema:
  possiede il tema questo use case.
- **Rimborsi e note di credito non sono modellati.** Lo storico distingue tre esiti — pagato, fallito,
  contestato — perché sono i soli che il set di eventi sottoscritto (#09 D21) sa distinguere. Un
  rimborso (`transaction.refunded` o simile) oggi non arriva affatto e quindi non compare: l'utente lo
  vedrebbe solo nel portale del fornitore. Va deciso insieme all'allargamento del set di eventi, che
  possiede **UC 0025** (pipeline webhook), e alla politica commerciale dei rimborsi.
- **L'importo mostrato è quello che il fornitore ci ha comunicato.** In locale lo stub lo ricava dal
  listino della fascia; in produzione arriverà dal payload reale di Paddle, il cui formato preciso è
  materia dell'ambiente di prova del fornitore (livello 3, UC 0029). Se il campo reale avesse un nome o
  un'unità diversi da quelli assunti qui (`amount` in unità minori, `currency`, `receipt_url`), la
  mappatura andrà corretta in un punto solo — `PaddleWebhookEvent` — e il resto regge.
- **Il read-model risolve nome dell'app e della fascia una riga alla volta.** Con decine di righe è
  irrilevante; se la pagina crescerà (vedi la paginazione qui sopra) conviene una sola lettura con
  l'unione, come già fatto per la vetrina nella change `0076`.
