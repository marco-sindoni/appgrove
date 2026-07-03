# Implementation Log — Change 0027: Gate privacy/RoPA in `new-change` (UC 0031)

**Branch**: `change/0027-use-case-0031-gate-privacy-ropa-new-change`
**Aree**: `.claude/skills/new-change` · `tools/compliance` · docs (13, use case 0002/0056, indice) · CLAUDE.md
**Completata**: 2026-07-03

## File modificati

| File | Azione |
|---|---|
| `tools/compliance/privacy-scan.mjs` | Creato — scanner segnali privacy sul diff (CLI + funzioni esportate testabili) |
| `tools/compliance/test/privacy-scan.test.mjs` | Creato — 10 test node:test su diff sintetici |
| `tools/compliance/package.json` | Modificato — script `privacy-scan` |
| `.claude/skills/new-change/SKILL.md` | Modificato — hook privacy: da promemoria a gate UC 0031 |
| `.claude/skills/new-change/step-02-requirements.md` | Modificato — campo "Tocca dati personali?" rimanda al gate + registrazione MAJOR/MINOR |
| `.claude/skills/new-change/step-03-implement.md` | Modificato — checkpoint → **gate co-pilota** (scanner, classificazione assistita, art. 9/DPIA, guardrail pseudonimizzazione≠erasure, MAJOR/MINOR, sub-processor) |
| `.claude/skills/new-change/step-04-close.md` | Modificato — verifica gate eseguito (scan) prima del commit-consent |
| `docs/13-compliance-privacy.md` | Modificato — dec. 46: path lista sub-processor allineato a UC 0002 (`content/legal/subprocessors.<lang>.md`) |
| `docs/usecases/01-business-legal/0002-*.md` | Modificato — "Punti aperti": riporto classificazioni accumulate + seed lista sub-processor |
| `docs/usecases/04-platform-core/0056-*.md` | Modificato — "Punti aperti": canale notifica minor / preavviso 30gg |
| `docs/usecases/_INDEX.md` | Modificato — 0031 → ✅ |
| `CLAUDE.md` | Modificato — descrizione area compliance (+ test scanner UC 0031) |

## Cosa è stato fatto

Il rilevamento segnali del gate privacy è ora **deterministico**: `privacy-scan.mjs` analizza il diff (default:
merge-base(main, HEAD) → working tree, **inclusi i file non tracciati**; range git esplicito opzionale) e riporta i
segnali #13 C16 — DDL nelle migrazioni Flyway (`CREATE TABLE`/`ADD COLUMN`), nuovi campi in `src/main/java`, nuove
dipendenze (`pom.xml`/`package.json`) e host esterni in config (potenziale sub-processor), classificazioni toccate nei
manifesti (`retention`/`purpose`/`legal_basis`) — con report leggibile o `--json`, exit 1 se segnali (informativo, non
build-gate). La skill `new-change` invoca lo scanner in step-03 e, sui segnali, agisce da **co-pilota di
classificazione** (elicita → propone con motivazione → conferma → manifesto+RoPA+`@PersonalData`), con escalation
art. 9/DPIA (criteri art. 35/EDPB), guardrail pseudonimizzazione≠erasure, classificazione **MAJOR/MINOR** e
segnalazione **sub-processor**; step-04 verifica che il gate sia stato eseguito prima del commit-consent.

## Decisioni prese

- **Path lista sub-processor — direzione del fix invertita rispetto ai requirements**: i requirements prevedevano di
  correggere la skill verso `content/subprocessors.md` (dec. 46), ma il drill-down di UC 0002 (più recente e artefatto
  implementativo) specifica `content/legal/subprocessors.{lang}.md` in 5 lingue. Allineato quindi **doc #13 dec. 46 a
  UC 0002** e lasciato il path della skill (già coerente). Il drift era tra i due documenti, non nella skill.
- **Scanner con logica esportata e CLI con guard** (`import.meta.url`): i test importano le funzioni pure su diff
  sintetici, senza repo git temporanei.
- **File non tracciati inclusi nello scan di default** (via `git ls-files --others`): una migrazione nuova non ancora
  `git add` è il caso tipico durante una change; `git diff` da solo la perderebbe.
- **Euristiche = pavimento, non soffitto**: dichiarato esplicitamente in step-03 (lo scanner non vede es. un cambio di
  finalità implementato in codice; il co-pilota deve considerare anche segnali non meccanici).

## Invarianti appgrove

Nessuno toccato: solo skill/tooling/docs, nessun codice runtime. Il gate rafforza l'accountability (art. 5.2) e il
privacy-by-design (art. 25).

## Note per il revisore

- **Decisioni differite tracciate** (schema concordato "meccanismo ora, artefatto al suo UC"):
  1. **UC 0002** ("Punti aperti"): riporto delle classificazioni MAJOR/MINOR accumulate negli artefatti change →
     front-matter al primo rilascio dei legali; seed di `content/legal/subprocessors.<lang>.md` da dec. 45 + segnalazioni
     accumulate.
  2. **UC 0056** ("Punti aperti"): canale di notifica per minor e preavviso 30gg sub-processor (il gate oggi registra).
- L'enforcement CI bloccante `@PersonalData`↔manifesto **non è di questa change**: è già attivo da UC 0030 (change 0026).
- Lo scanner ha exit 1 sui segnali ma **non è cablato come check in `run-tests.sh`** (per design: è strumento del gate
  di skill, il diff da esaminare dipende dalla change); in `run-tests.sh compliance` girano i suoi **test**.
- Nessun contratto cross-area.

## Test

- **compliance** (`./run-tests.sh compliance`): **verde** — 17 test node:test (7 preesistenti + 10 nuovi dello scanner:
  un caso per tipo di segnale, falsi positivi esclusi — `src/test/java`, lockfile, `_config.yaml`, localhost, `docs/` —
  più parseDiff, report e caso pulito) + `npm run check` sui manifesti reali.
- **Gate privacy su questa change** (dogfooding, step-04): `npm run privacy-scan` → exit 0, "nessun segnale" (coerente:
  la change tocca solo skill/tooling/docs). **Gate privacy: nessun segnale.**
- Parte skill/docs: non applicabile — solo Markdown, nessun codice eseguibile oltre allo scanner già coperto.
- backend/frontend/infra non toccati → suite non richieste.

## Stato criteri di accettazione

- [x] Scanner: su fixture rileva i 4 tipi di segnale (migrazione/campo entità/dipendenza esterna/manifesto) e su diff
      pulito esce 0 senza segnali; output `--json` stabile.
- [x] `./run-tests.sh compliance` verde, test scanner inclusi automaticamente (`node --test`).
- [x] Skill aggiornata (step-02/03/04 + SKILL.md): gate co-pilota con scanner, art. 9/DPIA, MAJOR/MINOR, sub-processor
      con path canonico (allineato a UC 0002, v. Decisioni prese).
- [x] Punti differiti scritti in UC 0002 e UC 0056 (sezione "Punti aperti / decisioni differite").
