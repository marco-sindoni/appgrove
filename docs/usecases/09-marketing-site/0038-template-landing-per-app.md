# UC 0038 — Template landing per-app + wiring `finalize-landing`

**Area**: 09-marketing-site · **Fase**: 3 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0036](0036-vetrina-astro-scheletro.md) (skeleton sito)
**Fonte decisioni**: #14 G25 (template landing), #14 9 (new-application genera landing), #14 51-55 (gate finalizzazione)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md), [10-skills-tooling/0046-skill-new-application](../10-skills-tooling/0046-skill-new-application.md)

## 1. Obiettivo / Scope
Definire il **template ripetibile di landing per-app** e il **wiring del gate di finalizzazione** (bozza da `new-application` →
finalizzazione/pubblicazione da `finalize-landing`).
**Incluso**: struttura a **8 sezioni** (Hero job-led + screenshot; Problema→soluzione; Feature 3-6 con icone Material Symbols;
Come funziona 2-3 step; Pricing/tier mensile/annuale default annuale + trial; Badge/sezione privacy EU; FAQ; CTA finale);
base **brand kit** (UC 0019), 5 lingue; due stati **draft/published**; gate di pubblicazione (build renderizza solo `published`).
**Escluso**: la skill `new-application` (UC 0046) e la skill `finalize-landing` (UC 0057) in sé; il SEO per-app (UC 0040); la landing concreta dell'app #1 (UC 0053).

## 2. Attori & ruoli
- **`new-application`** (UC 0046): genera la **bozza** della landing (copy AI + placeholder screenshot) → `status: draft`.
- **`finalize-landing`** (skill, UC 0057): cattura screenshot reali + rifinisce + `draft→published` con review utente.
- **CI**: pubblica solo `published` (UC 0036).

## 3. Precondizioni
- Skeleton sito (UC 0036); brand kit (UC 0019); posizionamento job-led + wedge (#14 E7).

## 4. Flusso principale
1. Definire il **template** (8 sezioni, #14 G25) come componenti/layout Astro parametrici, on-brand (UC 0019).
2. `new-application` genera la **bozza** md multilingua (copy AI on-brand + **placeholder** screenshot, l'app non esiste ancora) → `status: draft` (#14 51).
3. `finalize-landing` (app a MVP/beta): (1) **screenshot reali via Playwright** + seed (#10 I), una per lingua; (2) rifinisce copy su feature reali; (3) **OG image**; (4) **review utente** 5 lingue; (5) `draft→published` (#14 51).
4. **Pubblicazione**: il build Astro renderizza solo `published`; le skill creano **contenuti**, il deploy è la **CI** (PR/merge) (#14 52/53).

## 5. Flussi alternativi / edge / errori
- **Bozza non finalizzata** → resta nel repo, **non** va online (check CI `status: published`, #14 52).
- **Landing stale**: il gate qualità di `new-change` segnala quando un cambio rilevante all'app può rendere la landing obsoleta → propone di ri-eseguire `finalize-landing` (#14 55).
- **Lingua mancante** → check CI 5 lingue.
- **Niente deploy dalle skill**: solo file nel branch/PR (#14 53).

## 6. Schermate & stati
Le 8 sezioni in light/dark, responsive, screenshot-first (#14 F3). Stati: **draft** (placeholder, non pubblicata) → **published**
(screenshot reali + OG). Multilingua con selettore.

## 7. Dati toccati
Contenuti statici per-app (md + asset); gli screenshot provengono dall'app su **seed sintetico** (#10 I) → nessun dato reale.
Manifest: N/A (marketing). La pagina richiama pricing/trial coerenti con #09.

## 8. Permessi & gate
- **Invarianti**: N/A (sito pubblico). **Gate**: `published` per andare online; review utente in `finalize-landing`; nessuna pubblicazione accidentale di pagine incomplete (#14 52).

## 9. Requisiti di test
- **Check CI**: 5 lingue, `status: published` per le live, link/OG/meta presenti (coordinato UC 0040).
- **E2E screenshot** (in `finalize-landing`): cattura deterministica via seed; baseline gestite secondo #10 F (mai update alla cieca).

## 10. Riferimenti & Definition of Done
- **Decisioni**: #14 G25, 9, 51/52/53/54/55, F3.
- **DoD**:
  1. Template landing 8 sezioni parametrico on-brand, 5 lingue.
  2. Stati draft/published; build renderizza solo published.
  3. Wiring: `new-application`→bozza, `finalize-landing`→screenshot reali+pubblicazione (review utente).
  4. Check CI 5 lingue/published; segnalazione landing stale da `new-change`.
