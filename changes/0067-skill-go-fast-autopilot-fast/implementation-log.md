# Implementation Log — Change 0067: Skill `go-fast` + modalità `fast` di `new-change`

**Branch**: `change/0067-skill-go-fast-autopilot-fast`
**Aree**: `.claude/skills/` (go-fast nuova, new-change revisionata), `CLAUDE.md`
**Completata**: 2026-08-01
**Modalità**: autopilot — le risposte alle domande di approfondimento sono dell'agente e sono tracciate in
[decisions.json](decisions.json) (13 decisioni, 12 in autopilot); i tre gate classici rispettati.

## File modificati

| File | Azione |
|---|---|
| `.claude/skills/go-fast/SKILL.md` | Creato — skill orchestratrice del lotto di story |
| `.claude/skills/new-change/SKILL.md` | Modificato — tre modalità (`off`/`on`/`fast`), sezione "Fast mode", gate annotati, rilevazione e2e, suite completa in fast |
| `.claude/skills/new-change/step-01-init.md` | Modificato — domanda di modalità a tre opzioni; fast solo se dichiarata |
| `.claude/skills/new-change/step-02-requirements.md` | Modificato — gate requisiti: deroga fast (trasparenza sì, attesa no) |
| `.claude/skills/new-change/step-03-implement.md` | Modificato — ruolo fast in implementazione + rilevazione test e2e Playwright L2 |
| `.claude/skills/new-change/step-04-close.md` | Modificato — fast: suite completa obbligatoria, commit senza consenso solo a verde, merge al chiamante |
| `CLAUDE.md` | Modificato — sezione autopilot: tre modalità, deroga sanzionata fast, contropartite, skill go-fast |

## Cosa è stato fatto

1. **`new-change`**: introdotta la modalità **`fast`** — autopilot senza gate di workflow (niente revisione
   requisiti, niente consenso al commit), dichiarabile solo esplicitamente; contropartite: suite completa
   `./run-tests.sh` senza parametri verde prima del commit (mai commit su rosso, anche per change di soli
   documenti), registro decisioni integrale, riepiloghi dei gate comunque stampati, merge lasciato al chiamante.
   Fermate di escalation intatte in tutte le modalità. Aggiunta inoltre l'istruzione permanente di **rilevazione
   dei test end-to-end Playwright (L2)** per le change con superficie frontend (direzione UC 0093/0094).
2. **`go-fast`** (nuova): riceve story e/o epica (o le chiede con una domanda semplice in chat, senza
   `AskUserQuestion`); risolve e ordina secondo l'onda 2 (salta ✅ e 🟠 segnalando); per ogni story: **tag remoto
   `<change-id>-backup`** su `main` prima di partire → `new-change` fast → **`how-to-test.md`** (checklist di
   verifica manuale, prevalentemente visuale) committato sul branch → **commit+merge+push** → story successiva;
   si ferma al primo guasto con istruzioni di ripristino (che stampa ma non esegue).
3. **CLAUDE.md**: la costituzione recepisce la deroga — i gate restano dello sviluppatore, che in fast li
   rinuncia consapevolmente all'invocazione, con le contropartite elencate.

## Decisioni prese

Sintesi (registro completo in [decisions.json](decisions.json)): escalation attive anche in fast; merge/push a
carico del chiamante; story 🟠 saltate con segnalazione; `how-to-test.md` committato prima del merge; tag di
backup su `main` pre-change pushato su `origin`; fast mai desunta; visibilità dei gate conservata; suite completa
anche per change docs-only in fast; ripristino da backup stampato ma mai eseguito dalla skill; rilevazione e2e
frontend come istruzione permanente.

## Invarianti appgrove

Nessuno toccato direttamente (solo skill/documentazione). La modalità fast non indebolisce gli invarianti né i
gate privacy/RoPA e parità scaffold, che restano dentro ogni change fast.

## Note per il revisore

- La deroga fast modifica la **costituzione** (CLAUDE.md): è la formalizzazione di una scelta dello sviluppatore,
  dettata a specifica; i presidi di sicurezza (escalation, suite completa, backup, registro) sono la contropartita.
- `go-fast` non esegue mai `git reset --hard`/`push --force*`: stampa i comandi di ripristino, l'atto resta umano.
- Nessuna decisione differita da tracciare altrove.

## Test

Non applicabile — nessun codice eseguibile modificato (solo skill Markdown e documentazione). Gate privacy:
nessun segnale (privacy-scan exit 0). Gate parità scaffold: nessun percorso-sorgente toccato (scan exit 0).

## Stato criteri di accettazione

- [x] `new-change` documenta la modalità `fast` (mode question a tre valori, nessun gate di workflow, suite completa, commit senza consenso, merge al chiamante, escalation attive)
- [x] `.claude/skills/go-fast/SKILL.md` con: domanda preliminare in chat, epica→story in ordine onda 2 e salto 🟠, tag `<change-id>-backup` remoto, ciclo `new-change` fast, `how-to-test.md`, commit+merge+push per story, arresto al primo guasto
- [x] `CLAUDE.md` aggiornato (tre valori + deroga fast + contropartite)
- [x] `decisions.json` completo e coerente (13 voci)
