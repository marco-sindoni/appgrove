# appgrove

Marketplace di micro-SaaS multi-tenant su AWS — **monorepo** (un solo repo git).

Un backoffice shell in cui l'utente si autentica una volta e attiva le mini-applicazioni che
vuole; ogni app appare come voce di menù nella sidebar. Vedi
[recap_marketplace_microsaas.md](recap_marketplace_microsaas.md) per il contesto completo.

## Struttura (per tier)

```
appgrove/
├── infra/        # AWS CDK (TypeScript) — construct MicroSaasApp, provisiona tutto
├── frontend/     # LA React SPA: shell + UI delle app (modular monolith) → S3/CloudFront
├── services/     # N microservizi Quarkus (uno per app) → ECS Fargate
│   └── <app>/
├── changes/      # documentazione spec-driven delle change (skill new-change)
└── .claude/      # configurazione agente + skill
```

Asimmetria voluta: **N backend** (isolamento reale: deploy, schema DB, scaling separati) ma
**un solo frontend** per ora — i microfrontend sono distribuzione, non architettura, e si
rimandano finché non c'è un motivo. La cucitura (App Registry in `frontend/`) permette di
promuovere un'app a microfrontend in futuro senza riscrivere shell/auth/routing.

## Invarianti architetturali (non negoziabili)

1. **Tenant ID solo dal JWT verificato** — claim `tenant_id` (= account, iniettato dal Pre-Token-Gen Lambda); `sub` = user_id. Mai da request body/params
2. **Filtro row-level** `WHERE tenant_id = :tid` su ogni query tenant-scoped
3. **Construct `MicroSaasApp`** — nuova app = istanziare il construct, non infra bespoke
4. **Logging strutturato** ovunque: ogni log porta `tenant_id`, `app_id`, `user_id`

## Workflow delle change

Usa la skill `/new-change`: requirement → implementazione → log, con gate espliciti
(review, commit consent, merge consent). La documentazione vive in [changes/](changes/).

## Stack & test

| Area | Stack | Test |
|---|---|---|
| `infra/` | AWS CDK (TypeScript) | `npm test` |
| `frontend/` | React SPA | `npm test` |
| `services/<app>/` | Quarkus (Java) | `mvn test` |

Una change può toccare più aree in un singolo commit atomico (è il punto del monorepo);
i test girano per ogni area toccata.
