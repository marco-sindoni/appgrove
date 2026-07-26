# Implementation Log — Change 0061: Blog/risorse — motore SEO/GEO pillar-cluster

**Branch**: `change/0061-use-case-0042-blog-risorse`
**Aree**: `site` (Astro SSG)
**Completata**: 2026-07-26
**Modalità**: autopilot — le risposte alle domande di approfondimento e le scelte di implementazione sono dell'agente,
tracciate in [decisions.json](decisions.json) (13 decisioni, 11 marcate `(autopilot)`).

## File modificati

| File | Azione |
|---|---|
| `site/src/content/blog/types.ts` | Creato — modello dati `BlogPost` (pilastro/articolo), parità 5 lingue a compile-time |
| `site/src/content/blog/ui.ts` | Creato — stringhe di contorno del blog (indice + cornice) nelle 5 lingue |
| `site/src/content/blog/index.ts` | Creato — registro `BLOG_POSTS` |
| `site/src/content/blog/fatturazione-pmi-ue/{en,it,fr,es,de,index}.ts` | Creato — pilastro (5 lingue) |
| `site/src/content/blog/fattura-elettronica-a-norma/{en,it,fr,es,de,index}.ts` | Creato — articolo cluster 1 (5 lingue) |
| `site/src/content/blog/software-fatturazione-gdpr-pmi/{en,it,fr,es,de,index}.ts` | Creato — articolo cluster 2 (5 lingue) |
| `site/src/lib/blog.ts` | Creato — relazioni pilastro↔cluster, rotte statiche, hreflang, link landing, validazione |
| `site/src/pages/[lang]/blog/index.astro` | Creato — indice blog |
| `site/src/pages/[lang]/blog/[slug].astro` | Creato — pagina post (pilastro/articolo) |
| `site/src/components/BlogPostContent.astro` | Creato — corpo post (domanda, sezioni, FAQ, CTA, serie) |
| `site/src/lib/seo.ts` | Modificato — nuovo `articleJsonLd`; commento aggiornato (Article ora incluso) |
| `site/src/lib/sitemap.ts` | Modificato — gruppi blog (indice + post) nella sitemap |
| `site/src/lib/llms.ts` | Modificato — indice blog fra le pagine chiave di `llms.txt` |
| `site/src/layouts/BaseLayout.astro` | Modificato — voce "Blog" nella top nav |
| `site/src/content/marketing/types.ts` + `{en,it,fr,es,de}.ts` | Modificato — `nav.blog` nelle 5 lingue |
| `site/scripts/postbuild-check.mjs` | Modificato — controllo #15 blog (indice per lingua + parità/JSON-LD post) |
| `site/src/content/blog/blog.test.ts` | Creato — parità 5 lingue contenuti + BLOG_UI |
| `site/src/lib/blog.test.ts` | Creato — validazione registro, rotte, hreflang, link landing |
| `site/src/lib/seo.test.ts` | Modificato — test `articleJsonLd` |
| `docs/usecases/09-marketing-site/0042-blog-risorse.md` | Modificato — §Punti aperti (decisioni differite) |
| `docs/usecases/_INDEX.md` | Modificato — UC 0042 → ✅ |
| `docs/_BACKLOG.md` | Modificato — skill `new-blog-post` fra le skill da creare |

## Cosa è stato fatto

Costruito il **motore del blog/risorse** del sito vetrina secondo il modello **pilastro + cluster**: modello dati
tipizzato con parità 5 lingue a compile-time (stessa disciplina di marketing/landing), rotte indice/pilastro/articolo con
slug localizzati e hreflang verso i percorsi reali, dati strutturati Schema.org `Article` + breadcrumb + `FAQPage`,
integrazione in sitemap e `llms.txt`, controllo post-build dedicato e voce "Blog" in top nav. Seminato un corpus reale
ma minimo — **1 pilastro** ("Fatturazione per piccole imprese in UE") + **2 articoli cluster** question-based — nelle 5
lingue, con internal linking risolto per lingua verso la landing dell'app faro `fatture`.

## Decisioni prese

