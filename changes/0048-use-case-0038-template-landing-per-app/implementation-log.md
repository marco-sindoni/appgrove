# Implementation Log — Change 0048: Template landing per-app (UC 0038)

**Branch**: `change/0048-use-case-0038-template-landing-per-app`
**Aree**: `site/` (Astro SSG)
**Completata**: 2026-07-25
**Modalità**: autopilot — le risposte alle domande di approfondimento e le scelte di progetto sono
dell'agente (opzione raccomandata) e sono tracciate in [decisions.json](decisions.json). Confermata
esplicitamente dallo sviluppatore la scelta strutturale dell'URL delle landing (slug localizzato alla radice).

## File modificati

| File | Azione |
|---|---|
| `site/src/content/landings/types.ts` | Creato — tipo `LandingContent`/`Landing` + 8 sezioni + `LandingStatus`. |
| `site/src/content/landings/index.ts` | Creato — registro `LANDINGS` + fixture `example` (draft). |
| `site/src/content/landings/example/{en,it,fr,es,de}.ts` | Creati — contenuto fixture nelle 5 lingue. |
| `site/src/content/landings/landings.test.ts` | Creato — parità/valori, gate, slug riservati, resa template. |
| `site/src/lib/landings.ts` | Creato — `publishedLandings`, `landingParams`, `RESERVED_SLUGS`, `validateLandings`. |
| `site/src/components/LandingSections.astro` | Creato — template parametrico delle 8 sezioni (+ Material Symbols). |
| `site/src/pages/[lang]/[slug].astro` | Creato — rotta landing, `getStaticPaths` solo `published`. |
| `site/src/layouts/BaseLayout.astro` | Modificato — prop `ogImage`/`appId` + meta Open Graph/Twitter + marcatore `ag:app-id`. |
| `site/scripts/postbuild-check.mjs` | Modificato — controllo #6 (landing) + #7 (illustrazioni home). |
| `site/vitest.config.ts` | Modificato — `getViteConfig` per rendere `.astro` nei test (Container API). |
| `site/src/components/illustrations/{HeroVisual,AppsVisual,EcosystemVisual,AiVisual,PrivacyVisual}.astro` | Creati — illustrazioni SVG on-brand della homepage. |
| `site/src/pages/[lang]/index.astro` | Modificato — sezioni a due colonne testo + illustrazione. |
| `run-tests.sh` | Modificato — commento area `site` aggiornato. |
| `docs/usecases/_INDEX.md` | Modificato — UC 0038 → 🟡 (step-01) → ✅ (step-04). |
| `docs/usecases/10-skills-tooling/0046-skill-new-application.md` | Modificato — rimando: bozza landing da `new-application`. |
| `docs/usecases/10-skills-tooling/0057-skill-finalize-landing.md` | Modificato — rimando: contratto da consumare + scope skill. |
| `docs/usecases/09-marketing-site/0040-seo-tecnico.md` | Modificato — rimando: JSON-LD Schema.org + sitemap. |

## Cosa è stato fatto

