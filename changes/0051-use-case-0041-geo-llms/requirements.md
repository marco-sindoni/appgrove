# Requirements — Change 0051 · GEO (`llms.txt`, crawler AI, entità canonica) — UC 0041

**Use case sorgente**: [docs/usecases/09-marketing-site/0041-geo-llms.md](../../docs/usecases/09-marketing-site/0041-geo-llms.md)
**Modalità**: autopilot · **Aree toccate**: `site/` (Astro SSG) · `docs/` · nessuna modifica infra/backend/scaffold.
**Dipende da**: UC 0040 (SEO tecnico, change 0050, già su `main`) — questa change ne estende gli stessi file
(`seo.ts`, `robots.txt.ts`, `BaseLayout`, `postbuild-check.mjs`).

## 1. Obiettivo

Implementare la **GEO** (farsi citare/raccomandare dagli assistenti AI) sfruttando l'angolo tutto-UE/GDPR:
pubblicare **`llms.txt`**, dare **consenso esplicito ai crawler AI** in `robots.txt`, aggiungere **contenuti
machine-readable** (FAQ Q&A a livello brand) e stabilire un'**entità canonica/boilerplate** unica riusata identica
ovunque. Nessun dato personale è coinvolto: solo contenuto marketing pubblico.

## 2. Ambito

### In scope
1. **Entità canonica / boilerplate** — `site/src/lib/brand.ts`: **una** descrizione canonica autorevole di appgrove
   (nome, categoria, tagline, descrizione, fatti chiave, angolo UE/GDPR, dominio). Riusata da `llms.txt` e iniettata
   come `description` nell'`Organization` JSON-LD (presente su ogni pagina) → statement fattuale machine-readable.
2. **`llms.txt` multilingua** — endpoint radice `site/src/pages/llms.txt.ts` (servito su `/llms.txt`, lingua di default
   inglese) **+ endpoint per-lingua** `site/src/pages/[lang]/llms.txt.ts` (`/en|it|fr|es|de/llms.txt`), entrambi sopra un
   costruttore puro `site/src/lib/llms.ts` **parametrico sulla lingua**. Riassunto markdown curato secondo la convenzione
   llms.txt (titolo `#`, sommario, sezioni con link alle pagine chiave: home, perché, prezzi, legali) **nella lingua di
   ogni variante**, attingendo ai contenuti marketing già localizzati (UC 0037) + l'identità canonica di `brand.ts`. La
   variante radice elenca anche le altre lingue (sezione *Languages*). Include una sezione **App** dalle landing
   **pubblicate** (vuota finché non ce ne sono → onesta col catalogo). Sempre presente; il gate robots tiene fuori i
   crawler pre-go-live.
