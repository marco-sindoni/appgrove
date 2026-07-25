# SEO tecnico del sito vetrina appgrove (UC 0040)

Questo documento descrive l'assetto SEO tecnico del sito Astro e la **keyword strategy**. È guida al
copy e alla manutenzione; l'implementazione tecnica vive nel codice (vedi rimandi in fondo).

## 1. Assetto tecnico (cosa fa il sito, dove)

- **hreflang + x-default** per lingua, con **percorsi reali per lingua** (`src/lib/i18n.ts`,
  `src/lib/routes.ts`). Gli slug possono cambiare da lingua a lingua (brand e landing): gli hreflang
  puntano sempre all'URL che esiste davvero, mai a un percorso tradotto meccanicamente.
- **Slug localizzati** delle pagine brand: `/it/perche/` · `/en/why/` · `/fr/pourquoi/` · `/es/por-que/`
  · `/de/warum/`; `/it/prezzi/` · `/en/pricing/` · `/fr/tarifs/` · `/es/precios/` · `/de/preise/`. Le
  landing per-app nascono già con slug localizzati (UC 0038).
- **canonical** puliti (uno per pagina, verso l'URL della sua lingua).
- **sitemap.xml** multilingua (`src/pages/sitemap.xml.ts` + `src/lib/sitemap.ts`): ogni pagina con i suoi
  alternates hreflang. **robots.txt** (`src/pages/robots.txt.ts`) coerente col gate di indicizzazione.
- **Dati strutturati Schema.org (JSON-LD)** (`src/lib/seo.ts`): `Organization` e `BreadcrumbList` su ogni
  pagina; `SoftwareApplication` (con le offerte dai tier di prezzo) e `FAQPage` sulle landing.
- **meta + Open Graph/Twitter** per pagina e lingua (`BaseLayout.astro`), con immagine OG sulle landing
  pubblicate (generata da `finalize-landing`, UC 0057).
- **Gate di indicizzazione**: tutte le pagine sono `noindex` e `robots: Disallow` finché non si esegue il
  rebuild di go-live con `SITE_INDEXABLE=true` (atto operativo, runbook UC 0036).
- **Misurazione** (cablata, spenta di default): Plausible (`PUBLIC_PLAUSIBLE_DOMAIN`, cookieless, nessun
  dato personale) e meta di verifica Search Console/Bing (`SITE_VERIFICATION_GOOGLE`/`_BING`), emessi solo
  se le variabili sono valorizzate. Connessione reale ai servizi = post-go-live.
- **Newsletter** (UC 0039): il form (isola React `NewsletterForm`) chiama in POST l'endpoint pubblico del
  core all'URL `PUBLIC_CORE_API_URL` (in sviluppo `http://localhost:8080`). È l'unica variabile letta dal
  browser di questo flusso (prefisso `PUBLIC_`, esposta come prop dal frontmatter Astro). Se assente il
  sito compila e si renderizza comunque: il form resta inviabile solo dopo il consenso, ma l'invio va in
  errore gestito finché la variabile non è valorizzata. Sul successo emette l'evento Plausible
  `Newsletter: Subscribe` (best-effort).
- **Check SEO in continuous integration**: `run-tests.sh site` (vitest + `astro build` + controllo
  post-build) gira nel workflow `verify-pr` ed è bloccante.

## 2. Keyword strategy a due livelli

Il posizionamento organico si costruisce su **due livelli**, entrambi filtrati dalla **nicchia EU/GDPR**
(il nostro cuneo di fiducia: dati in Europa, pieni diritti GDPR, niente tracker nascosti).

### Livello 1 — brand / piattaforma (pagine home, "perché", "prezzi")
Intento: chi cerca un **ecosistema di piccoli strumenti** comodi, veloci, sostenibili, ospitati in UE.
Temi guida (localizzati per mercato, non tradotti a calco):

- ecosistema di micro-app per piccole imprese; un unico account, tanti strumenti;
- alternativa europea alle suite gonfie; strumenti che fanno bene **una** cosa;
- privacy e conformità GDPR come impostazione predefinita; dati ospitati in UE;
- "Ready for AI" — app richiamabili dagli assistenti (visione onesta, non disponibilità già attiva).

### Livello 2 — app (pagine landing per-app)
Intento: chi cerca **quel lavoro specifico** che l'app risolve (es. fatturazione, per l'app #1). La
keyword primaria della landing è il *job* dell'app nel mercato locale; le secondarie sono le sue feature
chiave e la firma EU/GDPR. Le landing sono generate da `new-application` (UC 0046) e rifinite da
`finalize-landing` (UC 0057): la strategia per-app si concretizza lì, seguendo questo schema.

### Nicchia trasversale EU/GDPR
Su entrambi i livelli, le parole della fiducia europea (UE, GDPR, dati in Europa, sovranità del dato,
niente tracker) sono **secondarie ma costanti**: differenziano da concorrenti extra-UE senza diventare il
messaggio principale (la firma, non il titolo — decisione #14 E7).

### Localizzazione per mercato
Le keyword si adattano al mercato di ciascuna lingua (ricerca locale, non traduzione letterale). I titoli
e le descrizioni delle pagine sono già contenuti strutturati per lingua (`content/marketing`,
`content/landings`): è lì che la strategia diventa copy.

## 3. Rimandi
- Codice: `src/lib/{i18n,routes,seo,sitemap}.ts`, `src/pages/{sitemap.xml,robots.txt}.ts`,
  `src/layouts/BaseLayout.astro`, `scripts/postbuild-check.mjs`.
- Use case: `docs/usecases/09-marketing-site/0040-seo-tecnico.md`.
- GEO / `llms.txt` (consenso crawler AI): UC 0041. Contenuti blog / Schema `Article`: UC 0042.
