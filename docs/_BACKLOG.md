# Backlog — topic trasversali da discutere

Lista dei temi sollevati durante le sessioni di decisione, da affrontare nell'argomento giusto (o in uno dedicato).

## Prodotto — "Ready for AI": app dell'ecosistema distribuite come strumenti richiamabili dagli assistenti AI (richiesto 2026-07-25) — GRANDE 🤖

Sollevato dall'utente durante la change `0047-use-case-0037-…` (contenuti marketing della homepage). Idea di prodotto
**nuova e strategica**: ogni applicazione del marketplace è esposta anche come **strumento richiamabile dagli assistenti
AI** tramite **MCP** (Model Context Protocol — lo standard aperto con cui gli assistenti AI, es. ChatGPT, Claude,
Perplexity, invocano strumenti e servizi esterni). In pratica l'utente, dalla chat del suo assistente AI, chiede in
linguaggio naturale di fare un'operazione (es. "emetti la fattura al cliente X") e l'assistente la esegue chiamando l'app
di appgrove. È un **secondo cuneo (wedge) differenziante** accanto alla privacy EU, ed è ottimo anche per la GEO
(essere non solo citati, ma **operativamente richiamabili** dagli assistenti).

**Stato attuale**: la capacità **non esiste** ancora nel prodotto. L'utente la introdurrà con **use case dedicati**
(server MCP per app, autenticazione delegata, mappatura operazioni→strumenti, quota/entitlement sulle chiamate AI,
sicurezza e audit). Da valutare tra l'altro: dove vive il server MCP (per-app in `services/<app>` vs gateway centrale),
il modello di autenticazione/consenso dall'assistente AI verso il tenant, il riuso dell'enforcement entitlement/quota
(UC 0027), l'industrializzazione in `new-application` (UC 0046) / `microsaas_app` (UC 0004), e la postura privacy
(nessun dato personale verso l'assistente oltre il necessario).

**Anticipo a livello marketing (deciso dall'utente, change 0047)**: il sito vetrina **anticipa** questo concetto come
**pilastro forte** del messaggio (homepage + "Perché appgrove"), inquadrato come **principio di design dell'ecosistema /
visione**, senza dichiararlo già attivo su ogni app (onestà del claim, requisito anche per la revisione di dominio di
Paddle). **Da riconciliare prima del go-live**: quando la capacità MCP atterra, allineare i contenuti del sito allo stato
reale (da "progettato per l'AI" a "richiamabile dalla tua AI", con prova). **Owner futuro**: decisione/roadmap di
prodotto trasversale (use case dedicati da creare), non un singolo UC esistente.

## Architettura di piattaforma — accoppiamento inter-servizio app↔core (richiesto 2026-06-29) — GRANDE 🏛️

Sollevato durante UC 0027 (change `0023-use-case-0027-…`, enforcement entitlement/quota). L'enforcement fine gira
**dentro l'app** (`fatture`), ma la verità di entitlement (accesso + tetti di quota) vive in `platform.subscription` /
`platform.app_tier.limits`, di proprietà di **core**. Per sbloccare l'enforcement reale **adesso** (local-first, un'app)
abbiamo scelto la via **semplice e sincrona**: core espone `GET /api/platform/v1/me/entitlements` e l'app lo **chiama via
HTTP server-to-server** (propagando il JWT) a ogni gate 402 / risoluzione cap quota.

**Questo è, a regime, un antipattern** (sollevato dall'utente): la chiamata sincrona `app → core` accoppia ogni app al
**ciclo di vita e alla disponibilità** di core (core giù → l'app non sa gatekeepare), crea un **fan-in sincrono** che
peggiora con ogni nuova app (UC 0054, 0046…) e mette core sul **path caldo** di ogni request applicativa.

**Target pulito = event-driven / disaccoppiato.** L'ossatura esiste già (pipeline webhook → SQS → consumer idempotente +
`webhook_event`, UC 0025): core **pubblica i cambi di lifecycle subscription** su uno **stream/bus interno**; ogni app
mantiene una **proiezione locale read-only di entitlement+limits** nel proprio schema `app_<id>` e fa l'enforcement
**leggendo solo dati propri** — niente chiamata sincrona, niente lettura cross-schema. Da valutare: bus (SNS/SQS,
EventBridge, …), formato evento, replay/bootstrap della proiezione, consistenza eventuale vs gate (finestra di staleness
accettabile sull'accesso/quota), e l'industrializzazione della proiezione in `new-application` (UC 0046) / `microsaas_app`
(UC 0004). **Owner futuro:** decisione di piattaforma trasversale (non un singolo UC); rivisitare prima della 2ª/3ª app o
quando il fan-in sincrono diventa un costo reale. Il seam attuale (`QuotaLimitSource` sostituibile + client REST isolato in
`fatture`) è progettato per essere rimpiazzato dalla proiezione locale **senza toccare il codice di dominio dell'app**.

> ✅ **RISOLTO dalla change `0041-use-case-0046-…`** (2026-07-19). Il seam ha retto: il codice di dominio di
> `fatture` non è stato toccato. Scelte effettive, decise col Platform Engineer:
> - **bus**: le code SQS già in casa (`entitlement-<app_id>` + coda degli scarti, generate dal modulo
>   `microsaas_app`), non un bus nuovo;
> - **formato evento**: **sottile** — solo "i diritti del tenant T sono cambiati". Un evento grasso avrebbe
>   costretto core a ri-derivare gli entitlement fuori da una richiesta autenticata, duplicando la logica di
>   `EntitlementReadModel` e aggirando il filtro per tenant: il rischio di due derivazioni divergenti è peggiore
>   del costo di un rinfresco;
> - **bootstrap/replay**: non serve un ripopolamento massivo — la proiezione si popola su richiesta al primo
>   accesso di ogni tenant, tramite la rete di sicurezza;
> - **finestra di incoerenza accettata**: postura **"cache con rete di sicurezza"** (proiezione presente → si usa;
>   da rinfrescare + core giù → si serve il valore vecchio; assente + core giù → si nega). La chiamata sincrona
>   non sparisce: retrocede da percorso caldo a caso limite, qualificata `@SafetyNet`.
>
> Resta un rischio nuovo, coperto da misure e allarmi (`safety_net`, `stale_served`, `denied_unknown`) nel modulo
> `microsaas_app`: decidere su dati vecchi senza accorgersene. Gli allarmi sono la contropartita esplicita della
> scelta di privilegiare la disponibilità sulla freschezza.

## Modello di gestione utenti — tenant-level vs per-app (B2B/B2C) (richiesto 2026-07-01) — GRANDE 🏛️

Sollevato durante UC 0028 (change `0024-use-case-0028-…`, portale self-service). Oggi la schermata **"Membri"**
(UC [0059](usecases/06-frontend/0059-gestione-membri-inviti.md) / [0017](usecases/05-auth/0017-flussi-auth.md)) gestisce gli
utenti a livello di **tenant** (appartenenze in `platform.membership` dopo UC 0116 + inviti tenant-scoped). Questo assume **implicitamente** che il tenant sia
**B2B** — ma non è detto: un'app può essere **B2C** (uso del solo owner, nessun invito) oppure **B2B** (utenti invitati). Il
modello tenant-level impone la semantica B2B a tutti. *(Nota: l'entità `App` ha già `user_model`/`AppUserModel` — segnale che
la distinzione B2C/B2B vive naturalmente a livello **app**, non tenant.)*

**Opzione scartata dall'utente**: gestione utenti **centralizzata di piattaforma** con un'**offerta/costo utenti centralizzato**
indipendente dalle singole app. Vantaggio (non re-invitare gli utenti su ogni app) ma richiederebbe un listino "posti" centrale
slegato dalle app → **non desiderato**.

**Direzione preferita (da approfondire, non ancora decisa)**:
- **Invito utenti per-singola-app B2B**: ogni app B2B definisce i **propri** limiti/pricing dei posti come metrica di quota
  **per-app** (es. app A: tier 20 utenti / €10; app B: stesso prezzo €10 ma 30 utenti). Coerente col modello quota
  `flow`/`stock` già presente (#09 E23) — i "posti" sarebbero una metrica **stock** per-app (già gestibile da
  `TierChangePolicy`/`app_tier.limits`).
- **Funzionalità "invita utenti" cross-app**: al momento dell'invito su un'app B2B, **leggere** gli utenti già presenti su
  **altre app B2B** del tenant per facilitare il ri-invito di utenti esistenti (directory di comodo), **senza** che
  l'entitlement/limite diventi centrale: il gate resta **per-app**.

**Da approfondire**: come distinguere B2C vs B2B (probabilmente `App.user_model`); ripensare la UI "Membri" tenant-level →
**per-app** per le app B2B (assente/ridotta per le B2C); la directory cross-app degli utenti del tenant per l'invito rapido;
l'interazione con identità/appartenenze (UC 0013 + UC 0116) e inviti, e col pricing dei posti (#09, `new-application`/`pricing-change`).
**Owner futuro**: decisione di piattaforma trasversale (non un singolo UC); tocca UC 0059/0017/0013 + catalogo/pricing #09.
**Approfondire in sessione dedicata** (richiesto dall'utente subito dopo UC 0028).

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
- **Tabelle di prova (audit) nel database: dichiararle o no nel manifesto?** (sollevato 2026-08-01, change `0071`/UC 0076).
  Le tre tabelle `platform.gdpr_purge_audit`, `platform.gdpr_restriction_audit` e — da questa change —
  `platform.app_status_audit` conservano l'identificativo opaco dell'**operatore** che ha agito (più, sulle prime due,
  il tenant bersaglio) per 12 mesi. Oggi **nessuna** ha una voce dedicata in `docs/compliance/manifests/platform.yaml`:
  sono considerate coperte dalla voce generica `logs.structured` (audit/sicurezza, legittimo interesse, 12 mesi), che
  però parla di CloudWatch e archivio su oggetti cloud, non di righe di database. Da decidere in una revisione di
  compliance: o si dichiara esplicitamente una voce "prove di audit nel database", o si estende il testo di
  `logs.structured` perché le comprenda in modo inequivocabile. Non urgente (nessun dato di utente finale, nessuna
  nuova finalità né base giuridica) ma va chiuso prima della revisione legale pre-go-live.

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

- ✅ **FATTO (2026-06-16)**: casi d'uso di **autenticazione e registrazione** → [usecases/05-auth](usecases/05-auth/) (UC1–UC10). Resta solo la stesura dei **testi** dei template email EN/IT.
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

## Data breach — runbook, registro, security.txt (#13 J) — ✅ fatto (change 0063, UC 0049)
- ✅ **Runbook IR interno** (`docs/compliance/breach-runbook.md`) + **registro breach interno** (`docs/compliance/breach-register.md`, art. 33.5).
- ✅ **`security.txt`** (già pubblicato UC 0037) + `security@appgrove.app` per responsible disclosure. Resta **azione operativa del founder**: provisioning/monitoraggio della casella `security@` (nessuna infra).
- **Template notifiche** (Garante/interessati/controller) → **bozze** pronte (`.claude/skills/breach-response/reference/template-notifiche.md`); validazione **legale** resta L12 (pre-go-live).
- ✅ Skill **`breach-response`** creata (`.claude/skills/breach-response/`).

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

### Trial una-tantum per tenant×app — GAP verificato (richiesto 2026-07-26)
**Regola attesa (owner):** la prova gratuita di 14 giorni è effettuabile **una sola volta nella storia del
tenant, per ciascuna applicazione**. Esempio: attivo l'app, faccio la prova, al giorno 10 disdico; torno
sulla stessa app dopo 6 mesi → **niente più prova**, posso solo sottoscrivere pagando, e l'**interfaccia
rende evidente** che la prova è già stata usata.

**Stato attuale del codice — NON è così (verificato):**
- La durata è solo un attributo del tier (`app_tier.trial_days`, `pricing/*.yaml: trialDays`); nel modello
  reale la prova la concede **Paddle** sul price, nello stub è hardcoded (+14 giorni in
  `StubScenarioEmitter`). La `subscription` registra solo `trial_end`, **nessuno storico** "prova consumata".
- Alla disdetta la riga diventa `canceled` ma **non** viene messo `deleted_at`; al ritorno il nuovo
  `subscription.created(trialing)` **riusa la stessa riga** (upsert `ON CONFLICT (tenant_id, app_id) WHERE
  deleted_at IS NULL`) con un **nuovo `trial_end`** → **prova riconcessa**. Nessun controllo lo impedisce.
- **Interfaccia**: il badge "14 giorni di prova gratis" (`Billing.tsx`) dipende **solo** da
  `tier.trialDays > 0`, uguale per tutti i tenant → al ritorno **rimostra** la prova.
- **Documenti**: `#09` prevede come anti-abuso solo la **carta upfront** (dec. #27), barriera più debole; la
  non-ri-eleggibilità **non è mai stata decisa**.

**Cosa servirebbe per implementarla:**
1. **Storico "prova consumata" per `(tenant_id, app_id)`**, immune a soft-delete/riuso riga (tabella o
   colonna dedicata scritta quando `trial_end` viene impostato la prima volta; non azzerabile dalla disdetta).
2. **Gate backend**: in `CheckoutResource.start` e/o nel mapping webhook, per un `(tenant, app)` che ha già
   consumato la prova → **azzerare la prova** (trattare come sottoscrizione pagante immediata, `trial_end`
   nullo) anche se il price Paddle prevede il trial.
3. **Interfaccia**: sopprimere il badge/prova e rendere evidente "prova già usata" in base a uno **stato
   per-tenant** (oggi il catalogo `/tiers` è identico per tutti → serve un dato per-tenant, es. campo
   `trialEligible` nell'entitlement/subscription read model).

**Punti aperti (decisione di prodotto, da confermare):** la prova si considera "consumata" appena
**iniziata** (anche se disdetta al giorno 3)? Vale per app con `trialDays: 0` (nessuna prova → nulla da
bloccare)? Interazione con Paddle reale (il trial è impostato sul price lato Paddle: va neutralizzato lato
nostro, non su Paddle). → Da chiudere in un UC dedicato (probabile scorporo/estensione di **UC 0026 ciclo
vita** + **UC 0024 checkout** + il self-service R4).

### Use case (già tracciati sopra)
"Acquisto / checkout", "Gestione abbonamento self-service", "Pausa subscription" (bassissima priorità).

## Backoffice shell (UC 0020) — rinvii cross-area (richiesto 2026-06-26)

Emersi implementando la shell SPA (change `0011-use-case-0020-…`). Gli item **solo-frontend** sono nei "Punti aperti"
di [usecases/06-frontend/0020](usecases/06-frontend/0020-shell-spa-backoffice.md); qui i **trasversali**:

- **Endpoint entitlement nel core (contratto frontend↔core)** — ✅ **fatto** (endpoint e provider reale con UC 0027;
  chiusura dei punti residui con la change `0065`, UC 0077). L'endpoint è `GET /api/platform/v1/me/entitlements` (famiglia
  `/me/*` del tenant corrente, **non** `/entitlements` come ipotizzato qui); lo stub resta ai soli test e sviluppo locale.
  Sul raccordo con l'**endpoint di stato per il polling post-checkout** (#09 C17) la decisione presa è: **due endpoint,
  una sola derivazione** — il polling interroga una singola app ogni 1–2 secondi e vuole una risposta minima, il
  read-model calcola l'elenco completo con tier e limiti; entrambi, con la matrice della console admin, chiedono il
  verdetto di accesso allo stesso punto (`EntitlementAccess`).
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
- ✅ **#4 Enforcement gate disable-app** — **chiuso dalla change `0039-use-case-0014-…`** *(collocazione rivista)*: il blocco NON
  sta all'edge (l'authorizer è **JWT nativo** e non legge il DB — vedi UC 0014) ma **nel servizio**, dove `EntitlementReadModel`
  già scarta le app con `status != active` (UC 0027). Il toggle admin resta la leva. **Rinuncia consapevole**: il traffico di
  un'app disabilitata raggiunge comunque il container; se servirà un kill-switch a monte, le strade sono tracciate in UC 0014
  §Punti aperti ("blocco all'edge").
- **#5 Provider entitlement reale del backoffice (sostituire `StubEntitlementsProvider`)** — può **riusare la derivazione**
  entitlement (tenant×app da `subscription` + `app.status`) introdotta qui nell'admin. Vedi anche il punto UC 0020 sopra. **Owner**: core/billing.
- **#6 `platform-admin` in cloud → UC 0016** (pre-token-gen) **+ UC 0015** (Cognito BFF). Ora: in locale il claim arriva dal provider locale del servizio auth (`admin@appgrove.test`).
- **#7 SSO cross-sottodominio** (cookie su `api.appgrove.app` valido per `app.`+`admin.`) **→ UC 0015**. Ora: `admin.local` è origin separato → login proprio per-host.
- **#8 Hosting prod SPA admin** (CloudFront+S3/OAC+alias Route53) **→ UC 0055** (già pianificato: "2 distribuzioni CloudFront, backoffice+admin") **+ ACM/zona UC 0003**. Ora: solo dev (Caddy `admin.local` + Vite :5174).
- **#9 Pipeline FE pubblica il bundle admin → UC 0005** (CI/CD).
- **#10 Audit persistito delle azioni admin** (disable-app) + archivio 12 mesi **→ UC 0035** (archivio audit) **+ #08**. Ora: solo **logging strutturato** (actor `sub`, `app_id`, esito).
- **#11 KPI ricchi Overview** (MRR/churn) **→ #08/UC 0025**. Ora: KPI base da conteggi nativi.
- **#12 Estrazione base `Table` + stati compositi → UC 0019** (anche in "Punti aperti" di UC 0021/0020).
- **#13 Integrazione SPA admin nel comando canonico `dev`/`dev service` → UC 0009/0046**. Ora: solo `app-start.sh` (:5174) + blocco Caddy `admin.local`.
- **#14 Formalizzazione pattern "endpoint admin non-tenant-scoped" → UC 0013 / doc #02 auth-sicurezza**: come/dove disabilitare in sicurezza il filtro `@TenantId`, test anti-leak sistematici. Ora: query native + gating `platform-admin` (eccezione esplicita all'invariante #2).
- **#15 Console "Diritti GDPR" → UC 0034** (già escluso da UC 0021; nessuna azione, solo confine).
- **#16 Override/toggle entitlement per-tenant** (dettaglio Account, oggi **read-only**) **→ UC 0027** (modello; non più UC 0014,
  che dalla change 0039 non ospita più i gate di business) **+ UC 0013** (schema). Nel modello attuale l'entitlement per-tenant ha **una sola leva** (la subscription): un override per-tenant-app non esiste in schema → decisione di data-model rimandata ai gate.
- **#17 Azione admin sulla subscription** (sospendi/cancella per tenant) **→ UC 0024/0025** (Paddle-coupled).
- **#18 Estrazione runtime auth/sessione condiviso backoffice+admin** — oggi **duplicato** il sottoinsieme minimo in `apps/admin` (config/auth/api). **Owner**: frontend condiviso (rinvio UC 0020) — valutare un pacchetto `@appgrove/app-runtime`.

## Script / tooling DevOps
- **Start/stop servizi test** (scale 0↔1 task Fargate) — ✅ deciso in [07-devops-cicd](07-devops-cicd.md) §28
  (avvio manuale `test-start`; spegnimento via **cron giornaliero `test-stop`**, idempotente, orario UTC fisso).
  Resta l'**implementazione** degli script `infra/scripts/test-start|test-stop` + workflow.
- **Workflow GitHub Actions** (YAML) da implementare: verifica-PR (`plan`+test), deploy-test, release-prod (tag+gate),
  flyway task ECS one-shot, frontend, cron `test-stop`. Specifica → [07-devops-cicd](07-devops-cicd.md).
- **Drift regione: `eu-south-1` nei servizi vs `eu-west-1` deciso** _(rilevato dalla change `0031-use-case-0003-…`)_.
  `appgrove.sqs.region=eu-south-1` nel profilo di default di `services/core` e `services/fatture` (e il commento
  della console "Diritti GDPR" UC 0034 suggerisce `appgrove.aws-console.region=eu-south-1`), mentre #06 6 fissa
  **eu-west-1** per tutta l'infrastruttura. In locale il valore è irrilevante (ElasticMQ/MinIO ignorano la regione),
  ma va **corretto/iniettato per ambiente** quando si configura il deploy cloud (UC 0005): allineare i default (o
  sovrascriverli via config per-env) a `eu-west-1`. Trasversale a più servizi, per questo tracciato qui.

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
- **`campaign-guide`** ✅ **implementata (change `0053-use-case-0050-…`)** — resta aperta solo l'evoluzione futura (assistente Playwright non-headless, tracciata in UC 0050). (richiesto 2026-06-21, #14 J) — **guida passo-passo** per creare campagne ads (Meta/Google)
  **rispettando la postura privacy** (cookieless, **no pixel/CAPI-PII**, no banner, EU-purista): obiettivi ammessi
  (Traffico/Lead Form native), **convenzioni UTM** per attribuzione **Plausible** cookieless, **copy/creatività AI
  on-brand** (tono F1, dec. 35), **checklist di conformità** a ogni step. L'utente non è esperto di advertising.
  **Evoluzione futura**: assistente **Playwright non-headless** che pilota la UI di creazione campagna. Memoria `skills-backlog`.
- ✅ **`breach-response`** (richiesto 2026-06-20, #13 J — **fatto** change 0063, UC 0049) — co-pilota **data breach**:
  guida la valutazione del rischio (albero soglie), decide notifica/non-notifica (Garante art. 33 / interessati art. 34 /
  esenzione cifratura art. 34.3), redige la voce del **registro breach** (art. 33.5) e i **draft notifiche**
  (Garante/interessati/controller B2B) IT/EN. Creata in `.claude/skills/breach-response/`.
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
- ✅ **`new-blog-post`** — **FATTA** dalla change `0085-use-case-0084-…` (2026-08-02): skill in
  `.claude/skills/new-blog-post/` + generatore deterministico `tools/new-blog-post/` (comandi `list`, `check`,
  `scaffold`, `remove`), collaudato nell'area `tooling` di `run-tests.sh`. Il testo che segue è la richiesta
  originale, tenuta per storia. Dettaglio dell'esito nello use case
  [0084](usecases/17-skill-e-tooling-contenuto/0084-skill-new-blog-post.md).
  (richiesto 2026-07-26, durante la change `0061-use-case-0042-…` che costruisce il motore blog) —
  co-pilota per **aggiungere un articolo al blog/risorse** del sito vetrina (UC [0042](usecases/09-marketing-site/0042-blog-risorse.md)),
  gemello di `finalize-landing`/`new-application`: **generatore deterministico** (`tools/new-blog-post`) per la parte
  meccanica (scaffold dei 5 file-lingua `site/src/content/blog/<slug>/{en,it,fr,es,de}.ts` + `index.ts`, entry nel
  registro, aggancio dei riferimenti reciproci **pilastro↔cluster**, aggiornamento della lista cluster del pilastro) +
  **co-pilota** che sceglie il pilastro (o ne apre uno nuovo), decide taglio/tema e genera la **copy question-based
  on-brand nelle 5 lingue** (tono lean, dec. #14 35) con **internal linking** alla landing giusta (via registro
  `LANDINGS`). Chiude con `astro build` + controllo post-build + **`new-change`** (branch + consenso). Il registro dei
  contenuti della change 0061 è disegnato apposta perché lo scaffolding sia "aggiungi una cartella, appendi una riga";
  la validazione (parità 5 lingue, coerenza pilastro↔cluster) fa da rete. Env-agnostica. Memoria `skills-backlog`.

## Recapito delle email transazionali (SES) — tracciato 2026-07-19 (change `0040`, UC 0018)

La change 0040 ha portato le email di autenticazione su SES con firma DKIM e testi EN/IT. Restano **due lavori che il
codice non può fare** e che, se ignorati, si manifestano come "gli utenti non ricevono le email" — un guasto che
blocca registrazioni e reimpostazioni password per **tutti**, senza che nulla vada in errore da nessuna parte.

### 1. Uscita dalla modalità di prova di SES (sandbox) — BLOCCANTE PER IL GO-LIVE ⏳

**Cosa**: un account SES nuovo può spedire **solo a indirizzi verificati a mano**. Ogni altro destinatario riceve un
errore. Serve chiedere ad AWS il passaggio all'accesso in produzione.

**Perché è qui e non nel codice**: è una **richiesta manuale** dal pannello AWS, con motivazione d'uso e descrizione
di come gestiamo i rimbalzi. La risposta arriva **in giorni**, non in minuti, e può essere respinta con richiesta di
chiarimenti.

**Quando**: **in anticipo** rispetto al go-live, non il giorno stesso. È il vincolo con il tempo di attesa più lungo
di tutta la messa in produzione, ed è anche quello che si scopre più facilmente all'ultimo momento.

**Nota**: la richiesta è più solida se presentata *dopo* aver predisposto la gestione dei rimbalzi (punto 2) —
è una delle cose che AWS chiede esplicitamente.

**Proprietario**: fase di messa in cloud. Riferimento: checklist di prima accensione (voce 11, sotto).

### 2. Gestione di rimbalzi e reclami SES — DA FARE PRIMA DI VOLUMI REALI

**Cosa**: oggi non esiste nulla. Nessuna notifica di rimbalzo, nessuna lista di soppressione degli indirizzi che
falliscono, nessun monitoraggio del tasso di rimbalzo e di reclamo.

**Perché conta davvero**: SES **sospende l'account** se il tasso di rimbalzo o di reclamo supera le sue soglie. Non è
un degrado graduale: da un momento all'altro smettono di partire *tutte* le email, comprese verifica indirizzo e
reimpostazione password. Chi si registra in quel momento resta bloccato fuori dal prodotto.

**Cosa serve, in ordine di valore**:
1. destinazione delle notifiche di rimbalzo e reclamo (SES → argomento di notifica → consumo);
2. lista di soppressione: smettere di riscrivere a indirizzi che rimbalzano stabilmente;
3. allarme sul tasso, prima che scatti la soglia di AWS (aggancio naturale con l'osservabilità, UC 0006).

**Quando**: prima di traffico reale. Con volumi da prova il rischio è teorico; al primo elenco di indirizzi non
validi diventa concreto.

**Proprietario**: UC 0018 (residuo) con UC 0006 per la parte di allarmi.

---

## Attivazione ambienti cloud — prima esecuzione live della pipeline + configurazione repo GitHub (tracciato 2026-07-17)

Sollevato dalla change `0036-use-case-0005-…` (pipeline CI/CD, UC 0005). La pipeline è **scritta e validata
staticamente** (actionlint + suite verdi + build Docker locale) ma **mai eseguita dal vivo**: gli ambienti AWS sono
spenti per scelta (attivazione per fasi, #12; cost-min). I workflow sono progettati per essere **inerti** finché la
variabile di repo `AWS_ACCOUNT_ID` non esiste. All'attivazione degli ambienti (dopo `first-run`/bootstrap) vanno fatti,
in ordine:

1. **Variabile di repo GitHub `AWS_ACCOUNT_ID`** (Settings → Secrets and variables → Actions → Variables): accende i
   job cloud (plan in PR, deploy-test, release-prod, env-ops). I ruoli OIDC `appgrove-github-actions-{test,prod}`
   esistono già in `infra/global/oidc.tf`.
2. **Environment GitHub `prod`** con **required reviewer** (gate di approvazione della release, #07 4/21).
3. **Branch protection su `main`**: PR obbligatoria + check required = job di `verify-pr` (almeno: backend o
   backend-security, frontend, oasdiff, compliance; infra-check e plan-test quando toccano infra). Squash-merge come
   convenzione (#07 8).
4. **Secret `INFRACOST_API_KEY`** (account Infracost free): senza, il job Infracost salta con warning. Nota
   residency: l'analisi è su HCL (nessun dato personale); valutare comunque il fornitore (USA) secondo la preferenza UE.
5. **Verifica fatturazione runner `ubuntu-24.04-arm`** su repo privato (build immagini ARM64): se fuori dal free
   tier, valutare build JVM cross-arch via QEMU sul runner x64 (la native resta su runner ARM, on-demand) o repo pubblico.
6. **Prima esecuzione live della pipeline**: PR di prova (plan commentato + suite), merge (deploy test: migrate →
   apply → health), dispatch native, tag `v*` (gate native + piano salvato + approvazione + promozione). Verificare il
   criterio di accettazione di UC 0005: "una PR con violazione cross-tenant non passa".
7. **Cron `env-ops`**: verificare che lo spegnimento notturno (20:00 UTC) agisca sul cluster `appgrove-test`.
8. **Auth cloud (UC 0015, change `0037`)** — al primo giro il bucket artefatti Lambda non esiste ancora: il job
   `build-auth` salta l'upload con warning e l'apply non crea la Lambda; dopo il primo apply **ri-eseguire
   `deploy-test`** (upload → apply → Lambda + route `/api/auth/*` attive). Poi **smoke su Cognito reale in test**
   (UC 0015 §9): signup+verifica (codice dall'email default Cognito, token `base64url(email|codice)`), login,
   refresh con rotazione cookie, logout; verificare attributi cookie e CORS dal dominio `app.test.appgrove.app`.
   La **prima build nativa** del `function.zip` (GraalVM ARM64, profilo Maven `lambda`) è anch'essa validata solo
   dal vivo in CI: mai eseguita localmente.
9. **Pre-Token-Gen + validazione JWT (UC 0016, change `0038`)** — tutto il percorso cloud è mai stato acceso. Alla
   prima accensione di `test`:
   - **Ruolo DB `auth_lambdas`**: l'invocazione `aws_lambda_invocation.auth_lambdas_grants` deve girare **dopo** lo
     schema `platform` (dipende da `module.app_platform`) e **dopo** che Flyway ha applicato le migrazioni (la
     pipeline fa `migrate` prima dell'apply). Verificare che il ruolo esista e abbia i grant attesi (`SELECT` su
     `platform`, `INSERT/UPDATE` su `accounts/users/invitations`) e che il proxy si autentichi col nuovo secret.
   - **Popolare `PLATFORM_ADMIN_SUBS`** (env della Lambda pre-token-gen) col `sub` Cognito reale del platform-admin:
     oggi è **vuota** → nessun claim `platform-admin` finché non la si valorizza. Valutare la tabella dedicata
     `platform.platform_admins` come fonte pulita (punto aperto UC 0016, con UC 0021).
   - **Cold-start Aurora vs limite ~5s di Cognito**: il trigger è sincrono; se Aurora è in pausa (scale-to-0) il
     primo login dopo la pausa può superare i 5s → login fallito al primo tentativo. Provare dal vivo e decidere la
     mitigazione (min capacity Aurora / keep-warm / retry lato SPA).
   - **Smoke Cognito reale**: login → l'access token porta `tenant_id`+`roles`; utente senza membership → negato
     (fail-closed); i servizi accettano solo `token_use=access` col `client_id` del pool.
   - **E23 residua** — la **stretta del security group** del proxy è ✅ **chiusa dalla change `0039-use-case-0014-…`**
     (ingress alle sole SG `auth-lambda` + `pre-token-gen-lambda`). Resta **E23-a**: `iam_auth = REQUIRED` sul proxy,
     **volutamente rimandato a dopo la prima accensione riuscita** (cambia il codice di connessione di entrambe le Lambda
     e non è provabile in locale: mescolarlo alla prima accensione renderebbe la diagnosi molto più difficile).
     Dettaglio in UC 0014 §Punti aperti.

10. **Authorizer all'edge (UC 0014, change `0039`)** — la configurazione è coperta dalle suite Terraform, ma il
    **comportamento** è verificabile solo dal vivo (in locale non c'è API Gateway: il dev stack passa da Caddy).
    Alla prima accensione di `test`, con `API=https://api.test.appgrove.app`:
    - **Senza token → 401** (l'header manca: API GW risponde da sé, non valuta nemmeno l'authorizer):
      `curl -si $API/api/platform/v1/me/entitlements | head -1` → `HTTP/2 401`.
    - **Token malformato/scaduto → 401** (NON 403: è il codice su cui poggia il refresh silenzioso della SPA):
      `curl -si -H 'Authorization: Bearer abc.def.ghi' $API/api/platform/v1/me/entitlements | head -1` → `HTTP/2 401`.
      ⚠️ Se qui esce **403**, l'authorizer non è quello nativo o l'audience/issuer non combaciano: **fermarsi**, perché
      il refresh silenzioso si romperebbe in modo silenzioso e quotidiano.
    - **Token valido → passa** e il 402/403/429 arriva **dal servizio** con corpo strutturato (non `{"message":"Forbidden"}`
      generato dal gateway): login vero dalla SPA e verifica che il banner "abbonamento richiesto" compaia per un tenant
      senza abbonamento (questo è il test che distingue il gate del servizio da un rifiuto del gateway).
    - **Webhook Paddle raggiungibile senza token** (eccezione dichiarativa): un POST con firma non valida deve dare
      **401 dal servizio** (`PaddleSignature`), **non** 401/403 dal gateway — si distinguono dal corpo. Verifica reale:
      un evento dalla sandbox Paddle deve attivare la subscription (è il canarino: se manca l'eccezione, i webhook
      falliscono **in silenzio**, con i retry di Paddle che si esauriscono). Vedi UC 0029 §Punti aperti.
    - **Rotte pubbliche per progetto intatte**: `POST /api/auth/login` e `POST /ingest/errors` devono continuare a
      rispondere senza token.
    - **Health non esposti** (deve restare vero per costruzione): `curl -so /dev/null -w '%{http_code}' $API/q/health/live`
      → `404` dal gateway (nessuna rotta lo intercetta), **non** 200.
    - **Stretta del proxy DB (E23-b)**: dopo l'apply, verificare che le due Lambda auth si colleghino ancora
      (login reale = prova end-to-end). Se il login fallisce con errore di connessione al DB, la causa più probabile è
      il security group del proxy: controllare che le SG `auth-lambda` e `pre-token-gen-lambda` siano quelle allegate
      alle Lambda in esecuzione.

11. **Email di autenticazione via SES (UC 0018, change `0040`)** — template e Lambda sono coperti dai test, ma la
    consegna reale è verificabile solo dal vivo. Alla prima accensione di `test`:
    - **Uscita dalla modalità di prova di SES (sandbox)** — è il vincolo con il tempo di attesa più lungo, e **non è
      risolvibile da codice**: un account SES nuovo può spedire **solo a indirizzi verificati a mano**. L'uscita si
      chiede ad AWS a mano (motivazione d'uso, gestione dei rimbalzi) e la risposta può richiedere **giorni**.
      Va **avviata in anticipo**, non il giorno del go-live: finché non è concessa, registrazione e reset password
      funzionano solo verso indirizzi verificati, e **ogni altro destinatario riceve un errore**.
    - **Verifica del dominio + firma DKIM**: dopo l'apply, controllare che l'identità `appgrove.app` risulti
      *verified* e che le tre chiavi di firma siano attive su Route53. Senza firma, la posta finisce nello spam.
    - **Primo invio reale nelle due lingue**: registrazione con lingua italiana e con lingua inglese → verificare che
      arrivino i testi giusti, dal mittente `noreply@appgrove.app`, e che il collegamento nel messaggio apra davvero
      la pagina di verifica (formato del token `base64url(email|codice)`).
    - **Email di invito dalla rete privata**: è l'unica che parte dal nostro backend e non da Cognito. Se fallisce con
      un errore di rete o un timeout, la causa più probabile è il punto di accesso a SES (`email-smtp` / API SESv2)
      o il security group associato. Sintomo tipico: `POST /api/auth/invitations/send` va in errore 5xx dopo alcuni
      secondi di attesa.
    - **Rimbalzi e reclami**: SES chiude l'account se il tasso di rimbalzo resta alto. Non c'è oggi alcuna gestione
      delle notifiche di rimbalzo (punto aperto tracciato in UC 0018).

Finché tutto ciò non avviene, UC 0005 è "implementato a codice" ma la sua Definition of Done operativa si chiude solo
con la prima run live. Owner: fase di **messa in cloud** (righe 29–37 dell'ordine in `docs/usecases/_INDEX.md`).

## Difetto — l'esportazione dei dati dell'account non è collaudabile in locale: `500` per una coda che non esiste (trovato 2026-08-21)

**Il fatto.** `POST /api/platform/v1/gdpr/exports` con `{"kind":"account"}` come `owner@acme.test` risponde **`500`**.
Nel registro del servizio `core`:
`software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException: The specified queue does not exist`,
riportata da `UncaughtExceptionMapper` — cioè eccezione **non gestita**.

**Causa.** L'esportazione dell'account interpella **tutte** le applicazioni a cui l'account ha diritto. Il seme dà ad
Acme abbonamenti a `teams`, `legacy` e `notes`: applicazioni **del catalogo di prova**, che non hanno un servizio
reale nel monorepo e quindi non hanno la loro coda locale. In ElasticMQ esistono solo `gdpr-export-crm`,
`gdpr-export-fatture`, `gdpr-export-platform` (più le rispettive code di scarto) — le sole app con un servizio vero.
In cloud il problema non si porrebbe: ogni app nasce dal modulo Terraform con la propria coda. È quindi un difetto
**dell'ambiente locale**, non del codice dell'esportazione.

**Perché conta.** Rende non verificabile a mano un diritto GDPR (art. 20) proprio nell'ambiente in cui si collauda:
è l'ultima riga del §9 della guida della change `0091`, e nessuno l'aveva mai eseguita. Inoltre il modo in cui
fallisce è sbagliato a prescindere dalla causa: un `500` con eccezione non gestita, invece di saltare le
applicazioni prive di coda o di rispondere con un errore comprensibile.

**Due rimedi, indipendenti.**
1. *Ambiente*: far creare allo stack locale una coda per **ogni** applicazione del catalogo, non solo per quelle con
   un servizio; oppure non dare al seme abbonamenti ad applicazioni senza servizio (ma servono ad altri collaudi,
   quindi la prima via è preferibile).
2. *Robustezza*: l'esportazione deve **tollerare** un'applicazione che non risponde — saltarla registrando il fatto,
   o completare in stato parziale dichiarato. Un diritto dell'interessato che si blocca perché una coda non esiste è
   fragile anche in cloud, dove una coda può esistere ed essere irraggiungibile.

**Verificato per altra via, nel frattempo**: l'atteso della guida è corretto. `PlatformDataContract.java:112` include
l'entità `app_access` nell'esportazione con `app` (slug), `role` e `identity_id`, esattamente come promesso.
Owner: **#08 (compliance/privacy)** con #11 (sviluppo locale) per la parte ambientale.

## Difetto — due corse di `run-tests.sh` insieme producono rossi falsi (trovato e CORRETTO 2026-08-21)

**Il fatto.** Durante il collaudo manuale del lotto 0088–0092, sviluppatore e agente hanno lanciato
`./run-tests.sh` **contemporaneamente** senza saperlo. Esito: `backend` e `platform` rossi su un albero sano,
con `J-QUOTA` fallito e «3 did not run». Nessuno dei due rossi aveva a che vedere col codice.

**Perché.** Due corse sulla stessa macchina condividono tutto ciò che conta: gli stessi `target/` Maven, la
stessa porta di prova Quarkus (8081), le stesse porte 20080-20082 dello stack di piattaforma e — soprattutto —
**lo stesso Postgres locale**. Non è una corsa più lenta: sono rossi sparsi e irriproducibili, in aree che non
hanno nulla di rotto. È il modo più efficace di far perdere fiducia nel comando unico, e costa ore di indagine
su difetti che non esistono.

**Corretto.** `run-tests.sh` prende ora un **lucchetto** all'avvio (`.run-tests.lock`, creato con `mkdir` che è
atomico su POSIX) e lo rilascia con `trap` anche su interruzione. Una seconda corsa si ferma con un messaggio
che spiega il perché invece di produrre un rosso falso; un lucchetto rimasto orfano di una corsa interrotta male
viene riconosciuto (il pid non è più attivo) e ripreso da sé, così non serve intervenire a mano. Provato in
entrambi i casi. Il lucchetto è in `.gitignore`.

**Resta da valutare** (non fatto): la stessa protezione servirebbe a `./app-start.sh`, e più in generale sarebbe
utile che le due cose si conoscessero — lanciare i test con lo stack acceso è l'altro difetto di questa famiglia
(voce qui sotto). Owner: #07 (DevOps/CI).

## Difetto — `./run-tests.sh` è ROSSO se lo stack locale è acceso (porta 8081), con diagnosi fuorviante (trovato 2026-08-21)

**Il fatto, verificato durante il collaudo manuale del lotto 0088–0092.** Con lo stack locale avviato
(`./app-start.sh`), `./run-tests.sh tooling` fallisce e stampa:

```
✗ la suite dell'app generata è ROSSA: i modelli-sorgente sono invecchiati.
✗ Correggi tools/new-application/templates/ — NON rattoppare l'output generato.
```

La diagnosi è **sbagliata**. La causa vera, in fondo a migliaia di righe di registro Maven, è
`io.quarkus.runtime.QuarkusBindException: Port(s) already bound: 8081: Address already in use`.

**La catena.** Nessun servizio dichiara `quarkus.http.test-port`, quindi vale il **predefinito di Quarkus:
8081**. E `services/fatture` — l'app #1, che è anche la sorgente della parità di scaffolding — gira in locale
proprio su `quarkus.http.port=8081`. Quindi ogni `@QuarkusTest` del monorepo trova la porta occupata quando lo
stack è su. Verificato non solo sull'area `tooling`: anche `app.appgrove.core.MeMembershipsApiTest` (area
**backend**) muore con lo stesso errore. Sono le due aree più grandi di `run-tests.sh`.

**Perché è un difetto e non un dettaglio d'uso.** Lo stack acceso è la condizione **normale** di chi collauda a
mano: le `how-to-test.md` cominciano tutte con `./app-start.sh`. Chi lancia i test in quel momento — cioè chi
fa esattamente quello che il progetto gli chiede di fare — riceve un rosso che accusa i modelli di scaffolding
di essere invecchiati. Il tempo si spende a cercare un problema che non esiste. E il caso opposto è peggio: un
rosso vero, quel giorno, verrebbe scambiato per «ah, è la porta».

**Rimedi possibili, dal più economico.**
1. **Dichiarare la porta di prova effimera**: `quarkus.http.test-port=0` (o `%test.quarkus.http.port=0`) nei
   `application.properties` dei servizi **e nel modello** `tools/new-application/templates/`, così ogni app
   nuova nasce immune. Quarkus assegna una porta libera e inietta l'indirizzo giusto nei test: nessuna
   collisione possibile, con nessuno stack e nessun altro processo. È la correzione alla radice.
2. **Guardia in `run-tests.sh`**: se una porta di servizio è occupata, fermarsi *prima* di lanciare Maven con un
   messaggio chiaro («lo stack locale è acceso: `./app-stop.sh`, oppure lancia i test in un'altra copia»).
   Utile comunque, perché copre anche i casi non-Quarkus.
3. **Correggere la diagnosi** di `tools/new-application/generate-smoke.sh`: distinguere «suite rossa» da «suite
   non partita». Attribuire a un invecchiamento dei modelli un fallimento di avvio è la parte peggiore del
   difetto, perché manda nella direzione sbagliata con autorevolezza.

Le tre sono complementari; la prima è quella che chiude il problema.

**Nota su come è passato inosservato.** I subagenti del lotto `go-fast` hanno visto la suite completa verde:
girano con lo stack spento. Il difetto si manifesta solo nella condizione del collaudo manuale — che nessun
collaudo automatico riproduce.

## Processo — nessuna change aggiorna lo stato di implementazione in `EPICS-WAVE-2.md` (sollevato 2026-08-21)

**Il fatto.** Dopo il lotto `go-fast` 0088–0092, le cinque storie implementate e mergiate (UC 0116, 0117, 0118,
0098, 0099) erano ancora marcate ⬜ «da implementare» nell'ordine di esecuzione dell'onda 2. Le ha marcate ✅
lo sviluppatore, accorgendosene a mano.

**La causa, verificata.** Il Definition of Done di `new-change` impone di sincronizzare
[docs/usecases/_INDEX.md](usecases/_INDEX.md), ma **non nomina** `EPICS-WAVE-2.md` — e le storie evolutive non
stanno in `_INDEX.md`, stanno solo lì. La skill `go-fast` nomina `EPICS-WAVE-2.md` una volta sola, e in
**lettura**: per ricavare l'ordine topologico e per filtrare le storie già fatte. Nessuno dei due lo scrive.
Effetto collaterale sgradevole: `go-fast` usa quella colonna per **saltare le storie già implementate**, quindi
un registro non aggiornato porterebbe un lotto futuro a reimplementare lavoro già in `main`.

**Vie possibili.** Aggiungere `EPICS-WAVE-2.md` al Definition of Done di `new-change` accanto a `_INDEX.md`
(minimo indispensabile), oppure farlo aggiornare da `go-fast` alla fine di ogni storia mergiata — che è il
punto in cui l'informazione è certa. Un controllo meccanico è possibile e a buon mercato: una storia con una
cartella `changes/*-use-case-YYYY-*` mergiata in `main` e ancora marcata ⬜ è un'incoerenza rilevabile.

**Chi lo possiede.** Le skill `new-change` (Definition of Done) e `go-fast` (chiusura di storia). Da decidere
insieme al punto sulle guide di collaudo qui sotto: sono lo stesso problema visto da due lati — **artefatti di
documentazione che nessun presidio sorveglia**.

## Processo — le guide di collaudo manuale (`how-to-test.md`) invecchiano in silenzio (sollevato 2026-08-21)

**Il fatto.** Durante il collaudo manuale del lotto `go-fast` 0088–0092 (epica 22), lo sviluppatore ha eseguito
la guida della change **0088** su un `main` che conteneva già le change 0089–0092, e ha inciampato **tre volte
in punti scaduti** — una per sessione di collaudo, ogni volta con un'indagine per capire se il difetto fosse
nel codice o nella guida. Estensione finale su quella sola guida: **sei punti riallineati a mano** (0.3, 1.2,
1.3, 1.6, 2.1, 2.4) più due voci della checklist finale, in tre categorie:

1. *ripieghi dichiarati e poi sciolti dalle storie successive* — «entra senza selettore, il selettore arriva
   con UC 0117» (2.4, parte 5): la guida conteneva perfino una guardia di perimetro («se lo vedi adesso,
   qualcuno ha anticipato lavoro») che dopo il lotto **scatta a vuoto**, accusando di un difetto il lavoro
   corretto della change successiva;
2. *comportamenti rimossi da una storia successiva* — il cambio ruolo da «Members», rimosso dalla 0091 (1.2,
   1.3, 1.6);
3. *misure sbagliate indipendenti dal lotto* — `platform.users` data per «vuota» quando la migrazione copia e
   non svuota, sbagliata due volte nella stessa guida (0.3 e parte 5), più il nome della tabella di storico
   Flyway senza schema, sbagliato in **cinque** guide diverse (0083, 0084, 0088, 0089, 0091).

4. *nomi tecnici al posto delle etichette visibili* — «scegli bersaglio **user**» dove a schermo si legge il
   menu **«Bersaglio»** con l'opzione **«Utente»**, in un riquadro in fondo alla pagina che la guida non
   indicava (3.2); e più in generale i nomi delle schermate scritti in inglese (*Members*, *My data*) mentre
   chi collauda ha l'interfaccia in italiano (**Membri**, **I miei dati**). Chi legge non trova l'elemento e
   sospetta un difetto del prodotto.

5. *comandi non incollabili, e quattro convenzioni diverse in quattro guide* — le guide del lotto usavano
   `psql()` come funzione di shell da definire prima (0089), un `alias pg` (0091), un indirizzo di
   connessione `postgres://…` (0090) e `PGPASSWORD=… psql -h localhost` (0092). Chi collauda deve risalire in
   testa al documento, copiare la definizione e poi sostituire il comando: scomodo al punto che lo
   sviluppatore ha chiesto di rifarli tutti. Peggio: **due delle quattro forme presuppongono `psql` installato
   sulla macchina**, che non è un requisito dello stack locale, e una porta perfino una password diversa dalle
   altre. Normalizzate tutte su `docker compose -f dev/docker-compose.yml exec -T postgres psql -U appgrove -d
   appgrove …` (29 comandi riscritti nelle quattro guide) e verificate eseguendole.

6. *comandi fragili che presuppongono un seme intatto, e prerequisito non dichiarato* — il punto 1.3 della
   guida 0089 assegna `active_membership_id` con una sottoquery su `platform.membership` **senza filtrare
   `deleted_at is null`**. Chi arriva dal collaudo della guida precedente ha righe cancellate logicamente (la
   0088 chiede proprio di rimuovere una persona da un account) e riceve `ERROR: more than one row returned by
   a subquery used as an expression` — un errore che sembra un difetto del prodotto e non lo è. Peggio: la
   guida 0089 dice di azzerare la banca dati **in fondo**, nella sezione «Ripulire», ma non dice di
   **partire** da una pulita; e diverse sue attese («Acme Owner ha una sola appartenenza») valgono solo sul
   seme intatto. Corretto irrobustendo le tre sottoquery, dichiarando il prerequisito in testa e aggiungendo
   un punto 0.3 che fotografa lo stato di partenza. Vale in generale: **una guida che si esegue in sequenza
   dopo un'altra eredita lo stato che l'altra ha lasciato**, e il lotto `go-fast` le produce proprio in
   sequenza.

Le categorie 3, 4, 5 e 6 dicono una cosa in più: **non è solo un problema di invecchiamento**. Nessuno *esegue* i
comandi delle guide e nessuno *ripercorre* i passi con l'interfaccia davanti, quindi un comando che non gira o
un'etichetta che non esiste sopravvive indefinitamente. Un rimedio che si limitasse a datare le guide (via 3)
non toccherebbe queste tre categorie: valgono già al primo giorno. La quinta suggerisce anche il presidio più
economico dell'insieme — **una forma canonica dichiarata** per i comandi verso la banca dati locale, che la
skill `new-change` impone alla guida e che un controllo meccanico può far rispettare, perché è una stringa
fissa e non un giudizio.

**Perché è un difetto di processo e non un difetto di quella change.** La 0088 era corretta il giorno in cui è
stata scritta, e i **test automatici hanno fatto il loro lavoro**: il percorso `J-MEMBERS` copriva il cambio
ruolo, si è rotto quando la 0091 l'ha rimosso, e la 0091 ha dovuto sostituire il segmento con l'asserzione
opposta (il comando non c'è più) motivandola nel codice. Ciò che **nessun presidio sorveglia** è la prosa delle
guide delle change **già chiuse**: sono file di archivio, non codice eseguibile, quindi quando una change
successiva cambia la realtà nulla diventa rosso. In un lotto `go-fast` di più change consecutive sullo stesso
sottosistema — che è il caso d'uso per cui `go-fast` esiste — il fenomeno è quasi garantito.

**Il rischio concreto.** Lo sviluppatore che collauda a mano perde fiducia nell'automazione proprio quando
questa ha funzionato: una guida scaduta si legge come «i test non ci sono». È accaduto, e ha richiesto
un'indagine per dimostrare il contrario.

**Vie possibili, da valutare (nessuna decisa).**
1. *Guida di lotto consolidata*: `go-fast` scrive, alla fine del lotto, un'unica `how-to-test.md` complessiva
   che rispecchia lo stato finale di `main`, e marca le guide delle singole change come «fotografia storica».
   Costa poco e coglie il caso più frequente.
2. *Passata di riallineamento a fine lotto*: l'ultima change del lotto rilegge le guide delle precedenti e
   annota i punti superati. Più fedele, più costoso, e resta prosa non sorvegliata.
3. *Marcatura esplicita*: ogni `how-to-test.md` nasce con in testa il commit su cui è stata scritta, così chi
   la esegue sa che sta leggendo una fotografia e non un documento vivo. Non evita lo scostamento, lo rende
   leggibile — è il rimedio minimo, e va fatto in ogni caso.

**Chi lo possiede.** La skill `go-fast` (contratto di fine lotto) e la skill `new-change` (intestazione della
guida). Da decidere insieme alla prossima revisione delle due skill.

## Tooling — unificare in `services/commons` i due renderer Java dei template email (sollevato 2026-07-25)

Sollevato dalla change `0052-use-case-0039-…` (newsletter). L'email di conferma della newsletter parte dal servizio
`core` e riusa la sorgente unica `shared/email-templates`, ma il **renderer** Java che risolve stringhe + impaginazione
è oggi **duplicato**: `EmailTemplates` in `services/auth` (email di autenticazione, UC 0018) e `NewsletterEmailRenderer`
in `services/core` (gemello compatto). I *testi* restano single-source (bene), solo la logica di resa è in due copie.
Da estrarre in `services/commons` un unico renderer (con set di lingue parametrizzabile) usato da entrambi i servizi.
Non fatto nella change 0052 per non rifattorizzare `services/auth` fuori dallo scope di UC 0039. Effort: piccolo/medio.

> **✅ CHIUSO (change `0079`, UC 0085).** Il renderer unico vive in `services/commons`
> (`app.appgrove.commons.email.EmailTemplateRenderer`) con il set di lingue come parametro
> (`EmailLocales`); `services/auth` e `services/core` lo usano attraverso adattatori sottili che
> dichiarano solo le proprie lingue. Parità verificata carattere per carattere su 37 casi resi prima
> e dopo la rifattorizzazione. **Resta fuori**, per impossibilità tecnica: il Custom Message Lambda in
> Python rende la stessa cartella con gli stessi due passaggi ma in un altro linguaggio — se la logica
> di resa fra Java e Python divergesse, il tema appartiene a UC 0018.

## Canale di notifica dal server al browser (sollevato 2026-07-30)

Sollevato dalla change `0065-use-case-0077-…` (provider entitlement reale). Il menu del backoffice si aggiorna quando gli
entitlement cambiano **per azione dell'utente** (acquisto, cambio piano, disdetta, ripresa: invalidazione esplicita) e
quando l'utente **torna sulla scheda** del browser o la rete si riconnette. Resta scoperta una finestra: un cambiamento
avvenuto **altrove** mentre la scheda è aperta — un'app disabilitata dalla piattaforma (UC 0076), un abbonamento scaduto a
fine periodo, un pagamento fallito — non raggiunge la scheda finché l'utente non vi ritorna.

Chiuderla in tempo reale richiede una **capacità di piattaforma nuova**: un canale persistente dal server al browser
(connessione a lunga vita o flusso di eventi), con quel che comporta — attraversamento del bilanciatore, autenticazione del
canale, riconnessione, costo per connessione aperta. Non è un dettaglio degli entitlement: servirebbe anche alle notifiche
in-app e agli aggiornamenti del supporto, e va quindi deciso come capacità, non come rattoppo.

**Attenzione a non confonderlo con quello che c'è già**: il core pubblica l'invalidazione degli entitlement su un bus
interno (`EntitlementInvalidationPublisher`, una coda per app) diretto ai **servizi delle app**, non al browser. Nessun
pezzo oggi porta quell'evento fino al cliente.

**Quanto è grave nel frattempo**: non è un buco di sicurezza — il servizio nega comunque (catena di enforcement UC 0027) e
la guardia di rotta è solo uno strato di comodità. L'effetto è una voce di menu che porta a un errore invece che a un'app.
**Owner**: da decidere fra #03 (frontend) e #08 (observability/piattaforma); diventerà uno use case quando la capacità sarà
richiesta da più consumatori. Effort: medio/grande.

## Instabilità osservate nell'esecuzione COMPLETA di `run-tests.sh` (change `0070`, 2026-08-01)

Due aree sono risultate rosse **solo** nella corsa completa `./run-tests.sh` e verdi rieseguite da sole subito dopo
(`./run-tests.sh frontend smoke`), su un albero che non tocca né il frontend né i servizi. Vanno indagate: una suite
che dipende dall'ordine delle aree erode la fiducia nel comando unico.

- **frontend** — `src/modules/crm/crm.test.tsx` e `src/modules/fatture/fatture.test.tsx` falliscono con «Unable to find
  an element with the text: Mario Rossi» preceduti da `[MSW] Error: intercepted a request without a matching request
  handler: GET /api/platform/v1/me/legal/status`. Sospetto: il gate legale (UC 0056) emette una richiesta non prevista
  dai gestori di quei test di modulo e, sotto carico, la sua risposta mancata ritarda/annulla il rendering. Rimedio
  candidato: aggiungere il gestore `me/legal/status` al set condiviso dei test di modulo. Owner: #03 (frontend).
- **smoke** — `build artefatti fallita` con
  `NoSuchFileException: services/core/target/classes/META-INF/openapi/openapi.yaml` durante `quarkus:build`. Sospetto:
  interferenza con l'area `backend` eseguita prima (lo schema OpenAPI è rigenerato in `src/main/resources` dai test e
  copiato in `target/classes`). Rimedio candidato: build dello smoke su target pulito o esclusione del file dalla
  copia. Owner: #07 (DevOps/CI) con #04.

**Il fenomeno si è ripresentato il 2026-08-21**, su aree diverse: lo sviluppatore ha visto **`infra` e `platform`**
rosse nella corsa completa, e rieseguite da sole sono risultate **entrambe verdi** — `infra` con tutti e sei i
passi effettivamente eseguiti (formato, validazione di quattro radici, `terraform test` su quattro moduli, checkov,
actionlint), non con un salto silenzioso. Di `platform` è stata trovata e corretta una causa reale e indipendente
dall'ordine (l'instabilità di `J-INVITE-EXISTING`, voce qui sotto): plausibile che nella corsa completa la macchina
sia più carica e la corsa sul gate legale si perda più spesso, quindi il difetto si manifestasse là e non nella
corsa isolata. Per `infra` **non è stata trovata alcuna causa**: da sola è verde in modo autentico, e senza
l'output del fallimento non è diagnosticabile.

Il punto di questa voce si rafforza: **non basta sapere che una suite è rossa, serve sapere in che corsa**. Sarebbe
utile che `run-tests.sh` conservasse l'output di ogni area in un file per corsa, così un rosso non riproducibile
resti esaminabile invece di scorrere via dal terminale. Owner: #07 (DevOps/CI).

## Il gate legale è fail-open: il passo condiviso dei percorsi di piattaforma perde la corsa (change `0089`, 2026-08-21)

`acceptLegalGateIfPresent` ([tools/platform-e2e/helpers/browser.ts](../tools/platform-e2e/helpers/browser.ts))
attende «gate **oppure** shell navigabile» e attraversa il gate solo se in quell'istante è visibile. Ma il gate
è **fail-open mentre il suo stato carica** (`LegalGate` rende i figli finché `useLegalStatus` non risponde):
per un istante la shell è navigabile e **solo dopo** il gate la sostituisce. Chi passa da quel passo può quindi
ritrovarsi il gate aperto sotto il naso subito dopo che il passo è tornato — e la successiva asserzione sulla
shell fallisce, oppure il percorso continua a cliccare su una pagina bloccata.

Osservato nella change `0089` in **due** punti: dopo l'accesso e dopo il cambio di account (quel percorso entra
in due account, quindi incontra il gate due volte ed è quello che paga la corsa più spesso). Il percorso
`J-ACCOUNT-SWITCH` si è reso robusto **da sé** — attende la risposta che *decide* il gate
(`/me/legal/status`) e poi insiste sull'esito — ma il passo condiviso è rimasto come era: sistemarlo lì
toglierebbe la stessa instabilità a tutti i tredici percorsi, e va fatto con attenzione perché non tutti gli
accessi arrivano alla shell nello stesso modo (il secondo fattore ne è un esempio). Owner: **#10 (testing)** con
UC 0091/0092, proprietari dei passi condivisi della suite di piattaforma.

**Aggiornamento (change `0090`, UC 0118)**: la soluzione robusta è stata **estratta** in
`helpers/browser.ts` come `expectInsideAccount` / `loginIntoAccount` / `switchAccountTo`, perché serviva a un
secondo percorso (`J-INVITE-EXISTING`) e duplicarla era il momento sbagliato per farlo. `browserLogin` — il
passo usato dagli altri percorsi — **non è stato toccato**: resta il lavoro di questa voce, ed è ora più
semplice, perché la versione corretta esiste già accanto ad esso.

**Aggiornamento (2026-08-21, correzione diretta su `main`)**: l'estrazione non era bastata, e il perché è
istruttivo. `J-INVITE-EXISTING` chiamava `expectInsideAccount` ma **non** l'attesa sulla decisione del gate,
perché il suo cambio di account non passa dal selettore — l'accettazione dell'invito lo cambia da sé — e quindi
non passava da `switchAccountTo`, che l'attesa se la fa in casa. Risultato: il percorso era **instabile**
(`1 flaky` nella corsa di piattaforma, fallito al primo tentativo e passato al secondo). La schermata salvata da
Playwright mostra il gate legale al posto della shell: prova diretta della corsa descritta sopra. Corretto
esportando l'attesa come `waitForLegalDecision(page)` e usandola nel percorso; nella stessa occasione l'ordine
delle due asserzioni è stato invertito, perché `toHaveCount(0)` su «Members» è vero **anche su una barra laterale
ancora vuota** e passava per il motivo sbagliato, coprendo proprio il difetto in esame.

Lezione per il lavoro che resta su `browserLogin`: **estrarre l'helper non basta se il chiamante può raggiungere
lo stesso stato per un'altra strada**. Qui la strada era «cambio di account senza selettore», che nessuno aveva
in mente quando l'helper è stato scritto.

## Indice di esecuzione degli use case: nessuna riga per le storie evolutive (change `0073`, 2026-08-01)

`docs/usecases/_INDEX.md` contiene la tabella di esecuzione dei soli **60 use case base** (0001–0060). Le **37 storie
evolutive** (0061–0097, epiche 12–21) vivono solo nel catalogo per area `docs/usecases/README.md`, che però riporta lo
stato del **drill-down** (la spec è scritta?), non lo stato di **implementazione** (il codice è in `main`?).

Conseguenze già osservate:

- la skill `new-change` non riesce a marcare 🟡/✅ una storia evolutiva: il suo `sed` cerca una riga `[NNNN](` che non
  esiste, quindi non fa nulla e nessuno se ne accorge (è successo alle change `0069`–`0072` per gli UC 0090–0092, e a
  questa change per lo UC 0093);
- il controllo del registro di copertura (UC 0093) ha dovuto usare l'esistenza di una cartella
  `changes/*-use-case-NNNN-*` come prova che una storia è stata implementata, invece dello stato dell'indice, che
  sarebbe la sorgente naturale.

Da decidere in una change dedicata all'indice: se estendere `_INDEX.md` alle storie evolutive (ri-eseguendo
l'ordinamento topologico, oggi calcolato sui soli 60 base) oppure aggiungere una colonna di stato di implementazione al
catalogo per area. Chi lo farà, aggiorni anche la guardia dell'esenzione `non-implementato` in `tools/e2e-coverage`.
Owner: Skills & Tooling (UC 0044/0045), in coordinamento con UC 0094.

## Lettura del listino come entità: un ciclo di fatturazione fuori enum spegne la pagina (change `0076`, 2026-08-01)

**Che cosa.** `AppPrice.billingCycle` è mappata su un enum di due valori (`monthly`, `annual`). Chi carica i
price come entità JPA — oggi `EntitlementReadModel.freeTier`, che chiede a `AppPriceRepository.listByTier` se
una fascia ha prezzi — fallisce con un errore non gestito (HTTP 500) se **una sola riga** del database porta un
valore diverso: un dato storico, una riga scritta da un'integrazione, o semplicemente una fixture. Non è
teorico: è successo davvero nella change `0076`, dove la lettura del catalogo esplodeva a causa di una riga di
prova con `billing_cycle = 'year'`.

**Che cosa è già stato fatto.** Il read-model del catalogo (UC 0095) legge prezzi e fasce gratuite in SQL
nativo, trattando il ciclo come **etichetta** e non come valore su cui decidere: una vetrina deve degradare,
non spegnersi. Ha anche un test di regressione (`CatalogApiTest.unCicloDiFatturazioneFuoriCatalogoNonSpegneLaVetrina`).

Anche `EntitlementReadModel.freeTier` è stato corretto nella stessa change, e non per prudenza ma per
necessità: il test di regressione appena aggiunto ha reso quella fragilità **attiva**, facendo fallire
`AccountDeletionApiTest`. Lì il ciclo non serve affatto (interessa solo *se* la fascia ha prezzi), quindi la
lettura è diventata un conteggio (`AppPriceRepository.existsForTier`) che non converte nulla. Restano a
caricare i price come entità i soli tre punti in cui il ciclo **è** l'informazione cercata (avvio del
checkout, elenco delle fasce, cambio fascia): lì il valore fuori catalogo è un dato sbagliato, non un caso
da tollerare.

**Che cosa resta.** Decidere se il valore fuori catalogo vada **rifiutato in scrittura** — un vincolo di
dominio sulla colonna `platform.app_price.billing_cycle` — invece che soltanto tollerato in lettura. Oggi
nulla impedisce a un'integrazione di scriverne uno. Chi lo possiede: **UC 0022** (pricing-as-code) insieme a
UC 0027.

## Esportazione dei dati: gli abbonamenti restano fuori, le transazioni ora ci sono (change `0077`, 2026-08-01)

**Contesto.** La change `0077` (UC 0096) ha introdotto lo storico pagamenti come tabella tenant-scoped e
l'ha inserita sia nella **cancellazione fisica** dei dati del conto sia nell'**esportazione** dei dati
dell'interessato (`PlatformDataContract`): uno storico di pagamenti è ciò che una persona si aspetta di
ritrovare quando chiede «che cosa avete su di me».

**Il punto aperto.** La tabella `platform.subscription` — che pure è tenant-scoped e racconta quali app
il conto ha attivato, con quali piani e per quanto tempo — **non compare nell'esportazione** e non è mai
stata inserita. Viene cancellata alla purga, ma non restituita all'interessato. Ora che accanto ad essa
c'è una tabella esportata, l'asimmetria è visibile e va sciolta in un verso o nell'altro:

- **o si esporta anche l'abbonamento** (posizione prudente: è un dato riferito al conto, come le
  transazioni, e completa la storia commerciale che l'esportazione racconta);
- **o si scrive perché non va esportato** (posizione difendibile solo se si sostiene che sia un dato
  contrattuale e non personale — argomento più debole ora che i pagamenti sono dentro).

**Chi possiede il tema**: UC 0033 (self-service GDPR) insieme a UC 0032 (esportazione), col conforto
della revisione legale (`docs/_REVISIONE-LEGALE.md`). Non è stato deciso nella change 0077 perché
allargare il contratto di esportazione a un'entità che quella change non introduce sarebbe stato uscire
dal suo scope scritto.

## Superfici pubbliche senza autenticazione: chi dice qual è il conto (change `0086`, 2026-08-04)

**Contesto.** Il drill-down del catalogo applicazioni (change `0086`) ha fatto scrivere epiche e storie a
diciassette applicazioni proposte. Tre di esse hanno una **pagina rivolta a un destinatario che non è un
utente della piattaforma**: chi riceve un preventivo e lo accetta (app 06), chi prenota un appuntamento
dalla pagina pubblica di un salone o di uno studio (app 07), chi consulta il proprio abbonamento dal
portale dell'abbonato (app 19). Su quelle pagine **non esiste un token verificato**, e l'invariante numero
uno della costituzione dice che l'identificativo del conto si ricava **solo** da lì.

**Il punto aperto.** Le tre applicazioni hanno proposto, indipendentemente, due meccanismi distinti — e la
distinzione è la parte interessante, perché non è la stessa situazione:

- **gettone di capacità**, quando il destinatario è **noto prima**: firmato dal server, valido per un solo
  scopo, a scadenza, revocabile, conservato come impronta, mai convertito in una sessione. È il caso
  dell'accettazione di un preventivo, del portale dell'abbonato, della singola prenotazione confermata;
- **identificativo pubblico di sede**, quando il visitatore è **ignoto e la pagina va pubblicata**: non è un
  segreto e non è una credenziale, il server lo risolve da sé nel conto, e concede soltanto ciò che è già
  pubblico più il diritto di *proporre* qualcosa. Le difese non sono la segretezza ma la verifica del
  contatto, i limiti di frequenza, le risposte indistinguibili e il divieto di enumerazione.

Nessuno dei due è stato approvato: sono proposte di agenti che scrivevano documenti. Servono una decisione
di piattaforma e **un'unica implementazione condivisa** — se ogni applicazione se lo costruisce da sé, la
prima che sbaglia apre un varco sull'isolamento fra conti, e nessun collaudo di piattaforma se ne accorge.

**Chi possiede il tema**: gli use case di autenticazione (area `05-auth`, in particolare UC 0017 sui flussi)
insieme all'architettura applicativa (`docs/01-architettura.md`, invariante 1). Va chiuso **prima** che
venga scaffoldata la prima applicazione con una superficie pubblica.

## Autenticazione di una macchina: il conto quando il chiamante non è una persona (change `0086`, 2026-08-04)

**Contesto.** L'app 31 del catalogo (registro e governo delle azioni degli agenti automatici) riceve eventi
da **sorgenti che non sono persone**: un agente, un servizio, un processo del cliente. La piattaforma oggi
ricava il conto dal claim di un token emesso per una persona; una sorgente non ne ha uno e non può averne.

**Il punto aperto.** La proposta scritta nella storia è una **chiave di sorgente** verificata lato server,
da cui il conto si deriva — mai dal corpo della richiesta, per non rompere l'invariante. Ma la proposta
eccede la singola applicazione: tocca l'emissione, la rotazione e la revoca delle credenziali, e il modello
di identità della piattaforma. Non è stata decisa.

Il tema è più generale del registro delle azioni: qualunque applicazione che riceva dati da un sistema del
cliente — sincronizzazione, importazioni automatiche, ricezione di eventi — incontra lo stesso muro.

**Chi possiede il tema**: gli use case di autenticazione (area `05-auth`), con il bordo su UC 0014 e UC 0016.

## «Prospetto sì, classifica no»: una regola emersa dal basso (change `0086`, 2026-08-04)

**Contesto.** Tre applicazioni del catalogo, scritte da agenti diversi che non si vedevano fra loro, sono
arrivate alla stessa conclusione su un punto che nessuno aveva posto come regola: le note spese (app 08), i
progetti e le ore (app 13) e il salone con le provvigioni (app 21) trattano tutte **dati riferiti a singoli
lavoratori** e tutte e tre hanno deciso di **non produrre aggregati per persona a fini di valutazione**.

La distinzione che hanno tracciato è netta e regge: il dato per persona esiste dove serve a un **fatto
dovuto** — si paga una provvigione a qualcuno, si rimborsa una spesa a qualcuno, quindi il prospetto
individuale è legittimo e necessario — ma **nessuna vista affianca più prospetti**, e chiedere di
raggruppare gli indicatori per operatore restituisce un rifiuto motivato. Nell'app 21 è diventato codice:
`operatore` come chiave di raggruppamento risponde `400`, con prova negativa su rotte ed esportazioni e un
passo dedicato nel percorso end-to-end. La stessa regola è stata estesa alla console di amministrazione, che
aggrega per conto e mai per operatore — perché una sorveglianza non diventa lecita solo perché a guardarla è
chi amministra la piattaforma.

**Il punto aperto.** Oggi è una convenzione ripetuta tre volte, non una regola di piattaforma: la quarta
applicazione che tratterà dati di lavoratori potrà ignorarla senza che nulla diventi rosso. Va deciso se
promuoverla a **invariante** — con un collaudo condiviso che la faccia valere — o lasciarla come buona
pratica documentata. La promozione ha un costo: va definita con precisione, perché «aggregato per persona»
non è un concetto che un collaudo riconosce da solo.

**Chi possiede il tema**: compliance e privacy (`docs/13-compliance-privacy.md`) insieme all'architettura
applicativa; il collaudo eventuale appartiene all'area `20-test-e2e-piattaforma`.

## La suite frontend è rossa fuori dal locale inglese (rilevato nella change `0086`, 2026-08-04)

**Contesto.** Eseguendo `./run-tests.sh` a chiusura della change `0086` — che è **documentale** e non tocca
una riga di frontend — l'area `frontend` è risultata rossa: due prove di
`frontend/apps/admin/src/pages/reconciliation.test.tsx` (riconciliazione netto/revenue, UC 0071). Verificato
su un albero di lavoro separato che **le stesse due prove falliscono identicamente su `main`**: il rosso è
preesistente, introdotto dalla change `0083`, e non ha nulla a che vedere con la `0086`.

**La causa.** `Reconciliation.tsx:24` formatta gli importi con
`value.toLocaleString(undefined, { style: 'currency', … })`. Il primo argomento `undefined` significa
«usa il locale della macchina». Le prove si aspettano il formato inglese (`€1,000.00`), che è quello che
esce dove il locale è inglese — presumibilmente l'integrazione continua. Su una macchina italiana lo stesso
codice produce `1000,00 €` e le prove falliscono.

**Perché conta più di due prove rosse.** Due difetti distinti, e il secondo è il peggiore:

1. **di prodotto** — l'importo è formattato secondo il locale del sistema operativo di chi guarda, non
   secondo la lingua scelta nell'applicazione. In un prodotto che si impegna a cinque lingue è una
   discrepanza, per quanto attenuata dal fatto che la console di amministrazione ha un solo utilizzatore;
2. **di fiducia nel cancello** — chiunque sviluppi su una macchina non inglese vede
   `./run-tests.sh` rosso per una ragione che non riguarda il proprio lavoro. Un cancello che mente
   sistematicamente smette di essere letto, ed è così che passano i rossi veri. È anche il motivo per cui
   questa voce non è un dettaglio estetico: oggi `run-tests.sh`, che la costituzione dichiara **sorgente di
   verità unica**, non lo è su metà delle macchine possibili.

**Che cosa va deciso.** Se la formattazione debba prendere la lingua dell'applicazione (probabile: coerente
con l'impegno alle cinque lingue), e in ogni caso **le prove non devono dipendere dal locale della macchina**
— o fissano il locale esplicitamente, o verificano il valore invece della sua resa.

Non è stato corretto dentro la change `0086`, che è documentale: toccare codice di produzione di un altro
use case per far tornare verde un cancello sarebbe esattamente il genere di sconfinamento che le regole del
repository vietano. Il rosso è **dimostrato preesistente** e segnalato, non ereditato in silenzio.

**Chi possiede il tema**: UC 0071 (riconciliazione) per il difetto di prodotto, UC 0060 (localizzazione a
cinque lingue) per la regola generale sulla formattazione, e la strategia di collaudo
(`docs/10-testing.md`) per la dipendenza delle prove dal locale.

## Appartenenza multipla: temi lasciati fuori dall'epica 22

L'epica 22 rende una persona capace di appartenere a **più account**
([UC 0116](usecases/22-refactor-membership-model/story/0116-identita-e-appartenenze.md)). Due temi che ne
discendono restano fuori, per natura e non per dimenticanza:

- **Piano commerciale per chi lavora per più clienti** (liberi professionisti, studi, consulenti). La
  tecnica lo rende possibile; il prodotto non lo prevede. È una decisione commerciale — quanto costa un
  posto a chi è già pagato da un altro account? Nessuna, per ora: **ogni account paga le persone che usano
  le sue applicazioni**, che è la regola più semplice e anche la più difendibile.
- **Unione di due identità** create per errore con indirizzi diversi dalla stessa persona. Lavoro
  sgradevole (dati da riattribuire, tracce di controllo da non falsificare) e richiesta di assistenza
  prevedibile. Da affrontare quando si presenta, con una decisione consapevole su cosa si può riattribuire
  e cosa no.
- **Rimozione fisica di `platform.users`** (tracciato dalla change `0088`, UC 0116). Il travaso verso
  `platform.identity` + `platform.membership` lascia la vecchia tabella in piedi come **rete di ritorno**:
  nessun codice la legge e nessuno la scrive, ma se il travaso avesse un difetto è l'unica via per
  tornare indietro. Va rimossa con una migrazione dedicata **dopo** che l'epica 22 è in esercizio da un
  po' — non prima, e non nella stessa change che l'ha resa fredda. Chi la rimuove tolga anche il permesso
  di scrittura residuo e il commento della tabella.

## Collegamenti rotti nei documenti — nessun presidio automatico

Un controllo manuale eseguito durante la change `0087` ha trovato **27 collegamenti interni rotti** nei
documenti di `docs/` (fuori dall'area 22, che è stata verificata e ne ha zero): riferimenti a percorsi
precedenti la suddivisione degli use case in sottocartelle (`usecases/01-auth-registrazione.md`), rimandi a
`_kit/` da applicazioni archiviate in `_catalogo/_escluse/`, e alcuni segnaposto nei modelli (che non sono
difetti). Nessuno di questi rende falso un documento vivo, ma nessuno diventa rosso: la classe di difetti
cresce in silenzio ogni volta che un file si sposta.

**Cosa servirebbe**: un controllo dei collegamenti interni nell'area `tooling` di `run-tests.sh`, con una
lista di eccezioni per i modelli. Piccolo, e chiude una classe intera. Non fatto in `0087` per non
sconfinare: la change è documentale e limitata all'area 22.

## Manifesto dei dati ed esportazione dell'account: lo stesso elenco per due domande diverse (change `0090`, 2026-08-21)

Il collaudo di contratto `PlatformGdprContractTest.exportCoversEveryManifestEntity` pretende che **ogni voce
del manifesto** compaia nell'esportazione dei dati dell'account. È un presidio prezioso — impedisce che un
dato dichiarato resti fuori dall'esportazione — ma tiene insieme due domande che non sono la stessa:

- il **manifesto** è il registro dei trattamenti: deve elencare tutto ciò che si tratta, altrimenti nessuno
  lo riesamina più;
- l'**esportazione** è ciò che un titolare (l'account) può legittimamente vedere: e non tutto ciò che si
  tratta *per* un account è informazione *dell'*account.

Il caso che l'ha fatto emergere: `invitations.identity_id` (UC 0118) dice che l'indirizzo invitato ha già
un'identità appgrove — informazione che UC 0118 §5 vieta di far arrivare a chi invita. Dichiararla nel
manifesto la trascinava nell'esportazione, cioè la rivelava per un'altra via dopo averla tenuta fuori dalla
risposta dell'invito. Risolto **restringendo** l'esportazione (solo inviti accettati, dove quella persona è
già un membro noto), come si era fatto in UC 0117 per `identity.active_membership_id`: due precedenti sono un
motivo per dare al problema un nome.

Cosa manca: un modo **dichiarativo** di dire «questa voce è nel registro, e nell'esportazione è ristretta
così», invece di una restrizione scritta a mano nella query e spiegata in un commento. Senza di esso, la
terza volta qualcuno la dimenticherà. Owner: **#13 (compliance/privacy)** con UC 0030/0032.
