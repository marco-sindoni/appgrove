# Architettura applicativa & multi-tenancy — Decisioni

**Stato**: 🟢 deciso (modello ruoli rinviato a [02-auth-sicurezza](02-auth-sicurezza.md))
**Ultimo aggiornamento**: 2026-06-14

## Scope
Forma architetturale del sistema: cos'è un tenant, cos'è un'app, come si attivano, e i confini/contratti
tra shell, moduli app (microfrontend) e microservizi. NON copre i dettagli di Cognito/JWT (→
[02-auth-sicurezza](02-auth-sicurezza.md)), i dettagli IaC/networking (→ [06-infra-iac](06-infra-iac.md))
né lo schema dati fisico (→ [05-persistenza-dati](05-persistenza-dati.md)).

## Topic dell'area (agenda)
- **A. Modello di tenancy** — un tenant è un singolo utente (B2C) o un'organizzazione con N utenti (B2B)? Conferma isolation shared-DB/schema-per-app/row-level filter.
- **B. Modello di "app" & attivazione** — cos'è un'app; come si attiva/disattiva per tenant; dove vivono gli entitlement.
- **C. Core/platform service** — esiste un servizio trasversale (catalogo, entitlement, profilo tenant, webhook billing, admin) o tutto è per-app + Cognito?
- **D. App Registry** — statico nel frontend o servito dal backend in base agli entitlement del tenant?
- **E. Routing & API surface** — un'unica API Gateway path-based (`/api/<app>/*`) o una per app? Comunicazione service-to-service?
- **F. Contratti & confini** — shell↔modulo app (cosa passa: token/tenant/theme/eventi); frontend↔service (convenzioni REST, formato errori); versioning dei contratti.
- **G. Naming & identificatori** — convenzioni app_id/app_name, schema, service, propagazione tenant_id.

## Decisioni prese

### Tenancy (topic A)
1. **Tenant = account/organizzazione**, sempre presente — unità di isolamento dati e di billing.
   Anche l'uso "per singolo privato" è un account con un solo utente; non esiste entità senza tenant.
2. **`tenant_id` = account id, distinto da `sub`** (= user id). `sub` identifica l'utente, `tenant_id`
   l'account. Logging: `tenant_id` (account) + `user_id` (sub). Filtro row-level invariato:
   `WHERE tenant_id = :tid` con tid = account id.
3. **Isolation**: shared Aurora + schema-per-app + filtro row-level per tenant (confermato dal recap).
4. **Modalità utente = capability per-app**, dichiarata nei metadati del catalogo:
   - `single-user`: un solo utente effettivo per tenant, dati isolati (esperienza "B2C", es. fatturazione privati).
   - `multi-user`: un owner invita N membri con ruoli dentro il tenant (es. mini-CRM).
   La granularità intra-tenant (per-utente/ruolo) sta *dentro* il filtro per tenant, non lo sostituisce.

