# Implementation Log — Change 0032: Risorse condivise per-ambiente (modulo `platform_shared`)

**Branch**: `change/0032-use-case-0055-risorse-condivise-per-ambiente`
**Aree**: infra
**Completata**: 2026-07-05

## File modificati

| File | Azione |
|---|---|
| `infra/modules/platform_shared/{versions,variables,data}.tf` | Creato |
| `infra/modules/platform_shared/aurora.tf` | Creato — cluster Aurora SsV2 + istanza `db.serverless` |
| `infra/modules/platform_shared/rds_proxy.tf` | Creato — proxy + ruolo IAM lettura segreto |
| `infra/modules/platform_shared/ecs.tf` | Creato — cluster + capacity provider Spot/on-demand |
| `infra/modules/platform_shared/ingress.tf` | Creato — API GW HTTP + VPC Link + Cloud Map + dominio `api.` |
| `infra/modules/platform_shared/eventbridge.tf` | Creato — bus dedicato per env |
| `infra/modules/platform_shared/cloudfront.tf` | Creato — 2 distribuzioni + bucket S3 OAC + DNS |
| `infra/modules/platform_shared/outputs.tf` | Creato — punti di aggancio per UC 0004/0005/0014 |
| `infra/envs/{test,prod}/main.tf` | Modificato — provider `us_east_1` + istanza `platform_shared` |
| `infra/envs/{test,prod}/outputs.tf` | Modificato — output essenziali (URL, bucket, cluster, endpoint DB) |
| `infra/README.md`, `infra/scripts/first-run` | Modificato — struttura, floor costi ~26 $/mese/env |
| `docs/_COSTI-AWS.md` | Modificato — RDS Proxy deciso/implementato; changelog UC 0055 |
| `docs/usecases/02-devops-infra/0055-…md`, `docs/usecases/README.md`, `_INDEX.md` | Modificato — stati (🟢 / ✅) |
| `docs/usecases/02-devops-infra/0004-…md`, `docs/usecases/04-platform-core/0014-…md` | Modificato — decisioni differite tracciate |

## Cosa è stato fatto

Creato il modulo `infra/modules/platform_shared/` (lo strato tra `env_baseline` e il futuro
`microsaas_app`) e istanziato in test e prod con parità di codice e differenze via input: Aurora
Serverless v2 con scale-to-0 (min 0 ACU, auto-pause 5', max 2 ACU, PITR 7gg, credenziali master
gestite da Secrets Manager; prod con deletion protection + snapshot finale), RDS Proxy TLS-only per
le sole Lambda auth, cluster ECS (Fargate Spot in test, on-demand in prod), API Gateway HTTP v2 +
VPC Link + namespace Cloud Map con dominio `api.<env>`, bus EventBridge dedicato, 2 distribuzioni
CloudFront (backoffice/admin) su S3 privato con OAC, fallback SPA, security headers gestiti e alias
Route53. Zona e certificati dello stack `global` risolti per nome via data source (nessun
accoppiamento con lo state remoto). Nessuna risorsa AWS creata (attivazione differita, pattern
change 0031).

## Decisioni prese

- **Certificati/zona via data source** (per nome/dominio) invece di `terraform_remote_state`:
  disaccoppia gli env dallo state di `global`; l'ordine di apply resta garantito da `first-run`.
- **Auto-pause Aurora a 300s** (minimo consentito): massimo risparmio, cold-start ~10-15s già
  accettato (#06 14); la readiness delle app sarà tarata in UC 0006.
- **Engine `aurora-postgresql` 16.6 pinnato** (min 0 ACU richiede ≥16.3): upgrade deliberati.
- **Security group perimetro-VPC** per Aurora e Proxy (i security group di servizi e Lambda non
  esistono ancora): UC 0004/0014 potranno stringere ai soli security group sorgente.
- **Policy gestite AWS su CloudFront** (`CachingOptimized` + `SecurityHeadersPolicy`): niente
  policy custom da mantenere.
- **PriceClass_100** su CloudFront (Europa+Nord America): cost-min coerente con #06 6.

## Invarianti appgrove

**Modulo `microsaas_app` (invariante #3)**: la change ne crea il presupposto — cluster ECS, ingress,
bus e database condivisi che il modulo per-app referenzia via output senza crearli (UC 0055 §8).
Gli altri invarianti non sono toccati (nessun codice applicativo).

## Note per il revisore

- **Contratto servizio ↔ infra**: nasce con UC 0004 (che consuma gli output del modulo); qui solo
  la superficie di output, nessun contratto attivo.
- **Decisioni differite tracciate** (gate CLAUDE.md): (1) **CORS sulla HTTP API condivisa** → UC 0004
  (con UC 0015), in `0004-modulo-microsaas-app.md` §Punti aperti; (2) **IAM auth delle Lambda verso
  il RDS Proxy** (oggi `DISABLED`) → UC 0014, in `0014-authorizer-custom.md` §Punti aperti.
- **Gate privacy: nessun segnale** (`npm run privacy-scan` → exit 0); solo risorse infra, nessun dato personale.
- **Costi**: nessuna voce nuova in `_COSTI-AWS.md`; dall'accensione di un env decorre il floor
  always-on ~26 $/mese (endpoint VPC + RDS Proxy) — README e `first-run` aggiornati. Il **minimo di
  fatturazione del RDS Proxy** resta da verificare alla prima accensione (già segnalato nella stima).
- Il `terraform plan` reale e **Infracost** girano in CI (UC 0005), come già tracciato in UC 0003.

## Test

Suite infra: `./run-tests.sh infra` → **verde** (`terraform fmt -check` + `terraform validate` su
tutte le root + **checkov**: 153 pass, 0 fail, 46 skip con soppressioni motivate inline — WAF→E6,
logging/insights→UC 0006, CMK→#06 §20bis, geo-restriction assente by-design, deletion protection
pilotata per ambiente). tflint saltato (non installato in locale — noto, in CI con UC 0005).
Non esistono unit test Terraform nel repo; la verifica "UC 0004 risolve le risorse via output" si
completa nel plan di UC 0004 (requisito di test del drill-down).

## Stato criteri di accettazione

- [x] `infra/modules/platform_shared/` esiste ed è istanziato in entrambi gli env con le 6 famiglie
      di risorse e le differenze per-ambiente corrette (protezioni/snapshot su prod, Spot e
      `force_destroy` su test).
- [x] Il modulo espone gli output necessari a UC 0004 (cluster ECS, Cloud Map, VPC Link/API GW,
      DB/Proxy, bus).
- [x] `./run-tests.sh infra` verde (fmt, validate, checkov; tflint non installato → saltato).
- [x] Nessuna risorsa AWS creata; nessuna modifica ai wrapper (`up <env>` include il nuovo modulo da sé).
