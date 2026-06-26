# Change 0009: Provider auth locale — security-core (UC 0010)

**Branch**: `change/0009-use-case-0010-auth-locale`
**Aree**: services (`services/auth-local` nuovo modulo + `services/core` config `%dev`) + `dev/` (compose/Caddyfile/up/down/setup) + docs (`_INDEX`, README, UC 0010/0058)
**Data**: 2026-06-26
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/03-local-dev/0010-provider-auth-locale.md](../../docs/usecases/03-local-dev/0010-provider-auth-locale.md) (ristretto al security-core; flussi → [UC 0058](../../docs/usecases/03-local-dev/0058-flussi-auth-locali-completi.md))
**Tocca dati personali?**: **No** — legge utenti **sintetici** dal seed (UC 0011), non persiste nuovi dati personali; ambiente dev, email solo verso Mailpit (non in questo MVP). Manifest GDPR N/A.

## Problema / Obiettivo

Realizzare il **security-core** del provider auth locale che sostituisce Cognito + Pre-Token-Gen in dev: un servizio
`services/auth-local` che emette **JWT con lo stesso shape di prod** (claim dal DB), espone **JWKS** locale, gestisce
**login/refresh/logout** con refresh cookie, ed è **fail-closed**. Rende testabili offline gli **invarianti auth**
(`tenant_id` dal JWT verificato, claim dal DB) e sblocca shell (UC 0020) e flussi UI (UC 0017).

## Scope

### 1. Nuovo servizio `services/auth-local` (Quarkus, porta 9100)

- Modulo Maven sotto `services/` (parent), package `app.appgrove.authlocal`; dipende da `commons` per i mapper problem+json/MDC.
- **Lettura utenti via JDBC diretto** su `platform.users` (+ `accounts`): il login è **pre-tenant**, quindi NON usa l'entità `User` tenant-scoped del core (il discriminator richiederebbe un tenant già noto). Query per `email` (case-insensitive) → `cognito_sub`, `tenant_id`, `role`, `status`.

### 2. Endpoint `/api/auth/*`

