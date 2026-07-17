# UC 0016 — Pre-Token-Gen Lambda (claim tenant_id/roles) + JWT validation (Quarkus OIDC)

**Area**: 05-auth · **Fase**: 2 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0012](../04-platform-core/0012-servizio-core-multitenancy.md) (core/DB), UC [0015](0015-cognito-auth-bff.md) (Cognito/BFF)
**Fonte decisioni**: #02 C/E (Pre-Token-Gen, verifica JWT), #01 (claim/identità), #06 G (Lambda in VPC/RDS Proxy)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [02-auth-sicurezza](../../02-auth-sicurezza.md), [01-architettura](../../01-architettura.md), [05-persistenza-dati](../../05-persistenza-dati.md)

## 1. Obiettivo / Scope
Implementare la **Pre-Token-Generation Lambda** che inietta i claim `tenant_id`+`roles` a ogni emissione token, e la
**verifica JWT** nei servizi (Quarkus OIDC) — il meccanismo che rende vera l'invariante "tenant_id solo dal JWT verificato".
**Incluso**: Lambda Pre-Token-Gen **in VPC** che legge la membership dallo schema `platform` (**RDS Proxy**), inietta
`tenant_id` (string) + `roles` (array), **fail-closed** se manca membership; verifica JWT nei servizi (issuer Cognito, **JWKS**,
audience), `roles`-claim-path = `roles`, uso **access token**.
**Escluso**: l'authorizer edge (UC [0014](../04-platform-core/0014-authorizer-custom.md)); in locale i claim li mette il provider locale (UC 0010).

## 2. Attori & ruoli
- **Cognito** invoca la Lambda al momento dell'emissione token.
- **Pre-Token-Gen Lambda**: legge membership/ruoli dal core, inietta claim.
- **Servizi Quarkus**: validano il JWT e leggono i claim verificati.

## 3. Precondizioni
- Core/DB (UC 0012) con `users` (membership/ruoli); Cognito + auth BFF (UC 0015); RDS Proxy + Secrets (cred DB) da infra (UC 0003/0004).

## 4. Flusso principale
1. All'emissione token Cognito invoca la **Pre-Token-Gen Lambda** (in VPC).
2. La Lambda interroga `platform` via **RDS Proxy** per membership+ruoli dell'utente (`cognito_sub` → `tenant_id`, `role`) (#02 9, #05 3).
3. Inietta i claim **`tenant_id`** + **`roles`**; se manca tenant/membership valida → **niente claim → accesso negato** (fail-closed) (#02 10).
4. I servizi validano il JWT via **Quarkus OIDC** (issuer Cognito, firma via **JWKS**, audience app client); authz mappata su `roles` (#02 11).
5. `TenantResolver` (UC 0012) legge `tenant_id` dal JWT verificato → filtro row-level automatico.

## 5. Flussi alternativi / edge / errori
- **Utente senza membership** → nessun claim → negato (fail-closed) (#02 10).
- **Token forgiato/scaduto** → firma/scadenza non valida → rifiutato dalla verifica JWKS (#10 D9).
- **Connection storm**: la Lambda usa **RDS Proxy** (non Agroal diretto) per evitarlo (#05 3).
- **Parità locale**: il provider locale (UC 0010) replica esattamente questi claim → stesso code path nei servizi.

## 6. Risorse & runbook
**Risorse**: Lambda Pre-Token-Gen (VPC, RDS Proxy, cred DB in Secrets Manager, VPC endpoint), trigger Cognito; config OIDC
nei servizi (issuer/JWKS/audience). **Runbook**: deploy via UC 0005; in caso di errori claim → log strutturati (#08) con `user_id`/`tenant_id`.

## 7. Dati toccati
Legge `platform.users` (membership/ruoli) — **dati personali** indiretti (associazione utente↔tenant/ruolo), base **contratto**.
I claim contengono `tenant_id`/`roles` (no PII sensibile; `sub` opaco). Cred DB in Secrets Manager. Manifest: rientra nel trattamento account (#13 B).

## 8. Permessi & gate
- **Invariante cardine**: `tenant_id` e `roles` provengono **solo** dal JWT verificato, iniettati server-side dal Pre-Token-Gen;
  il client non può fornirli. **Fail-closed** senza membership.
- I servizi fanno authz ruolo (`@RolesAllowed`); l'entitlement è gestito altrove (derivato, UC 0014/0027).

## 9. Requisiti di test
- **Integration**: la Lambda inietta i claim corretti dato un utente; fail-closed senza membership.
- **Security**: JWT senza `tenant_id` → negato; token forgiato/scaduto → negato; `tenant_id` in request ignorato (#10 D9/D10).
- Parità: i claim locali (UC 0010) e Cognito producono lo stesso comportamento nei servizi.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #02 9/10/11, #01 9/17, #05 3, #06 18.
- **DoD**:
  1. Pre-Token-Gen in VPC legge membership via RDS Proxy e inietta `tenant_id`+`roles` (fail-closed).
  2. Servizi validano JWT via Quarkus OIDC (issuer/JWKS/audience), authz su `roles`.
  3. Suite security verde (fail-closed, anti-override, token invalidi).
  4. Parità di claim col provider locale (UC 0010).

## Punti aperti / decisioni differite

_Tracciato dalla change `0009-use-case-0010-…` (regola CLAUDE.md "Tracciamento delle decisioni differite")._

- **Claim ruoli: `groups` vs `roles`.** Il `core` (UC 0013) mappa `@RolesAllowed` sul claim **`groups`** (default
  smallrye-jwt), mentre #02 dec.10 prevede il claim **`roles`** con `quarkus.oidc.roles.role-claim-path=roles`. Il
  provider locale (UC 0010) emette **entrambi** per compatibilità. Quando atterra il Pre-Token-Gen reale + Quarkus OIDC,
  **riconciliare** su un unico claim (`roles`) e allineare il `core`. **Proprietario**: UC 0016.

_Aggiunto dalla change `0037-use-case-0015-…` (Cognito + auth BFF):_

- **Accesso DB delle Lambda auth: ruolo dedicato least-privilege.** La Lambda BFF (UC 0015) scrive lo schema
  `platform` (signup/accept invito) autenticandosi al proxy RDS con le credenziali **master** Aurora — unico
  secret attaccato al proxy (`platform_shared/rds_proxy.tf`), perché `db-bootstrap` sa creare solo ruoli con
  privilegi sul **proprio** schema. Quando questo UC aggiunge il Pre-Token-Gen (lettura `platform`): creare un
  **ruolo DB dedicato** alle Lambda auth (estendere `db-bootstrap` con una modalità *grant su schema altrui* +
  `ALTER DEFAULT PRIVILEGES`), attaccarne il secret al proxy, e valutare l'**IAM auth** del proxy + la stretta
  del security group del proxy alla sola SG delle Lambda (già annotata in `rds_proxy.tf` per UC 0014).
  **Proprietario**: UC 0016 (con UC 0014). Vedi anche E23 in `_EVOLUZIONI-DEVOPS`.
