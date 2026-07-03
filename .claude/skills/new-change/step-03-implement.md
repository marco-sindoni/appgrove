# Step 03 — Implementation

## Role during implementation

Stay available to assist with the implementation. Do not proactively make changes —
wait for the developer to ask. Your role is to:
- Answer questions about the codebase
- Generate code when asked
- Point out if a change would violate an appgrove invariant or a documented convention
  (`AGENTS.md` / `CLAUDE.md` if present)

## Reminders to share once (not repeatedly)

- Change artifacts (`requirements.md`, `implementation-log.md`) are written **in Italian**; the skill instructions stay in English
- Uphold the appgrove invariants:
  - **tenant_id only from the verified JWT** — claim `tenant_id` (account); `sub` = user_id. Never from request body/params
  - **`WHERE tenant_id = :tid`** on every tenant-scoped query
  - new app = instantiate the **`microsaas_app`** Terraform module, not bespoke infra
  - **structured logging** carrying `tenant_id`, `app_id`, `user_id`

## Tests — keep every touched area's suite coherent (stack-aware)

If the change touches **executable code**, add or update tests covering both (1) the new/changed
behavior and (2) any **"Test Requirements"** from `requirements.md` (e.g. a test proving tenant_id
is sourced only from the JWT). Run the suite of **each area touched**:

- `services/<app>/` (Quarkus, `pom.xml`): JUnit under `src/test/java/...`. From the service dir:
  `mvn test`; faster cycles `mvn -Dtest=ClassName test`. Prefer deterministic offline fixtures
  (Quarkus test profile, Testcontainers, mocked Cognito/JWT) over live AWS/HTTP calls.
- `frontend/` (React, `package.json`): tests next to code (`*.test.ts(x)`); `npm test`.
- `infra/` (Terraform, `*.tf`): `terraform fmt -check && terraform validate` (+ `terraform plan`). Do not `apply` to test.

A cross-area change must keep **all** touched suites green. If the change touches **only**
Markdown/skills/prompts/config/docs (no executable code), tests are not applicable — record that,
with the reason, in the implementation log.

**E2E visual baseline rule (#10 F).** Never update a Playwright/visual snapshot baseline **blindly**: an unexpected visual
diff is a signal to **investigate** (real regression vs intended UI change), not to re-record. Update a baseline only when
the UI change is intentional and reviewed; note it in the implementation log. Prefer `aria-snapshot` as the primary net,
pixel diffs tolerant (#10 20).

## Privacy/RoPA gate (UC 0031) — mandatory before closing

When the implementation looks complete (and in any case before step-04), run the **deterministic signal scanner**:

```bash
( cd tools/compliance && npm run privacy-scan )        # default: merge-base(main, HEAD) → working tree, untracked included
# npm run privacy-scan -- main...HEAD                  # explicit git range
# npm run privacy-scan -- --json                       # machine-readable output
```

Exit ≠ 0 = signals found: new Flyway migration tables/columns · new entity/DTO fields (`src/main/java`) · new
dependencies/external hosts (**potential sub-processor**) · classification keys touched in the data manifests. The scanner
is deterministic but heuristic — treat its output as the **floor, not the ceiling**: also consider signals it cannot see
(e.g. a purpose change implemented in code, a new API exposing existing fields).

**No signals and nothing personal-data-related in the change** → record "gate privacy: nessun segnale" in the
implementation log and move on.

**On signals, act as the classification co-pilot (#13 C16)** — reason *with* the developer, not a passive checklist. For
each signal:

1. **Elicit the purpose**: "spiegami cos'è e a cosa serve questo campo/integrazione".
2. **Deduce and propose** nature / purpose / legal basis / retention **with your reasoning**; ask deepening questions
   **only if the case is ambiguous** (e.g. phone number for the feature vs commercial re-contact — the legal basis changes).
3. **Get explicit confirmation** ("finalità=X, base=Y, retention=Z, categoria=ordinaria — ti torna?") → update the
   **manifest YAML** (`docs/compliance/manifests/`, both languages), regenerate the RoPA (`npm run assemble`) and annotate
   the field **`@PersonalData`** — the CI verifier (UC 0030) enforces the pairing in `mvn test`.
4. **Art. 9 escalation** (health, biometric, genetic, …): **strong warning** + DPIA screening against the art. 35/EDPB
   criteria (#13 K67) + reinforced legal basis; never proceed silently. **Guardrail**: pseudonymization is **not**
   erasure (#13 L72) — reject retention/erasure designs that only pseudonymize.
5. **Classify MAJOR/MINOR** (#13 G41, #14 C18): **material** change (purposes / legal bases / data categories /
   retention) → **major** → scoped re-accept (UC 0056); otherwise **minor** → notice. Record the classification
   (major/minor + affected component: platform core vs app module + rationale) in `requirements.md`
   ("Tocca dati personali?") **and** in the implementation log. While `content/legal/` does not exist yet (UC 0002), this
   record **is** the deliverable — UC 0002 will replay the accumulated classifications into the front-matter versions;
   once the legal texts exist, bump the affected component's `version`/`effective_date` front-matter here.
6. **Sub-processor** (#13 C49): a new external integration → flag **"potenziale nuovo sub-processor"**; update
   `content/legal/subprocessors.*.md` + 30-day client notice once they exist (UC 0002 / notice channel UC 0056) — until
   then, record the flag in the implementation log so UC 0002 seeds the list from it.

The co-pilot assists **up to a solid draft**; validation remains a legal-review matter (`docs/_REVISIONE-LEGALE.md`).
The classification dialogue follows the **Questioning style** (one question at a time, verbose, confirm before recording).

## Cross-area contracts

When a change spans the frontend ↔ service API boundary (or service ↔ infra), keep both sides
coherent in the same commit — this is exactly why appgrove is a monorepo. Note any contract change
so step-04 can flag it for the reviewer.

## When the developer signals they are done

They may say "done", "finished", "ready to log", or invoke `/new-change` again.
Proceed to step-04-close.md.
