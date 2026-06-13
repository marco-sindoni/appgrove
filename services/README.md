# services — microservizi Quarkus

Un microservice **Quarkus** (GraalVM native) per ogni app. API REST con Cognito authorizer su
ogni endpoint, deploy su ECS Fargate, routing via API Gateway v2.

```
services/
├── notes/        # un'app = una cartella = un servizio
└── dashboard/
```

## Invarianti da rispettare in ogni servizio

- **Tenant ID solo dal JWT verificato** — mai da request body/params:
  ```java
  @Inject JsonWebToken jwt;
  String tenantId = jwt.getClaim("sub"); // mai dal body
  ```
- **Filtro row-level** `WHERE tenant_id = :tid` su ogni query tenant-scoped.
- **Schema DB dedicato** per app sull'istanza Aurora condivisa (`app_notes`, …).
- **Logging strutturato** (MDC): ogni log porta `tenant_id`, `app_name`, `user_id`.

## Test

```bash
mvn test                      # da services/<app>/
mvn -Dtest=ClassName test     # singola classe, ciclo veloce
```

Preferire fixture deterministiche offline (test profile Quarkus, Testcontainers, JWT/Cognito
mockati) alle chiamate AWS/HTTP live.
