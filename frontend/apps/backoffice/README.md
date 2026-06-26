# @appgrove/backoffice — SPA cliente (shell)

Shell del backoffice cliente (UC 0020): chrome permanente, App Registry, routing, auth store, API
client, i18n e theming. Ospita i **moduli app** come componenti React **lazy** (∩ entitlement).

## Stack

Vite + React + TS · React Router (lazy/nested) · TanStack Query + Zustand · React Hook Form + Zod ·
`@appgrove/design-system` (theming/primitivi) · `@appgrove/api-client` (client OpenAPI + interceptor
401→refresh→retry) · `@appgrove/i18n` (EN/IT).

## Comandi (dalla root `frontend/`, o con `-w @appgrove/backoffice`)

```bash
npm run dev      -w @appgrove/backoffice    # Vite dev server (:5173, dietro Caddy app.local.appgrove.app)
npm test         -w @appgrove/backoffice    # Vitest + RTL + MSW + axe
npm run typecheck -w @appgrove/backoffice
npm run build    -w @appgrove/backoffice
npm run e2e      -w @appgrove/backoffice    # Playwright (backend mockato via page.route)
```

## Config runtime

`public/config.json` è caricato **prima del render** (`src/config.ts`): un solo build promosso per env,
endpoint (`authBaseUrl`/`coreBaseUrl`) parametrizzati, **no-cache**.

## Confini

- Frontend = **solo UX**: `tenant_id`/`user_id`/ruoli letti **solo** dai claim del token (auth store) e
  propagati ai moduli via Context. L'enforcement è nel backend; le route guard sono difesa in profondità.
- Gli **entitlement** oggi vengono da un **provider stub** (`src/registry/entitlements.tsx`): il core non
  espone ancora l'endpoint. Vedi i rinvii tracciati nello use case 0020.
