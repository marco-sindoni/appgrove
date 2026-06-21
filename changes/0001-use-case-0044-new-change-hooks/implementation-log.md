# Implementation Log — Change 0001: hook tracciati di new-change (privacy/RoPA + baseline E2E) + artefatti in italiano

**Branch**: `change/0001-use-case-0044-new-change-hooks`
**Aree**: skills (`.claude/skills/new-change/`) — solo Markdown
**Completata**: 2026-06-22

## File modificati

| File | Azione |
|---|---|
| .claude/skills/new-change/SKILL.md | Modificato |
| .claude/skills/new-change/step-02-requirements.md | Modificato |
| .claude/skills/new-change/step-03-implement.md | Modificato |
| .claude/skills/new-change/step-04-close.md | Modificato |
| docs/usecases/_INDEX.md | Modificato (UC 0044 → ✅) |
| changes/0001-use-case-0044-new-change-hooks/requirements.md | Creato |
| changes/0001-use-case-0044-new-change-hooks/implementation-log.md | Creato |

## Cosa è stato fatto

Encodati i due **hook tracciati** che UC 0044 ancora richiedeva, completando la forma definitiva della skill, e tradotti gli
**artefatti** della skill in italiano (su richiesta dello sviluppatore):
- **Checkpoint privacy/RoPA** (step-03): si attiva quando un diff tocca dati personali (migrazioni/nuovi campi/nuova
  integrazione esterna) e ricorda classifica → manifesto/RoPA → bump versione PP/ToS (major→re-accept UC 0056 / minor→notifica),
  con escalation art. 9. Aggiunto il campo **"Tocca dati personali? (Sì/No)"** al template requirements (step-02).
- **Regola baseline visiva E2E** (#10 F) aggiunta a step-03 (guida ai test) e step-04 (run suite): mai ri-registrare una
  baseline alla cieca; i diff inattesi si indagano.
- **Artefatti in italiano**: tradotti i template di `requirements.md` (step-02) e `implementation-log.md` (step-04); la
  convenzione lingua in step-03 ora dice "artefatti in italiano, istruzioni skill in inglese". `SKILL.md` richiama i due hook.
- **Stile di domanda**: nuova sezione "Questioning style" in `SKILL.md` (una domanda alla volta, verbose, stop+dialogo, no
  wizard a opzioni sintetiche) + budget token rivisto (dialogo di chiarimento esente); clarification gate e domande dei
  requisiti in `step-02` riscritte per essere poste una alla volta con stop dopo ciascuna.

L'enforcement CI bloccante (check `@PersonalData` stile ArchUnit + co-pilota di classificazione) resta **UC 0031** — qui
`new-change` fa solo da promemoria guidato, così nulla sfugge prima che 0031 sia implementato.

## Decisioni prese

- Profondità hook privacy = **checkpoint guidato + flag nei requirements** (non solo promemoria minimale, non checklist PR completa) — scelta dello sviluppatore al clarification gate.
- Regola baseline collocata in **entrambi** step-03 e step-04 — scelta dello sviluppatore.
- Lingua: **artefatti** (`requirements.md`, `implementation-log.md`) in italiano; **istruzioni** della skill in inglese — su richiesta dello sviluppatore.
- Stile di domanda: **una alla volta**, verbose, con stop e dialogo (no wizard a opzioni sintetiche) — su richiesta dello sviluppatore; il budget token è stato rilassato per esentare il dialogo di chiarimento.

## Invarianti appgrove

Nessuno a runtime (tooling/Markdown). La skill resta **garante**: il checkpoint privacy rafforza l'accountability GDPR (#13) e
ogni change continua a documentare tenant_id-dal-JWT, filtro row-level, modulo `microsaas_app`, logging strutturato.

## Note per il revisore

- Change puramente Markdown/skill; nessun codice `infra/`, `frontend/`, `services/` toccato.
- Le parti già funzionanti (numerazione a 4 cifre, variante use-case, tre gate, test per-area alla chiusura, sync `_INDEX`) sono invariate.
- Scope ampliato rispetto al requirements iniziale per richiesta esplicita dello sviluppatore (artefatti in italiano) → requirements.md aggiornato di conseguenza.
- `_INDEX.md`: UC 0044 portato 🟡 (step-01) → ✅ (step-04), incluso in questa change; `main` mostra ✅ al merge.

## Test

Non applicabile — nessun codice eseguibile modificato (solo Markdown della skill + docs indice). Verifica funzionale per
lettura degli step modificati: hook e template (in italiano) si leggono correttamente; comportamenti esistenti intatti.

## Stato criteri di accettazione

- [x] Il template requirements ha il campo "Tocca dati personali? (Sì/No)" con la nota di rinvio a UC 0031.
- [x] step-03 ha un checkpoint guidato privacy/RoPA (si attiva sui diff con dati personali; ricorda classifica→manifesto/RoPA→bump PP/ToS; enforcement = UC 0031).
- [x] La regola baseline #10 F è presente sia in step-03 sia in step-04.
- [x] SKILL.md richiama i due hook tracciati.
- [x] I template `requirements.md` e `implementation-log.md` sono in italiano; le istruzioni della skill restano in inglese.
- [x] La skill istruisce a porre le domande una alla volta, verbose, con stop e dialogo (no wizard), in SKILL.md e step-02.
- [x] Nessuna modifica al comportamento esistente (numerazione/gate/sync `_INDEX`/test per-area).
