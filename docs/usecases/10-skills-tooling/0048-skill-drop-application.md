# UC 0048 — skill `drop-application`

**Area**: 10-skills-tooling · **Fase**: 6 · **Stato**: 🟢 deciso (skill da implementare)
**Dipendenze**: UC [0004](../02-devops-infra/0004-modulo-microsaas-app.md) (modulo/service-remove), UC [0046](0046-skill-new-application.md) (pattern app)
**Fonte decisioni**: #06 K (destroy safety), #07 19 (service-remove), #09 H35 (price archiviazione), #13 (purge/retention)
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [06-infra-iac](../../06-infra-iac.md), [07-devops-cicd](../../07-devops-cicd.md), [09-pagamenti](../../09-pagamenti.md), [13-compliance-privacy](../../13-compliance-privacy.md)

## 1. Obiettivo / Scope
Creare la skill **`drop-application`**: l'inverso di `new-application`, che **dismette un'app** in modo sicuro e conforme.
**Incluso**: `service-remove` (toglie il blocco `module` → `terraform destroy -target` con safety #06 K); **archiviazione price
Paddle** (no cancellazione con subscription attive — grandfathering/migrazione/disdetta gestite); **purge/retention** dei dati
dell'app (contratto GDPR `purgeData` + retention) con audit; rimozione modulo FE/manifest/CI/landing; comunicazione agli utenti
abbonati. Segue il workflow `new-change` (branch + PR).
**Escluso**: `new-application` (UC 0046), `pricing-change` (UC 0047), il framework purge in sé (UC 0032).

## 2. Attori & ruoli
- **Developer**: invoca `/drop-application <app_id>`, decide il trattamento degli abbonati, rivede la PR.
- **Skill** (tooling): orchestra dismissione infra + Paddle + dati + comunicazione.
- **Utenti abbonati**: ricevono comunicazione/disdetta/migrazione.

## 3. Precondizioni
- App esistente (UC 0046/0051); modulo `microsaas_app` (UC 0004); framework purge (UC 0032); pricing-as-code/sync (UC 0022).

## 4. Flusso principale
1. `/drop-application <app_id>` → la skill verifica subscription attive e propone il trattamento (migrazione/disdetta a fine periodo/comunicazione).
2. **Paddle**: **archivia** i price (mai cancellare con subscription attive); gestisce grandfathering/disdetta (#09 H35).
3. **Dati**: invoca `purgeData` per l'app (per tutti i tenant) con **audit**, rispettando retention/export prima della cancellazione (#13 L70, D).
4. **Infra**: `service-remove` → toglie il blocco `module` → al merge/tag `terraform destroy -target` mirato con **safety #06 K** (prod: snapshot/conferma) (#07 19).
5. **FE/CI/landing**: rimuove modulo/manifest/workflow + landing (o la marca dismessa); segue `new-change` → branch + **PR**.

## 5. Flussi alternativi / edge / errori
- **Subscription attive**: non si distrugge nulla finché non sono gestite (disdetta a fine periodo/migrazione) (#09 H35).
- **Export prima della cancellazione**: agli utenti va data la possibilità di esportare (diritti, #13 D); audit della purge (#13 L70).
- **Destroy prod**: snapshot + conferma esplicita (#06 K).
- **Rollback**: finché non si distrugge l'infra, la PR è reversibile.

## 6. Risorse & runbook
**File skill** `.claude/skills/drop-application/`. **Output**: branch + PR che rimuove modulo/infra/FE/CI/landing + archivia price +
pianifica purge. **Runbook**: `/drop-application` → decidere abbonati → PR → merge (test) → tag (prod) con safety.

## 7. Dati toccati
Cancella i dati dell'app (`app_<id>`) via `purgeData` con audit (dopo export/retention). Archivia price (catalogo). Manifest:
la dismissione chiude i trattamenti dell'app (aggiorna RoPA, UC 0030). `@PersonalData` cancellati.

## 8. Permessi & gate
- **Invarianti**: purge per scope con `tenant_id`; destroy mirato non tocca altri servizi; safety prod.
- **Gate**: workflow `new-change`; gestione abbonati prima della distruzione; diritti GDPR (export) garantiti prima della purge.

## 9. Requisiti di test
Skill di tooling: verifica che `service-remove`→destroy mirato non impatti altri servizi (`terraform test`/plan); price archiviati
(non cancellati con sub attive); purge con audit + no dati orfani; RoPA aggiornato.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #06 K, #07 19, #09 H35, #13 D/L70.
- **DoD**:
  1. `drop-application` dismette app: infra (service-remove+destroy mirato safety), Paddle (archivia price), dati (purge+audit), FE/CI/landing.
  2. Gestione abbonati (migrazione/disdetta) prima della distruzione; export garantito.
  3. Segue `new-change` (branch+PR); RoPA aggiornato.
  4. Test: destroy mirato isolato + purge no-orfani.
