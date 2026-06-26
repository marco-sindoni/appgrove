# Implementation Log — Change 0011: Backoffice SPA shell

**Branch**: `change/0011-use-case-0020-shell-backoffice`
**Aree**: frontend (`apps/backoffice` + `packages/api-client`, `packages/i18n`)
**Completata**: 2026-06-26

## File modificati

| File | Azione |
|---|---|
| `frontend/.npmrc` | Creato (legacy-peer-deps, vedi Decisioni) |
| `frontend/package.json` | Modificato (nessuna modifica strutturale finale: override rimosso) |
| `frontend/packages/api-client/**` | Creato — pkg: client OpenAPI (`openapi-typescript` + `openapi-fetch`), middleware auth, problem+json, drift guard, `schema.ts` generato, test |
| `frontend/packages/i18n/**` | Creato — pkg: setup react-i18next, cataloghi EN/IT, hook tipizzati, test |
| `frontend/apps/backoffice/**` | Creato — SPA: config runtime, auth store/refresh/interceptor, App Registry + entitlement stub + demo module, shell chrome (Sidebar/Topbar/Breadcrumb/Layout), routing lazy + 3 route guard, pagine, form RHF+Zod, test component/a11y, E2E Playwright |
| `frontend/README.md` | (invariato) |
| `docs/usecases/_INDEX.md` | Modificato — UC 0020 🟡→✅ |
| `docs/usecases/06-frontend/0020-…md` | Modificato — sezione "Punti aperti / decisioni differite" aggiornata |
| `docs/_BACKLOG.md` | Modificato — rinvii cross-area (endpoint entitlement core, legacy-peer-deps) |

## Cosa è stato fatto

Shell del backoffice cliente come **shell skeleton completa e funzionante**, cablata su ciò che esiste oggi (core
UC 0013 + auth-local UC 0010). Tre nuovi workspace: `api-client` (tipi generati dallo spec OpenAPI del core + client
`openapi-fetch` con middleware Bearer/401→refresh→retry/problem+json), `i18n` (react-i18next EN/IT), `apps/backoffice`
(chrome permanente, App Registry ∩ entitlement con modulo demo, routing lazy + route guard, auth store Zustand +
refresh-on-load, config runtime `config.json`, theming via design-system, form RHF+Zod allineato a `UpdateAccount`).

## Decisioni prese

- **Tooling client OpenAPI**: `openapi-typescript` + `openapi-fetch` (gate domanda 2) — fetch layer artigianale per il
  controllo su interceptor e problem+json. `schema.ts` è **committato** (snapshot del contratto); `npm run gen` lo rigenera.
- **Package condivisi creati subito** (gate domanda 3, opzione B): `packages/api-client` e `packages/i18n`.
- **`frontend/.npmrc` → `legacy-peer-deps=true`**: il monorepo usa **TypeScript 6** (ultima major); alcune librerie
  (`react-i18next`, `openapi-typescript`) dichiarano ancora un peer **opzionale** `typescript@^5` non aggiornato → npm
  strict lo tratta come conflitto. Rilassa solo il check peer in install (nessun effetto runtime). Follow-up tracciato.
- **Fix loop di render (React #185)**: il `SessionGate` ora **blocca** il render del router finché il refresh-on-load non
  ha risposto (status ≠ `idle`), evitando lo swap idle→auth *dentro* l'albero route che causava un loop infinito (visibile
  solo in app reale/E2E, non nei test che pre-impostavano lo stato). Coperto da un test di regressione (`App.smoke.test`).

## Invarianti appgrove

- **tenant_id/user_id solo dal JWT** — la shell legge i claim **solo** dall'access token (auth store, `src/auth/jwt.ts`),
  mai da params/body, e li propaga ai moduli via Context (`ShellContextValue`); i moduli non li leggono altrove.
- **Filtro row-level** — N/A lato client; enforcement nel backend. La shell è **solo UX**: le route guard sono difesa in
  profondità, non sostituiscono i gate edge/servizio.
- **Modulo Terraform `microsaas_app`** — N/A (nessuna infra).
- **Logging strutturato** — N/A (SPA statica, nessun log server-side introdotto).

## Note per il revisore

