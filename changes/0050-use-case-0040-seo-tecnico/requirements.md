# Requisiti — Change 0050 · SEO tecnico del sito vetrina (UC 0040)

**Use case sorgente**: [docs/usecases/09-marketing-site/0040-seo-tecnico.md](../../docs/usecases/09-marketing-site/0040-seo-tecnico.md)
**Modalità**: autopilot (le scelte di scope sono dell'agente, registrate in `decisions.json`; approvazione/commit/merge restano dello sviluppatore).
**Aree toccate**: `site/` (Astro SSG) · `.github/workflows/` (CI) · `docs/` (indice use case + keyword strategy).

## 1. Obiettivo

Portare il sito vetrina Astro allo stato di **SEO tecnico completo**: sitemap + robots, dati strutturati
Schema.org (JSON-LD), meta/Open Graph coerenti, **slug di pagina localizzati** per lingua, misurazione
cablata (ma spenta finché non attivata) e **controllo SEO che gira in CI** ed è bloccante. Il tutto resta
`noindex` fino al go-live (gate già esistente, invariato).

Lo use case è vasto: questa change ne copre gli **artefatti concreti e testabili** delimitati dai suoi due
"Punti aperti" (slug localizzati brand + JSON-LD/sitemap/robots) più le voci di DoD mancanti (misurazione,
keyword strategy, check in CI). Nulla appartenente ad altri use case viene anticipato (vedi §7 Fuori scope).

## 2. Contesto e stato attuale (cosa esiste già)

- `site/src/layouts/BaseLayout.astro` — `<head>` con `<title>`, description opzionale, **canonical**, **hreflang
  + x-default**, gate `noindex` (`SITE_INDEXABLE`), meta **Open Graph/Twitter** base (UC 0038), marcatore
  `ag:app-id` per le landing.
- `site/src/lib/i18n.ts` — `hreflangAlternates(path)` costruisce gli hreflang **da un unico percorso**, uguale
  per tutte le lingue.
- Pagine brand statiche: `[lang]/why.astro`, `[lang]/pricing.astro`, `[lang]/index.astro` (home).
- Landing per-app dinamiche: `[lang]/[slug].astro` (solo `published`), con **slug già localizzati per lingua**
  (`content[lang].slug`).
- `site/scripts/postbuild-check.mjs` — controllo post-build: parità 5 lingue, token residui, **presenza**
  hreflang, noindex, link interni, Open Graph landing, illustrazioni home.
- `run-tests.sh` area `site` → `vitest + astro build + postbuild-check`. **Ma `site` NON è cablato in CI**
  (`.github/workflows/verify-pr.yml` non ha job né filtro per `site/`).

### Difetto latente da correggere (fondamenta)

`hreflangAlternates` usa lo **stesso percorso per tutte le lingue**. Per le landing, che hanno slug diversi per
lingua (`example`/`esempio`/`beispiel`…), gli hreflang **puntano a URL inesistenti** (es. `/de/beispiel/` emette
`hreflang="it" href="/it/beispiel/"`, ma la pagina italiana è `/it/esempio/`). Il controllo attuale verifica solo
la *presenza* degli hreflang, non che *risolvano*, quindi il bug è invisibile. La correzione — un modello di
percorso **per-lingua** — è il prerequisito comune sia agli slug brand localizzati sia agli hreflang corretti.

## 3. Ambito funzionale (cosa fa questa change)

### A. Modello di percorso per-lingua + hreflang corretti (fondamenta)

- Nuovo modulo `site/src/lib/routes.ts`: identità di pagina → **percorso localizzato per lingua**. Copre le
  pagine brand (`why`, `pricing`) con slug tradotti e i legali (slug non tradotti). Espone helper per
  costruire link interni localizzati e la mappa `Record<Locale, path>` degli hreflang di una pagina.
- `BaseLayout` accetta una prop opzionale `hreflangPaths?: Record<Locale, string>` (percorso dentro ogni
  lingua). Quando assente → comportamento attuale (stesso percorso per tutte le lingue: corretto per home e
  legali). Le pagine con slug localizzati (brand, landing) passano la mappa.
- `[lang]/[slug].astro` passa `hreflangPaths` costruiti da `landing.content[<loc>].slug` per ogni lingua →
  **hreflang delle landing corretti**.

### B. Slug delle pagine brand localizzati (`why`, `pricing`)

- Slug tradotti per lingua (proposta, rivedibile): `why` → en `why` · it `perche` · fr `pourquoi` · es
  `por-que` · de `warum`; `pricing` → en `pricing` · it `prezzi` · fr `tarifs` · es `precios` · de `preise`.
  La **home** resta senza slug (`/<lang>/`).
- Vincolo di routing Astro: `/<lang>/<slug>/` è servito da **una sola** rotta dinamica. Le pagine brand
  (slug che varia per lingua) non possono restare file statici `why.astro`/`pricing.astro`; vengono unite nel
  dispatcher `[lang]/[slug].astro`, che in `getStaticPaths` emette **pagine brand + landing** e sceglie il
  corpo da renderizzare in base a un discriminante. I corpi brand diventano componenti
  (`WhyContent.astro`, `PricingContent.astro`); `why.astro`/`pricing.astro` sono rimossi.
- Tutti i link interni verso brand (top nav, footer, CTA home/why/pricing) passano dall'helper localizzato.
- `RESERVED_SLUGS` diventa **per-lingua** (include tutti gli slug brand localizzati + legali/statici), così una
  landing non può rivendicare `/it/prezzi/`. `validateLandings` verifica lo slug contro i riservati della sua
  lingua.
- Nessun redirect: il sito è pre-go-live (`noindex`, non ancora lanciato), non esistono URL già indicizzati.

### C. sitemap.xml multilingua + robots.txt

- Endpoint `site/src/pages/sitemap.xml.ts`: elenca tutte le pagine pubbliche localizzate (home, brand, legali
  `published`, landing `published`), ciascuna con gli **hreflang alternates** (`xhtml:link`) derivati dal
  modello di percorso per-lingua (A/B). Generazione propria, **senza nuove dipendenze** (`@astrojs/sitemap`
  non gestisce slug localizzati non uniformi): coerente con cost-min / output autoconsistente.
- Endpoint `site/src/pages/robots.txt.ts`: `noindex` (`SITE_INDEXABLE!=='true'`) → `User-agent: *` +
  `Disallow: /`; indicizzabile → `Allow: /` + riga `Sitemap: <site>/sitemap.xml`. Il consenso ai crawler AI
  (`llms.txt`/GEO) è UC 0041 → **non** qui.

### D. Dati strutturati Schema.org (JSON-LD)

- `BaseLayout` accetta `jsonLd?: object | object[]` e li rende in `<script type="application/ld+json">`.
- **Organization** (marchio "appgrove", `url` dal `site`, contatto `support@appgrove.app` da
  `content/legal/entity.yaml`): su tutte le pagine. Non si espongono i campi `DA COMPILARE` del titolare: il
  nome è il marchio "appgrove", non la ragione sociale legale.
- **BreadcrumbList** (Home → pagina) sulle pagine interne (brand, legali, landing).
- Sulle **landing** (`published`): **SoftwareApplication** (`applicationCategory: BusinessApplication`,
  `operatingSystem: Web`) con **offers** derivate dai tier di prezzo (parser tollerante prezzo+valuta; se il
  prezzo non è interpretabile l'offerta omette il prezzo, mai JSON-LD malformato) + **FAQPage** dalla sezione
  FAQ del template.
- **Article** è escluso: dipende dai contenuti blog (UC 0042) → rimando (§7).

### E. Misurazione (cablata, spenta di default)

- **Plausible** (deciso #14 33): snippet in `BaseLayout` emesso **solo** quando l'env `PUBLIC_PLAUSIBLE_DOMAIN`
  è valorizzata **e** il sito è indicizzabile. Default (nessuna env, pre-go-live) → nessuno snippet: nessun
  cambiamento di comportamento ora. Collega il percorso di codice; l'attivazione reale (account/dominio) è atto
  operativo post-go-live.
- **Google Search Console / Bing**: meta di verifica opzionali emessi solo se valorizzate le env
  (`SITE_VERIFICATION_GOOGLE`, `SITE_VERIFICATION_BING`). La connessione vera è atto operativo post-go-live
  (rimando nel runbook dello use case).

### F. Estensione del controllo SEO (postbuild-check) + cablaggio in CI

- Nuove asserzioni in `postbuild-check.mjs`:
  1. ogni pagina localizzata ha `<title>` e `<meta name="description">` **non vuoti** (le pagine legali oggi
     non passano description → si aggiunge una description ai legali);
  2. gli hreflang **risolvono** a pagine reali in `dist/` (non solo presenza) → cattura il bug di §2;
  3. `sitemap.xml` e `robots.txt` presenti; la sitemap è XML ben formato e non vuoto;
  4. ogni pagina ha ≥1 blocco JSON-LD che **parsa** come JSON valido e include `Organization`; le landing
     includono `SoftwareApplication` e `FAQPage`.
- **CI**: `.github/workflows/verify-pr.yml` guadagna un job `site` (path-filter `site/**`) che esegue
  `./run-tests.sh site`, rendendo il check SEO **bloccante** in PR (colma il DoD "check CI SEO verde").

### G. Keyword strategy (documentazione)

- Documento `site/SEO.md`: assetto SEO tecnico del sito + **keyword strategy a due livelli** (app-level e
  brand-level) con la **nicchia EU/GDPR**, localizzata per mercato. Artefatto di guida al copy, non codice.

## 4. Invarianti appgrove

Non pertinenti al backend multi-tenant (sito pubblico statico, nessun `tenant_id`/query/JWT). Nessun modulo
Terraform toccato. Nessun dato personale trattato (misurazione Plausible cookieless, spenta di default).

## 5. Requisiti di test

- **Unit (vitest, `site`)**:
  - `routes.ts`: percorsi/hreflang localizzati per ogni pagina brand × 5 lingue; parità delle chiavi.
  - builder JSON-LD: Organization/BreadcrumbList/SoftwareApplication(+offers dal parser prezzi)/FAQPage validi
    e serializzabili; parser prezzo tollerante (`€9`, `€9 / mo`, `€0`, stringa non numerica → prezzo omesso).
  - `validateLandings`: slug che collide con uno slug brand localizzato viene respinto.
  - sitemap: contiene tutte le pagine attese con gli alternates corretti.
- **Post-build (`postbuild-check.mjs`)**: le nuove asserzioni (title/description, hreflang risolvibili,
  sitemap/robots, JSON-LD valido) verdi sull'output reale.
- **Gate area**: `./run-tests.sh site` verde (vitest + astro build + postbuild-check); `./run-tests.sh
  frontend` non impattato.

## 6. Definition of Done

1. Modello percorso per-lingua; hreflang landing **corretti** (risolvono) e slug brand localizzati.
2. `sitemap.xml` + `robots.txt`; JSON-LD Organization/BreadcrumbList su tutte le pagine, SoftwareApplication+
   FAQPage sulle landing; meta+OG coerenti (già presenti).
3. Misurazione cablata e spenta di default (Plausible + meta di verifica), `noindex` fino al go-live invariato.
4. `postbuild-check` esteso e **verde**; job `site` in `verify-pr.yml` bloccante.
5. `site/SEO.md` con keyword strategy due livelli + nicchia EU/GDPR.
6. `run-tests.sh site` verde; UC 0040 → ✅ in `_INDEX.md`; `decisions.json` completo e coerente col log.

## 7. Fuori scope (rimandi tracciati)

- **`llms.txt` / consenso crawler AI (GEO)** → UC 0041 (già suo). `robots.txt` qui non decide il consenso AI.
- **Contenuti blog + Schema `Article`** → UC 0042.
- **SEO per-app generato dallo scaffolding** → `new-application` (UC 0046); qui si predispone solo il template.
- **Connessione reale Search Console/Bing e attivazione account Plausible** → atto operativo post-go-live
  (runbook UC 0040): qui si cabla solo il codice, spento di default.
- Ogni punto emerso in implementazione che appartenga ad altro use case verrà annotato nel suo file prima della
  chiusura (gate decisioni differite).