- **`POST /api/auth/login`** `{email, password}`: valida la **password dev universale** (config `%dev`); cerca l'utente seedato `active`; in caso di esito negativo → **401** problem+json (no user / wrong password / inactive, messaggio generico anti-enumeration). Costruisce i claim **dal DB** e firma:
  - `sub` = `cognito_sub`, `tenant_id` = `accounts.id`, `groups` = `[role]` (+ `platform-admin` se il subject è tra `auth.local.platform-admin-subjects`, default `seed-platform-admin`), `roles` = mirror di `groups` (parità prod #02 dec.10), `iss` = `https://local.appgrove.app`;
  - **access token** (15 min) + **id token**; refresh token (24h) in **cookie HttpOnly** `SameSite=Lax`, `Path=/api/auth`. Body: `{access_token, id_token, token_type, expires_in}`.
- **`POST /api/auth/refresh`**: legge il cookie; valida il refresh (firma/exp/tipo); ri-legge l'utente per `sub` (ruoli freschi); riemette access/id e **ruota** il cookie. Cookie assente/non valido/scaduto → **401** (fail-closed).
- **`POST /api/auth/logout`**: cancella il cookie (`Max-Age=0`) → **204**.
- **`GET /api/auth/jwks`**: JWKS (chiave pubblica RSA, `kid` coerente con l'header dei token firmati).

### 3. Firma JWT + chiavi

- **RSA keypair** PEM; il JWT è firmato con la privata (smallrye-jwt-build, `kid` nell'header), la pubblica è pubblicata in `/api/auth/jwks`.
- **`dev setup`** (step "6/8 Chiavi JWT locali", oggi stub) genera la coppia in `dev/auth/` (gitignored, mai segreti reali). I test del modulo usano chiavi PEM bundle in `src/test/resources` (come il core).

### 4. Validazione lato servizi (`services/core`, profilo `%dev`)

- Aggiungere `%dev.mp.jwt.verify.publickey.location=http://localhost:9100/api/auth/jwks` (issuer già `https://local.appgrove.app`): il core valida i token di `auth-local` con lo **stesso code path smallrye-jwt** usato nei test/prod — cambia solo la sorgente JWKS.

### 5. Aggancio allo stack dev (runbook)

- **`dev up`**: dopo lo stack, avvia `auth-local` come **processo host** su `:9100` (build del jar se assente, lancio in background con PID file `dev/.auth-local.pid`, log in `dev/.auth-local.log`); il proxy lo raggiunge via `host.docker.internal:9100`.
- **`dev down`**: arresta il processo `auth-local` (oltre allo stack).
- **`dev/Caddyfile`**: attiva la route `/api/auth/*` → `host.docker.internal:9100` (snippet `(api_routes)` + host `api.local.appgrove.app`).
- Il servizio commentato in `dev/docker-compose.yml` resta tale (modello **host-process** #11 §2, coerente col Caddyfile); annotato.

## Fuori scope (→ UC 0058, tracciato)

- **Signup + verifica email**, **accept invito**, **reset password**, **2FA TOTP + bypass dev**, **email Mailpit** → **UC 0058** (scorporato, riga 10 dell'indice).
- **Store credenziali per-utente** (password hash) → arriva con il signup in UC 0058; qui password dev universale.
- **BFF auth reale + selezione provider per profilo (Local vs Cognito)** → **UC 0015** (cloud).
- **UI dei flussi** (UC 0017), **E2E auth** (suite dedicata), **Cognito/Pre-Token-Gen reali** (UC 0015/0016).
- **Supervisione host-process generica** (`dev service <app>` multi-app) → **UC 0046**; qui lancio specifico di `auth-local`.

## Criteri di accettazione

- [ ] `services/auth-local` compila nel reactor; `POST /api/auth/login` su utente seedato → 200 con access/id token firmati e refresh cookie HttpOnly `Path=/api/auth`.
- [ ] I claim sono **dal DB**: `sub`=cognito_sub, `tenant_id`=account id, `groups`/`roles`=[role] (+`platform-admin` per il subject piattaforma del seed).
- [ ] `/api/auth/jwks` espone la chiave pubblica; un token emesso è **verificabile** contro quel JWKS (prova che il `core` lo accetterebbe).
- [ ] `/refresh` ruota il cookie; `/logout` lo cancella; **fail-closed** su token assente/scaduto/forgiato e su credenziali errate (401).
- [ ] `dev setup` genera le chiavi in `dev/auth/`; `dev up` avvia `auth-local` su :9100 e il Caddyfile instrada `/api/auth/*`; `core` `%dev` punta al JWKS locale.
- [ ] Flussi rimanenti tracciati in UC 0058; UC 0010 marcato ✅ (DoD security-core completa).

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT**: `auth-local` **emette** `tenant_id` nel JWT leggendolo dal DB (replica Pre-Token-Gen); i servizi lo consumano solo dal token verificato. Il login pre-tenant legge `users` via JDBC senza esporre `tenant_id` ad input client.
- **Filtro row-level**: invariato lato servizi (discriminator core); `auth-local` non è tenant-scoped (autentica, non serve dati tenant).
- **Logging strutturato**: `auth-local` riusa `MdcRequestFilter` (commons) per `request_id`; `tenant_id`/`user_id` loggati post-autenticazione dove disponibili.
- **Modulo `microsaas_app`**: N/A (nessuna infra cloud).

## Requisiti di test

- **Integration (`services/auth-local`, Testcontainers + seed)**: login OK (claim corretti: sub/tenant_id/groups, `platform-admin` per il subject piattaforma); login KO (password errata / utente assente / inattivo → 401 generico); **round-trip JWKS** (token emesso verificato contro `/jwks` ⇒ accettabile dal core); refresh rotazione; refresh fail-closed (cookie assente/forgiato/scaduto); logout azzera il cookie.
- **Regression core**: la suite `services/core` resta verde (nessuna modifica a runtime, solo config `%dev`).
- **Script `dev/`**: nessun framework bash; comportamento coperto dagli integration test + verifica manuale del runbook (`dev up` → curl login/jwks), annotata nel log.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (nuovo servizio + config `%dev`) |
| Contratto cross-area | Sì — nuove API `/api/auth/*` e **shape JWT** consumati da core (`%dev`) e dalle future UI (UC 0017/0020) |
| Version bump | minor (nuovo servizio auth-local) |
