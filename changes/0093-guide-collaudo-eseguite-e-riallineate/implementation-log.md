# Implementation Log — Change 0093: Le guide di collaudo manuale si eseguono, non si scrivono soltanto

**Branch**: `change/0093-guide-collaudo-eseguite-e-riallineate`
**Aree**: `.claude/skills/` (`new-change`, `go-fast`) · `CLAUDE.md` · `docs/_BACKLOG.md`
**Data**: 2026-08-22
**Modalità**: autopilot
**Use case sorgente**: nessuno (change ad-hoc, origine [docs/_BACKLOG.md](../../docs/_BACKLOG.md))

## File modificati

| File | Cosa |
|---|---|
| `.claude/skills/new-change/step-04-close.md` | Sezione nuova: scrittura **ed esecuzione** della guida di collaudo, con il triage dei fallimenti e la forma obbligatoria. Sta prima del gate del commit. |
| `.claude/skills/new-change/SKILL.md` | La guida entra fra le contropartite della modalità fast; nuovo presidio nell'elenco dei gate; descrizione della skill allineata. |
| `.claude/skills/go-fast/SKILL.md` | Punto 4 del ciclo: la guida è di `new-change` fast, il subagente ne **riferisce** l'esito. Contratto del report esteso con `how_to_test_eseguita`. **Step 4 nuovo**: passata di fine lotto che *riesegue* le guide del lotto. Step 5 = resoconto finale. |
| `CLAUDE.md` | Paragrafo non negoziabile «La guida di collaudo si esegue, non si scrive soltanto»; lo schema del ciclo `go-fast` ora termina con la passata di fine lotto. |
| `docs/_BACKLOG.md` | La voce di processo si chiude con le decisioni prese, e resta aperto il solo punto escluso (controllo meccanico). |

## Cosa è stato fatto

Il presidio è **doppio**, con ruoli diversi, ed è questa la sostanza della change.

Nella singola change (`new-change`, modalità fast): la guida si scrive e i suoi passi **non visivi** si
**eseguono** prima di committarla. Ogni fallimento va discriminato — *la guida è sbagliata* si corregge, *il
prodotto è sbagliato* è un difetto — e **mai** si ammorbidisce una guida per farla combaciare con un difetto,
perché lo nasconderebbe due volte. Una guida non eseguita si può committare solo dichiarandolo, col motivo.

Nel lotto (`go-fast`, passata di fine lotto): le guide di **tutte** le change del lotto si **rieseguono** contro
lo **stato finale di `main`**, con lo stack acceso una volta sola. Non una rilettura: un'esecuzione. Ogni
fallimento è triagiato in tre categorie (superato da una storia successiva · sbagliato in origine · difetto del
prodotto), e una guida corretta vede i suoi passi non visivi **rieseguiti** se la correzione li ha impattati.

Più la forma obbligatoria che chiude per costruzione tre delle sei categorie di difetto: intestazione col commit
su cui la guida è scritta, comandi **completi e incollabili** con forma canonica dichiarata per la banca dati
locale, etichette **come si leggono a schermo**.

## Decisioni prese

Dieci voci in [decisions.json](decisions.json). Le tre che orientano tutto il resto:

1. **il presidio vive in `new-change`, non in `go-fast`** (voce 3) — `how-to-test.md` era nominata solo da
   `go-fast`, mentre CLAUDE.md la attribuiva alla modalità fast: `new-change fast` invocata da sola non
   produceva alcuna guida, contro la costituzione. Buco chiuso;
2. **la passata di fine lotto è un'esecuzione, non una rilettura** (voce 5) — il backlog proponeva una
   rilettura, ammettendo che «resta prosa non sorvegliata». Rieseguendo, l'invecchiamento *fallisce*;
3. **escluso il controllo meccanico** (voce 6) — su guide d'archivio produrrebbe rumore permanente e suite
   rossa per documenti che nessuno riscriverà. Tracciato, non forzato.

## Invarianti appgrove

Nessuno degli invarianti architetturali è in gioco: la change non tocca codice eseguibile, dati o
infrastruttura. Restano mantenute vere le due regole di processo pertinenti: **lingua italiana** degli artefatti
(la guida è per lo sviluppatore, e la forma obbligatoria lo ribadisce imponendo le etichette come si leggono a
schermo) e **tracciamento delle decisioni differite** (il punto escluso è scritto nel backlog, non lasciato in
conversazione).

## Note per il revisore

Due cose che vale la pena guardare, perché sono scelte e non conseguenze.

**Ho toccato CLAUDE.md.** Non era nel perimetro iniziale, ma la costituzione descriveva il ciclo `go-fast` come
«tag di backup → `new-change` fast → `how-to-test.md` → commit+merge+push»: lasciarla così avrebbe reso le due
skill formalmente in violazione del documento che le governa. La modifica è additiva e non toglie nulla.

**Il perimetro resta la modalità fast.** Le modalità classica e autopilot continuano a *non* produrre la guida,
perché non è una loro contropartita. Se domani si volesse la guida sempre, è una decisione a sé — cambia il
costo di ogni change, e va presa sapendolo.

## Test

**Non applicabili** al merito della change: tocca soltanto istruzioni di processo in Markdown (skill,
costituzione, backlog), nessun codice eseguibile.

Eseguito comunque, come previsto dai criteri di accettazione:

- `./run-tests.sh tooling` → **verde** (55 collaudi, l'area che sorveglia gli strumenti di processo);
- `node tools/e2e-coverage/check.mjs` → **verde**.

Copertura end-to-end: **nessun impatto** (voce 9 del registro) — nessuna superficie applicativa, nessun percorso
utente, nessun contratto. La domanda è stata posta e questa è la risposta, come pretende UC 0094.

La verifica sostanziale è stata di **coerenza fra i tre documenti**: verificato che la titolarità della guida sia
ora unica (nessun punto di `go-fast` chiede più di scriverla) e che la forma canonica del comando verso la banca
dati sia dichiarata identica in CLAUDE.md e nella skill.

## Stato criteri di accettazione

- [x] `new-change` fast produce la guida **ed esegue** i suoi passi non visivi prima del gate del commit;
      l'esito, compresa una mancata esecuzione col motivo, va in `decisions.json`.
- [x] Forma obbligatoria dichiarata: intestazione col commit, comandi completi e incollabili, forma canonica
      per la banca dati, etichette come si leggono a schermo.
- [x] `go-fast` esegue a fine lotto i passi non visivi di **tutte** le guide del lotto contro lo stato finale di
      `main`, triagia in tre categorie e riesegue dopo ogni correzione che li impatti; l'esito entra nel
      resoconto finale.
- [x] La voce del backlog riporta le decisioni prese e il solo punto rimasto aperto.
- [x] `./run-tests.sh tooling` verde.
