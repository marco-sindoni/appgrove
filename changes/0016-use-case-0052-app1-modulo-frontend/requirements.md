# Change 0016: App #1 (fatture) — modulo frontend React lazy + endpoint quota-status backend

**Branch**: `change/0016-use-case-0052-app1-modulo-frontend`
**Aree**: `frontend` (apps/backoffice + co-located module), `services/fatture` (endpoint quota-status), `dev/` (Caddyfile)
**Data**: 2026-06-27
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/11-apps/0052-app1-modulo-frontend.md](../../docs/usecases/11-apps/0052-app1-modulo-frontend.md)
**Tocca dati personali?**: No — nessun nuovo trattamento. Il modulo **mostra** dati fattura (incl. dati personali dei clienti
dell'utente: `customerName`/`customerEmail`) già dichiarati in UC 0051; il nuovo endpoint quota-status espone solo conteggi
(uso/tetto), non dati personali. Manifest: nessun nuovo trattamento (#52 §7).

## Problema / Obiettivo

Rendere usabile la **prima app del marketplace** (`fatture`) dal backoffice cliente, montando il suo modulo frontend come
componente React **lazy** nella shell (UC 0020) e consumando il backend già implementato in UC 0051 (change 0015). Realizza
il DoD di UC 0052: modulo lazy + manifest registrato (∩ entitlement → sidebar), UI con design system (UC 0019), client API
tipizzato dallo spec OpenAPI di `fatture`, stati loading/empty/error, e **banner quota** con consumo/limite proattivo e CTA
upgrade. Per supportare il banner proattivo si **estende il backend `fatture`** con un endpoint quota-status di sola lettura
(decisione presa con lo sviluppatore: copertura completa del DoD §6 subito, vedi *Punti aperti* e nota su UC 0051).

## Scope

**Backend (`services/fatture`)** — endpoint quota-status:
- `GET /api/fatture/v1/quota` (o per-metrica) che restituisce `{ metric, used, limit, remaining, nature/window }` leggendo
  `QuotaService.currentUsage(metric)` + tetto da `QuotaLimitSource`. Tenant-scoped via JWT (invariante), `@RolesAllowed`
  coerente col resto del resource. Aggiornamento dello spec `openapi.yaml` (path + schema `QuotaStatusView`).
- Test JUnit (`mvn test`) per l'endpoint (uso/tetto, tenant isolation, ruoli).

**Frontend (`apps/backoffice`)** — modulo `fatture` **autocontenuto** in `src/modules/fatture/`:
- `manifest.ts`: `{ id: 'fatture', name, icon, accentToken, sections[], component: lazy(...) }`, registrato in `registry.ts`
  (`MODULES`) → visibile in sidebar solo se entitled (∩, invariante #01 10).
- **Client API co-locato**: tipi generati dallo spec OpenAPI di `fatture` (script `gen:fatture` → `modules/fatture/api/schema.ts`)
  + client tipizzato `Client<fatturePaths>` **riusando** `createApiClient`/`authMiddleware` da `@appgrove/api-client` (Bearer,
  401→refresh→retry, problem+json). Stesso origin (`coreBaseUrl`), path `/api/fatture/v1/*`.
- **Hook React Query**: `useInvoices` (paginata), `useCreateInvoice`, `useInvoiceDetail`, `useUpdateInvoice`,
  `useDeleteInvoice`, `useFattureQuota`.
- **UI** (design system UC 0019, light/dark, responsive), copertura completa del contratto:
  - **Lista** fatture paginata con stati loading/empty/error + **banner quota** (consumo/limite proattivo; a tetto: CTA upgrade).
  - **Editor di creazione** (form con righe; validazione Zod/react-hook-form coerente coi vincoli OpenAPI).
  - **Dettaglio** fattura.
  - **Cambio stato** (DRAFT→SENT→PAID/CANCELLED via PATCH) ed **eliminazione** (soft-delete).
  - Gestione **429** reattiva (difesa in profondità) → messaggio "limite raggiunto, fai upgrade".
- Il modulo legge `tenant_id`/`user_id`/ruoli/theme/nav **solo** da `useShellContext()`; nessun routing globale toccato.

**Wiring di supporto**:
- `dev/Caddyfile`: abilitare `handle /api/fatture/v1/*` → backend `fatture` (porta host assegnata).
- Stub entitlement locale (`App.tsx`): aggiungere `'fatture'` alla lista entitled in `local` (demo resta).

**Test**: Component (Vitest+RTL+MSW) per rendering condizionale (entitlement/ruolo/quota) e stati query; E2E (Playwright)
core-loop B2C entitled (crea→vede→cambia stato; banner quota; app non visibile se non entitled); a11y (axe) su lista ed editor.

## Fuori scope

- Landing app #1 (UC 0053), admin/console (UC 0021), checkout/billing (UC 0024/0028).
- Endpoint entitlement reale del core (resta stub; rinvio già tracciato su UC 0020/#09 dec.12).
- Modifiche al CRUD fatture esistente (UC 0051) oltre al nuovo endpoint quota-status.
- Promozione a `@appgrove/design-system` di nuovi primitivi (table/banner/empty-state): si realizzano nel modulo; eventuale
  promozione futura è fuori scope.

## Criteri di accettazione

- [ ] Backend: `GET /api/fatture/v1/quota` restituisce uso/tetto/rimanenza per il tenant del JWT; documentato in `openapi.yaml`;
      test JUnit verdi (incl. isolamento tenant e ruoli).
- [ ] Modulo `fatture` lazy + manifest registrato; appare in sidebar **solo** se entitled; route diretta non-entitled → guard UX.
- [ ] UI completa (lista paginata, editor creazione con righe, dettaglio, cambio stato, delete) con design system e stati
      loading/empty/error; banner quota mostra consumo/limite e CTA upgrade a tetto; 429 gestito reattivamente.
- [ ] Client `fatture` tipizzato da OpenAPI (drift-guard via `gen:fatture` + `tsc`), riusa auth/refresh/problem+json condivisi.
- [ ] Test verdi: Vitest (component, MSW), Playwright (core-loop E2E), axe (a11y) — più `mvn test` su `services/fatture`.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT**: il modulo non legge mai `tenant_id` dal contesto fuori da `useShellContext()`; il nuovo endpoint
  backend ricava il tenant dal JWT verificato, mai da params/body.
- **Filtro row-level**: `QuotaService.currentUsage` e il CRUD restano tenant-scoped lato server; il frontend è solo UX.
- **Modulo Terraform `microsaas_app`**: N/A (nessuna infra in questa change).
- **Logging strutturato**: l'endpoint quota-status logga con `tenant_id`/`app_id`/`user_id` come il resto del servizio.

## Requisiti di test

- Regression guard: test component che verifica che il modulo NON compaia in sidebar se `app_id` non in `entitled` (∩).
- Test che la lettura di `tenant_id` provenga solo dal contesto shell (nessun uso di `tenant_id` da response/props).
- Backend: test che `currentUsage`/tetto siano calcolati per il tenant del JWT e che un secondo tenant veda i propri conteggi.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | Sì — frontend ↔ API `fatture` (nuovo endpoint quota-status + consumo CRUD esistente via OpenAPI) |
| Version bump | minor (nuovo endpoint backend + nuovo modulo frontend) |

## Punti aperti / decisioni differite

- **Endpoint quota-status come responsabilità di UC 0051.** L'endpoint `GET /api/fatture/v1/quota` appartiene
  concettualmente al backend app #1 (UC 0051) ma è realizzato qui (UC 0052) per sbloccare il banner proattivo del DoD §6.
  Annotare la nota corrispondente su UC 0051. *Owner*: UC 0051.
- **Forma del tetto da `QuotaLimitSource`** (plan/entitlement-based) da confermare in implementazione; se il tetto non è
  ancora derivabile per-tenant, l'endpoint espone `limit: null` e il banner mostra solo l'uso. *Owner*: UC 0051/#09.