- **Contratto cross-area**: la SPA **consuma** lo spec OpenAPI del core (`services/core/.../openapi.yaml`) e gli endpoint
  auth (`/api/auth/*`). **Nessuna modifica ai servizi/infra**. Il drift dello spec rompe `tsc` (drift guard #10 G25):
  `packages/api-client/src/contract.ts` riferisce path precisi → la rigenerazione di `schema.ts` su uno spec incompatibile
  fa fallire la build; aggiornare il client di pari passo allo spec e ricommittare `schema.ts`.
- **Decisioni differite tracciate** (regola costituzione): endpoint entitlement reale del core + `legacy-peer-deps` →
  [docs/_BACKLOG.md](../../docs/_BACKLOG.md); modulo demo temporaneo, schermate auth (UC 0017), schermate funzionali,
  guard admin (UC 0021), condivisione schemi Zod↔Bean Validation, wiring Caddy dev (UC 0009), config Cognito cloud,
  code-splitting → [docs/usecases/06-frontend/0020-…md](../../docs/usecases/06-frontend/0020-shell-spa-backoffice.md),
  sezione "Punti aperti / decisioni differite". Nessun punto resta solo in chat.
- **E2E visual baseline (#10 F)**: gli E2E usano asserzioni `getByRole`/`getByText`, **nessuno snapshot visivo**
  registrato → nessun baseline da ri-registrare.
- **Privacy/RoPA**: nessun nuovo trattamento (la shell mostra dati già dichiarati in UC 0013, nessun token persistito).
  "Tocca dati personali? = No". Nessun bump PP/ToS.

## Test

Tutte le suite delle aree toccate (solo `frontend/`) sono **verdi**.

- **`@appgrove/api-client`** (Vitest, 6 test): interceptor 401→refresh→retry (3 scenari), mapping problem+json/`unwrap`.
- **`@appgrove/i18n`** (Vitest, 3 test): risoluzione chiavi EN/IT, parità di chiavi EN↔IT.
- **`@appgrove/backoffice`** (Vitest+RTL+MSW+axe, 21 test): App Registry = intersezione, route guard (predicati +
  navigazione entitled/non-entitled/anonimo), interceptor 401→refresh end-to-end (MSW), decodifica claim/auth store,
  Sidebar (statica+dinamica) **+ a11y axe**, form RHF+Zod (validazione+save), smoke App (idle→authenticated, regression
  loop).
- **E2E Playwright** (chromium, 2 test, **passati**): ripristino sessione + navigazione shell + montaggio modulo entitled;
  modulo non-entitled bloccato dalla guard. Backend mockato via `page.route` (login programmatico = mock di `/refresh`).
- **Typecheck** (`tsc --noEmit`) verde su tutti e 4 i workspace. **Build** di produzione della SPA OK (modulo demo in
  chunk lazy separato).

Esito: `npm test` (root) → api-client 6 ✓ · design-system 25 ✓ · i18n 3 ✓ · backoffice 21 ✓. Playwright 2 ✓.

## Stato criteri di accettazione

- [x] `api-client`: tipi da `openapi.yaml`, client con middleware Bearer/401→refresh→retry/problem+json, script `gen`,
      drift guard (`contract.ts` → `tsc` rompe sul drift).
- [x] `i18n`: init react-i18next, cataloghi EN/IT, hook tipizzati; nessuna stringa hardcoded nella shell.
- [x] `apps/backoffice`: chrome (sidebar PLATFORM + YOUR APPS dinamica, topbar completa) responsive; routing lazy nested;
      3 route guard; auth store + refresh-on-load + logout.
- [x] App Registry: sidebar = manifest ∩ entitlement (provider stub); modulo demo montato via Context; non-entitled
      bloccato — coperto da test component + E2E.
- [x] Theming via design-system (light/dark + accent); form RHF+Zod allineato a Bean Validation (`UpdateAccount`).
- [x] `config.json` runtime caricato pre-render; un solo build, endpoint parametrizzati.
- [x] Test verdi: component + E2E + a11y; `npm test` e `npm run typecheck` verdi su tutti i workspace.
- [x] Tutti i rinvii "Fuori scope" tracciati nei file UC/`_BACKLOG.md`.
