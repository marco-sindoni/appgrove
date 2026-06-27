# Change 0014: Console Admin SPA + API admin cross-tenant (UC 0021)

**Branch**: `change/0014-use-case-0021-console-admin-spa`
**Aree**: frontend (nuova app `apps/admin`; +`packages/i18n`), services/core (nuovo `AdminResource`), dev (Caddyfile, `app-start.sh`)
**Data**: 2026-06-27
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/06-frontend/0021-console-admin-spa.md](../../docs/usecases/06-frontend/0021-console-admin-spa.md)
**Tocca dati personali?**: Sì — vista admin di email/nome utenti (cross-tenant), base "amministrazione piattaforma". Nessun nuovo trattamento di dati (lettura di dati già dichiarati in UC 0013); audit persistito **rimandato** (UC 0035).

## Problema / Obiettivo

Non esiste una **console di amministrazione di piattaforma**. UC 0021 la introduce come **SPA separata** (`admin.appgrove.app`),
accessibile **solo a `platform-admin`**, per amministrare la piattaforma **cross-tenant**: KPI, account/utenti di tutti i
tenant, matrice entitlement (derivata da `subscription`), billing read-only, e **disable-app** (gate 2). Oggi il core è
interamente **tenant-scoped**: questa change introduce — con cura — la prima **superficie API admin non-tenant-scoped**
(gated `platform-admin`, read-only salvo il toggle `app.status`) e la SPA che la consuma. Slice verticale MVP (approccio A).

## Scope

### Frontend — nuova SPA `frontend/apps/admin` (`@appgrove/admin`, porta dev 5174)
- Scaffold come `apps/backoffice` (Vite+React+TS, design-system/i18n/api-client condivisi, Tailwind, Vitest+jest-axe, Playwright).
- **Auth**: login proprio (auth-local, stesso contratto `/api/auth/*`) + guard `requireRole('platform-admin')` (UX) → `/forbidden`.
- **Shell admin**: chrome con badge `PLATFORM ADMIN` + logo a scudo, sidebar, stati loading/empty/error, responsive.
- **Schermate** (MVP completo):
  1. **Overview** — KPI base (conteggi: account, utenti, subscription attive, app disabilitate).
  2. **Accounts** — lista cross-tenant (nome, stato, #utenti, #sub attive) + **dettaglio read-only** (utenti del tenant +
     entitlement derivato per app). *Nessun toggle per-tenant* (vedi rinvio #16).
  3. **Users** — lista utenti cross-tenant (email, nome, ruolo, stato, tenant).
  4. **Entitlements** — matrice **tenant×app** derivata da `subscription` (attivo ↔ `status IN (active,trialing)` AND app `active`).
  5. **Billing** read-only — per tenant: app, tier, stato subscription, periodo (solo dati **locali**; drift Paddle rimandato #1).
  6. **Danger zone** — **disable-app** platform-wide: toggle `app.status` active↔inactive (unico write), con conferma.
- i18n EN/IT, a11y (axe) sulle schermate chiave.

### Backend — `services/core`: nuovo `AdminResource` (`/api/platform/v1/admin/*`, gated `@RolesAllowed(Roles.PLATFORM_ADMIN)`)
- **Read-only cross-tenant via query native** (bypassano deliberatamente il filtro `@TenantId`; nessuna entità JPA nuova):
  - `GET /admin/overview` → KPI.
  - `GET /admin/accounts` (+ `GET /admin/accounts/{id}` con utenti + entitlement derivato).
  - `GET /admin/users`.
  - `GET /admin/entitlements` (matrice derivata).
  - `GET /admin/billing`.
  - `GET /admin/apps` (catalogo: id, slug, name, status).
- **Unico write**: `PATCH /admin/apps/{id}` `{status}` → `UPDATE platform.app.status` (disable/enable). **Logging strutturato**
  dell'azione (actor `sub`, `app_id`, esito). Errori problem+json.

### Dev
- **Caddyfile**: attivare il blocco `admin.local.appgrove.app` (`import api_routes` + `reverse_proxy …:5174`).
- **`app-start.sh`**: avviare anche la SPA admin (:5174) + elencarla nel riepilogo.
- `public/config.json` admin same-origin `https://admin.local.appgrove.app`; Vite `host:true` + `allowedHosts`.

### Sicurezza (pattern nuovo, documentato)
- Endpoint admin **non-tenant-scoped by design**: accesso **solo** `platform-admin` (gating backend `@RolesAllowed` + UX guard).
  `tenant_id` **mai** da request; il `tenant_id` del claim admin è **ignorato** per questi read (vede tutti i tenant).
- **Solo letture** + l'unico write `app.status` (non tocca dati tenant). **Nessuna impersonation**.

## Fuori scope / Rinvii tracciati (registro canonico — da propagare ai file UC a step-04)

**Dominio backend** — 1) entità/lifecycle `subscription` + **drift Paddle reale** → **UC 0025** (ora: native read-only, billing mostra solo stato locale); 2) dominio catalogo `app/app_tier/app_price` → **UC 0022** (ora: native read + `UPDATE app.status`); 3) creazione/modifica subscription (checkout) → **UC 0024**.
**Enforcement** — 4) **gate disable-app (403 runtime)** → **UC 0014** (+ **UC 0027**) (ora: solo toggle + esclusione dalla matrice admin); 5) sostituzione `StubEntitlementsProvider` del backoffice con entitlement reale → **_BACKLOG (rinvio UC 0020)** (può riusare questa derivazione).
**Auth cloud** — 6) `platform-admin` via Cognito/pre-token-gen → **UC 0016/0015**; 7) **SSO cross-sottodominio** (cookie su `api.appgrove.app`) → **UC 0015** (ora: admin.local è origin separato → login proprio per-host).
**Infra/deploy** — 8) hosting prod SPA admin (CloudFront+S3/OAC+Route53) → **UC 0055** (già pianificato) **+ ACM/zona UC 0003**; 9) pipeline FE pubblica il bundle admin → **UC 0005**.
**Audit/observability** — 10) **audit persistito** delle azioni admin + archivio 12 mesi → **UC 0035** (+ #08) (ora: solo logging strutturato); 11) KPI ricchi (MRR/churn) → #08/UC 0025.
**Frontend/dev** — 12) estrazione base `Table` + stati compositi → **UC 0019**; 13) integrazione SPA admin nel comando canonico `dev`/`dev service` → **UC 0009/0046** (ora: solo `app-start.sh` + Caddy); 18) estrazione runtime auth/sessione condiviso backoffice+admin (oggi **duplicato** il sottoinsieme minimo in `apps/admin`) → **_BACKLOG (rinvio UC 0020)**.
**Sicurezza** — 14) formalizzazione del pattern "endpoint admin non-tenant-scoped" (come/dove disabilitare il filtro tenant in sicurezza, test anti-leak sistematici) → **UC 0013 / doc #02**.
**Per-tenant (dalla scelta i)** — 16) **toggle/override entitlement per-tenant** (dettaglio Account, oggi read-only) → **UC 0014/0027** (modello) **+ UC 0013** (schema); 17) azione admin sulla **subscription** (sospendi/cancella per tenant) → **UC 0024/0025**.
**Compliance** — 15) console "Diritti GDPR" → **UC 0034** (già escluso da UC 0021; nessuna azione).

