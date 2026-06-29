# Implementation Log — Change 0023: Enforcement entitlement + quota (read-model, gate 402, cap reale flow/stock)

**Branch**: `change/0023-use-case-0027-enforcement-entitlement-quota`
**Aree**: `services/commons`, `services/core`, `services/fatture`, `frontend`
**Completata**: 2026-06-29

## File modificati

| File | Azione |
|---|---|
| services/commons/src/main/java/app/appgrove/commons/entitlement/MetricLimit.java | Creato |
| services/commons/src/main/java/app/appgrove/commons/entitlement/EntitlementView.java | Creato |
| services/commons/src/main/java/app/appgrove/commons/entitlement/MeEntitlementsView.java | Creato |
| services/commons/src/main/java/app/appgrove/commons/entitlement/EntitlementClient.java | Creato |
| services/commons/src/main/java/app/appgrove/commons/entitlement/EntitlementService.java | Creato |
| services/commons/src/main/java/app/appgrove/commons/entitlement/RestEntitlementService.java | Creato |
| services/commons/src/main/java/app/appgrove/commons/entitlement/RequiresEntitlement.java | Creato |
| services/commons/src/main/java/app/appgrove/commons/entitlement/EntitlementGateFilter.java | Creato |
| services/commons/src/main/java/app/appgrove/commons/entitlement/EntitlementRequiredException.java | Creato |
| services/commons/src/main/java/app/appgrove/commons/web/EntitlementRequiredMapper.java | Creato |
| services/commons/src/main/java/app/appgrove/commons/quota/EntitlementQuotaLimitSource.java | Creato |
| services/commons/pom.xml | Modificato (dep `quarkus-rest-client-jackson`) |
| services/commons/src/test/java/app/appgrove/commons/entitlement/RestEntitlementServiceTest.java | Creato |
| services/commons/src/test/java/app/appgrove/commons/entitlement/EntitlementGateFilterTest.java | Creato |
| services/commons/src/test/java/app/appgrove/commons/web/EntitlementRequiredMapperTest.java | Creato |
| services/core/src/main/java/app/appgrove/core/billing/EntitlementReadModel.java | Creato |
| services/core/src/main/java/app/appgrove/core/billing/MeResource.java | Creato |
| services/core/src/main/resources/application.properties | Modificato (config client) |
| services/core/src/main/resources/META-INF/openapi/openapi.{yaml,json} | Rigenerato (nuovo endpoint) |
| services/core/src/test/java/app/appgrove/core/billing/EntitlementsApiTest.java | Creato |
| services/core/src/test/resources/application.properties | Modificato |
| services/fatture/.../ConfigQuotaLimitSource.java | Eliminato (sostituito da impl entitlement-driven) |
| services/fatture/.../InvoiceResource.java | Modificato (`@RequiresEntitlement`) |
| services/fatture/src/main/resources/application.properties | Modificato (rimosso cap config; URL client) |
| services/fatture/src/test/resources/application.properties | Modificato |
| services/fatture/.../MockEntitlementService.java, EntitlementGateTest.java | Creati (test) |
| frontend/apps/backoffice/src/registry/entitlementsApi.ts (+ .test.ts) | Creato |
| frontend/apps/backoffice/src/registry/entitlements.tsx | Modificato (EntitlementsProvider reale) |
| frontend/apps/backoffice/src/App.tsx | Modificato (provider reale dentro SessionGate) |
| frontend/apps/backoffice/src/App.smoke.test.tsx | Modificato (mock `/me/entitlements`) |
| frontend/packages/api-client/src/schema.ts | Rigenerato da OpenAPI |

## Cosa è stato fatto

