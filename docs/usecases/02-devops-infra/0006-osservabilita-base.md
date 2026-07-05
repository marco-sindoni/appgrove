# UC 0006 — Observability baseline (log JSON+correlation, Micrometer/EMF, dashboard/alarm/SNS/Budgets, retention/archivio)

**Area**: 02-devops-infra · **Fase**: 1 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0003](0003-fondamenta-terraform.md) (foundation)
**Fonte decisioni**: #08 (observability), #06 (risorse), #13 (retention/no-PII/audit)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [08-observability](../../08-observability.md), [06-infra-iac](../../06-infra-iac.md), [13-compliance-privacy](../../13-compliance-privacy.md)

## 1. Obiettivo / Scope
Stabilire la **baseline di observability** cost-min e future-proof (strumentazione OTel-native, backend AWS-native), coerente
con l'invariante #4 (ogni segnale porta `tenant_id`/`app_id`/`user_id`).
**Incluso**: **log JSON** strutturato → CloudWatch (MDC dal JWT, semantic conventions OTel, **no-PII**); **correlation id**
generato all'edge e propagato; metriche tecniche native + business via **Micrometer/EMF**; **tracing OTel strumentato ma
export spento**; **dashboard/alarm/SNS** as-code; **AWS Budgets $100** con soglie; **retention** per log group + **archivio
audit/sicurezza** (Firehose→S3→Glacier, 12 mesi); **health/uptime**.
**Escluso**: l'accensione tracce (E10), Grafana (E11), RUM (E14), il canary prod cross-region (UC [0007](0007-osservabilita-irrobustimento.md)).

## 2. Attori & ruoli
- **Sistema** (servizi/Lambda/edge): emettono i segnali.
- **Platform Engineer**: riceve allarmi (email, topic `critical`/`warning`), consulta dashboard/Logs Insights.
- **Modulo `microsaas_app`** (UC 0004): genera widget/allarmi/log group per ogni servizio.

## 3. Precondizioni
- Foundation attiva (UC 0003); modulo `microsaas_app` (UC 0004) per generare le risorse per-servizio.

## 4. Flusso principale
1. **Logging**: `quarkus-logging-json` → CloudWatch; **MDC popolato dal JWT** all'inizio richiesta (`tenant_id`/`app_id`/`user_id`/
   `correlation_id`) → invariante #4 automatica; **no-PII** (identificativi opachi) (#08 1/2/3/5). **Livelli**: INFO in test/prod, DEBUG attivabile via **SSM Parameter Store senza rebuild** (config a runtime), DEBUG di default in locale (#08 dec.6).
