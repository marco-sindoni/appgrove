# UC 0026 — Ciclo di vita subscription (stati, upgrade/downgrade, dunning/grace, trial, cancellazione)

**Area**: 07-payments · **Fase**: 5 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0025](0025-pipeline-webhook.md) (webhook→subscription)
**Fonte decisioni**: #09 E (ciclo di vita), #05 (subscription)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [09-pagamenti](../../09-pagamenti.md), [05-persistenza-dati](../../05-persistenza-dati.md)

## 1. Obiettivo / Scope
Definire la **semantica del ciclo di vita** della subscription e la mappa **status → accesso** che alimenta l'entitlement derivato.
**Incluso**: stati `trialing/active/past_due/paused/canceled`; **upgrade** immediato (proration) / **downgrade** a fine periodo
(con gating `stock`); **dunning/grace** (past_due mantiene accesso, 2 settimane); **trial** con carta upfront (conversione
automatica); **cancellazione** = accesso fino a fine periodo (riattivabile); **mappa status→accesso**.
**Escluso**: l'ingest webhook (UC 0025), l'enforcement runtime dei gate (UC 0027), il self-service/portal (UC 0028); pause self-service (futuro #09 E28).

## 2. Attori & ruoli
- **Utente**: fa upgrade/downgrade, disdice/riattiva (self-service UC 0028).
- **Paddle**: proration, dunning (retry+email), trial.
- **Sistema**: deriva l'accesso dallo status corrente di `subscription`.

## 3. Precondizioni
- Pipeline webhook (UC 0025) che mantiene `subscription` aggiornata; catalogo tier/price (UC 0013/0022).

## 4. Flusso principale (semantica)
1. **Mappa status → accesso** (regola consolidata): **accesso se `status ∈ {trialing, active, past_due}`**; **no accesso se `∈ {paused, canceled}`** (#09 E29).
2. **Upgrade** (tier superiore): **immediato**, Paddle addebita la differenza proporzionale; limite superiore subito disponibile (#09 E22).
3. **Downgrade** (tier inferiore): **a fine periodo**, nessun rimborso; UX indica chiaramente "schedulato, attivo dal giorno X, fino ad allora tier attuale" (#09 E22).
4. **Natura metrica** (#09 E23): **flow** → downgrade sempre permesso (prossimo periodo riparte sotto il nuovo limite); **stock** → downgrade **gated** se lo stato eccede il target (blocco + remediation), poi programmato a fine periodo.
5. **Trial**: carta upfront, prova gratis, **conversione automatica** a pagamento (Paddle, trial sul price) (#09 E27).
6. **Dunning**: in `past_due` **accesso mantenuto** per la durata dei retry Paddle (**2 settimane**) + banner warning + email Paddle; esito finale negativo → canceled/paused → accesso off (#09 E26).
7. **Cancellazione**: `cancel_at = fine periodo`, accesso fino a scadenza (no rimborso), riattivabile prima della scadenza (#09 E25).

## 5. Flussi alternativi / edge / errori
- **"Accesso fino a fine periodo"**: lo status resta `active` con cambio programmato finché il periodo non scade (#09 E29).
- **Downgrade stock con stato sopra capacità**: gating a monte (UC 0054/0027); la singola app gestisce eventuali residui (es. read-only) (#09 E24).
- **Paused** (azione admin/Paddle): trattato come **no accesso** (#09 E28).
- **Out-of-order**: gli stati riflettono `occurred_at` (UC 0025).

## 6. Risorse & runbook  _(logica di dominio)_
La semantica vive nella **derivazione entitlement** (a partire da `subscription`) e nelle transizioni applicate dal consumer
webhook (UC 0025). **Runbook**: nessuna azione manuale ordinaria; admin può disabilitare app (UC 0021), non muta subscription a mano.

## 7. Dati toccati
`subscription` (status/period/cancel_at/trial_end/paddle_price_id). Nessun dato carta (MoR). **Accesso off ≠ dati cancellati**:
i dati restano secondo retention (#13 E), UX "abbonamento scaduto, riattiva/esporta". Manifest: billing/abbonamento (base contratto).

## 8. Permessi & gate
- **Invarianti**: entitlement **derivato** dallo status (nessuna tabella); fonte unica `subscription`.
- **Gate**: la mappa status→accesso è il cuore del gate 3 (entitled 402) della catena #09 dec.30 (applicato in UC 0014/0027). Diritti GDPR esenti (#09 F31).

## 9. Requisiti di test
- **L1/Integration**: transizioni per ogni evento; upgrade immediato vs downgrade a fine periodo; dunning grace 2 settimane; trial→conversione; cancel→fine periodo→canceled.
- **Security**: la mappa status→accesso è corretta per (tenant, app); nessun accesso con paused/canceled.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #09 E22/E23/E24/E25/E26/E27/E28/E29.
- **DoD**:
  1. Mappa status→accesso applicata (trialing/active/past_due = accesso; paused/canceled = no).
  2. Upgrade immediato (proration) / downgrade a fine periodo con gating stock; UX downgrade chiara.
  3. Dunning grace 2 settimane; trial carta upfront→conversione; cancel=accesso fino a scadenza, riattivabile.
  4. Entitlement derivato; test lifecycle (L1) verdi.

## Punti aperti / decisioni differite

### Stato dopo la change `0021-use-case-0026-…` (semantica di dominio backend)

**Fatto** (`services/core`, pura derivazione, nessuno schema nuovo):
- **Mappa status→accesso canonica** come *fonte di verità unica*: `SubscriptionStatus.grantsAccess()`
  (accesso `∈ {trialing, active, past_due}`); sanata l'omissione di `past_due` che era duplicata e
  incompleta in `AdminResource.ENTITLEMENTS_SQL` (ora `entitled` si deriva in Java dalla mappa ∧ app attiva).
- **Derivazione lifecycle** `SubscriptionLifecycle` (read-model che 0027/0028 consumeranno): fasi
  `TRIAL/ACTIVE/CANCELING/GRACE/ENDED` + `access` + `accessUntil` (cancel → fino a `cancel_at`,
  riattivabile azzerandolo; altrimenti fine periodo; null se ENDED). Tutto derivabile dallo schema attuale.
- **Test L1** puri (mappa completa + fasi) + regressione admin (`past_due` ora `entitled`).

**Differito** (con proprietario):
- **Enforcement runtime** del gate entitled (402) + risoluzione tetto quota dall'entitlement → **UC 0027**
  (il read-model `grantsAccess()`/`SubscriptionLifecycle` è il building block già pronto da consumare).
- **Avvio self-service** di upgrade/downgrade/disdetta/riattivazione e **UX downgrade** "schedulato,
  attivo dal giorno X" → **UC 0028**. La *surfacing* del cambio tier schedulato richiede inoltre la
  **persistenza del cambio schedulato** (colonna `scheduled_tier_id`/`scheduled_change_at` + mapping
  webhook `subscription.updated`), **non derivabile dallo schema attuale** → vedi nota in UC 0028. A
  livello di **accesso** un downgrade schedulato resta identico a `ACTIVE` (tier corrente fino a fine
  periodo), quindi non è una fase a sé.
- **Gating `stock`** del downgrade + nozione di **metrica flow/stock** (oggi inesistente nel codice) →
  **UC 0054/0027**.
- **Trial→conversione** e **2 settimane di dunning**: temporizzati da **Paddle** (che poi flippa lo
  status a `canceled`/`paused`); qui *nessun timer locale* — ci si fida dello status corrente.
