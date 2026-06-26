# Change 0011: Backoffice SPA shell (chrome, App Registry, routing, auth, API client, i18n, theme)

**Branch**: `change/0011-use-case-0020-shell-backoffice`
**Aree**: frontend (greenfield `apps/backoffice` + nuovi package `packages/api-client`, `packages/i18n`)
**Data**: 2026-06-26
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/06-frontend/0020-shell-spa-backoffice.md](../../docs/usecases/06-frontend/0020-shell-spa-backoffice.md)
**Tocca dati personali?**: No (nessun **nuovo** trattamento). La shell mostra profilo/account (email/nome)
ricevuti dall'API — dati già dichiarati in UC 0013 — e **non persiste alcun token** (refresh solo in cookie
HttpOnly lato auth-local, #02 3). Nessuna nuova voce RoPA; nessun bump PP/ToS.

## Problema / Obiettivo

Realizzare la **shell del backoffice cliente** (SPA React) che ospita i moduli app: la fondazione di tutto il
frontend cliente. È prerequisito di UC 0017 (flussi auth UI), 0021 (admin), 0052/0054 (moduli app). La shell
fornisce chrome permanente, App Registry, routing, auth store, API client, i18n e theming, **cablata su ciò che
esiste oggi nel repo** (core UC 0013 + auth-local UC 0010), con un **modulo demo interno** per esercitare il
registry e un **provider entitlement stub** (il core non espone ancora un endpoint entitlement).

## Scope

Area unica: **frontend**. Tutto sotto `frontend/`, con `npm workspaces`.

