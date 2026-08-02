# Step 03 — Generate, then prove it with the `site` area

## Dry run first — `check`

```bash
node tools/new-blog-post/generate.mjs check --spec /percorso/della/specifica.json
```

`check` writes nothing. It refuses when a language is missing, a string is empty, a translation has a
different shape from the English source, a slug is malformed / reserved / already taken, the key already
exists, the pillar does not exist or is not a pillar, or the destination app has no **published** landing.

**A refusal is information, not an obstacle.** Fix the specification — go back to step-02 if the fix is
editorial (a slug that has to change, a section that has to exist in every language). Never sidestep the
generator by writing the files by hand: the refusal is what protects the registry.

## Generate — `scaffold`

```bash
node tools/new-blog-post/generate.mjs scaffold --spec /percorso/della/specifica.json
```

It re-validates and then, in one transaction, creates the post folder (five language files plus the identity
file), appends the import and the entry to the registry, and wires the reciprocal references
(`clusterKeys` on the pillar, `pillarKey` on the article). If anything fails mid-way it rolls back: there is no
half-written state to clean up.

Read the list of touched files it prints — it is exactly what you will be committing.

## Prove it — the `site` area is the authoritative gate

```bash
./run-tests.sh site
```

Three things must be green: the vitest suite (five-language parity of the content, registry validation of
UC 0042 — slugs, reciprocal references, internal link resolving to a published landing), the real
`astro build`, and the post-build check (five languages, hreflang, no broken internal links).

Open the generated pages and read them once with your own eyes — the tests prove the shape, not the prose:

```bash
( cd site && npm run dev )   # poi /en/blog/<slug>/ e /it/blog/<slug-italiano>/
```

## If it turns red

Do **not** close. Report the error, then either fix it or roll back cleanly:

```bash
node tools/new-blog-post/generate.mjs remove --key <chiave>
```

`remove` is the exact inverse of the generation: the registry and the pillar go back to their previous state,
byte for byte. Fix the specification and generate again — that is the whole point of having an inverse.

Proceed to `step-04-chiudi.md`.
