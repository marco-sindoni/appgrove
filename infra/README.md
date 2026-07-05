# infra — Terraform (HCL)

Infrastruttura come codice di appgrove, a **costo minimo** (→ [docs/_COSTI-AWS.md](../docs/_COSTI-AWS.md)).
Decisioni: [docs/06-infra-iac.md](../docs/06-infra-iac.md) · use case fondativo:
[UC 0003](../docs/usecases/02-devops-infra/0003-fondamenta-terraform.md).

> **Stato**: il codice è completo e validato, ma **nessuna risorsa AWS è stata creata**
> (attivazione graduale degli ambienti, #12: prima si sviluppa in locale, il cloud si accende
> a sviluppo finito). Quando si è pronti: `./infra/scripts/first-run`.

## Struttura

```
infra/
├── bootstrap/    # state backend (bucket S3 + lock DynamoDB) — one-time, state locale
├── global/       # risorse condivise: zona Route53, certificati ACM, ruoli OIDC CI
├── envs/
│   ├── test/     # ambiente test  (state separato: envs/test/terraform.tfstate)
│   └── prod/     # ambiente prod  (state separato: envs/prod/terraform.tfstate)
├── modules/
│   ├── env_baseline/     # fondamenta di un ambiente: VPC no-NAT, endpoint VPC,
│   │                     # bucket export GDPR, baseline SSM
│   └── platform_shared/  # risorse condivise di un ambiente (UC 0055): Aurora SsV2
│                         # + RDS Proxy, cluster ECS, API GW + VPC Link + Cloud Map,
│                         # bus EventBridge, 2 CloudFront SPA (app./admin.)
└── scripts/      # wrapper: NON si lanciano comandi terraform grezzi (#06 25)
```

Regione unica: **eu-west-1** (#06 6). State remoto su S3 + lock DynamoDB, uno per stack:
`destroy` su test non può toccare prod (#06 5).

Il modulo **`microsaas_app`** (un'istanza = un microservizio: ECR, ECS, route API, schema DB,
coda purge, …) arriva con **UC 0004** e si innesta su queste fondamenta. Non scrivere infra
per-servizio a mano.

## I comandi che userai (tutti con `--help`)

| Comando | Cosa fa |
|---|---|
| `./infra/scripts/check` | valida tutto il codice **senza AWS** (fmt, validate, tflint, checkov) |
| `./infra/scripts/first-run` | **prima accensione** guidata: bootstrap → global → test |
| `./infra/scripts/plan <env>` | anteprima modifiche (`test` \| `prod` \| `global`), non applica |
| `./infra/scripts/up <env>` | applica uno stack (prod: conferma digitata, mai auto-approve) |
| `./infra/scripts/down <env>` | distrugge uno stack (prod: doppia conferma digitata) |
| `./infra/scripts/output <env>` | mostra gli output (name server, ARN, nomi bucket, …) |

`bootstrap` esiste anche da solo, ma normalmente lo invoca `first-run`.

## Prima accensione — cosa sapere

1. **Credenziali**: AWS CLI autenticata (`aws sts get-caller-identity`).
2. **Dominio**: `appgrove.app` deve essere registrato. Se è registrato **su Route53**, nell'account
   esiste già una hosted zone: `first-run` la rileva e la **importa** (niente zone doppie). Se la
   zona viene creata ex novo, **puntare i name server del registrar** alla zona
   (`./infra/scripts/output global name_servers`) — finché gli NS non sono delegati, la validazione
   DNS dei certificati ACM resta in attesa.
3. **Costi** una volta acceso test: floor always-on ~26 $/mese (endpoint VPC ~14 + RDS Proxy ~12);
   il resto ~0 da idle (Aurora scale-to-0, Fargate senza task, CloudFront/API GW a consumo).
   Spegnere: `./infra/scripts/down test`.
4. **Prod** non viene toccato da `first-run`: si accende esplicitamente (`up prod`, conferma digitata).

## Sicurezze di destroy / teardown completo (#06 24)

- **test**: destroy libero e pulito (bucket con `force_destroy`, niente protezioni).
- **prod**: mai auto-approve, doppia conferma digitata; i bucket **non** si svuotano da soli;
  le protezioni sui dati stateful (deletion protection + snapshot Aurora) nascono con il cluster.
- **Spegnere l'iniziativa** (ordine obbligato): `down test` → `down prod` → `down global` → per
  **ultimo** lo state. Bucket di state e tabella di lock sono protetti da `prevent_destroy`:
  l'eliminazione finale è volutamente manuale —

  ```bash
  # SOLO come ultimissimo passo del teardown completo:
  aws s3 rb "s3://appgrove-tfstate-<account-id>" --force
  aws dynamodb delete-table --table-name appgrove-tfstate-lock
  ```

## Test (#10 H)

```bash
./infra/scripts/check     # ciò che esegue anche ./run-tests.sh infra
```

`fmt -check` + `validate` (init senza backend: niente credenziali) su tutte le root, più
**tflint** e **checkov** se installati (soppressioni inline documentate nel codice, es. subnet
pubbliche by-design → evoluzione E1). `terraform plan` e Infracost girano in CI (**UC 0005**).

Strumenti: `brew install hashicorp/tap/terraform` e `brew install checkov`; tflint non è più su
Homebrew — binario dalle [release ufficiali](https://github.com/terraform-linters/tflint/releases)
(in CI ci pensa l'action dedicata, UC 0005).

## Scelte chiave (dettagli nei commenti del codice)

- **VPC senza NAT Gateway** (~32 $/mese risparmiati per ambiente): subnet pubbliche + security
  group stretti; uscita via internet gateway; Cognito/Secrets Manager via **endpoint VPC**
  (~14 $/mese/env). Hardening (subnet private + NAT) = evoluzione **E1**.
- **Encryption ovunque** (#06 §20bis): SSE su tutti i bucket, TLS-only enforcement, chiavi KMS
  gestite AWS (CMK solo se servirà).
- **Bucket export GDPR** per ambiente (UC 0032): privato, cifrato, auto-cancellazione oggetti a
  7 giorni; nome pubblicato su SSM (`/appgrove/<env>/gdpr/export-bucket`).
- **CI senza chiavi AWS** (#07 25): ruoli OIDC `appgrove-github-actions-{test,prod}`; prod
  assumibile **solo da tag**.
- **Convenzione SSM**: `/appgrove/<env>/<area>/<chiave>`; i servizi leggono a runtime, la CI mai
  (#07 26). Credenziali master DB in Secrets Manager, gestite da RDS (`manage_master_user_password`).
- **Risorse condivise per-ambiente** (UC 0055, modulo `platform_shared`): Aurora Serverless v2
  **scale-to-0** (min 0 ACU, auto-pause 5', max 2 ACU, PITR 7gg; prod protetta da deletion
  protection + snapshot finale); **RDS Proxy solo per le Lambda auth** (#05 dec.3 — i task Fargate
  e Flyway si connettono diretti); ingress **API GW HTTP + VPC Link + Cloud Map** (no ALB, E2);
  bus EventBridge per la purge; 2 distribuzioni **CloudFront + S3 privato (OAC)** con fallback SPA.
