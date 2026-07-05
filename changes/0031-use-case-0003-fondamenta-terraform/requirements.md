# Change 0031: Fondamenta Terraform (state, global, VPC no-NAT, KMS/secrets, wrapper script)

**Branch**: `change/0031-use-case-0003-fondamenta-terraform`
**Aree**: infra
**Data**: 2026-07-05
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/02-devops-infra/0003-fondamenta-terraform.md](../../docs/usecases/02-devops-infra/0003-fondamenta-terraform.md)
**Tocca dati personali?**: No — solo risorse infrastrutturali come codice; nessun dato applicativo/personale
(la residenza UE dei dati è una proprietà dell'infra: tutte le risorse in `eu-west-1`, cert CloudFront in
`us-east-1` come richiesto da AWS).

## Problema / Obiettivo

Porre le **fondamenta Terraform** dell'infrastruttura AWS a costo minimo, interamente **come codice**:
backend di state, stack `global` condiviso, ambienti `test`/`prod` (rete, cifratura, secrets), script
wrapper per chi non conosce Terraform.

**Decisione di questa change (opzione A)**: si scrive e si valida **tutto il codice**, ma **non si crea
alcuna risorsa AWS** — l'attivazione reale degli ambienti è volutamente rimandata (principio di
attivazione graduale, #12). La "prima accensione" diventa un comando dedicato (`first-run`), da lanciare
quando si deciderà di accendere l'ambiente di test (al più tardi con UC 0005, la pipeline che applica
Terraform). Costo AWS della change: **zero**.

## Scope

Tutto sotto `infra/` (oggi contiene solo un README), regione **eu-west-1**:

1. **Struttura**: `infra/bootstrap/` (state backend), `infra/global/`, `infra/envs/test/`,
   `infra/envs/prod/`, `infra/modules/` (predisposta, il modulo `microsaas_app` è di UC 0004),
   `infra/scripts/`.
2. **Bootstrap one-time** (state locale, mai in CI): bucket S3 di state (cifrato, versionato, accesso
   bloccato) + tabella DynamoDB di lock. Gli altri stack usano backend S3 con chiavi distinte
   (`global`, `envs/test`, `envs/prod` → state separati; `destroy` su test non tocca prod).
3. **Stack `global`**: zona Route53 `appgrove.app`; certificati ACM — `*.appgrove.app` + apex in
   `us-east-1` (CloudFront) e regionali in `eu-west-1` (API Gateway), incluso `*.test.appgrove.app`;
   **ruoli OIDC** `github-actions-test`/`github-actions-prod` con least privilege e trust policy vincolata
   al repo (prod: assunzione **solo da tag** `refs/tags/*`).
4. **Per ambiente** (`envs/test`, `envs/prod`): **VPC** con subnet pubbliche + security group stretti,
   **niente NAT Gateway** (uscita via internet gateway; hardening = evoluzione E1); **VPC endpoints**
   Cognito-IDP e Secrets Manager; baseline **KMS/encryption** at rest (S3, SQS, log, EBS…) e TLS in
   transito; baseline **SSM Parameter Store** (config/secret app) + **Secrets Manager** (credenziali DB).
5. **Bucket S3 export GDPR per ambiente** (punto differito di proprietà di UC 0003, richiesto da UC 0032):
   privato, cifrato (SSE), **lifecycle di auto-cancellazione a 7 giorni**, accesso solo dal ruolo dei
   servizi (download utente via URL firmati generati dal core); nome/endpoint esposti come parametri SSM
   con le chiavi `appgrove.s3.*` definite nella change 0028.
6. **Script wrapper** `infra/scripts/`: `bootstrap`, `plan <env>`, `up <env>`, `down <env>`,
   `output <env>`, `check`, più **`first-run`** (orchestrazione prima accensione: bootstrap → `up global`
   → `up test`, con verifiche). Ogni script con `--help`; README quickstart ("i comandi che userai").
   **Guardrail**: ambiente sempre esplicito; prod richiede conferma digitata; mai auto-approve su prod;
   `down prod` doppia conferma + rispetto deletion protection/snapshot; teardown completo documentato
   (ordine `envs` → `global` → bucket di state per ultimo).
7. **Tracciamento**: nota in UC 0003 ("Punti aperti") che l'attivazione reale è rimandata e si esegue con
   `first-run` (necessaria al più tardi con UC 0005); `_INDEX.md` 0003 → ✅; verifica/adeguamento della
   sezione infra di `run-tests.sh` (fmt+validate su tutte le root, senza credenziali AWS).

## Fuori scope

- **Modulo `microsaas_app`** e ogni risorsa per-servizio (ECR/ECS/route/schema DB/coda purge) → UC 0004.
- **Pipeline CI/CD** che esegue Terraform, Infracost e i check in PR → UC 0005.
- **Allarmi/dashboard** → UC 0006. Cognito/SES/API Gateway/Aurora → use case dedicati.
- **Esecuzione reale** di bootstrap/`up` su AWS: nessuna risorsa viene creata in questa change.

## Criteri di accettazione

- [ ] `terraform fmt -check` + `terraform validate` verdi su tutte le root (`bootstrap`, `global`,
      `envs/test`, `envs/prod`) **senza credenziali AWS** (`init -backend=false`); `./run-tests.sh infra` verde.
- [ ] tflint + checkov verdi (installati via Homebrew), con soppressioni **documentate** (es. subnet
      pubbliche → link a evoluzione E1).
- [ ] Script wrapper eseguibili con `--help` funzionante e guardrail prod attivi; `first-run` presente e
      documentato nel README; nessuna risorsa AWS creata (verificabile: nessun apply eseguito).
- [ ] Bucket export GDPR per env definito con SSE + lifecycle 7 giorni + parametri SSM `appgrove.s3.*`.
- [ ] Nota di attivazione differita tracciata in UC 0003; `_INDEX.md` → ✅ nel commit di chiusura.

## Invarianti appgrove toccati

- **Modulo Terraform `microsaas_app`**: questa change predispone `infra/modules/` e le fondamenta (VPC,
  KMS, SSM) su cui il modulo (UC 0004) si innesta — nessuna infra per-app bespoke viene creata qui.
- **Encryption ovunque** (#06 §20bis): tutte le risorse stateful definite (state bucket, export bucket,
  parametri) nascono cifrate; TLS in transito.
- Tenant ID dal JWT, filtro row-level, logging strutturato: **N/A** (nessun codice applicativo).

## Requisiti di test

- Area infra: `terraform fmt -check` + `terraform init -backend=false` + `terraform validate` per ogni
  root; **tflint** e **checkov** localmente (strumenti da installare: `terraform`, `tflint`, `checkov` —
  oggi assenti sulla macchina). `terraform plan`/Infracost restano per la CI (UC 0005): richiedono
  credenziali/state.
- Script: smoke test dei `--help` e dei guardrail (rifiuto senza env esplicito, richiesta conferma prod)
  senza toccare AWS.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | Sì — l'infra espone i parametri SSM `appgrove.s3.*` con le chiavi già lette dai servizi (change 0028); nessun cambiamento lato servizi |
| Version bump | Nessuno |
