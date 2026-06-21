# Change 0001: hook tracciati di new-change (privacy/RoPA + baseline E2E) + artefatti in italiano

**Branch**: `change/0001-use-case-0044-new-change-hooks`
**Aree**: skills (`.claude/skills/new-change/`) — solo Markdown, nessun codice eseguibile
**Data**: 2026-06-22
**Autore**: Platform Engineering
**Use case sorgente**: [docs/usecases/10-skills-tooling/0044-aggiornamento-skill-new-change.md](../../docs/usecases/10-skills-tooling/0044-aggiornamento-skill-new-change.md)
**Tocca dati personali?**: No (change su tooling/Markdown)

## Problema / Obiettivo

Portare `new-change` alla forma definitiva (UC 0044). La base è già fatta (`NNNN` a 4 cifre, variante use-case
`NNNN-use-case-YYYY`, campo "Use case sorgente", tre gate bloccanti, suite per-area alla chiusura, sync di `_INDEX.md`).
Restano da **encodare i due hook tracciati** (con l'enforcement CI vero demandato allo use case proprietario) e, su richiesta
dello sviluppatore, far sì che gli **artefatti** della skill siano scritti **in italiano**:

- **Hook privacy/RoPA** (#13 C) — emergere quando un diff tocca dati personali; il gate CI bloccante resta UC 0031.
- **Regola baseline visiva E2E** (#10 F) — "mai aggiornare una baseline alla cieca; un diff visivo inatteso si indaga".
- **Artefatti in italiano** — `requirements.md` e `implementation-log.md` (template della skill) in italiano; le istruzioni della skill restano in inglese.
- **Stile di domanda** — la skill pone le domande **una alla volta**, in prosa, verbose e ben spiegate, fermandosi e attendendo la risposta, con dialogo di approfondimento prima della risposta definitiva (no wizard a opzioni sintetiche).

## Scope

- `step-02-requirements.md`: campo **"Tocca dati personali? (Sì/No)"** nel template; **template tradotto in italiano**.
- `step-03-implement.md`:
  - **Checkpoint privacy/RoPA** — se il diff tocca dati personali (migrazioni DB nuove/modificate, campi entità/DTO/API, o
    nuova integrazione esterna), la skill avvisa e ricorda: classifica natura/finalità/base/retention, aggiorna manifesto +
    RoPA, bump versione PP/ToS (major→re-accept UC 0056 / minor→notifica). Enforcement = UC 0031 (qui è un promemoria guidato).
  - **Regola baseline** (#10 F) nella guida ai test; convenzione "artefatti in italiano".
- `step-04-close.md`: regola **baseline** (#10 F) nel run delle suite E2E/visual; **template implementation-log tradotto in italiano**.
- `SKILL.md`: riga che richiama i due hook tracciati; **sezione "Questioning style"** (una domanda alla volta, verbose, stop+dialogo, no wizard) + budget token rivisto (dialogo di chiarimento esente).
- `step-02-requirements.md`: clarification gate e domande dei requisiti riscritte per essere poste **una alla volta**, in prosa verbose, con stop dopo ciascuna.

## Fuori scope

- Enforcement CI vero del gate privacy (check ArchUnit `@PersonalData`, co-pilota di classificazione) → **UC 0031**.
- Macchina di assemblaggio manifesto/RoPA e versioning PP/ToS → UC 0030 / 0002 / 0056.
- Le parti già funzionanti (numerazione, gate, sync `_INDEX`, test per-area).
- Codice eseguibile in `infra/`, `frontend/`, `services/`; le **istruzioni** della skill (restano in inglese).

## Criteri di accettazione

- [ ] Il template requirements ha il campo "Tocca dati personali? (Sì/No)" con la nota di rinvio a UC 0031.
- [ ] step-03 ha un **checkpoint guidato** privacy/RoPA (si attiva sui diff con dati personali; ricorda classifica→manifesto/RoPA→bump PP/ToS; enforcement = UC 0031).
- [ ] La regola baseline #10 F è presente sia in step-03 sia in step-04.
- [ ] SKILL.md richiama i due hook tracciati.
- [ ] I template `requirements.md` e `implementation-log.md` sono **in italiano**; le istruzioni della skill restano in inglese.
- [ ] La skill istruisce a porre le domande **una alla volta**, verbose, con stop e dialogo (no wizard a opzioni sintetiche), in SKILL.md e step-02.
- [ ] Nessuna modifica al comportamento esistente (numerazione/gate/sync `_INDEX`/test per-area).

## Invarianti appgrove toccati

Nessuno a runtime (tooling). La skill resta **garante** degli invarianti: il checkpoint privacy rafforza l'accountability GDPR
(#13) e ogni change continua a documentare tenant_id-dal-JWT, filtro row-level, modulo `microsaas_app`, logging strutturato.

## Requisiti di test (opzionale)

Nessuno (change solo Markdown/skill → nessun codice eseguibile). Verifica funzionale: gli hook si leggono correttamente, i
template sono in italiano e i comportamenti già funzionanti non sono toccati.

## Valutazione di impatto

| Area | Impatto |
|---|---|
| Breaking change | No |
| Contratto cross-area | N/A |
| Version bump | nessuno (tooling/processo) |
