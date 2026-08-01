---
name: go-fast
description: >
  Implements a batch of user stories (use cases) from the appgrove catalog in
  sequence, hands-free: resolves the list from explicit story numbers or from a
  whole epic, then for each story creates a remote backup tag and delegates the
  whole implementation to a fresh-context subagent that runs new-change in FAST
  mode (no workflow gates, full ./run-tests.sh green before commit), writes a
  how-to-test.md manual-verification guide in the change folder and returns a
  structured report; the orchestrator stays lean (work list, backup tag, merge,
  push, final report), so its context grows by a few lines per story instead of
  a whole implementation. Stops the loop on the first unrecoverable failure or
  on any autopilot escalation case returned by the subagent (product direction,
  pricing, ambiguous personal data, irreversible external effects).
triggers:
  - /go-fast
tier: tier1
stack_aware: true
---

# appgrove — Go Fast

You are the **batch implementation orchestrator** of the appgrove monorepo. Given a list of user stories
(use cases from `docs/usecases/`) or a whole epic, you implement them **one after another** — but never in
your own context: each story is delegated to a **fresh-context subagent** that invokes the `new-change`
skill in **fast mode**, with a remote backup tag before every story and a manual-test guide after every
implementation. All commands run at the monorepo root `/Users/msindoni/Projects/appgrove`.

Your context is a scarce resource: a batch survives N stories only if each story adds a **report** to the
conversation, not an entire implementation (use-case reading, code, test runs). That is why the orchestrator
only coordinates — clean start, change id, backup tag, subagent launch, verification, merge, push — and the
heavy lifting happens in a subagent whose context is born empty and dies with the story.

Fast mode means the developer has waived the per-change workflow gates **for the whole batch** by invoking
this skill. The counterweights are non-negotiable: full `./run-tests.sh` green before every commit, the
complete `decisions.json` register per change, a remote restore point per story, and `how-to-test.md` so the
developer can verify manually afterwards. The autopilot **escalation stops remain active** (see `new-change`
SKILL.md): product/business direction, pricing/quotas, materially ambiguous personal-data classification,
irreversible or outward-facing effects beyond the sanctioned commit/merge/push flow. Inside a subagent an
escalation cannot become a dialogue — it becomes a **stop**: the subagent abstains from committing and
returns the question, and the loop halts (see "Failure handling").

## Step 1 — Acquire the work list

**Input forms accepted at invocation**: story numbers (`/go-fast 0095 0097`, `0095,0096`), an epic
(`/go-fast epica 21`, `/go-fast 21-catalogo-app-backoffice`), or a mix.

**If the invocation carries neither**, ask a **plain question in chat** — normal prose text, NOT
`AskUserQuestion` (the answer is free text; a multiple-choice prompt would add nothing):

> Quali user story devo implementare? Puoi darmi numeri di story (es. `0095 0097`), un'epica intera
> (es. `epica 21`), o una combinazione.

Then **STOP and wait** for the answer. This is the only planned question of the whole run.

## Step 2 — Resolve and order the stories

1. **Epic → stories**: read the epic's section in `docs/usecases/README.md` and collect its use cases.
2. **Order**: follow the **topological order of `docs/usecases/EPICS-WAVE-2.md`** (base use cases not listed
   there follow `docs/usecases/_INDEX.md`). Never invent an order: prerequisites first.
3. **Filter, reporting every exclusion**:
   - already implemented (✅ in the execution index) → skip, note "già implementata";
   - marked **🟠 (product decision pending)** → **skip and flag**: go-fast never takes product decisions
     (escalation rule applied to the loop). The final report lists them with the pending decision;
   - a requested story whose **dependencies are neither implemented nor earlier in this batch** → do not
     silently reorder the world: report the missing prerequisite and **ask the developer** whether to include
     it, skip the story, or proceed regardless (this is a scope question, not a design one).
4. **Confirm the plan in one message** (no consent wait — informative): the ordered list of story → planned
   change id, plus the skipped ones with reasons. Then start the loop.

## Step 3 — The loop (for each story, in order)

For story `YYYY` with slug from its use case file:

1. **Clean start**: `git checkout main && git pull origin main`; working tree must be clean (if not, stop and
   report — never build on top of unknown local state).
2. **Change id**: next `NNNN` from `changes/` → `CHANGE_ID="NNNN-use-case-YYYY-<brief-slug>"`.
3. **Remote backup tag — BEFORE anything else**:
   ```bash
   git tag "${CHANGE_ID}-backup" main
   git push origin "${CHANGE_ID}-backup"
   ```
   The tag freezes the pre-change state of `main` on the remote: the guaranteed restore point of the fast
   run (e.g. `0068-use-case-0095-pagina-app-catalog-backup`). If the tag cannot be pushed (no remote,
   auth failure), **stop the loop** — fast without a restore point is not sanctioned.
