# Change 0038: Pre-Token-Gen Lambda (claim tenant_id/roles) + validazione JWT nei servizi

**Branch**: `change/0038-use-case-0016-pre-token-gen-jwt`
**Aree**: `infra/` · `services/core` · `services/fatture` (config validazione JWT) · `docs/` (tracciamento rimandi, costi, manifesto)
**Data**: 2026-07-18
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/05-auth/0016-pre-token-gen-jwt.md](../../docs/usecases/05-auth/0016-pre-token-gen-jwt.md)
**Tocca dati personali?**: Sì — la Lambda legge `platform.users` (associazione utente↔tenant/ruolo, dati personali indiretti, base contratto). Trattamento **già censito** nel manifesto `platform` e nella RoPA (UC 0015/0013). Atteso gate privacy **MINOR**: nessun nuovo dato, nessun nuovo sub-processor (SDK/servizi AWS in `eu-west-1` già in uso). Lo scanner `privacy-scan` gira a step-03; classificazione definitiva registrata nel log.

## Problema / Obiettivo

Rendere **eseguibile** l'invariante cardine "`tenant_id` solo dal JWT verificato" nel percorso cloud (Cognito), oggi realizzata solo in locale dal provider Local (UC 0010). Servono due meccanismi complementari:

1. una **Lambda Pre-Token-Generation** (Python, in VPC) che a ogni emissione token legge la membership da `platform.users` via **proxy RDS** e inietta i claim `tenant_id` (stringa) + `roles` (array) nel token, **fail-closed** se manca una membership attiva;
2. la **validazione del JWT nei servizi** contro il pool Cognito (emittente, firma via chiavi pubbliche JWKS), con l'autorizzazione dei ruoli agganciata al claim `roles`.

Il risultato deve essere **comportamentalmente identico** a ciò che già fa il provider Local, così che i servizi abbiano un unico percorso di codice in locale e in cloud.

## Scope

### 1. Cognito — trigger e piano funzionalità (`infra/modules/platform_shared/auth.tf`)
- Impostare `user_pool_tier = "ESSENTIALS"` sul pool: è il prerequisito per personalizzare l'**access token** (evento trigger `V2_0`). A scala PoC è di fatto gratuito (fascia gratuita di 10.000 utenti attivi/mese; è già il piano predefinito dei nuovi pool).
- Aggiungere al pool `lambda_config { pre_token_generation_config { lambda_arn = <pre-token-gen>, lambda_version = "V2_0" } }`, condizionale come la Lambda BFF (attivazione a fasi: nessun trigger finché la Lambda non è creata).

### 2. Lambda Pre-Token-Gen (nuovo `infra/modules/platform_shared/pre_token_gen.tf` + sorgente Python)
- **Runtime Python** (come `db_bootstrap.py`/`error_ingest.py`), impacchettata da Terraform (`archive_file`); **in VPC** con gruppo di sicurezza che rispecchia la Lambda auth (uscita 443 verso gli endpoint VPC + 5432 verso il proxy RDS).
- Connessione a Postgres **via proxy RDS** con TLS, usando un driver Postgres **puro-Python vendorizzabile** (`pg8000`) — niente binari nativi, l'`archive_file` resta autocontenuto. (È la prima Lambda Python a passare dal proxy: `db_bootstrap` usa invece la RDS Data API fuori VPC.)
- Logica: dal `sub` dell'evento → `SELECT tenant_id, role, status FROM platform.users WHERE cognito_sub = :sub AND status = 'active' AND deleted_at IS NULL`. Se trovata: inietta `tenant_id` + `roles = [role]` (+ `platform-admin` con la stessa regola del locale, se applicabile) nell'access token (ed eventualmente id token). Se assente/non valida: **nessun claim** (fail-closed).
- Log strutturati (JSON) con `user_id`/`tenant_id`; **mai** credenziali nei log.
- `aws_lambda_permission` per l'invocazione da `cognito-idp.amazonaws.com`.
- Allarme errori (observability) coerente con quello della Lambda BFF.

