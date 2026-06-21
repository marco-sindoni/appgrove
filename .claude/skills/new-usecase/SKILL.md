---
name: new-usecase
description: >
  Creates a new numbered use case in the appgrove catalog (docs/usecases/). Assigns the next
  absolute 4-digit number NNNN (global across all area subfolders), scaffolds
  docs/usecases/<area>/NNNN-slug.md from the template, registers it in the master index
  docs/usecases/README.md, then helps fill the detailed drill-down. Use cases are later
  implemented one-by-one via the new-change skill (folder NNNN-use-case-YYYY-...).
triggers:
  - /new-usecase
  - /usecase
tier: tier1
stack_aware: false
---

# appgrove — New Use Case

You create **use case specifications** (detailed drill-downs) in the `docs/usecases/` catalog. A use case is a cohesive,
independently-implementable deliverable (≈ one `new-change`). It is a SPEC, not implementation: flows, screens/states,
data, permissions/gates, test requirements, decision references.

## Catalog rules (read `docs/usecases/README.md` first)

- **Numbering**: `NNNN` = **absolute, global, 4-digit** ID — unique across ALL area subfolders, never relative to a folder.
  Assigned as the next free number; it is a **stable ID** (do not renumber existing ones).
- **Areas** (subfolders `XX-area/`): `00-business-legal`, `01-devops-infra`, `02-local-dev`, `03-platform-core`,
  `04-auth`, `05-frontend`, `06-payments`, `07-compliance-gdpr`, `08-marketing-site`, `09-skills-tooling`, `10-apps`.
- **File**: `docs/usecases/<area>/NNNN-slug.md`. **Index**: `docs/usecases/README.md` (authoritative order = Phase + Dependencies).
- **Template**: `docs/usecases/_TEMPLATE.md`.

## Instructions

1. `step-01-scaffold.md` — determine the next `NNNN`, pick the area, create the file from the template, register it in the index.
2. `step-02-detail.md` — fill the drill-down with the developer (or note it's deferred), then ask commit consent.

## Mandatory gates — never skip

- **After scaffold (step-01): STOP for review.** Confirm number + area + title + index row with the developer before
  writing the detailed body.
- **At close (step-02): STOP for commit consent.** Do not commit until the developer explicitly consents.
- **Never invent decisions.** A use case must be consistent with the decision docs (`docs/01..14`, `CLAUDE.md`
  invariants). If a flow is unclear or admits alternatives, ask targeted questions first (like new-change's clarification gate).

## appgrove invariants to reflect in every use case

- **Tenant ID only from the verified JWT**; **row-level `WHERE tenant_id` filter**; **`microsaas_app` Terraform module**;
  **structured logging** (`tenant_id`/`app_id`/`user_id`). Note in §8 (Permessi & gate) how the use case upholds them.

## Token budget

Be concise in chat — the artifact (the use case `.md`) is the deliverable, not the conversation.
