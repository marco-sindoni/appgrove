# appgrove

Marketplace di micro-SaaS multi-tenant su AWS — **monorepo** (un solo repo git).

Un backoffice shell in cui l'utente si autentica una volta e attiva le mini-applicazioni che
vuole; ogni app appare come voce di menù nella sidebar. Vedi
[recap_marketplace_microsaas.md](recap_marketplace_microsaas.md) per il contesto completo.

## Struttura (per tier)

```
appgrove/
├── infra/        # AWS CDK (TypeScript) — construct MicroSaasApp, provisiona tutto
├── frontend/     # workspace React (npm workspaces)
│   ├── apps/backoffice/      # SPA cliente + vetrina pubblica (modular monolith) → app.appgrove.app
│   ├── apps/admin/           # console platform-admin separata → admin.appgrove.app
│   └── packages/design-system/  # token/componenti/i18n/auth client condivisi
├── services/     # N microservizi Quarkus (uno per app) + core → ECS Fargate
│   └── <app>/
├── changes/      # documentazione spec-driven delle change (skill new-change)
└── .claude/      # configurazione agente + skill
```

Asimmetrie volute: **N backend** (isolamento reale: deploy, schema DB, scaling separati); il
**backoffice cliente** è un modular monolith (microfrontend rimandati — sono distribuzione, non
architettura; la cucitura App Registry consente di estrarne uno in futuro senza riscrivere shell/auth/routing);
la **console admin è un'app separata** (i tenant non scaricano mai il codice admin).

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
