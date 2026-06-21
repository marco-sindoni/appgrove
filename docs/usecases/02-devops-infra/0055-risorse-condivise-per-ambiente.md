# UC 0055 — Risorse condivise per-ambiente (Aurora SsV2 + RDS Proxy, ECS cluster, API GW HTTP + VPC Link + Cloud Map, EventBridge bus, CloudFront SPA)

**Area**: 02-devops-infra · **Fase**: 1 · **Stato**: 🟡 in corso
**Dipendenze**: UC [0003](0003-fondamenta-terraform.md) (foundation) — prerequisito di UC [0004](0004-modulo-microsaas-app.md)/[0005](0005-pipeline-cicd.md)/[0006](0006-osservabilita-base.md)
**Fonte decisioni**: #06 (Aurora/ECS/API GW/EventBridge/CloudFront), #12 (DB test scale-to-0)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [06-infra-iac](../../06-infra-iac.md), [12-environments-config](../../12-environments-config.md), [_COSTI-AWS](../../_COSTI-AWS.md), [_EVOLUZIONI-DEVOPS](../../_EVOLUZIONI-DEVOPS.md)

## 1. Obiettivo / Scope
Provisionare le **risorse condivise per-ambiente** (lo strato tra la foundation UC 0003 e il modulo per-app UC 0004): le risorse
**stateful** ed **edge** che le app e le SPA **presuppongono già esistenti** e che nessun altro UC creava.
**Incluso**: **Aurora Serverless v2** (1 cluster writer per env, **scale-to-0** min 0 ACU/auto-pause in test e prod, **RDS Proxy**,
**PITR** ~7gg, **deletion protection + snapshot finale** su prod / `force_destroy` su test) (#06 13/14/15/16, #12 4); **cluster ECS**
per env (#06 9); **API Gateway HTTP API v2** + **VPC Link** + **Cloud Map namespace** per env (ingress condiviso, no ALB) (#06 8);
**bus EventBridge dedicato** per env (instrada `tenant.offboarded` alle code SQS per-servizio) (#06 19); **2 distribuzioni CloudFront**
delle SPA — **backoffice** (`app.appgrove.app`) e **admin** (`admin.appgrove.app`) — con **S3 privato + OAC**, fallback SPA, cert
us-east-1 (#06 17, #03 13/17).
**Escluso**: lo schema/ruolo/route/coda per-app (li crea il modulo `microsaas_app`, UC 0004); l'authorizer custom (UC 0014); le
tabelle (Flyway, UC 0005/0012); la distribuzione CloudFront della **vetrina** Astro (UC 0036, separata).

## 2. Attori & ruoli
- **Platform Engineer / CI**: applica via i wrapper di UC 0003 (`up <env>`).
- **Modulo `microsaas_app`** (UC 0004): aggancia ECS service/route/ruolo-DB/coda-SQS a queste risorse condivise.
- **Pipeline FE** (UC 0005): pubblica i bundle sulle 2 distribuzioni CloudFront.

## 3. Precondizioni
- Foundation attiva (UC 0003): VPC no-NAT + endpoints, Route53/ACM, KMS/secrets baseline, state, OIDC.

## 4. Flusso principale
1. **Aurora Serverless v2**: 1 cluster writer per env nelle subnet (SG stretto), **min 0 ACU** (auto-pause/scale-to-0, #12 4), **RDS Proxy**
   davanti (usato da Lambda auth/pre-token-gen/authorizer e dai task Flyway, #05 3), **PITR ~7gg**; **prod**: deletion protection + snapshot finale; **test**: `force_destroy` (#06 K).
2. **Cluster ECS** per env (Fargate; on-demand prod, Spot in test), su cui il modulo registra i service/task (#06 9/10).
3. **Ingress**: **API Gateway HTTP API v2** + **VPC Link** verso un **Cloud Map namespace** per env; l'authorizer (UC 0014) e le route `/api/<app_id>/v1/*` (UC 0004) vi si agganciano (#06 8).
4. **Bus EventBridge** dedicato per env: riceve `tenant.offboarded` e lo instrada alle code SQS purge per-servizio (UC 0004/0032/0035) (#06 19).
5. **2 distribuzioni CloudFront** (backoffice + admin) con **S3 privato + OAC**, fallback `index.html` (SPA), cert ACM us-east-1 + alias Route53 (#06 17, #03 17).

## 5. Flussi alternativi / edge / errori
- **Scale-to-0 / cold-start**: Aurora auto-pause a riposo; la readiness app è tarata sul cold-start (UC 0006 §4.7); `test-stop`/`test-start` (UC 0004) agiscono sui desired count ECS, **non** sull'auto-pause Aurora (gestito dal cluster).
- **`down prod`**: deletion protection + snapshot + conferma digitata (#06 K, UC 0003 §5).
- **Teardown completo**: queste risorse vanno prima di `global` e dopo gli `module "app_*"` (ordine UC 0003 §5).

## 6. Risorse & runbook
**Risorse Terraform** (in `infra/envs/{test,prod}/`, o modulo dedicato `infra/modules/platform_shared/`): Aurora cluster + RDS Proxy,
ECS cluster, API GW HTTP + VPC Link + Cloud Map namespace, EventBridge bus, 2 CloudFront + bucket S3 OAC. **Runbook**: create da
`up <env>` insieme alla foundation; il modulo per-app (UC 0004) le referenzia via output/data source. **Rollback**: `down <env>` con le safety #06 K (snapshot prod).

## 7. Dati toccati
Nessun dato applicativo/personale: risorse infra. Aurora/S3/EBS/SQS/log **cifrati** (KMS, #06 §20bis). Residency UE (eu-west-1) (#13 I51).

## 8. Permessi & gate
- **Invarianti**: abilita il modulo `microsaas_app` (invariante #3) e l'isolamento DB per-servizio (un ruolo per schema, #05 11) sul cluster condiviso.
- **Guardrail**: prod sempre esplicito; deletion protection + snapshot su Aurora prod (#06 K).

## 9. Requisiti di test
- **Infra** (#10 H): `fmt`/`validate`/**tflint**/**checkov** (soppressioni documentate per subnet pubbliche → E1) + **Infracost** sul delta (#10 30).
- Verifica: il modulo per-app (UC 0004) `terraform plan` risolve correttamente cluster ECS, VPC Link/Cloud Map, RDS Proxy e bus EventBridge condivisi.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #06 8/9/13/14/15/16/17/19, #05 3, #12 4, #13 I51.
- **DoD**:
  1. Aurora SsV2 per env (scale-to-0, RDS Proxy, PITR; prod con deletion protection+snapshot).
  2. Cluster ECS, API GW HTTP + VPC Link + Cloud Map namespace, bus EventBridge per env.
  3. 2 distribuzioni CloudFront (backoffice+admin) con S3 OAC + fallback SPA.
  4. UC 0004 si aggancia a queste risorse senza crearle; teardown con le safety #06 K.
