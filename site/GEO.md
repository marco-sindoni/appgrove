# GEO del sito vetrina appgrove (UC 0041)

GEO = *Generative Engine Optimization*: farsi **citare e raccomandare dagli assistenti AI**
(ChatGPT, Claude, Perplexity, Gemini, AI Overviews) sfruttando il cuneo tutto-UE/GDPR + micro-strumenti
focalizzati. È il gemello della SEO tecnica (UC 0040, `SEO.md`): stessa fonte di contenuti, obiettivo diverso
(gli assistenti AI invece dei motori di ricerca classici).

## 1. Cosa fa il sito, dove

- **`llms.txt` multilingua** (`src/lib/llms.ts` + `src/pages/llms.txt.ts` radice + `src/pages/[lang]/llms.txt.ts`):
  il "robots.txt per gli assistenti AI" — un riassunto markdown curato del prodotto. Variante radice `/llms.txt`
  in inglese (con in coda i link alle lingue) e una variante localizzata `/<lang>/llms.txt` per ciascuna delle 5
  lingue. Il testo è attinto dai contenuti marketing già tradotti (UC 0037); la sezione **App** deriva dalle sole
  landing **pubblicate** (vuota finché non ce ne sono → onesta col catalogo, e una nuova app pubblicata compare da sé).
- **Consenso ai crawler AI in `robots.txt`** (`src/lib/robots.ts`): al go-live (`SITE_INDEXABLE=true`) il file
  elenca esplicitamente, con `Allow: /`, il **più ampio insieme possibile** di crawler AI/LLM documentati (OpenAI,
  Anthropic, Google-Extended, Apple, Perplexity, Common Crawl, Meta, Amazon, ByteDance, Cohere, Mistral, You.com,
  Petal, DuckDuckGo, Diffbot, Timpi, Webz/Omgili, AI2), più la riga `Sitemap:` e un rimando a `/llms.txt`. Pre-go-live
  resta `Disallow: /` totale (coerente col meta `noindex`): i crawler AI agiscono **solo al lancio**. Il wildcard
  `Allow: /` ammette comunque chi non è elencato; l'elenco è la **dichiarazione d'intento verificabile** della GEO.
- **Contenuti machine-readable — FAQ ovunque** (`src/components/FaqSection.astro`): domande/risposte in HTML
  semantico (`<dl>`) **senza JavaScript**, su **homepage** (FAQ generale di marca), pagina **"perché"** (ecosistema,
  AI, UE) e pagina **"prezzi"** (fatturazione), nelle 5 lingue. Ogni pagina emette anche il dato strutturato
  **`FAQPage`** (JSON-LD, riusa `faqPageJsonLd` di `src/lib/seo.ts`). Le landing per-app hanno già la loro FAQ +
  `FAQPage` (UC 0038/0040): la FAQ è così su **tutte** le pagine chiave.
- **Entità canonica / boilerplate** (`src/lib/brand.ts`): **una** descrizione canonica autorevole di appgrove
  (nome, categoria, fatti chiave, angolo UE/GDPR), in inglese, da usare **identica ovunque** — è iniettata come
  `description`/`slogan` dell'`Organization` (JSON-LD, su ogni pagina) e alimenta la variante radice di `llms.txt`.
  Fuori dal sito (directory, profili social) va copiata **verbatim**, così l'entità non diverge tra i canali.
- **Statement fattuali machine-readable**: oltre alla FAQ, i dati strutturati Schema.org di UC 0040 (`Organization`
  con descrizione canonica, `SoftwareApplication`, `BreadcrumbList`) danno agli LLM fatti citabili in forma stabile.

## 2. Misurazione (referral dagli assistenti AI)

- **Plausible** (già cablato in UC 0040, cookieless, spento pre-go-live): al go-live si osservano i *referrer* dai
  motori AI (`chatgpt.com`, `perplexity.ai`, `gemini.google.com`, …) per stimare il traffico generato dalle citazioni.
- **Check manuali periodici**: interrogare gli LLM sulle query obiettivo (es. "strumenti gestionali europei conformi
  al GDPR", "alternativa UE a una suite gestionale") e verificare se e come appgrove viene citato/raccomandato.
- L'attivazione reale (connessione a Plausible + calendario dei check) è **operativa post-go-live**, come per la
  misurazione SEO di UC 0040.

## 3. Postura (privacy)

Solo **contenuto marketing pubblico** viene esposto ai crawler AI — **nessun dato utente** (postura purista,
coerente con il cuneo di fiducia). Nessun trattamento di dati personali: manifesto dati N/A.

## 4. Manutenzione (dove si tocca cosa)

- Cambiare l'entità canonica → `src/lib/brand.ts` (si propaga a `Organization` e a `/llms.txt`).
- Aggiungere/togliere un crawler AI → `AI_CRAWLERS` in `src/lib/robots.ts` (il test `robots.test.ts` verifica il consenso).
- Cambiare la FAQ → i contenuti marketing (`src/content/marketing/*.ts`, campi `faq`, `why.faq`, `pricing.faq`), in
  **parità 5 lingue** (il test di forma la impone). Il `FAQPage` si aggiorna da sé.
- Un'app **pubblicata** compare da sola nella sezione App di `llms.txt` (nessun passo manuale).
- Controllo in continuous integration: `run-tests.sh site` (vitest + `astro build` + controllo post-build) —
  verifica presenza/forma di `llms.txt`, consenso crawler AI al go-live, `FAQPage` sulle pagine chiave.
