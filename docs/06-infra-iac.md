# Infrastruttura / IaC (Terraform) — Decisioni

**Stato**: 🟢 deciso (hardening tracciato in _EVOLUZIONI-DEVOPS.md; pipeline che esegue Terraform → #07)
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

### Terraform: state & struttura (topic A)
4. **State remoto su S3 + lock DynamoDB** (un piccolo step di *bootstrap* crea bucket/tabella la prima volta).
5. **Struttura per directory**: `envs/test` ed `envs/prod` con **state distinti** + uno stack **`global`** per le
   risorse condivise (zona Route53, pool Cognito `dev`) + **moduli** riusabili (incl. `microsaas_app`). `local` non usa infra AWS.
   `destroy` su test non tocca prod.
6. **Regione = `eu-west-1` (Irlanda)** — scelta **cost-min** (regione più economica e completa); la latenza per gli
   utenti italiani è mitigata da CloudFront (edge) per le SPA.

### Networking / VPC (topic B)
7. **Subnet pubbliche + security group stretti, NIENTE NAT Gateway** (cost-min): uscita internet (Paddle, Cognito)
   via internet gateway gratuito; ingresso solo dal load balancer/API GW. Hardening a subnet private + NAT tracciato
   come evoluzione **E1** in [_EVOLUZIONI-DEVOPS.md](_EVOLUZIONI-DEVOPS.md).
8. **Ingress = API Gateway → VPC Link → Cloud Map (service discovery)**, **no ALB** (cost-min, ~$0). Passaggio ad
   **ALB** tracciato come evoluzione **E2**.

### Compute / ECS Fargate (topic C)
9. **Task 0.25 vCPU / 0.5 GB** (immagini native GraalVM), **1 task per servizio** (HA = evoluzione **E3**). Un **cluster ECS per ambiente**.
10. **Capacità: Fargate Spot in test** (~−70%, servizi stateless dietro retry), **on-demand in prod** (stabilità).
11. **Test scale-to-0** quando idle (riaccensione scheduled/manuale). TODO: script semplice **start/stop** servizi test (→ backlog/#07).

### Container registry / ECR (topic D)
12. **Un repository ECR per servizio**; **lifecycle policy** (mantieni ultime ~10 immagini, scadenza untagged);
    **scan on-push** gratuito. Storage minimo.

### Database / Aurora (topic E)
13. **Un cluster Aurora Serverless v2 per ambiente** (1 writer, no read replica nel PoC); schema-per-app; **max ACU basso** (≈2).
14. **scale-to-0 sia su test sia su prod** (cost-min; cold-start ~10-15s da idle, accettabile nel PoC). Min ACU >0 su prod = evoluzione **E4**.
15. **RDS Proxy abilitato (test + prod)** per le Lambda (conferma #05).
16. **PITR attivo** (retention ~7 giorni). **Sicurezza destroy**: **test** senza deletion protection né snapshot finale (destroy libero);
    **prod** con **deletion protection + snapshot finale** (dettaglio in topic K).

### Edge & DNS (topic F)
17. **S3 privato (OAC) + CloudFront** ×2 app ×env; fallback SPA (403/404 → `index.html`). **Cert CloudFront in `us-east-1`**
    (requisito AWS), cert regionali (API GW) in `eu-west-1`. **API Gateway HTTP API + custom Lambda authorizer**. Record Route53 alias.

### Auth infra (topic G)
18. **Cognito user pool per env** (dev/test/prod). **3 Lambda** (auth BFF, pre-token-gen, authorizer) **in VPC** (DB via RDS Proxy).
    Per la connettività con **no-NAT**: **VPC endpoints** (Cognito-IDP + Secrets Manager), ~$14/mese/env.
    **MFA TOTP opzionale** (opt-in da profilo → [usecases/01-auth-registrazione](usecases/01-auth-registrazione.md)).

### Messaging (topic H)
19. **Bus EventBridge dedicato** + **una coda SQS per servizio** per la purge (`tenant.offboarded`): il servizio consuma e purga il proprio schema.
19bis. **Coda SQS webhook Paddle + DLQ** (#09 D19): la **Lambda di ingest** webhook verifica firma/dedup e accoda; un
    **consumer** (capability billing core, #04) processa idempotente; **DLQ + allarme** (#08). Distinta dalla coda purge.

### Secrets/config (topic I)
20. **SSM Parameter Store** (config/secret app) + **Secrets Manager** (credenziali DB), per env; lettura a runtime.

### Encryption ovunque (baseline di sicurezza esplicita, 2026-06-20)
20bis. **Encryption at rest su TUTTE le risorse stateful**: Aurora (+ snapshot/PITR), **S3** (SSE — bucket export #13 D,
    audit #08, state Terraform, asset), **EBS**, **SQS**, **CloudWatch Logs**, **ECR**; **Secrets Manager** (cifrato di
    default) e **SSM SecureString** per i parametri sensibili. Chiavi: **KMS** (chiavi gestite AWS di default; valutare
    CMK dove serve). **Encryption in transit ovunque (TLS)**: **HTTPS** su CloudFront/API Gateway (**HSTS** sul dominio
    `.app`, #12), **cookie `Secure`** (#02), **TLS verso il DB** (Aurora/RDS Proxy), TLS su SES/SQS/chiamate interne.
    Principio: *nessun dato personale in chiaro, né a riposo né in transito* — rafforza #13 (riduce l'obbligo di notifica
    breach, art. 34.3) e gli invarianti di sicurezza.

21. **WAF** (firewall applicativo) **rimandato** → evoluzione **E6**.

### Modulo `microsaas_app` (topic J — direttiva #3)
22. **Un'istanza del modulo = un microservizio**. Crea: **ECR** repo, **ECS service/task** (Fargate + Cloud Map),
    **route API** `/api/<app_id>/v1/*`, **ruolo DB + schema `app_<app_id>`**, **coda SQS** (purge), **SSM/Secrets** (cred DB),
    **log group**. Aggiungere/eliminare un backend = aggiungere/togliere un blocco `module` + `apply` (o `destroy -target`).
23. **DB bootstrap**: **Terraform** crea ruolo+grant+**schema vuoto**; **Flyway/CI** crea le tabelle (coerente #05).

### Lifecycle apply/destroy (topic K — direttiva #2)
24. **`terraform apply`/`destroy` per ambiente**. **Destroy safety**: **test** = `force_destroy` su S3/log, niente
    snapshot né protezione (teardown pulito e libero); **prod** = **deletion protection** + **snapshot finale DB** +
    bucket da svuotare esplicitamente. **Teardown completo** ("spegnere l'iniziativa"): ordine `envs` → `global` →
    bucket di state per ultimo, così non restano risorse che generano costi.

### Wrapper script (DX — utente senza esperienza Terraform)
25. **Script wrapper super-semplici e super-documentati** in `infra/scripts/`, che incapsulano Terraform: nell'uso
    quotidiano **non si lanciano comandi `terraform` grezzi**. Requisiti:
    - **Nomi espliciti** e un solo argomento ambiente, es.: `bootstrap` (crea lo state, una tantum), `plan <env>`,
      `up <env>` (apply), `down <env>` (destroy), `output <env>`, `check` (fmt+validate),
      `service-add <app_id>` / `service-remove <app_id>` (istanzia/rimuove il modulo `microsaas_app`),
      `test-start` / `test-stop` (scale 0↔1 dei servizi test, → backlog).
    - **Ogni script ha `--help`** (cosa fa, prerequisiti, esempi) + un **README quickstart** con "i 5 comandi che userai".
    - **Guardrail**: ambiente sempre esplicito; **prod richiede conferma digitata** (es. scrivere `prod`); **mai auto-approve
      su prod**; `down prod` chiede doppia conferma e rispetta deletion protection/snapshot (topic K).
    - Forma: **shell script** (massima trasparenza per un principiante) + README; commenti abbondanti.

### Email & protezione login (dai casi d'uso, 2026-06-16)
26. **Amazon SES** per le email transazionali (dominio `appgrove.app` verificato + **DKIM** su Route53), invio da
    `noreply@appgrove.app`; **Custom Message Lambda** per localizzare le email Cognito (EN/IT). **Throttling API Gateway**
    su `/api/auth/*`; lockout login affidato a Cognito (built-in). Advanced Security = evoluzione **E7**.
    Dettaglio flussi → [usecases/01-auth-registrazione](usecases/01-auth-registrazione.md).

## Questioni aperte
_Nessuna — #06 chiuso._

## Alternative valutate / scartate
- **AWS CDK (TypeScript)** — scartato a favore di Terraform (richiesta utente: script di provisioning/deprovisioning Terraform).

## Impatti su altre aree
- [04-services-backend](04-services-backend.md), [05-persistenza-dati](05-persistenza-dati.md), [07-devops-cicd](07-devops-cicd.md), [12-environments-config](12-environments-config.md), [_COSTI-AWS.md](_COSTI-AWS.md)
