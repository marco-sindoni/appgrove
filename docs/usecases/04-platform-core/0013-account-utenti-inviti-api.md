# UC 0013 — Accounts/Users/Invitations + core REST API (problem+json, OpenAPI)

**Area**: 04-platform-core · **Fase**: 2 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0012](0012-servizio-core-multitenancy.md) (core + multitenancy)
**Fonte decisioni**: #05 A (data model core), #04 (REST/OpenAPI), #01 (core ownership)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [05-persistenza-dati](../../05-persistenza-dati.md), [04-services-backend](../../04-services-backend.md), [01-architettura](../../01-architettura.md), [13-compliance-privacy](../../13-compliance-privacy.md)

## 1. Obiettivo / Scope
Implementare il **data model e le API del core**: account (tenant), utenti (membership foldata), inviti, e il catalogo
(struttura; il modello pricing vive in #09/UC 0022).
**Incluso**: tabelle `accounts` (+`paddle_customer_id`), `users` (membership su users, ruolo, `cognito_sub`), `invitations`;
struttura catalogo `app`/`app_tier`/`app_price` + `subscription` (popolati da #09/UC 0022/0025); REST API core
`/api/platform/v1/*` (problem+json, **OpenAPI** committato, Swagger gated platform-admin).
**Escluso**: l'**entitlement** (DERIVATO da `subscription`, nessuna tabella — #09 dec.12), gestito a runtime in UC 0014/0027;
i flussi UI auth (UC 0017); il billing/webhook (UC 0025).

## 2. Attori & ruoli
- **Owner/admin/member** (tenant-level): gestiscono account/utenti/inviti secondo ruolo.
- **platform-admin**: accesso console admin (UC 0021), Swagger.
- **Sistema**: signup crea account+owner (UC 0015/0017); webhook popola subscription (UC 0025).

## 3. Precondizioni
- Core attivo (UC 0012); JWT con `tenant_id`/`roles` (UC 0016) per le API protette.

## 4. Flusso principale
1. **accounts**: `id` (=`tenant_id`), `name`, `status` (active/suspended), **`paddle_customer_id`** (lazy, #09 C15).
2. **users**: `id`, `cognito_sub` (unique), `email`, `display_name`, `tenant_id`, `role` (owner/admin/member), `status` —
   **membership foldata** (1 utente→1 tenant), nessuna tabella memberships (#05 7).
3. **invitations**: `email`, `role`, `token_hash` (single-use), `status`, `expires_at`, `invited_by`, `accepted_user_id` (#05 7, #02 14).
4. **Catalogo** (struttura): `app` (`app_id`, `name`, `user_model`, `status`, `paddle_product_id`), `app_tier` (`limits` JSON
   metrica/finestra/tetto, `features`, `trial_days`), `app_price` (`billing_cycle`, `paddle_price_id`, importo, valuta);
   **subscription** (tenant↔app, status, period, cancel_at, trial_end) — definizione autorevole #09 B (popolata in UC 0022/0025).
5. **API** `/api/platform/v1/*` (CRUD account/utenti/inviti secondo ruolo) con problem+json + **OpenAPI** generato e committato (#04 6/9). **Versioning**: la versione è nel path (`/v1/`); un eventuale `/v2/` coesisterà senza rompere i client (deprecazione graduale), con `oasdiff` a bloccare i breaking change non versionati (#01 dec.12, #10 G).

## 5. Flussi alternativi / edge / errori
- **Invito scaduto/già usato** → problem+json (coerente UC 0017 UC7).
- **Email con account esistente** (1 utente→1 tenant) → rifiuto con messaggio (#02 14).
- **Soft-delete** su account/utenti; hard-delete solo via erasure (UC 0032).
- **Catalogo/subscription**: scritti dalla pipeline pricing-sync (UC 0022) e dal consumer webhook (UC 0025), non da editor runtime (#09 H34).

## 6. Schermate & stati
API-first (le UI sono UC 0017/0020/0021). **OpenAPI** = contratto; **Swagger UI** sempre generata ma **gated platform-admin**
(libera in dev, gated in test/prod via authorizer) (#04 9).

## 7. Dati toccati
Schema `platform`: `accounts`, `users`, `invitations`, catalogo, `subscription`. **Dati personali** (manifest piattaforma #13 C):
`email`, `display_name`, `cognito_sub` (identità) — finalità **erogazione/gestione account**, base **contratto** (6.1.b),
retention "finché attivo" + grace 14gg cancellazione (#13 E25). `@PersonalData` su email/nome. Billing/fiscale = **in capo a Paddle**.

## 8. Permessi & gate
- **Invarianti**: ogni query tenant-scoped filtra `WHERE tenant_id` (discriminator UC 0012); `tenant_id` dal JWT. Le tabelle
  di **catalogo** sono platform-level (non tenant-scoped); `subscription` è tenant-scoped.
- **Ruoli** (`@RolesAllowed`): owner/admin gestiscono utenti/inviti; member sola lettura del proprio profilo; platform-admin via console.
- **Entitlement**: NON è una tabella — derivato da `subscription` a runtime (UC 0014/0027).

## 9. Requisiti di test
- **Integration** (Testcontainers + Flyway): CRUD account/utenti/inviti; vincolo 1 utente→1 tenant; inviti single-use/scadenza.
- **Security/multi-tenancy** (#10 D): A non legge utenti/inviti di B; anti-override `tenant_id`; matrice ruoli (#10 11).
- **Contract** (#10 G): risposte validate contro OpenAPI; drift detection; oasdiff bloccante sui breaking change.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #05 7 (data model core), #09 B10/B11 (catalogo/subscription), #04 6/9, #01 5, #13 C (manifest piattaforma).
- **DoD**:
  1. Tabelle `accounts`/`users`/`invitations` + struttura catalogo + `subscription` (Flyway `platform`).
  2. API core `/api/platform/v1/*` problem+json + OpenAPI committato + Swagger gated.
  3. Suite security/multi-tenancy + ruoli + contract verdi.
  4. Nessuna tabella entitlements (derivato); membership foldata su users.

## Punti aperti / decisioni differite

_Tracciati dalla change `0006-use-case-0012-…` (regola CLAUDE.md "Tracciamento delle decisioni differite")._

- **Rimuovere l'harness multitenancy di UC 0012.** Il core contiene un'entity/endpoint demo (`example.Widget`,
  `/api/_demo/widgets`) con tabella creata solo da una migration di test, usata per validare il multitenancy in
  assenza di entità di dominio. Quando si implementano `accounts`/`users`/`invitations` qui, **rimuovere l'harness**
  e spostare la suite multi-tenancy sulle entità reali.
- **Audit-attore (`created_by`/`updated_by`).** Valorizzarli dal `sub` del JWT (es. AuditListener) quando arrivano
  le entità con scrittura utente.

_Aggiunti dalla change `0013-use-case-0059-…` (UI gestione membri/inviti): due lacune lato core riscontrate._

- **Guard "ultimo owner" assente.** `PATCH /users/{id}` e `DELETE /users/{id}` non impediscono di declassare/rimuovere
  l'**ultimo owner** del tenant (né le self-action che chiuderebbero fuori l'unico owner): sono gated solo da
  `@RolesAllowed(OWNER,ADMIN)`. La UI (UC 0059) applica solo una protezione **UX** (disabilita le azioni su self/ultimo
  owner). Aggiungere l'enforcement server-side (rifiuto problem+json) qui.
- **OpenAPI di `POST /invitations` senza body di risposta.** Lo spec generato dichiara `200` **senza `content`**, mentre
  il servizio ritorna `InvitationView` col **token grezzo** (status atteso `201`). Il client (UC 0059) è costretto a
  castare manualmente la risposta (`components['schemas']['InvitationView']`) perché il tipo generato non la modella.
  Annotare `@APIResponse(responseCode="201", content=InvitationView)` su `InvitationResource.create` così che lo spec
  (e il client `gen`) riflettano il contratto reale.

_Aggiunti dalla change `0014-use-case-0021-…` (console admin). Dettaglio in [_BACKLOG](../../_BACKLOG.md) "Console admin (UC 0021)"._

- **Pattern "endpoint admin non-tenant-scoped".** `AdminResource` (UC 0021) introduce la **prima** superficie cross-tenant:
  query **native** read-only gated `platform-admin` che **bypassano** il filtro `@TenantId` (eccezione esplicita e
  documentata all'invariante #2). Da **formalizzare**: come/dove disabilitare in sicurezza il filtro tenant, test
  anti-leak sistematici, eventuale separazione del persistence path admin (coordinare con doc #02).
- **Schema override entitlement per-tenant.** La console admin **non** offre un toggle entitlement per-tenant perché nel
  modello attuale l'unica leva è la `subscription`. Se servirà (UC 0014/0027), serve **decidere lo schema** di un override
  per-tenant-app (tabella/colonna) qui.
