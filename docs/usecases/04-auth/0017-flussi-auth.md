# UC 0017 — Flussi auth UI (signup/verify/login/reset/invite/2FA/onboarding)

**Area**: 04-auth · **Fase**: 2 · **Stato**: 🟢 deciso (flussi UC1–UC10 definiti; restano solo i testi dei template email)
**Dipendenze**: UC 0014 (Cognito + auth BFF), UC 0015 (Pre-Token-Gen + JWT), UC 0019 (backoffice shell)
**Fonte decisioni**: #02, #03, #05 · **Ultimo aggiornamento**: 2026-06-16
**Aree collegate**: [02-auth-sicurezza](../../02-auth-sicurezza.md), [03-frontend](../../03-frontend.md), [05-persistenza-dati](../../05-persistenza-dati.md)

> Storia: migrato da `docs/usecases/01-auth-registrazione.md`, rinumerato a **0017** nel catalogo per-area. I "flussi"
> dettagliati UC1–UC10 qui sotto sono i **sotto-flussi** di questo use case.

Specifica dei flussi utente di auth/registrazione (backoffice cliente). Per ogni caso d'uso: attori, precondizioni,
flusso principale, varianti/errori, postcondizioni, mappatura tecnica.

## Vincoli ereditati (già decisi)
- **Login custom** → **auth Lambda** (BFF): `POST /api/auth/login` / `refresh` / `logout`; access/id token in memoria,
  **refresh token in cookie HttpOnly**; al reload `refresh`.
- **Tenant = account**; **1 utente → 1 tenant**; `tenant_id` + `roles` iniettati dal **Pre-Token-Gen** nel JWT.
- **Signup self-service** crea **account + membership owner**; **inviti** (B2B multi-utente) con token single-use + scadenza.
- Ruoli **owner/admin/member** (+ `platform-admin` lato console admin). Errori **problem+json**. UI **EN+IT**.
- Design v1: presente un **onboarding wizard** (Welcome → Workspace → Pick apps → Done).

## Casi d'uso (agenda)
- **UC1 — Registrazione (signup self-service)**: nuovo utente → nuovo account, diventa owner.
- **UC2 — Verifica email**.
- **UC3 — Login**.
- **UC4 — Ripristino sessione** (refresh al reload).
- **UC5 — Logout**.
- **UC6 — Reset password** (forgot password).
- **UC7 — Accept invitation** (utente invitato in un tenant B2B).
- **UC8 — Onboarding** (post-signup: nome workspace, scelta app) — raccordo col wizard del design.
- **UC9 — Errori & edge**: email già registrata, token scaduto, credenziali errate, account/utente sospeso, email non verificata.
- **UC10 — Setup 2FA (TOTP authenticator)**: opzionale, dal profilo; banner di nudge nel backoffice. (UC3 ha la variante challenge 2FA.)

## Decisioni che plasmano i flussi (2026-06-16)
1. **Signup minimale dentro il wizard**: step Account (email+password) → **verifica email come step intermedio** →
   solo dopo si prosegue allo step Workspace. Niente form unico.
2. **Verifica email obbligatoria**, posizionata **a metà wizard** (dopo creazione utente, prima di Workspace/app).
3. **Accept-invite (B2B)**: il **link d'invito vale come verifica email**; l'invitato imposta solo la password ed entra come member.
4. **2FA TOTP (authenticator)**: **opzionale**, attivabile **dal profilo utente** nel backoffice; **banner** che ne suggerisce
   l'attivazione finché non impostata. **Non** al signup. Login con 2FA attiva → challenge TOTP (variante UC3).
5. **Password policy** (default): min 10 caratteri, almeno maiuscola+minuscola+numero (policy Cognito).
6. **Email via SES** con **template propri EN/IT** (verifica/reset/invito), lingua dal `locale` utente; le email Cognito
   passano da un **Custom Message Lambda trigger**. Invio da **`noreply@appgrove.app`** (dominio verificato + DKIM su Route53).
7. **Rate-limit/lockout**: **lockout integrato di Cognito** (~5 tentativi → blocco temporaneo con backoff) + **throttling
   API Gateway** su `/api/auth/*` (es. 10 req/s, burst 20). Cognito Advanced Security (adaptive) = evoluzione **E7**.

## Flussi dettagliati

