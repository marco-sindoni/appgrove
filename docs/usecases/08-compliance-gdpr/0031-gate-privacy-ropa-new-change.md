# UC 0031 — Gate privacy/RoPA in `new-change` (co-pilota classificazione + enforcement ArchUnit)

**Area**: 08-compliance-gdpr · **Fase**: 6 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0044](../10-skills-tooling/0044-aggiornamento-skill-new-change.md) (new-change), UC [0030](0030-manifesti-dati-ropa.md) (manifesti/RoPA)
**Fonte decisioni**: #13 C16 (gate privacy), #13 G41 (versioning PP), #14 C18, #10 D16 (ArchUnit)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [13-compliance-privacy](../../13-compliance-privacy.md), [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md), [10-testing](../../10-testing.md)

## 1. Obiettivo / Scope
Implementare il **gate privacy/RoPA** dentro il workflow `new-change` (privacy-by-design, art. 25): ogni cambio che tocca dati
personali viene intercettato, classificato e applicato a manifesto/RoPA, con enforcement CI.
**Incluso**: **rilevamento segnali** (migrazioni con nuove colonne/tabelle, nuovi campi entità/DTO/API, nuove integrazioni esterne
= potenziale sub-processor, modifiche a retention/finalità); **classificazione assistita** (co-pilota che ragiona con l'utente:
elicita lo scopo, deduce/propone natura/finalità/base/retention con motivazione, domande solo se ambiguo, fa confermare);
**escalation art. 9** (avviso forte + DPIA); **enforcement CI bloccante** (campo `@PersonalData` non dichiarato → build rossa);
**classificazione MAJOR/MINOR** che pilota il **bump versione PP/ToS** → re-accept scoped / notifica.
**Escluso**: i manifesti/RoPA in sé (UC 0030), l'UI/derivazione di re-accept (UC 0056), i testi legali (UC 0002).

## 2. Attori & ruoli
- **Developer + skill `new-change`**: il co-pilota classifica insieme all'utente.
- **CI**: enforcement bloccante (ArchUnit-style).
- **Utenti vincolati**: ricevono re-accept (major) o notifica (minor) (UC 0056).

## 3. Precondizioni
- `new-change` (UC 0044) con hook predisposto; manifesti/RoPA (UC 0030); annotazione/tag `@PersonalData` nel codice.

## 4. Flusso principale
1. **Rilevamento segnali** sul diff: migrazioni Flyway con nuove colonne/tabelle, nuovi campi entità/DTO/API, nuove integrazioni esterne, modifiche retention/finalità (#13 C16).
2. **Classificazione assistita (co-pilota)**: elicita lo scopo del campo, **deduce e propone** natura/finalità/base/retention con motivazione, pone domande **solo se ambiguo**, fa **confermare** → aggiorna manifesto + RoPA (#13 C16).
3. **Escalation art. 9** (salute/biometrici/…): avviso **forte** + DPIA (#13 K) + base rafforzata; non si procede in sordina (#13 C16).
4. **Enforcement CI (bloccante)**: campo marcato `@PersonalData` **non dichiarato** nel manifesto → build fallisce (stile ArchUnit, #10 D16) (#13 C16).
5. **MAJOR/MINOR**: il gate classifica il cambio come **materiale** (finalità/basi/categorie/retention → **major** → re-accept scoped) o non materiale (**minor** → notifica), pilotando il bump versione PP/ToS (#13 G41, #14 C18).
6. **Sub-processor**: nuova integrazione esterna → segnala "potenziale nuovo sub-processor" → aggiorna lista + notifica clienti (preavviso 30gg) (#13 C49).

## 5. Flussi alternativi / edge / errori
- **Caso ambiguo** (es. telefono per feature vs ricontatto commerciale) → domanda di approfondimento (cambia la base) (#13 C16).
- **Pseudonimizzazione spacciata per erasure**: bloccata (guardrail anonimizzazione, #13 L72) — coordinato con `new-application`.
- **Cambio non materiale** → solo notifica non bloccante (#13 G41).
- **DPIA screening**: il gate estende l'escalation art. 9 con i criteri art. 35/EDPB (#13 K67).

## 6. Risorse & runbook
**Artefatti**: hook nel flusso `new-change` (step requisiti/implementazione), check CI ArchUnit-style, aggiornamento manifesto/RoPA/
lista sub-processor + log re-accept. **Runbook**: durante un `new-change` che tocca dati personali, il co-pilota guida la
classificazione → CI verifica → bump versione PP se major.

## 7. Dati toccati
Aggiorna **dichiarazioni** (manifesto/RoPA/sub-processor/versione PP), non dati utente. Innesca il re-accept (UC 0056).
Manifest: **è** il meccanismo che lo mantiene aggiornato (accountability).

## 8. Permessi & gate
- **Invarianti**: ogni dato personale dichiarato e coperto; privacy-by-design enforced a build-time.
- **Gate**: CI bloccante (`@PersonalData` non dichiarato); major → re-accept scoped (#13 G41); diritti GDPR sempre esenti dai gate runtime (#09 F31).

## 9. Requisiti di test
- **CI**: campo `@PersonalData` non dichiarato → build rossa; rilevamento segnali su migrazioni/campi/integrazioni.
- Verifica: major → bump major + re-accept scoped; minor → notifica; art. 9 → escalation/DPIA.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #13 C16/G41/K67/L72/C49, #14 C18, #10 D16.
- **DoD**:
  1. Gate in `new-change`: rilevamento segnali + classificazione assistita + escalation art. 9.
  2. Enforcement CI bloccante (`@PersonalData` non dichiarato).
  3. MAJOR/MINOR pilota il bump versione PP/ToS → re-accept scoped / notifica.
  4. Segnalazione nuovi sub-processor → aggiornamento lista + notifica.
