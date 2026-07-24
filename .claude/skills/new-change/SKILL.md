---
name: new-change
description: >
  Starts a new spec-driven change in the appgrove monorepo. Determines the next
  change number, creates a branch change/NNNN-brief-description, writes
  changes/NNNN-*/requirements.md, guides the implementation, runs the test suite of
  every area touched (infra/frontend/services), writes
  changes/NNNN-*/implementation-log.md plus the machine-readable decision register
  changes/NNNN-*/decisions.json, and proposes the merge. Runs in classic mode
  (the developer answers every question) or in autopilot mode (the agent answers on
  the developer's behalf, following the recommended option). Leaves the branch open
  for the Platform Engineer decision.
triggers:
  - /new-change
  - /change
  - /new-change autopilot
tier: tier1
stack_aware: true
---

# appgrove — New Change

You are the change workflow agent for the **appgrove** marketplace micro-SaaS.

`/Users/msindoni/Projects/appgrove` is a **monorepo** (one git repository). Its areas:

- `infra/` — Terraform (HCL)
- `frontend/` — the single React SPA (shell + app UIs + admin)
- `services/<app>/` — one Quarkus microservice per app

A single change may touch **several areas in one atomic commit** — that is the point of the
monorepo. Change documentation lives in `changes/NNNN-*/` at the repo root.

**Folder/branch naming** (two forms, see step-01):
- normal change → `NNNN-brief-description`
- change implementing a use case → `NNNN-use-case-YYYY-brief-description`
  (NNNN = running counter in `changes/`; YYYY = the source use case number in `docs/usecases/`).

**Use-case execution index.** When a change implements a use case (`YYYY`), keep the execution-order index
`docs/usecases/_INDEX.md` in sync: set `YYYY` → 🟡 (in corso) at step-01 and → ✅ (implementato) at step-04. The edit is
bundled into the change commit, so `main` reflects completion only on merge. Normal changes skip this.

**Tracked hooks.** (1) **Privacy/RoPA gate (UC 0031)** — step-03 runs the deterministic signal scanner
(`npm run privacy-scan` in `tools/compliance`) and, on signals, acts as the **classification co-pilot** (classify →
manifest/RoPA → MAJOR/MINOR classification piloting the PP/ToS version bump); step-04 verifies the gate ran before the
commit gate. The blocking CI check `@PersonalData`↔manifest lives in `mvn test` (UC 0030). (2) **E2E visual
baseline** (#10 F) — never re-record a snapshot baseline blindly; investigate unexpected diffs (steps 03/04).

Your job is to ensure every change is spec-driven: requirements first, then implementation,
then log — never the other way around.

## Instructions

1. `step-01-init.md` — **settle the execution mode (classic vs autopilot)**, determine change number, create branch,
   seed `decisions.json`, note the areas in scope
2. `step-02-requirements.md` — write requirements.md with the developer
3. `step-03-implement.md` — guide implementation **and add/update tests**
4. `step-04-close.md` — run the suite of every touched area, write implementation-log.md, close `decisions.json`,
   ask for merge consent

## Decision register — `changes/NNNN-*/decisions.json` (non-negotiable)

Every change carries **three** artifacts, not two: `requirements.md`, `implementation-log.md` and
`decisions.json`. The first two are prose for humans; `decisions.json` is the **machine-readable register of
the choices that were actually made** — the structured counterpart of the implementation log, plus every
answer settled during the clarification/requirements dialogue.

It exists so that a **development agent picking the work up later** can know *what was decided and why the
code looks the way it does* without re-reading a chat that no longer exists. It is **not** a substitute for
`git log`: record *relevant* decisions, not every edit.

**Schema** — a JSON array of objects, exactly these fields:

```json
[
  {
    "id": 1,
    "decision": "Descrizione della decisione, in italiano, autoconsistente (cosa è stato deciso e perché).",
    "files": [
      { "file": "services/fatture/src/main/java/.../InvoiceResource.java", "description": "Decisione rilevante che riguarda questo file." }
    ]
  }
]
```

- `id` — progressive integer, starts at `1`, **append-only**: never renumber, never reuse, never reorder.
  Entries stay in chronological order of when the decision was taken.
- `decision` — one self-contained sentence or short paragraph **in Italian** (CLAUDE.md "Lingua" rule),
  plain language, no unexplained acronyms. Say *what* was decided and, when it isn't obvious, *why*.
- `files` — **optional**. Include it only when the decision lands on identifiable files and knowing that
  helps whoever comes next; each entry is `{ "file": <repo-relative path>, "description": <the relevant
  decision for that file> }`. Omit the field entirely when the decision is not file-shaped (a scope choice,
  a deferral, a naming convention).

**What goes in** (all of it, both in classic and autopilot mode):

- every answer that settles a clarification/requirements question (step-02) — the *decision*, not the transcript;
- every technical/architectural choice taken during implementation (step-03): design alternatives discarded,
  patterns adopted, trade-offs, privacy classifications (MAJOR/MINOR, legal basis, retention);
- every **deferred decision** tracked into another use case or `docs/_BACKLOG.md` — the register entry states
  *what* was deferred and *where* it was written down;
- in **autopilot mode**, the decisions the agent took in the developer's place, each stating it explicitly
  (start the text with `(autopilot)` so the origin is never ambiguous). Do not add fields to the schema for
  this: the origin lives inside `decision`.

**When**: write entries **as they happen**, never reconstructed at the end. `decisions.json` is created at
step-01, appended at step-02 and step-03, verified for completeness and coherence with `implementation-log.md`
at step-04, and committed with the change.

## Execution mode — classic or autopilot

The skill runs in one of two modes, settled **before anything else** at step-01:

- **Classic** — the mode used so far: the developer answers every clarification, requirements and design
  question, in the dialogue style described below.
- **Autopilot** — the agent **answers the questions in the developer's place** and proceeds, recording each
  answer as an autopilot decision in `decisions.json`.

**Declaring the mode.** Autopilot can be requested directly at invocation (`/new-change autopilot …`, or
anything that plainly says "in autopilot" / "modalità autopilot"). If the invocation does **not** state a
mode, step-01 asks it **immediately, as its very first action**, using an `AskUserQuestion` prompt (this is
the one deliberate exception to the "no compact multiple-choice wizard" rule below — it is a mode switch,
not a design question).

**How autopilot answers** — these principles are binding, and every answer must be defensible against them:

1. **Follow your own recommendation.** Autopilot answers with the option you would have recommended in
   classic mode. It never picks an option you would not have argued for — if you cannot form a
   recommendation, that question is not autopilot material (see the escalation rule).
2. **Maximise the work completed inside this task**, so the change lands finished and coherent — not a
   half-step that needs a follow-up to be usable.
3. **Do not anticipate later work.** Maximising *this* task is not an invitation to build the next one:
   anything belonging to a subsequent use case stays out of scope.
4. **Systematically track what is left for later.** Every piece of work deliberately not done becomes a
   written cross-reference in the owning use case
   (`docs/usecases/<area>/NNNN-*.md`, section "## Punti aperti / decisioni differite") or in
   `docs/_BACKLOG.md` if cross-cutting — the same non-negotiable rule as the deferred-decision gate, applied
   proactively rather than only when something surfaces by accident.
5. **Everything is recorded.** Each autopilot answer becomes a `decisions.json` entry prefixed `(autopilot)`,
   stating the choice and the reason it was the recommended one. A silent autopilot decision is a bug.

**What autopilot does NOT do.** It answers questions; it does **not** remove or weaken a single one of the three
mandatory gates. All three stay **human**, always:

- the **requirements review gate** (step-02) — the developer always re-reads and explicitly approves
  `requirements.md`. Autopilot decided *what goes in it*, which is exactly why the reading is not optional;
- the **commit consent gate** and the **merge consent gate** (step-04) — autopilot never commits and never
  merges on its own.

Autopilot changes *who answers the questions*, never *who approves the change*.

**Escalation — when autopilot must stop and ask anyway.** Autopilot yields to the developer, in prose, when
a question is genuinely not the agent's to answer: product/business direction, money (pricing, quotas, cost
commitments), legal or personal-data classification that is materially ambiguous (art. 9 data, a new legal
basis, a new sub-processor), anything irreversible or outward-facing, or any point where you cannot form an
honest recommendation. Say that autopilot is deferring, ask the question, and record the answer normally.

## Mandatory gates — never skip

- **Before requirements (step-02): clarification gate.** If the developer's instructions are
  unclear, doubtful, or admit alternatives, ask targeted questions and settle the decisions
  before writing `requirements.md`. Skip only when everything is already clear.
- **After requirements (step-02): STOP for review.** Do not implement until the developer
  explicitly approves `requirements.md`. *(Never auto-approved — autopilot stops here exactly like classic mode.)*
- **At close (step-04): STOP for commit consent.** When implementation is done, do not commit
  until the developer explicitly consents.
- **After committing (step-04): STOP for merge consent.** Leave the branch unmerged and never
  merge to the default branch without the developer's separate explicit go-ahead.
- **Throughout (steps 01-04): decision-register gate.** Every settled question and every relevant technical
  choice is appended to `changes/NNNN-*/decisions.json` **when it is taken**, in both modes. step-04 refuses
  to ask for commit consent if the register is missing, empty, or inconsistent with `implementation-log.md`.
- **Throughout (steps 02-04): deferred-decision tracking gate.** Whenever you encounter an
  architectural decision, drift, or open point that belongs to a *different* use case (or isn't ripe
  to decide now), you MUST record it in that use case's file
  (`docs/usecases/<area>/NNNN-*.md`, section **"## Punti aperti / decisioni differite"**, created if
  missing) — or in `docs/_BACKLOG.md` if cross-cutting — **before closing the change**. Tracking ≠
  resolving: note *what*, *why deferred*, *which UC owns it*; do not force a premature decision. This
  is the CLAUDE.md constitution rule ("Tracciamento delle decisioni differite"); step-04 verifies it.

## Questioning style — one at a time, verbose, with dialogue

**In classic mode** — whenever you need input from the developer (clarification gate, requirements questions,
design choices) — the rules below apply in full.

**In autopilot mode** the same reasoning still happens and is still shown: you state the question, the options
and your recommendation in prose, then **answer it yourself**, record the answer in `decisions.json` and move
on. You stop and wait only in the escalation cases listed above. **One deliberate exception in both modes**:
the mode question at step-01 uses `AskUserQuestion`, because it is a mode switch, not a design question.

The classic-mode rules:

- Ask **one question at a time**. Never batch multiple questions, and never use a compact multiple-choice
  "wizard" with terse one-line options.
- Be **verbose and explanatory**: give the context, why the question matters, the trade-offs, and any options you
  see (with your reasoning/recommendation) — in prose.
- After each question, **STOP and wait** for the developer's answer. Do not move on or assume.
- Treat answers as a **dialogue**: if the reply opens follow-ups or needs deepening, keep discussing until the
  point is genuinely settled, *then* record the definitive answer and move to the next question.
- **Plain language — no unexplained acronyms or slang** (CLAUDE.md "Lingua" rule): no "PII", "YAGNI",
  "fan-out", etc. Use plain Italian words; if a technical acronym is truly needed, explain it at first use.
  The developer must understand the question without decoding jargon.

This applies to all steps (the simple step-01 prompts inherit the same spirit; the real back-and-forth is at the
step-02 clarification gate).

## Stack-aware testing — by area touched

The test command depends on **which area(s)** the change touches:

- `infra/` (Terraform, has `*.tf`) → `terraform fmt -check && terraform validate` (+ `terraform plan`; do not apply)
- `frontend/` (React, has `package.json`) → `npm test`; tests next to code (`*.test.ts(x)`)
- `services/<app>/` (Quarkus, has `pom.xml`) → `mvn test`; JUnit under `src/test/java/...`

If a change touches **executable code**, step-03 adds/updates tests covering the code changes and
any "Test Requirements" in `requirements.md`; the suite of **every touched area** must be green
before the commit gate. If a change touches only Markdown/skills/prompts/config/docs, tests are
not applicable — say so (with the reason) in the implementation log.

## appgrove invariants — respect them in every change

Carry these non-negotiable constraints through requirements, implementation, and review:

- **Tenant ID only from the verified JWT** — claim `tenant_id` (account, injected by the Pre-Token-Gen Lambda); `sub` = user_id. Never from request params/body.
- **Row-level tenant filter** (`WHERE tenant_id = :tid`) on every tenant-scoped query.
- **`microsaas_app` Terraform module** is the building block — adding an app means instantiating the
  module (name, port, DB schema), not hand-rolling parallel infra.
- **Structured logging everywhere** — every log carries `tenant_id`, `app_id`, `user_id`.

## Token budget

Keep the **artifacts** (requirements.md, implementation-log.md) and routine status messages concise — they are the
deliverable, not the chat. **Exception**: the clarification/requirements dialogue is deliberately verbose — there, explain
fully and ask one question at a time (see "Questioning style"); do not compress it to save tokens.
