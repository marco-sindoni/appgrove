# Step 01 — Identity and Generation

All commands run at the monorepo root `/Users/msindoni/Projects/appgrove`.

## Read the deviation register first

```bash
cat docs/_PARITA-SCAFFOLD.md
```

It records what the source templates **deliberately** do not carry, and why. Reading it prevents the
classic mistake of "fixing" in the generated app something that was left out on purpose.

## Open inside a change

This skill does not invent its own workflow: it runs **inside** `new-change`. Start it, with the use
case number when the app comes from one (e.g. UC 0054 for app #2):

```
/new-change
```

Give the description as `app <app_id> — <one line>`. From here on, the `new-change` gates apply:
requirements review, commit consent, merge consent.

## MANDATORY identity gate — ask, never assume

Four things must be settled **before** generating. Ask them **one at a time**, in prose, with the
context and your recommendation, and **STOP after each** (see SKILL.md "Questioning style").

They are not bureaucracy: **`app_id` is baked into the database schema name, the queue names, the
public API route `/api/<app_id>/v1/*` and the Terraform module instance.** Changing it after the
fact is a data migration, not a rename. Say so when you ask.

> 1. **`app_id`** — lowercase, `^[a-z][a-z0-9_]{0,30}$`. Short, stable, and about *what the app is*,
>    not how it is currently marketed (marketing names change; schema names should not).
> 2. **User model** — `single` (one person per account, business-to-consumer) or `multi` (several
>    people per account, with invitations and seats). This drives the seat/quota shape and is
>    awkward to change later: a single-user app has no notion of who did what.
> 3. **HTTP port** — local development only. Derive the free ones and propose the next:
>    `./dev.sh services` lists what is taken (convention: `8081+` for apps).
> 4. **Quota metric** — the *one* thing the plan limits (e.g. `fatture`, `documenti`, `progetti`).
>    Its nature (consumption per window vs standing level) is settled in step-02; here you only need
>    the name, because the template wires it into the quota service.

Ask about the **category icon and accent colour** too, but do not block on them: sensible defaults
exist in the design system and they are trivial to change later (unlike the four above).

## Run the generator

```bash
tools/new-application/generate.sh \
  --app-id <app_id> --port <N> --user-model <single|multi> --metric <metric> --dry-run
```

**Always `--dry-run` first**, show the developer what it will touch, then run it for real without
the flag. The generator refuses to overwrite an existing app — if it complains, stop and ask; do not
delete the existing one to make room.

It **creates** the service, the frontend module, the end-to-end test, the data manifest skeleton and
the pricing file; **modifies** the Maven module list, the frontend registry, the pricing index and
the local queue configuration; and **delegates** the Terraform wiring to `infra/scripts/service-add`.

It deliberately does **not** touch the local startup scripts, the reverse proxy or the workflows:
those discover services by themselves (`dev/lib/services.sh`, `tools/ci/services.sh`). If you find
yourself wanting to edit one of them by hand, that is a **bug in the discovery**, not a step of this
skill — fix the discovery and record it.

## Verify the generation before moving on

```bash
./dev.sh services                                  # the new app must appear, with its port and schema
( cd services && mvn -B -pl <app_id> -am test )    # the generated suite must be green
```

A red suite here means the **templates** are stale, not that the new app is special. Fix
`tools/new-application/templates/` and regenerate — never patch the generated output (SKILL.md).

Proceed to `step-02-pricing.md`.
