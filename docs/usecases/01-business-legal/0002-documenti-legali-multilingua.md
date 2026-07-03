# UC 0002 — Documenti legali reali 5 lingue (Privacy/ToS/Refund/Cookie, IT facente fede, md single-source, versioning + accettazione scoped)

**Area**: 01-business-legal · **Fase**: 3 · **Stato**: 🟡 in corso (impianto deciso; stesura testi = deliverable, revisione legale opzionale)
**Dipendenze**: — (prerequisito Paddle; alimenta UC 0001/0036)
**Fonte decisioni**: #13 G (privacy/ToS), #14 C (testi legali), [_REVISIONE-LEGALE](../../_REVISIONE-LEGALE.md) L2/L3/L13
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [13-compliance-privacy](../../13-compliance-privacy.md), [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md), [_REVISIONE-LEGALE](../../_REVISIONE-LEGALE.md)

## 1. Obiettivo / Scope
Produrre e gestire i **documenti legali pubblici reali** richiesti da Paddle e dal GDPR, come **md single-source multilingua**
con versioning e accettazione scoped.
**Incluso**: **Privacy Policy** (art. 13-14), **Terms & Conditions** (Paddle MoR), **Refund Policy** (3° doc Paddle), **cookie
disclosure** (sezione, non banner); **lista sub-processor pubblica** `content/legal/subprocessors.{en,it,fr,es,de}.md` (nome/finalità/regione/categorie dati, vivo, **linkato da Privacy Policy e DPA**, #13 H45/H46); **5 lingue** EN/IT/FR/ES/DE, **IT facente fede** sui legali; md in **`content/legal/`**
(fonte unica sito + rendering in-app); frontmatter `version`/`effective_date`/`lang`; **check CI 5 lingue**; modello di
**accettazione/versioning scoped** (piattaforma + per-app, major→re-accept / minor→notifica).
**Escluso**: i manifesti-dati per-app e il RoPA interno (UC 0030); gli snippet privacy per-app (generati da `new-application`, UC 0046);
l'implementazione runtime dell'accettazione/ri-accettazione (derivazione + schermata bloccante + log + tabella `legal_version`, UC 0056); la revisione legale (opzionale, [_REVISIONE-LEGALE](../../_REVISIONE-LEGALE.md)).

## 2. Attori & ruoli
- **Founder/titolare**: pubblica i documenti (responsabilità).
- **AI (Claude)**: redige i testi allo stato dell'arte (#14 C16), nelle 5 lingue, tono brand.
- **Avvocato/DPO** (opzionale): revisione finale pre-go-live (L2/L3/L13).
- **Utente/interessato**: accetta/prende atto secondo lo scoping.

## 3. Precondizioni
- Struttura `content/` (#13 §50); entità legale titolare (UC 0001/L11); brand/tono (#14 F1) per il registro testuale.

## 4. Flusso principale
1. **Redazione interna AI-assistita** dei 4 documenti (PP, ToS, Refund, cookie disclosure), **IT facente fede** + EN sorgente, poi FR/ES/DE traduzioni fedeli (#14 C16, #13 G38).
2. Salvataggio in **`content/legal/`** come md multilingua con frontmatter `version`/`effective_date`/`lang` (git-backed) (#13 §50, G41).
3. **Privacy policy modulare**: nucleo piattaforma (md a mano) + moduli per-app (snippet generati dai manifesti, UC 0046/0030) (#13 G35, #14 C17).
   - **Lista sub-processor**: inventario iniziale (AWS, Plausible; Paddle = ruolo MoR a sé) in `content/legal/subprocessors.*.md`, linkato da PP/DPA; il **mantenimento incrementale** (nuovo sub-processor → notifica 30gg) è guidato dal gate `new-change` (UC 0031, #13 H46/H49).
4. **ToS** riflette Paddle MoR; **Refund** = "vendite finali/no refund salvo legge + Buyer Terms Paddle" (#09 J43, #14 A2).
5. **Versioning/accettazione scoped**: piattaforma al signup, app all'attivazione; **major** (materiale) → ri-accettazione scoped; **minor** → notifica; classificazione pilotata dal **gate privacy di `new-change`** (UC 0031, #13 G41/#14 C18).
6. **Check CI**: ogni componente presente in **tutte le 5 lingue** (build rossa se manca) (#14 C13, #13 G38).

## 5. Flussi alternativi / edge / errori
- **Nuova app pubblicata** → **non** forza ri-accettazione a chi non la usa (#14 C17, #13 G41).
- **Ri-accettazione derivata** (non marcatura di massa): al login si confronta versione-accettata vs corrente (major) → schermata bloccante solo agli utenti vincolati (#14 C20, #13 G41).
- **Conflitto tra lingue** sui legali → prevale **IT** (clausola facente fede) (#14 C10).
- **Cookie**: nessun banner (solo tecnici + Plausible cookieless) → disclosure come sezione (#13 F27/28).

## 6. Risorse & runbook
**Artefatti**: `content/legal/{privacy,terms,refund,cookie,subprocessors}.{en,it,fr,es,de}.md` con frontmatter; al deploy la CI
popola la tabella **`legal_version`** e il **log di accettazione** vive lato core (UC 0056). **Runbook**: redigere/aggiornare via `new-change` (il gate privacy
classifica major/minor) → CI verifica 5 lingue → deploy sito (UC 0036) + rendering in-app (stessi md).

## 7. Dati toccati
I documenti **descrivono** i trattamenti (non contengono dati utente). Il **log di accettazione** registra utente+versione
(dato personale minimo, base **contratto/obbligo accountability**, retention coerente #13 E). Manifest: trattamento "gestione consensi/accettazioni".

## 8. Permessi & gate
- **Invarianti**: il rendering in-app dei legali è pubblico/per l'utente loggato; il log accettazione è tenant/utente-scoped (`WHERE tenant_id`).
- **Gate**: accettazione contestuale (signup/attivazione); schermata bloccante solo per major sugli utenti vincolati; diritti GDPR sempre esenti (#09 F31).

## 9. Requisiti di test
- **Check CI**: presenza 5 lingue per ogni componente; frontmatter `version`/`effective_date` valido.
- **Integration**: derivazione "accettata < major → bloccante" corretta; nuova app non forza re-accept a chi non la usa.
- Coerenza testo pubblico minimizzato vs RoPA dettagliato (UC 0030) — stessa fonte (manifesto) per gli snippet per-app.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #13 G34–G44, H45/H46/H49, §50, #14 C16/C17/C18/C19/C20/C10, #09 J43, _REVISIONE-LEGALE L2/L3/L13.
- **DoD**:
  1. PP+ToS+Refund+cookie **+ lista sub-processor** in 5 lingue (IT facente fede) in `content/legal/`, md single-source sito+app; subprocessors linkato da PP/DPA.
  2. Versioning frontmatter + accettazione scoped (major/minor) wired al gate privacy.
  3. Check CI 5 lingue verde; documenti linkati da menu/footer (requisito Paddle).
  4. Resta 🟡 finché i **testi** non sono redatti (revisione legale opzionale).

## Punti aperti / decisioni differite

- **Riporto delle classificazioni MAJOR/MINOR accumulate** (da change 0027, UC 0031): finché `content/legal/` non
  esiste, il gate privacy di `new-change` registra la classificazione (major/minor + componente + motivazione) negli
  artefatti della change (`requirements.md` campo "Tocca dati personali?" + implementation-log). **Al primo rilascio dei
  testi legali**, ripercorrere le change accumulate e riflettere le classificazioni nel front-matter
  `version`/`effective_date` dei componenti interessati. *Perché differito*: i file legali nascono qui. *Owner*: UC 0002.
- **Seed della lista sub-processor** (da change 0027, UC 0031): creare `content/legal/subprocessors.<lang>.md` seminata
  dall'inventario #13 dec. 45 (AWS; Plausible quando attivo; Paddle escluso = titolare autonomo) **più le segnalazioni
  "potenziale nuovo sub-processor"** registrate nel frattempo negli implementation-log delle change dal gate privacy.
  *Perché differito*: la struttura `content/legal/` nasce qui. *Owner*: UC 0002.
