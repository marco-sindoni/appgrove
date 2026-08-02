# Step 01 — Read the map, then run the editorial interview

All commands run at the monorepo root `/Users/msindoni/Projects/appgrove`.

## Settle the execution mode

Same rule as `new-change`: if the invocation declares classic / autopilot / fast, acknowledge it in one line.
If it does not, ask it as the very first action (single `AskUserQuestion`, three options). Two decisions stop
the run **in every mode**, including fast: opening a **new pillar** (step-01) and the **approval of the copy**
(step-02).

## Precondition — the blog engine

```bash
test -f site/src/content/blog/index.ts && test -f site/src/content/blog/types.ts && test -f site/src/lib/blog.ts
```

Missing → stop: the blog engine is UC 0042, and this skill only adds posts to it.

## Read the existing map

```bash
node tools/new-blog-post/generate.mjs list
```

It prints the pillars, the cluster articles hanging off each, and the English slug of every post. This is the
map you reason on — never guess the state of the registry from memory.

Also read which apps have a **published** landing (an article's internal link must resolve to a real page):

```bash
node tools/new-blog-post/generate.mjs list --json   # oppure: grep -l "status: 'published'" site/src/content/landings/*/index.ts
```

## The editorial interview — one question at a time

Ask in prose, verbosely, in Italian, waiting after each. In autopilot you state each question with the options
and your recommendation, then answer it yourself — **except the new-pillar question, which always stops.**

1. **Di cosa parla il pezzo, e a chi serve?** The real subject, and the reader who has that problem. Everything
   else follows from this.
2. **Sotto quale pilastro sta?** Show the existing pillars and argue for the one that fits. Placing an article
   under an existing pillar is an editorial choice you may settle. If none of them covers the theme, this
   becomes the **new-pillar gate**:

   > 🛑 Questo pezzo non ha un pilastro che lo copra. Aprirne uno nuovo significa aprire una **linea
   > editoriale** su questo tema: è una scelta di direzione di prodotto, non mia. Vuoi che apra il pilastro
   > "<nome proposto>" — e con quale promessa? Oppure preferisci ricondurre il pezzo a "<pilastro esistente>"?

   Stop and wait. Do not open a pillar on your own, in any mode.
3. **Che taglio ha?** Practical guide, comparison, explainer. The angle decides the shape of the body and the
   FAQ.
4. **Qual è la domanda-guida?** Formulated the way a person would ask an AI assistant (UC 0041) — it becomes
   the page's heading and the anchor of the structured data. One clear question, no marketing slogan.
5. **A quale app rimanda?** Only apps with a **published** landing: the internal link is resolved per language
   from the landings registry, and the generator refuses an app without one. If the natural destination has no
   published landing yet, say so and either pick another app or defer the article — and write the deferral
   down (the owning use case, or `docs/_BACKLOG.md`).

## Settle the key and the slugs

- **key**: the stable, untranslated folder name and the anchor of the pillar↔cluster references. Short,
  lowercase, hyphenated, and readable in a year (`fattura-elettronica-a-norma`, not `articolo-3`).
- **slug per language**: each language gets its own localised slug (#14 31) — a translation of the topic, not
  of the key. Lowercase `[a-z0-9-]`, never `index`, never one already used in that language (the map above
  tells you which are taken).

Record every settled point in the change's `decisions.json` when you close through `new-change` (step-04).

Proceed to `step-02-copy.md`.
