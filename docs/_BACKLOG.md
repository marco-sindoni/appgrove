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

## Dettaglio funzionalità / use case — richiesto 2026-06-14
Le decisioni di [03-frontend](03-frontend.md) (e affini) fissano stack/architettura/UX a grandi linee. Resta da
**progettare in dettaglio tutti gli use case** delle varie funzionalità (backoffice, moduli app, console admin):
flussi, schermate per stato, edge case, validazioni, permessi per ruolo. Da affrontare in sessioni dedicate per area/app.

- ✅ **FATTO (2026-06-16)**: casi d'uso di **autenticazione e registrazione** → [usecases/01-auth-registrazione](usecases/01-auth-registrazione.md) (UC1–UC10). Resta solo la stesura dei **testi** dei template email EN/IT.

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
