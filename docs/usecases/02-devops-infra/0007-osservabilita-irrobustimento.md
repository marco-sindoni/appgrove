# UC 0007 — Observability hardening (canary eu-central-1 prod, tuning Budgets)

**Area**: 02-devops-infra · **Fase**: 7 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0006](0006-osservabilita-base.md) (baseline observability)
**Fonte decisioni**: #08 G22 (canary cross-region), #08 17/18 (Budgets/allarmi)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [08-observability](../../08-observability.md), [06-infra-iac](../../06-infra-iac.md), [_EVOLUZIONI-DEVOPS](../../_EVOLUZIONI-DEVOPS.md)

## 1. Obiettivo / Scope
Irrobustire l'observability verso il **go-live**: uptime esterno cross-region e tuning degli allarmi/budget.
**Incluso**: **canary AWS-native cross-region** (EventBridge schedulato → Lambda di ping sull'health endpoint pubblico →
metrica → SNS), **interamente in eu-central-1 (Francoforte)**, separato da eu-west-1 (sopravvive a un outage regionale), **solo
prod, dal go-live**; **tuning Budgets** (eventuale abbassamento soglia $100→$80) e affinamento set allarmi anti-rumore.
**Escluso**: la baseline (UC 0006), l'accensione tracce/Grafana/RUM (evoluzioni E10/E11/E14), il monitor esterno non-AWS (E13).

## 2. Attori & ruoli
- **Canary (eu-central-1)**: pinga l'health prod e avvisa via SNS.
- **Platform Engineer**: riceve allarmi/budget tuning.

## 3. Precondizioni
- Baseline observability (UC 0006); prod live (go-live); health endpoint pubblico leggero (UC 0006/0003).

## 4. Flusso principale
1. **Canary cross-region**: EventBridge schedulato in **eu-central-1** → Lambda di ping sull'health pubblico → metrica → **SNS** (#08 G22).
2. **Solo prod, dal go-live**; **niente in test** (romperebbe lo scale-to-0) (#08 G22).
3. **Tuning Budgets**: eventuale abbassamento $100→$80 (modifica banale) + affinamento allarmi anti-rumore a regime (#08 17/18).
4. Resta **future-proof**: accensione tracce (E10), Grafana (E11), RUM (E14), monitor esterno (E13) tracciate ma non ora.

## 5. Flussi alternativi / edge / errori
- **Outage eu-west-1**: il canary in eu-central-1 sopravvive e avvisa (#08 G22).
- **Falsi allarmi**: il tuning anti-rumore evita allarmi fisiologici; test resta silenziato (#08 18).
- **Costo**: canary ~$0, full-IaC (#08 G22).

## 6. Risorse & runbook  _(infra)_
**Risorse Terraform**: stack canary in **eu-central-1** (EventBridge schedule + Lambda ping + metrica + SNS), separato da eu-west-1;
tuning Budgets/allarmi. **Runbook**: attivare al go-live; allarme canary → verifica disponibilità prod (anche durante outage regionale).

## 7. Dati toccati
Solo segnali tecnici (ping/metrica), nessun dato personale; health endpoint non porta PII (#08 5/21). Residency: eu-central-1 (UE).
Manifest: N/A.

## 8. Permessi & gate
- **Invarianti #4**: i segnali restano coerenti (low-cardinality dimensioni); nessun dato tenant nel canary.
- **Gate**: solo prod (mai test); cross-region per resilienza.

## 9. Requisiti di test
- Verifica: il canary in eu-central-1 rileva un down di prod e notifica via SNS; nessun canary in test; budget/allarmi tarati (no falsi positivi).

## 10. Riferimenti & Definition of Done
- **Decisioni**: #08 G21/G22, 17/18, evoluzioni E10-E14.
- **DoD**:
  1. Canary cross-region in eu-central-1 (EventBridge→Lambda ping→SNS), solo prod dal go-live.
  2. Sopravvive a outage eu-west-1; costo ~$0 full-IaC.
  3. Tuning Budgets ($100→$80 opzionale) + allarmi anti-rumore.
  4. Verifica down→notifica; niente canary in test.
