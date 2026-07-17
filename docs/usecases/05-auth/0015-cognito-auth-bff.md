# UC 0015 — Cognito + auth BFF (login/refresh/logout, HttpOnly cookie, CORS)

**Area**: 05-auth · **Fase**: 2 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0003](../02-devops-infra/0003-fondamenta-terraform.md) (infra/Cognito), UC [0012](../04-platform-core/0012-servizio-core-multitenancy.md) (core)
**Fonte decisioni**: #02 (auth/Cognito/BFF), #04 (Lambda/servizi), #06 G (Cognito/Lambda infra)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [02-auth-sicurezza](../../02-auth-sicurezza.md), [06-infra-iac](../../06-infra-iac.md), [04-services-backend](../../04-services-backend.md), [0010-provider-auth-locale](../03-local-dev/0010-provider-auth-locale.md)

## 1. Obiettivo / Scope
Implementare Cognito (test/prod) e l'**auth BFF** (mini-BFF via Lambda) che gestisce login/refresh/logout server-side,
con token in memoria + **refresh token in cookie HttpOnly**.
**Incluso**: User Pool Cognito per env; **app client confidenziale** (con secret); **auth Lambda** dietro API GW con
`POST /api/auth/login|refresh|logout`; cookie **host-only** su `api.appgrove.app` (`Secure`/`HttpOnly`/`SameSite=Lax`/`Path=/api/auth`);
**CORS** con credentials (no wildcard); TTL access 15m / refresh 24h con rotazione; throttling `/api/auth/*`.
**Escluso**: l'iniezione claim (Pre-Token-Gen, UC [0016](0016-pre-token-gen-jwt.md)); le schermate UI (UC 0017); email localizzate (UC 0018);
in locale l'auth è emulata (UC 0010).

## 2. Attori & ruoli
- **Utente** (SPA): posta credenziali al BFF su TLS (no SRP nel browser).
- **auth Lambda** (BFF): parla con Cognito (`InitiateAuth`/`RefreshToken`/`RevokeToken`).
- **Cognito**: solo autenticazione (identity provider).

## 3. Precondizioni
- Infra (UC 0003): VPC no-NAT + VPC endpoint Cognito-IDP, Secrets Manager (app client secret), API GW, Route53/ACM domini.
- Core (UC 0012) per la membership (letta dal Pre-Token-Gen, UC 0016).

## 4. Flusso principale
1. `POST /api/auth/login` → auth Lambda `InitiateAuth` su Cognito; **set cookie refresh HttpOnly** + ritorna access/id nel body (#02 2/3).
2. `POST /api/auth/refresh` → legge il cookie → `REFRESH_TOKEN_AUTH` → nuovi access/id + **ruota** il cookie (#02 2).
3. `POST /api/auth/logout` → cancella il cookie + Cognito `RevokeToken` (#02 2).
4. **CORS**: origin = dominio frontend esplicito, `Allow-Credentials: true`, niente `*` (#02 16).
5. **Cookie**: host-only su `api.appgrove.app` (nessun `Domain`), Secure/HttpOnly/SameSite=Lax/Path=/api/auth → isolamento automatico tra env (#02 17, #12 10).

## 5. Flussi alternativi / edge / errori
- **2FA attiva**: la Lambda gestisce la challenge MFA Cognito (`SOFTWARE_TOKEN_MFA`) (#02 18) — flusso UI in UC 0017.
- **Credenziali errate** → messaggio generico + **lockout Cognito** + **throttling API GW** su `/api/auth/*` (#02 7? → #06 26).
- **Email non verificata / utente sospeso** → negato (dettaglio UC 0017 UC9).
- **Reload SPA**: `refresh` ripristina la sessione senza esporre il refresh token (#02 3).
- **Parità locale**: stesso contratto del provider locale (UC 0010); la differenza Cognito reale si valida in test.

## 6. Schermate & stati
Nessuna UI propria (è API). Le schermate login/logout/refresh sono in UC 0017; qui il contratto degli endpoint `/api/auth/*` + cookie.

## 7. Dati toccati
Credenziali gestite da **Cognito** (mai memorizzate da noi); `paddle`/dati pagamento N/A. **Dati personali**: email (login),
gestita su base **contratto** (#13 B). Secrets (app client) in Secrets Manager; **nessun secret nel codice** (#02 15). Cookie = token opachi.

## 8. Permessi & gate
- **Invarianti**: l'auth è il punto in cui nasce il JWT che porterà `tenant_id` (iniettato dal Pre-Token-Gen, UC 0016) →
  abilita "tenant_id solo dal JWT verificato". **Fail-closed** a valle se mancano i claim.
- Cookie host-only → isolamento ambienti; CORS senza wildcard.

## 9. Requisiti di test
- **Integration**: login/refresh/logout, rotazione cookie, scadenze TTL; challenge 2FA (mock).
- **Security**: cookie HttpOnly/Secure/SameSite corretti; CORS rifiuta origin non ammessi; refresh token mai esposto in JS.
- **E2E** (UC 0017): flussi completi contro auth locale (UC 0010); smoke su Cognito reale in test.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #02 1/2/3/4/5/6/16/17/18, #06 18/26, #12 10.
- **DoD**:
  1. Cognito per env + app client confidenziale; auth Lambda con login/refresh/logout.
  2. Refresh token in cookie host-only HttpOnly; access/id in memoria; rotazione + TTL.
  3. CORS con credentials senza wildcard; throttling `/api/auth/*`.
  4. Contratto identico al provider locale (UC 0010) per la parità dev↔test.

## Punti aperti / decisioni differite

_Tracciato dalla change `0036-use-case-0005-…` (pipeline CI/CD)._

- ✅ **Campi Cognito nel `config.json` per-ambiente** — **risolto dalla change `0037-use-case-0015-…`**:
  `spa_config` (in `infra/envs/{test,prod}/outputs.tf`) ora valorizza `cognito.userPoolId`/`clientId` dagli
  output reali del modulo `platform_shared` (pool + app client creati da questo UC).

_Aggiunti dalla change `0037-use-case-0015-…` (implementazione):_

- **Contratto verify/reset in cloud: token opaco `base64url(email|codice)`.** Cognito conferma con
  email+codice; per mantenere il contratto a token unico del provider locale, il provider Cognito decodifica
  un token opaco. Il **link nelle email** con questo formato lo deve generare il **Custom Message Lambda**
  (→ UC 0018, tracciato lì); con le email default Cognito il codice va composto a mano (solo fase pre-0018).
- **Verify cloud senza auto-login.** Cognito non emette token alla conferma email (serve la password):
  `POST /verify` col provider Cognito risponde `{status:"confirmed"}` e la SPA rimanda al login
  (fallback implementato in `VerifyEmailPage`). Divergenza consapevole da UC 0017 UC1 step 4 (tracciata in UC 0017).
- **Prima esecuzione live** (build nativa `function.zip` in CI, doppio giro per il bucket artefatti, smoke su
  Cognito reale in test) → checklist di attivazione in [docs/_BACKLOG.md](../../_BACKLOG.md).
