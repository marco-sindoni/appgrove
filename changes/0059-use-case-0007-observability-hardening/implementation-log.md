# Implementation log — 0059 · Observability hardening (UC 0007)

## Esito

Canary di uptime cross-region implementato **full-IaC** come nuovo modulo Terraform, istanziato **solo in
prod** in **eu-central-1**, protetto da un gate di attivazione al go-live. Nessun `apply` (prod non è live).
Modalità **autopilot**; escalation sul bersaglio del ping risolta dallo sviluppatore (**Opzione A**).

## Cosa è stato fatto

**Nuovo modulo `infra/modules/uptime_canary/`**
- [main.tf](../../infra/modules/uptime_canary/main.tf): EventBridge schedulato (`rate(1 minute)`) → Lambda di
  ping → metrica custom `appgrove/uptime` `Healthy` → allarme (`Minimum(Healthy) < 1` per 3 periodi da 60s,
  `treat_missing_data = breaching`) → topic **SNS in-regione** `appgrove-prod-uptime-alarms` + subscription
  email. Ruolo IAM least-privilege (`logs` + `cloudwatch:PutMetricData` ristretto al namespace), log group
  con retention 30 giorni. Soppressioni checkov documentate (fuori-VPC by-design: il vantage point è esterno).
- [lambda/uptime_ping.py](../../infra/modules/uptime_canary/lambda/uptime_ping.py): GET del bersaglio con
  `urllib` (nessuna dipendenza), pubblica `Healthy` = 1 (2xx/3xx) / 0 (altro/errore/timeout). Logica pura
  (`classify`/`evaluate`/`build_metric_data`) separata dagli effetti collaterali → testabile offline.
- [variables.tf](../../infra/modules/uptime_canary/variables.tf), [outputs.tf](../../infra/modules/uptime_canary/outputs.tf),
  [versions.tf](../../infra/modules/uptime_canary/versions.tf).

**Cablaggio prod** — [infra/envs/prod/main.tf](../../infra/envs/prod/main.tf): provider alias
`aws.eu_central_1`, variabile `uptime_canary_enabled` (default `false`), blocco `module "uptime_canary"` con
`count` di gating. **Niente in test** (romperebbe lo scale-to-0). **Budget invariato a $100** (nessun `apply`,
nessuna spesa reale: abbassarlo ora darebbe solo falsi allarmi).

**Test**
- [tests/plan.tftest.hcl](../../infra/modules/uptime_canary/tests/plan.tftest.hcl): `terraform test` con
  provider finto (offline) — schedule, bersaglio, metrica/allarme (`LessThanThreshold`, `breaching`, 3 periodi),
  nome SNS, least-privilege del namespace.
- [lambda/test_uptime_ping.py](../../infra/modules/uptime_canary/lambda/test_uptime_ping.py): `unittest`
  offline (nessuna rete, nessun boto3) — classificazione HTTP, valutazione, payload metrica, handler.
- Registrazione: `modules/uptime_canary` in [infra/scripts/check](../../infra/scripts/check); test Lambda in
  [run-tests.sh](../../run-tests.sh).

**Tracciamento decisioni differite** (regola CLAUDE.md)
- Endpoint di **stato pubblico end-to-end** (Opzione C) → [UC 0006 §Punti aperti](../../docs/usecases/02-devops-infra/0006-osservabilita-base.md) (owner UC 0006/0003).
- Ritocco Budget $100→$80 e anti-rumore a regime + attivazione al go-live → [UC 0007 §Punti aperti](../../docs/usecases/02-devops-infra/0007-osservabilita-irrobustimento.md).

## Test — esito

`./run-tests.sh infra` **verde**: `terraform fmt` canonico, `validate` su tutte le root (incluso `envs/prod`
col nuovo modulo), `terraform test` dei 4 moduli (canary compreso), test Lambda Python (8/8), **checkov 0
rilievi** (612 pass, 214 skip documentati), actionlint verde. Nessun `apply` (per progetto: prod non live).

## Note per il go-live (runbook, non codice)

Portare `uptime_canary_enabled = true` in `envs/prod`, applicare, e **confermare la subscription email** del
topic SNS `appgrove-prod-uptime-alarms` (in eu-central-1). Verifica: un down simulato di `app.appgrove.app`
deve produrre la notifica; nessun canary in test.
