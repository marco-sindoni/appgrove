# Step 01 — Initialize Change

All commands run at the monorepo root `/Users/msindoni/Projects/appgrove`.

## Determine next change number

```bash
ls changes/ 2>/dev/null | grep -E '^[0-9]{3,4}-' | sort | tail -1
```

Take the highest number found and add 1. **Pad to 4 digits.**
If `changes/` has no numbered folders, start at `0001`.
(The `{3,4}` regex tolerates any legacy 3-digit folders when computing the max; new folders are always 4-digit.)

## Ask for change description

Ask the developer (one question):
> "Describe the change in a few words (will be used as the branch and folder name):"

Convert the answer to kebab-case, max 40 characters.
Example: "add Cognito authorizer to notes API" → `add-cognito-authorizer-notes-api`

## Use-case-originated changes — naming variant

Ask the developer (one question):
> "Does this change implement a use case from `docs/usecases/`? If yes, give its number (YYYY)."

This determines the **folder/branch naming convention** (two forms):

- **Normal change** → `NNNN-brief-description`
  (NNNN = next progressive number in `changes/`)
- **Use-case-originated change** → `NNNN-use-case-YYYY-brief-description`
  (NNNN = next progressive number in `changes/`; **YYYY** = the source use case's absolute number in `docs/usecases/`)

`NNNN` is **always** the running counter of the `changes/` folder (independent of YYYY). YYYY just embeds the source use
case so the change is traceable back to its spec. **Pad both NNNN and YYYY to 4 digits.**
Example: change #7 implementing use case 35 "checkout overlay" → `0007-use-case-0035-checkout-overlay`.

When a change comes from a use case, the `requirements.md` (step-02) must **link the source**
`docs/usecases/<area>/YYYY-*.md` (the use case lives in its area subfolder; see `docs/usecases/README.md`).

## Note the areas in scope

Identify which monorepo areas the change is expected to touch — this drives the test suites in
step-03/step-04. A change may span several:

- `infra/` (Terraform) → `terraform validate` / `plan`
- `frontend/` (React) → `npm test`
- `services/<app>/` (Quarkus) → `mvn test`

This is a best-guess at init; refine it in requirements.

## Create branch and folder

Detect the default branch instead of assuming `main`:

```bash
DEFAULT_BRANCH=$(git symbolic-ref --short refs/remotes/origin/HEAD 2>/dev/null | sed 's@^origin/@@')
DEFAULT_BRANCH=${DEFAULT_BRANCH:-main}

# CHANGE_ID is either "NNNN-brief-description" (normal)
# or "NNNN-use-case-YYYY-brief-description" (use-case-originated, see naming variant above)
CHANGE_ID="NNNN-brief-description"
git checkout "$DEFAULT_BRANCH"
git pull origin "$DEFAULT_BRANCH"   # only if a remote is configured
git checkout -b "change/$CHANGE_ID"
mkdir -p "changes/$CHANGE_ID"
```

## Aggiorna l'indice di esecuzione (solo use-case change)

If this is a **use-case-originated** change (you captured a `YYYY`), mark the use case **in corso** in the execution index
`docs/usecases/_INDEX.md`. The row is identified by its `[YYYY](…)` link; flip **only the last status cell**:

```bash
YYYY="0044"   # the source use case number (4 digits)
sed -i '' -E "/\[$YYYY\]\(/ s/\| (⬜|🟡|✅) \|$/| 🟡 |/" docs/usecases/_INDEX.md
```

(macOS `sed -i ''`.) This edit stays in the working tree and is committed with the change at step-04, where it is flipped
to ✅. For a **normal** change (no `YYYY`), skip this.

## Output

```json
{ "change_id": "NNNN-brief-description", "branch": "change/NNNN-brief-description", "areas": ["frontend", "services/notes", "infra"] }
```

Confirm to developer (one line):
```
✅ Branch: change/NNNN-brief-description | Areas: <list> | Now writing requirements...
```

Proceed to step-02-requirements.md.
