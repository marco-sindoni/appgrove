# Implementation Log — Change 0010: Flussi auth locali completi (UC 0058)

**Branch**: `change/0010-use-case-0058-flussi-auth-completi`
**Aree**: services (`services/auth-local`) + `dev/` (`common.sh`) + docs (`_INDEX`, UC 0018/0058)
**Completata**: 2026-06-26

## File modificati

| File | Azione |
|---|---|
| services/auth-local/pom.xml | Modificato (flyway, mailer, bcrypt, totp) |
| services/auth-local/src/main/resources/db/migration/V1__auth_local_schema.sql | Creato (schema dev-only `auth_local`) |
| services/auth-local/.../CredentialsRepository, PlatformWriter, Passwords, PasswordPolicy, TokenHashes, TotpService, EmailService, InvalidTokenException, InvalidTokenMapper | Creato |
| services/auth-local/.../AuthResource.java | Modificato (login esteso + signup/verify/reset/accept/2fa) |
| services/auth-local/.../AuthDtos.java, TokenService.java | Modificato (DTO flussi; token verify/reset/mfa + verifica generica) |
| services/auth-local/.../InvalidRefreshTokenException.java | Eliminato (→ InvalidTokenException) |
| services/auth-local/src/main/resources/application.properties | Modificato (flyway/mailer/2FA/mp.jwt verify) |
| services/auth-local/src/test/... (Flows, SignupVerify, ResetPassword, InviteAccept, TwoFactor) + test application.properties | Creato/Modificato |
| dev/lib/common.sh | Modificato (`-Dquarkus.profile=dev` per applicare i %dev) |
| docs/usecases/_INDEX.md | Modificato (0058 🟡→✅, "prossimo") |
| docs/usecases/05-auth/0018-… | Modificato (differita: template email EN/IT) |

## Cosa è stato fatto

Estensione del provider `auth-local` con i flussi non-core (UC 0058): **signup** (crea account+owner in `platform` via
JDBC) + **verifica email**, **reset password**, **accept invito** (token dell'invito → utente nel tenant invitante),
**2FA TOTP** (enroll/verify + login challenge in due passi con bypass dev), tutto con **email su Mailpit**. Lo stato
credenziali/2FA vive in uno **schema dev-only `auth_local`** (Flyway in auth-local, isolato da `platform` = parità Cognito).
Il login ora usa lo store credenziali (hash BCrypt + `email_verified` + 2FA) con **fallback** alla password dev per gli
utenti del seed. I token di verifica/reset/challenge sono JWT firmati a TTL breve; gli endpoint 2FA sono `@Authenticated`
(verifica del Bearer access token via mp.jwt con la chiave locale).

## Decisioni prese

- **Schema `auth_local` separato** (Flyway in auth-local, history propria): nessuna password in `platform` (in prod stanno
  in Cognito). Login con fallback password dev per gli utenti seed (nessuna riga credenziali).
- **2FA endpoint via `@Authenticated` + `JsonWebToken`**: il meccanismo smallrye-jwt (ereditato da `commons`) consuma il
  Bearer header, quindi @HeaderParam non lo vedeva → uso la verifica JWT integrata (mp.jwt con la chiave pubblica locale).
- **Host-process in profilo dev** (`-Dquarkus.profile=dev` in `common.sh`): senza, il jar girava in prod e i `%dev`
  (Flyway migrate `auth_local`, mailer→Mailpit, bypass 2FA) non si applicavano.
- **Email funzionali** (link/token); template localizzati EN/IT → UC 0018 (tracciato).

## Invarianti appgrove

- **Tenant ID dal JWT**: signup/accept scrivono righe con `tenant_id`=account id esplicito; i token emessi portano
  `tenant_id`/`groups` dal DB (come UC 0010); mai da input client.
- **Filtro row-level**: invariato lato servizi; auth-local scrive in `platform` con tenant esplicito, legge pre-tenant via JDBC.
- **Logging strutturato**: `MdcRequestFilter` (commons) — nessuna password/segreto loggato.
- **Modulo `microsaas_app`**: N/A.

## Note per il revisore

- **Contratto cross-area**: nuovi endpoint `/api/auth/*` (signup/verify/forgot/reset/invitations·accept|send/2fa·enroll|verify/login·2fa) consumati dalle UI (UC 0017) e dagli E2E.
- **Dati personali = No**: ambiente dev, dati sintetici; password locali comunque hashate (BCrypt). In prod l'auth è Cognito.
- **Decisioni differite tracciate**: template email localizzati EN/IT → **UC 0018**. (UI/onboarding/rate-limit → UC 0017; Cognito reale → UC 0015/0016, già fuori scope.)
- **Profilo dev del processo**: `dev up` ora lancia auth-local con `-Dquarkus.profile=dev` (necessario per i %dev).

## Test

`services` (`mvn test`, Testcontainers + MockMailbox): **47 verdi** — commons 3, core 24 (nessuna regressione),
**auth-local 20** (login fallback/credenziali, refresh, jwks, signup→verifica→login, duplicato/policy, forgot/reset neutro,
accept invito nel tenant + scaduto/invalido, 2FA enroll/verify + login challenge due passi + codice errato). **BUILD SUCCESS**.

**Runbook reale verificato** (`./dev.sh seed` + `./dev.sh up` + Mailpit): signup `runbook2@signup.test` → 201; email
"Verifica la tua email appgrove" catturata in Mailpit; verify (token estratto dall'email) → 200 (auto-login); login → 200;
`./dev.sh down` ferma tutto. Schema `auth_local` creato a runtime da Flyway (profilo dev).

## Stato criteri di accettazione

- [x] Migration `auth_local` applicata (schema separato); login con store credenziali + fallback dev (suite UC 0010 verdi).
- [x] Signup crea account+owner+credenziali e invia email di verifica (Mailpit); verifica → email_verified + auto-login; duplicato 409; policy 400.
- [x] Reset password: forgot neutro invia email; reset aggiorna l'hash e consente il login con la nuova password.
- [x] Accept invito: token seed → utente nel tenant invitante col ruolo, invito accepted; scaduto/invalido → errore.
- [x] 2FA: enroll/verify abilita TOTP; login con 2FA → challenge due passi; codice errato → 401; bypass dev.
