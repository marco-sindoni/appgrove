# Implementation Log — Change 0074: copertura end-to-end dentro il workflow delle skill

**Branch**: `change/0074-use-case-0094-copertura-e2e-workflow-skill`
**Use case**: [0094](../../docs/usecases/20-test-e2e-piattaforma/0094-e2e-platform-workflow-skill.md) — ultima storia dell'epica 20
**Aree**: skill (`.claude/skills/`), documenti di processo (`CLAUDE.md`, `docs/`), strumenti Node
(`tools/new-application`, `tools/drop-application`, `tools/scaffold-parity`, `tools/e2e-coverage`)
**Completata**: 2026-08-01
**Modalità**: fast — nessuna fermata di workflow; le risposte alle domande di approfondimento sono dell'agente e
sono tracciate una per una in [decisions.json](decisions.json) (18 voci, tutte marcate `(autopilot)` tranne la prima)

## File modificati

| File | Azione |
|---|---|
| `CLAUDE.md` | Modificato — nuova sezione «Registro di copertura end-to-end (non negoziabile)» |
| `docs/usecases/_TEMPLATE.md` | Modificato — sotto-sezione «Journey end-to-end di piattaforma» nel §9 |
| `docs/testing/README.md` | Modificato — sezione «Chi tiene vero il registro» + tabella di manutenzione aggiornata |
| `docs/_PARITA-SCAFFOLD.md` | Modificato — sezione «Le coppie confrontate» |
| `docs/usecases/20-test-e2e-piattaforma/0094-*.md` | Modificato — tre rimandi nei punti aperti |
| `docs/usecases/20-test-e2e-piattaforma/0092-*.md` | Modificato — rimando sull'instabilità di `A-ENTITLE` |
| `.claude/skills/new-usecase/SKILL.md`, `step-01-scaffold.md`, `step-02-detail.md` | Modificati — classificazione allo scaffolding + domanda sui journey |
| `.claude/skills/new-change/SKILL.md`, `step-03-implement.md`, `step-04-close.md` | Modificati — passo di copertura + varco di verifica |
| `.claude/skills/new-application/step-04-close.md`, `.claude/skills/drop-application/step-04-close.md` | Modificati — consegna e checklist |
| `tools/new-application/templates/platform-e2e/J-@@APP_UPPER@@.spec.ts` | **Creato** — modello del journey core-loop |
| `tools/new-application/templates/frontend-e2e/@@APP_ID@@.spec.ts` | Modificato — etichetta `[L2-@@APP_UPPER@@]` |
| `tools/new-application/lib/context.mjs`, `lib/edits.mjs`, `generate.mjs`, `generate-smoke.sh` | Modificati — segnaposto `APP_UPPER`, editor del registro, espansione del nuovo modello, verifica nel collaudo di livello 3 |
| `tools/drop-application/lib/plan.mjs`, `lib/unedits.mjs`, `test/unedits.test.mjs` | Modificati — l'inverso esatto + i suoi test |
| `tools/scaffold-parity/parity-check.mjs`, `parity.config.json`, `source-paths.json`, `test/parity-check.test.mjs` | Modificati — `soloFile`, controllo `importazioni`, coppia `platform-e2e`, percorsi-sorgente |
| `tools/e2e-coverage/test/check.test.mjs` | Modificato — quattro collaudi di processo |

## Cosa è stato fatto

La copertura end-to-end smette di essere un progetto una-tantum e diventa un prodotto del workflow. Le quattro
skill del ciclo di vita del monorepo la producono e la mantengono: `new-usecase` **classifica** lo use case nel
registro già allo scaffolding e ne fa dichiarare i journey nel drill-down; `new-change` esegue un **passo di
copertura** in implementazione (leggi → decidi → copri o rimanda → aggiorna) e lo **verifica** prima del varco di
commit; `new-application` genera il **journey core-loop di piattaforma** dell'app, etichetta il test di livello 2 e
scrive le due voci di registro; `drop-application` le toglie con un round-trip byte per byte.

Attorno a questo, i presidi che rendono l'obbligo reale: la parità del nuovo modello di journey contro il journey
vivo di `fatture` (`J-QUOTA`), i due percorsi-sorgente dichiarati, la verifica della copertura dentro il collaudo di
livello 3 dello scaffolding, e quattro collaudi di processo che dimostrano — in coppia rosso→verde — che il presidio
morde e che la via d'uscita è quella scritta.

## Decisioni prese

Sintesi; il registro completo è in [decisions.json](decisions.json). Le più portanti:

- **La classificazione di uno use case sta allo step-01, non allo step-02** (dec. 4): il controllo pretende che
  ogni file del catalogo sia classificato, quindi il solo scaffolding renderebbe rossa l'area `tooling`.
- **Il journey generato si salta finché il listino dell'app è `inactive`** (dec. 8). Non è prudenza ma un fatto
  verificato nel codice: `EntitlementAccess.granted()` nega l'accesso a ogni app non `active`, e
  `PricingSyncService` riporta lo `status` del listino sul catalogo. Senza guardia, **ogni app nuova nascerebbe
  con la suite rossa**. Il percorso si accende da solo quando il listino passa ad `active`.
- **Il journey generato non si aggancia ai testi dell'interfaccia** (dec. 9): identificativo di prova del modulo,
  ruolo del banner, codice del record, conteggio sul database. Il primo lavoro dopo lo scaffolding è riscrivere le
  stringhe: un journey agganciato a «New record» sarebbe rosso il giorno dopo per un motivo che non è il prodotto.
