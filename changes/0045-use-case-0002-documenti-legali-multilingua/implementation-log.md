# Implementation log — Change 0045 (UC 0002)

**Branch**: `change/0045-use-case-0002-documenti-legali-multilingua` · **Modalità**: autopilot
**Aree toccate**: `content/legal` (nuova) · `tools/compliance` · `docs/compliance/manifests` · `run-tests.sh` · `package.json`
**Data**: 2026-07-25

## Cosa è stato fatto

### 1. `content/legal/` — documenti legali fonte unica (30 file)
Nuova radice pubblica `content/`. Prodotti i 5 componenti in 5 lingue (`en/it/fr/es/de`), **IT facente fede**, frontmatter
`version: 1.0.0` / `effective_date: 2026-07-25` / `lang`:
- `privacy.*` — Informativa art. 13-14: titolare, tabella categorie/finalità/basi giuridiche, sub-responsabili,
  trasferimenti (DPF+SCC per AWS), retention, diritti (`privacy@`), rimando cookie, minori, versioning.
- `terms.*` — T&C: erogazione servizio, **Paddle Merchant of Record** + Buyer Terms, rinnovi/disdetta, uso accettabile,
  proprietà intellettuale, DPA verso i clienti incorporato, limitazione responsabilità, legge applicabile.
- `refund.*` — vendite definitive + **recesso 14 gg** dai Paddle Buyer Terms, eccezioni (doppio addebito/malfunzionamento/
  addebito non autorizzato), come richiedere.
- `cookie.*` — solo tecnici essenziali + Plausible cookieless → **nessun banner** (sezione, non popup).
- `subprocessors.*` — **AWS + Plausible** (Paddle escluso = MoR/titolare autonomo), colonne nome/finalità/regione/
  categorie dati, preavviso 30 gg sui cambi. Linkato dalle Privacy Policy.

Identità titolare: fonte unica `content/legal/entity.yaml` + token `{{titolare.<campo>}}` nei testi (decisione dello
sviluppatore, `decisions.json` #11). Valori `DA COMPILARE` per ragione_sociale/forma/sede/piva (posseduti da UC 0001);
email e dominio noti valorizzati. Contratto di sostituzione in `content/legal/README.md`.

### 2. Controllo CI — `tools/compliance`
- `legal.mjs` (logica pura): parità 5 lingue per componente, frontmatter valido (`version` semver, `effective_date` ISO,
  `lang` coerente col nome file), **integrità referenziale dei token** verso `entity.yaml` (errore se orfano; avviso non
  bloccante se valore `DA COMPILARE`).
- `legal-check.mjs` (CLI): legge `content/legal/`, splitta il frontmatter (delimitatori `---`) con `parse()` di `yaml`
  (nessuna nuova dipendenza), `process.exit(1)` se rosso.
- `test/legal.test.mjs`: 13 test (rami verdi/rossi + helper `splitFrontmatter`/`parseFileName`/`extractTokens`/`flattenKeys`).
- Cablaggio: nuovo script `legal-check` in `package.json`; riga `npm run legal-check` in `run_compliance` di
  `run-tests.sh` (+ header aggiornati). Il job `compliance` della CI gira sempre → gate bloccante automatico.

### 3. Riconciliazione path sub-responsabili
`docs/compliance/manifests/platform.yaml`: `content/subprocessors.md` → `content/legal/subprocessors.<lang>.md` (IT+EN).
RoPA rigenerato (`npm run assemble`); freshness verde.

### 4. Riporto decisioni differite di UC 0002
- Classificazioni accumulate: solo **MINOR / piattaforma core** (change 0029/0030/0035/0037), nessuna MAJOR → baseline
  `1.0.0` senza bump divergenti. Nessun nuovo sub-responsabile (Cognito/SES = AWS; Mailpit = solo dev).
- Punti aperti di UC 0002 marcati **risolti**; nota di risoluzione aggiunta al file dello use case.

### 5. Rimandi tracciati (lavoro non anticipato)
Note aggiunte ai "Punti aperti" di: UC 0036 (renderer sito deve sostituire i token), UC 0037 (footer linka i legali +
check link), UC 0056 (rendering in-app sostituisce i token), UC 0001 (compilare `entity.yaml` alla monetizzazione).

## Test
- `./run-tests.sh compliance` → **verde**: 30 test unitari (inclusi i 13 nuovi), RoPA freshness ok, `legal-check` ok
  (4 avvisi non bloccanti attesi per i valori `DA COMPILARE`).
- Gate privacy UC 0031: `npm run privacy-scan` sul diff → **nessun segnale** (la change descrive trattamenti, non ne
  introduce di nuovi).
- Nessun test backend/frontend/infra: la change non tocca codice eseguibile di quelle aree.

## Note / limiti
- I token `{{titolare.*}}` restano letterali finché non esistono i renderer (UC 0036 sito, UC 0056 in-app) — atteso e
  documentato nel contratto di sostituzione.
- I testi sono bozze allo stato dell'arte: la **revisione legale** resta opzionale/pre-go-live
  ([_REVISIONE-LEGALE](../../docs/_REVISIONE-LEGALE.md) L2/L3/L13).
