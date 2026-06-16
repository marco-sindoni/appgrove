# Step 01 — Initialize Change

All commands run at the monorepo root `/Users/msindoni/Projects/appgrove`.

## Determine next change number

```bash
ls changes/ 2>/dev/null | grep -E '^[0-9]{3}-' | sort | tail -1
```

Take the highest number found and add 1. Pad to 3 digits.
If `changes/` has no numbered folders, start at `001`.

## Ask for change description

Ask the developer (one question):
> "Describe the change in a few words (will be used as the branch and folder name):"

Convert the answer to kebab-case, max 40 characters.
Example: "add Cognito authorizer to notes API" → `add-cognito-authorizer-notes-api`

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

CHANGE_ID="NNN-brief-description"
git checkout "$DEFAULT_BRANCH"
git pull origin "$DEFAULT_BRANCH"   # only if a remote is configured
git checkout -b "change/$CHANGE_ID"
mkdir -p "changes/$CHANGE_ID"
```

## Output

```json
{ "change_id": "NNN-brief-description", "branch": "change/NNN-brief-description", "areas": ["frontend", "services/notes", "infra"] }
```

Confirm to developer (one line):
```
✅ Branch: change/NNN-brief-description | Areas: <list> | Now writing requirements...
```

Proceed to step-02-requirements.md.