### Core/platform service (topic C, B)
5. **Esiste UN core/platform service** che è la source of truth della piattaforma. Possiede (DB proprio):
   `accounts` (+ `paddle_customer_id`), **`identity`** (l'identità della persona: entità di piattaforma) +
   **`membership`** (la coppia account↔identità con ruolo e stato) — la membership ripiegata su `users` è
   **superata** da UC 0116, change 0088; `users` resta come rete di ritorno, fredda,
   `invitations`, **catalogo** (`app`/`app_tier`/`app_price` — #09 B), **`subscription`** (tenant↔app; **entitlement
   DERIVATO**, niente tabella entitlements — #09 dec.12), gestione **webhook billing Paddle**.
   Cognito fa **solo autenticazione** (identity provider). I servizi per-app restano verticali puri.

### Identità & token (topic A, → dettaglio in 02)
8. ~~**1 utente → 1 tenant**~~ → **una persona, più appartenenze** (UC 0116, change 0088). L'identità della
   persona è unica sulla piattaforma; le sue **appartenenze** agli account possono essere più di una, al
   massimo una per account. Un invitato riceve una **appartenenza** nel tenant che invita, e l'identità
   si crea solo se non esiste già: la stessa persona in due account è ormai un caso normale, con **un
   solo** login. Con una sola appartenenza — il caso di tutti gli utenti di oggi — nulla cambia: nessun
   selettore, nessun passaggio in più. L'account attivo di una sessione e il selettore sono di UC 0117.
9. **Pre-Token-Generation Lambda**: a ogni emissione del token legge dal core l'appartenenza attiva della
   persona (identità ⋈ appartenenza, UC 0116) e inietta
   `tenant_id` + ruoli come claim custom. Garantisce l'invariante "tenant_id solo dal JWT verificato".
   _Naming dei claim e meccanica Cognito: dettaglio in [02-auth-sicurezza](02-auth-sicurezza.md)._

### Routing & API surface (topic E, F)
6. **Unica API Gateway v2 (HTTP API)** condivisa, routing **path-based** `/api/<app_id>/v1/*`;
   il core su `/api/platform/v1/*`. **Authorizer centralizzato** (custom Lambda: JWT + entitlement →
   [02-auth-sicurezza](02-auth-sicurezza.md) §8 / [04-services-backend](04-services-backend.md) §7).
7. **Nessuna comunicazione service-to-service** nel PoC: ogni app è un verticale isolato.
12. **Versioning nel path**: `/api/<app_id>/v1/...`. Evoluzione a `v2` senza rompere i client.
13. **Formato errori = RFC 9457** `application/problem+json` (type/title/status/detail/instance +
    eventuali campi custom), uniforme su tutti i servizi. Auth via header `Authorization: Bearer <jwt>`.

### App Registry & contratto shell ↔ app (topic D, F)
10. **App Registry ibrido**: il frontend ha la mappa build-time dei moduli esistenti (`app_id → import lazy`);
    il core fornisce gli **entitlement** del tenant; la sidebar mostra l'**intersezione**.
11. **Contratto shell ↔ modulo (React-native)**: ogni app è un **componente React lazy**; la shell le passa
    un **contesto** via React Context — token getter, `tenant_id`, `user_id`, ruoli, theme, API di navigazione.
    Il modulo **non** gestisce auth, **non** legge `tenant_id` fuori dal contesto, usa l'**API client condiviso**.
    Nessuna macchinaria microfrontend ora; l'eventuale estrazione futura tocca solo la entry del registry.

### Naming & identificatori (topic G)
14. **`app_id`** = identificatore canonico, kebab-case, breve, stabile (`notes`, `mini-crm`). Usato in
    path API, registry frontend, entitlement, cartella servizio. **`app_name`** = solo display (`"Mini CRM"`).
15. **Cartella servizio** `services/<app_id>/`. **Path API** `/api/<app_id>/v1/...` (core `/api/platform/v1/...`).
16. **DB schema** `app_<app_id>` con `-`→`_` (Postgres snake_case), es. `app_mini_crm`. **Core schema** = `platform`.
17. **Chiavi di logging** standard: `tenant_id`, `app_id`, `user_id` (l'invariante #4 usa `app_id` come valore;
    `app_name` resta display-only). **Claim JWT**: `tenant_id`, `roles` (namespace/meccanica → 02).

## Questioni aperte
- **Modello ruoli** (owner/admin/member tenant-level e/o per-app; platform admin) → risolto in
  [02-auth-sicurezza](02-auth-sicurezza.md). Tutto il resto di #01 è deciso.

## Alternative valutate / scartate
- **Tenant = singolo utente (B2C puro)** — scartato: non copre il caso org multi-utente (CRM). Il modello
  account/org con capability per-app generalizza entrambi.
- **Una API Gateway per app** — scartato: troppe risorse/costo per il PoC; path-based è cost-first.

## Impatti su altre aree
- [02-auth-sicurezza](02-auth-sicurezza.md), [04-services-backend](04-services-backend.md), [05-persistenza-dati](05-persistenza-dati.md), [06-infra-iac](06-infra-iac.md)
