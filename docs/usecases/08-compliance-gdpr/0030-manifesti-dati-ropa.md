# UC 0030 — Manifesti dati per-app + RoPA automation (assembla IT+EN, check CI)

**Area**: 08-compliance-gdpr · **Fase**: 6 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0046](../10-skills-tooling/0046-skill-new-application.md) (new-application genera i manifesti)
**Fonte decisioni**: #13 C (RoPA/manifesti), #07 (check CI)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [13-compliance-privacy](../../13-compliance-privacy.md), [10-skills-tooling/0046-skill-new-application](../10-skills-tooling/0046-skill-new-application.md)

## 1. Obiettivo / Scope
Implementare il **manifesto-dati per-app** come **fonte unica** e l'**automazione del RoPA** (Registro trattamenti art. 30).
**Incluso**: formato del **manifesto** per-app (categorie dati personali, finalità, base giuridica, retention, ubicazione `app_<id>`,
`@PersonalData`); **assemblaggio RoPA** in **due file per lingua** (`docs/compliance/ropa.it.md` + `ropa.en.md`) dalla fonte unica
(sezione piattaforma bilingue + manifesti per-app bilingui); **check CI** (stesso set di voci nelle 2 lingue; campo `@PersonalData`
non dichiarato → build rossa). RoPA = **interno, non pubblico**.
**Escluso**: lo snippet privacy pubblico (UC 0002, stessa fonte), il gate `new-change` (UC 0031), export/erasure (UC 0032).

## 2. Attori & ruoli
- **`new-application`/`new-change`**: compilano il manifesto (IT+EN) (co-pilota #13 C16).
- **Sistema CI**: assembla RoPA + verifica completezza/parità lingue.
- **Garante** (su richiesta): destinatario del RoPA (interno).

## 3. Precondizioni
- Manifesti per-app generati da `new-application` (UC 0046); struttura `docs/compliance/` interna (#13 §50).

## 4. Flusso principale
1. Ogni app dichiara il **manifesto** (categorie/finalità/base/retention/ubicazione), in **IT+EN**, come fonte unica (#13 C14/C15).
2. La **sezione piattaforma** (stabile, bilingue) + i manifesti per-app → **assemblati** in `ropa.it.md` e `ropa.en.md` (zero drift) (#13 C14).
3. Il manifesto **alimenta tre output**: RoPA interno, **snippet privacy pubblico** (UC 0002), **export/erasure** (UC 0032) — una dichiarazione, tre usi (#13 C15, G35).
4. **Check CI**: le due lingue hanno lo **stesso set di voci** (traduzione mancante → segnala); campo marcato `@PersonalData` non dichiarato → **build rossa** (stile ArchUnit) (#13 C14/C16).

## 5. Flussi alternativi / edge / errori
- **Categoria particolare (art. 9)** in un manifesto → escalation forte + DPIA (gate UC 0031) (#13 C16).
- **RoPA non pubblico** (art. 30.4): solo per il Garante; ≠ privacy policy pubblica (#13 C17).
- **Lingua mancante** → check CI fallisce (#13 C14).
- **Nuova integrazione esterna** → potenziale sub-processor → segnalato dal gate (UC 0031) e aggiunto a `subprocessors.md`.

## 6. Risorse & runbook
**Artefatti**: schema manifesto per-app; `docs/compliance/ropa.{it,en}.md` (assemblati); check CI completezza/parità. **Runbook**:
il manifesto si compila in `new-application`/`new-change` → CI assembla il RoPA e verifica; aggiornamenti versionati in git.

## 7. Dati toccati
**Descrive** i trattamenti (non contiene dati utente). Riguarda i dati personali delle app (categorie/finalità/base/retention).
Manifest GDPR: **è** lo strumento di accountability. RoPA interno cifrato/versionato in `docs/`.

## 8. Permessi & gate
- **Invarianti**: ogni entità con dati personali deve essere **coperta** dal manifesto (e da export/purge, UC 0032); enforcement CI.
- **Gate**: check CI bloccante (parità lingue + `@PersonalData` dichiarato). Coordinato col gate `new-change` (UC 0031).

## 9. Requisiti di test
- **Check CI**: RoPA IT/EN stesso set di voci; `@PersonalData` non dichiarato → fallisce.
- **Compliance** (#13 L74): coerenza manifesto ↔ export/purge (nessuna entità personale scoperta).

## 10. Riferimenti & Definition of Done
- **Decisioni**: #13 C13/C14/C15/C16/C17/C18, G35, §50, L74.
- **DoD**:
  1. Manifesto per-app (fonte unica) + assemblaggio RoPA IT/EN automatico.
  2. Check CI: parità lingue + `@PersonalData` dichiarato (bloccante).
  3. Manifesto alimenta RoPA + privacy snippet + export/erasure.
  4. RoPA interno (non pubblico) versionato in `docs/compliance/`.