## Criteri di accettazione

- [ ] Nuova SPA `apps/admin` su :5174, servita via Caddy `admin.local.appgrove.app`; login `platform-admin` → console; non-`platform-admin` → `/forbidden`.
- [ ] `AdminResource` gated `platform-admin`: liste cross-tenant (accounts/users/entitlements/billing/overview/apps) corrette sui dati seed; un non-admin riceve **403**.
- [ ] **disable-app**: `PATCH /admin/apps/{id}` toggla `app.status`; la **matrice entitlement** esclude le app `inactive`; azione **loggata** (actor/app_id).
- [ ] Schermate Overview/Accounts(+dettaglio read-only)/Users/Entitlements/Billing/DangerZone con stati loading/empty/error; EN/IT; a11y axe verde.
- [ ] Test verdi: core (`mvn test`, incl. security 403 + cross-tenant + disable-app) e frontend (`npm test` + E2E + typecheck).
- [ ] Tutti i rinvii (1–18) registrati nei file UC proprietari / _BACKLOG.

## Invarianti appgrove toccati

- **tenant_id solo dal JWT** — mantenuto: gli endpoint admin **non** leggono `tenant_id` da request; i read sono volutamente **cross-tenant** e gated `platform-admin` (eccezione **esplicita e documentata** all'invariante #2, non una violazione accidentale). Le SPA non inviano mai `tenant_id`.
- **Filtro row-level** — gli endpoint admin sono **non-tenant-scoped by design** (query native, solo `platform-admin`); tutti gli endpoint **tenant** esistenti restano invariati e filtrati. Test anti-leak: un non-admin non raggiunge l'admin (403).
- **Modulo `microsaas_app`** — N/A (nessuna infra in questa change; hosting prod → UC 0055).
- **Logging strutturato** — l'azione disable-app emette log con `app_id`, actor `user_id` (`sub`), esito.

## Requisiti di test

- **Core** (Testcontainers): `platform-admin` lista cross-tenant (accounts/users/entitlements/billing/overview) coerenti col seed; **403** per ruolo non-`platform-admin`; `PATCH /admin/apps/{id}` toggla `status` e la matrice esclude le app `inactive`; nessun `tenant_id` da request.
- **Frontend** (Vitest+RTL+MSW): rendering schermate + stati; gating `requireRole('platform-admin')` → `/forbidden`; disable-app (PATCH) riflesso in matrice; a11y axe.
- **E2E** (Playwright): login `platform-admin` → Overview → disabilita un'app in Danger zone → riflesso nella matrice (backend mockato).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (nuova superficie additiva) |
| Contratto cross-area | Sì — nuova SPA admin → core `/api/platform/v1/admin/*` (nuovi endpoint) e → auth-local `/api/auth/*` (esistenti) |
| Version bump | minor (nuova app + nuovi endpoint) |
