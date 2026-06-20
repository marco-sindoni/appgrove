# Observability — Decisioni

**Stato**: 🟢 deciso
**Ultimo aggiornamento**: 2026-06-20

## Scope
Come capire cosa fa il sistema in esecuzione — **log, metriche, tracce, dashboard, allarmi, error tracking,
health/uptime** — in modo cost-min, future-proof (porta aperta a OTel+Grafana) e coerente con la multi-tenancy
(invariante #4: ogni segnale porta `tenant_id`/`app_id`/`user_id`). Copre backend (Quarkus/Fargate), Lambda
(auth/authorizer/pre-token-gen), edge (API GW/CloudFront) e frontend (SPA). Non copre i test (→ [10-testing](10-testing.md))
né la compliance/audit in senso legale (→ #13, vedi [_BACKLOG](_BACKLOG.md)).

## Principio guida: strumentazione neutra, backend AWS-native ora
**Strumentazione** (come l'app emette i segnali) ≠ **Backend** (dove si immagazzinano/visualizzano). Strumentiamo
**OTel-native / vendor-neutral** fin da ora (così migrare backend è config, non re-instrumentazione) ma usiamo backend
**AWS-native** (CloudWatch/X-Ray) per il cost-min. Migrazione a OTel+Grafana = swap di backend (evoluzioni E10/E11/E12/E14
in [_EVOLUZIONI-DEVOPS](_EVOLUZIONI-DEVOPS.md)).

## Decisioni prese

### A. Logging
1. **Log JSON strutturato** (`quarkus-logging-json`) → **CloudWatch Logs** (unica destinazione; niente ELK/Datadog/Grafana
   esterni = cost-min). Query via **CloudWatch Logs Insights**.
2. **MDC popolato dal JWT verificato** all'inizio della richiesta: `tenant_id`, `app_id`, `user_id`, `correlation_id`/
   `request_id` agganciati **automaticamente** a ogni log → invariante #4 garantita dal framework, non dal singolo dev.
3. **Nomi campo secondo le semantic conventions OTel** (incl. `trace_id`/`span_id`, `service.name`): rende i log
   pronti per Loki/Grafana senza toccare il codice (future-proof).
4. **Correlation id** generato all'**edge** (API GW) e **propagato** a valle (authorizer → servizio → SQS).
5. **No PII in chiaro** nei log: identificativi **opachi** (`user_id` = sub/UUID, mai email/nome). Masking dove serve.
   Linea guida da imporre subito (ripulire dopo è costoso; lega a #13).
6. **Livelli**: `INFO` in test/prod, `DEBUG` attivabile via config/SSM senza rebuild; `local` può stare a `DEBUG`.

### B. Metriche
7. **Metriche tecniche/infrastrutturali**: usare quelle **native AWS** (ECS, API GW, Lambda, Aurora, CloudFront) →
   gratis, nessuna instrumentazione.
8. **Metriche di business**: **Micrometer** (astrazione neutra, future-proof) pubblicate via **EMF (Embedded Metric
   Format)** — metriche scritte **dentro i log JSON**, estratte da CloudWatch → **niente chiamate `PutMetricData`**,
   cost-min, e dimensioni incluse senza esplosione di costi.
9. **Cardinalità**: dimensioni solo a **bassa cardinalità** (`app_id`, `env`, `endpoint`, `status`, `service`).
   `tenant_id`/`user_id` **NON** come dimensioni di metrica (vivono nei log) → vedi regola dei due piani (J).
10. Partire con **poche metriche mirate** (signup, login/2FA, errori per app, latenza operazioni chiave), estendere al bisogno.

### C. Tracing distribuito
11. **Strumentazione OTel attiva ora** (`quarkus-opentelemetry`) + **propagazione W3C trace context** (`traceparent`)
    attraverso API GW → authorizer → servizio → SQS; `trace_id`/`span_id` nei log (A). **MA export tracce SPENTO,
    nessun collector** (c1) → **costo $0**, nessuna infra. Per debug si usano log + `correlation_id`.
12. **Accensione tracce = evoluzione E10**: deploy collector **ADOT → X-Ray** (c2), oppure **OTel Collector → Grafana
    Cloud/Tempo**, abilitando l'exporter **via config (nessuna modifica al codice)**.

### D. Dashboard
13. **CloudWatch Dashboards as-code in Terraform** (creati/distrutti con l'ambiente; il modulo `microsaas_app` genera
    un widget/sezione per servizio). Set minimo: **overview per-env** + **per-servizio** (generato) + **auth/sicurezza**.
14. Dimensioni a bassa cardinalità (B/9); restare **entro i 3 dashboard gratuiti** dove possibile (poi ~$3/mese cad.).
    Drill-down per-tenant = query Logs Insights (J), non widget. Migrazione a Grafana = E11.

### E. Alerting
15. **CloudWatch Alarms + SNS as-code in Terraform**; recapito iniziale via **email**, due topic **`critical`/`warning`**,
    predisposti per **Slack/Telegram** futuri. Il modulo `microsaas_app` genera gli allarmi base per servizio.
16. **Set minimo anti-rumore** (con durata, non spike): 5xx API/servizi, latenza p95, errori/throttle Lambda,
    Aurora (connessioni/ACU/storage), health fallito ripetuto (G), picco anomalo login falliti/lockout.
17. **AWS Budgets $100/mese** con soglie **75% / 90% / 100%** (speso reale) **+ alert su forecast >100%** (early warning).
    Tetto a $100 (= max atteso) per non generare allarmi fisiologici; abbassare a $80 è una modifica banale a regime.
18. Allarmi **pieni in prod, minimi/silenziati in test**: lo **scale-to-0 / spegnimento notturno NON deve generare
    falsi allarmi**.

### F. Error tracking (backend)
19. **CloudWatch-native** (f1): eccezioni come **ERROR JSON** con stack trace + contesto MDC; **metric filter** conta gli
    ERROR → metrica → allarme (E); **triage via Logs Insights** (raggruppa per eccezione/endpoint, filtra per tenant,
    correla via `correlation_id`). Costo $0, nessun dato esce da AWS (privacy/#13).
20. **Strumento dedicato** (Sentry/GlitchTip/Grafana) = **evoluzione E12**, se la triage "a query" diventa scomoda.

### G. Health & uptime
21. **Health interni (ECS)** via `quarkus-smallrye-health`: **liveness** (`/q/health/live`) **senza DB** (un blip DB non
    deve killare i container); **readiness** (`/q/health/ready`) col DB ma **tarata sul cold start** Aurora scale-to-0
    (~10-15s). Endpoint **esclusi dall'authorizer** e non loggati rumorosamente. ECS riavvia i task malati; allarme su
    fallimenti ripetuti (E).
22. **Uptime esterno = canary AWS-native cross-region**: **EventBridge schedulato → Lambda di ping** sull'health endpoint
    pubblico leggero → metrica → **SNS**, **interamente in eu-central-1 (Francoforte)**, separato da eu-west-1 (così
    sopravvive a un outage regionale di eu-west-1 e può avvisare). Costo **~$0**, full-IaC. **Solo prod, dal go-live**;
    **niente in test** (romperebbe lo scale-to-0). Monitor esterno non-AWS (UptimeRobot/Synthetics) = evoluzione **E13**.

### H. Observability frontend
23. SPA statiche = niente log server-side → **cattura errori JS minimale, AWS-native** (h1): handler `window.onerror`/
    `onunhandledrejection` → `POST` a ingest **API GW → Lambda → CloudWatch Logs**, con contesto `app_id`/route/**build
    SHA**/`user_id`/`tenant_id`. **Solo errori** (base giuridica "legittimo interesse", **niente tracking comportamentale**
    → footprint consenso leggero). Costo ~$0, full-IaC; incluso nello scaffold di `new-application`.
24. **Source map** caricate dalla CI come **artifact privato** (non su CloudFront) → de-minificazione offline.
25. **RUM completo** (CloudWatch RUM/Sentry/Grafana Faro) = **evoluzione E14**, **consent-gated** (#13).

### I. Retention & costi
26. **Retention esplicita su ogni log group** (mai "never expire"): **test 7 giorni**, **prod log applicativi 30 giorni**
    (poi scadono). Impostata in Terraform (il modulo la applica ai log group del servizio).
27. **Contenere il volume alla fonte** (leva principale): `INFO` in prod, niente log rumorosi (health esclusi, no log per
    richiesta di routine ridondante con API GW).
28. **Archivio audit/sicurezza implementato da subito** (attivo in prod): **subscription filter** sul log group
    audit/sicurezza → **Kinesis Firehose → S3 → lifecycle Glacier → scadenza** a fine retention. Retention **12 mesi**
    (decisa internamente, #13 E — copertura forense vs detection incidenti ~200gg). Volumi bassi → pochi centesimi/mese.
    Eventuali aspetti legali → registro [_REVISIONE-LEGALE.md](_REVISIONE-LEGALE.md).
29. **Solo audit/sicurezza** va archiviato a lungo (login/logout, falliti, cambio password/2FA, azioni admin, modifiche
    ruoli/entitlement, export/cancellazione GDPR). I **log operativi NON** si archiviano a lungo: **minimizzazione GDPR**
    + costo. Metriche basse per cardinalità; tracce $0 perché spente (C).

### J. Dimensione multi-tenant — "regola dei due piani"
30. **Alta cardinalità → nei LOG/TRACCE**: `tenant_id`, `user_id`, `correlation_id`, `request_id` come campi MDC,
    filtrabili in Logs Insights / attributi di span. Analisi per-tenant = **a query** (costo zero di cardinalità).
31. **Bassa cardinalità → DIMENSIONI di metrica**: `app_id`, `env`, `endpoint`, `status`, `service`. Dashboard/allarmi
    **per-app/globali**; **per-tenant solo come eccezione mirata**, non la regola.
32. Drill-down per-tenant = **Logs Insights pre-salvate** linkate dai dashboard (non widget per tenant). La regola è
    **automatica** grazie all'MDC dal JWT (A) → invariante #4 = comportamento del framework, ereditato da `microsaas_app`
    e dallo scaffold `new-application`.
33. **Attention point futuro**: log/metriche/allarmi **"VIP" per-tenant** (SLA per cliente) → evoluzione **E15**
    (gestire la cardinalità con whitelist di tenant, non tutti).

## Questioni aperte
_Nessuna — #08 chiuso._ Dipendenze verso #13 (retention legale audit, consenso per RUM, privacy policy vs log tecnici).

## Alternative valutate / scartate
- **Stack observability esterno** (ELK/Datadog/Grafana self-hosted) ora — scartato (costo/gestione); Grafana = evoluzione.
- **`PutMetricData`** per le metriche custom — scartato a favore di **EMF** (cost-min).
- **`tenant_id` come dimensione di metrica** — scartato (esplosione cardinalità/costi) → vive nei log.
- **X-Ray acceso subito** (c2) — rimandato (collector always-on vs cost-min/fasi) → strumentazione pronta, export spento.
- **Sentry/SaaS error tracking da subito** — rimandato (E12); CloudWatch-native ora.
- **UptimeRobot/SaaS uptime** — rimandato (E13); canary AWS-native cross-region ora (più IaC-coerente).
- **RUM completo da subito** — rimandato (E14, consent-gated).
- **Archiviare tutti i log a lungo** — scartato (minimizzazione GDPR + costo): solo audit/sicurezza.

## Impatti su altre aree
- [04-services-backend](04-services-backend.md), [06-infra-iac](06-infra-iac.md), [07-devops-cicd](07-devops-cicd.md),
  [03-frontend](03-frontend.md), [02-auth-sicurezza](02-auth-sicurezza.md), [05-persistenza-dati](05-persistenza-dati.md),
  [_COSTI-AWS.md](_COSTI-AWS.md), [_EVOLUZIONI-DEVOPS.md](_EVOLUZIONI-DEVOPS.md), [_BACKLOG.md](_BACKLOG.md) (#13)
