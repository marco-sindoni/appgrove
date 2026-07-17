# Implementation Log — Change 0037: Cognito + auth BFF (UC 0015)

**Branch**: `change/0037-use-case-0015-cognito-auth-bff`
**Aree**: `services/auth` (rename da `auth-local` + porta provider + provider Cognito + artefatto Lambda) · `infra/` · `.github/workflows/` · `frontend/` (fallback verify) · script dev · docs/compliance
**Completata**: 2026-07-17

## File modificati

| File | Azione |
|---|---|
| `services/auth-local/` → `services/auth/` | Rinominato (cartella, artifactId, package `app.appgrove.auth`) |
| `services/auth/.../IdentityProvider.java`, `MailSender.java` | Creati — porta del BFF (UC 0010 §6) + porta trasporto email |
| `services/auth/.../AuthResource.java` | Riscritto — strato HTTP unico e sottile (route, cookie, status); logica nei provider |
| `services/auth/.../local/LocalIdentityProvider.java`, `local/MailerMailSender.java` | Creati — impl Local (comportamento invariato), classi locali spostate in `local/` |
| `services/auth/.../cognito/CognitoIdentityProvider.java` + `CognitoConfig`, `CognitoClients`, `OpaqueTokens`, `SesMailSender`, `SecretsManagerCredentialsProvider` | Creati — impl Cognito completa (14 endpoint) |
| `services/auth/pom.xml` | Modificato — SDK AWS (cognito/ssm/sesv2/secretsmanager, UrlConnection), mockito test, **profilo Maven `lambda`** (`quarkus-amazon-lambda-http` → `function.zip`) |
| `services/auth/src/main/resources/application.properties` | Modificato — `auth.provider=local` default, blocco `%cloud` (Cognito, log JSON, credenziali DB da Secrets Manager) |
| `services/auth/src/test/java/.../cognito/*` (5 file) | Creati — 15 test provider Cognito (SDK mockato) |
| `infra/modules/platform_shared/auth.tf` | Creato — User Pool per env, app client confidenziale (TTL 15m/24h, rotazione refresh), client secret → SSM SecureString |
| `infra/modules/platform_shared/auth_lambda.tf` | Creato — bucket artefatti, SG, IAM, log group, Lambda nativa ARM64 + route `POST /api/auth/{proxy+}` (condizionali su `auth_lambda_s3_key`) |
| `infra/modules/platform_shared/ingress.tf` | Modificato — **CORS** con credenziali e origin espliciti; **throttling** 10 req/s burst 20 su `/api/auth/*` (dinamico) |
| `infra/modules/env_baseline/endpoints.tf` | Modificato — endpoint VPC `ssm` aggiunto |
| `infra/envs/{test,prod}/{main,outputs}.tf` | Modificati — var `auth_lambda_s3_key`, `spa_config.cognito` valorizzato, output `auth_lambda_deploy`, observability |
| `infra/modules/observability/{variables,alarms}.tf` + test | Modificati — allarme `Errors` Lambda auth |
| `.github/workflows/deploy-test.yml` | Modificato — job `build-auth` (nativa ARM64 sempre, upload S3 per-SHA) + `TF_VAR_auth_lambda_s3_key` |
| `.github/workflows/release-prod.yml` | Modificato — gate bloccante `function.zip`, promozione S3 test→prod stesso SHA, `TF_VAR` nel plan |
| `frontend/apps/backoffice/src/auth/authApi.ts`, `pages/auth/VerifyEmailPage.tsx` (+ test) + i18n | Modificati — verify senza auto-login (provider Cognito) → messaggio "accedi"; chiave `verify.confirmed` EN/IT |
| `app-start.sh`, `app-stop.sh`, `dev/lib/*`, `dev/Caddyfile`, `dev/docker-compose.yml`, `.gitignore` | Modificati — rename meccanico (`auth`, pid/log `dev/.auth.*`) |
| `services/commons/.../messaging/SqsMessageQueues.java`, `services/core/.../billing/SqsWebhookQueue.java` + `SqsMessageQueuesTest.java` | Modificati — **bugfix regressione dev di change 0036**: `queue-prefix` da `defaultValue=""` a `Optional<String>` + guardia di regressione |
| `services/{core,fatture,auth}/.../ArchitectureTest.java` (+ dep archunit in auth) | Modificati/Creato — **regola ArchUnit** che vieta `@ConfigProperty(defaultValue = "")` su campi e parametri (#10 37bis, provata con controprova) |
| `tools/smoke/boot-profiles.sh` | Creato — smoke: artefatti impacchettati avviati nei **profili di spedizione** (core/fatture→prod, auth→cloud) con env finte a specchio di task definition/Lambda, fino a "Listening on" |
| `tools/smoke/stack-headless.sh` | Creato — smoke **headless dello stack dev**: Postgres+ElasticMQ dal compose, `dev migrate`+seed, 3 servizi (build-profile dev) su porte alternative, health + **login vero** col seed |
| `run-tests.sh` | Modificato — nuova area **`smoke`** (nel giro completo di default) |
| `.github/workflows/verify-pr.yml` | Modificato — job `smoke` **non bloccante** (`continue-on-error`, path-filter dedicato); promozione a required tracciata in UC 0005 |
| `CLAUDE.md`, `docs/10-testing.md` | Modificati — area smoke documentata; decisione **#10 37bis** (tre guardie della classe "l'app non parte fuori dal profilo test") |
| `docs/compliance/manifests/platform.yaml` + `ropa.{it,en}.md` | Modificati — testo `services/auth`, RoPA rigenerata |
| `docs/usecases/…/0015, 0016, 0017, 0018, 0010, _INDEX` · `docs/_BACKLOG.md`, `_COSTI-AWS.md`, `_EVOLUZIONI-DEVOPS.md`, `docs/06-infra-iac.md` | Modificati — rimandi differiti, costi (+endpoint SSM), E22/E23, drift "pool dev" corretto |

## Cosa è stato fatto

Implementato UC 0015 con **contratto completo** e **servizio unico**: `auth-local` è stato rinominato
`services/auth` e ristrutturato attorno alla porta `IdentityProvider` (design UC 0010 §6) — strato HTTP
unico, impl `Local` invariata (dev) e impl `Cognito` nuova (test/prod) che mappa tutti gli endpoint su
Cognito (`InitiateAuth`/challenge TOTP, `REFRESH_TOKEN_AUTH` con rotazione cookie, `RevokeToken`,
`SignUp`/`ConfirmSignUp`, `ForgotPassword`/`ConfirmForgotPassword`, `AdminCreateUser` per gli inviti,
`AssociateSoftwareToken`/`VerifySoftwareToken`) mantenendo le scritture `platform` (signup/accept) e i
messaggi problem+json identici al locale. Terraform: Cognito per env + client confidenziale + Lambda
nativa ARM64 dietro l'API condivisa con CORS/throttling; pipeline: build `function.zip` (profilo Maven
`lambda`, GraalVM) + promozione per-SHA test→prod con gate bloccante.

