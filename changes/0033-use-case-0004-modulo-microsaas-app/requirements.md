# Change 0033: Modulo Terraform `microsaas_app` + wrapper scripts

**Branch**: `change/0033-use-case-0004-modulo-microsaas-app`
**Aree**: infra (+ `run-tests.sh` alla root, docs)
**Data**: 2026-07-05
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/02-devops-infra/0004-modulo-microsaas-app.md](../../docs/usecases/02-devops-infra/0004-modulo-microsaas-app.md)
**Tocca dati personali?**: No — la change crea infrastruttura (schema DB vuoto, ruoli, code); nessun dato
applicativo viene raccolto o trattato. Il gate privacy (scanner UC 0031) gira comunque in step-03.

## Problema / Obiettivo

Realizzare l'**invariante architetturale #3**: una nuova app = istanziare il modulo Terraform
`microsaas_app`, non infrastruttura su misura. Oggi il modulo non esiste: `platform_shared` (UC 0055)
espone i punti di aggancio (cluster ECS, API Gateway+VPC Link, Cloud Map, bus EventBridge, Aurora) ma
nessun servizio è deployabile nel cloud. Servono anche i wrapper script per il ciclo di vita dei
servizi (`service-add`/`service-remove`) e per lo scale-to-0 dell'ambiente test (`test-start`/`test-stop`).

## Scope

