# Implementation Log — Change 0012: Flussi auth UI (UC 0017)

**Branch**: `change/0012-use-case-0017-flussi-auth-ui`
**Aree**: frontend (`apps/backoffice`; +chiavi `packages/i18n`)
**Completata**: 2026-06-26

## File modificati

| File | Azione |
|---|---|
| `frontend/packages/i18n/src/resources/en.ts`, `it.ts` | Modificato — namespace auth (signup/login/verify/forgot/reset/accept/2fa/errors/common) EN+IT |
| `frontend/apps/backoffice/src/auth/authApi.ts` | Modificato — signup/verify/resend/login(+mfa)/login2fa/forgot/reset/acceptInvite/enroll2fa/verify2fa |
| `frontend/apps/backoffice/src/auth/schemas.ts`, `authErrors.ts` | Creato — schemi Zod (policy/Bean Validation) + mapper problem+json→messaggio |
| `frontend/apps/backoffice/src/pages/auth/*` | Creato — AuthLayout, Field, LoginPage(+2FA), OnboardingWizard, VerifyEmailPage, ForgotPasswordPage, ResetPasswordPage, AcceptInvitePage (+ test) |
| `frontend/apps/backoffice/src/pages/SecurityPage.tsx` (+test) | Creato — setup 2FA (enroll/verify + QR) |
| `frontend/apps/backoffice/src/shell/TwoFaNudge.tsx` | Creato — banner nudge 2FA dismissibile |
| `frontend/apps/backoffice/src/shell/ShellLayout.tsx`, `Topbar.tsx` | Modificato — nudge banner + link "Sicurezza" nel menu utente |
| `frontend/apps/backoffice/src/routing/routes.tsx` | Modificato — route pubbliche auth + `/security`; rimosso placeholder `/login` |
| `frontend/apps/backoffice/src/pages/Login.tsx` | Eliminato — sostituito da `pages/auth/LoginPage.tsx` |
| `frontend/apps/backoffice/e2e/auth.spec.ts` | Creato — E2E login + signup wizard |
| `frontend/apps/backoffice/package.json` | Modificato — dep `qrcode.react` |
| `docs/usecases/06-frontend/0059-…md` | Creato — nuovo UC (membri/inviti) |
| `docs/usecases/README.md`, `_INDEX.md`, `CLAUDE.md` | Modificato — registrazione UC 0059 (catalogo 59), 0017→✅ |
| `docs/usecases/05-auth/0017-…md`, `03-local-dev/0058-…md` | Modificato — sezioni "Punti aperti / decisioni differite" |
| `app-start.sh`, `app-stop.sh`, `.gitignore` | Creato/Modificato — avvio/stop dell'intero stack locale (auth-local+core+SPA) per la verifica manuale; `dev/.run/` ignorato |
| `dev/Caddyfile` | Modificato — single-origin: blocco `app.local` attivato (SPA :5173 + `/api/auth/*` :9100) + rotta `/api/platform/* → core :8080` |
| `frontend/apps/backoffice/public/config.json` | Modificato — `authBaseUrl`/`coreBaseUrl` same-origin `https://app.local.appgrove.app` |
| `frontend/apps/backoffice/vite.config.ts` | Modificato — `server.host:true` + `allowedHosts` per il proxy Caddy |

## Cosa è stato fatto

Schermate dei flussi auth del backoffice (UC1–UC10) sopra la shell (UC 0020), cablate su `/api/auth/*` (auth-local in
locale; auth BFF/Cognito in cloud — stesso contratto). Onboarding **wizard coeso** (Opzione A) su `/signup`
(Account→Verifica→Workspace→Done); login con **variante challenge 2FA**; verifica email, reset (forgot+reset), accept
invitation (route dei link email `/verify|/reset|/accept` come da auth-local); **setup 2FA TOTP** (enroll/verify + QR)
dal profilo con banner di nudge. Errori RFC 9457 mappati in messaggi localizzati EN/IT; schemi **Zod** allineati a Bean
Validation + password policy.

## Decisioni prese

