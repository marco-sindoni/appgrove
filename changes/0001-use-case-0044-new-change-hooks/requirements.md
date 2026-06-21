# Change 0001: new-change tracked hooks (privacy/RoPA + E2E baseline)

**Branch**: `change/0001-use-case-0044-new-change-hooks`
**Areas**: skills (`.claude/skills/new-change/`) — Markdown only, no executable code
**Date**: 2026-06-22
**Author**: Platform Engineering
**Source use case**: [docs/usecases/10-skills-tooling/0044-aggiornamento-skill-new-change.md](../../docs/usecases/10-skills-tooling/0044-aggiornamento-skill-new-change.md)

## Problem / Goal

Bring `new-change` to its definitive form per UC 0044. Its base is already done (4-digit `NNNN`, use-case variant
`NNNN-use-case-YYYY`, "Source use case" field, three blocking gates, per-area test suite at close, `_INDEX.md` sync). What
remains is to **encode the two tracked hooks** so every change is reminded of them — with real CI enforcement deferred to its
owning use case:

- **Privacy/RoPA hook** (#13 C) — surface when a diff touches personal data; the blocking CI gate stays UC 0031.
- **E2E visual baseline rule** (#10 F) — "never update a baseline blindly; investigate unexpected visual diffs".

## Scope

- `step-02-requirements.md`: add a **"Personal data touched? (Y/N)"** line to the requirements template (+ short note: if Y,
  the step-03 privacy checkpoint applies; CI enforcement is UC 0031).
- `step-03-implement.md`:
  - **Privacy/RoPA checkpoint** — if the diff touches personal data (new/changed DB migrations, entity/DTO/API fields, or new
    external integrations), the skill warns and reminds: classify nature/purpose/legal-basis/retention, update manifest + RoPA,
    bump PP/ToS version (major→re-accept / minor→notice). Explicitly note enforcement is UC 0031 (this is a guided reminder).
  - **Baseline rule** — add the #10 F rule to the testing guidance.
- `step-04-close.md`: when running the touched suites, restate the **baseline rule** (#10 F) for E2E/visual suites.
- `SKILL.md`: one line under invariants/instructions pointing to the two tracked hooks.

## Out of Scope

- Real CI enforcement of the privacy gate (ArchUnit-style `@PersonalData` check, classification co-pilot) → **UC 0031**.
- Manifest/RoPA assembly and PP/ToS versioning machinery → UC 0030 / 0002 / 0056.
- Any change to the already-working parts (numbering, gates, `_INDEX` sync, per-area testing).
- Executable code in `infra/`, `frontend/`, `services/`.

## Acceptance Criteria

- [ ] requirements template has a "Personal data touched? (Y/N)" field with the UC 0031 deferral note.
- [ ] step-03 has a privacy/RoPA **guided checkpoint** (triggers on personal-data-touching diffs; reminds classify→manifest/RoPA→PP/ToS bump; enforcement = UC 0031).
- [ ] the #10 F baseline rule is present in **both** step-03 (testing guidance) and step-04 (suite run).
- [ ] SKILL.md references the two tracked hooks.
- [ ] No change to existing numbering/gates/`_INDEX` sync/per-area testing behavior.

## appgrove Invariants Touched

None at runtime (tooling). The skill remains the **guarantor** of the invariants: the privacy checkpoint reinforces GDPR
accountability (#13), and every change keeps documenting tenant_id-from-JWT, row-level filter, `microsaas_app` module,
structured logging.

## Test Requirements (optional)

None (Markdown/skill-only change → no executable code). Functional verification: the hooks read correctly and the
already-working behaviors are untouched.

## Impact Assessment

| Area | Impact |
|---|---|
| Breaking change | No |
| Cross-area contract | N/A |
| Version bump | none (process/tooling docs) |
