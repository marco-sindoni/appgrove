# UC 0094 — Integrazione della copertura end-to-end nel workflow delle skill (`new-usecase`/`new-change`/`new-application`)

**Area**: 20-test-e2e-piattaforma · **Fase**: evo · **Stato**: 🟢 scritto (evo, da implementare)
**Dipendenze**: UC 0093 (registro di copertura), UC 0044 (`new-change` update), UC 0045 (skill `new-usecase`), UC 0046 (skill `new-application`)
**Fonte decisioni**: #10 (testing), CLAUDE.md (autopilot, Definition of Done, parità scaffolding)
**Ultimo aggiornamento**: 2026-08-01

## 1. Obiettivo / Scope

Fare della copertura end-to-end un **prodotto del workflow, non un progetto una-tantum**: le skill che governano il
ciclo di vita del monorepo devono **identificare, registrare e mantenere** le necessità di test end-to-end a ogni
passo. Dopo questo UC, ogni nuova funzionalità nasce con la sua voce nel registro (UC 0093) — coperta subito o
dichiarata `da-coprire` con proprietario — e la batteria si arricchisce nel tempo insieme al prodotto.

**Incluso**: le modifiche alle skill `new-usecase`, `new-change`, `new-application`, `drop-application` e ai loro
template/documenti di processo, descritte sotto.
**Escluso**: il registro e il check (UC 0093, prerequisito); la scrittura di nuovi journey per feature esistenti
(UC 0091/0092).

## 2. Attori & ruoli

- **Skill `new-usecase`**: in fase di drill-down fa emergere le necessità end-to-end della specifica.
- **Skill `new-change`** (classica o autopilot): in fase di implementazione decide la copertura e aggiorna il
  registro; il check di UC 0093 la tiene onesta.
- **Skill `new-application` / `drop-application`**: creano/rimuovono il journey core-loop dell'app via scaffolding.
- **Sviluppatore**: resta il titolare dei tre gate (requisiti, commit, merge) — invariato.

## 3. Precondizioni

UC 0093 implementato (registro + check in `tooling`). Suite di piattaforma attiva (UC 0090–0092).

## 4. Flusso principale

1. **`new-usecase` — la specifica dichiara i bisogni.** Il template `docs/usecases/_TEMPLATE.md` §9 "Requisiti di
   test" acquisisce una sotto-sezione strutturata **"Journey end-to-end di piattaforma"**: quali journey nuovi
   servono, quali esistenti vanno estesi, o l'esenzione motivata. La skill (step-02) pone la domanda esplicitamente
   durante la stesura; un drill-down con superficie frontend senza questa sezione è incompleto.
2. **`new-change` — l'implementazione salda i conti.** Nel processo della change entra un passo obbligatorio (parte
   del Definition of Done, accanto a `run-tests.sh` e `_INDEX.md`):
   - la change che tocca superficie frontend o percorsi end-to-end **aggiorna il registro** — voce nuova `coperto`
     (col journey implementato e taggato) oppure `da-coprire` con motivo e proprietario;
   - la decisione di copertura (cosa si copre ora, cosa si rimanda, a che livello) è **una decisione della change**:
     finisce in `decisions.json` (in autopilot: marcata `(autopilot)`, come da regola);
   - il check di UC 0093 in `tooling` rende il passo non aggirabile: registro incoerente = suite rossa = niente
     commit.
   In **autopilot** vale il principio 4 di CLAUDE.md già in vigore (ciò che si lascia indietro si traccia
   sistematicamente): questo UC gli dà la sede strutturata.
3. **`new-application` — ogni app nasce col suo journey.** I modelli-sorgente dello scaffolding acquisiscono il
   template del **journey core-loop di piattaforma** dell'app (derivato dal journey dell'app #1 `fatture`, come gli
   altri modelli): elenco → crea → limite quota → tag `[J-<app>]` → voce `coperto` nel registro generata dal
   de/generatore. La **parità** col journey vivo di `fatture` entra nel collaudo `tools/scaffold-parity` e nel
   registro delle deviazioni `docs/_PARITA-SCAFFOLD.md`.