### UC1 — Registrazione (signup) + UC8 Onboarding (wizard)
**Attore**: visitatore. **Pre**: non autenticato.
1. **Welcome** → "Create account".
2. **Account**: email + password (validazione policy). Submit → auth Lambda registra l'utente su **Cognito (unconfirmed)** e invia **email di verifica**.
3. **Verifica email (gate intermedio, UC2)**: l'utente inserisce il codice (o clicca il link) → utente **confirmed**. Non si prosegue finché non verificato. (resend disponibile)
4. Post-verifica la Lambda **logga** l'utente (token in memoria + cookie refresh): gli step seguenti sono autenticati.
5. **Workspace**: nome account → il **core** crea `account` (tenant) + `users` (owner, legato a `cognito_sub`).
6. **Pick apps**: selezione app iniziali → crea `subscription` (trialing/active) → **entitlement derivato** (#09 dec.12); billing/Paddle → #09.
7. **Done** → dashboard. Mostra **banner "Imposta 2FA"** (UC10).
**Post**: account+owner creati, sessione attiva. **Errori**: email già registrata → UC9; codice scaduto → resend.

### UC2 — Verifica email
Cognito `ConfirmSignUp` con codice; **resend** con cooldown; codice a scadenza. È lo step 3 di UC1 (e parte di reset/inviti dove serve).

### UC3 — Login
1. email+password → auth Lambda `InitiateAuth`.
2. **Se 2FA attiva** → Cognito risponde con challenge `SOFTWARE_TOKEN_MFA` → UI chiede il **codice TOTP** → `RespondToAuthChallenge`.
3. Successo → set **cookie refresh HttpOnly** + ritorna access/id (memoria).
**Errori (UC9)**: credenziali errate (messaggio generico + rate-limit/lockout); **email non verificata** → invita a verificare; **account/utente sospeso** → bloccato.

### UC4 — Ripristino sessione (reload)
Al load la SPA chiama `POST /api/auth/refresh` (cookie) → nuovi access/id. Cookie assente/scaduto → redirect a login.

### UC5 — Logout
`POST /api/auth/logout` → cancella cookie + Cognito `RevokeToken`; pulisce i token in memoria → redirect login.

### UC6 — Reset password (forgot)
Email → Cognito `ForgotPassword` (invia codice) → inserimento codice + nuova password → `ConfirmForgotPassword`. Errori: email inesistente (messaggio neutro per non rivelare), codice scaduto → resend.

### UC7 — Accept invitation (B2B)
**Pre**: owner/admin ha invitato `email`+`role` → `invitations` (token single-use, scadenza) + email con link.
1. L'invitato apre il link → form **imposta password**.
2. auth Lambda crea utente Cognito **già confirmed** (email provata dal link, decisione #3) + il **core** crea `users` nel **tenant invitante** con il ruolo + segna l'invito **accepted**.
3. Login automatico → entra nel workspace come member.
**Errori (UC9)**: token **scaduto/già usato** → messaggio + richiesta nuovo invito; **email ha già un account** (vincolo 1 utente→1 tenant) → usare un'altra email.

### UC9 — Errori & edge (trasversale)
Email già registrata (signup) → suggerisci login/reset; codice/token scaduto → resend/nuovo; credenziali errate → errore generico + lockout dopo N tentativi; account o utente **sospeso** → accesso negato; email non verificata al login → prompt verifica; tutti gli errori in formato **problem+json** lato API.

### UC10 — Setup 2FA (TOTP)
**Dal profilo** → "Abilita 2FA" → Cognito `AssociateSoftwareToken` → mostra **QR/secret** → l'utente scansiona nell'app authenticator → inserisce codice → `VerifySoftwareToken` → `SetUserMFAPreference` (TOTP). **Banner** nel backoffice finché non attivata. **Disattivazione** dal profilo (richiede ri-autenticazione). Una volta attiva → UC3 chiede il codice TOTP.

## Mappatura tecnica
Cognito (login custom via **auth Lambda**), **core DB** (`accounts`/`users`/`invitations`/`subscription`; **entitlement derivato**, #09 dec.12), **Pre-Token-Gen** inietta `tenant_id`+`roles`, errori **problem+json**, **template email Cognito in EN+IT** (localizzazione). 2FA via Cognito TOTP MFA.

## Questioni aperte
_Nessuna — blocco chiuso. Resta solo la stesura dei **testi** dei template email EN/IT (contenuto, non decisione)._