- **Onboarding = Opzione A** (gate domanda 2): wizard coeso a `/signup`; link email come pagine standalone.
- **Confine** (gate domanda 1): "Pick apps" fuori scope (manca #09); **invio inviti** scorporato nel **nuovo UC 0059**.
- **Lacune backend riscontrate** → tracciate in UC 0058: manca `/2fa/disable` (implementata solo l'abilitazione del 2FA)
  e manca l'esposizione di `totpEnabled` (banner nudge dismissibile, senza verità server).
- **`qrcode.react`** per il QR del 2FA (oltre al secret in chiaro per inserimento manuale).

## Invarianti appgrove

- **tenant_id/user_id solo dal JWT** — i flussi producono il token; la shell legge i claim solo dal token (UC 0020). Le
  schermate auth non leggono `tenant_id` da params/body.
- **Filtro row-level** — N/A lato client (enforcement backend). Frontend = solo UX.
- **Modulo `microsaas_app`** / **logging strutturato** — N/A (SPA statica, nessuna infra/log server-side).

## Note per il revisore

- **Contratto cross-area**: frontend ↔ auth-local (`/api/auth/*`) e ↔ core (`PATCH /accounts/me`). **Nessuna modifica ai
  servizi**: gli endpoint esistono già (UC 0010/0058/0013).
- **Decisioni differite tracciate** (regola costituzione): "Pick apps" (#09/0024), UI invio inviti → **nuovo UC 0059**
  (creato + registrato in README/_INDEX/CLAUDE), disattivazione 2FA + stato `totpEnabled` → **UC 0058**, specificità prod
  Cognito → ☁0015/0016/0018, testi email → UC 0017 (già noto). Vedi le sezioni "Punti aperti" di UC 0017/0058.
- **E2E visual baseline (#10 F)**: E2E con asserzioni `getByRole`/`getByLabel`, **nessuno snapshot visivo** → nessun
  baseline da ri-registrare.
- **Privacy/RoPA**: nessun nuovo trattamento (UI di dati già dichiarati in UC 0010/0013; password mai persistita).
- **Nota dev**: in locale `auth.local.totp-bypass=true` → il login non emette challenge 2FA; il percorso 2FA è comunque
  coperto da test (MSW/mocked) e dalla pagina Sicurezza (enroll/verify).

## Test

Tutte le suite delle aree toccate (solo `frontend/`) **verdi**.

- **`@appgrove/i18n`** (3): parità chiavi EN↔IT (include il nuovo namespace auth).
- **`@appgrove/backoffice`** (36, +16 vs change 0011): LoginPage (login, **2FA challenge**, credenziali errate, a11y),
  OnboardingWizard (signup→verifica, 409 email già registrata, policy password), AcceptInvitePage (accept, 410, token
  mancante), Forgot (risposta neutra), Reset (token mancante/valido), SecurityPage (enroll→verify, codice errato), oltre
  alle suite esistenti (registry/guards/interceptor/sidebar/smoke…).
- **E2E Playwright** (chromium, 4, **passati**): shell (2, change 0011) + **auth (2): login→dashboard; signup wizard
  account→verifica→workspace→done→dashboard**. Backend mockato via `page.route`.
- **Typecheck** `tsc` verde su tutti e 4 i workspace.

Esito `npm test` (root): api-client 6 ✓ · design-system 25 ✓ · i18n 3 ✓ · backoffice 36 ✓. Playwright 4 ✓.

## Stato criteri di accettazione

- [x] Signup+onboarding wizard (Account→Verifica→Workspace→Done); 409 gestito; Workspace rinomina account; nudge 2FA.
- [x] Verifica email (`/verify?token`) + resend (neutra).
- [x] Login + challenge 2FA (`/login/2fa`); errori UC9 da problem+json.
- [x] Reset (`/forgot` neutra + `/reset?token`).
- [x] Accept-invite (`/accept?token`) → auto-login; errori 400/410.
- [x] Setup 2FA (enroll→QR/secret→verify).
- [x] Schemi Zod ↔ Bean Validation + policy; errori problem+json mappati; EN/IT; a11y axe.
- [x] Test verdi (component + E2E + a11y) + typecheck su tutti i workspace.
- [x] Rinvii tracciati; **UC 0059** creato e registrato in README/_INDEX (catalogo → 59).
