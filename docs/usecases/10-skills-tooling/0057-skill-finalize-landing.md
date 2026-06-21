# UC 0057 — skill `finalize-landing` (bozza → landing pubblicata: rifinitura testi/visual 5 lingue + flag `published` + CI deploy)

**Area**: 10-skills-tooling · **Fase**: 3 · **Stato**: 🟡 in corso
**Dipendenze**: UC [0038](../09-marketing-site/0038-template-landing-per-app.md) (template landing + wiring), UC [0046](0046-skill-new-application.md) (genera la bozza)
**Fonte decisioni**: #14 dec.51/52/53 (due momenti landing, gate pubblicazione, skill non-deploy)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md), [09-marketing-site/0038](../09-marketing-site/0038-template-landing-per-app.md), [09-marketing-site/0036](../09-marketing-site/0036-vetrina-astro-scheletro.md)

## 1. Obiettivo / Scope
Creare la skill **`finalize-landing`** che porta la **bozza** di landing (generata da `new-application`, UC 0046) allo stato
**pubblicato**: rifinitura testi/visual on-brand nelle 5 lingue, completamento SEO/GEO, e impostazione del **flag `published`**
che abilita il deploy (la CI pubblica al merge, UC 0036). È il **secondo momento** della landing (#14 dec.51).
**Incluso**: file skill `.claude/skills/finalize-landing/`; rifinitura copy/visual (sostituisce i placeholder della bozza),
parità **5 lingue** (EN sorgente marketing), completamento meta/OG/hreflang/Schema.org (UC 0040) e materiale GEO (UC 0041);
impostazione **`published: true`** nel frontmatter del content; segue `new-change` → branch + **PR**.
**Escluso**: il **deploy** (lo fa la CI al merge, UC 0036 — la skill scrive content, non pubblica, #14 dec.53); il template/struttura
(UC 0038); la bozza iniziale (UC 0046); i testi legali (UC 0002).

## 2. Attori & ruoli
- **Developer/Founder**: invoca `/finalize-landing <app_id>` quando l'app è MVP; rivede testi/visual; mergia la PR.
- **Skill** (tooling): rifinisce content e imposta `published`; lascia la **PR** all'utente.
- **CI** (UC 0005/0036): al merge builda solo i content `published` e fa deploy (gate pubblicazione #14 dec.52).

## 3. Precondizioni
- Bozza landing esistente per `app_id` (UC 0046) + template/wiring (UC 0038); vetrina Astro (UC 0036) attiva.

## 4. Flusso principale
1. `/finalize-landing <app_id>` → carica la bozza (8 sezioni, UC 0038) e i placeholder da rifinire.
2. **Rifinitura testi**: copy on-brand definitivo, EN sorgente + IT/FR/ES/DE; review dell'utente (#14 35).
3. **Rifinitura visual**: asset on-brand (icona/colore-categoria, immagini), coerenti col brand kit (UC 0019).
4. **SEO/GEO**: completa meta/OG, hreflang, Schema.org (UC 0040) e statement/FAQ machine-readable + `llms.txt`/entità (UC 0041).
5. **Gate pubblicazione**: imposta `published: true` nel frontmatter → la CI includerà la pagina nel build (#14 dec.52).
6. Segue `new-change`: branch + **PR**; al merge la CI pubblica (UC 0036) (#14 dec.53).

## 5. Flussi alternativi / edge / errori
- **Check CI 5 lingue** rosso se manca una lingua o un campo SEO obbligatorio (#14 C13, H34).
- **Landing stale**: una `new-change` che cambia feature/pricing dell'app segnala la landing da riallineare → ri-eseguire `finalize-landing` (#14 dec.55, UC 0038 §5).
- **Niente deploy diretto**: la skill **non** pubblica; solo content + flag (PR → CI) (#14 dec.53).

## 6. Risorse & runbook
**File skill** `.claude/skills/finalize-landing/`. **Output**: PR con content landing rifinito (5 lingue) + `published: true` + SEO/GEO.
**Runbook**: `/new-application` (bozza) → MVP → `/finalize-landing <app_id>` → rivedere/mergiare → CI pubblica.

## 7. Dati toccati
Genera **contenuti** (md/MDX della landing), non dati runtime. Nessun dato personale. Manifest GDPR N/A.

## 8. Permessi & gate
- **Invarianti tecnici**: N/A (tooling di contenuti). **Gate**: pubblicazione solo via `published` + merge → CI (#14 dec.52/53); parità 5 lingue bloccante.

## 9. Requisiti di test
Skill di tooling: l'output deve passare il **check CI** (5 lingue + SEO presenti) e buildare nella vetrina (UC 0036).
Verifica: bozza → published con placeholder risolti, hreflang/Schema.org completi, nessuna lingua mancante.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #14 dec.51/52/53/55, H34, C13, 35; UC 0038/0046/0036.
- **DoD**:
  1. `finalize-landing` rifinisce copy+visual 5 lingue e completa SEO/GEO.
  2. Imposta `published: true`; non fa deploy (CI al merge).
  3. Segue `new-change` (branch+PR); check CI 5 lingue + SEO verde.
  4. Gestisce il caso "landing stale" segnalato da `new-change`.
