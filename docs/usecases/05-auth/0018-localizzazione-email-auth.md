# UC 0018 — Localizzazione email auth (Custom Message Lambda EN/IT)

**Area**: 05-auth · **Fase**: 2 · **Stato**: 🟢 deciso (resta la stesura dei testi)
**Dipendenze**: UC [0015](0015-cognito-auth-bff.md) (Cognito/SES)
**Fonte decisioni**: #02 §6 (email/template), #06 §26 (SES/DKIM/Custom Message Lambda), #14 (i18n tono)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [02-auth-sicurezza](../../02-auth-sicurezza.md), [06-infra-iac](../../06-infra-iac.md), [0017-flussi-auth](0017-flussi-auth.md)

## 1. Obiettivo / Scope
Localizzare le email transazionali di auth (verifica/reset/invito) in **EN/IT**, in base al `locale` utente, tramite il
**Custom Message Lambda trigger** di Cognito e **SES**.
**Incluso**: template propri EN/IT (verifica email, reset password, invito); Custom Message Lambda che seleziona la lingua;
invio da **`noreply@appgrove.app`** (dominio verificato + DKIM su Route53); coerenza tono/brand (#14 F1).
**Escluso**: i flussi UI (UC 0017); le altre email non-auth (es. reminder/export — capability core, UC 0032/0033); il sito (UC 0036+).

## 2. Attori & ruoli
- **Cognito** invoca il Custom Message Lambda alla generazione del messaggio.
- **Custom Message Lambda**: sceglie lingua + template, compone il testo.
- **SES**: consegna l'email da `noreply@appgrove.app`.

## 3. Precondizioni
- Cognito + auth BFF (UC 0015); SES con dominio `appgrove.app` verificato + DKIM (infra UC 0003); `locale` utente disponibile.

## 4. Flusso principale
1. Un evento auth (signup/reset/invito) fa generare a Cognito un messaggio → invoca il **Custom Message Lambda** (#02 6, #06 26).
2. La Lambda determina la **lingua** dal `locale` utente (fallback EN) e seleziona il **template** EN/IT corrispondente.
3. Compone il testo (codice/link, tono brand #14 F1) e lo restituisce a Cognito → invio via **SES** da `noreply@appgrove.app`.

## 5. Flussi alternativi / edge / errori
- **Locale assente/non supportato** → fallback **EN** (default).
- **Invito (B2B)**: il link funge da verifica email (coerente UC 0017 UC7/decisione #3).
- **Resend**: cooldown lato flusso (UC 0017 UC2); il testo resta localizzato.
- **Solo EN/IT ora**: FR/ES/DE sono per i contenuti pubblici del sito (#13 G38), non per le email auth di base (estendibili in futuro).

## 6. Schermate & stati
Nessuna UI: artefatti = template email EN/IT. Coerenza visiva minima col brand (header/wordmark), testo "lean".

## 7. Dati toccati
Email dell'utente (destinatario) + codice/link temporaneo. **Dati personali**: email, base **contratto** (email transazionali,
#13 B). Invio via SES (UE). Nessun tracking nelle email. Manifest: trattamento "email transazionali" (#13 B).

## 8. Permessi & gate
- **Invarianti**: N/A diretto (è messaging); l'identità/tenant non transita nelle email se non l'indirizzo destinatario.
- Nessun gate entitlement (auth è pre-entitlement). Email transazionali ≠ newsletter (consenso separato, #13 F29).

## 9. Requisiti di test
- **Integration/unit**: selezione lingua corretta da `locale` (+ fallback EN); template renderizzati con i campi giusti.
- **E2E** (UC 0017): verifica/reset/invito generano l'email attesa (in locale via **Mailpit**, UC 0010); contenuto localizzato.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #02 6, #06 26, #14 F1, #13 B/F29.
- **DoD**:
  1. Custom Message Lambda seleziona EN/IT dal `locale` (fallback EN).
  2. Template verifica/reset/invito in EN+IT, tono brand, invio da `noreply@appgrove.app` (DKIM).
  3. Email transazionali su base contrattuale (separate dalla newsletter).
  4. Verificabili in locale via Mailpit. (Resta la stesura dei **testi** definitivi.)
