# UC 0010 — Local auth provider (JWT/JWKS, claim dal DB, refresh cookie, TOTP, Mailpit)

**Area**: 03-local-dev · **Fase**: 0 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0008](0008-stack-sviluppo-locale.md) (stack Compose)
**Fonte decisioni**: #11 B (emulazione auth), #02 (meccanica auth/claim/cookie)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [11-developer-experience](../../11-developer-experience.md), [02-auth-sicurezza](../../02-auth-sicurezza.md), [05-auth/0017-flussi-auth](../05-auth/0017-flussi-auth.md)

> **Aggancio da change 0002 (UC 0008).** Il provider auth locale è già predisposto come **placeholder**: servizio
> `auth-local` **commentato** in `dev/docker-compose.yml` (con env Postgres e porta `9100`) e route `/api/auth/*`
> **commentata** in `dev/Caddyfile` (snippet `(api_routes)` + per-host). Questo UC deve **scommentarli e riempirli**
> (immagine/build, firma JWT/JWKS, claim dal DB, refresh cookie, TOTP, email su Mailpit) — non reinventare lo stack.

> **Open point da change 0004 (UC 0009).** Negli script `dev/` ci sono due agganci da riempire qui:
> (1) `dev/lib/setup.sh` passo **"6/8 Chiavi JWT locali"** è uno **stub** — generare/persistere le chiavi JWT locali;
> (2) `dev/lib/up.sh` ha un **hook "processi-app"** vuoto — avviare `auth-local` (scommentando il servizio in
> `dev/docker-compose.yml`) e la route `/api/auth/*` nel `Caddyfile`. Sostituire i `warn` di stub con la logica reale.

## 1. Obiettivo / Scope
Definire il **provider auth locale** che sostituisce Cognito + auth Lambda in dev, così che l'**intero flusso auth**
(signup/verify/login/refresh/logout/invite/2FA) giri **offline** con **lo stesso shape di JWT** di prod (#11 B6/B7).
**Incluso**: identity provider dietro la **stessa interfaccia (port)** del BFF auth (selezione per profilo: Cognito test/prod
vs **Local** dev); firma JWT locale (access/id) con claim `sub`+`tenant_id`+`roles` **letti dal DB** (replica Pre-Token-Gen);
**JWKS** locale; **refresh cookie** HttpOnly identico; **2FA TOTP** con lib reale (+ toggle bypass dev); email su **Mailpit**.
**Escluso**: Cognito reale (validato in test, UC [0015](../05-auth/0015-cognito-auth-bff.md)/[0016](../05-auth/0016-pre-token-gen-jwt.md));
le schermate UI dei flussi (UC [0017](../05-auth/0017-flussi-auth.md)).

## 2. Attori & ruoli
- **Developer**: sviluppa contro l'auth locale come fosse Cognito.
- **Provider Local** (sistema): emette/valida token, espone JWKS, gestisce refresh/2FA, manda email a Mailpit.
- Utenti **dal Postgres locale** (seed UC 0011).

## 3. Precondizioni
- Stack locale attivo (UC 0008); chiavi di firma JWT locali generate da `dev setup` (UC 0009); utenti seedati (UC 0011).

## 4. Flusso principale
1. La SPA chiama `POST /api/auth/login` (via reverse proxy) → il **provider Local** valida le credenziali sul Postgres locale.
2. Costruisce i claim **dal DB** (`sub`, `tenant_id`, `roles`) **replicando il Pre-Token-Gen** (#11 B7) e **firma** access/id token.
3. Imposta il **refresh token in cookie HttpOnly** (stesso pattern #02 3) e ritorna access/id nel body.
4. I servizi Quarkus validano via **OIDC sul JWKS locale** in profilo `%dev` → **stesso code path di prod**, cambia solo l'issuer.
5. `POST /api/auth/refresh` ruota il cookie; `logout` lo cancella. Verifica email / reset → email su **Mailpit**.
6. **2FA TOTP**: associazione/verifica con lib reale; **toggle bypass dev** per E2E veloci.

## 5. Flussi alternativi / edge / errori
- **2FA attiva**: login risponde con challenge TOTP (come UC 0017 UC3); bypass dev disponibile.
- **Invito (B2B)**: il link vale come verifica email (coerente UC 0017 UC7); utente creato già confirmed nel tenant invitante.
- **Token assente/scaduto/forgiato** → negato (fail-closed, parità con prod #10 D9).
- **Divergenza claim vs Cognito**: il provider locale deve emettere **esattamente** `tenant_id`+`roles` come il Pre-Token-Gen
  (UC 0016) → la differenza reale Cognito si valida in test.

## 6. Risorse & runbook
**Componente**: servizio "auth-local" agganciato al reverse proxy su `/api/auth/*` (UC 0008); **JWKS** su endpoint locale;
chiavi di firma da `dev setup`. **Interfaccia (port)**: stessa astrazione del BFF auth (`IdentityProvider` con impl
`Local` vs `Cognito`), così il codice di prodotto è identico tra dev e test/prod.
**Runbook**: parte con `dev up`; utenti/ruoli dal seed; email ispezionabili nella UI Mailpit; per testare 2FA si usa un
authenticator reale o il bypass dev.

## 7. Dati toccati
Utenti/membership/ruoli **dal Postgres locale** (schema `platform`, sintetici, seed UC 0011). Nessun dato reale; email
solo verso Mailpit; chiavi di firma locali (mai segreti reali). Manifest GDPR N/A (ambiente dev).

## 8. Permessi & gate
- **Invarianti**: il provider locale è proprio ciò che rende testabili offline gli invarianti auth: **`tenant_id` dal JWT
  verificato**, claim dal DB, **fail-closed** senza tenant/membership. Stessa `roles`-claim-path di prod.
- Nessun gate entitlement/quota qui (sono nel servizio/authorizer, UC 0014/0027).

## 9. Requisiti di test
È il fondamento degli **E2E auth** (#10 F): signup/verifica/login+2FA/refresh/logout/reset/invite/onboarding girano contro
l'auth locale. DEVE risultare: token firmati validabili dai servizi via JWKS locale; claim identici al Pre-Token-Gen;
cookie refresh con stessi attributi; 2FA funzionante (+ bypass solo dev). Parità di comportamento con Cognito sulle proprietà
di sicurezza (fail-closed, no override `tenant_id`).

## 10. Riferimenti & Definition of Done
- **Decisioni**: #11 6/7, #02 2/3/9/10/11/18/19.
- **DoD**:
  1. Il BFF auth seleziona provider per profilo (Local in `%dev`).
  2. Il provider Local emette JWT con claim dal DB e JWKS locale; i servizi validano con lo stesso code path di prod.
  3. Refresh cookie, 2FA TOTP e email (Mailpit) funzionano offline.
  4. Gli E2E auth girano in locale senza alcun servizio AWS.

## Punti aperti / decisioni differite

_Tracciato dalla change `0008-use-case-0011-…` (regola CLAUDE.md "Tracciamento delle decisioni differite")._

- **Assegnazione gruppo `platform-admin` e claim dei JWT locali sui subject del seed.** Il seed (UC 0011) crea utenti
  con `cognito_sub` **stabili** (cast Acme/Bob + un utente piattaforma), ma `users.role` modella solo i ruoli tenant
  (owner/admin/member): il ruolo **`platform-admin`** è un gruppo JWT, non una colonna. Il provider auth locale deve
  mintare i token leggendo i subject del seed e **assegnare il gruppo `platform-admin`** all'utente piattaforma seedato
  (subject documentato in `dev/seed/README.md`), oltre a `tenant_id`/`roles` dal DB. **Proprietario**: UC 0010.
