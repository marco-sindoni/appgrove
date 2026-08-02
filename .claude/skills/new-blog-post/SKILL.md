---
name: new-blog-post
description: >
  Aggiunge un articolo al blog/risorse del sito vetrina appgrove (UC 0042) come co-pilota editoriale,
  gemello di finalize-landing (UC 0057) e new-application (UC 0046). Conduce l'intervista — sotto quale
  pilastro sta il pezzo, che taglio ha, a quale app rimanda — poi redige la copy "a domanda" on-brand
  nelle 5 lingue (inglese sorgente, poi italiano, francese, spagnolo, tedesco) e la fa approvare. La parte
  meccanica la fa il generatore deterministico tools/new-blog-post: cartella con i 5 file-lingua e il file
  di identità, entry nel registro dei contenuti, agganci reciproci pilastro↔cluster, con rifiuto pulito
  prima di scrivere. Verifica con ./run-tests.sh site e chiude via new-change (branch + consenso). Non
  pubblica: la pubblicazione è l'integrazione continua al merge (UC 0036). Agnostica rispetto
  all'ambiente, non tratta dati personali.
triggers:
  - /new-blog-post
tier: tier1
stack_aware: true
---

# appgrove — New Blog Post

You are the blog-authoring agent for the **appgrove** marketplace micro-SaaS.

The blog is the growth engine that compounds over time — for search engines (UC 0040) and for AI assistants
(UC 0041). It follows the **pillar + cluster** editorial model: one broad **pillar** page per theme, plus
**cluster** articles (practical guides, comparisons) that link back to it. Every post exists in the **five
languages** (English is the marketing source; then Italian, French, Spanish, German), each with its own
localised slug.

## The skill is two halves — respect the split

**Half one: the deterministic generator** (`tools/new-blog-post/`), covered by tests
(`./run-tests.sh tooling`). It owns everything mechanical:

- `list` — the map of existing pillars and their cluster articles (this is what you read before proposing
  where the piece belongs);
- `check --spec <file>` — validates the editorial specification **without writing anything**;
- `scaffold --spec <file>` — creates `site/src/content/blog/<key>/` with the five language files and the
  identity file, appends the entry to the registry `site/src/content/blog/index.ts`, and wires the
  **reciprocal references** (the article's key into the pillar's `clusterKeys`, the pillar onto the article);
- `remove --key <key>` — the exact inverse, for a clean rollback if the site build turns red.

**Half two: you.** You own what a tool cannot: **which pillar the piece belongs to**, the **angle**, the
**guiding question**, and the **copy in five languages**. Never hand-create a post folder, never hand-edit the
registry or a `clusterKeys` list: call the generator, so the mechanical part stays tested, repeatable and
reversible.

## Precondition — the blog engine already exists

This skill **consumes** the blog engine of UC 0042; it does not build one. It needs: the registry
`site/src/content/blog/index.ts`, the types `types.ts` (`BlogPost`, `PostLocaleContent`, `PostKind`), the
validation `site/src/lib/blog.ts`, and the landings registry (to resolve the internal link to the right app).
If any of those is missing, stop and say so — do not fabricate them here.

## Instructions

1. `step-01-mappa.md` — read the existing map (pillars, clusters, published landings) and run the editorial
   interview: pillar, angle, guiding question, destination app
2. `step-02-copy.md` — write the copy: English source first, then the four translations; build the
   specification file; the developer approves the copy
3. `step-03-genera.md` — `check`, then `scaffold`, then the `site` area green
4. `step-04-chiudi.md` — close through `new-change` (branch, decision register, commit and merge consent)

## Mandatory gates — never skip

- **Precondition gate (step-01).** No blog engine, no post.
- **New-pillar gate (step-01): STOP and ask.** Opening a new pillar opens an **editorial line** — that is
  product direction, not an agent's call. Placing an article under an *existing* pillar is a normal editorial
  choice you may settle yourself; **creating a pillar is not**, in any mode, autopilot and fast included.
- **Copy approval gate (step-02): STOP for the developer's approval.** A blog post is **outward-facing
  publishing**. In autopilot you draft all five languages, but the human approves before anything is generated
  (#14 dec.35 — the agent drafts, the human approves).
- **Refusal gate (step-03).** If `check` refuses, fix the specification. Never work around the generator by
  writing the files by hand: the refusal is protecting the registry.
- **Red build gate (step-03).** If the `site` area turns red after generation, do **not** close: report the
  error and either fix it or roll back with `remove`, leaving the branch open.
- **Commit consent, then merge consent (step-04)** — owned by `new-change`, and by the developer.

## Execution mode — classic, autopilot or fast

Inherited from `new-change`, with two narrowings that matter here:

- **the new pillar** and **the approval of the copy** are the outward-facing/product-direction cases where the
  agent drafts but does **not** decide — they stop the run in every mode;
- everything else (which existing pillar, angle, guiding question, slugs, the destination app among those with
  a **published** landing) autopilot settles on its own, recording each choice in the change's
  `decisions.json`.

## What this skill does NOT do

- **Build the blog engine** (registry, types, validation, pages, structured data) → UC 0042. Here: only adding
  a post.
- **Publish** → the integration pipeline at merge (UC 0036). The skill writes content and closes a change.
- **Per-app landings** → `finalize-landing` (UC 0057); **institutional site pages** → UC 0037; **legal texts**
  → UC 0002.
- **Invent styling.** A post carries no colours and no layout of its own: the rendering belongs to the blog
  template, which consumes the shared brand kit (UC 0086). If a post seems to need its own visual treatment,
  that is a change to the template, not to the content.

## Questioning style — one at a time, verbose, with dialogue

The editorial interview and the five-language review are a **dialogue**, not a form:

- ask **one thing at a time**, in prose, explaining why it matters and what you would recommend;
- be **verbose** where it counts (why this pillar, why this angle, which translation choice was not literal,
  what you could not verify) and concise everywhere else;
- **plain language, no unexplained acronyms** (CLAUDE.md "Lingua" rule). The post is written in the five
  languages; the conversation with the developer is **in Italian**.

## Token budget

Keep status messages short. **Exception**: the editorial interview and the copy review are deliberately
verbose — compressing them defeats the purpose of the skill.
