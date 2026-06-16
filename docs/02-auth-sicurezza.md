# Auth & sicurezza — Decisioni

**Stato**: 🟢 deciso
**Ultimo aggiornamento**: 2026-06-14

## Scope
Meccanica di autenticazione e autorizzazione: Cognito, flusso OAuth e gestione token nella SPA,
Pre-Token-Generation Lambda e naming dei claim, modello ruoli/authz, verifica JWT nei servizi,
enforcement dell'isolamento tenant, signup/inviti, secrets, CORS. Non copre la forma architetturale
(→ [01-architettura](01-architettura.md)) né i dettagli di networking/IaC (→ [06-infra-iac](06-infra-iac.md)).

## Vincoli ereditati da #01 (già decisi)
- Cognito = **solo autenticazione** (identity provider); membership/ruoli nel **core service** (DB).
- **1 utente → 1 tenant**; `tenant_id` = account id, `sub` = user_id.
- **Pre-Token-Generation Lambda** legge la membership dal core e inietta `tenant_id` + ruoli come claim.
- Authorizer Cognito **centralizzato** su API Gateway; invariante: `tenant_id` solo dal JWT verificato.

## Topic dell'area (agenda)
- **A. Setup Cognito** — User Pool, app client SPA (public, senza secret), domain, MFA, password policy.
- **B. Login UX & flusso OAuth** — Hosted UI vs login custom; Authorization Code + PKCE; storage token nella SPA; refresh; logout.
- **C. Pre-Token-Gen Lambda** — meccanica iniezione claim, naming/namespace (`tenant_id`, `roles`), come la Lambda legge il core (DB diretto vs API interna).
- **D. Modello ruoli & authz** (da #01) — set ruoli (owner/admin/member), tenant-level vs per-app; platform admin; dove si applica l'authz (API GW = authn, servizio = authz).
- **E. Verifica JWT nei servizi** — Quarkus OIDC, issuer/audience/JWKS.
- **F. Enforcement isolamento tenant** — come si garantisce `WHERE tenant_id` (Hibernate filter/interceptor/repo base). Border con #04/#05.
- **G. Signup & inviti** — self-signup crea account+owner; token d'invito (scadenza, single-use) e accept flow, sotto il vincolo 1 utente→1 tenant.
- **H. Secrets** — webhook Paddle, credenziali Lambda→core; dove (Secrets Manager/SSM). Border con #12.
- **I. CORS** — origin ammessi (CloudFront), config API Gateway.

## Decisioni prese

### Login & flusso (topic B)
1. **Login custom in React** (no Hosted UI): schermate di login/signup/reset dentro la shell.
2. **Pattern mini-BFF via Lambda**: l'autenticazione avviene **server-side** in una **auth Lambda**
   dietro API Gateway (il form posta le credenziali alla Lambda su TLS, non SRP nel browser). Route:
   - `POST /api/auth/login` → Cognito `InitiateAuth`; set refresh token in cookie `HttpOnly`+`Secure`+`SameSite`; ritorna access/id nel body.
   - `POST /api/auth/refresh` → legge il cookie; `REFRESH_TOKEN_AUTH`; ritorna nuovi access/id; **ruota** il cookie.
   - `POST /api/auth/logout` → cancella il cookie + Cognito `RevokeToken`.
3. **Storage token**: access/ID **in-memory** nella SPA; **refresh token in cookie `HttpOnly`** (mai in JS).
   Al reload la SPA chiama `/api/auth/refresh` → sessione ripristinata senza esporre il refresh token.
4. **App client Cognito confidenziale (con secret)**, possibile perché l'auth è server-side; secret in
   Secrets Manager (→ topic H / #12).
5. **TTL**: access/ID = **15 min**; refresh token = **24 h**, con rotazione a ogni refresh.
6. **Vincolo dominio (→ #06)**: frontend e API sotto lo stesso dominio registrabile (es. `app.appgrove.app`
   + `api.appgrove.app`, cookie su `.appgrove.app`) per avere il cookie **first-party**.

### Modello ruoli & authz (topic D, da #01)
7. **Ruoli tenant-level**: `owner`, `admin`, `member` (nel claim `roles`). **`platform-admin`** separato
   (livello piattaforma) per il backoffice admin.
8. **Divisione delle responsabilità**: l'**API Gateway** usa un **custom Lambda authorizer** che verifica il
   **JWT** *e* l'**entitlement** (tenant ha l'`app_id` del path attivo — vedi [04-services-backend](04-services-backend.md) §7);
   il **servizio** ri-valida il JWT (Quarkus OIDC) e fa l'**authz sui ruoli** (`@RolesAllowed`). Difesa in profondità.

### Pre-Token-Gen Lambda (topic C)
9. **Lettura DB diretta**: la Lambda (in VPC) interroga lo schema `platform` del DB core per membership e
   ruoli; credenziali in Secrets Manager. Meno hop nel path critico del login. (Accoppiamento accettato per il PoC.)
10. **Claim iniettati**: `tenant_id` (string) e `roles` (array). Quarkus mappa l'authz con
    `quarkus.oidc.roles.role-claim-path=roles`. **Fail-closed**: utente senza tenant/membership valida → niente claim → accesso negato.

### Verifica JWT nei servizi (topic E)
11. **Quarkus OIDC**: issuer = User Pool Cognito, verifica firma via **JWKS**, audience = app client.
    Si usa l'**access token** per l'authz; `tenant_id`/`roles` letti dai claim verificati.

### Isolamento tenant (topic F)
12. **Hibernate multitenancy `DISCRIMINATOR`** + **`TenantResolver`** request-scoped che legge `tenant_id`
    dal JWT: Hibernate aggiunge il filtro tenant a **ogni** query automaticamente. **Fail-closed** se manca
    il tenant. Rende esecutive le invarianti #1/#2 senza `WHERE` manuale.

### Signup & inviti (topic G)
13. **Signup self-service aperto**: nuovo utente → **nuovo account (tenant) + membership owner**; email
    verification via Cognito.
14. **Flusso inviti nel PoC** (una delle due app demo è B2B/multi-utente): owner/admin invita una email →
    **invitation** con token **single-use** e **scadenza** (default 7 giorni) → all'accept l'invitato è creato
    **dentro il tenant che invita** con ruolo assegnato (vincolo 1 utente→1 tenant).

### Secrets (topic H, dettaglio store → #12)
15. **Zero secret nel codice o in file committati**; tutti in AWS, iniettati a runtime. Per l'auth:
    app client secret Cognito, credenziali DB per la Lambda→core, signing secret webhook Paddle.
    Store (risolto in [12-environments-config](12-environments-config.md)): **SSM Parameter Store** per config/secret
    applicativi (app client secret, signing webhook Paddle); **Secrets Manager** solo per le credenziali DB.

### CORS & cookie (topic I, border con #06)
16. **API Gateway CORS**: origin = dominio frontend esplicito (es. `https://app.appgrove.app`),
    `Access-Control-Allow-Credentials: true`, **niente wildcard `*`** (incompatibile con credentials).
17. **Cookie refresh**: **host-only sull'host dell'API** (es. `api.appgrove.app`, **nessun** attributo `Domain`),
    `Secure`, `HttpOnly`, `SameSite=Lax`, **`Path=/api/auth`**. Richiede `app.*` e `api.*` sotto lo stesso
    registrable domain (`appgrove.app`) perché Lax lo invii nella fetch cross-sottodominio. Host-only →
    **isolamento automatico tra ambienti** (il cookie di prod non raggiunge test). Schema domini → [12-environments-config](12-environments-config.md).

## Questioni aperte
_Nessuna — #02 chiuso._

## Scope PoC (nota cross-area)
- **Una delle due app demo deve essere B2B/multi-utente** (l'altra single-user) per validare end-to-end
  inviti, membership e ruoli. Aggiorna la roadmap del [recap](../recap_marketplace_microsaas.md) (App1 note,
  App2 dashboard erano entrambe single-user). Dettaglio app → [03-frontend](03-frontend.md)/[04-services-backend](04-services-backend.md).

## Alternative valutate / scartate
- **Hosted UI** — scartata (per ora): si preferisce controllo totale su UX/branding col login custom.
- **localStorage / sessionStorage per i token** — scartati: il refresh token sta in cookie `HttpOnly` (no JS).
- **SRP nel browser** — scartato: per avere il cookie `HttpOnly` l'auth è server-side nella Lambda.
- **App client public** — scartato: con auth server-side si usa un client confidenziale (con secret).

## Impatti su altre aree
- [01-architettura](01-architettura.md), [04-services-backend](04-services-backend.md), [05-persistenza-dati](05-persistenza-dati.md), [06-infra-iac](06-infra-iac.md), [12-environments-config](12-environments-config.md)
