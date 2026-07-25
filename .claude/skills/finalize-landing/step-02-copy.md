# Step 02 — Refine the copy (five languages) and review with the developer

The draft copy is generic and marked with the `«DA RIFINIRE»` sentinel. Here you make it the app's real story,
in five languages, and get the developer's approval. This is the part a tool cannot do — and the gate that keeps
a landing honest.

## Refine, section by section

Refine the eight sections on the app's **real** features (gathered in step-01), keeping the on-brand voice
(job first, privacy as the signature of trust — #14 messaging):

- **EN is the source** marketing language; then IT/FR/ES/DE are proper translations, not literal ones.
- Replace the generic feature/benefit copy with the app's actual features (3–6 features, 2–3 steps — the site
  test enforces these bounds).
- Make the **pricing section match the real listino** (`pricing/<app_id>.yaml`): tier names, prices, free cap,
  trial. Do not invent numbers.
- **Remove the `«DA RIFINIRE»` sentinel** from the hero `badge` in every language, replacing it with the real
  badge (e.g. `all-EU · GDPR-first`). The preflight (step-04) fails while the sentinel survives.
- Complete `meta.title` / `meta.description` for SEO — concise, honest, keyword-aware (UC 0040). Leave
  `screenshot.src` and `meta.ogImage` as they are; step-03 fills them via the tool.

Edit the five language files directly. Keep the **shape identical across languages** (the type + the site test
enforce five-language parity; a missing or extra field breaks the build).

## Interactive five-language review — the human approves (gate)

Publishing a landing is outward-facing marketing: the developer approves the copy before anything is published
(#14 dec.35). Show your refinement and confirm — **one section at a time**, in prose, verbose:

- what you changed and why (the real feature it now reflects);
- the EN source, and note the translations that needed a non-literal choice;
- where you were unsure (a claim you could not verify, a term with no clean translation).

**In autopilot** you draft all five languages yourself and present them, but you still **stop for the
developer's explicit approval of the copy** before step-04 publishes — this is the outward-facing escalation
case. Record each settled copy decision in the change's `decisions.json` when you close through `new-change`.

Do not proceed to step-03 until the copy is approved (or the developer has asked for the changes and you have
applied them).

Proceed to `step-03-visual.md`.
