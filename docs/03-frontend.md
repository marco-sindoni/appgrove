# Frontend — Decisioni

**Stato**: 🟢 deciso (caching/fallback → #06/#07; pricing admin → #09; compliance → backlog)
**Ultimo aggiornamento**: 2026-06-14

## Scope
Stack e organizzazione delle SPA React (shell + moduli app + admin): build tool,
routing, state, data fetching, design system/UX, App Registry, integrazione auth, config runtime, build/deploy.
Copre **due** app frontend React: il **backoffice cliente** e la **console admin separata**
(`platform-admin`). Non copre la meccanica auth lato server (→ [02-auth-sicurezza](02-auth-sicurezza.md)) né
hosting/CDN provisioning (→ [06-infra-iac](06-infra-iac.md)).

> **Nota (aggiornamento)**: la **vetrina pubblica** è stata **estratta** dalla SPA React in un **progetto Astro separato**
> (SSG, i18n subpath) — decisa in [14-sito-vetrina-legale](14-sito-vetrina-legale.md) e implementata in
> [docs/usecases/09-marketing-site/0036](usecases/09-marketing-site/0036-vetrina-astro-scheletro.md). I riferimenti qui sotto
> alla "vetrina pubblica all'apex" vanno letti come **rimando a #14/UC 0036**, non più come parte della SPA React.

## Vincoli ereditati (già decisi)
- **Backoffice cliente = una SPA React modular monolith**; microfrontend rimandati; moduli app come **componenti React lazy**.
- **App Registry ibrido**: mappa statica dei moduli ∩ entitlement dal core; sidebar = intersezione.
- **Contratto shell↔app React-native**: la shell passa contesto via **React Context** (token getter, `tenant_id`,
  `user_id`, ruoli, theme, nav API); il modulo non gestisce auth né legge `tenant_id` fuori dal contesto.
- **Login custom** → auth Lambda: `POST /api/auth/login`; access/id token **in memoria**, refresh via **cookie HttpOnly**;
  al reload `POST /api/auth/refresh`.
- **Runtime `config.json`** per ambiente (un solo build). Build → **S3/CloudFront**. Vetrina pubblica all'apex `appgrove.app` → **progetto Astro separato** (#14/UC 0036), non parte della SPA React.
- **Form & validation**: **React Hook Form + Zod**; gli schemi Zod rispecchiano le regole Bean Validation del backend (validazione coerente client/server) → implementato in [docs/usecases/06-frontend/0020](usecases/06-frontend/0020-shell-spa-backoffice.md).
- API: base `/api/<app_id>/v1/`, errori **problem+json**, `Authorization: Bearer`. Swagger admin via backoffice (→ #04 §9).

## Da discutere (richiesto dall'utente, 2026-06-14)
- **Organizzazione della UX**: struttura del backoffice (navigazione/sidebar dinamica, layout area centrale,
  onboarding, gestione account/profilo, vetrina pubblica), design system, responsività, stati di caricamento/errore,
  pattern condivisi tra le app montate nella shell.
- **Configurazione admin in generale**: cosa amministra il `platform-admin` dal backoffice (account/tenant,
  utenti, catalogo app, entitlements, stato pagamenti, attiva/disattiva servizi per tenant).
- **Config admin del modello di costo per-app**: UI per definire/editare il pricing di ogni app → dettaglio in [09-pagamenti](09-pagamenti.md).

## Design di riferimento (v1)
Prototipo navigabile in [frontend-design/v1/](frontend-design/v1/) (`Appgrove.dc.html`, Design Component;
README descrittivo). Versioni successive in `v2/`, ecc. per tracciare i delta. Estratti chiave:

- **Estetica**: ispirazione **n8n** — neutri caldi, raggi generosi, ombre soffici. **CSS custom properties**
  per tema/accent (cambio istantaneo). NON usa una component library: componenti **custom** token-driven.
- **Tema**: light (`--bg #f4f4f1`, `--surface #fff`, `--text #262420`) + dark (`#161512`/`#211f1b`/`#f1efe9`).
  **Accent configurabile**: coral `#ec5a72` (default), violet `#7b6ef0`, teal `#16b6a4`, blue `#4f86e0`.
- **Font**: **Plus Jakarta Sans** (UI) + **JetBrains Mono** (numeri/importi/ID). Icone **Material Symbols Rounded**.
- **IA (12 schermate)**: Dashboard, App catalog, App detail/acquisto, Billing & subscriptions, Plans & pricing,
  Manage app, Cancel/Opt-out, app Invoices/Calendar/Mindmaps, Settings, Onboarding (wizard 4 step).
- **Chrome permanente**: sidebar 266px (PLATFORM + **YOUR APPS** menu dinamico) + topbar (breadcrumb, accent,
  lingua EN, toggle tema, notifiche).

### Allineamento con l'architettura (conferme)
- **Menu dinamico** "1 app = N sezioni in sidebar" = il nostro **App Registry** (moduli ∩ entitlement). ✓
- Badge **B2C/B2B** per app nel catalogo = la **capability per-app single/multi-user** (#01). ✓
- **Billing per-app** con switch attiva/sospendi indipendente = **entitlements** (#05) + Paddle (#09). ✓
- **Plans**: B2C per *funzionalità*, B2B *per numero utenti* (prezzo per utente) = modello di costo per-app (#09). ✓
- **Opt-out**: nessun taglio a metà periodo, dati tenuti 30 giorni = lifecycle/soft-delete (#05) + GDPR (backlog). ✓

### Implicazioni per le scelte di #03 (da valutare, non ancora decise)
- Il design è un **design system custom** (token CSS, no lib): orienta la scelta **E** verso Tailwind +
  componenti headless/own (es. shadcn/Radix) o una lib leggermente temata, più che verso MUI/Ant (look marcato).
- Font/icone/token sono già definiti: riusabili 1:1 come base del design system.

## Design admin di riferimento (admin/v1)
Console di amministrazione in [frontend-design/admin/v1/](frontend-design/admin/v1/) (`AppgroveAdmin.dc.html` + README).
**Stesso design system** del backoffice (token/accent/font identici). Area **operatore distinta** (badge `PLATFORM ADMIN`,
logo a scudo) con cross-link *"Open backoffice"* dal dettaglio account. Stati loading/empty/error, responsive (drawer).

- **Navigazione raggruppata**: **PLATFORM** (Overview, Accounts, Account detail, Users, Entitlements),
  **CATALOG** (App catalog, Pricing models), **REVENUE** (Billing & payments, Invitations),
  **GOVERNANCE** (Compliance/GDPR, System), + Settings.
- **Conferme di modello**: matrice **tenant × app** per entitlements; app con `model` **single-user (B2C) / multi-user (B2B)**;
  pricing editor **B2C a funzionalità / B2B a utenze** (prezzo per utente); billing Paddle con **dunning/past-due** e ID Paddle;
  **1 utente → 1 tenant**; **System** con stato servizi + link **Swagger** (coerente con #04 §9); **audit log** azioni admin.

### Implicazioni
- Pre-risolve gran parte del topic **I**: il design adotta un'**area `/admin` dedicata** (console separata) — input forte
  per la decisione ancora aperta, da confermare con l'utente.
- Alimenta **#09** (catalogo, modelli di prezzo B2C/B2B, billing/dunning) e il **backlog** (compliance/GDPR, audit log, config admin).

## Topic dell'area (agenda)
- **A. Build tool & framework** — Vite + React + TypeScript (SPA, no SSR).
- **B. Routing** — libreria e struttura (vetrina / auth / shell+app / admin).
- **C. State management** — server state vs client state.
- **D. API client & data fetching** — wrapper, iniezione token, refresh su 401, problem+json, client da OpenAPI?
- **E. Design system / UI library** — libreria componenti + theming.
- **F. App Registry** — implementazione mappa moduli + entitlement + contratto/tipi del modulo.
- **G. Integrazione auth** — login screens, token in memoria, refresh su reload/401, route guard (auth/ruolo/entitlement).
- **H. Organizzazione UX** — layout backoffice, vetrina, onboarding/signup, account/profilo, stati loading/error, responsività, i18n.
- **I. Pannello admin** — struttura e gating platform-admin, cosa gestisce.
- **J. Form & validation** — libreria form + validazione (allineata a Bean Validation backend).
- **K. Build/deploy** — output, fallback SPA su S3/CloudFront, caching (config.json non cache).

## Decisioni prese

### Stack fondativo (topic A, B, C, E)
1. **Vite + React + TypeScript**, SPA (no SSR). Output statico → S3/CloudFront.
2. **Routing: React Router** (lazy routes + nested routing per shell + moduli app lazy).
3. **State: TanStack Query** (stato server: cache/refetch/loading-error) + **Zustand** (stato client: auth, theme/accent, UI).
4. **Design system: Tailwind CSS + shadcn/ui (Radix headless)**. I **token del design v1** (tema light/dark, accent
   coral/violet/teal/blue, raggi, ombre) mappati nel theme Tailwind via **CSS custom properties**; font **Plus Jakarta
   Sans** + **JetBrains Mono**; icone **Material Symbols Rounded**. Componenti propri (no lib a look marcato).

### API client & data fetching (topic D)
5. **Client generato da OpenAPI** (es. orval/openapi-ts) + **TanStack Query**: tipi/hook generati dallo spec dei
   servizi, rigenerati al cambio API. Il **layer fetch** inietta il Bearer access token, gestisce **401 →
   `/api/auth/refresh` → retry**, e mappa **problem+json** in errori tipizzati.

### Contratto dei moduli app (topic F)
6. **Manifest per-app co-locato**: ogni modulo esporta `{ id, sezioni sidebar, route interne, metadata }` + il
   **componente lazy**; il **registry** aggrega i manifest **∩ entitlement** dal core (sidebar = intersezione).

### Form & validation (topic J)
7. **React Hook Form + Zod**; schemi Zod allineati alle regole **Bean Validation** del backend (possibile condivisione tipi).

### Integrazione auth (topic G — discende da #02 + stack)
8. **Auth store in Zustand** (access/id in memoria). Al load la SPA chiama **`/api/auth/refresh`** (cookie) per
   ripristinare la sessione; **interceptor 401 → refresh → retry**; logout → **`/api/auth/logout`**. **Route guard**:
   `requireAuth`, `requireRole('platform-admin')` per l'admin, `requireEntitlement(app_id)` per i moduli app
   (difesa in profondità lato UX, oltre al gate del Lambda authorizer).

### Organizzazione UX (topic H)
9. **Scope PoC = core loop del marketplace**: shell + menu dinamico, login/onboarding, catalog, app detail,
   billing/manage/opt-out, settings, + le **2 app demo** (una B2C, una B2B). UI billing presente; integrazione **Paddle reale → #09**.
10. **Multilingua EN+IT dal PoC**: libreria i18n (es. react-i18next), stringhe a chiave; design EN + traduzione IT.
11. **Light/dark + accent picker** come da design v1 (token CSS custom properties).
12. **Tutto responsive dal PoC**, backoffice incluso: sidebar collassabile/drawer su mobile, tabelle responsive,
    canvas (calendar/mindmaps) touch-friendly. Vetrina pubblica responsive.

### Pannello admin (topic I)
13. **App admin separata** (non una sezione del backoffice): build/bundle/deploy distinti, su sottodominio
    **`admin.appgrove.app`** (e `admin.test…`, `admin.local…`). I tenant **non scaricano mai** il codice admin
    (minor superficie d'attacco + bundle cliente più leggero). _Revisiona il "una sola SPA" iniziale._
14. **Design system condiviso** tra le due app via **package nel monorepo** (token, componenti, i18n, API/auth client),
    così l'estetica resta identica senza duplicazione. Struttura proposta: workspace `frontend/` con `apps/backoffice`,
    `apps/admin`, `packages/design-system` (npm workspaces).
15. **Nessuna impersonation**: l'admin **non entra mai** nei backoffice dei tenant. Il cross-link "Open backoffice"
    del prototipo **non si implementa**. L'operatore amministra solo dai dati della console admin.
16. L'admin condivide **Cognito + auth Lambda** (stesso `/api/auth`, cookie host-only su `api.appgrove.app` valido
    cross-sottodominio same-site) ma è accessibile **solo a `platform-admin`**.

### Build/deploy (topic K)
17. **Due SPA statiche** su S3/CloudFront, **una distribuzione ciascuna** (backoffice + admin). Dettagli di
    caching (asset hashati vs index.html/config.json no-cache), fallback SPA e invalidation → [06-infra-iac](06-infra-iac.md)/[07-devops-cicd](07-devops-cicd.md).

### Scope admin nel PoC
18. **Essenziale**: Overview (KPI base), Accounts (lista + dettaglio con users e toggle entitlement), Users,
    matrice Entitlements, App catalog (vista + capability), Billing in sola lettura. **Rimandati**: Pricing
    models editor (→ [09-pagamenti](09-pagamenti.md)), Compliance/GDPR (→ backlog), System/Swagger.

## Questioni aperte
_Nessuna — #03 chiuso. Caching/fallback/invalidation → #06/#07; pricing admin → #09; compliance → backlog._

## Alternative valutate / scartate
_—_

## Impatti su altre aree
_—_