Implementato l'enforcement runtime entitlement/quota di UC 0027. **core** espone `GET /api/platform/v1/me/entitlements`
(`EntitlementReadModel`): per il tenant del JWT, le app entitled con `appSlug`, `tierKey`, `phase`, `accessUntil` e
`limits{metric:{cap,nature,window}}`, derivando da `subscription` (`grantsAccess()`/`SubscriptionLifecycle`) ∧ app attiva,
con **free tier come baseline** in assenza di subscription, e riconciliazione slug↔UUID. **commons** fornisce il meccanismo
riusabile: client REST (`EntitlementClient`/`RestEntitlementService`, JWT propagato, cache per-richiesta), gate **402**
opt-in (`@RequiresEntitlement` + filtro + mapper) e `EntitlementQuotaLimitSource` (cap reale dall'entitlement, flow/stock).
**fatture** ritira `ConfigQuotaLimitSource` e annota `POST /invoices` col gate (lasciando `/quota` informativo fuori dal
gate). **frontend** popola il registry da `/me/entitlements` (real fetch dentro il SessionGate); `demo` resta solo in locale.

## Decisioni prese

- **Confine cross-servizio (A)**: endpoint unico `/me/entitlements` in core, chiamato dalle app via client REST in commons.
  La chiamata **sincrona app→core** è accettata ora ma è un antipattern a regime → disaccoppiamento event-driven tracciato.
- **Meccanismo in commons** (non in fatture) così UC 0046/0054 lo ereditano; gate **opt-in per annotazione** → esenzione
  GDPR per costruzione (endpoint non annotati non passano dal gate).
- **flow + stock** completi nel contratto/read-model + test; enforcement **live solo flow** (fatture). Consumatore stock
  reale (seats) → UC 0054.
- **Free tier come baseline** (nessuna subscription = entitlement al tier senza prezzo). Asimmetria con `canceled`/`paused`
  (niente fallback al free) tracciata come decisione di prodotto differita (UC 0027 Punti aperti / UC 0028).

## Invarianti appgrove

- **tenant_id dal JWT**: `/me/entitlements` deriva il tenant dal claim verificato; il client commons propaga il JWT del
  chiamante a core (mai da param/body).
- **Filtro row-level**: subscription lette tenant-scoped (discriminator); catalogo `app`/`app_tier` platform-level by design.
- **Modulo `microsaas_app`**: non toccato (nessuna infra).
- **Logging strutturato**: `MeResource` logga con MDC `tenant_id`/`user_id`; 402/429 sono problem+json.

## Note per il revisore

- **Contratto cross-area**: nuovo endpoint core consumato sia dal frontend sia da fatture (via commons); OpenAPI core e
  `schema.ts` FE rigenerati nello stesso commit. `commons` ora dipende da `quarkus-rest-client-jackson` (transitivo a core/
  fatture): aggiunti `quarkus.rest-client.core-api.url` nelle properties (core: self, non usato; fatture: core :8080 in dev).
- **Decisioni differite tracciate** (constitution): disaccoppiamento event-driven app→core → [_BACKLOG.md](../../docs/_BACKLOG.md)
  + [UC 0046](../../docs/usecases/10-skills-tooling/0046-skill-new-application.md) + [_INDEX.md](../../docs/usecases/_INDEX.md)
  Eccezioni #5; consumatore stock/seats → [UC 0054](../../docs/usecases/11-apps/0054-app2-b2b-via-new-application.md);
  UX azionabile 402/429 + riattivazione → [UC 0028](../../docs/usecases/07-payments/0028-portale-cliente-self-service.md);
  esenzione GDPR sugli endpoint reali export/erasure → [UC 0032](../../docs/usecases/08-compliance-gdpr/0032-framework-esportazione-cancellazione.md);
  `canceled`→free fallback (prodotto) → [UC 0027](../../docs/usecases/07-payments/0027-applicazione-entitlement-quota.md) Punti aperti.
- **Cache entitlement**: solo per-richiesta (RequestScoped). Cache cross-richiesta/invalidazione post-checkout è parte del
  disaccoppiamento futuro; oggi una nuova subscription compare ricaricando la query (FE) — accettabile local-first.

## Test

- **commons (`mvn test`)** — verde: `RestEntitlementServiceTest` (cap/natura flow **e** stock, accesso, sconosciuto),
  `EntitlementGateFilterTest` (402 vs pass), `EntitlementRequiredMapperTest` (402 problem+json). Tot. 9.
- **core (`mvn test`)** — verde (91): `EntitlementsApiTest` (6) — derivazione accesso (grantsAccess + app abilitata),
  baseline free, natura flow/stock nei limiti, isolamento per tenant, fail-closed (403) e 401.
- **fatture (`mvn test`)** — verde (21): `EntitlementGateTest` (402 su `POST /invoices` senza accesso; 201 con accesso;
  `/quota` non gated raggiungibile senza accesso = opt-in/esenzione GDPR); quota flow 10/mese risolta via entitlement
  (mock), suite esistenti invariate.
- **frontend (`npm test`)** — verde: `entitlementsApi.test.ts` (computeEntitled: slug, demo-in-local, vuoti), smoke App con
  `/me/entitlements` mockato. Typecheck `tsc --noEmit` verde (backoffice + api-client).
- **`./run-tests.sh`** — backend ✓, frontend ✓, infra saltata (terraform non installato; infra non toccata).
- **E2E visual baseline**: nessuno snapshot ritoccato (non applicabile a questa change).

## Stato criteri di accettazione

- [x] `GET /me/entitlements` ritorna le app entitled del tenant con `appSlug` + `limits{metric:{cap,nature,window}}`;
  regola `access = appActive && (grantsAccess ‖ free baseline)` verificata col seed (teams active; legacy inactive escluso;
  free baseline).
- [x] Gate **402** opt-in: endpoint annotato → 402 senza accesso, passa con accesso; endpoint non annotato raggiungibile
  senza accesso (esenzione GDPR per costruzione).
- [x] Quota `fatture` (flow, cap 10) risolta dall'entitlement; 11ª → 429; `ConfigQuotaLimitSource` rimosso.
- [x] Contratto **flow + stock** completo in commons e testato; `natureOf/capFor` espongono la natura da `app_tier.limits`.
- [x] Registry FE popolato da `/me/entitlements`; `demo` solo in locale.
