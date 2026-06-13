# Step 02 — Write Requirements

## MANDATORY clarification gate — before writing anything

Review the instructions the developer gave. If any aspect is **unclear, doubtful, or admits
viable alternatives** (scope boundaries, design choices, naming, which areas it touches, which
appgrove invariant applies, trade-offs), **ask targeted questions first** and record the
decisions — do not write `requirements.md` on assumptions. Prefer concrete either/or options over
open questions. If, and only if, the instructions are already fully clear with no real
alternatives, skip straight to the template below without inventing questions.

Only proceed once the open points are resolved.

## Fill the requirements template

Ask the developer up to 3 questions to fill the requirements template.
Ask them all at once, not one by one:

> 1. What problem does this change solve or what goal does it achieve?
> 2. What is in scope? (which areas — infra/frontend/services/<app> — files, behaviors)
> 3. What is explicitly out of scope or should not change?

Then write `changes/NNN-brief-description/requirements.md`:

```markdown
# Change NNN: Brief Description

**Branch**: `change/NNN-brief-description`
**Areas**: <infra | frontend | services/<app>, … — one or more>
**Date**: YYYY-MM-DD
**Author**: (developer's name or "Platform Engineering")

## Problem / Goal

<answer to question 1>

## Scope

<answer to question 2>

## Out of Scope

<answer to question 3>

## Acceptance Criteria

- [ ] <derive 2-3 concrete, verifiable criteria from the above>

## appgrove Invariants Touched

<List any of these the change must uphold, or "None": tenant_id only from JWT;
WHERE tenant_id row-level filter; MicroSaasApp construct pattern; structured
logging (tenant_id/app_name/user_id). Note how the change keeps each one true.>

## Test Requirements (optional)

<Include only if specific tests are wanted beyond what the code changes imply — acceptance tests,
edge cases, regression guards (e.g. a test asserting tenant_id is read only from the JWT).
Otherwise omit. step-03 must satisfy whatever is listed here.>

## Impact Assessment

| Area | Impact |
|---|---|
| Breaking change | Yes / No |
| Cross-area contract | Yes / No / N/A (frontend ↔ service API, or service ↔ infra) |
| Version bump | patch / minor / major / none |
```

Commit the file:
```bash
git add changes/NNN-brief-description/requirements.md
git commit -m "chore(change/NNN): write requirements"
```

## MANDATORY STOP — requirements review gate

After committing, **STOP. Do not start implementing.** Requirements must be reviewed and
explicitly approved by the developer before any implementation begins.

Print:
```
🛑 requirements.md written and committed. Please review it.
   Reply with your approval (or requested changes) before I start implementing.
```

Wait for the developer's explicit approval. If they request changes, update `requirements.md`,
re-commit (`chore(change/NNN): revise requirements`), and ask again. Only once they approve do
you proceed to step-03-implement.md.
