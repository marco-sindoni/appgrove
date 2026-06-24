# UC 0012 — Core service + multitenancy (Quarkus, TenantResolver, discriminator, schema `platform`, Flyway, audit/soft-delete)

**Area**: 04-platform-core · **Fase**: 2 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0004](../02-devops-infra/0004-modulo-microsaas-app.md) (modulo/infra servizio)
**Fonte decisioni**: #04 (backend Quarkus), #01 (architettura/multitenancy), #05 (persistenza)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [04-services-backend](../../04-services-backend.md), [01-architettura](../../01-architettura.md), [05-persistenza-dati](../../05-persistenza-dati.md), [02-auth-sicurezza](../../02-auth-sicurezza.md)

## 1. Obiettivo / Scope
Realizzare lo scheletro del **core/platform service** Quarkus e il **commons** condiviso che rende esecutive le invarianti
multi-tenancy per **tutti** i servizi.
**Incluso**: progetto **Maven multi-module** (`services/` parent + `commons` + `core`); **Hibernate `DISCRIMINATOR`** +
**`TenantResolver`** request-scoped dal JWT (fail-closed); **base entity** (audit + soft-delete + UUID v7); **exception mapper
problem+json**; paginazione; **MDC logging**; schema **`platform`** + **Flyway** (migrate-at-start off); native/JVM per profilo.
**Escluso**: le entità/API di dominio (UC [0013](0013-account-utenti-inviti-api.md)), l'auth/JWT issuance (UC 0015/0016),
l'authorizer (UC 0014).

## 2. Attori & ruoli
- **Developer**: scrive servizi sopra `commons`.
- **Sistema**: ogni richiesta autenticata risolve `tenant_id` dal JWT e filtra automaticamente.
- Riusato da **ogni** app via dipendenza `commons` (#04 2).

## 3. Precondizioni
- Infra servizio (UC 0004): schema `platform` vuoto + ruolo DB; JWT validabile (UC 0016) per i test d'integrazione.

## 4. Flusso principale
1. **Struttura**: `services/pom.xml` (parent) + `services/commons/` + `services/core/` (#04 2).
2. **`commons`**: `TenantResolver` legge `tenant_id` dal **JWT verificato**; Hibernate multitenancy `DISCRIMINATOR` aggiunge il
   filtro tenant a **ogni** query; **fail-closed** se manca il tenant (#02 12, #04 vincoli).
3. **Base entity**: `created_at/updated_at/created_by/updated_by`, `deleted_at` (soft-delete con filtro auto), **PK UUID v7**
   (`@UuidGenerator(style=TIME)`) (#05 4/5/6).
4. **REST**: DTO al boundary, Bean Validation, **exception mapper centralizzati → problem+json (RFC 9457)**, correlation id in MDC,
   paginazione offset (#04 6, #01 13).
5. **Persistence**: Panache repository pattern; **Flyway** per schema `platform`, migrate-at-start **off** (gira in CI one-shot, UC 0005) (#05 1/3).
6. **Build**: JVM in dev, **native** in test/prod (#04 4).

## 5. Flussi alternativi / edge / errori
- **JWT senza `tenant_id`/membership** → **fail-closed**, accesso negato (mai default tenant) (#10 D9).
- **`tenant_id` in body/params** → **ignorato** (solo dal JWT, anti-override) (#10 D10).
- **Soft-delete**: le entità di business filtrano `deleted_at`; hard-delete solo per erasure GDPR/offboarding (#05 6).
- **Cross-schema**: vietato FK/query tra schemi; `tenant_id` è riferimento logico (#05 9).

## 6. Risorse & runbook
**Risorse**: moduli Maven `commons`+`core`; migrazioni Flyway `platform`; `application.properties` con profili `%dev/%test/%prod`.
**Runbook (dev)**: `dev migrate` + `dev service core` (UC 0009) → core su Quarkus dev mode contro Postgres locale + auth locale (UC 0010).

## 7. Dati toccati
Schema `platform` (struttura base; le tabelle di dominio in UC 0013). Colonne audit ovunque. **Dati personali**: nascono in
UC 0013 (users/email) — qui si predispone solo l'infrastruttura (soft-delete, audit, isolamento). Manifest GDPR → UC 0013/0030.

## 8. Permessi & gate
- **Invarianti (cuore)**: `tenant_id` **solo dal JWT verificato**; **filtro row-level automatico** via discriminator; **logging
  strutturato** con MDC; il `commons` è il punto unico che le rende vere per tutti i servizi (anche quelli di `new-application`).
- **ArchUnit** (UC test) impedisce bypass del filtro o lettura di `tenant_id` dalla request (#10 16).

## 9. Requisiti di test
- **Integration** (`@QuarkusTest` + Testcontainers Postgres 17 + Flyway reale, #10 6): schemi veri, migrazioni testate.
- **Security/multi-tenancy** (#10 D, sempre eseguita): matrice cross-tenant (≥2 tenant), fail-closed, anti-override `tenant_id`, leak detector DB.
- **Identità nei test** (#10 D14): iniezione tramite `@TestSecurity` + **smallrye-jwt** (token firmati reali con claim `tenant_id`/`roles`); l'harness firma i JWT di test → copre fail-closed e matrice ruoli senza dipendere da Cognito.
- **ArchUnit** guardia statica degli invarianti (#10 16). Harness di isolamento riusabile (#10 15) ereditato dalle app.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #04 1/2/3/4/5/6, #01 (invarianti 1/2/3/4), #05 1/3/4/5/6/8/9, #02 11/12, #10 D14/15/16.
- **DoD**:
  1. `commons` + `core` Maven multi-module con TenantResolver + discriminator + base entity + problem+json + MDC.
  2. Flyway su `platform` (migrate-at-start off) eseguito in CI.
  3. Suite security/multi-tenancy + ArchUnit verdi (fail-closed, anti-override, leak detector).
  4. Build JVM (dev) / native (test+prod).

## Punti aperti / decisioni differite

_Tracciati dalla change `0006-use-case-0012-…` (regola CLAUDE.md "Tracciamento delle decisioni differite")._

- **Verifica JWT: smallrye-jwt (public key) ora, OIDC poi.** Il core verifica i JWT via MicroProfile JWT
  (smallrye-jwt) con chiave pubblica e issuer placeholder `https://local.appgrove.app`, così è testabile senza un
  issuer attivo. La decisione #04 prevede **Quarkus OIDC**: va confermato/sostituito quando l'issuer esiste — dev
  = auth locale (**UC 0010**), prod = Cognito/Pre-Token-Gen (**UC 0015/0016**).
- **Build native non eseguita.** Profilo configurato per JVM; la build **native GraalVM** (test/prod) gira in
  CI (**UC 0005**) — non eseguita in questo change.
- **Entity/endpoint harness `example.Widget`.** Entity tenant-scoped + endpoint `/api/_demo/widgets` + migration
  di test servono solo a esercitare il multitenancy. **Da rimuovere** quando **UC 0013** introduce le entità reali.
- **`created_by`/`updated_by` non popolati automaticamente.** Le colonne audit-attore esistono ma non vengono
  valorizzate dal `sub` del JWT (manca un AuditListener). Completare con l'integrazione auth (**UC 0013/0015**).
- **Datasource di produzione** (URL/credenziali, RDS Proxy) non configurato: arriva con l'infra (**UC 0004/0055**, ☁).