## Decisioni prese

1. **Token opaco `base64url(email|codice)`** per verify/reset in cloud: mantiene il contratto a token
   unico; il link nelle email lo genererà il Custom Message Lambda (rimando → UC 0018).
2. **Verify cloud senza auto-login** (Cognito non emette token alla conferma): risposta
   `{status:"confirmed"}` + fallback nella SPA (rimando UX → UC 0017).
3. **Cookie refresh cloud = `base64url(sub|refreshToken)`**: il `sub` serve al `SECRET_HASH` del flusso
   refresh. Challenge 2FA = `base64url(email|session)`. Sempre e solo nel cookie HttpOnly.
4. **Credenziali DB = secret master Aurora via proxy RDS** (unico secret attaccato al proxy): ruolo
   dedicato least-privilege rimandato → UC 0016/0014 (tracciato lì + E23).
5. **Lambda sempre nativa in deploy-test** (a differenza delle immagini ECS JVM-default): su Lambda il
   cold start JVM non è praticabile e una chiave vuota distruggerebbe la Lambda all'apply.
6. **Risorse Lambda condizionali** (`auth_lambda_s3_key` vuota = non create): plan valido prima della
   prima build, attivazione a fasi conservata; pool/client/bucket/SSM sono invece sempre presenti.
7. **Endpoint VPC `ssm` aggiunto** (~$7/mese/env, unica via dalla VPC no-NAT al client secret):
   costi aggiornati in `_COSTI-AWS.md`, alternativa tracciata (E22).
