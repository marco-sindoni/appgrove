---
name: go-fast
description: >
  Implements a batch of user stories (use cases) from the appgrove catalog in
  sequence, hands-free: resolves the list from explicit story numbers or from a
  whole epic, then for each story creates a remote backup tag, runs new-change in
  FAST mode (no workflow gates, full ./run-tests.sh green before commit), writes a
  how-to-test.md manual-verification guide in the change folder, commits, merges to
  main and pushes — then immediately moves to the next story. Stops the loop on the
  first unrecoverable failure or on any autopilot escalation case (product
  direction, pricing, ambiguous personal data, irreversible external effects).
triggers:
  - /go-fast
tier: tier1
stack_aware: true
---

# appgrove — Go Fast

You are the **batch implementation orchestrator** of the appgrove monorepo. Given a list of user stories
(use cases from `docs/usecases/`) or a whole epic, you implement them **one after another** by invoking the
`new-change` skill in **fast mode** for each, with a remote backup tag before every story and a manual-test
guide after every implementation. All commands run at the monorepo root `/Users/msindoni/Projects/appgrove`.

Fast mode means the developer has waived the per-change workflow gates **for the whole batch** by invoking
this skill. The counterweights are non-negotiable: full `./run-tests.sh` green before every commit, the
complete `decisions.json` register per change, a remote restore point per story, and `how-to-test.md` so the
developer can verify manually afterwards. The autopilot **escalation stops remain active** (see `new-change`
SKILL.md): product/business direction, pricing/quotas, materially ambiguous personal-data classification,
irreversible or outward-facing effects beyond the sanctioned commit/merge/push flow.

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
4. **Implement**: invoke the **`new-change` skill in FAST mode** for use case `YYYY` (the use case file is
   the requirements source; `new-change` fast writes `requirements.md`, implements within its scope,
   detects and implements Playwright end-to-end needs for frontend surface, runs the **full**
   `./run-tests.sh` and commits on the change branch only when green — no questions asked).
5. **`how-to-test.md`** — write it in `changes/${CHANGE_ID}/how-to-test.md`, **in Italian**, and commit it on
   the change branch before merging. Content: the **manual verification checklist** for this story —
   primarily **visual** walks (start the stack with `./app-start.sh`, navigate as the user: which pages to
   open, what must be visible, which states to force and how), plus the necessary **non-visual** checks
   (API calls to make, database rows to inspect, emails to find in Mailpit). Each item: action → expected
   result. Write what a human needs to *see with their own eyes*, not a copy of the automated tests.
6. **Merge and push** (go-fast owns this in fast mode):
   ```bash
   git add "changes/${CHANGE_ID}/how-to-test.md" && git commit -m "docs(change/NNNN): how-to-test"
   git checkout main
   git merge --no-ff "change/${CHANGE_ID}" -m "chore: change/${CHANGE_ID}"
   git push origin main
   git branch -d "change/${CHANGE_ID}"
   ```
7. **Immediately proceed to the next story** — no pause between stories. Print a one-line progress marker:
   `✅ story YYYY → change NNNN mergiata e pushata (tag di backup: ${CHANGE_ID}-backup) — passo alla prossima`.

## Failure handling — stop, never limp on

The loop **stops at the first unrecoverable problem** (red suite that cannot be fixed within the story's
scope, implementation error, dirty tree, failed push, escalation case raised by `new-change` fast). On stop:

- leave the current change branch **as is** for inspection (never delete work);
- do **not** start the next story;
- report: which story failed, why, the state of the branch, and the restore instructions:
  ```bash
  # ripristino completo di main allo stato pre-change:
  git checkout main && git reset --hard "${CHANGE_ID}-backup" && git push --force-with-lease origin main
  # oppure, per ispezionare soltanto: git diff "${CHANGE_ID}-backup"...main
  ```
  (`--force-with-lease`, never plain `--force`; the restore itself is the **developer's call** — go-fast
  prints the commands, it does not run them.)

## Step 4 — Final report

When all stories are done (or the loop stopped), summarize in Italian:

- stories implemented: story → change id → merge commit, link to each `how-to-test.md`;
- backup tags created (they stay on the remote — cleanup is the developer's choice, suggest
  `git push origin --delete <tag>` once verified);
- stories skipped and why (✅ already done, 🟠 pending decision, missing prerequisite);
- anything tracked as deferred during the changes (the per-change `decisions.json` files are the authority);
- the reminder that the manual pass over the `how-to-test.md` checklists is the developer's remaining task.

## Non-negotiables inherited from the constitution

Everything in `CLAUDE.md` applies unchanged inside the loop: Italian artifacts, decision registers per
change, deferred-decision tracking, privacy/RoPA gate, scaffold parity gate, `run-tests.sh` kept current.
go-fast adds speed by removing *waiting*, never by removing *evidence*.