4. **`drop-application` — l'inverso.** Il de-generatore rimuove il journey dell'app e la sua voce di registro
   (simmetria del round-trip genera→de-genera già collaudato).

## 5. Flussi alternativi / edge / errori

- **Change senza superficie frontend**: nessun obbligo; il check non la riguarda. La skill `new-change` deve però
  porsi la domanda (un percorso end-to-end può cambiare anche da una modifica solo backend, es. un nuovo stato
  della subscription) — la risposta "nessun impatto end-to-end" è legittima e va in `decisions.json`.
- **Copertura rimandata legittimamente** (feature dietro decisione di prodotto non matura): voce `da-coprire` con
  proprietario = lo use case che la possiede — mai il silenzio.
- **Conflitto col budget di tempo della suite** (UC 0090 §9): se un journey nuovo sfora il target, la change lo
  dichiara e aggiorna il target motivando — il tempo della suite è una risorsa governata, non un fatto subito.

## 6. Risorse & runbook

- `.claude/skills/new-usecase/` (step-02 + `_TEMPLATE.md`), `.claude/skills/new-change/` (passi del processo, DoD),
  `.claude/skills/new-application/` e `tools/new-application/templates/` (nuovo template journey),
  `.claude/skills/drop-application/` e `tools/drop-application/` (inverso), `tools/scaffold-parity/` (parità),
  `docs/_PARITA-SCAFFOLD.md` (registro deviazioni), CLAUDE.md (sezione "Esecuzione dei test" e DoD della change:
  menzione del registro di copertura).
- Runbook per l'agente: la sequenza-tipo dentro una change ("leggi il registro → decidi → implementa/rimanda →
  aggiorna registro → il check conferma").

## 7. Dati toccati

Nessun dato personale: modifiche a skill, template e documenti di processo.

## 8. Permessi & gate

Nessuna superficie runtime. I gate dello sviluppatore (requisiti, commit, merge) restano invariati — l'autopilot
risponde alle domande di copertura ma non rimuove i presidi (limiti invalicabili di CLAUDE.md). Il gate meccanico è
quello di UC 0093 (già in `tooling`).

## 9. Requisiti di test

- **Collaudo di processo su fixture**: una change simulata con superficie frontend e registro non aggiornato deve
  produrre `tooling` rosso (dimostrazione che il presidio morde).
- **Collaudo scaffolding**: il collaudo di livello 3 di `new-application` (genera un'app usa-e-getta ed esegue
  l'intera suite) include il journey generato: l'app finta nasce col suo `[J-<app>]` verde e la voce di registro;
  il round-trip di `drop-application` la rimuove senza residui.
- Parità: `tools/scaffold-parity` sorveglia il nuovo template journey contro `fatture`.

## 10. Riferimenti & Definition of Done

- **Decisioni**: CLAUDE.md (autopilot: principi 4–5; DoD della change; parità scaffolding), #10 (testing),
  UC 0093 (formato registro).
- **DoD**:
  1. `_TEMPLATE.md` e `new-usecase` aggiornati (sotto-sezione journey nel §9);
  2. `new-change` aggiornata: passo di copertura nel processo + DoD; menzione in CLAUDE.md;
  3. `new-application`/`drop-application` generano/rimuovono journey + voce di registro; parità sorvegliata;
  4. collaudi di processo e scaffolding verdi (`tooling`);
  5. `_INDEX.md` aggiornato dalla change.

## Punti aperti / decisioni differite

- **Skill minori** (`pricing-change`, `finalize-landing`): un cambio di pricing può alterare i limiti che i journey
  assertano (J-QUOTA). Se il collaudo mostrerà attrito, si valuterà un aggancio anche lì — differito a evidenza
  raccolta, tracciato qui.
- **Metriche di copertura** (percentuale UC coperti, tempo suite nel tempo): cruscotto possibile ma non necessario
  al processo; differito.
