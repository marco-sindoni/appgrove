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
> "Which area subfolder? 01-business-legal · 02-devops-infra · 03-local-dev · 04-platform-core · 05-auth · 06-frontend ·
> 07-payments · 08-compliance-gdpr · 09-marketing-site · 10-skills-tooling · 11-apps"

## Ask for the title

Ask (one question):
> "Describe the use case in a few words (used for the filename slug and title):"

Convert to kebab-case, max 40 chars → `slug`.

## Create the file from the template

```bash
AREA="<chosen area, e.g. 07-payments>"
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

## Classify it in the end-to-end coverage registry (mandatory, UC 0093/0094)

**Every** use case in `docs/usecases/` must be classified in `docs/testing/copertura-e2e.yaml` — the check
`tools/e2e-coverage` (area `tooling` of `run-tests.sh`) turns **red** on the first unclassified one. Creating the
file and leaving the registry alone therefore breaks the suite: classify it **in the same commit**.

Pick exactly one:

- the use case's **interactive application surface already exists in `main`** → add its number to
  `usecases_con_superficie`, **and** make sure at least one `percorsi` entry references it (a `da-coprire` entry with
  `motivo` and `possiede` is a legitimate answer — an unreferenced surface is a red);
- otherwise → add an entry under `esenzioni` with `categoria` + `motivo`:
  - `senza-superficie` — services, infrastructure, tooling, business/legal work, style libraries (permanent);
  - `vetrina-statica` — showcase-site pages, covered by the `site` area's post-build checks (permanent);
  - `non-implementato` — the surface does not exist yet (a future story). **Temporary and watched**: the check
    rejects it as soon as a `changes/*-use-case-NNNN-*` folder appears, which is when `new-change` must reclassify it.

Format, categories and how to read a red: `docs/testing/README.md`. Verify before moving on:

```bash
node tools/e2e-coverage/check.mjs
```

## STOP — scaffold review gate

Print:
```
🛑 Use case scaffolded: docs/usecases/<area>/NNNN-slug.md  | area: <area> | fase: <n>
   Index updated. Coverage registry: <usecases_con_superficie | esenzione <categoria>>.
   Confirm number/area/title before I write the detailed body.
```

Wait for the developer's confirmation, then proceed to `step-02-detail.md`.
