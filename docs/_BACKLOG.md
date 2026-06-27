# Backlog — topic trasversali da discutere

Lista dei temi sollevati durante le sessioni di decisione, da affrontare nell'argomento giusto (o in uno dedicato).

## Compliance & privacy (GDPR) — richiesto 2026-06-14
Probabilmente merita un **documento dedicato** (nuova area, es. `13-compliance-privacy.md`). Da coprire:
- **GDPR**: basi giuridiche, data retention, diritto all'oblio/erasure (già impostato a livello dati in [05-persistenza-dati](05-persistenza-dati.md) §12), portabilità, DPA.
- **Tracciamento dati comportamentali utenti**: cosa si traccia, consenso/cookie banner, anonimizzazione, finalità.
- **Log**: PII nei log, retention, masking (border con [08-observability](08-observability.md)).
- **Privacy policy & T&C vs impianto di log tecnici** (richiesto 2026-06-20): la policy deve dare al cliente la
  **visibilità dovuta per legge** sul trattamento dei log tecnici (finalità, categorie di dati, retention, basi
  giuridiche, eventuale profilazione/sicurezza) MA con **principio di minimizzazione informativa**: aderenza piena a
  legge+GDPR **senza** "dump" pubblico dell'architettura/dettagli implementativi (sicurezza). Descrivere il *cosa/perché*,
  non il *come* tecnico. Allineare con #08 (log/retention/no-PII) e #13.
