# Step 01 — Scaffold Use Case

All commands run at the monorepo root `/Users/msindoni/Projects/appgrove`.

## Determine the number (global, 4-digit)

**First check the index** `docs/usecases/README.md`:
- **If the use case is already listed in the catalog** (planned but not yet written), **use its reserved `NNNN`** — do
  NOT auto-compute. The catalog pre-reserves numbers (e.g. 0001–0054); writing a planned use case just fills its file.
- **Only if it's a brand-new use case not in the catalog**, assign the **next free** number by scanning all subfolders:

```bash
find docs/usecases -type f -name '[0-9][0-9][0-9][0-9]-*.md' \
  | sed 's#.*/##' | grep -oE '^[0-9]{4}' | sort | tail -1
```

Take `max(this, highest number in the README index)`, add 1, **pad to 4 digits**. If none, start at `0001`. Then add the
new entry to the catalog under the right phase.

## Pick the area

Ask the developer (one question), offering the area list:
> "Which area subfolder? 00-business-legal · 01-devops-infra · 02-local-dev · 03-platform-core · 04-auth · 05-frontend ·
> 06-payments · 07-compliance-gdpr · 08-marketing-site · 09-skills-tooling · 10-apps"

## Ask for the title

Ask (one question):
> "Describe the use case in a few words (used for the filename slug and title):"

Convert to kebab-case, max 40 chars → `slug`.

## Create the file from the template

```bash
AREA="<chosen area, e.g. 06-payments>"
NNNN="<computed number>"
SLUG="<kebab-slug>"
cp docs/usecases/_TEMPLATE.md "docs/usecases/$AREA/$NNNN-$SLUG.md"
```

Then edit the new file's header: set `# UC NNNN — Title`, Area, Fase (ask or infer from the index), Dipendenze, Fonte
decisioni (#NN), Stato `🟡 in corso`.

## Register in the master index

Add a row to the correct phase table in `docs/usecases/README.md`:
`| NNNN | <area-num> | <title> | <deps> | 🟡 |`
(If the use case is brand-new and not in the catalog yet, also note it under the right phase.)

## STOP — scaffold review gate

Print:
```
🛑 Use case scaffolded: docs/usecases/<area>/NNNN-slug.md  | area: <area> | fase: <n>
   Index updated. Confirm number/area/title before I write the detailed body.
```

Wait for the developer's confirmation, then proceed to `step-02-detail.md`.
