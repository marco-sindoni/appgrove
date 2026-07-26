# UC 0069 — Trial una-tantum per tenant×app

**Area**: 13-abbonamenti-self-service · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0026 (ciclo di vita subscription), UC 0024 (checkout), UC 0027 (applicazione entitlement & quota)
**Fonte**: R21 (Tabella residui _INDEX.md); docs/_BACKLOG.md §Trial una-tantum per tenant×app (GAP verificato 2026-07-26)
**Ultimo aggiornamento**: 2026-07-26

## 1. Obiettivo / Scope
Rendere la **prova gratuita di 14 giorni effettuabile una sola volta** nella storia del tenant, **per ciascuna app**. Se il
tenant prova un'app, disdice, e ritorna mesi dopo, non deve poter riavere la prova: solo sottoscrizione pagante, e
l'interfaccia deve rendere evidente che la prova è già stata usata.

Questo è un **gap verificato**: oggi il codice non si comporta così. La durata della prova è solo un attributo del tier
(`app_tier.trial_days`, alimentato dal listino `trialDays` nel pricing-as-code di UC 0022). Alla disdetta la riga
`subscription` diventa `canceled` ma **senza** `deleted_at`, e al ritorno il nuovo evento `subscription.created(trialing)`
riusa la stessa riga con un nuovo `trial_end` → **prova riconcessa**. In più il badge "14 giorni di prova gratis" (in
`Billing.tsx`) dipende solo da `tier.trialDays > 0`, quindi è uguale per tutti, senza memoria del tenant.

**Incluso**: (1) uno **storico "prova consumata"** per la coppia (`tenant_id`, `app_id`) immune al soft-delete e al riuso
della riga; (2) un **gate backend** che, per un (tenant, app) che ha già consumato la prova, neutralizza la prova alla
sottoscrizione successiva (pagante immediata, `trial_end` nullo) anche se il price di Paddle prevede il trial; (3)
l'**interfaccia** che sopprime il badge e rende evidente "prova già usata", tramite uno stato per-tenant.

**Escluso**: la semantica generale del trial→conversione (UC 0026); la meccanica del checkout (UC 0024); il calcolo del prezzo
(UC 0022).

## 2. Attori & ruoli
- **Utente owner del tenant**: avvia checkout/sottoscrizione; è colui per cui vale la regola una-tantum.
- **Backend `core`** (`CheckoutResource.start` e mapping webhook di UC 0025): applica il gate e scrive/legge lo storico.
- **Paddle**: il trial è configurato sul price lato Paddle; la neutralizzazione avviene **lato nostro**, non su Paddle.
- **Frontend backoffice** (`Billing.tsx` e catalogo `/tiers`): mostra o sopprime il badge in base allo stato per-tenant.

## 3. Precondizioni
- Utente autenticato con `tenant_id` nel token verificato.
- Catalogo tier/price sincronizzato (UC 0022); almeno un tier con `trial_days > 0`.
- Pipeline webhook attiva (UC 0025) che alimenta lo stato della `subscription`.

## 4. Flusso principale
1. L'owner apre il catalogo/piani di un'app mai provata: il read-model indica **prova disponibile** per questo tenant → il
   badge "14 giorni di prova gratis" è mostrato.
2. L'owner avvia il checkout: `CheckoutResource.start` verifica lo **storico prova** per (tenant, app). Non essendoci
   consumo, procede col trial.
3. Alla creazione (`subscription.created` con `status = trialing`) il consumer webhook, **la prima volta che imposta
   `trial_end`**, scrive la riga di storico "prova consumata" per (tenant, app). Questa scrittura è **non azzerabile** dalla
   disdetta.
4. Passano i 14 giorni: conversione automatica a pagamento (UC 0026), oppure l'owner disdice.
5. **Ritorno dopo la disdetta**: l'owner riapre i piani della stessa app. Il read-model indica **prova già usata** → il badge
   è **soppresso** e compare la dicitura "Prova già utilizzata".
6. L'owner avvia comunque il checkout: `CheckoutResource.start` (e/o il mapping webhook) vede lo storico e **azzera la prova**
   — sottoscrizione pagante immediata, `trial_end` nullo — anche se il price Paddle prevede il trial.

## 5. Flussi alternativi / edge / errori
- **App con `trialDays: 0`**: nulla da bloccare; il gate è un no-op, il badge non c'era comunque.
- **Prova disdetta al giorno 3**: da decidere se conta come "consumata" (vedi Punti aperti). L'ipotesi di lavoro è: la prova si
  considera consumata **appena iniziata** (primo `trial_end` impostato), a prescindere da quanto è durata.
- **Riuso della riga `subscription`**: lo storico è **separato** dalla riga `subscription` proprio per sopravvivere al riuso e
  al soft-delete; non si deduce dallo stato corrente della subscription.
- **Interazione con Paddle reale**: il trial vive sul price lato Paddle; noi non lo togliamo su Paddle, lo **neutralizziamo
  lato nostro** convertendo subito a pagante e ponendo `trial_end` nullo nella nostra `subscription`.
