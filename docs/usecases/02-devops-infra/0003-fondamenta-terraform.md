# UC 0003 — Terraform foundation (state, global Route53/ACM/OIDC, VPC no-NAT, KMS/secrets baseline)

**Area**: 02-devops-infra · **Fase**: 1 · **Stato**: 🟢 deciso
**Dipendenze**: — (base di tutta l'infra; prerequisito di UC 0004/0005/0006)
**Fonte decisioni**: #06 (IaC Terraform), #12 (tier/domini/secrets)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [06-infra-iac](../../06-infra-iac.md), [12-environments-config](../../12-environments-config.md), [_COSTI-AWS](../../_COSTI-AWS.md), [_EVOLUZIONI-DEVOPS](../../_EVOLUZIONI-DEVOPS.md)

## 1. Obiettivo / Scope
Porre le **fondamenta Terraform** dell'infrastruttura AWS cost-min: backend di state, stack `global` condiviso, networking
VPC **no-NAT**, baseline di sicurezza (KMS/encryption, secrets), DNS/cert, e i **wrapper script** `infra/scripts/`.
**Incluso**: state **S3 + lock DynamoDB** (bootstrap one-time); struttura `envs/test`+`envs/prod`+`global`+`modules/`;
**Route53** (zona `appgrove.app`) + **ACM** (cert wildcard, CloudFront in `us-east-1`); **OIDC IAM role per env** (CI);
**VPC** subnet pubbliche **senza NAT** + **VPC endpoints** (Cognito-IDP, Secrets Manager); **encryption ovunque** (#06 §20bis);
SSM + Secrets Manager baseline; regione **eu-west-1**.
**Escluso**: il modulo `microsaas_app` (UC [0004](0004-modulo-microsaas-app.md)), la pipeline che esegue Terraform (UC [0005](0005-pipeline-cicd.md)),
gli allarmi/dashboard (UC [0006](0006-osservabilita-base.md)).

## 2. Attori & ruoli
- **Developer/Platform Engineer**: esegue bootstrap e `up`/`down` via wrapper.
- **CI (GitHub Actions)**: assume i ruoli OIDC per applicare (UC 0005).
- **Sistema AWS**: risorse provisionate.

## 3. Precondizioni
- Account AWS; repo su GitHub; dominio `appgrove.app` su Route53; Terraform installato; nessuna dipendenza da AWS in locale (#11).

## 4. Flusso principale
1. **Bootstrap one-time** (manuale, non in CI): crea bucket S3 di state + tabella DynamoDB di lock (#06 4, #07 5).
2. **Stack `global`**: zona Route53 `appgrove.app`, cert ACM (`*.appgrove.app` + apex in `us-east-1` per CloudFront;
   regionali in `eu-west-1`), **ruoli OIDC** `github-actions-test`/`-prod` con least-privilege e trust policy (prod = solo da tag) (#07 25).
3. **Per env** (`envs/test`, `envs/prod`): **VPC** subnet pubbliche + SG stretti, **niente NAT** (uscita via internet gateway),
   **VPC endpoints** (Cognito-IDP, Secrets Manager) (#06 7/18); baseline **KMS/encryption** at rest e TLS in transit (#06 §20bis).
4. **Secrets/config**: SSM Parameter Store (config/secret app) + Secrets Manager (credenziali DB), per env (#06 20, #12 7).
5. Esposizione tramite **wrapper script** `infra/scripts/` (`bootstrap`, `plan <env>`, `up <env>`, `down <env>`, `output`, `check`) (#06 25).

## 5. Flussi alternativi / edge / errori
- **`down prod`**: doppia conferma digitata + rispetto deletion protection/snapshot (#06 K, §25 guardrail).
- **Teardown completo** ("spegnere l'iniziativa"): ordine `envs` → `global` → bucket di state per ultimo (#06 24).
- **No-NAT + connettività**: Paddle/Cognito raggiungibili via IGW; servizi interni via VPC endpoints; hardening a subnet
  private + NAT = evoluzione **E1**.
- **checkov segnala subnet pubbliche**: soppressione inline documentata con link a E1 (#10 28).

## 6. Risorse & runbook
**Struttura**: `infra/envs/{test,prod}/`, `infra/global/`, `infra/modules/` (incl. `microsaas_app` in UC 0004), `infra/scripts/`.
**Risorse create**: state backend (S3+DynamoDB), Route53 zone, ACM cert, OIDC roles, VPC+subnet+SG+endpoints, KMS keys,
SSM/Secrets baseline. **Runbook**: `infra/scripts/bootstrap` (una tantum) → `plan test` → `up test`; prod dietro gate (UC 0005).
**Rollback/teardown**: `down <env>` con safety #06 K.

## 7. Dati toccati
Nessun dato applicativo/personale: risorse infra. I bucket/log/DB sono **cifrati** (KMS, #06 §20bis). Manifest GDPR N/A
(la residency UE è una proprietà: tutte le risorse in eu-west-1/eu-central-1, #13 I51).

## 8. Permessi & gate
- **Invarianti**: predispone il **modulo `microsaas_app`** come unico mattone (UC 0004); il logging strutturato e l'isolamento
  si attivano nei moduli/servizi. **Least privilege** sui ruoli OIDC e (più avanti) sui ruoli DB per-servizio (#05 11).
- **Guardrail**: prod sempre esplicito, mai auto-approve, conferma digitata (#06 §25).

## 9. Requisiti di test
- **Infra testing** (#10 H): `terraform fmt -check` + `validate` + **tflint** + **checkov** (con soppressioni documentate);
  `terraform plan` in PR; **Infracost** sul delta di costo (#10 30). Il `terraform test` sul modulo è in UC 0004.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #06 4/5/6/7/18/20/§20bis/24/25, #07 5/25, #12 9/10/11/12, #13 I51.
- **DoD**:
  1. State remoto + lock attivi; `global`/`envs` separati; regione eu-west-1.
  2. VPC no-NAT con VPC endpoints; encryption at rest+in transit ovunque.
  3. Route53+ACM+OIDC roles creati; wrapper script con `--help` + README.
  4. `plan`/`validate`/checkov/Infracost girano in CI (UC 0005); teardown completo possibile con le safety.

## Punti aperti / decisioni differite

- ✅ **Bucket S3 export GDPR** _(tracciato dalla change `0028-use-case-0032-…`, **risolto dalla change
  `0031-use-case-0003-…`**)_. Implementato nel modulo `env_baseline`: bucket per env privato, SSE,
  lifecycle 7 giorni, TLS-only; nome pubblicato su SSM (`/appgrove/<env>/gdpr/export-bucket`, letto dal
  core come `appgrove.gdpr.export-bucket`). In locale resta MinIO (stack dev).
- **Attivazione reale degli ambienti volutamente rimandata** _(tracciato dalla change
  `0031-use-case-0003-…`)_. La change ha scritto e validato TUTTO il codice (bootstrap, global,
  envs, wrapper) **senza creare risorse AWS** (attivazione graduale, #12: il cloud si accende a
  sviluppo finito). La prima accensione è un comando unico e guidato: **`infra/scripts/first-run`**
  (bootstrap → `up global` con import della zona se esiste → `up test`). Diventa necessaria **al più
  tardi con UC 0005** (la pipeline CI/CD applica Terraform su ambienti reali) — la voce "state
  remoto + lock attivi" della DoD si completa in quel momento. Prerequisito da ricordare: delega dei
  name server del registrar alla hosted zone, altrimenti la validazione ACM resta in attesa.
- **tflint non più installabile via Homebrew** _(rilevato dalla change `0031-use-case-0003-…`)_: la
  formula è sparita dall'indice; in locale `scripts/check` lo salta con avviso (fmt/validate/checkov
  restano attivi). In CI (UC 0005) usare l'action ufficiale `setup-tflint`; in locale, binario dalle
  release GitHub (istruzioni in `infra/README.md`).
