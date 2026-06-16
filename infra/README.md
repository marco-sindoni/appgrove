# infra — Terraform (HCL)

Infrastruttura come codice per appgrove. Provisiona VPC, Cognito, CloudFront+S3 (×2 app),
API Gateway v2 + Lambda authorizer, ECR, Aurora Serverless v2 + RDS Proxy, ECS Fargate,
EventBridge/SQS, Route53/ACM, Secrets Manager/SSM. Decisioni: [docs/06-infra-iac.md](../docs/06-infra-iac.md).

## Modulo riusabile `microsaas_app`

Building block di tutto: aggiungere un microservizio = **istanziare il modulo** (nome, porta, schema DB,
route API…). Eliminarlo = togliere il blocco e fare `apply`/`destroy` mirato. Non scrivere infra parallela a mano.

```hcl
module "notes" {
  source    = "./modules/microsaas_app"
  app_id    = "notes"
  port      = 8080
  db_schema = "app_notes"
}
```

## Provisioning / deprovisioning

```bash
terraform apply     # crea/aggiorna tutta l'infrastruttura
terraform destroy   # smonta tutto (spegnere l'iniziativa senza residui che generano costi)
```

State remoto in **S3 + lock DynamoDB**, stati separati per ambiente (local/test/prod).

## Scelte chiave

- Persistenza cost-first: una istanza Aurora condivisa, **schema separato per app** (`app_notes`, …);
  path verso istanze dedicate = modifica Terraform.
- API Gateway con **custom Lambda authorizer** (JWT + entitlement). CloudWatch per logging.
- **Principio costo-minimo** (→ [docs/_COSTI-AWS.md](../docs/_COSTI-AWS.md)): attenzione al NAT Gateway.

## Test

```bash
terraform fmt -check && terraform validate
terraform plan      # anteprima delle modifiche (no apply)
```