**Nuovi package condivisi** (decisione: crearli subito — non rimandare l'estrazione):
- `packages/api-client` — **`openapi-typescript`** genera i **tipi** dallo spec del core
  (`services/core/.../openapi.yaml`); **`openapi-fetch`** come client type-safe. Fetch layer (middleware):
  iniezione `Authorization: Bearer`, **interceptor 401 → `/api/auth/refresh` → retry**, mapping
  **`problem+json` (RFC 9457) → errori tipizzati**. Consumer-agnostic: base URL, token getter e refresh fn
  **iniettati** dall'app. Script `gen` rigenera i tipi; il drift dello spec rompe `tsc`/build (#10 G25).
- `packages/i18n` — setup **react-i18next** (factory init, tipi, hook), cataloghi **EN+IT** (per ora del solo
  backoffice — l'admin secondo consumatore arriva con UC 0021).

**Nuova SPA `apps/backoffice`** (Vite + React + TS):
- **Chrome permanente**: sidebar (`PLATFORM` statica + `YOUR APPS` dinamica = App Registry ∩ entitlement) +
  topbar (breadcrumb, accent picker, lingua EN/IT, tema light/dark, notifiche [placeholder], menu utente/logout).
  Responsive: sidebar collassabile/drawer su mobile.
- **Auth store (Zustand)**: access/id token **in memoria**; al load `POST /api/auth/refresh` (cookie) ripristina
  la sessione; logout → `POST /api/auth/logout`. L'interceptor 401→refresh→retry vive nell'`api-client`.
- **App Registry**: manifest co-locati dei moduli (`{ id, sections, routes, metadata }` + componente **lazy**)
  aggregati ∩ entitlement → sidebar = intersezione. Esercitato da un **modulo demo interno**
  (`src/modules/demo/`), non da un'app reale (0052/0054 fuori scope).
- **Entitlement**: dietro un **provider astratto** con **implementazione stub** (config/fixture). L'endpoint
  entitlement reale del core **non esiste** → tracciato come rinvio (vedi sotto).
- **Routing** (React Router): aree vetrina/auth/shell+app **nested**, moduli **lazy**. **Route guard**
  (difesa in profondità UX, non sostituiscono l'enforcement backend): `requireAuth`,
  `requireRole(...)` (es. `platform-admin`), `requireEntitlement(app_id)`.
- **Contratto shell↔modulo**: React Context che espone token getter, `tenant_id`, `user_id`, ruoli, theme e
  nav API. Il modulo **non** gestisce auth e **non** legge `tenant_id` fuori dal contesto.
- **Form**: pattern **React Hook Form + Zod**; schemi Zod che rispecchiano le regole Bean Validation del backend
  (mirroring manuale per ora). Almeno un form della shell (es. settings/profilo) per fissare il pattern.
- **Config runtime**: `public/config.json` caricato all'avvio (API base URL auth/core, env name); un solo build
  promosso per env (#03/#12). `config.json` no-cache.
- **Theming**: via `@appgrove/design-system` (`ThemeProvider`, token CSS); light/dark + accent picker.
- **i18n**: tutte le stringhe a chiave, EN+IT dal PoC.

**Test** (DoD §9): **component** (Vitest + RTL + MSW: App Registry = intersezione, route guard, interceptor
401→refresh, stati query), **E2E** (Playwright: login programmatico via `storageState`, navigazione shell,
montaggio modulo entitled vs non-entitled), **a11y** (axe + aria-snapshot come rete primaria) sulle schermate chiave.

## Fuori scope (e rinvii tracciati — nulla va perso)

Da tracciare nei "Punti aperti / decisioni differite" dell'UC di competenza (o `_BACKLOG.md` se trasversale)
**prima della chiusura** (step-04), come richiesto dalla costituzione CLAUDE.md:

1. **Endpoint entitlement reale nel core** — la sidebar usa un provider **stub**; il vero endpoint che serve gli
   entitlement (derivati da `platform.subscription`, #01 dec.10/#09 dec.12) non esiste. → traccia in UC 0020 +
   in `_BACKLOG.md` (contratto frontend↔core, owner: area core/un nuovo UC).
2. **Moduli app reali** (UC 0052 B2C, 0054 B2B) — qui solo modulo **demo** interno temporaneo. → già di proprietà
   0052/0054; nota in UC 0020 che il demo è da rimuovere quando arriva la prima app reale.
3. **Schermate auth reali** (login/signup/verify/reset/invite/2FA) — di **UC 0017**. La shell fornisce solo la
   *plumbing* auth + route segnaposto. → nota in UC 0020.
4. **Console admin** (UC 0021) — qui solo la guard `requireRole('platform-admin')` generica; l'app admin è separata.
5. **Estrazione cataloghi i18n condivisi** — `packages/i18n` ora ospita config + cataloghi del solo backoffice;
   la separazione common/per-app si decide col secondo consumatore (admin). → rinvio di proprietà UC 0021.
6. **Tabelle + stati compositi (loading/empty/error/success)** — implementati **qui** sopra i primitivi del
   design-system; valutare estrazione di una base condivisa in UC 0019. → aggiorna la nota esistente in UC 0020.
7. **Condivisione schemi Zod ↔ Bean Validation** — per ora mirroring manuale; come generare/condividere i tipi
   è aperto (#03 dec.7 "possibile condivisione tipi"). → traccia in UC 0020.
8. **Schermate funzionali** (catalog, app detail, billing/manage, onboarding, settings completo) e **notifiche** —
   la shell fornisce chrome/route/placeholder; il contenuto reale è di UC dedicati (0024/0028/0033, app UCs). → nota.
9. **Config Cognito cloud** in `config.json` — in locale si usa auth-local; i campi pool/client id cloud sono
   placeholder, di proprietà degli UC auth cloud (☁0015/0016). → nota.
10. **E2E visual baseline (#10 F)** — non ri-registrare baseline alla cieca; aria-snapshot come rete primaria.

## Criteri di accettazione

- [ ] `packages/api-client`: tipi generati da `openapi.yaml` del core via `openapi-typescript`; client
      `openapi-fetch` con middleware Bearer + 401→refresh→retry + mapping `problem+json`; script `gen`; un drift
      simulato dello spec rompe `tsc` (regression guard).
- [ ] `packages/i18n`: init react-i18next, cataloghi EN+IT, hook tipizzati; nessuna stringa hardcoded nella shell.
- [ ] `apps/backoffice`: chrome (sidebar PLATFORM + YOUR APPS dinamica, topbar completa) responsive; routing lazy
      nested; le 3 route guard funzionanti; auth store Zustand + refresh-on-load + logout.
- [ ] App Registry: sidebar = manifest ∩ entitlement (provider stub); modulo **demo** montato via contratto Context;
      modulo non-entitled **non** montato (guard) — coperto da test component + E2E.
- [ ] Theming via design-system (light/dark + accent); almeno un form RHF+Zod allineato a Bean Validation.
- [ ] `config.json` runtime caricato pre-render; un solo build, endpoint parametrizzati.
- [ ] Test verdi: component (Vitest+RTL+MSW) + E2E (Playwright) + a11y (axe); `npm test` e `npm run typecheck`
      verdi su tutti i workspace.
- [ ] Tutti i rinvii della sezione "Fuori scope" tracciati nei file UC/`_BACKLOG.md` di competenza.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT verificato** — la shell legge `tenant_id`/`user_id`/ruoli **solo** dai claim del token
  (auth store), mai da params/body; il contratto Context li propaga ai moduli che **non** li leggono altrove.
- **Filtro row-level** — N/A lato client; l'enforcement resta backend. La shell è **solo UX** (#09 dec.30): le
  route guard sono difesa in profondità, non sostituiscono i gate edge/servizio.
- **Modulo Terraform `microsaas_app`** — N/A (nessuna infra in questa change).
- **Logging strutturato** — N/A lato SPA statica (nessun log server-side introdotto qui).

## Requisiti di test

- **Regression guard drift OpenAPI** (#10 G25): un test/typecheck dimostra che tipi generati non combacianti col
  codice rompono `tsc`/build.
- **App Registry = intersezione**: test che con entitlement assente il modulo non compare in sidebar **e** la route
  diretta è bloccata dalla guard (UX) — `requireEntitlement`.
- **Interceptor 401→refresh→retry**: test (MSW) che un 401 scatena `/api/auth/refresh` e ritenta la richiesta originale.
- **a11y**: axe verde + aria-snapshot come rete primaria sulle schermate chiave (#10 20/39).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (solo aggiunte: nuova SPA + 2 package) |
| Contratto cross-area | Sì — frontend ↔ API core (consuma `openapi.yaml`) e ↔ auth-local (`/api/auth/*`). Nessuna modifica ai servizi. |
| Version bump | minor (nuove feature, nessuna rottura) |
