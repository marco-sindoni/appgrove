# Log di implementazione — Change 0051 · GEO (`llms.txt`, crawler AI, entità canonica) — UC 0041

**Aree toccate**: `site/` (Astro SSG) · `docs/`. **Modalità**: autopilot (con tre override dello sviluppatore al gate
requisiti: crawler AI più ampi possibile, `llms.txt` multilingua, FAQ ovunque). Esito: **tutte le suite eseguite verdi**.

## Cosa è stato fatto

### Entità canonica / boilerplate
- `site/src/lib/brand.ts` (nuovo): **una** descrizione canonica autorevole di appgrove (nome, categoria, tagline, fatti
  chiave, angolo UE/GDPR, dominio), in inglese. "Identica ovunque" = una versione riusata verbatim, non 5 divergenti.
- `site/src/lib/seo.ts`: `organizationJsonLd` arricchita con `description` (canonica) + `slogan` → statement fattuale
  machine-readable su **ogni** pagina. Test esteso in `seo.test.ts`.

### `llms.txt` multilingua
- `site/src/lib/llms.ts` (nuovo): costruttore **puro** parametrico sulla lingua. Titolo di marca, sommario e link
  localizzati dai contenuti marketing (UC 0037) + identità da `brand.ts`; sezione **App** dalle sole landing
  **pubblicate** (vuota finché non ce ne sono → una app pubblicata compare da sé). La variante radice aggiunge la sezione
  *Languages*.
- `site/src/pages/llms.txt.ts` (nuovo): `/llms.txt` (inglese + link lingue). `site/src/pages/[lang]/llms.txt.ts` (nuovo):
  `/<lang>/llms.txt` per le 5 lingue. Test in `llms.test.ts` (localizzazione, link risolvibili, sezione app, lingue).

### Consenso ai crawler AI in `robots.txt` (elenco ampio)
- `site/src/lib/robots.ts` (nuovo): costruttore **puro** + costante `AI_CRAWLERS` con ~30 user-agent **reali e
  documentati** (OpenAI, Anthropic, Google-Extended, Apple, Perplexity, Common Crawl, Meta, Amazon, ByteDance, Cohere,
  Mistral, You.com, Petal, DuckDuckGo, Diffbot, Timpi, Webz/Omgili, AI2). Pre-go-live: `Disallow: /` totale (coerente col
  `noindex`). Go-live (`SITE_INDEXABLE=true`): `Allow` generale + un blocco `Allow: /` per crawler + `Sitemap:` + rimando
  a `/llms.txt`. Test in `robots.test.ts`.
- `site/src/pages/robots.txt.ts`: ridotto a guscio sottile che delega al costruttore.

### Contenuti machine-readable — FAQ ovunque
- `site/src/content/marketing/types.ts`: `FaqItem`/`FaqSection`; campo `faq` su `MarketingContent` (home) + `WhyPage.faq`
  + `PricingPage.faq`.
- `site/src/content/marketing/{en,it,fr,es,de}.ts`: FAQ nelle 5 lingue — home (5 Q&A generali di marca), "perché" (4 su
  ecosistema/AI/UE), "prezzi" (4 su fatturazione). Parità di forma garantita dal tipo + dal test.
- `site/src/components/FaqSection.astro` (nuovo): componente riusabile, HTML semantico (`<dl>`), senza JavaScript,
  marcato `data-faq`.
- Rendering + `FAQPage` JSON-LD: `[lang]/index.astro` (home), `WhyContent.astro`/`PricingContent.astro` (rese dal
  dispatcher `[lang]/[slug].astro`, che appende `faqPageJsonLd`). Le landing per-app avevano già FAQ + `FAQPage`.
- **Copy prezzi ammorbidito** (rilettura sviluppatore): nella pagina prezzi il box "rimborsi" e la relativa FAQ (5
  lingue) passano da un titolo negativo ("Nessun rimborso") a uno positivo ("Disdici quando vuoi"), spiegano senza
  asprezza e aggiungono la disdetta libera ("dal ciclo di fatturazione successivo non paghi più nulla, il periodo in
  corso resta attivo fino alla scadenza"). Coerente con la Refund Policy legale (§2) — nessun over-promise.

### Controllo post-build + documentazione
- `site/scripts/postbuild-check.mjs`: nuovi controlli — `llms.txt` radice + per-lingua presenti/ben formati; consenso
  crawler AI in `robots.txt` quando indicizzabile (+ riga `Sitemap`); `FAQPage` su home + "perché"/"prezzi" di ogni lingua.
- `site/GEO.md` (nuovo): assetto GEO, misurazione (referral AI via Plausible + check manuali), postura privacy, manutenzione.
- `docs/usecases/09-marketing-site/0041-geo-llms.md`: stato implementazione + sezione "Punti aperti / decisioni differite".
- `docs/usecases/_INDEX.md`: UC 0041 → ✅.

## DoD (UC 0041)
1. ✅ `llms.txt` (multilingua) + consenso crawler AI in `robots.txt` (al go-live).
2. ✅ FAQ + Schema.org (`FAQPage`, `Organization` con descrizione canonica) + entità canonica unica (`brand.ts`).
3. ✅ Misurazione referral AI documentata (Plausible già cablato UC 0040 + check manuali) — attivazione post-go-live tracciata.
4. ✅ `new-application` produce materiale GEO-friendly per-app: verificato — il modello landing genera già FAQ→`FAQPage`,
   `SoftwareApplication`, slug/meta canonici; una app pubblicata compare da sé in `llms.txt`. Nessuna modifica agli scaffold.

## Test
- `./run-tests.sh site` → **verde** (vitest 64 test — inclusi i nuovi `robots`/`llms`/`seo` e la parità marketing coi FAQ;
  `astro build` 42 pagine + endpoint `llms.txt`/`robots.txt`; controllo post-build).
- Verifica manuale go-live: `SITE_INDEXABLE=true` → `robots.txt` elenca i crawler AI (31 blocchi User-agent) + `Sitemap`;
  `/llms.txt` e `/<lang>/llms.txt` serviti; controllo post-build verde anche in modalità indicizzabile.
- Gate privacy (`npm run privacy-scan`) → **nessun segnale** (sito statico, solo contenuto marketing pubblico).

## Fuori scope (tracciato in UC 0041 → "Punti aperti / decisioni differite")
- Attivazione reale misurazione referral AI → post-go-live. Tabelle di confronto vs concorrenti → blog (UC 0042).
  `sameAs`/profili social sull'`Organization` → presenza off-site (UC 0043).
