# Change 0061: Blog/risorse — motore SEO/GEO pillar-cluster del sito vetrina

**Branch**: `change/0061-use-case-0042-blog-risorse`
**Aree**: `site` (Astro SSG)
**Data**: 2026-07-26
**Autore**: Platform Engineering (autopilot)
**Use case sorgente**: [docs/usecases/09-marketing-site/0042-blog-risorse.md](../../docs/usecases/09-marketing-site/0042-blog-risorse.md)
**Tocca dati personali?**: No — contenuti pubblici, nessun dato personale (UC 0042 §7: manifesto N/A). La misurazione resta la Plausible cookieless già presente; nessun nuovo trattamento.

## Problema / Obiettivo

Il sito vetrina non ha un **blog/risorse**. È il motore di posizionamento organico (ottimizzazione per i
motori di ricerca — SEO) e di citabilità dagli assistenti AI (ottimizzazione per gli assistenti generativi —
GEO) che deve **esistere dal lancio** e **crescere nel tempo**. Questa change costruisce la **struttura
completa** del blog secondo il modello **pilastro + cluster** (una pagina pilastro per tema, più articoli
long-tail how-to/confronto che vi rimandano) con **internal linking alle landing delle app**, contenuti
**question-based** (formattati per rispondere come si chiede a un assistente AI), tutto **on-brand nelle 5
lingue**, con i dati strutturati Schema.org `Article` e gli hreflang. Semina un corpus **minimo ma reale**
(1 pilastro + 2 articoli cluster) che dimostra il modello; la crescita dei contenuti è lavoro incrementale
successivo, tracciato nello UC.

## Scope

Solo l'area `site/` (Astro SSG). In dettaglio:

1. **Modello dati blog** (`site/src/content/blog/`): tipi tipizzati `Pillar` e `Article` con contenuto
   `Record<Locale, …>` che forza la **parità delle 5 lingue a compile-time** (stessa disciplina di marketing
   UC 0037 e landing UC 0038). Ogni post ha slug **localizzato per lingua**, meta (titolo/descrizione),
   corpo strutturato (introduzione + sezioni + FAQ question-based), data di pubblicazione, elenco degli
   articoli cluster (per il pilastro) e riferimento al pilastro (per l'articolo), più l'`appId` della landing
   a cui rimandare.
2. **Seme di contenuti**: **1 pilastro** — "Fatturazione per piccole imprese in UE" — e **2 articoli cluster**
   question-based collegati, nelle 5 lingue, on-brand (tono lean/onesto, firma privacy UE/GDPR), con internal
   link alla landing dell'app faro `fatture`.
3. **Registro e logica** (`site/src/lib/blog.ts`): raccolta dei post pubblicati, parametri statici delle
   rotte, mappe hreflang dagli slug localizzati, validazione (slug ben formati, non riservati, non duplicati,
   coerenza pilastro↔cluster), risoluzione del link interno alla landing per `appId` + lingua dal registro
   `LANDINGS`.
4. **Rotte** (`site/src/pages/[lang]/blog/…`): **indice blog** (`/<lang>/blog/`), **pagina pilastro** e
   **pagina articolo**, tutte localizzate, con breadcrumb e hreflang verso i percorsi reali per lingua.
5. **Dati strutturati** (`site/src/lib/seo.ts`): nuovo costruttore `articleJsonLd` (Schema.org `Article`) —
   il nodo esplicitamente rimandato a questo UC nel commento di `seo.ts`. Le pagine articolo/pilastro con FAQ
   emettono anche `FAQPage` (riuso di `faqPageJsonLd`).
6. **Integrazione SEO/GEO trasversale**: le pagine blog entrano nella **sitemap** (`lib/sitemap.ts`) con
   hreflang corretti; l'indice blog è aggiunto a **`llms.txt`** (`lib/llms.ts`); il **controllo post-build**
   (`scripts/postbuild-check.mjs`) verifica parità 5 lingue delle pagine blog, presenza di `Article` JSON-LD,
   e (già coperto in generale) link interni non rotti e hreflang che risolvono.
7. **Voce "Blog" nella top nav** (`layouts/BaseLayout.astro` + `content/marketing/*`) nelle 5 lingue —
   chiude il rimando aperto in UC 0042 (§Punti aperti) e nella change `0047-use-case-0037-…`: la nav nacque
   senza "Blog" perché la pagina non esisteva e avrebbe rotto il check dei link interni.
8. **Test** (`vitest`): parità 5 lingue e coerenza pilastro↔cluster del corpus, validazione del registro,
   costruttore `articleJsonLd`, presenza delle rotte blog nei parametri statici e nella sitemap.

## Fuori scope

- **SEO tecnico di base** (UC 0040) e **GEO di base** (UC 0041): già implementati; qui si riusano i loro
  presidi, non si riscrivono.
- **Lancio paid/social** (UC 0043) e **skill campagne** (UC 0050): non si tocca l'advertising né il riuso
  social dei contenuti.
- **Crescita del catalogo**: altri pilastri/temi e ulteriori articoli cluster oltre il seme minimo → tracciati
  come rimando in UC 0042 (§Punti aperti / decisioni differite). Il blog è progettato per crescere nel tempo;
  riempirlo ora sarebbe anticipare lavoro incrementale successivo.
- **Backend/infra/altre SPA**: nessuna modifica fuori da `site/`. Nessun nuovo trattamento di dati personali,
  nessuna modifica al form newsletter.
- **Editing markdown a mano runtime / CMS**: i contenuti restano strutturati e tipizzati (nessun content
  collection markdown), coerenti con marketing e landing.

## Criteri di accettazione

- [ ] Esistono l'indice blog `/<lang>/blog/`, la pagina pilastro e le pagine articolo del seme in **tutte e 5
  le lingue**, con slug localizzati; `astro build` + controllo post-build sono **verdi**.
- [ ] Il modello dati impone la **parità 5 lingue a compile-time** (`Record<Locale, …>`); un test vitest
  verifica valori non vuoti, stessa forma fra lingue e **coerenza pilastro↔cluster** (ogni articolo del seme
  rimanda al pilastro e viceversa).
- [ ] Ogni pagina articolo e pilastro emette un JSON-LD **`Article` valido** più breadcrumb; le pagine con
  sezione FAQ emettono anche `FAQPage`. Il controllo post-build verifica la presenza di `Article` sulle
  pagine blog.
- [ ] Gli articoli **rimandano alla landing** dell'app faro con link **interno risolto per lingua** (dal
  registro `LANDINGS`, mai cablato); i link interni non sono rotti (check post-build) e gli **hreflang**
  delle pagine blog risolvono a pagine reali.
