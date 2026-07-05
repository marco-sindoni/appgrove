# Stima costi AWS (documento vivo)

**Principio**: ogni scelta deve puntare al **costo minimo compatibile con i requisiti**. Questa stima è
**aggiornata a ogni decisione** rilevante. Valori mensili **indicativi** (USD), ambienti `test` + `prod`
(`local` = $0; pool Cognito `dev` ~$0 in free tier).

**Assunzioni**: regione **`eu-west-1` (Irlanda)** — scelta cost-min (#06 topic A); traffico PoC basso;
1 task per servizio (no HA nel PoC). Ultimo aggiornamento: 2026-06-21 (dopo #08/#10/#13/#09/#14).

## Voci e stima

| Voce | prod /mese | test /mese | Note / leva di costo |
|---|---|---|---|
| Route53 hosted zone | $0.50 | — | una zona per `appgrove.app` (test è sottodominio) |
| Dominio `.app` | ~$1.2 | — | ~$14/anno |
| ACM (certificati) | $0 | $0 | gratis con CloudFront/API GW |
| Cognito | $0 | $0 | free tier 50k MAU; **niente pool per il locale** (dev 100% offline, #11) |
| **ECS Fargate** | ~$30 | ~$0 | 3 servizi (core+2 app) × ~$10 (0.25 vCPU/0.5GB). Prod on-demand 24/7; **test Spot + scale-to-0 → ~$0 da idle** |
| **Aurora Serverless v2** | ~$0 idle | ~$0 idle | ✅ scale-to-0 su **test e prod** (#06 E): ~$0 da idle + storage (~$1). Min>0 prod = evoluzione E4 (~$44) |
| **RDS Proxy** (solo Lambda) | ~$10–15 | ~$10–15 | ✅ deciso #06 15 (test+prod), implementato in UC 0055; **minimo di fatturazione da verificare alla prima accensione** |
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
| CI/CD (GitHub Actions) | ~$0 | ~$0 | #07 A: free tier 2.000 min/mese (repo privato); OIDC (no costi); native GraalVM solo on-demand (`[graal]`/release) |
| Observability (#08) | ~$2–6 | ~$0 | Log JSON CloudWatch (retention test 7gg/prod 30gg), metriche tecniche native (gratis) + business via EMF (no `PutMetricData`), dashboard ≤3 gratuiti, allarmi/SNS, canary EventBridge+Lambda (eu-central-1), errori FE→CW. **Tracce SPENTE (C) = $0**. Archivio audit S3+Glacier ~centesimi |

## Totale indicativo (stato attuale delle decisioni)
Anche con tutto a scale-to-zero, esiste un **floor always-on** per ambiente: **RDS Proxy (~$12)** + **VPC endpoints (~$14)** ≈ **~$26/mese/env**.

- **test ≈ $25–30/mese** — floor always-on (RDS Proxy + endpoints) + misc; Fargate/Aurora ~$0 da idle.
- **prod ≈ $55–70/mese** — Fargate ~$30 + floor ~$26 + misc (~$8: Route53/dominio, Secrets, CloudWatch, ecc.).
- **TOTALE AWS ≈ $80–100/mese.** (Il budget AWS $100, #08, è **solo-AWS**.)
- **Costi ricorrenti NON-AWS** (fuori dal budget AWS): **Plausible Cloud €9/mese** (analytics EU, #13); **fee Paddle** =
  revenue-based ~5%+$0.50/transazione (#09 K, non un costo fisso); **domiciliazione/virtual office** per la sede ditta
  individuale (#14 D, poche centinaia €/anno, prerequisito business).

⚠️ **Variabile critica = RDS Proxy**: per Aurora Serverless v2 ha un **minimo di fatturazione** (da verificare): se elevato,
i totali salgono sensibilmente. Era l'opzione **cost-min** rimandarlo (Lambda dirette) — vale la pena riconsiderare.

- **Non attento** (NAT+ALB ovunque + Aurora min ACU on + più task): **$250+/mese**.

## Leve di costo prioritarie
1. ✅ **NAT Gateway** — risolto (#06 B): niente NAT. Hardening = evoluzione E1.
2. ✅ **Load balancer** — risolto (#06 B): no ALB (Cloud Map). Scaling = evoluzione E2.
3. **Fargate sempre-on** — `test` con scaling a 0 task quando non si testa (scheduled/manuale).
4. **Aurora scale-to-0** — già deciso (#12) per `test`; valutarlo anche per `prod` (evoluzione E4).
5. **RDS Proxy** — ✅ deciso (#06 15: test+prod, solo Lambda) e implementato (UC 0055); resta da **verificare il
   minimo di fatturazione** alla prima accensione — se elevato, riconsiderare il proxy in `test`.

## Changelog decisioni → costi
- **#12** (env): Aurora Serverless v2 **scale-to-0** (test) → abbatte il costo DB idle. 3 tier; local $0.
- **#12** (domini): un solo dominio `appgrove.app` (test sottodominio) → niente seconda registrazione.
- **#05/#04**: **RDS Proxy** (Lambda) → nuova voce ~$10–15/env, da monitorare.
- **#04**: native GraalVM in prod → task Fargate piccoli (0.25 vCPU/0.5GB) → costo Fargate minimo. EventBridge/SQS per purge → ~$0.
- **#03**: **app admin separata** (`admin.appgrove.app`) → seconda distribuzione S3+CloudFront per env (~$0, free tier); coperta dai wildcard ACM esistenti (nessun cert nuovo).
- **UC 0055** (risorse condivise per-env, change 0032): nessuna voce nuova — conferma in codice di Aurora scale-to-0
  (min 0 ACU/auto-pause 5', max 2), RDS Proxy test+prod, cluster ECS (Spot in test), API GW+VPC Link+Cloud Map,
  bus EventBridge, 2 CloudFront (PriceClass_100). Il floor always-on ~$26/mese/env decorre dall'accensione dell'ambiente.
- **#06 A**: regione **eu-west-1** (cost-min); state Terraform su S3+DynamoDB (~$0).
- **#06 B**: **niente NAT** e **no ALB** (Cloud Map) → networking ~$0; entrambi tracciati come evoluzioni E1/E2 in [_EVOLUZIONI-DEVOPS.md](_EVOLUZIONI-DEVOPS.md).
- **#06 C**: **Fargate Spot in test** (~−70%) + **test scale-to-0** da idle → costo Fargate test ~$0; prod on-demand (1 task/servizio, 0.25 vCPU/0.5GB).
- **#06 E**: Aurora **scale-to-0 anche su prod** → DB ~$0 da idle (era ~$44 prod); **RDS Proxy** confermato test+prod (~$10-15/env, voce da monitorare). ECR ~$0.
- **#06 F/G/H/I**: **VPC endpoints** per Lambda (~$14/env, conseguenza no-NAT); EventBridge/SQS, SSM/Secrets, S3+CloudFront già contati; **WAF** rimandato (E6).
- **usecases/01 (auth)**: **SES** per email EN/IT (~$0); throttling API GW + lockout Cognito (gratis); **2FA TOTP** opzionale (gratis); Advanced Security = evoluzione E7.
- **#07 (DevOps/CI-CD)**: **GitHub Actions** (free tier, ~$0); **OIDC** verso AWS (no chiavi, no costi); **native GraalVM solo on-demand** (`[graal]`/release) → minuti CI minimi; **cron `test-stop`** giornaliero → Fargate test a 0 task fuori orario (rafforza il ~$0 di test). Nessuna nuova voce AWS rilevante.
- **#10 (Testing)**: tutto **a costo ~$0**: Testcontainers/Playwright girano sui **runner GitHub** (free tier) e sullo
  **stack locale** (#11), non su AWS; `terraform test`/Infracost/oasdiff/checkov sono tool CI gratuiti. **Nessuna risorsa
  AWS** accesa per testare (coerente con l'attivazione a fasi). Load testing (k6) rimandato (E16). Risparmio indiretto:
  **Infracost** intercetta in PR gli aumenti di costo infra prima del merge.
- **#13 (Compliance/Privacy)**: postura **EU-purista**. **Plausible Cloud** (analytics vetrina, EU-hosted) = **€9/mese**
  (unico costo SaaS non-AWS ricorrente, oltre alle fee Paddle); Cloudflare (free) scartato per residency US. **Ticketing
  nativo in-house** (no Jira) = ~$0 extra (righe DB + UI admin, infra già presente). Export ZIP su S3 + auto-delete 7gg
  = centesimi. Nessun altro impatto AWS rilevante.
- **#14 (Sito vetrina & legale)**: sito statico **Astro** su **S3+CloudFront+Route53+ACM** (~**$0–1**, free tier CloudFront)
  → **primo artefatto prod** acceso "a fette" (rollout statico-first), prima del backend. Nessun costo nuovo rilevante
  (rientra nella voce CloudFront+S3 già presente). Build su GitHub Actions (#07, free tier). **Ambiente di test** del sito
  statico = altra distribuzione S3+CloudFront (~$0) protetta da **basic auth via CloudFront Function** (Functions ~gratis a
  questi volumi) + `noindex`. **Prod** pubblico (Paddle) con gate `published` + `noindex` pre-lancio. Review = **locale**
  (astro dev/preview, $0).
- **#09 (Pagamenti, Paddle)**: **nessuna nuova voce AWS rilevante** (Lambda ingest webhook + SQS/ElasticMQ già contati,
  ~$0 al volume webhook). Il costo dei pagamenti è una **fee di revenue, non AWS**: **~5% + $0.50/transazione** (effettiva
  ~7% con FX). La quota fissa $0.50 pesa sulle app cheap/mensili (€5/mese → ~15%); **mitigata dall'annuale di default**
  (1 transazione/anno → ~5-6%) e dal **bundling** (leva futura, K50). Paddle MoR copre tasse/fatturazione (nessun costo
  tax compliance a nostro carico). Lo **stub locale** (#09 I) = $0 (sviluppo/test senza Paddle).
- **#08 (Observability)**: backend AWS-native (CloudWatch Logs/Metrics) + strumentazione **OTel/Micrometer neutra** (porta aperta a Grafana, E11). **Tracce strumentate ma spente** (c1) → $0. Metriche business via **EMF** (no chiamate `PutMetricData`); dimensioni a bassa cardinalità (`tenant_id` nei log, non come dimensione). Dashboard ≤3 (gratis). **Retention esplicita** (test 7gg/prod 30gg) per non accumulare; archivio **audit→S3+Glacier** (12 mesi, #13). Uptime via **canary EventBridge+Lambda in eu-central-1** (~$0). **AWS Budgets $100/mese** (soglie 75/90/100% + forecast). Costo prod ~$2–6/mese, test ~$0.
- **UC 0004 (modulo `microsaas_app`, change 0033)**: nessuna variazione del floor. Il modulo aggiunge risorse
  a costo ~$0 finché i task non girano: ECR (storage a consumo, lifecycle "ultime 10 immagini"), code SQS +
  regole EventBridge (~$0 ai volumi GDPR), log group (retention 7/30gg), segreti per-app (~$0.40/segreto/mese
  → ~$0.8/env con 2 servizi), Lambda `db-bootstrap` invocata solo agli apply (~$0, usa la **Data API** di
  Aurora, gratuita — niente ENI/VPC). Il costo Fargate era già stimato (3 servizi × ~$10 prod on-demand;
  test Spot+scale-to-0 ~$0 da idle): le istanze attive oggi sono **2** (platform, fatture) → prod ~$20/mese
  quando acceso, meno con **ARM64/Graviton** (~−20%, task definition già ARM64). `test-stop` (cron in UC 0005)
  rafforza il ~$0 di test.
