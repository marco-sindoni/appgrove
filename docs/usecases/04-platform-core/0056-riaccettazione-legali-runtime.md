# UC 0056 — Ri-accettazione ToU/PP a runtime (derivazione al login + schermata bloccante + log accettazione)

**Area**: 04-platform-core · **Fase**: 3 · **Stato**: 🟡 in corso
**Dipendenze**: UC [0002](../01-business-legal/0002-documenti-legali-multilingua.md) (versioning legali), UC [0013](0013-account-utenti-inviti-api.md) (log accettazione/account), UC [0020](../06-frontend/0020-shell-spa-backoffice.md) (shell per la schermata)
**Fonte decisioni**: #14 C18/C20, #13 G41, #02 (login/refresh)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [14-sito-vetrina-legale](../../14-sito-vetrina-legale.md), [13-compliance-privacy](../../13-compliance-privacy.md), [01-business-legal/0002](../01-business-legal/0002-documenti-legali-multilingua.md)

## 1. Obiettivo / Scope
Implementare il **meccanismo runtime di accettazione e ri-accettazione** dei documenti legali (ToU/Privacy/Refund + componenti
per-app), **derivato** (non marcatura di massa): al login/refresh si confronta la versione accettata con quella corrente e, se è
cambiato un **major**, si presenta una **schermata bloccante** prima dell'ingresso. Chiude la copertura implementativa di #14 C20
(UC 0002 ne modella solo l'impianto).
**Incluso**: tabella/log **accettazione** (utente+componente+versione+`accepted_at`+commit hash) lato core; **API** per leggere lo
stato accettazione corrente vs versioni effettive (da `content/legal/` frontmatter, UC 0002); **derivazione** al login/refresh
(`versione-accettata < major corrente` → da ri-accettare); **schermata bloccante** nella shell (UC 0020) con scope corretto
(piattaforma vs componente per-app); registrazione dell'accettazione al signup (piattaforma) e all'attivazione app (per-app).
**Escluso**: la redazione/versioning dei testi (UC 0002); la classificazione major/minor di un cambio (gate `new-change`, UC 0031);
il consent center marketing/cookie (UC 0033/0039, trattamento distinto).

## 2. Attori & ruoli
- **Utente** (owner/admin/member): accetta al signup/attivazione; ri-accetta su major.
- **Sistema (core)**: deriva lo stato, espone API, registra il log; **auth Lambda** veicola il segnale al login/refresh (#02 2).
- **Shell** (UC 0020): rende la schermata bloccante e invia l'accettazione.

## 3. Precondizioni
- Documenti legali versionati in `content/legal/` con frontmatter `version`/`effective_date` (UC 0002); account/utenti (UC 0013); login attivo (UC 0015/0017).

## 4. Flusso principale
1. **Accettazione iniziale**: al **signup** l'utente accetta i legali di piattaforma; all'**attivazione di un'app** accetta i componenti per-app (#13 G41, #14 C17).
2. **Log**: il core registra `(user_id, tenant_id, componente, versione, accepted_at, commit_hash)` — dato personale minimo, base **accountability** (#13 E), `WHERE tenant_id`.
3. **Derivazione al login/refresh**: il core confronta la **versione accettata** con la **versione corrente** (major dal frontmatter); se inferiore → flag "da ri-accettare" per i componenti vincolanti (#14 C20).
4. **Schermata bloccante** (shell, UC 0020): mostrata **solo** agli utenti vincolati, per i soli componenti major cambiati; l'utente accetta → nuovo log → accesso sbloccato.
5. **Minor**: nessun blocco, sola **notifica** (#14 C18); changelog consultabile.

## 5. Flussi alternativi / edge / errori
- **Nuova app pubblicata**: **non** forza re-accept a chi non la usa (scope per-componente) (#14 C17, #13 G41).
- **Major su componente di un'app non attiva**: nessun blocco finché l'utente non attiva/usa quell'app.
- **Diritti GDPR esenti**: export/erasure/recesso restano accessibili anche con accettazione pendente (#09 F31).
- **Più componenti major insieme** → un'unica schermata con elenco; accettazione atomica.

## 6. Schermate & stati
Schermata bloccante post-login (modale full-screen): elenco documenti aggiornati + link al testo (rendering in-app dei md, UC 0002)
+ checkbox/accetta. Stati: loading (derivazione), error (problem+json), success (sblocco). Copy a chiave i18n (EN+IT).

## 7. Dati toccati
Schema `platform`: **log accettazione** (`user_id`/`tenant_id`/componente/versione/`accepted_at`/commit). `@PersonalData` minimo;
finalità **accountability/contratto**, retention coerente #13 E. Manifest: estende il trattamento "gestione consensi/accettazioni" (UC 0002 §7).

## 8. Permessi & gate
- **Invarianti**: log tenant/utente-scoped (`WHERE tenant_id`); stato derivato da `subscription`/membership dal JWT, mai da request.
- **Gate**: schermata bloccante = gate UX prima dell'app; i **diritti GDPR** restano esenti dal blocco (#09 F31).

## 9. Requisiti di test
- **Integration** (Testcontainers): "accettata < major → bloccante"; minor → solo notifica; nuova app non forza re-accept a chi non la usa; log scritto/idempotente.
- **Security/multi-tenancy** (#10 D): A non vede/accetta per conto di B; anti-override `tenant_id`.
- **E2E** (Playwright): login con major pendente → schermata bloccante → accetta → accesso.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #14 C18/C20/C17, #13 G41/E, #09 F31, #02 2.
- **DoD**:
  1. Log accettazione (componente+versione+commit) lato core, tenant/utente-scoped.
  2. Derivazione al login/refresh (major → da ri-accettare; minor → notifica).
  3. Schermata bloccante nella shell, scope per-componente; nuova app non forza re-accept.
  4. Diritti GDPR esenti; suite integration+security+E2E verde.
