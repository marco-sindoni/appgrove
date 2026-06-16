# Stima costi AWS (documento vivo)

**Principio**: ogni scelta deve puntare al **costo minimo compatibile con i requisiti**. Questa stima è
**aggiornata a ogni decisione** rilevante. Valori mensili **indicativi** (USD), ambienti `test` + `prod`
(`local` = $0; pool Cognito `dev` ~$0 in free tier).

**Assunzioni**: regione **`eu-west-1` (Irlanda)** — scelta cost-min (#06 topic A); traffico PoC basso;
1 task per servizio (no HA nel PoC). Ultimo aggiornamento: 2026-06-16 (dopo #06 topic A).

## Voci e stima

| Voce | prod /mese | test /mese | Note / leva di costo |
|---|---|---|---|
| Route53 hosted zone | $0.50 | — | una zona per `appgrove.app` (test è sottodominio) |
| Dominio `.app` | ~$1.2 | — | ~$14/anno |
| ACM (certificati) | $0 | $0 | gratis con CloudFront/API GW |
| Cognito | $0 | $0 | free tier 50k MAU; **niente pool per il locale** (dev 100% offline, #11) |
| **ECS Fargate** | ~$30 | ~$0 | 3 servizi (core+2 app) × ~$10 (0.25 vCPU/0.5GB). Prod on-demand 24/7; **test Spot + scale-to-0 → ~$0 da idle** |
| **Aurora Serverless v2** | ~$0 idle | ~$0 idle | ✅ scale-to-0 su **test e prod** (#06 E): ~$0 da idle + storage (~$1). Min>0 prod = evoluzione E4 (~$44) |
| **RDS Proxy** (solo Lambda) | ~$10–15 | ~$10–15 | **da tenere d'occhio**: non gratis; valutare se serve in `test` |
| **NAT Gateway** | **$0** | **$0** | ✅ deciso #06 B: niente NAT (subnet pubbliche + SG). Hardening = evoluzione E1 (~$32/mese) |
| Load balancer (ingress) | **$0** | **$0** | ✅ deciso #06 B: VPC Link + Cloud Map, no ALB. ALB = evoluzione E2 (~$16/mese) |
| State Terraform (S3+DynamoDB) | ~$0 | — | bucket S3 + tabella DynamoDB on-demand: pochi centesimi |
| VPC endpoints (Lambda) | ~$14 | ~$14 | #06 G: Cognito-IDP + Secrets Manager (~$7 l'uno) per Lambda in VPC senza NAT (cmq < NAT $32) |
| API Gateway HTTP | <$1 | <$1 | $1/M richieste |
| Lambda (auth, pre-token-gen, authorizer) | ~$0 | ~$0 | free tier |
| EventBridge + SQS (purge) | ~$0 | ~$0 | free tier / pochi eventi |
| CloudFront + S3 | ~$0–1 | ~$0–1 | **2 distribuzioni/env** (backoffice + console admin); free tier CloudFront 1TB/mese (primo anno) |
| Secrets Manager | ~$1–2 | ~$1–2 | $0.40/secret (credenziali DB) |
| SES (email) | ~$0 | ~$0 | ~$0.10/1000 email; verifica/reset/invito EN+IT (usecases/01) |
| SSM Parameter Store | $0 | $0 | parametri standard gratis |
| CloudWatch Logs | ~$1–5 | ~$1–5 | dipende dal volume log |

## Totale indicativo (stato attuale delle decisioni)
Anche con tutto a scale-to-zero, esiste un **floor always-on** per ambiente: **RDS Proxy (~$12)** + **VPC endpoints (~$14)** ≈ **~$26/mese/env**.

- **test ≈ $25–30/mese** — floor always-on (RDS Proxy + endpoints) + misc; Fargate/Aurora ~$0 da idle.
- **prod ≈ $55–70/mese** — Fargate ~$30 + floor ~$26 + misc (~$8: Route53/dominio, Secrets, CloudWatch, ecc.).
- **TOTALE ≈ $80–100/mese.**

⚠️ **Variabile critica = RDS Proxy**: per Aurora Serverless v2 ha un **minimo di fatturazione** (da verificare): se elevato,
i totali salgono sensibilmente. Era l'opzione **cost-min** rimandarlo (Lambda dirette) — vale la pena riconsiderare.

- **Non attento** (NAT+ALB ovunque + Aurora min ACU on + più task): **$250+/mese**.

## Leve di costo prioritarie
1. ✅ **NAT Gateway** — risolto (#06 B): niente NAT. Hardening = evoluzione E1.
2. ✅ **Load balancer** — risolto (#06 B): no ALB (Cloud Map). Scaling = evoluzione E2.
3. **Fargate sempre-on** — `test` con scaling a 0 task quando non si testa (scheduled/manuale).
4. **Aurora scale-to-0** — già deciso (#12) per `test`; valutarlo anche per `prod` (evoluzione E4).
5. **RDS Proxy** — confermare che serva (scelto per le Lambda); eventualmente solo in `prod`.

## Changelog decisioni → costi
- **#12** (env): Aurora Serverless v2 **scale-to-0** (test) → abbatte il costo DB idle. 3 tier; local $0.
- **#12** (domini): un solo dominio `appgrove.app` (test sottodominio) → niente seconda registrazione.
- **#05/#04**: **RDS Proxy** (Lambda) → nuova voce ~$10–15/env, da monitorare.
- **#04**: native GraalVM in prod → task Fargate piccoli (0.25 vCPU/0.5GB) → costo Fargate minimo. EventBridge/SQS per purge → ~$0.
- **#03**: **app admin separata** (`admin.appgrove.app`) → seconda distribuzione S3+CloudFront per env (~$0, free tier); coperta dai wildcard ACM esistenti (nessun cert nuovo).
- **#06 A**: regione **eu-west-1** (cost-min); state Terraform su S3+DynamoDB (~$0).
- **#06 B**: **niente NAT** e **no ALB** (Cloud Map) → networking ~$0; entrambi tracciati come evoluzioni E1/E2 in [_EVOLUZIONI-DEVOPS.md](_EVOLUZIONI-DEVOPS.md).
- **#06 C**: **Fargate Spot in test** (~−70%) + **test scale-to-0** da idle → costo Fargate test ~$0; prod on-demand (1 task/servizio, 0.25 vCPU/0.5GB).
- **#06 E**: Aurora **scale-to-0 anche su prod** → DB ~$0 da idle (era ~$44 prod); **RDS Proxy** confermato test+prod (~$10-15/env, voce da monitorare). ECR ~$0.
- **#06 F/G/H/I**: **VPC endpoints** per Lambda (~$14/env, conseguenza no-NAT); EventBridge/SQS, SSM/Secrets, S3+CloudFront già contati; **WAF** rimandato (E6).
- **usecases/01 (auth)**: **SES** per email EN/IT (~$0); throttling API GW + lockout Cognito (gratis); **2FA TOTP** opzionale (gratis); Advanced Security = evoluzione E7.