3. **Consenso crawler AI in `robots.txt` — il più ampio possibile** — corpo estratto in costruttore puro
   `site/src/lib/robots.ts` + endpoint sottile. Pre-go-live: `Disallow: /` per tutti (coerente col gate noindex).
   Go-live (`SITE_INDEXABLE=true`): `Allow` generale **+ blocchi `User-agent` espliciti** per il **più ampio insieme
   possibile** di crawler AI/LLM documentati e reputabili (OpenAI, Anthropic, Google-Extended, Apple, Perplexity, Common
   Crawl, Meta, Amazon, ByteDance, Cohere, Mistral, You.com, Petal, DuckDuckGo, Diffbot, Timpi, Webz/Omgili, AI2, …) con
   `Allow: /`, riga `Sitemap:` e commento che rimanda a `/llms.txt`. Elenco curato di user-agent **reali e documentati**
   (niente nomi inventati); il wildcard `Allow: /` copre comunque quelli non elencati. La postura resta purista: solo
   contenuto marketing pubblico, nessun dato utente (#14 39).
4. **Contenuti machine-readable — FAQ ovunque possibile** — campo `faq` (`FaqSection`) aggiunto a **tre** contesti di
   `MarketingContent`, nelle 5 lingue: **homepage** (FAQ generale di marca), pagina **"perché"** (FAQ su ecosistema/AI/UE)
   e pagina **"prezzi"** (FAQ su fatturazione: prova, cicli, rimborsi, prezzo per-app). Ogni pagina rende la sua FAQ (via
   un componente riusabile `FaqSection.astro`) e **emette `FAQPage` JSON-LD** (riusa `faqPageJsonLd` di `seo.ts`). Le
   landing per-app hanno già la loro FAQ + `FAQPage` (UC 0038/0040): così la FAQ machine-readable è presente su **tutte**
   le pagine chiave.
5. **Check di continuous integration** — `site/scripts/postbuild-check.mjs`: `/llms.txt` presente e ben formato; se
   indicizzabile, `robots.txt` contiene i crawler AI attesi; ogni home localizzata ha `FAQPage` JSON-LD.
6. **Documentazione** — nuovo `site/GEO.md` (assetto GEO: llms.txt, consenso crawler, entità canonica, FAQ, misurazione
   referral AI via Plausible + check manuali periodici).

### Fuori scope (tracciato in UC 0041 → "Punti aperti / decisioni differite")
- **Tabelle di confronto** vs concorrenti — outward-facing, affermazioni sui competitor: appartengono ai contenuti blog
  (UC 0042), non a questa change.
- **Attivazione reale** della misurazione referral AI in Plausible + check manuali periodici → operativo **post-go-live**
  (come per la misurazione di UC 0040).
- **`sameAs` / profili social/directory** sull'`Organization` — non esistono ancora profili reali; presenza off-site è
  UC 0043.
- **Materiale GEO per-app di `new-application`** (DoD 4): **già soddisfatto** dalla pipeline landing esistente (ogni
  landing generata porta FAQ → `FAQPage`, `SoftwareApplication`, slug/meta canonici localizzati). Una app **pubblicata**
  compare automaticamente nella sezione App di `llms.txt`. Nessuna modifica ai modelli di scaffolding.

## 3. Vincoli / invarianti

- Sito pubblico statico: **nessun invariante multi-tenant** applicabile, **nessun dato personale** (manifest N/A).
- **Coerenza col gate di indicizzazione** (#14 54): pre-go-live tutto `noindex` + `Disallow`; i crawler AI agiscono solo
  al go-live. `llms.txt` e i blocchi crawler seguono lo stesso interruttore `SITE_INDEXABLE` già in uso.
- **Nessuna nuova dipendenza** npm (coerente con UC 0040: endpoint puri).
- **Parità 5 lingue** preservata: il nuovo `faq` è nella forma `Record<Locale, …>` (parità a compile-time) con test dei
  valori (nessuna stringa vuota, stessa lunghezza liste).
- **Entità canonica unica**: il boilerplate è una **sola** versione autorevole (inglese, lingua franca degli LLM e dei
  canali off-site) — "identica ovunque" significa una versione riusata verbatim, non cinque divergenti.

## 4. Requisiti di test

- **Unit (vitest)**: `brand.test.ts` (invarianti del boilerplate: campi non vuoti, dominio coerente); `llms.test.ts`
  (il costruttore produce, **per ogni lingua**, titolo/sommario nella lingua giusta, link risolvibili, sezione app
  coerente con le landing pubblicate; la radice elenca le lingue); `robots.test.ts` (indicizzabile → blocchi crawler AI
  + `Sitemap` + rimando llms.txt; non indicizzabile → `Disallow: /` puro); estensione `seo.test.ts` (`Organization` ora
  ha `description` canonica).
- **Post-build**: `/llms.txt` e `/<lang>/llms.txt` presenti e ben formati; `robots.txt` con i crawler AI quando
  indicizzabile; `FAQPage` JSON-LD su **home, "perché" e "prezzi"** di ogni lingua; le asserzioni esistenti (parità,
  hreflang, noindex, Schema) restano verdi.
- **Verifica manuale go-live**: `SITE_INDEXABLE=true` → robots elenca i crawler AI + Sitemap; `/llms.txt` servito;
  home indicizzabile con `FAQPage`. Il controllo post-build resta verde anche in modalità indicizzabile.
- Suite: `./run-tests.sh site` (vitest + `astro build` + post-build) verde. `run-tests.sh` **non** cambia (nessun modulo
  aggiunto/rimosso). Gate privacy (`npm run privacy-scan`) atteso senza segnali (sito statico, misurazione spenta).

## 5. Definition of Done (da UC 0041)

1. `llms.txt` pubblicato; crawler AI consentiti in `robots.txt` (al go-live).
2. Contenuti machine-readable (FAQ + Schema.org già da UC 0040) + entità canonica unica.
3. Misurazione referral AI documentata (Plausible + check manuali) — attivazione post-go-live tracciata.
4. `new-application` produce materiale GEO-friendly per-app — soddisfatto dalla pipeline landing esistente (verificato).