8. **Schema DB dev-only resta `auth_local`** (stato credenziali del solo provider Local): rinominarlo
   avrebbe rotto i DB locali esistenti senza beneficio.
9. **Rafforzamento suite (#10 37bis, deciso col Platform Engineer)** — tre guardie contro la classe
   "l'app non parte fuori dal profilo `test`": regola ArchUnit (pattern vietato), boot smoke dei
   profili di spedizione, smoke headless dello stack dev. **Controprove eseguite**: col bug
   reintrodotto ad arte, la regola ArchUnit fallisce indicando il parametro esatto e lo smoke
   headless fallisce su core col log `Failed to load config value ... queue-prefix`; il boot smoke
   prod invece NON lo vede — correttamente: la task definition prod imposta `APPGROVE_SQS_QUEUE_PREFIX`
   (l'errore era solo-dev), a conferma della complementarità dei tre livelli. Scoperta collaterale
   incorporata: un jar impacchettato di default ha il wiring build-time "prod" (PaddlePaymentProvider
   non implementato) → lo smoke headless builda con `-Dquarkus.profile=dev` (stub provider), come il
   `quarkus:dev` di app-start. Il full `app-start.sh` in CI (browser/Caddy/TLS) è stato **scartato**
   come gate (fragilità: Colima, porte, mkcert); resta step manuale del DoD; job CI non bloccante con
   promozione a required tracciata in UC 0005.
10. **Bugfix regressione dev di change 0036 incluso qui** (approvato dallo sviluppatore in corso
   d'opera): `appgrove.sqs.queue-prefix` con `@ConfigProperty(defaultValue = "")` fa fallire l'avvio
   Quarkus (`SRCFG00014`) nei profili dove i bean SQS sono attivi (dev) — emerso solo alla prova dal
   vivo con `app-start.sh` perché quei bean sono `@UnlessBuildProfile("test")` e i test (profilo
   `test`) non li istanziano. Fix con `Optional<String>.orElse("")` in `SqsMessageQueues` (commons) e
   `SqsWebhookQueue` (core), stesso pattern già usato per `endpoint`. Estraneo a UC 0015 ma bloccava
   l'avvio locale (invariante "avvio locale" di CLAUDE.md). Verificato: `app-start.sh` ora tutto verde
   (sync-pricing e fatture ripristinati).

## Invarianti appgrove

- **Tenant ID solo dal JWT verificato**: nessun percorso legge il tenant da body/params; il BFF scrive
  la membership che il Pre-Token-Gen (UC 0016) trasformerà in claim; fail-closed conservato (token
  invalidi → 401 problem+json, `InvalidTokenException`).
- **Filtro row-level**: le sole scritture tenant-scoped (inviti) restano vincolate al tenant
  dell'invito individuato dal token single-use (comportamento identico al locale, condiviso in `PlatformWriter`).
- **Modulo `microsaas_app`**: non toccato — l'auth è piattaforma condivisa (`platform_shared`), come da requirements.
- **Logging strutturato**: profilo `%cloud` con log JSON su stdout (MDC di commons); mai credenziali/token nei log.

## Note per il revisore

- **Contratto cross-area**: `/api/auth/*` invariato per la SPA salvo il **verify** che col provider
  Cognito risponde `{status:"confirmed"}` senza token — gestito nel frontend (fallback implementato,
  testato); in locale nulla cambia.
- **Decisioni differite tracciate** (regola CLAUDe.md): ruolo DB dedicato Lambda auth → **UC 0016**
  (+E23); link email col token opaco + infra SES/raggiungibilità VPC + email default Cognito → **UC 0018**;
  UX auto-login post-verify in cloud → **UC 0017**; punto `spa_config` di UC 0005 → **risolto** (UC 0015);
  prima run live (build nativa CI, doppio giro bucket artefatti, smoke Cognito reale) → **_BACKLOG**
  (checklist attivazione, punto 8); evoluzioni E22/E23 in `_EVOLUZIONI-DEVOPS`.
- **Mai eseguito dal vivo**: come tutto il cloud (attivazione a fasi), la prima build nativa del
  `function.zip` e l'apply avverranno in CI all'attivazione; qui: suite verdi, `terraform
  fmt/validate/test`, checkov e actionlint puliti, `function.zip` verificato in build JVM locale.
- **Gate privacy (UC 0031): MINOR** — dettaglio in `requirements.md` ("Tocca dati personali?"):
  trattamento Cognito già censito nel manifesto/RoPA, nessun nuovo sub-processor (SDK AWS eu-west-1),
  segnali restanti = falsi positivi da rename; manifesto aggiornato e RoPA rigenerata.

## Test

- **Backend (`services/auth`)**: 35 test verdi — 20 preesistenti del provider locale (invariati:
  regression del refactor) + 15 nuovi del provider Cognito (login/challenge TOTP con `SECRET_HASH`
  verificato, refresh con rotazione cookie e hash sul `sub`, logout con revoca, signup→riga `platform`
  con sub Cognito, verify senza auto-login, token opachi malformati→400, reset, accept invito
  (SUPPRESS+password permanente+tenant/ruolo), send invito via SES) — attributi cookie
  (HttpOnly/Secure/SameSite=Lax/Path) e "refresh mai nel body" asseriti (requisiti security).
- **Frontend**: 85 test backoffice verdi (nuovo `VerifyEmailPage.test.tsx`: conferma senza auto-login,
  token invalido, token assente) + suite admin/packages + Playwright E2E L2 (17 pass). Nota: un primo
  giro rosso era dovuto alla `dist/` stale (gitignored) di `@appgrove/i18n` in locale — risolto
  ricompilando il pacchetto, nessuna modifica di codice.
- **Infra**: `terraform fmt/validate` test+prod, `terraform test` moduli (7 pass), checkov pulito
  (soppressioni documentate), actionlint verde (tflint non installato in locale: gira in `verify-pr`).
- **Compliance**: verde (parità lingue manifesti + freshness RoPA + test scanner).
- **`./run-tests.sh` completo**: verde su tutte le aree (backend, frontend con E2E, infra, compliance).
- **Smoke (nuova area `./run-tests.sh smoke`)**: verde — boot-profiles (core/fatture in prod, auth in
  cloud: "Listening on" con config finta) + stack headless (health 3 servizi + login `owner@acme.test`
  → 200 con token). Controprove col bug reintrodotto: ArchUnit rosso sul parametro esatto, stack
  headless rosso su core con l'errore `queue-prefix` nel log; ripristino → tutto verde. Regola
  ArchUnit verde su commons/core/auth/fatture; actionlint verde sul nuovo job verify-pr.
- **Prova dal vivo (`app-start.sh`)**: stack dev interamente verde — auth `:9100` (jwks 200), core,
  fatture, backoffice, admin su; sync-pricing e seed OK. Ha fatto emergere e verificare il **bugfix
  `queue-prefix`** (prima: `fatture 500` + `sync-pricing` fallito; dopo il fix: tutti ✓). La regressione
  non era coperta dai test perché i bean SQS sono `@UnlessBuildProfile("test")`: aggiunta **guardia**
  in `SqsMessageQueuesTest` che costruisce il bean con prefisso `Optional.empty()`. Backend ri-eseguito
  a stack spento (per evitare la collisione porta 8081 dei `@QuarkusTest`): 26 commons / 158 core /
  35 auth / 29 fatture, tutti verdi.

## Stato criteri di accettazione

- [x] `services/auth` espone il contratto completo con `IdentityProvider` a due implementazioni; suite del provider locale verde invariata.
- [x] Implementazione `Cognito` coperta da test con SDK mockato (flussi, cookie, fail-closed).
- [x] Terraform: User Pool + app client confidenziale + Lambda + route/throttling/CORS; `fmt`/`validate` puliti; `spa_config` valorizzato dagli output Cognito.
- [x] Pipeline: artefatto Lambda nativo ARM64 cablato in deploy-test/release-prod; i cablaggi che dipendono dall'attivazione (prima build live, doppio giro bucket) sono tracciati in `_BACKLOG`.
- [x] Punto differito UC 0005→0015 (`spa_config`) risolto e annotato in UC 0015.
