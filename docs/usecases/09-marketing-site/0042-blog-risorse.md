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
