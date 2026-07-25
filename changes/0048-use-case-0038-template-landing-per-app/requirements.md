# Requisiti — Template landing per-app (UC 0038)

**Change**: `0048-use-case-0038-template-landing-per-app` · **Modalità**: autopilot
**Use case sorgente**: [docs/usecases/09-marketing-site/0038-template-landing-per-app.md](../../docs/usecases/09-marketing-site/0038-template-landing-per-app.md)
**Fonte decisioni di prodotto**: [docs/14-sito-vetrina-legale.md](../../docs/14-sito-vetrina-legale.md) — dec. 25 (8 sezioni), 9 (bozza da `new-application`), 31 (slug localizzati), 51–55 (gate finalizzazione).
**Area toccata**: `site/` (Astro SSG). Nessun backend, nessuna infra, nessuna SPA.

## 1. Obiettivo

Definire il **template ripetibile di landing per-app** del sito vetrina come componenti Astro parametrici,
on-brand (design system UC 0019), nelle **5 lingue**, con i **due stati `draft`/`published`** e un **gate di
pubblicazione** per cui il build renderizza **solo** le landing `published`. La change consegna l'unità
ripetibile e i controlli automatici; **non** modifica le skill che la produrranno/finalizzeranno.

## 2. Scope

### Incluso
1. **Modello dati** `LandingContent` (TypeScript) con le **8 sezioni** strutturate (dec. 25), i meta di pagina
   (titolo, descrizione, immagine Open Graph), lo **slug localizzato** per lingua (dec. 31), l'`appId` e lo
   `status: 'draft' | 'published'`. Parità delle 5 lingue garantita a compile-time dal tipo
   `Record<Locale, LandingContent>`, come già per i contenuti marketing (UC 0037).
2. **Template parametrico** (componente Astro) che rende le 8 sezioni on-brand, responsive, light/dark,
   screenshot-first, con icone **Material Symbols** nelle feature.
3. **Rotta dinamica** `site/src/pages/[lang]/[slug].astro` che genera **solo** le landing `published`
   (`getStaticPaths` filtra per `status`), una pagina per lingua, con `pathWithinLocale` = slug localizzato.
4. **Gate draft/published**: strutturale — una bozza non compare in `getStaticPaths`, quindi non finisce mai
   in `dist/`. Nessuna pubblicazione accidentale (dec. 52).
5. **Meta/Open Graph** per le landing: la pagina emette descrizione, canonical, hreflang (via `BaseLayout`) e
   **Open Graph/Twitter card** (titolo, descrizione, immagine, tipo, URL). L'`ogImage` è un campo del contenuto
   (placeholder in bozza, immagine reale in `finalize-landing`).
6. **Fixture di esempio**: una landing `example` (5 lingue, `status: draft`, screenshot placeholder) che collauda
   template e test **senza** pubblicare contenuti finti. Non è la landing dell'app #1 (quella è UC 0053).
7. **Controlli automatici** (Definition of Done UC 0038 §9):
   - **vitest**: parità 5 lingue e forma dei contenuti landing (stesso schema del test marketing); nessuna
     stringa vuota; il **gate** (`published` incluso, `draft` escluso dai path); rifiuto degli **slug riservati**;
     resa del template via **Astro Container API** (le 8 sezioni presenti).
   - **controllo post-build** (`scripts/postbuild-check.mjs`): ogni landing pubblicata ha **5 lingue** e i **meta
     Open Graph**; nessuna bozza in `dist/` (garanzia strutturale, verificata anche a valle); link interni non rotti
     (già coperto genericamente).
