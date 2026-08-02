# UC 0067 — Gestione abbonamento self-service (backoffice "Abbonamenti")

**Area**: 13-abbonamenti-self-service · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0026 (ciclo di vita subscription), UC 0028 (portale cliente self-service), UC 0027 (applicazione entitlement & quota), UC 0024 (checkout), UC 0020 (shell SPA backoffice)
**Fonte**: R4 (Tabella residui _INDEX.md); docs/_BACKLOG.md §Pagamenti (#09 G)
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Costruire la **sezione "Abbonamenti"** completa del backoffice: la pagina dove l'utente vede e governa da solo i propri
abbonamenti, senza aprire un ticket. È l'ombrello attorno a ciò che UC 0028 ha già aperto (la sessione del **Customer Portal**
di Paddle, il fornitore di pagamento che è anche **Merchant of Record**, cioè venditore ufficiale verso il cliente finale).

**Incluso**: vista di riepilogo per ogni abbonamento (stato, tier scelto, fine periodo corrente, eventuale cambio già
programmato); **upgrade e downgrade** con i controlli di gating della metrica `flow` (consumo a finestra) e `stock` (livello,
es. numero di posti); l'esperienza di **downgrade programmato** a fine periodo; **disdici** e **riattiva**; il **display
dell'uso quota** con i banner di soglia; il pulsante **"Gestisci pagamento e fatture"** che apre la sessione Customer Portal
generata server-side.

**Escluso**: la semantica del ciclo di vita e la mappa stato→accesso (UC 0026); il confine di enforcement runtime dei gate
402/429 (UC 0027); il checkout iniziale (UC 0024); la console admin (UC 0021); la pausa/ripresa (UC 0068, file fratello
`0068-pausa-ripresa-subscription.md`); la regola trial una-tantum (UC 0069, file fratello `0069-trial-una-tantum-tenant-app.md`).

## 2. Attori & ruoli
- **Utente owner del tenant**: unico ruolo che può cambiare tier, disdire e riattivare (azioni con impatto sul contratto).
- **Utente member**: vede lo stato ma non compie azioni di billing (gate di ruolo, vedi §8).
- **Backend `core`**: legge la `subscription`, valida i cambi contro il gating, invia il comando al provider, aggiorna il read-model quando torna il webhook.
- **Paddle** (Merchant of Record): applica proration sull'upgrade, dunning sui pagamenti falliti, ospita il Customer Portal.
- **Sistema webhook** (UC 0025): riconcilia lo stato reale dell'abbonamento dopo ogni azione.

