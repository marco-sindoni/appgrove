# Change 0012: Flussi auth UI (signup wizard, verify, login+2FA, reset, accept-invite, 2FA setup)

**Branch**: `change/0012-use-case-0017-flussi-auth-ui`
**Aree**: frontend (`apps/backoffice`; +chiavi `packages/i18n`)
**Data**: 2026-06-26
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/05-auth/0017-flussi-auth.md](../../docs/usecases/05-auth/0017-flussi-auth.md)
**Tocca dati personali?**: No (nessun **nuovo** trattamento). La UI raccoglie email/password/displayName e li
trasmette agli endpoint **già esistenti** (auth-local UC 0010/0058, core UC 0013); nessuna persistenza client
(password mai salvata; access/id in memoria, refresh in cookie HttpOnly). Nessuna nuova voce RoPA, nessun bump PP/ToS.

## Problema / Obiettivo

Realizzare le **schermate dei flussi di autenticazione** del backoffice cliente sopra la shell (UC 0020), cablate
sul contratto `/api/auth/*` (auth-local in locale; auth BFF/Cognito in cloud — stesso contratto, ☁0015/0016). Copre
i sotto-flussi UC1–UC10: signup+onboarding (wizard), verifica email, login con variante 2FA, ripristino sessione,
logout, reset password, accept invitation, setup 2FA TOTP, ed errori/edge trasversali (problem+json), EN/IT.

## Scope

Area unica: **frontend** (`apps/backoffice`). Nessuna modifica ai servizi (gli endpoint esistono già).

**Data layer auth** (estende `src/auth/authApi.ts`): funzioni tipizzate per `signup`, `verify`, `verify/resend`,
`login` (ritorna `TokenResponse | MfaChallenge`), `login/2fa`, `password/forgot`, `password/reset`,
`invitations/accept`, `2fa/enroll`, `2fa/verify` (Bearer). Errori → `ApiError` (riuso `toApiError`/problem+json da
`@appgrove/api-client`). Schemi **Zod** allineati alle Bean Validation (`@Email`, `@NotBlank`) e alla **password
policy** (min 10, ≥1 maiuscola, ≥1 minuscola, ≥1 cifra).

**Onboarding wizard (Opzione A — wizard coeso)**: componente `OnboardingWizard` su route **pubblica `/signup`**,
macchina a stati interna: **Account** (email+password+nome) → submit `POST /signup` → **Verifica email** (schermata
"controlla la mail" + resend + "ho verificato, continua" che tenta `/refresh`; il link email apre `/verify`) →
auto-login → **Workspace** (rinomina account via `PATCH /accounts/me`, poiché il signup crea già account+owner) →
**Done** (→ dashboard, banner "Imposta 2FA"). `/signup` **non** fa il redirect-se-autenticato (a differenza di `/login`).

**Pagine standalone** (route pubbliche, atterraggio link email o flussi singoli):
- `/login` — sostituisce il placeholder: email+password → su `MfaChallenge` mostra step **codice TOTP** (inline) →
  `POST /login/2fa`. Gestisce errori UC9 (credenziali, email non verificata → prompt verifica, sospeso).
- `/verify?token=` — verifica server-side (`POST /verify`) → auto-login → prosegue all'onboarding (Workspace) o dashboard.
- `/forgot` — richiesta reset (`POST /password/forgot`, risposta **neutra** anti-enumeration).
- `/reset?token=` — nuova password (`POST /password/reset`) → conferma → `/login`.
- `/accept?token=` — imposta password (`POST /invitations/accept`) → auto-login come member → dashboard. Errori
  token scaduto/usato (410), invalido (400), email con account esistente.

**Protette (sotto shell)**:
- **Setup 2FA** in una sezione sicurezza del profilo/impostazioni: `POST /2fa/enroll` → mostra **QR/secret**
  (`otpauth_uri`) → input codice → `POST /2fa/verify` → attivata. **Banner di nudge** dismissibile nella shell.

**i18n**: tutte le stringhe a chiave, EN+IT (estende `packages/i18n`).

## Fuori scope (rinvii tracciati — registrati prima della chiusura)

