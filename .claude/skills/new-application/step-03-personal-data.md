# Step 03 — Personal Data Co-pilot

You are filling `docs/compliance/manifests/<app_id>.yaml`, which the generator left as a skeleton.
It is not paperwork: it is the **single source** feeding the processing register (`ropa.*.md`), the
public privacy text, and the export/erasure tooling. A field that is not in the manifest is a field
the export forgets and the erasure leaves behind.

## MANDATORY — no manifest, no app

The change cannot close with an empty or invented manifest ("no contract, no production", #13 L74).
An invented manifest is **worse than none**: it looks like compliance while being fiction, and every
downstream artefact inherits the fiction.

There is also a build-level enforcement: a field annotated `@PersonalData` that is not declared in
the manifest turns `mvn test` red (UC 0030). You cannot quietly skip this step.

## Co-pilot, not checklist

For **each** field of the new app's domain that could concern a person (SKILL.md "Questioning
style" — one at a time, in prose):

1. **Elicit the purpose**: "tell me what this field is and what it is for". Not its type — what the
   app *does* with it.
2. **Deduce and propose, showing your reasoning**: nature, purpose, legal basis, retention. Say why.
   Then ask deepening questions **only where genuinely ambiguous** — and be alert to the ambiguities
   that actually matter. The classic: a phone number *to deliver the service* and a phone number
   *to call the customer with offers later* are the same column and a different legal basis.
3. **Have it confirmed explicitly** — "purpose = X, basis = Y, retention = Z, ordinary category —
   does that match?" — and only then write the entry, **in both languages** (Italian and English:
   the parity check is enforced by `tools/compliance`).
4. Annotate the Java field with `@PersonalData` so the build-level pairing holds.

## Article 9 — stop and warn loudly

Health, biometric, genetic, political opinions, religious beliefs, sexual orientation, trade-union
membership: these are **special categories**. If any appear:

- **Warn strongly and explicitly.** Never proceed quietly, and never let it pass because the field
  "is only optional" — optional special-category data is still special-category data.
- Screen for an **impact assessment** against the article 35 criteria (#13 K67).
- Require a **reinforced legal basis**: the ordinary bases are not enough on their own.

Say plainly that this materially raises what the app must do before going live. An app that can
avoid special categories usually should.

## Guardrail — pseudonymisation is not erasure

If the erasure design replaces names with codes while keeping the rows, that is **pseudonymisation**
(#13 L72): the data is still personal data, and the person's deletion request is not satisfied. Say
so and reject the design. The generated `<App>DataContract` performs **physical** deletion and
writes a purge audit row as proof — keep it that way.

Verify the contract really covers the new domain: every table holding personal data must appear in
both `exportData` and `purgeData`. A table added to the domain and forgotten here is the single most
likely compliance bug in a new app.

## Regenerate the register and classify the change

```bash
( cd tools/compliance && npm run assemble && npm run check )
( cd services && mvn -B -pl <app_id> -am test )    # includes the @PersonalData ↔ manifest pairing
```

Then classify the change and **record the classification** (major/minor + affected component +
rationale) in the change's `requirements.md` and implementation log — a new app introducing new
purposes and new data categories is normally a **material** change, hence **major**, hence a scoped
re-acceptance (UC 0056). Do not soften this to avoid the re-acceptance: the classification describes
reality, it is not a lever.

If the app brings a **new external integration**, flag it as a **potential new sub-processor**
(#13 C49) so it reaches the sub-processor list and the client notice.

The co-pilot takes this to a solid draft; validation remains a legal-review matter
(`docs/_REVISIONE-LEGALE.md`).

Proceed to `step-04-close.md`.