8. **`run-tests.sh`**: nessun nuovo modulo (il sito è già l'area `site`); si aggiorna solo il commento descrittivo.

### Escluso (tracciato come rimando — vedi §7)
- La skill **`new-application`** che genera la **bozza** della landing (UC 0046, skill già esistente da estendere).
- La skill **`finalize-landing`**: cattura screenshot reali via Playwright + seed, generazione **immagine OG**,
  review interattiva 5 lingue, transizione `draft → published` (UC 0057).
- La **landing concreta dell'app #1** (UC 0053).
- I **dati strutturati Schema.org** completi (`SoftwareApplication`/`Product`+`Offer`, `BreadcrumbList`, `FAQPage`)
  e **sitemap/robots** (UC 0040 — SEO tecnico). Qui si emettono solo Open Graph + meta di base.
- Il segnale di **landing stale** nel gate qualità di `new-change` (dec. 55) — dipende dall'esistenza di
  `finalize-landing`.
- Gli **ambienti hostati** del sito (test con basic auth, prod) e la pipeline di deploy (dec. 54, UC 0055/0005).

### Aggiunto in corso d'opera (richiesta esplicita dello sviluppatore)
- **Homepage più visuale** (UC 0037): illustrazioni vettoriali SVG on-brand, una per sezione (hero, vetrina app,
  cross-sell, Ready for AI, privacy/EU). Non immagini raster generate con AI (l'ambiente non ha uno strumento di
  generazione immagini; un generatore esterno contraddirebbe la linea "tutto in UE / self-hosted"). Le illustrazioni
  seguono accento e tema chiaro/scuro tramite i token del design system. È un ampliamento di scope verso UC 0037,
  voluto dallo sviluppatore; registrato in `decisions.json` (#13/#14).

## 3. Decisioni di progetto (autopilot)

1. **URL = slug localizzato alla radice della lingua** (`/en/invoicing/`, `/it/fatture/`), coerente con dec. 31,
   invece di un prefisso `/apps/`. URL più corto e con la keyword nel percorso (migliore per SEO/GEO). Per evitare
   collisioni con le pagine statiche (`why`, `pricing`, `legal`, `coming-soon`) c'è una **lista di slug riservati**:
   una landing non può usarli (validazione + test). In Astro le rotte statiche hanno comunque priorità sulla
   dinamica `[slug]`, quindi il rischio è già contenuto; la guardia lo rende esplicito.
2. **Gate strutturale** invece che solo post-build: `getStaticPaths` filtra `status === 'published'`. È la garanzia
   più forte (una bozza non esiste in output); il controllo post-build resta come rete di sicurezza.
3. **Modello contenuti TypeScript** (non markdown) come per il marketing (UC 0037): le landing sono contenuti
   **strutturati** (8 sezioni con campi tipizzati), non prosa; il tipo dà la parità 5 lingue gratis a compile-time.
4. **Screenshot come `{ src: string | null, alt }`**: `null` → riquadro placeholder on-brand (stato bozza);
   `finalize-landing` riempirà `src` con la cattura reale. Il template distingue i due stati visivamente.
5. **Open Graph nel `BaseLayout`** (beneficio a tutte le pagine del sito, non solo alle landing) + marcatore
   `<meta name="ag:app-id">` sulle landing, così il controllo post-build le riconosce senza importare il registro
   TypeScript e senza esporre informazioni sulle bozze.
6. **Icone Material Symbols** importate nel componente delle sezioni (il font icone si carica **solo** sulle pagine
   landing, non su tutto il sito).
7. **Fixture `example` in bozza**: collaudo del template senza contenuti pubblicati finti né anticipo di UC 0053.

## 4. File previsti

- `site/src/content/landings/types.ts` — tipo `LandingContent` + tipi delle 8 sezioni + `LandingStatus`.
- `site/src/content/landings/index.ts` — registro `LANDINGS: Record<appId, Record<Locale, LandingContent>>`.
- `site/src/content/landings/example/{en,it,fr,es,de}.ts` — fixture di esempio (bozza).
- `site/src/content/landings/landings.test.ts` — parità/valori + gate + slug riservati + resa template.
- `site/src/lib/landings.ts` — `publishedLandings()`, `landingParams()`, `RESERVED_SLUGS`, `validateLanding()`.
- `site/src/components/LandingSections.astro` — template parametrico delle 8 sezioni (+ import Material Symbols).
- `site/src/pages/[lang]/[slug].astro` — rotta dinamica, `getStaticPaths` da sole landing pubblicate.
- `site/src/layouts/BaseLayout.astro` — aggiunta Open Graph/Twitter (prop `ogImage`, slot `head` opzionale).
- `site/scripts/postbuild-check.mjs` — controllo parità + Open Graph delle landing pubblicate.
- `run-tests.sh` — aggiornamento del solo commento dell'area `site`.

## 5. Le 8 sezioni del template (dec. 25)

1. **Hero** — badge, headline orientata al *job*, sotto-beneficio, CTA primaria/secondaria, screenshot UI.
2. **Problema → soluzione**.
3. **Feature chiave** (3–6) — icona Material Symbols + titolo + testo (+ mini-screenshot opzionale).
4. **Come funziona** (2–3 step numerati).
5. **Pricing/tier** — tier con ciclo mensile/annuale (default annuale) e prova gratuita; i numeri veri arriveranno
   dal pricing-as-code (#09) tramite le skill — qui sono campi di contenuto (placeholder nella fixture).
6. **Badge/sezione privacy EU** — firma di fiducia (wedge #14 E7).
7. **FAQ** — elenco domanda/risposta.
8. **CTA finale**.

## 6. Invarianti appgrove

Non applicabili nel merito (sito pubblico statico, nessun tenant, nessuna query, nessuna infra bespoke): si annota
in `implementation-log.md`. Resta valida la **parità 5 lingue** e il **gate di indicizzazione** `noindex` già in essere.

## 7. Decisioni differite (tracciate prima della chiusura)

- **`new-application` → bozza landing** (dec. 9/51): estendere la skill perché generi la bozza usando questo template
  → tracciato in **UC 0046** ("Punti aperti / decisioni differite").
- **`finalize-landing`** (dec. 51, screenshot reali/OG/review/publish) + **segnale landing stale** in `new-change`
  (dec. 55) → tracciati in **UC 0057**.
- **Schema.org completo + sitemap/robots** (dec. 29) → tracciati in **UC 0040**.
- **Landing app #1** concreta → **UC 0053** (già in indice, nessuna azione).

## 8. Requisiti di test (riepilogo)

- `./run-tests.sh site` verde: `vitest` + `astro build` + controllo post-build.
- Test nuovi: parità 5 lingue landing; nessuna stringa vuota; gate `draft`/`published`; slug riservati rifiutati;
  resa del template (8 sezioni) via Container API; Open Graph/parità nel controllo post-build.
- Baseline snapshot visive: **non** introdotte qui (le screenshot e2e sono di `finalize-landing`, UC 0057).

## Definition of Done

1. Template 8 sezioni parametrico on-brand, 5 lingue, light/dark, responsive, icone Material Symbols. ✅
2. Stati `draft`/`published`; il build renderizza solo `published`. ✅
3. Controlli automatici (vitest + post-build): parità 5 lingue, gate, Open Graph/meta, slug riservati. ✅
4. `./run-tests.sh site` verde; `decisions.json` completo e coerente col log; rimandi §7 scritti negli UC proprietari. ✅