1. **Step "Pick apps" dell'onboarding** (UC1 step 6) — crea `subscription`/entitlement: **nessun backend** (#09
   pagamenti non implementato). L'onboarding si ferma a Workspace→Done. → owner: #09/checkout (UC 0024) + moduli app.
2. **UI di invio inviti / gestione membri** — **nuovo UC dedicato** da creare (`0059`, area 04-platform-core o 06-frontend)
   e registrare in `README.md` + `_INDEX.md`. L'accept-invite (lato invitato) è in scope; il *mandare* inviti no.
3. **Disattivazione 2FA** — auth-local non espone `/2fa/disable`: implemento solo l'**abilitazione**. → serve endpoint
   backend (area UC 0058); traccia in UC 0017 + UC 0058.
4. **Stato "2FA attivo?"** — nessun endpoint/claim espone `totpEnabled` → il banner di nudge è **dismissibile** senza
   verità server. → serve un campo (claim id-token o `GET /me`); traccia in UC 0017 + UC 0058.
5. **Specificità prod Cognito** (☁0015/0016/0018) — challenge names, Custom Message Lambda, throttling API GW: la UI è
   backend-agnostica (`/api/auth/*`); localmente auth-local. → owner UC 0015/0016/0018.
6. **Testi finali template email EN/IT** — contenuto, non UI (già notato in UC 0017 "Questioni aperte").

## Criteri di accettazione

- [ ] **Signup+onboarding**: wizard `/signup` Account→Verifica→Workspace→Done; signup `201`/`409 email già registrata`;
      Workspace rinomina l'account (`PATCH /accounts/me`); Done mostra il nudge 2FA.
- [ ] **Verifica email**: `/verify?token=` verifica+auto-login; resend con risposta neutra; token invalido → errore.
- [ ] **Login**: email+password; su `MfaChallenge` step codice TOTP → `/login/2fa`; errori UC9 (credenziali generiche,
      email non verificata, sospeso) da problem+json.
- [ ] **Reset**: `/forgot` (neutra) + `/reset?token=` (policy password) → login.
- [ ] **Accept-invite**: `/accept?token=` imposta password → auto-login member; errori 400/410.
- [ ] **Setup 2FA**: enroll → QR/secret → verify → attivata.
- [ ] **Validazione**: schemi Zod allineati a Bean Validation + password policy; tutti gli errori mappati da problem+json.
- [ ] **i18n EN/IT** su tutte le schermate; **a11y axe** sulle schermate chiave.
- [ ] Test verdi (component Vitest+RTL+MSW + E2E Playwright) + `npm test`/`typecheck` verdi su tutti i workspace.
- [ ] Rinvii tracciati nei file UC/`_BACKLOG.md`; **nuovo UC 0059** creato e registrato in `README.md`+`_INDEX.md`.

## Invarianti appgrove toccati

- **tenant_id/user_id solo dal JWT** — i flussi auth producono il token; la shell legge i claim solo dal token (già in
  0020). Le schermate auth non leggono `tenant_id` da params/body.
- **Filtro row-level** — N/A lato client (enforcement backend). Frontend = solo UX.
- **Modulo Terraform `microsaas_app`** — N/A.
- **Logging strutturato** — N/A (SPA statica).

## Requisiti di test

- **Login → 2FA challenge** (MSW): `login` ritorna `MfaChallenge` → UI mostra step codice → `login/2fa` → sessione.
- **Signup wizard** (component): step Account valida policy/email; `409` → messaggio "email già registrata";
  transizione a Verifica; Workspace chiama `PATCH /accounts/me`.
- **Accept-invite** (MSW): `200` → auto-login; `410` → messaggio token scaduto/usato.
- **Reset/forgot**: risposta **neutra** del forgot non rivela l'esistenza dell'email.
- **problem+json**: un errore tipizzato (es. `401`/`403`/`409`) è mappato e mostrato come messaggio localizzato.
- **a11y**: axe verde sulle schermate auth chiave; form con label/aria-describedby per gli errori.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (solo aggiunte: nuove route/schermate + data layer auth) |
| Contratto cross-area | Sì — frontend ↔ auth-local (`/api/auth/*`) e ↔ core (`PATCH /accounts/me`). Nessuna modifica ai servizi. |
| Version bump | minor |
