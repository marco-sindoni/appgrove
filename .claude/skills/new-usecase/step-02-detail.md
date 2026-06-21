# Step 02 — Write the drill-down

## MANDATORY clarification gate — before writing the body

The use case must be consistent with the decision docs (`docs/01..14`, `CLAUDE.md` invariants) and the related area docs.
If any flow/screen/data/permission is unclear or admits viable alternatives, **ask targeted questions first** (concrete
either/or) and settle them. Do not invent decisions.

## Fill the template sections

Working through `docs/usecases/<area>/NNNN-slug.md` (from `_TEMPLATE.md`), fill with the developer:

1. **Obiettivo / Scope** — what it delivers, in/out of scope.
2. **Attori & ruoli**.
3. **Precondizioni**.
4. **Flusso principale** — numbered steps.
5. **Flussi alternativi / edge / errori**.
6. **Schermate & stati** (UI) — or **Risorse & runbook** (devops/infra).
7. **Dati toccati** — entities/schema; if personal data → category/purpose/legal-basis/retention (manifest GDPR #13 C),
   mark `@PersonalData` fields.
8. **Permessi & gate** — entitlement/role/quota; **uphold invariants** (tenant_id from JWT, row-level filter, etc.).
9. **Requisiti di test** — unit/integration/security/E2E/L1-L3 per #10.
10. **Riferimenti & DoD** — decision refs (#NN) + objective Definition of Done.

Remove the scaffolding note. Set Stato `🟢` when complete; update the index row status.

If the body is intentionally **deferred** (scaffold only for now), leave Stato `🟡` and say so — that's allowed.

## MANDATORY STOP — commit consent gate

Summarize the file written + the index update, then ask explicit consent to commit. Do **not** commit without it.

Print:
```
🛑 Use case NNNN drafted: docs/usecases/<area>/NNNN-slug.md (+ index row). Reply with consent to commit.
```

On consent:
```bash
git add docs/usecases/<area>/NNNN-slug.md docs/usecases/README.md
git commit -m "docs(usecase NNNN): <title>"
```

Implementation comes later via **new-change** (folder `NNNN-use-case-YYYY-...` referencing this spec).
