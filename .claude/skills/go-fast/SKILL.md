---
name: go-fast
description: >
  Implements a batch of user stories (use cases) from the appgrove catalog in
  sequence, hands-free: resolves the list from explicit story numbers or from a
  whole epic, then for each story creates a remote backup tag and delegates the
  whole implementation to a fresh-context subagent that runs new-change in FAST
  mode (no workflow gates, full ./run-tests.sh green before commit), writes AND
  runs the how-to-test.md manual-verification guide in the change folder, and
  returns a structured report; after the last story, one end-of-batch pass
  re-runs the non-visual steps of every guide of the batch against the final
  state of main, so that guides superseded by a later story fail instead of
  surprising the developer. The orchestrator stays lean (work list, backup tag,
  merge, push, end-of-batch pass, final report), so its context grows by a few
  lines per story instead of a whole implementation. Stops the loop on the first unrecoverable failure or
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
skill in **fast mode**, with a remote backup tag before every story, a manual-test guide produced *and run*
by every implementation, and one end-of-batch pass that re-runs all the batch's guides against the final state
of `main`. All commands run at the monorepo root `/Users/msindoni/Projects/appgrove`.

Your context is a scarce resource: a batch survives N stories only if each story adds a **report** to the
conversation, not an entire implementation (use-case reading, code, test runs). That is why the orchestrator
only coordinates — clean start, change id, backup tag, subagent launch, verification, merge, push — and the
heavy lifting happens in a subagent whose context is born empty and dies with the story.

