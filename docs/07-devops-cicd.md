# DevOps / CI-CD — Decisioni

**Stato**: 🟢 deciso
**Ultimo aggiornamento**: 2026-06-19

## Scope
Come il codice diventa infrastruttura e servizi in esecuzione, in modo automatico, ripetibile e cost-min:
piattaforma CI/CD, trigger, pipeline che **esegue** Terraform (#06), build/test/deploy di frontend e servizi,
migrazioni DB, lifecycle dei microservizi, promozione test→prod, secrets in CI, costi.
Non copre le scelte infra in sé (→ [06-infra-iac](06-infra-iac.md)) né observability/alerting (→ [08-observability](08-observability.md)).

## Vincoli ereditati (già decisi)
- Monorepo + skill `new-change` (branch `change/NNN-*`, PR verso `main`).
- 3 tier: `local` (offline, nessuna CI), `test` (AWS), `prod` (AWS) — #12.
- IaC = Terraform; state S3+DynamoDB; wrapper script `infra/scripts/` (#06 A, §25).
- Backend: JVM in dev, **native GraalVM in prod** (#04). DB migrations **via CI/CD** (#05).
- 2 SPA statiche (backoffice + admin) su S3+CloudFront, **config runtime `config.json`** (#03/#12).
- No-NAT: DB Aurora non raggiungibile da internet (#06 B); Aurora test+prod scale-to-0 (#06 E).

## Decisioni prese

### A. Piattaforma CI/CD
1. **GitHub Actions** (repo già su GitHub), **hosted runner**, autenticazione AWS via **OIDC** (niente chiavi
   AWS long-lived). Pipeline come YAML in `.github/workflows/`. Costo ~$0 nel free tier (2.000 min/mese, repo privato).
   Scartati: GitLab CI (sposterebbe il repo), AWS CodePipeline/CodeBuild (più verboso/costoso, anti cost-min),
   Jenkins/self-hosted (server da gestire).

### B. Branching & trigger
2. **Flusso trunk-based leggero** con la skill `new-change`:
   - **PR** (`change/NNN-*` → `main`): **solo verifica**, nessun deploy. `terraform plan` (commentato sulla PR) +
     build/test/lint delle aree toccate. Nessuna risorsa AWS modificata.
   - **Merge su `main`**: deploy **automatico in test** (`apply` su `envs/test`, push immagini, deploy servizi, frontend).
   - **Prod**: **manuale e controllato** via **tag/release** (`v*`, opzione b1), con **gate di approvazione**
     (GitHub Environments / required reviewer) prima dell'`apply`.
3. **Path-filtering** (monorepo): ogni pipeline parte **solo** per le aree toccate; il **design-system condiviso**
   → ripubblica **entrambe** le SPA.

### C. Pipeline infrastruttura (Terraform)
4. **OIDC verso AWS** (no chiavi). Sequenza: **PR** = `terraform plan` commentato sulla PR (no apply); **test** =
   `apply` automatico al merge; **prod** = `plan` salvato → **approvazione** → `apply` del **plan salvato**
   (si applica esattamente ciò che è stato approvato).
5. **State & lock** remoti su S3+DynamoDB (#06 A): il lock serializza apply concorrenti. **Bootstrap** (bucket+tabella)
   resta una-tantum manuale, non in CI.
6. **La CI invoca gli stessi wrapper script `infra/scripts/`** usati in locale (un'unica fonte di verità: niente
   logica Terraform duplicata negli YAML).

### D. Pipeline servizi backend (Quarkus) — JVM/native on-demand
7. **Build JVM di default in test** (veloce/economica). **Build native GraalVM in test on-demand**: se il messaggio
   del commit di **squash-merge** contiene il marcatore **`[graal]`** (case-insensitive). **Native sempre in prod.**
   Motivo: la native GraalVM è lenta/affamata di RAM → la si paga solo quando serve la **parità test↔prod**
   (tipicamente l'ultimo merge prima di un rilascio).
8. **Convenzione `squash merge`** per le PR (un commit pulito su `main`, messaggio editabile dove inserire `[graal]`).
9. **Toggle manuale** su `workflow_dispatch` (checkbox "build native") per forzare una build native sullo stesso
   commit senza un nuovo merge.
10. Resto pipeline servizio: `mvn test` (anche in PR) → build immagine (JVM/native) → **push su ECR** taggata con lo
    **SHA** → deploy ECS (rolling) → **health check**. Path-filtering per servizio.

### E. Pipeline frontend (2 SPA)
11. Build Vite → sync S3 (asset con hash = cache lunga `immutable`; `index.html`/`config.json` = no-cache) →
    **invalidation CloudFront** mirata su `index.html`/`config.json`. Test/lint in PR, deploy su merge→test e tag→prod.
12. **`config.json` generato come output di Terraform** e caricato su S3 dalla pipeline (unica fonte di verità,
    zero valori hardcoded). Bundle JS **identico** tra env.
13. **Build una volta + promozione dello stesso artifact** test→prod (cambia solo `config.json`): prod = esattamente
    ciò che è stato testato.

### F. Migrazioni DB (Flyway)
14. **Flyway come task ECS one-shot dentro la VPC** (raggiunge il DB via RDS Proxy, poi termina): nessuna esposizione
    del DB (coerente no-NAT), costo trascurabile. Scartati: step da runner CI (DB non raggiungibile da internet),
    Lambda di migrazione (timeout/packaging scomodi).
15. **Ordine**: `build → test → flyway migrate → deploy ECS` (migrazione **prima** del deploy del servizio).
16. **Disciplina expand/contract** (linea guida): migrazioni sempre retro-compatibili col codice in esecuzione →
    un solo task per servizio basta, niente downtime. Su test scale-to-0 il primo accesso risveglia Aurora (~10-15s).
17. Ogni servizio ha le **proprie migrazioni** versionate (schema `app_<app_id>`); il core le sue (schema `platform`).
    Terraform crea ruolo+schema vuoto (#06 §23), Flyway crea le tabelle.

### G. Lifecycle microservizi — principio "script genera, CI applica"
18. Aggiungere/rimuovere un microservizio passa dal **normale flusso PR→CI**: lo **script genera/modifica codice**
    (blocco `module "app_<id>"` + scaffold servizio), **la CI applica** con i gate di C (plan in PR, apply su test al
    merge, apply con approvazione su prod al tag).
19. **Rimozione** via `service-remove`: toglie il blocco `module` → al merge/tag `terraform destroy -target` (mirato),
    con le **safety di #06 K** (prod: snapshot/conferma esplicita prima di distruggere dati; test: teardown libero).
20. **L'invocazione manuale di `service-add` è sostituita dalla skill `new-application`** (vedi sotto): non si lancia
    lo scaffolding a mano.

#### Skill `new-application` (da implementare — vedi _BACKLOG)
- Invocazione: `/new-application <descrizione breve>` (es. "crea una calcolatrice scientifica").
- Fa **tutto il bootstrap**: scaffold **frontend** (nuova app/manifest) + **backend** (modulo Maven, Dockerfile,
  cartella migrazioni Flyway, logging strutturato di default con `tenant_id`/`app_id`/`user_id`), chiama **`service-add`**
  (istanza modulo `microsaas_app`), genera/aggiorna i **workflow CI** e il wiring `config.json`, più ogni altra azione
  di bootstrap necessaria.
- **Segue lo stesso workflow di `new-change`**: crea il branch con le modifiche e lascia la **PR all'utente**.
- Obiettivo: l'utente si concentra **solo sul business**; scaffolding + DevOps sono assorbiti dalla skill.
- **Quando implementarla**: dopo che la fotografia DevOps+cross-cutting è completa (almeno #07; idealmente dopo
  **#08 observability** e **#10 testing**, così lo scaffold nasce già con metriche/log e test pronti).

### H. Ambienti & promozione (artifact immutabili)
21. **Promozione = stesso SHA congelato dal tag.** Il tag (`v*`) su un commit di `main` già passato per test fissa lo
    SHA che va in prod (frontend, immagini, Terraform di quel commit), dietro **gate di approvazione**.
22. **Frontend**: stesso bundle test→prod (cambia solo `config.json`) — vedi E.
23. **Servizi**: **gate prod bloccante** — prod promuove **solo immagini native già su ECR per quello SHA**. Se manca,
    la pipeline del tag **fallisce con un messaggio d'errore chiaro** che invita a **lanciare il toggle native manuale
    (workflow_dispatch) sullo stesso commit** (via maestra; PR `[graal]` come fallback). Niente rebuild in prod.
24. **Terraform**: prod applica lo **stesso commit** già applicato in test (parità naturale).

#### Preview del messaggio d'errore del gate prod (decisione 23)
```
════════════════════════════════════════════════════════════════════
❌  RELEASE IN PROD BLOCCATA — manca l'immagine native (GraalVM)
════════════════════════════════════════════════════════════════════
Tag: v1.3.0   Commit (SHA): a1b2c3d
Servizi senza immagine native su ECR: core, app-<id>

Una release in prod promuove SOLO immagini GraalVM già costruite e testate.

✅ COME SBLOCCARTI (consigliato — nessun commit aggiuntivo)
  1. Actions → "Deploy test" → Run workflow
  2. Branch: main (fermo su a1b2c3d)
  3. ☑ build native (GraalVM)
  4. Avvia; quando è verde, ri-crea il tag su a1b2c3d.

↩️ Fallback: PR minima mergiata con [graal], poi tagga il nuovo commit.

Promemoria: fai SEMPRE l'ultimo merge pre-release con [graal].
════════════════════════════════════════════════════════════════════
```
I valori (tag, SHA, elenco servizi) sono dinamici.

### I. Secrets in CI
25. **OIDC con un IAM role per ambiente** (`github-actions-test`, `github-actions-prod`), **creati da Terraform** nello
    stack `global`, con **least privilege** e **trust policy** che vincola il repo e — per **prod** — l'assunzione
    **solo da tag** (`ref:refs/tags/*`): una PR qualunque non può toccare prod. **Nessuna chiave AWS** su GitHub.
26. **I segreti applicativi** (DB, app client Cognito, webhook Paddle) restano in **SSM/Secrets Manager** e li leggono i
    **servizi a runtime**: la **CI non li legge mai**, al più li gestisce come **risorse** via Terraform (es. genera una
    password e la scrive in Secrets Manager, senza esporla nei log). Masking attivo; gli script non stampano segreti.

### J. Gestione costi CI
27. **Restare nel free tier**: path-filtering (B/3), **caching dipendenze** (Maven `~/.m2`, npm), **`concurrency` con
    cancellazione** delle run superate; build native solo via `[graal]`/release. Costo CI ≈ **$0**.
28. **Start/stop test**:
    - **Avvio = manuale** (`test-start` via `workflow_dispatch`): porta i servizi a 1 task quando serve lavorare.
    - **Spegnimento Fargate = cron giornaliero attivo** che lancia **`test-stop`** (desired count → 0). Operazione
      **idempotente** (se già a 0, no-op). Orario = **UTC fisso** (~21:00 italiane; accettata la deriva di 1h per l'ora
      legale, è solo una rete di sicurezza notturna). `test-stop` resta lanciabile anche a mano.
    - **Aurora**: scale-to-0 **automatico da idle** (nessuna azione).
    - **Nota tecnica**: i task Fargate **non** si spengono da soli per inattività → serve `test-stop` (manuale o cron).
29. ECR (lifecycle ~10 immagini, #06 D) e S3/CloudFront = costi trascurabili.

## Questioni aperte
_Nessuna — #07 chiuso. Implementazione concreta dei workflow YAML + skill `new-application` → a backlog._

## Alternative valutate / scartate
- **GitLab CI / AWS CodePipeline / Jenkins** — scartati a favore di GitHub Actions (vedi 1).
- **Native GraalVM ovunque (anche test)** — scartato: build lenta/costosa a ogni merge; parità on-demand via `[graal]`.
- **Rebuild del frontend al tag** — scartato a favore di "build una volta + stesso artifact".
- **Flyway da runner CI / da Lambda** — scartati (DB non esposto / packaging scomodo) a favore del task ECS one-shot.
- **Invocazione manuale di `service-add`** — superata dalla skill `new-application`.
- **Schedule cron di avvio test** — non adottato (avvio on-demand); resta solo il cron di **spegnimento**.

## Impatti su altre aree
- [06-infra-iac](06-infra-iac.md), [04-services-backend](04-services-backend.md), [05-persistenza-dati](05-persistenza-dati.md),
  [03-frontend](03-frontend.md), [12-environments-config](12-environments-config.md), [02-auth-sicurezza](02-auth-sicurezza.md),
  [_COSTI-AWS.md](_COSTI-AWS.md), [_EVOLUZIONI-DEVOPS.md](_EVOLUZIONI-DEVOPS.md), [_BACKLOG.md](_BACKLOG.md)
