# Step 02 — Write Requirements

## MANDATORY clarification gate — before writing anything

Review the instructions the developer gave. If any aspect is **unclear, doubtful, or admits viable alternatives** (scope
boundaries, design choices, naming, which areas it touches, which appgrove invariant applies, trade-offs), resolve it **before**
writing `requirements.md` — never write on assumptions.

Follow the **Questioning style** (see SKILL.md): ask **one question at a time**, in **prose**, **verbosely** — explain the
context, why it matters, the options you see and your reasoning/recommendation. Then **STOP and wait** for the answer. Treat it
as a **dialogue**: if the reply needs deepening or opens follow-ups, keep discussing until the point is genuinely settled, then
record the decision and move to the **next** question. Do **not** batch questions and do **not** use a compact multiple-choice
wizard with terse options.

If, and only if, the instructions are already fully clear with no real alternatives, skip straight to the template below
without inventing questions. Only proceed once all open points are resolved.

## Fill the requirements template

Gather what you need to fill the template with up to 3 questions — but ask them **one at a time**, not all at once, following
the Questioning style (verbose prose, explain context, STOP and wait after each, allow dialogue before settling). Ask the next
only once the previous is answered:

> 1. Quale problema risolve questa change, o quale obiettivo raggiunge?
> 2. Cosa è in scope? (quali aree — infra/frontend/services/<app> — file, comportamenti)
> 3. Cosa è esplicitamente fuori scope o non deve cambiare?

(If the clarification gate above already settled some of these, don't re-ask — just confirm and fill them in.)

Then write `changes/NNNN-brief-description/requirements.md` **in Italian** (the artifact is in Italian; the skill
instructions stay in English):

```markdown
# Change NNNN: Titolo breve

**Branch**: `change/NNNN-descrizione-breve`
**Aree**: <infra | frontend | services/<app>, … — una o più>
**Data**: AAAA-MM-GG
**Autore**: (nome dello sviluppatore o "Platform Engineering")
**Use case sorgente**: <`docs/usecases/<area>/YYYY-*.md` se la change implementa uno use case del catalogo, altrimenti "Nessuno (change ad-hoc)">
**Tocca dati personali?**: <Sì / No — se Sì si applica il gate privacy/RoPA di step-03 (UC 0031: scanner + co-pilota → manifesto/RoPA; classificazione **MAJOR/MINOR** del cambio con motivazione, registrata qui finché `content/legal/` non esiste — poi pilota il bump versione PP/ToS); l'enforcement CI `@PersonalData`↔manifesto è in `mvn test` (UC 0030)>

## Problema / Obiettivo

<risposta alla domanda 1>

## Scope

<risposta alla domanda 2>

## Fuori scope

<risposta alla domanda 3>

## Criteri di accettazione

- [ ] <2-3 criteri concreti e verificabili derivati da quanto sopra>

## Invarianti appgrove toccati

<Elenca quelli che la change deve mantenere, o "Nessuno": tenant_id solo dal JWT; filtro row-level
WHERE tenant_id; pattern modulo Terraform microsaas_app; logging strutturato (tenant_id/app_id/user_id).
Nota come la change mantiene vero ciascuno.>

## Requisiti di test (opzionale)

<Includere solo se servono test specifici oltre a quanto implicano le modifiche — test di accettazione,
edge case, regression guard (es. un test che verifica che tenant_id sia letto solo dal JWT).
Altrimenti omettere. step-03 deve soddisfare quanto elencato qui.>

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | Sì / No |
| Contratto cross-area | Sì / No / N/A (frontend ↔ API servizio, o servizio ↔ infra) |
| Version bump | patch / minor / major / nessuno |
```

Commit the file:
```bash
git add changes/NNNN-brief-description/requirements.md
git commit -m "chore(change/NNNN): write requirements"
```

## MANDATORY STOP — requirements review gate

After committing, **STOP. Do not start implementing.** Requirements must be reviewed and
explicitly approved by the developer before any implementation begins.

Print:
```
🛑 requirements.md written and committed. Please review it.
   Reply with your approval (or requested changes) before I start implementing.
```

Wait for the developer's explicit approval. If they request changes, update `requirements.md`,
re-commit (`chore(change/NNNN): revise requirements`), and ask again. Only once they approve do
you proceed to step-03-implement.md.
