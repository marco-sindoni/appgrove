# Step 03 — Implementation

## Role during implementation

Stay available to assist with the implementation. Do not proactively make changes —
wait for the developer to ask. Your role is to:
- Answer questions about the codebase
- Generate code when asked
- Point out if a change would violate an appgrove invariant or a documented convention
  (`AGENTS.md` / `CLAUDE.md` if present)

## Reminders to share once (not repeatedly)

- All change documentation in English
- Uphold the appgrove invariants:
  - **tenant_id only from the verified JWT** (`jwt.getClaim("sub")`), never from request body/params
  - **`WHERE tenant_id = :tid`** on every tenant-scoped query
  - new app = instantiate the **`MicroSaasApp`** CDK construct, not bespoke infra
  - **structured logging** carrying `tenant_id`, `app_name`, `user_id`

## Tests — keep every touched area's suite coherent (stack-aware)

If the change touches **executable code**, add or update tests covering both (1) the new/changed
behavior and (2) any **"Test Requirements"** from `requirements.md` (e.g. a test proving tenant_id
is sourced only from the JWT). Run the suite of **each area touched**:

- `services/<app>/` (Quarkus, `pom.xml`): JUnit under `src/test/java/...`. From the service dir:
  `mvn test`; faster cycles `mvn -Dtest=ClassName test`. Prefer deterministic offline fixtures
  (Quarkus test profile, Testcontainers, mocked Cognito/JWT) over live AWS/HTTP calls.
- `frontend/` (React, `package.json`): tests next to code (`*.test.ts(x)`); `npm test`.
- `infra/` (CDK, `package.json`): assertions/snapshots; `npm test`. Do not deploy to test.

A cross-area change must keep **all** touched suites green. If the change touches **only**
Markdown/skills/prompts/config/docs (no executable code), tests are not applicable — record that,
with the reason, in the implementation log.

## Cross-area contracts

When a change spans the frontend ↔ service API boundary (or service ↔ infra), keep both sides
coherent in the same commit — this is exactly why appgrove is a monorepo. Note any contract change
so step-04 can flag it for the reviewer.

## When the developer signals they are done

They may say "done", "finished", "ready to log", or invoke `/new-change` again.
Proceed to step-04-close.md.
