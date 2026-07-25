# Log di implementazione — Change 0050 · SEO tecnico del sito vetrina (UC 0040)

**Aree toccate**: `site/` (Astro SSG) · `.github/workflows/` (CI) · `tools/` (copie slug riservati) · `docs/`.
**Modalità**: autopilot. Esito: **tutte le suite eseguite verdi** (`site`, `tooling`).

## Cosa è stato fatto

### Fondamenta: percorsi per-lingua + hreflang corretti
- `site/src/lib/routes.ts` (nuovo): slug brand localizzati (`BRAND_SLUGS`), helper `brandPath`/`brandHref`/
  `brandHreflangPaths`/`slugHreflangPaths` e `brandParams` per il dispatcher.
- `site/src/lib/i18n.ts`: aggiunta `hreflangAlternatesByLocale(Record<Locale,path>)` per hreflang con percorso
  diverso per lingua.
- **Bug corretto**: gli hreflang delle landing puntavano a URL inesistenti (slug uguale per tutte le lingue).
  Ora ogni hreflang usa il percorso reale della sua lingua; nuova asserzione post-build che li verifica *risolti*.

### Slug brand localizzati (why/pricing)
- Pagine brand statiche rimosse (`[lang]/why.astro`, `[lang]/pricing.astro`); corpi estratti in
  `WhyContent.astro`/`PricingContent.astro`.
- `[lang]/[slug].astro` diventa dispatcher unico (brand + landing) via `getStaticPaths` = `brandParams()` +
  `landingParams()`, scelta del corpo per discriminante `kind`.
- `landings.ts`: slug riservati **per-lingua** (`reservedSlugs(lang)`), `validateLandings` aggiornato,
  `landingParams` con `kind:'landing'`.
- Link interni brand localizzati in `BaseLayout` (nav), `index.astro` (CTA), `LandingSections.astro`.

### sitemap.xml + robots.txt
- `site/src/lib/sitemap.ts` (funzioni pure) + endpoint `site/src/pages/sitemap.xml.ts`: sitemap multilingua con
  alternates hreflang localizzati (senza `@astrojs/sitemap`, nessuna dipendenza nuova).
- `site/src/pages/robots.txt.ts`: coerente col gate di indicizzazione (Disallow pre-go-live; Allow + Sitemap al
  go-live).

### Dati strutturati Schema.org
- `site/src/lib/seo.ts`: `Organization`, `BreadcrumbList`, `SoftwareApplication` (+offerte dal parser prezzo
  tollerante), `FAQPage`, `serializeJsonLd` (neutralizza `<`).
- `BaseLayout`: `Organization` su ogni pagina + nodi specifici via prop `jsonLd`. Le landing pubblicate
  emettono `SoftwareApplication` + `FAQPage`.

### Misurazione + gate indicizzazione
- Plausible + meta di verifica Search Console/Bing in `BaseLayout`, **spenti di default** (emessi solo con env
  valorizzate; Plausible anche solo se indicizzabile).
- **Fix gate noindex** (difetto latente di UC 0036): `import.meta.env.SITE_INDEXABLE` → `process.env` in
  `BaseLayout` e `robots.txt.ts`, così il rebuild go-live ha davvero effetto.

### Controllo SEO + CI
- `scripts/postbuild-check.mjs`: parità con slug localizzati; nuove asserzioni title/description, hreflang
  risolvibili, sitemap/robots presenti, JSON-LD valido (Organization ovunque, SoftwareApplication+FAQPage sulle
  landing).
- `.github/workflows/verify-pr.yml`: filtro + job **`site`** bloccante (`./run-tests.sh site`).
- Legali: aggiunta meta description localizzata.
- `tools/new-application` e `tools/finalize-landing`: copie slug riservati allineate agli slug brand localizzati.
- `site/SEO.md`: assetto SEO tecnico + keyword strategy due livelli + nicchia EU/GDPR.

## Test
- `./run-tests.sh site` → **verde** (vitest 51 test: `routes`/`seo`/`sitemap`/`landings`/`marketing`/`legal`;
  `astro build` 42 pagine; controllo post-build).
- `./run-tests.sh tooling` → **verde** (parità scaffolding + strumenti landing).
- Gate privacy (`npm run privacy-scan`) → nessun segnale (sito statico, misurazione cookieless spenta).
- Verifica manuale go-live: `SITE_INDEXABLE=true` rimuove il noindex, robots passa ad Allow + Sitemap, Plausible
  e meta di verifica compaiono; controllo post-build verde anche in modalità indicizzabile.

## Fuori scope (tracciato)
- Connessione reale Search Console/Bing + attivazione Plausible → operativo post-go-live (nota nei Punti aperti
  UC 0040 e in `site/SEO.md`).
- `llms.txt`/consenso crawler AI → UC 0041. Contenuti blog + Schema `Article` → UC 0042. SEO per-app generato →
  `new-application` (UC 0046).