### 3. Ruolo DB dedicato a privilegi minimi per le Lambda auth (opzione **B** di E23)
- Estendere la Lambda `db-bootstrap` (`infra/modules/platform_shared/lambda/db_bootstrap.py`) con una **modalità "grant su schema altrui"**: creare un ruolo DB `auth_lambdas` con privilegi minimi sullo schema `platform` (`SELECT` per la Pre-Token-Gen; `SELECT/INSERT/UPDATE` sulle tabelle che il BFF scrive: `accounts`, `users`, `invitations`) + `ALTER DEFAULT PRIVILEGES`, in modo idempotente.
- Password del ruolo in Secrets Manager (convenzione esistente), **agganciata al proxy RDS** accanto al segreto master.
- Puntare **entrambe** le Lambda auth al nuovo segreto: la Pre-Token-Gen (lettura) e — via sola variabile Terraform `AUTH_DB_SECRET_ARN`, **senza modifiche di codice** — la Lambda BFF di UC 0015. Obiettivo di sicurezza di E23 (parte "ruolo"): le Lambda auth **non usano più le credenziali master**.

### 4. Validazione JWT nei servizi (`services/core`, `services/fatture`)
- **Riconciliazione `groups` → `roles`** (decisione differita di proprietà di questo UC): impostare `smallrye.jwt.path.groups=roles` così `@RolesAllowed` matcha sul claim `roles` (#02 dec.10). Il provider Local emette già sia `groups` sia `roles` con lo stesso contenuto → nessun cambio al token; è un cambio di *dove* i servizi leggono i ruoli. Allineare il commento in `Roles.java` (core).
- Aggiungere la config profilo cloud/prod: `mp.jwt.verify.issuer` = emittente del pool Cognito e `mp.jwt.verify.publickey.location` = URL JWKS del pool (iniettati per-ambiente). Manteniamo **smallrye-jwt** (già usato da tutti i servizi, stesso percorso della parità locale): la verifica di firma via JWKS + emittente soddisfa la decisione #02.11 "Quarkus OIDC" nella sostanza. Gli access token Cognito portano `client_id` (non `aud`): oltre a firma + emittente, i servizi verificano **`token_use = access`** (accettano solo access token, mai id token) e confrontano **`client_id`** con l'identificatore del client confidenziale (difesa in profondità sul "destinatario", fatta sul campo corretto per Cognito). Nel profilo locale la parità è garantita perché il provider Local emette gli stessi claim (`token_use=access`, ruoli in `roles`).

### 5. Documentazione e tracciamento (`docs/`)
- Spostare la nota `groups`/`roles` nel documento UC 0016 da *differita* a **risolta in questa change**.
- Aggiornare **E23** in `_EVOLUZIONI-DEVOPS`: la parte "ruolo dedicato" è realizzata qui (B); autenticazione IAM del proxy + stretta del gruppo di sicurezza restano a **UC 0014**.
- Tracciare i rimandi residui nel documento di **UC 0014** (vedi "Fuori scope").
- `_COSTI-AWS.md`: annotare il piano **Essentials** del pool (gratuito sotto 10.000 utenti attivi/mese; nota il costo marginale oltre soglia).

## Fuori scope

- **Authorizer edge JWT + entitlement** (verifica dell'`app_id` del path) → **UC 0014** (già escluso nel documento UC 0016).
- **Autenticazione IAM del proxy RDS** e **stretta del gruppo di sicurezza** del proxy alla sola rete delle Lambda auth → **UC 0014** (parte residua di E23; già annotata in `rds_proxy.tf`). **Tracciamento**: da registrare esplicitamente nel documento di UC 0014, sezione "Punti aperti / decisioni differite".
- **Esecuzione dal vivo in cloud**: come tutto il cloud (attivazione a fasi), la prima build/apply e la prova reale del trigger Cognito avvengono all'accensione ambienti — qui ci si ferma a test verdi + `terraform fmt/validate/plan`. La checklist di prima accensione resta in `_BACKLOG`.
- **Link email col token opaco / infra SES** → **UC 0018** (già tracciato, estraneo).

## Criteri di accettazione

- [ ] La Lambda Pre-Token-Gen (Python, in VPC) legge la membership via proxy RDS e inietta `tenant_id` + `roles` nell'access token; **fail-closed** senza membership attiva. Coperta da test Python (claim corretti; assenza di membership → nessun claim; utente `platform-admin`).
- [ ] Cognito: `user_pool_tier = ESSENTIALS` + trigger `pre_token_generation_config` `V2_0` cablato in modo condizionale; `terraform fmt/validate` puliti; nessuna risorsa creata finché la Lambda non esiste (attivazione a fasi conservata).
- [ ] Ruolo DB `auth_lambdas` a privilegi minimi creato da `db-bootstrap` (idempotente), segreto agganciato al proxy; Pre-Token-Gen e BFF puntano al nuovo segreto (BFF senza modifiche di codice); le Lambda auth non usano più il master.
- [ ] Servizi (`core`, `fatture`): `@RolesAllowed` matcha su `roles`; config cloud emittente/JWKS Cognito presente. Suite **security** verde: JWT senza `tenant_id` → negato; token forgiato/scaduto → negato; `tenant_id` in body/param **ignorato**.
- [ ] Servizi: la validazione del token verifica **`token_use = access`** (un id token viene rifiutato) **e** `client_id` = client confidenziale atteso (token con `client_id` diverso → rifiutato). Coperto da test.
- [ ] **Parità**: i claim del provider Local e quelli attesi dalla Pre-Token-Gen producono lo stesso comportamento di autorizzazione nei servizi (stesso percorso di codice).
- [ ] Tutti i rimandi a UC 0014 / E23 tracciati nei file dei rispettivi UC/registro **prima** della chiusura (cancello di tracciamento).

## Invarianti appgrove toccati

- **`tenant_id` solo dal JWT verificato**: è il cuore della change — il claim è iniettato server-side dal Pre-Token-Gen e letto dai servizi solo dal token verificato; il client non può fornirlo. Fail-closed conservato.
- **Filtro row-level `WHERE tenant_id`**: invariato — il `JwtTenantResolver` (UC 0012) continua a leggere `tenant_id` dal JWT; questa change ne rende reale la sorgente in cloud.
- **Modulo `microsaas_app`**: non toccato — la Pre-Token-Gen è piattaforma condivisa (`platform_shared`), come Cognito e la Lambda BFF.
- **Logging strutturato** (`tenant_id`/`app_id`/`user_id`): la Lambda emette log JSON con `user_id`/`tenant_id`; nessuna credenziale/token nei log.

## Requisiti di test

- **Lambda (Python)**: iniezione claim corretta dato un utente attivo; **fail-closed** senza membership / utente disattivato / `deleted_at` valorizzato; regola `platform-admin`. (Cablare l'area di test in `run-tests.sh` se introduce un comando nuovo.)
- **Security nei servizi** (core/fatture): JWT privo di `tenant_id` → 401/403; token con firma non valida o scaduto → rifiutato; claim `tenant_id`/ruoli forniti in body/param → **ignorati** (letti solo dal token). Autorizzazione su `roles` (guard che il match non avvenga più su `groups`). Token con `token_use` ≠ `access` (es. id token) → rifiutato; token con `client_id` diverso da quello atteso → rifiutato.
- **Parità**: un token in forma "locale" (claim `roles` + `tenant_id`) supera le stesse guardie di autorizzazione dei servizi.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (locale invariato; cloud mai acceso — attivazione a fasi) |
| Contratto cross-area | Sì — servizio ↔ infra (Cognito emette i claim che i servizi validano); nessun cambio al contratto `/api/*` verso la SPA |
| Version bump | minor |