- **Due voci di registro per app, non una** (dec. 10), coerenti con la convenzione già in uso (`J-QUOTA` e
  `L2-FATTURE` sono percorsi distinti). Nella stessa mossa il modello di livello 2 acquisisce l'etichetta: estingue
  il debito per cui l'area `tooling` restava rossa apposta a ogni app nuova.
- **La parità del journey si fa su `soloFile` + moduli importati** (dec. 12): il corpo dei due test *deve*
  divergere, le importazioni no — sono l'analogo delle dipendenze del `pom.xml`.
- **Il collaudo di livello 3 verifica la copertura in modo strutturale, non eseguendo il journey** (dec. 13):
  servirebbe l'intero stack per un'app che è per costruzione inattiva. Divario tracciato.

## Invarianti appgrove

Nessuno toccato: la change non contiene una riga di codice eseguibile del prodotto (nessun servizio, nessun
frontend, nessuna infrastruttura). Il modello di journey generato **legge** il diritto d'uso dall'API di
piattaforma col token del tenant di prova — non ricava mai il tenant da altro che dal JWT emesso dall'accesso.

## Note per il revisore

- **Cambia il comportamento delle skill**, cioè gli strumenti di lavoro del progetto. Le modifiche sono chirurgiche
  e innestate nelle sezioni esistenti: nessun gate di workflow nuovo, nessuna fermata aggiuntiva. Il blocco esiste
  già ed è la suite rossa; la sezione nuova di step-04 verifica ciò che il comando non può vedere (che la domanda
  sia stata posta, che una storia evolutiva non resti esentata).
- **Un'app generata ora tocca `docs/testing/copertura-e2e.yaml`**: è il sesto file condiviso del generatore, con il
  suo inverso e il suo round-trip.
- **Instabilità osservata, non introdotta**: nella prima esecuzione della suite completa `A-ENTITLE` è fallito al
  primo tentativo (attesa scaduta sull'accesso dal browser) ed è passato al secondo. La riesecuzione mirata
  dell'area `platform` è 13/13 verde al primo colpo, e il diff non contiene codice eseguibile: tracciato come punto
  aperto di UC 0092, che possiede quel journey (dec. 17).
- **`_INDEX.md` non aggiornato**: la sua tabella copre i soli 60 use case base, nessuna riga per le storie
  evolutive — stesso comportamento delle change 0069–0073, divario già nel backlog (dec. 3).
- **Decisioni differite tracciate**: tre nei punti aperti di UC 0094 (voce `coperto` con test che si salta; journey
  generato mai eseguito; `usecases` segnaposto nelle voci generate), una in UC 0092 (instabilità di `A-ENTITLE`).

## Test

**`./run-tests.sh` completa: 8 aree su 8 verdi** (`backend`, `frontend`, `infra`, `compliance`, `tooling`, `smoke`,
`platform`, `site`), uscita 0. Riesecuzione mirata di `platform`: 13/13 al primo tentativo.

Test aggiunti:

| Dove | Cosa coprono |
|---|---|
| `tools/e2e-coverage/test/check.test.mjs` | Quattro **collaudi di processo**: superficie nuova senza voce di registro → rosso e, con la voce, verde; test senza etichetta → rosso; storia evolutiva ancora esentata `non-implementato` → rosso e, riclassificata, verde; use case scaffoldato non classificato → rosso e, classificato, verde |
| `tools/scaffold-parity/test/parity-check.test.mjs` | Lettura dei moduli importati; `soloFile` che restringe il confronto e fa emergere un import mancante; `soloFile` che punta a un file sparito |
| `tools/drop-application/test/unedits.test.mjs` | Round-trip dell'inverso sul registro; rifiuto del doppio innesto; la verifica di idempotenza ora **pretende** un contenuto di prova per ogni file condiviso (prima l'elenco era scritto a mano e un file nuovo poteva sfuggirle) |
| `tools/new-application/generate-smoke.sh` | Il collaudo di livello 3 verifica che l'app generata nasca col journey, con le due voci di registro e col controllo di copertura verde sulla copia usa-e-getta |

Varchi eseguiti: **privacy/RoPA** (`npm run privacy-scan` → nessun segnale), **registro di copertura**
(`node tools/e2e-coverage/check.mjs` → verde), **parità scaffold** (`parity-check.mjs` → verde;
`source-paths-scan.mjs` → nessun percorso-sorgente toccato), **registro decisioni** (18 voci, id progressivi).

## Stato criteri di accettazione

- [x] `_TEMPLATE.md` ha la sotto-sezione dei journey; `new-usecase` classifica (step-01) e interroga (step-02)
- [x] `new-change` ha il passo di copertura in step-03 e il varco di verifica in step-04; `CLAUDE.md` menziona il registro
- [x] Un'app generata nasce con journey etichettato, test di livello 2 etichettato e due voci di registro; `drop-application` le toglie e il round-trip resta byte per byte identico
- [x] La parità del modello di journey è sorvegliata e i percorsi-sorgente sono dichiarati
- [x] `./run-tests.sh` completa verde
- [x] `decisions.json` registra ogni scelta, marcata `(autopilot)`
- [ ] `_INDEX.md` aggiornato — **non applicabile**: nessuna riga per le storie evolutive (dec. 3, divario nel backlog)
