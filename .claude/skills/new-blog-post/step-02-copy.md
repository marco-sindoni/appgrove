# Step 02 — Write the copy in five languages and build the specification

This is the half a tool cannot do. Write the article, then get it approved.

## Write the English source first

English is the marketing source language; the other four are proper translations, not literal ones.

The voice is the lean one already used across the site (#14 dec.35): **the job first, privacy as the signature
of trust**. Plain sentences, no slogans, no claims you cannot support. A post that oversells is worse than no
post — it is the one thing an AI assistant will quote back at you.

The shape of one language:

- `question` — the guiding question settled in step-01, exactly as a person would ask it. It is the page's
  heading and the anchor of the structured data.
- `title` — the page title without the `· appgrove` suffix (the layout adds it).
- `description` — one honest sentence that works as a search-result snippet and as a social preview.
- `intro` — one or two paragraphs: the problem, and what the reader will be able to do by the end.
- `sections` — the body: a heading plus one to three paragraphs each. Three or four sections is the usual
  shape of a cluster article; a pillar is broader and links out to its clusters.
- `faq` — the question-based part that assistants quote (UC 0041): real questions, answered in two or three
  sentences, no filler.
- `ctaText` — the wording of the internal link to the app's landing. The **URL is not yours to write**: it is
  resolved per language from the landings registry (`landingHref`). Write only the sentence.

Two hard rules:

- **the shape must be identical across the five languages** — same number of intro paragraphs, sections,
  paragraphs per section, FAQ entries. The site test enforces it, and the generator refuses beforehand;
- **no empty strings anywhere**, in any language.

## Build the specification file

Write the whole thing into one JSON file (anywhere outside the repository — a temporary folder is right; it is
an input, not an artifact):

```json
{
  "posts": [
    {
      "key": "chiave-stabile",
      "kind": "article",
      "datePublished": "AAAA-MM-GG",
      "appId": "fatture",
      "pillarKey": "chiave-del-pilastro",
      "content": { "en": { … }, "it": { … }, "fr": { … }, "es": { … }, "de": { … } }
    }
  ]
}
```

- `kind`: `article` for a cluster piece, `pillar` for a pillar.
- `pillarKey`: only for articles. **Never** write `clusterKeys` yourself — the generator wires the reciprocal
  references, and declaring them by hand is refused on purpose.
- **New pillar** (only after the developer approved it at step-01): put the pillar **and** its first article in
  the same `posts` array, pillar first. They are created in one run, already coherent with each other.

`node tools/new-blog-post/generate.mjs --help` prints the full field list.

## Copy approval gate — the human approves

Publishing a blog post is outward-facing. Present the copy and confirm — **one part at a time**, in prose:

- the English source, section by section, saying what each one is for;
- the translations, flagging the choices that were **not** literal (an idiom, a legal term with no clean
  equivalent, a slug that had to diverge from the title);
- what you could **not** verify — a figure, a legal deadline, a claim about a country's rules. Say it plainly
  rather than smoothing it into the prose. Anything factual about invoicing rules or tax deadlines is exactly
  where a confident sentence does the most damage.

**In autopilot and in fast**, you draft all five languages yourself, but you still **stop for the developer's
explicit approval** before generating. This is the outward-facing escalation case of `new-change`.

Do not proceed to step-03 until the copy is approved (or the requested changes are applied).

Proceed to `step-03-genera.md`.