- [ ] Le pagine blog compaiono nella **sitemap** con hreflang corretti e l'indice blog è citato in
  **`llms.txt`**; la voce **"Blog"** è presente nella top nav nelle 5 lingue.

## Invarianti appgrove toccati

Nessuno degli invarianti applicativi (tenant_id dal JWT, filtro row-level, modulo `microsaas_app`) è in gioco:
il sito vetrina è statico, pubblico, senza tenant né database. Vale la disciplina propria del sito:

- **Parità 5 lingue a compile-time** via `Record<Locale, …>` (come marketing/landing) — mantenuta dal modello
  dati blog.
- **Slug localizzati per lingua** con hreflang verso i percorsi reali (presidio UC 0040 contro hreflang rotti)
  — mantenuta riusando `slugHreflangPaths`/`hreflangAlternatesByLocale` e la generazione sitemap propria.
- **Gate di indicizzazione** (`noindex` finché `SITE_INDEXABLE=true`) e **misurazione cookieless** — invariati:
  le pagine blog passano dal `BaseLayout` che li applica già.
- **Nessun tracking comportamentale, nessun dato personale** (postura purista UE) — rispettata: contenuti
  pubblici, nessuna raccolta.

## Requisiti di test

- Parità 5 lingue del corpus blog (nessuna stringa vuota; stessa forma della sorgente EN) — mirror del test
  marketing.
- Validazione del registro blog: slug ben formati `[a-z0-9-]`, non riservati, non duplicati per lingua;
  coerenza pilastro↔cluster (riferimenti reciproci esistenti).
- Costruttore `articleJsonLd`: `@type` `Article`, campi `headline`/`inLanguage`/`datePublished` presenti;
  serializzazione che neutralizza `<`.
- Parametri statici blog presenti per ogni post × lingua; presenza dei gruppi blog nella sitemap.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A (tutto dentro `site/`; nessun contratto verso backend/infra) |
| Version bump | minor (nuova sezione del sito, retro-compatibile) |