## 3. Precondizioni
- Utente autenticato; `tenant_id` presente nel token verificato.
- Almeno una `subscription` esiste per il tenant (creata dal checkout UC 0024 o da un trial).
- Shell del backoffice montata (UC 0020) con la voce di menù "Abbonamenti".
- Read-model `GET /me/subscriptions` disponibile (introdotto in UC 0028): ritorna tutte le subscription, anche non attive.
- Provider raggiungibile: stub locale in sviluppo/test, Paddle reale in produzione (quest'ultimo gated da #14).

## 4. Flusso principale
1. L'utente apre **Abbonamenti**: la pagina chiama `GET /me/subscriptions` e mostra una card per app.
2. Ogni card riepiloga: **stato** (in prova / attivo / pagamento in ritardo / disdetta programmata / scaduto), **tier**
   corrente, **fine periodo**, ed eventuale **cambio programmato** ("dal 12 agosto passi a Base").
3. L'utente sceglie **Cambia piano**: si apre un selettore dei tier disponibili per quell'app con prezzo e limiti.
4. **Upgrade** (tier superiore): applicato **subito**; il backend invia il comando, Paddle addebita la differenza
   proporzionale, il limite più alto è disponibile appena il webhook conferma. La card mostra transitoriamente "in aggiornamento".
5. **Downgrade** (tier inferiore): **programmato a fine periodo**; la card spiega "attivo dal giorno X, fino ad allora resti
   sul tier attuale". Nessun rimborso.
6. **Uso quota**: la card mostra il consumo corrente rispetto al limite del piano (es. "73 su 100") con banner quando si
   supera la soglia di attenzione.
7. **Disdici**: imposta la disdetta a fine periodo; l'accesso resta fino alla scadenza; la card diventa "disdetta programmata".
8. **Riattiva**: annulla la disdetta programmata prima della scadenza; la card torna "attivo".
9. **"Gestisci pagamento e fatture"**: il backend genera server-side una sessione del Customer Portal e apre Paddle (metodo di
   pagamento e fatture/ricevute, che sono di competenza del Merchant of Record).

## 5. Flussi alternativi / edge / errori
- **Downgrade `stock` sopra capacità**: se il tier target ha meno posti di quelli in uso, il selettore **blocca** la scelta e
  mostra una remediation ("hai 8 posti occupati, il piano Base ne prevede 5: liberane 3 per procedere"). Nessun comando inviato.
- **Downgrade `flow`**: sempre permesso; il nuovo limite vale dal prossimo periodo.
- **Pagamento fallito (dunning)**: lo stato passa a "pagamento in ritardo"; banner arancione persistente con invito ad
  aggiornare la carta dal portale. L'accesso resta per la finestra di grazia (2 settimane, gestita da Paddle, UC 0026).
- **Abbonamento scaduto**: la card offre **riattiva** e, in parallelo, **esporta/elimina i dati** — i diritti sulla protezione
  dei dati personali restano sempre esercitabili, esenti dai gate (#09 F31).
- **Errore del comando al provider**: la mutazione risponde `problem+json`; la UI mostra un messaggio non distruttivo e lo stato
  resta invariato finché il webhook non conferma (nessun ottimismo che diverga dal read-model).
- **Cambio non ancora riconciliato**: se il webhook tarda, la card resta in "in aggiornamento" con polling breve; non si
  presenta come completato finché la `subscription` non riflette il nuovo tier.

## 6. Schermate & stati
- **Pagina Abbonamenti**: lista di card, una per app abbonata.
  - *loading*: skeleton delle card.
  - *empty*: nessun abbonamento → messaggio "Non hai ancora abbonamenti" + link alla vetrina/catalogo per attivarne uno.
  - *error*: banner "Non riusciamo a caricare i tuoi abbonamenti" con retry.
  - *success*: card con stato colorato, tier, fine periodo, uso quota, azioni.
- **Modale "Cambia piano"**: elenco tier con prezzo/limiti; badge "consigliato"; il tier corrente è marcato; i tier non
  ammissibili (downgrade stock sopra capacità) appaiono disabilitati con tooltip di spiegazione.
- **Conferme**: ogni azione con impatto (upgrade con addebito, downgrade, disdici) passa da una conferma esplicita che
  riepiloga cosa succede e quando ("il downgrade sarà attivo dal 12 agosto").
- **Banner**: soglia quota ("stai per raggiungere il limite del piano"); pagamento in ritardo (dunning); abbonamento scaduto
  (riattiva / esporta dati).
- **Copy chiave** (italiano): "Attivo fino al …", "Disdetta programmata per il …", "Dal … passi a …", "Gestisci pagamento e
  fatture", "Limite raggiunto: passa a un piano superiore".

## 7. Dati toccati
- **Lettura**: `platform.subscription` (`status`, `app_tier_id`, `current_period_end`, `cancel_at`, `scheduled_tier_id`,
  `scheduled_change_at`, `trial_end`), `platform.app_tier` (`limits`, `features`, `name`), `platform.app_price`
  (`amount`, `currency`, `billing_cycle`).
- **Scrittura logica**: i cambi non modificano la riga a mano; il comando va al provider e la `subscription` è aggiornata dal
  consumer webhook (UC 0025). Il downgrade programmato scrive `scheduled_tier_id` + `scheduled_change_at`.
- **Uso quota**: il consumo corrente è **applicativo** (contratto di lettura usage per-app, vedi Punti aperti); il piano e i
  suoi limiti vengono da `app_tier.limits`.
- **Dati personali**: nessun dato nuovo. Il metodo di pagamento e le fatture sono in capo a Paddle (Merchant of Record). La
  base del trattamento dell'abbonamento è l'esecuzione del contratto; retention secondo il manifesto billing (#13).

## 8. Permessi & gate
- **Invariante 1**: `tenant_id` letto solo dal token verificato, mai dal corpo o dai parametri della richiesta.
- **Invariante 2**: ogni query è row-level, `WHERE tenant_id = :tid`; si gestisce esclusivamente l'abbonamento del proprio tenant.
- **Invariante 4**: ogni log della sezione porta `tenant_id`, `app_id`, `user_id`.
- **Catena dei gate**: la pagina rispetta la catena entitled → ruolo → quota. Le azioni di billing (cambia piano, disdici,
  riattiva, apri portale) sono riservate al **ruolo owner** (`@RolesAllowed`); un member le vede disabilitate.
- **Diritti sulla protezione dei dati personali**: sempre disponibili, anche ad abbonamento scaduto, esenti dai gate (#09 F31).

## 9. Requisiti di test
- **Integration (Testcontainers)**: `GET /me/subscriptions` ritorna tutte le subscription del tenant con stato e cambio
  programmato; upgrade produce cambio immediato, downgrade produce `scheduled_tier_id`/`scheduled_change_at`; disdici imposta
  `cancel_at`, riattiva lo azzera.
- **Gating**: downgrade `stock` sopra capacità viene rifiutato (`TierChangePolicy`); downgrade `flow` accettato.
- **Security / isolamento cross-tenant**: un utente non può leggere né mutare l'abbonamento di un altro tenant (il `tenant_id`
  del token vince sempre).
- **E2E Playwright (L2)**: percorso vedi → cambia piano (upgrade/downgrade) → disdici → riattiva → apri portale (mockato in dev);
  banner dunning e banner scaduto visibili nei rispettivi stati.
- **Verde prima del merge**: le aree `frontend` e `backend` di `run-tests.sh`.

## 10. Riferimenti & Definition of Done
- **Fonte**: R4 (Tabella residui _INDEX.md), docs/_BACKLOG.md §Pagamenti (#09 G).
- **Storie collegate**: UC 0026 (ciclo di vita), UC 0027 (enforcement), UC 0028 (portale), UC 0024 (checkout), UC 0020 (shell).
- **Definition of Done**:
  1. Pagina "Abbonamenti" con card per app: stato, tier, fine periodo, cambio programmato, uso quota.
  2. Upgrade immediato / downgrade programmato con gating `flow`/`stock`; conferme esplicite.
  3. Disdici/riattiva; banner dunning e banner scaduto (con esporta/elimina dati).
  4. Pulsante "Gestisci pagamento e fatture" che apre il Customer Portal server-side.
  5. Test integration + gating + security + E2E L2 verdi; log strutturati completi.

## Punti aperti / decisioni differite
- ~~**Consumo quota in tempo reale nel banner**~~ — **chiuso a metà** dalla change `0082`. L'uso **a giacenza** (posti occupati
  e simili) arriva ora dalla proiezione `platform.app_usage_stock` (UC 0054), è esposto dal read-model `/me/subscriptions` e la
  card mostra "8 su 10" con barra e avviso di soglia.
- **Consumo delle metriche "a finestra"** *(owner: questo UC 0067, condiviso con UC 0027)* — resta aperto. Per le metriche che si
  azzerano a ogni periodo (es. fatture al mese) `core` non conosce il consumo corrente: la proiezione UC 0054 riguarda solo la
  giacenza. Per quelle metriche la card mostra il **solo tetto del piano**, non "73 su 100". Serve estendere il contratto di
  riporto d'uso app → core al consumo a finestra (nome tabella/metrica e finestra di competenza sono la parte da decidere:
  un contatore a finestra va azzerato o riportato cumulativo?).
- ~~**Gate `stock` del downgrade contro l'uso reale**~~ — **chiuso** da UC 0054 (comando) e dalla change `0082`, che espone anche
  in anticipo i piani non ammissibili (`blockedTiers`) così la finestra di cambio piano li disabilita spiegando il perché,
  invece di lasciarli cliccabili e mostrare un rifiuto 409.
- **Implementazione reale Paddle** *(gated #14)*. I metodi del provider Paddle per cambio tier/cancel/resume/portal restano
  non implementati finché non esiste l'account Paddle (bloccato da #14); lo stub locale copre sviluppo e test.
- **Riconciliazione del netto incassato**: l'osservabilità del netto (al netto delle fee Paddle) non è di questa sezione →
  appartiene a UC 0071 (file fratello `0071-riconciliazione-netto-revenue.md`).
