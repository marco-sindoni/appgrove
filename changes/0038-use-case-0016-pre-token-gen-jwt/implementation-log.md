# Implementation Log — Change 0038: Pre-Token-Gen Lambda + validazione JWT (UC 0016)

**Branch**: `change/0038-use-case-0016-pre-token-gen-jwt`
**Aree**: `infra/` · `services/commons` · `services/auth` · `services/core` · `services/fatture` · `docs/`
**Completata**: 2026-07-18

## File modificati

| File | Azione |
|---|---|
| `infra/modules/platform_shared/lambda/pre_token_gen/handler.py` | Creato — Lambda Pre-Token-Gen (Python): legge `platform.users` via proxy RDS (pg8000), inietta `tenant_id`+`roles` nell'access token, fail-closed |
| `infra/modules/platform_shared/lambda/pre_token_gen/vendor/**` | Creato — `pg8000`+`scramp`+`asn1crypto` vendorizzati (driver puro-Python, ~1.4MB) |
| `infra/modules/platform_shared/lambda/pre_token_gen/test_handler.py` | Creato — 6 test unitari (iniezione, fail-closed, platform-admin) |
| `infra/modules/platform_shared/lambda/db_bootstrap.py` | Modificato — nuova **modalità grant su schema altrui** (ruolo `auth_lambdas` least-privilege) |
| `infra/modules/platform_shared/lambda/test_db_bootstrap.py` | Creato — 4 test (modalità servizio + grant) |
| `infra/modules/platform_shared/pre_token_gen.tf` | Creato — Lambda in VPC, SG dedicato, IAM, permission Cognito |
| `infra/modules/platform_shared/auth_db_role.tf` | Creato — secret + password del ruolo DB dedicato `auth_lambdas` |
| `infra/modules/platform_shared/auth.tf` | Modificato — `user_pool_tier=ESSENTIALS` + trigger `pre_token_generation_config` V2_0 |
| `infra/modules/platform_shared/rds_proxy.tf` | Modificato — 2° secret agganciato al proxy + permesso di lettura |
| `infra/modules/platform_shared/auth_lambda.tf` | Modificato — BFF ripuntato al secret `auth_lambdas` (nessun codice) |
| `infra/modules/platform_shared/outputs.tf` | Modificato — output pre-token-gen, secret auth_lambdas, `cognito_issuer`/`cognito_jwks_url` |
| `infra/modules/microsaas_app/{variables,ecs}.tf` | Modificato — env JWT cloud dei servizi (issuer/JWKS/client-id atteso) |
| `infra/modules/observability/{variables,alarms}.tf` (+ test) | Modificato — allarme errori Lambda pre-token-gen |
| `infra/envs/{test,prod}/main.tf` | Modificato — invocazione grant `auth_lambdas` + wiring shared/observability |
| `infra/modules/microsaas_app/tests/**` | Modificato — fixture `shared` coi 3 campi Cognito |
| `services/commons/.../security/AccessTokenGuardFilter.java` | Creato — filtro condiviso: `token_use=access` + `client_id` atteso |
| `services/auth/.../local/TokenService.java` | Modificato — access token locale emette `client_id` (parità) |
| `services/core/.../platform/Roles.java` | Modificato — commento: ruoli dal claim `roles` |
| `services/{core,fatture}/.../application.properties` | Modificato — `smallrye.jwt.path.groups=roles` + `expected-client-id` |
| `services/{core,fatture}/.../TestTokens.java` | Modificato — token a specchio del prod (`roles`, `token_use`, `client_id`) + varianti "cattive" |
| `services/core/.../AccessTokenGuardTest.java` | Creato — 6 test sicurezza (id token, client_id, scaduto, malformato, senza token) |
| `run-tests.sh` | Modificato — area infra esegue anche i test Python delle Lambda |
| `.gitignore` | Modificato — `__pycache__/`, `*.pyc` |
| `docs/usecases/05-auth/0016-*.md`, `04-platform-core/0014-*.md`, `_INDEX.md` | Modificato — punti risolti/aperti, ✅ implementato |
| `docs/_EVOLUZIONI-DEVOPS.md`, `_COSTI-AWS.md` | Modificato — E23 aggiornata, piano Essentials |

## Cosa è stato fatto

Implementata la **Pre-Token-Generation Lambda** (Python, in VPC) che a ogni emissione token legge la
membership da `platform.users` via proxy RDS e inietta `tenant_id`+`roles` nell'**access token**, fail-closed
se manca membership attiva. Cognito passa al piano **ESSENTIALS** (prerequisito del trigger V2_0). I servizi
validano il JWT (smallrye-jwt, JWKS+emittente del pool) con **autorizzazione sul claim `roles`** e un filtro
condiviso che impone `token_use=access` e il `client_id` atteso. Le Lambda auth usano ora un **ruolo DB dedicato
least-privilege** (`auth_lambdas`) invece del master.

## Decisioni prese

1. **Runtime Python + `pg8000` vendorizzato** (scelta dello sviluppatore): cold-start leggero sul percorso di
   login, `archive_file` autocontenuto senza pipeline S3. Costo: ~1.4MB di dipendenze vendored nel repo.
