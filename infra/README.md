# infra — AWS CDK (TypeScript)

Infrastruttura come codice per appgrove. Provisiona VPC, Cognito, CloudFront+S3, API Gateway v2,
ECR, Aurora Serverless v2 PostgreSQL, ECS Fargate.

## Construct riusabile `MicroSaasApp`

Building block di tutto: aggiungere un'app = **istanziare il construct** con nome, porta e schema
DB. Non scrivere infra parallela a mano.

```ts
new MicroSaasApp(this, "Notes", { name: "notes", port: 8080, dbSchema: "app_notes" });
```

## Scelte (dal recap)

- Persistenza cost-first: una istanza Aurora condivisa, **schema separato per app**
  (`app_notes`, `app_dashboard`, …). Path di migrazione verso istanze dedicate = modifica CDK.
- Cognito authorizer su ogni endpoint API Gateway.
- CloudWatch per logging centralizzato.

## Test

```bash
npm test   # assertion/snapshot dei construct — non deployare per testare
```
