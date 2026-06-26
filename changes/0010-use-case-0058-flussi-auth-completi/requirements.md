# Change 0010: Flussi auth locali completi (UC 0058)

**Branch**: `change/0010-use-case-0058-flussi-auth-completi`
**Aree**: services (`services/auth-local`) + `dev/` (.env.example/compose già con Mailpit) + docs (`_INDEX`, UC 0058/0018)
**Data**: 2026-06-26
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/03-local-dev/0058-flussi-auth-locali-completi.md](../../docs/usecases/03-local-dev/0058-flussi-auth-locali-completi.md)
**Tocca dati personali?**: **No** — ambiente **dev**, dati sintetici; in prod l'auth (password/PII) è in **Cognito**, non nel nostro DB. Le password locali sono comunque hashate (BCrypt). Manifest GDPR N/A (dev).

## Problema / Obiettivo

Completare il provider auth locale (`auth-local`, security-core di UC 0010) con i **flussi non-core**: **signup + verifica
email**, **accept invito**, **reset password**, **2FA TOTP** (+ bypass dev), con **email su Mailpit**. Emula in locale ciò
che in prod fanno Cognito + i Lambda, così l'intero percorso auth gira offline (fondamento degli E2E auth).

## Scope

### 1. Stato credenziali dev-only — schema `auth_local` (Flyway in auth-local)

Migration `auth-local/src/main/resources/db/migration/V1__auth_local_schema.sql` (schema **separato** `auth_local`, history propria): tabella `auth_local.credentials` (`cognito_sub` PK, `password_hash` BCrypt, `email_verified`, `totp_secret`, `totp_enabled`, audit). **Parità prod**: nessuna password nello schema `platform` (in prod stanno in Cognito). Login: se esiste la riga credenziali → verifica hash + `email_verified` + 2FA; **se manca** (utenti del seed) → **fallback** alla password dev universale (verificati, no 2FA).

### 2. Endpoint flussi (`services/auth-local`)

- **Signup**: `POST /api/auth/signup` `{email, password, displayName?}` → valida policy password (#02 dec.19: ≥10, maiuscola+minuscola+numero); rifiuta email già utente (409); crea **account+owner** in `platform` (JDBC, `tenant_id`=account id, `cognito_sub` generato) + riga credenziali (`email_verified=false`); invia **email di verifica** (Mailpit) con link/token; **201** (`verification_required`, nessun token).
- **Verifica email**: `POST /api/auth/verify` `{token}` → valida il token di verifica → `email_verified=true` → emette token (auto-login, UC1 step4). `POST /api/auth/verify/resend` `{email}` → reinvio (risposta neutra).
- **Reset password**: `POST /api/auth/password/forgot` `{email}` → email Mailpit con token (risposta **neutra** 202, no enumeration); `POST /api/auth/password/reset` `{token, password}` → aggiorna l'hash → 204.
- **Accept invito**: `POST /api/auth/invitations/accept` `{token, password, displayName?}` → trova `platform.invitations` per `token_hash`, **pending** e non scaduto (altrimenti 410/400); crea l'utente nel **tenant invitante** col ruolo dell'invito (`email_verified=true`, il link prova l'email) + credenziali; segna l'invito **accepted** (`accepted_user_id`); emette token (auto-login). `POST /api/auth/invitations/send` `{email, token}` → invia l'email d'invito (Mailpit).
- **2FA TOTP**: `POST /api/auth/2fa/enroll` (Bearer access token) → genera segreto + URI `otpauth://`; `POST /api/auth/2fa/verify` `{code}` (Bearer) → `totp_enabled=true`. Login con 2FA attivo (due passi): `login` → **200 `{mfa_required, challenge_token}`**; `POST /api/auth/login/2fa` `{challenge_token, code}` → token completi. **Bypass dev** `auth.local.totp-bypass` (`%dev` default on) salta la challenge.

### 3. Infrastruttura tecnica

- **Dipendenze** (auth-local): `quarkus-flyway` (schema `auth_local`), `quarkus-mailer` (→ Mailpit), `at.favre.lib:bcrypt` (hash), `dev.samstevens.totp` (TOTP) — versioni pinnate dove fuori BOM.
- **Email**: `quarkus-mailer` SMTP → Mailpit (`%dev`: `localhost:1025`, from `noreply@appgrove.app`); email **funzionali** (link/codice). Template **localizzati EN/IT** → UC 0018 (tracciato).
- **Token verifica/reset/mfa-challenge**: JWT firmati (stateless) con `token_use` dedicato e TTL brevi (verify 24h, reset 1h, challenge 5m), verificati con la chiave locale.
- **Verifica access token** negli endpoint 2FA (Bearer) via la chiave pubblica locale (jose4j), coerente col security-core.

## Fuori scope

- **Security-core** (login/refresh/logout/JWKS) → già UC 0010; qui si **estende** `login` (credenziali store + 2FA).
- **UI dei flussi** (wizard signup/onboarding, schermate, banner 2FA, copy) → **UC 0017**.
- **Template email localizzati EN/IT** + Custom Message Lambda → **UC 0018** (tracciato).
- **Cognito/Pre-Token-Gen reali** → UC 0015/0016. **Onboarding** (nome workspace/scelta app, UC8) → UC 0017.
- **Rate-limit/lockout** login (UC9) → non in questo MVP locale (tracciato a UC 0017/sicurezza se necessario).

## Criteri di accettazione

- [ ] Migration `auth_local` applicata (schema separato); login usa lo store credenziali con **fallback** dev per gli utenti seed (le suite di UC 0010 restano verdi).
- [ ] Signup crea account+owner+credenziali e invia email di verifica (Mailpit); verifica → `email_verified` + auto-login; email duplicata → 409; password debole → 400.
- [ ] Reset password: forgot invia email (neutra), reset aggiorna l'hash e consente il login con la nuova password.
- [ ] Accept invito: token seed → utente creato nel tenant invitante col ruolo, invito `accepted`; token scaduto/usato → errore.
- [ ] 2FA: enroll/verify abilita il TOTP; login con 2FA → challenge in due passi; codice errato → 401; bypass dev salta la challenge.

## Invarianti appgrove toccati

- **Tenant ID dal JWT**: signup/accept creano righe con `tenant_id`=account id; i token emessi portano `tenant_id`/`groups` dal DB (come UC 0010). Mai da input client.
- **Filtro row-level**: invariato lato servizi; le scritture di auth-local in `platform` (signup/accept) impostano `tenant_id` esplicito; lettura via JDBC pre-tenant.
- **Logging strutturato**: `MdcRequestFilter` (commons) — `request_id` su ogni log; nessun segreto/password loggato.
- **Modulo `microsaas_app`**: N/A.

## Requisiti di test

- **Integration (`services/auth-local`, Testcontainers + MockMailbox)**: signup→email→verify→login; duplicato (409)/password debole (400); forgot+reset (email neutra, nuova password valida); accept invito (token seed → utente nel tenant, invito accepted; token scaduto/usato → errore); 2FA enroll/verify + login challenge due passi (codice valido/errato) + bypass.
- **Regression**: suite UC 0010 (login/refresh/jwks) e core restano verdi (login fallback per utenti seed senza credenziali).
- **Security**: token verifica/reset/invito a scadenza; password hashate (mai in chiaro); risposte neutre anti-enumeration su forgot.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (estende auth-local; nuovo schema `auth_local` isolato) |
| Contratto cross-area | Sì — nuovi endpoint `/api/auth/*` consumati dalle UI (UC 0017) e dagli E2E |
| Version bump | minor (flussi auth locali completi) |
