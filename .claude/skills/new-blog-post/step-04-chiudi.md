# Step 04 — Close through `new-change`

A blog post is content, but it is content that ships with the site build — so it closes through the normal
change workflow, not a private shortcut.

## Open the change

Invoke `new-change` (`docs`-flavoured change: `NNNN-blog-<chiave>`, or
`NNNN-use-case-0042-<chiave>` when the post is part of implementing the blog use case). Let it own the branch,
the requirements file, the decision register and the two consent gates.

What belongs in that change:

- `site/src/content/blog/<chiave>/` — the six generated files;
- `site/src/content/blog/index.ts` — the registry entry;
- `site/src/content/blog/<pilastro>/index.ts` — the updated cluster list (and the new pillar's folder, if one
  was approved).

The specification file stays **out** of the repository: it is the input to the generation, not an artifact.

## What must land in `decisions.json`

The register is what a future author will read instead of this conversation. At minimum:

- which pillar the piece was placed under, and why that one;
- the angle and the guiding question, and the reader they serve;
- the destination app and why its landing was the right internal link (or why the article had to be deferred
  because no published landing existed);
- **if a new pillar was opened**: the developer's decision, quoted — it is an editorial line, and the register
  must show that a human opened it;
- anything the copy could not verify and that a person should check before it stays up.

In autopilot and fast, prefix the entries you settled yourself with `(autopilot)`.

## Coverage, privacy, and the other standing questions

- **End-to-end coverage (UC 0093/0094)**: a new post adds pages but no new interactive path — the blog
  journey, when one exists, already covers "index → post → internal link". Answer the question explicitly
  ("nessun impatto", or extend the journey) and record it.
- **Privacy (UC 0031)**: a post is public marketing content. No personal data, no manifest, no RoPA entry. Say
  so and move on.
- **Freshness**: if the post makes a claim about a feature or a price, it inherits that claim's expiry date.
  Note it, so a future pricing change knows the article exists.

## Then stop

`new-change` asks for commit consent, then for merge consent. The publication itself is the integration
pipeline at merge (UC 0036) — never this skill, and never a manual deploy.