2. **Correlation id** all'**edge** (API GW) → propagato authorizer→servizio→SQS (#08 4).
3. **Metriche**: tecniche native AWS; business via **Micrometer → EMF** (dentro i log JSON, niente PutMetricData); dimensioni
   **bassa cardinalità** (`app_id`/`env`/`endpoint`/`status`/`service`) — `tenant_id`/`user_id` **solo nei log** (regola dei due piani, #08 J).
4. **Tracing**: OTel strumentato + W3C trace context; **export SPENTO** (costo $0), debug via log+correlation (#08 11).
5. **Dashboard/alarm/SNS** as-code Terraform (overview per-env + per-servizio + auth/sicurezza); **set minimo anti-rumore**;
   **Budgets $100** (75/90/100% + forecast) (#08 13/16/17). Allarmi **pieni in prod, silenziati in test** (#08 18).
6. **Retention**: ogni log group ha retention esplicita (test 7gg / prod app 30gg); **archivio audit/sicurezza** via
   subscription filter → Firehose → S3 → Glacier → scadenza **12 mesi** (#08 26/28).
7. **Health**: liveness senza DB, readiness col DB tarata sul cold-start Aurora; endpoint esclusi dall'authorizer (#08 21).

## 5. Flussi alternativi / edge / errori
- **Scale-to-0/spegnimento notturno**: non deve generare falsi allarmi (allarmi test minimi/silenziati, #08 18).
- **Error tracking**: eccezioni come ERROR JSON + metric filter → metrica → allarme; triage via Logs Insights (#08 19).
- **Frontend errori**: `window.onerror`/`onunhandledrejection` → ingest API GW→Lambda→CloudWatch, **solo errori** (legittimo interesse, no tracking comportamentale); contesto `app_id`/route/**build SHA**/`user_id`/`tenant_id`; le **source map** restano **artifact privato CI** (de-minificazione offline, #08 24, generato in UC 0005). L'handler è **incluso nello scaffold `new-application`** (UC 0046) (#08 23).
- **Drill-down per-tenant**: Logs Insights pre-salvate linkate dai dashboard, **non** widget per-tenant (#08 32).

## 6. Risorse & runbook
**Risorse Terraform**: log group (+retention), CloudWatch Dashboards, Alarms + SNS (`critical`/`warning`, email; predisposti
Slack/Telegram), AWS Budgets, subscription filter + Firehose + S3/Glacier per l'archivio audit. Il modulo `microsaas_app`
genera la sezione per-servizio. **Runbook**: allarme → Logs Insights (filtro `correlation_id`/`tenant_id`) → triage. **Costi**:
metriche EMF (no PutMetricData), tracce spente, ≤3 dashboard gratuiti dove possibile.

## 7. Dati toccati
Log/metriche/tracce: **no PII in chiaro** (identificativi opachi, #08 5). L'**archivio audit/sicurezza** (login/logout/falliti,
cambio password/2FA, azioni admin, modifiche ruoli/entitlement, export/cancellazione GDPR) è cifrato (S3 SSE) e conservato 12
mesi (#08 29, #13 E). I log operativi **non** si archiviano a lungo (minimizzazione GDPR). Manifest: i segnali rispettano #13 E.

## 8. Permessi & gate
- **Invariante #4**: garantita dal framework (MDC dal JWT), ereditata da `microsaas_app` e dallo scaffold `new-application` (#08 32).
- Accesso a Swagger/health gated (authorizer ruolo platform-admin, #04 9) — non loggati rumorosamente.

## 9. Requisiti di test
- Verifica che ogni log porti `tenant_id`/`app_id`/`user_id`/`correlation_id` (test sul wiring MDC).
- Nessun PII nei log (controllo). Allarmi as-code creati/distrutti con l'ambiente; budget configurato. Health endpoint esclusi dall'authorizer.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #08 1–33 (baseline, incl. dec.6 livelli/SSM, dec.23 frontend error ingest, dec.24 source map CI), #06 §20bis (encryption), #13 E (retention/audit).
- **DoD**:
  1. Log JSON con MDC dal JWT (no-PII) → CloudWatch; correlation id propagato.
  2. Metriche business via EMF a bassa cardinalità; tracce strumentate ma spente.
  3. Dashboard/alarm/SNS/Budgets as-code; retention + archivio audit 12 mesi.
  4. Allarmi pieni in prod, silenziati in test; health liveness/readiness corretti.

## Punti aperti / decisioni differite

_Tracciato dalla change `0033-use-case-0004-…` (regola CLAUDE.md "Tracciamento delle decisioni differite")._

- **Widget e allarmi base per-servizio sul log group del modulo `microsaas_app`.** La change 0033
  (UC 0004) crea per ogni servizio il **log group** cifrato con retention esplicita (test 7 gg, prod
  30 gg, #08 26) e le **DLQ** delle code SQS per-app, ma **non** i widget di dashboard né gli allarmi
  (error rate, DLQ non vuota, task che non parte). Restano a questo UC, che possiede dashboard/alarm
  as-code (#08): quando si implementa, agganciare le risorse già esposte dal modulo (nome log group,
  ARN delle code/DLQ negli output). Differito perché gli allarmi appartengono al disegno complessivo
  dell'osservabilità, non al mattone infra per-servizio.
