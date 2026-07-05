# Implementation Log — Change 0031: Fondamenta Terraform (UC 0003)

**Branch**: `change/0031-use-case-0003-fondamenta-terraform`
**Aree**: infra (+ `run-tests.sh`, `.gitignore`, docs di tracciamento)
**Completata**: 2026-07-05

## File modificati

| File | Azione |
|---|---|
| `infra/bootstrap/{main,variables,outputs}.tf` | Creato — state backend: bucket S3 (versionato, SSE, TLS-only, `prevent_destroy`) + tabella lock DynamoDB |
| `infra/global/{providers,variables,route53,acm,oidc,outputs}.tf` | Creato — zona Route53, 4 cert ACM (prod/test × us-east-1/eu-west-1) con validazione DNS condivisa, OIDC GitHub (ruoli `appgrove-github-actions-{test,prod}`, prod solo da tag) |
| `infra/modules/env_baseline/*.tf` | Creato — VPC no-NAT (2 subnet pubbliche, default SG chiuso), endpoint VPC cognito-idp+secretsmanager (1 AZ, cost-min), bucket export GDPR (SSE, lifecycle 7gg, TLS-only), parametro SSM `/appgrove/<env>/gdpr/export-bucket` |
| `infra/envs/{test,prod}/{main,outputs}.tf` | Creato — istanza `env_baseline` per env; state separati; test `force_destroy`, prod no |
| `infra/scripts/{_lib.sh,bootstrap,plan,up,down,output,check,first-run}` | Creato — wrapper con `--help`, guardrail prod (conferma digitata, doppia su `down prod`, `--yes` vietato su prod), `first-run` = prima accensione guidata |
| `infra/{.checkov.yaml,.tflint.hcl}` | Creato — config analisi statica |
| `infra/README.md` | Riscritto — struttura, quickstart comandi, prima accensione, teardown, scelte chiave |
| `run-tests.sh` | Modificato — area infra delega a `infra/scripts/check` (multi-root) |
| `.gitignore` | Modificato — esclusioni Terraform (`.terraform/`, `*.tfstate*`, piani); i `.terraform.lock.hcl` si committano |
| `docs/usecases/02-devops-infra/0003-fondamenta-terraform.md` | Modificato — punti aperti: bucket export ✅ risolto; attivazione differita (via `first-run`, al più tardi UC 0005); nota tflint |
| `docs/_BACKLOG.md` | Modificato — drift regione `eu-south-1` nei servizi vs `eu-west-1` deciso (#06 6) |
| `docs/usecases/_INDEX.md` | Modificato — UC 0003 → ✅ |

## Cosa è stato fatto

Fondamenta Terraform complete **come codice, senza creare alcuna risorsa AWS** (opzione A dei
requirements, attivazione graduale #12): bootstrap dello state, stack `global`, ambienti `test`/`prod`
via modulo condiviso `env_baseline`, script wrapper documentati per chi non conosce Terraform.
La prima accensione è orchestrata da `infra/scripts/first-run` (bootstrap → `up global` con
import automatico della hosted zone se già esistente → `up test`).

## Decisioni prese

- **Modulo interno `env_baseline`**: test e prod istanziano lo stesso modulo (differenze esplicite:
  CIDR e `force_destroy_buckets`) — niente duplicazione tra gli env.
- **Nomi bucket con suffisso account-id** (`appgrove-tfstate-<acct>`, `appgrove-gdpr-export-<env>-<acct>`):
  i nomi S3 sono globali; il backend riceve il nome via `-backend-config` dagli script (non cablabile nei `.tf`).
- **Endpoint VPC su 1 sola AZ** (cost-min ~14 $/mese/env, coerente con la stima in _COSTI-AWS);
  multi-AZ insieme all'hardening E1/E3.
- **Chiavi KMS gestite AWS** (SSE-S3/default), nessuna CMK: come da #06 §20bis ("CMK dove serve").
- **Permessi CI per-namespace** (niente `*:*`, IAM limitato al prefisso `appgrove-*`): la stretta
  per-risorsa è di UC 0005; soppressioni checkov documentate inline.
- **`prevent_destroy` su state bucket + tabella lock**: l'eliminazione finale del teardown è un passo
  manuale documentato nel README (#06 24).

## Invarianti appgrove

- **Modulo `microsaas_app`**: predisposta `infra/modules/` e le fondamenta (VPC, SSM, bucket) su cui
  UC 0004 innesta il modulo; nessuna infra per-servizio creata a mano.
- **Encryption ovunque** (#06 §20bis): SSE su tutti i bucket, policy TLS-only, SSM come da convenzione.
- Tenant dal JWT / filtro row-level / logging strutturato: non toccati (nessun codice applicativo).

## Note per il revisore

- **Nessuna risorsa AWS creata**: nessun apply/bootstrap eseguito; costo della change = 0.
- **Contratto servizio ↔ infra**: il parametro SSM `/appgrove/<env>/gdpr/export-bucket` pubblica il
  valore che il core legge come `appgrove.gdpr.export-bucket` (chiave della change 0028); in cloud le
  credenziali S3 arriveranno dal ruolo del task (UC 0004), niente `appgrove.s3.access-key`.
- **Decisioni differite tracciate**: (1) attivazione ambienti rimandata → UC 0003 "Punti aperti"
  (comando `first-run`, necessaria al più tardi con UC 0005); (2) drift regione `eu-south-1` nei
  default di core/fatture vs `eu-west-1` (#06 6) → `docs/_BACKLOG.md`; (3) tflint sparito da
  Homebrew → UC 0003 "Punti aperti" (in CI: action `setup-tflint`, UC 0005).
- **tflint non installato** in locale (formula rimossa da Homebrew; download diretto del binario non
  autorizzato in questa sessione): `scripts/check` lo salta con avviso. fmt/validate/checkov coprono
  la validazione; se vuoi tflint anche in locale, installa il binario dalle release ufficiali.
- La validazione ACM (`global`) richiede la delega dei name server: `first-run` lo segnala prima dell'apply.

## Test

- `./run-tests.sh infra` → **verde**. Esegue `infra/scripts/check`: `terraform fmt -check` su tutto,
  `terraform validate` (init `-backend=false`, nessuna credenziale) su 4 root (bootstrap, global,
  envs/test, envs/prod), checkov 3.3.0 (**75 pass, 0 fail, 10 skip documentati inline**), tflint saltato (vedi sopra).
- Smoke test script: `--help` di tutti e 7 gli script → exit 0; `plan`/`up` senza ambiente → errore
  guidato; `up prod` con conferma errata → annullato; `up prod --yes` → rifiutato; `down test` con
  conferma errata → annullato; `down prod` → doppia conferma verificata; ambiente sconosciuto → rifiutato.
- Gate privacy (UC 0031): `npm run privacy-scan` → **nessun segnale** (solo risorse infra, nessun dato personale).

## Stato criteri di accettazione

- [x] `fmt -check` + `validate` verdi su tutte le root senza credenziali AWS; `./run-tests.sh infra` verde.
- [x] checkov verde con soppressioni documentate (subnet pubbliche → E1, perimetro CI → UC 0005, …);
      **tflint**: non installabile via Homebrew (deviazione documentata: locale = release GitHub, CI = UC 0005).
- [x] Script wrapper eseguibili, `--help` funzionanti, guardrail prod attivi, `first-run` presente e
      documentato; nessuna risorsa AWS creata.
- [x] Bucket export GDPR per env con SSE + lifecycle 7 giorni + parametro SSM.
- [x] Attivazione differita tracciata in UC 0003; `_INDEX.md` 0003 → ✅.
