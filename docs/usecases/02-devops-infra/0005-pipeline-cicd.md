# UC 0005 — Pipeline CI/CD (OIDC, terraform, backend build/test+`[graal]`, frontend, Flyway one-shot, prod gate, path-filter, Infracost)

**Area**: 02-devops-infra · **Fase**: 1 · **Stato**: 🟢 deciso
**Dipendenze**: UC [0003](0003-fondamenta-terraform.md), UC [0004](0004-modulo-microsaas-app.md)
**Fonte decisioni**: #07 (CI/CD), #06 (infra eseguita), #10 (gate test)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [07-devops-cicd](../../07-devops-cicd.md), [06-infra-iac](../../06-infra-iac.md), [10-testing](../../10-testing.md)

## 1. Obiettivo / Scope
Realizzare la **pipeline GitHub Actions** che porta il codice in infra e servizi in modo automatico, ripetibile, cost-min.
**Incluso**: autenticazione **OIDC** (no chiavi); **PR = solo verifica** (terraform plan commentato + suite completa con
path-filter); **merge→test = apply automatico**; **tag→prod = plan salvato + gate di approvazione + apply**; build backend
**JVM di default**, **native GraalVM on-demand** (`[graal]` o workflow_dispatch) e **sempre in prod**; build **2 SPA**
(stesso bundle, cambia `config.json`); **Flyway one-shot** (task ECS in VPC); **gate prod bloccante** (solo immagini native
già su ECR); **Infracost** sulle PR.
**Escluso**: definizione infra (UC 0003/0004), strategia di test in sé (#10), observability (UC 0006).

## 2. Attori & ruoli
- **Developer**: apre PR, fa squash-merge (può mettere `[graal]`), crea tag di release.
- **CI (GitHub Actions)**: esegue verifica/deploy con ruoli OIDC per env.
- **Approver**: dà l'OK al gate prod (GitHub Environments/required reviewer).

## 3. Precondizioni
- Foundation (UC 0003) con ruoli OIDC e state; modulo `microsaas_app` (UC 0004); repo con convenzione `new-change` (#07 B2).

## 4. Flusso principale
1. **PR** (`change/NNNN-*` → `main`): `terraform plan` commentato; suite completa (BE unit+integration+security+ArchUnit, FE
   component+**E2E**, contract+drift+**oasdiff**, infra fmt/validate/tflint/checkov/terraform test/**Infracost**) con **path-filter**
   (#10 34; security/E2E/oasdiff **sempre** bloccanti, #10 35). Nessuna risorsa AWS toccata.
2. **Merge su `main`** (squash): deploy **automatico in test** — `apply` su `envs/test`, `mvn test` → build immagine (JVM, o
   native se `[graal]`) → push **ECR taggata SHA** → **Flyway migrate** (task ECS one-shot in VPC, connessione **diretta** Agroal — il Proxy è solo per le Lambda, #05 dec.3) → **deploy ECS** rolling → health check (#07 10/14/15).
3. Frontend: build Vite → sync S3 (asset immutable, `index.html`/`config.json` no-cache) → **invalidation CloudFront** mirata;
   **`config.json` generato come output Terraform** (#07 11/12). Le **source map** sono prodotte ma **non pubblicate**: caricate come **artifact privato CI** per la de-minificazione offline degli errori frontend (#08 24, UC 0006 §5). Una modifica al `design-system` (path-filter) **ripubblica entrambe le SPA** (#07 dec.3).
4. **Tag `v*`** → prod: `plan` salvato → **approvazione** → `apply` del plan salvato; promozione **stesso SHA** (frontend
   stesso bundle, immagini native già su ECR) dietro **gate bloccante** (#07 21/23).
5. La CI **invoca gli stessi wrapper `infra/scripts/`** del locale (unica fonte di verità, #07 6).

## 5. Flussi alternativi / edge / errori
- **Gate prod senza immagine native**: pipeline del tag **fallisce** col messaggio guidato (run workflow native su stesso SHA,
  poi ri-tag; fallback PR `[graal]`) (#07 23).
- **Ordine migrazioni**: sempre `build → test → flyway migrate → deploy` (#07 15); disciplina **expand/contract** (#07 16).
- **Cron spegnimento test**: `test-stop` giornaliero (desired→0), idempotente, UTC fisso (#07 28).
- **Costi CI**: free tier via path-filter + caching `~/.m2`/npm + `concurrency` cancel + native solo on-demand (#07 27).

## 6. Risorse & runbook
**File**: `.github/workflows/*.yml` (verifica PR, deploy test, release prod, native toggle dispatch, cron test-stop).
**Runbook**: aprire PR → verde → squash-merge (deploy test) → quando pronto, **`[graal]` o native dispatch** sullo stesso
commit → **tag `v*`** → approvare il gate → prod. **Secrets**: la CI **non legge mai** i segreti app; al più li gestisce via
Terraform (genera password in Secrets Manager senza esporle) (#07 26).

## 7. Dati toccati
Nessun dato personale: la CI muove artefatti (immagini, bundle, plan). Flyway tocca **struttura** DB, non dati. Encryption/UE
residency ereditate dall'infra. Manifest GDPR N/A; il **gate privacy** (UC 0031) è un check CI separato sul contenuto del cambio.

## 8. Permessi & gate
- **Invarianti**: la **suite security/multi-tenancy gira SEMPRE** (mai esclusa dal path-filter) ed è bloccante (#10 35) →
  protegge tenant_id-da-JWT, filtro row-level. **OIDC least-privilege**, prod solo da tag (#07 25).
- **Gate**: approvazione prod; gate native bloccante; oasdiff/E2E/security bloccanti.

## 9. Requisiti di test
La pipeline **è** l'esecutore della strategia #10: deve far girare e **bloccare** sui rossi (security, E2E, oasdiff, contract
drift). Coverage **riportata, non bloccante** (#10 36). Smoke E2E opzionale su test al merge; **L3 Paddle smoke** pre-release
(UC 0029). Verifica: una PR con violazione cross-tenant **non passa**.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #07 1/2/3/4/6/7/10/11/12/14/15/21/23/25/26/27/28, #10 34/35/36/37, #06 (infra), #08 24 (source map artifact CI).
- **DoD**:
  1. OIDC attivo; PR = verifica, merge = test, tag = prod con gate.
  2. Backend JVM default + native `[graal]`/dispatch + sempre prod; gate prod bloccante senza native.
  3. Flyway one-shot in VPC prima del deploy; frontend stesso bundle + `config.json` da Terraform.
  4. Path-filter + Infracost + suite #10 bloccante sui rossi; CI ≈ $0.
