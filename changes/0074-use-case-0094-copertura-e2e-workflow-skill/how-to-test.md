# Come verificare a mano la change 0074 — copertura end-to-end dentro il workflow delle skill

**Use case**: [0094](../../docs/usecases/20-test-e2e-piattaforma/0094-e2e-platform-workflow-skill.md) · **Branch**: `change/0074-use-case-0094-copertura-e2e-workflow-skill`

Questa storia non aggiunge schermate: cambia **il comportamento delle skill** e il modo in cui il repository
si accorge di una copertura mancante. Le verifiche sono quindi di due tipi: leggere ciò che una skill dirà a chi
la userà (le skill sono istruzioni: si collaudano rileggendole nel punto in cui verranno applicate) e provare col
proprio terminale che i presidi mordono davvero.

Tutti i comandi si lanciano dalla radice del monorepo.

---

## 1. Il controllo di copertura è verde sul repository vero

| Azione | Risultato atteso |
|---|---|
| `node tools/e2e-coverage/check.mjs` | `✓ copertura e2e: docs/testing/copertura-e2e.yaml coerente con i test presenti nel repository.`, uscita 0 |
| `./run-tests.sh tooling` | area `tooling` verde: parità, livello 3 dello scaffolding, round-trip del de-generatore, listino, landing, copertura |

---

## 2. Il presidio morde: simula una change che dimentica il registro