Fast mode means the developer has waived the per-change workflow gates **for the whole batch** by invoking
this skill. The counterweights are non-negotiable: full `./run-tests.sh` green before every commit, the
complete `decisions.json` register per change, a remote restore point per story, and `how-to-test.md` so the
developer can verify manually afterwards — a guide whose non-visual steps are **actually executed**, first by
the change that wrote it and again by the end-of-batch pass (step 4), because a guide nobody ran is prose and
not a checklist. The autopilot **escalation stops remain active** (see `new-change`
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
   - **`how-to-test.md` belongs to `new-change` fast, not to an extra instruction here**: fast mode writes
     the guide **and runs its non-visual steps before committing it**, triaging every failure into "the guide
     is wrong" or "the product is wrong" (see `new-change`, `step-04-close.md`). The subagent must not
     re-invent it — it must **report** what fast mode did with it, including a guide committed unexecuted and
     the reason (the stack is not always available inside a batch). An unexecuted guide is not a failure: the
     end-of-batch pass below is exactly where it gets run;
   - **hard boundaries**: never merge, never push, never touch `main` — merge and push belong to the
     orchestrator;
   - **escalation behaviour**: on any escalation case (product/business direction, pricing/quotas,
     materially ambiguous personal-data classification, irreversible or outward-facing effects), do **not**
     commit, leave the branch and working tree as they are, and return outcome `escalation` with the exact
     question the developer must answer;
   - **the report contract**: the final message must be **only** a structured report with these fields —
     `esito` (`successo` | `guasto` | `escalation`), `change_id`, `branch`, `esito_suite` (full
     `./run-tests.sh` outcome), `decisioni_registrate` (entry count of `decisions.json`),
     `how_to_test` (path), `how_to_test_eseguita` (`si` | `no — <reason>`: whether the guide's non-visual
     steps were actually run before the commit, and if not why — the end-of-batch pass needs to know),
     `rimandi` (deferred cross-references written into use cases or `docs/_BACKLOG.md`, or "nessuno"),
     `dettaglio` (failure detail or escalation question; empty on success). No prose around it: the report is
     data for the orchestrator, not a message to a human.
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

## Step 4 — End-of-batch pass: RUN the batch's guides against the final state

**Runs once, after the last story is merged and pushed, before the final report.** Skip it only if the batch
implemented a single story (there is nothing to age against) or if the loop stopped early — a halted batch has
no "final state" to test against, and say so in the report.

### Why this step exists

The guides of a batch are the one artifact **nothing guards**. Automated tests protect the code: when a later
story removes a behaviour, the journey covering it breaks and must be rewritten. The prose of an already-closed
change is a file in an archive, so when a later story changes reality **nothing turns red** — and in a batch of
consecutive changes over the same subsystem, which is exactly what `go-fast` exists for, the drift is nearly
guaranteed. It happened over the epic-22 batch (`0088`–`0092`): the developer hit stale points three times, once
per verification session, each time having to investigate whether the defect was in the code or in the guide.

**The remedy is to run them, not to re-read them.** A re-reading pass produces a judgement; a run produces a
failure. Only a failure is evidence — and only a run finds what nobody suspected, like the `500` hiding behind
the last command of §9 of guide `0091`, unseen because no one had ever executed it.

### How

1. **Start the local stack once**: `./app-start.sh`. This is why the pass is at the end and not per story — the
   stack goes up one time, and `main` is in the state the developer will actually meet.
2. **For each `changes/<batch-change>/how-to-test.md` of this batch, in the batch's own order**, run every step
   whose outcome is observable without looking at a screen: database commands, API calls, queue/log/mailbox
   inspections, and every assertion about a row or a status code. Visual steps are not this pass's business —
   they stay for the developer, and their prose is checked only for the things a run cannot check (labels that
   no longer exist, screens that moved).
3. **Triage every failure into one of three, and say which**:
   - **superseded by a later story of this batch** — this is the ageing the pass exists for. Rewrite the point
     to the final truth, and note in the guide that the earlier behaviour was intermediate. The nastiest
     instance is a **perimeter guard** ("if you see it now, someone anticipated work"): after the batch it
     fires on nothing, accusing the *correct* work of the next change. Convert it, do not delete it silently;
   - **wrong from the start** — the categories that were never about ageing: a command that does not run, a
     wrong table or expected value, an undeclared prerequisite. Fix it, and if the same mistake appears in more
     than one guide of the batch, fix it in all of them: it is a habit, not an accident;
   - **a product defect** — the run found something real. Track it (owning use case or `docs/_BACKLOG.md`) and,
     if it is safe and in scope, fix it. **Never soften a guide to match a defect**: that hides it twice.
4. **Re-run after every correction that touches a non-visual step.** A fix that was never executed is a fix
   that was never verified — the very failure this whole step is about. Corrections to purely visual prose need
   no re-run; say so rather than implying otherwise.
5. **Commit the corrected guides on `main`**, one commit for the pass, listing per guide what was corrected and
   in which of the three categories. The batch's `decisions.json` files are already closed and must not be
   rewritten: the pass's findings live in its commit message and in the final report.
6. **Restore what the run changed.** Executing guides mutates the local stack (rows created, roles revoked,
   applications switched on). Bring it back — `./dev.sh seed` where that is enough — and **state in the report
   what you left behind** (test rows, an application left on, an account created): the developer works on that
   stack next.

### What to report

Per guide: run · not run (with the reason) · corrected in categories 1/2/3, with the count. Plus the product
defects found and where they were tracked. A pass that corrected nothing is a good outcome and must be said
plainly — not omitted, or the next reader will assume it never ran.

## Step 5 — Final report

When all stories are done (or the loop stopped), summarize in Italian from the collected subagent reports
and from the end-of-batch pass:

- stories implemented: story → change id → merge commit, link to each `how-to-test.md`;
- backup tags created (they stay on the remote — cleanup is the developer's choice, suggest
  `git push origin --delete <tag>` once verified);
- stories skipped and why (✅ already done, 🟠 pending decision, missing prerequisite);
- anything tracked as deferred during the changes (each report's `rimandi`; the per-change `decisions.json`
  files are the authority);
- **the end-of-batch pass** (step 4): which guides were run, what the triage found in each of the three
  categories, which product defects surfaced and where they were tracked, and the state the local stack was
  left in;
- the reminder that the **visual** part of the `how-to-test.md` checklists is the developer's remaining task —
  the non-visual part has already been run, twice: by each change and by the end-of-batch pass.

## Non-negotiables inherited from the constitution

Everything in `CLAUDE.md` applies unchanged inside the loop — and inside **each subagent**, which loads the
project constitution and the `new-change` skill on its own: Italian artifacts, decision registers per
change, deferred-decision tracking, privacy/RoPA gate, scaffold parity gate, `run-tests.sh` kept current.
go-fast adds speed by removing *waiting*, never by removing *evidence* — and adds endurance by keeping the
orchestrator's context lean, never by skipping a counterweight.

One counterweight is go-fast's alone, because only the orchestrator sees the **batch**: the guides of the
individual changes are the one artifact nothing guards, since a closed change's prose is an archive file and no
later change can turn it red. Step 4 is where that gap is closed — by running them, not by re-reading them.
