# Step 04 — Landing Draft and Close

## Landing draft — a draft, and only a draft

The generator produced a landing page in `draft` state, five languages, with on-brand placeholder
copy. Refine it enough to be a genuine starting point — what the app does, for whom, the promise —
and leave it **`draft`**.

Publishing is the job of `finalize-landing` (UC 0057), which does the parts that cannot be faked at
scaffold time: real screenshots of a working app, refined copy, social preview image, an interactive
five-language review. The build only renders `published` pages, so a draft is invisible by design.

Do not be tempted to promote it because the copy "looks fine": at this point the app does not exist
yet, so any screenshot would be a lie and any concrete claim unverified.

## Run every touched suite

Creating an app touches every area, so **all** of them must be green:

```bash
./run-tests.sh                    # everything
./run-tests.sh tooling            # parity of the source templates against app #1
```

The `tooling` area matters especially here: it is what proves the generator still matches the app it
was derived from. If it is red *after* generating, the templates had already aged — fix the
templates and regenerate rather than patching the new app (SKILL.md).

Infrastructure is validated, never applied:

```bash
( cd infra/envs/test && terraform fmt -check && terraform validate )
```

## Deliverables checklist

Before closing, confirm each of these exists and is real, not a placeholder:

- service, frontend module, `microsaas_app` instance in both environments
- **data manifest** filled in both languages, register regenerated, `@PersonalData` pairing green
- **pricing file** with the metric nature settled, registered in the pricing index
- generated test suite green, including tenant isolation and the entitlement gate
- landing draft in five languages, still `draft`
- privacy classification (major/minor) recorded with its rationale

## Deferred decisions — record them before closing

Anything you hit that belongs to a *different* use case, or is not ripe to decide, goes into that
use case's **"Punti aperti / decisioni differite"** section, or `docs/_BACKLOG.md` if cross-cutting.
This is the CLAUDE.md constitution rule. If the app needed something the templates could not give,
that belongs in `docs/_PARITA-SCAFFOLD.md` as a deliberate deviation — with the reason.

## MANDATORY STOP — hand back to new-change

The commit and merge gates are `new-change`'s, and they are **not** weakened here. Print:

```
🛑 Application <app_id> scaffolded (service + frontend module + infra + manifest + pricing + landing draft).
   Suites: <esiti reali per area>
Review it, then give explicit consent to commit. I will not commit, merge or deploy without your go-ahead.
```

Then **STOP**. This skill writes code and leaves a branch. It never merges, never deploys, never
talks to the payment provider — script generates, CI applies (#07 G18).
