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

## Dettaglio funzionalità / use case — richiesto 2026-06-14
Le decisioni di [03-frontend](03-frontend.md) (e affini) fissano stack/architettura/UX a grandi linee. Resta da
**progettare in dettaglio tutti gli use case** delle varie funzionalità (backoffice, moduli app, console admin):
flussi, schermate per stato, edge case, validazioni, permessi per ruolo. Da affrontare in sessioni dedicate per area/app.

- ✅ **FATTO (2026-06-16)**: casi d'uso di **autenticazione e registrazione** → [usecases/01-auth-registrazione](usecases/01-auth-registrazione.md) (UC1–UC10). Resta solo la stesura dei **testi** dei template email EN/IT.
- **DA PROGETTARE — use case "GDPR / diritti dell'interessato & gestione account"** (richiesto 2026-06-20, da #13 D):
  i diritti self-service decisi in #13 D diventano casi d'uso in `docs/usecases/` (flussi/schermate/stati/edge): "I miei
  dati"+download (accesso/portabilità), rettifica profilo, **elimina account** (totale) vs **recedi da app** (per-app,
  esporta→conferma→cancella), **unsubscribe** newsletter, opposizione a legittimo interesse, **tooling admin B2B**
  (export/cancellazione dati dei propri utenti = assistenza al titolare), canale `privacy@` + workflow tracciato (SLA 1
  mese) + verifica identità. **Consenso (da #13 F)**: subscribe box newsletter (vetrina) + checkbox non pre-spuntata al
  signup + **centro preferenze consensi** in impostazioni account (hub unico, revoca facile) + disclosure cookie tecnici.
  Da fare insieme al dettaglio UX backoffice/account e al contratto per-app export/erasure.

## Sito vetrina (marketing) — richiesto 2026-06-20
Nuovo artefatto, distinto dalle 2 SPA (#03 backoffice+admin). **Statico**, **multilingua EN/IT/FR/ES/DE**, **contenuti
come file `.md`** (fonte unica: gli stessi md servono sito + rendering in-app delle policy). Ospita **Privacy Policy, ToS,
cookie disclosure** (5 lingue, IT facente fede), **subscribe newsletter** (#13 F), **Cloudflare Web Analytics cookieless**
(#13 B/F). Build statica (SSG/Vite) su S3+CloudFront. Versioning policy: **versione + `effective_date`** nel front-matter,
git-backed; **check CI** presenza di tutte le 5 lingue per componente. Da definire in area frontend/#03 (estensione) o doc dedicato.

## Script / tooling DevOps
- **Start/stop servizi test** (scale 0↔1 task Fargate) — ✅ deciso in [07-devops-cicd](07-devops-cicd.md) §28
  (avvio manuale `test-start`; spegnimento via **cron giornaliero `test-stop`**, idempotente, orario UTC fisso).
  Resta l'**implementazione** degli script `infra/scripts/test-start|test-stop` + workflow.
- **Workflow GitHub Actions** (YAML) da implementare: verifica-PR (`plan`+test), deploy-test, release-prod (tag+gate),
  flyway task ECS one-shot, frontend, cron `test-stop`. Specifica → [07-devops-cicd](07-devops-cicd.md).

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
- **RoPA versionato** `docs/compliance/ropa.md` (#13 C): sezione piattaforma + sezione per-app assemblata dai manifesti.
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
