# UC 0021 — Admin console SPA (accounts, users, matrice entitlement, billing, danger zone, disable-app)

**Area**: 06-frontend · **Fase**: 6 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0019](0019-design-system-brand-kit.md) (design system), UC [0013](../04-platform-core/0013-account-utenti-inviti-api.md) (API core)
**Fonte decisioni**: #03 I (admin), #01 (entitlement derivato), #09 H (pricing read-only), #09 F30 (disable-app)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [03-frontend](../../03-frontend.md), [09-pagamenti](../../09-pagamenti.md), [13-compliance-privacy](../../13-compliance-privacy.md)

## 1. Obiettivo / Scope
Realizzare la **console admin** come **SPA separata** (`admin.appgrove.app`), accessibile solo a `platform-admin`.
**Incluso**: app React separata (build/bundle/deploy distinti) sul sottodominio admin; **design system condiviso** (UC 0019);
**Cognito + auth Lambda condivisi** (gated platform-admin); scope MVP: **Overview** (KPI base), **Accounts** (lista+dettaglio
utenti, **toggle entitlement**/**disable-app**), **Users**, **matrice Entitlements** (tenant×app, derivata), **Billing read-only**
(status/tier/drift vs Paddle), **danger zone** (disable-app). **Nessuna impersonation**.
**Escluso**: console Diritti GDPR (UC 0034, vive nell'admin), pricing editor (read-only, #09 H34), Swagger/System (rimandato #03 18), il backoffice cliente (UC 0020).

## 2. Attori & ruoli
- **platform-admin**: unico attore; amministra dai dati della console.
- **Sistema**: deriva la matrice entitlement da `subscription`; applica disable-app.

## 3. Precondizioni
- Design system (UC 0019); API core (UC 0013) + auth (UC 0015); ruolo `platform-admin` nei claim (UC 0016).

## 4. Flusso principale
1. **SPA separata** su `admin.appgrove.app` (build/deploy distinti; i tenant non scaricano il codice admin) (#03 13).
2. **Auth**: condivide Cognito + auth Lambda (cookie host-only su `api.appgrove.app` valido cross-sottodominio same-site), accessibile **solo a platform-admin** (#03 16).
3. **Overview/Accounts/Users**: KPI base, lista+dettaglio account con users e **toggle entitlement**; matrice **Entitlements** (tenant×app, **derivata** da subscription, #09 dec.12).
4. **Billing read-only**: status/tier/fine periodo/drift vs Paddle (no editor prezzi, pricing-as-code #09 H34).
5. **Danger zone / disable-app**: il platform-admin può **disabilitare** un'app per un tenant → gate 2 della catena (403, precede entitlement, reversibile, non tocca dati) (#09 F30 gate 2).

## 5. Flussi alternativi / edge / errori
- **Disable-app**: rende l'app indisponibile a tutti i tenant **senza** toccare dati/subscription (reversibile) (#09 F30 gate 2).
- **Nessuna impersonation**: l'admin non entra mai nei backoffice dei tenant; niente "Open backoffice" (#03 15).
- **Matrice derivata**: l'entitlement non è una tabella; si calcola da `subscription` (#09 dec.12).
- **Pricing**: solo lettura/observability del drift (#09 H34).

## 6. Schermate & stati
Overview (KPI), Accounts (lista+dettaglio+toggle), Users, matrice Entitlements (tenant×app), Billing read-only, danger zone.
Badge `PLATFORM ADMIN`, logo a scudo; stati loading/empty/error; responsive (drawer). Audit log azioni admin (#03).

## 7. Dati toccati
Legge account/users/subscription (derivazione entitlement) + drift Paddle. **Dati personali**: email/nome utenti (vista admin) —
base contratto/legittimo interesse (gestione piattaforma); **audit** delle azioni admin (#08, archivio 12 mesi). Manifest: trattamento "amministrazione piattaforma".

## 8. Permessi & gate
- **Invarianti**: accesso **solo platform-admin** (`requireRole`); query con scoping; **nessuna impersonation**; azioni admin **auditate** (#08).
- **disable-app** = gate 2 della catena (#09 F30). Entitlement derivato (no tabella).

## 9. Requisiti di test
- **E2E admin** (#10 F): solo platform-admin accede; toggle entitlement/disable-app; matrice corretta; billing read-only.
- **Security**: i tenant non accedono all'admin; nessuna impersonation; azioni auditate.
- **a11y** sulle schermate chiave.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #03 13/14/15/16/17/18, #09 dec.12/F30/H34, #08 (audit).
- **DoD**:
  1. SPA admin separata su `admin.appgrove.app`, gated platform-admin, design system condiviso.
  2. Overview/Accounts/Users/matrice Entitlements (derivata)/Billing read-only/danger zone (disable-app).
  3. Nessuna impersonation; azioni admin auditate; pricing read-only.
  4. E2E + security + a11y verdi.