Change in **autopilot**: le scelte sono in [decisions.json](decisions.json). In sintesi — costruire il motore completo +
un seme minimo (non anticipare la crescita dei contenuti, #3/#4); contenuti strutturati e tipizzati come marketing/landing;
slug localizzati sotto il prefisso riservato `/blog/`; internal linking dal registro `LANDINGS` (mai cablato); `Article`
come nuovo costruttore in `seo.ts` (era esplicitamente rimandato a UC 0042); riconoscimento dei post nel controllo
post-build via marcatore `data-blog-post`; nessuno stato `draft`/`published` per i post (non serve al seme). Alla domanda
dello sviluppatore su "come/quando si aggiungono articoli" è stata registrata la futura skill **`new-blog-post`**
(backlog + memoria), con il registro dei contenuti disegnato apposta per renderla banale.

## Invarianti appgrove

Nessuno degli invarianti applicativi è in gioco: il sito vetrina è statico e pubblico, senza tenant né database. Sono
rispettate le discipline proprie del sito: parità 5 lingue a compile-time (`Record<Locale, …>`), slug localizzati con
hreflang verso i percorsi reali, gate di indicizzazione `noindex` (via `BaseLayout`), misurazione cookieless, nessun dato
personale.

## Note per il revisore

- **Contenuti AI-generati on-brand** (dec. #14 35): il seme (3 post × 5 lingue) è copy di marca da rivedere — tono lean,
  onesto (catalogo piccolo di proposito, direzione "richiamabile dall'AI" come visione, non funzione già completa).
- **Decisioni differite tracciate** in [docs/usecases/09-marketing-site/0042-blog-risorse.md](../../docs/usecases/09-marketing-site/0042-blog-risorse.md)
  §Punti aperti: (a) voce "Blog" in nav — **RISOLTA** da questa change; (b) crescita dei contenuti oltre il seme (UC 0042,
  via skill `new-blog-post` — anche in [docs/_BACKLOG.md](../../docs/_BACKLOG.md)); (c) stato `draft`/`published` dei post
  rimandato; (d) autore `Article` come Organizzazione (nota per un eventuale `author` di tipo `Person` editoriale).
- **Nessun contratto cross-area**: tutto dentro `site/`; nessun impatto su backend/infra.
- **Nessuna landing resa stale**: il diff non tocca `services/<app>`, moduli app frontend né pricing.
- **run-tests.sh** non modificato: la change resta nell'area `site` esistente (nessun modulo nuovo, nessun comando di test
  cambiato).
- **Tabella dei residui in `_INDEX.md`** (richiesta dello sviluppatore prima del commit): aggiunta una seconda tabella
  "Tabella dei residui — lavoro non ancora numerato" (R1..R20), separata e in coda alla tabella di esecuzione, che
  sintetizza da `docs/_BACKLOG.md` e dai "Punti aperti" le task/user story residue non ancora numerate — inclusa la skill
  `new-blog-post`. Documentazione, nessun impatto su codice/test.

## Test

Area `site` — eseguita via `./run-tests.sh site` (sorgente di verità): **verde**.

- `src/content/blog/blog.test.ts` (creato): parità 5 lingue dei 3 post e di `BLOG_UI` (nessuna stringa vuota, stessa forma
  della sorgente EN).
- `src/lib/blog.test.ts` (creato): `validateBlog` sul registro reale (vuoto), coerenza pilastro↔cluster reciproca,
  `blogParams` (una entry per post × lingua), hreflang localizzati, `landingHref` risolto per lingua + ripiego, ordinamento
  per data; più tre casi negativi che dimostrano che il validatore rileva slug malformati, cluster non reciproci e appId
  senza landing.
- `src/lib/seo.test.ts` (esteso): `articleJsonLd` (campi portanti, serializzazione sicura).
- Suite completa: `vitest` **138 test verdi**; `astro build` **67 pagine**; controllo post-build **verde** (incl. nuovo
  controllo #15 blog).

**Gate privacy/RoPA (UC 0031)**: scanner deterministico eseguito → **nessun segnale**. Coerente con UC 0042 §7 (contenuti
pubblici, nessun dato personale, manifesto N/A). Nessuna classificazione MAJOR/MINOR.

**Gate parità scaffold (UC 0046)**: nessun percorso-sorgente dei modelli toccato — non scatta.

## Stato criteri di accettazione

- [x] Indice `/<lang>/blog/`, pagina pilastro e pagine articolo del seme in tutte e 5 le lingue, slug localizzati; build +
  controllo post-build verdi.
- [x] Parità 5 lingue a compile-time (`Record<Locale, …>`) + test su valori/forma e coerenza pilastro↔cluster.
- [x] Ogni pagina post emette `Article` valido + breadcrumb; le pagine con FAQ emettono anche `FAQPage`; il controllo
  post-build verifica `Article` sui post.
- [x] Internal linking alla landing risolto per lingua dal registro `LANDINGS`; link interni non rotti; hreflang blog che
  risolvono.
- [x] Pagine blog nella sitemap con hreflang corretti; indice blog citato in `llms.txt`; voce "Blog" in top nav nelle 5
  lingue.