2. **Ruolo DB dedicato = opzione B** (scelta dello sviluppatore): `db-bootstrap` esteso con modalità grant;
   l'invocazione grant vive nel **root env** (dopo `app_platform`, quando lo schema `platform` esiste) — non in
   `platform_shared`, che viene applicato prima. I grant si **riconciliano a ogni deploy** (input legato a
   `image_tag`) coprendo le tabelle di nuove migrazioni; evita `ALTER DEFAULT PRIVILEGES` e i suoi vincoli di
   proprietà.
3. **Access token (non ID token)** + `user_pool_tier=ESSENTIALS`: onora #02.11, gratuito sotto 10.000 utenti
   attivi/mese, e combacia col provider Local (che già emette i claim nell'access token).
4. **smallrye-jwt mantenuto** (non introdotto `quarkus-oidc`): coerente con tutti i servizi e con la parità
   locale; la verifica firma/JWKS+emittente soddisfa #02.11 nella sostanza.
5. **Verifica `client_id` + `token_use`** in un filtro condiviso in `commons` (non esisteva un componente di
   sicurezza comune): il provider Local ora emette `client_id` per la parità.

## Invarianti appgrove

- **`tenant_id` solo dal JWT verificato**: è il cuore della change — iniettato server-side dal Pre-Token-Gen,
  letto dai servizi solo dal token verificato; il filtro non introduce fonti alternative. Fail-closed conservato
  (Lambda senza claim; `JwtTenantResolver` nega).
- **Filtro row-level**: invariato — il resolver (UC 0012) continua a leggere `tenant_id` dal JWT.
- **Modulo `microsaas_app`**: la Lambda è piattaforma condivisa (`platform_shared`); il modulo per-app è toccato
  solo per iniettare la config JWT cloud comune a tutti i servizi.
- **Logging strutturato**: la Lambda emette log JSON con `user_id`/`tenant_id`, mai credenziali/token.

## Note per il revisore

- **Contratto cross-area servizio ↔ infra**: Cognito (via Pre-Token-Gen) emette i claim che i servizi validano;
  l'infra inietta issuer/JWKS/`client_id` nelle task definition. Nessun cambio al contratto `/api/*` verso la SPA.
- **Cloud mai acceso** (attivazione a fasi): tutta la parte cloud è verificata solo con test + `terraform
  fmt/validate/test`; la prima build/apply e la prova reale del trigger avverranno all'accensione.
- **Decisioni differite tracciate** (regola CLAUDE.md): → **UC 0014** (autenticazione IAM del proxy + stretta del
  security group del proxy; authorizer edge); **punti aperti** in UC 0016 (cold-start Aurora vs limite 5s di
  Cognito; allow-list `PLATFORM_ADMIN_SUBS` da popolare + eventuale tabella dedicata); **E23** aggiornata (parte
  "ruolo" realizzata qui). Nessun punto lasciato solo in chat.
- **Fuori dalla change**: `.claude/settings.json` (hook "rispondi in italiano") è nel working tree ma **estraneo**
  a UC 0016 — **da NON includere** in questo commit; va isolato a parte su `main`.
- **Gate privacy (UC 0031)**: `privacy-scan` → **nessun segnale**. Valutazione oltre il floor: nessun dato
  personale nuovo (la lettura di `platform.users` è trattamento account già censito), nessun sub-processor
  (`pg8000` è driver DB interno). Classificazione **MINOR**, nessuna modifica a manifesto/RoPA.

## Test

- **Lambda Python** (area infra di `run-tests.sh`): `test_handler` 6/6 (iniezione claim, fail-closed su assenza
  membership/`sub`, platform-admin da allow-list, forma della query); `test_db_bootstrap` 4/4 (modalità servizio +
  grant, rifiuto identificatori non validi). Verdi.
- **Backend**: `core` 165/165, `fatture` 36/36, `auth` 30/30, `commons` compilato — 0 fallimenti. Nuovo
  `AccessTokenGuardTest` (id token→401, client_id errato→401, scaduto→401, malformato→401, senza token→401/403,
  access valido→200). `AdminApiTest` fa da guardia sul match `roles` (owner→403, platform-admin→200).
- **Infra**: `terraform fmt/validate` test+prod puliti; `terraform test` moduli `microsaas_app` (5) e
  `observability` (2) verdi; actionlint verde.
- **`./run-tests.sh backend infra`**: **verde** su entrambe le aree.

## Stato criteri di accettazione

- [x] Pre-Token-Gen (Python, VPC) legge via proxy e inietta `tenant_id`+`roles`; fail-closed; test verdi.
- [x] Cognito `ESSENTIALS` + trigger V2_0 condizionale coerente; `fmt/validate` puliti; attivazione a fasi conservata.
- [x] Ruolo DB `auth_lambdas` least-privilege da `db-bootstrap`; secret al proxy; Pre-Token-Gen e BFF vi puntano; niente più master.
- [x] Servizi: `@RolesAllowed` su `roles`; config cloud issuer/JWKS presente; suite security verde (no tenant_id, forgiato/scaduto, override ignorato).
- [x] Verifica `token_use=access` **e** `client_id` atteso, coperta da test.
- [x] Parità: claim Local e attesi dalla Pre-Token-Gen → stesso comportamento nei servizi (stesso percorso).
- [x] Rimandi a UC 0014 / E23 / punti aperti tracciati prima della chiusura.