1. **Modulo `infra/modules/microsaas_app/`** (file per-tema, come gli altri moduli). Un'istanza crea,
   per il servizio `<app_id>`:
   - **ECR repo** (immagine del servizio; scan on push, lifecycle immagini);
   - **ECS service + task definition** Fargate: 0.25 vCPU / 0.5 GB, 1 task, Spot in test / on-demand
     in prod (#06 9/10), architettura ARM64/Graviton (~-20% di costo), registrato nel cluster e nel
     namespace Cloud Map di `platform_shared` (record SRV: l'API Gateway scopre ip+porta);
   - **route API** `/api/<app_id>/v1/*` sull'API HTTP condivisa, integrazione via VPC Link + Cloud Map;
   - **ruolo DB + schema vuoto** (default `app_<app_id>`, parametrizzabile: il core usa `platform`,
     #05 11): password generata da Terraform e salvata in **Secrets Manager** per-app; creazione
     fisica di ruolo/schema/grant (least-privilege, solo il proprio schema) via **invocazione della
     Lambda `db-bootstrap`** con SQL idempotente. Le tabelle restano a Flyway (#06 23, UC 0005/0012);
   - **code SQS**: `tenant-purge-<app_id>` + DLQ e `gdpr-export-<app_id>` + DLQ (nomi **logici** = locale
     `dev/elasticmq.conf`, decisione differita di UC 0032), cifrate, con redrive. **Vincolo scoperto in
     implementazione**: test e prod convivono nello stesso account/regione AWS e i nomi SQS devono essere
     unici → il nome **fisico** è `appgrove-<env>-` + nome logico; il prefisso arriva ai servizi a runtime
     (env var `APPGROVE_SQS_QUEUE_PREFIX` nella task definition, vuota in locale — adeguamento di
     `services/commons` tracciato su UC 0005);
   - **regola EventBridge** sul bus condiviso: evento `tenant.offboarded` → target la coda
     `tenant-purge-<app_id>` (fan-in verso tutti i servizi, #06 H-19);
   - **log group** cifrato con retention esplicita: 7 giorni test, 30 prod (#08 26, #06 20bis);
   - parametri SSM/Secrets con la convenzione `/appgrove/<env>/...` per ciò che il task consuma.
2. **Estensioni a `platform_shared`** (risorse condivise, non per-app):
   - **Lambda `db-bootstrap`** in VPC (raggiunge Aurora e il master secret via endpoint interni);
   - coda condivisa **`gdpr-export-results`** + DLQ, consumata dal core (UC 0032).
3. **Istanze del modulo** per i due servizi esistenti — `platform` (core, schema `platform`) e
   `fatture` (schema `app_fatture`) — in **entrambi** gli ambienti `envs/test` e `envs/prod`.
   Nessun apply: l'attivazione resta differita (prima accensione guidata / CI UC 0005).
4. **Wrapper script** in `infra/scripts/`, stesso pattern di UC 0003 (`--help`, guardrail prod,
   conferma digitata, mai auto-approve su prod):
   - `service-add <app_id>` / `service-remove <app_id>`: aggiunge/rimuove il blocco `module` negli
     ambienti (il flusso normale resta PR→CI, #07 18/19/20); `service-remove` documenta nel `--help`
     la pulizia **manuale** di schema/ruolo DB (scelta prudente: nessun drop automatico);
   - `test-start` / `test-stop`: desired count 1↔0 dei servizi ECS in test (#07 28), idempotenti,
     no-op se non ci sono servizi; il cron giornaliero che li invoca è di UC 0005 (differito).
5. **`terraform test` sul modulo** (`*.tftest.hcl`, `command = plan`, senza credenziali AWS): dato un
   input, il plan produce le risorse attese (#10 29). Integrazione in `infra/scripts/check` e quindi
   in `run-tests.sh` (area infra), nello stesso commit.
6. **Docs**: aggiornamento `docs/_COSTI-AWS.md` (stima per-servizio quando acceso), README infra,
   sync decisione differita UC 0032 nel file dello use case, indice `_INDEX.md` → ✅ a chiusura.

## Fuori scope

- **Scaffold del codice del servizio** e generazione automatica del blocco `module` da skill →
  UC 0046 (`new-application`).
- **Pipeline CI/CD** (plan in PR, apply, cron giornaliero `test-stop` via workflow) → UC 0005.
- **Tabelle e migrazioni** negli schemi (Flyway) → UC 0005/0012 (già attive in locale).
- **Widget/allarmi CloudWatch** sul log group → UC 0006.
- **CORS sull'API condivisa** e **autorizzazione IAM sul RDS Proxy** → differite (UC 0004+0015 e
  UC 0014, già tracciate).
- **Drop automatico di schema/ruolo DB** su `service-remove` (manuale documentato).
- Nessun `terraform apply` su alcun ambiente.

## Criteri di accettazione

- [ ] `terraform validate` verde su `envs/test` e `envs/prod` con le istanze `platform` e `fatture`;
      `check` (fmt/validate/tflint/checkov) verde.
- [ ] `terraform test` sul modulo verde e agganciato a `run-tests.sh` (area infra).
- [ ] I nomi **logici** delle code SQS coincidono con quelli locali di `dev/elasticmq.conf`
      (`tenant-purge-platform|fatture` + DLQ, `gdpr-export-platform|fatture` + DLQ,
      `gdpr-export-results` + DLQ); il nome fisico aggiunge il prefisso d'ambiente
      `appgrove-<env>-` (vincolo: nomi SQS unici per account/regione).
- [ ] I 4 wrapper script rispettano il pattern UC 0003 (`--help`, guardrail prod) e `test-stop` è
      idempotente/no-op a lista vuota.
- [ ] Ruolo DB per-servizio least-privilege (grant solo sul proprio schema); schema creato vuoto.

## Invarianti appgrove toccati

- **Modulo `microsaas_app`**: questa change *è* l'invariante — la crea e la esercita con due istanze.
- **Filtro row-level / tenant dal JWT**: non toccati direttamente; il ruolo DB per-servizio
  (privilegi solo sul proprio schema) aggiunge la difesa in profondità prevista da #05 11.
- **Logging strutturato**: il modulo crea il log group con retention/cifratura; le convenzioni MDC
  restano nel codice dei servizi (già in `services/commons`).

## Requisiti di test

- `terraform test` sul modulo: dato `app_id = "demo"` (+ variante schema override), il plan contiene
  ECR, service ECS con la capacity provider attesa (Spot/on-demand), route API `/api/demo/v1/*`,
  code `tenant-purge-demo`/`gdpr-export-demo` + DLQ, regola EventBridge, log group con retention
  corretta per env, secret credenziali DB.
- Verifica (via plan con due istanze) che rimuovere un'istanza non alteri le risorse dell'altra.
- fmt/validate/tflint/checkov verdi su tutte le radici (via `infra/scripts/check`).

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | Sì — servizio ↔ infra: nomi logici code SQS = `GdprQueues`/locale; il prefisso fisico e regione/bucket viaggiano come env var della task definition (`APPGROVE_SQS_QUEUE_PREFIX`, `APPGROVE_SQS_REGION`, `APPGROVE_GDPR_EXPORT_BUCKET`, `QUARKUS_DATASOURCE_*`); l'adeguamento cloud di `services/commons` è tracciato su UC 0005 |
| Version bump | Nessuno |
