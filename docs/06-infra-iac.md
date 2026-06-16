# Infrastruttura / IaC (Terraform) — Decisioni

**Stato**: 🟡 in corso
**Ultimo aggiornamento**: 2026-06-16

## Scope
Provisioning di tutta l'infrastruttura AWS come codice con **Terraform**: networking, compute, DB, edge/DNS,
auth infra, messaging, secrets, e il **modulo `microsaas_app`** (ciclo di vita di un microservizio). Include
provisioning **e** deprovisioning completi. Non copre la pipeline che *esegue* Terraform (→ [07-devops-cicd](07-devops-cicd.md))
né le convenzioni applicative (→ [04-services-backend](04-services-backend.md)).

## ⚠️ Cambio rispetto al recap: Terraform sostituisce CDK
Il recap indicava **AWS CDK (TypeScript)** con *construct* `MicroSaasApp`. **Decisione superata**: si usa
**Terraform (HCL)**. Il concetto resta identico, cambia il nome: il **construct CDK** diventa un **modulo Terraform
`microsaas_app`**. Ovunque nei doc precedenti "CDK/construct" va letto come "Terraform/modulo".

## Direttive dell'utente (decise, 2026-06-16)
1. **IaC = Terraform** (no CDK).
2. **Provisioning e deprovisioning completi**: `terraform apply` crea tutto, `terraform destroy` smonta tutto
   (requisito: poter spegnere l'iniziativa se non porta risultati, senza residui che generano costi).
3. **Ciclo di vita dei microservizi via Terraform**: aggiungere/eliminare un backend = aggiungere/togliere
   un'istanza del modulo `microsaas_app` e fare `apply`/`destroy` (mirato).

## Vincoli ereditati (da concretizzare qui)
- Aurora Serverless v2 (scale-to-0 su test), **RDS Proxy** per le Lambda, **ruolo DB per servizio** (least privilege).
- API Gateway v2 (HTTP API) + **custom Lambda authorizer** (JWT + entitlement). Cognito (pool dev/test/prod).
- Lambda: auth BFF, pre-token-gen, authorizer (in VPC, accesso DB via RDS Proxy).
- **EventBridge + SQS** per la purge per-tenant. **2 distribuzioni S3/CloudFront** per env (backoffice + admin).
- Route53 (`appgrove.app`) + ACM (wildcard `*.appgrove.app`, `*.test.appgrove.app`). Secrets: SSM + Secrets Manager (DB).
- **Principio costo-minimo** (→ [_COSTI-AWS.md](_COSTI-AWS.md)): leva grossa = **NAT Gateway** (~$32/mese, evitabile).

## Topic dell'area (agenda) — con spiegazione
- **A. Terraform: state & struttura** — dove vive lo "state" (S3 + lock DynamoDB), separazione per ambiente,
  organizzazione in moduli, regione AWS.
- **B. Networking (VPC)** — rete privata AWS: subnet pubbliche/private, **NAT Gateway sì/no** (costo!), VPC endpoint, security group.
- **C. Compute (ECS Fargate)** — dove girano i container dei servizi: cluster, task definition, service, autoscaling.
- **D. Container registry (ECR)** — dove stanno le immagini Docker dei servizi; lifecycle policy.
- **E. Database (Aurora + RDS Proxy)** — cluster Aurora Serverless v2, scale-to-0, RDS Proxy, ruoli/grant per servizio, backup/PITR, deletion protection.
- **F. Edge & DNS** — S3+CloudFront (×2 app), API Gateway + authorizer, Route53, ACM.
- **G. Auth infra** — Cognito pool, le 3 Lambda, loro accesso a VPC/DB.
- **H. Messaging** — EventBridge bus + code SQS per la purge.
- **I. Secrets/config** — SSM Parameter Store + Secrets Manager per env.
- **J. Modulo `microsaas_app`** — cosa crea un'istanza del modulo per un nuovo microservizio (direttiva #3).
- **K. Lifecycle apply/destroy** — provisioning/deprovisioning completi e relative sicurezze sui dati stateful (direttiva #2).

## Decisioni prese
1. **IaC = Terraform (HCL)**; supera CDK. Il mattone riusabile è il **modulo `microsaas_app`**.
2. **Provisioning + deprovisioning completi** via `apply`/`destroy` (con accortezze su risorse stateful → topic K).
3. **Ciclo di vita microservizi** dichiarativo: un'istanza del modulo per servizio.

## Questioni aperte
_Vedi agenda (A→K)._

## Alternative valutate / scartate
- **AWS CDK (TypeScript)** — scartato a favore di Terraform (richiesta utente: script di provisioning/deprovisioning Terraform).

## Impatti su altre aree
- [04-services-backend](04-services-backend.md), [05-persistenza-dati](05-persistenza-dati.md), [07-devops-cicd](07-devops-cicd.md), [12-environments-config](12-environments-config.md), [_COSTI-AWS.md](_COSTI-AWS.md)