- **Funzionalità GDPR dentro le applicazioni**: export/cancellazione dati per-tenant/per-utente lato app.
- **Tool generico di export/erasure per-applicazione** (richiesto 2026-06-20 — DA ANALIZZARE, #13 D/L):
  framework riusabile che ogni app implementa per **esportare** i propri dati (formato dati **diverso app per app** →
  serve un contratto/interfaccia comune che ogni servizio realizza, es. `exportTenantData()`/`purgeTenantData()`).
  **Recesso per-singola-app**: l'utente con app A,B,C può recedere da **C** mantenendo A,B (entitlement per-app #09,
  schema `app_<id>` #05). **Modello semplice preferito** (se la normativa lo permette): esporti C → **conferma esplicita
  "ho esportato, puoi cancellare"** → cancellazione immediata dei dati live di C; A,B restano attive. Accortezze:
  (1) purga via **EventBridge per-tenant** (#06 H); (2) dati nei **backup/PITR** spariscono col ciclo di rotazione
  (dichiararlo in policy, non istantaneo); (3) **audit log della cancellazione** conservato come prova (#08).
  Definire i **periodi di retention** per categoria. Eventuali obblighi fiscali → in capo a **Paddle (MoR)**.
- **Privacy Policy & Terms and Conditions**: tenuto conto che **Paddle è Merchant of Record** (gestisce
  tax/fatturazione, ma privacy/T&C del servizio restano a noi) — border con [09-pagamenti](09-pagamenti.md).

## Strategia di attivazione ambienti "a fasi" (cost-min) — richiesto 2026-06-20 — DISCUTERE PRESTO
**Vincolo trasversale su tutto il DevOps**: nessuna accensione di infrastruttura "early" che aumenti i costi.
Fasi previste:
1. **Fase 1 — solo locale**: sviluppo 100% offline, **zero AWS** (#11). Nessun `apply` su nessun env.
2. **Fase 2 — accendi solo `test`**: quando si inizia a mettere su l'infra e provarla. `test` con **scale-to-0**
   (Aurora) + **autospegnimento notturno** Fargate (cron `test-stop`, #07 §28). Avvio on-demand.
3. **Fase 3 — accendi `prod`**: **solo appena prima del go-live**.
Implicazioni da verificare quando si discute: il bootstrap Terraform (state, OIDC role, stack `global`) e le pipeline
devono **non forzare** la creazione di test/prod prima del momento scelto; `global` (Route53/Cognito dev) va valutato
quando serve davvero (registrazione dominio = quando si attiva test?). Allineare CLAUDE/#06/#07/#11/#12 a questa logica.

## Configurazione admin — richiesto 2026-06-14
Già parcheggiato negli scope di [03-frontend](03-frontend.md) (pannello admin in generale) e
[09-pagamenti](09-pagamenti.md) (config admin del modello di costo per-app).

- **Disabilita applicazione** (feature admin, richiesto 2026-06-20): in fase iniziale un'app può "non vendere"/avere
  cattivo riscontro → l'admin deve poter **disabilitare** l'app rendendola **indisponibile a tutti i tenant**
  (catalog/entitlements), operazione **reversibile** che **NON** tocca dati/infra. Distinta dalla skill distruttiva
  `drop-application` (vedi sotto). Da progettare nel pannello admin (#03) + modello dati (#05 apps/entitlements).

## Brand kit & design system (#14 F)
- **Pacchetto token condiviso nel monorepo** (#14 F2/F3): estrarre i token dai mockup [docs/frontend-design/](frontend-design/)
  in un brand kit unico (colori light/dark, type scale Plus Jakarta Sans/JetBrains Mono, radii/ombre/spacing, logo
  light/dark, Material Symbols, nota stile illustrazioni). Fonte unica per backoffice SPA + admin SPA + sito vetrina +
  landing generate da `new-application`. Base del design system #03. **Artwork logo finale** (foglia-in-quadrato) = task
  di produzione (anche AI). **Illustrazioni custom AI-assistite** dentro uno stile definito (poche, on-style).

## Feature deprioritizzate (note)
- **Search globale dal menu del workspace** (sidebar backoffice, presente nei mockup #03) — **NON prioritaria** (richiesto
  2026-06-21): l'utente non è particolarmente interessato a questo use case al lancio. Resta nei mockup ma a bassa priorità
  di implementazione; rivalutare quando il numero di app/sezioni per tenant lo rende davvero utile.

## Dettaglio funzionalità / use case — richiesto 2026-06-14
Le decisioni di [03-frontend](03-frontend.md) (e affini) fissano stack/architettura/UX a grandi linee. Resta da
**progettare in dettaglio tutti gli use case** delle varie funzionalità (backoffice, moduli app, console admin):
flussi, schermate per stato, edge case, validazioni, permessi per ruolo. Da affrontare in sessioni dedicate per area/app.

- ✅ **FATTO (2026-06-16)**: casi d'uso di **autenticazione e registrazione** → [usecases/01-auth-registrazione](usecases/01-auth-registrazione.md) (UC1–UC10). Resta solo la stesura dei **testi** dei template email EN/IT.
- **DA PROGETTARE (priorità BASSISSIMA) — use case "Pausa subscription self-service"** (richiesto 2026-06-21, da #09 E):
  sospensione/ripresa di un abbonamento da parte dell'utente (Paddle pause/resume). **Non al lancio**; lo status `paused`
  è comunque gestito (= no accesso). Evoluzione futura.
- **DA PROGETTARE — use case "Gestione abbonamento self-service"** (richiesto 2026-06-21, da #09 G): sezione backoffice
  "Abbonamenti" — vista status/tier/fine periodo/cambi programmati; **upgrade/downgrade** (con gating `flow`/`stock` E23 e
  UX downgrade programmato E22); **disdici/riattiva** (E25); display uso quota/banner; pulsante **"Gestisci pagamento e
  fatture"** che apre la sessione Customer Portal Paddle (generata server-side). Edge: dunning/`past_due` banner, scaduto.
- **DA PROGETTARE — use case "Acquisto / checkout"** (richiesto 2026-06-21, da #09 A/C): schermata di **scelta tier**
  con doppio billing mensile/annuale e **default annuale + sconto** (#09 A); **checkout overlay** Paddle.js iniziato
  lato server (token, custom_data `tenant_id` dal JWT + `app_id`); **UX post-checkout a stati con polling** (spinner
  "attivazione in corso" → "attivato" → fallback rassicurante oltre ~30–60 s, **mai errore**: il pagamento è già
  riuscito, l'entitlement arriva via webhook). Edge: utente chiude l'overlay, pagamento fallito/abbandonato, retry.
- **DA PROGETTARE — use case "GDPR / diritti dell'interessato & gestione account"** (richiesto 2026-06-20, da #13 D):
  i diritti self-service decisi in #13 D diventano casi d'uso in `docs/usecases/` (flussi/schermate/stati/edge): "I miei
  dati"+download (accesso/portabilità), rettifica profilo, **elimina account** (totale) vs **recedi da app** (per-app,
  esporta→conferma→cancella), **unsubscribe** newsletter, opposizione a legittimo interesse, **tooling admin B2B**
  (export/cancellazione dati dei propri utenti = assistenza al titolare), canale `privacy@` + workflow tracciato (SLA 1
  mese) + verifica identità. **Consenso (da #13 F)**: subscribe box newsletter (vetrina) + checkbox non pre-spuntata al
  signup + **centro preferenze consensi** in impostazioni account (hub unico, revoca facile) + disclosure cookie tecnici.
  Da fare insieme al dettaglio UX backoffice/account e al contratto per-app export/erasure.
- **DA PROGETTARE — Console "Diritti GDPR" (admin, single pane of glass)** (richiesto 2026-06-20, #13 L): vista di
  **aggregazione** read/ops per platform-admin che convoglia tutti gli "oggetti" da esercizio diritti (export con link
  S3+scadenza, recessi per-app, eliminazioni account+stato grace, cambi consenso, ticket privacy), con stato/timeline e
  **deep-link all'accessorio** (CloudWatch Logs Insights pre-filtrato, job export/oggetto S3, registro audit/breach).
  **Scoped alla retention** (#13 E). NON è un nuovo store: aggrega ticketing in-house (I) + export job (D) + audit (#08).
  Vive in console admin (#03) / capability core compliance (#04). Use case dedicato.
- **DA PROGETTARE — Ticketing nativo in-house** (capability di piattaforma, richiesto 2026-06-20, #13 D/I): sostituisce
  Jira (purismo: zero sub-processor, PII in casa, EU). Entità `support_ticket` (#04 core) + UI backoffice (utente) +
  console admin (agente). Copre **privacy (tipo speciale, SLA legale 1 mese, auto-creato da eventi) + supporto generico
  best-effort**. **MVP minimale, NO allegati**: tipo/priorità/stato, thread messaggi utente↔admin, notifiche email SES,
  trigger auto (form in-app, `privacy@`/`support@` via SES→Lambda, eventi export FAILED / escalation art. 9). Use case dedicato.

## Data breach — runbook, registro, security.txt (#13 J)
- **Runbook IR interno** (`docs/compliance/breach-runbook.md`) + **registro breach interno** (art. 33.5) — da redigere.
- **`security.txt`** + `security@appgrove.app` per responsible disclosure (al lancio vetrina).
- **Template notifiche** (Garante/interessati/controller) → deliverable pre-go-live (L12).
- Skill **`breach-response`** → memoria `skills-backlog`.

## Sito vetrina (marketing) — richiesto 2026-06-20
Nuovo artefatto, distinto dalle 2 SPA (#03 backoffice+admin). **Statico**, **multilingua EN/IT/FR/ES/DE**, **contenuti
come file `.md`** (fonte unica: gli stessi md servono sito + rendering in-app delle policy). Ospita **Privacy Policy, ToS,
cookie disclosure** (5 lingue, IT facente fede), **subscribe newsletter** (#13 F), **Plausible Cloud analytics cookieless (EU)**
(#13 B/F/I). Build statica (SSG/Vite) su S3+CloudFront. Versioning policy: **versione + `effective_date`** nel front-matter,
git-backed; **check CI** presenza di tutte le 5 lingue per componente. Da definire in area frontend/#03 (estensione) o doc dedicato.
- **NOTA MARKETING/POSIZIONAMENTO** (richiesto 2026-06-20): l'essere **"all-EU deployed" con garanzie privacy EU**
  (residency UE, fornitori UE/in-house, postura purista #13 I) è un **elemento di vanto da valorizzare nel sito vetrina**,
  anche per diffusione **worldwide**: *"appgrove offre a tutti le massime garanzie di privacy secondo le normative UE"*.
  Trasformare la compliance in **proposta di valore** (badge/sezione dedicata, copy multilingua).

## Pagamenti (Paddle, #09) — story di implementazione (richiesto 2026-06-21)
Story emergenti dalle decisioni #09 (tracciare con accuratezza). **Prerequisito**: vedi blocker #14 (sito) per ogni uso
del vero Paddle, sandbox incluso — lo **stub locale** (sotto) è l'unica via non bloccata.

### Sviluppo locale — Stub Paddle (ESSENZIALE, #09 I) 🔑
- **Port `PaymentProvider`** (interfaccia di astrazione, come il provider auth locale #11 B) con due adattatori:
  **`StubPaymentProvider`** (profilo dev) e **`PaddlePaymentProvider`** reale (test/prod). Selezione per profilo.
- **Paddle.js finto** (frontend, profilo dev): stessa interfaccia del vero (`Paddle.Checkout.open(...)` + event callback),
  mostra una mini-UI finta / auto-conferma ed emette l'evento sintetico `checkout.completed`.
- **API Paddle finta** (`StubPaymentProvider`): `createTransaction`/`createCustomer`/`createPortalSession`/`syncPrices`
  ritornano ID plausibili (transaction/customer/price id finti), zero rete esterna.
- **Emettitore di webhook sintetici firmati**: genera eventi Paddle (firma HMAC col secret di test) e li invia
  all'endpoint locale → passano per la **pipeline reale** (Lambda ingest → SQS via **ElasticMQ** #11 → consumer →
  `subscription`). Tutto il nostro flusso gira davvero in locale; finto è solo Paddle.
- **Scenari ciclo di vita** (comando `dev` / pannellino): far scattare a comando happy path, `payment_failed`/`past_due`
  (dunning), `canceled`, **upgrade/downgrade** → simula tutto #09 E in locale. Integrare nei **script `dev/`** (#11 C).
- **Tunnel opt-in** (es. cloudflared) per webhook reali dal sandbox in locale (debug occasionale, richiede #14+account).

### Backend billing (capability core #04)
- **Checkout server-initiated** (#09 C14): endpoint che legge `tenant_id` dal JWT, risolve `paddle_price_id`, crea la
  transazione Paddle (custom_data `{tenant_id, app_id}` + customer lazy C15), ritorna il checkout token.
- **Pipeline webhook** (#09 D19): **Lambda ingest** (verifica firma HMAC, dedup `event_id`, accoda) + **consumer** SQS
  idempotente (out-of-order via `occurred_at`) + **DLQ + allarme** (#08). Mapping eventi → `subscription` (D21).
- **Modello dati** (#09 B): tabelle `app`/`app_tier`/`app_price` (catalogo) + `subscription` (tenant) + `paddle_customer_id`
  su account; **entitlement derivato** (B12). Migrazioni Flyway.
- **Contratto/SPI di quota** (#09 A5/E23/F32): interfaccia generica che ogni app implementa; metrica `flow`/`stock`,
  finestra, tetto; enforcement **hard-limit** + gestione "sopra capacità" su downgrade `stock`.
- **Catena di gate enforcement** (#09 F30): app-abilitata → entitled (402) → ruolo → quota (429), centralizzata nel layer
  di piattaforma; **diritti GDPR esenti** dai gate (F31).
- **Endpoint stato entitlement** per il **polling post-checkout** (#09 C17).

### Pricing-as-code & sync (#09 H)
- **Definizione pricing nel repo** (config versionata, fonte di verità importi/tier/limiti/ciclo) + **chiave interna
  stabile `price_id`**.
- **Step di sync in pipeline** (test→Paddle sandbox, tag→Paddle production): idempotente, crea i mancanti, **archivia** i
  rimossi, **mai** muta l'importo di un price vivo, non cancella price con subscription attive (grandfathering); riempie
  `app_price.paddle_price_id` **per ambiente**.
- Skill **`new-application`** (pricing iniziale + co-pilota flow/stock) e **`pricing-change`** (cambi successivi) →
  memoria `skills-backlog`.

### Test del flusso pagamento (#09 D20) — 3 livelli
- **L1** integration webhook esaustivo (per-PR, bloccante): payload sintetici firmati, Testcontainers Postgres.
- **L2** E2E Playwright dei nostri pezzi (per-PR, bloccante) con **Paddle.js mockato**.
- **L3** smoke reale su **Paddle sandbox** (**pre-release**, nel flusso tag→prod, confluisce nel gate; **override manuale**
  con motivazione se sandbox down). Richiede #14 + account.

### Leve di business (#09 K)
- **Bundling (BASSISSIMA priorità, #09 K50)**: più app in un unico abbonamento = una transazione → quota fissa Paddle
  ($0.50) diluita. Leva futura per migliorare il margine su pacchetti di app economiche. Richiede modellare un "bundle"
  sopra i tier per-app. Non al lancio.
- **Riconciliazione netto/revenue** (#09 K51): Paddle paga al netto delle fee su schedule di payout; osservabilità del
  netto incassato (non un blocco). Nota ops.

### Use case (già tracciati sopra)
"Acquisto / checkout", "Gestione abbonamento self-service", "Pausa subscription" (bassissima priorità).

## Backoffice shell (UC 0020) — rinvii cross-area (richiesto 2026-06-26)

Emersi implementando la shell SPA (change `0011-use-case-0020-…`). Gli item **solo-frontend** sono nei "Punti aperti"
di [usecases/06-frontend/0020](usecases/06-frontend/0020-shell-spa-backoffice.md); qui i **trasversali**:

- **Endpoint entitlement nel core (contratto frontend↔core)** 🔑 — la sidebar "YOUR APPS" è *manifest ∩ entitlement del
  tenant* (#01 dec.10), ma il core (UC 0013) **non espone** un endpoint che serva gli entitlement (derivati da
  `platform.subscription`, #09 dec.12). La shell usa per ora un **provider stub** (`StubEntitlementsProvider`, set
  statico). **Serve un endpoint core** (es. `GET /api/platform/v1/entitlements` → `app_id[]` del tenant dal JWT) da
  consumare via api-client + TanStack Query; la sostituzione tocca **solo** quel provider. **Owner**: area core/billing
  (probabilmente lo stesso UC che materializza `subscription` via pipeline webhook #09 D, o un piccolo UC core dedicato).
  Allineare con la "catena di gate enforcement" #09 F30 (402) e l'**endpoint stato entitlement per polling post-checkout
  #09 C17** (vedi sopra): valutare se è lo **stesso** endpoint.
- **`legacy-peer-deps` nel frontend** (DevX/tooling) — `frontend/.npmrc` imposta `legacy-peer-deps=true` perché il
  monorepo usa **TypeScript 6** (ultima major) mentre alcune librerie (es. `react-i18next`, `openapi-typescript`)
  dichiarano ancora un peer **opzionale** `typescript@^5` non aggiornato → npm strict lo tratta come conflitto. Solo
  rilassamento del check peer in install (nessun effetto a runtime). **Follow-up**: rimuovere quando l'ecosistema
  aggiorna i peer a TS6.

## App modules frontend (UC 0052+) — rinvii cross-area (richiesto 2026-06-27)

Emersi implementando il primo modulo app reale (`fatture`, change `0016-use-case-0052-…`):

- **i18n dei moduli app (EN/IT) — standardizzazione** 🔑 — la shell e le pagine di piattaforma sono bilingui (EN/IT via
  `@appgrove/i18n` con switch lingua), ma i **moduli app** (demo, fatture) hanno **stringhe per-modulo inline in italiano**
  (#03 dec.6: "le stringhe app sono per-modulo"). Manca una convenzione su **come** un modulo app espone le proprie
  stringhe in più lingue (namespace i18n per-modulo? bundle co-locato registrato nello switch lingua della shell?). Per ora
  IT-only, coerente col modulo demo e con la lingua di prodotto. **Owner**: da decidere fra #03 (contratto moduli) e #19
  (design-system/i18n); rilevante per ogni nuova app (`new-application`, UC 0046). Tracciato anche in
  [usecases/11-apps/0052](usecases/11-apps/0052-app1-modulo-frontend.md).

## Console admin (UC 0021) — rinvii cross-area (richiesto 2026-06-27)

Emersi implementando la console admin come **slice verticale MVP** (change `0014-use-case-0021-…`): nuova SPA `apps/admin`
+ `AdminResource` nel core (read-only **cross-tenant** via query native, gated `platform-admin`, unico write `app.status`).
Registro canonico anche in `changes/0014-use-case-0021-…/requirements.md`. Item per **UC proprietario**:

- **#1 Subscription: entità/lifecycle + drift Paddle reale → UC 0025** (pipeline-webhook). Ora: lettura native read-only; il
  Billing admin mostra solo lo **stato locale**, non il drift vs Paddle.
- **#2 Catalogo app (`app`/`app_tier`/`app_price`): dominio/entità → UC 0022** (pricing-as-code). Ora: lettura native + `UPDATE app.status`.
- **#3 Creazione/modifica subscription (checkout) → UC 0024**. Ora: l'admin vede le subscription esistenti (seed), non le crea.
- **#4 Enforcement gate disable-app (403 runtime) → UC 0014** (Lambda authorizer "app-abilitata") **+ UC 0027** (enforcement runtime).
  Ora: solo toggle `app.status` + esclusione delle app `inactive` dalla matrice admin; **nessun blocco effettivo** dell'accesso tenant.
- **#5 Provider entitlement reale del backoffice (sostituire `StubEntitlementsProvider`)** — può **riusare la derivazione**
  entitlement (tenant×app da `subscription` + `app.status`) introdotta qui nell'admin. Vedi anche il punto UC 0020 sopra. **Owner**: core/billing.
- **#6 `platform-admin` in cloud → UC 0016** (pre-token-gen) **+ UC 0015** (Cognito BFF). Ora: in locale il claim arriva da auth-local (`admin@appgrove.test`).
- **#7 SSO cross-sottodominio** (cookie su `api.appgrove.app` valido per `app.`+`admin.`) **→ UC 0015**. Ora: `admin.local` è origin separato → login proprio per-host.
- **#8 Hosting prod SPA admin** (CloudFront+S3/OAC+alias Route53) **→ UC 0055** (già pianificato: "2 distribuzioni CloudFront, backoffice+admin") **+ ACM/zona UC 0003**. Ora: solo dev (Caddy `admin.local` + Vite :5174).
- **#9 Pipeline FE pubblica il bundle admin → UC 0005** (CI/CD).
- **#10 Audit persistito delle azioni admin** (disable-app) + archivio 12 mesi **→ UC 0035** (archivio audit) **+ #08**. Ora: solo **logging strutturato** (actor `sub`, `app_id`, esito).
- **#11 KPI ricchi Overview** (MRR/churn) **→ #08/UC 0025**. Ora: KPI base da conteggi nativi.
- **#12 Estrazione base `Table` + stati compositi → UC 0019** (anche in "Punti aperti" di UC 0021/0020).
- **#13 Integrazione SPA admin nel comando canonico `dev`/`dev service` → UC 0009/0046**. Ora: solo `app-start.sh` (:5174) + blocco Caddy `admin.local`.
- **#14 Formalizzazione pattern "endpoint admin non-tenant-scoped" → UC 0013 / doc #02 auth-sicurezza**: come/dove disabilitare in sicurezza il filtro `@TenantId`, test anti-leak sistematici. Ora: query native + gating `platform-admin` (eccezione esplicita all'invariante #2).
- **#15 Console "Diritti GDPR" → UC 0034** (già escluso da UC 0021; nessuna azione, solo confine).
- **#16 Override/toggle entitlement per-tenant** (dettaglio Account, oggi **read-only**) **→ UC 0014/0027** (modello) **+ UC 0013** (schema). Nel modello attuale l'entitlement per-tenant ha **una sola leva** (la subscription): un override per-tenant-app non esiste in schema → decisione di data-model rimandata ai gate.
- **#17 Azione admin sulla subscription** (sospendi/cancella per tenant) **→ UC 0024/0025** (Paddle-coupled).
- **#18 Estrazione runtime auth/sessione condiviso backoffice+admin** — oggi **duplicato** il sottoinsieme minimo in `apps/admin` (config/auth/api). **Owner**: frontend condiviso (rinvio UC 0020) — valutare un pacchetto `@appgrove/app-runtime`.

## Script / tooling DevOps
- **Start/stop servizi test** (scale 0↔1 task Fargate) — ✅ deciso in [07-devops-cicd](07-devops-cicd.md) §28
  (avvio manuale `test-start`; spegnimento via **cron giornaliero `test-stop`**, idempotente, orario UTC fisso).
  Resta l'**implementazione** degli script `infra/scripts/test-start|test-stop` + workflow.
- **Workflow GitHub Actions** (YAML) da implementare: verifica-PR (`plan`+test), deploy-test, release-prod (tag+gate),
  flyway task ECS one-shot, frontend, cron `test-stop`. Specifica → [07-devops-cicd](07-devops-cicd.md).

## Dev locale — script `dev/` + README (#11)
- Implementare gli script **`dev/`** (#11 C): `setup` (one-time idempotente), `up`/`down`, `seed`/`reset`/`migrate`,
  `service <app_id>`, **`dev doctor`** (preflight). Orchestrazione **ibrida** (#11 A): Compose (Postgres, provider auth
  locale, reverse proxy, Mailpit, **MinIO**, **ElasticMQ**) + Quarkus dev mode + Vite. **README estremamente chiaro**
  (copia-incolla, output atteso, troubleshooting). Provider auth locale (#11 B): JWT/JWKS locali + claim dal DB.

## Skill Claude Code da creare — richiesto 2026-06-14
- **`new-application`** (sostituisce il vecchio "setup-nuova-applicazione") — **decisa in [07-devops-cicd](07-devops-cicd.md) §G**:
  `/new-application <descrizione breve>` → scaffold **frontend + backend**, chiama **`service-add`** (modulo `microsaas_app`),
  genera workflow CI + wiring `config.json`, logging strutturato di default, e ogni altro bootstrap. **Segue il workflow
  di `new-change`** (crea branch, lascia la PR all'utente). **Quando**: dopo #07 e **idealmente dopo #08 (observability)
  e #10 (testing)**, così lo scaffold nasce già con metriche/log e test pronti. Dettaglio in memoria `skills-backlog`.
  Deve generare anche lo **scaffold di test** (#10): unit/integration/security-isolamento (harness cross-tenant), E2E
  Playwright base, seed-base multi-tenant, e **encodare la regola "mai aggiornare baseline snapshot alla cieca"**.
  La skill `new-change` (esistente) va aggiornata con gli stessi gate di test/snapshot (#10 J).
- **`new-application` + `new-change`: gate privacy/RoPA** (richiesto 2026-06-20, #13 C): `new-application` deve **obbligare**
  a compilare il **manifesto dati per-app** (categorie dati personali/finalità/base/retention) — fonte unica per RoPA +
  tool export/erasure. `new-change` deve **intercettare** i cambiamenti che toccano dati personali (migrazioni Flyway con
  nuove colonne, nuovi campi entità/DTO/API, nuove integrazioni esterne, modifiche retention/finalità), **classificarli**
  e **aggiornare manifesto + RoPA contestualmente**. La classificazione è **assistita/co-pilota** (non checklist passiva):
  la skill ragiona con l'utente, elicita lo scopo del campo, **propone con motivazione** natura/finalità/base/retention,
  fa domande di approfondimento solo se ambiguo, propone-e-fa-confermare. **Escalation forte per categorie particolari art. 9** (DPIA #13 K);
  **enforcement CI bloccante** (campo `@PersonalData` non dichiarato nel manifesto → build rossa, stile ArchUnit #10 D).
  **Anello versioning/re-accept (#14 C18, richiesto 2026-06-21)**: la classificazione del gate **pilota il bump di
  versione** del componente Privacy/ToS interessato (#13 G41) — cambio **materiale** (finalità/basi/categorie/retention) →
  **bump major** → **ri-accettazione scoped** ai soli utenti vincolati; non materiale → **bump minor** → notifica.
- **RoPA versionato** `docs/compliance/ropa.md` (#13 C): sezione piattaforma + sezione per-app assemblata dai manifesti.
- **`pricing-change`** (richiesto 2026-06-21, #09 H) — co-pilota per i **cambi di pricing successivi** di un'app esistente
  (`new-application` scrive il pricing iniziale): aggiungi/rimuovi tier, cambia prezzo, cambia limiti (metrica `flow`/`stock`
  #09 E23), aggiungi mensile/annuale. **Env-agnostica**, scrive solo il **pricing-as-code** nel repo, **segue `new-change`**
  (branch+PR), **nessun dialogo diretto con Paddle**. Gestisce **immutabilità prezzi Paddle** (cambio importo = nuovo Price
  + archivia vecchio) e **grandfathering** (esistenti restano vs migrazione). Il **sync a Paddle** è nella **pipeline di
  deploy** (test→sandbox, tag→production), `paddle_*_id` per-ambiente nel DB catalogo riempiti dal sync. Memoria `skills-backlog`.
- **`finalize-landing`** (richiesto 2026-06-21, #14 gate finalizzazione) — finalizza/pubblica la **landing** di un'app
  quando è MVP/beta (`new-application` crea la bozza `draft`): **screenshot reali via Playwright** + seed (#10 I) per lingua,
  **copy rifinito** AI on-brand, **OG image**, **review interattiva** 5 lingue, poi `draft → published`. Build Astro
  renderizza solo `published`. Crea contenuti via `new-change` (deploy = CI). `new-change` segnala landing stale →
  propone re-run. Ambienti sito: **locale** (preview), **test** (basic auth + noindex), **prod** (pubblico, gate published +
  noindex pre-lancio). Memoria `skills-backlog`.
- **`campaign-guide`** (richiesto 2026-06-21, #14 J) — **guida passo-passo** per creare campagne ads (Meta/Google)
  **rispettando la postura privacy** (cookieless, **no pixel/CAPI-PII**, no banner, EU-purista): obiettivi ammessi
  (Traffico/Lead Form native), **convenzioni UTM** per attribuzione **Plausible** cookieless, **copy/creatività AI
  on-brand** (tono F1, dec. 35), **checklist di conformità** a ogni step. L'utente non è esperto di advertising.
  **Evoluzione futura**: assistente **Playwright non-headless** che pilota la UI di creazione campagna. Memoria `skills-backlog`.
- **`breach-response`** (richiesto 2026-06-20, #13 J) — co-pilota **data breach**: guida la valutazione del rischio
  (albero soglie), decide notifica/non-notifica (Garante art. 33 / interessati art. 34 / esenzione cifratura art. 34.3),
  redige la voce del **registro breach** (art. 33.5) e i **draft notifiche** (Garante/interessati/controller B2B) IT/EN.
  Memoria `skills-backlog`. (Vedi anche sezione "Data breach" sopra.)
- **`drop-application`** (richiesto 2026-06-20) — **inverso** di `new-application`: **decommissioning DevOps completo e
  irreversibile** di un'app. Cancella **tutte** le risorse AWS create per quell'app (ECS service/task, ECR repo, route API,
  schema `app_<id>` + ruolo DB, coda SQS, SSM/Secrets, log group, dashboard/allarmi, ecc.) via `service-remove` /
  `terraform destroy -target` — **zero risorse residue**. **Guardrail obbligatori** (operazione distruttiva non reversibile):
  1) chiede **quale applicazione** (descrizione) → la skill **identifica e mostra l'`app_id` risolto** per conferma;
  2) **prima conferma** sì/no;
  3) mostra l'**elenco esatto delle risorse** che verranno cancellate + avviso irreversibilità;
  4) **conferma definitiva** digitando una **frase casuale generata dalla skill** (es. "oggi la temperatura esterna è gradevole").
  Rispetta le safety di [06-infra-iac](06-infra-iac.md) §K (in prod: valutare snapshot finale del DB prima del drop).
  Distinta da "disabilita applicazione" (reversibile, runtime). Dettaglio in memoria `skills-backlog`.
