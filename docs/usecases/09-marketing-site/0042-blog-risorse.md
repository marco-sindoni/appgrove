# UC 0042 — Blog/risorse (pillar-cluster, contenuti SEO/GEO)

**Area**: 09-marketing-site · **Fase**: 7 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0036](0036-vetrina-astro-scheletro.md) (sito), UC [0040](0040-seo-tecnico.md) (SEO)
**Fonte decisioni**: #14 G28 (blog dal lancio), #14 H32 (pillar-cluster), #14 I42 (question-based), #14 35 (AI-generato)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md)

## 1. Obiettivo / Scope
Implementare il **blog/risorse** come motore di SEO/GEO che cresce nel tempo.
**Incluso**: **struttura presente dal lancio** (base SEO/GEO); modello **pillar + cluster** (pilastri per tema + long-tail
how-to/confronti con internal linking alle landing app); contenuti **question-based** (rispondono come si chiede a un'AI);
tutto **AI-generato on-brand** (tono lean), 5 lingue; integrazione SEO (Schema.org Article, hreflang) e GEO (FAQ/confronti).
**Escluso**: il SEO tecnico di base (UC 0040), la GEO di base (UC 0041), il lancio paid/social (UC 0043), la skill campagne (UC 0050).

## 2. Attori & ruoli
- **Lettori/ICP + motori di ricerca + assistenti AI**: consumano/indicizzano/citano.
- **AI (Claude)**: genera articoli on-brand; **utente rivede e approva** (#14 35).

## 3. Precondizioni
- Sito (UC 0036) con i18n/content md; SEO/GEO (UC 0040/0041); landing app per l'internal linking (UC 0038/0053).

## 4. Flusso principale
1. **Struttura dal lancio** (categorie/cluster) anche con pochi contenuti; cresce nel tempo (#14 G28).
2. **Pillar + cluster**: pagine pilastro per tema + articoli long-tail (how-to/confronti) con **internal linking** alle landing app (#14 H32).
3. **Question-based** per la GEO: articoli "best [tool] for [ICP] GDPR/EU", confronti, how-to, formattati per le risposte AI (#14 I42).
4. Contenuti **AI-generati on-brand**, 5 lingue, con Schema.org `Article` + hreflang (UC 0040) (#14 35).

## 5. Flussi alternativi / edge / errori
- **Localizzazione**: per mercato/lingua (non traduzione meccanica), slug localizzati (UC 0040) (#14 30/31).
- **Riuso social**: i contenuti alimentano LinkedIn/X (UC 0043) (#14 46).
- **Lingua mancante** → check CI 5 lingue.

## 6. Schermate & stati
Indice blog + pagine pilastro + articoli (responsive, light/dark), con breadcrumb/Schema.org. Stati statici (SSG). Internal
link alle landing app.

## 7. Dati toccati
Contenuti pubblici, nessun dato personale. Misura via Plausible (cookieless) + referral AI (UC 0041). Manifest: N/A.

## 8. Permessi & gate
- **Invarianti**: N/A (sito pubblico). Gate `published`; nessun tracking comportamentale.

## 9. Requisiti di test
- **Check CI**: 5 lingue, link non rotti (incl. internal verso le landing), Schema.org Article valido, `published`.
- Coerenza pillar-cluster/internal linking; perf statica.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #14 G28, H30/31/32, I42, 35.
- **DoD**:
  1. Struttura blog dal lancio (pillar+cluster), 5 lingue, AI-generata on-brand (review utente).
  2. Contenuti question-based GEO + internal linking alle landing app.
  3. Schema.org Article + hreflang; misura Plausible/referral AI.
  4. Check CI 5 lingue/link/published verde.

## Punti aperti / decisioni differite

- ✅ **Voce "Blog" nella top nav del sito** — **RISOLTO dalla change `0061-use-case-0042-…`** (2026-07-26): aggiunta la
  voce `nav.blog` al modello marketing nelle 5 lingue e il link `/<lang>/blog/` in `site/src/layouts/BaseLayout.astro`.
  Il link non è più rotto perché l'indice blog ora esiste (il check "link interni non rotti" resta verde).

- **Crescita dei contenuti (altri pilastri e articoli cluster oltre il seme)** _(tracciato dalla change
  `0061-use-case-0042-…`)_: la change 0061 costruisce il **motore** del blog e un **seme minimo** — 1 pilastro
  ("Fatturazione per piccole imprese in UE") + 2 articoli cluster question-based, nelle 5 lingue. Il blog è progettato
  per **crescere nel tempo** (flusso principale §1): nuovi pilastri per altri temi e nuovi articoli cluster si aggiungono
  in seguito. Lo strumento previsto è la futura skill **`new-blog-post`** (registrata in
  [docs/_BACKLOG.md](../../_BACKLOG.md) §Skill da creare + memoria `skills-backlog`): generatore deterministico per lo
  scaffold dei 5 file-lingua + registro + agganci pilastro↔cluster, co-pilota per la copy on-brand. Il registro
  (`site/src/content/blog/`) è disegnato apposta perché aggiungere un articolo sia "una cartella + una entry nell'array".

- **Stato `draft`/`published` per i post del blog** _(tracciato dalla change `0061-use-case-0042-…`)_: le landing hanno un
  gate `draft`→`published` (UC 0038, #14 52); i post del blog **no** — nella change 0061 ogni post nel registro è
  pubblicato (scelta "più semplice e corretta": il seme è approvato dallo sviluppatore al merge, dec. #14 35). Se la skill
  `new-blog-post` avrà bisogno di **preparare una bozza** prima di pubblicarla (revisione a più passaggi), introdurre allo
  `BlogPost` un campo `status` con lo stesso gate strutturale delle landing (solo `published` in `getStaticPaths`). Non
  fatto ora perché non serve al seme e non va anticipato.

- **Autore dello Schema.org `Article`** _(tracciato dalla change `0061-use-case-0042-…`)_: l'`Article` usa
  l'**Organizzazione** appgrove come `author`/`publisher` (contenuti AI-generati on-brand, dec. #14 35), non una persona
  fisica. Coerente con la postura attuale; se in futuro si vorrà una firma d'autore editoriale, estendere il costruttore
  `articleJsonLd` con un `author` di tipo `Person`.
