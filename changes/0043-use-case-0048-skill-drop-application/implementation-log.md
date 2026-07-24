# Log di implementazione — Change 0043: skill `drop-application` (UC 0048)

**Branch**: `change/0043-use-case-0048-skill-drop-application` · **Modalità**: autopilot
**Aree toccate**: `services/core` (backend), `tools/drop-application` (tooling), `.claude/skills/drop-application` (skill), `run-tests.sh`, `docs/usecases` (tracciamenti).

## Cosa è stato costruito

La skill `drop-application` — inverso di `new-application` — in tre pezzi, come deciso in fase di requisiti:

### A. De-generatore deterministico — `tools/drop-application/`
- `remove.mjs` (`--dry-run` / `--skip-ropa`): rimuove i due alberi dell'app (`services/<id>`,
  `frontend/apps/backoffice/src/modules/<id>`) e i tre file nelle cartelle condivise (test end-to-end,
  manifesto dati, listino); disfa le cinque modifiche ai file condivisi; rigenera la RoPA.
- `lib/unedits.mjs`: l'inverso di `tools/new-application/lib/edits.mjs`, da cui **importa** `EDITED_FILES` e
  `toCamelCase` (sorgente unica → simmetria garantita, decisione 8). Le unedit tollerano l'assenza del
  marcatore (ri-eseguibili).
- `lib/plan.mjs`: le destinazioni da rimuovere, specchio di `buildPlan` del generatore.
- **Non** invoca `service-remove` né esegue atti irreversibili (decisione 13): li stampa nel runbook.
- Test (`tools/drop-application/test/`): **round-trip** genera→de-genera che verifica il ritorno
  byte-identico dei file condivisi (presidio anti-divergenza); inversi 1:1 di ogni modifica; parità
  `UNEDITORS == EDITED_FILES`; idempotenza; rifiuto di app inesistente; `--dry-run` che non tocca nulla.

### B. Primitiva di purga app-wide — `services/core`
- `gdpr/AppOffboarding.java`: gemello app-level di `TenantOffboarding`. Enumera i tenant con dati nell'app
  (`platform.subscription`, soft-deleted inclusi) e accoda un `TenantPurgeMessage` sulla coda
  `tenant-purge-<app_id>` per ciascuno, riusando `TenantPurgeConsumer` + audit + invalidazione proiezioni.
- `CoreMain.java`: dispatch del comando one-shot `offboard-app <app_id>` (accanto a `sync-pricing`/`migrate`);
  exit 1 se manca l'app_id.
- Test: `AppOffboardingTest` (fan-out isolato per app, soft-deleted inclusi, no-op senza tenant),
  `OffboardAppCommandTest` (percorso arg→exit del comando).

### C. Skill conversazionale — `.claude/skills/drop-application/`
- `SKILL.md` + `step-01..04`: identità/de-generazione → gate abbonati (escalation) → purga pianificata →
  runbook/chiusura. Chiude dentro `new-change`; nessun atto irreversibile eseguito dalla skill.

### D. Integrazione test
- `run-tests.sh` area `tooling` estesa con il collaudo del de-generatore.

## Decisioni di rilievo
Tutte in `decisions.json` (15 voci). In sintesi: struttura a due metà come il gemello (3); la skill non
esegue atti irreversibili (4); archiviazione price via YAML `inactive` + rimozione da index, soft-delete al
sync (5); primitiva `offboard-app` (6, approvata dallo sviluppatore, 10); abbonati come escalation (7);
simmetria de-generatore↔generatore via sorgente unica (8); il de-generatore non invoca `service-remove`
per il guardrail #06 K (13).

## Punti aperti tracciati (fuori scope, nei loro UC)
- Rimozione landing per-app → UC 0048 (vetrina non ancora esistente).
- Conservazione audit di erasure oltre il `DROP SCHEMA` → UC 0035.
- Esecuzione atti irreversibili → runbook, dopo il merge.

## Gate e test
- **Gate privacy (UC 0031)**: `npm run privacy-scan` → *nessun segnale*. Atteso: la change fornisce lo
  strumento per cancellare dati, non introduce nuovi trattamenti/campi `@PersonalData`. Classificazione:
  **minor**, nessun bump Informativa/Condizioni.
- **Rilevatore percorsi-sorgente**: nessun percorso-sorgente dei modelli toccato → varco non scattato.
- **Suite** (aree eseguibili toccate): `./run-tests.sh backend tooling` → **entrambe verdi** (backend ✓,
  tooling ✓, incluso il round-trip del de-generatore su Postgres reale). Le aree `compliance`/`frontend` non
  sono toccate dalla change (nessun manifesto reale né modulo frontend reale modificato: il de-generatore
  opera a runtime, non modifica quei file nel commit).
