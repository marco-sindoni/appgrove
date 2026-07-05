# UC 0020 — Backoffice SPA shell (sidebar, app registry, routing, auth store, API client, i18n, theme)

**Area**: 06-frontend · **Fase**: 2 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0019](0019-design-system-brand-kit.md) (design system), UC [0013](../04-platform-core/0013-account-utenti-inviti-api.md) (API core)
**Fonte decisioni**: #03 (frontend), #01 (App Registry/contratto shell↔app)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [03-frontend](../../03-frontend.md), [01-architettura](../../01-architettura.md), [02-auth-sicurezza](../../02-auth-sicurezza.md)

## 1. Obiettivo / Scope
Realizzare la **shell del backoffice cliente** (SPA React) che ospita i moduli app: chrome permanente, App Registry,
routing, auth store, API client, i18n, theming.
**Incluso**: **Vite+React+TS**; **React Router** (lazy + nested); **TanStack Query** (server state) + **Zustand** (auth/theme/UI);
**design system** (UC 0019); **App Registry ibrido** (manifest moduli ∩ entitlement → sidebar = intersezione); **API client**
da OpenAPI (inietta Bearer, **401→refresh→retry**, problem+json; il client TS rigenerato + `tsc` **rompe la build** sul drift, #10 G25); **route guard** (requireAuth/requireRole/requireEntitlement);
**form & validation** (**React Hook Form + Zod**, schemi allineati alle regole Bean Validation del backend, #03 dec.7); **i18n EN+IT**; light/dark + accent; responsive.
**Escluso**: i moduli app concreti (UC 0052/0054); l'admin SPA (UC 0021); i flussi auth UI (UC 0017, che vivono nella shell).

## 2. Attori & ruoli
- **Utente** (owner/admin/member): usa il backoffice.
- **Moduli app**: componenti React lazy montati dalla shell via contesto (#01 11).

## 3. Precondizioni
- Design system (UC 0019); API core (UC 0013) + auth (`/api/auth/*`, UC 0015) raggiungibili; OpenAPI per generare il client (#03 5).

## 4. Flusso principale
1. **Chrome permanente**: sidebar (PLATFORM + **YOUR APPS** dinamico) + topbar (breadcrumb, accent, lingua, tema, notifiche) (#03 IA).
2. **Auth**: al load `POST /api/auth/refresh` (cookie) ripristina la sessione; access/id in **Zustand** (memoria); **interceptor 401→refresh→retry**; logout (#03 8).
3. **App Registry**: manifest co-locati dei moduli (`{id, sezioni, route, metadata}` + componente lazy) ∩ **entitlement** dal core → sidebar = intersezione (#03 6, #01 10).
4. **Routing**: vetrina/auth/shell+app nested; moduli **lazy**; **route guard** `requireAuth`/`requireRole('platform-admin')`/`requireEntitlement(app_id)` (difesa in profondità UX, oltre al gate edge) (#03 8).
5. **Contratto shell↔modulo**: React Context con token getter, `tenant_id`, `user_id`, ruoli, theme, nav API; il modulo **non** gestisce auth né legge `tenant_id` fuori dal contesto (#01 11).
   - **Form**: i form della shell (login/onboarding/settings/inviti) usano **React Hook Form + Zod**; gli schemi Zod rispecchiano le regole Bean Validation del backend (validazione coerente client/server, #03 dec.7); i moduli app riusano lo stesso pattern.
6. **Config runtime** `config.json` (un solo build promosso per env) (#03/#12).

## 5. Flussi alternativi / edge / errori
- **Entitlement assente** per un'app: non compare in sidebar; accesso diretto a route → guard blocca (UX) + 402 dal backend (enforcement vero).
- **problem+json** → errori tipizzati con stati loading/empty/error (TanStack Query) (#03 5).
- **Mobile**: sidebar collassabile/drawer; tabelle responsive (#03 12).
- **i18n**: stringhe a chiave (react-i18next), EN+IT dal PoC (#03 10).

## 6. Schermate & stati
Shell + Dashboard base; ospita catalog/app detail/billing/manage/settings/onboarding (#03 9) e i flussi auth (UC 0017).
Stati loading/empty/error/success ovunque; light/dark + accent picker (UC 0019). Drill-down moduli app = UC 0052/0054.

## 7. Dati toccati
Nessuna persistenza client oltre lo stato in memoria; legge dati dal core via API (filtrati per tenant lato backend).
**Dati personali**: mostra profilo/account dell'utente (email/nome) ricevuti dall'API; nessun token persistente in storage
(refresh solo in cookie HttpOnly, #02 3). Manifest: nessun nuovo trattamento (UI di dati già dichiarati in UC 0013).

## 8. Permessi & gate
- **Invarianti**: il frontend è **solo UX** (#09 dec.30); non legge `tenant_id` se non dal contesto/token; il confine di
  enforcement è il backend. Le **route guard** sono difesa in profondità, non sostituiscono i gate edge/servizio.
- `requireRole('platform-admin')` riservato all'admin (che però è SPA separata, UC 0021).

## 9. Requisiti di test
- **Component** (Vitest+RTL+MSW): App Registry (sidebar = intersezione), route guard, interceptor 401→refresh, stati query (#10 E).
- **E2E** (Playwright, #10 F): login programmatico (storageState), navigazione shell, montaggio modulo entitled vs non-entitled.
- **a11y** (axe) sulle schermate chiave; aria-snapshot come rete primaria (#10 20/39).

## 10. Riferimenti & Definition of Done
- **Decisioni**: #03 1/2/3/4/5/6/7/8/9/10/11/12, #01 10/11, #10 G25.
- **DoD**:
  1. Shell con chrome, App Registry (∩ entitlement), routing lazy, route guard.
  2. Auth store + refresh-on-load + interceptor 401→refresh→retry; client da OpenAPI (drift → `tsc` rompe la build, #10 G25).
  3. i18n EN+IT, light/dark+accent, responsive; form via React Hook Form + Zod (schemi ↔ Bean Validation, #03 dec.7); design system condiviso (UC 0019).
  4. Component + E2E + a11y verdi; frontend = solo UX (enforcement nel backend).

## Punti aperti / decisioni differite

_Tracciati dalla change `0005-use-case-0019-…` (regola CLAUDE.md "Tracciamento delle decisioni differite")._

- **Table + stati composti (loading/empty/error/success)** — il design-system (UC 0019) fornisce solo i
  **primitivi**; questi pattern compositi vanno implementati **qui** (shell + tabelle del backoffice) sopra
  i primitivi. Valutare se estrarne una base condivisa da promuovere in UC 0019.
  - *Stato (change 0011):* implementato il componente base `QueryState` (loading/empty/error+retry) in
    `apps/backoffice/src/shell/QueryState.tsx`. **Resta aperto**: estrarne una base condivisa in UC 0019 e i pattern
    **tabella** veri (non ancora necessari: nessuna lista densa nella shell skeleton).
- **Package condivisi `api-client` / `i18n`** — ✅ **fatti (change 0011)**: creati `frontend/packages/api-client`
  (`openapi-typescript` + `openapi-fetch`, middleware Bearer/401→refresh→retry/problem+json) e `frontend/packages/i18n`
  (react-i18next + cataloghi EN/IT). **Resta aperto**: `packages/i18n` ospita per ora i cataloghi del **solo backoffice**;
  la separazione common/per-app (e l'eventuale catalogo admin) si decide col **secondo consumatore = console admin
  UC 0021**, che possiede questo rinvio.

_Aggiunti dalla change `0011-use-case-0020-…` (implementazione shell skeleton):_

- **Endpoint entitlement reale (core)** 🔑 — la sidebar usa un **provider stub** (`StubEntitlementsProvider`): il core non
  espone ancora gli entitlement del tenant. **Trasversale → tracciato in [_BACKLOG.md](../../_BACKLOG.md)** ("Backoffice
  shell — rinvii cross-area"). Sostituzione = solo quel provider (+ una query TanStack).
- **Modulo demo temporaneo** — `apps/backoffice/src/modules/demo/` esercita l'App Registry finché non arriva la **prima
  app reale** (UC 0052 B2C / 0054 B2B): rimuoverlo allora. *Owner:* 0052/0054.
- **Schermate auth reali** (login/signup/verify/reset/invite/2FA) — la shell fornisce solo la *plumbing* (auth store,
  refresh-on-load, interceptor, logout) e una **route `/login` segnaposto**; le UI sono di **UC 0017**.
- **Schermate funzionali** (catalog, app detail, billing/manage, onboarding, settings completo) e **notifiche topbar** —
  oggi chrome/route/segnaposto; il contenuto reale è di UC dedicati (checkout 0024, portale 0028, GDPR self-service 0033,
  moduli app). La topbar ha un'icona notifiche **non funzionale** (nessun backend notifiche).
- **Guard `requireRole('platform-admin')`** — implementata e testata, ma la **console admin è app separata (UC 0021)**:
  qui non esiste route admin.
- **Condivisione schemi Zod ↔ Bean Validation** — gli schemi Zod dei form **rispecchiano a mano** le regole Bean
  Validation del backend (#03 dec.7 "possibile condivisione tipi"): come **generarli/condividerli** automaticamente resta
  aperto.
- **Wiring del proxy dev (Caddy) per servire la SPA** — `dev/Caddyfile` ha i `reverse_proxy` verso la SPA (`:5173`)
  **commentati**; attivarli è di competenza dell'orchestrazione dev (**UC 0009**). La change 0011 non ha toccato `dev/`.
- **Config Cognito cloud in `config.json`** — i campi `cognito.userPoolId/clientId` sono **placeholder** (in locale si usa
  auth-local); valorizzarli è degli UC auth cloud (☁0015/0016) + config per-env (#12).
- **Code-splitting bundle** — il chunk principale supera 500 kB (warning Vite); valutare split di vendor/route quando il
  peso conta. Non bloccante per la shell skeleton.
- **Componenti della shell del mockup rimandati (change 0034 — fedeltà al design)** — la change 0034 ha
  allineato shell/sidebar/topbar al mockup `docs/frontend-design/v1/`, ma ha lasciato fuori (per scelta di
  scope: richiedono dati/comportamenti nuovi) questi elementi che appartengono a **questo** UC:
  ricerca globale nella sidebar ("Search apps & sections", scorciatoia ⌘K); selettore workspace nel blocco
  brand (`unfold_more`); badge numerici sulle voci di menu e conteggi sulle sezioni app; stato **PAUSED**
  delle app in sidebar (dipende dal lifecycle subscription). Nota: la change 0034 ha anche aggiornato la
  nota sopra sui pattern tabella — il primitivo `Table` ora **esiste** nel design system (UC 0019) ed è usato
  dalle liste di backoffice e admin.
