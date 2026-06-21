# UC 0043 — Lancio paid/social (Product Hunt, directory, LinkedIn, Meta/Google cookieless)

**Area**: 09-marketing-site · **Fase**: 7 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0037](0037-homepage-nav-footer.md) (homepage/sito pronto)
**Fonte decisioni**: #14 J (paid/social/launch), #14 I41 (off-site/directory)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md), [10-skills-tooling/0050-skill-campaign-guide](../10-skills-tooling/0050-skill-campaign-guide.md)

## 1. Obiettivo / Scope
Eseguire il **lancio lean a due livelli** (per-app + brand) con canali organici/paid coerenti con la postura privacy.
**Incluso**: **Product Hunt**, **directory** (AlternativeTo/G2/Capterra/SaaSHub — anche GEO), **community indie/dev**;
**social organico** account **brand** (**LinkedIn** primario, X opzionale, **no personale/no build-in-public**); **paid** Google
Search primario + Meta dopo (budget piccoli di validazione); **postura cookieless difesa** (no pixel/CAPI-PII, obiettivo Traffico
+ Lead Form, attribuzione UTM+Plausible); **owned** newsletter come nurture.
**Escluso**: la skill `campaign-guide` (UC 0050, che guida la creazione), il blog (UC 0042), la newsletter backend (UC 0039).

## 2. Attori & ruoli
- **Founder/marketer**: esegue il lancio (guidato da `campaign-guide`, UC 0050).
- **Piattaforme** (Product Hunt/directory/LinkedIn/Meta/Google): canali.

## 3. Precondizioni
- Sito + homepage + landing app pronti (UC 0036/0037/0053); newsletter/Plausible (UC 0039); entità canonica/GEO (UC 0041); `campaign-guide` (UC 0050).

## 4. Flusso principale
1. **Lancio a due livelli**: per-app (community della sua ICP) + brand (#14 J45).
2. **Off-site/directory** (Product Hunt, AlternativeTo/G2/Capterra/SaaSHub, community indie/dev) → corroborazione + GEO (#14 J45, I41).
3. **Social organico brand**: LinkedIn primario (ICP SMB EU), X opzionale; contenuti **riusati dal blog**, AI-generati on-brand (#14 J46).
4. **Paid**: Google Search primario (intento alto) + Meta dopo (discovery/retargeting), budget piccoli di validazione (#14 J47).
5. **Cookieless difesa**: niente pixel/CAPI-PII; obiettivo **Traffico** + **Lead Form native** (lead sulla piattaforma); **attribuzione** UTM + goal Plausible + click piattaforme (#14 J48/49).

## 5. Flussi alternativi / edge / errori
- **Trade-off**: ottimizzazione/attribuzione più deboli, ma **coerenza di brand** (#14 J48).
- **Conformità**: ogni campagna passa dalla checklist di `campaign-guide` (no pixel/CAPI-PII/banner) (#14 J50).
- **Valida-prima-di-monetizzare**: budget piccoli, coerente cost-min (#14 J47).

## 6. Schermate & stati
Non è UI di prodotto: sono **artefatti/azioni** (post Product Hunt, schede directory, post LinkedIn, campagne Google/Meta) +
misura via Plausible (UTM/goal/referral, incl. AI). Lead Form → newsletter (UC 0039).

## 7. Dati toccati
Lead da Lead Form → **sulla piattaforma** (newsletter, consenso, UC 0039); nessun tracking PII sul sito. Manifest: la raccolta
lead segue il trattamento newsletter (#13 F). Misura aggregata Plausible.

## 8. Permessi & gate
- **Invarianti**: N/A (marketing). **Gate**: conformità privacy (checklist `campaign-guide`); lead solo con consenso (double opt-in UC 0039).

## 9. Requisiti di test
Operativo/non-software: verifica che le campagne rispettino la checklist (no pixel/CAPI-PII, UTM coerenti, Lead Form), che
l'attribuzione Plausible funzioni (UTM/goal) e che i lead entrino nel flusso newsletter con consenso.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #14 J45/J46/J47/J48/J49, I41.
- **DoD**:
  1. Lancio a due livelli (Product Hunt/directory/community + brand LinkedIn).
  2. Paid Google Search + Meta cookieless (Traffico/Lead Form), attribuzione UTM+Plausible.
  3. Postura privacy difesa (no pixel/CAPI-PII); lead con consenso (UC 0039).
  4. Campagne conformi via `campaign-guide`; misura referral (incl. AI).
