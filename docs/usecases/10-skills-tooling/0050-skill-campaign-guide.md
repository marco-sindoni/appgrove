# UC 0050 — skill `campaign-guide`

**Area**: 10-skills-tooling · **Fase**: 7 · **Stato**: 🟢 deciso (skill da implementare)
**Dipendenze**: — (si appoggia a #14 J e Plausible UC 0039)
**Fonte decisioni**: #14 J50 (skill campaign-guide), #14 J48 (cookieless difesa), #14 35 (AI on-brand)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md), [09-marketing-site/0039-newsletter-consenso-plausible](../09-marketing-site/0039-newsletter-consenso-plausible.md)

## 1. Obiettivo / Scope
Creare la skill **`campaign-guide`**: una **guida passo-passo** alla creazione di campagne (Meta/Google) che **non violano** la
postura privacy (cookieless, no-pixel/CAPI-PII, no-banner, EU-purista).
**Incluso**: guida passo-passo con **checklist di conformità** ad ogni step; **obiettivi ammessi** (Traffico / **Lead Form native**);
**convenzioni UTM** coerenti (così Plausible attribuisce); **copy/creatività AI on-brand** (tono F1); cosa **non** fare (niente
pixel Meta/Google, niente CAPI con PII). **Evoluzione futura**: assistente **Playwright non-headless** che guida/pilota la UI di
creazione campagna.
**Escluso**: il lancio operativo (UC 0043), la newsletter/analytics in sé (UC 0039), la generazione contenuti del sito (UC 0037/0042).

## 2. Attori & ruoli
- **Founder/marketer**: invoca la skill, segue gli step, crea la campagna sulle piattaforme.
- **Skill** (co-pilota): guida + checklist conformità + UTM + copy AI on-brand.

## 3. Precondizioni
- Plausible attivo (UC 0039) per l'attribuzione UTM/goal; postura privacy definita (#14 J48); brand/tono (#14 F1).

## 4. Flusso principale
1. `/campaign-guide` → tipo campagna (Google Search primario / Meta dopo) + obiettivo **ammesso** (Traffico/Lead Form) (#14 J47/48).
2. **Checklist di conformità** ad ogni step: **niente pixel/CAPI-PII**, no-banner, EU-purista; lead raccolti **sulla piattaforma** (Lead Form), zero tracking sul sito (#14 J48).
3. **UTM coerenti**: convenzioni che permettono a **Plausible** di attribuire (UTM + goal + click delle piattaforme) (#14 J48/49).
4. **Copy/creatività AI on-brand** (tono F1, dec. 35) per la campagna (#14 J50).
5. (Futuro) assistente **Playwright non-headless** che guida la UI di creazione campagna (#14 J50).

## 5. Flussi alternativi / edge / errori
- **Trade-off accettato**: attribuzione/ottimizzazione più deboli in cambio di **coerenza di brand** (on-message) (#14 J48).
- **Tentativo di usare pixel/CAPI-PII** → la checklist lo **blocca** (violerebbe il pilastro privacy) (#14 J48).
- **Niente build-in-public/personale**: account brand (coerente no-founder-story) (#14 J46).

## 6. Risorse & runbook
**File skill** `.claude/skills/campaign-guide/`. **Output**: guida step-by-step + checklist conformità + UTM + copy AI on-brand
(eventualmente file/asset nel repo). **Runbook**: `/campaign-guide` → seguire gli step → creare la campagna sulle piattaforme →
misurare via Plausible (UTM/goal).

## 7. Dati toccati
Nessun dato utente trattato dalla skill (produce guida/copy/UTM). I lead da Lead Form arrivano **sulla piattaforma** (newsletter
UC 0039, consenso). Manifest: N/A (tooling); la raccolta lead segue il trattamento newsletter (#13 F).

## 8. Permessi & gate
- **Invarianti**: N/A (tooling marketing). **Gate**: checklist di conformità privacy ad ogni step (no pixel/CAPI-PII/banner).

## 9. Requisiti di test
Skill/deliverable: verifica che la guida produca solo configurazioni conformi (obiettivi ammessi, UTM coerenti, niente
pixel/CAPI-PII) e copy on-brand. (Nessun test runtime di prodotto.)

## 10. Riferimenti & Definition of Done
- **Decisioni**: #14 J46/J47/J48/J49/J50, 35, F1.
- **DoD**:
  1. `campaign-guide` guida passo-passo con checklist di conformità (cookieless/no-pixel/no-banner/EU-purista).
  2. Obiettivi ammessi (Traffico/Lead Form) + UTM coerenti per Plausible + copy AI on-brand.
  3. Blocca configurazioni non conformi (pixel/CAPI-PII).
  4. Evoluzione futura: assistente Playwright non-headless (tracciata).
