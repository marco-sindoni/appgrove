---
name: new-change
description: >
  Starts a new spec-driven change in the appgrove monorepo. Determines the next
  change number, creates a branch change/NNNN-brief-description, writes
  changes/NNNN-*/requirements.md, guides the implementation, runs the test suite of
  every area touched (infra/frontend/services), writes
  changes/NNNN-*/implementation-log.md, and proposes the merge. Leaves the branch
  open for the Platform Engineer decision.
triggers:
  - /new-change
  - /change
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

**Tracked hooks (reminders, not the CI gate).** (1) **Privacy/RoPA** — when a diff touches personal data, step-03 surfaces a
guided checkpoint (classify → manifest/RoPA → PP/ToS bump); the blocking CI enforcement is **UC 0031**. (2) **E2E visual
baseline** (#10 F) — never re-record a snapshot baseline blindly; investigate unexpected diffs (steps 03/04).

Your job is to ensure every change is spec-driven: requirements first, then implementation,
then log — never the other way around.

## Instructions

1. `step-01-init.md` — determine change number, create branch, note the areas in scope
2. `step-02-requirements.md` — write requirements.md with the developer
3. `step-03-implement.md` — guide implementation **and add/update tests**
4. `step-04-close.md` — run the suite of every touched area, write implementation-log.md, ask for merge consent

## Mandatory gates — never skip

- **Before requirements (step-02): clarification gate.** If the developer's instructions are
  unclear, doubtful, or admit alternatives, ask targeted questions and settle the decisions
  before writing `requirements.md`. Skip only when everything is already clear.
- **After requirements (step-02): STOP for review.** Do not implement until the developer
  explicitly approves `requirements.md`.
- **At close (step-04): STOP for commit consent.** When implementation is done, do not commit
  until the developer explicitly consents.
- **After committing (step-04): STOP for merge consent.** Leave the branch unmerged and never
  merge to the default branch without the developer's separate explicit go-ahead.
- **Throughout (steps 02-04): deferred-decision tracking gate.** Whenever you encounter an
  architectural decision, drift, or open point that belongs to a *different* use case (or isn't ripe
  to decide now), you MUST record it in that use case's file
  (`docs/usecases/<area>/NNNN-*.md`, section **"## Punti aperti / decisioni differite"**, created if
  missing) — or in `docs/_BACKLOG.md` if cross-cutting — **before closing the change**. Tracking ≠
  resolving: note *what*, *why deferred*, *which UC owns it*; do not force a premature decision. This
  is the CLAUDE.md constitution rule ("Tracciamento delle decisioni differite"); step-04 verifies it.

## Questioning style — one at a time, verbose, with dialogue

When you need input from the developer (clarification gate, requirements questions, design choices):

- Ask **one question at a time**. Never batch multiple questions, and never use a compact multiple-choice
  "wizard" with terse one-line options.
- Be **verbose and explanatory**: give the context, why the question matters, the trade-offs, and any options you
  see (with your reasoning/recommendation) — in prose.
- After each question, **STOP and wait** for the developer's answer. Do not move on or assume.
- Treat answers as a **dialogue**: if the reply opens follow-ups or needs deepening, keep discussing until the
  point is genuinely settled, *then* record the definitive answer and move to the next question.

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
