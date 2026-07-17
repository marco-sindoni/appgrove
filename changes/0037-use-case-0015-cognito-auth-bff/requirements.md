# Change 0037: Cognito + auth BFF (login/refresh/logout cloud, cookie HttpOnly, CORS)

**Branch**: `change/0037-use-case-0015-cognito-auth-bff`
**Aree**: `services/auth` (rename da `auth-local` + refactor porta provider + provider Cognito + artefatto Lambda) · `infra/` (Cognito, Lambda, API Gateway, SSM, output) · `.github/workflows/` (build/deploy artefatto Lambda nativo) · script dev locali (rename meccanico) · docs (indice use case, punti differiti)
**Data**: 2026-07-17
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/05-auth/0015-cognito-auth-bff.md](../../docs/usecases/05-auth/0015-cognito-auth-bff.md)
**Tocca dati personali?**: Sì — email e credenziali degli utenti transitano dal BFF verso Cognito (nuovo responsabile del trattamento lato cloud). Si applica il gate privacy/RoPA di step-03 (scanner UC 0031 + classificazione MAJOR/MINOR).

## Problema / Obiettivo

Oggi l'autenticazione esiste solo in locale (`auth-local`, UC 0010/0058): in test/prod non c'è alcun
identity provider né BFF (backend di autenticazione dietro API Gateway). Questa change implementa
UC 0015: **Cognito per ambiente (test/prod) + auth Lambda** che espone `/api/auth/*` server-side con
refresh token in cookie HttpOnly, CORS con credenziali senza wildcard e throttling — con **contratto
identico al provider locale**, così le UI già implementate (UC 0017) funzionano invariate in cloud.

## Scope

