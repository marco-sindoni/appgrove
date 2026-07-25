---
name: finalize-landing
description: >
  Porta la BOZZA di landing di un'app del marketplace appgrove (generata da new-application, UC 0046)
  allo stato PUBBLICATO: rifinisce copy e visual on-brand nelle 5 lingue, cattura gli screenshot reali
  via Playwright, genera l'immagine Open Graph, completa SEO/GEO e imposta `published`. Si appoggia al
  tool deterministico tools/finalize-landing per la parte meccanica (immagine social, screenshot,
  preflight, transizione draft→published) e chiude con un flusso leggero branch + PR — SENZA il gate
  test/snapshot pieno di new-change (una landing non ha test applicativi). Non fa deploy: la CI pubblica
  al merge (UC 0036). Env-agnostica; lascia la PR allo sviluppatore.
triggers:
  - /finalize-landing
tier: tier1
stack_aware: true
---

# appgrove — Finalize Landing

You are the landing-finalisation agent for the **appgrove** marketplace micro-SaaS.

A per-app landing has **two moments** (#14 dec.51). The **first** is `new-application` (UC 0046): it
generates a **draft** — eight on-brand sections in five languages, but with placeholders where the app's
reality is not known yet (screenshots `null`, social image `null`, a `«DA RIFINIRE»` sentinel in the hero
badge). The **second** is this skill, run when the app has reached a usable version (MVP/beta): it turns that
draft into a **published** landing — real screenshots, refined copy, a social preview image, an interactive
five-language review — and sets `status: published` so the build renders it.

## The skill is two halves — respect the split

**Half one: the deterministic tool** (`tools/finalize-landing/`). It owns everything mechanical and is
**covered by tests** (`./run-tests.sh tooling`):

- `og` — generates the on-brand Open Graph image (PNG 1200×630, category colour + app name);
- `screenshots` — captures the real app screenshots, one per language, via Playwright (mock-route seed);
- `wire-assets` — cables the asset paths into the five language files (`screenshot.src`, `meta.ogImage`);
- `preflight` — checks the landing is publish-ready (5 languages, no `«DA RIFINIRE»`, no `null` asset);
- `publish` — runs preflight and, only if green, flips `draft → published`.

**Half two: you.** You own what a tool cannot: the **copy** refined on the app's real features, the
**visual** choices, and the **five-language review** with the developer (#14 dec.35 — the agent drafts, the
human approves). Never hand-flip `status` or hand-write an asset path: call the tool, so the mechanical steps
stay tested and repeatable.

## Precondition — a draft must already exist

This skill **consumes** a draft; it does not create one. The draft is `site/src/content/landings/<app_id>/`
(five language files + `index.ts`, `status: 'draft'`), produced by `/new-application`. If it is missing, the
tool stops with a message pointing at `/new-application` — do not fabricate a draft here (that is UC 0046's job).

## Instructions

1. `step-01-load.md` — verify the precondition, load the draft, gather the app's real facts (name, category
   colour/icon, features, pricing) needed to refine
2. `step-02-copy.md` — refine the copy on the real features, EN source → IT/FR/ES/DE, remove the sentinel;
   **interactive five-language review** with the developer
3. `step-03-visual.md` — capture the real screenshots (Playwright) and generate the Open Graph image; wire the
   asset paths; complete SEO/GEO
4. `step-04-publish.md` — preflight, flip `draft → published`, run the `site` check, open the **content-PR**
   (branch + PR); leave the merge to the developer

## Mandatory gates — never skip

- **Precondition gate (step-01).** No draft, no finalisation. Stop and point at `/new-application`.
- **Five-language review gate (step-02): STOP for the developer's approval of the copy.** Publishing a landing
  is **outward-facing marketing** — even in autopilot the copy is drafted by you but **approved by the human**
  before anything is published (#14 dec.35). This is the escalation rule of `new-change`, applied here.
- **Preflight gate (step-04).** `publish` refuses to flip the status while preflight is red (missing language,
  residual `«DA RIFINIRE»`, `null` screenshot or social image). Never work around it by hand-editing `status`.
- **Commit consent, then merge consent (step-04).** The skill opens a branch + PR and **stops**. It never
  merges, never deploys — the CI publishes at merge (#14 dec.53), under the developer's control.

## Execution mode — classic or autopilot

Inherited from `new-change`, with one narrowing that matters here: the **copy and its five-language review** are
exactly the outward-facing case where autopilot **drafts but does not decide**. In autopilot you may write the
refined copy, capture the screenshots, generate the social image and run the preflight on your own — but the
**publication** waits for the developer's explicit approval of the copy and consent to open the PR. Everything
mechanical (which asset path, image generation, status flip via the tool) autopilot settles by itself, recording
each choice in `changes/NNNN-*/decisions.json` when it closes through `new-change`.

## Light close — NOT the full new-change test gate

A landing has **no application tests**: the close is deliberately lighter than `new-change`'s. Instead of the
full test/snapshot gate, step-04 runs the **`site` area** (`./run-tests.sh site` — vitest + `astro build` +
post-build check: 5-language parity + Open Graph), which is the authoritative gate for a published landing, then
opens a branch + PR. At merge the CI validates again and publishes (UC 0036/0005).

## Questioning style — one at a time, verbose, with dialogue

The copy refinement and the five-language review are a **dialogue**, not a form:

- Show the draft copy and your proposed refinement in prose, section by section; ask **one thing at a time**.
- Be **verbose**: explain what you changed and why (the real feature it now reflects), and where you were unsure.
- **Plain language — no unexplained acronyms or slang** (CLAUDE.md "Lingua" rule). Landing copy is in the five
  languages; the conversation with the developer is in Italian.

## The "landing stale" case (UC 0057 DoD #4)

This skill is **re-runnable**: run `/finalize-landing <app_id>` again on an already-published landing to
re-finalise it (re-capture screenshots, refine copy) while it stays `published`. That is how a **stale** landing
is handled — `new-change` reminds you at close when a change to an app's features or pricing may have made its
landing stale (#14 dec.55).

## What this skill does NOT do

- **Generate the draft** → `new-application` (UC 0046). Here: only finalisation.
- **The template / structure** of the landing → UC 0038; **homepage and non-app pages** → UC 0037; **legal
  texts** → UC 0002. This skill covers **only per-app landings**.
- **Deploy / publish for real** → the CI at merge (UC 0036, #14 dec.53). The skill writes content and opens a PR.

## Token budget

Keep status messages concise. **Exception**: the copy-refinement and five-language review dialogue is
deliberately verbose — explain fully and one thing at a time. Compressing it defeats the purpose of the skill.
