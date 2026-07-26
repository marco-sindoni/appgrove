# Requirements — 0059 · Observability hardening (UC 0007)

**Use case sorgente**: [docs/usecases/02-devops-infra/0007-osservabilita-irrobustimento.md](../../docs/usecases/02-devops-infra/0007-osservabilita-irrobustimento.md)
**Modalità**: autopilot (ON) · **Aree toccate**: `infra/` (Terraform) — nessun apply (solo `fmt`/`validate`/`test`)

## Obiettivo

Irrobustire l'observability verso il go-live con un **canary di uptime cross-region**: da
**eu-central-1 (Francoforte)**, separato da eu-west-1, si pinga periodicamente il prodotto pubblico e,
se non risponde, si avvisa via email. Cross-region perché il canary deve **sopravvivere a un outage di
eu-west-1** e poter comunque allertare. Tutto in Terraform, costo **~$0** (dentro il piano gratuito).

## Decisioni che delimitano la change (dettaglio in `decisions.json`)

- **Bersaglio del ping = `https://app.appgrove.app/`** (Opzione A, decisa dallo sviluppatore all'escalation).
  Lo UC assumeva un "health endpoint pubblico" che **non esiste** e per **invariante di sicurezza** non deve
  esistere (`/q/health/*` resta non esposto). Il canary sfrutta il valore vero — il **punto di vista esterno**
  — mentre il degrado backend resta coperto dagli allarmi **in-regione** già esistenti (UC 0006).
- **Solo prod, attivazione al go-live**: gated dietro `uptime_canary_enabled` (default `false`) così `up prod`
  prima del go-live non lo crea (eviterebbe falsi allarmi finché il sito non è live). **Niente in test.**
- **Budget**: soglia mensile lasciata a **$100** (non abbassata a $80): nessuna spesa reale ancora, abbassarla
  ora darebbe solo falsi allarmi. Ritocco opzionale a regime.

## Scope

**Incluso**
1. Nuovo modulo `infra/modules/uptime_canary/`, provider-agnostico, istanziato in eu-central-1:
   - Lambda Python (`urllib`, nessuna dipendenza): GET del bersaglio → pubblica la metrica custom
     `appgrove/uptime` `Healthy` = `1` (risposta 2xx/3xx) / `0` (altro o errore), dimensione `Target`;
   - `EventBridge` schedulato `rate(1 minute)` → invoca la Lambda;
   - allarme metrico (`Minimum(Healthy) < 1` per 3 periodi da 60s, `treat_missing_data = breaching`) →
   - topic **SNS in eu-central-1** `appgrove-prod-uptime-alarms` + subscription email;
   - log group con retention (30 giorni), ruolo IAM least-privilege (`logs` + `cloudwatch:PutMetricData`).
2. Cablaggio in `infra/envs/prod/main.tf`: provider alias `aws.eu_central_1`, variabile `uptime_canary_enabled`
   (default `false`), blocco `module "uptime_canary"` con `count` di gating.
3. Test: `terraform test` del modulo (provider mock, offline) + test unitario della Lambda Python; registrazione
   del modulo in `infra/scripts/check` e del test Lambda in `run-tests.sh`.

**Escluso (deliberatamente, con owner)**
- Endpoint di **stato pubblico end-to-end** (edge→ECS→DB) per un canary più profondo → proprietà **UC 0006/0003**,
  tracciato nei loro "Punti aperti".
- Abbassamento budget $100→$80 e affinamento fine anti-rumore → ritocchi a regime, tracciati in UC 0007.
- Accensione tracce (E10), Grafana (E11), RUM (E14), monitor esterno non-AWS (E13) → evoluzioni già tracciate.
- Nessun `apply`: prod non è live; le risorse nascono al go-live (variabile a `true`).

## Requisiti di test

- **`terraform test`** del modulo (`tests/plan.tftest.hcl`, `mock_provider`): la schedule è `rate(1 minute)`;
  la metrica dell'allarme è `Healthy` in namespace `appgrove/uptime` con `LessThanThreshold` e
  `treat_missing_data = breaching`; il topic SNS ha nome `appgrove-<env>-uptime-alarms`; in prod
  `actions_enabled = true`.
- **Lambda Python** (`unittest`, offline): `200`→`Healthy=1`, `503`/errore/timeout→`Healthy=0`; la metrica è
  pubblicata sul namespace/dimensione attesi.
- **Verde richiesto**: `./run-tests.sh infra` (fmt + validate su ogni root incluso `envs/prod` + `terraform test`
  dei moduli + test Lambda Python).

## Definition of Done

1. Modulo `uptime_canary` completo, istanziato in prod dietro gate, `envs/prod` valida.
2. Canary in eu-central-1 (EventBridge→Lambda ping→metrica→allarme→SNS), solo prod, ~$0 full-IaC.
3. Budget invariato a $100; ritocchi opzionali tracciati.
4. `./run-tests.sh infra` verde; deviazioni/deferral tracciati (UC 0006/0007).
5. UC 0007 → ✅ in `_INDEX.md` al merge.
