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

**E2E visual baseline rule (#10 F).** For frontend/E2E suites with visual snapshots: an unexpected visual diff is
**investigated**, not silently re-recorded. Update a baseline only for an intentional, reviewed UI change, and record that
update (and why) in the implementation log. Never `--update-snapshots` blindly to make a red suite green.

## Write implementation-log.md

Write `changes/NNNN-brief-description/implementation-log.md` **in Italian** (the artifact is in Italian; the skill
instructions stay in English):

```markdown
# Implementation Log — Change NNNN: Titolo breve

**Branch**: `change/NNNN-descrizione-breve`
**Aree**: <infra | frontend | services/<app>, …>
**Completata**: AAAA-MM-GG

## File modificati

| File | Azione |
|---|---|
| <path> | Creato / Modificato / Eliminato |

## Cosa è stato fatto

<2-3 frasi che descrivono cosa è stato implementato e come>

## Decisioni prese

<eventuali decisioni prese durante l'implementazione, o "Nessuna — implementato come da specifica">

## Invarianti appgrove

<Come la change mantiene vero ciascun invariante toccato (tenant_id dal JWT, filtro row-level,
modulo Terraform microsaas_app, logging strutturato), o "Nessuno toccato">

## Note per il revisore

<qualsiasi cosa il Platform Engineer debba sapere prima del merge, incl. impatto su contratti cross-area
(frontend ↔ API servizio, servizio ↔ infra), o "Nessuna">

## Test

<test aggiunti/aggiornati e cosa coprono, per area, ed esito di ogni suite (`mvn test` /
`npm test`) — OPPURE "Non applicabile — nessun codice eseguibile modificato (solo <docs/skill/config>)">

## Stato criteri di accettazione

- [x] <criterio da requirements.md>
- [x] <criterio da requirements.md>
```

## Aggiorna l'indice di esecuzione (solo use-case change)

If this change implements a use case (`YYYY`), set it **implementato** in `docs/usecases/_INDEX.md` (the `git add -A`
below includes it in the change commit, so `main` shows ✅ on merge):

```bash
YYYY="0044"   # the source use case number (4 digits)
sed -i '' -E "/\[$YYYY\]\(/ s/\| (⬜|🟡|✅) \|$/| ✅ |/" docs/usecases/_INDEX.md
```

If you also added **new** use cases as part of this change, re-run the Fase+Dipendenze ordering so `_INDEX.md` stays a
valid topological order. For a **normal** change (no `YYYY`), skip.

## MANDATORY STOP — commit consent gate

Do **not** commit yet. Summarize what will be committed (the changed files and the
`implementation-log.md`) and ask the developer's explicit consent to commit.

Print:
```
🛑 Implementation complete. Ready to commit change NNNN (areas: <list>):
   <short list of changed files>
Reply with explicit consent and I will commit. I will not commit without your go-ahead.
```

Then STOP. Only after the developer explicitly consents, run:
```bash
git add changes/NNNN-brief-description/implementation-log.md
git add -A
git commit -m "chore(change/NNNN): implementation complete"
git push origin change/NNNN-brief-description   # only if a remote is configured
```

## MANDATORY STOP — merge consent gate

After committing, leave the change branch **unmerged**. Do **not** merge to the default branch and
do **not** run the merge command on your own — merging requires the developer's separate explicit
consent.

Print:
```
🛑 Change NNNN complete. Branch left unmerged: change/NNNN-brief-description

Reply with explicit consent to merge, and I will run:
  git checkout "$DEFAULT_BRANCH" && git merge --no-ff change/NNNN-brief-description -m "chore: change/NNNN-brief-description"

Or, to open a PR instead:
  gh pr create --base "$DEFAULT_BRANCH" --head change/NNNN-brief-description --title "change/NNNN: brief description"

I will not merge without your explicit go-ahead.
```

Then STOP. Only merge after the developer explicitly tells you to.
