# Change 0032: Risorse condivise per-ambiente (modulo `platform_shared`)

**Branch**: `change/0032-use-case-0055-risorse-condivise-per-ambiente`
**Aree**: infra
**Data**: 2026-07-05
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/02-devops-infra/0055-risorse-condivise-per-ambiente.md](../../docs/usecases/02-devops-infra/0055-risorse-condivise-per-ambiente.md)
**Tocca dati personali?**: No — solo risorse infrastrutturali; nessun dato applicativo o personale (UC 0055 §7). Lo scanner dei segnali privacy gira comunque a step-03.

## Problema / Obiettivo

Provisionare lo strato di **risorse condivise per-ambiente** che sta tra la foundation (UC 0003, già scritta)
e il modulo per-app `microsaas_app` (UC 0004, prossimo): le risorse stateful ed edge che ogni app e le due SPA
presuppongono già esistenti. Senza questo strato, UC 0004 non ha nulla a cui agganciare service, route e code.

## Scope

Nuovo modulo **`infra/modules/platform_shared/`**, istanziato in `infra/envs/test/main.tf` e
`infra/envs/prod/main.tf` (parità test/prod, differenze via input), con:

1. **Aurora Serverless v2** (PostgreSQL): 1 cluster writer per env, **min 0 ACU** (scale-to-0/auto-pause,
   test e prod) / **max ≈2 ACU**, **PITR ~7 giorni**, credenziali master in **Secrets Manager**, security
   group stretto; **prod**: deletion protection + snapshot finale; **test**: distruzione libera (#06 13–16, #12 4).
2. **RDS Proxy** davanti al cluster, **solo per le Lambda** auth/pre-token-gen/authorizer (UC 0014); i task
   Fargate — incluso Flyway — si connetteranno **diretti** (#05 dec.3).
3. **Cluster ECS** per env (Fargate; capacity provider **Spot in test, on-demand in prod**) (#06 9/10).
4. **API Gateway HTTP API v2** + **VPC Link** + **Cloud Map namespace** per env (ingress condiviso, no ALB);
   authorizer e route per-app si aggancono negli UC 0014/0004 (#06 8).
5. **Bus EventBridge dedicato** per env, destinato all'instradamento di `tenant.offboarded` verso le code
   SQS per-servizio (create da UC 0004) (#06 19).
6. **2 distribuzioni CloudFront** per env — backoffice (`app.appgrove.app` / `app.test.appgrove.app`) e admin
   (`admin.appgrove.app` / `admin.test.appgrove.app`) — con **S3 privato + OAC**, fallback SPA (403/404 →
   `index.html`), certificati ACM us-east-1 già emessi dallo stack `global`, record Route53 alias (#06 17, #03 17).

Inoltre: **output** del modulo per l'aggancio di UC 0004 (cluster ECS, namespace Cloud Map, VPC Link/API GW,
endpoint Proxy/cluster, ARN bus); cifratura at rest ovunque (KMS) e TLS in transito (#06 20bis); tag/naming
coerenti con `env_baseline`; aggiornamento di [docs/_COSTI-AWS.md](../../docs/_COSTI-AWS.md) se la stima viva
non copre già queste voci; allineamento intestazione UC 0055 a 🟢 (specifica decisa) e `_INDEX.md` (✅ a chiusura).

## Fuori scope

- **Risorse per-app** (ECR, service ECS, route, schema/ruolo DB, coda SQS purge): UC 0004.
- **Authorizer custom** (UC 0014), **Cognito user pool** per env (UC 0013/0014), **tabelle** (Flyway, UC 0005/0012).
- **CloudFront della vetrina** Astro (UC 0036).
- **Attivazione reale**: nessuna risorsa AWS viene creata — pattern change 0031 (attivazione differita via
  `first-run`, necessaria al più tardi con UC 0005). `terraform plan` reale e **Infracost** girano in CI (UC 0005).

## Criteri di accettazione

- [ ] `infra/modules/platform_shared/` esiste ed è istanziato in entrambi gli env con le 6 famiglie di risorse
      e le differenze per-ambiente corrette (protezioni/snapshot su prod, Spot e `force_destroy` su test).
- [ ] Il modulo espone gli output necessari a UC 0004 (cluster ECS, Cloud Map, VPC Link/API GW, DB/Proxy, bus).
- [ ] `./run-tests.sh infra` verde (fmt, validate, checkov con eventuali soppressioni documentate; tflint se installato).
- [ ] Nessuna risorsa AWS creata; nessuna modifica richiesta ai wrapper (`up <env>` include il nuovo modulo da sé).

## Invarianti appgrove toccati

- **Modulo Terraform `microsaas_app`** (invariante #3): questa change ne crea il presupposto — il substrato
  condiviso che il modulo referenzia senza crearlo. Gli altri invarianti (tenant dal JWT, filtro row-level,
  logging strutturato) non sono toccati: nessun codice applicativo.

## Requisiti di test

Suite infra di `run-tests.sh` (→ `infra/scripts/check`): `terraform fmt -check`, `terraform validate` su ogni
stack/modulo, checkov (soppressioni motivate nel codice). La verifica "UC 0004 risolve le risorse condivise via
output" si completa nel plan di UC 0004; il plan reale + Infracost restano alla CI (UC 0005, già tracciato).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A (contratto servizio ↔ infra nasce con UC 0004, che consuma gli output) |
| Version bump | nessuno |
