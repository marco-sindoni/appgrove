# Step 03 — Screenshots, Open Graph image, asset wiring

Here the deterministic tool does the mechanical visual work. You drive it with the facts from step-01.

## Generate the Open Graph image

The social preview image is an on-brand graphic (not a screenshot): category colour + app name + a benefit line.

```bash
node tools/finalize-landing/finalize.mjs og \
  --app-id "$APP_ID" \
  --app-name "<nome app>" \
  --tagline "<frase-beneficio breve, sorgente EN>" \
  --accent "<cat-token dal manifest, es. cat-violet>"
# → site/public/landings/<app_id>/og.png (1200×630)
```

## Capture the real screenshots (one per language)

The screenshots are of the **real running app**, seeded via route-interception (mock-route — the same technique
as the app's generated end-to-end tests, so no full backend stack is needed). Serve the frontend app in preview,
then capture:

```bash
# 1) Serve the backoffice app (preview build) — from frontend/apps/backoffice:
( cd frontend/apps/backoffice && npm run build && npm run preview )   # serves http://localhost:4173
# 2) In another shell, capture one screenshot per language:
node tools/finalize-landing/finalize.mjs screenshots \
  --app-id "$APP_ID" --base-url http://localhost:4173 \
  --metric "<metric>" --free-cap "<N>"
# → site/public/landings/<app_id>/hero.<lang>.png  (una per lingua)
```

The capture needs a Playwright browser; it reuses the **global** browser cache shared with the frontend, so no
extra browser download. If the browser is unavailable, the tool says so — install it once
(`npx playwright install chromium`) or run from a machine where the frontend e2e suite already runs.

**Review the screenshots** before wiring them: an unrepresentative or broken capture is worse than the
placeholder. Re-run the capture (adjust the seed/route) rather than shipping a bad shot.

## Wire the asset paths into the language files

Once `og.png` and `hero.<lang>.png` exist, cable their paths into the five language files (replaces the `null`
placeholders):

```bash
node tools/finalize-landing/finalize.mjs wire-assets --app-id "$APP_ID"
# screenshot.src → /landings/<app_id>/hero.<lang>.png ; meta.ogImage → /landings/<app_id>/og.png
```

## Complete SEO/GEO

Confirm the SEO fields are complete (title/description/OG — the post-build check requires og:title/description/url
on every published landing) and, where the data model carries it, the GEO material (machine-readable
statement/FAQ, UC 0041). hreflang and Schema.org are wired by the template (UC 0038/0040) — you fill the values.

Proceed to `step-04-publish.md`.
