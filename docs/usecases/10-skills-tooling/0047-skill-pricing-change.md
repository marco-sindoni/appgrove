# UC 0047 — skill `pricing-change`

**Area**: 10-skills-tooling · **Fase**: 5 · **Stato**: 🟢 deciso (skill da implementare)
**Dipendenze**: UC [0022](../07-payments/0022-pricing-as-code-sincronizzazione.md) (pricing-as-code + sync)
**Fonte decisioni**: #09 H35/H36/H37 (immutabilità/grandfathering/sync), #09 K (fee)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [09-pagamenti](../../09-pagamenti.md), [07-payments/0022-pricing-as-code-sincronizzazione](../07-payments/0022-pricing-as-code-sincronizzazione.md)

## 1. Obiettivo / Scope
Creare la skill **`pricing-change`** (env-agnostica, workflow `new-change` → branch + PR, nessun dialogo diretto con Paddle) che
gestisce i **cambi di pricing successivi** al lancio.
**Incluso**: aggiungere tier, **cambiare prezzo** (gestendo immutabilità: **nuovo Price + archivia il vecchio**), cambiare limiti,
aggiungere mensile/annuale; far **decidere il grandfathering** (esistenti restano vs migrazione); mostrare la **fee effettiva**
(warning soft >10%); aggiornare il pricing-as-code che la pipeline (UC 0022) sincronizza.
**Escluso**: il pricing iniziale (è `new-application`, UC 0046); la pipeline di sync (UC 0022); il dialogo diretto con le API Paddle (lo fa la sync).

## 2. Attori & ruoli
- **Developer**: invoca `/pricing-change`, decide grandfathering, rivede la PR.
- **Skill** (tooling): co-pilota immutabilità/fee/grandfathering; produce il pricing-as-code aggiornato.
- **Pipeline** (UC 0022): applica il sync (sandbox al merge, production al tag).

## 3. Precondizioni
- Pricing-as-code + sync (UC 0022) in essere; catalogo con price esistenti (UC 0013).

## 4. Flusso principale
1. `/pricing-change` → tipo di cambio (nuovo tier / cambio prezzo / cambio limiti / aggiungi ciclo) (#09 H36).
2. **Immutabilità**: per un cambio prezzo, genera un **nuovo Price** + **archivia** il vecchio (mai mutare un price vivo) (#09 H35/H37).
3. **Grandfathering**: fa **decidere** se le subscription esistenti **restano sul vecchio** o **migrano** (#09 H36).
4. **Fee effettiva**: per ogni prezzo (mensile e annuale) calcola % fee + netto, **warning soft >10%**, spinge verso annuale (#09 K47/48/49).
5. Aggiorna il **pricing-as-code**; segue `new-change` → branch + **PR** all'utente; la **sync** (UC 0022) propaga a sandbox/prod.

## 5. Flussi alternativi / edge / errori
- **Cambio ad alto impatto** (prezzi): passa da PR (review/audit/rollback) (#09 H35).
- **Price con subscription attive**: non si cancella (grandfathering); si archivia e si crea il nuovo (#09 H37).
- **Niente editor runtime**: i cambi sono codice, non UI admin (admin = read-only/observability) (#09 H34).

## 6. Risorse & runbook
**File skill** `.claude/skills/pricing-change/`. **Output**: branch + PR con pricing-as-code aggiornato + nota grandfathering.
**Runbook**: `/pricing-change` → co-pilota → PR → merge (sync sandbox) → tag (sync production).

## 7. Dati toccati
Modifica il **pricing-as-code** (catalogo), non dati utente. La sync riempie gli ID Paddle per env (UC 0022). Manifest: N/A.

## 8. Permessi & gate
- **Invarianti**: catalogo platform-level; nessun impatto sull'isolamento tenant.
- **Gate**: workflow `new-change` (requisiti/commit/merge); decisione esplicita sul grandfathering; immutabilità enforced dalla sync.

## 9. Requisiti di test
Skill di tooling: verifica che produca un pricing-as-code valido (nuovo price + archivia, mai mutazione importo vivo) e che la
sync (UC 0022) lo applichi idempotente. Fee effettiva calcolata correttamente; warning >10%.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #09 H34/H35/H36/H37, K47/48/49.
- **DoD**:
  1. `pricing-change` gestisce nuovo tier/prezzo/limiti/ciclo con immutabilità (nuovo Price + archivia).
  2. Fa decidere il grandfathering; mostra fee effettiva (warning >10%).
  3. Segue `new-change` (branch+PR); la sync propaga a sandbox/prod.
  4. Test: pricing-as-code valido + sync idempotente.
