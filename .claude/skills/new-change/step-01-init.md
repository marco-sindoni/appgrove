# Step 01 — Initialize Change

All commands run at the monorepo root `/Users/msindoni/Projects/appgrove`.

## FIRST ACTION — settle the execution mode (classic vs autopilot)

Before the change number, before the description, before anything else.

**1. Was the mode declared at invocation?** Autopilot counts as declared if the invocation says so plainly —
`/new-change autopilot`, "in autopilot", "modalità autopilot", "autopilota", "rispondi tu alle domande". If so,
acknowledge it in one line and skip the question.

**2. If it was not declared, ask it immediately** with a single `AskUserQuestion` prompt (the one deliberate
exception to the "no multiple-choice wizard" rule — it is a mode switch, not a design question):

- header: `Modalità`
- question: `Come vuoi condurre questa change: in autopilot (rispondo io alle domande di approfondimento e le accetto per tuo conto) o in modalità classica (rispondi tu a ogni domanda, come finora)?`
- option A — `Autopilot`: `Rispondo io alle domande seguendo l'opzione raccomandata, massimizzo il lavoro fatto in questo task senza anticipare quello successivo, e traccio come rimandi negli use case ciò che resta da fare. Ogni scelta finisce in decisions.json. I tre gate restano tuoi: rilettura e approvazione dei requisiti, consenso al commit, consenso al merge.`
- option B — `Classica (come finora)`: `Ti faccio le domande una alla volta, in prosa, e aspetto la tua risposta prima di procedere.`
- `multiSelect: false`

Do **not** guess the mode and do **not** proceed without it. Once settled, follow the corresponding rules in
`SKILL.md` ("Execution mode") for the whole change: the mode does not change mid-run unless the developer says so
(if they do, record the switch as a decision).

State the outcome in one line, e.g. `▶︎ Modalità: autopilot — rispondo io alle domande e traccio ogni scelta in decisions.json.`

## Determine next change number

```bash
ls changes/ 2>/dev/null | grep -E '^[0-9]{3,4}-' | sort | tail -1
```

Take the highest number found and add 1. **Pad to 4 digits.**
If `changes/` has no numbered folders, start at `0001`.
(The `{3,4}` regex tolerates any legacy 3-digit folders when computing the max; new folders are always 4-digit.)

## Ask for change description

Ask the developer (one question):
> "Describe the change in a few words (will be used as the branch and folder name):"

*(Autopilot: do not ask — derive the description from the invocation text and state the name you chose.)*

Convert the answer to kebab-case, max 40 characters.
Example: "add Cognito authorizer to notes API" → `add-cognito-authorizer-notes-api`

## Use-case-originated changes — naming variant

Ask the developer (one question):
> "Does this change implement a use case from `docs/usecases/`? If yes, give its number (YYYY)."

*(Autopilot: do not ask — look the answer up. If the invocation names a use case, use it; otherwise search
`docs/usecases/` and `docs/usecases/_INDEX.md` for a use case that plainly covers this work, and treat the change
as ad-hoc only if none does. Record the outcome, with the reasoning, in `decisions.json`.)*

This determines the **folder/branch naming convention** (two forms):

- **Normal change** → `NNNN-brief-description`
  (NNNN = next progressive number in `changes/`)
- **Use-case-originated change** → `NNNN-use-case-YYYY-brief-description`
  (NNNN = next progressive number in `changes/`; **YYYY** = the source use case's absolute number in `docs/usecases/`)

`NNNN` is **always** the running counter of the `changes/` folder (independent of YYYY). YYYY just embeds the source use
case so the change is traceable back to its spec. **Pad both NNNN and YYYY to 4 digits.**
Example: change #7 implementing use case 35 "checkout overlay" → `0007-use-case-0035-checkout-overlay`.

When a change comes from a use case, the `requirements.md` (step-02) must **link the source**
`docs/usecases/<area>/YYYY-*.md` (the use case lives in its area subfolder; see `docs/usecases/README.md`).

## Note the areas in scope

Identify which monorepo areas the change is expected to touch — this drives the test suites in
step-03/step-04. A change may span several:

- `infra/` (Terraform) → `terraform validate` / `plan`
- `frontend/` (React) → `npm test`
- `services/<app>/` (Quarkus) → `mvn test`

This is a best-guess at init; refine it in requirements.

## Create branch and folder

Detect the default branch instead of assuming `main`:

```bash
DEFAULT_BRANCH=$(git symbolic-ref --short refs/remotes/origin/HEAD 2>/dev/null | sed 's@^origin/@@')
DEFAULT_BRANCH=${DEFAULT_BRANCH:-main}

# CHANGE_ID is either "NNNN-brief-description" (normal)
# or "NNNN-use-case-YYYY-brief-description" (use-case-originated, see naming variant above)
CHANGE_ID="NNNN-brief-description"
git checkout "$DEFAULT_BRANCH"
git pull origin "$DEFAULT_BRANCH"   # only if a remote is configured
git checkout -b "change/$CHANGE_ID"
mkdir -p "changes/$CHANGE_ID"
```

## Seed the decision register `decisions.json`

Create `changes/$CHANGE_ID/decisions.json` right away — it is a change artifact like `requirements.md`, and it
must exist **before** the first question is answered, so that nothing has to be reconstructed later. Schema and
rules: see `SKILL.md` ("Decision register"). Seed it with the decisions already taken in this step — at minimum
the execution mode, plus the use-case binding if there was anything to decide there:

```json
[
  {
    "id": 1,
    "decision": "Modalità di esecuzione della change: autopilot — le domande di approfondimento hanno risposta dall'agente secondo l'opzione raccomandata, i consensi a commit e merge restano allo sviluppatore. (Oppure: modalità classica, richiesta dallo sviluppatore.)"
  },
  {
    "id": 2,
    "decision": "(autopilot) La change implementa lo use case 0046 (skill new-application): il testo dell'invocazione corrisponde allo scope descritto in docs/usecases/10-skills-tooling/0046-skill-new-application.md. Da qui la forma del branch NNNN-use-case-0046-…",
    "files": [
      { "file": "docs/usecases/_INDEX.md", "description": "Lo use case 0046 passa a 🟡 in corso all'apertura della change e a ✅ alla chiusura." }
    ]
  }
]
```

Write it as a plain JSON array (no wrapper object, no comments — it must parse). Keep it valid at every step:
after each append, `node -e "JSON.parse(require('fs').readFileSync('changes/$CHANGE_ID/decisions.json','utf8'))"`
must succeed. Ids are append-only: never renumber and never reorder existing entries.

## Aggiorna l'indice di esecuzione (solo use-case change)

If this is a **use-case-originated** change (you captured a `YYYY`), mark the use case **in corso** in the execution index
`docs/usecases/_INDEX.md`. The row is identified by its `[YYYY](…)` link; flip **only the last status cell**:

```bash
YYYY="0044"   # the source use case number (4 digits)
sed -i '' -E "/\[$YYYY\]\(/ s/\| (⬜|🟡|✅) \|$/| 🟡 |/" docs/usecases/_INDEX.md
```

(macOS `sed -i ''`.) This edit stays in the working tree and is committed with the change at step-04, where it is flipped
to ✅. For a **normal** change (no `YYYY`), skip this.

## Output

```json
{ "change_id": "NNNN-brief-description", "branch": "change/NNNN-brief-description", "mode": "autopilot | classic", "areas": ["frontend", "services/notes", "infra"] }
```

Confirm to developer (one line):
```
✅ Branch: change/NNNN-brief-description | Modalità: <autopilot|classica> | Aree: <list> | Now writing requirements...
```

Proceed to step-02-requirements.md.
