# Implementation Log — Change 0016: App #1 (fatture) — modulo frontend + endpoint quota-status

**Branch**: `change/0016-use-case-0052-app1-modulo-frontend`
**Aree**: `frontend` (apps/backoffice + packages/api-client), `services/fatture` (backend), `dev/` (Caddyfile)
**Completata**: 2026-06-27

## File modificati

| File | Azione |
|---|---|
| services/fatture/.../QuotaDtos.java | Creato — record `QuotaStatusView` (+ factory `of`, nullability illimitato) |
| services/fatture/.../QuotaResource.java | Creato — `GET /api/fatture/v1/quota` (uso/tetto/rimanenza, tenant dal JWT) |
| services/fatture/.../META-INF/openapi/openapi.{yaml,json} | Modificato — rigenerati (augmentation) con il path quota |
| services/fatture/.../QuotaStatusTest.java | Creato — integrazione: uso/limite/rimanenza, isolamento tenant, 403 no-tenant |
| services/fatture/.../QuotaStatusViewTest.java | Creato — unit della mappatura uso→vista (con/illimitato/oltre tetto) |
| frontend/packages/api-client/src/client.ts | Modificato — `createTypedClient<P>` generico (riusa il middleware auth) |
| frontend/packages/api-client/src/index.ts | Modificato — export di `createTypedClient` |
| frontend/apps/backoffice/src/api/apiClient.tsx | Modificato — estratto `buildAuthClientConfig` (riusato dai client per-app) |
| frontend/apps/backoffice/src/modules/fatture/** | Creato — manifest, modulo lazy, client+hooks co-locati, schermate, banner quota, strings, test |
| frontend/apps/backoffice/src/registry/registry.ts | Modificato — registrato `fattureManifest` |
| frontend/apps/backoffice/src/registry/registry.test.ts | Modificato — guard: fatture visibile solo se entitled |
| frontend/apps/backoffice/src/App.tsx | Modificato — stub entitlement locale: `['demo','fatture']` |
| frontend/apps/backoffice/package.json | Modificato — script `gen:fatture` + devDep `openapi-typescript` |
| frontend/apps/backoffice/e2e/fatture.spec.ts | Creato — core-loop E2E + guard non-entitled |
| dev/Caddyfile | Modificato — route `/api/fatture/v1/*` → `:8081` |
| app-start.sh / app-stop.sh | Modificato — avvio/stop del backend `fatture` (:8081) nel launcher locale |
| docs/usecases/_INDEX.md | Modificato — UC 0052 → ✅ |
| docs/usecases/11-apps/0051-0052.md, docs/_BACKLOG.md | Modificato — decisioni differite tracciate |

## Cosa è stato fatto

Implementato il modulo frontend della prima app (`fatture`) come componente React **lazy** registrato nell'App Registry
(∩ entitlement → sidebar) e autocontenuto: client tipizzato **co-locato** generato dall'OpenAPI del servizio fatture
(`gen:fatture`) che riusa la meccanica auth condivisa via il nuovo `createTypedClient`, hook React Query, e UI col design
system (lista paginata, editor di creazione con righe, dettaglio, cambio stato, soft-delete) con stati loading/empty/error.
Il **banner quota** mostra consumo/limite e a tetto raggiunto offre la CTA upgrade; il `429` sulla creazione è gestito
reattivamente. Lato backend è stato aggiunto l'endpoint di sola lettura `GET /api/fatture/v1/quota`.

## Decisioni prese

- **Quota (opzione B, concordata)**: aggiunto subito l'endpoint backend quota-status per il banner proattivo; tracciato come
  responsabilità di UC 0051.
- **Client per-app (opzione A)**: `createTypedClient<P>` nel pacchetto condiviso + tipi/client/hook **co-locati** nel modulo
  → modulo autocontenuto (estrazione microfrontend tocca solo la entry del registry, #01 dec.11).
- **UI completa (opzione A)**: coperto tutto il contratto backend.
- **i18n**: stringhe del modulo per-modulo inline in italiano (come demo, #03 dec.6); standardizzazione bilingue differita.

## Invarianti appgrove

- **tenant_id solo dal JWT**: il modulo legge tenant/user/ruoli **solo** da `useShellContext()`; l'endpoint quota ricava il
  tenant da `CallerContext` (JWT), mai da params/body. Test `QuotaStatusTest` verifica isolamento + 403 senza claim.
- **Filtro row-level**: l'uso quota e il CRUD restano tenant-scoped lato server (discriminator); il frontend è solo UX.
- **Modulo Terraform `microsaas_app`**: non toccato (nessuna infra).
- **Logging strutturato**: l'endpoint quota eredita MDC `tenant_id/app_id/user_id` dal meccanismo commons come il resto del servizio.

## Note per il revisore

- **Contratto cross-area** frontend ↔ API fatture: nuovo endpoint `GET /api/fatture/v1/quota`; OpenAPI rigenerato e tipi FE
  generati di pari passo (drift-guard via `gen:fatture` + `tsc`).
- **`dist/` di `@appgrove/api-client` è gitignored**: la modifica è in `src`; CI ricompila i package prima dei test. In locale
  ho ricompilato il dist per eseguire i test del backoffice.
- **Decisioni differite tracciate** (constitution): endpoint quota-status (owner UC 0051) → "Punti aperti" di UC 0051 e 0052;
  i18n moduli app (trasversale) → `docs/_BACKLOG.md` ("App modules frontend (UC 0052+)").
- **Nullability**: `limit`/`remaining` nello spec sono `int64` non-nullable ma a runtime possono essere `null` (illimitato);
  il FE lo gestisce (nota su UC 0051).
- **Verifica locale (Bob)**: la lista dava "Something went wrong" perché il **backend `fatture` non era avviato** in locale
  (`app-start.sh` lanciava solo auth-local + core + SPA; Caddy proxava `/api/fatture/v1/*` → :8081 morto → 502). Risolto
  cablando il processo `fatture` in `app-start.sh`/`app-stop.sh` (stesso pattern di `core`: Postgres condiviso, `%dev`
  migra `app_fatture`, debug 5006). L'**auto-discovery multi-servizio** del dev stack resta di **UC 0046** (`dev service`
  è uno stub dichiarato). Nota: dopo il pull, se Caddy non ha ancora la nuova route, riavviare il proxy.

## Test

- **frontend** — `npm test` (backoffice) **verde**: 50 test (16 file), inclusi i 6 nuovi del modulo fatture (lista, empty,
  creazione, 429+CTA, guard non-entitled, a11y axe) e la guard di registry. `tsc --noEmit` pulito su backoffice e api-client;
  api-client `vitest` verde (6). **E2E Playwright verde**: 7 test (inclusi i 2 nuovi `fatture.spec.ts`: core-loop + guard),
  build di produzione inclusa. Nessuna baseline visiva ri-registrata.
- **services/fatture** — `mvn test` **non eseguibile in questo ambiente** (i `@QuarkusTest` richiedono Testcontainers/Docker,
  non disponibile). I sorgenti di test **compilano** (test-compile in `mvn package`); i test (`QuotaStatusTest`,
  `QuotaStatusViewTest`) girano in CI con Docker. **Da rieseguire in CI prima del merge.**

## Stato criteri di accettazione

- [x] Backend `GET /api/fatture/v1/quota` (uso/tetto/rimanenza dal JWT), documentato in OpenAPI; test scritti (esecuzione in CI).
- [x] Modulo `fatture` lazy + manifest registrato; visibile solo se entitled; route non-entitled → guard (verificato in E2E/component).
- [x] UI completa con design system e stati loading/empty/error; banner quota consumo/limite + CTA; 429 reattivo.
- [x] Client `fatture` tipizzato da OpenAPI (drift-guard), riusa auth/refresh/problem+json condivisi.
- [x] Test FE verdi (Vitest + Playwright + axe). Backend: test scritti, esecuzione delegata alla CI (Docker).
