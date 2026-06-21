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

## Privacy/RoPA checkpoint (tracked hook — blocking CI enforcement is UC 0031)

If the diff **touches personal data**, surface it during implementation (this is a guided reminder, not the CI gate):

- **Triggers**: new/changed Flyway migrations (columns/tables), new entity/DTO/API fields, or a **new external integration**
  (potential sub-processor).
- **Then remind the developer to**: classify the field's **nature / purpose / legal basis / retention**; update the data
  **manifest + RoPA** (#13 C); if the integration is a new sub-processor, update `content/legal/subprocessors.*.md` (+ 30-day
  client notice); and **bump the PP/ToS version** — material change → **major** (re-accept, UC 0056) / minor → notice (#13 G41).
- **Art. 9** (health/biometric/…): strong warning + DPIA (#13 K); do not proceed silently.
- Set **"Personal data touched? = Yes"** in `requirements.md` and note the classification in the implementation log.

The real **blocking** check (`@PersonalData` not declared → build red, ArchUnit-style classification co-pilot) is delivered by
**UC 0031**; here `new-change` only reminds, so nothing slips through unnoticed before 0031 lands.

## Cross-area contracts

When a change spans the frontend ↔ service API boundary (or service ↔ infra), keep both sides
coherent in the same commit — this is exactly why appgrove is a monorepo. Note any contract change
so step-04 can flag it for the reviewer.

## When the developer signals they are done

They may say "done", "finished", "ready to log", or invoke `/new-change` again.
Proceed to step-04-close.md.
