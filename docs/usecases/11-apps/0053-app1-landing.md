# UC 0053 — App #1 landing (bozza → `finalize-landing`)

**Area**: 11-apps · **Fase**: 4 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0038](../09-marketing-site/0038-template-landing-per-app.md) (template landing), UC [0052](0052-app1-modulo-frontend.md) (app MVP per gli screenshot)
**Fonte decisioni**: #14 G25/9/51-55 (landing + gate finalizzazione), #14 F (brand)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md), [0038-template-landing-per-app](../09-marketing-site/0038-template-landing-per-app.md), [0046-skill-new-application](../10-skills-tooling/0046-skill-new-application.md)

## 1. Obiettivo / Scope
Produrre la **landing pubblica della prima app** seguendo il flusso bozza→finalizzazione, così il sito mostra un'app reale (il
"seme del grove") prima di sottomettere a Paddle.
**Incluso**: **bozza** generata dal template (UC 0038) con copy AI on-brand + placeholder; **finalizzazione** con screenshot
reali (Playwright + seed), copy rifinito, OG image, review utente 5 lingue, `draft→published`; SEO/GEO per-app coerenti.
**Escluso**: il template in sé (UC 0038), la skill `new-application` (UC 0046), la skill `finalize-landing` (skill dedicata,
qui se ne descrive l'uso sull'app #1); il deploy (CI, UC 0005).

## 2. Attori & ruoli
- **`new-application`/template** (UC 0038/0046): genera la bozza.
- **`finalize-landing`** (skill): cattura screenshot reali + finalizza + pubblica.
- **Founder**: review/approva nelle 5 lingue (#14 35).

## 3. Precondizioni
- Template landing (UC 0038); app #1 a **MVP/beta** in esecuzione + seed (UC 0051/0052/0011) per gli screenshot reali (#14 51).

## 4. Flusso principale
1. **Bozza**: copy job-led + value prop + feature + pricing/tier (mensile/annuale default annuale, trial) + badge privacy EU, 8 sezioni, 5 lingue, **placeholder** screenshot → `status: draft` (#14 G25/9/51).
2. **Finalizzazione** (`finalize-landing`): screenshot reali via **Playwright** contro l'app + seed (una per lingua); copy rifinito su feature reali; **OG image**; **review utente** (#14 51).
3. `draft → published`; il build Astro la renderizza (gate `published`); deploy via CI (#14 52/53).

## 5. Flussi alternativi / edge / errori
- **App non ancora MVP**: resta in bozza (placeholder), non pubblicata (#14 51/52).
- **Landing stale** dopo cambi rilevanti all'app → `new-change` propone di ri-eseguire `finalize-landing` (#14 55).
- **Lingua/screenshot mancante** → check CI (5 lingue/published) blocca.

## 6. Schermate & stati
Le 8 sezioni della landing app #1 in light/dark, screenshot reali post-finalizzazione, responsive. Stati draft (placeholder) →
published (screenshot + OG). Multilingua.

## 7. Dati toccati
Contenuti statici per-app; screenshot dall'app su **seed sintetico** (no dati reali). Manifest: N/A (marketing). Pricing/trial
coerenti con #09 (la landing è "vetrina", non checkout).

## 8. Permessi & gate
- **Invarianti**: N/A (sito pubblico). **Gate**: `published` per andare online; review utente; nessuna pubblicazione di pagina incompleta.

## 9. Requisiti di test
- **Check CI**: 5 lingue, `published`, OG/meta/SEO per-app presenti (UC 0040/0041).
- **E2E screenshot** (finalize-landing): cattura deterministica via seed; baseline gestite secondo #10 F.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #14 G25, 9, 51/52/53/55, 35, F3.
- **DoD**:
  1. Bozza landing app #1 (8 sezioni, 5 lingue, copy AI on-brand, placeholder).
  2. Finalizzazione con screenshot reali + OG + review utente → published.
  3. SEO/GEO per-app coerenti; check CI verde.
  4. La homepage mostra un'app reale prima della sottomissione Paddle.
