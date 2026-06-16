# Step 04 — Close Change

All commands run at the monorepo root.

## Gather implementation data

Ask the developer (one question):
> "Any decisions made or issues encountered during implementation worth noting?"

Then inspect the git diff to identify changed files and the areas touched:
```bash
git diff --name-only "$DEFAULT_BRANCH"...HEAD
```

## Verify tests are green — every touched area (stack-aware)

Run the suite of **each area** the diff touches, and confirm all pass before going further:

```bash
# services/<app>/ (Quarkus):
( cd services/<app> && mvn test )
# frontend/ (React):
( cd frontend && npm test )
# infra/ (Terraform):
( cd infra && terraform fmt -check && terraform validate )
```

If the change touched executable code, the relevant suites must include the tests added/updated in
step-03. If any suite fails, fix it before the commit gate. If the change touched no executable
code, no suite run is required — note that in the log.

## Write implementation-log.md

Write `changes/NNN-brief-description/implementation-log.md`:

```markdown
# Implementation Log — Change NNN: Brief Description

**Branch**: `change/NNN-brief-description`
**Areas**: <infra | frontend | services/<app>, …>
**Completed**: YYYY-MM-DD

## Files Changed

| File | Action |
|---|---|
| <path> | Created / Modified / Deleted |

## What Was Done

<2-3 sentences describing what was implemented and how>

## Decisions Made

<any decisions made during implementation, or "None — implemented as specified">

## appgrove Invariants

<How the change keeps each touched invariant true (tenant_id-from-JWT, row-level filter,
microsaas_app Terraform module, structured logging), or "None touched">

## Notes for Reviewer

<anything the Platform Engineer should know before merge, incl. cross-area contract impact
(frontend ↔ service API, service ↔ infra), or "None">

## Tests

<tests added/updated and what they cover, per area, and each suite result (`mvn test` /
`npm test`) — OR "Not applicable — no executable code changed (only <docs/skills/config>)">

## Acceptance Criteria Status

- [x] <criterion from requirements.md>
- [x] <criterion from requirements.md>
```

## MANDATORY STOP — commit consent gate

Do **not** commit yet. Summarize what will be committed (the changed files and the
`implementation-log.md`) and ask the developer's explicit consent to commit.

Print:
```
🛑 Implementation complete. Ready to commit change NNN (areas: <list>):
   <short list of changed files>
Reply with explicit consent and I will commit. I will not commit without your go-ahead.
```

Then STOP. Only after the developer explicitly consents, run:
```bash
git add changes/NNN-brief-description/implementation-log.md
git add -A
git commit -m "chore(change/NNN): implementation complete"
git push origin change/NNN-brief-description   # only if a remote is configured
```

## MANDATORY STOP — merge consent gate

After committing, leave the change branch **unmerged**. Do **not** merge to the default branch and
do **not** run the merge command on your own — merging requires the developer's separate explicit
consent.

Print:
```
🛑 Change NNN complete. Branch left unmerged: change/NNN-brief-description

Reply with explicit consent to merge, and I will run:
  git checkout "$DEFAULT_BRANCH" && git merge --no-ff change/NNN-brief-description -m "chore: change/NNN-brief-description"

Or, to open a PR instead:
  gh pr create --base "$DEFAULT_BRANCH" --head change/NNN-brief-description --title "change/NNN: brief description"

I will not merge without your explicit go-ahead.
```

Then STOP. Only merge after the developer explicitly tells you to.
