# Backend / services (Quarkus) — Decisioni

**Stato**: 🟢 deciso (trasporto eventi/authorizer/RDS Proxy → #06; logging → #08)
**Ultimo aggiornamento**: 2026-06-14

## Scope
Come si scrive un microservizio Quarkus in appgrove: stack (reactive/imperative), struttura del codice,
convenzioni REST/errori/validation, persistence wiring, auth/authz, librerie condivise, build native.
Vale sia per i servizi per-app sia per il **core/platform service** (entrambi Quarkus). Non copre il provisioning
(→ [06-infra-iac](06-infra-iac.md)) né la pipeline (→ [07-devops-cicd](07-devops-cicd.md)).

## Vincoli ereditati (già decisi)
- Quarkus (GraalVM native), **un servizio per app** + core; ECS Fargate; API GW v2 path `/api/<app_id>/v1/*`.
- Errori **RFC 9457 problem+json**; versioning nel path; auth `Authorization: Bearer`.
- **Quarkus OIDC** (issuer Cognito, JWKS, audience); **`@RolesAllowed`**, role-claim-path = `roles`.
- **Hibernate multitenancy `DISCRIMINATOR`** + `TenantResolver` da JWT (fail-closed).
- **Flyway** via CI/CD (migrate-at-start off), migration per-schema. **Agroal** diretto da Fargate.
- **PK UUID v7** (`@UuidGenerator`), colonne audit + **soft-delete**; ruolo DB **per servizio** (least privilege).
- Ogni app espone una **purge per `tenant_id`** (erasure GDPR orchestrata dal core).

## Topic dell'area (agenda)
- **A. Stack: reactive vs imperative** — Quarkus REST + Hibernate ORM (blocking) vs Hibernate Reactive + Mutiny.
- **B. Struttura del codice** — layer/package (resource/service/repository/entity/dto), layout del modulo.
- **C. Librerie condivise nel monorepo** — commons (tenant/JWT, problem+json, base entity, paginazione, MDC): come si condivide (Maven multi-module?).
- **D. Persistence wiring** — Panache (repository/active-record) vs JPA puro; base entity (audit/soft-delete/UUID v7); config discriminator; location Flyway per schema.
- **E. REST & validation** — resource naming, paginazione/filtri/sort, DTO vs entity, Bean Validation, exception mapper → problem+json, correlation id.
- **F. Native build** — native ovunque vs JVM in dev/native in test+prod.
- **G. Enforcement entitlement** — come un servizio verifica che il tenant abbia l'app **attiva** (no service-to-service deciso): claim, gateway, o eccezione?
- **H. Orchestrazione purge** — chi/come invoca la purge per-tenant (il core deve raggiungere le app: rivedere il "no service-to-service").

## Decisioni prese

### Stack (topic A)
1. **Imperative**: Quarkus REST + **Hibernate ORM (blocking)**. Niente reactive/Mutiny (multitenancy più maturo, debugging più semplice).

### Librerie condivise (topic C)
2. **Maven multi-module** sotto `services/`: parent pom + modulo **`commons`** + un modulo per servizio
   (core + ogni app). `commons` contiene: `TenantResolver`/security JWT, exception mapper **problem+json**,
   **base entity** (audit/soft-delete/UUID v7), paginazione, logging MDC. I servizi dipendono da `commons`.
   _Struttura: `services/pom.xml` (parent), `services/commons/`, `services/<app_id>/`._

### Persistence (topic D)
3. **Panache, repository pattern** (repository dedicati su Hibernate ORM). Base entity condivisa in `commons`
   (audit, soft-delete, `@UuidGenerator` v7). Config discriminator multitenancy + `TenantResolver` da JWT.

### Native build (topic F)
4. **JVM in dev** (Quarkus dev mode, hot reload), **native GraalVM in test+prod** (avvio rapido, RAM bassa su Fargate).

### Struttura del codice (topic B)
5. **Package-by-layer**: `api` (resource + DTO) / `service` / `persistence` (entity + repository) / `config`.
   Semplice per servizi piccoli e verticali.

### REST & validation (topic E)
6. **DTO sempre al boundary** (entity mai esposte). **Bean Validation** (Jakarta) sui DTO. **Exception mapper
   centralizzati** in `commons` → **problem+json**. **Correlation id** (`X-Request-Id`/`traceparent`) propagato in MDC
   (→ [08-observability](08-observability.md)). **Paginazione offset-based** (`page`/`size` + total) per il PoC.

### Enforcement entitlement (topic G — aggiorna #01/#02)
7. **Custom Lambda authorizer** su API Gateway: verifica il **JWT** *e* controlla che il tenant abbia **attivo
   l'`app_id` del path** (legge `platform.entitlements` via RDS Proxy, con **caching** dell'authorizer; revoca con
   lag = TTL cache). Sostituisce l'authorizer Cognito nativo. Il servizio (Quarkus OIDC) **ri-valida il JWT** e fa
   l'**authz sui ruoli** (`@RolesAllowed`) — difesa in profondità.

### Orchestrazione purge (topic H — aggiorna #05)
8. **Eventi async (EventBridge)**: il core emette `tenant.offboarded`; ogni servizio **consuma e purga** i dati del
   tenant nel proprio schema. Primo pezzo di messaging; trasporto (EventBridge→SQS→servizio vs Lambda per-app) → [06-infra-iac](06-infra-iac.md).

### OpenAPI / Swagger (topic E)
9. **OpenAPI + Swagger su tutti i backend** (core e app). Spec `/q/openapi` **sempre generata**; **Swagger UI**
   abilitata anche fuori dal dev mode (`quarkus.swagger-ui.always-include=true`). **Accesso solo `platform-admin`**
   (non admin di tenant): **libero in local** (dev mode), in **test+prod** esposto solo via route **gated platform-admin**
   (il Lambda authorizer verifica il ruolo). Serving esatto (diretto vs via backoffice admin) → dettaglio impl/[03-frontend](03-frontend.md).

## Questioni aperte
_Nessuna sul backend. Trasporto eventi e provisioning authorizer/RDS Proxy → #06; dettagli logging/correlation → #08._

## Alternative valutate / scartate
- **Reactive (Hibernate Reactive + Mutiny)** — scartato: complessità e multitenancy meno maturo, non giustificati ora.
- **Commons come lib versionata a parte / nessuna condivisione** — scartati: il multi-module è più semplice e senza drift.
- **Native ovunque / JVM ovunque** — scartati: JVM in dev (iterazione) + native in test/prod (costo/avvio) è l'equilibrio.
- **Entitlement via claim JWT / via chiamata al core** — scartati a favore del Lambda authorizer (revoca più pronta del claim, niente service-to-service).
- **Purge via chiamata diretta core→app / manuale** — scartati a favore degli eventi EventBridge (disaccoppiamento).
- **Package-by-feature / paginazione cursor** — scartati per semplicità nel PoC.

## Impatti su altre aree
- [02-auth-sicurezza](02-auth-sicurezza.md), [05-persistenza-dati](05-persistenza-dati.md), [06-infra-iac](06-infra-iac.md), [07-devops-cicd](07-devops-cicd.md), [10-testing](10-testing.md)