- **Catalogo `/tiers` oggi identico per tutti**: va introdotto uno **stato per-tenant** (es. campo `trialEligible`
  nell'entitlement/subscription read-model) perché il badge dipenda dal tenant e non solo dal tier.
- **Corsa fra due checkout concorrenti dello stesso tenant**: la scrittura dello storico deve essere idempotente/vincolata da
  unicità su (tenant, app) per non registrare due consumi né riaprire una finestra.

## 6. Schermate & stati
- **Catalogo piani / pagina Billing** dell'app:
  - *prova disponibile*: badge "14 giorni di prova gratis" sul tier con `trialDays > 0`; call-to-action "Inizia la prova".
  - *prova già usata*: badge soppresso; dicitura chiara **"Prova gratuita già utilizzata"** vicino al piano; la call-to-action
    diventa "Abbonati" (pagante immediato).
  - *loading*: skeleton dei piani mentre si risolve lo stato per-tenant.
  - *error*: fallback prudente → se lo stato per-tenant non è determinabile, **non** mostrare il badge (evita di promettere una
    prova che poi il gate backend neutralizza).
- **Copy chiave** (italiano): "14 giorni di prova gratis", "Prova gratuita già utilizzata", "Hai già usato la prova di questa
  app: puoi abbonarti direttamente", "Inizia la prova" / "Abbonati".
- Coerenza fondamentale: **UI e backend devono concordare**; il badge non deve mai promettere una prova che il gate backend poi
  toglie. La verità è il gate; la UI la riflette.

## 7. Dati toccati
- **Nuovo storico "prova consumata"**: tabella o colonna dedicata per (`tenant_id`, `app_id`), scritta la **prima volta** che
  `trial_end` viene impostato, **non** azzerata dalla disdetta né dal soft-delete della `subscription`. Chiave/unicità su
  (`tenant_id`, `app_id`). Porta `tenant_id` (varchar, discriminatore) e `app_id`; timestamp di primo consumo.
- **Lettura**: `platform.subscription` (`trial_end`, `status`), `platform.app_tier.trial_days`.
- **Read-model**: aggiunta di `trialEligible` (per-tenant) all'entitlement/subscription esposto al frontend.
- **Dati personali**: lo storico è un dato di **relazione contrattuale** (quale tenant ha consumato quale prova), non una
  categoria particolare; finalità = correttezza commerciale (una prova per app); base = esecuzione/gestione del contratto;
  retention = coerente col ciclo di vita dell'account (sopravvive alla singola subscription per definizione). Da riflettere nel
  manifesto dati dell'app e nel registro dei trattamenti (#13).

## 8. Permessi & gate
- **Invariante 1**: `tenant_id` letto solo dal token verificato; lo storico è scritto/letto con quel `tenant_id`, mai da input.
- **Invariante 2**: filtro row-level `WHERE tenant_id = :tid` sullo storico e sulla subscription.
- **Invariante 4**: log strutturati con `tenant_id`, `app_id`, `user_id` a ogni scrittura di consumo e a ogni neutralizzazione.
- **Gate**: il gate trial è **aggiuntivo** rispetto alla catena entitled→ruolo→quota; agisce in `CheckoutResource.start` e/o
  nel mapping webhook. È il backend a decidere; la UI riflette. Diritti sulla protezione dei dati personali esenti (#09 F31).

## 9. Requisiti di test
- **Integration (Testcontainers)**: primo trial scrive lo storico; disdetta → nuovo checkout della **stessa** app → la prova è
  neutralizzata (`trial_end` nullo, pagante immediato) e lo storico non viene riscritto/azzerato.
- **Soft-delete/riuso riga**: se la `subscription` viene riusata (nuovo `subscription.created`), lo storico resiste e il gate
  scatta comunque.
- **`trialDays: 0`**: nessun effetto collaterale (no-op).
- **Read-model**: `trialEligible` è `false` per un tenant che ha consumato, `true` per uno nuovo; due tenant diversi vedono
  stati diversi per la stessa app.
- **Security / isolamento cross-tenant**: il consumo di un tenant non influenza l'idoneità di un altro.
- **E2E Playwright (L2)**: badge presente al primo accesso, soppresso dopo consumo+disdetta+ritorno.
- **Verde prima del merge**: aree `backend` e `frontend` di `run-tests.sh`; check `@PersonalData`↔manifesto se si aggiungono campi.

## 10. Riferimenti & Definition of Done
- **Fonte**: R21 (Tabella residui _INDEX.md); docs/_BACKLOG.md §Trial una-tantum (GAP verificato 2026-07-26).
- **Storie collegate**: UC 0026 (trial→conversione), UC 0024 (checkout), UC 0027 (entitlement/quota), UC 0022 (pricing-as-code).
- **Definition of Done**:
  1. Storico "prova consumata" per (tenant, app), immune a soft-delete e riuso riga.
  2. Gate backend che neutralizza la prova ai ritorni (pagante immediato, `trial_end` nullo), coerente col trial Paddle sul price.
  3. Read-model `trialEligible` per-tenant; UI che sopprime il badge e mostra "prova già utilizzata".
  4. Test integration + read-model + security + E2E L2 verdi; manifesto dati aggiornato.

## Punti aperti / decisioni differite
- **DECISIONE DI PRODOTTO DA CONFERMARE** *(owner: questo UC 0069)*: la prova si considera **consumata appena iniziata** (anche
  se disdetta al giorno 3)? L'ipotesi di lavoro è sì (primo `trial_end` impostato = consumo), ma è una scelta commerciale che
  va confermata prima di implementare. Non forzarla in autopilot: è direzione di prodotto.
- **App con `trialDays: 0`**: confermato irrilevante (niente da bloccare); annotato per completezza.
- **Interazione con Paddle reale** *(gated #14)*: il trial è configurato sul price lato Paddle; la regola una-tantum si applica
  **lato nostro** (neutralizzazione a valle), senza modificare la configurazione del price su Paddle. Da verificare col client
  Paddle reale quando esisterà l'account.
- **Granularità dello storico**: valutare se registrare anche l'esito (convertito vs disdetto) per futuri KPI, senza però
  legare l'idoneità all'esito — l'idoneità dipende solo dal fatto che la prova sia iniziata.