4. **Implement — in a fresh-context subagent, never inline**: launch **one** subagent with the `Agent`
   tool, `subagent_type: general-purpose` (it needs the full toolset: `Skill`, `Bash`, file edits),
   **synchronous** (`run_in_background: false` — the loop is sequential, the next story starts only after
   this one is merged). Do not implement anything of the story in your own context, and do not run a
   parallel copy of the work while the subagent runs. The subagent's prompt must state:
   - **the story**: use case number `YYYY` and the path of its file under `docs/usecases/` — the use case
     file is the requirements source;
   - **the change id**: `CHANGE_ID` as computed at point 2. The subagent verifies that `new-change` derives
     the **same** `NNNN` from `changes/`; a mismatch is a failure to report, not to repair;
   - **the job**: invoke the **`new-change` skill in FAST mode** for use case `YYYY` (`new-change` fast
     writes `requirements.md`, implements within its scope, detects and implements Playwright end-to-end
     needs for frontend surface, runs the **full** `./run-tests.sh` and commits on the change branch only
     when green — no questions asked);
   - **`how-to-test.md`**: after the implementation commit, write `changes/${CHANGE_ID}/how-to-test.md`
     **in Italian** and commit it on the change branch. Content: the **manual verification checklist** for
     this story — primarily **visual** walks (start the stack with `./app-start.sh`, navigate as the user:
     which pages to open, what must be visible, which states to force and how), plus the necessary
     **non-visual** checks (API calls to make, database rows to inspect, emails to find in Mailpit). Each
     item: action → expected result. Write what a human needs to *see with their own eyes*, not a copy of
     the automated tests;
   - **hard boundaries**: never merge, never push, never touch `main` — merge and push belong to the
     orchestrator;
   - **escalation behaviour**: on any escalation case (product/business direction, pricing/quotas,
     materially ambiguous personal-data classification, irreversible or outward-facing effects), do **not**
     commit, leave the branch and working tree as they are, and return outcome `escalation` with the exact
     question the developer must answer;
   - **the report contract**: the final message must be **only** a structured report with these fields —
     `esito` (`successo` | `guasto` | `escalation`), `change_id`, `branch`, `esito_suite` (full
     `./run-tests.sh` outcome), `decisioni_registrate` (entry count of `decisions.json`),
     `how_to_test` (path), `rimandi` (deferred cross-references written into use cases or
     `docs/_BACKLOG.md`, or "nessuno"), `dettaglio` (failure detail or escalation question; empty on
     success). No prose around it: the report is data for the orchestrator, not a message to a human.
5. **Verify the report — lightweight, deterministic**: before merging, trust but verify:
   - the report says `esito: successo` (anything else → "Failure handling" below; a `null`/absent report —
     subagent died or was skipped — counts as `guasto`);
   - branch `change/${CHANGE_ID}` exists and carries commits ahead of `main`;
   - the four artifacts exist in `changes/${CHANGE_ID}/`: `requirements.md`, `implementation-log.md`,
     `decisions.json`, `how-to-test.md`;
   - the working tree is clean.
   The suite is **not** re-run by the orchestrator: the subagent already ran the full `./run-tests.sh`
   inside `new-change` fast, and the backup tag is the safety net. Any check failing → treat as `guasto`.
6. **Merge and push** (go-fast owns this in fast mode; `how-to-test.md` is already committed by the
   subagent):
   ```bash
   git checkout main
   git merge --no-ff "change/${CHANGE_ID}" -m "chore: change/${CHANGE_ID}"
   git push origin main
   git branch -d "change/${CHANGE_ID}"
   ```
7. **Immediately proceed to the next story** — no pause between stories. Print a one-line progress marker:
   `✅ story YYYY → change NNNN mergiata e pushata (tag di backup: ${CHANGE_ID}-backup) — passo alla prossima`.

## Failure handling — stop, never limp on

The loop **stops at the first negative outcome** — a report with `esito: guasto` (red suite that could not
be fixed within the story's scope, implementation error, change-id mismatch, missing artifacts, dead
subagent), a failed orchestrator step (dirty tree, failed tag or push, verification failure), or a report
with `esito: escalation`. On stop:

- leave the current change branch **as is** for inspection (never delete work);
- do **not** start the next story;
- **on `guasto`** — report: which story failed, why (the report's `dettaglio` or the orchestrator check
  that failed), the state of the branch, and the restore instructions:
  ```bash
  # ripristino completo di main allo stato pre-change:
  git checkout main && git reset --hard "${CHANGE_ID}-backup" && git push --force-with-lease origin main
  # oppure, per ispezionare soltanto: git diff "${CHANGE_ID}-backup"...main
  ```
  (`--force-with-lease`, never plain `--force`; the restore itself is the **developer's call** — go-fast
  prints the commands, it does not run them.)
- **on `escalation`** — relay the subagent's question to the developer verbatim, in Italian, with the story
  and branch it comes from. The answer is the developer's; resuming the batch (this story or the remaining
  ones) is a fresh `/go-fast` invocation once the point is settled — the loop never answers an escalation
  in the developer's place.

## Step 4 — Final report

When all stories are done (or the loop stopped), summarize in Italian from the collected subagent reports:

- stories implemented: story → change id → merge commit, link to each `how-to-test.md`;
- backup tags created (they stay on the remote — cleanup is the developer's choice, suggest
  `git push origin --delete <tag>` once verified);
- stories skipped and why (✅ already done, 🟠 pending decision, missing prerequisite);
- anything tracked as deferred during the changes (each report's `rimandi`; the per-change `decisions.json`
  files are the authority);
- the reminder that the manual pass over the `how-to-test.md` checklists is the developer's remaining task.

## Non-negotiables inherited from the constitution

Everything in `CLAUDE.md` applies unchanged inside the loop — and inside **each subagent**, which loads the
project constitution and the `new-change` skill on its own: Italian artifacts, decision registers per
change, deferred-decision tracking, privacy/RoPA gate, scaffold parity gate, `run-tests.sh` kept current.
go-fast adds speed by removing *waiting*, never by removing *evidence* — and adds endurance by keeping the
orchestrator's context lean, never by skipping a counterweight.
