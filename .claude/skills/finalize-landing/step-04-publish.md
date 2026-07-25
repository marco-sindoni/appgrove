# Step 04 — Preflight, publish, and open the content-PR

The copy is approved, the assets are in place. Now flip the status and open the PR — leaving the merge (and the
actual publication, which the CI does at merge) to the developer.

## Preflight

```bash
node tools/finalize-landing/finalize.mjs preflight --app-id "$APP_ID"
```

The preflight fails (exit ≠ 0) if any language is missing, the `«DA RIFINIRE»` sentinel survives, a screenshot is
still `null`, or the Open Graph image is missing. Fix the cause (go back to step-02/03) — **never** hand-edit
`status` to get around it.

## Publish — flip draft → published

```bash
node tools/finalize-landing/finalize.mjs publish --app-id "$APP_ID"
```

`publish` re-runs the preflight and, only if green, sets `status: 'published'` in the per-app `index.ts`. This is
the single switch that makes the build render the landing (gate #14 dec.52). The tool does **not** deploy.

## Validate through the `site` area (the authoritative gate)

A landing has no application tests: the light close runs the **`site`** area, which is the real gate for a
published landing — vitest (`validateLandings` + template) + `astro build` + post-build check (5-language parity,
Open Graph, hreflang, no broken links):

```bash
./run-tests.sh site
```

Must be green. **E2E/visual note (#10 F)**: if a snapshot diff appears, investigate it — do not re-record blindly.

## Open the content-PR — light flow (branch + PR), then STOP

This is **not** the full `new-change` test/snapshot gate (a landing has no application tests). It is a light
content flow: a branch, a commit of the content + assets, a PR for the human review. **Commit and merge stay the
developer's** — ask consent before committing, and never merge.

```bash
DEFAULT_BRANCH=$(git symbolic-ref --short refs/remotes/origin/HEAD 2>/dev/null | sed 's@^origin/@@'); DEFAULT_BRANCH=${DEFAULT_BRANCH:-main}
git checkout -b "landing/$APP_ID-publish"
git add "site/src/content/landings/$APP_ID" "site/public/landings/$APP_ID"
```

Then **STOP for commit consent**. After the developer consents:

```bash
git commit -m "feat(landing/$APP_ID): finalizza e pubblica la landing (5 lingue + screenshot + OG)"
git push origin "landing/$APP_ID-publish"
gh pr create --base "$DEFAULT_BRANCH" --title "landing($APP_ID): pubblicazione" \
  --body "Landing di $APP_ID finalizzata: copy rifinito 5 lingue, screenshot reali, immagine Open Graph, status published. Al merge la CI valida (5 lingue + SEO) e pubblica (UC 0036)."
```

Then **STOP for merge consent**. Print:

```
🛑 Landing "<app_id>" finalizzata e pubblicata (status: published). Branch: landing/<app_id>-publish
   Site check: <esito ./run-tests.sh site>
   PR aperta per la tua review. Al merge la CI valida (5 lingue + SEO) e pubblica (UC 0036).
Non mergio e non faccio deploy senza il tuo via libera.
```

## If this run closes through `new-change`

When `finalize-landing` is invoked inside a `new-change` (autopilot or a bundled change), record the settled
copy/visual decisions in that change's `decisions.json` and let `new-change` own the commit/merge gates instead
of the light flow above. Standalone (`/finalize-landing <app_id>`), the light branch + PR flow is the close.
