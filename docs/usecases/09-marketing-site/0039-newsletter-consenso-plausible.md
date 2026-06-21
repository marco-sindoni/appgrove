# UC 0039 — Newsletter subscribe + consent log + Plausible (cookieless)

**Area**: 09-marketing-site · **Fase**: 3 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0036](0036-vetrina-astro-scheletro.md) (sito), UC [0013](../04-platform-core/0013-account-utenti-inviti-api.md) (core/consent store)
**Fonte decisioni**: #13 F (consenso/newsletter), #13 B (analytics), #14 49 (owned & misura)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [13-compliance-privacy](../../13-compliance-privacy.md), [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md)

## 1. Obiettivo / Scope
Implementare l'iscrizione **newsletter** con **double opt-in** + **consent log**, e l'analytics **Plausible** cookieless EU sul
solo sito vetrina.
**Incluso**: **subscribe box** sul sito + **checkbox non pre-spuntata** al signup + **toggle in impostazioni account**;
**double opt-in** + disiscrizione facile; **consent log** (chi/quando/testo-versione/come, art. 7); **Plausible Cloud**
(cookieless, EU-hosted) snippet sul sito (no banner). Newsletter = base **consenso** (6.1.a), separata dalle transazionali.
**Escluso**: il centro preferenze consensi completo in account (UC 0033); l'eventuale CMP futura per tracker non essenziali (#13 F32); SEO/GEO (UC 0040/0041).

## 2. Attori & ruoli
- **Visitatore/utente**: si iscrive/disiscrive, dà/revoca consenso.
- **Core (platform)**: registra subscriber + **consent log**; invia conferma double opt-in via SES.
- **Plausible**: analytics aggregato cookieless del sito.

## 3. Precondizioni
- Sito (UC 0036); core con store consensi/subscriber (UC 0013); SES (UC 0003/0018) per la mail di conferma; account Plausible (#13 11).

## 4. Flusso principale
1. **Subscribe box** sul sito → POST al core → crea subscriber `pending` + **email di conferma** (double opt-in) via SES (#13 F29/10).
2. Click di conferma → subscriber `confirmed`; **consent log** registra prova (testo/versione/timestamp/canale) (#13 30).
3. **Al signup**: checkbox **non pre-spuntata** (privacy by default); **toggle** in impostazioni account (UC 0033) (#13 F29/66).
4. **Disiscrizione** facile in ogni email + toggle account → revoca registrata nel consent log.
5. **Plausible**: snippet cookieless sul sito (no PII, no banner) → metriche aggregate + goal/UTM (#13 11, #14 49).

## 5. Flussi alternativi / edge / errori
- **Cookie-consent ≠ newsletter-consent**: non si uniscono (anti-pattern art. 7); il banner non serve (no tracker non essenziali) (#13 F29).
- **Email non confermata** → resta `pending`, nessun invio marketing.
- **App loggata = zero tracking** (solo Plausible sul vetrina) (#13 11).
- **Lead Form da ads** (UC 0043): lead raccolti sulla piattaforma, niente pixel/tracking sul sito (#14 48).

## 6. Schermate & stati
Subscribe box (idle/loading/success/error), email di conferma double opt-in, checkbox signup, toggle account (UC 0033).
Disiscrizione one-click. Plausible invisibile all'utente (no banner).

## 7. Dati toccati
**Subscriber** (email) + **consent log** (prova consenso/revoca). **Dati personali**: email — finalità **marketing diretto**,
base **consenso** (6.1.a); retention **iscritto + 24 mesi** post-unsubscribe (#13 E). Plausible = **aggregato non
identificativo** (legittimo interesse, cookieless). `@PersonalData` su email subscriber. Manifest: trattamenti "newsletter" + "web analytics vetrina".

## 8. Permessi & gate
- **Invarianti**: il consent store è platform-level; il toggle in account è user-scoped (tenant_id dal JWT).
- **Privacy by default**: checkbox non pre-spuntata; nessun consenso pre-attivato; revoca facile (#13 66). Diritti GDPR esenti da gate (#09 F31).

## 9. Requisiti di test
- **Integration**: double opt-in (pending→confirmed); consent log con prova; unsubscribe registra revoca; retention applicata.
- **E2E**: subscribe box, checkbox signup non pre-spuntata, toggle account, unsubscribe.
- Verifica: nessun cookie/tracker oltre Plausible cookieless; nessun banner.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #13 10/11/27/29/30/66, E23, #14 48/49.
- **DoD**:
  1. Subscribe box + checkbox signup non pre-spuntata + toggle account, double opt-in.
  2. Consent log con prova (art. 7); unsubscribe facile; retention 24 mesi post-unsubscribe.
  3. Plausible cookieless EU sul solo vetrina, nessun banner; app loggata zero tracking.
  4. Test consenso/double opt-in/unsubscribe verdi.
