# Change 0035: Osservabilità di base (UC 0006)

**Branch**: `change/0035-use-case-0006-osservabilita-base`
**Aree**: infra · services/commons · services/core · services/fatture · frontend
**Data**: 2026-07-05
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/02-devops-infra/0006-osservabilita-base.md](../../docs/usecases/02-devops-infra/0006-osservabilita-base.md)
**Tocca dati personali?**: Sì — i log strutturati portano identificativi opachi (`tenant_id`, `user_id`) e l'archivio
audit/sicurezza conserva 12 mesi di eventi riferibili a persone (azioni admin, export/cancellazione GDPR). Si applica il
gate privacy/RoPA di step-03 (UC 0031): scanner + eventuale aggiornamento manifesti/RoPA + classificazione MAJOR/MINOR.

**Esito del gate (step-03)**: 10 segnali dello scanner = sole dipendenze **in-process** (Micrometer, OpenTelemetry API,
estensioni Quarkus per log JSON/health/metriche): nessun dato lascia AWS, **nessun nuovo sub-responsabile**. Trattamenti:
log strutturati e archivio audit 12 mesi **già dichiarati** nel manifesto (`logs.structured`, #13 E); **nuova voce**
`logs.frontend_errors` (solo errori JS: messaggio/stack, rotta, build, id opachi; niente IP/user agent; base = legittimo
interesse #08 H23; retention 7/30 gg) + RoPA rigenerato. **Classificazione: MINOR** (componente: platform core) — nessuna
nuova finalità/base/categoria oltre la famiglia "sicurezza/diagnostica" già dichiarata, nessuna retention nuova; quando
`content/legal/` esisterà (UC 0002), la voce vale come notice, non ri-accettazione.

## Problema / Obiettivo

Stabilire la baseline di osservabilità cost-min e future-proof decisa in #08 (1–33): oggi i servizi loggano testo
semplice senza campi strutturati né correlazione, non esistono health endpoint, metriche di business, dashboard,
allarmi, budget, né archivio audit. Obiettivo: ogni segnale porta `tenant_id`/`app_id`/`user_id`/`correlation_id`
(invariante #4 garantita dal framework), e l'infrastruttura di osservazione (dashboard/allarmi/SNS/Budgets/archivio) è
interamente as-code.

## Scope

### Backend (services/commons + core + fatture)
1. **Log JSON strutturato**: estensione `quarkus-logging-json` in core/fatture; `quarkus.log.console.json=true` in
   test/prod, testo leggibile in locale (`%dev`). Nomi campo secondo le semantic conventions OTel (`trace_id`,
   `span_id`, `service.name`). Nessun dato personale in chiaro (identificativi opachi).
2. **MDC dal JWT**: estendere `MdcRequestFilter` (commons) con `correlation_id` letto dall'header `X-Correlation-Id`
   iniettato all'edge (fallback: nuovo UUID); restano `request_id`, `tenant_id`, `user_id`. `AppIdMdcFilter` anche per
   `services/core` (`app_id=platform`), oggi presente solo in fatture.
3. **Livelli log**: INFO in test/prod, DEBUG in locale; DEBUG attivabile a runtime via SSM Parameter Store
   (parametro per servizio letto come variabile d'ambiente ECS `valueFrom` — cambio = update parametro + force new
   deployment, nessun rebuild).
4. **Metriche business — Micrometer→EMF**: `quarkus-micrometer` + bridge EMF in commons (metriche scritte dentro i log
   JSON in Embedded Metric Format, niente `PutMetricData`). Dimensioni solo a bassa cardinalità (`app_id`, `env`,
   `endpoint`, `status`, `service`); `tenant_id`/`user_id` mai come dimensioni (regola dei due piani). Prime metriche
   mirate: conteggio errori per app, latenza operazioni chiave.
5. **Tracing strumentato, export spento**: `quarkus-opentelemetry` con propagazione W3C (`traceparent`) e
   `trace_id`/`span_id` nei log; exporter disabilitato via config (costo $0, nessun collector).
6. **Health**: `quarkus-smallrye-health` — liveness `/q/health/live` senza DB; readiness `/q/health/ready` col DB,
   tarata sul cold start Aurora (~10-15 s). Health check del container ECS agganciato alla liveness. Endpoint non
   esposti dalle rotte API Gateway (`/api/<app_id>/v1/*`): nessuna esposizione pubblica.
7. **AuditLogger (commons)**: API per eventi audit/sicurezza come log JSON con campo fisso `log_type:"audit"` +
   campi evento (azione, esito, MDC). Strumentazione degli eventi già esistenti: export/cancellazione GDPR e azioni
   admin rilevanti in core. Gli eventi auth (login/logout/2FA…) arriveranno da UC 0014 con la stessa convenzione.

### Infra (Terraform)
8. **Correlation id all'edge**: parameter mapping sull'integrazione API Gateway (`microsaas_app/api.tf`) che appende
   `X-Correlation-Id` = `$context.requestId`; access logging dello stage abilitato (chiude la soppressione checkov
   `CKV_AWS_76` in `platform_shared/ingress.tf`).
9. **SNS**: due topic per ambiente, `critical` e `warning`, sottoscrizione email (indirizzo via variabile), predisposti
   per Slack/Telegram futuri.
10. **Allarmi as-code, set minimo anti-rumore** (con durata, non spike): 5xx API Gateway, latenza p95, errori Aurora
    (connessioni/ACU/storage), ERROR applicativi via metric filter per servizio, DLQ non vuota (export sqs mancanti:
    aggiungere output ARN DLQ al modulo), task ECS che non parte / health fallito ripetuto. **Pieni in prod,
    silenziati in test** via variabile (lo scale-to-0/spegnimento notturno non deve generare falsi allarmi).
11. **Dashboard as-code** (≤3 per ambiente, nei limiti gratuiti dove possibile): overview per-env + sezione
    per-servizio generata dal modulo `microsaas_app` (widget su log group, 5xx, latenza, code) + auth/sicurezza
    (minimale finché l'auth cloud non esiste). Drill-down per-tenant: query Logs Insights pre-salvate linkate, non
    widget per-tenant.
12. **AWS Budgets $100/mese** a livello account (`infra/global`): soglie 75/90/100% su speso reale + alert su
    forecast >100%.
13. **Archivio audit/sicurezza**: subscription filter con pattern `{ $.log_type = "audit" }` su ogni log group
    applicativo (generato dal modulo) → Kinesis Firehose → S3 (SSE) → lifecycle Glacier → scadenza 12 mesi.
14. **Ingest errori frontend**: rotta API Gateway + Lambda + log group dedicato (retention come gli applicativi);
    la Lambda valida/normalizza e logga in JSON (stessa convenzione campi).
15. Retention: già a posto per i log group del modulo (7/30 gg); retention esplicita anche sui nuovi log group
    (access log API GW, ingest errori, Lambda).

### Frontend
16. **Handler globale errori** (`window.onerror` + `onunhandledrejection`) nelle app `backoffice` e `admin` tramite
    utility condivisa in un package frontend: POST all'ingest con `app_id`, rotta, SHA di build, `user_id`/`tenant_id`
    se disponibili; solo errori (nessun tracking comportamentale), dedupe/rate-limit di base per non inondare.

## Fuori scope

- Accensione export tracce (E10), Grafana (E11), error tracking dedicato (E12), uptime esterno non-AWS (E13), RUM (E14),
  allarmi VIP per-tenant (E15).
- Canary prod cross-region → UC 0007.
- Eventi audit dei flussi auth (login/logout/2FA/lockout) e esclusione health dall'authorizer → UC 0014 (l'authorizer
  non esiste ancora; oggi gli health non sono comunque raggiungibili dalle rotte pubbliche).
- Handler errori nello scaffold `new-application` → UC 0046; caricamento source map come artifact CI → già UC 0005.
- `services/auth-local` (solo sviluppo locale): nessuna strumentazione.
- Nessun `terraform apply` (pattern consolidato: solo fmt/validate/test/plan).

## Criteri di accettazione

- [ ] Una richiesta autenticata a core/fatture produce log JSON con `tenant_id`, `app_id`, `user_id`,
      `correlation_id` (+ `trace_id`/`span_id`), verificato da test sul wiring MDC; nessun dato personale in chiaro.
- [ ] `/q/health/live` risponde senza toccare il DB; `/q/health/ready` verifica il DB; health check ECS configurato.
- [ ] Le risorse Terraform nuove (SNS, allarmi, dashboard, Budgets, Firehose/S3/Glacier, ingest errori, parameter
      mapping) passano `terraform test`/`validate`/`plan` e checkov; allarmi con `actions_enabled` pieno in prod e
      silenziato in test; DLQ ARN esportati dal modulo.
- [ ] Un evento `AuditLogger` finisce nel log JSON con `log_type:"audit"` e il subscription filter lo instrada (pattern
      verificato nel test Terraform); gli eventi GDPR/admin esistenti sono strumentati.
- [ ] L'handler frontend cattura un errore non gestito e invia il payload atteso all'ingest (unit test); le app
      restano funzionanti (suite frontend verde).
- [ ] `run-tests.sh` verde per tutte le aree toccate; `infra/scripts/check` aggiornato se nascono nuovi moduli testati.

## Invarianti appgrove toccati

- **#4 Logging strutturato (tenant_id/app_id/user_id)**: è l'oggetto della change — garantito dal framework
  (MDC dal JWT in commons), non dal singolo sviluppatore.
- **#1 Tenant ID solo dal JWT**: l'MDC legge `tenant_id` esclusivamente dal JWT verificato (filtro esistente esteso,
  mai da header/body); l'ingest errori frontend tratta `tenant_id`/`user_id` come contesto non fidato (solo log, mai
  usati per autorizzare).
- **#3 Modulo `microsaas_app`**: widget, allarmi, metric filter e subscription filter per-servizio sono generati dal
  modulo, non cablati a mano per app.
- **#2 Filtro row-level**: non toccato (nessuna query nuova tenant-scoped).

## Requisiti di test

- Test backend (JUnit) sul wiring MDC: log di una richiesta autenticata contiene i 4 campi; `correlation_id` riusa
  l'header se presente, generato altrimenti. Test `AuditLogger` (campo `log_type`, niente dati personali in chiaro).
- Test health: liveness up senza DB; readiness riflette lo stato DB.
- `terraform test` (provider mock) per le nuove risorse: retention dei nuovi log group, pattern del subscription
  filter, `actions_enabled` per env, presenza output DLQ.
- Unit test frontend dell'handler (cattura, payload, dedupe). E2E esistenti restano verdi.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No (log format cambia in test/prod ma nessun consumatore automatico esistente) |
| Contratto cross-area | Sì — frontend → ingest errori (nuova rotta API GW); edge → servizi (header `X-Correlation-Id`) |
| Version bump | minor |
