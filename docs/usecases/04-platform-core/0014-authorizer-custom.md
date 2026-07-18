# UC 0014 — Authorizer all'edge (gate 1 della catena: token verificato prima del servizio)

**Area**: 04-platform-core · **Fase**: 2 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0013](0013-account-utenti-inviti-api.md) (catalogo/subscription), UC [0016](../05-auth/0016-pre-token-gen-jwt.md) (JWT)
**Fonte decisioni**: #04 §7 (enforcement edge), #09 dec.30 (catena gate), #01 (authorizer centralizzato), #02 §8
**Ultimo aggiornamento**: 2026-07-18 (change `0039-use-case-0014-…`)

> ## ⚠️ Revisione in implementazione (change 0039): authorizer **nativo**, non Lambda
>
> L'impianto originale — una **Lambda scritta da noi** che eseguisse all'edge i tre gate
> (JWT 401, app-abilitata 403, entitled 402) — **non è esprimibile** sulla variante di gateway che
> usiamo (**HTTP API v2**, scelta cost-min): il *deny* di un authorizer custom diventa **sempre 403**,
> non personalizzabile (la riscrittura delle gateway response esiste solo su **REST API**, più costosa).
> L'unico 401 che il gateway produce da sé è quello per richiesta **priva** dell'header `Authorization`,
> caso in cui la Lambda non viene nemmeno invocata.
>
> **Conseguenze verificate sul codice esistente** (per questo la deviazione è obbligata, non estetica):
> - token **scaduto** → 403 invece di 401 ⇒ il **refresh silenzioso** della SPA
>   (`frontend/packages/api-client/src/auth-middleware.ts`, contratto #03 dec.5/8) non scatterebbe mai e
>   l'utente cadrebbe fuori a **ogni** scadenza dell'access token (pochi minuti);
> - tenant non entitled → 403 invece di 402 ⇒ sparirebbe il **banner azionabile** "abbonamento richiesto"
>   (`frontend/apps/backoffice/src/billing/enforcement.ts`, UC 0028).
>
> **Decisione**: all'edge va l'**authorizer JWT nativo** di API Gateway (401 corretto, **costo zero**,
> nessun cold start, nessuna query al DB sul percorso critico di ogni richiesta — che la Lambda avrebbe
> fatto, dato che questo UC **vieta la cache**). I gate **app-abilitata** ed **entitled** restano **nel
> servizio** (UC [0027](../07-payments/0027-applicazione-entitlement-quota.md)), dove sono **già
> implementati e testati** con i codici giusti e un DB vero.
>
> Le sezioni sotto vanno lette con questa ripartizione. Motivazione estesa e alternative scartate:
> `changes/0039-use-case-0014-authorizer-custom/requirements.md`; codice: `infra/modules/platform_shared/authorizer.tf`.
**Aree collegate**: [04-services-backend](../../04-services-backend.md), [09-pagamenti](../../09-pagamenti.md), [02-auth-sicurezza](../../02-auth-sicurezza.md)

## 1. Obiettivo / Scope
Chiudere l'API pubblica: **nessuna richiesta priva di un token valido raggiunge i servizi**. Fino a qui le route
`/api/<app_id>/v1/*` nascevano **senza autenticazione** (deroga Checkov CKV_AWS_309 esplicita nel modulo `microsaas_app`,
in attesa di questo UC).
**Incluso**: **gate 1** della catena (#09 dec.30) all'**edge** — firma, issuer, audience e **scadenza** del JWT verificati da
API Gateway (authorizer **JWT nativo**) prima del VPC Link; **eccezione dichiarativa** per le route pubbliche by-design
(route più specifiche, senza authorizer: oggi il solo webhook Paddle, autenticato dalla **firma HMAC**); **stretta del
security group** del RDS Proxy alle sole Lambda auth (residuo **E23-b**).
**Escluso**: i gate **(2) app-abilitata** e **(3) entitled**, che restano **nel servizio** (UC 0027) — vedi il riquadro di
revisione sopra; i gate fini (ruolo 403, quota 429); la materializzazione entitlement (**abolita**, #09 dec.12); la cache
authorizer (non pertinente: l'authorizer nativo non ne ha bisogno).

## 2. Attori & ruoli
- **Authorizer Lambda** (edge): esegue i check grossolani.
- **Servizio** (Quarkus): ri-valida JWT e applica i gate fini (UC 0027).
- **platform-admin**: può **disabilitare** un'app (gate 2, UC 0021).

## 3. Precondizioni
- JWT valido con `tenant_id`/`roles` (UC 0016); catalogo+`subscription` nel core (UC 0013); flag disable-app gestibile (UC 0021).

## 4. Flusso principale (per ogni richiesta a `/api/<app_id>/v1/...`)
1. **Edge — AuthN (gate 1)**: API Gateway verifica il JWT contro le JWKS del pool (issuer + audience = il `client_id`
   dell'app client BFF, dato che l'access token Cognito non porta `aud`) e la **scadenza**. Header assente/token
   invalido/scaduto → **401** (il codice su cui è costruito il refresh silenzioso della SPA).
2. **Servizio — (2) App abilitata?** il gate legge `app.status`: se `inactive`, il tenant non ha entitlement per quell'app
   (rende l'app indisponibile a tutti **senza toccare dati/subscription**, reversibile) (#09 dec.30 gate 2).
3. **Servizio — (3) Tenant entitled?** entitlement **DERIVATO** (`EntitlementReadModel`) — **non** una tabella `entitlements`
   (abolita): `access = app.status==active && (subscription.grantsAccess() ‖ baseline tier free)`, con account in
   `pending_deletion` → **zero entitlement** (#13 E25). Se non entitled → **402** "abbonamento richiesto/scaduto"
   (#09 dec.12/dec.30 gate 3).
   > La regola **include il tier free**: un'app con piano gratuito è accessibile **senza** subscription. Formulazione
   > allineata al codice dalla change 0039 — la versione precedente ("solo `subscription` con status ∈
   > `{trialing,active,past_due}`") avrebbe prodotto **402 spurii** sulle app free.
4. Poi ruolo (403) e quota (429), sempre nel servizio (UC 0027).

## 5. Flussi alternativi / edge / errori
- **Rotte pubbliche by-design**: chi non ha un access token e si autentica altrimenti ottiene una **route dedicata più
  specifica**, senza authorizer — il gateway sceglie sempre la più specifica, quindi l'eccezione è **dichiarativa**, non una
  allow-list nel codice (che si disallineerebbe in silenzio). Oggi: `POST /api/platform/v1/webhooks/paddle` (firma HMAC,
  UC 0025); fuori dal pattern restano `POST /api/auth/{proxy+}` (UC 0015) e `POST /ingest/errors` (#08).
  Il **censimento esaustivo** (change 0039) ha verificato che tutto il resto sotto `/api/<app_id>/v1/*` è già
  `@Authenticated`/`@RolesAllowed`: l'authorizer **anticipa** all'edge un rifiuto che il servizio darebbe comunque.
- **Diritti GDPR esenti** dai gate (2)/(3): niente `@RequiresEntitlement` sulle risorse GDPR (#09 F31, già così nel core) —
  restano ovviamente **autenticati** (gate 1 si applica).
- **Health/Swagger**: non raggiungibili dalle rotte pubbliche (`/api/<app_id>/v1/*` non copre `/q/health/*`), quindi
  l'esclusione (#08 21) è vera **per costruzione**; Swagger gated platform-admin (#04 9).
- **Out-of-order/stato**: l'entitlement riflette `subscription` (aggiornata idempotente dal webhook, UC 0025).
- **Derivazione economica**: tabelle piccole → **nessuna cache** (#04 §7); nel servizio il read-model è memoizzato
  **per-richiesta** (`RestEntitlementService`) — la cache cross-richiesta resta un'evoluzione tracciata.

## 6. Risorse & runbook
**Risorse**: `aws_apigatewayv2_authorizer` di tipo **JWT** in `platform_shared` (`authorizer.tf`), agganciato dal modulo
`microsaas_app` alle route `/api/<app_id>/v1/*` via l'oggetto `shared` → **ogni app futura è protetta per costruzione**
(invariante #3). Nessuna Lambda, nessun accesso al DB, **costo $0**.
**Runbook**: deploy via UC 0005; l'edge appende già `X-Correlation-Id` (#08 4) e l'authorizer nativo non lo altera.
Verificabile solo in cloud (in locale il dev stack passa da Caddy, non da API Gateway): il **canarino** post-deploy è il test
e2e L3, che dipende dall'arrivo del webhook Paddle.

## 7. Dati toccati
Nessuno: l'authorizer nativo valuta solo la firma/scadenza del token e non legge il DB. I gate che leggono
`platform.subscription`/`app.status` sono nel servizio (UC 0027). Nessuna scrittura, nessun dato personale nei log dell'edge
(l'access log dello stage esclude l'IP sorgente per minimizzazione, #13).

## 8. Permessi & gate
- **Catena gate (#09 dec.30)**: **authN(401) [edge, questo UC]** → app-abilitata + entitled(402) → ruolo(403) → quota(429)
  [servizio, UC 0027].
- **Invarianti**: `tenant_id` dal JWT (rafforzato: firma/issuer/audience/scadenza verificati **prima** del servizio);
  entitlement **derivato**; nessuna tabella entitlements; **difesa in profondità** — il servizio ri-verifica comunque
  `token_use`/`client_id` (`AccessTokenGuardFilter`, UC 0016).

## 9. Requisiti di test
- **Terraform** (`modules/microsaas_app`, `modules/platform_shared`): la route dell'app nasce `authorization_type = "JWT"`
  agganciata all'authorizer condiviso; senza `public_routes` **nessuna** route scoperta; il webhook ha la sua route senza
  authorizer **senza** scoperchiare il proxy generico; l'authorizer è JWT nativo, legge il solo header `Authorization`, con
  issuer = pool e audience = app client BFF; l'ingress del RDS Proxy non contiene più CIDR.
- **Non-regressione dei contratti**: le suite frontend (401 → refresh silenzioso; 402 → banner entitlement) e backend (gate
  entitlement/ruolo/quota) devono restare **verdi senza modifiche** — è la verifica che la ripartizione scelta non ha
  intaccato nulla.
- **Security/multi-tenancy** (#10 D): coperture invariate in UC 0027 (entitlement per (tenant, app), nessun cross-tenant).
- Verifica che i **diritti GDPR** non siano bloccati dall'authorizer (#09 F31).

## 10. Riferimenti & Definition of Done
- **Decisioni**: #04 §7, #09 dec.12/dec.29/dec.30, #01 6, #02 8.
- **DoD** (rivista dalla change 0039):
  1. Le route `/api/<app_id>/v1/*` sono protette dall'authorizer JWT dell'edge; la deroga Checkov CKV_AWS_309 è **rimossa**
     e il check passa.
  2. Le rotte pubbliche by-design sono eccezioni **dichiarative** (route dedicate più specifiche), non allow-list nel codice.
  3. I gate app-abilitata/entitled restano nel servizio con i codici corretti (402), senza tabella entitlements e senza cache.
  4. Diritti GDPR non bloccati; health/Swagger non esposti.
  5. Suite Terraform verdi (incluso il nuovo modulo `platform_shared`); frontend e backend verdi **senza modifiche**.
  6. Residuo **E23-b** chiuso (ingress del RDS Proxy ristretto alle sole Lambda auth).

## Punti aperti / decisioni differite

_Tracciati dalla change `0014-use-case-0021-…` (console admin). Dettaglio in [_BACKLOG](../../_BACKLOG.md) "Console admin (UC 0021)"._

- ✅ **CHIUSO dalla change 0039 — Enforcement disable-app (gate 2).** Il blocco effettivo NON sta nell'authorizer (che è
  nativo e non legge il DB): sta nel servizio, dove `EntitlementReadModel` già scarta le app con `status != active`
  (UC 0027). La console admin (UC 0021) continua a togglare `platform.app.status`. **Rinuncia consapevole**: il blocco non
  agisce più *prima* del servizio — vedi il punto aperto "blocco all'edge" qui sotto.
- **Override entitlement per-tenant.** Oggi l'entitlement è derivato **solo** da `subscription` (+ baseline tier free). Se
  servirà una leva admin per-tenant-app (UC 0021 #16), va definita qui (modello gate) + schema in UC 0013.
- ✅ **CHIUSO dalla change 0039 — E23-b: stretta del security group del RDS Proxy.** L'ingress è passato dall'intera VPC
  (`var.vpc_cidr`) alle sole SG `auth-lambda` (UC 0015) e `pre-token-gen-lambda` (UC 0016). Verificato che i task Fargate
  NON usano il proxy (si connettono diretti al cluster). Coperto da `modules/platform_shared/tests/plan.tftest.hcl`.
- ⏳ **RESIDUO — E23-a: autenticazione IAM delle Lambda verso il RDS Proxy.** Il proxy resta `iam_auth = DISABLED`: le due
  Lambda auth si autenticano con **utente e password** dal segreto `auth_lambdas` (least-privilege, UC 0016). Il passaggio a
  `iam_auth = REQUIRED` (token temporaneo dall'identità della Lambda, **niente password in circolazione**) è **rimandato
  dalla change 0039** perché: (1) cambia il **codice di connessione di entrambe** le Lambda — `pg8000` in Python
  (`pre_token_gen/handler.py`) e Agroal/Quarkus in Java (`SecretsManagerCredentialsProvider`), meccanismi diversi;
  (2) richiede nuovi permessi IAM (`rds-db:connect`); (3) **non è provabile in locale** e, se sbagliato, si manifesta come
  "nessuna Lambda si collega più al DB" — mescolarlo alla **prima accensione cloud** (mai avvenuta) renderebbe la diagnosi
  molto più difficile. È un **irrobustimento**, non la chiusura di una falla: il segreto è già leggibile dalle sole due
  Lambda. **Prossimo passo**: intervento dedicato subito dopo la prima accensione riuscita. Vedi E23 in
  [_EVOLUZIONI-DEVOPS](../../_EVOLUZIONI-DEVOPS.md).
- ⏳ **NUOVO (change 0039) — Blocco all'edge per app disabilitata / tenant non entitled.** Con l'authorizer nativo i gate 2/3
  agiscono solo nel servizio: una richiesta di tenant non entitled **raggiunge comunque** il container (e, per le app,
  costa un salto verso il core, `RestEntitlementService`). **Rinuncia accettata** perché HTTP API v2 non sa esprimere
  402/401 da un authorizer custom (vedi riquadro in testa). **Se servirà davvero** un blocco a monte — per esempio per
  spegnere un'app che sta causando problemi, o sotto abuso da account autenticati — le strade sono: (a) una **regola WAF**
  sull'API (evoluzione E6, già tracciata); (b) tornare a un **authorizer Lambda con cache attiva**, accettando 403 al posto
  di 402/401 e adeguando il frontend; (c) rimuovere temporaneamente la route dell'app (kill-switch infrastrutturale).
  **Nessuna è da implementare ora**: si decide quando esisterà il bisogno misurato.
- ✅ **DECADUTO (change 0039) — Propagazione del correlation id attraverso l'authorizer.** Non c'è più codice nostro
  all'edge: `X-Correlation-Id` è appeso dall'integrazione (`microsaas_app/api.tf`) e l'authorizer nativo non lo altera.
  Nulla da fare.
- ➡️ **RIASSEGNATO (change 0039) — Eventi audit dei flussi auth.** Il punto sotto (convenzione `AuditLogger`) era in capo a
  questo UC perché si prevedeva una Lambda authorizer scritta da noi. Non esistendo più, l'emissione degli eventi audit
  (login/logout, tentativi falliti, cambio password/2FA, lockout) resta **interamente** alle Lambda auth: **UC 0015** (BFF
  auth) e **UC 0016** (Pre-Token-Gen). Il testo è conservato qui sotto per continuità storica.

_Tracciato dalla change `0035-use-case-0006-…` (osservabilità di base)._

- **Eventi audit dei flussi auth con la convenzione `AuditLogger`.** UC 0006 fissa la convenzione: eventi
  audit/sicurezza = log JSON con `log_type=audit` nell'MDC (API `AuditLogger` in `services/commons`), che il
  subscription filter instrada all'archivio 12 mesi (#08 28/29). Login/logout, tentativi falliti, cambio
  password/2FA, lockout devono emettere eventi con QUESTA convenzione quando nascono qui (le Lambda auth,
  fuori da Quarkus, devono produrre lo stesso formato JSON). La sezione "auth/sicurezza" della dashboard e
  l'allarme "picco login falliti" (#08 16) si completano qui.
- ✅ **CHIUSO dalla change 0039 — Esclusione health/Swagger dall'authorizer (#08 21).** Vera **per costruzione**: la route
  copre solo `/api/<app_id>/v1/{proxy+}`, che non intercetta `/q/health/*`. Nessuna esclusione da configurare.
- ✅ **CHIUSO/DECADUTO dalla change 0039 — Propagazione del correlation id attraverso l'authorizer.** Vedi sopra: nessun
  codice nostro all'edge.
