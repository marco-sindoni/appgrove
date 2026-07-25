# Change 0044 — Log di implementazione: skill `pricing-change` (UC 0047)

**Branch**: `change/0044-use-case-0047-skill-pricing-change` · **Modalità**: autopilot
**Aree toccate**: `tools/pricing-change/` (nuovo, codice eseguibile) · `.claude/skills/pricing-change/` (nuova skill) · `run-tests.sh` · `docs/usecases/` (indice + punti aperti UC 0022)

## Cosa è stato fatto

Realizzata la skill **`pricing-change`** — il gemello di `new-application` per i cambi di pricing **successivi** al
lancio — con la stessa architettura a due metà delle skill sorella (#09 H36): un **tool deterministico** per la
parte meccanica/numerica testabile, più un **co-pilota** in prosa per le decisioni che uno strumento non può
prendere.

### 1. Tool deterministico `tools/pricing-change/` (Node ESM, `node --test`)

- **`lib/fee.mjs`** — calcolo puro della **fee effettiva + netto** (#09 K46/K47): 5% + parte fissa $0,50 (tasso
  dollaro→euro assunto 1,0, documentato e prudenziale), avviso soft **strettamente** sopra il 10%. Le costanti
  riproducono esattamente gli esempi della decisione (€5/mese → 15%, €50/anno → 6%).
- **`lib/pricing.mjs`** — le quattro operazioni sul pricing-as-code (contratto YAML **congelato dalla change
  0019**): `addTier`, `addCycle`, `setLimits`, e il cambio prezzo nelle due vie dell'immutabilità
  (`changePriceInPlace` per un prezzo non ancora vivo; `changePriceNewTier` per un prezzo vivo → nuovo tier che
  clona il vecchio, lasciandolo intatto per il grandfathering). Usa la Document API di `yaml`, che **preserva i
  commenti** dei listini. `validate()` è un presidio di forma locale; il loader Java del core resta l'autorità.
- **`change.mjs`** — la riga di comando: `fee` + le quattro modifiche, con `--slug`/`--file`/`--dry-run`. Il
  comando `change-price` **rifiuta** di procedere senza una via esplicita (`--in-place` o `--new-tier`), per non
  indovinare l'immutabilità al posto della persona.

### 2. Skill co-pilota `.claude/skills/pricing-change/`

`SKILL.md` + quattro step: tipo di cambio → applica (con la fee davanti e la scelta della via) → gate abbonati
(grandfathering vs migrazione, **escalation**) → runbook e chiusura via `new-change`. Env-agnostica: scrive gli
YAML e lascia un branch; non parla col fornitore di pagamento, non sincronizza, non migra nessuno.

### 3. Integrazione e tracciamento

- `run-tests.sh`: `tools/pricing-change` aggiunto all'area **tooling** (con guardia di installazione della
  dipendenza, come `scaffold-parity`).
- `docs/usecases/_INDEX.md`: 0047 → ✅.
- `docs/usecases/07-payments/0022-…` (Punti aperti): i due item di proprietà UC 0047 marcati **risolti**;
  aggiunto il nuovo punto aperto **versione a livello di prezzo** (proprietà UC 0022, in coordinamento con UC
  0024) — vedi sotto.

## Decisione di progetto centrale — l'immutabilità e la via del nuovo tier

L'identità di un prezzo nel catalogo è la tripla `(slug, tier.key, billingCycle)` → UUID deterministico **senza
versione** (`CatalogIds.priceId`). Perciò un prezzo **vivo** non si può mutare nell'importo (il motore di sync lo
rifiuta), e il «nuovo Price + archivia il vecchio» di #09 H35 si mappa sul modello appgrove — dove `(tier ×
ciclo) = un prezzo` — come un **nuovo tier** che porta il nuovo prezzo, lasciando il vecchio per gli abbonati
esistenti (grandfathering per costruzione: la sync non archivia un tier con subscription attive). La scelta fra
«in loco» (bozza) e «nuovo tier» (vivo) dipende da un fatto che lo YAML non contiene (la presenza di un
`paddle_price_id` nel DB per-ambiente): perciò è una **decisione del co-pilota/sviluppatore**, non dedotta dal
file. Dettaglio in `decisions.json` (voci 4, 5, 9).

## Confine di scope

Non toccati: l'**engine** (identità/sync/DDL, di UC 0022, congelato dalla change 0019), `services/core`, infra,
frontend, e il **listino di ogni app reale** (`crm.yaml`/`fatture.yaml` invariati: cambiare i prezzi di un'app
reale è una decisione di prodotto, non «implementare la skill»). I test del tool usano YAML fixture.

## Punti aperti tracciati (regola CLAUDE.md)

In `docs/usecases/07-payments/0022-…` (Punti aperti): manca una **dimensione di versione a livello di prezzo**
che eviti di coniare un nuovo tier visibile a ogni cambio prezzo (e il coordinamento con UC 0024 per nascondere
ai nuovi clienti un tier archiviato-ma-grandfathered). È **lavoro sull'engine**, di proprietà di UC 0022, non
anticipato qui.

## Test

Area **tooling** (parte Node) — verde:

- `tools/pricing-change` — **18/18** (fee: esempi #09 K46, soglia soft, tasso sovrascrivibile, importi non
  validi; operazioni: le quattro modifiche, guardrail, immutabilità del cambio prezzo, conservazione dei
  commenti, resa inline dei limiti, round-trip di validità).
- `tools/scaffold-parity` 20/20 e `tools/drop-application` 10/10 — **invariati** dal cambio (confermato che nulla
  regredisce).
- Collaudo di **livello 3** (Docker, genera un'app reale) e `mvn -pl core test`: **non ri-eseguiti** perché il
  cambio non tocca i modelli di `new-application` né `services/core`; nessun listino reale è stato modificato.

## Gate privacy/RoPA (UC 0031)

Lo scanner segnala la nuova dipendenza `yaml@^2.8.0` come «potenziale sub-processor»: **falso positivo
euristico**. `yaml` è una libreria **locale** di parsing (già usata da `tools/compliance`), non un servizio
esterno, non tratta dati personali, non fa chiamate di rete. Classificazione: **non** è un responsabile esterno
del trattamento; nessun dato personale nel cambio; nessun manifesto/RoPA da aggiornare; **MINOR/non-applicabile**.
Caso non ambiguo → risolto in autopilot (decisione 11).
