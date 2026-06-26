# Implementation Log — Change 0009: Provider auth locale security-core (UC 0010)

**Branch**: `change/0009-use-case-0010-auth-locale`
**Aree**: services (`services/auth-local` nuovo + `services/core` `%dev`) + `dev/` (script/Caddyfile/gitignore) + docs (`_INDEX`, README, UC 0010/0058/0016)
**Completata**: 2026-06-26

## File modificati

| File | Azione |
|---|---|
| services/auth-local/** (pom + 7 classi main + 5 classi test + config + chiavi) | Creato |
| services/pom.xml | Modificato (modulo auth-local) |
| services/core/src/main/resources/application.properties | Modificato (`%dev` JWKS locale) |
| dev/lib/common.sh | Modificato (costanti + lifecycle auth-local) |
| dev/lib/up.sh, down.sh, setup.sh | Modificato (avvio/arresto/chiavi auth-local) |
| dev/Caddyfile | Modificato (route `/api/auth/*` → :9100) |
| .gitignore | Modificato (dev/auth/ + pid/log) |
| docs/usecases/_INDEX.md | Modificato (0010 ✅, 0058 inserito riga 10, rinumerazione) |
| docs/usecases/README.md, CLAUDE.md | Modificato (catalogo 58, 0010 ristretto) |
| docs/usecases/03-local-dev/0058-… | Creato (scorporo flussi auth) |
| docs/usecases/03-local-dev/0010-…, 05-auth/0016-… | Modificato (scope/decisioni differite) |

## Cosa è stato fatto

Nuovo servizio Quarkus **`services/auth-local`** (porta 9100) che emette JWT con lo **stesso shape di prod**: legge gli
utenti dallo schema `platform` via **JDBC diretto** (login pre-tenant, niente discriminator), firma access/id token con
chiave RSA locale (`kid`), espone il **JWKS** e gestisce **refresh cookie** HttpOnly con rotazione. Login con **password
dev universale** (config `%dev`); claim `sub`/`tenant_id`/`groups`+`roles` dal DB (+`platform-admin` per il subject
piattaforma del seed). Il **core** in `%dev` valida i token via JWKS locale (stesso code path smallrye-jwt). Agganciato
allo stack dev: `dev setup` genera le chiavi, `dev up` avvia auth-local come **processo host**, `dev down` lo ferma, il
Caddyfile instrada `/api/auth/*`.

## Decisioni prese

- **Servizio separato `auth-local` come processo host** (non container): coerente col Caddyfile (`host.docker.internal:9100`)
  e col modello ibrido #11 §2; avviato/fermato da `dev up`/`dev down` (PID file). La supervisione generica multi-servizio → UC 0046.
- **Login pre-tenant via JDBC** (non l'entità `User` tenant-scoped del core): il discriminator richiederebbe un tenant già
  noto, impossibile prima del login (replica la lettura DB del Pre-Token-Gen, #02 dec.9).
- **Password dev universale** (#02 dec.19) per gli utenti seedati: store credenziali per-utente → con il signup in UC 0058.
- **Claim `groups` + `roles`** entrambi emessi: `groups` per `@RolesAllowed` del core (com'è oggi), `roles` per parità prod;
  riconciliazione su un unico claim quando arriva OIDC reale → tracciata in UC 0016.
- **Firma self-contained** (smallrye-jwt-build + jose4j, niente quarkus-smallrye-jwt): nessuna verify-extension richiesta.

## Invarianti appgrove

- **Tenant ID dal JWT**: auth-local **emette** `tenant_id` leggendolo dal DB; i servizi lo consumano solo dal token
  verificato. Il login non accetta `tenant_id` da input. **Fail-closed** verificato (credenziali/refresh non validi → 401).
- **Filtro row-level**: invariato lato servizi (discriminator core); auth-local autentica, non serve dati tenant.
- **Logging strutturato**: auth-local riusa `MdcRequestFilter` (commons) → `request_id` su ogni log.
- **Modulo `microsaas_app`**: N/A (nessuna infra cloud).

## Note per il revisore

- **Contratto cross-area**: nuove API `/api/auth/*` + shape JWT, consumati dal core (`%dev`) e dalle future UI (UC 0017/0020).
- **Dati personali = No**: legge utenti **sintetici** del seed; nessun nuovo PII persistito; ambiente dev.
- **Decisioni differite tracciate**: flussi completi (signup/verifica/invito/reset/2FA/Mailpit) → **UC 0058** (scorporato,
  riga 10 dell'indice, riga `⬜` di prima classe); riconciliazione claim `groups`/`roles` → **UC 0016**; supervisione
  host-process generica → **UC 0046**; BFF + selezione provider per profilo → **UC 0015**.
- **Riassetto indice**: inserito UC 0058 alla riga 10, rinumerati i `#` (totale 58), aggiornata la dipendenza di UC 0017
  (+0058) e i riferimenti di riga dei traguardi.
- **Esecuzione**: test richiedono Docker (Testcontainers); runbook richiede `java` + `openssl` (chiavi) + lo stack su.

## Test

`services` (`mvn test`): **37 verdi** — commons 3, core 24 (nessuna regressione), **auth-local 10** (login+claim dal DB,
gruppo platform-admin, JWKS round-trip = verificabile dal core, refresh rotazione, fail-closed su credenziali/refresh,
logout). **BUILD SUCCESS**.

**Runbook reale verificato** (`./dev.sh seed` + `./dev.sh up`):
- `GET /api/auth/jwks` → kid `auth-local-dev`;
- login `owner@acme.test` → 200, claim `sub=seed-acme-owner`, `tenant_id=<Acme>`, `groups=[owner]`, cookie `appgrove_refresh`
  HttpOnly `Path=/api/auth` Secure SameSite=Lax;
- via proxy TLS `https://api.local.appgrove.app/api/auth/login` (HTTP/2) → 200; platform-admin → `groups=[owner, platform-admin]`;
- password errata → 401; `./dev.sh down` ferma auth-local + stack.

## Stato criteri di accettazione

- [x] `services/auth-local` compila/parte; login utente seedato → 200 con token firmati + refresh cookie HttpOnly Path=/api/auth.
- [x] Claim dal DB (`sub`/`tenant_id`/`groups`/`roles` + `platform-admin` per il subject piattaforma).
- [x] `/api/auth/jwks` espone la chiave; token verificabile contro il JWKS (accettabile dal core).
- [x] `/refresh` ruota il cookie; `/logout` lo cancella; fail-closed (401) su credenziali errate / refresh non valido.
- [x] `dev setup` genera le chiavi; `dev up` avvia auth-local; Caddyfile instrada `/api/auth/*`; core `%dev` → JWKS locale.
- [x] Flussi rimanenti tracciati in UC 0058; UC 0010 ✅ (DoD security-core completa).
