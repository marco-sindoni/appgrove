# Stima costi AWS (documento vivo)

**Principio**: ogni scelta deve puntare al **costo minimo compatibile con i requisiti**. Questa stima è
**aggiornata a ogni decisione** rilevante. Valori mensili **indicativi** (USD), ambienti `test` + `prod`
(`local` = $0; pool Cognito `dev` ~$0 in free tier).

**Assunzioni**: regione da decidere in #06 (probabile `eu-south-1` Milano o `eu-west-1` Irlanda — Irlanda
più economica); traffico PoC basso; 1 task per servizio (no HA nel PoC). Ultimo aggiornamento: 2026-06-14
(dopo #04).

## Voci e stima

| Voce | prod /mese | test /mese | Note / leva di costo |
|---|---|---|---|
| Route53 hosted zone | $0.50 | — | una zona per `appgrove.app` (test è sottodominio) |
| Dominio `.app` | ~$1.2 | — | ~$14/anno |
| ACM (certificati) | $0 | $0 | gratis con CloudFront/API GW |
| Cognito | $0 | $0 | free tier 50k MAU |
| **ECS Fargate** | ~$30 | ~$0–30 | **leva grossa**: 3 servizi (core+2 app) × ~$10 (0.25 vCPU/0.5GB, native). Test scalabile a 0 task da idle |
| **Aurora Serverless v2** | ~$0–44 | ~$0 | **leva grossa**: scale-to-0 → ~$0 idle + storage (~$1). Senza scale-to-0, min 0.5 ACU ≈ $44 |
| **RDS Proxy** (solo Lambda) | ~$10–15 | ~$10–15 | **da tenere d'occhio**: non gratis; valutare se serve in `test` |
| **NAT Gateway** | ~$32 | ~$32 | **leva grossa nascosta**: evitabile (subnet pubbliche + SG, o VPC endpoints). Decisione in #06 |
| API Gateway HTTP | <$1 | <$1 | $1/M richieste |
| Lambda (auth, pre-token-gen, authorizer) | ~$0 | ~$0 | free tier |
| EventBridge + SQS (purge) | ~$0 | ~$0 | free tier / pochi eventi |
| CloudFront + S3 | ~$0–1 | ~$0–1 | **2 distribuzioni/env** (backoffice + console admin); free tier CloudFront 1TB/mese (primo anno) |
| Secrets Manager | ~$1–2 | ~$1–2 | $0.40/secret (credenziali DB) |
| SSM Parameter Store | $0 | $0 | parametri standard gratis |
| CloudWatch Logs | ~$1–5 | ~$1–5 | dipende dal volume log |

## Totale indicativo
- **Frugale** (Aurora scale-to-0, **niente NAT**, 1 task/servizio, test a 0 task da idle): **prod ~$45–65/mese**, **test ~$15/mese**.
- **Non attento** (NAT in entrambi + Aurora min ACU sempre on + RDS Proxy ovunque + più task): **$200+/mese**.

## Leve di costo prioritarie (da decidere in #06)
1. **NAT Gateway (~$32/mese × env)** — il singolo costo evitabile più alto. Preferire subnet pubbliche con
   security group restrittivi o VPC endpoints mirati. ⚠️ Decisione aperta.
2. **Fargate sempre-on** — `test` con scaling a 0 task quando non si testa (scheduled/manuale).
3. **Aurora scale-to-0** — già deciso (#12) per `test`; valutarlo anche per `prod` nel PoC.
4. **RDS Proxy** — confermare che serva (scelto per le Lambda); eventualmente solo in `prod`.

## Changelog decisioni → costi
- **#12** (env): Aurora Serverless v2 **scale-to-0** (test) → abbatte il costo DB idle. 3 tier; local $0.
- **#12** (domini): un solo dominio `appgrove.app` (test sottodominio) → niente seconda registrazione.
- **#05/#04**: **RDS Proxy** (Lambda) → nuova voce ~$10–15/env, da monitorare.
- **#04**: native GraalVM in prod → task Fargate piccoli (0.25 vCPU/0.5GB) → costo Fargate minimo. EventBridge/SQS per purge → ~$0.
- **#03**: **app admin separata** (`admin.appgrove.app`) → seconda distribuzione S3+CloudFront per env (~$0, free tier); coperta dai wildcard ACM esistenti (nessun cert nuovo).