| Azione | Risultato atteso |
|---|---|
| Crea un test finto: `printf "import { test } from '@playwright/test'\ntest('[L2-FINTO] prova', async () => {})\n" > frontend/apps/backoffice/e2e/finto.spec.ts` | — |
| `node tools/e2e-coverage/check.mjs` | **rosso** (uscita 1) con la regola `[etichetta]` e il messaggio `etichetta [L2-FINTO] assente dal registro — copertura fantasma` |
| Togli l'etichetta dal titolo (`test('prova', …)`) e rilancia il controllo | **rosso** con `non porta l'etichetta del percorso — attesa la forma test('[ID] …')` |
| `rm frontend/apps/backoffice/e2e/finto.spec.ts` e rilancia | di nuovo verde |

Se uno dei due rossi non compare, il presidio non morde e la storia non regge.

---

## 3. Le skill dicono la cosa giusta, nel punto giusto

Da leggere con gli occhi: sono le istruzioni che un agente seguirà.

| Azione | Risultato atteso |
|---|---|
| Apri `.claude/skills/new-usecase/step-01-scaffold.md` | Fra «Register in the master index» e il gate di revisione c'è la sezione **«Classify it in the end-to-end coverage registry»**: dice che la classificazione va nello stesso commit che crea il file, elenca le tre categorie di esenzione e chiude con `node tools/e2e-coverage/check.mjs`. Il riquadro 🛑 finale riporta anche la classificazione scelta |
| Apri `.claude/skills/new-usecase/step-02-detail.md` | C'è la sezione **«The end-to-end journey question»** con la domanda in italiano da porre allo sviluppatore, e la regola «un drill-down con superficie e senza risposta è incompleto» |
| Apri `docs/usecases/_TEMPLATE.md`, sezione 9 | C'è la sotto-sezione **«Journey end-to-end di piattaforma»** con le tre risposte ammesse (nuovo / estensione / esenzione) |
| Apri `.claude/skills/new-change/step-03-implement.md` | C'è la sezione **«End-to-end coverage step»** con i quattro passi (leggi → decidi → registra → il controllo conferma) e i due casi che una change dimentica: storia evolutiva ancora esentata, change solo backend |
| Apri `.claude/skills/new-change/step-04-close.md` | Prima del varco di commit c'è **«Verify the end-to-end coverage registry gate»**. Verifica che stia **prima** del riquadro 🛑 di consenso al commit: se finisse dopo, non varrebbe nulla |
| Apri `CLAUDE.md` | Dopo «Esecuzione dei test» c'è la sezione **«Registro di copertura end-to-end (non negoziabile)»** coi quattro doveri (use case nuovo, change, storia evolutiva, app nuova) |

**Controprova utile**: chiediti, leggendo la sezione di step-03, che cosa faresti se la tua change non toccasse
il frontend. La risposta deve essere lì e deve essere «rispondi comunque, "nessun impatto" è una risposta».

---

## 4. Un'app generata nasce coperta (il pezzo più importante)

Genera un'app usa-e-getta nel repository e guarda cosa produce. **Ricordati di rimuoverla alla fine.**

| Azione | Risultato atteso |
|---|---|
| `node tools/new-application/generate.mjs --app-id provax --metric cose --skip-infra` | Generazione riuscita. In fondo, il riquadro «DA COMPLETARE» ha un **punto 5 «Journey end-to-end»** che nomina `tools/platform-e2e/journeys/J-PROVAX.spec.ts` e spiega che il percorso si salta finché il listino è `inactive` |
| `sed -n '/app provax (voci/,/fine app provax/p' docs/testing/copertura-e2e.yaml` | Il blocco fra i due marcatori contiene i percorsi `J-PROVAX` (livello `platform`) e `L2-PROVAX` (livello `l2`), entrambi `coperto`, più le tre righe di commento «DA COMPLETARE» sugli `usecases` segnaposto |
| Apri `tools/platform-e2e/journeys/J-PROVAX.spec.ts` | Nessun `@@SEGNAPOSTO@@` rimasto; il titolo comincia con `[J-PROVAX]`; c'è la guardia `test.skip(…)` col motivo che cita il file di listino `pricing/provax.yaml` |
| `grep -n "^test(" frontend/apps/backoffice/e2e/provax.spec.ts` | Entrambi i test cominciano con `[L2-PROVAX]` |
| `node tools/e2e-coverage/check.mjs` | **verde**: l'app è nata coperta, senza che nessuno abbia toccato il registro a mano |
| `node tools/drop-application/remove.mjs --app-id provax --skip-ropa` | Rimozione riuscita |
| `git status --short` | **Nessuna modifica residua**: il registro è tornato com'era e il journey non c'è più. Se compare `docs/testing/copertura-e2e.yaml` modificato, il round-trip è rotto |
| `node tools/e2e-coverage/check.mjs` | di nuovo verde |

---

## 5. La parità del modello di journey è sorvegliata

| Azione | Risultato atteso |
|---|---|
| `node tools/scaffold-parity/parity-check.mjs` | Verde, con la nota delle 2 deviazioni già derogate |
| Aggiungi una riga `import { pollUntil } from '../helpers/api'` in cima a `tools/new-application/templates/platform-e2e/J-@@APP_UPPER@@.spec.ts` e rilancia | **rosso**: `imp:tools/platform-e2e/journeys/J-QUOTA.spec.ts#…  — importato dal modello, non dall'app #1` |
| Annulla la modifica (`git checkout -- tools/new-application/templates/platform-e2e/`) e rilancia | verde |
| `node tools/scaffold-parity/source-paths-scan.mjs --paths tools/platform-e2e/journeys/J-QUOTA.spec.ts` | Segnala il percorso-sorgente colpito e spiega le due vie d'uscita (aggiorna il modello / registra la deviazione) |

---

## 6. Documentazione coerente

| Azione | Risultato atteso |
|---|---|
| Apri `docs/testing/README.md` | C'è la sezione **«Chi tiene vero il registro (UC 0094)»** con la tabella delle quattro skill; nella tabella di manutenzione la riga «Nuova app» **non** dice più che il test nasce senza etichetta e che l'area resta rossa apposta |
| Apri `docs/_PARITA-SCAFFOLD.md` | C'è la sezione **«Le coppie confrontate»** con tre righe, e la spiegazione di `soloFile` e del controllo sulle importazioni |
| Apri `docs/usecases/20-test-e2e-piattaforma/0094-*.md`, in fondo | Tre rimandi nuovi in «Punti aperti / decisioni differite», ognuno con lo use case proprietario |

---

## Cosa NON si può verificare a mano qui

Il journey generato **non viene eseguito** da nessun collaudo: richiederebbe l'intero stack per un'app che nasce
con il listino `inactive` e il cui percorso si salterebbe comunque. La prima esecuzione vera è quella di chi porta
un'app ad `active`, con `tools/platform-e2e/run.sh --journey J-<APP>`. Il divario è scritto nei punti aperti di
UC 0094: è noto, non dimenticato.
