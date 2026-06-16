# appgrove — Riferimento applicativo

Indice delle decisioni tecnologiche, architetturali, DevOps e applicative del monorepo appgrove.
Questo file è il punto di ingresso: ogni area ha un documento dedicato in [docs/](docs/) che viene
popolato man mano che le scelte vengono dipanate (un argomento alla volta, per domande e risposte).

- Contesto di prodotto: [recap_marketplace_microsaas.md](recap_marketplace_microsaas.md)
- Struttura del repo: [README.md](README.md)

## Invarianti architetturali (non negoziabili)

1. **Tenant ID solo dal JWT verificato** — claim `tenant_id` (= account, iniettato dal Pre-Token-Gen Lambda); `sub` = user_id. Mai da request body/params
2. **Filtro row-level** `WHERE tenant_id = :tid` su ogni query tenant-scoped
3. **Modulo Terraform `microsaas_app`** — nuova app = istanziare il modulo, non infra bespoke
4. **Logging strutturato** ovunque: ogni log porta `tenant_id`, `app_id`, `user_id`

## Documenti di decisione

Legenda stato: 🔴 da definire · 🟡 in corso · 🟢 deciso

| # | Area | Documento | Stato |
|---|---|---|---|
| 01 | Architettura applicativa & multi-tenancy | [docs/01-architettura.md](docs/01-architettura.md) | 🟢 |
| 02 | Auth & sicurezza | [docs/02-auth-sicurezza.md](docs/02-auth-sicurezza.md) | 🟢 |
| 03 | Frontend | [docs/03-frontend.md](docs/03-frontend.md) | 🟢 |
| 04 | Backend / services (Quarkus) | [docs/04-services-backend.md](docs/04-services-backend.md) | 🟢 |
| 05 | Persistenza & dati | [docs/05-persistenza-dati.md](docs/05-persistenza-dati.md) | 🟢 |
| 06 | Infrastruttura / IaC (Terraform) | [docs/06-infra-iac.md](docs/06-infra-iac.md) | 🟢 |
| 07 | DevOps / CI-CD | [docs/07-devops-cicd.md](docs/07-devops-cicd.md) | 🔴 |
| 08 | Observability | [docs/08-observability.md](docs/08-observability.md) | 🔴 |
| 09 | Pagamenti (Paddle) | [docs/09-pagamenti.md](docs/09-pagamenti.md) | 🔴 |
| 10 | Testing strategy | [docs/10-testing.md](docs/10-testing.md) | 🔴 |
| 11 | Developer experience / local dev | [docs/11-developer-experience.md](docs/11-developer-experience.md) | 🟡 |
| 12 | Environments & config management | [docs/12-environments-config.md](docs/12-environments-config.md) | 🟢 |

## Casi d'uso (use case)

Specifiche di flusso dettagliate per funzionalità, in [docs/usecases/](docs/usecases/):
- [01 — Autenticazione & registrazione](docs/usecases/01-auth-registrazione.md) 🟢 (UC1–UC10)

## Backlog trasversale

Temi sollevati da affrontare nell'argomento giusto (o dedicato): [docs/_BACKLOG.md](docs/_BACKLOG.md)
— compliance/privacy (GDPR, tracking, T&C con Paddle MoR), configurazione admin, skill da creare.

## Costi AWS

Stima costi viva (principio: costo minimo compatibile coi requisiti), aggiornata a ogni decisione:
[docs/_COSTI-AWS.md](docs/_COSTI-AWS.md).

## Evoluzioni DevOps

Registro vivo delle scelte cost-min con il relativo percorso di hardening/scaling (NAT, ALB, HA, …):
[docs/_EVOLUZIONI-DEVOPS.md](docs/_EVOLUZIONI-DEVOPS.md).

## Come si decide (processo)

Un argomento alla volta. Per ciascuno: si elencano i topic, si risolvono per domande e risposte,
e le scelte confermate vengono scritte nel documento dell'area (template in
[docs/_TEMPLATE.md](docs/_TEMPLATE.md)). Lo stato nell'indice passa da 🔴 → 🟡 → 🟢.
Le decisioni prese qui sono vincolanti per la skill `/new-change`.
