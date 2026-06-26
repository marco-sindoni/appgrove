# UC 0058 — Flussi auth locali completi (signup/verifica, accept invito, reset password, 2FA TOTP, email Mailpit)

**Area**: 03-local-dev · **Fase**: 0 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0010](0010-provider-auth-locale.md) (provider auth locale security-core), UC [0013](../04-platform-core/0013-account-utenti-inviti-api.md) (accounts/users/invitations), UC [0011](0011-dati-seed.md) (seed)
**Fonte decisioni**: #02 (13/14/18/19 signup/inviti/2FA/password), #11 B
**Ultimo aggiornamento**: 2026-06-26
**Aree collegate**: [02-auth-sicurezza](../../02-auth-sicurezza.md), [05-auth/0017-flussi-auth](../05-auth/0017-flussi-auth.md)

> **Scorporato da UC 0010 (change `0009-use-case-0010-…`).** UC 0010 implementa il **security-core** del provider auth
> locale (login/refresh/logout, firma JWT + JWKS, claim dal DB, validazione servizi, fail-closed). Questo UC completa i
> **restanti flussi** del provider locale, da implementare quando atterrano le UI auth (UC 0017) e gli E2E che li
> esercitano. Stesso servizio `services/auth-local`, stessi pattern (#02): qui si aggiungono gli endpoint mancanti.

## 1. Obiettivo / Scope
Completare il provider auth locale (`services/auth-local`, UC 0010) con i flussi non-core, così che l'**intero** percorso
auth giri offline con lo stesso shape di prod (#11 B6/B7).
**Incluso**: **signup self-service** (nuovo account+owner) + **verifica email** via Mailpit; **accept invito** (utente
creato nel tenant invitante, single-use/scadenza, #02 14); **reset password** via Mailpit; **2FA TOTP** (enroll/verify con
lib reale) + **toggle bypass dev**; invio email transazionali a **Mailpit**.
**Escluso**: il security-core (UC 0010); le **schermate UI** dei flussi (UC 0017); Cognito reale (test, UC 0015/0016).

## 2. Attori & ruoli
- **Developer / suite E2E**: esercitano i flussi offline.
- **Provider Local** (`auth-local`): gestisce signup/verifica/reset/2FA, manda email a Mailpit.
- Utenti dal Postgres locale (seed UC 0011) + nuovi creati da signup/invito.

## 3. Precondizioni
- UC 0010 attivo (provider security-core: login/refresh/logout, JWKS, claim dal DB); schema `platform` (UC 0013); seed (UC 0011).

## 4. Flusso principale
1. **Signup**: `POST /api/auth/signup` → crea **account (tenant) + utente owner** (#02 13); invia email di verifica a Mailpit; utente `pending` finché non verifica.
2. **Verifica email**: link/codice dalla mail Mailpit → utente confirmed.
3. **Accept invito (B2B)**: `POST /api/auth/invitations/accept` con il token → crea l'utente nel tenant invitante col ruolo dell'invito (single-use/scadenza, #02 14); il link vale come verifica email (UC 0017 UC7).
4. **Reset password**: `POST /api/auth/password/forgot` → email Mailpit; `POST /api/auth/password/reset` con token.
5. **2FA TOTP**: enroll (associazione segreto + QR/otpauth) e verify con lib reale; al login, challenge TOTP se attiva; **toggle bypass dev** per E2E veloci.

## 5. Flussi alternativi / edge / errori
- **Invito scaduto/già usato** → problem+json (coerente UC 0017 UC7).
- **Email con account esistente** (1 utente→1 tenant) → rifiuto (#02 14).
- **2FA attiva**: login risponde con challenge TOTP; bypass dev disponibile.
- **Token verifica/reset assente/scaduto/forgiato** → negato (fail-closed).

## 6. Risorse & runbook
**Componente**: estende `services/auth-local` (UC 0010) con gli endpoint signup/verify/invite-accept/reset/2FA e l'invio
SMTP verso **Mailpit** (UI `:8025`). **Credenziali locali**: vedi UC 0010 (password dev / store credenziali). **Runbook**:
flussi esercitati via API (e via UI quando arriva UC 0017); email ispezionabili in Mailpit; 2FA con authenticator reale o bypass dev.

## 7. Dati toccati
Schema `platform`: `accounts`/`users`/`invitations` (creazione da signup/invito), eventuale store credenziali/2FA-secret
(definito in UC 0010). Dati **sintetici** in dev; email solo verso Mailpit. Manifest GDPR N/A (ambiente dev).

## 8. Permessi & gate
- **Invarianti**: claim dal DB, `tenant_id` dal JWT, fail-closed (come UC 0010). Signup crea owner del nuovo tenant; accept invito rispetta 1 utente→1 tenant.
- Nessun gate entitlement/quota qui (UC 0014/0027).

## 9. Requisiti di test
- **Integration** (Testcontainers): signup→verifica→login; accept invito (single-use/scadenza); reset password; 2FA enroll/verify + bypass dev.
- **Security**: token verifica/reset/invito single-use e scadenza; fail-closed; no override `tenant_id`.
- Fondamento degli **E2E auth** (#10 F) assieme a UC 0010.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #02 13/14/18/19, #11 6/7.
- **DoD**:
  1. Signup (account+owner) + verifica email (Mailpit) funzionanti offline.
  2. Accept invito crea l'utente nel tenant invitante (single-use/scadenza).
  3. Reset password (Mailpit) e **2FA TOTP** (enroll/verify) + bypass dev funzionanti.
  4. Suite integration + security verdi; nessun servizio AWS.

## Punti aperti / decisioni differite

_Aggiunti dalla change `0012-use-case-0017-…`: la UI auth (UC 0017) ha riscontrato due lacune lato auth-local._

- **Endpoint `/api/auth/2fa/disable`** — auth-local offre enroll/verify ma **non** la disattivazione del 2FA; la UI
  (UC 0017 UC10, "disattivazione dal profilo con ri-autenticazione") non è implementabile finché manca. Aggiungerlo qui.
- **Esporre lo stato `totpEnabled`** — nessun claim/endpoint indica se il 2FA è attivo, quindi il banner di nudge della
  shell non riflette la verità server (oggi è dismissibile lato client). Aggiungere il flag nell'id-token o un
  `GET /api/auth/me` (coordinare con `users/me` del core UC 0013).