Consegnato il **template ripetibile di landing per-app** del sito vetrina come componenti Astro parametrici,
on-brand (design system UC 0019), nelle 5 lingue, con le **8 sezioni** (#14 25): hero, problema→soluzione,
feature (icone Material Symbols), come funziona, pricing (mensile/annuale + prova), privacy EU, FAQ, CTA finale.
Il modello dati porta lo `status: draft|published`: la rotta `[lang]/[slug].astro` genera **solo** le landing
`published` (gate strutturale, #14 52) con **slug localizzato** per lingua (#14 31). Aggiunti Open Graph/Twitter
al `BaseLayout` e i controlli automatici (vitest + post-build). Una fixture `example` in bozza collauda template
e test senza pubblicare contenuti finti.

**Aggiunta su richiesta dello sviluppatore** (ampliamento di scope verso UC 0037): la **homepage** è stata resa più
visuale con **illustrazioni vettoriali SVG on-brand**, una per sezione (hero, vetrina app, cross-sell, Ready for AI,
privacy/EU). Non sono immagini raster generate con AI: l'ambiente non ha uno strumento di generazione immagini e un
generatore esterno contraddirebbe la linea "tutto in UE / self-hosted". Le illustrazioni usano i token del design
system, quindi seguono accento e tema chiaro/scuro, e sono decorative (aria-hidden). Il template landing (UC 0038)
resta senza illustrazioni decorative perché è screenshot-first per design.

## Decisioni prese

Condotta in **autopilot**; sintesi (registro completo in [decisions.json](decisions.json), 12 voci):

- **Scope**: solo template + modello draft/published + gate + controlli. Le skill (`new-application`,
  `finalize-landing`), la landing dell'app #1, i dati strutturati completi e la sitemap restano fuori e sono
  tracciati come rimandi (vedi "Note per il revisore").
- **URL** landing = slug localizzato alla radice della lingua (`/en/invoicing/`, `/it/fatture/`), onorando #14 31,
  con lista di **slug riservati** contro le collisioni. *(Confermato esplicitamente dallo sviluppatore.)*
- **Gate strutturale**: `getStaticPaths` filtra `published` → una bozza non finisce mai in `dist/`.
- **Contenuti TypeScript** strutturati (come marketing UC 0037): parità 5 lingue a compile-time.
- **Screenshot** `{ src: string | null, alt }`: `null` → placeholder on-brand in bozza; `finalize-landing` lo riempie.
- **Open Graph** nel `BaseLayout` (tutto il sito) + marcatore `ag:app-id` sulle landing per il controllo post-build.
- **Test di resa** via Astro Container API (vitest reso Astro-aware con `getViteConfig`).

## Invarianti appgrove

Nessuno toccato nel merito: sito pubblico statico (nessun tenant, nessuna query, nessun modulo Terraform, nessun
log applicativo). Restano rispettati la **parità 5 lingue** e il **gate `noindex`** già in essere. Il gate di
pubblicazione (`published`) è l'analogo, per i contenuti, dell'isolamento: nessuna pagina incompleta va online.

## Note per il revisore

- **Nessun cambio di contratto cross-area**: la change è confinata a `site/`.
- **Decisioni differite tracciate** (nessuna lasciata in chat):
  - generazione della **bozza landing** da `new-application` → **UC 0046** (Punti aperti);
  - **`finalize-landing`** (screenshot reali/immagine OG/review/publish) + contratto da consumare + caso
    **"landing stale"** da `new-change` → **UC 0057** (Punti aperti, sezione aggiunta);
  - **JSON-LD Schema.org** (incl. `FAQPage`, `SoftwareApplication`/`Offer`) + **sitemap/robots** → **UC 0040**;
  - **landing concreta dell'app #1** → **UC 0053** (già in indice).
- **gate parità scaffold** (UC 0046): nessun percorso-sorgente dei modelli toccato — non scatta.
- **gate privacy** (UC 0031): nessun segnale.
- La fixture `example` resta **draft** (non genera pagine); il percorso `published` è stato validato con una prova
  temporanea (flip → build → check → ripristino), documentata in `decisions.json` #10.

## Test

Area `site` (`./run-tests.sh site` → verde):

- **vitest** (`npm test`): 37 test verdi, di cui **17 nuovi** in `landings.test.ts` — parità 5 lingue e forma dei
  contenuti landing, nessuna stringa vuota, vincoli 3–6 feature / 2–3 step, gate `draft`/`published`, rifiuto degli
  slug riservati, e **resa del template** (8 sezioni + placeholder screenshot in bozza) via Astro Container API.
- **`astro build`**: 42 pagine (nessuna landing, la fixture è draft → gate strutturale verificato); le 5 illustrazioni
  SVG compilano e si legano ai token del design system (classi `fill-*`/`stroke-*` generate → tema chiaro/scuro).
- **controllo post-build** (`npm run check`): verde, incluso il controllo landing (parità 5 lingue + Open Graph) e la
  rete di regressione sulle illustrazioni (ogni home localizzata ne porta 5, soglia ≥ 4).

Baseline snapshot visive: non introdotte qui (le screenshot end-to-end sono di `finalize-landing`, UC 0057).

## Stato criteri di accettazione (DoD UC 0038)

- [x] Template landing 8 sezioni parametrico on-brand, 5 lingue, light/dark, responsive, icone Material Symbols.
- [x] Stati `draft`/`published`; il build renderizza solo `published` (gate strutturale + controllo a valle).
- [x] Controlli automatici: parità 5 lingue, gate, Open Graph/meta, slug riservati (vitest + post-build).
- [x] `./run-tests.sh site` verde; `decisions.json` completo e coerente col log; rimandi scritti negli UC proprietari.
- [x] (extra, su richiesta) Homepage più visuale: illustrazioni SVG on-brand per sezione, tema chiaro/scuro, verificate.