**Decisioni prese nel gate di chiarimento** (dialogo con lo sviluppatore):
1. **Contratto completo** (opzione a): la Lambda implementa *tutti* gli endpoint del provider locale
   (login, login/2fa, refresh, logout, jwks*, signup, verify, verify/resend, password/forgot,
   password/reset, invitations/accept, invitations/send, 2fa/enroll, 2fa/verify), non solo
   login/refresh/logout — è il DoD 4 di UC 0015 e nessun altro UC possiede gli endpoint restanti.
   (*`jwks` in cloud: i servizi validano contro il JWKS Cognito; l'endpoint locale resta per `%dev`.)
2. **Servizio unico con porta provider** (opzione a1): si rifattorizza `auth-local` estraendo
   l'interfaccia `IdentityProvider` (design già prescritto da UC 0010 §6) con due implementazioni —
   `Local` (Postgres, profilo `%dev`, comportamento invariato) e `Cognito` (test/prod). Lo strato
   HTTP (route, validazione, cookie, errori problem+json) è **uno solo e condiviso**.
3. **Rename `services/auth-local` → `services/auth`** (un nome che finisce in prod non può chiamarsi
   "-local"): aggiornamento meccanico di pom padre, `app-start.sh`/`app-stop.sh`, helper `dev/lib`,
   commenti Caddyfile/compose. Route locale `/api/auth/*` → `:9100` invariata.

**Lavori:**
- **`services/auth`**: rename + estrazione `IdentityProvider`; implementazione `Cognito`
  (`InitiateAuth`, `RespondToAuthChallenge` per `SOFTWARE_TOKEN_MFA`, `REFRESH_TOKEN_AUTH` con
  rotazione cookie, `RevokeToken`, `SignUp`/`ConfirmSignUp`/resend, `ForgotPassword`/
  `ConfirmForgotPassword`, `AssociateSoftwareToken`/`VerifySoftwareToken`/`SetUserMFAPreference`,
  creazione utente già confermato per accept invito). Scritture sullo schema `platform` per gli
  inviti (parità con `PlatformWriter`) via RDS Proxy. Cookie refresh host-only
  `Secure`/`HttpOnly`/`SameSite=Lax`/`Path=/api/auth`; TTL access/id 15 min, refresh 24 h.
  Secret app client letto da SSM a runtime (zero secret nel codice, #02 15).
- **Artefatto Lambda nativo**: pacchettizzazione `quarkus-amazon-lambda-http`, build **nativa
  GraalVM ARM64** (zip con `bootstrap`). Il profilo `%dev` resta l'app HTTP normale; il codice
  solo-locale (Flyway credenziali, Mailpit, bypass 2FA) è escluso dal profilo cloud.
- **`infra/`**: User Pool Cognito per env (test/prod — niente pool `dev`: in locale l'auth è emulata,
  UC 0010) con password policy #02 19, MFA TOTP opzionale, lockout built-in; **app client
  confidenziale** con secret in SSM; auth Lambda in VPC (VPC endpoint Cognito-IDP se non già
  presente da UC 0003); route API Gateway `POST /api/auth/*` → Lambda con **throttling 10 req/s,
  burst 20**; **CORS** con `Allow-Credentials` e origin espliciti per env (mai `*`);
  valorizzazione dei campi `cognito.userPoolId`/`clientId` in `spa_config` dagli output reali
  (punto differito di UC 0005, proprietario UC 0015).
- **Pipeline (`.github/workflows/`)**: build dell'artefatto Lambda nativo in `deploy-test`,
  promozione stesso artefatto in `release-prod`, aggancio del deploy Lambda (coerente col pattern
  wrapper/one-shot esistente). **Ogni pezzo di cablaggio build/deploy non realizzabile in questa
  change va tracciato esplicitamente come rimando** (richiesta esplicita dello sviluppatore).

## Fuori scope

- **Iniezione claim `tenant_id`/`roles`** (Pre-Token-Gen Lambda) → UC 0016.
- **Custom Lambda authorizer** (JWT + entitlement all'edge) → UC 0014.
- **Email localizzate** (Custom Message Lambda EN/IT, template SES) → UC 0018: qui le email Cognito
  usano i template di default.
- **UI** → UC 0017 (già implementata; non si tocca il frontend, salvo eventuale consumo config).
- **Attivazione ambienti / prima run live** → `docs/_BACKLOG.md`: questa change è solo codice +
  `terraform plan`; nessun `apply`, lo smoke su Cognito reale avverrà all'attivazione di `test`.

## Criteri di accettazione

- [ ] `services/auth` (rinominato) espone il contratto completo con `IdentityProvider` a due
      implementazioni; la suite integration esistente del provider locale resta verde invariata.
- [ ] Implementazione `Cognito` coperta da test (Cognito mockato): login, challenge 2FA, refresh con
      rotazione, logout con revoca, signup/verify, reset, accept/send invito, enroll 2FA; attributi
      cookie e fail-closed verificati.
- [ ] Terraform: User Pool + app client confidenziale + Lambda + route/throttling/CORS definiti;
      `fmt`/`validate`/`plan` puliti; `spa_config` valorizzato dagli output Cognito.
- [ ] Pipeline: artefatto Lambda nativo ARM64 costruito e cablato in deploy-test/release-prod (o
      rimandi tracciati dove il cablaggio dipende dall'attivazione ambienti).
- [ ] Punto differito UC 0005→0015 (`spa_config`) risolto e rimosso/aggiornato in UC 0015.

## Invarianti appgrove toccati

- **Tenant ID solo dal JWT verificato**: qui nasce il JWT cloud; i claim `tenant_id`/`roles` arrivano
  con UC 0016 — fail-closed a valle già garantito dai servizi (nessun claim → negato). La change non
  introduce alcun percorso che legga il tenant da body/params.
- **Filtro row-level**: le uniche query tenant-scoped sono le scritture inviti sullo schema
  `platform`, vincolate al tenant dell'invito (token single-use), stesso comportamento del locale.
- **Logging strutturato**: la Lambda logga JSON con `correlation_id`/`user_id` (mai credenziali,
  mai token); `tenant_id` quando disponibile dal contesto invito.
- **Modulo `microsaas_app`**: N/A (l'auth è piattaforma condivisa, non una app del marketplace).

## Requisiti di test

- **Integration** (`mvn test`, area backend di `run-tests.sh`): tutti i flussi del provider Cognito
  con SDK mockato; TTL e rotazione; challenge 2FA; errori Cognito → problem+json identici al locale.
- **Security**: attributi cookie esatti; CORS rifiuta origin non ammessi; il refresh token non
  compare mai nel body delle risposte.
- **Regression**: suite `auth-local` esistente verde dopo refactor+rename senza modifiche di merito.
- **Infra**: `terraform fmt -check`, `validate`, `plan` per `envs/test` e `envs/prod`; actionlint
  sui workflow toccati.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (contratto `/api/auth/*` invariato; rename interno al repo) |
| Contratto cross-area | Sì (servizio ↔ infra ↔ pipeline: artefatto Lambda e config SSM/Cognito) |
| Version bump | minor |
