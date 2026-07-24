# Step 04 — Close Change

All commands run at the monorepo root.

## Gather implementation data

Ask the developer (one question):
> "Any decisions made or issues encountered during implementation worth noting?"

*(Autopilot: do not ask — the decisions are the ones you already recorded in `decisions.json`; re-read them and
carry them into the log.)*

Then re-read `changes/NNNN-*/decisions.json`: it is the backbone of the log you are about to write.

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
**Modalità**: <classica | autopilot — se autopilot, le risposte alle domande di approfondimento
sono dell'agente e sono tracciate in [decisions.json](decisions.json)>

## File modificati

| File | Azione |
|---|---|
| <path> | Creato / Modificato / Eliminato |

## Cosa è stato fatto

<2-3 frasi che descrivono cosa è stato implementato e come>

## Decisioni prese

<sintesi in prosa delle decisioni; il registro completo e strutturato è in
[decisions.json](decisions.json) — le due viste devono raccontare la stessa storia.
Se la change è stata condotta in **autopilot**, dirlo qui e indicare quali decisioni
sono state prese dall'agente. Oppure "Nessuna — implementato come da specifica">

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

## Verify the privacy/RoPA gate ran (UC 0031)

Before the commit gate, confirm the step-03 privacy gate was executed on the final diff:

```bash
( cd tools/compliance && npm run privacy-scan )
```

- **Exit 0** → record "gate privacy: nessun segnale" in the implementation log (if not already there).
- **Exit ≠ 0** → every reported signal must have been classified via the step-03 co-pilot (manifest + RoPA updated,
  `@PersonalData` annotated, MAJOR/MINOR + sub-processor flags recorded in the log). If any signal is unaddressed, go
  back to step-03 **before** asking for commit consent — never close a change with unclassified privacy signals.

## Verify and close the decision register `decisions.json` (non-negotiable)

Before the commit gate, check the register is complete, valid and coherent:

```bash
node -e "const d=JSON.parse(require('fs').readFileSync(process.argv[1],'utf8'));
if(!Array.isArray(d)||d.length===0) throw new Error('decisions.json vuoto o non è un array');
d.forEach((e,i)=>{ if(e.id!==i+1) throw new Error('id non progressivo alla posizione '+i);
  if(typeof e.decision!=='string'||!e.decision.trim()) throw new Error('decision mancante per id '+e.id);
  if(e.files!==undefined){ if(!Array.isArray(e.files)) throw new Error('files non è un array per id '+e.id);
    e.files.forEach(f=>{ if(!f.file||!f.description) throw new Error('file/description mancante per id '+e.id); }); } });
console.log('decisions.json ok — '+d.length+' decisioni');" "changes/NNNN-brief-description/decisions.json"
```

Then verify by reading, not only by parsing:

- every question settled during this change (clarification, requirements, design, privacy classification) has an entry —
  in **autopilot** that means *every* question, each marked `(autopilot)`;
- every deferred decision tracked in a use case or `docs/_BACKLOG.md` has an entry pointing at where it was written;
- the register and `implementation-log.md` do not contradict each other;
- `files` entries reference paths that actually exist in the diff (or explain why they do not).

If the register is missing, empty, or clearly thinner than what the change actually decided, **fill it now from the
implementation and say so** — but treat that as a process failure to avoid next time: entries are meant to be written
when the decisions are taken. Do not ask for commit consent with an unverified register.

## Traccia le decisioni differite (non-negoziabile)

Before the commit gate, confirm that **every** architectural decision, drift, or open point you
encountered during this change that belongs to a *different* use case (or isn't ripe to decide) has
been written into the right place — that use case's file (`docs/usecases/<area>/NNNN-*.md`, section
**"## Punti aperti / decisioni differite"**) or `docs/_BACKLOG.md` if cross-cutting — capturing *what*,
*why deferred*, *which UC owns it*. List these tracked points in the implementation log's
"Note per il revisore" (or state "Nessuna decisione differita"). This is the CLAUDE.md constitution
rule ("Tracciamento delle decisioni differite"): **never** close a change with such points living
only in chat.

## Verify the scaffold source-path gate (UC 0046)

Before the commit gate, check whether this change touched a **source path** — one of the paths the
`new-application` source templates are derived from (declared in `tools/scaffold-parity/source-paths.json`):

```bash
node tools/scaffold-parity/source-paths-scan.mjs
```

- **Exit 0** → no source path touched. Nothing to do.
- **Exit ≠ 0** → the change moved a path that feeds the templates. Pick **one** of the two, never neither:
  1. **Update the templates** in `tools/new-application/templates/` **in this same commit**, so newly
     generated apps inherit the change. Confirm with `node tools/scaffold-parity/parity-check.mjs` (exit 0).
  2. **Record the justification** in `docs/_PARITA-SCAFFOLD.md` (the deliberate-deviation register), if the
     change is specific to `fatture`'s own domain — or otherwise not generalisable — and the templates must
     deliberately stay behind. State *what*, *why*, and *what would close it*.

Then note the outcome in the implementation log ("gate parità scaffold: ..."). Templates that age in
silence break nothing today and make every app of tomorrow born out of date — which is exactly why this
gate is not optional. If neither branch has been taken, print:

```
🛑 Change NNNN touched a scaffold source path but the templates were neither updated nor a deviation
   recorded: <list of source paths touched>
Choose one — update tools/new-application/templates/, or record the reason in docs/_PARITA-SCAFFOLD.md —
and tell me which. I will not ask for commit consent until this is resolved.
```

Then STOP and resolve it before going further. This gate is symmetric to the deferred-decisions one above:
both exist because the thing they guard is invisible at commit time and expensive later.

## MANDATORY STOP — commit consent gate

Do **not** commit yet. Summarize what will be committed (the changed files, the `implementation-log.md` and the
`decisions.json`) and ask the developer's explicit consent to commit. **This gate is never auto-approved — not
even in autopilot**: it is where the developer sees what the agent decided, and that is precisely the point.

Print:
```
🛑 Implementation complete. Ready to commit change NNNN (areas: <list>, modalità: <classica|autopilot>):
   <short list of changed files>
   Decisioni registrate: <N> in changes/NNNN-*/decisions.json (<in autopilot: quante prese dall'agente>)
Reply with explicit consent and I will commit. I will not commit without your go-ahead.
```

Then STOP. Only after the developer explicitly consents, run:
```bash
git add changes/NNNN-brief-description/implementation-log.md changes/NNNN-brief-description/decisions.json
git add -A
git commit -m "chore(change/NNNN): implementation complete"
git push origin change/NNNN-brief-description   # only if a remote is configured
```

## MANDATORY STOP — merge consent gate

After committing, leave the change branch **unmerged**. Do **not** merge to the default branch and
do **not** run the merge command on your own — merging requires the developer's separate explicit
consent. **Autopilot does not weaken this gate either**: it answers questions, it never merges.

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
